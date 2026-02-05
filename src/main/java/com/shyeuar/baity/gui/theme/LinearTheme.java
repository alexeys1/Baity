package com.shyeuar.baity.gui.theme;

import java.awt.Color;

public class LinearTheme {
    public static final Color BG_PRIMARY = new Color(18, 18, 20);
    public static final Color BG_SECONDARY = new Color(28, 28, 32);
    public static final Color BG_TERTIARY = new Color(26, 26, 30);
    public static final Color BG_SIDEBAR = new Color(22, 22, 24);
    public static final Color BG_ACTIVE = new Color(35, 35, 40);
    public static final Color BG_HOVER = new Color(40, 40, 45);
    
    public static final Color ACCENT_PRIMARY = new Color(98, 74, 255);
    public static final Color ACCENT_SECONDARY = new Color(138, 104, 255);
    
    public static final Color TEXT_PRIMARY = new Color(245, 245, 248);
    public static final Color TEXT_SECONDARY = new Color(200, 200, 210);
    public static final Color TEXT_TERTIARY = new Color(150, 150, 160);
    
    public static final Color BORDER_PRIMARY = new Color(60, 60, 70);
    public static final Color BORDER_ACCENT = new Color(98, 74, 255);

    public static void applyToTheme(Theme theme) {
        theme.BG = BG_PRIMARY;
        theme.BG_2 = BG_SECONDARY;
        theme.BG_3 = ACCENT_PRIMARY;
        theme.Modules = BG_TERTIARY;
        theme.FONT = TEXT_SECONDARY;
        theme.FONT_C = TEXT_PRIMARY;
    }
}
