package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.value.ModuleCategory;
import java.util.HashMap;
import java.util.Map;

public class ClickGuiState {
    private float windowX = 200;
    private float windowY = 200;
    public static final float WIDTH = 500;
    public static final float HEIGHT = 310;
    public static final float BASE_GUI_SCALE = 3.0f;
    
    public static final float SIDEBAR_WIDTH = 120f;
    public static final float CONTENT_WIDTH = WIDTH - SIDEBAR_WIDTH;
    public static final float HEADER_HEIGHT = 50f;
    public static final float FOOTER_HEIGHT = 20f;
    
    private float dragX = 0;
    private float dragY = 0;
    private boolean isDragging = false;
    
    private float guiScale = 1.0f;
    
    private ModuleCategory selectedCategory = ModuleCategory.MISC;
    
    private float scrollOffset = 0f;
    public static final float LIST_TOP_PADDING = 60f;
    public static final float ITEM_HEIGHT = 30f;
    
    private String searchText = "";
    private boolean isSearchFocused = false;
    
    private boolean isListeningForKey = false;
    private String currentKeyDisplay = "Right Ctrl";
    private String listeningButtonValueModule = null;
    private String listeningButtonValueName = null;
    
    private SliderDragInfo draggingSlider = null;
    private GradientDragInfo draggingGradient = null;
    
    private SliderInputInfo editingSlider = null;
    private String sliderInputText = "";
    
    private final Map<String, Float> moduleExpandAnimations = new HashMap<>();
    
    private final Map<String, ShimmerAnimationState> moduleShimmerAnimations = new HashMap<>();
    
    private String versionCheckStatus = null;
    private String latestVersion = null;
    private long versionCheckStartTime = 0;
    private boolean isVersionChecking = false;
    private boolean isVersionHovered = false;
    private boolean isAutoCheck = false;
    
    private String hoveredTooltip = null;
    private net.minecraft.network.chat.Component hoveredTooltipText = null;
    private int tooltipX = 0;
    private int tooltipY = 0;
    
    private int hudButtonX = 0;
    private int hudButtonY = 0;
    private int hudButtonWidth = 0;
    private int hudButtonHeight = 0;
    
    public float getWindowX() { return windowX; }
    public void setWindowX(float x) { windowX = x; }
    
    public float getWindowY() { return windowY; }
    public void setWindowY(float y) { windowY = y; }
    
    public float getDragX() { return dragX; }
    public void setDragX(float x) { dragX = x; }
    
    public float getDragY() { return dragY; }
    public void setDragY(float y) { dragY = y; }
    
    public boolean isDragging() { return isDragging; }
    public void setDragging(boolean dragging) { isDragging = dragging; }
    
    public float getGuiScale() { return guiScale; }
    public void setGuiScale(float scale) { guiScale = scale; }
    
    public ModuleCategory getSelectedCategory() { return selectedCategory; }
    public void setSelectedCategory(ModuleCategory category) { selectedCategory = category; }
    
    public float getScrollOffset() { return scrollOffset; }
    public void setScrollOffset(float offset) { scrollOffset = offset; }
    
    public boolean isListeningForKey() { return isListeningForKey; }
    public void setListeningForKey(boolean listening) { isListeningForKey = listening; }
    
    public String getCurrentKeyDisplay() { return currentKeyDisplay; }
    public void setCurrentKeyDisplay(String display) { currentKeyDisplay = display; }
    
    public String getListeningButtonValueName() { return listeningButtonValueName; }
    public void setListeningButtonValueName(String name) { listeningButtonValueName = name; }
    
    public String getListeningButtonValueModule() { return listeningButtonValueModule; }
    public void setListeningButtonValueModule(String module) { listeningButtonValueModule = module; }
    
    public void setListeningButtonValue(String moduleName, String valueName) {
        listeningButtonValueModule = moduleName;
        listeningButtonValueName = valueName;
    }
    
    public void clearListeningButtonValue() {
        listeningButtonValueModule = null;
        listeningButtonValueName = null;
    }
    
    public boolean isListeningForInput() {
        return isListeningForKey || listeningButtonValueName != null;
    }
    
    public Map<String, Float> getModuleExpandAnimations() { return moduleExpandAnimations; }
    
    public Map<String, ShimmerAnimationState> getModuleShimmerAnimations() { return moduleShimmerAnimations; }
    
    public String getHoveredTooltip() { return hoveredTooltip; }
    public void setHoveredTooltip(String tooltip) { hoveredTooltip = tooltip; }
    
    public net.minecraft.network.chat.Component getHoveredTooltipText() { return hoveredTooltipText; }
    public void setHoveredTooltipText(net.minecraft.network.chat.Component text) { hoveredTooltipText = text; }
    
    public int getTooltipX() { return tooltipX; }
    public void setTooltipX(int x) { tooltipX = x; }
    
    public int getTooltipY() { return tooltipY; }
    public void setTooltipY(int y) { tooltipY = y; }
    
    public void setHudButtonBounds(int x, int y, int width, int height) {
        hudButtonX = x;
        hudButtonY = y;
        hudButtonWidth = width;
        hudButtonHeight = height;
    }
    
    public boolean isHudButtonHovered(float mouseX, float mouseY) {
        return mouseX >= hudButtonX && mouseX <= hudButtonX + hudButtonWidth &&
               mouseY >= hudButtonY && mouseY <= hudButtonY + hudButtonHeight;
    }
    
    public String getVersionCheckStatus() { return versionCheckStatus; }
    public void setVersionCheckStatus(String status) { versionCheckStatus = status; }
    
    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String version) { latestVersion = version; }
    
    public long getVersionCheckStartTime() { return versionCheckStartTime; }
    public void setVersionCheckStartTime(long time) { versionCheckStartTime = time; }
    
    public boolean isVersionChecking() { return isVersionChecking; }
    public void setVersionChecking(boolean checking) { isVersionChecking = checking; }
    
    public boolean isVersionHovered() { return isVersionHovered; }
    public void setVersionHovered(boolean hovered) { isVersionHovered = hovered; }
    
    public boolean isAutoCheck() { return isAutoCheck; }
    public void setAutoCheck(boolean autoCheck) { isAutoCheck = autoCheck; }
    
    public String getSearchText() { return searchText; }
    public void setSearchText(String text) { searchText = text; }
    
    public boolean isSearchFocused() { return isSearchFocused; }
    public void setSearchFocused(boolean focused) { isSearchFocused = focused; }
    
    public void resetDragState() {
        dragX = 0;
        dragY = 0;
        isDragging = false;
    }
    
    public SliderDragInfo getDraggingSlider() { return draggingSlider; }
    public void setDraggingSlider(SliderDragInfo info) { draggingSlider = info; }
    public GradientDragInfo getDraggingGradient() { return draggingGradient; }
    public void setDraggingGradient(GradientDragInfo info) { draggingGradient = info; }
    
    public SliderInputInfo getEditingSlider() { return editingSlider; }
    public void setEditingSlider(SliderInputInfo info) { 
        editingSlider = info;
        if (info == null) {
            sliderInputText = "";
            originalSliderValue = null;
        }
    }
    
    public String getSliderInputText() { return sliderInputText; }
    public void setSliderInputText(String text) { sliderInputText = text; }
    
    public boolean isEditingSlider() { return editingSlider != null; }
    
    private Double originalSliderValue = null;
    public Double getOriginalSliderValue() { return originalSliderValue; }
    public void setOriginalSliderValue(Double value) { originalSliderValue = value; }
    
    public static class SliderDragInfo {
        public final String moduleName;
        public final String valueName;
        public final int sliderX;
        public final int sliderWidth;
        
        public SliderDragInfo(String moduleName, String valueName, int sliderX, int sliderWidth) {
            this.moduleName = moduleName;
            this.valueName = valueName;
            this.sliderX = sliderX;
            this.sliderWidth = sliderWidth;
        }
    }
    
    public static class SliderInputInfo {
        public final String moduleName;
        public final String valueName;
        
        public SliderInputInfo(String moduleName, String valueName) {
            this.moduleName = moduleName;
            this.valueName = valueName;
        }
    }

    public static class GradientDragInfo {
        public final String moduleName;
        public final String valueName;
        public final float mapX1;
        public final float mapY1;
        public final float mapX2;
        public final float mapY2;
        public final boolean dragValue; // true = dragging value slider; false = dragging hue/sat map

        public GradientDragInfo(String moduleName, String valueName, float mapX1, float mapY1, float mapX2, float mapY2) {
            this(moduleName, valueName, mapX1, mapY1, mapX2, mapY2, false);
        }

        public GradientDragInfo(String moduleName, String valueName, float mapX1, float mapY1, float mapX2, float mapY2, boolean dragValue) {
            this.moduleName = moduleName;
            this.valueName = valueName;
            this.mapX1 = mapX1;
            this.mapY1 = mapY1;
            this.mapX2 = mapX2;
            this.mapY2 = mapY2;
            this.dragValue = dragValue;
        }
    }
    
    private GradientInputInfo editingGradient = null;
    private String gradientInputText = "";
    
    public GradientInputInfo getEditingGradient() { return editingGradient; }
    public void setEditingGradient(GradientInputInfo info) {
        editingGradient = info;
        if (info == null) {
            gradientInputText = "";
        }
    }
    public boolean isEditingGradient() { return editingGradient != null; }
    public String getGradientInputText() { return gradientInputText; }
    public void setGradientInputText(String text) { gradientInputText = text; }
    
    public static class GradientInputInfo {
        public final String moduleName;
        public final String valueName;
        public final int lineIndex; // 0=start, 1=end
        public GradientInputInfo(String moduleName, String valueName, int lineIndex) {
            this.moduleName = moduleName;
            this.valueName = valueName;
            this.lineIndex = lineIndex;
        }
    }

    public static class ShimmerAnimationState {
        public boolean isActive = false;
        public float mouseX = 0f;
        public float mouseY = 0f;
        public float progress = 0f;
        public float direction = 1f;
        public long lastUpdateTime = System.currentTimeMillis();
        public boolean isExiting = false;
        public float appearSpeed = 0f;
        public float exitSpeed = 0f;
        
        public void reset() {
            isActive = false;
            isExiting = false;
            progress = 0f;
            direction = 1f;
            appearSpeed = 0f;
            exitSpeed = 0f;
        }
    }
}

