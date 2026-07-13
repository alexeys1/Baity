package com.shyeuar.baity.utils;

import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4fc;

public final class FloatingWorldTextCompat {

    private FloatingWorldTextCompat() {
    }

    public static boolean usesHookedFontEngine(Font font) {
        if (font == null) {
            return false;
        }
        return font.getSplitter().getClass() != StringSplitter.class;
    }

    public static void beginFrame() {
        RenderScope.enterFloatingWorldText();
    }

    public static void endFrame() {
        RenderScope.exitFloatingWorldText();
    }

    public static void drawInBatch(
            Font font,
            Component text,
            float x,
            float y,
            int color,
            Matrix4fc matrix,
            MultiBufferSource buffers,
            int packedLight
    ) {
        font.drawInBatch(
                text,
                x,
                y,
                color,
                false,
                matrix,
                buffers,
                Font.DisplayMode.SEE_THROUGH,
                0,
                packedLight
        );
    }
}