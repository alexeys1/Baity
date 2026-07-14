package com.shyeuar.baity.features.radialmenu;

import com.mojang.blaze3d.platform.NativeImage;
import com.shyeuar.baity.config.BaityConfigDir;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
    private static final Map<String, String[]> BLOCK_SPRITE_CANDIDATES = Map.of(
            "fire", new String[]{"fire_0", "fire"},
            "soul_fire", new String[]{"soul_fire_0", "soul_fire"},
            "nether_portal", new String[]{"nether_portal"},
            "end_portal", new String[]{"end_portal", "obsidian"},
            "end_gateway", new String[]{"end_gateway", "obsidian"},
            "water", new String[]{"water_still", "water_flow"},
            "lava", new String[]{"lava_still", "lava_flow"}
    );

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

    public static boolean isResolvableIcon(String icon) {
        if (icon == null || icon.isBlank()) {
            return false;
        }
        if (resolveFileTexture(icon) != null) {
            return true;
        }
        if (!resolveItemStack(icon).isEmpty()) {
            return true;
        }
        return resolveBlockSprite(icon) != null;
    }

    public static ItemStack resolveItemStack(String icon) {
        String normalized = normalizeIconName(icon);
        Item item = resolveItem(normalized);
        if (item != null) {
            return new ItemStack(item);
        }
        Block block = resolveBlock(normalized);
        if (block != null) {
            Item blockItem = block.asItem();
            if (blockItem != null && blockItem != Items.AIR) {
                return new ItemStack(blockItem);
            }
        }
        return ItemStack.EMPTY;
    }

    public static TextureAtlasSprite resolveBlockSprite(String icon) {
        String normalized = normalizeIconName(icon);
        Block block = resolveBlock(normalized);
        if (block == null) {
            return null;
        }
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == null) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getAtlasManager() == null) {
            return null;
        }

        String[] candidates = BLOCK_SPRITE_CANDIDATES.getOrDefault(blockId.getPath(), new String[]{blockId.getPath()});
        for (String path : candidates) {
            Identifier textureId = Identifier.fromNamespaceAndPath(blockId.getNamespace(), path);
            SpriteId spriteId = Sheets.BLOCKS_MAPPER.apply(textureId);
            TextureAtlasSprite sprite = mc.getAtlasManager().get(spriteId);
            if (!isMissingSprite(sprite)) {
                return sprite;
            }
        }
        return null;
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
        Identifier id = parseId(normalized);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) {
            return null;
        }
        return item;
    }

    private static Block resolveBlock(String normalized) {
        Identifier id = parseId(normalized);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        return block;
    }

    private static Identifier parseId(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (normalized.indexOf(':') >= 0) {
            return Identifier.tryParse(normalized);
        }
        return Identifier.withDefaultNamespace(normalized);
    }

    private static boolean isMissingSprite(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return true;
        }
        Identifier name = sprite.contents().name();
        return name != null && "missingno".equals(name.getPath());
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
