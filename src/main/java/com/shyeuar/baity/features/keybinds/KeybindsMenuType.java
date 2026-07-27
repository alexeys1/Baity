package com.shyeuar.baity.features.keybinds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public enum KeybindsMenuType {
    WARDROBE(Pattern.compile("^\\(\\d+/\\d+\\) Armor Sets$", Pattern.CASE_INSENSITIVE)),
    EQUIPMENT(Pattern.compile("^\\(\\d+/\\d+\\) Equipment Sets$", Pattern.CASE_INSENSITIVE)),
    LOADOUT(Pattern.compile("^\\(\\d+/\\d+\\) Loadouts$", Pattern.CASE_INSENSITIVE)),
    NONE(null);

    private final Pattern titlePattern;

    KeybindsMenuType(Pattern titlePattern) {
        this.titlePattern = titlePattern;
    }

    public static KeybindsMenuType fromTitle(Component title) {
        if (title == null) {
            return NONE;
        }
        String plain = title.getString().trim();
        for (KeybindsMenuType type : values()) {
            if (type.titlePattern != null && type.titlePattern.matcher(plain).matches()) {
                return type;
            }
        }
        return NONE;
    }
}
