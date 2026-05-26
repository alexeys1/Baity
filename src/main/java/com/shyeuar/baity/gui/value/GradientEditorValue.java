package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class GradientEditorValue implements Value {
    public static final float MAP_FIXED_VALUE = 1.0f;
    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    private int startColor;
    private int endColor;
    private final int defaultStartColor;
    private final int defaultEndColor;
    private int selectedPoint;
    private float startHue;
    private float startSat;
    private float startVal;
    private float endHue;
    private float endSat;
    private float endVal;

    public GradientEditorValue(String name, String displayName, ModuleCategory category, int startColor, int endColor) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
        this.startColor = startColor & 0xFFFFFF;
        this.endColor = endColor & 0xFFFFFF;
        this.defaultStartColor = this.startColor;
        this.defaultEndColor = this.endColor;
        updateHsvFromColors();
    }

    public void resetToDefault() {
        startColor = defaultStartColor;
        endColor = defaultEndColor;
        updateHsvFromColors();
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
        return String.format("#%06X,#%06X", startColor & 0xFFFFFF, endColor & 0xFFFFFF);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof String raw) {
            String[] parts = raw.split(",", 2);
            if (parts.length == 2) {
                Integer s = parseHex(parts[0]);
                Integer e = parseHex(parts[1]);
                if (s != null && e != null) {
                    startColor = s;
                    endColor = e;
                    updateHsvFromColors();
                }
            }
        }
    }

    @Override
    public ModuleCategory getCategory() {
        return category;
    }

    @Override
    public ValueStyle getStyle() {
        return ValueStyle.GRADIENT_EDITOR;
    }

    public int getStartColor() {
        return startColor;
    }

    public int getEndColor() {
        return endColor;
    }

    public int getSelectedPoint() {
        return selectedPoint;
    }

    public void selectPoint(int point) {
        if (point == 0 || point == 1) {
            selectedPoint = point;
        }
    }

    public void syncColors() {
        if (selectedPoint == 0) {
            startColor = endColor;
        } else {
            endColor = startColor;
        }
        updateHsvFromColors();
    }

    public void setSelectedFromHueSat(float hue, float sat) {
        hue = clamp01(hue);
        sat = clamp01(sat);
        if (selectedPoint == 0) {
            startHue = hue;
            startSat = sat;
            startColor = hsvToRgb(startHue, startSat, startVal);
        } else {
            endHue = hue;
            endSat = sat;
            endColor = hsvToRgb(endHue, endSat, endVal);
        }
    }

    public void setSelectedValue(float val) {
        val = clamp01(val);
        if (selectedPoint == 0) {
            startVal = val;
            startColor = hsvToRgb(startHue, startSat, startVal);
        } else {
            endVal = val;
            endColor = hsvToRgb(endHue, endSat, endVal);
        }
    }

    public float getSelectedHue() {
        return selectedPoint == 0 ? startHue : endHue;
    }

    public float getSelectedSat() {
        return selectedPoint == 0 ? startSat : endSat;
    }

    public float getSelectedVal() {
        return selectedPoint == 0 ? startVal : endVal;
    }

    public String getStartHex() {
        return String.format("#%06X", startColor & 0xFFFFFF);
    }

    public String getEndHex() {
        return String.format("#%06X", endColor & 0xFFFFFF);
    }

    public String getSelectedHex() {
        return selectedPoint == 0 ? getStartHex() : getEndHex();
    }

    public void applyHexToSelected(String hex) {
        Integer parsed = parseHex(hex);
        if (parsed == null) {
            return;
        }
        if (selectedPoint == 0) {
            startColor = parsed;
        } else {
            endColor = parsed;
        }
        updateHsvFromColors();
    }

    private void updateHsvFromColors() {
        float[] start = java.awt.Color.RGBtoHSB((startColor >> 16) & 0xFF, (startColor >> 8) & 0xFF, startColor & 0xFF, null);
        float[] end = java.awt.Color.RGBtoHSB((endColor >> 16) & 0xFF, (endColor >> 8) & 0xFF, endColor & 0xFF, null);
        startHue = start[0];
        startSat = start[1];
        startVal = start[2];
        endHue = end[0];
        endSat = end[1];
        endVal = end[2];
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static int hsvToRgb(float h, float s, float v) {
        return java.awt.Color.HSBtoRGB(clamp01(h), clamp01(s), clamp01(v)) & 0xFFFFFF;
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
}
