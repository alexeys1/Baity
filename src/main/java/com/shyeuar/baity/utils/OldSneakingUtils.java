package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.mixin.accessor.PlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class OldSneakingUtils {

    public static final float LEGACY_EYE_HEIGHT_MULTIPLIER = 1.54F;
    private static final float EYE_HEIGHT_EPSILON = 0.001F;

    private OldSneakingUtils() {}

    public static boolean isEligiblePlayer(Player player) {
        return ConfigManager.oldSneakingEnabled
            && ((PlayerAccessor) player).baity$canChangeIntoPose(Pose.STANDING);
    }

    public static boolean isPhysicallyCrouching(Player player) {
        return player.getEyeHeight() < getStandingEyeHeight(player) - EYE_HEIGHT_EPSILON;
    }

    public static boolean isLandSneakContext(Player player) {
        return player.getPose() != Pose.SWIMMING && !player.isSwimming();
    }

    public static float getVisualEyeHeight(Player player) {
        if (NoSwimPoseUtils.shouldDeferCameraEyeHeightToVanilla(player)) {
            return player.getEyeHeight();
        }
        float currentEyeHeight = player.getEyeHeight();
        if (!isEligiblePlayer(player) || !isLandSneakContext(player) || !isPhysicallyCrouching(player)) {
            return currentEyeHeight;
        }

        float standingEyeHeight = getStandingEyeHeight(player);
        float vanillaSneakEyeHeight = player.getDimensions(Pose.CROUCHING).eyeHeight() * player.getScale();
        float legacyEyeHeight = LEGACY_EYE_HEIGHT_MULTIPLIER * player.getScale();

        float vanillaDrop = standingEyeHeight - vanillaSneakEyeHeight;
        if (vanillaDrop <= EYE_HEIGHT_EPSILON) {
            return legacyEyeHeight;
        }

        float progress = Mth.clamp((standingEyeHeight - currentEyeHeight) / vanillaDrop, 0.0F, 1.0F);
        return getLegacyStyleEyeHeight(player, standingEyeHeight, progress);
    }

    public static float getLegacyStyleEyeHeight(Player player, float standingEyeHeight, float sneakProgress) {
        float legacyEyeHeight = LEGACY_EYE_HEIGHT_MULTIPLIER * player.getScale();
        return Mth.lerp(Mth.clamp(sneakProgress, 0.0F, 1.0F), standingEyeHeight, legacyEyeHeight);
    }

    public static boolean shouldApplyEyeHeightChange() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        return player != null
            && !NoSwimPoseUtils.shouldDeferCameraEyeHeightToVanilla(player)
            && isEligiblePlayer(player)
            && isLandSneakContext(player)
            && isPhysicallyCrouching(player);
    }

    public static boolean shouldApplyCameraEffects() {
        return shouldApplyEyeHeightChange();
    }

    public static boolean shouldApplyInCurrentView() {
        return shouldApplyCameraEffects();
    }

    public static boolean shouldApplyLegacyPick(Player player) {
        return ConfigManager.oldSneakingEnabled
            && !NoSwimPoseUtils.shouldDeferCameraEyeHeightToVanilla(player)
            && isEligiblePlayer(player)
            && player.isCrouching();
    }

    public static float getLegacyPickEyeHeight(Player player) {
        if (shouldApplyLegacyPick(player)) {
            return LEGACY_EYE_HEIGHT_MULTIPLIER * player.getScale();
        }
        return player.getEyeHeight();
    }

    private static float getStandingEyeHeight(Player player) {
        return player.getDimensions(Pose.STANDING).eyeHeight() * player.getScale();
    }
}
