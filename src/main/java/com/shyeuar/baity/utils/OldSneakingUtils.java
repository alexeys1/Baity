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
        return Mth.lerp(progress, standingEyeHeight, legacyEyeHeight);
    }

    public static boolean shouldApplyEyeHeightChange() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        return player != null
            && isEligiblePlayer(player)
            && isLandSneakContext(player)
            && isPhysicallyCrouching(player);
    }

    public static boolean appliesInCurrentView() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.options.getCameraType().isFirstPerson();
    }

    public static boolean shouldApplyInCurrentView() {
        return appliesInCurrentView() && shouldApplyEyeHeightChange();
    }

    public static boolean shouldApplyLegacyPick(Player player) {
        return ConfigManager.oldSneakingEnabled
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
