package com.shyeuar.baity.features.sounds;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

@Environment(EnvType.CLIENT)
public final class SoundsHooks {
    private static volatile short[] cachedPitched = null;
    private static volatile short[] cachedPitchedInverted = null;
    private static volatile int cachedSampleRate = 0;
    private static volatile CompletableFuture<Void> cacheBuildFuture = null;
    private static final float LISTENER_RELATIVE_BEHIND_Z = -1.0f;
    private static final String ATOMSPLIT_ID = "ATOMSPLIT_KATANA";
    private static final String ITEM_MODEL_DIAMOND_SWORD = "minecraft:diamond_sword";
    private static final String ITEM_MODEL_GOLDEN_SWORD = "minecraft:golden_sword";
    private static final long SOULCRY_DURATION_MS = 4000L;
    private static final int PCM_CACHE_VERSION = 16;
    private static final String PCM_CLOSE_FILE = "atomsplit_katana_close_v1.pcm16";
    private static final String PCM_OPEN_FILE = "atomsplit_katana_open_v1.pcm16";
    private static final float SOULCRY_VOLUME_MULTIPLIER = 1.0f;
    private static final String PCM_CACHE_SUBDIR = "sounds-cache";
    private static boolean lastMainHandIsAtomsplit = false;
    private static boolean lastMainHandModelDiamond = false;
    private static boolean lastMainHandModelGold = false;
    private static long scheduledSoulcryEndAt = 0L;
    private static long pendingOpenUntilAt = 0L;
    private static long pendingCloseAt = 0L;
    private static boolean prewarmStarted = false;

    private SoundsHooks() {}

    public static void prewarm(Minecraft client) {
        if (prewarmStarted) return;
        if (client == null) return;
        prewarmStarted = true;
        ensureCachedAsync(client);
    }

    public static void tick(Minecraft client) {
        if (client == null) return;
        if (client.player == null) return;

        long now = System.currentTimeMillis();

        processMainHandState(client);

        if (pendingOpenUntilAt > 0L) {
            if (isCacheReady()) {
                playOpenSound(currentGain(client));
                scheduledSoulcryEndAt = now + SOULCRY_DURATION_MS;
                pendingOpenUntilAt = 0L;
                pendingCloseAt = 0L;
            } else if (now > pendingOpenUntilAt) {
                pendingOpenUntilAt = 0L;
            }
        }

        if (scheduledSoulcryEndAt > 0L && now >= scheduledSoulcryEndAt) {
            if (isCacheReady()) {
                playCloseSound(currentGain(client));
                scheduledSoulcryEndAt = 0L;
            } else {
                pendingCloseAt = scheduledSoulcryEndAt;
                scheduledSoulcryEndAt = 0L;
            }
        }
        if (pendingCloseAt > 0L && now >= pendingCloseAt && isCacheReady()) {
            playCloseSound(currentGain(client));
            pendingCloseAt = 0L;
        }

        RawAlSoundPlayer.tickCleanup();
    }

    private static void processMainHandState(Minecraft client) {
        if (!isFeatureEnabled()) return;

        ItemStack stack = client.player.getMainHandItem();
        if (stack == null || stack.isEmpty()) {
            lastMainHandIsAtomsplit = false;
            lastMainHandModelDiamond = false;
            lastMainHandModelGold = false;
            return;
        }

        String skyblockId = readSkyblockId(stack);
        boolean isAtomsplit = ATOMSPLIT_ID.equalsIgnoreCase(skyblockId);
        if (!isAtomsplit) {
            lastMainHandIsAtomsplit = false;
            lastMainHandModelDiamond = false;
            lastMainHandModelGold = false;
            return;
        }

        String itemModel = readItemModel(stack);
        boolean isDiamond = ITEM_MODEL_DIAMOND_SWORD.equals(itemModel);
        boolean isGold = ITEM_MODEL_GOLDEN_SWORD.equals(itemModel);
        if (!isDiamond && !isGold) {
            lastMainHandIsAtomsplit = true;
            lastMainHandModelDiamond = false;
            lastMainHandModelGold = false;
            return;
        }

        if (isGold && lastMainHandIsAtomsplit && !lastMainHandModelGold && lastMainHandModelDiamond) {
            ensureCachedAsync(client);
            if (isCacheReady()) {
                playOpenSound(currentGain(client));
                scheduledSoulcryEndAt = System.currentTimeMillis() + SOULCRY_DURATION_MS;
                pendingOpenUntilAt = 0L;
                pendingCloseAt = 0L;
            } else {
                pendingOpenUntilAt = System.currentTimeMillis() + 1500L;
            }
        }

        lastMainHandIsAtomsplit = true;
        lastMainHandModelDiamond = isDiamond;
        lastMainHandModelGold = isGold;
    }

    private static boolean isCacheReady() {
        return cachedPitched != null && cachedPitchedInverted != null && cachedSampleRate > 0;
    }

    private static void ensureCachedAsync(Minecraft client) {
        if (isCacheReady()) return;

        int sourceHash = readOggSourceHash();
        if (sourceHash != 0 && tryLoadPersistentPcm(sourceHash)) return;

        CompletableFuture<Void> f = cacheBuildFuture;
        if (f != null) return;

        Identifier id = Identifier.fromNamespaceAndPath("baity", "sounds/affectionate_scream.ogg");
        byte[] bytes;
        try {
            var res = client.getResourceManager().getResource(id);
            if (res.isEmpty()) return;
            try (InputStream in = res.get().open()) {
                bytes = in.readAllBytes();
            }
        } catch (Throwable t) {
            return;
        }

        cacheBuildFuture = CompletableFuture.runAsync(() -> {
            try {
                OggPcmProcessor.Decoded decoded = OggPcmProcessor.decodeBytes(bytes);
                if (decoded == null) return;
                int sr = decoded.sampleRate();
                float[] pitched = OggPcmProcessor.resampleRate(decoded.mono(), 1.9f);
                float[] closeF = OggPcmProcessor.addCaveReverb(OggPcmProcessor.extendTail(pitched, sr), sr);
                short[] close = OggPcmProcessor.toPcm16(closeF);
                float[] openF = OggPcmProcessor.addCaveReverb(OggPcmProcessor.invertPitchContour(closeF, sr), sr);
                short[] open = OggPcmProcessor.toPcm16(openF);
                cachedSampleRate = sr;
                cachedPitched = close;
                cachedPitchedInverted = open;
                if (sourceHash != 0) {
                    persistPcmCache(sourceHash);
                }
            } catch (Throwable t) {
            } finally {
                cacheBuildFuture = null;
            }
        });
    }

    private static float currentGain(Minecraft client) {
        if (client == null || client.options == null) return 1.0f;
        float master = client.options.getSoundSourceVolume(SoundSource.MASTER);
        if (master < 0.0f) master = 0.0f;
        if (master > 1.0f) master = 1.0f;
        return master;
    }

    private static void playOpenSound(float gain) {
        float finalGain = gain * SOULCRY_VOLUME_MULTIPLIER;
        RawAlSoundPlayer.playMono16(
            cachedPitchedInverted,
            cachedSampleRate,
            finalGain,
            0.0f,
            0.0f,
            LISTENER_RELATIVE_BEHIND_Z,
            true
        );
    }

    private static void playCloseSound(float gain) {
        float finalGain = gain * SOULCRY_VOLUME_MULTIPLIER;
        RawAlSoundPlayer.playMono16(
            cachedPitched,
            cachedSampleRate,
            finalGain,
            0.0f,
            0.0f,
            LISTENER_RELATIVE_BEHIND_Z,
            true
        );
    }

    private static String readSkyblockId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return "";
            CompoundTag extraAttributes = customData.copyTag();
            if (extraAttributes == null || !extraAttributes.contains("id")) return "";
            return extraAttributes.getString("id").orElse("");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String readItemModel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        Identifier itemModel = stack.get(DataComponents.ITEM_MODEL);
        return itemModel != null ? itemModel.toString() : "";
    }

    private static boolean isFeatureEnabled() {
        Module sounds = ModuleManager.getModuleByName("Sounds");
        if (sounds == null || !sounds.isEnabled()) return false;
        return ModuleUtils.getOptionBoolean(sounds, "restore soulcry sound of atomsplit katana", false);
    }

    private static boolean tryLoadPersistentPcm(int sourceHash) {
        return tryLoadPersistentPcmFromDir(pcmCacheDir(), sourceHash);
    }

    private static boolean tryLoadPersistentPcmFromDir(Path dir, int sourceHash) {
        Path closePath = dir.resolve(PCM_CLOSE_FILE);
        Path openPath = dir.resolve(PCM_OPEN_FILE);
        if (!Files.exists(closePath) || !Files.exists(openPath)) return false;
        try {
            PersistedPcm closeData = readPersisted(closePath);
            PersistedPcm openData = readPersisted(openPath);
            if (closeData == null || openData == null) return false;
            if (closeData.version != PCM_CACHE_VERSION || openData.version != PCM_CACHE_VERSION) return false;
            if (closeData.sourceHash != sourceHash || openData.sourceHash != sourceHash) return false;
            if (closeData.sampleRate <= 0 || openData.sampleRate <= 0) return false;
            if (closeData.sampleRate != openData.sampleRate) return false;
            if (closeData.pcm.length == 0 || openData.pcm.length == 0) return false;
            cachedSampleRate = closeData.sampleRate;
            cachedPitched = closeData.pcm;
            cachedPitchedInverted = openData.pcm;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void persistPcmCache(int sourceHash) {
        if (cachedSampleRate <= 0 || cachedPitched == null || cachedPitchedInverted == null) return;
        Path dir = pcmCacheDir();
        try {
            Files.createDirectories(dir);
            writePersisted(dir.resolve(PCM_CLOSE_FILE), cachedPitched, cachedSampleRate, sourceHash);
            writePersisted(dir.resolve(PCM_OPEN_FILE), cachedPitchedInverted, cachedSampleRate, sourceHash);
        } catch (Throwable t) {
        }
    }

    private static Path pcmCacheDir() {
        return FabricLoader.getInstance().getGameDir().resolve("baity").resolve(PCM_CACHE_SUBDIR);
    }

    private static void writePersisted(Path path, short[] pcm, int sampleRate, int sourceHash) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(PCM_CACHE_VERSION);
        header.putInt(sourceHash);
        header.putInt(sampleRate);
        header.putInt(pcm.length);
        try (OutputStream out = Files.newOutputStream(path)) {
            out.write(header.array());
            ByteBuffer body = ByteBuffer.allocate(pcm.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (short s : pcm) {
                body.putShort(s);
            }
            out.write(body.array());
        }
    }

    private static PersistedPcm readPersisted(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return readPersistedFromStream(in);
        }
    }

    private static PersistedPcm readPersistedFromStream(InputStream in) throws IOException {
            byte[] headerBytes = in.readNBytes(16);
            if (headerBytes.length != 16) return null;
            ByteBuffer header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
            int version = header.getInt();
            int sourceHash = header.getInt();
            int sampleRate = header.getInt();
            int len = header.getInt();
            if (len <= 0 || sampleRate <= 0) return null;
            byte[] body = in.readNBytes(len * 2);
            if (body.length != len * 2) return null;
            ByteBuffer bb = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
            short[] pcm = new short[len];
            for (int i = 0; i < len; i++) {
                pcm[i] = bb.getShort();
            }
            return new PersistedPcm(version, sourceHash, sampleRate, pcm);
    }

    private static int readOggSourceHash() {
        Identifier id = Identifier.fromNamespaceAndPath("baity", "sounds/affectionate_scream.ogg");
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return 0;
            var res = mc.getResourceManager().getResource(id);
            if (res.isEmpty()) return 0;
            try (InputStream in = res.get().open()) {
                byte[] data = in.readAllBytes();
                if (data.length == 0) return 0;
                CRC32 crc = new CRC32();
                crc.update(data);
                return (int) crc.getValue();
            }
        } catch (Throwable t) {
            return 0;
        }
    }

    private record PersistedPcm(int version, int sourceHash, int sampleRate, short[] pcm) {}
}