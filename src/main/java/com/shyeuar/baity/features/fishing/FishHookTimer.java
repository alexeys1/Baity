package com.shyeuar.baity.features.fishing;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.hud.HudElement;
import com.shyeuar.baity.gui.hud.HudManager;
import com.shyeuar.baity.utils.ComponentTextUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class FishHookTimer implements HudElement {

    private static final Pattern FISHING_TIMER_ARMOR_STAND_NAME_PATTERN = Pattern.compile("\u00A7e\u00A7l(\\d+(\\.\\d+)?)");
    private static final String BITE_MARKER_PLAIN = "!!!";
    private static final String ASSET_NAMESPACE = "fishtimer";
    private static final String BAR_TEXTURE_PATH = "textures/skyblock/fishing_timer_bar.png";
    private static final int FRAME_W = 128;
    private static final int FRAME_H = 32;
    private static final int FRAME_STRIDE = 33;
    private static final int SHEET_W = 128;
    private static final int SHEET_H = 395;

    private static FishHookTimer instance;
    private static final net.minecraft.sounds.SoundEvent[] FRAME_SOUNDS = new net.minecraft.sounds.SoundEvent[12];
    private static boolean clientHooksRegistered;

    private boolean selected;
    private boolean clicked;
    private int currentTick = -1;
    private boolean biteMode;
    private int lastSoundFrame = -1;

    private final List<ArmorStand> potentialTimerArmorStands = new ArrayList<>();
    private ArmorStand resolvedTimerArmorStand;

    private FishHookTimer() {
    }

    public static FishHookTimer getInstance() {
        if (instance == null) {
            instance = new FishHookTimer();
            HudManager.getInstance().register(instance);
        }
        return instance;
    }

    public static void init() {
        getInstance();
        HypixelFishingRodCatalog.init();
        FishHookTimerTemplateManager.init();
        registerSounds();
        if (!clientHooksRegistered) {
            clientHooksRegistered = true;
            ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> getInstance().resetFishingHookTimerTracking());
            ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> getInstance().onClientEntityLoadForFishingTimer(entity));
        }
    }

    private static void registerSounds() {
        for (int i = 0; i < 12; i++) {
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(ASSET_NAMESPACE, "fishing_timer_" + i);
            FRAME_SOUNDS[i] = net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT,
                id,
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id)
            );
        }
    }

    private void resetFishingHookTimerTracking() {
        potentialTimerArmorStands.clear();
        resolvedTimerArmorStand = null;
        clearTimerState();
    }

    private void onClientEntityLoadForFishingTimer(Entity entity) {
        if (!ConfigManager.fishHookTimerEnabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (!LocateUtils.inSkyBlock(mc)) {
            return;
        }
        if (entity instanceof ArmorStand) {
            if (!HypixelFishingRodCatalog.mainHandHoldsHypixelFishingRod()) {
                return;
            }
            potentialTimerArmorStands.add((ArmorStand) entity);
            return;
        }
        if (entity instanceof FishingHook hook && hook.getOwner() == mc.player) {
            resetTimerTrackingForNewBobber();
        }
    }

    private void resetTimerTrackingForNewBobber() {
        if (!HypixelFishingRodCatalog.mainHandHoldsHypixelFishingRod()) {
            return;
        }
        resetFishingHookTimerTracking();
    }

    public static boolean shouldSuppressBoundTimerStandBody(LivingEntityRenderState state) {
        if (!ConfigManager.fishHookTimerEnabled || !ConfigManager.fishHookTimerHideDefaultTimer) {
            return false;
        }
        if (state.entityType != EntityType.ARMOR_STAND) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        FishHookTimer timer = getInstance();
        if (!timer.canRenderFishHookTimerHud(mc)) {
            return false;
        }
        ArmorStand bound = timer.resolvedTimerArmorStand;
        if (bound == null || !bound.isAlive()) {
            return false;
        }
        double dx = state.x - bound.getX();
        double dy = state.y - bound.getY();
        double dz = state.z - bound.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        return distSq < 0.25;
    }

    private boolean canRenderFishHookTimerHud(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return false;
        }
        if (!LocateUtils.inSkyBlock(mc)) {
            return false;
        }
        if (!HypixelFishingRodCatalog.mainHandHoldsHypixelFishingRod()) {
            return false;
        }
        ArmorStand stand = resolvedTimerArmorStand;
        if (stand == null || !stand.isAlive()) {
            return false;
        }
        if (!stand.hasCustomName() || !stand.isCustomNameVisible()) {
            return false;
        }
        return isFishHookTimerStandName(stand.getName());
    }

    public void tick() {
        if (!ConfigManager.fishHookTimerEnabled) {
            resetFishingHookTimerTracking();
            clearTimerState();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            resetFishingHookTimerTracking();
            clearTimerState();
            return;
        }
        if (!LocateUtils.inSkyBlock(mc)) {
            return;
        }
        if (!HypixelFishingRodCatalog.mainHandHoldsHypixelFishingRod()) {
            return;
        }

        if (resolvedTimerArmorStand == null) {
            List<ArmorStand> filter = new ArrayList<>();
            for (ArmorStand s : potentialTimerArmorStands) {
                if (s.hasCustomName() && isFishHookTimerStandName(s.getName())) {
                    filter.add(s);
                }
            }
            if (filter.size() == 1) {
                resolvedTimerArmorStand = filter.get(0);
            }
        }

        if (resolvedTimerArmorStand != null && resolvedTimerArmorStand.isAlive()) {
            syncUiStateFromResolvedStand(resolvedTimerArmorStand);
        } else {
            clearTimerState();
        }
    }

    public static boolean isFishHookTimerStandName(Component nameComponent) {
        if (nameComponent == null) {
            return false;
        }
        if (BITE_MARKER_PLAIN.equals(nameComponent.getString())) {
            return true;
        }
        return FISHING_TIMER_ARMOR_STAND_NAME_PATTERN.matcher(ComponentTextUtils.formattedLessResets(nameComponent)).matches();
    }

    private void syncUiStateFromResolvedStand(ArmorStand stand) {
        Component name = stand.getName();
        if (BITE_MARKER_PLAIN.equals(name.getString())) {
            currentTick = 0;
            biteMode = true;
            return;
        }
        Matcher m = FISHING_TIMER_ARMOR_STAND_NAME_PATTERN.matcher(ComponentTextUtils.formattedLessResets(name));
        if (!m.matches()) {
            clearTimerState();
            return;
        }
        try {
            double v = Double.parseDouble(m.group(1));
            currentTick = (int) (v * 10.0);
            biteMode = false;
        } catch (NumberFormatException e) {
            clearTimerState();
        }
    }

    private void clearTimerState() {
        currentTick = -1;
        biteMode = false;
        lastSoundFrame = -1;
    }

    private void tryPlayFrameSound(int frameIndex) {
        if (frameIndex < 0 || frameIndex > 11) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        if (FRAME_SOUNDS[frameIndex] == null) {
            return;
        }

        mc.level.playSound(
            mc.player,
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            FRAME_SOUNDS[frameIndex],
            SoundSource.PLAYERS,
            1.0f,
            1.0f
        );
    }

    @Override
    public String getId() {
        return "fishHookTimer";
    }

    @Override
    public String getDisplayName() {
        return "FishHookTimer";
    }

    @Override
    public double getX() {
        return ConfigManager.fishHookTimerX;
    }

    @Override
    public void setX(double x) {
        ConfigManager.fishHookTimerX = x;
    }

    @Override
    public double getY() {
        return ConfigManager.fishHookTimerY;
    }

    @Override
    public void setY(double y) {
        ConfigManager.fishHookTimerY = y;
    }

    @Override
    public float getScale() {
        return ConfigManager.fishHookTimerScale;
    }

    @Override
    public void setScale(float scale) {
        ConfigManager.fishHookTimerScale = Math.max(0.1f, Math.min(10.0f, scale));
    }

    @Override
    public double getDefaultX() {
        return FishHookTimerConfig.DEFAULT_X;
    }

    @Override
    public double getDefaultY() {
        return FishHookTimerConfig.DEFAULT_Y;
    }

    @Override
    public float getDefaultScale() {
        return FishHookTimerConfig.DEFAULT_SCALE;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isClicked() {
        return clicked;
    }

    @Override
    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    @Override
    public int getWidth() {
        return FRAME_W;
    }

    @Override
    public int getHeight() {
        return FRAME_H;
    }

    @Override
    public boolean shouldRender() {
        if (!ConfigManager.fishHookTimerEnabled) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        ArmorStand stand = resolvedTimerArmorStand;
        if (stand != null && !stand.isAlive()) {
            resetFishingHookTimerTracking();
            return false;
        }
        return canRenderFishHookTimerHud(mc);
    }

    @Override
    public void render(GuiGraphics guiGraphics, float partialTicks) {
        if (!ConfigManager.fishHookTimerEnabled || currentTick < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        int w = (int) (FRAME_W * getScale());
        int h = (int) (FRAME_H * getScale());
        int px = getAbsX(w);
        int py = getAbsY(h);
        boolean useTexture = false;
        try {
            var texId = Identifier.fromNamespaceAndPath(ASSET_NAMESPACE, BAR_TEXTURE_PATH);
            if (mc.getResourceManager().getResource(texId).isPresent()) {
                useTexture = true;
            }
        } catch (Exception ignored) {
        }
        if (useTexture) {
            int frame = biteMode ? 0 : Math.min(currentTick, 11);
            if (frame != lastSoundFrame) {
                tryPlayFrameSound(frame);
                lastSoundFrame = frame;
            }
            var matrices = guiGraphics.pose();
            matrices.pushMatrix();
            matrices.translate((float) px, (float) py);
            matrices.scale(getScale(), getScale());
            guiGraphics.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath(ASSET_NAMESPACE, BAR_TEXTURE_PATH),
                0, 0, 0, FRAME_STRIDE * (11 - frame),
                FRAME_W, FRAME_H, SHEET_W, SHEET_H
            );
            matrices.popMatrix();
        } else {
            String txt = biteMode ? "§c§l!!!" : String.format("§e§l%.1f", currentTick / 10.0);
            float scale = (float) w / (float) mc.font.width(txt) * 0.27f;
            var matrices = guiGraphics.pose();
            matrices.pushMatrix();
            matrices.translate((float) (px + w / 2.0), (float) (py + h / 2.0));
            matrices.scale(scale, scale);
            int tw = mc.font.width(txt);
            guiGraphics.drawString(mc.font, txt, -tw / 2, -mc.font.lineHeight / 2, 0xFFFFFFFF, false);
            matrices.popMatrix();
        }
    }
}