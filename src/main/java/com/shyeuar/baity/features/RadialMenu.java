package com.shyeuar.baity.features;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static com.shyeuar.baity.utils.RadialMenuRendererUtils.*;

@Environment(EnvType.CLIENT)
public class RadialMenu {

    private static boolean isOpen = false;
    private static boolean wasKeyPressed = false;
    private static boolean wasMousePressed = false;
    private static int hoveredSection = -1;

    private static final List<RadialSection> sections = new ArrayList<>();

    static {
        sections.add(new RadialSection("warpmenu", "\u2690", "WarpMenu"));
        sections.add(new RadialSection("ah", "\u2692", "AH"));
        sections.add(new RadialSection("bz", "\u2696", "BZ"));
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

    public static void tick(Minecraft client) {
        if (client.screen != null) {
            if (isOpen) close(client);
            wasKeyPressed = false;
            wasMousePressed = false;
            return;
        }

        Module radialMenuModule = ModuleManager.getModuleByName("RadialMenu");
        if (radialMenuModule == null || !radialMenuModule.isEnabled()) {
            if (isOpen) close(client);
            wasKeyPressed = false;
            wasMousePressed = false;
            return;
        }

        int keybind = ConfigManager.radialMenuKeybind;
        if (keybind == 0) {
            if (isOpen) close(client);
            wasKeyPressed = false;
            wasMousePressed = false;
            return;
        }

        long windowHandle = client.getWindow().handle();
        boolean isKeyPressed = KeyMappingUtils.isKeyPressed(windowHandle, keybind);

        if (isKeyPressed && !wasKeyPressed) {
            open(client);
        } else if (!isKeyPressed && wasKeyPressed) {
            executeAndClose(client);
        }
        wasKeyPressed = isKeyPressed;

        if (isOpen) {
            boolean isMousePressed = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (isMousePressed && !wasMousePressed && hoveredSection >= 0) {
                executeAndClose(client);
            }
            wasMousePressed = isMousePressed;
        }
    }

    private static void open(Minecraft client) {
        if (isOpen) return;
        isOpen = true;
        long windowHandle = client.getWindow().handle();
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        double centerX = client.getWindow().getScreenWidth() / 2.0;
        double centerY = client.getWindow().getScreenHeight() / 2.0;
        GLFW.glfwSetCursorPos(windowHandle, centerX, centerY);
    }

    private static void close(Minecraft client) {
        if (!isOpen) return;
        isOpen = false;
        hoveredSection = -1;
        GLFW.glfwSetInputMode(client.getWindow().handle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    private static void executeAndClose(Minecraft client) {
        String actionId = null;
        if (hoveredSection >= 0 && hoveredSection < sections.size()) {
            actionId = sections.get(hoveredSection).id;
        }

        if ("warpmenu".equals(actionId)) {
            SoundUtils.playWoodenButton();
            isOpen = false;
            hoveredSection = -1;
            double mouseX = client.mouseHandler.xpos();
            double mouseY = client.mouseHandler.ypos();
            WarpMenuScreen.setInitialMousePosition(mouseX, mouseY);
            client.setScreen(new WarpMenuScreen());
        } else {
            close(client);
            if (actionId != null) {
                SoundUtils.playWoodenButton();
                executeAction(client, actionId);
            }
        }
    }

    private static void executeAction(Minecraft client, String actionId) {
        if (client.player == null) return;
        switch (actionId) {
            case "bz" -> client.player.connection.sendCommand("bz");
            case "ah" -> client.player.connection.sendCommand("ah");
        }
    }

    public static void render(GuiGraphics context, Minecraft client) {
        if (!isOpen) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        double mouseX = client.mouseHandler.xpos() * screenWidth / client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * screenHeight / client.getWindow().getScreenHeight();

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        hoveredSection = -1;
        if (distance > INNER_RADIUS && distance < OUTER_RADIUS + 20) {
            double angle = Math.atan2(dy, dx);
            double degrees = Math.toDegrees(angle);
            if (degrees < 0) degrees += 360;
            hoveredSection = getSectionFromAngle(degrees, sections.size());
        }

        int sectionCount = sections.size();
        double anglePerSection = 360.0 / sectionCount;
        double startAngle = getStartAngle(sectionCount);

        for (int i = 0; i < sectionCount; i++) {
            double sectionStartAngle = startAngle + i * anglePerSection;
            double sectionEndAngle = sectionStartAngle + anglePerSection;
            boolean isHovered = (hoveredSection == i);
            int sectionColor = isHovered ? SECTION_HOVER_COLOR : SECTION_COLOR;
            drawArcSection(context, centerX, centerY, INNER_RADIUS, OUTER_RADIUS,
                    sectionStartAngle, sectionEndAngle, sectionColor);
        }

        for (int i = 0; i < sectionCount; i++) {
            double lineAngle = Math.toRadians(startAngle + i * anglePerSection);
            drawRadialLine(context, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, lineAngle, BORDER_COLOR);
        }

        for (int i = 0; i < sectionCount; i++) {
            RadialSection section = sections.get(i);
            double sectionStartAngle = startAngle + i * anglePerSection;
            double sectionEndAngle = sectionStartAngle + anglePerSection;
            boolean isHovered = (hoveredSection == i);

            double midAngle = Math.toRadians((sectionStartAngle + sectionEndAngle) / 2);
            int iconRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
            int iconX = centerX + (int) (Math.cos(midAngle) * iconRadius);
            int iconY = centerY + (int) (Math.sin(midAngle) * iconRadius);

            int textColor = isHovered ? 0xFFFFFF00 : 0xFFFFFFFF;
            String icon = section.icon;
            
            float scale = 3.0f;
            var matrices = context.pose();
            matrices.pushMatrix();
            matrices.translate(iconX, iconY);
            matrices.scale(scale, scale);
            context.drawString(client.font, icon, -client.font.width(icon) / 2, -client.font.lineHeight / 2, textColor, true);
            matrices.popMatrix();
        }

        drawFilledCircle(context, centerX, centerY, INNER_RADIUS + 2, CENTER_COLOR);

        if (hoveredSection >= 0 && hoveredSection < sectionCount) {
            RadialSection hoveredSec = sections.get(hoveredSection);
            String labelText = hoveredSec.displayName;
            int labelWidth = client.font.width(labelText);

            double sectionStartAngle = startAngle + hoveredSection * anglePerSection;
            double sectionEndAngle = sectionStartAngle + anglePerSection;
            double midAngle = Math.toRadians((sectionStartAngle + sectionEndAngle) / 2);

            int labelRadius = OUTER_RADIUS + 15;
            int labelX = centerX + (int) (Math.cos(midAngle) * labelRadius) - labelWidth / 2;
            int labelY = centerY + (int) (Math.sin(midAngle) * labelRadius) - 4;

            context.drawString(client.font, labelText, labelX, labelY, 0xFFFFFF00, true);
        }

        String centerIcon = "\u2726";
        int centerTextWidth = client.font.width(centerIcon);
        context.drawString(client.font, centerIcon, centerX - centerTextWidth / 2, centerY - 4, 0xFFAAAAAA, true);
    }
}
