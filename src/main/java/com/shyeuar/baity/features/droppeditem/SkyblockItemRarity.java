package com.shyeuar.baity.features.droppeditem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public enum SkyblockItemRarity {
    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    EPIC("epic"),
    LEGENDARY("legendary"),
    MYTHIC("mythic"),
    DIVINE("divine"),
    SPECIAL("special"),
    VERY_SPECIAL("very special"),
    ULTIMATE("ultimate"),
    ADMIN("admin"),
    UNKNOWN("unknown");

    private final String sliderName;
    private final String loreToken;

    SkyblockItemRarity(String sliderName) {
        this.sliderName = sliderName;
        this.loreToken = name().replace('_', ' ');
    }

    public String sliderName() {
        return sliderName;
    }

    public boolean hasScaleSlider() {
        return this != UNKNOWN;
    }

    public static Optional<SkyblockItemRarity> fromLoreLine(String line) {
        if (line == null || line.isEmpty()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(r -> r != UNKNOWN)
                .filter(r -> line.contains(r.loreToken))
                .reduce((first, second) -> second);
    }

    public static SkyblockItemRarity fromTierName(String tier) {
        if (tier == null || tier.isEmpty()) {
            return UNKNOWN;
        }
        try {
            return valueOf(tier.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}