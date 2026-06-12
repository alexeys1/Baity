package com.shyeuar.baity.gui.owo;

import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class OwoRenderAdapter {
    private final OwoUIGraphics owoContext;
    private final GuiGraphicsExtractor guiGraphics;
    
    public OwoRenderAdapter(OwoUIGraphics owoContext, GuiGraphicsExtractor guiGraphics) {
        this.owoContext = owoContext;
        this.guiGraphics = guiGraphics;
    }
    
    public static OwoRenderAdapter of(OwoUIGraphics owoContext, GuiGraphicsExtractor guiGraphics) {
        return new OwoRenderAdapter(owoContext, guiGraphics);
    }
    
    public void drawText(net.minecraft.network.chat.Component text, float x, float y, float scale, int color) {
        owoContext.drawText(text, x, y, scale, color);
    }
    
    public void drawCircle(int centerX, int centerY, int segments, double radius, Color color) {
        owoContext.drawCircle(centerX, centerY, segments, radius, color);
    }
    
    public void drawRing(int centerX, int centerY, double fromDeg, double toDeg, int segments, 
                        double innerRadius, double outerRadius, Color innerColor, Color outerColor) {
        owoContext.drawRing(centerX, centerY, fromDeg, toDeg, segments, innerRadius, outerRadius, innerColor, outerColor);
    }
    
    public void fill(int x1, int y1, int x2, int y2, int color) {
        guiGraphics.fill(x1, y1, x2, y2, color);
    }
    
    public void drawString(net.minecraft.client.gui.Font font, String text, int x, int y, int color, boolean shadow) {
        guiGraphics.text(font, text, x, y, color, shadow);
    }
    
    public void drawString(net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int x, int y, int color, boolean shadow) {
        guiGraphics.text(font, text, x, y, color, shadow);
    }
    
    public void blit(Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight, int color) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight, color);
    }
    
    public void blit(Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
    
    public void enableScissor(int x, int y, int width, int height) {
        guiGraphics.enableScissor(x, y, width, height);
    }
    
    public void disableScissor() {
        guiGraphics.disableScissor();
    }
    
    public OwoUIGraphics getOwoContext() {
        return owoContext;
    }
    
    public GuiGraphicsExtractor getGuiGraphics() {
        return guiGraphics;
    }
}

