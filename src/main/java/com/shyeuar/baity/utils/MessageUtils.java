package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

@Environment(EnvType.CLIENT)
public class MessageUtils {
    
    public static MutableComponent createColoredText(String text, int color) {
        return Component.literal(text).withStyle(style -> style.withColor(color));
    }
    
    public static MutableComponent createStyledText(String text, int color, boolean bold, boolean italic) {
        return Component.literal(text).withStyle(style -> style.withColor(color).withBold(bold).withItalic(italic));
    }
    
    public static MutableComponent createTextWithEmoji(String prefix, String emoji, String suffix, int emojiColor) {
        MutableComponent prefixText = Component.literal(prefix);
        MutableComponent emojiText = Component.literal(emoji).withStyle(style -> style.withColor(emojiColor));
        MutableComponent suffixText = Component.literal(suffix);
        return prefixText.append(emojiText).append(suffixText);
    }
    
    public static MutableComponent createBaityPrefix() {
        int bracketColor = com.shyeuar.baity.gui.theme.LinearTheme.TEXT_PRIMARY.getRGB();
        MutableComponent leftBracket = createColoredText("[", bracketColor);
        
        int accentStart = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
        int accentEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_SECONDARY.getRGB();
        
        MutableComponent baityText = Component.empty();
        String baity = "baity";
        for (int i = 0; i < baity.length(); i++) {
            float progress = i / (float)(baity.length() - 1);
            int letterColor = interpolateColor(accentStart, accentEnd, progress);
            baityText.append(createColoredText(String.valueOf(baity.charAt(i)), letterColor));
        }
        
        MutableComponent rightBracket = createColoredText("] ", bracketColor);
        return leftBracket.append(baityText).append(rightBracket);
    }
    
    private static int interpolateColor(int startColor, int endColor, float progress) {
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;
        
        int endR = (endColor >> 16) & 0xFF;
        int endG = (endColor >> 8) & 0xFF;
        int endB = endColor & 0xFF;
        
        int r = (int)(startR + (endR - startR) * progress);
        int g = (int)(startG + (endG - startG) * progress);
        int b = (int)(startB + (endB - startB) * progress);
        
        return (r << 16) | (g << 8) | b;
    }
    
    public static MutableComponent createMessageWithPrefix(String message, int messageColor) {
        MutableComponent prefix = createBaityPrefix();
        MutableComponent messageText = createColoredText(message, messageColor);
        return prefix.append(messageText);
    }
    
    public static MutableComponent createMessageWithPrefix(MutableComponent message) {
        MutableComponent prefix = createBaityPrefix();
        return prefix.append(message);
    }
    
    public static void sendBaityMessage(String message) {
        if (Minecraft.getInstance().player != null) {
            MutableComponent prefix = createBaityPrefix();
            MutableComponent messageText = createColoredText(message, 0xFFFFFF);
            MutableComponent fullMessage = prefix.append(messageText);
            Minecraft.getInstance().gui.getChat().addMessage(fullMessage);
        }
    }
    
    public static void sendCustomMessage(MutableComponent message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().gui.getChat().addMessage(message);
        }
    }
}