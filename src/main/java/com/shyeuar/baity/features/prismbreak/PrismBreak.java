package com.shyeuar.baity.features.prismbreak;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.mixin.accessor.MultiPlayerGameModeAccessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PrismBreak {

    private static final HashMap<Long, Float> SMOOTH_REACH = new HashMap<>();
    private static long lastFrameMs;
    private static BlockPos lastLocalPos;
    private static float lastLocalRaw = -1.0f;
    private static long lastLocalRawMs;
    private static float localVelocity;

    public static boolean isActive() {
        return ConfigManager.prismBreakEnabled;
    }

    public static void render(LevelRenderer levelRenderer, LevelRenderState levelRenderState) {
        if (!isActive()) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        double speed = ConfigManager.prismBreakSpeed;
        float edgeWidth = (float) ConfigManager.prismBreakEdgeWidth;
        float lightness = (float) Mth.clamp(ConfigManager.prismBreakChromaLightness, 0.2, 1.0);
        float saturation = (float) (Mth.clamp(ConfigManager.prismBreakChromaChroma, 0.0, 0.4) / 0.4);
        float size = (float) Math.max(0.1, ConfigManager.prismBreakChromaSize);
        long now = Util.getMillis();
        float dt = lastFrameMs == 0L ? 0.016f : Mth.clamp((now - lastFrameMs) / 1000.0f, 0.001f, 0.05f);
        lastFrameMs = now;
        HashSet<Long> seen = new HashSet<>();
        if (!levelRenderState.blockBreakingRenderStates.isEmpty()) {
            try (var _ = levelRenderer.collectPerFrameGizmos()) {
                for (BlockBreakingRenderState state : levelRenderState.blockBreakingRenderStates) {
                    if (state.blockState().getRenderShape() != RenderShape.MODEL) {
                        continue;
                    }
                    BlockPos pos = state.blockPos();
                    long key = pos.asLong();
                    float targetReach = resolveTargetReach(pos, state.progress());
                    Float stored = SMOOTH_REACH.get(key);
                    float reach;
                    if (stored == null || isLocalDestroying(pos)) {
                        reach = targetReach;
                    } else {
                        float alpha = 1.0f - (float) Math.exp(-18.0 * dt);
                        reach = stored + (targetReach - stored) * alpha;
                    }
                    SMOOTH_REACH.put(key, reach);
                    seen.add(key);
                    float phase = (float) ((now / 1000.0) * (speed * 0.5));
                    float posOffset = (float) ((Math.floorMod(key, 100)) * 0.01);
                    var shape = state.blockState().getShape(level, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    var boxes = shape.toAabbs();
                    if (boxes.size() > 8) {
                        boxes = List.of(shape.bounds());
                    }
                    if (state.blockState().getBlock() instanceof CrossCollisionBlock) {
                        drawCrossOutline(boxes, pos, reach, phase, posOffset, saturation, lightness, size, edgeWidth);
                    } else {
                        for (var box : boxes) {
                            drawBrackets(pos, box, reach, phase, posOffset, saturation, lightness, size, edgeWidth);
                        }
                    }
                }
            }
        }
        SMOOTH_REACH.keySet().removeIf(k -> !seen.contains(k));
        if (Minecraft.getInstance().gameMode == null || !Minecraft.getInstance().gameMode.isDestroying()) {
            lastLocalPos = null;
            lastLocalRaw = -1.0f;
            localVelocity = 0.0f;
        }
    }

    private static boolean isLocalDestroying(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || !mc.gameMode.isDestroying()) {
            return false;
        }
        BlockPos destroying = ((MultiPlayerGameModeAccessor) mc.gameMode).baity$getDestroyBlockPos();
        return destroying != null && destroying.equals(pos);
    }

    private static float resolveTargetReach(BlockPos pos, int progress) {
        if (isLocalDestroying(pos)) {
            return resolveLocalReach(pos);
        }
        return Math.min(1.0f, (progress + 1) / 10.0f);
    }

    private static float resolveLocalReach(BlockPos pos) {
        float raw = Mth.clamp(((MultiPlayerGameModeAccessor) Minecraft.getInstance().gameMode).baity$getDestroyProgress(), 0.0f, 1.0f);
        long now = Util.getMillis();
        if (lastLocalPos == null || !lastLocalPos.equals(pos) || lastLocalRaw < 0.0f) {
            lastLocalPos = pos.immutable();
            lastLocalRaw = raw;
            lastLocalRawMs = now;
            localVelocity = 0.0f;
            return raw;
        }
        if (Float.compare(raw, lastLocalRaw) != 0) {
            float dt = Math.max(0.001f, (now - lastLocalRawMs) / 1000.0f);
            localVelocity = (raw - lastLocalRaw) / dt;
            lastLocalRaw = raw;
            lastLocalRawMs = now;
        }
        float extrapolated = raw + localVelocity * ((now - lastLocalRawMs) / 1000.0f);
        return Mth.clamp(extrapolated, 0.0f, 1.0f);
    }

    private static void drawBrackets(BlockPos pos, AABB box, float reach, float phase, float posOffset,
                                     float saturation, float lightness, float size, float edgeWidth) {
        double mx = pos.getX() + box.minX;
        double my = pos.getY() + box.minY;
        double mz = pos.getZ() + box.minZ;
        double Mx = pos.getX() + box.maxX;
        double My = pos.getY() + box.maxY;
        double Mz = pos.getZ() + box.maxZ;
        double ex = Mx - mx;
        double ey = My - my;
        double ez = Mz - mz;
        if (ex <= 0.0 || ey <= 0.0 || ez <= 0.0) {
            return;
        }
        double armX = reach * ex * 0.5;
        double armY = reach * ey * 0.5;
        double armZ = reach * ez * 0.5;
        double centerX = (mx + Mx) * 0.5;
        double centerZ = (mz + Mz) * 0.5;
        for (int dz = 0; dz < 2; dz++) {
            for (int dy = 0; dy < 2; dy++) {
                for (int dx = 0; dx < 2; dx++) {
                    double cx = dx == 0 ? mx : Mx;
                    double cy = dy == 0 ? my : My;
                    double cz = dz == 0 ? mz : Mz;
                    double tx = cx + (dx == 0 ? armX : -armX);
                    double ty = cy + (dy == 0 ? armY : -armY);
                    double tz = cz + (dz == 0 ? armZ : -armZ);
                    drawArm(cx, cy, cz, tx, cy, cz, (cx + tx) * 0.5, cy, cz, centerX, centerZ, phase, posOffset, saturation, lightness, size, edgeWidth);
                    drawArm(cx, cy, cz, cx, ty, cz, cx, (cy + ty) * 0.5, cz, centerX, centerZ, phase, posOffset, saturation, lightness, size, edgeWidth);
                    drawArm(cx, cy, cz, cx, cy, tz, cx, cy, (cz + tz) * 0.5, centerX, centerZ, phase, posOffset, saturation, lightness, size, edgeWidth);
                    drawJoint(cx, cy, cz, centerX, centerZ, phase, posOffset, saturation, lightness, size, edgeWidth);
                }
            }
        }
    }

    private static void drawCrossOutline(List<AABB> boxes, BlockPos pos, float reach, float phase, float posOffset,
                                         float saturation, float lightness, float size, float edgeWidth) {
        double bx = pos.getX();
        double by = pos.getY();
        double bz = pos.getZ();
        double minX = 1.0;
        double minY = 1.0;
        double minZ = 1.0;
        double maxX = 0.0;
        double maxY = 0.0;
        double maxZ = 0.0;
        for (AABB b : boxes) {
            minX = Math.min(minX, b.minX);
            minY = Math.min(minY, b.minY);
            minZ = Math.min(minZ, b.minZ);
            maxX = Math.max(maxX, b.maxX);
            maxY = Math.max(maxY, b.maxY);
            maxZ = Math.max(maxZ, b.maxZ);
        }
        double worldCenterX = bx + (minX + maxX) * 0.5;
        double worldCenterZ = bz + (minZ + maxZ) * 0.5;
        for (AABB b : boxes) {
            double mx = b.minX;
            double my = b.minY;
            double mz = b.minZ;
            double Mx = b.maxX;
            double My = b.maxY;
            double Mz = b.maxZ;
            double[] xs = {mx, Mx};
            double[] ys = {my, My};
            double[] zs = {mz, Mz};
            for (double y : ys) {
                for (double z : zs) {
                    crossEdge(boxes, bx, by, bz, worldCenterX, worldCenterZ, reach, phase, posOffset, saturation, lightness, size, edgeWidth, xs[0], y, z, xs[1], y, z);
                }
            }
            for (double x : xs) {
                for (double z : zs) {
                    crossEdge(boxes, bx, by, bz, worldCenterX, worldCenterZ, reach, phase, posOffset, saturation, lightness, size, edgeWidth, x, ys[0], z, x, ys[1], z);
                }
            }
            for (double x : xs) {
                for (double y : ys) {
                    crossEdge(boxes, bx, by, bz, worldCenterX, worldCenterZ, reach, phase, posOffset, saturation, lightness, size, edgeWidth, x, y, zs[0], x, y, zs[1]);
                }
            }
        }
    }

    private static void crossEdge(List<AABB> boxes, double bx, double by, double bz,
                                  double worldCenterX, double worldCenterZ, float reach, float phase, float posOffset,
                                  float saturation, float lightness, float size, float edgeWidth,
                                  double a0, double a1, double a2, double b0, double b1, double b2) {
        if (!edgeBoundary(boxes, a0, a1, a2, b0, b1, b2)) {
            return;
        }
        double dx = b0 - a0;
        double dy = b1 - a1;
        double dz = b2 - a2;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0.0) {
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double uz = dz / len;
        double arm = reach * len * 0.5;
        if (isOuterCorner(boxes, a0, a1, a2)) {
            drawArm(bx + a0, by + a1, bz + a2, bx + a0 + ux * arm, by + a1 + uy * arm, bz + a2 + uz * arm,
                    bx + a0 + ux * arm * 0.5, by + a1 + uy * arm * 0.5, bz + a2 + uz * arm * 0.5,
                    worldCenterX, worldCenterZ, phase, posOffset, saturation, lightness, size, edgeWidth);
            drawJoint(bx + a0, by + a1, bz + a2, worldCenterX, worldCenterZ, phase, posOffset, saturation, lightness, size, edgeWidth);
        }
        if (isOuterCorner(boxes, b0, b1, b2)) {
            drawArm(bx + b0, by + b1, bz + b2, bx + b0 - ux * arm, by + b1 - uy * arm, bz + b2 - uz * arm,
                    bx + b0 - ux * arm * 0.5, by + b1 - uy * arm * 0.5, bz + b2 - uz * arm * 0.5,
                    worldCenterX, worldCenterZ, phase, posOffset, saturation, lightness, size, edgeWidth);
            drawJoint(bx + b0, by + b1, bz + b2, worldCenterX, worldCenterZ, phase, posOffset, saturation, lightness, size, edgeWidth);
        }
    }

    private static boolean edgeBoundary(List<AABB> boxes, double a0, double a1, double a2, double b0, double b1, double b2) {
        double mx = (a0 + b0) * 0.5;
        double my = (a1 + b1) * 0.5;
        double mz = (a2 + b2) * 0.5;
        int axis;
        if (Math.abs(b0 - a0) > 1.0E-9) {
            axis = 0;
        } else if (Math.abs(b1 - a1) > 1.0E-9) {
            axis = 1;
        } else if (Math.abs(b2 - a2) > 1.0E-9) {
            axis = 2;
        } else {
            return false;
        }
        double eps = 1.0E-4;
        double[] signs = {-1.0, 1.0};
        for (double s1 : signs) {
            for (double s2 : signs) {
                boolean inside;
                if (axis == 0) {
                    inside = insideAnyStrict(boxes, mx, my + s1 * eps, mz + s2 * eps);
                } else if (axis == 1) {
                    inside = insideAnyStrict(boxes, mx + s1 * eps, my, mz + s2 * eps);
                } else {
                    inside = insideAnyStrict(boxes, mx + s1 * eps, my + s2 * eps, mz);
                }
                if (!inside) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isOuterCorner(List<AABB> boxes, double px, double py, double pz) {
        double eps = 1.0E-4;
        double[] signs = {-1.0, 1.0};
        int inside = 0;
        for (double sx : signs) {
            for (double sy : signs) {
                for (double sz : signs) {
                    if (insideAnyStrict(boxes, px + sx * eps, py + sy * eps, pz + sz * eps)) {
                        inside++;
                    }
                }
            }
        }
        return inside == 1;
    }

    private static boolean insideAnyStrict(List<AABB> boxes, double x, double y, double z) {
        for (AABB t : boxes) {
            if (x > t.minX && x < t.maxX && y > t.minY && y < t.maxY && z > t.minZ && z < t.maxZ) {
                return true;
            }
        }
        return false;
    }

    private static final int JOINT_SEGMENTS = 16;

    private static float jointRadius(double x, double y, double z, float edgeWidth) {
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera();
        double dist = Math.max(0.05, camera.position().distanceTo(new Vec3(x, y, z)));
        float vfov = (float) Math.toRadians(Math.max(1.0f, camera.getFov()));
        int height = Math.max(1, mc.getWindow().getHeight());
        return (float) (edgeWidth * 0.5 * 2.0 * dist * Math.tan(vfov * 0.5) / height);
    }

    private static void drawJoint(double x, double y, double z, double centerX, double centerZ,
                                  float phase, float posOffset, float saturation, float lightness, float size, float edgeWidth) {
        Vec3 pos = new Vec3(x, y, z);
        int packed = chromaColor(x, z, centerX, centerZ, phase, posOffset, saturation, lightness, size);
        int r = ARGB.red(packed);
        int g = ARGB.green(packed);
        int b = ARGB.blue(packed);
        float radius = jointRadius(x, y, z, edgeWidth);
        drawRoundDisc(pos, ARGB.color(90, r, g, b), radius);
        drawRoundDisc(pos, ARGB.color(255, r, g, b), radius * 0.72f);
    }

    private static void drawRoundDisc(Vec3 pos, int color, float radius) {
        if (radius <= 1.0E-5f) {
            return;
        }
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var left = camera.leftVector();
        var up = camera.upVector();
        double rx = -left.x();
        double ry = -left.y();
        double rz = -left.z();
        double ux = up.x();
        double uy = up.y();
        double uz = up.z();
        Vec3[] fan = new Vec3[JOINT_SEGMENTS + 2];
        fan[0] = pos;
        for (int i = 0; i <= JOINT_SEGMENTS; i++) {
            double a = (Math.PI * 2.0) * i / JOINT_SEGMENTS;
            double c = Math.cos(a) * radius;
            double s = Math.sin(a) * radius;
            fan[i + 1] = pos.add(rx * c + ux * s, ry * c + uy * s, rz * c + uz * s);
        }
        Gizmos.addGizmo((primitives, opacity) -> primitives.addTriangleFan(fan, ARGB.multiplyAlpha(color, opacity)));
    }

    private static void drawArm(double x0, double y0, double z0, double x1, double y1, double z1,
                                double midX, double midY, double midZ, double centerX, double centerZ,
                                float phase, float posOffset, float saturation, float lightness, float size, float edgeWidth) {
        Vec3 from = new Vec3(x0, y0, z0);
        Vec3 to = new Vec3(x1, y1, z1);
        if (from.distanceToSqr(to) <= 1.0E-12) {
            return;
        }
        int packed = chromaColor(midX, midZ, centerX, centerZ, phase, posOffset, saturation, lightness, size);
        int r = ARGB.red(packed);
        int g = ARGB.green(packed);
        int b = ARGB.blue(packed);
        Gizmos.line(from, to, ARGB.color(90, r, g, b), edgeWidth);
        Gizmos.line(from, to, ARGB.color(255, r, g, b), edgeWidth * 0.72f);
    }

    private static int chromaColor(double midX, double midZ, double centerX, double centerZ,
                                   float phase, float posOffset, float saturation, float lightness, float size) {
        float ang = (float) (Math.atan2(midZ - centerZ, midX - centerX) / (Math.PI * 2.0));
        float spa = positiveMod(ang, 1.0f);
        float hue = positiveMod((spa / size) - phase + posOffset, 1.0f);
        return Mth.hsvToRgb(hue, saturation, lightness);
    }

    private static float positiveMod(float value, float mod) {
        float result = value % mod;
        return result < 0 ? result + mod : result;
    }
}