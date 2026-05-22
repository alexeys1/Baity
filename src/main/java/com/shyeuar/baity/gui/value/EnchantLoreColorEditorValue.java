package com.shyeuar.baity.gui.value;

import com.shyeuar.baity.features.enchantlore.EnchantLoreColorSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class EnchantLoreColorEditorValue implements Value {
    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private final GradientEditorValue gradient;
    private int editingTier;

    public EnchantLoreColorEditorValue(String name, String displayName, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        EnchantLoreColorSettings.initDefaults();
        this.gradient = new GradientEditorValue(
                name,
                displayName,
                category,
                EnchantLoreColorSettings.getStartColor(0),
                EnchantLoreColorSettings.getEndColor(0)
        );
        loadTier(0);
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
        saveTier(editingTier);
        return EnchantLoreColorSettings.encode();
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String raw) {
            EnchantLoreColorSettings.decode(raw);
            loadTier(editingTier);
        }
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.ENCHANT_LORE_COLOR_EDITOR;
    }

    public GradientEditorValue gradient() {
        return gradient;
    }

    public int getEditingTier() {
        return editingTier;
    }

    public void cycleEditingTier() {
        saveTier(editingTier);
        editingTier = (editingTier + 1) % EnchantLoreColorSettings.TIER_COUNT;
        loadTier(editingTier);
    }

    public boolean isBold() {
        return EnchantLoreColorSettings.isBold(editingTier);
    }

    public void toggleBold() {
        EnchantLoreColorSettings.setBold(editingTier, !isBold());
    }

    public boolean isRainbow() {
        return EnchantLoreColorSettings.isRainbow(editingTier);
    }

    public void toggleRainbow() {
        EnchantLoreColorSettings.setRainbow(editingTier, !isRainbow());
    }

    public void resetCurrentTier() {
        EnchantLoreColorSettings.resetTier(editingTier);
        loadTier(editingTier);
    }

    public void persistCurrentTier() {
        saveTier(editingTier);
    }

    private void loadTier(int tier) {
        gradient.setValue(String.format("#%06X,#%06X",
                EnchantLoreColorSettings.getStartColor(tier),
                EnchantLoreColorSettings.getEndColor(tier)));
    }

    private void saveTier(int tier) {
        EnchantLoreColorSettings.setStartColor(tier, gradient.getStartColor());
        EnchantLoreColorSettings.setEndColor(tier, gradient.getEndColor());
    }
}
