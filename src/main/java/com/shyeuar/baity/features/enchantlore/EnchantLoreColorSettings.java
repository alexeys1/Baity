package com.shyeuar.baity.features.enchantlore;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EnchantLoreColorSettings {
    public static final int TIER_COUNT = 5;
    public static final String[] TIER_LABELS = {"Poor", "Good", "Great", "Perfect", "Ultimate"};
    private static final int[] DEFAULT_START = {
            packRgb(-5592406),
            packRgb(-11184641),
            packRgb(-22016),
            packRgb(-22016),
            packRgb(-43521)
    };
    private static final int[] DEFAULT_END = {
            packRgb(-5592406),
            packRgb(-11184641),
            packRgb(-22016),
            packRgb(-22016),
            packRgb(-43521)
    };
    private static final boolean[] DEFAULT_BOLD = {false, false, false, false, true};
    private static final boolean[] DEFAULT_RAINBOW = {false, false, false, true, false};

    private EnchantLoreColorSettings() {
    }

    public static void initDefaults() {
        if (ConfigManager.enchantLoreTierStartColor == null
                || ConfigManager.enchantLoreTierStartColor.length != TIER_COUNT) {
            ConfigManager.enchantLoreTierStartColor = DEFAULT_START.clone();
        }
        if (ConfigManager.enchantLoreTierEndColor == null
                || ConfigManager.enchantLoreTierEndColor.length != TIER_COUNT) {
            ConfigManager.enchantLoreTierEndColor = DEFAULT_END.clone();
        }
        if (ConfigManager.enchantLoreTierBold == null
                || ConfigManager.enchantLoreTierBold.length != TIER_COUNT) {
            ConfigManager.enchantLoreTierBold = DEFAULT_BOLD.clone();
        }
        if (ConfigManager.enchantLoreTierRainbow == null
                || ConfigManager.enchantLoreTierRainbow.length != TIER_COUNT) {
            ConfigManager.enchantLoreTierRainbow = DEFAULT_RAINBOW.clone();
        }
    }

    public static int getStartColor(int tier) {
        initDefaults();
        return ConfigManager.enchantLoreTierStartColor[tier] & 0xFFFFFF;
    }

    public static int getEndColor(int tier) {
        initDefaults();
        return ConfigManager.enchantLoreTierEndColor[tier] & 0xFFFFFF;
    }

    public static boolean isBold(int tier) {
        initDefaults();
        return ConfigManager.enchantLoreTierBold[tier];
    }

    public static boolean isRainbow(int tier) {
        initDefaults();
        return ConfigManager.enchantLoreTierRainbow[tier];
    }

    public static void setStartColor(int tier, int rgb) {
        initDefaults();
        ConfigManager.enchantLoreTierStartColor[tier] = rgb & 0xFFFFFF;
    }

    public static void setEndColor(int tier, int rgb) {
        initDefaults();
        ConfigManager.enchantLoreTierEndColor[tier] = rgb & 0xFFFFFF;
    }

    public static void setBold(int tier, boolean bold) {
        initDefaults();
        ConfigManager.enchantLoreTierBold[tier] = bold;
    }

    public static void setRainbow(int tier, boolean rainbow) {
        initDefaults();
        ConfigManager.enchantLoreTierRainbow[tier] = rainbow;
    }

    public static void resetTier(int tier) {
        setStartColor(tier, DEFAULT_START[tier]);
        setEndColor(tier, DEFAULT_END[tier]);
        setBold(tier, DEFAULT_BOLD[tier]);
        setRainbow(tier, DEFAULT_RAINBOW[tier]);
    }

    public static int tierIndex(EnchantLore.Tier tier) {
        return switch (tier) {
            case POOR -> 0;
            case GOOD -> 1;
            case GREAT -> 2;
            case PERFECT -> 3;
            case ULTIMATE -> 4;
        };
    }

    public static boolean isRainbow(EnchantLore.Tier tier) {
        return isRainbow(tierIndex(tier));
    }

    public static boolean isBold(EnchantLore.Tier tier) {
        return isBold(tierIndex(tier));
    }

    public static int colorAt(EnchantLore.Tier tier, float progress) {
        int start = getStartColor(tierIndex(tier));
        int end = getEndColor(tierIndex(tier));
        if (start == end) {
            return start;
        }
        return lerpRgb(start, end, progress);
    }

    public static String encode() {
        initDefaults();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TIER_COUNT; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(String.format("%06X,%06X,%d,%d",
                    getStartColor(i),
                    getEndColor(i),
                    isBold(i) ? 1 : 0,
                    isRainbow(i) ? 1 : 0));
        }
        return sb.toString();
    }

    public static void decode(String raw) {
        initDefaults();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String[] tiers = raw.split(";");
        for (int i = 0; i < TIER_COUNT && i < tiers.length; i++) {
            String[] parts = tiers[i].split(",", 4);
            if (parts.length < 4) {
                continue;
            }
            Integer start = parseHex(parts[0]);
            Integer end = parseHex(parts[1]);
            if (start != null && end != null) {
                setStartColor(i, start);
                setEndColor(i, end);
            }
            setBold(i, "1".equals(parts[2].trim()));
            setRainbow(i, "1".equals(parts[3].trim()));
        }
    }

    private static int lerpRgb(int start, int end, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int sr = (start >> 16) & 0xFF;
        int sg = (start >> 8) & 0xFF;
        int sb = start & 0xFF;
        int er = (end >> 16) & 0xFF;
        int eg = (end >> 8) & 0xFF;
        int eb = end & 0xFF;
        int r = Math.round(sr + (er - sr) * t);
        int g = Math.round(sg + (eg - sg) * t);
        int b = Math.round(sb + (eb - sb) * t);
        return (r << 16) | (g << 8) | b;
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

    private static int packRgb(int color) {
        return color & 0xFFFFFF;
    }
}
