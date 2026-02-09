package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.material.Fluids;

public final class NoSwimChangeUtils {

    public static final float STANDING_EYE_HEIGHT = 1.62F;
    private static final long DELAY_MS = 500L;
    private static long exitWaterTime = 0L;
    private static boolean wasInWater = false;

    private NoSwimChangeUtils() {}

    public static boolean isFeatureActive() {
        Module m = ModuleManager.getModuleByName("NoSwimChange");
        return m != null && m.isEnabled();
    }

    public static boolean shouldApplyEyeHeightChange() {
        if (!isFeatureActive()) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        boolean isInWater = mc.player.isUnderWater() || isPlayerInWaterBlock() || mc.player.getPose() == Pose.SWIMMING;
        long currentTime = System.currentTimeMillis();

        if (isInWater) {
            wasInWater = true;
            exitWaterTime = 0L;
            return true;
        }

        if (wasInWater) {
            if (exitWaterTime == 0L) {
                exitWaterTime = currentTime;
            }
            long timeSinceExit = currentTime - exitWaterTime;
            if (timeSinceExit < DELAY_MS) {
                return true;
            } else {
                wasInWater = false;
                exitWaterTime = 0L;
                return false;
            }
        }

        return false;
    }

    public static boolean isSelfPlayer(Object entity) {
        Minecraft mc = Minecraft.getInstance();
        return entity == mc.player;
    }

    public static boolean isSelfPlayerById(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getId() == entityId;
    }

    public static boolean isSneaking() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.options.keyShift.isDown();
    }

    public static boolean isPlayerInWaterBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos feetPos = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        
        if (mc.level.getBlockState(playerPos).getFluidState().is(Fluids.WATER)) {
            return true;
        }
        if (mc.level.getBlockState(feetPos).getFluidState().is(Fluids.WATER)) {
            return true;
        }
        
        BlockPos bodyPos = BlockPos.containing(mc.player.getX(), mc.player.getY() + 0.5, mc.player.getZ());
        if (mc.level.getBlockState(bodyPos).getFluidState().is(Fluids.WATER)) {
            return true;
        }
        
        return false;
    }
}

