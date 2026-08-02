package com.shyeuar.baity.features.smolpeople;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.sync.BaityPresenceSync;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class SmolFriendManager {
    private static final Map<String, String> FRIENDS = new LinkedHashMap<>();
    private static final String FRIENDS_FILE_NAME = "smol-friends.txt";
    private static final Pattern VALID_PLAYER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final long LOBBY_PLAYERS_REFRESH_INTERVAL_MS = 500L;
    private static final double MIRROR_ARMOR_STAND_RADIUS = 1.0;
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

        if (isLocalPlayerMirror(targetPlayer)) {
            return true;
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

    public static boolean isMirrorNametagArmorStand(int entityId) {
        if (!SmolPeopleNametag.isSmolPeopleActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return false;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof ArmorStand armorStand)) {
            return false;
        }
        if (!armorStand.isInvisible()) {
            return false;
        }
        if (!armorStandNameMatchesLocalPlayer(mc, armorStand)) {
            return false;
        }
        for (Player player : mc.level.players()) {
            if (player == mc.player || !isLocalPlayerMirror(player)) {
                continue;
            }
            if (isWithinMirrorArmorStandRadius(player, armorStand)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocalPlayerMirror(Player other) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || other == mc.player) {
            return false;
        }
        return matchesProfileIdMirrorPath(mc, other) || matchesLegacyMirrorPath(mc, other);
    }

    private static boolean matchesProfileIdMirrorPath(Minecraft mc, Player other) {
        UUID skinOwnerId = getSkinTextureProfileId(other);
        return skinOwnerId != null && skinOwnerId.equals(mc.player.getUUID());
    }

    private static boolean matchesLegacyMirrorPath(Minecraft mc, Player other) {
        if (other.getUUID().equals(mc.player.getUUID())) {
            return true;
        }

        String selfName = mc.player.getGameProfile().name();
        String otherName = other.getGameProfile().name();
        if (namesMatchMirror(selfName, otherName)) {
            return true;
        }

        if (matchesVisibleName(mc.player, other)) {
            return true;
        }

        if (!(mc.player instanceof AbstractClientPlayer selfClient)) {
            return false;
        }
        if (!(other instanceof AbstractClientPlayer otherClient)) {
            return false;
        }

        var selfSkin = selfClient.getSkin();
        var otherSkin = otherClient.getSkin();
        if (selfSkin == null || otherSkin == null
                || selfSkin.body() == null || otherSkin.body() == null) {
            return false;
        }
        if (!selfSkin.body().texturePath().equals(otherSkin.body().texturePath())) {
            return false;
        }
        return !isListedInTab(other.getUUID());
    }

    private static boolean isListedInTab(UUID uuid) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || uuid == null) {
            return false;
        }
        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            if (entry != null && entry.getProfile() != null && uuid.equals(entry.getProfile().id())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesVisibleName(Player self, Player other) {
        for (String selfVisible : collectVisibleNames(self)) {
            for (String otherVisible : collectVisibleNames(other)) {
                if (namesMatchMirror(selfVisible, otherVisible)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String[] collectVisibleNames(Player player) {
        String plainName = LocateUtils.toPlainText(player.getName().getString());
        String displayName = player.getDisplayName() != null
            ? LocateUtils.toPlainText(player.getDisplayName().getString())
            : null;
        if (displayName != null && displayName.equals(plainName)) {
            return new String[] { plainName };
        }
        return new String[] { plainName, displayName };
    }

    private static boolean namesMatchMirror(String selfName, String otherName) {
        if (selfName == null || selfName.isBlank() || otherName == null || otherName.isBlank()) {
            return false;
        }
        return selfName.equalsIgnoreCase(otherName)
            || otherName.equalsIgnoreCase(reverse(selfName));
    }

    private static String reverse(String value) {
        return new StringBuilder(value).reverse().toString();
    }

    private static UUID getSkinTextureProfileId(Player player) {
        String textureValue = getTexturesPropertyValue(player.getGameProfile());
        if (textureValue == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                if (info != null && info.getProfile() != null) {
                    textureValue = getTexturesPropertyValue(info.getProfile());
                }
            }
        }
        if (textureValue == null) {
            return null;
        }
        return parseProfileIdFromTextureValue(textureValue);
    }

    private static String getTexturesPropertyValue(GameProfile profile) {
        if (profile == null || profile.properties() == null) {
            return null;
        }
        var textures = profile.properties().get("textures");
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        for (Property property : textures) {
            if (property != null && property.value() != null && !property.value().isBlank()) {
                return property.value();
            }
        }
        return null;
    }

    private static UUID parseProfileIdFromTextureValue(String base64Value) {
        try {
            String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            if (!object.has("profileId") || object.get("profileId").isJsonNull()) {
                return null;
            }
            return parseUuidFlexible(object.get("profileId").getAsString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static UUID parseUuidFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim();
        if (!id.contains("-") && id.length() == 32) {
            id = id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                    + "-" + id.substring(16, 20) + "-" + id.substring(20, 32);
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isWithinMirrorArmorStandRadius(Player mirrorPlayer, ArmorStand armorStand) {
        AABB searchBox = mirrorPlayer.getBoundingBox().inflate(MIRROR_ARMOR_STAND_RADIUS);
        return searchBox.intersects(armorStand.getBoundingBox());
    }

    private static boolean armorStandNameMatchesLocalPlayer(Minecraft mc, ArmorStand armorStand) {
        if (!armorStand.hasCustomName() || armorStand.getCustomName() == null) {
            return false;
        }
        String standName = LocateUtils.toPlainText(armorStand.getCustomName().getString());
        if (standName.isBlank()) {
            return false;
        }
        for (String localName : collectLocalPlayerNames(mc.player)) {
            if (localName == null || localName.isBlank()) {
                continue;
            }
            if (standName.equalsIgnoreCase(localName) || standName.equalsIgnoreCase(reverse(localName))) {
                return true;
            }
        }
        return false;
    }

    private static String[] collectLocalPlayerNames(Player player) {
        String profileName = player.getGameProfile().name();
        String plainName = LocateUtils.toPlainText(player.getName().getString());
        String displayName = player.getDisplayName() != null
            ? LocateUtils.toPlainText(player.getDisplayName().getString())
            : null;
        if (displayName != null && displayName.equals(plainName)) {
            return new String[] { profileName, plainName };
        }
        return new String[] { profileName, plainName, displayName };
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
        if (!Files.exists(filePath)) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                for (String entry : line.split(",")) {
                    addNameToMemory(entry);
                }
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

    private static void syncLegacyConfigField() {
        ConfigManager.smolFriendList = String.join(",", FRIENDS.values());
        ConfigManager.requestSave();
    }
}
