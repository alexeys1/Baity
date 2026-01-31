package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.internal.ClickGuiLayout;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class ClickGuiRenderer {
    
    public static void render(GuiGraphics context, Minecraft client, 
                             ClickGuiState state, Theme theme,
                             Function<String, String> getTooltipText,
                             Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                             Function<Object, String> getDisplayTextFormatter,
                             ModuleStyleRenderer.TooltipInfo tooltipInfo,
                             double mouseX, double mouseY) {
        
        updateModuleExpandAnimations(state);
        
        state.setHoveredTooltip(null);
        state.setHoveredTooltipText(null);
        tooltipInfo.tooltip = null;
        tooltipInfo.tooltipText = null;
        
        float scaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);

        var matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(state.getWindowX(), state.getWindowY());
        matrices.scale(scaleRatio, scaleRatio);
        
        renderWindowBackground(context, theme);
        
        renderCategoryBar(context, client, state, theme);
        
        float visibleTop = ClickGuiState.LIST_TOP_PADDING;
        float visibleBottom = ClickGuiState.HEIGHT - 20;
        float visibleHeight = Math.max(0, visibleBottom - visibleTop);
        float contentHeight = ClickGuiLayout.calculateContentHeight(state, visibleHeight);
        
        ClickGuiLayout.ScrollbarInfo scrollbarInfo = ClickGuiLayout.calculateScrollbar(state, contentHeight, visibleHeight);
        ClickGuiLayout.clampScrollOffset(state, scrollbarInfo.maxScroll);
        
        float modY = 60 - state.getScrollOffset();
        List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
        
        if (modules.isEmpty()) {
            renderPlaceholder(context, client, theme, modY);
            modY += 100;
        }
        
        context.enableScissor(0, (int)ClickGuiState.LIST_TOP_PADDING, 
                             (int)ClickGuiState.WIDTH, (int)(ClickGuiState.HEIGHT - 20));
        
        for (Module module : modules) {
            renderModule(context, client, module, theme, state, 
                        ClickGuiState.WIDTH - 20, modY,
                        coords.mouseX, coords.mouseY,
                        getTooltipText, getTooltipTextWithColors, tooltipInfo);
            
            if (tooltipInfo.tooltip != null) {
                state.setHoveredTooltip(tooltipInfo.tooltip);
                state.setHoveredTooltipText(tooltipInfo.tooltipText);
                state.setTooltipX(tooltipInfo.x);
                state.setTooltipY(tooltipInfo.y);
            }
            
            modY += 30;
            
            modY += renderSubOptions(context, client, module, theme, state,
                                   modY, visibleHeight,
                                   coords.mouseX, coords.mouseY,
                                   getTooltipText, getTooltipTextWithColors,
                                   getDisplayTextFormatter, tooltipInfo);
        }
        
        context.disableScissor();
        
        if (contentHeight > visibleHeight) {
            renderScrollbar(context, theme, scrollbarInfo);
        }
        
        renderWatermark(context, client, theme);

        matrices.popMatrix();
        
        if (state.getHoveredTooltip() != null) {
            renderTooltip(context, client, theme, state, mouseX, mouseY);
        }
    }
    
    private static void renderWindowBackground(GuiGraphics context, Theme theme) {
        GuiRenderUtil.drawRoundedRect(context, 0, 0, ClickGuiState.WIDTH, ClickGuiState.HEIGHT, 
                                      6, theme.BG.getRGB());
        GuiRenderUtil.stroke1px(context, 0, 0, ClickGuiState.WIDTH, ClickGuiState.HEIGHT, 
                                new java.awt.Color(255, 255, 255, 20).getRGB());
    }
    
    private static void renderCategoryBar(GuiGraphics context, Minecraft client,
                                        ClickGuiState state, Theme theme) {
        float cateX = 20;
        float cateY = 30;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
             com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            boolean active = category == state.getSelectedCategory();
            String label = category.getDisplayName();
            int w = client.font.width(label);
            int color = active ? theme.FONT_C.getRGB() : theme.FONT.getRGB();
            context.drawString(client.font, label, (int)cateX, (int)cateY, color, false);
            
            if (active) {
                float textLeft = cateX;
                float textRight = cateX + w;
                float textCenterX = (textLeft + textRight) / 2f;
                float lineExtension = 6f;
                float lineLeft = textCenterX - w/2f - lineExtension;
                float lineRight = textCenterX + w/2f + lineExtension;
                GuiRenderUtil.divider(context, lineLeft, cateY + 12, lineRight, cateY + 13,
                                     new java.awt.Color(255, 255, 255, 64).getRGB());
            }
            cateX += w + 28;
        }
    }
    
    private static void renderPlaceholder(GuiGraphics context, Minecraft client,
                                         Theme theme, float modY) {
        String placeholderText = "not coming soon~~~";
        int textWidth = client.font.width(placeholderText);
        int textX = (int)((ClickGuiState.WIDTH - textWidth) / 2);
        int textY = (int)(modY + 50);
        context.drawString(client.font, placeholderText, textX, textY, theme.FONT.getRGB(), false);
    }
    
    private static void renderModule(GuiGraphics context, Minecraft client,
                                   Module module, Theme theme, ClickGuiState state,
                                   float width, float modY,
                                   float mouseX, float mouseY,
                                   Function<String, String> getTooltipText,
                                   Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                   ModuleStyleRenderer.TooltipInfo tooltipInfo) {
        ModuleStyleRenderer.renderModule(context, client, module, theme,
                                        20, modY, width, 25,
                                        mouseX, mouseY,
                                        state.isListeningForKey(), state.getCurrentKeyDisplay(),
                                        getTooltipText, getTooltipTextWithColors, tooltipInfo);
    }
    
    private static float renderSubOptions(GuiGraphics context, Minecraft client,
                                         Module module, Theme theme, ClickGuiState state,
                                         float modY, float visibleHeight,
                                         float mouseX, float mouseY,
                                         Function<String, String> getTooltipText,
                                         Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                         Function<Object, String> getDisplayTextFormatter,
                                         ModuleStyleRenderer.TooltipInfo tooltipInfo) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return 0;
        
        float expandProgress = getModuleExpandProgress(state, module.getName());
        if (expandProgress <= 0.0f) return 0;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        int fullContainerHeight = subOptionCount * dims.subOptionHeight + dims.padding * 2 + extraHeight;
        int containerHeight = (int)(fullContainerHeight * expandProgress);

        int containerBg = new java.awt.Color(30, 30, 30, 200).getRGB();
        int containerX1 = 30;
        int containerY1 = (int)modY;
        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
        int containerY2 = (int)(modY + containerHeight);
        
        context.fill(containerX1, containerY1, containerX2, containerY2, containerBg);
        GuiRenderUtil.stroke1px(context, containerX1, containerY1, containerX2, containerY2,
                               new java.awt.Color(255, 255, 255, 40).getRGB());
        
        int innerVisible = Math.max(0, containerHeight - dims.padding * 2);
        if (innerVisible >= dims.subOptionHeight / 2) {
            float subModY = modY + dims.padding;
            
            Value previousValue = null;
            for (Value value : module.getValues()) {
                if ("enabled".equals(value.getName())) continue;
                
                if (value.needsSeparatorBefore(previousValue)) {
                    subModY += 12; 
                }
                
                float currentHeight = dims.subOptionHeight;
                if (value.getStyle() == com.shyeuar.baity.gui.value.ValueStyle.COLOR_PALETTE) {
                    currentHeight = dims.subOptionHeight * 2;
                }
                
                float localAlphaF = Math.min(1f, Math.max(0f, 
                    (innerVisible - (subModY - modY - dims.padding)) / (float)dims.subOptionHeight));
                int localAlpha = (int)(255 * expandProgress * localAlphaF);
                
                int subX1 = containerX1 + 4;
                int subX2 = containerX2 - 4;
                
                ValueStyleRenderer.renderValue(context, client, module, value, theme,
                                              subX1, subModY, subX2, dims.subOptionHeight,
                                              mouseX, mouseY, localAlpha,
                                              getTooltipText, getTooltipTextWithColors,
                                              getDisplayTextFormatter,
                                              state.getListeningButtonValueName(),
                                              tooltipInfo,
                                              state.getEditingSlider(),
                                              state.getSliderInputText());
                
                if (tooltipInfo.tooltip != null) {
                    state.setHoveredTooltip(tooltipInfo.tooltip);
                    state.setHoveredTooltipText(tooltipInfo.tooltipText);
                    state.setTooltipX(tooltipInfo.x);
                    state.setTooltipY(tooltipInfo.y);
                }
                
                subModY += currentHeight;
                previousValue = value;
            }
        }
        
        return containerHeight + 5;
    }
    
    private static void renderScrollbar(GuiGraphics context, Theme theme,
                                       ClickGuiLayout.ScrollbarInfo info) {
        float barX1 = ClickGuiState.WIDTH - 6;
        float barX2 = ClickGuiState.WIDTH - 2;
        GuiRenderUtil.drawRoundedRect(context, barX1, info.barY, barX2, 
                                     info.barY + info.barHeight, 2, theme.BG_2.getRGB());
    }
    
    private static void renderWatermark(GuiGraphics context, Minecraft client, Theme theme) {
        String watermark = "Baity by 11YearCookieBuff (AKA raueyhs , shyeuar)";
        int wmRawWidth = client.font.width(watermark);
        float wmScale = 0.70f;
        float scaledWidth = wmScale * wmRawWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledWidth - 8;
        float baseY = 8;
        
        int wmColor = new java.awt.Color(120, 124, 132).getRGB();
        var matrices = context.pose();
        matrices.pushMatrix();
        matrices.scale(wmScale, wmScale);
        context.drawString(client.font, watermark, 
                        (int)(baseX / wmScale), (int)(baseY / wmScale), wmColor, false);
        matrices.popMatrix();
    }
    
    private static void renderTooltip(GuiGraphics context, Minecraft client,
                                     Theme theme, ClickGuiState state,
                                     double mouseX, double mouseY) {
        float tooltipScaleRatio = ClickGuiState.BASE_GUI_SCALE / state.getGuiScale();
        float tipScale = 0.75f * tooltipScaleRatio;
        int offsetFromCursor = (int)(3 * tooltipScaleRatio);
        
        int rawTextWidth;
        if (state.getHoveredTooltipText() != null) {
            rawTextWidth = client.font.width(state.getHoveredTooltipText());
        } else {
            rawTextWidth = client.font.width(state.getHoveredTooltip());
        }
        
        int bgPadding = 10;
        int rawFontHeight = 9;
        int rawTooltipWidth = rawTextWidth + bgPadding;
        int rawTooltipHeight = rawFontHeight + 8;
        
        int scaledTooltipWidth = (int)(rawTooltipWidth * tipScale);
        int scaledTooltipHeight = (int)(rawTooltipHeight * tipScale);
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        
        int finalTooltipX = (int)mouseX + offsetFromCursor;
        int finalTooltipY = (int)mouseY - offsetFromCursor - scaledTooltipHeight;
        
        if (finalTooltipX + scaledTooltipWidth > screenWidth) {
            finalTooltipX = (int)mouseX - scaledTooltipWidth - offsetFromCursor;
        }
        if (finalTooltipY < 2) {
            finalTooltipY = (int)mouseY + offsetFromCursor;
        }
        if (finalTooltipY + scaledTooltipHeight > screenHeight) {
            finalTooltipY = screenHeight - scaledTooltipHeight - 2;
        }
        if (finalTooltipX < 2) {
            finalTooltipX = 2;
        }
        
        var guiMatrices = context.pose();
        guiMatrices.pushMatrix();
        guiMatrices.translate((float)finalTooltipX, (float)finalTooltipY);
        guiMatrices.scale(tipScale, tipScale);
        
        GuiRenderUtil.drawRoundedRect(context, 0, 0,
                                      rawTooltipWidth, rawTooltipHeight,
                                      4, theme.BG_2.getRGB());
        
        int textX = bgPadding / 2;
        int textY = (rawTooltipHeight - rawFontHeight) / 2;
        
        if (state.getHoveredTooltipText() != null) {
            context.drawString(client.font, state.getHoveredTooltipText(), textX, textY, 0xFFFFFFFF, false);
        } else if (state.getHoveredTooltip() != null) {
            context.drawString(client.font, state.getHoveredTooltip(), textX, textY, theme.FONT_C.getRGB() | 0xFF000000, false);
        }

        guiMatrices.popMatrix();
    }
    
    private static void updateModuleExpandAnimations(ClickGuiState state) {
        for (Module module : ModuleManager.getModulesByCategory(state.getSelectedCategory())) {
            updateModuleExpandAnimation(state, module.getName(), module.isExpanded());
        }
    }
    
    private static void updateModuleExpandAnimation(ClickGuiState state, 
                                                    String moduleName, boolean expanded) {
        float target = expanded ? 1.0f : 0.0f;
        float current = state.getModuleExpandAnimations().getOrDefault(moduleName, 0.0f);
        
        float speed = 0.18f;
        float lin = current + (target - current) * speed;
        float t = Math.max(0f, Math.min(1f, lin));
        float eased = t * t * (3 - 2 * t);
        float newValue = eased;
        
        if (Math.abs(newValue - target) < 0.01f) {
            newValue = target;
        }
        
        state.getModuleExpandAnimations().put(moduleName, newValue);
    }
    
    private static float getModuleExpandProgress(ClickGuiState state, String moduleName) {
        return state.getModuleExpandAnimations().getOrDefault(moduleName, 0.0f);
    }
}
