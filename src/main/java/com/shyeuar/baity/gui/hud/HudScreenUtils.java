package com.shyeuar.baity.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class HudScreenUtils {
    private static Minecraft getMc() {
        return Minecraft.getInstance();
    }
    
    public static int getScaledWidth() {
        return getMc().getWindow().getGuiScaledWidth();
    }
    
    public static int getScaledHeight() {
        return getMc().getWindow().getGuiScaledHeight();
    }
    
    public static int getDisplayWidth() {
        return getMc().getWindow().getWidth();
    }
    
    public static int getDisplayHeight() {
        return getMc().getWindow().getHeight();
    }
    
    private static int getGlobalMouseX() {
        return (int) getMc().mouseHandler.xpos();
    }
    
    private static int getGlobalMouseY() {
        return (int) getMc().mouseHandler.ypos();
    }
    
    public static int getMouseX() {
        int x = getGlobalMouseX() * getScaledWidth() / getDisplayWidth();
        if (getMc().getWindow().getWidth() > getMc().getWindow().getScreenWidth()) {
            x *= 2;
        }
        return x;
    }
    
    public static int getMouseY() {
        int height = getScaledHeight();
        int y = getGlobalMouseY() * height / getDisplayHeight();
        if (getMc().getWindow().getHeight() > getMc().getWindow().getScreenHeight()) {
            y *= 2;
        }
        return y;
    }
    
    public static int[] getMousePos() {
        return new int[]{getMouseX(), getMouseY()};
    }
    
    public static boolean isPointInRect(int x, int y, int left, int top, int width, int height) {
        return left <= x && x < left + width && top <= y && y < top + height;
    }
}
