package com.shyeuar.baity.features.enchantlore;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
final class EnchantLoreParser {
    private static final Gson GSON = new Gson();
    private static final Pattern ENCHANTMENT_PATTERN = Pattern.compile(
            "(?<enchant>[A-Za-z][A-Za-z -]+) (?<levelNumeral>(?=[MDCLXVI])M{0,3}(?:CM|CD|D?C{0,3})(?:XC|XL|L?X{0,3})(?:IX|IV|V?I{0,3}))(?=, |$| [\\d,]+$)"
    );
    private static final TreeMap<Integer, String> INT_ROMAN_MAP = new TreeMap<>(Map.ofEntries(
            Map.entry(1000, "M"),
            Map.entry(900, "CM"),
            Map.entry(500, "D"),
            Map.entry(400, "CD"),
            Map.entry(100, "C"),
            Map.entry(90, "XC"),
            Map.entry(50, "L"),
            Map.entry(40, "XL"),
            Map.entry(10, "X"),
            Map.entry(9, "IX"),
            Map.entry(5, "V"),
            Map.entry(4, "IV"),
            Map.entry(1, "I")
    ));
    private static final Comparator<EnchantDef> ENCHANT_ORDER = Comparator
            .comparingInt((EnchantDef e) -> e.ultimate ? 0 : 1)
            .thenComparingInt(e -> e.stacking ? 0 : 1)
            .thenComparing(e -> e.loreName);
    private static Catalog catalog;
    private EnchantLoreParser() {
    }

    static Section findSection(List<Component> lore, ItemStack stack) {
        Map<String, Integer> enchantments = enchantmentsOn(stack);
        if (enchantments.isEmpty() && !isSuperpairsScreen()) {
            return null;
        }
        Map<String, Integer> attributes = attributesOn(stack);
        int start = -1;
        int end = -1;
        int maxTooltipWidth = 0;
        for (int i = 0; i < lore.size(); i++) {
            Component line = lore.get(i);
            String stripped = EnchantLoreRender.stripColor(line.getString());
            if (start == -1) {
                if (lineContainsItemEnchant(enchantments, attributes, stripped)) {
                    start = i;
                }
            } else if (stripped.isBlank() && end == -1) {
                end = i - 1;
            }
            if (start == -1 || end != -1) {
                maxTooltipWidth = Math.max(Minecraft.getInstance().font.width(line), maxTooltipWidth);
            }
        }
        if (enchantments.isEmpty() && end == -1 && start != -1) {
            end = start;
        }
        if (start == -1 || end == -1) {
            return null;
        }
        maxTooltipWidth = correctTooltipWidth(maxTooltipWidth);
        return new Section(start, end, maxTooltipWidth);
    }

    static CollectResult collectEnchants(List<Component> lore, ItemStack stack, Section section) {
        Map<String, Integer> enchantments = enchantmentsOn(stack);
        Map<String, Integer> attributes = attributesOn(stack);
        TreeSet<ParsedEnchant> ordered = new TreeSet<>();
        ParsedEnchant lastEnchant = null;
        boolean hasLore = false;
        for (int i = section.start(); i <= section.end(); i++) {
            Component originalLine = lore.get(i);
            String unformattedLine = EnchantLoreRender.stripColor(originalLine.getString());
            Matcher matcher = ENCHANTMENT_PATTERN.matcher(unformattedLine);
            boolean containsEnchant = false;
            while (matcher.find()) {
                Optional<EnchantDef> def = catalog().fromLore(matcher.group("enchant"));
                if (def.isEmpty() || !isEnchantOnItem(def.get(), enchantments, attributes)) {
                    continue;
                }
                int level = parseNumeral(matcher.group("levelNumeral"));
                if (level <= 0) {
                    continue;
                }
                ParsedEnchant candidate = new ParsedEnchant(stack, def.get(), level);
                if (!ordered.add(candidate)) {
                    for (ParsedEnchant existing : ordered) {
                        if (existing.compareTo(candidate) == 0) {
                            lastEnchant = existing;
                            break;
                        }
                    }
                } else {
                    lastEnchant = candidate;
                }
                containsEnchant = true;
            }
            if (!containsEnchant && lastEnchant != null) {
                lastEnchant.addLore(originalLine);
                hasLore = true;
            }
        }
        return new CollectResult(ordered, hasLore);
    }

    static String integerToRoman(int number) {
        StringBuilder result = new StringBuilder();
        while (number > 0) {
            var entry = INT_ROMAN_MAP.floorEntry(number);
            result.append(entry.getValue());
            number -= entry.getKey();
        }
        return result.toString();
    }

    static boolean isMiningTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(ItemTags.PICKAXES)) {
            return true;
        }
        CompoundTag extra = extraAttributes(stack);
        if (extra == null) {
            return false;
        }
        if (extra.contains("drill_fuel")) {
            return true;
        }
        return "GEMSTONE_GAUNTLET".equals(extra.getString("id").orElse(null));
    }

    static String skyblockItemId(ItemStack stack) {
        CompoundTag extra = extraAttributes(stack);
        return extra == null ? null : extra.getString("id").orElse(null);
    }

    private static boolean isSuperpairsScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        return EnchantLoreRender.stripColor(screen.getTitle().getString()).contains("Superpairs");
    }

    private static boolean lineContainsItemEnchant(
            Map<String, Integer> enchantments,
            Map<String, Integer> attributes,
            String strippedLine
    ) {
        Matcher matcher = ENCHANTMENT_PATTERN.matcher(strippedLine);
        while (matcher.find()) {
            Optional<EnchantDef> def = catalog().fromLore(matcher.group("enchant"));
            if (def.isPresent() && isEnchantOnItem(def.get(), enchantments, attributes)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEnchantOnItem(
            EnchantDef enchant,
            Map<String, Integer> enchantments,
            Map<String, Integer> attributes
    ) {
        if (!enchantments.containsKey(enchant.nbtName)) {
            return false;
        }
        return !attributes.containsKey(enchant.nbtName);
    }

    private static Catalog catalog() {
        if (catalog == null) {
            catalog = Catalog.load();
        }
        return catalog;
    }

    private static Map<String, Integer> enchantmentsOn(ItemStack stack) {
        CompoundTag tag = extraAttributes(stack);
        if (tag == null) {
            return Collections.emptyMap();
        }
        HashMap<String, Integer> map = new HashMap<>();
        tag.getCompound("enchantments").ifPresent(enchants -> enchants.forEach((key, value) ->
                map.put(key, value.asInt().orElse(0))));
        return map;
    }

    private static Map<String, Integer> attributesOn(ItemStack stack) {
        CompoundTag tag = extraAttributes(stack);
        if (tag == null) {
            return Collections.emptyMap();
        }
        HashMap<String, Integer> map = new HashMap<>();
        tag.getCompound("attributes").ifPresent(attrs -> attrs.forEach((key, value) ->
                map.put(key, value.asInt().orElse(0))));
        return map;
    }

    private static CompoundTag extraAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    private static int parseNumeral(String numeral) {
        if (numeral == null || numeral.isEmpty()) {
            return 0;
        }
        int total = 0;
        int prev = 0;
        for (int i = numeral.length() - 1; i >= 0; i--) {
            int val = romanValue(numeral.charAt(i));
            if (val < prev) {
                total -= val;
            } else {
                total += val;
            }
            prev = val;
        }
        return total;
    }

    private static int romanValue(char c) {
        return switch (c) {
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

    private static int correctTooltipWidth(int maxTooltipWidth) {
        var mc = Minecraft.getInstance();
        var window = mc.getWindow();
        int mouseX = (int) mc.mouseHandler.xpos();
        int tooltipX = mouseX + 12;
        if (tooltipX + maxTooltipWidth + 4 > window.getGuiScaledWidth()) {
            tooltipX = mouseX - 16 - maxTooltipWidth;
            if (tooltipX < 4) {
                if (mouseX > window.getGuiScaledWidth() / 2) {
                    maxTooltipWidth = mouseX - 12 - 8;
                } else {
                    maxTooltipWidth = window.getGuiScaledWidth() - 16 - mouseX;
                }
            }
        }
        if (window.getGuiScaledWidth() > 0 && maxTooltipWidth > window.getGuiScaledWidth()) {
            maxTooltipWidth = window.getGuiScaledWidth();
        }
        return maxTooltipWidth;
    }

    record Section(int start, int end, int maxTooltipWidth) {
    }

    record CollectResult(TreeSet<ParsedEnchant> ordered, boolean hasLore) {
    }

    static final class ParsedEnchant implements Comparable<ParsedEnchant> {
        final ItemStack stack;
        final EnchantDef def;
        final int level;
        private final List<Component> loreDescription = new java.util.ArrayList<>();
        ParsedEnchant(ItemStack stack, EnchantDef def, int level) {
            this.stack = stack;
            this.def = def;
            this.level = level;
        }
        void addLore(Component line) {
            loreDescription.add(line);
        }
        List<Component> lore() {
            return loreDescription;
        }
        EnchantLore.Entry toEntry() {
            EnchantLore.Tier tier = tierFor(def, level);
            boolean rainbow = EnchantLore.isEnabled()
                    && !def.ultimate
                    && level >= def.maxLevel
                    && EnchantLoreColorSettings.isRainbow(tier);
            return new EnchantLore.Entry(def, level, tier, rainbow);
        }
        @Override
        public int compareTo(ParsedEnchant other) {
            return def.compareTo(other.def);
        }
    }

    private static EnchantLore.Tier tierFor(EnchantDef enchant, int level) {
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

    static final class EnchantDef implements Comparable<EnchantDef> {
        final String loreName;
        final String nbtName;
        final int goodLevel;
        final int maxLevel;
        final boolean ultimate;
        final boolean stacking;
        EnchantDef(String loreName, String nbtName, int goodLevel, int maxLevel, boolean ultimate, boolean stacking) {
            this.loreName = loreName;
            this.nbtName = nbtName;
            this.goodLevel = goodLevel;
            this.maxLevel = maxLevel;
            this.ultimate = ultimate;
            this.stacking = stacking;
        }
        @Override
        public int compareTo(EnchantDef other) {
            return ENCHANT_ORDER.compare(this, other);
        }
    }

    private static final class Catalog {
        @SerializedName("NORMAL")
        HashMap<String, RawEnchant> normal = new HashMap<>();
        @SerializedName("ULTIMATE")
        HashMap<String, RawEnchant> ultimate = new HashMap<>();
        @SerializedName("STACKING")
        HashMap<String, RawEnchant> stacking = new HashMap<>();
        final Map<String, EnchantDef> byLoreKey = new HashMap<>();
        static Catalog load() {
            try (var reader = new InputStreamReader(
                    EnchantLore.class.getResourceAsStream("/assets/baity/enchants.json"),
                    StandardCharsets.UTF_8)) {
                Catalog loaded = GSON.fromJson(reader, Catalog.class);
                if (loaded != null) {
                    loaded.index();
                    return loaded;
                }
            } catch (Exception ignored) {
            }
            Catalog empty = new Catalog();
            empty.index();
            return empty;
        }
        void index() {
            indexGroup(normal, false, false);
            indexGroup(ultimate, true, false);
            indexGroup(stacking, false, true);
        }
        void indexGroup(Map<String, RawEnchant> group, boolean ultimate, boolean stacking) {
            for (RawEnchant raw : group.values()) {
                EnchantDef def = new EnchantDef(
                        raw.loreName,
                        raw.nbtName,
                        raw.goodLevel,
                        raw.maxLevel,
                        ultimate,
                        stacking
                );
                byLoreKey.put(raw.loreName.toLowerCase(Locale.US), def);
            }
        }
        Optional<EnchantDef> fromLore(String loreName) {
            if (loreName == null || loreName.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(byLoreKey.get(loreName.trim().toLowerCase(Locale.US)));
        }
    }

    private static final class RawEnchant {
        @SerializedName("loreName")
        String loreName;
        @SerializedName("nbtName")
        String nbtName;
        @SerializedName("goodLevel")
        int goodLevel;
        @SerializedName("maxLevel")
        int maxLevel;
    }
}
