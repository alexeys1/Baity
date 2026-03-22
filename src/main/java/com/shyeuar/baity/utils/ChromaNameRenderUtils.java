package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.sync.BaityPresenceSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Style;

@Environment(EnvType.CLIENT)
public final class ChromaNameRenderUtils {
    private ChromaNameRenderUtils() {
    }

    public static String handleString(String text) {
        if (text == null || text.isEmpty()) return text;
        List<Target> targets = collectTargets();
        if (targets.isEmpty()) return text;

        String lower = text.toLowerCase(Locale.ROOT);
        for (Target target : targets) {
            if (!lower.contains(target.nameLower())) continue;
            return applySectionColorToString(text, targets);
        }
        return text;
    }

    public static FormattedText handleFormattedText(FormattedText text) {
        return text;
    }

    public static Component handleComponent(Component component) {
        return component;
    }

    public static FormattedCharSequence handleCharSequence(FormattedCharSequence original) {
        if (original == null) return null;
        List<Target> targets = collectTargets();
        if (targets.isEmpty()) return original;

        List<Glyph> glyphs = toGlyphs(original);
        if (glyphs.isEmpty()) return original;

        TargetMatch[] matchByIndex = matchTargets(glyphs, targets);
        boolean matched = false;
        for (TargetMatch value : matchByIndex) {
            if (value != null) {
                matched = true;
                break;
            }
        }
        if (!matched) return original;

        long nowMs = System.currentTimeMillis();
        List<FormattedCharSequence> out = new ArrayList<>(glyphs.size());
        for (int i = 0; i < glyphs.size(); i++) {
            Glyph glyph = glyphs.get(i);
            Style style = glyph.style();
            TargetMatch match = matchByIndex[i];
            if (match != null) {
                int len = Math.max(1, match.length());
                double progress = len == 1 ? 0.0 : (double) (i - match.start()) / (len - 1);
                int rgb = match.target().colorAt(progress, nowMs);
                style = style.withColor(rgb);
            }
            out.add(FormattedCharSequence.codepoint(glyph.codepoint(), style));
        }
        return FormattedCharSequence.composite(out);
    }

    private static String applySectionColorToString(String text, List<Target> targets) {
        List<Glyph> glyphs = new ArrayList<>();
        text.codePoints().forEach(cp -> glyphs.add(new Glyph(cp, Style.EMPTY)));
        TargetMatch[] matchByIndex = matchTargets(glyphs, targets);

        StringBuilder sb = new StringBuilder();
        long nowMs = System.currentTimeMillis();
        for (int i = 0; i < glyphs.size(); i++) {
            TargetMatch match = matchByIndex[i];
            if (match != null) {
                int len = Math.max(1, match.length());
                double progress = len == 1 ? 0.0 : (double) (i - match.start()) / (len - 1);
                appendHexColor(sb, match.target().colorAt(progress, nowMs));
            }
            sb.appendCodePoint(glyphs.get(i).codepoint());
        }
        return sb.toString();
    }

    private static void appendHexColor(StringBuilder sb, int rgb) {
        String hex = String.format("%06X", rgb & 0xFFFFFF);
        sb.append('\u00A7').append('x');
        for (char c : hex.toCharArray()) {
            sb.append('\u00A7').append(c);
        }
    }

    private static List<Glyph> toGlyphs(FormattedCharSequence sequence) {
        List<Glyph> glyphs = new ArrayList<>();
        sequence.accept((index, style, codepoint) -> {
            glyphs.add(new Glyph(codepoint, style));
            return true;
        });
        return glyphs;
    }

    private static TargetMatch[] matchTargets(List<Glyph> glyphs, List<Target> targets) {
        TargetMatch[] out = new TargetMatch[glyphs.size()];
        for (Target target : targets) {
            int[] targetCodePoints = target.codePoints();
            if (targetCodePoints.length == 0 || targetCodePoints.length > glyphs.size()) continue;

            for (int i = 0; i <= glyphs.size() - targetCodePoints.length; i++) {
                if (!isRangeFree(out, i, targetCodePoints.length)) continue;
                if (!matchesAtIgnoreCase(glyphs, targetCodePoints, i)) continue;

                int end = i + targetCodePoints.length;
                boolean leftBoundary = i == 0 || !isNameCodepoint(glyphs.get(i - 1).codepoint());
                boolean rightBoundary = end >= glyphs.size() || !isNameCodepoint(glyphs.get(end).codepoint());
                if (!leftBoundary || !rightBoundary) continue;

                TargetMatch match = new TargetMatch(target, i, targetCodePoints.length);
                for (int j = 0; j < targetCodePoints.length; j++) {
                    out[i + j] = match;
                }
            }
        }
        return out;
    }

    private static boolean isRangeFree(TargetMatch[] matches, int start, int len) {
        for (int i = 0; i < len; i++) {
            if (matches[start + i] != null) return false;
        }
        return true;
    }

    private static boolean matchesAtIgnoreCase(List<Glyph> glyphs, int[] target, int offset) {
        for (int i = 0; i < target.length; i++) {
            int a = Character.toLowerCase(glyphs.get(offset + i).codepoint());
            int b = Character.toLowerCase(target[i]);
            if (a != b) return false;
        }
        return true;
    }

    private static boolean isNameCodepoint(int codepoint) {
        return Character.isLetterOrDigit(codepoint) || codepoint == '_';
    }

    private static List<Target> collectTargets() {
        Module module = ModuleManager.getModuleByName("NickTweaks");
        if (module == null || !module.isEnabled()) return List.of();

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return List.of();

        List<Target> targets = new ArrayList<>();
        String selfName = client.player.getName().getString();
        if (selfName != null && !selfName.isBlank()) {
            targets.add(Target.local(selfName));
        }

        if (client.level != null) {
            client.level.players().forEach(player -> {
                String name = player.getName().getString();
                if (name == null || name.isBlank()) return;
                BaityPresenceSync.ChromaProfile profile = BaityPresenceSync.getChromaProfileByName(name);
                if (profile == null) return;
                targets.add(Target.remote(name, profile));
            });
        }

        targets.removeIf(t -> t.name().isBlank());
        targets.sort(Comparator.comparingInt((Target t) -> t.codePoints().length).reversed());
        return targets;
    }

    private static int lerpRgb(int a, int b, double t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = (int) Math.round(ar + (br - ar) * t);
        int rg = (int) Math.round(ag + (bg - ag) * t);
        int rb = (int) Math.round(ab + (bb - ab) * t);
        return (rr << 16) | (rg << 8) | rb;
    }

    private static double positiveModulo(double value, double mod) {
        double result = value % mod;
        return result < 0 ? result + mod : result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Glyph(int codepoint, Style style) {
    }

    private record TargetMatch(Target target, int start, int length) {
    }

    private record Target(
            String name,
            String nameLower,
            int[] codePoints,
            boolean local,
            int[] palette,
            double speed,
            boolean remoteChromaEnabled,
            int remoteGradientStart,
            int remoteGradientEnd
    ) {
        static Target local(String name) {
            return new Target(
                    name,
                    name.toLowerCase(Locale.ROOT),
                    name.codePoints().toArray(),
                    true,
                    new int[0],
                    clamp(ConfigManager.nickTweaksChromaSpeed, 0.0, 8.0),
                    false,
                    0,
                    0
            );
        }

        static Target remote(String name, BaityPresenceSync.ChromaProfile profile) {
            return new Target(
                    name,
                    name.toLowerCase(Locale.ROOT),
                    name.codePoints().toArray(),
                    false,
                    profile.paletteView(),
                    clamp(profile.speed(), 0.0, 8.0),
                    profile.chromaEnabled(),
                    profile.gradientStart(),
                    profile.gradientEnd()
            );
        }

        int colorAt(double progress, long nowMs) {
            if (!local) {
                if (remoteChromaEnabled && palette.length > 0) {
                    double phase = (nowMs / 1000.0) * speed;
                    double position = positiveModulo(progress + phase, 1.0) * palette.length;
                    int fromIndex = Math.floorMod((int) Math.floor(position), palette.length);
                    int toIndex = Math.floorMod(fromIndex + 1, palette.length);
                    double frac = position - Math.floor(position);
                    return lerpRgb(palette[fromIndex], palette[toIndex], frac);
                }
                if (remoteGradientStart == remoteGradientEnd) {
                    return remoteGradientStart;
                }
                return lerpRgb(remoteGradientStart, remoteGradientEnd, progress);
            }

            if (!ConfigManager.nickTweaksChromaEnabled) {
                int localStart = ConfigManager.nickTweaksGradientStartColor & 0xFFFFFF;
                int localEnd = ConfigManager.nickTweaksGradientEndColor & 0xFFFFFF;
                if (localStart == localEnd) {
                    return localStart;
                }
                return lerpRgb(
                    localStart,
                    localEnd,
                    progress
                );
            }

            double lightness = clamp(ConfigManager.nickTweaksChromaLightness, 0.2, 1.0);
            double chroma = clamp(ConfigManager.nickTweaksChromaChroma, 0.0, 0.4);
            double size = Math.max(0.1, ConfigManager.nickTweaksChromaSize);
            double phase = (nowMs / 1000.0) * (speed * 0.5);
            float saturation = (float) (chroma / 0.4);
            float hue = (float) positiveModulo((progress / size) - phase, 1.0);
            return Mth.hsvToRgb(hue, saturation, (float) lightness);
        }
    }
}
