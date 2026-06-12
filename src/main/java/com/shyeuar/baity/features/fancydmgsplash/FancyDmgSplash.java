package com.shyeuar.baity.features.fancydmgsplash;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class FancyDmgSplash implements LevelRenderEvents.AfterTranslucentFeatures {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final List<DamageNumber> damageNumbers = new ArrayList<>();
    private static final List<ReactionText> reactionTexts = new ArrayList<>();
    private static final Random random = new Random();
    
    private static final long ANIMATION_DURATION_MS = 1200; 
    private static final float PHASE1_RATIO = 0.4f;
    private static final float BASE_SCALE = 0.0325f; 
    private static final float PHASE2_FLOAT_UP = 0.18f; 
    private static final float PHASE2_DISPLAY_RATIO = 0.25f; 
    
    private static long lastWetHealCheckTime = 0;
    private static final long WET_HEAL_CHECK_INTERVAL_MS = 500;

    public static void addDamageNumber(double damage, Vec3 targetPos, Component originalText) {
        Module moduleRef = ModuleManager.getModuleByName("FancyDmgSplash");
        if (moduleRef == null || !moduleRef.isEnabled()) return;
        if (mc.player == null || mc.gameRenderer == null) return;
        if (originalText == null) return;

        float targetRandomX = (random.nextFloat() - 0.5f) * 1.8f;
        float targetRandomY = (random.nextFloat() - 0.2f) * 1.4f;
        float targetRandomZ = (random.nextFloat() - 0.5f) * 1.8f;
        Vec3 finalTargetPos = targetPos.add(targetRandomX, targetRandomY, targetRandomZ);

        FancyDmgSplashPresetStore.PresetData style = FancyDmgSplashPresetStore.pickRandomForDamage();
        FancyDmgSplashSettings.DamageKind kind = FancyDmgSplashSettings.classifyDamage(originalText);
        boolean syncEnabled = FancyDmgSplashSettings.isSyncNonCriticalEnabled();

        Component formattedText;
        int color;
        boolean preserveComponentColors;

        if (kind == FancyDmgSplashSettings.DamageKind.CRITICAL) {
            formattedText = FancyDmgSplashSettings.formatCriticalDamage(originalText, damage, style);
            color = style.primaryColor();
            preserveComponentColors = true;
        } else if (kind == FancyDmgSplashSettings.DamageKind.PLAIN_NORMAL && syncEnabled) {
            formattedText = FancyDmgSplashSettings.formatSyncNonCrit(originalText, damage, style);
            color = style.primaryColor();
            preserveComponentColors = true;
        } else {
            formattedText = FancyDmgSplashSettings.formatLegacyNonCrit(originalText, damage);
            if (formattedText == null) {
                formattedText = originalText;
            }
            color = FancyDmgSplashSettings.extractColorFromText(formattedText);
            preserveComponentColors = false;
        }

        damageNumbers.add(new DamageNumber(damage, finalTargetPos, formattedText, color,
                System.currentTimeMillis(), preserveComponentColors));

        boolean genshinReaction = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(
                moduleRef,
                "genshin elemental reaction",
                com.shyeuar.baity.config.ConfigManager.fancyDmgSplashGenshinReaction
        );
        if (genshinReaction) {
            Integer reactionColor = resolveReactionColor(kind, style, syncEnabled);
            if (reactionColor != null) {
                ElementalReactionDetector.ReactionResult reaction = kind == FancyDmgSplashSettings.DamageKind.BURN
                        ? ElementalReactionDetector.recordForcedElementDamage(reactionColor, targetPos)
                        : ElementalReactionDetector.recordDamageAndCheckReaction(reactionColor, targetPos);
                if (reaction != null) {
                    Vec3 reactionPos = finalTargetPos.add(0.3, 0.15, 0);
                    reactionTexts.add(new ReactionText(reaction.name, reactionPos, reaction.color, System.currentTimeMillis()));
                }
            }
        }
    }

    private static Integer resolveReactionColor(FancyDmgSplashSettings.DamageKind kind,
                                                FancyDmgSplashPresetStore.PresetData style,
                                                boolean syncEnabled) {
        if (kind == FancyDmgSplashSettings.DamageKind.BURN) {
            return ElementalReactionDetector.PYRO;
        }
        if (kind == FancyDmgSplashSettings.DamageKind.CRITICAL) {
            if (!FancyDmgSplashPresetStore.isReactionEligible(style)) {
                return null;
            }
            return FancyDmgSplashPresetStore.resolveReactionElementColor(style);
        }
        if (kind == FancyDmgSplashSettings.DamageKind.PLAIN_NORMAL && syncEnabled) {
            if (!FancyDmgSplashPresetStore.isReactionEligible(style)) {
                return null;
            }
            return FancyDmgSplashPresetStore.resolveReactionElementColor(style);
        }
        return null;
    }

    public static void addImmuneReaction(Vec3 targetPos) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (!com.shyeuar.baity.config.ConfigManager.fancyDmgSplashGenshinReaction) return;
        
        ElementalReactionDetector.ReactionResult result = 
            ElementalReactionDetector.checkImmuneReaction(targetPos, false);
        if (result != null) {
            reactionTexts.add(new ReactionText(result.name, targetPos, result.color, System.currentTimeMillis()));
        }
    }
    
    public static void addPlayerImmuneReaction() {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.player == null) return;
        if (!com.shyeuar.baity.config.ConfigManager.fancyDmgSplashGenshinReaction) return;
        
        Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getBbHeight() * 0.5, mc.player.getZ());
        ElementalReactionDetector.ReactionResult result = 
            ElementalReactionDetector.checkPlayerImmuneItem(playerPos);
        if (result != null) {
            reactionTexts.add(new ReactionText(result.name, playerPos, result.color, System.currentTimeMillis()));
        }
    }
    
    public static void addWitherCloakImmuneReaction(ElementalReactionDetector.ReactionResult result) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (result == null || result.position == null) return;
        
        reactionTexts.add(new ReactionText(result.name, result.position, result.color, System.currentTimeMillis()));
    }
    
    @Override
    public void afterTranslucentFeatures(LevelRenderContext context) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.level == null || mc.player == null) return;

        PoseStack matrices = context.poseStack();
        MultiBufferSource buffers = context.bufferSource();
        if (matrices == null || buffers == null) return;
        
        long currentTime = System.currentTimeMillis();
        Vec3 cameraPos = context.levelState().cameraRenderState.pos;
        Camera camera = mc.gameRenderer.getMainCamera();
        float cameraYaw = camera.yRot();
        float cameraPitch = camera.xRot();
        
        if (currentTime - lastWetHealCheckTime >= WET_HEAL_CHECK_INTERVAL_MS) {
            lastWetHealCheckTime = currentTime;
            checkWetAndHealReactions();
        }
        
        Iterator<DamageNumber> iterator = damageNumbers.iterator();
        while (iterator.hasNext()) {
            DamageNumber dn = iterator.next();
            if (currentTime - dn.startTime > ANIMATION_DURATION_MS) {
                iterator.remove();
            }
        }
        
        Iterator<ReactionText> reactionIterator = reactionTexts.iterator();
        while (reactionIterator.hasNext()) {
            ReactionText rt = reactionIterator.next();
            if (currentTime - rt.startTime > ANIMATION_DURATION_MS) {
                reactionIterator.remove();
            }
        }
        
        for (DamageNumber dn : damageNumbers) {
            renderDamageNumber(matrices, buffers, dn, cameraPos, cameraYaw, cameraPitch, currentTime);
        }
        
        for (ReactionText rt : reactionTexts) {
            renderReactionText(matrices, buffers, rt, cameraPos, cameraYaw, cameraPitch, currentTime);
        }

        if (buffers instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
    }
    
    private static void checkWetAndHealReactions() {
        if (mc.level == null || mc.player == null) return;
        if (!com.shyeuar.baity.config.ConfigManager.fancyDmgSplashGenshinReaction) return;
        
        Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double range = 32.0;
        
        AABB searchBox = new AABB(
            playerPos.x - range, playerPos.y - range, playerPos.z - range,
            playerPos.x + range, playerPos.y + range, playerPos.z + range
        );
        
        List<Entity> entities = mc.level.getEntities(null, searchBox);
        
        checkEntityReactions(mc.player, playerPos);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                checkEntityReactions(livingEntity, playerPos);
            }
        }
    }
    
    private static void checkEntityReactions(LivingEntity entity, Vec3 playerPos) {
        ElementalReactionDetector.ReactionResult wetResult = 
            ElementalReactionDetector.checkEntityWetState(entity, playerPos);
        if (wetResult != null && wetResult.position != null) {
            reactionTexts.add(new ReactionText(wetResult.name, wetResult.position, 
                wetResult.color, System.currentTimeMillis()));
        }
        
        ElementalReactionDetector.ReactionResult burningResult = 
            ElementalReactionDetector.checkEntityBurningState(entity, playerPos);
        if (burningResult != null && burningResult.position != null) {
            reactionTexts.add(new ReactionText(burningResult.name, burningResult.position, 
                burningResult.color, System.currentTimeMillis()));
        }
        
        ElementalReactionDetector.ReactionResult healResult = 
            ElementalReactionDetector.checkHealReaction(entity);
        if (healResult != null && healResult.position != null) {
            reactionTexts.add(new ReactionText(healResult.name, healResult.position, 
                healResult.color, System.currentTimeMillis()));
        }
    }
    
    private void renderDamageNumber(
            PoseStack matrices,
            MultiBufferSource buffers,
            DamageNumber dn,
            Vec3 cameraPos,
            float cameraYaw,
            float cameraPitch,
            long currentTime
    ) {
        float totalProgress = (float)(currentTime - dn.startTime) / ANIMATION_DURATION_MS;
        totalProgress = Math.min(1.0f, Math.max(0.0f, totalProgress));
        
        Vec3 currentPos;
        float alpha = 1.0f;
        float scaleAnimation = 1.0f;
        
        if (totalProgress < PHASE1_RATIO) {
            float phase1Progress = totalProgress / PHASE1_RATIO;
            
            currentPos = dn.targetPos;
            
            if (phase1Progress < 0.3f) {
                scaleAnimation = 0.5f + phase1Progress / 0.3f * 1.0f;
            } else if (phase1Progress < 0.6f) {
                scaleAnimation = 1.5f - (phase1Progress - 0.3f) / 0.3f * 0.5f;
            }
            
            alpha = 1.0f;
        } else {
            float phase2Progress = (totalProgress - PHASE1_RATIO) / (1.0f - PHASE1_RATIO);
            
            float easedFloatProgress = 1.0f - (1.0f - phase2Progress) * (1.0f - phase2Progress);
            float floatUp = easedFloatProgress * PHASE2_FLOAT_UP;
            currentPos = dn.targetPos.add(0, floatUp, 0);
            
            if (phase2Progress < PHASE2_DISPLAY_RATIO) {
                alpha = 1.0f;
            } else {
                float fadeProgress = (phase2Progress - PHASE2_DISPLAY_RATIO) / (1.0f - PHASE2_DISPLAY_RATIO);
                float acceleratedFade = Math.min(1.0f, fadeProgress * 2.0f);
                alpha = 1.0f - acceleratedFade * acceleratedFade;
            }
            
            scaleAnimation = 1.0f;
        }
        
        double x = currentPos.x - cameraPos.x;
        double y = currentPos.y - cameraPos.y;
        double z = currentPos.z - cameraPos.z;
        
        float damageScale;
        if (dn.damage < 1000) {
            damageScale = 0.65f; 
        } else if (dn.damage < 10000) {
            damageScale = 0.7f;
        } else if (dn.damage < 100000) {
            damageScale = 0.72f;
        } else if (dn.damage < 500000) {
            damageScale = 0.78f;
        } else if (dn.damage < 900000) {
            damageScale = 0.88f;
        } else if (dn.damage < 1200000) {
            float progress = (float)(dn.damage - 900000) / 300000f;
            damageScale = 0.88f + progress * 0.27f;
        } else {
            damageScale = 1.15f;
        }
        
        float finalScale = BASE_SCALE * scaleAnimation * damageScale;
        
        matrices.pushPose();
        try {
            matrices.translate(x, y, z);
            
            matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-cameraYaw)));
            matrices.mulPose(new Quaternionf().rotationX((float) Math.toRadians(cameraPitch)));
            
            matrices.scale(-finalScale, -finalScale, finalScale);
            
            Component styledText;
            int color;
            
            if (dn.originalText != null) {
                styledText = dn.originalText;
            } else {
                String damageText = String.valueOf((long) dn.damage);
                styledText = Component.literal(damageText);
            }
            color = dn.color;
            
            int alphaInt = (int)(alpha * 255) << 24;
            int finalColor = dn.preserveComponentColors
                    ? alphaInt | 0xFFFFFF
                    : (color & 0x00FFFFFF) | alphaInt;
            
            Font textRenderer = mc.font;
            int textWidth = textRenderer.width(styledText);

            textRenderer.drawInBatch(styledText, -textWidth / 2.0f, 0, finalColor, false,
                matrices.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        } finally {
            matrices.popPose();
        }
    }
    
    
    private void renderReactionText(
            PoseStack matrices,
            MultiBufferSource buffers,
            ReactionText rt,
            Vec3 cameraPos,
            float cameraYaw,
            float cameraPitch,
            long currentTime
    ) {
        float totalProgress = (float)(currentTime - rt.startTime) / ANIMATION_DURATION_MS;
        totalProgress = Math.min(1.0f, Math.max(0.0f, totalProgress));
        
        Vec3 currentPos;
        float alpha = 1.0f;
        float scaleAnimation = 1.0f;
        
        if (totalProgress < PHASE1_RATIO) {
            float phase1Progress = totalProgress / PHASE1_RATIO;
            currentPos = rt.pos;
            
            if (phase1Progress < 0.3f) {
                scaleAnimation = 0.5f + phase1Progress / 0.3f * 0.8f;
            } else if (phase1Progress < 0.6f) {
                scaleAnimation = 1.3f - (phase1Progress - 0.3f) / 0.3f * 0.3f;
            }
            
            alpha = 1.0f;
        } else {
            float phase2Progress = (totalProgress - PHASE1_RATIO) / (1.0f - PHASE1_RATIO);
            
            float easedFloatProgress = 1.0f - (1.0f - phase2Progress) * (1.0f - phase2Progress);
            float floatUp = easedFloatProgress * PHASE2_FLOAT_UP * 0.8f;
            currentPos = rt.pos.add(0, floatUp, 0);
            
            if (phase2Progress < PHASE2_DISPLAY_RATIO) {
                alpha = 1.0f;
            } else {
                float fadeProgress = (phase2Progress - PHASE2_DISPLAY_RATIO) / (1.0f - PHASE2_DISPLAY_RATIO);
                float acceleratedFade = Math.min(1.0f, fadeProgress * 2.0f);
                alpha = 1.0f - acceleratedFade * acceleratedFade;
            }
            
            scaleAnimation = 1.0f;
        }
        
        double x = currentPos.x - cameraPos.x;
        double y = currentPos.y - cameraPos.y;
        double z = currentPos.z - cameraPos.z;
        
        float finalScale = BASE_SCALE * scaleAnimation * 1.15f * 0.5f;
        
        matrices.pushPose();
        try {
            matrices.translate(x, y, z);
            
            matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-cameraYaw)));
            matrices.mulPose(new Quaternionf().rotationX((float) Math.toRadians(cameraPitch)));
            
            matrices.scale(-finalScale, -finalScale, finalScale);
            
            Component styledText = Component.literal(rt.text);
            
            int alphaInt = (int)(alpha * 255) << 24;
            int finalColor = (rt.color & 0x00FFFFFF) | alphaInt;
            
            Font textRenderer = mc.font;
            int textWidth = textRenderer.width(styledText);

            textRenderer.drawInBatch(styledText, -textWidth / 2.0f, 0, finalColor, false,
                matrices.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
        } finally {
            matrices.popPose();
        }
    }
    
    private static class DamageNumber {
        final double damage;
        final Vec3 targetPos;
        final Component originalText;
        final int color;
        final long startTime;
        final boolean preserveComponentColors;

        DamageNumber(double damage, Vec3 targetPos, Component originalText, int color, long startTime,
                       boolean preserveComponentColors) {
            this.damage = damage;
            this.targetPos = targetPos;
            this.originalText = originalText;
            this.color = color;
            this.startTime = startTime;
            this.preserveComponentColors = preserveComponentColors;
        }
    }
    
    private static class ReactionText {
        final String text;
        final Vec3 pos;
        final int color;
        final long startTime;
        
        ReactionText(String text, Vec3 pos, int color, long startTime) {
            this.text = text;
            this.pos = pos;
            this.color = color;
            this.startTime = startTime;
        }
    }
}


