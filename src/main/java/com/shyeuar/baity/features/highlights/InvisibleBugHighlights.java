package com.shyeuar.baity.features.highlights;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class InvisibleBugHighlights implements WorldRenderEvents.AfterEntities {

    private static final Minecraft MC = Minecraft.getInstance();

    private static final float R = 1.0f;
    private static final float G = 0.9f;
    private static final float B = 0.2f;
    private static final float A = 0.9f;

    @Override
    public void afterEntities(WorldRenderContext context) {
        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.highlightsInvisibleBugEnabled) return;

        if (MC.level == null || MC.player == null) return;

        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        MultiBufferSource buffers = context.consumers();
        if (matrices == null || buffers == null) return;

        Set<net.minecraft.world.entity.LivingEntity> invisbugEntities = InvisibleBugDetector.getCurrentInvisbugEntities();
        if (invisbugEntities.isEmpty()) return;

        java.util.List<Vec3> locationsToRender = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.LivingEntity entity : invisbugEntities) {
            if (entity == null || !entity.isAlive()) continue;
            
            Vec3 entityPos = entity.position();
            double distanceToPlayer = entityPos.distanceTo(MC.player.position());
            
            if (distanceToPlayer <= 32.0 && MC.player.hasLineOfSight(entity)) {
                locationsToRender.add(entityPos);
            }
        }
        
        if (locationsToRender.isEmpty()) return;

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        matrices.pushPose();
        PoseStack.Pose pose = matrices.last();

        Vec3 renderOffset = new Vec3(0.4, -0.2, 0.4);
        double extraSize = -0.2;

        for (Vec3 location : locationsToRender) {
            if (location == null) continue;
            
            Vec3 renderLocation = location.subtract(renderOffset);
            
            AABB box = new AABB(
                renderLocation.x - extraSize, renderLocation.y - extraSize, renderLocation.z - extraSize,
                renderLocation.x + 1 + extraSize, renderLocation.y + 1 + extraSize, renderLocation.z + 1 + extraSize
            );
            
            double x1 = box.minX - cameraPos.x;
            double y1 = box.minY - cameraPos.y;
            double z1 = box.minZ - cameraPos.z;
            double x2 = box.maxX - cameraPos.x;
            double y2 = box.maxY - cameraPos.y;
            double z2 = box.maxZ - cameraPos.z;

            drawLine(pose, lines, x1, y1, z1, x2, y1, z1, R, G, B, A);
            drawLine(pose, lines, x2, y1, z1, x2, y1, z2, R, G, B, A);
            drawLine(pose, lines, x2, y1, z2, x1, y1, z2, R, G, B, A);
            drawLine(pose, lines, x1, y1, z2, x1, y1, z1, R, G, B, A);
            
            drawLine(pose, lines, x1, y2, z1, x2, y2, z1, R, G, B, A);
            drawLine(pose, lines, x2, y2, z1, x2, y2, z2, R, G, B, A);
            drawLine(pose, lines, x2, y2, z2, x1, y2, z2, R, G, B, A);
            drawLine(pose, lines, x1, y2, z2, x1, y2, z1, R, G, B, A);
            
            drawLine(pose, lines, x1, y1, z1, x1, y2, z1, R, G, B, A);
            drawLine(pose, lines, x2, y1, z1, x2, y2, z1, R, G, B, A);
            drawLine(pose, lines, x2, y1, z2, x2, y2, z2, R, G, B, A);
            drawLine(pose, lines, x1, y1, z2, x1, y2, z2, R, G, B, A);
            
            drawLine(pose, lines, x1, y1, z1, x2, y1, z1, R, G, B, A);
            drawLine(pose, lines, x2, y1, z1, x2, y1, z2, R, G, B, A);
            drawLine(pose, lines, x2, y1, z2, x1, y1, z2, R, G, B, A);
            drawLine(pose, lines, x1, y1, z2, x1, y1, z1, R, G, B, A);
            drawLine(pose, lines, x1, y2, z1, x2, y2, z1, R, G, B, A);
            drawLine(pose, lines, x2, y2, z1, x2, y2, z2, R, G, B, A);
            drawLine(pose, lines, x2, y2, z2, x1, y2, z2, R, G, B, A);
            drawLine(pose, lines, x1, y2, z2, x1, y2, z1, R, G, B, A);
        }

        matrices.popPose();
    }

    private static void drawLine(PoseStack.Pose pose, VertexConsumer vc,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0.001) {
            float nx = (float) (dx / length);
            float ny = (float) (dy / length);
            float nz = (float) (dz / length);
            vc.addVertex(pose.pose(), (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz);
            vc.addVertex(pose.pose(), (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz);
        }
    }
}


