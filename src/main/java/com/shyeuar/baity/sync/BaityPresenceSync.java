package com.shyeuar.baity.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public final class BaityPresenceSync {
    private static final long FETCH_INTERVAL_MS = 180_000L;
    private static final long FETCH_JITTER_RANGE_MS = 90_000L;
    private static final long FETCH_BACKOFF_BASE_MS = 5_000L;
    private static final long FETCH_BACKOFF_MAX_MS = 60_000L;
    private static final long REPORT_HEARTBEAT_MS = 600_000L;
    private static final long REPORT_CHANGE_DEBOUNCE_MS = 3_000L;
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;
    private static final String DEFAULT_SYNC_URL = "https://raw.githubusercontent.com/raueyhs/baity-sync-data/main/data/v1/users.json";

    private static final AtomicBoolean FETCHING = new AtomicBoolean(false);
    private static final AtomicBoolean REPORTING = new AtomicBoolean(false);

    private static volatile long nextFetchAt = 0L;
    private static volatile long nextReportHeartbeatAt = 0L;
    private static volatile long nextReportAllowedAt = 0L;
    private static volatile Instant updatedAt = Instant.EPOCH;
    private static volatile String lastReportedSignature = "";
    private static volatile long stableFetchJitterMs = -1L;
    private static volatile int consecutiveFetchFailures = 0;

    private static final Map<UUID, RemoteUserState> USERS_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, ChromaProfile> CHROMA_BY_NAME = new ConcurrentHashMap<>();

    private static final boolean PERF_DEBUG = Boolean.getBoolean("baity.perfDebug");
    private static final long PERF_SLOW_THRESHOLD_NS = Long.getLong("baity.perfDebug.syncSlowThresholdNs", 10_000_000L);

    private BaityPresenceSync() {
    }

    public static void init() {
        nextFetchAt = 0L;
        nextReportHeartbeatAt = 0L;
        nextReportAllowedAt = 0L;
        lastReportedSignature = "";
        stableFetchJitterMs = -1L;
        consecutiveFetchFailures = 0;
    }

    public static void tick() {
        final long startNs = PERF_DEBUG ? System.nanoTime() : 0L;
        String url = resolveFetchUrl();

        long now = System.currentTimeMillis();
        if (url != null && !url.isBlank() && now >= nextFetchAt && FETCHING.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    boolean success = fetchAndReplace(url.trim());
                    if (success) {
                        consecutiveFetchFailures = 0;
                        nextFetchAt = System.currentTimeMillis() + FETCH_INTERVAL_MS + getStableFetchJitterMs();
                    } else {
                        consecutiveFetchFailures = Math.min(consecutiveFetchFailures + 1, 16);
                        nextFetchAt = System.currentTimeMillis() + computeFetchBackoffMs(consecutiveFetchFailures);
                    }
                } finally {
                    FETCHING.set(false);
                }
            });
        }

        String reportUrl = ConfigManager.baityPresenceReportUrl;
        if (reportUrl != null && !reportUrl.isBlank() && REPORTING.compareAndSet(false, true)) {
            LocalUserState state = snapshotLocalState();
            if (state != null) {
                String signature = state.signature();
                boolean changed = !signature.equals(lastReportedSignature);
                boolean dueHeartbeat = now >= nextReportHeartbeatAt;
                boolean allowedByDebounce = now >= nextReportAllowedAt;
                if ((changed && allowedByDebounce) || dueHeartbeat) {
                    nextReportAllowedAt = now + REPORT_CHANGE_DEBOUNCE_MS;
                    nextReportHeartbeatAt = now + REPORT_HEARTBEAT_MS;
                    CompletableFuture.runAsync(() -> {
                        try {
                            reportLocalState(reportUrl.trim(), state);
                            lastReportedSignature = signature;
                        } finally {
                            REPORTING.set(false);
                        }
                    });
                } else {
                    REPORTING.set(false);
                }
            } else {
                REPORTING.set(false);
            }
        }

        if (PERF_DEBUG) {
            long dtNs = System.nanoTime() - startNs;
            if (dtNs >= PERF_SLOW_THRESHOLD_NS) {
                System.out.println("[Baity][Perf][PresenceSync] tick slow dtNs=" + dtNs);
            }
        }
    }

    private static long getStableFetchJitterMs() {
        if (stableFetchJitterMs >= 0L) return stableFetchJitterMs;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            long hash = client.player.getUUID().getMostSignificantBits() ^ client.player.getUUID().getLeastSignificantBits();
            long nonNegative = hash & Long.MAX_VALUE;
            stableFetchJitterMs = nonNegative % (FETCH_JITTER_RANGE_MS + 1L);
        } else {
            stableFetchJitterMs = ThreadLocalRandom.current().nextLong(FETCH_JITTER_RANGE_MS + 1L);
        }
        return stableFetchJitterMs;
    }

    private static String resolveFetchUrl() {
        String syncUrl = ConfigManager.baityPresenceSyncUrl;
        String reportUrl = ConfigManager.baityPresenceReportUrl;
        if (reportUrl != null && !reportUrl.isBlank()) {
            String trimmed = reportUrl.trim();
            if (trimmed.endsWith("/report")) {
                return trimmed.substring(0, trimmed.length() - "/report".length()) + "/users.json";
            }
            if (trimmed.endsWith("/")) {
                return trimmed + "users.json";
            }
            if (trimmed.endsWith(".json")) {
                return trimmed;
            }
            return trimmed + "/users.json";
        }
        if (syncUrl == null || syncUrl.isBlank()) return DEFAULT_SYNC_URL;
        return syncUrl.trim();
    }

    private static long computeFetchBackoffMs(int failures) {
        int clampedFailures = Math.max(1, failures);
        long multiplier = 1L << Math.min(clampedFailures - 1, 12);
        long base = FETCH_BACKOFF_BASE_MS * multiplier;
        return Math.min(base, FETCH_BACKOFF_MAX_MS);
    }

    public static boolean isSmolEnabledFor(UUID uuid) {
        if (uuid == null) return false;
        RemoteUserState state = USERS_BY_UUID.get(uuid);
        return state != null && state.smolPeopleEnabled();
    }

    public static ChromaProfile getChromaProfileByName(String name) {
        if (name == null || name.isBlank()) return null;
        return CHROMA_BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    public static boolean isBaityUser(UUID uuid) {
        if (uuid == null) return false;
        RemoteUserState state = USERS_BY_UUID.get(uuid);
        return state != null && state.isBaityUser();
    }

    public static Instant getUpdatedAt() {
        return updatedAt;
    }

    private static boolean fetchAndReplace(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) return false;

            try (InputStream stream = connection.getInputStream()) {
                String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                applyPayload(json);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static LocalUserState snapshotLocalState() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return null;

        String playerName = player.getName().getString();
        if (playerName == null || playerName.isBlank()) return null;

        Module chromaModule = ModuleManager.getModuleByName("NickTweaks");
        boolean nickTweaksEnabled = chromaModule != null && chromaModule.isEnabled();
        if (!nickTweaksEnabled) {
            return null; // 主功能未开启则不写入
        }
        boolean chromaEnabled = ConfigManager.nickTweaksChromaEnabled;
        boolean smolEnabled = ConfigManager.smolpeopleMode;
        int[] palette = generateLocalChromaPalette(); // chroma off -> [start,end]; chroma on -> HSV palette
        double speed = chromaEnabled ? Math.max(0.0, Math.min(8.0, ConfigManager.nickTweaksChromaSpeed)) : 0.0;

        int gradientStart = ConfigManager.nickTweaksGradientStartColor & 0xFFFFFF;
        int gradientEnd = ConfigManager.nickTweaksGradientEndColor & 0xFFFFFF;
        boolean boldSelf = ConfigManager.nickTweaksBoldSelf;
        return new LocalUserState(player.getUUID(), playerName, true, chromaEnabled, smolEnabled, palette, speed, gradientStart, gradientEnd, boldSelf);
    }

    private static void reportLocalState(String url, LocalUserState state) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            String token = ConfigManager.baityPresenceReportToken;
            if (token != null && !token.isBlank()) {
                connection.setRequestProperty("x-baity-token", token.trim());
            }

            JsonObject root = new JsonObject();
            root.addProperty("version", 1);

            JsonObject user = new JsonObject();
            user.addProperty("uuid", state.uuid().toString());
            user.addProperty("name", state.name());
            user.addProperty("isBaityUser", state.isBaityUser());

            JsonObject features = new JsonObject();
            JsonObject chroma = new JsonObject();
            chroma.addProperty("nickTweaksEnabled", true);
            chroma.addProperty("enabled", state.chromaEnabled());
            chroma.addProperty("speed", state.chromaSpeed());
            JsonArray palette = new JsonArray();
            for (int color : state.chromaPalette()) {
                palette.add(String.format("#%06X", color & 0xFFFFFF));
            }
            chroma.add("palette", palette);
            chroma.addProperty("gradientStart", String.format("#%06X", state.gradientStart() & 0xFFFFFF));
            chroma.addProperty("gradientEnd", String.format("#%06X", state.gradientEnd() & 0xFFFFFF));
            chroma.addProperty("boldSelf", state.boldSelf());
            features.add("chromaOwnName", chroma);
            features.add("nickTweaks", chroma.deepCopy());

            JsonObject smol = new JsonObject();
            smol.addProperty("enabled", state.smolEnabled());
            features.add("smolPeople", smol);
            user.add("features", features);

            JsonObject meta = new JsonObject();
            meta.addProperty("protocol", 1);
            meta.addProperty("reportedAt", Instant.now().toString());
            user.add("meta", meta);

            root.add("user", user);

            byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);
            connection.getOutputStream().write(payload);
            connection.getOutputStream().flush();

            int code = connection.getResponseCode();
            if (code >= 400) {
                try (InputStream ignored = connection.getErrorStream()) {
                }
            } else {
                try (InputStream ignored = connection.getInputStream()) {
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static int[] generateLocalChromaPalette() {
        if (!ConfigManager.nickTweaksChromaEnabled) {
            return new int[]{
                ConfigManager.nickTweaksGradientStartColor & 0xFFFFFF,
                ConfigManager.nickTweaksGradientEndColor & 0xFFFFFF
            };
        }
        int count = 6;
        int[] colors = new int[count];
        double chroma = Math.max(0.0, Math.min(0.4, ConfigManager.nickTweaksChromaChroma));
        double lightness = Math.max(0.2, Math.min(1.0, ConfigManager.nickTweaksChromaLightness));
        float saturation = (float) (chroma / 0.4);
        for (int i = 0; i < count; i++) {
            float hue = (float) i / (float) count;
            colors[i] = Mth.hsvToRgb(hue, saturation, (float) lightness);
        }
        return colors;
    }

    private static void applyPayload(String json) {
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) return;

        JsonObject root = rootElement.getAsJsonObject();
        JsonObject users = root.getAsJsonObject("users");
        if (users == null) return;

        Map<UUID, RemoteUserState> newUsers = new ConcurrentHashMap<>();
        Map<String, ChromaProfile> newChromaByName = new ConcurrentHashMap<>();

        for (Map.Entry<String, JsonElement> entry : users.entrySet()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(entry.getKey());
            } catch (Exception ignored) {
                continue;
            }

            if (!entry.getValue().isJsonObject()) continue;
            JsonObject userObj = entry.getValue().getAsJsonObject();
            String name = getAsString(userObj, "name", "");
            if (name.isBlank()) continue;

            boolean isBaityUser = getAsBoolean(userObj, "isBaityUser", false);

            JsonObject features = userObj.getAsJsonObject("features");
            JsonObject smolObj = features == null ? null : features.getAsJsonObject("smolPeople");
            boolean smolEnabled = smolObj != null && getAsBoolean(smolObj, "enabled", false);

            JsonObject chromaObj = features == null ? null : features.getAsJsonObject("nickTweaks");
            if (chromaObj == null && features != null) {
                chromaObj = features.getAsJsonObject("chromaOwnName");
            }
            boolean nickTweaksEnabled = chromaObj != null && getAsBoolean(chromaObj, "nickTweaksEnabled", true);
            boolean chromaEnabled = chromaObj != null && getAsBoolean(chromaObj, "enabled", false);
            double speed = chromaObj == null ? 1.0 : getAsDouble(chromaObj, "speed", 1.0);
            int[] palette = parsePalette(chromaObj == null ? null : chromaObj.getAsJsonArray("palette"));
            int gradientStart = parseHexColor(chromaObj, "gradientStart", 0xFF0000);
            int gradientEnd = parseHexColor(chromaObj, "gradientEnd", 0x0000FF);
            boolean boldSelf = chromaObj != null && getAsBoolean(chromaObj, "boldSelf", false);
            if (palette.length == 0 && chromaEnabled) {
                palette = new int[]{0xFF4D4D, 0xFFAA00, 0xFFFF66, 0x66FF99, 0x66CCFF, 0xC299FF};
            }

            RemoteUserState state = new RemoteUserState(uuid, name, isBaityUser, smolEnabled, chromaEnabled, palette, speed);
            newUsers.put(uuid, state);
            if (nickTweaksEnabled) {
                newChromaByName.put(name.toLowerCase(Locale.ROOT), new ChromaProfile(chromaEnabled, palette, speed, gradientStart, gradientEnd, boldSelf));
            }
        }

        USERS_BY_UUID.clear();
        USERS_BY_UUID.putAll(newUsers);
        CHROMA_BY_NAME.clear();
        CHROMA_BY_NAME.putAll(newChromaByName);
        updatedAt = Instant.now();
    }

    private static int[] parsePalette(JsonArray array) {
        if (array == null || array.isEmpty()) return new int[0];
        ArrayList<Integer> colors = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) continue;
            String hex = element.getAsString();
            if (hex == null || !hex.matches("^#([A-Fa-f0-9]{6})$")) continue;
            try {
                colors.add(Integer.parseInt(hex.substring(1), 16));
            } catch (Exception ignored) {
            }
        }
        int[] out = new int[colors.size()];
        for (int i = 0; i < colors.size(); i++) out[i] = colors.get(i);
        return out;
    }

    private static String getAsString(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseHexColor(JsonObject obj, String key, int fallback) {
        String raw = getAsString(obj, key, "");
        if (!raw.matches("^#([A-Fa-f0-9]{6})$")) return fallback;
        try {
            return Integer.parseInt(raw.substring(1), 16);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getAsBoolean(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getAsDouble(JsonObject obj, String key, double fallback) {
        if (obj == null || !obj.has(key)) return fallback;
        try {
            return obj.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public record ChromaProfile(boolean chromaEnabled, int[] palette, double speed, int gradientStart, int gradientEnd, boolean boldSelf) {
        public ChromaProfile {
            if (palette == null) palette = new int[0];
            gradientStart &= 0xFFFFFF;
            gradientEnd &= 0xFFFFFF;
        }

        public int[] paletteView() {
            return palette.length == 0 ? new int[0] : palette.clone();
        }
    }

    public record RemoteUserState(
            UUID uuid,
            String name,
            boolean isBaityUser,
            boolean smolPeopleEnabled,
            boolean chromaEnabled,
            int[] chromaPalette,
            double chromaSpeed
    ) {
    }

    private record LocalUserState(
            UUID uuid,
            String name,
            boolean isBaityUser,
            boolean chromaEnabled,
            boolean smolEnabled,
            int[] chromaPalette,
            double chromaSpeed,
            int gradientStart,
            int gradientEnd,
            boolean boldSelf
    ) {
        String signature() {
            return uuid + "|" + name + "|" + isBaityUser + "|" + chromaEnabled + "|" + smolEnabled + "|" + chromaSpeed
                    + "|" + gradientStart + "|" + gradientEnd + "|" + boldSelf + "|" + java.util.Arrays.toString(chromaPalette);
        }
    }
}
