package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.shyeuar.baity.features.MotionBlur;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class MotionBlurMixin {

    @Mixin(LevelRenderer.class)
    public abstract static class LevelRendererMixin {

        @Inject(method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", at = @At("HEAD"))
        private void baity$onRenderHead(
                GraphicsResourceAllocator resourceAllocator,
                DeltaTracker deltaTracker,
                boolean renderOutline,
                CameraRenderState cameraState,
                Matrix4fc modelViewMatrix,
                GpuBufferSlice terrainFog,
                Vector4f fogColor,
                boolean shouldRenderSky,
                CallbackInfo ci) {
            MotionBlur.onRenderHead(
                    resourceAllocator,
                    modelViewMatrix,
                    cameraState.projectionMatrix,
                    cameraState.pos.x(),
                    cameraState.pos.y(),
                    cameraState.pos.z());
        }

        @Inject(method = "render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", at = @At("TAIL"))
        private void baity$onRenderLevelTail(
                GraphicsResourceAllocator resourceAllocator,
                DeltaTracker deltaTracker,
                boolean renderOutline,
                CameraRenderState cameraState,
                Matrix4fc modelViewMatrix,
                GpuBufferSlice terrainFog,
                Vector4f fogColor,
                boolean shouldRenderSky,
                CallbackInfo ci) {
            if (MotionBlur.isActive()) {
                MotionBlur.onAfterLevel();
            }
        }
    }
}
