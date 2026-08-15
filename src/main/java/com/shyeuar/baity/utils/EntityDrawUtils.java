package com.shyeuar.baity.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EntityDrawUtils {

    public static final float ESP_LINE_WIDTH = 3.5f;

    public static void drawLine(
            PoseStack.Pose pose,
            VertexConsumer vc,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float r,
            float g,
            float b,
            float a
    ) {
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
                    .setLineWidth(ESP_LINE_WIDTH);
            vc.addVertex(pose.pose(), (float) x2, (float) y2, (float) z2)
                    .setColor(r, g, b, a)
                    .setNormal(pose, nx, ny, nz)
                    .setLineWidth(ESP_LINE_WIDTH);
        }
    }

    public static void drawWireCube(
            PoseStack.Pose pose,
            VertexConsumer lines,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float r,
            float g,
            float b,
            float a
    ) {
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
    }

    public static void drawWireBoxAtWorld(
            PoseStack matrices,
            VertexConsumer lines,
            AABB box,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        matrices.pushPose();
        matrices.translate(box.minX - cameraPos.x, box.minY - cameraPos.y, box.minZ - cameraPos.z);
        drawWireCube(
                matrices.last(),
                lines,
                0,
                0,
                0,
                box.getXsize(),
                box.getYsize(),
                box.getZsize(),
                r,
                g,
                b,
                a
        );
        matrices.popPose();
    }

    public static void drawFilledBoxAtWorld(
            PoseStack matrices,
            VertexConsumer fill,
            AABB box,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        double x1 = box.minX - cameraPos.x;
        double y1 = box.minY - cameraPos.y;
        double z1 = box.minZ - cameraPos.z;
        double x2 = box.maxX - cameraPos.x;
        double y2 = box.maxY - cameraPos.y;
        double z2 = box.maxZ - cameraPos.z;
        matrices.pushPose();
        drawFilledBoxFaces(matrices.last(), fill, x1, y1, z1, x2, y2, z2, r, g, b, a);
        matrices.popPose();
    }

    public static AABB interpolatedEntityBox(Entity entity, float partialTick, double inflate) {
        Vec3 at = entity.getPosition(partialTick);
        AABB original = entity.getBoundingBox();
        double width = original.getXsize();
        double height = original.getYsize();
        return new AABB(
                at.x - width / 2.0,
                at.y,
                at.z - width / 2.0,
                at.x + width / 2.0,
                at.y + height,
                at.z + width / 2.0
        ).inflate(inflate);
    }

    public static void drawFilledUpperHalfBlockAtWorld(
            PoseStack matrices,
            VertexConsumer fill,
            double blockX,
            double blockY,
            double blockZ,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        AABB box = new AABB(blockX, blockY + 0.5, blockZ, blockX + 1.0, blockY + 1.0, blockZ + 1.0);
        drawFilledBoxAtWorld(matrices, fill, box, cameraPos, r, g, b, a);
    }

    private static void drawFilledBoxFaces(
            PoseStack.Pose pose,
            VertexConsumer vc,
            double x1,
            double y1,
            double z1,
            double x2,
            double y2,
            double z2,
            float r,
            float g,
            float b,
            float a
    ) {
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z1, (float) x2, (float) y1, (float) z1, (float) x2, (float) y1, (float) z2, (float) x1, (float) y1, (float) z2, r, g, b, a, 0f, -1f, 0f);
        drawQuad(pose, vc, (float) x1, (float) y2, (float) z1, (float) x1, (float) y2, (float) z2, (float) x2, (float) y2, (float) z2, (float) x2, (float) y2, (float) z1, r, g, b, a, 0f, 1f, 0f);
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z1, (float) x1, (float) y1, (float) z2, (float) x1, (float) y2, (float) z2, (float) x1, (float) y2, (float) z1, r, g, b, a, -1f, 0f, 0f);
        drawQuad(pose, vc, (float) x2, (float) y1, (float) z2, (float) x2, (float) y1, (float) z1, (float) x2, (float) y2, (float) z1, (float) x2, (float) y2, (float) z2, r, g, b, a, 1f, 0f, 0f);
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z1, (float) x2, (float) y1, (float) z1, (float) x2, (float) y2, (float) z1, (float) x1, (float) y2, (float) z1, r, g, b, a, 0f, 0f, -1f);
        drawQuad(pose, vc, (float) x1, (float) y1, (float) z2, (float) x1, (float) y2, (float) z2, (float) x2, (float) y2, (float) z2, (float) x2, (float) y1, (float) z2, r, g, b, a, 0f, 0f, 1f);
    }

    private static void drawQuad(
            PoseStack.Pose pose,
            VertexConsumer vc,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz,
            float r,
            float g,
            float b,
            float a,
            float nx,
            float ny,
            float nz
    ) {
        vc.addVertex(pose.pose(), ax, ay, az).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), bx, by, bz).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), cx, cy, cz).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose.pose(), dx, dy, dz).setColor(r, g, b, a).setNormal(pose, nx, ny, nz);
    }

    public static void drawLineAtWorld(
            PoseStack matrices,
            VertexConsumer lines,
            Vec3 start,
            Vec3 end,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        matrices.pushPose();
        matrices.translate(start.x - cameraPos.x, start.y - cameraPos.y, start.z - cameraPos.z);
        drawLine(
                matrices.last(),
                lines,
                0,
                0,
                0,
                end.x - start.x,
                end.y - start.y,
                end.z - start.z,
                r,
                g,
                b,
                a
        );
        matrices.popPose();
    }

    private EntityDrawUtils() {}
}