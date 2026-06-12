package com.shyeuar.baity.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public class GuiRenderUtil {
    
    public static void drawRoundedRect(GuiGraphicsExtractor context, float x, float y, float x2, float y2, float radius, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)y2, color);
    }

    public static void stroke1px(GuiGraphicsExtractor context, float x, float y, float x2, float y2, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)(y + 1), color);
        context.fill((int)x, (int)(y2 - 1), (int)x2, (int)y2, color);
        context.fill((int)x, (int)y, (int)(x + 1), (int)y2, color);
        context.fill((int)(x2 - 1), (int)y, (int)x2, (int)y2, color);
    }

    public static void divider(GuiGraphicsExtractor context, float x, float y, float x2, float y2, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)y2, color);
    }
    
    
    public static boolean isHovered(float x, float y, float x1, float y1, float mouseX, float mouseY) {
        return mouseX > x && mouseY > y && mouseX < x1 && mouseY < y1;
    }
    
    public static void drawRoundedRectOutline(GuiGraphicsExtractor context, float x, float y, float x2, float y2, float radius, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)(y + 1), color);
        context.fill((int)x, (int)(y2 - 1), (int)x2, (int)y2, color);
        context.fill((int)x, (int)y, (int)(x + 1), (int)y2, color);
        context.fill((int)(x2 - 1), (int)y, (int)x2, (int)y2, color);
    }
    
    public static void drawCircle(GuiGraphicsExtractor context, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    public static void drawGradientRect(GuiGraphicsExtractor context, float x, float y, float x2, float y2, 
                                       int colorStart, int colorEnd, float radius) {
        int ix = (int) x;
        int iy = (int) y;
        int ix2 = (int) x2;
        int iy2 = (int) y2;
        int width = ix2 - ix;
        
        if (width <= 0) return;
        
        int a1 = (colorStart >> 24) & 0xFF;
        int r1 = (colorStart >> 16) & 0xFF;
        int g1 = (colorStart >> 8) & 0xFF;
        int b1 = colorStart & 0xFF;
        
        int a2 = (colorEnd >> 24) & 0xFF;
        int r2 = (colorEnd >> 16) & 0xFF;
        int g2 = (colorEnd >> 8) & 0xFF;
        int b2 = colorEnd & 0xFF;
        
        int slices = Math.min(64, Math.max(1, width / 2));
        int sliceW = Math.max(1, (int) Math.ceil(width / (double) slices));
        for (int px = ix; px < ix2; px += sliceW) {
            int next = Math.min(ix2, px + sliceW);
            float t = (float) ((px + next) * 0.5f - ix) / (float) width;
            t = Math.max(0f, Math.min(1f, t));
            int a = (int)(a1 + (a2 - a1) * t);
            int rVal = (int)(r1 + (r2 - r1) * t);
            int g = (int)(g1 + (g2 - g1) * t);
            int b = (int)(b1 + (b2 - b1) * t);
            int color = (a << 24) | (rVal << 16) | (g << 8) | b;
            context.fill(px, iy, next, iy2, color);
        }
    }

    public static void drawFrostedGlass(GuiGraphicsExtractor context, float x, float y, float x2, float y2, int baseColor, float radius) {
        drawRoundedRect(context, x, y, x2, y2, radius, baseColor);
        
        int overlay1 = ((int)(0.15f * 255) << 24) | (255 << 16) | (255 << 8) | 255;
        drawRoundedRect(context, x, y, x2, y2, radius, overlay1);
        
        int overlay2 = ((int)(0.08f * 255) << 24) | (255 << 16) | (255 << 8) | 255;
        drawRoundedRect(context, x, y, x2, y2, radius, overlay2);
    }

    public static void draw3DRect(GuiGraphicsExtractor context, float x, float y, float x2, float y2, int baseColor, float radius) {
        int ix = (int) x;
        int iy = (int) y;
        int ix2 = (int) x2;
        int iy2 = (int) y2;
        
        drawRoundedRect(context, x, y, x2, y2, radius, baseColor);
        
        int baseR = (baseColor >> 16) & 0xFF;
        int baseG = (baseColor >> 8) & 0xFF;
        int baseB = baseColor & 0xFF;
        
        int highlightR = Math.min(255, baseR + 50);
        int highlightG = Math.min(255, baseG + 50);
        int highlightB = Math.min(255, baseB + 50);
        int highlightAlpha = (int)(0.6f * 255);
        int highlightColor = (highlightAlpha << 24) | (highlightR << 16) | (highlightG << 8) | highlightB;
        
        int shadowR = Math.max(0, baseR - 40);
        int shadowG = Math.max(0, baseG - 40);
        int shadowB = Math.max(0, baseB - 40);
        int shadowAlpha = (int)(0.7f * 255);
        int shadowColor = (shadowAlpha << 24) | (shadowR << 16) | (shadowG << 8) | shadowB;
        
        context.fill(ix, iy, ix2, iy + 2, highlightColor);
        
        context.fill(ix, iy, ix + 2, iy2, highlightColor);
        
        context.fill(ix, iy2 - 2, ix2, iy2, shadowColor);
        
        context.fill(ix2 - 2, iy, ix2, iy2, shadowColor);
    }

    public static void draw3DGradientRect(GuiGraphicsExtractor context, float x, float y, float x2, float y2, 
                                        int colorStart, int colorEnd, float radius) {
        int ix = (int) x;
        int iy = (int) y;
        int ix2 = (int) x2;
        int iy2 = (int) y2;
        int width = ix2 - ix;
        
        if (width <= 0) return;
        
        drawGradientRect(context, x, y, x2, y2, colorStart, colorEnd, radius);
        
        int avgR = (((colorStart >> 16) & 0xFF) + ((colorEnd >> 16) & 0xFF)) / 2;
        int avgG = (((colorStart >> 8) & 0xFF) + ((colorEnd >> 8) & 0xFF)) / 2;
        int avgB = ((colorStart & 0xFF) + (colorEnd & 0xFF)) / 2;
        
        int highlightR = Math.min(255, avgR + 50);
        int highlightG = Math.min(255, avgG + 50);
        int highlightB = Math.min(255, avgB + 50);
        int highlightAlpha = (int)(0.6f * 255);
        int highlightColor = (highlightAlpha << 24) | (highlightR << 16) | (highlightG << 8) | highlightB;
        
        int shadowR = Math.max(0, avgR - 40);
        int shadowG = Math.max(0, avgG - 40);
        int shadowB = Math.max(0, avgB - 40);
        int shadowAlpha = (int)(0.7f * 255);
        int shadowColor = (shadowAlpha << 24) | (shadowR << 16) | (shadowG << 8) | shadowB;
        
        context.fill(ix, iy, ix2, iy + 2, highlightColor);
        
        context.fill(ix, iy, ix + 2, iy2, highlightColor);
        
        context.fill(ix, iy2 - 2, ix2, iy2, shadowColor);
        
        context.fill(ix2 - 2, iy, ix2, iy2, shadowColor);
    }
}


