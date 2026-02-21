package com.shyeuar.baity.features.enchantchroma;

import com.shyeuar.baity.features.enchantchroma.data.EnchantDatabase;
import com.shyeuar.baity.features.enchantchroma.data.EnchantInfo;
import com.shyeuar.baity.features.enchantchroma.data.EnchantLoader;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class EnchantChromaProcessor {

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!EnchantChromaConfig.isEnabled()) {
                return;
            }
            processTooltip(lines, stack);
        });
    }

    private static void processTooltip(List<Component> lines, ItemStack stack) {
        EnchantDatabase database = EnchantLoader.getData();
        if (database == null || !database.hasData()) {
            return;
        }

        for (Component line : lines) {
            processComponent(line, database, stack);
        }
    }

    private static void processComponent(Component comp, EnchantDatabase database, ItemStack stack) {
        String text = comp.getString();
        
        if (!text.isBlank()) {
            EnchantChromaUtils.ParsedResult parsed = EnchantChromaUtils.parseEnchantText(text);
            if (parsed != null) {
                applyEnchantColor(comp, parsed, database);
            }
        }

        for (Component sibling : comp.getSiblings()) {
            processComponent(sibling, database, stack);
        }
    }

    private static void applyEnchantColor(Component comp, EnchantChromaUtils.ParsedResult parsed, EnchantDatabase database) {
        if (!(comp instanceof MutableComponent mutable)) {
            return;
        }

        EnchantInfo info = database.findEnchant(parsed.name());
        boolean isUltimate = EnchantChromaUtils.isUltimateEnchant(database, parsed.name());
        EnchantTier tier = EnchantChromaUtils.determineTier(info, parsed.level(), null, isUltimate);

        int colorValue = getTierColor(tier);
        int rgbValue = colorValue & 0xFFFFFF;

        Style style = mutable.getStyle().withColor(TextColor.fromRgb(rgbValue));
        if (tier == EnchantTier.ULTIMATE) {
            style = style.withBold(true);
        }
        mutable.setStyle(style);
    }

    private static int getTierColor(EnchantTier tier) {
        return switch (tier) {
            case PERFECT -> EnchantChromaConfig.MARKER_COLOR;
            case GREAT -> EnchantChromaConfig.COLOR_GREAT;
            case GOOD -> EnchantChromaConfig.COLOR_GOOD;
            case POOR -> EnchantChromaConfig.COLOR_POOR;
            case ULTIMATE -> EnchantChromaConfig.COLOR_ULTIMATE;
        };
    }
}
