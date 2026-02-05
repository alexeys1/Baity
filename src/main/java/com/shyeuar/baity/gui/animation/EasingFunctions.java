package com.shyeuar.baity.gui.animation;

public class EasingFunctions {

    public static float easeOutCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float f = 1.0f - t;
        return 1.0f - f * f * f;
    }

    public static float easeInCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * t;
    }
}

