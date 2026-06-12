package com.shyeuar.baity.gui.owo;

import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.theme.LinearTheme;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.List;

public class RadialMenuComponent extends BaseUIComponent {

    public static final int OUTER_RADIUS = 80;
    public static final int INNER_RADIUS = 30;
    public static final int CENTER_RADIUS = 30;
    public static final int RING_SEGMENTS = 220;

    public static final float ICON_BASE_SCALE = 3f;
    public static final int WARP_ICON_BASE_SIZE = 24;
    public static final int CENTER_HEAD_SIZE = 24;
    public static final int ICON_RADIUS_INSET = 4;

    private static final int SKIN_HEAD_U = 8;
    private static final int SKIN_HEAD_V = 8;
    private static final int SKIN_HAT_U = 40;
    private static final int SKIN_HAT_V = 8;
    private static final int SKIN_HEAD_TEXEL = 8;
    private static final float HEAD_FACE_INNER_RATIO = 48f / 56f;

    public static final int LABEL_TEXT_COLOR = 0xFFFF55FF;
    public static final int SYMBOL_ICON_COLOR = 0xFFFFFFFF;

    public record Entry(String id, String icon, String displayName) {}

    public enum CenterStyle {
        EXIT,
        BACK
    }

    private static final int CENTER_HUB_SEGMENTS = 40;

    private static final int BG = LinearTheme.BG_PRIMARY.getRGB();
    private static final int BG3 = LinearTheme.BG_TERTIARY.getRGB();
    private static final int ACCENT = LinearTheme.ACCENT_PRIMARY.getRGB();
    private static final int ACCENT2 = LinearTheme.ACCENT_SECONDARY.getRGB();
    private static final int BORDER = LinearTheme.BORDER_PRIMARY.getRGB();
    private static final int TEXT_SECONDARY = LinearTheme.TEXT_SECONDARY.getRGB();
    private static final int YELLOW = 0xFFFFFF55;
    private static final int YELLOW_RIM = 0xCCFFFF55;
    private static final int YELLOW_DARK = 0xFFCCCC44;

    private final List<Entry> entries;
    private int hoveredIndex = -1;
    private GuiGraphicsExtractor guiGraphics;

    public RadialMenuComponent(List<Entry> entries) {
        this.entries = entries;
        this.horizontalSizing(Sizing.fill(100));
        this.verticalSizing(Sizing.fill(100));
        this.positioning(Positioning.absolute(0, 0));
    }

    public int hoveredIndex() {
        return hoveredIndex;
    }

    public void setGuiGraphics(GuiGraphicsExtractor guiGraphics) {
        this.guiGraphics = guiGraphics;
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);

        final int centerX = this.x + this.width / 2;
        final int centerY = this.y + this.height / 2;

        final double dx = mouseX - centerX;
        final double dy = mouseY - centerY;
        final double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist <= INNER_RADIUS || dist > OUTER_RADIUS || entries.isEmpty()) {
            this.hoveredIndex = -1;
        } else {
            double degrees = Math.toDegrees(Math.atan2(dy, dx));
            if (degrees < 0) degrees += 360;
            this.hoveredIndex = getSectionFromAngle(degrees, entries.size());
        }
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        final int centerX = this.x + this.width / 2;
        final int centerY = this.y + this.height / 2;
        if (guiGraphics == null) {
            drawWheelContent(context, centerX, centerY);
            return;
        }

        Minecraft client = Minecraft.getInstance();
        float ratio = ClickGuiState.fixedScaleRatio(client);
        var pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(ratio, ratio);
        pose.translate(-centerX, -centerY);
        try {
            drawWheelContent(context, centerX, centerY);
            drawIconsAndText(centerX, centerY);
        } finally {
            pose.popMatrix();
        }
    }

    private void drawWheelContent(OwoUIGraphics context, int centerX, int centerY) {
        drawWheel(context, centerX, centerY);

        if (!entries.isEmpty()) {
            final double anglePerSection = 360d / entries.size();
            final double startAngle = getStartAngle(entries.size());
            drawSectorDividers(context, centerX, centerY, entries.size(), startAngle, anglePerSection);

            if (hoveredIndex >= 0 && hoveredIndex < entries.size()) {
                double sectionStart = startAngle + hoveredIndex * anglePerSection;
                drawHoveredSector(context, centerX, centerY, sectionStart, sectionStart + anglePerSection);
            }
        }
    }

    private void drawIconsAndText(int centerX, int centerY) {
        if (entries.isEmpty() || guiGraphics == null) return;

        final var mc = Minecraft.getInstance();
        if (mc == null) return;

        final Font font = mc.font;
        final double anglePerSection = 360d / entries.size();
        final double startAngle = getStartAngle(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            final var entry = entries.get(i);
            final float[] pos = sectorCenter(
                    centerX, centerY, startAngle, anglePerSection, i, INNER_RADIUS, OUTER_RADIUS);
            drawUnicodeSymbol(guiGraphics, font, entry.icon(), pos[0], pos[1], ICON_BASE_SCALE);
        }

        if (hoveredIndex >= 0 && hoveredIndex < entries.size()) {
            final String label = entries.get(hoveredIndex).displayName();
            float[] labelPos = sectorLabelPosition(
                    centerX, centerY, startAngle, anglePerSection, hoveredIndex, OUTER_RADIUS + 15, font, label);
            drawRadialLabel(guiGraphics, font, label, labelPos[0], labelPos[1]);
        }
    }

    public static int textSecondary() {
        return TEXT_SECONDARY;
    }

    public static int getSectionFromAngle(double degrees, int sectionCount) {
        double anglePerSection = 360.0 / sectionCount;
        double startAngle = getStartAngle(sectionCount);
        if (startAngle < 0) startAngle += 360;

        double adjustedDegrees = degrees - startAngle;
        if (adjustedDegrees < 0) adjustedDegrees += 360;

        return ((int) (adjustedDegrees / anglePerSection)) % sectionCount;
    }

    public static double getStartAngle(int sectionCount) {
        return -90 - (360.0 / sectionCount) / 2;
    }

    public static void drawUnicodeSymbol(GuiGraphicsExtractor graphics, Font font, String symbol,
                                         float centerX, float centerY, float scale) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(scale, scale);
        Component text = Component.literal(symbol);
        float w = font.width(text);
        graphics.text(font, text, Math.round(-w / 2f), Math.round(labelBaselineOffset(font)),
                SYMBOL_ICON_COLOR, false);
        pose.popMatrix();
    }

    public static float[] sectorCenter(int centerX, int centerY, double startAngle, double anglePerSection,
                                       int sectionIndex, int innerRadius, int outerRadius) {
        double midDeg = startAngle + (sectionIndex + 0.5) * anglePerSection;
        double midRad = Math.toRadians(midDeg);
        double radius = sectorIconRadius(innerRadius, outerRadius);
        return new float[]{
                (float) (centerX + Math.cos(midRad) * radius),
                (float) (centerY + Math.sin(midRad) * radius)
        };
    }

    public static float[] sectorLabelPosition(int centerX, int centerY, double startAngle, double anglePerSection,
                                              int sectionIndex, int labelRadius, Font font, String text) {
        double midDeg = startAngle + (sectionIndex + 0.5) * anglePerSection;
        double midRad = Math.toRadians(midDeg);
        float cx = (float) (centerX + Math.cos(midRad) * labelRadius);
        float cy = (float) (centerY + Math.sin(midRad) * labelRadius);
        return new float[]{
                cx - font.width(text) / 2f,
                cy + labelBaselineOffset(font)
        };
    }

    public static float labelBaselineOffset(Font font) {
        return -font.lineHeight / 2f + 1f;
    }

    public static void drawWheel(OwoUIGraphics context, int centerX, int centerY) {
        final int segments = RING_SEGMENTS;
        final int baseOuter = OUTER_RADIUS;
        final int baseInner = OUTER_RADIUS - 7;

        int shellEdge = lerpArgb(BG3, BORDER, 0.4f);
        int ringFace = lerpArgb(BG3, BG, 0.15f);
        int ringDepth = lerpArgb(BG, 0x000000, 0.22f);

        context.drawCircle(centerX, centerY, segments, baseOuter, Color.ofArgb(withAlpha(shellEdge, 0xFF)));
        context.drawCircle(centerX, centerY, segments, baseInner, Color.ofArgb(withAlpha(ringFace, 0xF8)));

        int depthInner = withAlpha(ringDepth, 0x55);
        int depthOuter = withAlpha(ringDepth, 0x00);
        drawRingSplit(context, centerX, centerY, 0, 360, segments,
                baseInner, baseOuter - 2, Color.ofArgb(depthInner), Color.ofArgb(depthOuter));

        int highlight = withAlpha(lerpArgb(ringFace, 0xFFFFFF, 0.35f), 0x70);
        int shadow = withAlpha(lerpArgb(ringFace, 0x000000, 0.45f), 0x85);
        drawLayoutRing(context, centerX, centerY, -140, -40, baseOuter - 1.5, baseOuter + 1.2, Color.ofArgb(highlight), Color.ofArgb(highlight));
        drawLayoutRing(context, centerX, centerY, 40, 140, baseOuter - 1.5, baseOuter + 1.2, Color.ofArgb(shadow), Color.ofArgb(shadow));

        int accentRim = withAlpha(lerpArgb(ACCENT, ACCENT2, 0.4f), 0x45);
        drawRingSplit(context, centerX, centerY, 0, 360, segments,
                baseOuter - 1, baseOuter + 0.75, Color.ofArgb(accentRim), Color.ofArgb(withAlpha(accentRim, 0x00)));

        int innerRadius = CENTER_RADIUS - 2;
        int innerFace = lerpArgb(BG, BG3, 0.25f);
        int innerRimHi = withAlpha(lerpArgb(innerFace, 0xFFFFFF, 0.28f), 0xA8);
        int innerRimLo = withAlpha(lerpArgb(innerFace, 0x000000, 0.35f), 0x90);

        context.drawCircle(centerX, centerY, segments, innerRadius, Color.ofArgb(withAlpha(innerFace, 0xFF)));
        drawLayoutRing(context, centerX, centerY, -130, -50, innerRadius - 1, innerRadius + 1,
                Color.ofArgb(innerRimHi), Color.ofArgb(innerRimHi));
        drawLayoutRing(context, centerX, centerY, 50, 130, innerRadius - 1, innerRadius + 1,
                Color.ofArgb(innerRimLo), Color.ofArgb(innerRimLo));
    }

    public static void drawSectorDividers(OwoUIGraphics context, int centerX, int centerY,
                                          int sectionCount, double startAngle, double anglePerSection) {
        if (sectionCount <= 1) return;
        Color c = Color.ofArgb(withAlpha(lerpArgb(BORDER, 0x000000, 0.25f), 0x65));
        double inner = INNER_RADIUS + 1;
        double outer = OUTER_RADIUS - 8;
        for (int i = 0; i < sectionCount; i++) {
            double deg = startAngle + i * anglePerSection;
            drawLayoutRing(context, centerX, centerY, deg, deg + 0.35, inner, outer, c, c);
        }
    }

    public static void drawHoveredSector(OwoUIGraphics context, int centerX, int centerY,
                                         double layoutStartDeg, double layoutEndDeg) {
        Color inner = Color.ofArgb(withAlpha(ACCENT, 0xE8));
        Color outer = Color.ofArgb(withAlpha(ACCENT2, 0xF8));
        drawLayoutRing(context, centerX, centerY, layoutStartDeg, layoutEndDeg,
                INNER_RADIUS, OUTER_RADIUS - 7, inner, outer);

        Color hi = Color.ofArgb(withAlpha(lerpArgb(ACCENT2, 0xFFFFFF, 0.4f), 0x90));
        Color hiOut = Color.ofArgb(withAlpha(lerpArgb(ACCENT2, 0xFFFFFF, 0.4f), 0x00));
        drawLayoutRing(context, centerX, centerY, layoutStartDeg, layoutEndDeg,
                OUTER_RADIUS - 9, OUTER_RADIUS - 5, hi, hiOut);
    }

    public static void drawCenter(OwoUIGraphics context, int centerX, int centerY, CenterStyle style) {
        switch (style) {
            case EXIT -> {
                int hubFace = withAlpha(lerpArgb(BG3, 0xFF6A5C, 0.35f), 0xFF);
                int hubRim = withAlpha(lerpArgb(0xFF8A7A, 0xFFB0A8, 0.5f), 0xEE);
                drawCenterHub(context, centerX, centerY, hubFace, hubRim);
                drawCenterCloseIcon(context, centerX, centerY, Color.ofArgb(0xFFFFECEA));
            }
            case BACK -> {
                int hubFace = withAlpha(lerpArgb(BG3, YELLOW_DARK, 0.45f), 0xFF);
                int hubRim = YELLOW_RIM;
                drawCenterHub(context, centerX, centerY, hubFace, hubRim);
                drawCenterBackIcon(context, centerX, centerY, Color.ofArgb(YELLOW));
            }
        }
    }

    private static void drawCenterHub(OwoUIGraphics context, int centerX, int centerY, int hubFace, int hubRim) {
        context.drawCircle(centerX, centerY, CENTER_HUB_SEGMENTS, 11, Color.ofArgb(hubRim));
        context.drawCircle(centerX, centerY, CENTER_HUB_SEGMENTS, 9.5, Color.ofArgb(hubFace));
        int hubHi = withAlpha(lerpArgb(hubFace, 0xFFFFFF, 0.35f), 0x80);
        drawLayoutRing(context, centerX, centerY, -120, -60, 8.5, 10.5, Color.ofArgb(hubHi), Color.ofArgb(hubHi));
    }

    public static void drawCenterPlayerHead(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

        PlayerSkin skin = client.player.getSkin();
        if (skin == null || skin.body() == null) return;

        Identifier texture = skin.body().texturePath();
        if (texture == null) return;

        int texSize = resolveSkinTextureSize(client, texture);
        int region = skinUv(SKIN_HEAD_TEXEL, texSize);
        int faceU = skinUv(SKIN_HEAD_U, texSize);
        int faceV = skinUv(SKIN_HEAD_V, texSize);
        int hatU = skinUv(SKIN_HAT_U, texSize);
        int hatV = skinUv(SKIN_HAT_V, texSize);

        int outer = CENTER_HEAD_SIZE;
        int inner = Math.round(outer * HEAD_FACE_INNER_RATIO);
        int inset = (outer - inner) / 2;
        int x = centerX - outer / 2;
        int y = centerY - outer / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + inset, y + inset,
                faceU, faceV, inner, inner, region, region, texSize, texSize);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y,
                hatU, hatV, outer, outer, region, region, texSize, texSize);
    }

    private static int resolveSkinTextureSize(Minecraft client, Identifier skinId) {
        AbstractTexture tex = client.getTextureManager().getTexture(skinId);
        if (tex instanceof DynamicTexture dynamic) {
            var image = dynamic.getPixels();
            if (image != null && image.getWidth() >= 128) {
                return image.getWidth();
            }
        }
        return 64;
    }

    private static int skinUv(int base64, int textureSize) {
        return base64 * textureSize / 64;
    }

    public static void drawRadialLabel(GuiGraphicsExtractor graphics, Font font, String text, float x, float y) {
        if (text == null || text.isEmpty()) return;
        graphics.text(font, text, Math.round(x), Math.round(y), LABEL_TEXT_COLOR, true);
    }

    public static void drawLabel(GuiGraphicsExtractor graphics, Font font, String text, float x, float y, int color) {
        if (text == null || text.isEmpty()) return;
        graphics.text(font, text, Math.round(x), Math.round(y), color, true);
    }

    private static double sectorIconRadius(int innerRadius, int outerRadius) {
        double mid = (innerRadius + outerRadius) * 0.5;
        return Math.max(innerRadius + 4, mid - ICON_RADIUS_INSET);
    }

    private static void drawCenterCloseIcon(OwoUIGraphics context, int centerX, int centerY, Color color) {
        float half = 4.6f;
        float stroke = 2.15f;
        drawThickRoundLine(context, centerX - half, centerY - half, centerX + half, centerY + half, stroke, color);
        drawThickRoundLine(context, centerX - half, centerY + half, centerX + half, centerY - half, stroke, color);
    }

    private static void drawCenterBackIcon(OwoUIGraphics context, int centerX, int centerY, Color color) {
        float stroke = 2.05f;
        float pad = stroke * 0.52f;
        float tailX = 3.6f;
        float tipX = -5.2f;
        float midY = 0.5f;
        float headSpread = 3.4f;
        float headDepth = 3.2f;
        float headX = tipX + headDepth;
        float geoX = (tipX - pad + tailX + pad) * 0.5f;
        float geoY = (midY - headSpread - pad + midY + headSpread + pad) * 0.5f;
        drawThickRoundLineCentered(context, centerX, centerY, geoX, geoY, tailX, midY, tipX, midY, stroke, color);
        drawThickRoundLineCentered(context, centerX, centerY, geoX, geoY, tipX, midY, headX, midY - headSpread, stroke, color);
        drawThickRoundLineCentered(context, centerX, centerY, geoX, geoY, tipX, midY, headX, midY + headSpread, stroke, color);
    }

    private static void drawThickRoundLineCentered(OwoUIGraphics context, int hubX, int hubY,
                                                   float geoX, float geoY,
                                                   float x0, float y0, float x1, float y1,
                                                   float thickness, Color color) {
        drawThickRoundLine(context,
                hubX + x0 - geoX, hubY + y0 - geoY,
                hubX + x1 - geoX, hubY + y1 - geoY,
                thickness, color);
    }

    private static void drawThickRoundLine(OwoUIGraphics context, float x0, float y0, float x1, float y1,
                                           float thickness, Color color) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;

        float step = Math.max(0.32f, thickness * 0.42f);
        int segments = Math.max(1, Math.round(len / step));
        double radius = thickness * 0.52;

        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float x = x0 + dx * t;
            float y = y0 + dy * t;
            context.drawCircle(Math.round(x), Math.round(y), 10, radius, color);
        }
    }

    private static void drawLayoutRing(OwoUIGraphics context, int centerX, int centerY,
                                       double layoutFromDeg, double layoutToDeg,
                                       double innerRadius, double outerRadius,
                                       Color innerColor, Color outerColor) {
        double owoFrom = toOwoAngle(layoutFromDeg);
        double owoTo = toOwoAngle(layoutToDeg);
        drawRingSplit(context, centerX, centerY, owoFrom, owoTo, RING_SEGMENTS,
                innerRadius, outerRadius, innerColor, outerColor);
    }

    private static void drawRingSplit(OwoUIGraphics context, int centerX, int centerY,
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

    private static double toOwoAngle(double layoutDegrees) {
        return normalizeDeg(layoutDegrees + 180.0);
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
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

    private static double normalizeDeg(double deg) {
        deg %= 360d;
        if (deg < 0) deg += 360d;
        return deg;
    }
}
