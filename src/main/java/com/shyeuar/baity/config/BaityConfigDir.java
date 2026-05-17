package com.shyeuar.baity.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BaityConfigDir {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/ConfigDir");
    private static Path baityConfigDir = null;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        Path configBaity = FabricLoader.getInstance().getConfigDir().resolve("baity");
        try {
            Files.createDirectories(configBaity);
            baityConfigDir = configBaity;
        } catch (IOException e) {
            LOGGER.warn("[ConfigDir] Failed to initialize config directory: {}", e.toString());
            baityConfigDir = configBaity;
        }
    }

    public static Path getBaityConfigDir() {
        if (baityConfigDir == null) {
            init();
        }
        return baityConfigDir;
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
