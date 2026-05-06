package com.shyeuar.baity.features.smolpeople;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.sync.BaityPresenceSync;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class SmolFriendManager {
    private static final Map<String, String> FRIENDS = new LinkedHashMap<>();
    private static final String FRIENDS_FILE_NAME = "smol-friends.txt";
    private static final String LEGACY_FRIENDS_FILE_NAME = "synced_players.txt";
    private static final Pattern VALID_PLAYER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final long LOBBY_PLAYERS_REFRESH_INTERVAL_MS = 500L;
    private static List<String> cachedLobbyPlayers = List.of();
    private static long lastLobbyPlayersRefreshTime = 0L;

    private SmolFriendManager() {
    }

    public static void reloadFromConfig() {
        FRIENDS.clear();

        if (loadFromFile()) {
            return;
        }

        String serialized = ConfigManager.smolFriendList;
        if (serialized != null && !serialized.isBlank()) {
            String[] names = serialized.split(",");
            for (String name : names) {
                addNameToMemory(name);
            }
        }
        saveToFile();
    }

    public static boolean shouldApplySmolTo(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entityId == mc.player.getId()) {
            return true;
        }

        Player targetPlayer = getPlayerByEntityId(entityId);
        if (targetPlayer == null) {
            return false;
        }

        Boolean remotePreference = BaityPresenceSync.getRemoteSmolPreference(targetPlayer.getUUID());
        if (remotePreference != null) {
            return remotePreference;
        }

        if (!ConfigManager.smolFriendsEnabled) {
            return false;
        }

        return isFriend(targetPlayer.getName().getString());
    }

    public static Player getPlayerByEntityId(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof Player player) {
            return player;
        }
        return null;
    }

    public static boolean addFriend(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }

        if (FRIENDS.containsKey(normalized)) {
            return false;
        }

        FRIENDS.put(normalized, name.trim());
        persistToConfig();
        refreshLobbyPlayersCache();
        return true;
    }

    public static boolean removeFriend(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }

        if (FRIENDS.remove(normalized) == null) {
            return false;
        }

        persistToConfig();
        refreshLobbyPlayersCache();
        return true;
    }

    public static boolean isFriend(String name) {
        String normalized = normalizeName(name);
        return normalized != null && FRIENDS.containsKey(normalized);
    }

    public static String getStoredName(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return null;
        }
        return FRIENDS.get(normalized);
    }

    public static List<String> getFriends() {
        List<String> names = new ArrayList<>(FRIENDS.values());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static List<String> getCurrentLobbyPlayers() {
        long now = System.currentTimeMillis();
        if (now - lastLobbyPlayersRefreshTime >= LOBBY_PLAYERS_REFRESH_INTERVAL_MS) {
            refreshLobbyPlayersCache();
        }
        return cachedLobbyPlayers;
    }

    public static void refreshLobbyPlayersCache() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            cachedLobbyPlayers = List.of();
            lastLobbyPlayersRefreshTime = System.currentTimeMillis();
            return;
        }

        String selfName = mc.player != null ? mc.player.getName().getString() : null;
        String normalizedSelfName = normalizeName(selfName);
        Map<String, String> lobbyPlayers = new LinkedHashMap<>();

        for (var entry : mc.getConnection().getOnlinePlayers()) {
            if (entry == null || entry.getProfile() == null) {
                continue;
            }

            String rawName = entry.getProfile().name();
            String normalizedName = normalizeLobbyPlayerName(rawName);
            if (normalizedName == null || normalizedName.equals(normalizedSelfName) || FRIENDS.containsKey(normalizedName)) {
                continue;
            }

            lobbyPlayers.putIfAbsent(normalizedName, rawName.trim());
        }

        List<String> names = new ArrayList<>(lobbyPlayers.values());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        cachedLobbyPlayers = List.copyOf(names);
        lastLobbyPlayersRefreshTime = System.currentTimeMillis();
    }

    private static void persistToConfig() {
        saveToFile();
        syncLegacyConfigField();
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String normalizeLobbyPlayerName(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return null;
        }

        String trimmed = name.trim();
        if (trimmed.startsWith("!")) {
            return null;
        }

        if (!VALID_PLAYER_NAME_PATTERN.matcher(trimmed).matches()) {
            return null;
        }

        return normalized;
    }

    private static void addNameToMemory(String rawName) {
        String normalized = normalizeName(rawName);
        if (normalized == null) {
            return;
        }
        FRIENDS.putIfAbsent(normalized, rawName.trim());
    }

    private static boolean loadFromFile() {
        Path filePath = getFriendsFilePath();
        Path legacyPath = getLegacyFriendsFilePath();
        if (!Files.exists(filePath) && Files.exists(legacyPath)) {
            filePath = legacyPath;
        }
        if (!Files.exists(filePath)) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                String[] entries = line.split(",");
                for (String entry : entries) {
                    addNameToMemory(entry);
                }
            }
            if (filePath.equals(legacyPath)) {
                saveToFile();
            }
            return true;
        } catch (IOException e) {
            System.err.println("[Baity] Failed to load friend list file: " + e.getMessage());
            return false;
        }
    }

    private static void saveToFile() {
        Path filePath = getFriendsFilePath();
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, new ArrayList<>(FRIENDS.values()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Baity] Failed to save friend list file: " + e.getMessage());
        }
    }

    private static Path getFriendsFilePath() {
        return BaityConfigDir.getBaityConfigDir().resolve(FRIENDS_FILE_NAME);
    }

    private static Path getLegacyFriendsFilePath() {
        return BaityConfigDir.getBaityConfigDir().resolve(LEGACY_FRIENDS_FILE_NAME);
    }

    private static void syncLegacyConfigField() {
        ConfigManager.smolFriendList = String.join(",", FRIENDS.values());
        ConfigManager.requestSave();
    }
}
