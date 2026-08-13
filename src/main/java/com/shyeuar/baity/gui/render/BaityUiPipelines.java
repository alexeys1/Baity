package com.shyeuar.baity.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class BaityUiPipelines {

    public static RenderPipeline GUI_TRIANGLE_FAN;
    public static RenderPipeline GUI_TRIANGLE_STRIP;

    private BaityUiPipelines() {
    }

    public static void register() {
        GUI_TRIANGLE_FAN = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath("baity", "pipeline/gui_triangle_fan"))
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
                        .build()
        );
        GUI_TRIANGLE_STRIP = RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath("baity", "pipeline/gui_triangle_strip"))
                        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
                        .build()
        );
    }
}
