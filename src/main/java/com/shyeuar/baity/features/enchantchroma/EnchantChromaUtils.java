package com.shyeuar.baity.features.enchantchroma;

import com.shyeuar.baity.features.enchantchroma.data.EnchantDatabase;
import com.shyeuar.baity.features.enchantchroma.data.EnchantInfo;
import net.minecraft.world.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnchantChromaUtils {

    private static final Pattern ENCHANT_TEXT_PATTERN = Pattern.compile(
        "(?:§[0-9a-fk-or])*(?<name>[A-Za-z][A-Za-z '-]+) (?<value>[IVXLCDM]+|[0-9]+)"
    );

    public static ParsedResult parseEnchantText(String text) {
        String cleaned = stripFormatting(text).trim();
        Matcher matcher = ENCHANT_TEXT_PATTERN.matcher(cleaned);
        if (!matcher.find()) {
            return null;
        }

        String enchantName = matcher.group("name").trim();
        String levelText = matcher.group("value").trim();
        int enchantLevel = convertToInteger(levelText);
        if (enchantLevel <= 0) {
            return null;
        }

        return new ParsedResult(enchantName, enchantLevel, text);
    }

    public static EnchantTier determineTier(EnchantInfo info, int level, ItemStack stack, boolean isUltimate) {
        if (isUltimate) {
            return EnchantTier.ULTIMATE;
        }

        if (info == null) {
            return EnchantTier.POOR;
        }

        return evaluateTier(info, level);
    }

    public static boolean isUltimateEnchant(EnchantDatabase database, String loreName) {
        String normalizedKey = loreName.toLowerCase().trim();
        return database.ultimate != null && database.ultimate.containsKey(normalizedKey);
    }

    private static EnchantTier evaluateTier(EnchantInfo info, int level) {
        if (level >= info.maxLevel) {
            return EnchantTier.PERFECT;
        }
        if (level > info.goodLevel) {
            return EnchantTier.GREAT;
        }
        if (level == info.goodLevel) {
            return EnchantTier.GOOD;
        }
        return EnchantTier.POOR;
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    private static int convertToInteger(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {}
        
        return parseRomanNumerals(text);
    }

    private static int parseRomanNumerals(String text) {
        int total = 0;
        int previousValue = 0;
        String upperText = text.toUpperCase();
        
        for (int i = upperText.length() - 1; i >= 0; i--) {
            int currentValue = getRomanValue(upperText.charAt(i));
            total += (currentValue < previousValue) ? -currentValue : currentValue;
            previousValue = currentValue;
        }
        return total;
    }

    private static int getRomanValue(char ch) {
        return switch (ch) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            case 'D' -> 500;
            case 'M' -> 1000;
            default -> 0;
        };
    }

    public record ParsedResult(String name, int level, String originalText) {}
}
