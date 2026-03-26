package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.client.multiplayer.ServerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class AntiBotUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static Map<String, String> playerMap = new HashMap<>();
    private static int tickCount = 0;
    
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\[\\d+\\]");

    private static boolean isOnHypixel() {
        if (mc == null || mc.getConnection() == null) return false;
        if (mc.hasSingleplayerServer()) return false;
        ServerData data = mc.getCurrentServer();
        if (data == null || data.ip == null || data.ip.isBlank()) return false;
        String ip = data.ip.toLowerCase();
        return ip.contains("hyp");
    }

    private static String removeColorCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
    
    private static boolean isInSkyBlock() {
        if (mc.level == null || mc.player == null) return false;
        
        try {
            Scoreboard scoreboard = mc.level.getScoreboard();
            if (scoreboard != null) {
                Objective sidebarObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
                if (sidebarObjective != null) {
                    String displayName = sidebarObjective.getDisplayName().getString();
                    String displayNameWithoutColor = removeColorCodes(displayName);
                    if (displayNameWithoutColor.toLowerCase().contains("skyblock")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        
        return false;
    }
   
    public static void updatePlayerMap() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        if (!isOnHypixel()) {
            playerMap.clear();
            return;
        }
        
        boolean inSkyBlock = isInSkyBlock();
        
        tickCount++;
        if (tickCount % 60 == 0) {
            playerMap.clear();
            
            for (Player worldPlayer : mc.level.players()) {
                try {
                    if (worldPlayer == null || worldPlayer == mc.player) continue;
                    
                    UUID uuid = worldPlayer.getUUID();
                    if (uuid == null) continue;
                    
                    String playerName = worldPlayer.getName().getString();
                    
                    if (inSkyBlock) {
                        String displayName = null;
                        if (worldPlayer.getDisplayName() != null) {
                            displayName = worldPlayer.getDisplayName().getString();
                        }
                        
                        if (displayName != null && !displayName.isEmpty()) {
                            String nameWithoutColor = removeColorCodes(displayName);
                            if (LEVEL_PREFIX_PATTERN.matcher(nameWithoutColor).find()) {
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
                    
                    try {
                        UUID.fromString(uuid.toString());
                    } catch (IllegalArgumentException e) {
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
        if (!isOnHypixel()) return true;
        if (player == null || player == mc.player) return true; 
        
        String uuid = player.getUUID().toString();
        
        
        if (playerMap.isEmpty()) {
            return false;
        }
        
        return playerMap.containsKey(uuid);
    }
    
    
    public static boolean isBot(Player player) {
        if (!isOnHypixel()) return false;
        return !isRealPlayer(player);
    }
    
    public static void reset() {
        playerMap.clear();
        tickCount = 0;
    }
}
