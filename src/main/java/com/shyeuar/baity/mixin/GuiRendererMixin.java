package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.shyeuar.baity.mixin.accessor.GuiRendererDrawAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @WrapOperation(
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
            Object indexType,
            Operation<Void> original,
            Object draw
    ) {
        GuiRendererDrawAccessor drawAccessor = (GuiRendererDrawAccessor) draw;
        RenderPipeline pipeline = drawAccessor.baity$pipeline();
        if (!"baity".equals(pipeline.getLocation().getNamespace())
                || pipeline.getPrimitiveTopology() == PrimitiveTopology.QUADS) {
            original.call(pass, buffer, indexType);
            return;
        }
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer =
                RenderSystem.getSequentialBuffer(pipeline.getPrimitiveTopology());
        pass.setIndexBuffer(shapeIndexBuffer.getBuffer(drawAccessor.baity$indexCount()), shapeIndexBuffer.type());
    }
}
