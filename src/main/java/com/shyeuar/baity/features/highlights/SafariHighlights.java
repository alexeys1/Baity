package com.shyeuar.baity.features.highlights;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.EntityDrawUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class SafariHighlights implements LevelRenderEvents.AfterSolidFeatures {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String HIDEYHO_NAME = "Hideyho";
    private static final String EXCLUDED_HUNTER_TOKEN = "\"hunter\"";
    private static final String SPARKLING_LABEL = "SPARKLING";
    private static final float SPARKLING_R = 1.0f;
    private static final float SPARKLING_G = 215.0f / 255.0f;
    private static final float SPARKLING_B = 0.0f;
    private static final float SPARKLING_FILL_ALPHA = 0.25f;

    private static final RenderPipeline BAITY_SAFARI_LINES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation("pipeline/baity_safari_lines")
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .build()
    );

    private static final RenderType THROUGH_WALLS_LINE = RenderType.create(
            "baity_safari_lines",
            RenderSetup.builder(BAITY_SAFARI_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    private static final RenderPipeline BAITY_SAFARI_FILL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("baity", "pipeline/baity_safari_fill"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build()
    );

    private static final RenderType THROUGH_WALLS_FILL = RenderType.create(
            "baity_safari_fill",
            RenderSetup.builder(BAITY_SAFARI_FILL)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
    );

    @Override
    public void afterSolidFeatures(LevelRenderContext context) {
        if (!ConfigManager.safariRenderTargetESP) return;
        if (MC.level == null) return;
        if (!LocateUtils.isInSafari(MC)) return;

        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) return;

        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        PoseStack matrices = context.poseStack();
        MultiBufferSource buffers = context.bufferSource();
        if (matrices == null || buffers == null) return;

        List<ArmorStand> namedArmorStands = new ArrayList<>();
        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand && armorStand.isAlive() && armorStand.hasCustomName()) {
                namedArmorStands.add(armorStand);
            }
        }

        VertexConsumer lines = buffers.getBuffer(THROUGH_WALLS_LINE);
        boolean needsFill = ConfigManager.safariFloorDropEnabled || ConfigManager.safariMobEnabled;
        VertexConsumer fill = needsFill ? buffers.getBuffer(THROUGH_WALLS_FILL) : null;
        float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 rayStart = MC.player == null
                ? null
                : MC.player.getEyePosition(partialTick).add(MC.player.getViewVector(partialTick).scale(0.12));
        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof Display.ItemDisplay itemDisplay) {
                if (ConfigManager.safariFloorDropEnabled && isFloorDrop(itemDisplay)) {
                    AABB box = new AABB(itemDisplay.blockPosition());
                    EntityDrawUtils.drawFilledBoxAtWorld(matrices, fill, box, cameraPos, 0.0f, 1.0f, 0.0f, 0.25f);
                    EntityDrawUtils.drawWireBoxAtWorld(matrices, lines, box, cameraPos, 0.0f, 1.0f, 0.0f, 0.9f);
                }
                continue;
            }

            if (entity instanceof Player player && player != MC.player) {
                if (player.isAlive()) {
                    String name = LocateUtils.toPlainText(
                            player.getDisplayName() != null
                                    ? player.getDisplayName().getString()
                                    : player.getName().getString()
                    );
                    if ((ConfigManager.safariHideyhoEnabled && HIDEYHO_NAME.equals(name))
                            || (ConfigManager.safariNpcEnabled
                            && (isSafariNpc(name) || hasAssociatedSafariNpcLabel(player, namedArmorStands)))) {
                        drawBox(matrices, lines, player, cameraPos, 1.0f, 1.0f, 1.0f);
                    }
                }
                continue;
            }

            if (!ConfigManager.safariMobEnabled
                    || !(entity instanceof Mob mob)
                    || !mob.isAlive()
                    || mob instanceof HappyGhast) {
                continue;
            }

            ArmorStand associatedArmorStand = findAssociatedArmorStand(mob, namedArmorStands);
            if (hasLabel(associatedArmorStand, SPARKLING_LABEL)) {
                AABB box = mob.getBoundingBox().inflate(0.01);
                if (fill != null) {
                    EntityDrawUtils.drawFilledBoxAtWorld(
                            matrices,
                            fill,
                            box,
                            cameraPos,
                            SPARKLING_R,
                            SPARKLING_G,
                            SPARKLING_B,
                            SPARKLING_FILL_ALPHA
                    );
                }
                EntityDrawUtils.drawWireBoxAtWorld(
                        matrices,
                        lines,
                        box,
                        cameraPos,
                        SPARKLING_R,
                        SPARKLING_G,
                        SPARKLING_B,
                        0.9f
                );
                if (rayStart != null) {
                    EntityDrawUtils.drawLineAtWorld(
                            matrices,
                            lines,
                            rayStart,
                            mob.getBoundingBox().getCenter(),
                            cameraPos,
                            SPARKLING_R,
                            SPARKLING_G,
                            SPARKLING_B,
                            0.9f
                    );
                }
            } else {
                drawBox(matrices, lines, mob, cameraPos, 1.0f, 0.0f, 0.0f);
            }
        }

        if (fill != null && buffers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(THROUGH_WALLS_FILL);
        }
        if (buffers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch(THROUGH_WALLS_LINE);
        }
    }

    private static boolean isFloorDrop(Display.ItemDisplay itemDisplay) {
        if (!itemDisplay.getItemStack().is(Items.STRING)) return false;
        BlockPos position = itemDisplay.blockPosition();
        BlockPos above = position.above();
        return !MC.level.getBlockState(above).isCollisionShapeFullBlock(MC.level, above);
    }

    private static boolean isSafariNpc(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return !lowerName.contains(EXCLUDED_HUNTER_TOKEN)
                && (lowerName.contains("hunter") || lowerName.contains("huntress"));
    }

    private static boolean hasAssociatedSafariNpcLabel(Player player, List<ArmorStand> armorStands) {
        for (ArmorStand armorStand : armorStands) {
            double dx = player.getX() - armorStand.getX();
            double dz = player.getZ() - armorStand.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 1.0 || armorStand.getY() + 2.0 < player.getY()) continue;
            if (isSafariNpc(LocateUtils.toPlainText(armorStand.getName().getString()))) return true;
        }
        return false;
    }

    private static ArmorStand findAssociatedArmorStand(Mob mob, List<ArmorStand> armorStands) {
        ArmorStand closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (ArmorStand armorStand : armorStands) {
            double dx = mob.getX() - armorStand.getX();
            double dz = mob.getZ() - armorStand.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 1.0 || armorStand.getY() + 2.0 < mob.getY()) continue;
            if (distance < closestDistance) {
                closest = armorStand;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static boolean hasLabel(ArmorStand armorStand, String label) {
        return armorStand != null
                && LocateUtils.toPlainText(armorStand.getName().getString()).contains(label);
    }

    private static void drawBox(
            PoseStack matrices,
            VertexConsumer lines,
            Entity entity,
            Vec3 cameraPos,
            float r,
            float g,
            float b
    ) {
        EntityDrawUtils.drawWireBoxAtWorld(
                matrices,
                lines,
                entity.getBoundingBox().inflate(0.01),
                cameraPos,
                r,
                g,
                b,
                0.9f
        );
    }
}
