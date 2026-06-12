package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.gui.owo.RadialMenuComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public class RadialMenuScreen extends BaseOwoScreen<StackLayout> {

    private final int keybind;
    private final RadialMenuComponent wheel;
    private boolean wasKeyPressed = true;
    private boolean wasMousePressed = false;

    public RadialMenuScreen(int keybind) {
        super(Component.literal("Radial Menu"));
        this.keybind = keybind;

        List<RadialMenuComponent.Entry> entries = RadialMenu.sections().stream()
                .map(s -> new RadialMenuComponent.Entry(s.id, s.icon, s.displayName))
                .toList();

        this.wheel = new RadialMenuComponent(entries);
    }

    private boolean cursorCentered = false;

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void init() {
        super.init();
        if (!cursorCentered) {
            final Minecraft client = Minecraft.getInstance();
            if (client != null && client.getWindow() != null) {
                long windowHandle = client.getWindow().handle();
                double centerX = client.getWindow().getScreenWidth() / 2.0;
                double centerY = client.getWindow().getScreenHeight() / 2.0;
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(windowHandle, centerX, centerY);
            }
            cursorCentered = true;
        }
    }

    @Override
    protected void build(StackLayout rootComponent) {
        rootComponent.child(this.wheel);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft client = Minecraft.getInstance();
        float ratio = ClickGuiState.fixedScaleRatio(client);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        mouseX = Math.round(ClickGuiState.fixedCoord(mouseX, centerX, ratio));
        mouseY = Math.round(ClickGuiState.fixedCoord(mouseY, centerY, ratio));
        try {
            wheel.setGuiGraphics(graphics);
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(centerX, centerY);
            pose.scale(ratio, ratio);
            pose.translate(-centerX, -centerY);
            try {
                RadialMenuComponent.drawCenterPlayerHead(graphics, centerX, centerY);
            } finally {
                pose.popMatrix();
            }
        } finally {
            wheel.setGuiGraphics(null);
        }
    }

    @Override
    public void tick() {
        final Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        final long windowHandle = client.getWindow().handle();

        final boolean isKeyPressed = KeyMappingUtils.isKeyPressed(windowHandle, keybind);
        final boolean isMousePressed = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (isMousePressed && !wasMousePressed) {
            RadialMenu.activate(client, wheel.hoveredIndex());
            if (client.screen == this) client.setScreen(null);
            return;
        }
        wasMousePressed = isMousePressed;

        if (!isKeyPressed && wasKeyPressed) {
            RadialMenu.activate(client, wheel.hoveredIndex());
            if (client.screen == this) client.setScreen(null);
            return;
        }
        wasKeyPressed = isKeyPressed;
    }

    @Override
    public void onClose() {
        final Minecraft client = Minecraft.getInstance();
        if (client != null) RadialMenu.forceClose(client);
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}


