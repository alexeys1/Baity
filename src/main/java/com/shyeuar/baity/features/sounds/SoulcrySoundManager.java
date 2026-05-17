package com.shyeuar.baity.features.sounds;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.PackMcmetaUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
public final class SoulcrySoundManager {
    public static final String MOD_ID = "baity";
    public static final String SOUND_OPEN = "atomsplit_soulcry_open";
    public static final String SOUND_CLOSE = "atomsplit_soulcry_close";
    public static final String TEMPLATE_DIR_NAME = "baity-custom-soulcry-sounds";
    public static final String DOCS_URL =
            "https://github.com/raueyhs/Baity/blob/baity-1.21.11/docs/custom-soulcry-sound.md";

    private static final String PURPLE = "#FF55FF";

    private static final String[] DEFAULT_OGG_RESOURCES = {
            "assets/baity/sounds/soulcry/atomsplit_soulcry_open.ogg",
            "assets/baity/sounds/soulcry/atomsplit_soulcry_close.ogg"
    };

    private SoulcrySoundManager() {}

    public static void init() {
        File templateRoot = templateRoot();
        if (!templateRoot.exists()) {
            createTemplatePack(templateRoot);
        } else {
            PackMcmetaUtils.write(new File(templateRoot, "pack.mcmeta"), packDescriptionJson());
        }
    }

    public static boolean isEnabled() {
        Module sounds = ModuleManager.getModuleByName("Sounds");
        return sounds != null && sounds.isEnabled();
    }

    public static void playOpen(Minecraft client) {
        play(client, SOUND_OPEN);
    }

    public static void playClose(Minecraft client) {
        play(client, SOUND_CLOSE);
    }

    private static void play(Minecraft client, String soundName) {
        if (!isEnabled() || client == null) return;
        SoundManager soundManager = client.getSoundManager();
        if (soundManager == null) return;
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, soundName);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        float volume = client.options.getSoundSourceVolume(SoundSource.MASTER);
        if (volume < 0f) volume = 0f;
        if (volume > 1f) volume = 1f;
        soundManager.play(SimpleSoundInstance.forUI(event, 1f, volume));
    }

    private static File configBaityDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("baity").toFile();
    }

    private static File templateRoot() {
        return new File(configBaityDir(), TEMPLATE_DIR_NAME);
    }

    private static void createTemplatePack(File root) {
        File soundsDir = new File(root, "assets/" + MOD_ID + "/sounds");
        soundsDir.mkdirs();
        PackMcmetaUtils.write(new File(root, "pack.mcmeta"), packDescriptionJson());
        writePackIcon(new File(root, "pack.png"));
        copyDefaultOggs(soundsDir);
        writeDefaultSoundsJson(new File(root, "assets/" + MOD_ID + "/sounds.json"));
    }

    private static void copyDefaultOggs(File soundsDir) {
        soundsDir.mkdirs();
        for (String resourcePath : DEFAULT_OGG_RESOURCES) {
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            File target = new File(soundsDir, fileName);
            try (InputStream in = SoulcrySoundManager.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) continue;
                try (FileOutputStream out = new FileOutputStream(target)) {
                    in.transferTo(out);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static void writeDefaultSoundsJson(File soundsJsonFile) {
        soundsJsonFile.getParentFile().mkdirs();
        String json = "{\n"
                + "  \"" + SOUND_OPEN + "\": { \"sounds\": [ { \"name\": \""
                + MOD_ID + ':' + SOUND_OPEN + "\", \"stream\": true } ] },\n"
                + "  \"" + SOUND_CLOSE + "\": { \"sounds\": [ { \"name\": \""
                + MOD_ID + ':' + SOUND_CLOSE + "\", \"stream\": true } ] }\n"
                + "}\n";
        try (FileOutputStream out = new FileOutputStream(soundsJsonFile)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static String packDescriptionJson() {
        return "[{\"text\":\"custom soulcry sound\",\"color\":\"" + PURPLE + "\"}]";
    }

    private static void writePackIcon(File target) {
        try (InputStream in = SoulcrySoundManager.class.getClassLoader()
                .getResourceAsStream("assets/baity/textures/gui/logo.png")) {
            if (in == null) return;
            try (FileOutputStream out = new FileOutputStream(target)) {
                in.transferTo(out);
            }
        } catch (Exception ignored) {
        }
    }
}
