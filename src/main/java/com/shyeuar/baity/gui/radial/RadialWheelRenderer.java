package com.shyeuar.baity.gui.radial;

import com.shyeuar.baity.gui.render.UiShapeRenderer;
import com.shyeuar.baity.gui.theme.LinearTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

public final class RadialWheelRenderer {

    private RadialWheelRenderer() {
    }

    public static final int OUTER_RADIUS = 80;
    public static final int INNER_RADIUS = 30;
    public static final int CENTER_RADIUS = 30;

    /** Target arc length per segment (~1 GUI px); capped to keep wheel cost bounded. */
    private static final double ARC_STEP_PX = 1.0;
    private static final int MIN_ARC_SEGMENTS = 6;
    private static final int MAX_ARC_SEGMENTS = 512;

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

    public enum CenterStyle {
        EXIT,
        BACK
    }

    private static final int CENTER_HUB_RADIUS = 11;

    private static final int BG = LinearTheme.BG_PRIMARY.getRGB();
    private static final int BG3 = LinearTheme.BG_TERTIARY.getRGB();
    private static final int ACCENT = LinearTheme.ACCENT_PRIMARY.getRGB();
    private static final int ACCENT2 = LinearTheme.ACCENT_SECONDARY.getRGB();
    private static final int BORDER = LinearTheme.BORDER_PRIMARY.getRGB();
    private static final int TEXT_SECONDARY = LinearTheme.TEXT_SECONDARY.getRGB();
    private static final int YELLOW = 0xFFFFFF55;
    private static final int YELLOW_RIM = 0xCCFFFF55;
    private static final int YELLOW_DARK = 0xFFCCCC44;

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
        drawUnicodeSymbol(graphics, font, symbol, centerX, centerY, scale, SYMBOL_ICON_COLOR);
    }

    public static void drawUnicodeSymbol(GuiGraphicsExtractor graphics, Font font, String symbol,
                                         float centerX, float centerY, float scale, int colorArgb) {
        if (symbol == null || symbol.isEmpty()) {
            return;
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(scale, scale);
        Component text = Component.literal(symbol);
        float w = font.width(text);
        graphics.text(font, text, Math.round(-w / 2f), Math.round(labelBaselineOffset(font)),
                colorArgb, false);
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

    public static void drawWheel(GuiGraphicsExtractor context, int centerX, int centerY) {
        final int baseOuter = OUTER_RADIUS;
        final int baseInner = OUTER_RADIUS - 7;

        int shellEdge = lerpArgb(BG3, BORDER, 0.4f);
        int ringFace = lerpArgb(BG3, BG, 0.15f);
        int ringDepth = lerpArgb(BG, 0x000000, 0.22f);

        UiShapeRenderer.drawCircle(context, centerX, centerY,
                segmentsForRadius(baseOuter), baseOuter, withAlpha(shellEdge, 0xFF));
        UiShapeRenderer.drawCircle(context, centerX, centerY,
                segmentsForRadius(baseInner), baseInner, withAlpha(ringFace, 0xF8));

        int depthInner = withAlpha(ringDepth, 0x55);
        int depthOuter = withAlpha(ringDepth, 0x00);
        drawRingSplit(context, centerX, centerY, 0, 360,
                baseInner, baseOuter - 2, depthInner, depthOuter);

        int highlight = withAlpha(lerpArgb(ringFace, 0xFFFFFF, 0.35f), 0x70);
        int shadow = withAlpha(lerpArgb(ringFace, 0x000000, 0.45f), 0x85);
        drawLayoutRing(context, centerX, centerY, -140, -40, baseOuter - 1.5, baseOuter + 1.2, highlight, highlight);
        drawLayoutRing(context, centerX, centerY, 40, 140, baseOuter - 1.5, baseOuter + 1.2, shadow, shadow);

        int accentRim = withAlpha(lerpArgb(ACCENT, ACCENT2, 0.4f), 0x45);
        drawRingSplit(context, centerX, centerY, 0, 360,
                baseOuter - 1, baseOuter + 0.75, accentRim, withAlpha(accentRim, 0x00));

        int fringeColor = withAlpha(shellEdge, 0x55);
        drawRingSplit(context, centerX, centerY, 0, 360,
                baseOuter - 0.2, baseOuter + 1.1, fringeColor, withAlpha(fringeColor, 0x00));

        int innerRadius = CENTER_RADIUS - 2;
        int innerFace = lerpArgb(BG, BG3, 0.25f);
        int innerRimHi = withAlpha(lerpArgb(innerFace, 0xFFFFFF, 0.28f), 0xA8);
        int innerRimLo = withAlpha(lerpArgb(innerFace, 0x000000, 0.35f), 0x90);

        UiShapeRenderer.drawCircle(context, centerX, centerY,
                segmentsForRadius(innerRadius), innerRadius, withAlpha(innerFace, 0xFF));
        drawLayoutRing(context, centerX, centerY, -130, -50, innerRadius - 1, innerRadius + 1,
                innerRimHi, innerRimHi);
        drawLayoutRing(context, centerX, centerY, 50, 130, innerRadius - 1, innerRadius + 1,
                innerRimLo, innerRimLo);
    }

    public static void drawSectorDividers(GuiGraphicsExtractor context, int centerX, int centerY,
                                          int sectionCount, double startAngle, double anglePerSection) {
        if (sectionCount <= 1) return;
        int c = withAlpha(lerpArgb(BORDER, 0x000000, 0.25f), 0x65);
        double inner = INNER_RADIUS + 1;
        double outer = OUTER_RADIUS - 8;
        for (int i = 0; i < sectionCount; i++) {
            double deg = startAngle + i * anglePerSection;
            drawLayoutRing(context, centerX, centerY, deg, deg + 0.35, inner, outer, c, c);
        }
    }

    public static void drawHoveredSector(GuiGraphicsExtractor context, int centerX, int centerY,
                                         double layoutStartDeg, double layoutEndDeg) {
        int inner = withAlpha(ACCENT, 0xE8);
        int outer = withAlpha(ACCENT2, 0xF8);
        drawLayoutRing(context, centerX, centerY, layoutStartDeg, layoutEndDeg,
                INNER_RADIUS, OUTER_RADIUS - 7, inner, outer);

        int hi = withAlpha(lerpArgb(ACCENT2, 0xFFFFFF, 0.4f), 0x90);
        int hiOut = withAlpha(lerpArgb(ACCENT2, 0xFFFFFF, 0.4f), 0x00);
        drawLayoutRing(context, centerX, centerY, layoutStartDeg, layoutEndDeg,
                OUTER_RADIUS - 9, OUTER_RADIUS - 5, hi, hiOut);
    }

    public static void drawCenterAvatarHub(GuiGraphicsExtractor context, int centerX, int centerY) {
        int hubFace = withAlpha(lerpArgb(BG3, BG, 0.2f), 0xFF);
        int hubRim = withAlpha(lerpArgb(BG3, BORDER, 0.35f), 0xEE);
        drawCenterHub(context, centerX, centerY, hubFace, hubRim);
    }

    public static void drawCenter(GuiGraphicsExtractor context, int centerX, int centerY, CenterStyle style) {
        switch (style) {
            case EXIT -> {
                int hubFace = withAlpha(lerpArgb(BG3, 0xFF6A5C, 0.35f), 0xFF);
                int hubRim = withAlpha(lerpArgb(0xFF8A7A, 0xFFB0A8, 0.5f), 0xEE);
                drawCenterHub(context, centerX, centerY, hubFace, hubRim);
                drawCenterCloseIcon(context, centerX, centerY, 0xFFFFECEA);
            }
            case BACK -> {
                int hubFace = withAlpha(lerpArgb(BG3, YELLOW_DARK, 0.45f), 0xFF);
                int hubRim = YELLOW_RIM;
                drawCenterHub(context, centerX, centerY, hubFace, hubRim);
                drawCenterBackIcon(context, centerX, centerY, YELLOW);
            }
        }
    }

    private static void drawCenterHub(GuiGraphicsExtractor context, int centerX, int centerY, int hubFace, int hubRim) {
        int hubSegments = segmentsForRadius(CENTER_HUB_RADIUS);
        UiShapeRenderer.drawCircle(context, centerX, centerY, hubSegments, CENTER_HUB_RADIUS, hubRim);
        UiShapeRenderer.drawCircle(context, centerX, centerY, hubSegments, 9.5, hubFace);
        int hubHi = withAlpha(lerpArgb(hubFace, 0xFFFFFF, 0.35f), 0x80);
        drawLayoutRing(context, centerX, centerY, -120, -60, 8.5, 10.5, hubHi, hubHi);
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

    private static void drawCenterCloseIcon(GuiGraphicsExtractor context, int centerX, int centerY, int color) {
        float half = 4.6f;
        float stroke = 2.15f;
        drawThickRoundLine(context, centerX - half, centerY - half, centerX + half, centerY + half, stroke, color);
        drawThickRoundLine(context, centerX - half, centerY + half, centerX + half, centerY - half, stroke, color);
    }

    private static void drawCenterBackIcon(GuiGraphicsExtractor context, int centerX, int centerY, int color) {
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

    private static void drawThickRoundLineCentered(GuiGraphicsExtractor context, int hubX, int hubY,
                                                   float geoX, float geoY,
                                                   float x0, float y0, float x1, float y1,
                                                   float thickness, int color) {
        drawThickRoundLine(context,
                hubX + x0 - geoX, hubY + y0 - geoY,
                hubX + x1 - geoX, hubY + y1 - geoY,
                thickness, color);
    }

    private static void drawThickRoundLine(GuiGraphicsExtractor context, float x0, float y0, float x1, float y1,
                                           float thickness, int color) {
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
            UiShapeRenderer.drawCircle(context, (int) x, (int) y, 10, radius, color);
        }
    }

    private static void drawLayoutRing(GuiGraphicsExtractor context, int centerX, int centerY,
                                       double layoutFromDeg, double layoutToDeg,
                                       double innerRadius, double outerRadius,
                                       int innerColor, int outerColor) {
        double renderFrom = toRenderAngle(layoutFromDeg);
        double renderTo = toRenderAngle(layoutToDeg);
        drawRingSplit(context, centerX, centerY, renderFrom, renderTo,
                innerRadius, outerRadius, innerColor, outerColor);
    }

    private static void drawRingSplit(GuiGraphicsExtractor context, int centerX, int centerY,
                                      double fromDeg, double toDeg,
                                      double innerRadius, double outerRadius,
                                      int innerColor, int outerColor) {
        double f = normalizeDeg(fromDeg);
        double t = normalizeDeg(toDeg);
        if (t <= f) t += 360d;

        double edgeRadius = Math.max(innerRadius, outerRadius);
        if (t <= 360d) {
            int segments = segmentsForArc(edgeRadius, t - f);
            UiShapeRenderer.drawRing(context, centerX, centerY, f, t, segments,
                    innerRadius, outerRadius, innerColor, outerColor);
        } else {
            int firstSegments = segmentsForArc(edgeRadius, 360d - f);
            UiShapeRenderer.drawRing(context, centerX, centerY, f, 360d, firstSegments,
                    innerRadius, outerRadius, innerColor, outerColor);
            int secondSegments = segmentsForArc(edgeRadius, t - 360d);
            UiShapeRenderer.drawRing(context, centerX, centerY, 0d, t - 360d, secondSegments,
                    innerRadius, outerRadius, innerColor, outerColor);
        }
    }

    private static int segmentsForRadius(double radius) {
        return segmentsForArc(radius, 360d);
    }

    private static int segmentsForArc(double radius, double angleDegrees) {
        if (angleDegrees <= 0d) {
            return MIN_ARC_SEGMENTS;
        }
        double clampedAngle = Math.min(360d, angleDegrees);
        double arcPx = Math.toRadians(clampedAngle) * Math.max(1d, radius);
        return Math.min(MAX_ARC_SEGMENTS, Math.max(MIN_ARC_SEGMENTS, (int) Math.ceil(arcPx / ARC_STEP_PX)));
    }

    private static double toRenderAngle(double layoutDegrees) {
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
