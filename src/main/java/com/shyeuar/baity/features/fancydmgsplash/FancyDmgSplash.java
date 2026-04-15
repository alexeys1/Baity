package com.shyeuar.baity.features.fancydmgsplash;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ColorGradientUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class FancyDmgSplash implements WorldRenderEvents.AfterEntities {
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
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.player == null || mc.gameRenderer == null) return;
        
        float targetRandomX = (random.nextFloat() - 0.5f) * 1.8f;
        float targetRandomY = (random.nextFloat() - 0.2f) * 1.4f;
        float targetRandomZ = (random.nextFloat() - 0.5f) * 1.8f;
        Vec3 finalTargetPos = targetPos.add(targetRandomX, targetRandomY, targetRandomZ);
        
        int colorMask = com.shyeuar.baity.config.ConfigManager.fancyDmgSplashColorPalette;
        int color;
        Component textToUse;
        
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
                Vec3 reactionPos = finalTargetPos.add(0.3, 0.15, 0);
                reactionTexts.add(new ReactionText(reaction.name, reactionPos, reaction.color, System.currentTimeMillis()));
            }
        }
    }

    private static int extractColorFromText(Component text) {
        if (text == null) return 0xFFFFFF;
        
        Style style = text.getStyle();
        if (style != null) {
            TextColor textColor = style.getColor();
            if (textColor != null) {
                return textColor.getValue();
            }
        }
        
        for (Component sibling : text.getSiblings()) {
            Style siblingStyle = sibling.getStyle();
            if (siblingStyle != null) {
                TextColor textColor = siblingStyle.getColor();
                if (textColor != null) {
                    return textColor.getValue();
                }
            }
        }
        
        return 0xFFFFFF;
    }
    
    private static final Pattern DAMAGE_TEXT_PATTERN = Pattern.compile("([✧✯]?)[\\d,]+[✧✯]?([❤+⚔☄♞]?)");
    private static final Pattern COMPACT_SUFFIX_PATTERN = Pattern.compile(".*[kKmMbBtTqQ]$");
    
    private static boolean hasCompactSuffix(String text) {
        if (text == null || text.isEmpty()) return false;
        String cleaned = text.replaceAll("[^\\d.,kKmMbBtTqQ]", "");
        return COMPACT_SUFFIX_PATTERN.matcher(cleaned).find();
    }
    
    public static Component applyCompactFormatting(Component originalText, double damage) {
        if (originalText == null) return null;
        
        String textContent = originalText.getString();
        
        if (hasCompactSuffix(textContent)) {
            return originalText;
        }
        
        Matcher matcher = DAMAGE_TEXT_PATTERN.matcher(textContent);
        if (!matcher.matches()) return originalText;
        
        List<Component> siblings = originalText.getSiblings();
        if (siblings.isEmpty()) return originalText;
        
        boolean isCritical = !matcher.group(1).isEmpty();
        String numericPart = textContent.replaceAll("\\D", "");
        if (numericPart.isEmpty()) return originalText;
        
        long damageValue;
        try {
            damageValue = Long.parseLong(numericPart);
        } catch (NumberFormatException e) {
            return originalText;
        }
        
        TextColor originalColor = siblings.getFirst().getStyle().getColor();
        MutableComponent result = Component.empty();
        
        if (isCritical) {
            String critSymbol = matcher.group(1);
            String displayText;
            if (damageValue < 1000) {
                displayText = critSymbol + String.valueOf(damageValue) + critSymbol;
            } else {
                String compactText = CompactDamageNumber.formatDamage(damageValue, 4);
                displayText = critSymbol + compactText + critSymbol;
            }
            
            int textLength = displayText.length();
            int gradientStart = com.shyeuar.baity.config.ConfigManager.fancyDmgSplashCritGradientStart & 0x00FFFFFF;
            int gradientEnd = com.shyeuar.baity.config.ConfigManager.fancyDmgSplashCritGradientEnd & 0x00FFFFFF;
            
            for (int i = 0; i < textLength; i++) {
                float ratio = i / (textLength - 1.0f);
                int color = ColorGradientUtils.blendColors(
                    gradientStart,
                    gradientEnd,
                    ratio
                );
                result.append(Component.literal(displayText.substring(i, i + 1))
                    .withStyle(Style.EMPTY.withColor(color)));
            }
            result.setStyle(originalText.getStyle());
        } else {
            String compactText = CompactDamageNumber.formatDamage(damageValue, 4);
            int displayColor;
            if (originalColor == null || originalColor == TextColor.fromLegacyFormat(ChatFormatting.GRAY)) {
                displayColor = com.shyeuar.baity.config.ConfigManager.fancyDmgSplashNormalDamageColor & 0x00FFFFFF;
            } else {
                displayColor = originalColor.getValue();
            }
            result = Component.literal(compactText)
                .setStyle(originalText.getStyle())
                .withStyle(Style.EMPTY.withColor(displayColor));
        }
        
        if (!matcher.group(2).isEmpty()) {
            result.append(Component.literal(matcher.group(2))
                .setStyle(siblings.getLast().getStyle()));
        }
        
        return result;
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
    
    public static void addTestDamageNumber(Vec3 targetPos) {
        double damage = random.nextDouble() * 1999999 + 1; 
        addDamageNumber(damage, targetPos, null);
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
    public void afterEntities(WorldRenderContext context) {
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) return;
        if (mc.level == null || mc.player == null) return;
        
        long currentTime = System.currentTimeMillis();
        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        Camera camera = mc.gameRenderer.getMainCamera();
        float cameraYaw = camera.yRot();
        float cameraPitch = camera.xRot();
        
        PoseStack matrices = context.matrices();
        if (matrices == null) {
            matrices = new PoseStack();
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
    
    private void renderDamageNumber(PoseStack matrices, DamageNumber dn, Vec3 cameraPos, 
                                     float cameraYaw, float cameraPitch, long currentTime) {
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
                color = dn.color;
            } else {
                String damageText = String.valueOf((long) dn.damage);
                styledText = Component.literal(damageText);
                color = dn.color;
            }
            
            int alphaInt = (int)(alpha * 255) << 24;
            int finalColor = (color & 0x00FFFFFF) | alphaInt;
            
            Font textRenderer = mc.font;
            int textWidth = textRenderer.width(styledText);
            
            MultiBufferSource.BufferSource immediate = mc.renderBuffers().bufferSource();
            
            textRenderer.drawInBatch(styledText, -textWidth / 2.0f, 0, finalColor, false, 
                matrices.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            
            immediate.endBatch();
        } finally {
            matrices.popPose();
        }
    }
    
    
    private void renderReactionText(PoseStack matrices, ReactionText rt, Vec3 cameraPos,
                                    float cameraYaw, float cameraPitch, long currentTime) {
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
            
            MultiBufferSource.BufferSource immediate = mc.renderBuffers().bufferSource();
            
            textRenderer.drawInBatch(styledText, -textWidth / 2.0f, 0, finalColor, false, 
                matrices.last().pose(), immediate, Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            
            immediate.endBatch();
        } finally {
            matrices.popPose();
        }
    }
    
    @SuppressWarnings("unused")
    private static class DamageNumber {
        final double damage;
        final Vec3 spawnPos;   
        final Vec3 targetPos;  
        final Component originalText; 
        final int color;
        final long startTime;
        
        DamageNumber(double damage, Vec3 spawnPos, Vec3 targetPos, Component originalText, int color, long startTime) {
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


