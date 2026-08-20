package com.shyeuar.baity.features;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
public final class AutoSprint {

    private AutoSprint() {
    }

    public static void handleSprintKeyIsDown(KeyMapping keyMapping, CallbackInfoReturnable<Boolean> cir) {
        if (!ConfigManager.autoSprintEnabled) {
            return;
        }
        if (keyMapping != Minecraft.getInstance().options.keySprint) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (client.player.isSprinting()) {
            return;
        }
        if (!ConfigManager.autoSprintUnderWater && client.player.isInWater()) {
            return;
        }
        cir.setReturnValue(true);
    }
}
