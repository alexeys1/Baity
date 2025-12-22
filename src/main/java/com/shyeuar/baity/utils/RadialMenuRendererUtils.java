package com.shyeuar.baity.utils;

import net.minecraft.client.gui.DrawContext;

public class RadialMenuRendererUtils {

    public static final int OUTER_RADIUS = 80;
    public static final int INNER_RADIUS = 30;
    public static final int BORDER_COLOR = 0xA0202020;
    public static final int SECTION_COLOR = 0x60362A96;
    public static final int SECTION_HOVER_COLOR = 0xC0362A96;
    public static final int CENTER_COLOR = 0xA0202020;

    public static void drawFilledCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        int radiusSq = radius * radius;
        for (int y = -radius; y <= radius; y++) {
            int halfWidth = (int) Math.sqrt(radiusSq - y * y);
            if (halfWidth > 0) {
                context.fill(centerX - halfWidth, centerY + y, centerX + halfWidth, centerY + y + 1, color);
            }
        }
    }

    public static void drawRadialLine(DrawContext context, int centerX, int centerY,
                                      int innerRadius, int outerRadius, double angle, int color) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (int i = innerRadius; i <= outerRadius; i++) {
            int x = centerX + (int) (cos * i);
            int y = centerY + (int) (sin * i);
            context.fill(x, y, x + 1, y + 1, color);
        }
    }

    public static void drawArcSection(DrawContext context, int centerX, int centerY,
                                      int innerRadius, int outerRadius,
                                      double startAngleDeg, double endAngleDeg, int color) {
        int outerRadiusSq = outerRadius * outerRadius;
        int innerRadiusSq = innerRadius * innerRadius;

        for (int dy = -outerRadius; dy <= outerRadius; dy++) {
            int y = centerY + dy;
            int outerHalfWidth = (int) Math.sqrt(Math.max(0, outerRadiusSq - dy * dy));
            if (outerHalfWidth == 0) continue;

            int segStart = Integer.MIN_VALUE;
            for (int dx = -outerHalfWidth; dx <= outerHalfWidth; dx++) {
                int distSq = dx * dx + dy * dy;
                boolean inRing = distSq >= innerRadiusSq && distSq <= outerRadiusSq;

                boolean inAngle = false;
                if (inRing) {
                    double pointAngleDeg = Math.toDegrees(Math.atan2(dy, dx));
                    inAngle = isAngleInRange(pointAngleDeg, startAngleDeg, endAngleDeg);
                }

                if (inRing && inAngle) {
                    if (segStart == Integer.MIN_VALUE) segStart = dx;
                } else {
                    if (segStart != Integer.MIN_VALUE) {
                        context.fill(centerX + segStart, y, centerX + dx, y + 1, color);
                        segStart = Integer.MIN_VALUE;
                    }
                }
            }
            if (segStart != Integer.MIN_VALUE) {
                context.fill(centerX + segStart, y, centerX + outerHalfWidth + 1, y + 1, color);
            }
        }
    }

    public static double normalizeAngle360(double angle) {
        while (angle < 0) angle += 360;
        while (angle >= 360) angle -= 360;
        return angle;
    }

    public static boolean isAngleInRange(double angle, double startDeg, double endDeg) {
        angle = normalizeAngle360(angle);
        double normStart = normalizeAngle360(startDeg);
        double normEnd = normalizeAngle360(endDeg);

        if (normStart <= normEnd) {
            return angle >= normStart && angle < normEnd;
        } else {
            return angle >= normStart || angle < normEnd;
        }
    }

    public static int getSectionFromAngle(double degrees, int sectionCount) {
        double anglePerSection = 360.0 / sectionCount;
        double startAngle = 270 - anglePerSection / 2;
        if (startAngle < 0) startAngle += 360;

        double adjustedDegrees = degrees - startAngle;
        if (adjustedDegrees < 0) adjustedDegrees += 360;

        int section = (int) (adjustedDegrees / anglePerSection);
        return section % sectionCount;
    }

    public static double getStartAngle(int sectionCount) {
        return -90 - (360.0 / sectionCount) / 2;
    }
}