package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class TextLineInputValue implements Value {
    private final String name;
    private final String displayName;
    private String value;
    private final ModuleCategory category;
    private boolean needsSeparator = false;

    public TextLineInputValue(String name, String displayName, String defaultValue, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.value = defaultValue == null ? "" : defaultValue;
        this.category = category;
    }

    public TextLineInputValue setNeedsSeparator(boolean needsSeparator) {
        this.needsSeparator = needsSeparator;
        return this;
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
        return value;
    }

    @Override
    public void setValue(Object value) {
        this.value = value == null ? "" : String.valueOf(value);
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.TEXT_LINE_INPUT;
    }

    @Override
    public boolean needsSeparatorBefore(Value previousValue) {
        return needsSeparator;
    }
}

