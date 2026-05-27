package com.shyeuar.baity.features.fancydmgsplash;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.value.FancyDmgSplashColorEditorValue;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.value.ValueTreeUtils;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public final class FancyDmgSplashPresetStore {
    public static final int BUILTIN_COUNT = 8;
    public static final int CUSTOM_MAX = 24;
    public static final int CUSTOM_COLUMNS = 8;
    public static final int CUSTOM_MAX_ROWS = 3;
    public static final float PRESET_EDIT_FRAME_PAD = 3f;
    public static final int PRESET_SKY_BLUE_RGB = 0x55FFFF;

    public static final int HIT_NONE = -1;
    public static final int HIT_ADD = -2;
    public static final int HIT_BUILTIN_BASE = 0;
    public static final int HIT_CUSTOM_BASE = 100;

    private static final int BUILTIN_WEIGHT = 10;
    private static final int PHYSICAL_WEIGHT = 4;
    private static final int CUSTOM_WEIGHT = 10;

    private static final Random random = new Random();
    private static final List<PresetData> customPresets = new ArrayList<>();
    private static final List<PresetCycleEntry> cycleEntries = new ArrayList<>();

    private static int editingCycleIndex = 0;
    private static int previewingBuiltinIndex = -1;
    private static int deleteArmedCustomIndex = -1;
    private static boolean loadingPreset = false;
    private static boolean storeInitialized = false;

    private FancyDmgSplashPresetStore() {
    }

    public static void ensureInitialized() {
        if (storeInitialized) {
            return;
        }
        migrateLegacyBuiltinMask();
        if (customPresets.isEmpty()) {
            decodeCustomPresets(ConfigManager.fancyDmgSplashCustomPresets);
        }
        if (customPresets.isEmpty()
                && (ConfigManager.fancyDmgSplashCustomPresets == null || ConfigManager.fancyDmgSplashCustomPresets.isEmpty())
                && ConfigManager.fancyDmgSplashBuiltinPresetMask == 0
                && ConfigManager.fancyDmgSplashCustomPresetMask == 0) {
            customPresets.add(PresetData.vanillaDefault());
            ConfigManager.fancyDmgSplashCustomPresetMask = 1;
            ConfigManager.fancyDmgSplashEditingCustomIndex = 0;
        }
        ensureAtLeastOneSelectedForUse();
        rebuildCycleEntries();
        if (!customPresets.isEmpty() && previewingBuiltinIndex < 0) {
            int index = ConfigManager.fancyDmgSplashEditingCustomIndex;
            if (index < 0 || index >= customPresets.size()) {
                index = 0;
            }
            loadEditingCustomIntoEditor(index);
        } else {
            int builtin = previewingBuiltinIndex >= 0 ? previewingBuiltinIndex : BUILTIN_COUNT - 1;
            selectEditingBuiltin(builtin, false);
        }
        storeInitialized = true;
    }

    private static void ensureAtLeastOneSelectedForUse() {
        if (ConfigManager.fancyDmgSplashCustomPresetMask == 0
                && ConfigManager.fancyDmgSplashBuiltinPresetMask == 0) {
            if (!customPresets.isEmpty()) {
                ConfigManager.fancyDmgSplashCustomPresetMask = 1;
            } else {
                ConfigManager.fancyDmgSplashBuiltinPresetMask = 1 << (BUILTIN_COUNT - 1);
            }
        }
    }

    public static void decodeCustomPresets(String raw) {
        customPresets.clear();
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (String part : raw.split(";", -1)) {
            if (!part.isEmpty()) {
                customPresets.add(PresetData.decode(part));
            }
        }
    }

    public static String encodeCustomPresets() {
        if (customPresets.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < customPresets.size(); i++) {
            if (i > 0) {
                builder.append(';');
            }
            builder.append(customPresets.get(i).encode());
        }
        return builder.toString();
    }

    public static void persist() {
        ConfigManager.fancyDmgSplashCustomPresets = encodeCustomPresets();
        ConfigManager.saveConfig();
    }

    public static int getBuiltinMask() {
        return ConfigManager.fancyDmgSplashBuiltinPresetMask;
    }

    public static int getCustomMask() {
        return ConfigManager.fancyDmgSplashCustomPresetMask;
    }

    public static int getCustomCount() {
        return customPresets.size();
    }

    public static int getCustomRowCount() {
        int slots = customPresets.size() + 1;
        int rows = (slots + CUSTOM_COLUMNS - 1) / CUSTOM_COLUMNS;
        return Math.min(CUSTOM_MAX_ROWS, Math.max(1, rows));
    }

    public static float presetSwatchSize(net.minecraft.client.gui.Font font) {
        return previewButtonHeight(font);
    }

    public static float presetRowStride(float subOptionHeight, net.minecraft.client.gui.Font font) {
        return presetSwatchSize(font) + PRESET_EDIT_FRAME_PAD * 2f + 4f;
    }

    public static float getPresetPanelHeight(float subOptionHeight, net.minecraft.client.gui.Font font) {
        return computeRenderedPresetPanelHeight(subOptionHeight, font);
    }

    public static float computeRenderedPresetPanelHeight(float subOptionHeight, net.minecraft.client.gui.Font font) {
        float boxSize = presetSwatchSize(font);
        float rowStride = presetRowStride(subOptionHeight, font);
        int customRows = getCustomRowCount();
        float firstRowY = subOptionHeight;
        float boxY = firstRowY + customRows * rowStride + (rowStride - boxSize) * 0.5f;
        return boxY + boxSize + PRESET_EDIT_FRAME_PAD + 4f;
    }

    public static float estimatePresetPanelHeight(float subOptionHeight) {
        float boxSize = 22f;
        float rowStride = boxSize + PRESET_EDIT_FRAME_PAD * 2f + 4f;
        int customRows = getCustomRowCount();
        float firstRowY = subOptionHeight;
        float boxY = firstRowY + customRows * rowStride + (rowStride - boxSize) * 0.5f;
        return boxY + boxSize + PRESET_EDIT_FRAME_PAD + 4f;
    }

    public static boolean canDeleteCurrentEditingPreset() {
        return previewingBuiltinIndex < 0 && !customPresets.isEmpty();
    }

    public static PresetData getCustomPreset(int index) {
        if (index < 0 || index >= customPresets.size()) {
            return PresetData.vanillaDefault();
        }
        return customPresets.get(index);
    }

    public static boolean isBuiltinSelected(int index) {
        return index >= 0 && index < BUILTIN_COUNT && (getBuiltinMask() & (1 << index)) != 0;
    }

    public static boolean isCustomSelected(int index) {
        return index >= 0 && index < customPresets.size() && (getCustomMask() & (1 << index)) != 0;
    }

    public static boolean isEditingBuiltin(int index) {
        return index >= 0 && index < BUILTIN_COUNT && previewingBuiltinIndex == index;
    }

    public static boolean isEditingCustom(int index) {
        return previewingBuiltinIndex < 0 && ConfigManager.fancyDmgSplashEditingCustomIndex == index;
    }

    public static int getDeleteArmedCustomIndex() {
        return deleteArmedCustomIndex;
    }

    public static void clearDeleteArmed() {
        deleteArmedCustomIndex = -1;
    }

    public static void toggleBuiltin(int index) {
        if (index < 0 || index >= BUILTIN_COUNT) {
            return;
        }
        int mask = getBuiltinMask();
        int bit = 1 << index;
        if ((mask & bit) != 0) {
            if (Integer.bitCount(mask) + Integer.bitCount(getCustomMask()) <= 1) {
                return;
            }
            mask &= ~bit;
            ConfigManager.fancyDmgSplashBuiltinPresetMask = mask;
            rebuildCycleEntries();
            syncPaletteValue();
            persist();
            return;
        }
        mask |= bit;
        ConfigManager.fancyDmgSplashBuiltinPresetMask = mask;
        selectEditingBuiltin(index, true);
    }

    public static void toggleCustom(int index) {
        if (index < 0 || index >= customPresets.size()) {
            return;
        }
        int mask = getCustomMask();
        int bit = 1 << index;
        if ((mask & bit) != 0) {
            if (Integer.bitCount(mask) + Integer.bitCount(getBuiltinMask()) <= 1) {
                return;
            }
            mask &= ~bit;
            ConfigManager.fancyDmgSplashCustomPresetMask = mask;
            rebuildCycleEntries();
            syncPaletteValue();
            persist();
            return;
        }
        mask |= bit;
        ConfigManager.fancyDmgSplashCustomPresetMask = mask;
        selectEditingCustom(index, true);
    }

    public static void selectEditingBuiltin(int index) {
        selectEditingBuiltin(index, true);
    }

    public static void selectEditingCustom(int index) {
        selectEditingCustom(index, true);
    }

    private static void selectEditingBuiltin(int index, boolean persistChanges) {
        if (index < 0 || index >= BUILTIN_COUNT) {
            return;
        }
        if (persistChanges) {
            saveEditorToCurrentTarget();
        }
        previewingBuiltinIndex = index;
        deleteArmedCustomIndex = -1;
        rebuildCycleEntries();
        editingCycleIndex = findCycleIndexForBuiltin(index);
        applyPresetToEditor(PresetData.builtin(index));
        syncPaletteValue();
        if (persistChanges) {
            persist();
        }
    }

    private static void selectEditingCustom(int index, boolean persistChanges) {
        if (index < 0 || index >= customPresets.size()) {
            return;
        }
        if (persistChanges) {
            saveEditorToCurrentTarget();
        }
        previewingBuiltinIndex = -1;
        ConfigManager.fancyDmgSplashEditingCustomIndex = index;
        deleteArmedCustomIndex = -1;
        rebuildCycleEntries();
        editingCycleIndex = findCycleIndexForCustom(index);
        loadEditingCustomIntoEditor(index);
        syncPaletteValue();
        if (persistChanges) {
            persist();
        }
    }

    public static void addCustomPreset() {
        if (customPresets.size() >= CUSTOM_MAX) {
            customPresets.remove(0);
            ConfigManager.fancyDmgSplashCustomPresetMask = shiftMaskRight(getCustomMask());
            int editing = ConfigManager.fancyDmgSplashEditingCustomIndex;
            if (editing > 0) {
                ConfigManager.fancyDmgSplashEditingCustomIndex = editing - 1;
            }
            if (deleteArmedCustomIndex == 0) {
                deleteArmedCustomIndex = -1;
            } else if (deleteArmedCustomIndex > 0) {
                deleteArmedCustomIndex--;
            }
        }
        int newIndex = customPresets.size();
        customPresets.add(PresetData.vanillaDefault());
        ConfigManager.fancyDmgSplashCustomPresetMask |= (1 << newIndex);
        ConfigManager.fancyDmgSplashEditingCustomIndex = newIndex;
        previewingBuiltinIndex = -1;
        deleteArmedCustomIndex = -1;
        rebuildCycleEntries();
        editingCycleIndex = findCycleIndexForCustom(newIndex);
        loadEditingCustomIntoEditor(newIndex);
        syncPaletteValue();
        persist();
    }

    public static void cycleEditingPreset() {
        saveEditorToCurrentTarget();
        if (cycleEntries.isEmpty()) {
            rebuildCycleEntries();
        }
        if (cycleEntries.isEmpty()) {
            return;
        }
        editingCycleIndex = (editingCycleIndex + 1) % cycleEntries.size();
        applyCycleEntry(cycleEntries.get(editingCycleIndex));
        deleteArmedCustomIndex = -1;
    }

    public static void armDeleteCurrentCustom() {
        if (!canDeleteCurrentEditingPreset()) {
            return;
        }
        int index = ConfigManager.fancyDmgSplashEditingCustomIndex;
        if (deleteArmedCustomIndex == index) {
            deleteCustomPreset(index);
            return;
        }
        deleteArmedCustomIndex = index;
    }

    public static void deleteCustomPreset(int index) {
        if (index < 0 || index >= customPresets.size()) {
            deleteArmedCustomIndex = -1;
            return;
        }
        saveEditorToCurrentTarget();
        customPresets.remove(index);
        ConfigManager.fancyDmgSplashCustomPresetMask = removeMaskBit(getCustomMask(), index);
        deleteArmedCustomIndex = -1;
        if (customPresets.isEmpty()) {
            ConfigManager.fancyDmgSplashCustomPresetMask = 0;
            ensureAtLeastOneSelectedForUse();
            selectEditingBuiltin(BUILTIN_COUNT - 1, false);
            syncPaletteValue();
            persist();
            return;
        }
        int newEditing = Math.min(index, customPresets.size() - 1);
        if ((getCustomMask() & (1 << newEditing)) == 0) {
            int first = firstSetBit(getCustomMask());
            newEditing = first >= 0 ? first : 0;
            ConfigManager.fancyDmgSplashCustomPresetMask |= (1 << newEditing);
        }
        selectEditingCustom(newEditing, false);
        syncPaletteValue();
        persist();
    }

    public static void handleAppearanceEdit() {
        if (loadingPreset) {
            return;
        }
        if (previewingBuiltinIndex >= 0) {
            int builtin = previewingBuiltinIndex;
            int mask = getBuiltinMask();
            if ((mask & (1 << builtin)) != 0) {
                if (Integer.bitCount(mask) + Integer.bitCount(getCustomMask()) <= 1) {
                    mask |= (1 << builtin);
                } else {
                    mask &= ~(1 << builtin);
                }
                ConfigManager.fancyDmgSplashBuiltinPresetMask = mask;
            }
            forkEditorStateToNewCustom();
            return;
        }
        saveEditorToCurrentTarget();
        persist();
    }

    public static PresetData pickRandomForDamage() {
        ensureInitialized();
        List<WeightedPick> picks = buildWeightedPicks();
        if (picks.isEmpty()) {
            return readLiveEditorPreset();
        }
        int total = 0;
        for (WeightedPick pick : picks) {
            total += pick.weight;
        }
        int roll = random.nextInt(total);
        int cumulative = 0;
        for (WeightedPick pick : picks) {
            cumulative += pick.weight;
            if (roll < cumulative) {
                return pick.data;
            }
        }
        return picks.getLast().data;
    }

    public static boolean allSelectedPresetCompact() {
        ensureInitialized();
        List<WeightedPick> picks = buildWeightedPicks();
        if (picks.isEmpty()) {
            return false;
        }
        for (WeightedPick pick : picks) {
            if (!pick.data.compact) {
                return false;
            }
        }
        return true;
    }

    public static int matchingBuiltinAppearance(PresetData data) {
        if (data == null) {
            return -1;
        }
        for (int i = 0; i < BUILTIN_COUNT; i++) {
            PresetData builtin = PresetData.builtin(i);
            if ((data.gradientStart & 0xFFFFFF) == (builtin.gradientStart & 0xFFFFFF)
                    && (data.gradientEnd & 0xFFFFFF) == (builtin.gradientEnd & 0xFFFFFF)
                    && data.symbols.equals(builtin.symbols)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isReactionEligible(PresetData data) {
        return matchingBuiltinAppearance(data) >= 0;
    }

    public static int resolveReactionElementColor(PresetData data) {
        int index = matchingBuiltinAppearance(data);
        if (index >= 0) {
            return com.shyeuar.baity.gui.value.ColorPaletteValue.PRESET_COLORS[index] & 0xFFFFFF;
        }
        return data.primaryColor();
    }

    public static boolean isActiveReactionColor(int color) {
        ensureInitialized();
        int normalized = color & 0xFFFFFF;
        for (WeightedPick pick : buildWeightedPicks()) {
            if (!isReactionEligible(pick.data)) {
                continue;
            }
            if ((resolveReactionElementColor(pick.data) & 0xFFFFFF) == normalized) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBuiltinColorForReaction(int color) {
        return isActiveReactionColor(color);
    }

    public static Component buildPreviewForEditor(FancyDmgSplashColorEditorValue editor) {
        return FancyDmgSplashSettings.formatPreviewWithPreset(captureEditorPreset(editor));
    }

    public static int previewButtonHeight(net.minecraft.client.gui.Font font) {
        return font.lineHeight + 8;
    }

    public static void saveEditorToCurrentTarget() {
        if (previewingBuiltinIndex >= 0) {
            return;
        }
        FancyDmgSplashColorEditorValue editor = findColorEditor();
        if (editor == null) {
            return;
        }
        int index = ConfigManager.fancyDmgSplashEditingCustomIndex;
        if (index < 0 || index >= customPresets.size()) {
            return;
        }
        editor.persistToConfig();
        customPresets.set(index, captureEditorPreset(editor));
    }

    private static void forkEditorStateToNewCustom() {
        FancyDmgSplashColorEditorValue editor = findColorEditor();
        if (editor == null) {
            return;
        }
        PresetData data = captureEditorPreset(editor);
        previewingBuiltinIndex = -1;
        if (customPresets.size() >= CUSTOM_MAX) {
            customPresets.remove(0);
            ConfigManager.fancyDmgSplashCustomPresetMask = shiftMaskRight(getCustomMask());
        }
        int newIndex = customPresets.size();
        customPresets.add(data);
        ConfigManager.fancyDmgSplashCustomPresetMask |= (1 << newIndex);
        ConfigManager.fancyDmgSplashEditingCustomIndex = newIndex;
        rebuildCycleEntries();
        editingCycleIndex = findCycleIndexForCustom(newIndex);
        applyPresetToEditor(data);
        syncPaletteValue();
        persist();
    }

    public static void loadEditingCustomIntoEditor(int index) {
        if (index < 0 || index >= customPresets.size()) {
            index = 0;
        }
        previewingBuiltinIndex = -1;
        ConfigManager.fancyDmgSplashEditingCustomIndex = index;
        applyPresetToEditor(customPresets.get(index));
    }

    private static void applyCycleEntry(PresetCycleEntry entry) {
        if (entry.builtin) {
            previewingBuiltinIndex = entry.index;
            applyPresetToEditor(PresetData.builtin(entry.index));
            return;
        }
        previewingBuiltinIndex = -1;
        ConfigManager.fancyDmgSplashEditingCustomIndex = entry.index;
        applyPresetToEditor(customPresets.get(entry.index));
    }

    private static void applyPresetToEditor(PresetData data) {
        loadingPreset = true;
        try {
            ConfigManager.fancyDmgSplashCritGradientStart = data.gradientStart;
            ConfigManager.fancyDmgSplashCritGradientEnd = data.gradientEnd;
            ConfigManager.fancyDmgSplashDamageSymbols = data.symbols;
            ConfigManager.fancyDmgSplashBold = data.bold;
            ConfigManager.fancyDmgSplashCompactDamageNumber = data.compact;

            Module module = ModuleManager.getModuleByName("FancyDmgSplash");
            if (module == null) {
                return;
            }
            for (Value value : module.getValues()) {
                switch (value.getName()) {
                    case "color editor" -> {
                        if (value instanceof FancyDmgSplashColorEditorValue editor) {
                            editor.loadFromConfig();
                        }
                    }
                    case "bold" -> value.setValue(data.bold);
                    case "compact damage number" -> value.setValue(data.compact);
                    default -> {
                    }
                }
            }
        } finally {
            loadingPreset = false;
        }
    }

    private static PresetData captureEditorPreset(FancyDmgSplashColorEditorValue editor) {
        editor.persistToConfig();
        return new PresetData(
                editor.gradient().getStartColor(),
                editor.gradient().getEndColor(),
                editor.getSymbols(),
                ModuleUtils.getOptionBoolean(ModuleManager.getModuleByName("FancyDmgSplash"), "bold", ConfigManager.fancyDmgSplashBold),
                ModuleUtils.getOptionBoolean(ModuleManager.getModuleByName("FancyDmgSplash"), "compact damage number", ConfigManager.fancyDmgSplashCompactDamageNumber)
        );
    }

    private static PresetData readLiveEditorPreset() {
        FancyDmgSplashColorEditorValue editor = findColorEditor();
        if (editor != null) {
            return captureEditorPreset(editor);
        }
        return PresetData.vanillaDefault();
    }

    private static void rebuildCycleEntries() {
        cycleEntries.clear();
        for (int i = 0; i < BUILTIN_COUNT; i++) {
            if (isBuiltinSelected(i)) {
                cycleEntries.add(new PresetCycleEntry(true, i));
            }
        }
        for (int i = 0; i < customPresets.size(); i++) {
            if (isCustomSelected(i)) {
                cycleEntries.add(new PresetCycleEntry(false, i));
            }
        }
        if (cycleEntries.isEmpty() && !customPresets.isEmpty()) {
            cycleEntries.add(new PresetCycleEntry(false, 0));
            ConfigManager.fancyDmgSplashCustomPresetMask = 1;
        }
        if (cycleEntries.isEmpty()) {
            int lastBuiltin = BUILTIN_COUNT - 1;
            ConfigManager.fancyDmgSplashBuiltinPresetMask |= 1 << lastBuiltin;
            cycleEntries.add(new PresetCycleEntry(true, lastBuiltin));
        }
        editingCycleIndex = Math.min(editingCycleIndex, Math.max(0, cycleEntries.size() - 1));
    }

    private static int findCycleIndexForBuiltin(int builtinIndex) {
        for (int i = 0; i < cycleEntries.size(); i++) {
            PresetCycleEntry entry = cycleEntries.get(i);
            if (entry.builtin && entry.index == builtinIndex) {
                return i;
            }
        }
        return 0;
    }

    private static int findCycleIndexForCustom(int customIndex) {
        for (int i = 0; i < cycleEntries.size(); i++) {
            PresetCycleEntry entry = cycleEntries.get(i);
            if (!entry.builtin && entry.index == customIndex) {
                return i;
            }
        }
        return 0;
    }

    private static List<WeightedPick> buildWeightedPicks() {
        List<WeightedPick> picks = new ArrayList<>();
        for (int i = 0; i < BUILTIN_COUNT; i++) {
            if (!isBuiltinSelected(i)) {
                continue;
            }
            int weight = i == 7 ? PHYSICAL_WEIGHT : BUILTIN_WEIGHT;
            picks.add(new WeightedPick(PresetData.builtin(i), weight));
        }
        for (int i = 0; i < customPresets.size(); i++) {
            if (!isCustomSelected(i)) {
                continue;
            }
            picks.add(new WeightedPick(customPresets.get(i), CUSTOM_WEIGHT));
        }
        return picks;
    }

    private static int shiftMaskRight(int mask) {
        int result = 0;
        int shift = 0;
        for (int i = 0; i < CUSTOM_MAX; i++) {
            if ((mask & (1 << i)) != 0) {
                result |= (1 << shift);
                shift++;
            }
        }
        return result;
    }

    private static int removeMaskBit(int mask, int removedIndex) {
        int result = 0;
        int shift = 0;
        for (int i = 0; i < CUSTOM_MAX; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            if (i == removedIndex) {
                continue;
            }
            result |= (1 << shift);
            shift++;
        }
        return result;
    }

    private static int firstSetBit(int mask) {
        if (mask == 0) {
            return -1;
        }
        return Integer.numberOfTrailingZeros(mask);
    }

    private static void syncPaletteValue() {
        Module module = ModuleManager.getModuleByName("FancyDmgSplash");
        if (module == null) {
            return;
        }
        if (ConfigSynchronizer.hasValueConfig(module.getName(), "preset")) {
            ConfigSynchronizer.handleValueUpdate(module.getName(), "preset", packPaletteConfig());
        }
    }

    public static long packPaletteConfig() {
        return ((long) getBuiltinMask() << 32) | (getCustomMask() & 0xFFFFFFFFL);
    }

    public static void unpackPaletteConfig(long packed) {
        ConfigManager.fancyDmgSplashBuiltinPresetMask = (int) (packed >>> 32);
        ConfigManager.fancyDmgSplashCustomPresetMask = (int) packed;
    }

    private static FancyDmgSplashColorEditorValue findColorEditor() {
        Module module = ModuleManager.getModuleByName("FancyDmgSplash");
        if (module == null) {
            return null;
        }
        Value value = ValueTreeUtils.findByName(module, "color editor");
        return value instanceof FancyDmgSplashColorEditorValue editor ? editor : null;
    }

    private static void migrateLegacyBuiltinMask() {
        int mask = getBuiltinMask();
        if (mask > 0 && mask < BUILTIN_COUNT) {
            ConfigManager.fancyDmgSplashBuiltinPresetMask = 1 << mask;
        }
    }

    private record PresetCycleEntry(boolean builtin, int index) {
    }

    private record WeightedPick(PresetData data, int weight) {
    }

    public static final class PresetData {
        public final int gradientStart;
        public final int gradientEnd;
        public final String symbols;
        public final boolean bold;
        public final boolean compact;

        public PresetData(int gradientStart, int gradientEnd, String symbols, boolean bold, boolean compact) {
            this.gradientStart = gradientStart & 0xFFFFFF;
            this.gradientEnd = gradientEnd & 0xFFFFFF;
            this.symbols = FancyDmgSplashSettings.clampSymbols(symbols);
            this.bold = bold;
            this.compact = compact;
        }

        public static PresetData vanillaDefault() {
            return new PresetData(
                    FancyDmgSplashSettings.DEFAULT_GRADIENT_START,
                    FancyDmgSplashSettings.DEFAULT_GRADIENT_END,
                    FancyDmgSplashSettings.DEFAULT_SYMBOL,
                    false,
                    true
            );
        }

        public static PresetData builtin(int index) {
            int color = com.shyeuar.baity.gui.value.ColorPaletteValue.PRESET_COLORS[index] & 0xFFFFFF;
            return new PresetData(
                    color,
                    color,
                    FancyDmgSplashSettings.PRESET_SYMBOLS[index],
                    false,
                    false
            );
        }

        public int primaryColor() {
            return gradientStart;
        }

        public String encode() {
            return String.format("%06X,%06X,%s,%s,%s",
                    gradientStart & 0xFFFFFF,
                    gradientEnd & 0xFFFFFF,
                    escape(symbols),
                    bold ? "1" : "0",
                    compact ? "1" : "0");
        }

        public static PresetData decode(String raw) {
            if (raw == null || raw.isEmpty()) {
                return vanillaDefault();
            }
            String[] parts = raw.split(",", 6);
            if (parts.length < 5) {
                return vanillaDefault();
            }
            try {
                int start = Integer.parseInt(parts[0], 16);
                int end = Integer.parseInt(parts[1], 16);
                return new PresetData(
                        start,
                        end,
                        unescape(parts[2]),
                        "1".equals(parts[3]),
                        "1".equals(parts[4])
                );
            } catch (NumberFormatException ignored) {
                return vanillaDefault();
            }
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace(",", "\\,");
        }

        private static String unescape(String value) {
            StringBuilder builder = new StringBuilder();
            boolean escape = false;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (escape) {
                    builder.append(c);
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else {
                    builder.append(c);
                }
            }
            return builder.toString();
        }
    }
}
