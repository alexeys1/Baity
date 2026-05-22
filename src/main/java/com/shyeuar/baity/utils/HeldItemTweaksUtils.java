package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class HeldItemTweaksUtils {

    public static final String MODULE_NAME = "HeldItemTweaks";
    public static final String NO_ITEMSWAP_ANIMATION = "no itemswap animation";
    public static final String NO_ARM_SWAY = "no arm sway";

    private HeldItemTweaksUtils() {
    }

    public static boolean isNoItemswapAnimationActive() {
        Module module = ModuleManager.getModuleByName(MODULE_NAME);
        return module != null
                && module.isEnabled()
                && ModuleUtils.getOptionBoolean(module, NO_ITEMSWAP_ANIMATION, false);
    }

    public static boolean isNoArmSwayActive() {
        Module module = ModuleManager.getModuleByName(MODULE_NAME);
        return module != null
                && module.isEnabled()
                && ModuleUtils.getOptionBoolean(module, NO_ARM_SWAY, false);
    }
}