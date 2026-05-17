package com.shyeuar.baity.features.droppeditem;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class DroppedItemScale {

    private static final String MODULE = "DroppedItem";

    private DroppedItemScale() {
    }

    public static boolean is2dActive() {
        Module module = ModuleManager.getModuleByName(MODULE);
        return module != null && module.isEnabled()
                && ModuleUtils.getOptionBoolean(module, "2D dropped item", false);
    }

    public static boolean isRarityScaleActive() {
        return ConfigManager.droppedItemEnabled && ConfigManager.droppedItemRarityScaleEnabled;
    }

    public static float multiplier(ItemStack stack) {
        if (!isRarityScaleActive() || stack == null || stack.isEmpty()) {
            return 1.0f;
        }
        SkyblockItemRarity rarity = resolveRarity(stack);
        if (rarity == SkyblockItemRarity.UNKNOWN) {
            return 1.0f;
        }
        return (float) ConfigManager.getDroppedItemRarityScale(rarity);
    }

    private static SkyblockItemRarity resolveRarity(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag != null && "PET".equals(tag.getString("id").orElse(""))) {
                return petRarity(tag);
            }
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return SkyblockItemRarity.UNKNOWN;
        }
        List<net.minecraft.network.chat.Component> lines = lore.lines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            var found = SkyblockItemRarity.fromLoreLine(lines.get(i).getString());
            if (found.isPresent()) {
                return found.get();
            }
        }
        return SkyblockItemRarity.UNKNOWN;
    }

    private static SkyblockItemRarity petRarity(CompoundTag tag) {
        String petInfo = tag.getString("petInfo").orElse("");
        if (petInfo.isEmpty()) {
            return SkyblockItemRarity.UNKNOWN;
        }
        try {
            JsonObject json = JsonParser.parseString(petInfo).getAsJsonObject();
            if (!json.has("tier")) {
                return SkyblockItemRarity.UNKNOWN;
            }
            return SkyblockItemRarity.fromTierName(json.get("tier").getAsString());
        } catch (Throwable ignored) {
            return SkyblockItemRarity.UNKNOWN;
        }
    }
}