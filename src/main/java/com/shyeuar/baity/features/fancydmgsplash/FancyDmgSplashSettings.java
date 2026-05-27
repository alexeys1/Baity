package com.shyeuar.baity.features.fancydmgsplash;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueTreeUtils;
import com.shyeuar.baity.utils.ColorGradientUtils;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class FancyDmgSplashSettings {
    public static final String DEFAULT_SYMBOL = "✧";
    public static final int MAX_DAMAGE_SYMBOL_CODE_POINTS = 1;
    public static final int DEFAULT_GRADIENT_START = 0xFFFF55;
    public static final int DEFAULT_GRADIENT_END = 0xFF5555;

    public enum DamageKind {
        CRITICAL,
        PLAIN_NORMAL,
        BURN,
        SPECIAL
    }

    private static final Pattern DAMAGE_TEXT_PATTERN =
            Pattern.compile("([✧✯]?)([\\d,]+(?:\\.\\d+)?[kKmMbBtTqQ]?)([✧✯]?)([❤+⚔☄♞✷ﬗ✯]*)");
    private static final Pattern COMPACT_SUFFIX_PATTERN = Pattern.compile(".*[kKmMbBtTqQ]$");
    private static final int[] COMPACT_DECIMAL_DIGIT_ESTIMATES = {
            0, 1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10,
            10, 10, 10, 11, 11, 11, 12, 12, 12, 13, 13, 13, 13, 14, 14, 14, 15, 15, 15, 16, 16, 16, 16, 17, 17, 17, 18, 18, 18, 19, 19, 19
    };
    private static final long[] COMPACT_TEN_POWERS = {
            1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L,
            10000000000L, 100000000000L, 1000000000000L, 10000000000000L, 100000000000000L,
            1000000000000000L, 10000000000000000L, 100000000000000000L, 1000000000000000000L
    };

    private FancyDmgSplashSettings() {
    }

    public static int symbolCodePointCount(String symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return 0;
        }
        return symbols.codePointCount(0, symbols.length());
    }

    public static String clampSymbols(String symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return "";
        }
        if (symbolCodePointCount(symbols) <= MAX_DAMAGE_SYMBOL_CODE_POINTS) {
            return symbols;
        }
        return symbols.substring(0, symbols.offsetByCodePoints(0, MAX_DAMAGE_SYMBOL_CODE_POINTS));
    }

    public static String maxWidthSymbolPaddingSample() {
        return DEFAULT_SYMBOL;
    }

    public static float symbolInputWidth(net.minecraft.client.gui.Font font) {
        return Math.max(font.width(DEFAULT_SYMBOL), font.width("☀"));
    }

    public static final String[] PRESET_SYMBOLS = {"❄", "⚶", "⚡", "♨", "☀", "≈", "☘", "✷"};
    public static final String[] PRESET_TOOLTIPS = {
            "Frozen",
            "Wet",
            "Electro-Charged",
            "Burning",
            "Crystallize",
            "Swirl",
            "Bloom",
            "Shatter"
    };

    public static String formatCompactDamage(double damage, int maxPrecision) {
        long damageLong = (long) damage;
        long adjustedDamage = damageLong;
        int currentDigits = countCompactDecimalDigits(adjustedDamage);
        if (currentDigits > maxPrecision) {
            double roundingFactor = COMPACT_TEN_POWERS[currentDigits - maxPrecision];
            adjustedDamage = (long) (Math.round((double) adjustedDamage / roundingFactor) * roundingFactor);
        }
        if (adjustedDamage < 1_000L) {
            return String.valueOf(adjustedDamage);
        }
        if (adjustedDamage < 1_000_000L) {
            return String.format("%.1fk", adjustedDamage / 1_000.0);
        }
        if (adjustedDamage < 1_000_000_000L) {
            return String.format("%.1fM", adjustedDamage / 1_000_000.0);
        }
        if (adjustedDamage < 1_000_000_000_000L) {
            return String.format("%.1fB", adjustedDamage / 1_000_000_000.0);
        }
        if (adjustedDamage < 1_000_000_000_000_000L) {
            return String.format("%.1fT", adjustedDamage / 1_000_000_000_000.0);
        }
        return String.format("%.1fQ", adjustedDamage / 1_000_000_000_000_000.0);
    }

    private static int countCompactDecimalDigits(long value) {
        int estimate = COMPACT_DECIMAL_DIGIT_ESTIMATES[64 - Long.numberOfLeadingZeros(value)];
        return estimate + ((value >= COMPACT_TEN_POWERS[estimate]) ? 1 : 0);
    }

    public static void onAppearanceSettingChanged() {
        FancyDmgSplashPresetStore.handleAppearanceEdit();
    }

    public static Component createCritSourceComponent(long damage) {
        return Component.literal(DEFAULT_SYMBOL + damage + DEFAULT_SYMBOL);
    }

    public static DamageKind classifyDamage(Component originalText) {
        if (originalText == null) {
            return DamageKind.SPECIAL;
        }
        String textContent = originalText.getString();
        Matcher matcher = DAMAGE_TEXT_PATTERN.matcher(textContent);
        if (!matcher.matches()) {
            return DamageKind.SPECIAL;
        }
        if (!matcher.group(1).isEmpty() || !matcher.group(3).isEmpty()) {
            return DamageKind.CRITICAL;
        }
        String suffix = matcher.group(4);
        if (suffix != null && !suffix.isEmpty()) {
            return DamageKind.SPECIAL;
        }
        int color = extractColorFromText(originalText);
        if (isBurnColor(color)) {
            return DamageKind.BURN;
        }
        if (isPlainNormalColor(color)) {
            return DamageKind.PLAIN_NORMAL;
        }
        return DamageKind.SPECIAL;
    }

    public static int extractColorFromText(Component text) {
        if (text == null) {
            return 0xFFFFFF;
        }
        Style style = text.getStyle();
        if (style != null) {
            TextColor textColor = style.getColor();
            if (textColor != null) {
                return textColor.getValue();
            }
        }
        for (Component sibling : text.getSiblings()) {
            Style siblingStyle = sibling.getStyle();
            if (siblingStyle != null) {
                TextColor textColor = siblingStyle.getColor();
                if (textColor != null) {
                    return textColor.getValue();
                }
            }
        }
        return 0xFFFFFF;
    }

    public static boolean isPlainNormalColor(int color) {
        int normalized = color & 0xFFFFFF;
        return normalized == 0xFFFFFF
                || normalized == 0xAAAAAA
                || normalized == 0x7F7F7F
                || normalized == (TextColor.fromLegacyFormat(ChatFormatting.GRAY).getValue() & 0xFFFFFF);
    }

    public static boolean isBurnColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return r >= 0xC0 && g >= 0x35 && g <= 0xB5 && b <= 0x70;
    }

    public static boolean isSyncNonCriticalEnabled() {
        Module module = ModuleManager.getModuleByName("FancyDmgSplash");
        if (module != null) {
            return ModuleUtils.getOptionBoolean(module, "sync non-critical dmg", ConfigManager.fancyDmgSplashSyncNonCritical);
        }
        return ConfigManager.fancyDmgSplashSyncNonCritical;
    }

    public static Component formatCriticalDamage(Component originalText, double damage, FancyDmgSplashPresetStore.PresetData style) {
        if (originalText == null || style == null) {
            return originalText;
        }
        String textContent = originalText.getString();
        if (style.compact && hasCompactSuffix(textContent)) {
            return applyCriticalToExistingText(originalText, textContent, style);
        }
        Matcher matcher = DAMAGE_TEXT_PATTERN.matcher(textContent);
        if (!matcher.matches()) {
            return originalText;
        }
        String suffix = matcher.group(4) == null ? "" : matcher.group(4);
        String symbols = style.symbols;
        String numericDisplay = buildCriticalNumericDisplay(damage, textContent, style);
        String displayText = symbols + numericDisplay + symbols;
        MutableComponent result = buildPresetColoredText(displayText, style.gradientStart, style.gradientEnd);
        if (style.bold) {
            result = result.copy().withStyle(result.getStyle().withBold(true));
        }
        appendSuffix(result, originalText, suffix);
        return result;
    }

    public static Component formatSyncNonCrit(Component originalText, double damage, FancyDmgSplashPresetStore.PresetData style) {
        if (originalText == null || style == null) {
            return originalText;
        }
        NonCritFormat format = resolveNonCritFormat(originalText, damage);
        if (format == null) {
            return originalText;
        }
        MutableComponent result = buildPresetColoredText(format.numericDisplay, style.gradientStart, style.gradientEnd);
        appendSuffix(result, originalText, format.suffix);
        return result;
    }

    public static Component formatLegacyNonCrit(Component originalText, double damage) {
        if (originalText == null) {
            return null;
        }
        if (hasCompactSuffix(originalText.getString())) {
            return originalText;
        }
        List<Component> siblings = originalText.getSiblings();
        if (siblings.isEmpty()) {
            return originalText;
        }
        NonCritFormat format = resolveNonCritFormat(originalText, damage);
        if (format == null) {
            return originalText;
        }
        TextColor originalColor = siblings.getFirst().getStyle().getColor();
        int displayColor;
        if (originalColor == null || originalColor == TextColor.fromLegacyFormat(ChatFormatting.GRAY)) {
            displayColor = ConfigManager.fancyDmgSplashNormalDamageColor & 0xFFFFFF;
        } else {
            displayColor = originalColor.getValue();
        }
        MutableComponent result = Component.literal(format.numericDisplay)
                .setStyle(originalText.getStyle())
                .withStyle(Style.EMPTY.withColor(displayColor));
        appendSuffix(result, originalText, format.suffix);
        return result;
    }

    private static NonCritFormat resolveNonCritFormat(Component originalText, double damage) {
        String textContent = originalText.getString();
        Matcher matcher = DAMAGE_TEXT_PATTERN.matcher(textContent);
        if (!matcher.matches()) {
            return null;
        }
        String suffix = matcher.group(4) == null ? "" : matcher.group(4);
        String numericDisplay = hasCompactSuffix(textContent)
                ? matcher.group(2)
                : buildNonCritNumericDisplay(damage, matcher.group(2));
        return new NonCritFormat(numericDisplay, suffix);
    }

    private static String buildCriticalNumericDisplay(double damage, String originalText, FancyDmgSplashPresetStore.PresetData style) {
        long damageValue = (long) damage;
        if (style.compact && (damageValue >= 1000L)) {
            return formatCompactDamage(damageValue, 4);
        }
        String numericPart = originalText.replaceAll("[^\\d]", "");
        if (numericPart.isEmpty()) {
            numericPart = String.valueOf(damageValue);
        }
        return applySeparator(numericPart);
    }

    private static String buildNonCritNumericDisplay(double damage, String matchedNumeric) {
        long damageValue = (long) damage;
        if (FancyDmgSplashPresetStore.allSelectedPresetCompact() && damageValue >= 1000L) {
            return formatCompactDamage(damageValue, 4);
        }
        String digits = matchedNumeric == null ? "" : matchedNumeric.replaceAll("[^\\d]", "");
        if (digits.isEmpty()) {
            digits = String.valueOf(damageValue);
        }
        return applySeparator(digits);
    }

    private static Component applyCriticalToExistingText(Component originalText, String textContent, FancyDmgSplashPresetStore.PresetData style) {
        Matcher matcher = DAMAGE_TEXT_PATTERN.matcher(textContent);
        if (!matcher.matches()) {
            return originalText;
        }
        String suffix = matcher.group(4) == null ? "" : matcher.group(4);
        String symbols = style.symbols;
        String numericPart = matcher.group(2);
        String displayText = symbols + numericPart + symbols;
        MutableComponent result = buildPresetColoredText(displayText, style.gradientStart, style.gradientEnd);
        if (style.bold) {
            result = result.copy().withStyle(result.getStyle().withBold(true));
        }
        appendSuffix(result, originalText, suffix);
        return result;
    }

    private static void appendSuffix(MutableComponent result, Component originalText, String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return;
        }
        Style suffixStyle = originalText.getSiblings().isEmpty()
                ? originalText.getStyle()
                : originalText.getSiblings().getLast().getStyle();
        result.append(Component.literal(suffix).setStyle(suffixStyle));
    }

    public static String applySeparator(String digits) {
        return applySeparator(digits, separatorMode());
    }

    public static String applySeparator(String digits, String separatorMode) {
        char separator = separatorChar(separatorMode);
        if (separator == 0 || digits.length() <= 3) {
            return digits;
        }
        StringBuilder builder = new StringBuilder();
        int len = digits.length();
        int firstGroup = len % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }
        builder.append(digits, 0, firstGroup);
        for (int i = firstGroup; i < len; i += 3) {
            builder.append(separator);
            builder.append(digits, i, i + 3);
        }
        return builder.toString();
    }

    public static char separatorChar(String separatorMode) {
        return switch (separatorMode) {
            case "comma" -> ',';
            case "hyphen" -> '-';
            case "underscore" -> '_';
            default -> 0;
        };
    }

    public static String separatorMode() {
        Module module = ModuleManager.getModuleByName("FancyDmgSplash");
        if (module != null) {
            String mode = ModuleUtils.getOptionString(module, "separator", ConfigManager.fancyDmgSplashSeparator);
            if (mode == null || mode.isEmpty()) {
                return "none";
            }
            return mode;
        }
        String mode = ConfigManager.fancyDmgSplashSeparator;
        if (mode == null || mode.isEmpty()) {
            return "none";
        }
        return mode;
    }

    public static Component formatPreviewWithPreset(FancyDmgSplashPresetStore.PresetData style) {
        String symbols = style.symbols;
        long previewDamage = 1_100_000L;
        String numericPart = style.compact
                ? formatCompactDamage(previewDamage, 4)
                : applySeparator(String.valueOf(previewDamage));
        String displayText = symbols + numericPart + symbols;
        MutableComponent result = buildPresetColoredText(displayText, style.gradientStart, style.gradientEnd);
        if (style.bold) {
            result = result.copy().withStyle(result.getStyle().withBold(true));
        }
        return result;
    }

    private static MutableComponent buildPresetColoredText(String displayText, int start, int end) {
        boolean useGradient = (start & 0xFFFFFF) != (end & 0xFFFFFF);
        MutableComponent result = Component.empty();
        int textLength = displayText.length();
        if (textLength == 0) {
            return result;
        }
        if (useGradient) {
            for (int i = 0; i < textLength; i++) {
                float ratio = textLength <= 1 ? 0f : i / (textLength - 1.0f);
                int color = ColorGradientUtils.blendColors(start & 0xFFFFFF, end & 0xFFFFFF, ratio);
                appendChar(result, displayText, i, color);
            }
            return result;
        }
        for (int i = 0; i < textLength; i++) {
            appendChar(result, displayText, i, start & 0xFFFFFF);
        }
        return result;
    }

    private static void appendChar(MutableComponent result, String text, int index, int color) {
        int cp = text.codePointAt(index);
        result.append(Component.literal(new String(Character.toChars(cp)))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF))));
    }

    private static boolean hasCompactSuffix(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String cleaned = text.replaceAll("[^\\d.,kKmMbBtTqQ]", "");
        return COMPACT_SUFFIX_PATTERN.matcher(cleaned).find();
    }

    public static String encodeColorEditor() {
        FancyDmgSplashColorEditorValue editor = findColorEditor();
        if (editor != null) {
            editor.persistToConfig();
            return String.format("#%06X,#%06X|%s",
                    editor.gradient().getStartColor() & 0xFFFFFF,
                    editor.gradient().getEndColor() & 0xFFFFFF,
                    editor.getSymbols());
        }
        return String.format("#%06X,#%06X|%s",
                ConfigManager.fancyDmgSplashCritGradientStart & 0xFFFFFF,
                ConfigManager.fancyDmgSplashCritGradientEnd & 0xFFFFFF,
                ConfigManager.fancyDmgSplashDamageSymbols);
    }

    public static void decodeColorEditor(String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split("\\|", 2);
        String gradientPart = parts[0];
        String[] colors = gradientPart.split(",", 2);
        if (colors.length == 2) {
            Integer start = parseHex(colors[0]);
            Integer end = parseHex(colors[1]);
            if (start != null) {
                ConfigManager.fancyDmgSplashCritGradientStart = start;
            }
            if (end != null) {
                ConfigManager.fancyDmgSplashCritGradientEnd = end;
            }
        }
        if (parts.length == 2) {
            ConfigManager.fancyDmgSplashDamageSymbols = clampSymbols(parts[1]);
        }
        onAppearanceSettingChanged();
    }

    public static void resetColorEditorDefaults() {
        applyPresetToLiveConfig(FancyDmgSplashPresetStore.PresetData.vanillaDefault());
        onAppearanceSettingChanged();
    }

    private static void applyPresetToLiveConfig(FancyDmgSplashPresetStore.PresetData data) {
        ConfigManager.fancyDmgSplashCritGradientStart = data.gradientStart;
        ConfigManager.fancyDmgSplashCritGradientEnd = data.gradientEnd;
        ConfigManager.fancyDmgSplashDamageSymbols = data.symbols;
        ConfigManager.fancyDmgSplashBold = data.bold;
        ConfigManager.fancyDmgSplashCompactDamageNumber = data.compact;
        FancyDmgSplashColorEditorValue editor = findColorEditor();
        if (editor != null) {
            editor.loadFromConfig();
        }
    }

    private static FancyDmgSplashColorEditorValue findColorEditor() {
        Module module = ModuleManager.getModuleByName("FancyDmgSplash");
        if (module == null) {
            return null;
        }
        Value value = ValueTreeUtils.findByName(module, "color editor");
        return value instanceof FancyDmgSplashColorEditorValue editor ? editor : null;
    }

    private static Integer parseHex(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("^[0-9A-Fa-f]{6}$")) {
            return null;
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record NonCritFormat(String numericDisplay, String suffix) {
    }
}
