package com.shyeuar.baity.features;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class AutoSprint {

    private AutoSprint() {
    }

    public static boolean shouldAutoSprint(boolean forward) {
        if (!ConfigManager.autoSprintEnabled) {
            return false;
        }
        if (!forward) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        if (client.player.isSprinting()) {
            return false;
        }
        return ConfigManager.autoSprintUnderWater || !client.player.isInWater();
    }
}
