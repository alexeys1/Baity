package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.multiplayer.ServerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class AntiBotUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static Map<String, String> playerMap = new HashMap<>();
    private static long lastRebuildGameTime = Long.MIN_VALUE;
    private static final int REBUILD_INTERVAL_GAME_TICKS = 100;
    private static String lastRulesServerSignature = null;
    private static Boolean lastRulesShouldApply = null;
    private static String lastSkyblockSignature = null;
    private static Boolean lastIsInSkyblock = null;
    
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\[\\d+\\]");

    private static boolean shouldApplyAntiBotRules() {
        if (mc == null || mc.getConnection() == null) return false;
        if (mc.hasSingleplayerServer()) return false;
        ServerData data = mc.getCurrentServer();
        if (data == null) return false;

        String signature = String.valueOf(data);
        if (signature != null && signature.equals(lastRulesServerSignature) && lastRulesShouldApply != null) {
            return lastRulesShouldApply;
        }

        boolean shouldApply = !isLocalLanServer(data);
        lastRulesServerSignature = signature;
        lastRulesShouldApply = shouldApply;
        return shouldApply;
    }

    private static boolean isLocalLanServer(ServerData data) {
        String host = getServerHost(data);
        if (host.isEmpty()) return false;
        host = stripPort(host);
        if (host.isEmpty()) return false;

        if ("localhost".equals(host) || "127.0.0.1".equals(host)) return true;
        if (host.startsWith("10.")) return true;
        if (host.startsWith("192.168.")) return true;
        if (host.startsWith("169.254.")) return true;

        if (host.startsWith("172.")) {
            String[] parts = host.split("\\.");
            if (parts.length >= 2) {
                try {
                    int secondOctet = Integer.parseInt(parts[1]);
                    return secondOctet >= 16 && secondOctet <= 31;
                } catch (Exception ignored) {
                    return false;
                }
            }
        }

        return false;
    }

    private static String getServerHost(ServerData data) {
        String ip = safeLower(data.ip);
        if (!ip.isEmpty()) return ip;
        String address = safeLower(reflectStringField(data, "address"));
        return address == null ? "" : address;
    }

    private static String stripPort(String host) {
        String h = host.trim();
        if (h.isEmpty()) return "";
        if (h.startsWith("[")) {
            int end = h.indexOf(']');
            if (end > 1) return h.substring(1, end);
            return h;
        }
        int idx = h.indexOf(':');
        if (idx >= 0) return h.substring(0, idx);
        return h;
    }

    private static String reflectStringField(Object obj, String fieldName) {
        if (obj == null || fieldName == null) return "";
        try {
            var f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String safeLower(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.isEmpty()) return "";
        return s.toLowerCase(Locale.ROOT);
    }

    private static String removeColorCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        return text
            .replaceAll("(?i)\u00A7x(\u00A7[0-9a-f]){6}", "")
            .replaceAll("§[0-9a-fk-or]", "");
    }

    private static boolean isInSkyBlock() {
        if (mc.level == null || mc.player == null || mc.getConnection() == null) return false;

        ServerData data = mc.getCurrentServer();
        if (data == null) return false;
        String signature = String.valueOf(data);
        if (signature != null && signature.equals(lastSkyblockSignature) && lastIsInSkyblock != null) {
            return lastIsInSkyblock;
        }

        boolean inSkyblock = com.shyeuar.baity.utils.LocateUtils.inSkyBlock(mc);
        lastSkyblockSignature = signature;
        lastIsInSkyblock = inSkyblock;
        return inSkyblock;
    }
   
    public static void updatePlayerMap() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) {
            playerMap.clear();
            lastRulesServerSignature = null;
            lastRulesShouldApply = null;
            lastSkyblockSignature = null;
            lastIsInSkyblock = null;
            lastRebuildGameTime = Long.MIN_VALUE;
            return;
        }
        if (!shouldApplyAntiBotRules()) {
            playerMap.clear();
            lastRebuildGameTime = Long.MIN_VALUE;
            return;
        }

        long gameTime = mc.level.getGameTime();
        if (!playerMap.isEmpty() && gameTime - lastRebuildGameTime < REBUILD_INTERVAL_GAME_TICKS) {
            return;
        }

        rebuildPlayerMap(isInSkyBlock());
        lastRebuildGameTime = gameTime;
    }

    private static void rebuildPlayerMap(boolean inSkyBlock) {
        playerMap.clear();
        for (Player worldPlayer : mc.level.players()) {
            try {
                if (worldPlayer == null || worldPlayer == mc.player) {
                    continue;
                }

                UUID uuid = worldPlayer.getUUID();
                if (uuid == null) {
                    continue;
                }

                String playerName = worldPlayer.getName().getString();

                if (inSkyBlock) {
                    String displayName = null;
                    if (worldPlayer.getDisplayName() != null) {
                        displayName = worldPlayer.getDisplayName().getString();
                    }

                    if (displayName != null && !displayName.isEmpty()) {
                        String nameWithoutColor = removeColorCodes(displayName).trim();
                        boolean hasLevelPrefix = LEVEL_PREFIX_PATTERN.matcher(nameWithoutColor).find();
                        if (hasLevelPrefix) {
                            playerMap.put(uuid.toString(), playerName);
                        }
                    }
                    continue;
                }

                if (playerName.startsWith("!")) {
                    continue;
                }

                if (worldPlayer.getActiveEffects().isEmpty()) {
                    continue;
                }

                playerMap.put(uuid.toString(), playerName);
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean isAntiBotActive() {
        return shouldApplyAntiBotRules();
    }

    public static boolean isPlayerMapEmpty() {
        return playerMap.isEmpty();
    }

    public static boolean isConfirmedBot(Player player) {
        if (!shouldApplyAntiBotRules() || player == null || player == mc.player) {
            return false;
        }
        if (playerMap.isEmpty()) {
            return false;
        }
        return !playerMap.containsKey(player.getUUID().toString());
    }

    public static boolean isRealPlayer(Player player) {
        if (!shouldApplyAntiBotRules()) {
            return true;
        }
        if (player == null || player == mc.player) {
            return true;
        }
        if (playerMap.isEmpty()) {
            return false;
        }
        return playerMap.containsKey(player.getUUID().toString());
    }

    public static boolean isBot(Player player) {
        if (!shouldApplyAntiBotRules()) {
            return false;
        }
        return !isRealPlayer(player);
    }
}
