package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public final class BlockAnimationUtils {
    private BlockAnimationUtils() {}
    
    public static final int DEFAULT_ITEM_USE_DURATION = 72_000;

    public static boolean isFeatureActive() {
        com.shyeuar.baity.gui.module.Module blockAnimationModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("BlockAnimation");
        return blockAnimationModule != null && blockAnimationModule.isEnabled();
    }

    public static boolean isInteractAnimationsEnabled() {
        return com.shyeuar.baity.config.ConfigManager.blockAnimationInteractAnimations;
    }

    public static boolean isNoReequipWhenUsingEnabled() {
        return com.shyeuar.baity.config.ConfigManager.blockAnimationNoReequipWhenUsing;
    }

    public static boolean isSlowdownEnabled() {
        return com.shyeuar.baity.config.ConfigManager.blockAnimationSlowdown;
    }

    public static boolean isPlayerBlockingWithSword(Player player) {
        if (!isFeatureActive()) return false;
        if (player == null) return false;
        return isPlayerRightClicking() && canSwordBlock(player);
    }
 
    public static boolean isPlayerRightClicking() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return false;
        return client.options.keyUse.isDown();
    }
  
    public static boolean canSwordBlock(Player player) {
        if (!isFeatureActive()) return false;
        if (player == null) return false;
        Item mainHandItem = player.getMainHandItem().getItem();
        Item offHandItem = player.getOffhandItem().getItem();
        return isSword(mainHandItem) || isSword(offHandItem);
    }
    
    public static InteractionHand getBlockingHand(Player player) {
        if (!canSwordBlock(player)) return null;
        return isSword(player.getMainHandItem().getItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static boolean isSword(Item item) {
        return item == Items.WOODEN_SWORD ||
               item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD ||
               item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD ||
               item == Items.NETHERITE_SWORD ||
               item == Items.COPPER_SWORD;
    }

    public static boolean canActivateBlocking(Player player, ItemStack offHand) {
        if (offHand.isEmpty()) return true;
        
        switch (offHand.getUseAnimation()) {
            case BLOCK, SPYGLASS, BRUSH -> {
                return false;
            }
            default -> {
                return true;
            }
        }
    }
}

