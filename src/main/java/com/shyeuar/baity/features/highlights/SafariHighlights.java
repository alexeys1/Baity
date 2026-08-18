package com.shyeuar.baity.features.highlights;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.config.DevConfig;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.EntityDrawUtils;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
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

    private static final float MOB_R = ((DevConfig.DEV_PREFIX_COLOR >> 16) & 0xFF) / 255.0f;
    private static final float MOB_G = ((DevConfig.DEV_PREFIX_COLOR >> 8) & 0xFF) / 255.0f;
    private static final float MOB_B = (DevConfig.DEV_PREFIX_COLOR & 0xFF) / 255.0f;

    private static final float HIDEYHO_R = 1.0f;
    private static final float HIDEYHO_G = 105.0f / 255.0f;
    private static final float HIDEYHO_B = 180.0f / 255.0f;

    private static final float NPC_R = 1.0f;
    private static final float NPC_G = 1.0f;
    private static final float NPC_B = 1.0f;

    private static final float FLOOR_FILL_R = 0.7f;
    private static final float FLOOR_FILL_G = 1.0f;
    private static final float FLOOR_FILL_B = 0.0f;
    private static final float FLOOR_FILL_ALPHA = 0.25f;

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
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
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

    private static final RenderPipeline BAITY_SAFARI_MODEL_OUTLINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.OUTLINE_SNIPPET)
                    .withLocation("pipeline/baity_safari_model_outline")
                    .withFragmentShader(Identifier.fromNamespaceAndPath("baity", "core/baity_safari_model_outline"))
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
                    .build()
    );

    private static final RenderType THROUGH_WALLS_MODEL_OUTLINE = RenderType.create(
            "baity_safari_model_outline",
            RenderSetup.builder(BAITY_SAFARI_MODEL_OUTLINE)
                    .setOutputTarget(OutputTarget.OUTLINE_TARGET)
                    .setOutline(RenderSetup.OutlineProperty.IS_OUTLINE)
                    .createRenderSetup()
    );

    @Override
    public void afterSolidFeatures(LevelRenderContext context) {
        if (!ConfigManager.safariRenderTargetESP) return;
        if (MC.level == null) return;
        if (!LocateUtils.isInSafari(MC)) return;

        Module module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) return;

        CameraRenderState cameraRenderState = context.levelState().cameraRenderState;
        Vec3 cameraPos = cameraRenderState.pos;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) return;

        SafariZoneUtils.Zone playerZone = SafariZoneUtils.playerZone(MC);
        float partialTick = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        List<ArmorStand> namedArmorStands = namedArmorStands();

        Vec3 rayStart = MC.player == null
                ? null
                : MC.player.getEyePosition(partialTick).add(MC.player.getViewVector(partialTick).scale(0.12));

        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof Display.ItemDisplay itemDisplay) {
                if (ConfigManager.safariFloorDropEnabled && isFloorDrop(itemDisplay)) {
                    BlockPos position = itemDisplay.blockPosition();
                    if (SafariZoneUtils.matchesPlayerZone(playerZone, position.getX(), position.getZ())) {
                        AABB box = new AABB(
                                position.getX(),
                                position.getY() + 0.5,
                                position.getZ(),
                                position.getX() + 1.0,
                                position.getY() + 1.0,
                                position.getZ() + 1.0
                        );
                        drawFilledBox(
                                matrices,
                                submits,
                                box,
                                cameraPos,
                                FLOOR_FILL_R,
                                FLOOR_FILL_G,
                                FLOOR_FILL_B,
                                FLOOR_FILL_ALPHA
                        );
                    }
                }
                continue;
            }

            if (entity instanceof Player player && player != MC.player) {
                if (!player.isAlive()) continue;
                if (!SafariZoneUtils.matchesPlayerZone(playerZone, player.getX(), player.getZ())) continue;

                String name = LocateUtils.toPlainText(
                        player.getDisplayName() != null
                                ? player.getDisplayName().getString()
                                : player.getName().getString()
                );
                boolean isHideyho = ConfigManager.safariHideyhoEnabled && HIDEYHO_NAME.equals(name);
                boolean isOtherNpc = ConfigManager.safariNpcEnabled
                        && (isSafariNpc(name) || hasAssociatedSafariNpcLabel(player, namedArmorStands));
                if (isHideyho || isOtherNpc) {
                    int outlineColor = isHideyho
                            ? ARGB.colorFromFloat(1.0f, HIDEYHO_R, HIDEYHO_G, HIDEYHO_B)
                            : ARGB.colorFromFloat(1.0f, NPC_R, NPC_G, NPC_B);
                    drawEntityModel(
                            player,
                            partialTick,
                            cameraPos,
                            matrices,
                            submits,
                            cameraRenderState,
                            outlineColor
                    );
                }
                continue;
            }

            if (!ConfigManager.safariMobEnabled
                    || !(entity instanceof Mob mob)
                    || !mob.isAlive()
                    || mob instanceof HappyGhast) {
                continue;
            }
            if (!SafariZoneUtils.matchesPlayerZone(playerZone, mob.getX(), mob.getZ())) continue;

            ArmorStand associatedArmorStand = findAssociatedArmorStand(mob, namedArmorStands);
            drawEntityModel(
                    mob,
                    partialTick,
                    cameraPos,
                    matrices,
                    submits,
                    cameraRenderState,
                    mobOutlineColor(associatedArmorStand)
            );
            if (rayStart != null && hasLabel(associatedArmorStand, SPARKLING_LABEL)) {
                drawLine(
                        matrices,
                        submits,
                        rayStart,
                        EntityDrawUtils.interpolatedEntityBox(mob, partialTick, 0.01).getCenter(),
                        cameraPos,
                        SPARKLING_R,
                        SPARKLING_G,
                        SPARKLING_B,
                        0.9f
                );
            }
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

    private static int mobOutlineColor(ArmorStand associatedArmorStand) {
        if (hasLabel(associatedArmorStand, SPARKLING_LABEL)) {
            return ARGB.colorFromFloat(1.0f, SPARKLING_R, SPARKLING_G, SPARKLING_B);
        }
        return ARGB.colorFromFloat(1.0f, MOB_R, MOB_G, MOB_B);
    }

    public static boolean isSafariMobOutlineActive() {
        if (!ConfigManager.safariRenderTargetESP
                || MC.level == null
                || !LocateUtils.isInSafari(MC)) {
            return false;
        }
        if (!ConfigManager.safariMobEnabled
                && !ConfigManager.safariHideyhoEnabled
                && !ConfigManager.safariNpcEnabled) {
            return false;
        }
        Module module = ModuleManager.getModuleByName("Highlights");
        return module != null && module.isEnabled();
    }

    private static void drawEntityModel(
            Entity entity,
            float partialTick,
            Vec3 cameraPos,
            PoseStack matrices,
            SubmitNodeCollector submits,
            CameraRenderState cameraRenderState,
            int color
    ) {
        EntityRenderDispatcher dispatcher = MC.getEntityRenderDispatcher();
        EntityRenderState state = dispatcher.extractEntity(entity, partialTick);
        state.isInvisible = false;
        dispatcher.submit(
                state,
                cameraRenderState,
                state.x - cameraPos.x,
                state.y - cameraPos.y,
                state.z - cameraPos.z,
                matrices,
                new SafariMobModelCollector(submits, color)
        );
    }

    private static List<ArmorStand> namedArmorStands() {
        List<ArmorStand> armorStands = new ArrayList<>();
        for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand armorStand && armorStand.isAlive() && armorStand.hasCustomName()) {
                armorStands.add(armorStand);
            }
        }
        return armorStands;
    }

    private static final class SafariMobModelCollector extends SubmitNodeStorage {
        private final SubmitNodeCollector target;
        private final int color;

        private SafariMobModelCollector(SubmitNodeCollector target, int color) {
            this.target = target;
            this.color = color;
        }

        @Override
        public SubmitNodeCollection order(int order) {
            return new SafariMobOrderedCollector(this);
        }

        @Override
        public <S> void submitModel(
                Model<? super S> model,
                S state,
                PoseStack poseStack,
                RenderType renderType,
                int lightCoords,
                int overlayCoords,
                int tintedColor,
                TextureAtlasSprite textureAtlasSprite,
                int ignoredOutlineColor,
                ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
        ) {
            target.submitModel(
                    model,
                    state,
                    poseStack,
                    THROUGH_WALLS_MODEL_OUTLINE,
                    lightCoords,
                    overlayCoords,
                    tintedColor,
                    textureAtlasSprite,
                    color,
                    crumblingOverlay
            );
        }

        @Override
        public void submitModelPart(
                ModelPart modelPart,
                PoseStack poseStack,
                RenderType renderType,
                int lightCoords,
                int overlayCoords,
                TextureAtlasSprite textureAtlasSprite,
                int tintedColor,
                ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                int ignoredOutlineColor
        ) {
            target.submitModelPart(
                    modelPart,
                    poseStack,
                    THROUGH_WALLS_MODEL_OUTLINE,
                    lightCoords,
                    overlayCoords,
                    textureAtlasSprite,
                    tintedColor,
                    crumblingOverlay,
                    color
            );
        }

        private static final class SafariMobOrderedCollector extends SubmitNodeCollection {
            private final SafariMobModelCollector parent;

            private SafariMobOrderedCollector(SafariMobModelCollector parent) {
                super();
                this.parent = parent;
            }

            @Override
            public <S> void submitModel(
                    Model<? super S> model,
                    S state,
                    PoseStack poseStack,
                    RenderType renderType,
                    int lightCoords,
                    int overlayCoords,
                    int tintedColor,
                    TextureAtlasSprite textureAtlasSprite,
                    int ignoredOutlineColor,
                    ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
            ) {
                parent.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, textureAtlasSprite, ignoredOutlineColor, crumblingOverlay);
            }

            @Override
            public void submitModelPart(
                    ModelPart modelPart,
                    PoseStack poseStack,
                    RenderType renderType,
                    int lightCoords,
                    int overlayCoords,
                    TextureAtlasSprite textureAtlasSprite,
                    int tintedColor,
                    ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
                    int ignoredOutlineColor
            ) {
                parent.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, textureAtlasSprite, tintedColor, crumblingOverlay, ignoredOutlineColor);
            }
        }
    }

    private static void drawFilledBox(
            PoseStack matrices,
            SubmitNodeCollector submits,
            AABB box,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        submits.submitCustomGeometry(
                matrices,
                THROUGH_WALLS_FILL,
                (pose, fill) -> EntityDrawUtils.drawFilledBoxAtWorld(pose, fill, box, cameraPos, r, g, b, a)
        );
    }

    private static void drawLine(
            PoseStack matrices,
            SubmitNodeCollector submits,
            Vec3 start,
            Vec3 end,
            Vec3 cameraPos,
            float r,
            float g,
            float b,
            float a
    ) {
        submits.submitCustomGeometry(
                matrices,
                THROUGH_WALLS_LINE,
                (pose, lines) -> EntityDrawUtils.drawLineAtWorld(pose, lines, start, end, cameraPos, r, g, b, a)
        );
    }
}
