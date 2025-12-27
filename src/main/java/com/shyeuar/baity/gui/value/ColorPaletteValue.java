package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ColorPaletteValue implements Value {
    private final String name;
    private final String displayName;
    private final ModuleCategory category;
    
    public static final int[] PRESET_COLORS = {
        0x99FFFF, // 冰伤
        0x33CCFF, // 水伤
        0xE19BFF, // 雷伤
        0xFF9B00, // 火伤
        0xFFCC66, // 岩伤
        0x66FFCC, // 风伤
        0xBAFF37, // 草伤
        0xFFFFFF  // 物伤
    };
    
    public static final String[] COLOR_NAMES = {
        "冰伤", "水伤", "雷伤", "火伤", "岩伤", "风伤", "草伤", "物伤"
    };
    
    public static final int COLOR_REACTION_WET = 0x33CCFF;        // 潮湿
    public static final int COLOR_REACTION_FROZEN = 0x99FFFF;     // 冻结
    public static final int COLOR_REACTION_ELECTRO_CHARGED = 0xE19BFF; // 感电
    public static final int COLOR_REACTION_HEAL = 0xBCFF37;       // 治疗
    public static final int COLOR_REACTION_VAPORIZE = 0xFFCC66;   // 蒸发
    public static final int COLOR_REACTION_SUPERCONDUCT = 0xB4B4FF; // 超导
    public static final int COLOR_REACTION_OVERLOADED = 0xFF809B; // 超载
    public static final int COLOR_REACTION_BURNING = 0xFF9B00;    // 燃烧
    public static final int COLOR_REACTION_MELT = 0xFFCC66;       // 融化
    public static final int COLOR_REACTION_SHATTERED = 0xFFFFFF;  // 碎冰
    public static final int COLOR_REACTION_CRYSTALLIZE = 0xFFCC66; // 结晶
    public static final int COLOR_REACTION_IMMUNE = 0xA8A8A8;     // 免疫
    public static final int COLOR_REACTION_HEALING = 0xBCFF37;    // 治疗
    
    private int selectedMask = 0;
    
    private static final Random random = new Random();
    
    public ColorPaletteValue(String name, String displayName, ModuleCategory category) {
        this.name = name;
        this.displayName = displayName;
        this.category = category;
    }
    
    @Override
    public String getName() { return name; }
    
    @Override
    public String getDisplayName() { return displayName; }
    
    @Override
    public Object getValue() { return selectedMask; }
    
    @Override
    public void setValue(Object value) {
        if (value instanceof Number) {
            this.selectedMask = ((Number) value).intValue();
        }
    }
    
    @Override
    public ModuleCategory getCategory() { return category; }
    
    @Override
    public ValueStyle getStyle() {
        return ValueStyle.COLOR_PALETTE;
    }
    
    public void toggleColor(int index) {
        if (index >= 0 && index < PRESET_COLORS.length) {
            selectedMask ^= (1 << index);
        }
    }
    
    public boolean isColorSelected(int index) {
        if (index >= 0 && index < PRESET_COLORS.length) {
            return (selectedMask & (1 << index)) != 0;
        }
        return false;
    }
    
    public List<Integer> getSelectedColors() {
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (isColorSelected(i)) {
                colors.add(PRESET_COLORS[i]);
            }
        }
        return colors;
    }
    
    public int getRandomSelectedColor() {
        List<Integer> selected = getSelectedColors();
        if (selected.isEmpty()) {
            return 0xA8A8A8;
        }
        return selected.get(random.nextInt(selected.size()));
    }
    
    public int getColorCount() {
        return PRESET_COLORS.length;
    }
    
    public int getColor(int index) {
        if (index >= 0 && index < PRESET_COLORS.length) {
            return PRESET_COLORS[index];
        }
        return 0xFFFFFF;
    }
    
    public String getColorName(int index) {
        if (index >= 0 && index < COLOR_NAMES.length) {
            return COLOR_NAMES[index];
        }
        return "";
    }
}
