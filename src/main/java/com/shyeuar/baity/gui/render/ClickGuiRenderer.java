package com.shyeuar.baity.gui.render;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.internal.ClickGuiLayout;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.config.DevConfig;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

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
        
        updateModuleShimmerAnimations(state, coords.mouseX, coords.mouseY);

        var matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(state.getWindowX(), state.getWindowY());
        matrices.scale(scaleRatio, scaleRatio);
        
        renderWindowBackground(context, theme);
        
        renderSidebar(context, client, state, theme, coords.mouseX, coords.mouseY);
        
        renderSearchBar(context, client, state, theme, coords.mouseX, coords.mouseY);
        
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        
        float visibleTop = contentY;
        float visibleBottom = ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        float visibleHeight = Math.max(0, visibleBottom - visibleTop);
        
        List<Module> modules = getFilteredModules(state);
        float calculatedContentHeight = ClickGuiLayout.calculateContentHeightForModules(modules, visibleHeight);
        
        ClickGuiLayout.ScrollbarInfo scrollbarInfo = ClickGuiLayout.calculateScrollbar(state, calculatedContentHeight, visibleHeight);
        ClickGuiLayout.clampScrollOffset(state, scrollbarInfo.maxScroll);
        
        context.enableScissor((int)contentX, (int)contentY, 
                             (int)(contentX + contentWidth), (int)(contentY + visibleHeight));
        
        float modY = contentY + 10 - state.getScrollOffset();
        
        if (modules.isEmpty()) {
            renderPlaceholder(context, client, theme, modY, contentX, contentWidth);
            modY += 100;
        }
        
        ModuleStyleRenderer.setState(state);
        
        for (Module module : modules) {
            renderModule(context, client, module, theme, state, 
                        contentX + 10, modY, contentX + contentWidth - 10,
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
                                   contentX + 20, modY, contentX + contentWidth - 20,
                                   visibleHeight,
                                   coords.mouseX, coords.mouseY,
                                   getTooltipText, getTooltipTextWithColors,
                                   getDisplayTextFormatter, tooltipInfo);
        }
        
        context.disableScissor();
        
        if (calculatedContentHeight > visibleHeight) {
            renderScrollbar(context, theme, scrollbarInfo, contentX + contentWidth);
        }
        
        renderWatermark(context, client, theme);
        
        updateVersionCheckStatus(state);
        renderVersion(context, client, theme, state, coords.mouseX, coords.mouseY);

        matrices.popMatrix();
        
        if (state.getHoveredTooltip() != null) {
            renderTooltip(context, client, theme, state, mouseX, mouseY);
        }
    }
    
    private static void renderWindowBackground(GuiGraphics context, Theme theme) {
        GuiRenderUtil.draw3DRect(context, 0, 0, ClickGuiState.WIDTH, ClickGuiState.HEIGHT, 
                                 theme.BG.getRGB(), 6f);
    }
    
    private static List<Module> getFilteredModules(ClickGuiState state) {
        String searchText = state.getSearchText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            return ModuleManager.getModulesByCategory(state.getSelectedCategory());
        }
        
        return ModuleManager.getModules().stream()
                .filter(module -> module.getName().toLowerCase().contains(searchText))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private static void renderLogoAndTitle(GuiGraphics context, Minecraft client, ClickGuiState state, 
                                          Theme theme, float mouseX, float mouseY) {
        float availableHeight = ClickGuiState.HEADER_HEIGHT + 20;
        float availableWidth = ClickGuiState.SIDEBAR_WIDTH;
        
        int logoSize = (int)(Math.min(availableWidth, availableHeight) * 1.3f);
        
        int logoX = (int)((availableWidth - logoSize) / 2);
        int logoY = (int)((availableHeight - logoSize) / 2);
        
        ResourceLocation logoTexture = ResourceLocation.fromNamespaceAndPath("baity", "textures/gui/logo.png");
        
        context.blit(RenderPipelines.GUI_TEXTURED, logoTexture,
            logoX, logoY,
            0.0f, 0.0f, logoSize, logoSize, logoSize, logoSize);
    }
    
    private static void renderSidebar(GuiGraphics context, Minecraft client,
                                     ClickGuiState state, Theme theme,
                                     float mouseX, float mouseY) {
        GuiRenderUtil.draw3DRect(context, 0, 0, (int)ClickGuiState.SIDEBAR_WIDTH, (int)ClickGuiState.HEIGHT, 
                                 theme.BG.getRGB(), 0f);
        
        renderLogoAndTitle(context, client, state, theme, mouseX, mouseY);
        
        GuiRenderUtil.divider(context, ClickGuiState.SIDEBAR_WIDTH, 0, 
                            ClickGuiState.SIDEBAR_WIDTH, ClickGuiState.HEIGHT,
                            new java.awt.Color(255, 255, 255, 20).getRGB());
        
        float categoryY = ClickGuiState.HEADER_HEIGHT + 20;
        float categorySpacing = 35f;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
            com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            boolean active = category == state.getSelectedCategory();
            boolean hovered = mouseX >= 0 && mouseX < ClickGuiState.SIDEBAR_WIDTH &&
                            mouseY >= categoryY - 5 && mouseY < categoryY + 25;
            
            int categoryBgColor = theme.BG_2.getRGB();
            if (active) {
                GuiRenderUtil.draw3DRect(context, 5, categoryY - 5, ClickGuiState.SIDEBAR_WIDTH - 5, categoryY + 25, categoryBgColor, 0f);
                
                int purpleBar = theme.BG_3.getRGB();
                GuiRenderUtil.draw3DRect(context, 0, categoryY - 5, 3, categoryY + 25, purpleBar, 0f);
            } else if (hovered) {
                int hoverBg = new java.awt.Color(255, 255, 255, 24).getRGB();
                GuiRenderUtil.draw3DRect(context, 5, categoryY - 5, ClickGuiState.SIDEBAR_WIDTH - 5, categoryY + 25, hoverBg, 0f);
            } else {
                int normalBg = new java.awt.Color(
                    (int)((categoryBgColor >> 16) & 0xFF) - 10,
                    (int)((categoryBgColor >> 8) & 0xFF) - 10,
                    (int)(categoryBgColor & 0xFF) - 10,
                    255
                ).getRGB();
                GuiRenderUtil.draw3DRect(context, 5, categoryY - 5, ClickGuiState.SIDEBAR_WIDTH - 5, categoryY + 25, normalBg, 0f);
            }
            
            String label = category.getDisplayName();
            int textX = 15;
            int textY = (int)categoryY;
            int color = active ? theme.FONT_C.getRGB() : theme.FONT.getRGB();
            
            context.drawString(client.font, label, textX, textY, color, false);
            
            categoryY += categorySpacing;
        }
        
        renderGitHubIcon(context, client, state, theme, mouseX, mouseY);
    }
    
    private static void renderGitHubIcon(GuiGraphics context, Minecraft client,
                                        ClickGuiState state, Theme theme,
                                        float mouseX, float mouseY) {
        int iconSize = 20;
        int padding = 8;
        int iconX = padding;
        int iconY = (int)(ClickGuiState.HEIGHT - iconSize - padding);
        
        boolean isHovered = mouseX >= iconX && mouseX < iconX + iconSize &&
                          mouseY >= iconY && mouseY < iconY + iconSize;
        
        float alpha = isHovered ? 1.0f : 0.7f;
        int color = (int)(alpha * 255) << 24 | 0xFFFFFF;
        
        ResourceLocation githubIcon = ResourceLocation.fromNamespaceAndPath("baity", "textures/gui/github.png");
        
        context.blit(RenderPipelines.GUI_TEXTURED, githubIcon, 
                    iconX, iconY, 
                    0, 0, iconSize, iconSize, iconSize, iconSize, color);
    }
    
    private static void renderSearchBar(GuiGraphics context, Minecraft client,
                                       ClickGuiState state, Theme theme,
                                       float mouseX, float mouseY) {
        float iconSize = 12;
        float iconPadding = 4;
        float searchX = ClickGuiState.SIDEBAR_WIDTH + 20;
        float searchY = 15;
        float searchWidth = ClickGuiState.CONTENT_WIDTH - 40;
        float searchHeight = 20;
        
        ResourceLocation searchIcon = ResourceLocation.fromNamespaceAndPath("baity", "textures/gui/search.png");
        int iconX = (int)(searchX + iconPadding);
        int iconY = (int)(searchY + (searchHeight - iconSize) / 2);
        int iconColor = 0xFFFFFFFF;
        context.blit(RenderPipelines.GUI_TEXTURED, searchIcon,
                    iconX, iconY,
                    0, 0, (int)iconSize, (int)iconSize, (int)iconSize, (int)iconSize, iconColor);
        
        float textStartX = searchX + iconSize + iconPadding * 2;
        
        boolean focused = state.isSearchFocused();
        
        String searchText = state.getSearchText();
        String displayText = (focused && searchText.isEmpty()) ? "" : 
                            (searchText.isEmpty() ? "Search..." : searchText);
        int textColor = searchText.isEmpty() ? 
            theme.FONT.getRGB() : 
            theme.FONT_C.getRGB();
        
        int textX = (int)textStartX;
        int textY = (int)(searchY + 6);
        if (!displayText.isEmpty()) {
            context.drawString(client.font, displayText, textX, textY, textColor, false);
        }
        
        if (focused && System.currentTimeMillis() % 1000 < 500) {
            int cursorX = textX + client.font.width(displayText);
            context.fill(cursorX, textY, cursorX + 1, textY + 9, theme.FONT_C.getRGB());
        }
        
        float lineY = searchY + searchHeight - 1;
        int lineColor = new java.awt.Color(150, 150, 150, 200).getRGB();
        context.fill((int)searchX, (int)lineY, (int)(searchX + searchWidth), (int)(lineY + 1), lineColor);
    }
    
    private static void renderPlaceholder(GuiGraphics context, Minecraft client,
                                         Theme theme, float modY, float contentX, float contentWidth) {
        String placeholderText = "nothing~~~";
        int textWidth = client.font.width(placeholderText);
        int textX = (int)(contentX + (contentWidth - textWidth) / 2);
        int textY = (int)(modY + 50);
        context.drawString(client.font, placeholderText, textX, textY, theme.FONT.getRGB(), false);
    }
    
    private static void renderModule(GuiGraphics context, Minecraft client,
                                   Module module, Theme theme, ClickGuiState state,
                                   float x, float modY, float width,
                                   float mouseX, float mouseY,
                                   Function<String, String> getTooltipText,
                                   Function<String, net.minecraft.network.chat.Component> getTooltipTextWithColors,
                                   ModuleStyleRenderer.TooltipInfo tooltipInfo) {
        float x2 = x + (width - x);
        
        ModuleStyleRenderer.renderModule(context, client, module, theme,
                                        x, modY, x2, 25f,
                                        (float)mouseX, (float)mouseY,
                                        state.isListeningForKey(), state.getCurrentKeyDisplay(),
                                        getTooltipText, getTooltipTextWithColors, tooltipInfo);
    }
    
    private static float renderSubOptions(GuiGraphics context, Minecraft client,
                                         Module module, Theme theme, ClickGuiState state,
                                         float containerX1, float modY, float containerX2,
                                         float visibleHeight,
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

        int containerBg = com.shyeuar.baity.gui.theme.LinearTheme.BG_TERTIARY.getRGB();
        float containerY1 = modY;
        float containerY2 = modY + containerHeight;
        
        GuiRenderUtil.draw3DRect(context, containerX1, containerY1, containerX2, containerY2, containerBg, 0f);
        GuiRenderUtil.stroke1px(context, containerX1, containerY1, containerX2, containerY2,
                               com.shyeuar.baity.gui.theme.LinearTheme.BORDER_PRIMARY.getRGB());
        
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
                
                int subX1 = (int)(containerX1 + 4);
                int subX2 = (int)(containerX2 - 4);
                
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
                                       ClickGuiLayout.ScrollbarInfo info, float contentRight) {
        float barX1 = contentRight - 6;
        float barX2 = contentRight - 2;
        GuiRenderUtil.drawRoundedRect(context, barX1, info.barY, barX2, 
                                     info.barY + info.barHeight, 2, theme.BG_2.getRGB());
    }
    
    private static void renderWatermark(GuiGraphics context, Minecraft client, Theme theme) {
        String authorName = getAuthorRealName(client);
        String watermark = "By " + authorName + " (AKA raueyhs , shyeuar)";
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
    
    private static String getAuthorRealName(Minecraft client) {
        java.util.UUID authorUUID = java.util.UUID.fromString("8b8e7203-bdda-489e-bc20-f226f5b59c62");
        
        if (client.player == null || client.player.connection == null) {
            return "11YearCookieBuff";
        }
        
        try {
            net.minecraft.client.multiplayer.ClientPacketListener connection = client.player.connection;
            net.minecraft.client.multiplayer.PlayerInfo playerInfo = connection.getPlayerInfo(authorUUID);
            if (playerInfo != null) {
                com.mojang.authlib.GameProfile profile = playerInfo.getProfile();
                if (profile != null) {
                    try {
                        java.lang.reflect.Method getNameMethod = profile.getClass().getMethod("getName");
                        Object nameObj = getNameMethod.invoke(profile);
                        if (nameObj instanceof String) {
                            String name = (String) nameObj;
                            if (name != null && !name.isEmpty()) {
                                return name;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        
        try {
            java.util.Collection<net.minecraft.client.multiplayer.PlayerInfo> allPlayers = 
                client.player.connection.getOnlinePlayers();
            for (net.minecraft.client.multiplayer.PlayerInfo info : allPlayers) {
                com.mojang.authlib.GameProfile profile = info.getProfile();
                if (profile != null) {
                    try {
                        java.lang.reflect.Method getIdMethod = profile.getClass().getMethod("getId");
                        Object uuidObj = getIdMethod.invoke(profile);
                        if (uuidObj instanceof java.util.UUID && uuidObj.equals(authorUUID)) {
                            java.lang.reflect.Method getNameMethod = profile.getClass().getMethod("getName");
                            Object nameObj = getNameMethod.invoke(profile);
                            if (nameObj instanceof String) {
                                String name = (String) nameObj;
                                if (name != null && !name.isEmpty()) {
                                    return name;
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return "11YearCookieBuff";
    }
    
    private static void renderVersion(GuiGraphics context, Minecraft client, Theme theme, 
                                     ClickGuiState state, float mouseX, float mouseY) {
        String currentVersion = getModVersion();
        if (currentVersion == null || currentVersion.isEmpty()) {
            currentVersion = "v1.1.7";
        } else {
            if (!currentVersion.startsWith("v") && !currentVersion.startsWith("V")) {
                currentVersion = "v" + currentVersion;
            }
        }
        
        long currentTime = System.currentTimeMillis();

        float versionScale = 0.70f;
        int currentVersionWidth = client.font.width(currentVersion);
        float scaledCurrentVersionWidth = versionScale * currentVersionWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledCurrentVersionWidth - 8;
        float baseY = ClickGuiState.HEIGHT - (int)(client.font.lineHeight * versionScale) - 8;
        
        var matrices = context.pose();
        matrices.pushMatrix();
        matrices.scale(versionScale, versionScale);
        
        if (state.isVersionChecking()) {
            long startTime = state.getVersionCheckStartTime();
            if (startTime <= 0) {
                startTime = currentTime;
                state.setVersionCheckStartTime(currentTime);
            }
            long elapsed = currentTime - startTime;
            int phase = (int)((elapsed / 300) % 3);
            String checkingText = switch (phase) {
                case 1 -> "Checking..";
                case 2 -> "Checking...";
                default -> "Checking.";
            };

            int textWidth = client.font.width(checkingText);
            float scaledTextWidth = versionScale * textWidth;
            float renderX = baseX + scaledCurrentVersionWidth - scaledTextWidth;

            context.drawString(client.font, checkingText,
                    (int)(renderX / versionScale), (int)(baseY / versionScale),
                    0xFFFFFF00, false);

            matrices.popMatrix();
            return;
        }

        String displayText = currentVersion;
        boolean showFeedback = false;

        String checkStatus = state.getVersionCheckStatus();
        long startTime = state.getVersionCheckStartTime();
        boolean isError = false;
        if (checkStatus != null && startTime > 0 &&
            currentTime - startTime < 2000) {
            showFeedback = true;
            if ("latest".equals(checkStatus)) {
                displayText = "It's already the latest version！";
            } else if ("error".equals(checkStatus)) {
                String errorMsg = state.getLatestVersion();
                if (errorMsg != null && errorMsg.equals("Unknown error")) {
                    displayText = "Unknown error";
                    isError = true;
                } else {
                    displayText = "It's already the latest version！Network error！";
                    isError = true;
                }
            } else if ("update_available".equals(checkStatus)) {
                String latest = state.getLatestVersion();
                if (latest != null) {
                    displayText = "Available updates！Check " + latest + "！";
                } else {
                    displayText = "Available updates！";
                }
            }
        }

        int displayTextWidth = client.font.width(displayText);
        float scaledDisplayTextWidth = versionScale * displayTextWidth;
        float renderX = baseX + scaledCurrentVersionWidth - scaledDisplayTextWidth;

        boolean isHovered = false;
        int versionColor;

        if (showFeedback) {
            versionColor = 0xFFFFFF00;
        } else {
            float lineY = baseY + (int)(client.font.lineHeight * versionScale) + 1;
            float lineHeight = 1;
            isHovered = mouseX >= baseX && mouseX <= baseX + scaledCurrentVersionWidth &&
                       ((mouseY >= baseY && mouseY <= baseY + (int)(client.font.lineHeight * versionScale)) ||
                        (mouseY >= lineY && mouseY <= lineY + lineHeight));
            state.setVersionHovered(isHovered);
            versionColor = isHovered ? 0xFFFFFF00 : new java.awt.Color(120, 124, 132).getRGB();
        }

        if (showFeedback && "update_available".equals(state.getVersionCheckStatus())) {
            String latest = state.getLatestVersion();
            if (latest != null) {
                String prefix = "Available updates！Check ";
                String suffix = "！";
                int prefixWidth = client.font.width(prefix);
                int versionWidth = client.font.width(latest);

                context.drawString(client.font, prefix,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                0xFFFFFF00, false);
                context.drawString(client.font, latest,
                                (int)((renderX + prefixWidth * versionScale) / versionScale),
                                (int)(baseY / versionScale),
                                0xFF00FF00, false);
                context.drawString(client.font, suffix,
                                (int)((renderX + (prefixWidth + versionWidth) * versionScale) / versionScale),
                                (int)(baseY / versionScale),
                                0xFFFFFF00, false);
            } else {
                context.drawString(client.font, displayText,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                versionColor, false);
            }
        } else if (showFeedback && isError) {
            String errorMsg = state.getLatestVersion();
            if (errorMsg != null && errorMsg.equals("Unknown error")) {
                context.drawString(client.font, displayText,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                DevConfig.DEV_PREFIX_COLOR, false);
            } else {
                String prefix = "It's already the latest version！";
                String suffix = "Network error！";
                int prefixWidth = client.font.width(prefix);

                context.drawString(client.font, prefix,
                                (int)(renderX / versionScale), (int)(baseY / versionScale),
                                0xFFFFFF00, false);
                context.drawString(client.font, suffix,
                                (int)((renderX + prefixWidth * versionScale) / versionScale),
                                (int)(baseY / versionScale),
                                DevConfig.DEV_PREFIX_COLOR, false);
            }
        } else if (showFeedback) {
            context.drawString(client.font, displayText,
                            (int)(renderX / versionScale), (int)(baseY / versionScale),
                            versionColor, false);
        } else {
            context.drawString(client.font, displayText,
                            (int)(baseX / versionScale), (int)(baseY / versionScale),
                            versionColor, false);
        }

        matrices.popMatrix();

        if (!showFeedback) {
            float lineY = baseY + (int)(client.font.lineHeight * versionScale) + 1;
            float lineX1 = baseX;
            float lineX2 = baseX + scaledCurrentVersionWidth;
            int lineColor = (versionColor & 0xFFFFFF) | 0x64000000;
            context.fill((int)lineX1, (int)lineY, (int)lineX2, (int)lineY + 1, lineColor);
        } else if (showFeedback && "update_available".equals(state.getVersionCheckStatus())) {
            String latest = state.getLatestVersion();
            if (latest != null) {
                String prefix = "Available updates！Check ";
                int prefixWidth = client.font.width(prefix);
                int versionWidth = client.font.width(latest);

                float lineY = baseY + (int)(client.font.lineHeight * versionScale) + 1;
                float versionLineX1 = renderX + prefixWidth * versionScale;
                float versionLineX2 = versionLineX1 + versionWidth * versionScale;
                int lineColor = (0xFF00FF00 & 0xFFFFFF) | 0x64000000;
                context.fill((int)versionLineX1, (int)lineY, (int)versionLineX2, (int)lineY + 1, lineColor);
            }
        }
    }
    
    private static void updateVersionCheckStatus(ClickGuiState state) {
        long currentTime = System.currentTimeMillis();
        String checkStatus = state.getVersionCheckStatus();
        long startTime = state.getVersionCheckStartTime();
        if (checkStatus != null && startTime > 0 && 
            currentTime - startTime >= 2000) {
            state.setVersionCheckStatus(null);
            state.setLatestVersion(null);
        }
    }
    
    private static String getModVersion() {
        try {
            net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            java.util.Optional<net.fabricmc.loader.api.ModContainer> modContainer = loader.getModContainer("baity");
            if (modContainer.isPresent()) {
                return modContainer.get().getMetadata().getVersion().getFriendlyString();
            }
        } catch (Exception e) {
        }
        return null;
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
        float newValue = com.shyeuar.baity.gui.animation.InterpolationHelper.lerp(current, target, speed);
        
        if (expanded) {
            newValue = com.shyeuar.baity.gui.animation.EasingFunctions.easeOutCubic(newValue);
        } else {
            newValue = com.shyeuar.baity.gui.animation.EasingFunctions.easeInCubic(newValue);
        }
        
        if (Math.abs(newValue - target) < 0.01f) {
            newValue = target;
        }
        
        state.getModuleExpandAnimations().put(moduleName, newValue);
    }
    
    private static float getModuleExpandProgress(ClickGuiState state, String moduleName) {
        return state.getModuleExpandAnimations().getOrDefault(moduleName, 0.0f);
    }
    
    private static void updateModuleShimmerAnimations(ClickGuiState state, float mouseX, float mouseY) {
        List<Module> modules = getFilteredModules(state);
        if (modules.isEmpty()) return;
        
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        
        boolean mouseInContentArea = mouseX >= contentX && mouseX <= contentX + contentWidth &&
                                    mouseY >= contentY && mouseY <= ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        
        if (!mouseInContentArea) {
            for (Module module : modules) {
                ClickGuiState.ShimmerAnimationState animState = 
                    state.getModuleShimmerAnimations().getOrDefault(module.getName(), null);
                if (animState != null && animState.isActive) {
                    animState.isActive = false;
                    animState.isExiting = true;
                    animState.progress = 1f;
                    animState.lastUpdateTime = System.currentTimeMillis();
                }
            }
        }
        
        float modY = contentY + 10 - state.getScrollOffset();
        long currentTime = System.currentTimeMillis();
        
        for (Module module : modules) {
            float moduleX1 = contentX + 10;
            float moduleX2 = contentX + contentWidth - 10;
            float moduleY1 = modY;
            float moduleY2 = modY + 25;
            
            ClickGuiState.ShimmerAnimationState animState = 
                state.getModuleShimmerAnimations().computeIfAbsent(module.getName(), 
                    k -> new ClickGuiState.ShimmerAnimationState());
            
            boolean hovered = mouseInContentArea && mouseX >= moduleX1 && mouseY >= moduleY1 && 
                             mouseX <= moduleX2 && mouseY <= moduleY2;
            
            if (hovered) {
                if (!animState.isActive) {
                    if (animState.isExiting) {
                        animState.isExiting = false;
                        animState.progress = 0f;
                    }
                    
                    float moduleWidth = moduleX2 - moduleX1;
                    float mouseRelativeX = (mouseX - moduleX1) / moduleWidth;
                    mouseRelativeX = Math.max(0f, Math.min(1f, mouseRelativeX));
                    
                    float appearDistance = mouseRelativeX;
                    float exitDistance = 1f - mouseRelativeX;
                    
                    if (appearDistance <= 0.05f) {
                        animState.progress = 1f;
                        animState.isActive = true;
                        animState.appearSpeed = 0f;
                        animState.exitSpeed = exitDistance / 100f;
                        animState.direction = 1f;
                        animState.isExiting = false;
                        animState.lastUpdateTime = currentTime;
                    } else {
                        float targetTime = 100f;
                        
                        animState.appearSpeed = appearDistance / targetTime;
                        animState.exitSpeed = exitDistance / targetTime;
                        
                        if (animState.appearSpeed < 0.0001f) animState.appearSpeed = 0.0001f;
                        if (animState.exitSpeed < 0.0001f) animState.exitSpeed = 0.0001f;
                        
                        animState.direction = 1f;
                        animState.progress = 0f;
                        animState.isExiting = false;
                        animState.lastUpdateTime = currentTime;
                    }
                }
                animState.isActive = true;
                animState.mouseX = mouseX;
                animState.mouseY = mouseY;
                
                if (animState.progress < 1f) {
                    long elapsed = currentTime - animState.lastUpdateTime;
                    animState.progress = Math.min(1f, animState.progress + elapsed * animState.appearSpeed);
                } else {
                    animState.progress = 1f;
                }
                animState.lastUpdateTime = currentTime;
            } else {
                if (animState.isActive) {
                    animState.isActive = false;
                    animState.isExiting = true;
                    animState.progress = 1f;
                    animState.lastUpdateTime = currentTime;
                }
                
                if (animState.isExiting && animState.progress > 0f) {
                    long elapsed = currentTime - animState.lastUpdateTime;
                    animState.progress = Math.max(0f, animState.progress - elapsed * animState.exitSpeed);
                    
                    if (animState.progress <= 0f) {
                        animState.progress = 0f;
                        animState.isExiting = false;
                    }
                }
            }
            
            modY += 30;
            if (module.isExpanded()) {
                modY += getSubOptionContainerHeight(module);
            }
        }
    }
    
    private static float getSubOptionContainerHeight(Module module) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        if (subOptionCount == 0) return 0;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        int fullContainerHeight = subOptionCount * dims.subOptionHeight + dims.padding * 2 + extraHeight;
        return fullContainerHeight + 5;
    }
}
