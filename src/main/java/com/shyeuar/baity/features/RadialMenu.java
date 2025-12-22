package com.shyeuar.baity.features;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.utils.RadialMenuRendererUtils;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
        sections.add(new RadialSection("warpmenu", "\u2690"));
        sections.add(new RadialSection("bz", "\u2696"));
        sections.add(new RadialSection("ah", "\u2692"));
    }

    public static class RadialSection {
        public final String id;
        public final String icon;

        public RadialSection(String id, String icon) {
            this.id = id;
            this.icon = icon;
        }
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void tick(MinecraftClient client) {
        if (client.currentScreen != null) {
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

        long windowHandle = client.getWindow().getHandle();
        boolean isKeyPressed = KeyMappingUtils.isKeyPressed(windowHandle, keybind);

        if (isKeyPressed && !wasKeyPressed) {
            open(client);
        } else if (!isKeyPressed && wasKeyPressed) {
            executeAndClose(client);
        }
        wasKeyPressed = isKeyPressed;

        // 处理鼠标点击选择
        if (isOpen) {
            boolean isMousePressed = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (isMousePressed && !wasMousePressed && hoveredSection >= 0) {
                executeAndClose(client);
            }
            wasMousePressed = isMousePressed;
        }
    }

    private static void open(MinecraftClient client) {
        if (isOpen) return;
        isOpen = true;
        long windowHandle = client.getWindow().getHandle();
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        // 将鼠标移到屏幕中心
        double centerX = client.getWindow().getWidth() / 2.0;
        double centerY = client.getWindow().getHeight() / 2.0;
        GLFW.glfwSetCursorPos(windowHandle, centerX, centerY);
    }

    private static void close(MinecraftClient client) {
        if (!isOpen) return;
        isOpen = false;
        hoveredSection = -1;
        GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    private static void executeAndClose(MinecraftClient client) {
        String actionId = null;
        if (hoveredSection >= 0 && hoveredSection < sections.size()) {
            actionId = sections.get(hoveredSection).id;
        }

        if ("warpmenu".equals(actionId)) {
            SoundUtils.playWoodenButton();
            isOpen = false;
            hoveredSection = -1;
            double mouseX = client.mouse.getX();
            double mouseY = client.mouse.getY();
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

    private static void executeAction(MinecraftClient client, String actionId) {
        if (client.player == null) return;
        switch (actionId) {
            case "bz" -> client.player.networkHandler.sendChatCommand("bz");
            case "ah" -> client.player.networkHandler.sendChatCommand("ah");
        }
    }

    public static void render(DrawContext context, MinecraftClient client) {
        if (!isOpen) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        double mouseX = client.mouse.getX() * screenWidth / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * screenHeight / client.getWindow().getHeight();

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
            int textWidth = client.textRenderer.getWidth(icon);
            context.drawText(client.textRenderer, icon, iconX - textWidth / 2, iconY - 4, textColor, true);
        }

        drawFilledCircle(context, centerX, centerY, INNER_RADIUS + 2, CENTER_COLOR);

        String centerIcon = "\u2726";
        int centerTextWidth = client.textRenderer.getWidth(centerIcon);
        context.drawText(client.textRenderer, centerIcon, centerX - centerTextWidth / 2, centerY - 4, 0xFFAAAAAA, true);
    }
}
