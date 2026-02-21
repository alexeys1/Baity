package com.shyeuar.baity.gui.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Environment(EnvType.CLIENT)
public class HudManager {
    private static final HudManager INSTANCE = new HudManager();
    
    private final List<HudElement> registeredElements = new CopyOnWriteArrayList<>();
    private HudElement activeSelection = null;
    
    private HudManager() {}
    
    public static HudManager getInstance() {
        return INSTANCE;
    }
    
    public void register(HudElement element) {
        if (element != null && !registeredElements.contains(element)) {
            registeredElements.add(element);
        }
    }
    
    public void unregister(HudElement element) {
        registeredElements.remove(element);
        if (activeSelection == element) {
            activeSelection = null;
        }
    }
    
    public List<HudElement> getElements() {
        return new ArrayList<>(registeredElements);
    }
    
    public HudElement getSelectedElement() {
        return activeSelection;
    }
    
    public void selectElement(HudElement element) {
        if (activeSelection != null) {
            activeSelection.setSelected(false);
        }
        activeSelection = element;
        if (element != null) {
            element.setSelected(true);
        }
    }
    
    public void deselectAll() {
        if (activeSelection != null) {
            activeSelection.setSelected(false);
            activeSelection = null;
        }
    }
    
}
