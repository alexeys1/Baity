package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.NoSwimPoseUtils;
import com.shyeuar.baity.utils.OldSneakingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class OldSneakingMixin {

    @Mixin(Camera.class)
    public static abstract class OldSneakingCameraMixin {

        @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeHeight()F"))
        private float baity$overrideEyeHeightForSneaking(Entity instance, Operation<Float> original) {
            if (ConfigManager.oldSneakingEnabled && instance instanceof Player player) {
                if (NoSwimPoseUtils.shouldApplyCameraEyeHeightChange()) {
                    return NoSwimPoseUtils.getCameraEyeHeight(player);
                }
                if (!OldSneakingUtils.shouldApplyCameraEffects()) {
                    return original.call(instance);
                }
                return OldSneakingUtils.getVisualEyeHeight(player);
            }
            return original.call(instance);
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
