package com.shyeuar.baity.features.keybinds;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class Keybinds {
    private Keybinds() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, _, _) -> {
            if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || client.gameMode == null) {
                return;
            }

            KeybindsAutoClose.onScreenOpened(containerScreen);
            ScreenKeyboardEvents.allowKeyPress(containerScreen).register((_, keyInput) ->
                    handleKeyboard(client, containerScreen, keyInput)
            );
            ScreenMouseEvents.allowMouseClick(containerScreen).register((_, click) ->
                    handleMouseClick(client, containerScreen, click)
            );
        });
    }

    public static boolean isActive() {
        var module = ModuleManager.getModuleByName("Keybinds");
        return module != null && module.isEnabled() && ConfigManager.keybindsEnabled;
    }

    public static boolean shouldPreventUnequip(KeybindsMenuType menuType, ItemStack stack, long windowHandle) {
        if (!isActive() || stack == null || !ContainerGuiUtils.isEquippedSetButton(stack)) {
            return false;
        }
        boolean enabled;
        boolean prevent;
        String releaseMode;
        if (menuType == KeybindsMenuType.WARDROBE) {
            enabled = ConfigManager.keybindsWardrobeEnabled;
            prevent = ConfigManager.keybindsWardrobePreventUnequip;
            releaseMode = ConfigManager.keybindsWardrobeHoldToUnequip;
        } else if (menuType == KeybindsMenuType.EQUIPMENT) {
            enabled = ConfigManager.keybindsEquipmentEnabled;
            prevent = ConfigManager.keybindsEquipmentPreventUnequip;
            releaseMode = ConfigManager.keybindsEquipmentHoldToUnequip;
        } else {
            return false;
        }
        if (!enabled || !prevent) {
            return false;
        }
        return !HoldToUnequip.isHeld(windowHandle, releaseMode);
    }

    private static boolean handleKeyboard(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent keyInput) {
        if (!GuiPaginationHandler.handleKeyPress(screen, keyInput)) {
            return false;
        }
        if (!isActive() || !ContainerGuiUtils.tryConsumeClickCooldown()) {
            return true;
        }

        KeybindsMenuType menuType = KeybindsMenuType.fromTitle(screen.getTitle());
        return switch (menuType) {
            case WARDROBE -> handleSetMenu(client, screen, keyInput, menuType);
            case EQUIPMENT -> handleSetMenu(client, screen, keyInput, menuType);
            case LOADOUT -> handleLoadoutMenu(client, screen, keyInput);
            case NONE -> true;
        };
    }

    private static boolean handleMouseClick(Minecraft client, AbstractContainerScreen<?> screen, MouseButtonEvent click) {
        if (!isActive() || !ContainerGuiUtils.tryConsumeClickCooldown()) {
            return true;
        }

        KeybindsMenuType menuType = KeybindsMenuType.fromTitle(screen.getTitle());
        if (menuType == KeybindsMenuType.WARDROBE && ConfigManager.keybindsWardrobeEnabled) {
            return handleSetMenu(client, screen, binding -> binding.matchesMouse(click), menuType);
        }
        if (menuType == KeybindsMenuType.EQUIPMENT && ConfigManager.keybindsEquipmentEnabled) {
            return handleSetMenu(client, screen, binding -> binding.matchesMouse(click), menuType);
        }
        if (menuType == KeybindsMenuType.LOADOUT && ConfigManager.keybindsLoadoutEnabled) {
            return handleLoadoutMenu(client, screen, binding -> binding.matchesMouse(click));
        }
        return true;
    }

    private static boolean handleSetMenu(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent keyInput, KeybindsMenuType menuType) {
        if (!isSetMenuEnabled(menuType)) {
            return true;
        }
        return handleSetMenu(client, screen, binding -> binding.matches(keyInput), menuType);
    }

    private static boolean handleSetMenu(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            Predicate<KeyMapping> matcher,
            KeybindsMenuType menuType
    ) {
        int hotbarIndex = resolveHotbarIndex(client, matcher);
        if (hotbarIndex < 0) {
            return true;
        }

        int slotIndex = ContainerGuiUtils.wardrobeHotbarIndexToSlot(hotbarIndex);
        Slot slot = screen.getMenu().getSlot(slotIndex);
        ItemStack stack = slot.getItem();
        if (!ContainerGuiUtils.isWardrobeButton(stack)) {
            return true;
        }

        long windowHandle = client.getWindow().handle();
        if (shouldPreventUnequip(menuType, stack, windowHandle)) {
            return false;
        }

        ContainerGuiUtils.leftClickSlot(screen.getMenu().containerId, slotIndex);
        if (isAutoCloseEnabled(menuType)) {
            KeybindsAutoClose.schedule(menuType);
        }
        return false;
    }

    private static boolean handleLoadoutMenu(Minecraft client, AbstractContainerScreen<?> screen, KeyEvent keyInput) {
        if (!ConfigManager.keybindsLoadoutEnabled) {
            return true;
        }
        int loadoutIndex = resolveLoadoutIndexFromKey(client, keyInput);
        return executeLoadoutIndex(client, screen, loadoutIndex);
    }

    private static boolean handleLoadoutMenu(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            Predicate<KeyMapping> matcher
    ) {
        int loadoutIndex = resolveHotbarIndex(client, matcher);
        if (loadoutIndex < 0) {
            loadoutIndex = resolveExtraLoadoutIndexFromMouse(matcher);
        }
        return executeLoadoutIndex(client, screen, loadoutIndex);
    }

    private static boolean executeLoadoutIndex(Minecraft client, AbstractContainerScreen<?> screen, int loadoutIndex) {
        if (loadoutIndex < 0) {
            return true;
        }

        int slotIndex = ContainerGuiUtils.loadoutIndexToSlot(loadoutIndex);
        ItemStack stack = screen.getMenu().getSlot(slotIndex).getItem();
        if (!ContainerGuiUtils.isLoadoutEquipable(stack)) {
            return true;
        }

        ContainerGuiUtils.leftClickSlot(screen.getMenu().containerId, slotIndex);
        playLoadoutEquipSound(client);
        if (ConfigManager.keybindsLoadoutAutoCloseOnUse) {
            KeybindsAutoClose.schedule(KeybindsMenuType.LOADOUT);
        }
        return false;
    }

    private static int resolveHotbarIndex(Minecraft client, Predicate<KeyMapping> matcher) {
        KeyMapping[] slots = client.options.keyHotbarSlots;
        for (int i = 0; i < slots.length; i++) {
            if (matcher.test(slots[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int resolveLoadoutIndexFromKey(Minecraft client, KeyEvent keyInput) {
        int hotbarIndex = resolveHotbarIndex(client, binding -> binding.matches(keyInput));
        if (hotbarIndex >= 0) {
            return hotbarIndex;
        }
        int key = keyInput.input();
        int[] extraKeys = {
                ConfigManager.keybindsLoadoutSlot10Key,
                ConfigManager.keybindsLoadoutSlot11Key,
                ConfigManager.keybindsLoadoutSlot12Key
        };
        for (int i = 0; i < extraKeys.length; i++) {
            if (extraKeys[i] > 0 && extraKeys[i] == key) {
                return 9 + i;
            }
        }
        return -1;
    }

    private static int resolveExtraLoadoutIndexFromMouse(Predicate<KeyMapping> matcher) {
        int[] extraKeys = {
                ConfigManager.keybindsLoadoutSlot10Key,
                ConfigManager.keybindsLoadoutSlot11Key,
                ConfigManager.keybindsLoadoutSlot12Key
        };
        for (int i = 0; i < extraKeys.length; i++) {
            int keyCode = extraKeys[i];
            if (keyCode <= 0) {
                continue;
            }
            if (matcher.test(new KeyMapping("baity.loadout.extra", keyCode, KeyMapping.Category.MISC))) {
                return 9 + i;
            }
        }
        return -1;
    }

    private static boolean isSetMenuEnabled(KeybindsMenuType menuType) {
        return switch (menuType) {
            case WARDROBE -> ConfigManager.keybindsWardrobeEnabled;
            case EQUIPMENT -> ConfigManager.keybindsEquipmentEnabled;
            default -> false;
        };
    }

    private static boolean isAutoCloseEnabled(KeybindsMenuType menuType) {
        return switch (menuType) {
            case WARDROBE -> ConfigManager.keybindsWardrobeAutoCloseOnUse;
            case EQUIPMENT -> ConfigManager.keybindsEquipmentAutoCloseOnUse;
            default -> false;
        };
    }

    private static void playLoadoutEquipSound(Minecraft client) {
        if (client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 0.0f);
        }
    }
}
