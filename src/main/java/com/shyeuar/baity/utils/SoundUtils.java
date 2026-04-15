package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

@Environment(EnvType.CLIENT)
public class SoundUtils {

    private static final Identifier WOODEN_BUTTON_ID = Identifier.fromNamespaceAndPath("baity", "wooden_button");
    private static final Identifier BUBBLE_ID = Identifier.fromNamespaceAndPath("baity", "bubble");

    public static SoundEvent WOODEN_BUTTON;
    public static SoundEvent BUBBLE;

    public static void registerSounds() {
        WOODEN_BUTTON = Registry.register(BuiltInRegistries.SOUND_EVENT, WOODEN_BUTTON_ID, SoundEvent.createVariableRangeEvent(WOODEN_BUTTON_ID));
        BUBBLE = Registry.register(BuiltInRegistries.SOUND_EVENT, BUBBLE_ID, SoundEvent.createVariableRangeEvent(BUBBLE_ID));
    }

    public static void playWoodenButton() {
        playSound(WOODEN_BUTTON, 1.0f, 1.0f);
    }

    public static void playBubble() {
        playSound(BUBBLE, 0.25f, 1.0f);
    }

    private static void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }
}
