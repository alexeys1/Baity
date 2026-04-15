package com.shyeuar.baity.gui.owo;

import com.shyeuar.baity.gui.theme.LinearTheme;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RadialMenuComponent extends BaseUIComponent {

    public record Entry(String id, String icon, String displayName) {}

    private static final int OUTER_RADIUS = 80;
    private static final int INNER_RADIUS = 30;
    private static final int CENTER_RADIUS = 30;

    private static final int BG_COLOR = LinearTheme.BG_PRIMARY.getRGB();

    private final List<Entry> entries;
    private int hoveredIndex = -1;

    public RadialMenuComponent(List<Entry> entries) {
        this.entries = entries;
        this.horizontalSizing(Sizing.fill(100));
        this.verticalSizing(Sizing.fill(100));
        this.positioning(Positioning.absolute(0, 0));
    }

    public int hoveredIndex() {
        return hoveredIndex;
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);

        final int centerX = this.x + this.width / 2;
        final int centerY = this.y + this.height / 2;

        final double dx = mouseX - centerX;
        final double dy = mouseY - centerY;
        final double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= INNER_RADIUS) {
            this.hoveredIndex = -1;
            return;
        }

        if (dist > OUTER_RADIUS) {
            this.hoveredIndex = -1;
            return;
        }

        if (entries.isEmpty()) {
            this.hoveredIndex = -1;
            return;
        }

        double degrees = Math.toDegrees(Math.atan2(dy, dx));
        if (degrees < 0) degrees += 360;

        this.hoveredIndex = getSectionFromAngle(degrees, entries.size());

    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        final int centerX = this.x + this.width / 2;
        final int centerY = this.y + this.height / 2;

        final int segments = 220;

        final int baseOuter = OUTER_RADIUS;
        final int baseInner = OUTER_RADIUS - 7;

        int faceColor = BG_COLOR;
        int edgeColor = lerpArgb(BG_COLOR, LinearTheme.BG_SECONDARY.getRGB(), 0.35f);

        int edgeArgb = withAlpha(edgeColor, 0x99);
        int faceArgb = withAlpha(faceColor, 0x66);

        context.drawCircle(centerX, centerY, segments, baseOuter, Color.ofArgb(edgeArgb));
        context.drawCircle(centerX, centerY, segments, baseInner, Color.ofArgb(faceArgb));

        int aaInner = withAlpha(edgeColor, 0x60);
        int aaOuter = withAlpha(edgeColor, 0x00);
        drawRingSplit(context, centerX, centerY, 0, 360, segments,
                baseOuter - 1, baseOuter + 0.75, Color.ofArgb(aaInner), Color.ofArgb(aaOuter));

        int innerBase = withAlpha(lerpArgb(faceColor, 0x00000000, 0.18f), 0x66); // 略深主体，更透明
        int innerEdge = withAlpha(lerpArgb(faceColor, LinearTheme.BG_SECONDARY.getRGB(), 0.4f), 0x88); // 细亮边

        int innerRadius = CENTER_RADIUS - 2;
        context.drawCircle(centerX, centerY, segments, innerRadius, Color.ofArgb(innerBase));
        drawRingSplit(context, centerX, centerY, 0, 360, segments,
                innerRadius - 1, innerRadius + 1, Color.ofArgb(innerEdge), Color.ofArgb(innerEdge));

        drawIconsAndText(context, centerX, centerY);
    }

    private void drawRingSplit(OwoUIGraphics context, int centerX, int centerY,
                               double fromDeg, double toDeg, int segments,
                               double innerRadius, double outerRadius,
                               Color innerColor, Color outerColor) {
        double f = normalizeDeg(fromDeg);
        double t = normalizeDeg(toDeg);
        if (t <= f) t += 360d;

        if (t <= 360d) {
            context.drawRing(centerX, centerY, f, t, segments, innerRadius, outerRadius, innerColor, outerColor);
        } else {
            context.drawRing(centerX, centerY, f, 360d, segments, innerRadius, outerRadius, innerColor, outerColor);
            context.drawRing(centerX, centerY, 0d, t - 360d, segments, innerRadius, outerRadius, innerColor, outerColor);
        }
    }

    private void drawIconsAndText(OwoUIGraphics context, int centerX, int centerY) {
        if (entries.isEmpty()) return;

        final var mc = Minecraft.getInstance();
        if (mc == null) return;

        final var font = mc.font;
        final double anglePerSection = 360d / entries.size();
        final double startAngle = getStartAngle(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            final var entry = entries.get(i);
            final boolean hovered = (i == this.hoveredIndex);

            final double midAngle = Math.toRadians(startAngle + (i + .5d) * anglePerSection);
            final int iconRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
            final int iconX = centerX + (int) (Math.cos(midAngle) * iconRadius);
            final int iconY = centerY + (int) (Math.sin(midAngle) * iconRadius);

            final int textColor = hovered ? 0xFFFFFF00 : 0xFFFFFFFF;

            final float scale = 3f;
            final var iconText = Component.literal(entry.icon());
            final int iconW = font.width(iconText);
            final int iconH = font.lineHeight;

            float drawX = iconX - (iconW * scale) / 2f;
            float drawY = iconY - (iconH * scale) / 2f;
            int shadowColor = 0xAA000000;
            context.drawText(iconText, drawX + 1, drawY + 1, scale, shadowColor);
            context.drawText(iconText, drawX, drawY, scale, textColor);
        }

        if (this.hoveredIndex >= 0 && this.hoveredIndex < entries.size()) {
            final var entry = entries.get(this.hoveredIndex);
            final var label = Component.literal(entry.displayName());
            final int labelW = font.width(label);

            final double midAngle = Math.toRadians(startAngle + (this.hoveredIndex + .5d) * anglePerSection);
            final int labelRadius = OUTER_RADIUS + 15;
            final int labelX = centerX + (int) (Math.cos(midAngle) * labelRadius) - labelW / 2;
            final int labelY = centerY + (int) (Math.sin(midAngle) * labelRadius) - 4;

            int labelShadow = 0xAA000000;
            context.drawText(label, labelX + 1, labelY + 1, 1f, labelShadow);
            context.drawText(label, labelX, labelY, 1f, 0xFFFFFF00);
        }

        int centerOuterR = 7;
        int centerInnerR = 4;
        context.drawCircle(centerX, centerY, 48, centerOuterR, Color.ofArgb(0xCCFFFFFF));
        context.drawCircle(centerX, centerY, 48, centerInnerR, Color.ofArgb(withAlpha(BG_COLOR, 0xFF)));
    }

    private static double getStartAngle(int sectionCount) {
        return -90 - (360.0 / sectionCount) / 2;
    }

    private static int getSectionFromAngle(double degrees, int sectionCount) {
        double anglePerSection = 360.0 / sectionCount;
        double startAngle = -90 - anglePerSection / 2;
        if (startAngle < 0) startAngle += 360;

        double adjustedDegrees = degrees - startAngle;
        if (adjustedDegrees < 0) adjustedDegrees += 360;

        int section = (int) (adjustedDegrees / anglePerSection);
        return section % sectionCount;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int lerpArgb(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(lerp(aa, ba, t));
        int rr = Math.round(lerp(ar, br, t));
        int rg = Math.round(lerp(ag, bg, t));
        int rb = Math.round(lerp(ab, bb, t));
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static double normalizeDeg(double deg) {
        deg %= 360d;
        if (deg < 0) deg += 360d;
        return deg;
    }
}


