package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.SliderValue;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class ValueStyleRenderer {
   
   public static void renderValue(DrawContext context, MinecraftClient client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       renderValue(context, client, module, value, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha,
                  getTooltipText, getTooltipTextWithColors, getDisplayTextFormatter, listeningButtonValueName, hoveredTooltipInfo, null, "");
   }
   
   public static void renderValue(DrawContext context, MinecraftClient client, Module module, Value value, Theme theme,
                                 float x1, float y, float x2, float subOptionHeight,
                                 float mouseX, float mouseY, int localAlpha,
                                 java.util.function.Function<String, String> getTooltipText,
                                 java.util.function.Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                 java.util.function.Function<Object, String> getDisplayTextFormatter,
                                 String listeningButtonValueName,
                                 ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo,
                                 com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider,
                                 String sliderInputText) {
       
       ValueStyle style = value.getStyle();
       
       if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
           renderButtonLikeValue(context, client, module, (ButtonValue) value, theme,
                              x1, y, x2, subOptionHeight,
                              mouseX, mouseY, localAlpha,
                              getDisplayTextFormatter, listeningButtonValueName);
       } else if (style == ValueStyle.SLIDER && value instanceof SliderValue) {
           renderSliderValue(context, client, module, (SliderValue) value, theme,
                            x1, y, x2, subOptionHeight,
                            mouseX, mouseY, localAlpha, editingSlider, sliderInputText);
       } else {
           renderDefaultValue(context, client, module, value, theme,
                          x1, y, x2, subOptionHeight,
                          mouseX, mouseY, localAlpha,
                          getTooltipText, getTooltipTextWithColors, hoveredTooltipInfo);
       }
   }
   
   public static void renderDefaultValue(DrawContext context, MinecraftClient client, Module module, Value value, Theme theme,
                                       float x1, float y, float x2, float subOptionHeight,
                                       float mouseX, float mouseY, int localAlpha,
                                       java.util.function.Function<String, String> getTooltipText,
                                       java.util.function.Function<String, net.minecraft.text.Text> getTooltipTextWithColors,
                                       ModuleStyleRenderer.TooltipInfo hoveredTooltipInfo) {
       
       boolean subHovered = GuiRenderUtil.isHovered(x1, y, x2, y + subOptionHeight, mouseX, mouseY);
       int baseValueColor = subHovered ? new java.awt.Color(60, 60, 60, 80).getRGB() : 
                           new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + subOptionHeight, 6, valueColor);
       
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
       context.drawText(client.textRenderer, displayText, (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       String status;
       int statusColor;
       Object val = value.getValue();
       var handler = ValueTypeRegistry.getHandlerForValue(val);
       if (handler != null) {
           status = handler.formatValue(val);
           if (val instanceof Boolean) {
               boolean boolValue = (Boolean) val;
               statusColor = boolValue ? theme.BG_3.getRGB() : theme.FONT.getRGB();
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
       context.drawText(client.textRenderer, status, statusX, (int)(y + 6), statusColor, false);
   }
   
   public static void renderButtonLikeValue(DrawContext context, MinecraftClient client, Module module, ButtonValue buttonValue, Theme theme,
                                             float x1, float y, float x2, float subOptionHeight,
                                             float mouseX, float mouseY, int localAlpha,
                                             java.util.function.Function<Object, String> getDisplayTextFormatter,
                                             String listeningButtonValueName) {
       
       int buttonEnabledBg = new java.awt.Color(54, 42, 150).getRGB();
       int valueColor = (buttonEnabledBg & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + subOptionHeight, 6, valueColor);
       
       int textColor = (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawText(client.textRenderer, buttonValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);
       
       boolean isListeningThis = listeningButtonValueName != null && listeningButtonValueName.equals(buttonValue.getName());
       String boxText;
       if (isListeningThis) {
           boxText = "Press a key...";
       } else {
           boxText = buttonValue.getDisplayText(getDisplayTextFormatter);
       }
       
       ModuleStyleRenderer.renderKeybindBoxContent(context, client, theme, x2, y, subOptionHeight, mouseX, mouseY, isListeningThis, boxText);
   }
   
   public static void renderSliderValue(DrawContext context, MinecraftClient client, Module module, SliderValue sliderValue, Theme theme,
                                        float x1, float y, float x2, float subOptionHeight,
                                        float mouseX, float mouseY, int localAlpha) {
       renderSliderValue(context, client, module, sliderValue, theme, x1, y, x2, subOptionHeight, mouseX, mouseY, localAlpha, null, "");
   }
   
   public static void renderSliderValue(DrawContext context, MinecraftClient client, Module module, SliderValue sliderValue, Theme theme,
                                        float x1, float y, float x2, float subOptionHeight,
                                        float mouseX, float mouseY, int localAlpha,
                                        com.shyeuar.baity.gui.internal.ClickGuiState.SliderInputInfo editingSlider, String inputText) {
       
       int baseValueColor = new java.awt.Color(40, 40, 40, 50).getRGB();
       int valueColor = (baseValueColor & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, x1, y, x2, y + subOptionHeight, 6, valueColor);
       
       int textColor = (theme.FONT.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       context.drawText(client.textRenderer, sliderValue.getDisplayName(), (int)(x1 + 8), (int)(y + 6), textColor, false);
       
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
       int resetTextWidth = client.textRenderer.getWidth(resetText);
       context.drawText(client.textRenderer, resetText, resetBoxX + (resetBoxWidth - resetTextWidth) / 2, resetBoxY + 2, resetTextColor, false);
       
       boolean isEditing = editingSlider != null && 
                          editingSlider.moduleName.equals(module.getName()) && 
                          editingSlider.valueName.equals(sliderValue.getName());
       
       String valueText = isEditing ? inputText + "_" : sliderValue.getFormattedValue();
       int valueTextWidth = client.textRenderer.getWidth(valueText);
       int valueDisplayWidth = Math.max(valueTextWidth + 8, 35);
       int valueDisplayX = resetBoxX - valueDisplayWidth - 8;
       int valueDisplayY = (int)(y + 4);
       
       int lineY = (int)(y + subOptionHeight - 6);
       int lineColor = (new java.awt.Color(100, 100, 100, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawRoundedRect(context, valueDisplayX, lineY, valueDisplayX + valueDisplayWidth, lineY + 1, 0, lineColor);
       
       int valueTextColor = isEditing ? 
           (new java.awt.Color(255, 255, 100, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24) :
           (theme.FONT_C.getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       int textX = valueDisplayX + (valueDisplayWidth - client.textRenderer.getWidth(valueText)) / 2;
       context.drawText(client.textRenderer, valueText, textX, valueDisplayY, valueTextColor, false);
       
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
           int sliderFillColor = (new java.awt.Color(54, 42, 150, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
           GuiRenderUtil.drawRoundedRect(context, sliderX, sliderY, sliderX + filledWidth, sliderY + sliderHeight, 2, sliderFillColor);
       }
       
       int handleRadius = 5;
       int handleX = sliderX + filledWidth;
       int handleY = sliderY + sliderHeight / 2;
       int handleColor = (new java.awt.Color(255, 255, 255, 255).getRGB() & 0x00FFFFFF) | (localAlpha << 24);
       GuiRenderUtil.drawCircle(context, handleX, handleY, handleRadius, handleColor);
   }
}

