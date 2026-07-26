package com.shyeuar.baity.gui.value;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ValueCycleUtils {
    private ValueCycleUtils() {
    }

    public static String cycle(String current, String[] order, boolean forward) {
        if (order == null || order.length == 0) {
            return current;
        }
        if (order.length == 1) {
            return order[0];
        }
        if (current == null || current.isBlank()) {
            return forward ? order[0] : order[order.length - 1];
        }
        for (int i = 0; i < order.length; i++) {
            if (order[i].equalsIgnoreCase(current)) {
                int nextIndex = forward
                        ? (i + 1) % order.length
                        : (i - 1 + order.length) % order.length;
                return order[nextIndex];
            }
        }
        return forward ? order[0] : order[order.length - 1];
    }
}
