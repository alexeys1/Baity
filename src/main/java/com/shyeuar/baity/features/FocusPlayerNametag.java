package com.shyeuar.baity.features;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class FocusPlayerNametag {

    public static final String MODE_TOGGLE = "Toggle";
    public static final String MODE_HOLD = "Hold";

    private static boolean wasKeyPressed = false;

    private FocusPlayerNametag() {
    }

    public static boolean isHoldMode() {
        return MODE_HOLD.equalsIgnoreCase(ConfigManager.nametagFocusPlayerMode);
    }

    public static boolean isActive() {
        Module nametag = ModuleManager.getModuleByName("Nametag");
        if (nametag == null || !nametag.isEnabled()) {
            return false;
        }

        if (isHoldMode()) {
            int keybind = ConfigManager.nametagFocusPlayerKeybind;
            if (keybind == 0) {
                return false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null || mc.gui.screen() != null) {
                return false;
            }
            return KeyMappingUtils.isKeyPressed(mc.getWindow().handle(), keybind);
        }

        return ConfigManager.nametagFocusPlayerNametag;
    }

    public static boolean shouldUseCustomNametagPath() {
        Module nametag = ModuleManager.getModuleByName("Nametag");
        if (nametag == null || !nametag.isEnabled()) {
            return false;
        }
        if (isActive()) {
            return true;
        }
        return !ConfigManager.nametagDefaultNametag;
    }

    public static void tick(Minecraft client) {
        Module nametag = ModuleManager.getModuleByName("Nametag");
        if (nametag == null || !nametag.isEnabled()) {
            wasKeyPressed = false;
            return;
        }

        int keybind = ConfigManager.nametagFocusPlayerKeybind;
        if (keybind == 0 || client.gui.screen() != null) {
            wasKeyPressed = false;
            return;
        }

        long windowHandle = client.getWindow().handle();
        boolean pressed = KeyMappingUtils.isKeyPressed(windowHandle, keybind);

        if (isHoldMode()) {
            wasKeyPressed = pressed;
            return;
        }

        if (pressed && !wasKeyPressed) {
            ConfigManager.nametagFocusPlayerNametag = !ConfigManager.nametagFocusPlayerNametag;
            ConfigManager.requestSave();
        }
        wasKeyPressed = pressed;
    }
}
