package com.shyeuar.baity.features.keybinds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ContainerGuiUtils {
    private static final int CLICK_COOLDOWN_MS = 300;

    private static long lastClickAt;

    private ContainerGuiUtils() {
    }

    public static boolean tryConsumeClickCooldown() {
        long now = System.currentTimeMillis();
        if (now - lastClickAt < CLICK_COOLDOWN_MS) {
            return false;
        }
        lastClickAt = now;
        return true;
    }

    public static void clickSlot(int containerId, int slotIndex, int mouseButton, ContainerInput input) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode == null || client.player == null) {
            return;
        }
        client.gameMode.handleContainerInput(containerId, slotIndex, mouseButton, input, client.player);
    }

    public static void leftClickSlot(int containerId, int slotIndex) {
        clickSlot(containerId, slotIndex, GLFW.GLFW_MOUSE_BUTTON_LEFT, ContainerInput.PICKUP);
    }

    public static void clickNavigationSlot(int containerId, int slotIndex, ItemStack stack) {
        boolean extraLines = getLoreLineCount(stack) > 1;
        if (extraLines) {
            clickSlot(containerId, slotIndex, GLFW.GLFW_MOUSE_BUTTON_LEFT, ContainerInput.PICKUP);
        } else {
            clickSlot(containerId, slotIndex, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, ContainerInput.CLONE);
        }
    }

    public static int getLoreLineCount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? 0 : lore.lines().size();
    }

    public static boolean hasLoreLine(ItemStack stack, String needle) {
        if (stack == null || stack.isEmpty() || needle == null) {
            return false;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return false;
        }
        for (Component line : lore.lines()) {
            if (line.getString().contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEquippedSetButton(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.LIME_DYE);
    }

    public static boolean isWardrobeButton(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.is(Items.LIME_DYE) || stack.is(Items.GRAY_DYE) || stack.is(Items.PINK_DYE);
    }

    public static boolean isLoadoutEquipable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.GRAY_DYE) || stack.is(Items.RED_DYE) || stack.is(Items.BLACK_STAINED_GLASS_PANE)) {
            return false;
        }
        return hasLoreLine(stack, "Left-click to equip!");
    }

    public static int wardrobeHotbarIndexToSlot(int hotbarIndex) {
        if (hotbarIndex < 0 || hotbarIndex > 8) {
            return -1;
        }
        return 36 + hotbarIndex;
    }

    public static int loadoutIndexToSlot(int loadoutIndex) {
        return switch (loadoutIndex) {
            case 0 -> 14;
            case 1 -> 15;
            case 2 -> 16;
            case 3 -> 23;
            case 4 -> 24;
            case 5 -> 25;
            case 6 -> 32;
            case 7 -> 33;
            case 8 -> 34;
            case 9 -> 41;
            case 10 -> 42;
            case 11 -> 43;
            default -> -1;
        };
    }
}
