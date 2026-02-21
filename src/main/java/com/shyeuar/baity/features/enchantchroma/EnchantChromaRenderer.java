package com.shyeuar.baity.features.enchantchroma;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Vector2f;

import java.awt.Color;

@Environment(EnvType.CLIENT)
public final class EnchantChromaRenderer {

    private static final int BASE_CYCLE_MS = 4000;
    private static final float SHADOW_MULTIPLIER = 0.25f;

    private EnchantChromaRenderer() {}

    public static int[] computeGradient(Vector2f topLeft, Vector2f bottomLeft, Vector2f bottomRight, Vector2f topRight, boolean shadowed) {
        float timeProgress = calculateTimeProgress();
        Vector2f[] vertices = {topLeft, bottomLeft, bottomRight, topRight};
        int[] colors = new int[vertices.length];

        for (int i = 0; i < vertices.length; i++) {
            colors[i] = computeVertexColor(vertices[i].x, vertices[i].y, timeProgress);
        }

        if (shadowed) {
            applyShadowEffect(colors);
        }
        return colors;
    }

    private static float calculateTimeProgress() {
        long currentTime = System.currentTimeMillis();
        int fullCycle = (int) (BASE_CYCLE_MS / EnchantChromaConfig.ANIMATION_SPEED);
        float progress = (float) (currentTime % fullCycle) / (float) fullCycle;
        return 1.0f - progress;
    }

    private static int computeVertexColor(float x, float y, float timeProgress) {
        float waveLength = EnchantChromaConfig.GRADIENT_WIDTH;
        int flowDirection = EnchantChromaConfig.GRADIENT_ANGLE;
        
        double radians = Math.toRadians(flowDirection);
        double slope = Math.tan(radians);
        double projectionFactor = Math.cos(radians);
        
        double projectedY = (double) y + (double) x * slope;
        double wavePosition = Math.abs(projectedY) * projectionFactor;
        double phaseOffset = (wavePosition % waveLength) / waveLength;
        
        float hue = (float) ((timeProgress % 1.0f + phaseOffset) % 1.0);
        return Color.HSBtoRGB(hue, (float) EnchantChromaConfig.COLOR_SATURATION, 1.0f);
    }

    private static void applyShadowEffect(int[] colors) {
        for (int i = 0; i < colors.length; i++) {
            colors[i] = EnchantChromaConfig.applyShadow(colors[i], SHADOW_MULTIPLIER);
        }
    }
}
