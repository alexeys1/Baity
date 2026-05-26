package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shyeuar.baity.utils.ClientPickUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ClientPickMixin {

    @Mixin(Entity.class)
    public static class EntityPickEyeMixin {

        @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
        private void baity$useCrosshairEyePosition(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || (Entity) (Object) this != mc.player) {
                return;
            }
            if (ClientPickUtils.shouldOverrideFirstPersonPickEye()) {
                cir.setReturnValue(ClientPickUtils.getCrosshairEyePosition());
            }
        }
    }

    @Mixin(LocalPlayer.class)
    public static class LocalPlayerPickMixin {

        @WrapOperation(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/player/LocalPlayer;filterHitResult(Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/phys/Vec3;D)Lnet/minecraft/world/phys/HitResult;"
            )
        )
        private static HitResult baity$filterPickReachInPick(
            HitResult hitResult,
            Vec3 cameraPos,
            double interactionRange,
            Operation<HitResult> original,
            Entity camera,
            double blockInteractionRange,
            double entityInteractionRange,
            float tickDelta
        ) {
            return baity$filterPickReach(hitResult, cameraPos, interactionRange, original, camera, tickDelta);
        }

        @WrapOperation(
            method = "raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/player/LocalPlayer;filterHitResult(Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/phys/Vec3;D)Lnet/minecraft/world/phys/HitResult;"
            )
        )
        private static HitResult baity$filterPickReachInRaycast(
            HitResult hitResult,
            Vec3 cameraPos,
            double interactionRange,
            Operation<HitResult> original,
            float tickDelta,
            Entity camera
        ) {
            return baity$filterPickReach(hitResult, cameraPos, interactionRange, original, camera, tickDelta);
        }

        private static HitResult baity$filterPickReach(
            HitResult hitResult,
            Vec3 cameraPos,
            double interactionRange,
            Operation<HitResult> original,
            Entity camera,
            float tickDelta
        ) {
            if (!ClientPickUtils.shouldOverrideFirstPersonPickEye()) {
                return original.call(hitResult, cameraPos, interactionRange);
            }

            HitResult filtered = original.call(
                hitResult,
                ClientPickUtils.getReachClampPosition(camera, tickDelta),
                interactionRange
            );
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || camera != mc.player || !(camera instanceof Player player)) {
                return filtered;
            }
            if (!ClientPickUtils.shouldApplyPhysicalReachFilter(player)) {
                return filtered;
            }
            return ClientPickUtils.filterByPhysicalReach(filtered, camera, tickDelta, interactionRange);
        }
    }
}