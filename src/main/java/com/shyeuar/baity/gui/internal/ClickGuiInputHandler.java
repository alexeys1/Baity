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
        
        if (handleCategoryClick(coords)) {
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
        
        if (GuiRenderUtil.isHovered(0, ClickGuiState.LIST_TOP_PADDING, 
                                    ClickGuiState.WIDTH, ClickGuiState.HEIGHT - 20, 
                                    coords.mouseX, coords.mouseY)) {
            float modY = 60 - state.getScrollOffset();
            List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
            
            for (Module module : modules) {
                if (module.isExpanded()) {
                    handleSubOptionScroll(module, modY, coords, verticalAmount);
                    modY += getSubOptionContainerHeight(module);
                }
                modY += 30;
            }
            
            float delta = (float)(-verticalAmount * 20);
            state.setScrollOffset(state.getScrollOffset() + delta);
            return true;
        }
        
        return false;
    }
    
    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
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
    
    private boolean handleCategoryClick(ClickGuiLayout.ScaledCoordinates coords) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        float cateX = 20;
        float cateY = 30;
        
        for (com.shyeuar.baity.gui.value.ModuleCategory category : 
             com.shyeuar.baity.gui.value.ModuleCategory.values()) {
            String label = category.getDisplayName();
            int textWidth = client.font.width(label);
            
            if (GuiRenderUtil.isHovered(cateX, cateY, cateX + textWidth, cateY + 12, 
                                       coords.mouseX, coords.mouseY) && 
                timer.delay(100)) {
                state.setSelectedCategory(category);
                timer.reset();
                return true;
            }
            cateX += textWidth + 28;
        }
        
        return false;
    }
    
    private boolean handleModuleAndSubOptionClick(ClickGuiLayout.ScaledCoordinates coords, int button) {
        float modY = 60 - state.getScrollOffset();
        List<Module> modules = ModuleManager.getModulesByCategory(state.getSelectedCategory());
        
        for (Module module : modules) {
            if (GuiRenderUtil.isHovered(20, modY, ClickGuiState.WIDTH - 20, modY + 25, 
                                       coords.mouseX, coords.mouseY) && 
                timer.delay(100)) {
                if (handleModuleClick(module, modY, coords, button)) {
                    timer.reset();
                    return true;
                }
            }
            
            modY += 30;
            
            if (module.isExpanded()) {
                if (handleSubOptionClick(module, modY, coords, button)) {
                    timer.reset();
                    return true;
                }
                modY += getSubOptionContainerHeight(module);
            }
        }
        
        return false;
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
                                        ClickGuiLayout.ScaledCoordinates coords, int button) {
        if (button != 0 || !timer.delay(100)) return false;
        
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return false;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING, extraHeight);
        
        int containerX1 = 30;
        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
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
                int subX2 = containerX2 - 4;
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
                
                int subX2 = containerX2 - 4;
                
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
                int containerX1Local = 30;
                int containerX2Local = (int)(ClickGuiState.WIDTH - 30);
                
                int hoveredIndex = com.shyeuar.baity.gui.render.ValueStyleRenderer.getHoveredColorIndex(
                    paletteValue, containerX1Local + 4, subModY, containerX2Local - 4, dims.subOptionHeight,
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
                subModY += dims.subOptionHeight;
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
                                         double verticalAmount) {
        int subOptionCount = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) subOptionCount++;
        }
        
        if (subOptionCount == 0) return false;
        
        int extraHeight = ClickGuiLayout.calculateExtraHeight(module);
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING, extraHeight);
        
        int containerX1 = 30;
        int containerX2 = (int)(ClickGuiState.WIDTH - 30);
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
        int extraHeight = 0;
        for (Value value : module.getValues()) {
            if (!"enabled".equals(value.getName())) {
                subOptionCount++;
                if (value.getStyle() == ValueStyle.COLOR_PALETTE) {
                    extraHeight += 20; 
                }
            }
        }
        
        if (subOptionCount == 0) return 0;
        
        ClickGuiLayout.ContainerDimensions dims = 
            ClickGuiLayout.calculateSubOptionContainer(subOptionCount, 
            ClickGuiState.HEIGHT - 20 - ClickGuiState.LIST_TOP_PADDING);
        return dims.height + 5 + extraHeight;
    }
    
    private void updateKeyDisplay() {
        state.setCurrentKeyDisplay(KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, ""));
    }
}

