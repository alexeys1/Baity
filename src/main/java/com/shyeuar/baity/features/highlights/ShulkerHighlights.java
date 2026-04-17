package com.shyeuar.baity.features.highlights;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

@Environment(EnvType.CLIENT)
public class ShulkerHighlights implements WorldRenderEvents.AfterEntities {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final float LINE_WIDTH = 3.5f;

    private static final RenderPipeline BAITY_SHULKER_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/baity_shulker_lines")
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

    private static final RenderType NO_DEPTH_LINES = RenderType.create(
            "baity_shulker_lines",
            RenderSetup.builder(BAITY_SHULKER_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    @Override
    public void afterEntities(WorldRenderContext context) {
        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.highlightsShulkerEnabled) return;
        if (MC.level == null || MC.player == null) return;
        if (!isInGalatea()) return;

        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        PoseStack matrices = context.matrices();
        MultiBufferSource buffers = context.consumers();
        if (matrices == null || buffers == null) return;

        for (Entity entity : MC.level.entitiesForRendering()) {
            if (!(entity instanceof Shulker shulker)) continue;
            if (!shulker.isAlive()) continue;

            AABB box = shulker.getBoundingBox().inflate(0.01);

            double x1 = box.minX - cameraPos.x;
            double y1 = box.minY - cameraPos.y;
            double z1 = box.minZ - cameraPos.z;
            double x2 = box.maxX - cameraPos.x;
            double y2 = box.maxY - cameraPos.y;
            double z2 = box.maxZ - cameraPos.z;

            float r = 0.7f;
            float g = 1.0f;
            float b = 0.0f;
            float a = 0.9f;

            VertexConsumer lines = buffers.getBuffer(NO_DEPTH_LINES);

            matrices.pushPose();
            PoseStack.Pose pose = matrices.last();

            drawLine(pose, lines, x1, y1, z1, x2, y1, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z1, x2, y1, z2, r, g, b, a);
            drawLine(pose, lines, x2, y1, z2, x1, y1, z2, r, g, b, a);
            drawLine(pose, lines, x1, y1, z2, x1, y1, z1, r, g, b, a);

            drawLine(pose, lines, x1, y2, z1, x2, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y2, z1, x2, y2, z2, r, g, b, a);
            drawLine(pose, lines, x2, y2, z2, x1, y2, z2, r, g, b, a);
            drawLine(pose, lines, x1, y2, z2, x1, y2, z1, r, g, b, a);

            drawLine(pose, lines, x1, y1, z1, x1, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z1, x2, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z2, x2, y2, z2, r, g, b, a);
            drawLine(pose, lines, x1, y1, z2, x1, y2, z2, r, g, b, a);

            drawLine(pose, lines, x1, y1, z1, x2, y1, z1, r, g, b, a);
            drawLine(pose, lines, x2, y1, z1, x2, y1, z2, r, g, b, a);
            drawLine(pose, lines, x2, y1, z2, x1, y1, z2, r, g, b, a);
            drawLine(pose, lines, x1, y1, z2, x1, y1, z1, r, g, b, a);
            drawLine(pose, lines, x1, y2, z1, x2, y2, z1, r, g, b, a);
            drawLine(pose, lines, x2, y2, z1, x2, y2, z2, r, g, b, a);
            drawLine(pose, lines, x2, y2, z2, x1, y2, z2, r, g, b, a);
            drawLine(pose, lines, x1, y2, z2, x1, y2, z1, r, g, b, a);

            matrices.popPose();
        }
    }

    private static boolean isInGalatea() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                for (var entry : mc.getConnection().getOnlinePlayers()) {
                    if (entry.getTabListDisplayName() == null) continue;
                    String text = removeColorCodes(entry.getTabListDisplayName().getString()).trim();
                    if (text.isEmpty()) continue;
                    if (text.startsWith("Area:") && text.contains("Galatea")) return true;
                    if (text.startsWith("Island:") && text.contains("Galatea")) return true;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return false;
            Scoreboard scoreboard = mc.level.getScoreboard();
            if (scoreboard == null) return false;
            Objective sidebarObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebarObjective == null) return false;

            java.util.List<?> scores = tryGetSortedScores(scoreboard, sidebarObjective);
            if (scores == null || scores.isEmpty()) return false;
            for (Object scoreObj : scores) {
                String raw = extractScoreOwnerText(scoreObj);
                String line = removeColorCodes(raw).trim();
                if (line.contains("Galatea")) return true;
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    private static String removeColorCodes(String text) {
        if (text == null || text.isEmpty()) return text;
        return text
            .replaceAll("(?i)\u00A7x(\u00A7[0-9a-f]){6}", "")
            .replaceAll("§[0-9a-fk-or]", "");
    }

    private static java.util.List<?> tryGetSortedScores(Scoreboard scoreboard, Objective objective) {
        try {
            for (java.lang.reflect.Method m : scoreboard.getClass().getMethods()) {
                if (!"getSortedScores".equals(m.getName())) continue;
                if (m.getParameterCount() != 1) continue;
                try {
                    Object res = m.invoke(scoreboard, objective);
                    if (res instanceof java.util.List<?> list) return list;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return java.util.Collections.emptyList();
    }

    private static String extractScoreOwnerText(Object scoreObj) {
        if (scoreObj == null) return "";
        try {
            for (String methodName : new String[]{"getOwner", "getName", "getPlayerName"}) {
                try {
                    java.lang.reflect.Method m = scoreObj.getClass().getMethod(methodName);
                    Object v = m.invoke(scoreObj);
                    if (v == null) continue;
                    try {
                        java.lang.reflect.Method getString = v.getClass().getMethod("getString");
                        Object s = getString.invoke(v);
                        if (s != null) return String.valueOf(s);
                    } catch (Exception ignored) {
                    }
                    if (v instanceof String) return (String) v;
                    return String.valueOf(v);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return String.valueOf(scoreObj);
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
                    .setNormal(pose, nx, ny, nz)
                    .setLineWidth(LINE_WIDTH);
            vc.addVertex(pose.pose(), (float) x2, (float) y2, (float) z2)
                    .setColor(r, g, b, a)
                    .setNormal(pose, nx, ny, nz)
                    .setLineWidth(LINE_WIDTH);
        }
    }
}