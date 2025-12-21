package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SmolPeopleMixin {

    @Mixin(PlayerEntityRenderer.class)
    public static class SmolNameTagMixin {
        
        @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
        private void baity$adjustNameTagHeight(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) return;
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && state.id == mc.player.getId()) {
                matrices.translate(0, -0.4, 0); 
            }
        }
    }
    
    @Mixin(PlayerEntityRenderer.class)
    public static class SmolPlayerEntityRendererMixin {
        
        @Inject(method = "scale(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("TAIL"))
        private void baity$additionalScale(PlayerEntityRenderState playerEntityRenderState, MatrixStack matrixStack, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) return;
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && playerEntityRenderState.id == mc.player.getId()) {
                matrixStack.scale(0.5f, 0.5f, 0.5f);
            }
        }
    }

    @Mixin(PlayerEntityModel.class)
    public static class SmolPlayerRendererMixin {
        
        @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
        private void baity$modifyModel(PlayerEntityRenderState playerEntityRenderState, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) return;
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && playerEntityRenderState.id == mc.player.getId()) {
                
                PlayerEntityModel model = (PlayerEntityModel) (Object) this;
                
                if (playerEntityRenderState.limbSwingAmplitude > 0) {
                    float speedMultiplier = 2.5f;
                    float enhancedLimbAngle = playerEntityRenderState.limbSwingAnimationProgress * speedMultiplier;
                    float enhancedLimbDistance = Math.min(playerEntityRenderState.limbSwingAmplitude * speedMultiplier, 1.0f);
                    
                    model.rightLeg.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f) * 1.4f * enhancedLimbDistance);
                    model.leftLeg.pitch = (float) (Math.cos(enhancedLimbAngle * 0.6662f + Math.PI) * 1.4f * enhancedLimbDistance);
                    
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

    @Mixin(Camera.class)
    public static class SmolCameraMixin {
        
        @Shadow
        private Vec3d pos;
        
        @Unique
        private static final float SMOL_CAMERA_Y_OFFSET = -0.65f;
        
        @Inject(method = "update", at = @At("TAIL"))
        private void baity$adjustCameraForSmolPeople(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
            com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) return;
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (focusedEntity != mc.player) return;
            
            if (mc.options.getPerspective() != Perspective.THIRD_PERSON_FRONT) return;
            
            this.pos = new Vec3d(this.pos.x, this.pos.y + SMOL_CAMERA_Y_OFFSET, this.pos.z);
        }
    }
}
