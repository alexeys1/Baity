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
    private static int tickCount = 0;
    
    public static void updatePlayerMap() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        
        boolean onServer = mc.hasSingleplayerServer() == false && mc.getCurrentServer() != null;
        
        tickCount++;
        if (tickCount % 40 == 0) {
            playerMap.clear();
            
            for (PlayerInfo playerInfo : mc.getConnection().getOnlinePlayers()) {
                try {
                    if (playerInfo == null) continue;
                    
                    UUID uuid = playerInfo.getProfile().id();
                    if (uuid == null) continue;
                    
                    String playerName;
                    if (playerInfo.getTabListDisplayName() != null) {
                        playerName = playerInfo.getTabListDisplayName().getString();
                    } else {
                        playerName = uuid.toString();
                    }
                    
                    // 检测方法1：名称前缀（hypixel npc通常以!开头)
                    if (playerName.startsWith("!")) {
                        continue;
                    }
                    
                    // 检测方法2：状态效果（仅在服务器上使用，防止本地世界 / LAN 把无效果玩家当成 Bot）
                    Player worldPlayer = mc.level.getPlayerByUUID(uuid);
                    if (onServer && worldPlayer != null && worldPlayer.getActiveEffects().isEmpty()) {
                        continue;
                    }
                    
                    // 检测方法3：UUID
                    try {
                        UUID.fromString(uuid.toString());
                    } catch (IllegalArgumentException e) {
                        continue; 
                    }
                    
                    if (worldPlayer != null) {
                        playerMap.put(uuid.toString(), playerName);
                    }
                } catch (Exception e) {
                    // 忽略异常，继续处理下一个玩家
                    continue;
                }
            }
        }
    }
    
    
    public static boolean isRealPlayer(Player player) {
        if (player == null || player == mc.player) return true; 
        
        String uuid = player.getUUID().toString();
        
        
        if (playerMap.isEmpty()) {
            return true;
        }
        
        return playerMap.containsKey(uuid);
    }
    
    
    public static boolean isBot(Player player) {
        return !isRealPlayer(player);
    }
    
    public static void reset() {
        playerMap.clear();
        tickCount = 0;
    }
}
