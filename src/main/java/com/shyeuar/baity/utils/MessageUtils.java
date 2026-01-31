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
        MutableComponent leftBracket = createColoredText("[",0xAAAAAA);
        MutableComponent baityText = createColoredText("baity", 0xC000C0);
        MutableComponent rightBracket = createColoredText("] ", 0xAAAAAA);
        return leftBracket.append(baityText).append(rightBracket);
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