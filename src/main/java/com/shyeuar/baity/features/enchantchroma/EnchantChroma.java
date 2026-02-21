package com.shyeuar.baity.features.enchantchroma;

import com.shyeuar.baity.features.enchantchroma.data.EnchantLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EnchantChroma {

    public static void init() {
        EnchantLoader.init();
    }

    public static boolean isEnabled() {
        return EnchantChromaConfig.isEnabled();
    }
}
