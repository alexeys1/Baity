package com.shyeuar.baity.features.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Environment(EnvType.CLIENT)
public final class OggPcmProcessor {
    private static final float CONTOUR_FRONT_WINDOW = 0.68f;
    private static final float CONTOUR_FRONT_INSERT_FACTOR = 2.0f;
    private static final float CONTOUR_TARGET_LENGTH_FACTOR = 1.00f;

    private static final float OPEN_FRONT_TIME_FRACTION = 0.32f;
    private static final float OPEN_FRONT_RATIO_SCALE = 0.95f;

    private static final float CLOSE_TAIL_WINDOW_SEC = 0.080f;
    private static final float CLOSE_TAIL_ADD_SEC = 0.520f;
    private static final float CLOSE_TAIL_DECAY_PER_WINDOW = 0.86f;
    private static final float CLOSE_TAIL_FADE_IN_SEC = 0.020f;
    private static final float CLOSE_TAIL_FADE_OUT_SEC = 0.360f;

    private static final float CENTER_PICK_MAX_LOG_DISTANCE = 0.10f;

    private static final float REVERB_TAIL_SEC = 0.450f;
    private static final float REVERB_WET = 0.32f;
    private static final float REVERB_DRY = 1.00f;
    private static final float REVERB_LP_ALPHA = 0.20f;

    private OggPcmProcessor() {}

    public static Decoded decodeBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            ByteBuffer ogg = MemoryUtil.memAlloc(bytes.length);
            ogg.put(bytes);
            ogg.flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer error = stack.mallocInt(1);
                long handle = STBVorbis.stb_vorbis_open_memory(ogg, error, null);
                if (handle == MemoryUtil.NULL) {
                    MemoryUtil.memFree(ogg);
                    return null;
                }

                STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                STBVorbis.stb_vorbis_get_info(handle, info);
                int channels = info.channels();
                int sampleRate = info.sample_rate();
                int samples = STBVorbis.stb_vorbis_stream_length_in_samples(handle);

                float[] interleaved = new float[samples * channels];
                int read = STBVorbis.stb_vorbis_get_samples_float_interleaved(handle, channels, interleaved);
                STBVorbis.stb_vorbis_close(handle);
                MemoryUtil.memFree(ogg);

                int frames = Math.max(0, Math.min(samples, read));
                if (frames == 0) return null;

                float[] mono = new float[frames];
                if (channels == 1) {
                    System.arraycopy(interleaved, 0, mono, 0, frames);
                } else {
                    for (int i = 0; i < frames; i++) {
                        float sum = 0.0f;
                        int base = i * channels;
                        for (int c = 0; c < channels; c++) {
                            sum += interleaved[base + c];
                        }
                        float v = sum;
                        if (v > 1.0f) v = 1.0f;
                        if (v < -1.0f) v = -1.0f;
                        mono[i] = v;
                    }
                }

                return new Decoded(mono, sampleRate);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    public static float[] resampleRate(float[] in, float rate) {
        if (in == null) return null;
        if (in.length == 0) return in;
        if (!(rate > 0.0f)) return in;
        int outLen = Math.max(1, (int) Math.floor(in.length / rate));
        float[] out = new float[outLen];
        for (int i = 0; i < outLen; i++) {
            float pos = i * rate;
            int a = (int) pos;
            float t = pos - a;
            float va = in[Math.min(a, in.length - 1)];
            float vb = in[Math.min(a + 1, in.length - 1)];
            out[i] = va + (vb - va) * t;
        }
        return out;
    }

    public static float[] invertPitchContour(float[] in, int sampleRate) {
        if (in == null || in.length < 1024 || sampleRate <= 0) return in;
        final int frame = 1024;
        final int hop = 256;
        final int frames = Math.max(1, 1 + (in.length - frame) / hop);
        final int minLag = Math.max(8, sampleRate / 1200);
        final int maxLag = Math.min(frame - 2, sampleRate / 60);

        float[] f0 = new float[frames];
        boolean[] voiced = new boolean[frames];
        int voicedCount = 0;

        for (int fi = 0; fi < frames; fi++) {
            int start = fi * hop;
            F0Result r = estimateF0Autocorr(in, start, frame, minLag, maxLag, sampleRate);
            f0[fi] = r.f0;
            voiced[fi] = r.voiced;
            if (r.voiced) voicedCount++;
        }

        if (voicedCount == 0) {
            return in;
        }

        float center = pickCenterF0(f0, voiced);
        float[] ratio = new float[frames];
        for (int i = 0; i < frames; i++) {
            float r = 1.0f;
            if (voiced[i] && f0[i] > 1e-6f) {
                float target = (center * center) / f0[i];
                r = target / f0[i];
            }
            float frontAttenuation = frontPitchAttenuation(i, frames);
            r *= frontAttenuation;
            ratio[i] = clamp(r, 0.5f, 2.0f);
        }

        int frontFrames = Math.max(1, Math.round(frames * OPEN_FRONT_TIME_FRACTION));
        for (int i = 0; i < frontFrames; i++) {
            ratio[i] = clamp(ratio[i] * OPEN_FRONT_RATIO_SCALE, 0.5f, 2.0f);
        }
        smoothInPlace(ratio, 2);
        normalizeMeanInPlace(ratio);
        float[] ratioExpanded = expandFrontContour(ratio, CONTOUR_FRONT_WINDOW, CONTOUR_FRONT_INSERT_FACTOR);
        int outLen = Math.max(1, Math.round(in.length * CONTOUR_TARGET_LENGTH_FACTOR));
        float[] out = variableRateResampleFixedLength(in, ratioExpanded, outLen);
        matchRmsInPlace(out, in);

        softenInPlace(out, 1);
        return out;
    }

    public static float[] extendTail(float[] in, int sampleRate) {
        if (in == null || in.length == 0 || sampleRate <= 0) return in;
        int window = Math.max(8, Math.round(sampleRate * CLOSE_TAIL_WINDOW_SEC));
        int add = Math.max(0, Math.round(sampleRate * CLOSE_TAIL_ADD_SEC));
        if (add == 0) return in;
        window = Math.min(window, in.length);

        int fadeIn = Math.max(0, Math.round(sampleRate * CLOSE_TAIL_FADE_IN_SEC));
        int fadeOut = Math.max(0, Math.round(sampleRate * CLOSE_TAIL_FADE_OUT_SEC));

        float[] out = new float[in.length + add];
        System.arraycopy(in, 0, out, 0, in.length);

        int tailStart = in.length - window;
        float gain = 1.0f;
        int written = 0;
        while (written < add) {
            int chunk = Math.min(window, add - written);
            for (int i = 0; i < chunk; i++) {
                float s = in[tailStart + i] * gain;
                int idx = in.length + written + i;
                float gIn = 1.0f;
                if (fadeIn > 0 && written + i < fadeIn) {
                    float p = (written + i) / (float) fadeIn;
                    gIn = p * p * (3.0f - 2.0f * p);
                }
                float gOut = 1.0f;
                int fromEnd = add - 1 - (written + i);
                if (fadeOut > 0 && fromEnd < fadeOut) {
                    float p = fromEnd / (float) fadeOut;
                    gOut = p * p * (3.0f - 2.0f * p);
                }
                out[idx] = s * gIn * gOut;
            }
            written += chunk;
            gain *= CLOSE_TAIL_DECAY_PER_WINDOW;
            if (gain < 0.02f) break;
        }
        return out;
    }

    public static float[] addCaveReverb(float[] in, int sampleRate) {
        if (in == null || in.length == 0 || sampleRate <= 0) return in;
        int tail = Math.max(0, Math.round(sampleRate * REVERB_TAIL_SEC));
        if (tail == 0) return in;

        float[] out = new float[in.length + tail];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[i] * REVERB_DRY;
        }

        int d1 = Math.max(1, Math.round(sampleRate * 0.035f));
        int d2 = Math.max(1, Math.round(sampleRate * 0.071f));
        int d3 = Math.max(1, Math.round(sampleRate * 0.113f));
        int d4 = Math.max(1, Math.round(sampleRate * 0.163f));

        float y1 = 0.0f;
        float y2 = 0.0f;
        float y3 = 0.0f;
        float y4 = 0.0f;

        for (int n = 0; n < out.length; n++) {
            float x = n < in.length ? in[n] : 0.0f;

            float r = 0.0f;
            int a1 = n - d1;
            int a2 = n - d2;
            int a3 = n - d3;
            int a4 = n - d4;
            if (a1 >= 0) r += out[a1] * 0.34f;
            if (a2 >= 0) r += out[a2] * 0.24f;
            if (a3 >= 0) r += out[a3] * 0.18f;
            if (a4 >= 0) r += out[a4] * 0.12f;

            y1 += REVERB_LP_ALPHA * (r - y1);
            y2 += REVERB_LP_ALPHA * (y1 - y2);
            y3 += REVERB_LP_ALPHA * (y2 - y3);
            y4 += REVERB_LP_ALPHA * (y3 - y4);

            out[n] += (x * 0.0f) + (y4 * REVERB_WET);
        }

        float[] trimmed = out;
        int end = trimmed.length;
        while (end > 1 && Math.abs(trimmed[end - 1]) < 1e-6f) end--;
        if (end != trimmed.length) {
            float[] t = new float[end];
            System.arraycopy(trimmed, 0, t, 0, end);
            trimmed = t;
        }
        return trimmed;
    }

    public static short[] toPcm16(float[] in) {
        if (in == null) return null;
        float peak = 0.0f;
        for (float v : in) {
            float a = Math.abs(v);
            if (a > peak) peak = a;
        }
        float scale = 1.0f;
        if (peak > 0.0f) {
            float target = 0.90f;
            float s = target / peak;
            if (s < 1.0f) {
                scale = s;
            } else {
                scale = Math.min(6.0f, s);
            }
        }
        short[] out = new short[in.length];
        for (int i = 0; i < in.length; i++) {
            float v = in[i] * scale;
            if (v > 1.0f) v = 1.0f;
            if (v < -1.0f) v = -1.0f;
            out[i] = (short) Math.round(v * 32767.0f);
        }
        return out;
    }

    private static void matchRmsInPlace(float[] out, float[] ref) {
        if (out == null || ref == null || out.length == 0 || ref.length == 0) return;
        double sr = 0.0;
        for (float v : ref) sr += v * v;
        double so = 0.0;
        for (float v : out) so += v * v;
        if (so <= 1e-12) return;
        double rr = Math.sqrt(sr / ref.length);
        double ro = Math.sqrt(so / out.length);
        if (ro <= 1e-12) return;
        float scale = (float) (rr / ro);
        for (int i = 0; i < out.length; i++) {
            out[i] *= scale;
        }
    }

    private static F0Result estimateF0Autocorr(float[] x, int start, int frame, int minLag, int maxLag, int sr) {
        double e0 = 0.0;
        for (int i = 0; i < frame; i++) {
            float v = x[start + i];
            e0 += v * v;
        }
        if (e0 < 1e-6) return new F0Result(0.0f, false);

        double best = -1.0;
        int bestLag = -1;
        for (int lag = minLag; lag <= maxLag; lag++) {
            double num = 0.0;
            double e1 = 0.0;
            for (int i = 0; i < frame - lag; i++) {
                float a = x[start + i];
                float b = x[start + i + lag];
                num += a * b;
                e1 += b * b;
            }
            double den = Math.sqrt(e0 * Math.max(e1, 1e-9));
            double corr = den > 0 ? (num / den) : 0.0;
            if (corr > best) {
                best = corr;
                bestLag = lag;
            }
        }
        boolean voiced = best > 0.35 && bestLag > 0;
        float f0 = voiced ? (float) sr / bestLag : 0.0f;
        return new F0Result(f0, voiced);
    }

    private static float geometricMedianF0(float[] f0, boolean[] voiced) {
        float[] logs = new float[f0.length];
        int n = 0;
        for (int i = 0; i < f0.length; i++) {
            if (voiced[i] && f0[i] > 1e-6f) logs[n++] = (float) Math.log(f0[i]);
        }
        java.util.Arrays.sort(logs, 0, n);
        float med = logs[n / 2];
        return (float) Math.exp(med);
    }

    private static float pickCenterF0(float[] f0, boolean[] voiced) {
        float reference = geometricMedianF0(f0, voiced);
        float refLog = (float) Math.log(reference);

        int firstClose = -1;
        int closest = -1;
        float closestDist = Float.POSITIVE_INFINITY;

        for (int i = 0; i < f0.length; i++) {
            if (!voiced[i]) continue;
            float v = f0[i];
            if (!(v > 1e-6f)) continue;
            float d = Math.abs((float) Math.log(v) - refLog);
            if (d < closestDist) {
                closestDist = d;
                closest = i;
            }
            if (firstClose < 0 && d <= CENTER_PICK_MAX_LOG_DISTANCE) {
                firstClose = i;
            }
        }

        int idx = firstClose >= 0 ? firstClose : closest;
        if (idx < 0) return reference;
        return f0[idx] > 1e-6f ? f0[idx] : reference;
    }

    private static void smoothInPlace(float[] v, int radius) {
        if (v == null || v.length == 0) return;
        float[] tmp = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            float sum = 0.0f;
            int c = 0;
            for (int j = Math.max(0, i - radius); j <= Math.min(v.length - 1, i + radius); j++) {
                sum += v[j];
                c++;
            }
            tmp[i] = sum / Math.max(1, c);
        }
        System.arraycopy(tmp, 0, v, 0, v.length);
    }

    private static void normalizeMeanInPlace(float[] v) {
        if (v == null || v.length == 0) return;
        float sum = 0.0f;
        for (float x : v) sum += x;
        float mean = sum / v.length;
        if (mean <= 1e-6f) return;
        for (int i = 0; i < v.length; i++) v[i] /= mean;
    }

    private static void softenInPlace(float[] v, int radius) {
        if (v == null || v.length == 0) return;
        if (radius <= 0) return;
        float[] tmp = new float[v.length];
        float denom = radius + 1.0f;
        for (int i = 0; i < v.length; i++) {
            float sum = 0.0f;
            float wsum = 0.0f;
            int j0 = Math.max(0, i - radius);
            int j1 = Math.min(v.length - 1, i + radius);
            for (int j = j0; j <= j1; j++) {
                float dist = Math.abs(i - j);
                float w = 1.0f - (dist / denom);
                sum += v[j] * w;
                wsum += w;
            }
            tmp[i] = wsum <= 1e-6f ? v[i] : (sum / wsum);
        }
        System.arraycopy(tmp, 0, v, 0, v.length);
    }

    private static float[] variableRateResampleFixedLength(float[] in, float[] ratio, int outLen) {
        float[] out = new float[outLen];
        float srcPos = 0.0f;
        for (int i = 0; i < outLen; i++) {
            float progress = outLen <= 1 ? 1.0f : (i / (float) (outLen - 1));
            float r = ratioAtProgress(ratio, progress);
            if (r < 0.05f) r = 0.05f;
            srcPos += r;
            int a = (int) srcPos;
            if (a >= in.length - 1) {
                out[i] = 0.0f;
                continue;
            }
            float t = srcPos - a;
            float va = in[a];
            float vb = in[a + 1];
            out[i] = va + (vb - va) * t;
        }
        return out;
    }

    private static float ratioAtProgress(float[] ratio, float progress) {
        float framePos = progress * (ratio.length - 1);
        int a = (int) Math.floor(framePos);
        int b = Math.min(ratio.length - 1, a + 1);
        if (a < 0) return ratio[0];
        if (a >= ratio.length) return ratio[ratio.length - 1];
        float t = framePos - a;
        return ratio[a] + (ratio[b] - ratio[a]) * t;
    }

    private static float clamp(float v, float lo, float hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static float frontPitchAttenuation(int idx, int total) {
        if (total <= 1) return 1.0f;
        float p = idx / (float) (total - 1);
        float window = CONTOUR_FRONT_WINDOW;
        if (p >= window) return 1.0f;
        float t = p / window;
        float eased = t * t * (3.0f - 2.0f * t);
        float min = 0.88f;
        return min + (1.0f - min) * eased;
    }

    private static float[] expandFrontContour(float[] ratio, float window, float insertFactor) {
        if (ratio == null || ratio.length == 0) return ratio;
        if (!(insertFactor > 1.0f)) return ratio;
        int front = Math.max(2, Math.min(ratio.length, Math.round(ratio.length * window)));
        int extraPerEdge = Math.max(1, Math.round(insertFactor - 1.0f));
        java.util.ArrayList<Float> out = new java.util.ArrayList<>(ratio.length + (front - 1) * extraPerEdge);
        for (int i = 0; i < ratio.length - 1; i++) {
            float a = ratio[i];
            float b = ratio[i + 1];
            out.add(a);
            if (i < front - 1) {
                float p = front <= 1 ? 1.0f : (i / (float) (front - 1));
                float lift = interpolationLift(p);
                for (int k = 1; k <= extraPerEdge; k++) {
                    float t = k / (float) (extraPerEdge + 1);
                    float v = a + (b - a) * t;
                    out.add(v * lift);
                }
            }
        }
        out.add(ratio[ratio.length - 1]);
        float[] arr = new float[out.size()];
        for (int i = 0; i < out.size(); i++) arr[i] = out.get(i);
        return arr;
    }

    private static float interpolationLift(float progress) {
        if (progress >= 1.0f) return 1.0f;
        if (progress <= 0.0f) return 1.08f;
        float eased = progress * progress * (3.0f - 2.0f * progress);
        return 1.08f + (1.0f - 1.08f) * eased;
    }

    private record F0Result(float f0, boolean voiced) {}

    public record Decoded(float[] mono, int sampleRate) {}
}