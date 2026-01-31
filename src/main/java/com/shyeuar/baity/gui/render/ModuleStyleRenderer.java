package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ModuleStyleRenderer {
   
   public static void renderModule(GuiGraphics context, Minecraft client, Module module, Theme theme,
                                  float x1, float y, float x2, float moduleHeight,
                                  float mouseX, float mouseY,
                                  boolean isListeningForKey, String currentKeyDisplay,
                                  java.util.function.Function<String, String> getTooltipText,
                                  java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                  TooltipInfo hoveredTooltipInfo) {

       boolean hovered = GuiRenderUtil.isHovered(x1, y, x2, y + moduleHeight, mouseX, mouseY);
       int enabledBg = new java.awt.Color(54, 42, 150).getRGB();
       int cardBg = module.isEnabled() ? enabledBg : theme.Modules.getRGB();
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + moduleHeight, 6, cardBg);

       if (hovered && !"ClickGUI".equals(module.getName())) {
           int hi = new java.awt.Color(255, 255, 255, 24).getRGB();
           int lx = (int)(x1 + 1);
           int ty = (int)(y + 1);
           int rx = (int)(x2 - 1);
           int by = (int)(y + moduleHeight - 1);
           context.fill(lx, ty, rx, by, hi);
       }

       String displayName = module.getName();
       if ("ClickGUI".equals(module.getName())) {
           displayName = "ClickGUI";
       }
       context.drawString(client.font, displayName, (int)(x1 + 10), (int)(y + 8), theme.FONT_C.getRGB(), false);

       if (hovered) {
           String tooltip = getTooltipText.apply(module.getName());
           if (tooltip != null) {
               hoveredTooltipInfo.tooltip = tooltip;
               hoveredTooltipInfo.tooltipText = getTooltipTextWithColors.apply(module.getName());
               float tooltipOffset = 5f;
               hoveredTooltipInfo.x = (int)(mouseX + tooltipOffset);
               hoveredTooltipInfo.y = (int)(mouseY + tooltipOffset);
           }
       }

       boolean hasChildren = false;
       for (com.shyeuar.baity.gui.value.Value v : module.getValues()) {
           if (!"enabled".equals(v.getName())) {
               hasChildren = true;
               break;
           }
       }
       if (hasChildren && !module.getName().equals("ClickGUI")) {
           String arrow = module.isExpanded() ? "▼" : "▶";
           context.drawString(client.font, arrow, (int)(x2 - 25), (int)(y + 8), theme.FONT_C.getRGB(), false);
       }

       if (module.getName().equals("ClickGUI")) {
           renderKeybindBox(context, client, theme, x1, y, x2, moduleHeight,
                          mouseX, mouseY, isListeningForKey, currentKeyDisplay);
       }
   }

   public static void renderKeybindBox(GuiGraphics context, Minecraft client, Theme theme,
                                      float containerX1, float containerY, float containerX2, float containerHeight,
                                      float mouseX, float mouseY,
                                      boolean isListening, String keyDisplay) {
       String keyText = isListening ? "Press a key..." : keyDisplay;
       renderKeybindBoxContent(context, client, theme, containerX2, containerY, containerHeight, mouseX, mouseY, isListening, keyText);
   }

   public static void renderKeybindBoxContent(GuiGraphics context, Minecraft client, Theme theme,
                                              float containerX2, float containerY, float containerHeight,
                                              float mouseX, float mouseY, boolean isListening, String displayText) {
       if (displayText == null || displayText.isEmpty()) {
           displayText = "☄ NOTSET";
       }
       String plainText = displayText.replaceAll("§[0-9a-fklmnor]", "");
       int textWidth = client.font.width(plainText);
       int boxWidth = textWidth + 16;
       float boxCenterY = containerY + containerHeight / 2f;
       int boxHeight = 12;

       int boxX1 = (int)(containerX2 - boxWidth - 10);
       int boxY1 = (int)(boxCenterY - boxHeight / 2f);
       int boxX2 = (int)(containerX2 - 10);
       int boxY2 = (int)(boxCenterY + boxHeight / 2f);

       boolean boxHovered = GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, mouseX, mouseY);
       int boxBgColor = isListening ? theme.BG_3.getRGB() :
                       (boxHovered ? new java.awt.Color(255, 255, 255, 24).getRGB() : theme.BG_2.getRGB());
       context.fill(boxX1, boxY1, boxX2, boxY2, boxBgColor);

       int baseX = boxX1 + 8;
       int baseY = (int)(boxCenterY - 4);

       if (isListening) {
           String hintText = "Press Backspace to reset";
           int hintColor = 0xFFFFFF00;
           int hintX = boxX1 - client.font.width(hintText) - 8;
           context.drawString(client.font, hintText, hintX, baseY, hintColor, false);
           
           net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
           context.drawString(client.font, textObj, baseX, baseY, theme.FONT_C.getRGB(), false);
       } else {
           String displayPlainText = displayText.replaceAll("§[0-9a-fklmnor]", "");

           if (displayPlainText.startsWith("✎")) {
               String prefix = "✎";
               String keyName = displayPlainText.substring(1);
               int prefixRGB = com.shyeuar.baity.utils.KeyMappingUtils.getModuleEnabledPurpleRGB();
               int keyNameRGB = theme.FONT.getRGB();

               net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
               context.drawString(client.font, prefixText, baseX, baseY, prefixRGB, false);

               int prefixWidth = client.font.width(prefix);
               net.minecraft.network.chat.Component keyTextObj = net.minecraft.network.chat.Component.literal(keyName);
               context.drawString(client.font, keyTextObj, baseX + prefixWidth, baseY, keyNameRGB, false);
           } else if (displayPlainText.startsWith("☄") || 
                       displayPlainText.toUpperCase().contains("NOTSET") || 
                       displayPlainText.toUpperCase().contains("NONE") || 
                       displayPlainText.toUpperCase().contains("UNKNOWN")) {
               String prefix = "☄";
               String notsetText = displayPlainText.startsWith("☄") ? displayPlainText.substring(1) : (" " + displayPlainText);
               int prefixRGB = 0xFFFFFF00;  // 黄色
               int notsetRGB = 0xFFAAAAAA;  // 灰色

               net.minecraft.network.chat.Component prefixText = net.minecraft.network.chat.Component.literal(prefix);
               context.drawString(client.font, prefixText, baseX, baseY, prefixRGB, false);

               int prefixWidth = client.font.width(prefix);
               net.minecraft.network.chat.Component notsetTextObj = net.minecraft.network.chat.Component.literal(notsetText);
               context.drawString(client.font, notsetTextObj, baseX + prefixWidth, baseY, notsetRGB, false);
           } else {
               net.minecraft.network.chat.Component textObj = net.minecraft.network.chat.Component.literal(displayText);
               context.drawString(client.font, textObj, baseX, baseY, theme.FONT.getRGB(), false);
           }
       }
   }

   public static class TooltipInfo {
       public String tooltip;
       public net.minecraft.network.chat.Component tooltipText;
       public int x, y;
   }
}

