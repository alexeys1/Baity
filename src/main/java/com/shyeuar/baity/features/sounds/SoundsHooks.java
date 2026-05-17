package com.shyeuar.baity.features.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@Environment(EnvType.CLIENT)
public final class SoundsHooks {
    private static final String ATOMSPLIT_ID = "ATOMSPLIT_KATANA";
    private static final String ITEM_MODEL_DIAMOND_SWORD = "minecraft:diamond_sword";
    private static final String ITEM_MODEL_GOLDEN_SWORD = "minecraft:golden_sword";
    private static final long SOULCRY_DURATION_MS = 4000L;

    private static boolean lastMainHandIsAtomsplit = false;
    private static boolean lastMainHandModelDiamond = false;
    private static boolean lastMainHandModelGold = false;
    private static long scheduledSoulcryEndAt = 0L;

    private SoundsHooks() {}

    public static void tick(Minecraft client) {
        if (client == null || client.player == null) return;
        if (!SoulcrySoundManager.isEnabled()) {
            resetState();
            return;
        }

        long now = System.currentTimeMillis();
        processMainHandState(client, now);

        if (scheduledSoulcryEndAt > 0L && now >= scheduledSoulcryEndAt) {
            SoulcrySoundManager.playClose(client);
            scheduledSoulcryEndAt = 0L;
        }
    }

    private static void processMainHandState(Minecraft client, long now) {
        ItemStack stack = client.player.getMainHandItem();
        if (stack == null || stack.isEmpty()) {
            lastMainHandIsAtomsplit = false;
            lastMainHandModelDiamond = false;
            lastMainHandModelGold = false;
            return;
        }

        if (!ATOMSPLIT_ID.equalsIgnoreCase(readSkyblockId(stack))) {
            lastMainHandIsAtomsplit = false;
            lastMainHandModelDiamond = false;
            lastMainHandModelGold = false;
            return;
        }

        String itemModel = readItemModel(stack);
        boolean isDiamond = ITEM_MODEL_DIAMOND_SWORD.equals(itemModel);
        boolean isGold = ITEM_MODEL_GOLDEN_SWORD.equals(itemModel);
        if (!isDiamond && !isGold) {
            lastMainHandIsAtomsplit = true;
            lastMainHandModelDiamond = false;
            lastMainHandModelGold = false;
            return;
        }

        if (isGold && lastMainHandIsAtomsplit && !lastMainHandModelGold && lastMainHandModelDiamond) {
            SoulcrySoundManager.playOpen(client);
            scheduledSoulcryEndAt = now + SOULCRY_DURATION_MS;
        }

        lastMainHandIsAtomsplit = true;
        lastMainHandModelDiamond = isDiamond;
        lastMainHandModelGold = isGold;
    }

    private static void resetState() {
        lastMainHandIsAtomsplit = false;
        lastMainHandModelDiamond = false;
        lastMainHandModelGold = false;
        scheduledSoulcryEndAt = 0L;
    }

    private static String readSkyblockId(ItemStack stack) {
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return "";
            CompoundTag extraAttributes = customData.copyTag();
            if (extraAttributes == null || !extraAttributes.contains("id")) return "";
            return extraAttributes.getString("id").orElse("");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String readItemModel(ItemStack stack) {
        Identifier itemModel = stack.get(DataComponents.ITEM_MODEL);
        return itemModel != null ? itemModel.toString() : "";
    }
}
