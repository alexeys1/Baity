package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.render.GuiRenderUtil;
import com.shyeuar.baity.gui.value.Value;
import com.shyeuar.baity.gui.value.ValueStyle;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.value.SliderValue;
import com.shyeuar.baity.gui.value.ValueTypeRegistry;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.TimerUtils;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.SoundUtils;
import com.shyeuar.baity.utils.VersionCheckUtils;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;

public class ClickGuiInputHandler {
    
    private final ClickGuiState state;
    private final TimerUtils timer;
    private final BiConsumer<com.shyeuar.baity.gui.module.Module, com.shyeuar.baity.gui.value.ButtonValue> onTriggerValueClick;
    
    public ClickGuiInputHandler(ClickGuiState state, TimerUtils timer, 
                               BiConsumer<com.shyeuar.baity.gui.module.Module, com.shyeuar.baity.gui.value.ButtonValue> onTriggerValueClick) {
        this.state = state;
        this.timer = timer;
        this.onTriggerValueClick = onTriggerValueClick;
    }
   
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if ((state.isListeningForKey() || state.getListeningButtonValueName() != null) && button >= 2 && button <= 4) {
            return handleMouseKeybindBinding(button);
        }
        
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        if (state.isEditingSlider() && button == 0) {
            ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
            if (editInfo != null) {
                boolean clickedOnValueDisplay = false;
                float modY = 60 - state.getScrollOffset();
                List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
                for (Module module : modules) {
                    if (!module.getName().equals(editInfo.moduleName)) {
                        if (module.isExpanded()) {
                            modY += getSubOptionContainerHeight(module);
                        }
                        modY += 30;
                        continue;
                    }
                    if (module.isExpanded()) {
                        int subOptionCount = 0;
                        for (Value v : module.getValues()) {
                            if (!"enabled".equals(v.getName())) subOptionCount++;
                        }
                        if (subOptionCount == 0) break;
                        
                        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
                        ClickGuiLayout.ContainerDimensions dims = 
                            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
                            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING, extraHeight);
                        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
                        float subModY = modY + dims.padding;
                        
                        for (Value v : module.getValues()) {
                            if ("enabled".equals(v.getName())) continue;
                            if (v instanceof SliderValue && v.getName().equals(editInfo.valueName)) {
                                SliderValue sv = (SliderValue) v;
                                int resetBoxWidth = 30;
                                int resetBoxX = containerX2 - 4 - resetBoxWidth - 6;
                                String valueText = sv.getFormattedValue();
                                int valueTextWidth = Minecraft.getInstance().font.width(valueText);
                                int valueDisplayWidth = Math.max(valueTextWidth + 8, 35);
                                int valueDisplayX = resetBoxX - valueDisplayWidth - 8;
                                int valueDisplayY = (int)(subModY + 2);
                                int valueDisplayHeight = dims.subOptionHeight - 4;
                                
                                if (GuiRenderUtil.isHovered(valueDisplayX, valueDisplayY, 
                                    valueDisplayX + valueDisplayWidth, valueDisplayY + valueDisplayHeight, 
                                    coords.mouseX, coords.mouseY)) {
                                    clickedOnValueDisplay = true;
                                }
                                break;
                            }
                            subModY += dims.subOptionHeight;
                        }
                    }
                    break;
                }
                
                if (!clickedOnValueDisplay) {
                    cancelSliderInput();
                }
            }
        }
        
        if (button == 0 && handleWindowDrag(coords, mouseX, mouseY)) {
            return true;
        }
        
        if (handleSearchInput(coords, button)) {
            return true;
        }
        
        if (handleCategoryClick(coords)) {
            return true;
        }
        
        if (handleGitHubIconClick(coords, button)) {
            return true;
        }
        
        if (handleVersionUpdateClick(coords, button)) {
            return true;
        }
        
        if (handleVersionClick(coords, button)) {
            return true;
        }
        
        return handleModuleAndSubOptionClick(coords, button);
    }
    
    private void cancelSliderInput() {
        if (state.isEditingSlider()) {
            ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
            if (editInfo != null && state.getOriginalSliderValue() != null) {
                for (Module module : ModuleManager.getModules()) {
                    if (!module.getName().equals(editInfo.moduleName)) continue;
                    for (Value value : module.getValues()) {
                        if (value instanceof SliderValue && value.getName().equals(editInfo.valueName)) {
                            SliderValue sliderValue = (SliderValue) value;
                            sliderValue.setValue(state.getOriginalSliderValue());
                            break;
                        }
                    }
                }
            }
            state.setEditingSlider(null);
        }
    }
    
    public boolean handleMouseScroll(double mouseX, double mouseY, double verticalAmount) {
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        float visibleBottom = ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        float visibleHeight = visibleBottom - contentY;
        
        if (GuiRenderUtil.isHovered(contentX, contentY, contentX + contentWidth, visibleBottom, 
                                    coords.mouseX, coords.mouseY)) {
            List<Module> modules = getFilteredModules();
            
            float calculatedContentHeight = ClickGuiLayout.calculateContentHeightForModules(modules, visibleHeight);
            float contentStartY = ClickGuiState.HEADER_HEIGHT + 10;
            float contentEndY = ClickGuiState.HEADER_HEIGHT + visibleHeight;
            float maxScroll = Math.max(0, calculatedContentHeight + contentStartY - contentEndY);
            
            float delta = (float)(-verticalAmount * 20);
            
            float clampedDelta = ClickGuiLayout.clampScrollDelta(state, maxScroll, delta);
            
            if (clampedDelta != 0) {
                state.setScrollOffset(state.getScrollOffset() + clampedDelta);
                
                float modY = contentY + 10 - state.getScrollOffset();
                for (Module module : modules) {
                    modY += 30;
                    if (module.isExpanded()) {
                        handleSubOptionScroll(module, modY, coords, verticalAmount, contentX, contentWidth);
                        modY += getSubOptionContainerHeight(module);
                    }
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (state.isSearchFocused()) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                String current = state.getSearchText();
                if (!current.isEmpty()) {
                    state.setSearchText(current.substring(0, current.length() - 1));
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                state.setSearchFocused(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                state.setSearchFocused(false);
                return true;
            }
        }
        
        if (state.isListeningForKey()) {
            return handleClickGuiKeybindInput(keyCode);
        }
        
        if (state.getListeningButtonValueName() != null) {
            return handleButtonValueKeybindInput(keyCode);
        }
        
        if (state.isEditingSlider()) {
            return handleSliderInput(keyCode);
        }
        
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (state.isListeningForInput()) {
                state.setListeningForKey(false);
                state.setListeningButtonValueName(null);
                return true;
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        
        return false;
    }
    
    public boolean handleCharTyped(char chr, int modifiers) {
        if (state.isEditingSlider()) {
            String current = state.getSliderInputText();
            if (Character.isDigit(chr) || chr == '.' || chr == '-') {
                if (chr == '-' && !current.isEmpty()) return true;
                if (chr == '.' && current.contains(".")) return true;
                state.setSliderInputText(current + chr);
            }
            return true;
        }
        
        if (state.isSearchFocused()) {
            if (chr >= 32 && chr < 127) {
                state.setSearchText(state.getSearchText() + chr);
                return true;
            }
        }
        
        return false;
    }
    
    private boolean handleSliderInput(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            cancelSliderInput();
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            String current = state.getSliderInputText();
            if (!current.isEmpty()) {
                state.setSliderInputText(current.substring(0, current.length() - 1));
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
            if (editInfo != null) {
                String inputText = state.getSliderInputText();
                try {
                    double newValue = Double.parseDouble(inputText);
                    for (Module module : ModuleManager.getModules()) {
                        if (!module.getName().equals(editInfo.moduleName)) continue;
                        for (Value value : module.getValues()) {
                            if (value instanceof SliderValue && value.getName().equals(editInfo.valueName)) {
                                SliderValue sliderValue = (SliderValue) value;
                                if (sliderValue.trySetValue(newValue)) {
                                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), sliderValue.getValue());
                                    }
                                }
                                break;
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            state.setEditingSlider(null);
            state.setSliderInputText("");
            return true;
        }
        
        return false;
    }
    
    public void handleMouseRelease(int button) {
        if (button == 0) {
            state.resetDragState();
            state.setDraggingSlider(null);
        }
    }
   
    public void handleMouseMove(double mouseX, double mouseY) {
        if (state.isDragging()) {
            ClickGuiLayout.updateWindowPosition(state, mouseX, mouseY, state.getDragX(), state.getDragY());
        }
        
        if (state.getDraggingSlider() != null) {
            handleSliderDrag(mouseX, mouseY);
        }
    }
    
    private void handleSliderDrag(double mouseX, double mouseY) {
        ClickGuiState.SliderDragInfo dragInfo = state.getDraggingSlider();
        if (dragInfo == null) return;
        
        ClickGuiLayout.ScaledCoordinates coords = ClickGuiLayout.getScaledCoordinates(state, mouseX, mouseY);
        
        for (Module module : ModuleManager.getModules()) {
            if (!module.getName().equals(dragInfo.moduleName)) continue;
            
            for (Value value : module.getValues()) {
                if (value instanceof SliderValue && value.getName().equals(dragInfo.valueName)) {
                    SliderValue sliderValue = (SliderValue) value;
                    
                    double percentage = (coords.mouseX - dragInfo.sliderX) / (double) dragInfo.sliderWidth;
                    percentage = Math.max(0, Math.min(1, percentage));
                    sliderValue.setFromPercentage(percentage);
                    
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), sliderValue.getValue());
                    }
                    return;
                }
            }
        }
    }
    
    private boolean handleMouseKeybindBinding(int mouseKeyCode) {
        if (state.isListeningForKey()) {
            ConfigManager.guiKeyCode = mouseKeyCode;
            ConfigManager.saveConfig();
            updateKeyDisplay();
            state.setListeningForKey(false);
            return true;
        }
        
        if (state.getListeningButtonValueName() != null) {
            String listeningModule = state.getListeningButtonValueModule();
            String listeningName = state.getListeningButtonValueName();
            Module module = ModuleManager.getModuleByName(listeningModule);
            if (module != null) {
                for (Value value : module.getValues()) {
                    if (value instanceof ButtonValue && value.getName().equals(listeningName)) {
                        ButtonValue buttonValue = (ButtonValue) value;
                        buttonValue.setValue(mouseKeyCode);
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), mouseKeyCode);
                        }
                        state.clearListeningButtonValue();
                        return true;
                    }
                }
            }
            state.clearListeningButtonValue();
            return true;
        }
        
        return false;
    }
    
    private boolean handleWindowDrag(ClickGuiLayout.ScaledCoordinates coords, double mouseX, double mouseY) {
        if (GuiRenderUtil.isHovered(0, 0, ClickGuiState.WIDTH, 20, coords.mouseX, coords.mouseY)) {
            if (state.getDragX() == 0 && state.getDragY() == 0) {
                state.setDragX(coords.mouseX);
                state.setDragY(coords.mouseY);
            } else {
                ClickGuiLayout.updateWindowPosition(state, mouseX, mouseY, state.getDragX(), state.getDragY());
            }
            state.setDragging(true);
            return true;
        }
        return false;
    }
    
    private boolean handleVersionClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        long currentTime = System.currentTimeMillis();
        boolean inFeedback = state.getVersionCheckStatus() != null && 
                           currentTime - state.getVersionCheckStartTime() < 2000;
        
        if (inFeedback) {
            return false;
        }
        
        String currentVersion = getModVersion();
        if (currentVersion == null || currentVersion.isEmpty()) {
            currentVersion = "v1.1.7";
        } else {
            if (!currentVersion.startsWith("v") && !currentVersion.startsWith("V")) {
                currentVersion = "v" + currentVersion;
            }
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        float versionScale = 0.70f;
        int versionRawWidth = client.font.width(currentVersion);
        float scaledWidth = versionScale * versionRawWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledWidth - 8;
        float baseY = ClickGuiState.HEIGHT - 8 - (int)(client.font.lineHeight * versionScale) - 2;
        float baseHeight = (int)(client.font.lineHeight * versionScale);
        float lineY = baseY + baseHeight + 1;
        float lineHeight = 1;
        
        float expandedBottom = lineY + lineHeight + 3;
        if (coords.mouseX >= baseX && coords.mouseX <= baseX + scaledWidth &&
            coords.mouseY >= baseY && coords.mouseY <= expandedBottom) {
            
            if (state.isVersionChecking()) {
                return true;
            }
            
            state.setVersionChecking(true);
            state.setVersionCheckStatus(null);
            state.setLatestVersion(null);
            state.setVersionCheckStartTime(System.currentTimeMillis());
            Minecraft mc = client;
            VersionCheckUtils.checkVersionAsync(currentVersion).thenAccept(result -> {
                if (mc != null && mc.level != null) {
                    mc.schedule(() -> {
                        state.setVersionChecking(false);
                        if (result.hasError) {
                            state.setVersionCheckStatus("error");
                            state.setVersionCheckStartTime(System.currentTimeMillis());
                            return;
                        }
                        
                        if (result.isLatest) {
                            state.setVersionCheckStatus("latest");
                        } else {
                            state.setVersionCheckStatus("update_available");
                            state.setLatestVersion(result.latestVersion);
                        }
                        state.setVersionCheckStartTime(System.currentTimeMillis());
                    });
                } else {
                    state.setVersionChecking(false);
                }
            }).exceptionally(throwable -> {
                if (client != null && client.level != null) {
                    client.schedule(() -> {
                        state.setVersionChecking(false);
                    });
                } else {
                    state.setVersionChecking(false);
                }
                return null;
            });
            
            return true;
        }
        
        return false;
    }
    
    private boolean handleVersionUpdateClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        long currentTime = System.currentTimeMillis();
        if (state.getVersionCheckStatus() == null || 
            !"update_available".equals(state.getVersionCheckStatus()) ||
            currentTime - state.getVersionCheckStartTime() >= 2000) {
            return false;
        }
        
        String latest = state.getLatestVersion();
        if (latest == null || latest.isEmpty()) {
            return false;
        }
        
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        float versionScale = 0.70f;
        String prefix = "Available updates！Check ";
        String suffix = "！";
        String displayText = prefix + latest + suffix;
        int displayTextWidth = client.font.width(displayText);
        float scaledWidth = versionScale * displayTextWidth;
        
        float baseX = ClickGuiState.WIDTH - scaledWidth - 8;
        float baseY = ClickGuiState.HEIGHT - 8 - (int)(client.font.lineHeight * versionScale) - 2;
        float baseHeight = (int)(client.font.lineHeight * versionScale);
        
        int prefixWidth = client.font.width(prefix);
        float scaledPrefixWidth = versionScale * prefixWidth;
        float versionX = baseX + scaledPrefixWidth;
        float versionY = baseY;
        float scaledVersionWidth = versionScale * client.font.width(latest);
        
        if (coords.mouseX >= versionX && coords.mouseX <= versionX + scaledVersionWidth &&
            coords.mouseY >= versionY && coords.mouseY <= versionY + baseHeight) {
            
            try {
                net.minecraft.Util.getPlatform().openUri(new java.net.URI("https://github.com/raueyhs/Baity/releases"));
                return true;
            } catch (Exception e) {
                if (client.player != null) {
                    client.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("无法打开浏览器，请手动访问: https://github.com/raueyhs/Baity/releases"),
                        false
                    );
                }
                return true;
            }
        }
        
        return false;
    }
    
    private String getModVersion() {
        try {
            net.fabricmc.loader.api.FabricLoader loader = net.fabricmc.loader.api.FabricLoader.getInstance();
            java.util.Optional<net.fabricmc.loader.api.ModContainer> modContainer = loader.getModContainer("baity");
            if (modContainer.isPresent()) {
                String version = modContainer.get().getMetadata().getVersion().getFriendlyString();
                if (!version.startsWith("v") && !version.startsWith("V")) {
                    version = "v" + version;
                }
                return version;
            }
        } catch (Exception e) {
        }
        return "v1.1.7";
    }
    
    private boolean handleSearchInput(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        float searchX = ClickGuiState.SIDEBAR_WIDTH + 20;
        float searchY = 15;
        float searchWidth = ClickGuiState.CONTENT_WIDTH - 40;
        float searchHeight = 20;
        
        if (GuiRenderUtil.isHovered(searchX, searchY, searchX + searchWidth, searchY + searchHeight, 
                                    coords.mouseX, coords.mouseY)) {
            state.setSearchFocused(true);
            return true;
        } else {
            state.setSearchFocused(false);
        }
        
        return false;
    }
    
    private boolean handleGitHubIconClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0) return false;
        
        int iconSize = 20;
        int padding = 8;
        int iconX = padding;
        int iconY = (int)(ClickGuiState.HEIGHT - iconSize - padding);
        
        if (coords.mouseX >= iconX && coords.mouseX < iconX + iconSize &&
            coords.mouseY >= iconY && coords.mouseY < iconY + iconSize) {
            
            try {
                net.minecraft.Util.getPlatform().openUri(new java.net.URI("https://github.com/raueyhs/Baity"));
                return true;
            } catch (Exception e) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("无法打开浏览器，请手动访问: https://github.com/raueyhs/Baity"),
                        false
                    );
                }
                return true;
            }
        }
        
        return false;
    }
    
    private boolean handleCategoryClick(ClickGuiLayout.ScaledCoordinates coords) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        float categoryY = ClickGuiState.HEADER_HEIGHT + 20;
        float categorySpacing = 35f;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
             com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            boolean hovered = coords.mouseX >= 0 && coords.mouseX < ClickGuiState.SIDEBAR_WIDTH &&
                            coords.mouseY >= categoryY - 5 && coords.mouseY < categoryY + 25;
            
            if (hovered && timer.delay(100)) {
                state.setSelectedCategory(category);
                state.setSearchText("");
                state.setSearchFocused(false);
                timer.reset();
                return true;
            }
            
            categoryY += categorySpacing;
        }
        
        return false;
    }
    
    private boolean handleModuleAndSubOptionClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        float contentX = ClickGuiState.SIDEBAR_WIDTH;
        float contentY = ClickGuiState.HEADER_HEIGHT;
        float contentWidth = ClickGuiState.CONTENT_WIDTH;
        
        if (coords.mouseX < contentX || coords.mouseX >= contentX + contentWidth ||
            coords.mouseY < contentY || coords.mouseY >= ClickGuiState.HEIGHT - ClickGuiState.FOOTER_HEIGHT) {
            return false;
        }
        
        List<Module> modules = getFilteredModules();
        float modY = contentY + 10 - state.getScrollOffset();
        
        for (Module module : modules) {
            float moduleX1 = contentX + 10;
            float moduleX2 = contentX + contentWidth - 10;
            if (GuiRenderUtil.isHovered(moduleX1, modY, moduleX2, modY + 25, 
                                       coords.mouseX, coords.mouseY) && 
                timer.delay(100)) {
                if (handleModuleClick(module, modY, coords, button)) {
                    timer.reset();
                    return true;
                }
            }
            
            modY += 30;
            
            if (module.isExpanded()) {
                if (handleSubOptionClick(module, modY, coords, button, contentX, contentWidth)) {
                    timer.reset();
                    return true;
                }
                modY += getSubOptionContainerHeight(module);
            }
        }
        
        return false;
    }
    
    private List<Module> getFilteredModules() {
        String searchText = state.getSearchText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            return ModuleManager.getModulesByCategory(state.getSelectedCategory());
        }
        
        return ModuleManager.getModules().stream()
                .filter(module -> module.getName().toLowerCase().contains(searchText))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private boolean handleModuleClick(Module module, float modY, 
                                     ClickGuiLayout.ScaledCoordinates coords, int button) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        if ("ClickGUI".equals(module.getName())) {
            String keyText = state.isListeningForKey() ? "Press a key..." : state.getCurrentKeyDisplay();
            String plainText = keyText.replaceAll("§[0-9a-fklmnor]", "");
            int keyTextWidth = client.font.width(plainText);
            int keyBoxWidth = keyTextWidth + 16;
            float boxCenterY = modY + 25 / 2f;
            int boxHeight = 12;
            int containerX2 = (int)(ClickGuiState.WIDTH - 20);
            int keyBoxX1 = (int)(containerX2 - keyBoxWidth - 10);
            int keyBoxY1 = (int)(boxCenterY - boxHeight / 2f);
            int keyBoxX2 = (int)(containerX2 - 10);
            int keyBoxY2 = (int)(boxCenterY + boxHeight / 2f);
            
            if (button == 0 && GuiRenderUtil.isHovered(keyBoxX1, keyBoxY1, keyBoxX2, keyBoxY2, 
                                                      coords.mouseX, coords.mouseY)) {
                state.setListeningForKey(true);
                timer.reset();
                return true;
            }
        } else {
            boolean hasChildrenClick = false;
            for (Value v : module.getValues()) {
                if (!"enabled".equals(v.getName())) {
                    hasChildrenClick = true;
                    break;
                }
            }
            
            if (button == 0) {
                if (hasChildrenClick && 
                    GuiRenderUtil.isHovered(ClickGuiState.WIDTH - 35, modY, 
                                          ClickGuiState.WIDTH - 15, modY + 25, 
                                          coords.mouseX, coords.mouseY) && 
                    timer.delay(100)) {
                    module.toggleExpanded();
                } else {
                    module.toggle();
                    SoundUtils.playBubble();
                    if (ConfigSynchronizer.hasModuleConfig(module.getName())) {
                        ConfigSynchronizer.handleModuleToggle(module.getName(), module.isEnabled());
                    }
                }
            } else if (button == 1 && hasChildrenClick) {
                module.toggleExpanded();
            }
            
            return true;
        }
        
        return false;
    }
    
    private boolean handleSubOptionClick(Module module, float modY, 
                                        ClickGuiLayout.ScaledCoordinates coords, int button,
                                        float contentX, float contentWidth) {
        if (button != 0 || !timer.delay(100)) return false;
        
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return false;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        
        float containerX1 = contentX + 20;
        float containerX2 = contentX + contentWidth - 20;
        float subModY = modY + dims.padding;
        
        int containerHeight = dims.height;
        Value previousValue = null;
        
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            
            if (subModY > modY + containerHeight - dims.padding) {
                break;
            }
            
            if (value.needsSeparatorBefore(previousValue)) {
                subModY += 12;
            }
            
            ValueStyle style = value.getStyle();
            if (style == ValueStyle.BUTTON_LIKE && value instanceof ButtonValue) {
                ButtonValue buttonValue = (ButtonValue) value;
                
                String boxText = buttonValue.getDisplayText(val -> {
                    if (val instanceof Integer) {
                        int keyCode = (Integer) val;
                        return com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(keyCode, "");
                    }
                    return val != null ? val.toString() : "☄ NOTSET";
                });
                String plainText = boxText.replaceAll("§[0-9a-fklmnor]", "");
                Minecraft client = Minecraft.getInstance();
                if (client == null) return false;
                
                int boxTextWidth = client.font.width(plainText);
                int boxWidth = boxTextWidth + 16;
                float boxCenterY = subModY + dims.subOptionHeight / 2f;
                int boxHeight = 12;
                int subX2 = (int)(containerX2 - 4);
                int boxX1 = (int)(subX2 - boxWidth - 10);
                int boxY1 = (int)(boxCenterY - boxHeight / 2f);
                int boxX2 = (int)(subX2 - 10);
                int boxY2 = (int)(boxCenterY + boxHeight / 2f);
                
                if (GuiRenderUtil.isHovered(boxX1, boxY1, boxX2, boxY2, coords.mouseX, coords.mouseY)) {
                    if (buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.KEYBIND) {
                        state.setListeningButtonValue(module.getName(), value.getName());
                        timer.reset();
                        return true;
                    } else if (buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.TRIGGER ||
                               buttonValue.getButtonValueType() == ButtonValue.ButtonValueType.FONT_SELECTOR) {
                        if (onTriggerValueClick != null) {
                            onTriggerValueClick.accept(module, buttonValue);
                        }
                        timer.reset();
                        return true;
                    }
                }
            } else if (style == ValueStyle.SLIDER && value instanceof SliderValue) {
                SliderValue sliderValue = (SliderValue) value;
                Minecraft client = Minecraft.getInstance();
                if (client == null) return false;
                
                int subX2 = (int)(containerX2 - 4);
                
                int resetBoxWidth = 30;
                int resetBoxHeight = 12;
                int resetBoxX = subX2 - resetBoxWidth - 6;
                int resetBoxY = (int)(subModY + (dims.subOptionHeight - resetBoxHeight) / 2);
                
                int resetClickMargin = 2;
                if (GuiRenderUtil.isHovered(
                    resetBoxX - resetClickMargin, 
                    resetBoxY - resetClickMargin, 
                    resetBoxX + resetBoxWidth + resetClickMargin, 
                    resetBoxY + resetBoxHeight + resetClickMargin, 
                    coords.mouseX, coords.mouseY)) {
                    sliderValue.resetToDefault();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), sliderValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
                
                String valueText = sliderValue.getFormattedValue();
                int valueTextWidth = client.font.width(valueText);
                int valueDisplayWidth = Math.max(valueTextWidth + 8, 35);
                int valueDisplayX = resetBoxX - valueDisplayWidth - 8;
                int valueDisplayY = (int)(subModY + 2);
                int valueDisplayHeight = dims.subOptionHeight - 4;
                
                if (GuiRenderUtil.isHovered(valueDisplayX, valueDisplayY, valueDisplayX + valueDisplayWidth, valueDisplayY + valueDisplayHeight, coords.mouseX, coords.mouseY)) {
                    state.setEditingSlider(new ClickGuiState.SliderInputInfo(module.getName(), value.getName()));
                    state.setSliderInputText(sliderValue.getFormattedValue());
                    state.setOriginalSliderValue(sliderValue.getDoubleValue());
                    timer.reset();
                    return true;
                }
                
                int sliderWidth = 80;
                int sliderHeight = 10;
                int sliderX = valueDisplayX - sliderWidth - 10;
                int sliderY = (int)(subModY + (dims.subOptionHeight - sliderHeight) / 2);
                
                if (GuiRenderUtil.isHovered(sliderX - 5, sliderY - 3, sliderX + sliderWidth + 5, sliderY + sliderHeight + 3, coords.mouseX, coords.mouseY)) {
                    double percentage = (coords.mouseX - sliderX) / (double) sliderWidth;
                    percentage = Math.max(0, Math.min(1, percentage));
                    sliderValue.setFromPercentage(percentage);
                    
                    state.setDraggingSlider(new ClickGuiState.SliderDragInfo(module.getName(), value.getName(), sliderX, sliderWidth));
                    
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), sliderValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
            } else if (style == ValueStyle.COLOR_PALETTE && value instanceof com.shyeuar.baity.gui.value.ColorPaletteValue) {
                com.shyeuar.baity.gui.value.ColorPaletteValue paletteValue = (com.shyeuar.baity.gui.value.ColorPaletteValue) value;
                
                int hoveredIndex = com.shyeuar.baity.gui.render.ValueStyleRenderer.getHoveredColorIndex(
                    paletteValue, containerX1 + 4, subModY, containerX2 - 4, dims.subOptionHeight,
                    coords.mouseX, coords.mouseY);
                
                if (hoveredIndex >= 0) {
                    paletteValue.toggleColor(hoveredIndex);
                    SoundUtils.playBubble();
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), paletteValue.getValue());
                    }
                    timer.reset();
                    return true;
                }
            } else {
                if (GuiRenderUtil.isHovered(containerX1 + 4, (int)subModY, 
                                           containerX2 - 4, (int)(subModY + dims.subOptionHeight), 
                                           coords.mouseX, coords.mouseY)) {
                    if (value.getValue() instanceof Boolean) {
                        value.setValue(!((Boolean)value.getValue()));
                        SoundUtils.playBubble();
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), value.getValue());
                        }
                    }
                    timer.reset();
                    return true;
                }
            }
            
            float currentHeight = dims.subOptionHeight;
            if (style == ValueStyle.COLOR_PALETTE) {
                currentHeight = dims.subOptionHeight * 2;
            }
            subModY += currentHeight;
            previousValue = value;
        }
        
        return false;
    }
    
    private boolean handleSubOptionScroll(Module module, float modY, 
                                         ClickGuiLayout.ScaledCoordinates coords, 
                                         double verticalAmount,
                                         float contentX, float contentWidth) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return false;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        float visibleHeight = ClickGuiState.HEIGHT - ClickGuiState.HEADER_HEIGHT - ClickGuiState.FOOTER_HEIGHT;
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, visibleHeight, extraHeight);
        
        float containerX1 = contentX + 20;
        float containerX2 = contentX + contentWidth - 20;
        float subModY = modY + dims.padding;
        int containerHeight = dims.height;
        
        if (state.getDraggingSlider() != null) {
            return false;
        }
        
        Value previousValue = null;
        for (Value value : module.getValues()) {
            if ("enabled".equals(value.getName())) continue;
            
            if (subModY > modY + containerHeight - dims.padding) {
                break;
            }
            
            if (value.needsSeparatorBefore(previousValue)) {
                subModY += 12;
            }
            
            Object currentVal = value.getValue();
            var handler = ValueTypeRegistry.getHandlerForValue(currentVal);
            
            if (handler != null && 
                GuiRenderUtil.isHovered(containerX1 + 4, (int)subModY, 
                                       containerX2 - 4, (int)(subModY + dims.subOptionHeight), 
                                       coords.mouseX, coords.mouseY)) {
            }
            
            float currentHeight = dims.subOptionHeight;
            if (value.getStyle() == ValueStyle.COLOR_PALETTE) {
                currentHeight = dims.subOptionHeight * 2;
            }
            subModY += currentHeight;
            previousValue = value;
        }
        
        return false;
    }
    
    private boolean handleClickGuiKeybindInput(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            state.setListeningForKey(false);
            return true;
        }
        
        if (KeyMappingUtils.isResetKey(keyCode)) {
            ConfigManager.guiKeyCode = 0;
            ConfigManager.saveConfig();
            updateKeyDisplay();
            state.setListeningForKey(false);
            return true;
        }
        
        if (!KeyMappingUtils.isKeySupported(keyCode)) {
            return false;
        }
        
        ConfigManager.guiKeyCode = keyCode;
        ConfigManager.saveConfig();
        updateKeyDisplay();
        state.setListeningForKey(false);
        return true;
    }
    
    private boolean handleButtonValueKeybindInput(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            state.clearListeningButtonValue();
            return true;
        }
        
        String listeningModule = state.getListeningButtonValueModule();
        String listeningName = state.getListeningButtonValueName();
        
        if (KeyMappingUtils.isResetKey(keyCode)) {
            Module module = ModuleManager.getModuleByName(listeningModule);
            if (module != null) {
                for (Value value : module.getValues()) {
                    if (value instanceof ButtonValue && value.getName().equals(listeningName)) {
                        ButtonValue buttonValue = (ButtonValue) value;
                        buttonValue.setValue(0);
                        if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                            ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), 0);
                        }
                        state.clearListeningButtonValue();
                        return true;
                    }
                }
            }
            state.clearListeningButtonValue();
            return true;
        }
        
        if (!KeyMappingUtils.isKeySupported(keyCode)) {
            return false;
        }
        
        Module module = ModuleManager.getModuleByName(listeningModule);
        if (module != null) {
            for (Value value : module.getValues()) {
                if (value instanceof ButtonValue && value.getName().equals(listeningName)) {
                    ButtonValue buttonValue = (ButtonValue) value;
                    buttonValue.setValue(keyCode);
                    if (ConfigSynchronizer.hasValueConfig(module.getName(), value.getName())) {
                        ConfigSynchronizer.handleValueUpdate(module.getName(), value.getName(), keyCode);
                    }
                    state.clearListeningButtonValue();
                    return true;
                }
            }
        }
        
        state.clearListeningButtonValue();
        return true;
    }
    
    private int getSubOptionContainerHeight(Module module) {
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
        
        float expandProgress = state.getModuleExpandAnimations().getOrDefault(module.getName(), 0.0f);
        int containerHeight = (int)(fullContainerHeight * expandProgress);
        
        return containerHeight + 5;
    }
    
    private void updateKeyDisplay() {
        state.setCurrentKeyDisplay(KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, ""));
    }
}

