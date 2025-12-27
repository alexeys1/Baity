package com.shyeuar.baity.features;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class FancyDmgSplash implements WorldRenderEvents.AfterEntities {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
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
    
    public static void addDamageNumber(double damage, Vec3d targetPos, Text originalText) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.player == null || mc.gameRenderer == null) return;
        
        float targetRandomX = (random.nextFloat() - 0.5f) * 1.8f;
        float targetRandomY = (random.nextFloat() - 0.2f) * 1.4f;
        float targetRandomZ = (random.nextFloat() - 0.5f) * 1.8f;
        Vec3d finalTargetPos = targetPos.add(targetRandomX, targetRandomY, targetRandomZ);
        
        int colorMask = com.shyeuar.baity.config.ConfigManager.fancyDmgSplashColorPalette;
        int color;
        Text textToUse;
        
        if (colorMask != 0) {
            color = generateDamageColor(damage);
            textToUse = null;
        } else {
            color = extractColorFromText(originalText);
            textToUse = originalText;
        }
        
        damageNumbers.add(new DamageNumber(damage, finalTargetPos, finalTargetPos, textToUse, color, System.currentTimeMillis()));
        
        if (com.shyeuar.baity.config.ConfigManager.fancyDmgSplashGenshinReaction && colorMask != 0) {
            ElementalReactionDetector.ReactionResult reaction = ElementalReactionDetector.recordDamageAndCheckReaction(color, targetPos);
            if (reaction != null) {
                Vec3d reactionPos = finalTargetPos.add(0.3, 0.15, 0);
                reactionTexts.add(new ReactionText(reaction.name, reactionPos, reaction.color, System.currentTimeMillis()));
            }
        }
    }
   
    private static int extractColorFromText(Text text) {
        if (text == null) return 0xFFFFFF;
        
        Style style = text.getStyle();
        if (style != null) {
            TextColor textColor = style.getColor();
            if (textColor != null) {
                return textColor.getRgb();
            }
        }
        
        for (Text sibling : text.getSiblings()) {
            Style siblingStyle = sibling.getStyle();
            if (siblingStyle != null) {
                TextColor textColor = siblingStyle.getColor();
                if (textColor != null) {
                    return textColor.getRgb();
                }
            }
        }
        
        return 0xFFFFFF;
    }
    
    public static int generateDamageColor(double damage) {
        int colorMask = com.shyeuar.baity.config.ConfigManager.fancyDmgSplashColorPalette;
        
        if (colorMask == 0) {
            return 0xFFFFFF;
        }
        
        java.util.List<Integer> selectedColors = new java.util.ArrayList<>();
        int[] presetColors = com.shyeuar.baity.gui.value.ColorPaletteValue.PRESET_COLORS;
        for (int i = 0; i < presetColors.length; i++) {
            if ((colorMask & (1 << i)) != 0) {
                selectedColors.add(presetColors[i]);
            }
        }
        
        if (selectedColors.isEmpty()) {
            return 0xFFFFFF;
        }
        
        return selectedColors.get(random.nextInt(selectedColors.size()));
    }
    
    public static void addTestDamageNumber(Vec3d targetPos) {
        double damage = random.nextDouble() * 1999999 + 1; 
        addDamageNumber(damage, targetPos, null);
    }
    
    public static void addImmuneReaction(Vec3d targetPos) {
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
        
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getHeight() * 0.5, mc.player.getZ());
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
    public void afterEntities(WorldRenderContext context) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.world == null || mc.player == null) return;
        
        long currentTime = System.currentTimeMillis();
        Vec3d cameraPos = context.worldState().cameraRenderState.pos;
        Camera camera = mc.gameRenderer.getCamera();
        float cameraYaw = camera.getYaw();
        float cameraPitch = camera.getPitch();
        
        MatrixStack matrices = context.matrices();
        if (matrices == null) {
            matrices = new MatrixStack();
        }
        
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
            renderDamageNumber(matrices, dn, cameraPos, cameraYaw, cameraPitch, currentTime);
        }
        
        for (ReactionText rt : reactionTexts) {
            renderReactionText(matrices, rt, cameraPos, cameraYaw, cameraPitch, currentTime);
        }
    }
    
    private static void checkWetAndHealReactions() {
        if (mc.world == null || mc.player == null) return;
        if (!com.shyeuar.baity.config.ConfigManager.fancyDmgSplashGenshinReaction) return;
        
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        double range = 32.0;
        
        Box searchBox = new Box(
            playerPos.x - range, playerPos.y - range, playerPos.z - range,
            playerPos.x + range, playerPos.y + range, playerPos.z + range
        );
        
        List<Entity> entities = mc.world.getOtherEntities(null, searchBox);
        
        checkEntityReactions(mc.player, playerPos);
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                checkEntityReactions(livingEntity, playerPos);
            }
        }
    }
    
    private static void checkEntityReactions(LivingEntity entity, Vec3d playerPos) {
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
    
    private void renderDamageNumber(MatrixStack matrices, DamageNumber dn, Vec3d cameraPos, 
                                     float cameraYaw, float cameraPitch, long currentTime) {
        float totalProgress = (float)(currentTime - dn.startTime) / ANIMATION_DURATION_MS;
        totalProgress = Math.min(1.0f, Math.max(0.0f, totalProgress));
        
        Vec3d currentPos;
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
        
        matrices.push();
        try {
            matrices.translate(x, y, z);
            
            matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(-cameraYaw)));
            matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(cameraPitch)));
            
            matrices.scale(-finalScale, -finalScale, finalScale);
            
            String damageText = formatDamage(dn.damage);
            
            Text styledText;
            int color;
            
            if (dn.originalText != null) {
                styledText = dn.originalText;
                color = dn.color; 
            } else {
                styledText = Text.literal(damageText);
                color = dn.color;
            }
            
            int alphaInt = (int)(alpha * 255) << 24;
            int finalColor = (color & 0x00FFFFFF) | alphaInt;
            
            TextRenderer textRenderer = mc.textRenderer;
            int textWidth = textRenderer.getWidth(styledText);
            
            VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            
            textRenderer.draw(styledText, -textWidth / 2.0f, 0, finalColor, false, 
                matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
            
            immediate.draw();
        } finally {
            matrices.pop();
        }
    }
    
    private String formatDamage(double damage) {
        if (damage >= 1000000) {
            return String.format("%.1fM", damage / 1000000);
        } else if (damage >= 1000) {
            return String.format("%.1fK", damage / 1000);
        } else {
            return String.format("%.0f", damage);
        }
    }
    
    private void renderReactionText(MatrixStack matrices, ReactionText rt, Vec3d cameraPos,
                                    float cameraYaw, float cameraPitch, long currentTime) {
        float totalProgress = (float)(currentTime - rt.startTime) / ANIMATION_DURATION_MS;
        totalProgress = Math.min(1.0f, Math.max(0.0f, totalProgress));
        
        Vec3d currentPos;
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
        
        matrices.push();
        try {
            matrices.translate(x, y, z);
            
            matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(-cameraYaw)));
            matrices.multiply(new Quaternionf().rotationX((float) Math.toRadians(cameraPitch)));
            
            matrices.scale(-finalScale, -finalScale, finalScale);
            
            Text styledText = Text.literal(rt.text);
            
            int alphaInt = (int)(alpha * 255) << 24;
            int finalColor = (rt.color & 0x00FFFFFF) | alphaInt;
            
            TextRenderer textRenderer = mc.textRenderer;
            int textWidth = textRenderer.getWidth(styledText);
            
            VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            
            textRenderer.draw(styledText, -textWidth / 2.0f, 0, finalColor, false, 
                matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
            
            immediate.draw();
        } finally {
            matrices.pop();
        }
    }
    
    @SuppressWarnings("unused")
    private static class DamageNumber {
        final double damage;
        final Vec3d spawnPos;   
        final Vec3d targetPos;  
        final Text originalText; 
        final int color;
        final long startTime;
        
        DamageNumber(double damage, Vec3d spawnPos, Vec3d targetPos, Text originalText, int color, long startTime) {
            this.damage = damage;
            this.spawnPos = spawnPos;
            this.targetPos = targetPos;
            this.originalText = originalText;
            this.color = color;
            this.startTime = startTime;
        }
    }
    
    private static class ReactionText {
        final String text;
        final Vec3d pos;
        final int color;
        final long startTime;
        
        ReactionText(String text, Vec3d pos, int color, long startTime) {
            this.text = text;
            this.pos = pos;
            this.color = color;
            this.startTime = startTime;
        }
    }
}
