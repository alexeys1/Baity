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
    private static final String SOUNDS_CACHE_DIR_NAME = "sounds-cache";
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
                moveOldBaitySoundsCacheToGameRoot(gameDir, oldBaityDir);
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
            LOGGER.warn("[ConfigDir] Failed to initialize config directory: {}", e.toString());
            try {
                Files.createDirectories(newBaityDir);
                baityConfigDir = newBaityDir;
            } catch (IOException e2) {
                LOGGER.error("[ConfigDir] Could not create config/baity after failure: {}", e2.toString());
                baityConfigDir = newBaityDir;
            }
        }

        try {
            ensureGameRootSoundsCacheDir(gameDir);
        } catch (IOException e) {
            LOGGER.warn("[ConfigDir] Could not ensure game-root sounds cache dir: {}", e.getMessage());
        }
    }

    private static void ensureGameRootSoundsCacheDir(Path gameDir) throws IOException {
        Path dir = gameDir.resolve("baity").resolve(SOUNDS_CACHE_DIR_NAME);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            LOGGER.debug("[ConfigDir] Created {}", dir);
        }
    }

    private static void moveOldBaitySoundsCacheToGameRoot(Path gameDir, Path oldBaityDir) throws IOException {
        Path src = oldBaityDir.resolve(SOUNDS_CACHE_DIR_NAME);
        if (!Files.exists(src) || !Files.isDirectory(src)) {
            return;
        }
        Path destRoot = gameDir.resolve("baity");
        Path dest = destRoot.resolve(SOUNDS_CACHE_DIR_NAME);
        if (src.toAbsolutePath().normalize().equals(dest.toAbsolutePath().normalize())) {
            return;
        }
        Files.createDirectories(destRoot);
        if (Files.exists(dest)) {
            deleteDirectory(dest);
        }
        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("[ConfigDir] Moved {} from legacy baity folder to {}", SOUNDS_CACHE_DIR_NAME, dest);
    }

    private static void mergeDirectories(Path source, Path target) throws IOException {
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }

        try (var stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    if (isUnderSoundsCacheOnly(source, sourcePath)) {
                        return;
                    }
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

    private static boolean isUnderSoundsCacheOnly(Path sourceRoot, Path path) {
        Path rel = sourceRoot.relativize(path);
        if (rel.getNameCount() == 0) {
            return false;
        }
        return SOUNDS_CACHE_DIR_NAME.equalsIgnoreCase(rel.getName(0).toString());
    }
}
