package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public class RadialMenu {

    private static boolean isOpen = false;
    private static boolean wasKeyPressed = false;

    private static final List<RadialSection> sections = new ArrayList<>();

    static {
        sections.add(new RadialSection("warpmenu", "\u2690", "WarpMenu"));
        sections.add(new RadialSection("ah", "\u2692", "Auction"));
        sections.add(new RadialSection("bz", "\u2696", "Bazaar"));
    }

    public static class RadialSection {
        public final String id;
        public final String icon;
        public final String displayName;

        public RadialSection(String id, String icon, String displayName) {
            this.id = id;
            this.icon = icon;
            this.displayName = displayName;
        }
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static List<RadialSection> sections() {
        return Collections.unmodifiableList(sections);
    }

    public static void tick(Minecraft client) {
        if (client.screen != null) {
            if (client.screen instanceof RadialMenuScreen) return;
            if (isOpen) forceClose(client);
            wasKeyPressed = false;
            return;
        }

        Module radialMenuModule = ModuleManager.getModuleByName("RadialMenu");
        if (radialMenuModule == null || !radialMenuModule.isEnabled()) {
            if (isOpen) forceClose(client);
            wasKeyPressed = false;
            return;
        }

        int keybind = ConfigManager.radialMenuKeybind;
        if (keybind == 0) {
            if (isOpen) forceClose(client);
            wasKeyPressed = false;
            return;
        }

        long windowHandle = client.getWindow().handle();
        boolean isKeyPressed = KeyMappingUtils.isKeyPressed(windowHandle, keybind);

        if (isKeyPressed && !wasKeyPressed) {
            open(client, keybind);
        }
        wasKeyPressed = isKeyPressed;
    }

    private static void open(Minecraft client, int keybind) {
        if (isOpen) return;
        isOpen = true;
        long windowHandle = client.getWindow().handle();
        double centerX = client.getWindow().getScreenWidth() / 2.0;
        double centerY = client.getWindow().getScreenHeight() / 2.0;
        GLFW.glfwSetCursorPos(windowHandle, centerX, centerY);
        client.setScreen(new RadialMenuScreen(keybind));
    }

    public static void forceClose(Minecraft client) {
        if (!isOpen) return;
        isOpen = false;
    }

    public static void activate(Minecraft client, int index) {
        String actionId = null;
        if (index >= 0 && index < sections.size()) {
            actionId = sections.get(index).id;
        }

        if ("warpmenu".equals(actionId)) {
            SoundUtils.playWoodenButton();
            isOpen = false;
            double mouseX = client.mouseHandler.xpos();
            double mouseY = client.mouseHandler.ypos();
            WarpMenuScreen.setInitialMousePosition(mouseX, mouseY);
            client.setScreen(new WarpMenuScreen());
            return;
        }

        forceClose(client);
        if (actionId != null) {
            SoundUtils.playWoodenButton();
            executeAction(client, actionId);
        }
    }

    private static void executeAction(Minecraft client, String actionId) {
        if (client.player == null) return;
        switch (actionId) {
            case "bz" -> client.player.connection.sendCommand("bz");
            case "ah" -> client.player.connection.sendCommand("ah");
        }
    }
}


