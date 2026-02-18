package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class AntiBotUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static Map<String, String> playerMap = new HashMap<>();
    private static Map<UUID, Long> joinTimes = new HashMap<>();
    private static int tickCount = 0;
    private static Map<UUID, Long> lastUpdatePlayers = new HashMap<>();
    
    public static void updatePlayerMap() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        
        tickCount++;
        if (tickCount % 40 == 0) {
            Map<UUID, Long> currentPlayers = new HashMap<>();
            long currentTime = System.currentTimeMillis();
            
            for (PlayerInfo playerInfo : mc.getConnection().getOnlinePlayers()) {
                try {
                    if (playerInfo == null) continue;
                    UUID uuid = playerInfo.getProfile().id();
                    if (uuid == null) continue;
                    
                    if (!lastUpdatePlayers.containsKey(uuid)) {
                        joinTimes.put(uuid, currentTime);
                    }
                    currentPlayers.put(uuid, currentTime);
                } catch (Exception e) {
                    continue;
                }
            }
            
            joinTimes.keySet().retainAll(currentPlayers.keySet());
            lastUpdatePlayers = currentPlayers;
            
            playerMap.clear();
            
            for (PlayerInfo playerInfo : mc.getConnection().getOnlinePlayers()) {
                try {
                    if (playerInfo == null) continue;
                    
                    UUID uuid = playerInfo.getProfile().id();
                    if (uuid == null) continue;
                    
                    try {
                        UUID.fromString(uuid.toString());
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    
                    String playerName;
                    if (playerInfo.getTabListDisplayName() != null) {
                        playerName = playerInfo.getTabListDisplayName().getString();
                    } else {
                        playerName = uuid.toString();
                    }
                    
                    String unformattedName = playerName.replaceAll("§[0-9a-fk-or]", "");
                    if (unformattedName.contains(" ")) {
                        continue;
                    }
                    
                    Player worldPlayer = mc.level.getPlayerByUUID(uuid);
                    if (worldPlayer == null) {
                        continue;
                    }
                    
                    playerMap.put(uuid.toString(), playerName);
                } catch (Exception e) {
                    continue;
                }
            }
        }
    }
    
    
    public static boolean isRealPlayer(Player player) {
        if (player == null || player == mc.player) return true; 
        
        String uuid = player.getUUID().toString();
        
        if (playerMap.isEmpty()) {
            return false;
        }
        
        if (!playerMap.containsKey(uuid)) {
            return false;
        }
        
        if (mc.getConnection() != null) {
            boolean inTabList = mc.getConnection().getOnlinePlayers().stream()
                    .anyMatch(info -> info.getProfile().id().equals(player.getUUID()));
            if (!inTabList) {
                return false;
            }
        }
        
        Long spawnTime = joinTimes.get(player.getUUID());
        if (spawnTime != null && System.currentTimeMillis() - spawnTime < 1000) {
            return false;
        }
        
        if (!player.isAlive()) {
            return false;
        }
        
        String name = player.getName().getString();
        if (name.contains(" ")) {
            return false;
        }
        
        return true;
    }
    
    
    public static boolean isBot(Player player) {
        return !isRealPlayer(player);
    }
    
    public static void reset() {
        playerMap.clear();
        joinTimes.clear();
        lastUpdatePlayers.clear();
        tickCount = 0;
    }
}
