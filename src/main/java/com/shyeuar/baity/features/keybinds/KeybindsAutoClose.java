package com.shyeuar.baity.features.keybinds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Environment(EnvType.CLIENT)
public final class KeybindsAutoClose {
    private static final long TIMEOUT_MS = 3000L;

    private static KeybindsMenuType pendingMenu = KeybindsMenuType.NONE;
    private static long pendingExpiresAt;

    private KeybindsAutoClose() {
    }

    public static void schedule(KeybindsMenuType menuType) {
        pendingMenu = menuType;
        pendingExpiresAt = System.currentTimeMillis() + TIMEOUT_MS;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.closeContainer();
        }
    }

    public static void onScreenOpened(AbstractContainerScreen<?> screen) {
        if (pendingMenu == KeybindsMenuType.NONE) {
            return;
        }
        if (System.currentTimeMillis() > pendingExpiresAt) {
            clear();
            return;
        }
        KeybindsMenuType opened = KeybindsMenuType.fromTitle(screen.getTitle());
        if (opened != pendingMenu) {
            return;
        }
        clear();
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.closeContainer();
        }
    }

    public static void clear() {
        pendingMenu = KeybindsMenuType.NONE;
        pendingExpiresAt = 0L;
    }
}
