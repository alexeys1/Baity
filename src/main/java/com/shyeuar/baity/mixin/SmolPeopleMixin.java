package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SmolPeopleMixin {

    @Mixin(PlayerEntityRenderer.class)
    public static class SmolNameTagMixin {
        
        @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
        private void baity$adjustNameTagHeight(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (ConfigManager.smolpeopleMode && mc.player != null && state.id == mc.player.getId()) {
                matrices.push();
                matrices.translate(0, -0.4, 0); 
            }
        }
        
        @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("RETURN"))
        private void baity$restoreNameTagHeight(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (ConfigManager.smolpeopleMode && mc.player != null && state.id == mc.player.getId()) {
                matrices.pop();
            }
        }
    }
    
    @Mixin(PlayerEntityRenderer.class)
    public static class SmolPlayerEntityRendererMixin {
        
        @Inject(method = "scale(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
        private void baity$additionalScale(PlayerEntityRenderState playerEntityRenderState, MatrixStack matrixStack, CallbackInfo ci) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (ConfigManager.smolpeopleMode && mc.player != null && playerEntityRenderState.id == mc.player.getId()) {
                matrixStack.scale(0.5f, 0.5f, 0.5f);
            }
        }
    }

    @Mixin(PlayerEntityModel.class)
    public static class SmolPlayerRendererMixin {
        
        @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
        private void baity$modifyModel(PlayerEntityRenderState playerEntityRenderState, CallbackInfo ci) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (ConfigManager.smolpeopleMode && mc.player != null && playerEntityRenderState.id == mc.player.getId()) {
                
                PlayerEntityModel model = (PlayerEntityModel) (Object) this;
                
                // 加速腿部和手臂摆动
                if (playerEntityRenderState.limbSwingAmplitude > 0) {
                    float speedMultiplier = 2.5f;
                    float enhancedLimbAngle = playerEntityRenderState.limbSwingAnimationProgress * speedMultiplier;
                    float enhancedLimbDistance = Math.min(playerEntityRenderState.limbSwingAmplitude * speedMultiplier, 1.0f);
                    
                    // 腿部：直接设置加速后的摆动
                    model.rightLeg.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f) * 1.4f * enhancedLimbDistance);
                    model.leftLeg.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f + Math.PI) * 1.4f * enhancedLimbDistance);
                    
                    // 手臂：在原始角度基础上叠加额外的摆动增量
                    // 计算原始摆动和加速摆动的差值，然后叠加到当前手臂角度上
                    float originalLimbAngle = playerEntityRenderState.limbSwingAnimationProgress;
                    float originalArmSwing = (float) (Math.cos(originalLimbAngle * 0.6662f + Math.PI) * 2.0f * playerEntityRenderState.limbSwingAmplitude * 0.5f);
                    float enhancedArmSwing = (float) (Math.cos(enhancedLimbAngle * 0.6662f + Math.PI) * 2.0f * enhancedLimbDistance * 0.5f);
                    float armSwingDelta = enhancedArmSwing - originalArmSwing;
                    
                    model.rightArm.pitch += armSwingDelta;
                    model.leftArm.pitch -= armSwingDelta;
                }
                
                try {
                    org.joml.Vector3f s = new org.joml.Vector3f(1.0f, 1.0f, 1.0f);
                    model.head.scale(s);
                } catch (Throwable ignored) {
                    // 忽略潜在的模型实现差异
                }
            }
        }
    }
}
