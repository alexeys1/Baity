package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.utils.NoSwimPoseUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class NoSwimPoseMixin {

    @Mixin(AvatarRenderer.class)
    public static class PlayerRendererTransformMixin {

        @Inject(method = "setupRotations", at = @At("HEAD"))
        private void baity$preventSwimTransform(AvatarRenderState state, PoseStack matrixStack, float f, float g, CallbackInfo ci) {
            if (NoSwimPoseUtils.shouldClearSelfSwimState(state)) {
                NoSwimPoseUtils.clearSwimRenderState(state);
            }
        }

        @Inject(method = "setupRotations", at = @At("TAIL"))
        private void baity$clearSwimAfterRotations(AvatarRenderState state, PoseStack matrixStack, float f, float g, CallbackInfo ci) {
            if (NoSwimPoseUtils.shouldClearSelfSwimState(state)) {
                NoSwimPoseUtils.clearSwimRenderState(state);
            }
        }
    }

    @Mixin(PlayerModel.class)
    public static class PlayerModelPoseMixin {

        @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("HEAD"))
        private void baity$modifySwimmingPose(AvatarRenderState state, CallbackInfo ci) {
            if (NoSwimPoseUtils.shouldClearSelfSwimState(state)) {
                NoSwimPoseUtils.clearSwimRenderState(state);
            }
        }

        @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
        private void baity$clearSwimAfterAnim(AvatarRenderState state, CallbackInfo ci) {
            if (NoSwimPoseUtils.shouldClearSelfSwimState(state)) {
                NoSwimPoseUtils.clearSwimRenderState(state);
            }
        }
    }

    @Mixin(Camera.class)
    public static class PlayerCameraMixin {

        @Shadow
        private float eyeHeightOld;

        @Shadow
        private float eyeHeight;

        @Shadow
        private Entity entity;

        @Unique
        private boolean baity$wasSwimEyeHeightOverride;

        @ModifyExpressionValue(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F", ordinal = 1)
        )
        private float baity$modifyEyeHeightLerp(float original, @Local(argsOnly = true) Entity focusedEntity) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || focusedEntity != mc.player) {
                return original;
            }
            if (mc.player != null && NoSwimPoseUtils.shouldSnapCameraEyeHeight(mc.player)) {
                return NoSwimPoseUtils.getCameraEyeHeight(mc.player);
            }
            return original;
        }

        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeHeight()F"))
        private float baity$standingEyeHeightDuringSwimVisual(Entity instance, Operation<Float> original) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && instance == mc.player && instance instanceof Player player
                && NoSwimPoseUtils.shouldApplyCameraEyeHeightChange()) {
                return NoSwimPoseUtils.getCameraEyeHeight(player);
            }
            return original.call(instance);
        }

        @Inject(method = "tick", at = @At("HEAD"))
        private void baity$syncEyeHeightOnSwimVisualRelease(CallbackInfo ci) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || this.entity != mc.player) {
                return;
            }

            boolean swimOverride = NoSwimPoseUtils.shouldApplyCameraEyeHeightChange();
            if (this.baity$wasSwimEyeHeightOverride && !swimOverride) {
                float realEyeHeight = mc.player.getEyeHeight();
                this.eyeHeightOld = realEyeHeight;
                this.eyeHeight = realEyeHeight;
            }
            this.baity$wasSwimEyeHeightOverride = swimOverride;
        }

        @WrapOperation(
            method = "tick",
            at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/Camera;eyeHeight:F")
        )
        private void baity$snapEyeHeightDuringSwimVisual(Camera instance, float value, Operation<Void> original) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && this.entity == mc.player && NoSwimPoseUtils.shouldSnapCameraEyeHeight((Player) mc.player)) {
                float visualEye = NoSwimPoseUtils.getCameraEyeHeight((Player) mc.player);
                this.eyeHeight = visualEye;
                this.eyeHeightOld = visualEye;
                return;
            }
            original.call(instance, value);
        }
    }

    @Mixin(LivingEntity.class)
    public static class EntitySwimAmountRenderMixin {

        @ModifyReturnValue(method = "getSwimAmount", at = @At("RETURN"))
        private float baity$freezeSwimAmountDuringRender(float original) {
            if (!NoSwimPoseUtils.shouldFreezeSwimAmount((Entity) (Object) this)) {
                return original;
            }
            return 0.0F;
        }
    }

    @Mixin(AvatarRenderer.class)
    public static class WorldSwimNametagMixin {

        @Inject(
            method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD")
        )
        private void baity$liftWorldSwimNametag(
            AvatarRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState,
            CallbackInfo ci
        ) {
            if (!RenderScope.isWorldEntityRender(cameraState)
                || !NoSwimPoseUtils.isSelfPlayerById(state.id)) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !NoSwimPoseUtils.shouldApplyWorldSwimNametagOffset(mc.player)) {
                return;
            }

            matrices.translate(0.0F, NoSwimPoseUtils.WORLD_SWIM_NAMETAG_Y_OFFSET, 0.0F);
        }
    }
}
