package com.shyeuar.baity.features.enchantlore;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.RomanNumeralUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.List;

@Environment(EnvType.CLIENT)
final class EnchantLoreNumeralApplier {
    private EnchantLoreNumeralApplier() {
    }

    static void applyToTooltip(List<Component> lore, Integer enchantStart, Integer enchantEnd) {
        if (!ConfigManager.enchantLoreArabicNumerals) {
            return;
        }
        int startIndex = ConfigManager.enchantLoreDontReplaceRomanInItemName ? 1 : 0;
        for (int i = startIndex; i < lore.size(); i++) {
            if (enchantStart != null && enchantEnd != null && i >= enchantStart && i <= enchantEnd) {
                continue;
            }
            Component line = lore.get(i);
            Component replaced = RomanNumeralUtils.replaceNumeralsWithIntegers(line);
            if (replaced != line) {
                lore.set(i, replaced);
            }
        }
    }
}
