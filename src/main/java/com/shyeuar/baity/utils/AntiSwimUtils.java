package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;

public final class AntiSwimUtils {

    public static final float STANDING_EYE_HEIGHT = 1.62F;

    private AntiSwimUtils() {}

    public static boolean isFeatureActive() {
        Module m = ModuleManager.getModuleByName("AntiSwim");
        return m != null && m.isEnabled();
    }

    public static boolean isDisablePoseActive() {
        if (!isFeatureActive()) return false;
        return ConfigManager.antiSwimDisablePose;
    }

    public static boolean isDisableEyeHeightActive() {
        if (!isFeatureActive()) return false;
        return ConfigManager.antiSwimDisableEyeHeight;
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
}
