package com.shyeuar.baity.features.sidepanel;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;

@Environment(EnvType.CLIENT)
final class SidePanelUtils {
    private static final String[] EQUIPMENT_SLOT_SUFFIXES = {
            " Necklace",
            " Cloak",
            " Belt",
            " Gloves",
            " Bracelet"
    };

    private SidePanelUtils() {
    }

    static String stripFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("§.", "");
    }

    static String normalizeDisplayName(String text) {
        return stripFormatting(text).trim();
    }

    static String stripEquipmentSlotSuffix(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return "";
        }
        String trimmed = displayName.trim();
        for (String suffix : EQUIPMENT_SLOT_SUFFIXES) {
            if (trimmed.endsWith(suffix)) {
                return trimmed.substring(0, trimmed.length() - suffix.length()).trim();
            }
        }
        return trimmed;
    }

    static boolean isPlaceholderPane(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        if (isGlassPane(stack)) {
            return true;
        }
        if (stack.is(Items.GRAY_STAINED_GLASS_PANE)
                || stack.is(Items.LIGHT_GRAY_STAINED_GLASS_PANE)
                || stack.is(Items.BLACK_STAINED_GLASS_PANE)) {
            return true;
        }
        String name = stack.getHoverName().getString().toLowerCase();
        return name.startsWith("empty") || name.startsWith("slot ");
    }

    private static boolean isGlassPane(ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        return block == Blocks.GLASS_PANE || block instanceof StainedGlassPaneBlock;
    }
}
