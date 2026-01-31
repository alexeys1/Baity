package com.shyeuar.baity.gui.tooltip;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;

public class TooltipManager {
    private static final Map<String, TooltipInfo> tooltipMap = new HashMap<>();
    
    public static void registerTooltip(String name, String text, int color) {
        tooltipMap.put(name, new TooltipInfo(text, color));
    }
    
    public static void registerTooltip(String name, Component coloredText) {
        tooltipMap.put(name, new TooltipInfo(coloredText));
    }
    
    public static String getTooltipText(String name) {
        TooltipInfo info = tooltipMap.get(name);
        return info != null ? info.getText() : null;
    }
    
    public static Component getTooltipTextWithColors(String name) {
        TooltipInfo info = tooltipMap.get(name);
        return info != null ? info.getColoredText() : null;
    }
    
    public static boolean hasTooltip(String name) {
        return tooltipMap.containsKey(name);
    }
    
    private static class TooltipInfo {
        private final String text;
        private final Component coloredText;
        
        TooltipInfo(String text, int color) {
            this.text = text;
            this.coloredText = Component.literal(text).withStyle(style -> style.withColor(color));
        }
        
        TooltipInfo(Component coloredText) {
            this.text = coloredText.getString();
            this.coloredText = coloredText;
        }
        
        String getText() {
            return text;
        }
        
        Component getColoredText() {
            return coloredText;
        }
    }
}
