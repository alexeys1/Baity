package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @Redirect(
            method = "executeDraw(Lnet/minecraft/client/gui/render/GuiRenderer$Draw;Lcom/mojang/blaze3d/systems/RenderPass;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderPass;setIndexBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)V"
            ),
            require = 1
    )
    private void baity$fixNonQuadIndexing(
            RenderPass pass,
            GpuBuffer buffer,
            VertexFormat.IndexType indexType,
            GuiRenderer.Draw draw
    ) {
        RenderPipeline pipeline = draw.pipeline();
        if (!"baity".equals(pipeline.getLocation().getNamespace())
                || pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            pass.setIndexBuffer(buffer, indexType);
            return;
        }
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
                RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
        pass.setIndexBuffer(shapeIndexBuffer.getBuffer(draw.indexCount()), shapeIndexBuffer.type());
    }
}
