package com.shyeuar.baity.features.highlights;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.EntityDrawUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Environment(EnvType.CLIENT)
public final class InvisibugHighlights implements WorldRenderEvents.AfterEntities {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final double NEAR_MARKER_DISTANCE = 5.0;
    private static final Set<LivingEntity> trackedMarkerArmorStands = new CopyOnWriteArraySet<>();

    private static final float R = 1.0f;
    private static final float G = 0.84f;
    private static final float B = 0.0f;
    private static final float A = 0.9f;

    public static void onParticleAt(Vec3 particleLocation) {
        if (particleLocation == null) {
            return;
        }
        if (MC.level == null || MC.player == null) {
            return;
        }

        if (!hasExactlyOneMarkerNear(particleLocation)) {
            return;
        }

        for (LivingEntity existing : trackedMarkerArmorStands) {
            if (existing != null && existing.isAlive()) {
                Vec3 entityPos = existing.position();
                if (entityPos.distanceTo(particleLocation) < NEAR_MARKER_DISTANCE) {
                    return;
                }
            }
        }

        AABB searchBox = new AABB(
                particleLocation.x - 0.5,
                particleLocation.y - 1.0,
                particleLocation.z - 0.5,
                particleLocation.x + 0.5,
                particleLocation.y + 1.0,
                particleLocation.z + 0.5
        );

        ArmorStand markerArmorStand = null;

        for (ArmorStand armorStand : MC.level.getEntitiesOfClass(ArmorStand.class, searchBox)) {
            if (armorStand == null || !armorStand.isAlive()) {
                continue;
            }

            if (isInvisibugMarkerArmorStand(armorStand)) {
                markerArmorStand = armorStand;
                break;
            }
        }

        final ArmorStand finalMarkerArmorStand = markerArmorStand;
        if (finalMarkerArmorStand != null) {
            MC.execute(() -> trackedMarkerArmorStands.add(finalMarkerArmorStand));
        }
    }

    public static void removeTrackedMarker(ArmorStand entity) {
        trackedMarkerArmorStands.remove(entity);
    }

    private static Set<LivingEntity> getTrackedMarkerArmorStands() {
        trackedMarkerArmorStands.removeIf(entity -> entity == null || !entity.isAlive());
        return new CopyOnWriteArraySet<>(trackedMarkerArmorStands);
    }

    private static boolean hasExactlyOneMarkerNear(Vec3 pos) {
        AABB searchBox = new AABB(
                pos.x - 0.5,
                pos.y - 1.0,
                pos.z - 0.5,
                pos.x + 0.5,
                pos.y + 1.0,
                pos.z + 0.5
        );

        List<ArmorStand> markers = new ArrayList<>();
        for (ArmorStand armorStand : MC.level.getEntitiesOfClass(ArmorStand.class, searchBox)) {
            if (armorStand != null && armorStand.isAlive() && isInvisibugMarkerArmorStand(armorStand)) {
                markers.add(armorStand);
            }
        }

        return markers.size() == 1;
    }

    private static boolean isInvisibugMarkerArmorStand(ArmorStand armorStand) {
        if (armorStand == null || !armorStand.isAlive()) {
            return false;
        }

        if (!armorStand.isMarker()) {
            return false;
        }

        if (armorStand.hasCustomName()) {
            return false;
        }

        ItemStack mainHand = armorStand.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHand.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (!ConfigManager.highlightsInvisibugEnabled) {
            return;
        }

        if (MC.level == null || MC.player == null) {
            return;
        }

        if (!LocateUtils.isGalatea(MC)) {
            return;
        }

        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        var buffers = context.consumers();
        if (matrices == null || buffers == null) {
            return;
        }

        Set<LivingEntity> markers = getTrackedMarkerArmorStands();
        if (markers.isEmpty()) {
            return;
        }

        float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        List<AABB> boxesToRender = new ArrayList<>();
        for (LivingEntity entity : markers) {
            if (entity == null || !entity.isAlive()) {
                continue;
            }

            Vec3 at = entity.getPosition(partialTick);
            double distanceToPlayer = at.distanceTo(MC.player.getPosition(partialTick));
            if (distanceToPlayer <= 32.0 && MC.player.hasLineOfSight(entity)) {
                Vec3 renderOffset = new Vec3(0.4, -0.2, 0.4);
                double extraSize = -0.2;
                Vec3 renderLocation = at.subtract(renderOffset);
                boxesToRender.add(new AABB(
                        renderLocation.x - extraSize, renderLocation.y - extraSize, renderLocation.z - extraSize,
                        renderLocation.x + 1 + extraSize, renderLocation.y + 1 + extraSize, renderLocation.z + 1 + extraSize
                ));
            }
        }

        if (boxesToRender.isEmpty()) {
            return;
        }

        VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
        matrices.pushPose();
        PoseStack.Pose pose = matrices.last();

        for (AABB box : boxesToRender) {
            double x1 = box.minX - cameraPos.x;
            double y1 = box.minY - cameraPos.y;
            double z1 = box.minZ - cameraPos.z;
            double x2 = box.maxX - cameraPos.x;
            double y2 = box.maxY - cameraPos.y;
            double z2 = box.maxZ - cameraPos.z;

            EntityDrawUtils.drawWireCube(pose, lines, x1, y1, z1, x2, y2, z2, R, G, B, A);
        }

        matrices.popPose();
    }

}