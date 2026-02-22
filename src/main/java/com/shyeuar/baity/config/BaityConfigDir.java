package com.shyeuar.baity.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class BaityConfigDir {
    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/ConfigDir");
    private static Path baityConfigDir = null;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path configDir = gameDir.resolve("config");
        Path newBaityDir = configDir.resolve("baity");
        Path oldBaityDir = gameDir.resolve("baity");

        try {
            if (Files.exists(oldBaityDir) && Files.isDirectory(oldBaityDir)) {
                if (Files.exists(newBaityDir) && Files.isDirectory(newBaityDir)) {
                    LOGGER.warn("[ConfigDir] Both old and new baity directories exist. Merging files from old to new...");
                    mergeDirectories(oldBaityDir, newBaityDir);
                    deleteDirectory(oldBaityDir);
                    LOGGER.info("[ConfigDir] Merged and removed old baity directory");
                } else {
                    Files.createDirectories(configDir);
                    Files.move(oldBaityDir, newBaityDir, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("[ConfigDir] Migrated baity directory from game root to config/baity");
                }
            } else {
                if (!Files.exists(newBaityDir)) {
                    Files.createDirectories(newBaityDir);
                    LOGGER.debug("[ConfigDir] Created config/baity directory");
                }
            }
            baityConfigDir = newBaityDir;
        } catch (IOException e) {
            LOGGER.error("[ConfigDir] Failed to initialize config directory: {}", e.getMessage(), e);
            baityConfigDir = newBaityDir;
        }
    }

    private static void mergeDirectories(Path source, Path target) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }

        try (var stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    Path targetPath = target.resolve(source.relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                    } else {
                        if (!Files.exists(targetPath)) {
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("[ConfigDir] Failed to merge file: {}", e.getMessage());
                }
            });
        }
    }

    private static void deleteDirectory(Path dir) {
        try {
            try (var stream = Files.walk(dir)) {
                stream.sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOGGER.warn("[ConfigDir] Failed to delete: {}", path);
                        }
                    });
            }
        } catch (IOException e) {
            LOGGER.warn("[ConfigDir] Failed to delete directory: {}", e.getMessage());
        }
    }

    public static Path getBaityConfigDir() {
        if (baityConfigDir == null) {
            init();
        }
        return baityConfigDir;
    }

    public static Path getConfigDir() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        return gameDir.resolve("config");
    }
}
