package com.shyeuar.baity.features.highlights;

import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class SafariZoneUtils {

    public enum Zone {
        CAVERN,
        FOREST,
        HAUNTED,
        ICY,
        UNKNOWN
    }

    private SafariZoneUtils() {
    }

    public static Zone playerZone(Minecraft mc) {
        if (!LocateUtils.isInSafari(mc)) {
            return Zone.UNKNOWN;
        }
        Zone fromScoreboard = fromSubAreaName(LocateUtils.scoreboardSubAreaName(mc));
        if (fromScoreboard != Zone.UNKNOWN) {
            return fromScoreboard;
        }
        if (mc.player == null) {
            return Zone.UNKNOWN;
        }
        return zoneAt(mc.player.getX(), mc.player.getZ());
    }

    public static boolean matchesPlayerZone(Zone playerZone, double x, double z) {
        if (playerZone == Zone.UNKNOWN) {
            return true;
        }
        return zoneAt(x, z) == playerZone;
    }

    public static Zone zoneAt(double x, double z) {
        if (z >= 22.0) {
            return Zone.FOREST;
        }
        if (x <= -58.0 && z >= -18.0) {
            return Zone.CAVERN;
        }
        if (x >= -58.0 && z < -18.0) {
            return Zone.HAUNTED;
        }
        if (z < -18.0) {
            return Zone.ICY;
        }
        return Zone.CAVERN;
    }

    private static Zone fromSubAreaName(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Zone.UNKNOWN;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("cavern")) {
            return Zone.CAVERN;
        }
        if (lower.contains("forest")) {
            return Zone.FOREST;
        }
        if (lower.contains("haunted") || lower.contains("spooky")) {
            return Zone.HAUNTED;
        }
        if (lower.contains("icy") || lower.contains("ice")) {
            return Zone.ICY;
        }
        return Zone.UNKNOWN;
    }
}
