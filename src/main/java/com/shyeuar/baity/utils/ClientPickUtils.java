package com.shyeuar.baity.utils;

import com.shyeuar.baity.mixin.accessor.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClientPickUtils {

    private static final double REACH_FILTER_MARGIN = 0.0625D;
    private static final double REACH_HYSTERESIS_EXTRA = 0.125D;

    private static BlockPos cachedReachBlockPos;

    private ClientPickUtils() {}

    public static boolean isFirstPerson() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.options.getCameraType().isFirstPerson();
    }

    public static boolean shouldUseNoSwimPickAdjustments() {
        return isFirstPerson() && NoSwimPoseUtils.shouldApplyEyeHeightChange();
    }

    public static boolean shouldOverrideFirstPersonPickEye() {
        if (!isFirstPerson()) {
            return false;
        }
        return shouldUseNoSwimPickAdjustments() || OldSneakingUtils.shouldApplyInCurrentView();
    }

    public static boolean needsPickEyeOverride(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity != mc.player) {
            return false;
        }
        return shouldOverrideFirstPersonPickEye();
    }

    public static float getVisualEyeHeight(Player player) {
        if (shouldUseNoSwimPickAdjustments()) {
            return NoSwimPoseUtils.getCameraEyeHeight(player);
        }
        if (OldSneakingUtils.shouldApplyInCurrentView()) {
            return OldSneakingUtils.getVisualEyeHeight(player);
        }
        return player.getEyeHeight();
    }

    public static float getCameraEyeHeightLerped(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.mainCamera();
        CameraAccessor accessor = (CameraAccessor) camera;
        return Mth.lerp(partialTick, accessor.baity$getOldEyeHeight(), accessor.baity$getEyeHeight());
    }

    public static float getSelfNametagHeightOffset(Player player, float partialTick) {
        float standingEye = player.getDimensions(Pose.STANDING).eyeHeight() * player.getScale();
        float cameraEye = getCameraEyeHeightLerped(partialTick);
        return player.getDimensions(Pose.STANDING).height() + 0.5F - (standingEye - cameraEye);
    }

    public static Vec3 getPickEyePosition(Entity entity, float tickDelta) {
        if (!shouldOverrideFirstPersonPickEye()) {
            return getPhysicalEyePosition(entity, tickDelta);
        }
        return getCrosshairEyePosition();
    }

    public static Vec3 getReachClampPosition(Entity entity, float tickDelta) {
        if (shouldOverrideFirstPersonPickEye()) {
            return getPhysicalEyePosition(entity, tickDelta);
        }
        return getPickEyePosition(entity, tickDelta);
    }

    public static Vec3 getCrosshairEyePosition() {
        return Minecraft.getInstance().gameRenderer.mainCamera().position();
    }

    public static Vec3 getPhysicalEyePosition(Entity entity, float tickDelta) {
        return new Vec3(
            entity.getX(tickDelta),
            entity.getY(tickDelta) + entity.getEyeHeight(),
            entity.getZ(tickDelta)
        );
    }

    public static HitResult filterByPhysicalReach(HitResult hit, Entity entity, float tickDelta, double interactionRange) {
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            cachedReachBlockPos = null;
            return hit;
        }

        Vec3 realEye = getPhysicalEyePosition(entity, tickDelta);
        double strictRange = Math.max(0.0D, interactionRange - REACH_FILTER_MARGIN);
        double strictRangeSq = strictRange * strictRange;
        double hysteresisRangeSq = (interactionRange + REACH_HYSTERESIS_EXTRA) * (interactionRange + REACH_HYSTERESIS_EXTRA);
        double distSq = realEye.distanceToSqr(hit.getLocation());

        if (distSq <= strictRangeSq) {
            if (hit instanceof BlockHitResult blockHit) {
                cachedReachBlockPos = blockHit.getBlockPos();
            } else {
                cachedReachBlockPos = null;
            }
            return hit;
        }

        if (hit instanceof BlockHitResult blockHit) {
            BlockPos blockPos = blockHit.getBlockPos();
            if (blockPos.equals(cachedReachBlockPos) && distSq <= hysteresisRangeSq) {
                return hit;
            }
        }

        cachedReachBlockPos = null;
        return BlockHitResult.miss(
            hit.getLocation(),
            hit instanceof BlockHitResult blockHit ? blockHit.getDirection() : Direction.UP,
            hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : BlockPos.containing(hit.getLocation())
        );
    }
}
