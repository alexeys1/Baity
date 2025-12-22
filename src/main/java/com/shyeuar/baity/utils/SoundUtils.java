package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SoundUtils {

    private static final Identifier WOODEN_BUTTON_ID = Identifier.of("baity", "wooden_button");
    private static final Identifier BUBBLE_ID = Identifier.of("baity", "bubble");

    public static SoundEvent WOODEN_BUTTON;
    public static SoundEvent BUBBLE;

    public static void registerSounds() {
        WOODEN_BUTTON = Registry.register(Registries.SOUND_EVENT, WOODEN_BUTTON_ID, SoundEvent.of(WOODEN_BUTTON_ID));
        BUBBLE = Registry.register(Registries.SOUND_EVENT, BUBBLE_ID, SoundEvent.of(BUBBLE_ID));
    }

    public static void playWoodenButton() {
        playSound(WOODEN_BUTTON, 1.0f, 1.0f);
    }

    public static void playBubble() {
        playSound(BUBBLE, 0.25f, 1.0f);
    }

    private static void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
        }
    }
}
