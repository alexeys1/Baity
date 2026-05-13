package com.shyeuar.baity.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;

public final class ComponentTextUtils {

    private ComponentTextUtils() {
    }

    public static String formattedLessResets(Component component) {
        if (component == null) {
            return "";
        }
        return buildLegacyFormatted(component, true, false);
    }

    private static String buildLegacyFormatted(Component component, boolean noExtraResets, boolean leadingWhite) {
        StringBuilder sb = new StringBuilder(50);
        boolean wasFormatted = false;
        for (Component part : flattenDepthFirst(component)) {
            String chatStyle = styleToSectionCodes(part.getStyle());
            if (!chatStyle.isEmpty()
                && (leadingWhite
                    || (wasFormatted && (sb.length() != 2 || sb.charAt(0) != '\u00A7' || sb.charAt(1) != 'r'))
                    || !"\u00A7f".equals(chatStyle))) {
                sb.append(chatStyle);
                wasFormatted = true;
            }
            sb.append(unstyledText(part));
            if (!noExtraResets) {
                sb.append("\u00A7r");
                wasFormatted = true;
            } else if (part == Component.empty()) {
                sb.append("\u00A7r");
                wasFormatted = true;
            }
        }
        String s = sb.toString();
        while (s.endsWith("\u00A7r")) {
            s = s.substring(0, s.length() - 2);
        }
        while (s.startsWith("\u00A7r")) {
            s = s.substring(2);
        }
        return s;
    }

    private static List<Component> flattenDepthFirst(Component root) {
        List<Component> out = new ArrayList<>();
        appendDepthFirst(root, out);
        return out;
    }

    private static void appendDepthFirst(Component node, List<Component> out) {
        out.add(node);
        for (Component sibling : node.getSiblings()) {
            appendDepthFirst(sibling, out);
        }
    }

    private static String unstyledText(Component c) {
        if (c.getContents() instanceof TranslatableContents) {
            return c.getString();
        }
        if (c.getContents() instanceof PlainTextContents ptc) {
            return ptc.text();
        }
        return "";
    }

    private static String styleToSectionCodes(Style style) {
        Style s = style == null ? Style.EMPTY : style;
        StringBuilder sb = new StringBuilder();
        TextColor color = s.getColor();
        if (color != null) {
            ChatFormatting cf = textColorToNamedFormat(color);
            if (cf != null) {
                sb.append(cf);
            } else {
                sb.append('<').append(Integer.toHexString(color.getValue())).append('>');
            }
        }
        if (s.isBold()) {
            sb.append(ChatFormatting.BOLD);
        }
        if (s.isItalic()) {
            sb.append(ChatFormatting.ITALIC);
        }
        if (s.isUnderlined()) {
            sb.append(ChatFormatting.UNDERLINE);
        }
        if (s.isStrikethrough()) {
            sb.append(ChatFormatting.STRIKETHROUGH);
        }
        if (s.isObfuscated()) {
            sb.append(ChatFormatting.OBFUSCATED);
        }
        return sb.toString();
    }

    private static ChatFormatting textColorToNamedFormat(TextColor color) {
        if (color == null) {
            return null;
        }
        int v = color.getValue();
        for (ChatFormatting cf : ChatFormatting.values()) {
            if (cf.getColor() != null && cf.getColor().intValue() == v) {
                return cf;
            }
        }
        return null;
    }
}