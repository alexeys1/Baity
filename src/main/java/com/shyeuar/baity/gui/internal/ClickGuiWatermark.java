package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.config.DevConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

@Environment(EnvType.CLIENT)
public final class ClickGuiWatermark {
    public static final String PREFIX = "Baity by ";
    public static final float SCALE = 0.70f;

    public record Layout(
        float baseX,
        float baseY,
        float handleX1,
        float handleX2,
        float handleY1,
        float handleY2,
        float lineY
    ) {}

    private ClickGuiWatermark() {
    }

    public static String handleName() {
        return DevConfig.getWatermarkHandle();
    }

    public static Layout layout(Minecraft client) {
        Font font = client.font;
        String handleName = handleName();

        int prefixWidth = font.width(PREFIX);
        int handleNameWidth = font.width(handleName);

        float totalScaledWidth = SCALE * (prefixWidth + handleNameWidth);
        float baseX = ClickGuiState.WIDTH - totalScaledWidth - 8;
        float baseY = 8;

        float handleX1 = baseX + SCALE * prefixWidth;
        float handleX2 = handleX1 + SCALE * handleNameWidth;
        float lineY = baseY + (int) (font.lineHeight * SCALE) + 1;
        float handleY1 = baseY;
        float handleY2 = baseY + (int) (font.lineHeight * SCALE);

        return new Layout(baseX, baseY, handleX1, handleX2, handleY1, handleY2, lineY);
    }

    public static boolean isHandleHovered(float mouseX, float mouseY, Layout layout) {
        return mouseX >= layout.handleX1() && mouseX <= layout.handleX2()
            && ((mouseY >= layout.handleY1() && mouseY <= layout.handleY2())
            || (mouseY >= layout.lineY() && mouseY <= layout.lineY() + 1));
    }
}
