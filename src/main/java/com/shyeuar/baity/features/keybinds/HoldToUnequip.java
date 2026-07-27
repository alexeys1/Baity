package com.shyeuar.baity.features.keybinds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class HoldToUnequip {
    private HoldToUnequip() {
    }

    public static boolean isHeld(long windowHandle, String mode) {
        if (mode == null) {
            return false;
        }
        return switch (mode.toLowerCase()) {
            case "ctrl" -> isCtrlDown(windowHandle);
            case "shift" -> isShiftDown(windowHandle);
            case "alt" -> isAltDown(windowHandle);
            default -> false;
        };
    }

    private static boolean isCtrlDown(long windowHandle) {
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private static boolean isShiftDown(long windowHandle) {
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean isAltDown(long windowHandle) {
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
}
