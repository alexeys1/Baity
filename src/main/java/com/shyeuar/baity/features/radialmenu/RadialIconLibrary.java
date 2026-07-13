package com.shyeuar.baity.features.radialmenu;

import com.mojang.blaze3d.platform.NativeImage;
import com.shyeuar.baity.config.BaityConfigDir;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public final class RadialIconLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/RadialIcons");
    private static final String ICON_DIR_NAME = "radial-icons";
    private static final Map<String, Identifier> DYNAMIC_TEXTURES = new HashMap<>();

    private static Path iconDir;
    private static boolean initialized;

    private RadialIconLibrary() {
    }

    public static void ensureInitialized() {
        if (initialized) {
            return;
        }
        initialized = true;
        iconDir = BaityConfigDir.getBaityConfigDir().resolve(ICON_DIR_NAME);
        try {
            Files.createDirectories(iconDir);
            seedIconsFromJar();
        } catch (IOException e) {
            LOGGER.warn("Failed to initialize radial icon directory: {}", e.toString());
        }
    }

    public static String normalizeIconName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static ItemStack resolveItemStack(String icon) {
        Item item = resolveItem(normalizeIconName(icon));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static Identifier resolveFileTexture(String icon) {
        if (icon == null || icon.isBlank()) {
            return null;
        }
        String normalized = normalizeIconName(icon);
        Path file = iconDir.resolve(normalized + ".png");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        return DYNAMIC_TEXTURES.computeIfAbsent(normalized, key -> registerTexture(key, file));
    }

    private static Item resolveItem(String normalized) {
        if (normalized.isEmpty()) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(normalized));
        if (item != null) {
            return item;
        }
        return BuiltInRegistries.ITEM.getValue(Identifier.tryParse(normalized));
    }

    private static Identifier registerTexture(String key, Path file) {
        Minecraft mc = Minecraft.getInstance();
        Identifier id = Identifier.fromNamespaceAndPath("baity", "radial_icon/" + key);
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            DynamicTexture texture = new DynamicTexture(() -> "baity_radial_icon_" + key, image);
            mc.getTextureManager().register(id, texture);
            return id;
        } catch (IOException e) {
            LOGGER.warn("Failed to load radial icon {}: {}", file, e.toString());
            return null;
        }
    }

    private static void seedIconsFromJar() throws IOException {
        var modContainer = FabricLoader.getInstance().getModContainer("baity");
        if (modContainer.isEmpty()) {
            return;
        }
        var rootPaths = modContainer.get().getRootPaths();
        for (Path root : rootPaths) {
            Path warpDir = root.resolve("assets/baity/textures/gui/warp");
            if (!Files.isDirectory(warpDir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(warpDir)) {
                for (Path png : files.toList()) {
                    if (!png.getFileName().toString().endsWith(".png")) {
                        continue;
                    }
                    String fileName = png.getFileName().toString();
                    Path target = iconDir.resolve(fileName);
                    if (!Files.exists(target)) {
                        Files.copy(png, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
}
