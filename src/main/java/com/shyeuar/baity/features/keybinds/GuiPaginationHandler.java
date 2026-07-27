package com.shyeuar.baity.features.keybinds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public final class GuiPaginationHandler {
    private enum NavButton {
        NEXT,
        PREVIOUS
    }

    private GuiPaginationHandler() {
    }

    public static boolean handleKeyPress(AbstractContainerScreen<?> screen, KeyEvent keyInput) {
        if (!Keybinds.isActive()) {
            return true;
        }
        KeybindsMenuType menuType = KeybindsMenuType.fromTitle(screen.getTitle());
        if (menuType == KeybindsMenuType.NONE) {
            return true;
        }

        Minecraft client = Minecraft.getInstance();
        NavButton nav = resolveNavButton(client, keyInput);
        if (nav == null) {
            return true;
        }
        if (!ContainerGuiUtils.tryConsumeClickCooldown()) {
            return false;
        }

        Slot target = findNavSlot(screen, nav);
        if (target == null) {
            return true;
        }

        ContainerGuiUtils.clickNavigationSlot(screen.getMenu().containerId, target.index, target.getItem());
        return false;
    }

    private static NavButton resolveNavButton(Minecraft client, KeyEvent keyInput) {
        if (client.options.keyLeft.matches(keyInput)) {
            return NavButton.PREVIOUS;
        }
        if (client.options.keyRight.matches(keyInput)) {
            return NavButton.NEXT;
        }
        return null;
    }

    private static Slot findNavSlot(AbstractContainerScreen<?> screen, NavButton nav) {
        for (Slot slot : screen.getMenu().slots) {
            NavButton type = getNavButtonType(slot.getItem());
            if (type == nav) {
                return slot;
            }
        }
        return null;
    }

    private static NavButton getNavButtonType(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (!item.equals(Items.ARROW) && !item.equals(Items.PLAYER_HEAD)) {
            return null;
        }
        String name = stack.getHoverName().getString();
        if (name.contains("Next Page") || name.contains("Scroll Right")) {
            return NavButton.NEXT;
        }
        if (name.contains("Previous Page") || name.contains("Scroll Left")) {
            return NavButton.PREVIOUS;
        }
        return null;
    }
}
