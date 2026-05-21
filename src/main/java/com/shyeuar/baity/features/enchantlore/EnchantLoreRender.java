package com.shyeuar.baity.features.enchantlore;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Environment(EnvType.CLIENT)
final class EnchantLoreRender {
    private static final String COMMA = ", ";
    private EnchantLoreRender() {
    }

    static List<Component> buildInsertLines(
            TreeSet<EnchantLoreParser.ParsedEnchant> ordered,
            boolean hasLore,
            int maxTooltipWidth,
            long nowMs
    ) {
        if (ordered.isEmpty()) {
            return List.of();
        }
        int numEnchants = ordered.size();
        for (EnchantLoreParser.ParsedEnchant parsed : ordered) {
            maxTooltipWidth = Math.max(maxTooltipWidth, getRenderLength(parsed, nowMs));
        }
        if (numEnchants != 1 && !hasLore) {
            return compress(ordered, maxTooltipWidth, nowMs);
        }
        return expand(ordered, hasLore, nowMs);
    }

    static Component formatEnchant(EnchantLoreParser.ParsedEnchant parsed, long nowMs) {
        EnchantLore.Entry entry = parsed.toEntry();
        String levelText = EnchantLoreParser.integerToRoman(parsed.level);
        int styleLevel = styleLevelFor(parsed);
        Style nameStyle = styleForLevel(entry, styleLevel);
        if (entry.rainbow()) {
            return rainbowText(parsed.def.loreName + " " + levelText, nameStyle, nowMs, 0.0f);
        }
        MutableComponent component = Component.literal(parsed.def.loreName).withStyle(nameStyle);
        component.append(Component.literal(" " + levelText).withStyle(component.getStyle()));
        return component;
    }

    static String stripColor(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ChatFormatting.PREFIX_CODE && i + 1 < input.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private static List<Component> compress(
            TreeSet<EnchantLoreParser.ParsedEnchant> ordered,
            int maxTooltipWidth,
            long nowMs
    ) {
        Font font = Minecraft.getInstance().font;
        int commaLength = font.width(COMMA);
        List<Component> lines = new ArrayList<>();
        int sum = 0;
        MutableComponent loreLine = Component.empty();
        float lineRainbowX = 0.0f;
        for (EnchantLoreParser.ParsedEnchant parsed : ordered) {
            Component formatted = formatEnchant(parsed, nowMs);
            int renderLength = font.width(formatted);
            if (sum + renderLength > maxTooltipWidth && !loreLine.getSiblings().isEmpty()) {
                trimTrailingComma(loreLine);
                lines.add(loreLine);
                loreLine = Component.empty();
                sum = 0;
                lineRainbowX = 0.0f;
            }
            loreLine.append(formatted);
            appendCommaAfterEnchant(loreLine, parsed, lineRainbowX + renderLength, nowMs);
            lineRainbowX += renderLength + commaLength;
            sum += renderLength + commaLength;
        }
        if (font.width(loreLine) >= commaLength) {
            trimTrailingComma(loreLine);
            lines.add(loreLine);
        }
        return lines;
    }

    private static List<Component> expand(
            TreeSet<EnchantLoreParser.ParsedEnchant> ordered,
            boolean hasLore,
            long nowMs
    ) {
        List<Component> lines = new ArrayList<>((hasLore ? 3 : 1) * ordered.size());
        for (EnchantLoreParser.ParsedEnchant parsed : ordered) {
            lines.add(formatEnchant(parsed, nowMs));
            if (hasLore) {
                lines.addAll(parsed.lore());
            }
        }
        return lines;
    }

    private static int getRenderLength(EnchantLoreParser.ParsedEnchant parsed, long nowMs) {
        return Minecraft.getInstance().font.width(formatEnchant(parsed, nowMs));
    }

    private static int styleLevelFor(EnchantLoreParser.ParsedEnchant parsed) {
        if ("efficiency".equals(parsed.def.nbtName)) {
            if (!EnchantLoreParser.isMiningTool(parsed.stack)
                    || "STONK".equals(EnchantLoreParser.skyblockItemId(parsed.stack))) {
                if (parsed.level >= 5) {
                    return parsed.def.maxLevel;
                }
            }
        }
        return parsed.level;
    }

    private static Style styleForLevel(EnchantLore.Entry entry, int styleLevel) {
        EnchantLore.Tier tier = tierForStyle(entry.def(), styleLevel);
        int rgb = switch (tier) {
            case ULTIMATE -> EnchantLore.ULTIMATE_RGB;
            case PERFECT -> EnchantLore.MAX_LEVEL_SOLID_RGB;
            case GREAT -> EnchantLore.GREAT_RGB;
            case GOOD -> EnchantLore.GOOD_RGB;
            case POOR -> EnchantLore.POOR_RGB;
        };
        Style style = Style.EMPTY.withColor(rgb);
        if (entry.ultimate()) {
            style = style.withBold(true);
        }
        return style;
    }

    private static EnchantLore.Tier tierForStyle(EnchantLoreParser.EnchantDef enchant, int level) {
        if (enchant.ultimate) {
            return EnchantLore.Tier.ULTIMATE;
        }
        if (level >= enchant.maxLevel) {
            return EnchantLore.Tier.PERFECT;
        }
        if (level > enchant.goodLevel) {
            return EnchantLore.Tier.GREAT;
        }
        if (level == enchant.goodLevel) {
            return EnchantLore.Tier.GOOD;
        }
        return EnchantLore.Tier.POOR;
    }

    private static Component rainbowText(String text, Style base, long nowMs, float xStart) {
        if (text.isEmpty()) {
            return Component.empty();
        }
        Font font = Minecraft.getInstance().font;
        MutableComponent root = Component.empty();
        float x = xStart;
        for (int i = 0; i < text.length(); i++) {
            String glyph = String.valueOf(text.charAt(i));
            int rgb = EnchantLore.rainbowRgbAt(x, 0.0f, nowMs);
            root.append(Component.literal(glyph).withStyle(base.withColor(rgb)));
            x += font.width(glyph);
        }
        return root;
    }

    private static void appendCommaAfterEnchant(
            MutableComponent loreLine,
            EnchantLoreParser.ParsedEnchant parsed,
            float commaRainbowX,
            long nowMs
    ) {
        EnchantLore.Entry entry = parsed.toEntry();
        if (entry.rainbow()) {
            Style base = styleForLevel(entry, styleLevelFor(parsed));
            loreLine.append(rainbowText(COMMA, base, nowMs, commaRainbowX));
            return;
        }
        loreLine.append(Component.literal(COMMA).withStyle(styleForLevel(entry, styleLevelFor(parsed))));
    }

    private static void trimTrailingComma(MutableComponent line) {
        if (!line.getSiblings().isEmpty()) {
            line.getSiblings().removeLast();
        }
    }
}
