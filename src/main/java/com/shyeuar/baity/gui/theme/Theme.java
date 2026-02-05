package com.shyeuar.baity.gui.theme;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.awt.*;

@Environment(EnvType.CLIENT)
public class Theme {
    public Color BG   = new Color(18, 18, 20); 
    public Color BG_2 = new Color(28, 28, 32);
    public Color BG_3 = new Color(98, 74, 255);
    public Color Modules = new Color(26, 26, 30);
    public Color FONT_C = new Color(245, 245, 248);
    public Color FONT   = new Color(164, 168, 176);
    
    public void setDark(){
        LinearTheme.applyToTheme(this);
    }
}