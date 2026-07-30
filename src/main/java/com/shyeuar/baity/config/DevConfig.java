package com.shyeuar.baity.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.entity.player.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 开发者配置类
 * 此文件包含开发者相关的特殊配置，用于标识模组作者
 *
 * 算是我自己的小小私心啦喵~
 */
public class DevConfig {
    private static final Set<String> DEV_UUIDS = new HashSet<>(Arrays.asList(
        "8b8e7203-bdda-489e-bc20-f226f5b59c62"
    ));

    private static final String FALLBACK_WATERMARK_HANDLE = "@11YearCookieBuff";

    public static final String DEV_PREFIX = "[Dev]";
    public static final int DEV_PREFIX_COLOR = 0xFF6B6B;
    public static final int DEV_TEXT_COLOR = 0xFFFFFF;
    public static final String BILIBILI_UID = "522178337";

    private static volatile String cachedWatermarkHandle;
    private static volatile boolean watermarkHandleFetchStarted;

    public static String getBilibiliSpaceUrl() {
        return "https://space.bilibili.com/" + BILIBILI_UID;
    }

    public static void initWatermarkHandle() {
        startWatermarkHandleFetchIfNeeded();
    }

    public static String getWatermarkHandle() {
        String cached = cachedWatermarkHandle;
        if (cached != null) {
            return cached;
        }
        startWatermarkHandleFetchIfNeeded();
        return FALLBACK_WATERMARK_HANDLE;
    }

    public static boolean isDeveloper(Player player) {
        if (player == null) return false;
        return DEV_UUIDS.contains(player.getUUID().toString());
    }

    private static void startWatermarkHandleFetchIfNeeded() {
        if (watermarkHandleFetchStarted) {
            return;
        }
        watermarkHandleFetchStarted = true;
        CompletableFuture.runAsync(DevConfig::fetchWatermarkHandle);
    }

    private static void fetchWatermarkHandle() {
        String handle = requestWatermarkHandleFromBilibili();
        if (handle == null) {
            watermarkHandleFetchStarted = false;
            return;
        }
        cachedWatermarkHandle = handle;
    }

    private static String requestWatermarkHandleFromBilibili() {
        String fromCardApi = requestHandleFromApi(
            "https://api.bilibili.com/x/web-interface/card?mid=" + BILIBILI_UID,
            true
        );
        if (fromCardApi != null) {
            return fromCardApi;
        }
        return requestHandleFromApi(
            "https://api.bilibili.com/x/space/acc/info?mid=" + BILIBILI_UID,
            false
        );
    }

    private static String requestHandleFromApi(String url, boolean cardResponse) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            applyBilibiliRequestHeaders(connection);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject root = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (root.get("code").getAsInt() != 0 || !root.has("data") || root.get("data").isJsonNull()) {
                return null;
            }

            JsonObject data = root.getAsJsonObject("data");
            String name;
            if (cardResponse) {
                if (!data.has("card") || data.get("card").isJsonNull()) {
                    return null;
                }
                JsonObject card = data.getAsJsonObject("card");
                name = card.has("name") && !card.get("name").isJsonNull()
                    ? card.get("name").getAsString()
                    : null;
            } else {
                name = data.has("name") && !data.get("name").isJsonNull()
                    ? data.get("name").getAsString()
                    : null;
            }

            if (name == null || name.isBlank()) {
                return null;
            }
            return "@" + name.trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void applyBilibiliRequestHeaders(HttpURLConnection connection) {
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );
        connection.setRequestProperty("Referer", "https://www.bilibili.com");
        connection.setRequestProperty("Accept", "application/json");
    }
}
