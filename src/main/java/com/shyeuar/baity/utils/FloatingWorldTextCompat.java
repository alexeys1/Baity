package com.shyeuar.baity.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;

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
            PoseStack poseStack,
            SubmitNodeCollector submits,
            int packedLight
    ) {
        submits.submitText(
                poseStack,
                x,
                y,
                text.getVisualOrderText(),
                false,
                Font.DisplayMode.SEE_THROUGH,
                packedLight,
                color,
                0,
                0
        );
    }
}
