package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;

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
        if (mc.player == null || mc.level == null) return false;

        boolean isHeadInWater = isPlayerHeadInWaterBlock();
        long currentTime = System.currentTimeMillis();

        if (isHeadInWater) {
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

    public static boolean isPlayerHeadInWaterBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        
        AABB box = mc.player.getBoundingBox();
        double eyeY = box.maxY - 0.12D;
        BlockPos eyePos = BlockPos.containing(mc.player.getX(), eyeY, mc.player.getZ());
        
        if (mc.level.getBlockState(eyePos).getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        
        BlockPos eyePosAbove = BlockPos.containing(mc.player.getX(), eyeY + 0.1, mc.player.getZ());
        if (mc.level.getBlockState(eyePosAbove).getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        
        BlockPos eyePosBelow = BlockPos.containing(mc.player.getX(), eyeY - 0.1, mc.player.getZ());
        if (mc.level.getBlockState(eyePosBelow).getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        
        return false;
    }
    
    @Deprecated
    public static boolean isPlayerInWaterBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos feetPos = BlockPos.containing(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        
        if (mc.level.getBlockState(playerPos).getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        if (mc.level.getBlockState(feetPos).getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        
        BlockPos bodyPos = BlockPos.containing(mc.player.getX(), mc.player.getY() + 0.5, mc.player.getZ());
        if (mc.level.getBlockState(bodyPos).getFluidState().is(FluidTags.WATER)) {
            return true;
        }
        
        return false;
    }
}

