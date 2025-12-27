package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.shyeuar.baity.utils.AntiSwimUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class AntiSwimMixin {

    @Mixin(PlayerEntityRenderer.class)
    public static class PlayerRendererTransformMixin {

        @Inject(method = "setupTransforms(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V",
                at = @At("HEAD"))
        private void baity$preventSwimTransform(PlayerEntityRenderState state, MatrixStack matrixStack, float f, float g, CallbackInfo ci) {
            if (!AntiSwimUtils.isSelfPlayerById(state.id)) return;
            if (!AntiSwimUtils.isFeatureActive()) return;

            if (state.isSwimming || state.leaningPitch > 0.0F) {
                state.isSwimming = false;
                state.leaningPitch = 0.0F;

                if (AntiSwimUtils.isSneaking()) {
                    state.isInSneakingPose = true;
                }
            }
        }
    }

    @Mixin(PlayerEntityModel.class)
    public static class PlayerModelPoseMixin {

        @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
                at = @At("HEAD"))
        private void baity$modifySwimmingPose(PlayerEntityRenderState state, CallbackInfo ci) {
            if (!AntiSwimUtils.isSelfPlayerById(state.id)) return;
            if (!AntiSwimUtils.isFeatureActive()) return;

            if (state.isSwimming || state.leaningPitch > 0.0F) {
                state.isSwimming = false;
                state.leaningPitch = 0.0F;

                if (AntiSwimUtils.isSneaking()) {
                    state.isInSneakingPose = true;
                }
            }
        }
    }

    @Mixin(Camera.class)
    public static class PlayerCameraMixin {

        @ModifyExpressionValue(method = "update", 
                at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 1))
        private float baity$modifyEyeHeightLerp(float original, @Local(argsOnly = true) Entity focusedEntity) {
            if (!AntiSwimUtils.isFeatureActive()) return original;
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || focusedEntity != mc.player) return original;
            
            if (mc.player.isTouchingWater()) {
                return AntiSwimUtils.STANDING_EYE_HEIGHT;
            }

            return original;
        }
    }

    @Mixin(PlayerEntityRenderer.class)
    public static class PlayerNameTagMixin {

        @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
                at = @At("HEAD"))
        private void baity$adjustNameTagHeight(PlayerEntityRenderState state, net.minecraft.client.util.math.MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo ci) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (!AntiSwimUtils.isSelfPlayerById(state.id)) return;
            if (!AntiSwimUtils.isFeatureActive()) return;
            if (mc.player.getPose() != EntityPose.SWIMMING) return;

            matrices.translate(0, 1.2, 0);
        }
    }

    @Mixin(Entity.class)
    public static class PlayerRaycastMixin {

        @Inject(method = "getCameraPosVec", at = @At("HEAD"), cancellable = true)
        private void baity$modifyCameraPosVec(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
            Entity self = (Entity) (Object) this;
            if (!(self instanceof PlayerEntity)) return;
            if (!AntiSwimUtils.isSelfPlayer(self)) return;
            if (!AntiSwimUtils.isFeatureActive()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;
            if (mc.player.getPose() != EntityPose.SWIMMING) return;

            double x = mc.player.getX();
            double y = mc.player.getY() + AntiSwimUtils.STANDING_EYE_HEIGHT;
            double z = mc.player.getZ();
            cir.setReturnValue(new Vec3d(x, y, z));
        }
    }
}
