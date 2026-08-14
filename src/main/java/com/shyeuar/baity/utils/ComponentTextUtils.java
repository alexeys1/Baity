package com.shyeuar.baity.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.StringUtil;

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

    public static String legacyFormatted(Component component) {
        return trimLegacyResets(getFormattedText(component, false));
    }

    public static String getFormattedText(Component component) {
        return getFormattedText(component, false);
    }

    public static String getFormattedText(Component component, boolean sideBar) {
        if (component == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(128);
        boolean[] firstChunk = { true };
        component.visit((style, string) -> {
            if (string == null || string.isEmpty()) {
                return java.util.Optional.empty();
            }
            String codes = styleToSectionCodes(style);
            if (firstChunk[0] && sideBar && "\u00A7f".equals(codes)) {
                codes = "";
            }
            firstChunk[0] = false;
            out.append(codes).append(string);
            if (!sideBar) {
                out.append(ChatFormatting.RESET);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    public static String stripLegacyResets(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        return text.replace("\u00A7r", "");
    }

    private static String trimLegacyResets(String text) {
        String s = text;
        while (s.endsWith("\u00A7r")) {
            s = s.substring(0, s.length() - 2);
        }
        while (s.startsWith("\u00A7r")) {
            s = s.substring(2);
        }
        return s;
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
        return trimLegacyResets(sb.toString());
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
            TextColor legacyColor = TextColor.fromLegacyFormat(cf);
            if (legacyColor != null && legacyColor.getValue() == v) {
                return cf;
            }
        }
        return null;
    }

    public static Component replaceComponent(Component original, String target, String replacement) {
        if (original.getContents() instanceof PlainTextContents originalContents) {
            String contentsText = originalContents.text();

            if (!StringUtil.isNullOrEmpty(contentsText) && contentsText.contains(target)) {
                return copyWithReplacedText(original, contentsText.replace(target, replacement));
            }
        }

        List<Component> originalSiblings = original.getSiblings();
        for (int i = 0; i < originalSiblings.size(); i++) {
            Component sibling = originalSiblings.get(i);

            if (sibling.getContents() instanceof PlainTextContents siblingContents) {
                String contentsText = siblingContents.text();

                if (!StringUtil.isNullOrEmpty(contentsText) && contentsText.contains(target)) {
                    MutableComponent newComponent = original.copy();
                    newComponent.getSiblings().set(
                            i, copyWithReplacedText(sibling, contentsText.replace(target, replacement))
                    );
                    return newComponent;
                }
            }

            if (!sibling.getSiblings().isEmpty()) {
                Component replaced = replaceComponent(sibling, target, replacement);

                if (replaced != sibling) {
                    MutableComponent newComponent = original.copy();
                    newComponent.getSiblings().set(i, replaced);
                    return newComponent;
                }
            }
        }

        return original;
    }

    private static MutableComponent copyWithReplacedText(Component source, String replacedText) {
        MutableComponent newComponent = MutableComponent.create(
                PlainTextContents.create(replacedText)
        ).withStyle(source.getStyle());
        source.getSiblings().forEach(newComponent::append);
        return newComponent;
    }
}
