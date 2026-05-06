package com.shyeuar.baity.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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

    private EntityDrawUtils() {}
}