package com.shyeuar.baity.utils;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public final class NametagUtils {

    private NametagUtils() {
    }

    public static boolean isNametagModuleActive() {
        return ConfigManager.nametagEnabled;
    }

    public static boolean isDefaultNametagMode() {
        return ConfigManager.nametagEnabled && ConfigManager.nametagDefaultNametag;
    }

    public static boolean isOwnNametagDisplayEnabled() {
        return ConfigManager.nametagEnabled
            && ConfigManager.nametagDefaultNametag
            && ConfigManager.nametagShowOwnNametag;
    }

    public static boolean isDefaultNametagEnabled() {
        Module nametag = ModuleManager.getModuleByName("Nametag");
        return nametag != null && nametag.isEnabled()
            && ModuleUtils.getOptionBoolean(nametag, "default nametag", false);
    }

    public static boolean isHudEntityRender(CameraRenderState cameraState) {
        return !RenderScope.isWorldEntityRender(cameraState);
    }

    public static void ensureOwnNameTag(EntityRenderState state) {
        if (state == null || state.nameTag != null || !isOwnNametagDisplayEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        state.nameTag = mc.player.getDisplayName();
    }

    public static boolean shouldProcessNametagText() {
        return isDefaultNametagEnabled();
    }

    public static boolean shouldApplyNametagLayoutCompat() {
        return isDefaultNametagEnabled() && RenderScope.isWorldNameTagAdd();
    }

    public static Component applyNickProcessedText(Component original) {
        FormattedText processed = NickRenderUtils.handleFormattedText(original);
        if (processed instanceof Component processedComponent) {
            return processedComponent;
        }
        return original;
    }
}