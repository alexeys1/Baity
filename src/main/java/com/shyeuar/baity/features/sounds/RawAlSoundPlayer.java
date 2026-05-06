package com.shyeuar.baity.features.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.openal.AL10;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class RawAlSoundPlayer {
    private static final List<Entry> active = new ArrayList<>();

    private RawAlSoundPlayer() {}

    public static void tickCleanup() {
        for (Iterator<Entry> it = active.iterator(); it.hasNext(); ) {
            Entry e = it.next();
            int state = AL10.alGetSourcei(e.source, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED) {
                AL10.alDeleteSources(e.source);
                AL10.alDeleteBuffers(e.buffer);
                it.remove();
            }
        }
    }

    public static boolean playMono16(short[] pcm, int sampleRate, float gain, float x, float y, float z, boolean relative) {
        try {
            AL10.alGetError();
        } catch (Throwable t) {
            return false;
        }

        if (pcm == null || pcm.length == 0) return false;
        if (sampleRate <= 0) return false;

        int buffer = AL10.alGenBuffers();
        int source = AL10.alGenSources();
        int err0 = AL10.alGetError();
        if (err0 != AL10.AL_NO_ERROR) {
            return false;
        }

        java.nio.ByteBuffer data = java.nio.ByteBuffer.allocateDirect(pcm.length * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (short v : pcm) {
            data.putShort(v);
        }
        data.flip();

        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, data, sampleRate);
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
        AL10.alSource3f(source, AL10.AL_POSITION, x, y, z);
        AL10.alSourcef(source, AL10.AL_ROLLOFF_FACTOR, relative ? 0.85f : 1.0f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, relative ? 2.0f : 1.0f);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, relative ? 40.0f : 64.0f);
        AL10.alSourcef(source, AL10.AL_GAIN, gain);
        AL10.alSourcef(source, AL10.AL_PITCH, 1.0f);
        AL10.alSourcePlay(source);
        int err = AL10.alGetError();
        if (err != AL10.AL_NO_ERROR) {
            AL10.alDeleteSources(source);
            AL10.alDeleteBuffers(buffer);
            return false;
        }

        active.add(new Entry(source, buffer));
        return true;
    }

    private record Entry(int source, int buffer) {}
}