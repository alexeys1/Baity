package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SliderValue implements Value {
    
    private final String name;
    private final String displayName;
    private double value;
    private final double defaultValue;
    private final double minValue;
    private final double maxValue;
    private final double step;
    private final ModuleCategory category;
    private final int decimalPlaces;
    
    public SliderValue(String name, String displayName, double defaultValue, double minValue, double maxValue, double step, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.category = category;
        this.decimalPlaces = calculateDecimalPlaces(step);
    }
    
    public SliderValue(String name, String displayName, double defaultValue, double minValue, double maxValue, ModuleCategory category) {
        this(name, displayName, defaultValue, minValue, maxValue, 1.0, category);
    }
    
    private int calculateDecimalPlaces(double step) {
        String stepStr = String.valueOf(step);
        int dotIndex = stepStr.indexOf('.');
        if (dotIndex < 0) return 0;
        return Math.min(stepStr.length() - dotIndex - 1, 2);
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDisplayName() { return displayName; }
    
    @Override
    public Object getValue() { return value; }
    
    @Override
    public void setValue(Object value) {
        if (value instanceof Number) {
            double newValue = ((Number) value).doubleValue();
            newValue = Math.max(minValue, Math.min(maxValue, newValue));
            newValue = Math.round(newValue / step) * step;
            this.value = newValue;
        }
    }
    
    public boolean trySetValue(double newValue) {
        if (newValue < minValue || newValue > maxValue) {
            return false;
        }
        setValue(newValue);
        return true;
    }
    
    @Override
    public ModuleCategory getCategory() { return category; }
    
    @Override
    public ValueStyle getStyle() {
        return ValueStyle.SLIDER;
    }
    
    public double getDoubleValue() { return value; }
    
    public double getDefaultValue() { return defaultValue; }
    
    public double getMinValue() { return minValue; }
    
    public double getMaxValue() { return maxValue; }
    
    public double getStep() { return step; }
    
    public int getDecimalPlaces() { return decimalPlaces; }
    
    public double getPercentage() {
        return (value - minValue) / (maxValue - minValue);
    }
    
    public void setFromPercentage(double percentage) {
        percentage = Math.max(0, Math.min(1, percentage));
        double newValue = minValue + percentage * (maxValue - minValue);
        setValue(newValue);
    }
    
    public void resetToDefault() {
        this.value = defaultValue;
    }
    
    public String getFormattedValue() {
        if (decimalPlaces == 0) {
            return String.valueOf((int) value);
        }
        return String.format("%." + decimalPlaces + "f", value);
    }
}
