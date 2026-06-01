package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.mixin.accessor.AvatarRenderStateAccessor;
import com.shyeuar.baity.mixin.accessor.CameraRenderStateAccessor;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.utils.NoSwimPoseUtils;
import com.shyeuar.baity.utils.OldSneakingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class OldSneakingMixin {

    private static final float BODY_ROTATION_X = 0.5F;
    private static final float ARM_ROTATION_OFFSET = 0.4F;
    private static final float LEG_POSITION_Z = 4.0F;
    private static final float LEG_POSITION_Y = 9.0F;
    private static final float HEAD_POSITION_Y = 1.0F;

    @Mixin(Camera.class)
    public static abstract class OldSneakingCameraMixin {

        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeHeight()F"))
        private float baity$overrideEyeHeightForSneaking(Entity instance, Operation<Float> original) {
            if (ConfigManager.oldSneakingEnabled && instance instanceof Player player) {
                if (NoSwimPoseUtils.shouldApplyCameraEyeHeightChange()) {
                    return NoSwimPoseUtils.getCameraEyeHeight(player);
                }
                if (!OldSneakingUtils.appliesInCurrentView()) {
                    return original.call(instance);
                }
                return OldSneakingUtils.getVisualEyeHeight(player);
            }
            return original.call(instance);
        }
    }

    @Mixin(HumanoidModel.class)
    public static abstract class OldSneakingHumanoidModelMixin<T extends HumanoidRenderState> {
        @Shadow
        public ModelPart rightArm;

        @Shadow
        public ModelPart leftArm;

        @Shadow
        public ModelPart head;

        @Shadow
        public ModelPart body;

        @Shadow
        public ModelPart rightLeg;

        @Shadow
        public ModelPart leftLeg;

        @WrapOperation(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;isCrouching:Z")
        )
        private boolean baity$applyLegacySneakPose(HumanoidRenderState instance, Operation<Boolean> original) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (ConfigManager.oldSneakingEnabled
                && OldSneakingUtils.appliesInCurrentView()
                && player != null
                && OldSneakingUtils.isLandSneakContext(player)
                && instance.isCrouching
                && RenderScope.shouldApplyWorldEntityChanges()) {
                body.xRot = BODY_ROTATION_X;
                rightArm.xRot += ARM_ROTATION_OFFSET;
                leftArm.xRot += ARM_ROTATION_OFFSET;
                rightLeg.z = LEG_POSITION_Z;
                leftLeg.z = LEG_POSITION_Z;
                rightLeg.y = LEG_POSITION_Y;
                leftLeg.y = LEG_POSITION_Y;
                head.y = HEAD_POSITION_Y;
                return false;
            }
            return original.call(instance);
        }
    }

    @Mixin(LivingEntityRenderer.class)
    public static abstract class OldSneakingLivingEntityRendererMixin<S extends LivingEntityRenderState> {

        @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 1)
        )
        private void baity$adjustModelPositionForEyeHeight(
            S livingEntityRenderState,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
        ) {
            if (!ConfigManager.oldSneakingEnabled
                || !OldSneakingUtils.appliesInCurrentView()
                || !baity$isPlayerSelf(livingEntityRenderState)
                || livingEntityRenderState.hasPose(Pose.SWIMMING)
                || !RenderScope.isWorldEntityRender(cameraRenderState)) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || !OldSneakingUtils.isLandSneakContext(player)) {
                return;
            }

            AvatarRenderStateAccessor avatarAccessor = (AvatarRenderStateAccessor) livingEntityRenderState;
            EntityDimensions standingDimensions = avatarAccessor.baity$getStandingDimensions();
            if (standingDimensions == null) {
                return;
            }

            CameraRenderStateAccessor cameraAccessor = (CameraRenderStateAccessor) cameraRenderState;
            float cameraLerpValue = Mth.lerp(
                cameraAccessor.baity$getPartialTickTime(),
                cameraAccessor.baity$getOldEyeHeight(),
                cameraAccessor.baity$getEyeHeight()
            );
            poseStack.translate(
                0.0F,
                standingDimensions.eyeHeight() * livingEntityRenderState.scale - cameraLerpValue,
                0.0F
            );
        }

        @Unique
        private static boolean baity$isPlayerSelf(LivingEntityRenderState state) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            return player != null && state instanceof AvatarRenderState avatarRenderState && avatarRenderState.id == player.getId();
        }
    }

    @Mixin(Entity.class)
    public static abstract class OldSneakingEntityEyeHeightMixin {

        @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F", at = @At("HEAD"), cancellable = true)
        private void baity$modifyEyeHeightForRaycast(Pose pose, CallbackInfoReturnable<Float> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
                return;
            }
            if (!ConfigManager.oldSneakingEnabled) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                return;
            }

            Entity self = (Entity) (Object) this;
            if (self != mc.player || !(self instanceof Player player)) {
                return;
            }

            if (OldSneakingUtils.shouldApplyLegacyPick(player)) {
                cir.setReturnValue(OldSneakingUtils.getLegacyPickEyeHeight(player));
            }
        }
    }
}
