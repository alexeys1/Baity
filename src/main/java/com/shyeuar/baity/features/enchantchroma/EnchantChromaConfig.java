package com.shyeuar.baity.features.enchantchroma;

import com.shyeuar.baity.config.ConfigManager;

public final class EnchantChromaConfig {

    private EnchantChromaConfig() {}

    public static boolean isEnabled() {
        return ConfigManager.enchantChromaEnabled;
    }

    public static final double ANIMATION_SPEED = 1.0;
    public static final double COLOR_SATURATION = 0.35;
    public static final float GRADIENT_WIDTH = 100f;
    public static final int GRADIENT_ANGLE = 45;

    public static final int MARKER_COLOR = -11910870;
    public static final int MARKER_COLOR_SHADOW = applyShadow(MARKER_COLOR, 0.25f);

    public static final int COLOR_GREAT = 0xFFAA00;
    public static final int COLOR_GOOD = 0x5555FF;
    public static final int COLOR_POOR = 0xAAAAAA;
    public static final int COLOR_ULTIMATE = 0xFF55FF;

    public static int applyShadow(int color, float factor) {
        int alpha = extractComponent(color, 24);
        int red = extractComponent(color, 16);
        int green = extractComponent(color, 8);
        int blue = extractComponent(color, 0);

        red = (int) (red * factor);
        green = (int) (green * factor);
        blue = (int) (blue * factor);

        return combineComponents(alpha, red, green, blue);
    }

    private static int extractComponent(int color, int shift) {
        return (color >> shift) & 0xFF;
    }

    private static int combineComponents(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
