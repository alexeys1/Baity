package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;

public final class NoSwimChangeUtils {

    public static final float STANDING_EYE_HEIGHT = 1.62F;

    private NoSwimChangeUtils() {}

    public static boolean isFeatureActive() {
        Module m = ModuleManager.getModuleByName("NoSwimChange");
        return m != null && m.isEnabled();
    }

    public static boolean isDisablePoseActive() {
        if (!isFeatureActive()) return false;
        return ConfigManager.noSwimChangeDisablePose;
    }

    public static boolean isDisableEyeHeightActive() {
        if (!isFeatureActive()) return false;
        return ConfigManager.noSwimChangeDisableEyeHeight;
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

