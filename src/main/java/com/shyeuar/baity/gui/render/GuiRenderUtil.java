package com.shyeuar.baity.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

@Environment(EnvType.CLIENT)
public class GuiRenderUtil {
    
    public static void drawRoundedRect(GuiGraphics context, float x, float y, float x2, float y2, float radius, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)y2, color);
    }

    public static void stroke1px(GuiGraphics context, float x, float y, float x2, float y2, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)(y + 1), color);
        context.fill((int)x, (int)(y2 - 1), (int)x2, (int)y2, color);
        context.fill((int)x, (int)y, (int)(x + 1), (int)y2, color);
        context.fill((int)(x2 - 1), (int)y, (int)x2, (int)y2, color);
    }

    public static void divider(GuiGraphics context, float x, float y, float x2, float y2, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)y2, color);
    }
    
    
    public static boolean isHovered(float x, float y, float x1, float y1, float mouseX, float mouseY) {
        return mouseX > x && mouseY > y && mouseX < x1 && mouseY < y1;
    }
    
    public static void drawRoundedRectOutline(GuiGraphics context, float x, float y, float x2, float y2, float radius, int color) {
        context.fill((int)x, (int)y, (int)x2, (int)(y + 1), color);
        context.fill((int)x, (int)(y2 - 1), (int)x2, (int)y2, color);
        context.fill((int)x, (int)y, (int)(x + 1), (int)y2, color);
        context.fill((int)(x2 - 1), (int)y, (int)x2, (int)y2, color);
    }
    
    public static void drawCircle(GuiGraphics context, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }
}


