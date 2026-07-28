package com.shyeuar.baity.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@Environment(EnvType.CLIENT)
public final class AntiBotUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final float NPC_HEALTH = 20.0f;

    private AntiBotUtils() {
    }

    public static boolean isNpc(Player player) {
        if (player == null || player == mc.player) {
            return false;
        }
        return player.getUUID().version() == 2 && player.getHealth() == NPC_HEALTH;
    }

    public static boolean isBot(Player player) {
        return isNpc(player);
    }
}
