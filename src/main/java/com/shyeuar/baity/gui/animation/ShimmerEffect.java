package com.shyeuar.baity.gui.animation;

import net.minecraft.client.gui.GuiGraphics;

public class ShimmerEffect {

    public static void renderHoverShimmer(GuiGraphics graphics, float left, float top, float right, float bottom,
                                         float mouseX, float mouseY, boolean isActive, boolean isExiting,
                                         float progress, float direction, boolean isModuleEnabled) {
        if (!isActive && !isExiting && progress <= 0f) return;
        
        float width = right - left;
        float shimmerWidth = width * 0.3f;
        
        float centerX;
        float currentProgress = progress;
        
        if (isActive) {
            if (progress >= 1f) {
                centerX = Math.max(left, Math.min(right, mouseX));
            } else {
                float targetX = Math.max(left, Math.min(right, mouseX));
                float travelDistance = targetX - left + shimmerWidth / 2f;
                centerX = left - shimmerWidth / 2f + travelDistance * currentProgress;
            }
        } else if (isExiting) {
            float startX = Math.max(left, Math.min(right, mouseX));
            float travelDistance = width + shimmerWidth - (startX - left + shimmerWidth / 2f);
            float exitProgress = 1f - currentProgress;
            
            centerX = startX + travelDistance * exitProgress;
        } else {
            return;
        }
        
        float shimmerLeft = centerX - shimmerWidth / 2f;
        float shimmerRight = centerX + shimmerWidth / 2f;
        
        if (shimmerRight > left && shimmerLeft < right) {
            float drawLeft = Math.max(left, shimmerLeft);
            float drawRight = Math.min(right, shimmerRight);
            
            float maxDist = shimmerWidth / 2.0f;
            
            float baseAlpha;
            if (isActive && progress < 1f) {
                baseAlpha = progress;
            } else if (isExiting) {
                baseAlpha = currentProgress;
            } else {
                baseAlpha = 1.0f;
            }
            
            int ix = (int) drawLeft;
            int ix2 = (int) Math.ceil(drawRight);
            int widthI = Math.max(1, ix2 - ix);
            int slices = Math.min(40, Math.max(1, widthI / 2));
            int sliceW = Math.max(1, (int) Math.ceil(widthI / (double) slices));

            int rgb = isModuleEnabled ? 0xFFFFFF : 0xAAAAAA;

            for (int px = ix; px < ix2; px += sliceW) {
                int next = Math.min(ix2, px + sliceW);
                float sampleX = (px + next) * 0.5f;
                float distFromCenter = Math.abs(sampleX - centerX);
                float alpha = Math.max(0f, 1.0f - distFromCenter / maxDist);
                alpha = Math.max(0f, Math.min(1f, alpha));
                alpha *= baseAlpha;

                int color = ((int)(alpha * 0.5f * 255) << 24) | rgb;
                graphics.fill(px, (int) top, next, (int) bottom, color);
            }
        }
    }
}

