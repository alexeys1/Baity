package com.shyeuar.baity.gui.value;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashPresetStore;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class FancyDmgSplashColorEditorValue implements Value {
    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final GradientEditorValue gradient;
    private String symbols;

    public FancyDmgSplashColorEditorValue(String name, String displayName, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.gradient = new GradientEditorValue(
                name,
                displayName,
                category,
                FancyDmgSplashSettings.DEFAULT_GRADIENT_START,
                FancyDmgSplashSettings.DEFAULT_GRADIENT_END
        );
        this.symbols = FancyDmgSplashSettings.DEFAULT_SYMBOL;
        loadFromConfig();
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
        persistToConfig();
        return FancyDmgSplashSettings.encodeColorEditor();
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String raw) {
            FancyDmgSplashSettings.decodeColorEditor(raw);
            loadFromConfig();
        }
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.FANCY_DMG_COLOR_EDITOR;
    }

    public GradientEditorValue gradient() {
        return gradient;
    }

    public String getSymbols() {
        return symbols == null ? "" : symbols;
    }

    public void setSymbols(String symbols) {
        this.symbols = FancyDmgSplashSettings.clampSymbols(symbols);
        ConfigManager.fancyDmgSplashDamageSymbols = getSymbols();
    }

    public void loadFromConfig() {
        gradient.setValue(String.format("#%06X,#%06X",
                ConfigManager.fancyDmgSplashCritGradientStart & 0xFFFFFF,
                ConfigManager.fancyDmgSplashCritGradientEnd & 0xFFFFFF));
        symbols = FancyDmgSplashSettings.clampSymbols(ConfigManager.fancyDmgSplashDamageSymbols);
        ConfigManager.fancyDmgSplashDamageSymbols = symbols;
    }

    public void persistToConfig() {
        ConfigManager.fancyDmgSplashCritGradientStart = gradient.getStartColor();
        ConfigManager.fancyDmgSplashCritGradientEnd = gradient.getEndColor();
        ConfigManager.fancyDmgSplashDamageSymbols = getSymbols();
    }

    public void resetToDefault() {
        FancyDmgSplashSettings.resetColorEditorDefaults();
        loadFromConfig();
    }

    public int getDeleteArmedCustomIndex() {
        return FancyDmgSplashPresetStore.getDeleteArmedCustomIndex();
    }

    public int getEditingCustomIndex() {
        return ConfigManager.fancyDmgSplashEditingCustomIndex;
    }

    public boolean isBold() {
        return ConfigManager.fancyDmgSplashBold;
    }

    public boolean isCompact() {
        return ConfigManager.fancyDmgSplashCompactDamageNumber;
    }

    public void toggleBold() {
        applyBold(!isBold());
    }

    public void toggleCompact() {
        applyCompact(!isCompact());
    }

    private void applyBold(boolean bold) {
        ConfigManager.fancyDmgSplashBold = bold;
        syncHiddenOption("bold", bold);
    }

    private void applyCompact(boolean compact) {
        ConfigManager.fancyDmgSplashCompactDamageNumber = compact;
        syncHiddenOption("compact damage number", compact);
    }

    private void syncHiddenOption(String optionName, boolean value) {
        com.shyeuar.baity.gui.module.Module module =
                com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("FancyDmgSplash");
        if (module == null) {
            FancyDmgSplashSettings.onAppearanceSettingChanged();
            return;
        }
        Value option = ValueTreeUtils.findByName(module, optionName);
        if (option != null) {
            option.setValue(value);
        }
        if (com.shyeuar.baity.gui.sync.ConfigSynchronizer.hasValueConfig(module.getName(), optionName)) {
            com.shyeuar.baity.gui.sync.ConfigSynchronizer.handleValueUpdate(module.getName(), optionName, value);
        } else {
            FancyDmgSplashSettings.onAppearanceSettingChanged();
        }
    }
}
