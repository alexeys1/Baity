package com.shyeuar.baity.gui;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.internal.ClickGuiLayout;
import com.shyeuar.baity.gui.internal.ClickGuiInputHandler;
import com.shyeuar.baity.gui.owo.ClickGuiRootComponent;
import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.theme.Theme;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.tooltip.TooltipManager;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.utils.TimerUtils;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class ClickGui extends BaseOwoScreen<FlowLayout> {
    
    private final ClickGuiState state;
    private final TimerUtils valuetimer;
    private final ClickGuiInputHandler inputHandler;
    private ClickGuiRootComponent rootComponent;
    
    public static Theme theme = new Theme();
    private final com.shyeuar.baity.gui.render.ModuleStyleRenderer.TooltipInfo tooltipInfo = 
        new com.shyeuar.baity.gui.render.ModuleStyleRenderer.TooltipInfo();
    
    private static ClickGui currentInstance;
    
    public static ClickGui getInstance() {
        return currentInstance;
    }
    
    public ClickGui() {
        super(Component.literal("Baity ClickGui"));
        this.state = new ClickGuiState();
        this.valuetimer = new TimerUtils();
        this.inputHandler = new ClickGuiInputHandler(state, valuetimer, this::handleTriggerValueClick);
        currentInstance = this;
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::horizontalFlow);
    }
    
    @Override
    protected void init() {
        super.init();
        theme.setDark();
        
        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }
        
        if (this.minecraft != null) {
            int guiScaleOption = this.minecraft.options.guiScale().get();
            float actualGuiScale = (guiScaleOption <= 0) 
                ? this.minecraft.getWindow().getGuiScale() 
                : guiScaleOption;
            state.setGuiScale(actualGuiScale);
        }
        
        ConfigSynchronizer.syncModuleStates();
        updateKeyDisplay();
        
        if (this.minecraft != null && this.minecraft.getWindow() != null) {
            int screenW = this.minecraft.getWindow().getGuiScaledWidth();
            int screenH = this.minecraft.getWindow().getGuiScaledHeight();
            ClickGuiLayout.initializeWindowPosition(state, screenW, screenH);
        }
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        this.rootComponent = new ClickGuiRootComponent(state, theme,
            this::getTooltipText,
            this::getTooltipTextWithColors,
            this::getDisplayTextFormatter,
            tooltipInfo);
        rootComponent.child(this.rootComponent);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (rootComponent != null) {
            rootComponent.setGuiGraphics(graphics);
        }
        
        super.render(graphics, mouseX, mouseY, delta);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inputHandler.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        if (inputHandler.handleMouseClick(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, isInsideWindow);
    }
    
    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        inputHandler.handleMouseRelease(click.button());
        return super.mouseReleased(click);
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        inputHandler.handleMouseMove(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }
    
    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.input();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (inputHandler.handleKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(input);
    }
    
    @Override
    public boolean charTyped(CharacterEvent input) {
        char chr = (char) input.codepoint();
        if (inputHandler.handleCharTyped(chr, input.modifiers())) {
            return true;
        }
        return super.charTyped(input);
    }
    
    @Override
    public void resize(Minecraft client, int width, int height) {
        super.resize(client, width, height);
        if (this.minecraft != null && this.minecraft.getWindow() != null) {
            int screenW = this.minecraft.getWindow().getGuiScaledWidth();
            int screenH = this.minecraft.getWindow().getGuiScaledHeight();
            ClickGuiLayout.initializeWindowPosition(state, screenW, screenH);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public void onClose() {
        if (state.isEditingSlider()) {
            ClickGuiState.SliderInputInfo editInfo = state.getEditingSlider();
            if (editInfo != null && state.getOriginalSliderValue() != null) {
                for (Module module : ModuleManager.getModules()) {
                    if (!module.getName().equals(editInfo.moduleName)) continue;
                    for (com.shyeuar.baity.gui.value.Value value : module.getValues()) {
                        if (value instanceof com.shyeuar.baity.gui.value.SliderValue && value.getName().equals(editInfo.valueName)) {
                            com.shyeuar.baity.gui.value.SliderValue sliderValue = (com.shyeuar.baity.gui.value.SliderValue) value;
                            sliderValue.setValue(state.getOriginalSliderValue());
                            break;
                        }
                    }
                }
            }
            state.setEditingSlider(null);
        }
        super.onClose();
    }
    
    private String getTooltipText(String name) {
        return TooltipManager.getTooltipText(name);
    }
    
    private net.minecraft.network.chat.Component getTooltipTextWithColors(String name) {
        return TooltipManager.getTooltipTextWithColors(name);
    }
    
    private String getDisplayTextFormatter(Object value) {
        if (value instanceof Integer) {
            int keyCode = (Integer) value;
            return com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(keyCode, "");
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : "☄ NOTSET";
    }
    
    private void updateKeyDisplay() {
        state.setCurrentKeyDisplay(com.shyeuar.baity.utils.KeyMappingUtils.formatKeyDisplay(ConfigManager.guiKeyCode, ""));
    }
    
    private void handleTriggerValueClick(Module module, ButtonValue buttonValue) {
        String valueName = buttonValue.getName();
        
    }
    
    public boolean isListeningForInput() {
        return state.isListeningForInput();
    }
}

