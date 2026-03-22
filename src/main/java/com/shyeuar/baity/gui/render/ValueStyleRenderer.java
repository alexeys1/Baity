package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.GradientEditorValue;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.SliderValue;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ValueStyleRenderer {
   
   public static void renderValue(GuiGraphics context, Minecraft client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
      renderValue(context, client, module, value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha,
                 getTooltipText, getTooltipTextWithColors, getDisplayTextFormatter, listeningButtonValueName, hoveredTooltipInfo, null, "", null, "");
   }
   
   public static void renderValue(GuiGraphics context, Minecraft client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider,
                                 String sliderInputText,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                 String gradientInputText) {
       
       ValueStyle style = value.getStyle();
       
       if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
           renderButtonLikeValue(context, client, module, (ButtonValue) value, theme,
                              x1, y, x2, subOptionHeight,
                              mouseX, mouseY, localAlpha,
                              getDisplayTextFormatter, listeningButtonValueName);
      } else if (style == ValueStyle.GROUP && value instanceof GroupValue) {
          renderGroupValue(context, client, (GroupValue) value, theme, x1, y, x2, subOptionHeight, localAlpha);
       } else if (style == ValueStyle.SLIDER && value instanceof SliderValue) {
           renderSliderValue(context, client, module, (SliderValue) value, theme,
                            x1, y, x2, subOptionHeight,
                            mouseX, mouseY, localAlpha, editingSlider, sliderInputText);
       } else if (style == ValueStyle.COLOR_PALETTE && value instanceof com.shyeuar.baity.gui.value.ColorPaletteValue) {
           renderColorPaletteValue(context, client, module, (com.shyeuar.baity.gui.value.ColorPaletteValue) value, theme,
                                   x1, y, x2, subOptionHeight,
                                   mouseX, mouseY, localAlpha);
      } else if (style == ValueStyle.GRADIENT_EDITOR && value instanceof GradientEditorValue) {
          renderGradientEditorValue(context, client, (GradientEditorValue) value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, editingGradient, gradientInputText, hoveredTooltipInfo);
       } else {
           renderDefaultValue(context, client, module, value, theme,
                          x1, y, x2, subOptionHeight,
                          mouseX, mouseY, localAlpha,
                          getTooltipText, getTooltipTextWithColors, hoveredTooltipInfo);
       }
   }

   public static void renderGroupValue(GuiGraphics context, Minecraft client, GroupValue groupValue, Theme theme,
                                       float x1, float y, float x2, float subOptionHeight,
                                       int localAlpha) {
      int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
      int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);

      int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      context.drawString(client.font, groupValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);

      String arrow = groupValue.isExpanded() ? "▼" : "▶";
      int arrowColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      context.drawString(client.font, arrow, (int)(x2 - 16), (int)(y + 6), arrowColor, false);
   }

   
   public static void renderDefaultValue(GuiGraphics context, Minecraft client, Module module, Value value, Theme theme,
                                       float x1, float y, float x2, float subOptionHeight,
                                       float mouseX, float mouseY, int localAlpha,
                                       java.util.function.Function<String, String> getTooltipText,
                                       java.util.function.Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                       ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       
       boolean subHovered = GuiRenderUtil.isHovered(x1, y, x2, y + subOptionHeight, mouseX, mouseY);
       int baseValueColor = subHovered ? new java.awt.Color(60, 60, 60, 80).getRGB() : 
                           new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
       
       if (subHovered) {
           String tooltip = getTooltipText.apply(value.getName());
           if (tooltip != null) {
               hoveredTooltipInfo.tooltip = tooltip;
               hoveredTooltipInfo.tooltipText = getTooltipTextWithColors.apply(value.getName());
               float tooltipOffset = 5f;
               hoveredTooltipInfo.x = (int)(mouseX + tooltipOffset);
               hoveredTooltipInfo.y = (int)(mouseY + tooltipOffset);
           }
       }
       
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       String displayText = value.getDisplayName();
       context.drawString(client.font, displayText, (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       String status;
       int statusColor;
       Object val = value.getValue();
       var handler = ValueTypeRegistry.getHandlerForValue(val);
       if (handler != null) {
           status = handler.formatValue(val);
          if (val instanceof Boolean) {
              boolean boolValue = (Boolean) val;
              statusColor = boolValue ? com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB() : theme.FONT.getRGB();
           } else {
               statusColor = theme.FONT.getRGB();
           }
       } else {
           status = val != null ? val.toString() : "";
           statusColor = theme.FONT.getRGB();
       }
       if (!module.isEnabled()) {
           statusColor = theme.FONT.getRGB();
       }
       statusColor = (statusColor & 0x00FFFFFF) | (localAlpha << 24);
       
       int statusX = (int)(x2 - 40);
       if (val instanceof Double) {
           statusX = (int)(x2 - 60);
       } else if (val instanceof String) {
           statusX = (int)(x2 - 80);
       }
       context.drawString(client.font, status, statusX, (int)(y + 6), statusColor, false);
   }
   
   public static void renderButtonLikeValue(GuiGraphics context, Minecraft client, Module module, ButtonValue buttonValue, Theme theme,
                                             float x1, float y, float x2, float subOptionHeight,
                                             float mouseX, float mouseY, int localAlpha,
                                             java.util.function.Function<Object, String> getDisplayTextFormatter,
                                             String listeningButtonValueName) {
       
       Object buttonVal = buttonValue.getValue();
       boolean isEnabled = buttonVal instanceof Boolean && (Boolean)buttonVal;
       
       if (isEnabled) {
           int accentStart = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
           int accentEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_SECONDARY.getRGB();
           int aStart = ((accentStart >> 24) & 0xFF) * localAlpha / 255;
           int aEnd = ((accentEnd >> 24) & 0xFF) * localAlpha / 255;
           accentStart = (aStart << 24) | (accentStart & 0x00FFFFFF);
           accentEnd = (aEnd << 24) | (accentEnd & 0x00FFFFFF);
           GuiRenderUtil.draw3DGradientRect(context, x1, y, x2, y + subOptionHeight, accentStart, accentEnd, 6f);
       } else {
           int cardBg = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB();
           int valueColor = (cardBg & 0x00FFFFFF) | (localAlpha << 24);
           GuiRenderUtil.drawFrostedGlass(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
           GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
       }
       
       int textColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, buttonValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       boolean isListeningThis = listeningButtonValueName != null && listeningButtonValueName.equals(buttonValue.getName());
       String boxText;
       if (isListeningThis) {
           boxText = "Press a key...";
       } else {
           boxText = buttonValue.getDisplayText(getDisplayTextFormatter);
       }
       
       ModuleStyleRenderer.renderKeybindBoxContent(context, client, theme, x2, y, subOptionHeight, mouseX, mouseY, isListeningThis, boxText);
   }

   
   public static void renderSliderValue(GuiGraphics context, Minecraft client, Module module, SliderValue sliderValue, Theme theme,
                                        float x1, float y, float x2, float subOptionHeight,
                                        float mouseX, float mouseY, int localAlpha) {
       renderSliderValue(context, client, module, sliderValue, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, null, "");
   }
   
   public static void renderSliderValue(GuiGraphics context, Minecraft client, Module module, SliderValue sliderValue, Theme theme,
                                        float x1, float y, float x2, float subOptionHeight,
                                        float mouseX, float mouseY, int localAlpha,
                                        com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider, String inputText) {
       
       int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.draw3DRect(context, x1, y, x2, y + subOptionHeight, valueColor, 6f);
       
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, sliderValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       int subX2 = (int)(x2 - 4);
       
       int resetBoxWidth = 30;
       int resetBoxHeight = 12;
       int resetBoxX = subX2 - resetBoxWidth - 6;
       int resetBoxY = (int)(y + (subOptionHeight - resetBoxHeight) / 2);
       
       boolean resetHovered = GuiRenderUtil.isHovered(resetBoxX, resetBoxY, resetBoxX + resetBoxWidth, resetBoxY + resetBoxHeight, mouseX, mouseY);
       int resetBgColor = resetHovered ? 
           (new java.awt.Color(70, 70, 70, 200).getRGB() & 0x00FFFFFF) | (localAlpha << 24) :
           (new java.awt.Color(40, 40, 40, 200).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int resetBorderColor = (new java.awt.Color(80, 80, 80, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, resetBoxX, resetBoxY, resetBoxX + resetBoxWidth, resetBoxY + resetBoxHeight, 3, resetBgColor);
       GuiRenderUtil.drawRoundedRectOutline(context, resetBoxX, resetBoxY, resetBoxX + resetBoxWidth, resetBoxY + resetBoxHeight, 3, resetBorderColor);
       
       int resetTextColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       String resetText = "Reset";
       int resetTextWidth = client.font.width(resetText);
       context.drawString(client.font, resetText, resetBoxX + (resetBoxWidth - resetTextWidth) / 2, resetBoxY + 2, resetTextColor, false);
       
       boolean isEditing = editingSlider != null && 
                          editingSlider.moduleName.equals(module.getName()) && 
                          editingSlider.valueName.equals(sliderValue.getName());
       
       String valueText = isEditing ? inputText + "_" : sliderValue.getFormattedValue();
       int valueTextWidth = client.font.width(valueText);
       int valueDisplayWidth = Math.max(valueTextWidth + 8, 35);
       int valueDisplayX = resetBoxX - valueDisplayWidth - 8;
       int valueDisplayY = (int)(y + 4);
       
       int lineY = (int)(y + subOptionHeight - 6);
       int lineColor = (new java.awt.Color(100, 100, 100, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, valueDisplayX, lineY, valueDisplayX + valueDisplayWidth, lineY + 1, 0, lineColor);
       
       int valueTextColor = isEditing ? 
           (new java.awt.Color(255, 255, 100, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24) :
           (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int textX = valueDisplayX + (valueDisplayWidth - client.font.width(valueText)) / 2;
       context.drawString(client.font, valueText, textX, valueDisplayY, valueTextColor, false);
       
       int sliderWidth = 80;
       int sliderHeight = 4;
       int sliderX = valueDisplayX - sliderWidth - 10;
       int sliderY = (int)(y + (subOptionHeight - sliderHeight) / 2);
       
       int sliderBgColor = (new java.awt.Color(20, 20, 20, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 2, sliderBgColor);
       
       int sliderBorderColor = (new java.awt.Color(60, 60, 60, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRectOutline(context, sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 2, sliderBorderColor);
       
       double percentage = sliderValue.getPercentage();
       int filledWidth = (int)(sliderWidth * percentage);
       if (filledWidth > 0) {
           int accentStart = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_PRIMARY.getRGB();
           int accentEnd = com.shyeuar.baity.gui.theme.LinearTheme.ACCENT_SECONDARY.getRGB();
           int aStart = ((accentStart >> 24) & 0xFF) * localAlpha / 255;
           int aEnd = ((accentEnd >> 24) & 0xFF) * localAlpha / 255;
           accentStart = (aStart << 24) | (accentStart & 0x00FFFFFF);
           accentEnd = (aEnd << 24) | (accentEnd & 0x00FFFFFF);
           GuiRenderUtil.drawGradientRect(context, sliderX, sliderY, sliderX + filledWidth, sliderY + sliderHeight, accentStart, accentEnd, 2f);
       }
       
       int handleRadius = 5;
       int handleX = sliderX + filledWidth;
       int handleY = sliderY + sliderHeight / 2;
       int handleColor = (new java.awt.Color(255, 255, 255, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawCircle(context, handleX, handleY, handleRadius, handleColor);
   }

   public static void renderColorPaletteValue(GuiGraphics context, Minecraft client, Module module,
                                              com.shyeuar.baity.gui.value.ColorPaletteValue paletteValue, Theme theme,
                                              float x1, float y, float x2, float subOptionHeight,
                                              float mouseX, float mouseY, int localAlpha) {
       float paletteHeight = subOptionHeight * 2;
       
       int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + paletteHeight, 6, valueColor);
       
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       String displayText = paletteValue.getDisplayName();
       float textY = y + 6;
       context.drawString(client.font, displayText, (int)(x1 + 8), (int) textY, textColor, false);
       
       int colorCount = paletteValue.getColorCount();
       float colorAreaY = y + subOptionHeight; 
       float colorAreaHeight = subOptionHeight - 8; 
       
       float totalWidth = x2 - x1 - 16;
       float boxSize = Math.min(colorAreaHeight - 2, (totalWidth - (colorCount - 1) * 3) / colorCount);
       float spacing = (totalWidth - boxSize * colorCount) / (colorCount - 1);
       float startX = x1 + 8;
       float boxY = colorAreaY + (colorAreaHeight - boxSize) / 2;
       
       int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
       int themePurpleBorder = theme.BG_3.getRGB();
       
       for (int i = 0; i < colorCount; i++) {
           float boxX = startX + i * (boxSize + spacing);
           int color = paletteValue.getColor(i);
           boolean isSelected = paletteValue.isColorSelected(i);
           boolean isHovered = GuiRenderUtil.isHovered(boxX, boxY, boxX + boxSize, boxY + boxSize, mouseX, mouseY);
           
           int fillColor = (color & 0x00FFFFFF) | (localAlpha << 24);
           GuiRenderUtil.drawRoundedRect(context, boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, 2, fillColor);
           
           int borderColor;
           if (isSelected || isHovered) {
               borderColor = (themePurpleBorder & 0x00FFFFFF) | (localAlpha << 24);
           } else {
               borderColor = (themeDarkBorder & 0x00FFFFFF) | (localAlpha << 24);
           }
           GuiRenderUtil.drawRoundedRectOutline(context, boxX, boxY, boxX + boxSize, boxY + boxSize, 2, borderColor);
           
           if (isSelected) {
               GuiRenderUtil.drawRoundedRectOutline(context, boxX - 1, boxY - 1, boxX + boxSize + 1, boxY + boxSize + 1, 3, borderColor);
           }
       }
   }
   
   public static float getColorPaletteHeight(float subOptionHeight) {
       return subOptionHeight * 2;
   }

   public static float getGradientEditorHeight(float subOptionHeight) {
       return subOptionHeight * 6;
   }

   public static void renderGradientEditorValue(GuiGraphics context, Minecraft client, GradientEditorValue value, Theme theme,
                                                float x1, float y, float x2, float subOptionHeight, float mouseX, float mouseY, int localAlpha,
                                                com.shyeuar.baity.gui.internal.ClickGuiState.GradientInputInfo editingGradient,
                                                String gradientInputText,
                                                ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       float blockHeight = getGradientEditorHeight(subOptionHeight);
       int bg = (new java.awt.Color(35, 35, 35, 180).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + blockHeight, 6, bg);

       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawString(client.font, value.getDisplayName(), (int) (x1 + 8), (int) (y + 6), textColor, false);

       float mapX1 = x1 + 8;
       float mapY1 = y + 22;
       float mapX2 = x2 - 60;
       float mapY2 = y + blockHeight - 40;
       drawHueSatMap(context, mapX1, mapY1, mapX2, mapY2, localAlpha);

      float p1x = mapX1 + value.getSelectedHue() * (mapX2 - mapX1);
      float p1y = mapY1 + (1f - value.getSelectedSat()) * (mapY2 - mapY1);
      int pointFill = java.awt.Color.HSBtoRGB(value.getSelectedHue(), value.getSelectedSat(), GradientEditorValue.MAP_FIXED_VALUE) & 0xFFFFFF;
      int selectorColor = (pointFill & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRect(context, p1x - 2, p1y - 2, p1x + 3, p1y + 3, 0, selectorColor);
      float mapMidY = (mapY1 + mapY2) * 0.5f;
      int pointBorderBase = p1y >= mapMidY ? 0x000000 : 0xFFFFFF;
      int pointBorder = (pointBorderBase & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRectOutline(context, p1x - 3, p1y - 3, p1x + 4, p1y + 4, 0, pointBorder);

       float sliderX1 = x2 - 48;
       float sliderX2 = x2 - 36;
       drawValueSlider(context, sliderX1, mapY1, sliderX2, mapY2, value.getSelectedHue(), value.getSelectedSat(), localAlpha);
       float knobY = mapY2 - value.getSelectedVal() * (mapY2 - mapY1);
      int knobColor = (new java.awt.Color(0, 0, 0, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      int knobBorder = (new java.awt.Color(255, 255, 255, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRect(context, sliderX1 - 2, knobY - 2, sliderX2 + 2, knobY + 2, 1, knobColor);
      GuiRenderUtil.drawRoundedRectOutline(context, sliderX1 - 3, knobY - 3, sliderX2 + 3, knobY + 3, 1, knobBorder);

       float box1X1 = x2 - 30;
       float box1X2 = x2 - 12;
       float box1Y1 = y + 24;
       float box1Y2 = y + 42;
      float box2Y2 = mapY2;
      float box2Y1 = box2Y2 - 18;
      int color1 = (0xFF000000 | value.getStartColor() & 0xFFFFFF);
      int color2 = (0xFF000000 | value.getEndColor() & 0xFFFFFF);
      GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box1Y1 + 1, box1X2 - 1, box1Y2 - 1, 2, color1);
      GuiRenderUtil.drawRoundedRect(context, box1X1 + 1, box2Y1 + 1, box1X2 - 1, box2Y2 - 1, 2, color2);
      int themeDarkBorder = new java.awt.Color(50, 50, 50, 255).getRGB();
      int themePurpleBorder = theme.BG_3.getRGB();
      boolean hoverL = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(box1X1, box1Y1, box1X2, box1Y2, mouseX, mouseY);
      boolean hoverR = com.shyeuar.baity.gui.render.GuiRenderUtil.isHovered(box1X1, box2Y1, box1X2, box2Y2, mouseX, mouseY);
      int borderL = (value.getSelectedPoint() == 0 || hoverL ? themePurpleBorder : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
      int borderR = (value.getSelectedPoint() == 1 || hoverR ? themePurpleBorder : themeDarkBorder) & 0x00FFFFFF | (localAlpha << 24);
      GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box1Y1, box1X2, box1Y2, 2, borderL);
      GuiRenderUtil.drawRoundedRectOutline(context, box1X1, box2Y1, box1X2, box2Y2, 2, borderR);
      if (value.getSelectedPoint() == 0) {
          GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box1Y1 - 1, box1X2 + 1, box1Y2 + 1, 3, borderL);
      } else {
          GuiRenderUtil.drawRoundedRectOutline(context, box1X1 - 1, box2Y1 - 1, box1X2 + 1, box2Y2 + 1, 3, borderR);
      }
      drawScaledLabel(context, client, "L", box1X1 + 6, box1Y2 + 2, textColor, 0.85f);
      drawScaledLabel(context, client, "R", box1X1 + 6, box2Y1 - 8, textColor, 0.85f);

       String selectedHex = value.getSelectedHex();
       if (editingGradient != null && "gradient editor".equals(editingGradient.valueName)) {
           selectedHex = "#" + gradientInputText.toUpperCase(java.util.Locale.ROOT);
       }
      int inputWidth = client.font.width("#FFFFFF");
       float inputX2 = mapX2;
       float inputX1 = inputX2 - inputWidth;
      float syncX2 = x2 - 8;
      float syncY2 = y + blockHeight - 8;
      float syncX1 = syncX2 - 48;
      float syncY1 = syncY2 - 14;
      float inputY = syncY2 - 3; // align with Sync row
      boolean inputHovered = GuiRenderUtil.isHovered(inputX1, inputY - 12, inputX2, inputY + 6, mouseX, mouseY);
      int hoverYellow = (new java.awt.Color(255, 255, 0, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
      int inputTextColor = (editingGradient != null) ? hoverYellow : (inputHovered ? hoverYellow : textColor);
       context.drawString(client.font, selectedHex, (int) inputX1, (int) (inputY - 9), inputTextColor, false);
      int lineColor = inputHovered || editingGradient != null
          ? hoverYellow
          : ((new java.awt.Color(120, 120, 120, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
      GuiRenderUtil.drawRoundedRect(context, inputX1, inputY, inputX2, inputY + 1, 0, lineColor);
      boolean syncHovered = GuiRenderUtil.isHovered(syncX1, syncY1, syncX2, syncY2, mouseX, mouseY);
      int syncBg = syncHovered
          ? ((new java.awt.Color(60, 60, 60, 80).getRGB() & 0x00FFFFFF) | (localAlpha << 24))
          : ((new java.awt.Color(40, 40, 40, 50).getRGB() & 0x00FFFFFF) | (localAlpha << 24));
      GuiRenderUtil.draw3DRect(context, syncX1, syncY1, syncX2, syncY2, syncBg, 0f);
       context.drawString(client.font, "Sync", (int) (syncX1 + 12), (int) (syncY1 + 3), textColor, false);
      if (syncHovered && hoveredTooltipInfo != null) {
          hoveredTooltipInfo.tooltip = "Copy the unselected endpoint color into the selected endpoint";
          hoveredTooltipInfo.tooltipText = net.minecraft.network.chat.Component.literal("Copy the unselected endpoint color into the selected endpoint");
          hoveredTooltipInfo.x = (int) (mouseX + 5);
          hoveredTooltipInfo.y = (int) (mouseY + 5);
      }

      String preview = client.player != null ? client.player.getName().getString() : "NickTweaks";
      if (com.shyeuar.baity.config.ConfigManager.nickTweaksChromaEnabled) {
          drawChromaPreview(context, client, preview, x1 + 10, y + blockHeight - 18);
      } else {
          drawGradientText(context, client, preview, x1 + 10, y + blockHeight - 18, value.getStartColor(), value.getEndColor());
      }
   }

   private static void drawHueSatMap(GuiGraphics context, float x1, float y1, float x2, float y2, int alpha) {
       int width = Math.max(1, (int) (x2 - x1));
       int height = Math.max(1, (int) (y2 - y1));
       for (int px = 0; px < width; px += 4) {
           float hue = px / (float) width;
           for (int py = 0; py < height; py += 4) {
               float sat = 1f - py / (float) height;
               int rgb = java.awt.Color.HSBtoRGB(hue, sat, GradientEditorValue.MAP_FIXED_VALUE) & 0xFFFFFF;
               int color = (alpha << 24) | rgb;
               context.fill((int) x1 + px, (int) y1 + py, (int) x1 + px + 4, (int) y1 + py + 4, color);
           }
       }
   }

   private static void drawChromaPreview(GuiGraphics context, Minecraft client, String text, float x, float y) {
       if (text == null || text.isEmpty()) return;
       long nowMs = System.currentTimeMillis();
       double lightness = Math.max(0.2, Math.min(1.0, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaLightness));
       double chroma = Math.max(0.0, Math.min(0.4, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaChroma));
       double size = Math.max(0.1, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaSize);
       double speed = Math.max(0.0, Math.min(8.0, com.shyeuar.baity.config.ConfigManager.nickTweaksChromaSpeed));
       double phase = (nowMs / 1000.0) * (speed * 0.5);
       float saturation = (float) (chroma / 0.4);

       int len = text.codePointCount(0, text.length());
       int index = 0;
       float cursor = x;
       for (int offset = 0; offset < text.length(); ) {
           int cp = text.codePointAt(offset);
           int charCount = Character.charCount(cp);
           String chunk = new String(Character.toChars(cp));
           double progress = len <= 1 ? 0.0 : (double) index / (double) (len - 1);
           float hue = (float) positiveModulo((progress / size) - phase, 1.0);
           int color = 0xFF000000 | (net.minecraft.util.Mth.hsvToRgb(hue, saturation, (float) lightness) & 0xFFFFFF);
           context.drawString(client.font, chunk, (int) cursor, (int) y, color, false);
           cursor += client.font.width(chunk);
           offset += charCount;
           index++;
       }
   }

   private static double positiveModulo(double value, double mod) {
       double result = value % mod;
        return result < 0 ? result + mod : result;
   }

   private static void drawValueSlider(GuiGraphics context, float x1, float y1, float x2, float y2, float hue, float sat, int alpha) {
       int height = Math.max(1, (int) (y2 - y1));
       for (int py = 0; py < height; py += 2) {
           float val = 1f - py / (float) height;
           int rgb = java.awt.Color.HSBtoRGB(hue, sat, val) & 0xFFFFFF;
           int color = (alpha << 24) | rgb;
           context.fill((int) x1, (int) y1 + py, (int) x2, (int) y1 + py + 2, color);
       }
   }

   private static void drawGradientText(GuiGraphics context, Minecraft client, String text, float x, float y, int startColor, int endColor) {
       if (text == null || text.isEmpty()) {
           return;
       }
       int len = text.codePointCount(0, text.length());
       int index = 0;
       float cursor = x;
       for (int offset = 0; offset < text.length(); ) {
           int cp = text.codePointAt(offset);
           int charCount = Character.charCount(cp);
           String chunk = new String(Character.toChars(cp));
           double t = len <= 1 ? 0.0 : (double) index / (double) (len - 1);
           int r = (int) Math.round(((startColor >> 16) & 0xFF) + (((endColor >> 16) & 0xFF) - ((startColor >> 16) & 0xFF)) * t);
           int g = (int) Math.round(((startColor >> 8) & 0xFF) + (((endColor >> 8) & 0xFF) - ((startColor >> 8) & 0xFF)) * t);
           int b = (int) Math.round((startColor & 0xFF) + ((endColor & 0xFF) - (startColor & 0xFF)) * t);
           int color = 0xFF000000 | (r << 16) | (g << 8) | b;
           context.drawString(client.font, chunk, (int) cursor, (int) y, color, false);
           cursor += client.font.width(chunk);
           offset += charCount;
           index++;
       }
   }

   private static void drawScaledLabel(GuiGraphics context, Minecraft client, String text, float x, float y, int color, float scale) {
      var pose = context.pose();
      pose.pushMatrix();
      pose.translate(x, y);
      pose.scale(scale, scale);
      context.drawString(client.font, text, 0, 0, color, false);
      pose.popMatrix();
   }
   
   public static int getHoveredColorIndex(com.shyeuar.baity.gui.value.ColorPaletteValue paletteValue,
                                          float x1, float y, float x2, float subOptionHeight,
                                          float mouseX, float mouseY) {
       int colorCount = paletteValue.getColorCount();
       float colorAreaY = y + subOptionHeight;
       float colorAreaHeight = subOptionHeight - 8;
       
       float totalWidth = x2 - x1 - 16;
       float boxSize = Math.min(colorAreaHeight - 2, (totalWidth - (colorCount - 1) * 3) / colorCount);
       float spacing = (totalWidth - boxSize * colorCount) / (colorCount - 1);
       float startX = x1 + 8;
       float boxY = colorAreaY + (colorAreaHeight - boxSize) / 2;
       
       for (int i = 0; i < colorCount; i++) {
           float boxX = startX + i * (boxSize + spacing);
           if (GuiRenderUtil.isHovered(boxX, boxY, boxX + boxSize, boxY + boxSize, mouseX, mouseY)) {
               return i;
           }
       }
       return -1;
   }
}
