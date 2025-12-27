package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.MinecraftClient;

import java.util.Set;

public final class AntiSwimUtils {

    public static final float STANDING_EYE_HEIGHT = 1.62F;

    private static final Set<String> MODERN_ISLANDS = Set.of(
        "The Park",
        "Galatea"
    );

    private AntiSwimUtils() {}

    public static boolean isFeatureActive() {
        Module m = ModuleManager.getModuleByName("AntiSwim");
        if (m == null || !m.isEnabled()) return false;
        
        if (!isOnHypixelSkyblock()) return false;
        
        if (isOnModernIsland()) return false;
        
        return true;
    }

    public static boolean isOnHypixelSkyblock() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return false;
        
        if (mc.isInSingleplayer()) return false;
        
        if (mc.getCurrentServerEntry() != null) {
            String serverAddress = mc.getCurrentServerEntry().address.toLowerCase();
            if (!serverAddress.contains("hypixel")) return false;
        } else {
            return false;
        }
        
        String area = getCurrentArea();
        return !area.isEmpty();
    }

    public static boolean isOnModernIsland() {
        String area = getCurrentArea();
        return MODERN_ISLANDS.contains(area);
    }

    public static String getCurrentArea() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return "";
        for (var entry : mc.getNetworkHandler().getPlayerList()) {
            if (entry.getDisplayName() != null) {
                String text = entry.getDisplayName().getString().trim();
                if (text.startsWith("Area:") || text.startsWith("Dungeon:")) {
                    return text.split(":", 2)[1].trim();
                }
            }
        }
        return "";
    }

    public static boolean isSelfPlayer(Object entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return entity == mc.player;
    }

    public static boolean isSelfPlayerById(int entityId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.player.getId() == entityId;
    }

    public static boolean isSneaking() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.player != null && mc.options.sneakKey.isPressed();
    }
}
