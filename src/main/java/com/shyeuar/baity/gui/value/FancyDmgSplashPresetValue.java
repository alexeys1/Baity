package com.shyeuar.baity.gui.value;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class FancyDmgSplashPresetValue implements Value {
    public static final int[] PRESET_COLORS = ColorPaletteValue.PRESET_COLORS;

    private final String name;
    private final String displayName;
    private final ModuleCategory category;

    public FancyDmgSplashPresetValue(String name, String displayName, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        FancyDmgSplashPresetStore.ensureInitialized();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Object getValue() {
        return FancyDmgSplashPresetStore.packPaletteConfig();
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Number number) {
            FancyDmgSplashPresetStore.unpackPaletteConfig(number.longValue());
        }
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.FANCY_DMG_PRESET;
    }

    public int getBuiltinCount() {
        return FancyDmgSplashPresetStore.BUILTIN_COUNT;
    }

    public int getCustomCount() {
        return FancyDmgSplashPresetStore.getCustomCount();
    }

    public int getCustomRowCount() {
        return FancyDmgSplashPresetStore.getCustomRowCount();
    }

    public boolean isBuiltinSelected(int index) {
        return FancyDmgSplashPresetStore.isBuiltinSelected(index);
    }

    public boolean isCustomSelected(int index) {
        return FancyDmgSplashPresetStore.isCustomSelected(index);
    }

    public int getCustomGradientStart(int index) {
        return FancyDmgSplashPresetStore.getCustomPreset(index).gradientStart;
    }

    public int getCustomGradientEnd(int index) {
        return FancyDmgSplashPresetStore.getCustomPreset(index).gradientEnd;
    }

    public int getBuiltinColor(int index) {
        if (index >= 0 && index < PRESET_COLORS.length) {
            return PRESET_COLORS[index];
        }
        return 0xFFFFFF;
    }

    public String getBuiltinTooltip(int index) {
        if (index >= 0 && index < FancyDmgSplashSettings.PRESET_TOOLTIPS.length) {
            return FancyDmgSplashSettings.PRESET_TOOLTIPS[index];
        }
        return null;
    }

    public int getDeleteArmedCustomIndex() {
        return FancyDmgSplashPresetStore.getDeleteArmedCustomIndex();
    }

    public int getEditingCustomIndex() {
        return ConfigManager.fancyDmgSplashEditingCustomIndex;
    }

    public boolean isEditingBuiltin(int index) {
        return FancyDmgSplashPresetStore.isEditingBuiltin(index);
    }

    public boolean isEditingCustom(int index) {
        return FancyDmgSplashPresetStore.isEditingCustom(index);
    }
}
