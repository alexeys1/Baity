package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.render.interfaces.EntityRenderStateInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public final class NoSwimPoseUtils {

    public static final float STANDING_EYE_HEIGHT = 1.62F;
    public static final float WORLD_SWIM_NAMETAG_Y_OFFSET = 1.2F;

    private static final long EXIT_GRACE_MS = 1000L;

    private static boolean wasInWater = false;
    private static long exitWaterTime = 0L;

    private NoSwimPoseUtils() {}

    public static boolean isFeatureActive() {
        Module m = ModuleManager.getModuleByName("NoSwimPose");
        return m != null && m.isEnabled();
    }

    private enum SwimVisualPhase {
        NONE,
        ACTIVE,
        GRACE
    }

    private static boolean isInSwimAction(Player player) {
        return player.getPose() == Pose.SWIMMING || player.isSwimming();
    }

    private static SwimVisualPhase resolveSwimVisualPhase() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return SwimVisualPhase.NONE;
        }

        Player player = mc.player;
        long now = System.currentTimeMillis();

        if (isInSwimAction(player)) {
            wasInWater = true;
            exitWaterTime = 0L;
            return SwimVisualPhase.ACTIVE;
        }

        if (!wasInWater) {
            return SwimVisualPhase.NONE;
        }

        if (exitWaterTime == 0L) {
            exitWaterTime = now;
        }
        if (now - exitWaterTime < EXIT_GRACE_MS) {
            return SwimVisualPhase.GRACE;
        }

        wasInWater = false;
        exitWaterTime = 0L;
        return SwimVisualPhase.NONE;
    }

    public static boolean isInWaterSwimVisualContext() {
        SwimVisualPhase phase = resolveSwimVisualPhase();
        return phase == SwimVisualPhase.ACTIVE || phase == SwimVisualPhase.GRACE;
    }

    public static boolean isInActiveSwimVisualContext() {
        return resolveSwimVisualPhase() == SwimVisualPhase.ACTIVE;
    }

    public static boolean shouldApplyEyeHeightChange() {
        return isFeatureActive() && isInActiveSwimVisualContext();
    }

    public static boolean shouldApplyCameraEyeHeightChange() {
        if (!shouldApplyEyeHeightChange()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return false;
        }
        if (mc.options.getCameraType().isFirstPerson()) {
            return true;
        }
        return isSwimmingPose(mc.player);
    }

    public static boolean shouldSnapCameraEyeHeight(Player player) {
        return shouldApplyCameraEyeHeightChange() && isSwimmingPose(player);
    }

    public static boolean isSwimmingPose(Player player) {
        return player.getPose() == Pose.SWIMMING;
    }

    public static float getCameraEyeHeight(Player player) {
        if (!shouldApplyEyeHeightChange() || !shouldApplyCameraEyeHeightChange()) {
            return player.getEyeHeight();
        }
        if (isSwimmingPose(player)) {
            return STANDING_EYE_HEIGHT;
        }
        return getWadingEyeHeight(player);
    }

    private static float getWadingEyeHeight(Player player) {
        float standing = STANDING_EYE_HEIGHT;
        if (!isSneaking() && !player.isCrouching()) {
            return standing;
        }
        float crouch = player.getDimensions(Pose.CROUCHING).eyeHeight() * player.getScale();
        float vanillaStand = player.getDimensions(Pose.STANDING).eyeHeight() * player.getScale();
        float span = vanillaStand - crouch;
        if (span > 0.001F) {
            float progress = Mth.clamp((vanillaStand - player.getEyeHeight()) / span, 0.0F, 1.0F);
            return Mth.lerp(progress, standing, crouch);
        }
        return crouch;
    }

    public static boolean shouldApplyVisualOverrides() {
        return isFeatureActive() && isInWaterSwimVisualContext();
    }

    public static boolean shouldApplyWorldSwimNametagOffset(Player player) {
        return shouldApplyVisualOverrides() && player != null && player.getPose() == Pose.SWIMMING;
    }

    public static boolean isSelfPlayerById(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == entityId;
    }

    public static boolean isSelfPlayer(Object entity) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && entity == mc.player;
    }

    public static boolean isSneaking() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.options.keyShift.isDown();
    }

    public static boolean isWorldRenderContext(AvatarRenderState state) {
        if (state instanceof EntityRenderStateInterface context) {
            return context.baity$isWorldCameraContext();
        }
        return false;
    }

    public static boolean shouldClearSelfSwimState(AvatarRenderState state) {
        return isSelfPlayerById(state.id) && isWorldRenderContext(state);
    }

    public static boolean isAbnormalDrySwimPose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return mc.player.getPose() == Pose.SWIMMING && !isInWaterSwimVisualContext();
    }

    public static boolean shouldFreezeSwimAmount(Object entity) {
        if (!shouldApplyVisualOverrides() || !isSelfPlayer(entity)) {
            return false;
        }
        return RenderScope.isEntityRenderScope() && RenderScope.shouldApplyWorldEntityChanges();
    }

    public static void clearSwimRenderState(AvatarRenderState state) {
        if (!shouldApplyVisualOverrides()) {
            return;
        }
        state.isVisuallySwimming = false;
        state.swimAmount = 0.0F;

        Minecraft mc = Minecraft.getInstance();
        if (isSneaking() && mc.player != null && !isSwimmingPose(mc.player)) {
            state.isCrouching = true;
        }
    }

    public static void restoreSwimRenderStateFromEntity(AvatarRenderState state, LivingEntity entity) {
        if (state == null || entity == null) {
            return;
        }
        state.swimAmount = entity.getSwimAmount(1.0F);
        state.isVisuallySwimming = entity.isVisuallySwimming();
    }
}
