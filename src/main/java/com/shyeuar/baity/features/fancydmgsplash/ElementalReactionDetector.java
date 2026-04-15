package com.shyeuar.baity.features.fancydmgsplash;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.value.ColorPaletteValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
@Environment(EnvType.CLIENT)
public class ElementalReactionDetector {
    
    public static final int CRYO = 0x99FFFF;    // 冰
    public static final int HYDRO = 0x33CCFF;   // 水
    public static final int ELECTRO = 0xE19BFF; // 雷
    public static final int PYRO = 0xFF9B00;    // 火
    public static final int GEO = 0xFFCC66;     // 岩
    public static final int ANEMO = 0x66FFCC;   // 风
    public static final int DENDRO = 0xBAFF37;  // 草
    public static final int PHYSICAL = 0xFFFFFF; // 物理
    
    private static final List<DamageRecord> recentDamages = new ArrayList<>();
    private static final int MAX_HISTORY = 6;
    private static final long REACTION_WINDOW_MS = 2000; 
    
    private static final Map<String, Integer> reactionCounters = new HashMap<>();
    
    private static boolean frozenState = false;
    private static Vec3 frozenTargetPos = null;
    
    private static boolean quickenState = false;
    private static Vec3 quickenTargetPos = null;
    
    private static boolean bloomState = false;
    private static Vec3 bloomTargetPos = null;
    
    private static long lastWetReactionTime = 0;
    private static final long WET_COOLDOWN_MS = 3000; 
    
    private static long lastImmuneReactionTime = 0;
    private static final long IMMUNE_COOLDOWN_MS = 2000; 
    
    private static final Map<UUID, Float> entityHealthMap = new HashMap<>();
    
    private static final Map<UUID, Long> entityRainCooldown = new HashMap<>();
    private static final long RAIN_COOLDOWN_MS = 3000; 
    
    private static final Map<UUID, Boolean> entityWasInWater = new HashMap<>();
    
    private static final Map<UUID, Boolean> entityInRange = new HashMap<>();
    
    private static final Map<UUID, Boolean> entityWasOnFire = new HashMap<>();
    private static final Map<UUID, Long> entityFireCooldown = new HashMap<>();
    private static final long FIRE_COOLDOWN_MS = 3000;
    
    private static long lastWitherCloakImmuneTime = 0;
    private static final long WITHER_CLOAK_COOLDOWN_MS = 2500;
    private static boolean witherCloakActive = false;
    
    public static ReactionResult recordDamageAndCheckReaction(int color, Vec3 targetPos) {
        if (!ConfigManager.fancyDmgSplashGenshinReaction) {
            return null;
        }
        
        if (!isColorSelected(color)) {
            return null;
        }
        
        long currentTime = System.currentTimeMillis();
        
        cleanExpiredHistory(currentTime);
        
        recentDamages.add(new DamageRecord(color, targetPos, currentTime));
        
        while (recentDamages.size() > MAX_HISTORY) {
            recentDamages.remove(0);
        }
        
        return detectReaction(color, targetPos);
    }
    
    private static void cleanExpiredHistory(long currentTime) {
        while (!recentDamages.isEmpty() && currentTime - recentDamages.get(0).time > REACTION_WINDOW_MS) {
            recentDamages.remove(0);
        }
        if (frozenState && currentTime - getLastDamageTime() > REACTION_WINDOW_MS) {
            frozenState = false;
            frozenTargetPos = null;
        }
        if (quickenState && currentTime - getLastDamageTime() > REACTION_WINDOW_MS) {
            quickenState = false;
            quickenTargetPos = null;
        }
        if (bloomState && currentTime - getLastDamageTime() > REACTION_WINDOW_MS) {
            bloomState = false;
            bloomTargetPos = null;
        }
    }
    
    private static long getLastDamageTime() {
        if (recentDamages.isEmpty()) return 0;
        return recentDamages.get(recentDamages.size() - 1).time;
    }
    
    private static boolean isColorSelected(int color) {
        int colorMask = ConfigManager.fancyDmgSplashColorPalette;
        int[] presetColors = ColorPaletteValue.PRESET_COLORS;
        for (int i = 0; i < presetColors.length; i++) {
            if (presetColors[i] == color && (colorMask & (1 << i)) != 0) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isSameTarget(Vec3 pos1, Vec3 pos2) {
        if (pos1 == null || pos2 == null) return false;
        return pos1.distanceToSqr(pos2) < 4.0; 
    }
    
    private static boolean hasRecentColor(int color, Vec3 targetPos) {
        for (int i = 0; i < recentDamages.size() - 1; i++) {
            DamageRecord record = recentDamages.get(i);
            if (record.color == color && isSameTarget(record.pos, targetPos)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean hasConsecutiveSequence(int color1, int color2, int color3, Vec3 targetPos) {
        if (recentDamages.size() < 3) return false;
        
        int size = recentDamages.size();
        DamageRecord r1 = recentDamages.get(size - 3);
        DamageRecord r2 = recentDamages.get(size - 2);
        DamageRecord r3 = recentDamages.get(size - 1);
        
        return r1.color == color1 && r2.color == color2 && r3.color == color3
            && isSameTarget(r1.pos, targetPos) && isSameTarget(r2.pos, targetPos) && isSameTarget(r3.pos, targetPos);
    }
    
    private static ReactionResult tryTriggerReaction(String name, int color) {
        int count = reactionCounters.getOrDefault(name, 0) + 1;
        reactionCounters.put(name, count);
        
        if (count % 4 == 1) {
            return new ReactionResult(name, color);
        }
        return null;
    }
    
    private static ReactionResult detectReaction(int currentColor, Vec3 targetPos) {
        ReactionResult wetResult = checkWetReaction(currentColor, targetPos);
        if (wetResult != null) return wetResult;
        
        if (frozenState && isSameTarget(frozenTargetPos, targetPos)) {
            if (currentColor == PHYSICAL || currentColor == GEO || currentColor == CRYO) {
                frozenState = false;
                frozenTargetPos = null;
                return tryTriggerReaction("碎冰", ColorPaletteValue.COLOR_REACTION_SHATTERED);
            }
            frozenState = false;
            frozenTargetPos = null;
        }
        
        if (hasConsecutiveSequence(HYDRO, DENDRO, ELECTRO, targetPos)) {
            bloomState = false;
            bloomTargetPos = null;
            return tryTriggerReaction("超绽放", ELECTRO);
        }
        
        if (hasConsecutiveSequence(HYDRO, DENDRO, PYRO, targetPos)) {
            bloomState = false;
            bloomTargetPos = null;
            return tryTriggerReaction("烈绽放", PYRO);
        }
        
        if (quickenState && isSameTarget(quickenTargetPos, targetPos) && currentColor == DENDRO) {
            return tryTriggerReaction("蔓激化", DENDRO);
        }
        
        if (quickenState && isSameTarget(quickenTargetPos, targetPos) && currentColor == ELECTRO) {
            return tryTriggerReaction("超激化", ELECTRO);
        }
        
        if (currentColor == PYRO && hasRecentColor(HYDRO, targetPos)) {
            return tryTriggerReaction("蒸发", ColorPaletteValue.COLOR_REACTION_VAPORIZE);
        }
        
        if (currentColor == PYRO && hasRecentColor(CRYO, targetPos)) {
            return tryTriggerReaction("融化", ColorPaletteValue.COLOR_REACTION_MELT);
        }
        
        if (currentColor == PYRO && hasRecentColor(DENDRO, targetPos)) {
            return tryTriggerReaction("燃烧", ColorPaletteValue.COLOR_REACTION_BURNING);
        }
        
        if (currentColor == ELECTRO && hasRecentColor(CRYO, targetPos)) {
            return tryTriggerReaction("超导", ColorPaletteValue.COLOR_REACTION_SUPERCONDUCT);
        }
        
        if (currentColor == HYDRO && hasRecentColor(CRYO, targetPos)) {
            ReactionResult result = tryTriggerReaction("冻结", ColorPaletteValue.COLOR_REACTION_FROZEN);
            if (result != null) {
                frozenState = true;
                frozenTargetPos = targetPos;
            }
            return result;
        }
        
        if (currentColor == ELECTRO && hasRecentColor(HYDRO, targetPos)) {
            return tryTriggerReaction("感电", ColorPaletteValue.COLOR_REACTION_ELECTRO_CHARGED);
        }
        
        if (currentColor == PYRO && hasRecentColor(ELECTRO, targetPos)) {
            return tryTriggerReaction("超载", ColorPaletteValue.COLOR_REACTION_OVERLOADED);
        }
        
        if (currentColor == DENDRO && hasRecentColor(HYDRO, targetPos)) {
            ReactionResult result = tryTriggerReaction("绽放", DENDRO);
            if (result != null) {
                bloomState = true;
                bloomTargetPos = targetPos;
            }
            return result;
        }
        
        if ((currentColor == DENDRO && hasRecentColor(ELECTRO, targetPos)) ||
            (currentColor == ELECTRO && hasRecentColor(DENDRO, targetPos))) {
            ReactionResult result = tryTriggerReaction("原激化", DENDRO);
            if (result != null) {
                quickenState = true;
                quickenTargetPos = targetPos;
            }
            return result;
        }
        
        if (currentColor == GEO && hasAnyElementRecent(targetPos)) {
            return tryTriggerReaction("结晶", ColorPaletteValue.COLOR_REACTION_CRYSTALLIZE);
        }
        
        if (currentColor == ANEMO && hasAnyElementRecent(targetPos)) {
            return tryTriggerReaction("扩散", ANEMO);
        }
        
        return null;
    }
    
    private static boolean hasAnyElementRecent(Vec3 targetPos) {
        for (int i = 0; i < recentDamages.size() - 1; i++) {
            DamageRecord record = recentDamages.get(i);
            int c = record.color;
            if ((c == CRYO || c == HYDRO || c == ELECTRO || c == PYRO || c == DENDRO) 
                && isSameTarget(record.pos, targetPos)) {
                return true;
            }
        }
        return false;
    }
    
    private static ReactionResult checkWetReaction(int currentColor, Vec3 targetPos) {
        if (currentColor != HYDRO) return null;
        
        if (recentDamages.size() >= 3) {
            int size = recentDamages.size();
            DamageRecord r1 = recentDamages.get(size - 3);
            DamageRecord r2 = recentDamages.get(size - 2);
            DamageRecord r3 = recentDamages.get(size - 1);
            
            if (r1.color == HYDRO && r2.color == HYDRO && r3.color == HYDRO
                && isSameTarget(r1.pos, targetPos) && isSameTarget(r2.pos, targetPos)) {
                return tryTriggerReaction("潮湿", ColorPaletteValue.COLOR_REACTION_WET);
            }
        }
        return null;
    }
  
    private static boolean shouldTriggerReaction(Entity entity) {
        if (entity == null) return false;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity == mc.player) return true;
        
        if (entity instanceof ArmorStand) return false;
        if (entity instanceof Boat) return false;
        if (entity instanceof AbstractMinecart) return false;
        
        return entity instanceof LivingEntity;
    }
    
    public static ReactionResult checkEntityWetState(Entity entity, Vec3 playerPos) {
        if (!ConfigManager.fancyDmgSplashGenshinReaction) return null;
        if (entity == null) return null;
        
        if (!shouldTriggerReaction(entity)) return null;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        
        long currentTime = System.currentTimeMillis();
        UUID entityId = entity.getUUID();
        Vec3 entityPos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
        
        boolean isPlayer = entity == mc.player;
        if (!isPlayer) {
            double distance = entityPos.distanceTo(playerPos);
            if (distance > 10.0) {
                entityInRange.remove(entityId);
                return null;
            }
        }
        
        boolean isInWater = entity.isUnderWater() || entity.isInWater();
        Boolean wasInWater = entityWasInWater.get(entityId);
        Boolean wasInRange = entityInRange.get(entityId);
        
        entityWasInWater.put(entityId, isInWater);
        entityInRange.put(entityId, true);
        
        if (wasInRange == null && isInWater) {
            entityRainCooldown.put(entityId, currentTime);
            return new ReactionResult("潮湿", ColorPaletteValue.COLOR_REACTION_WET, entityPos);
        }
        
        if (isInWater && (wasInWater == null || !wasInWater)) {
            entityRainCooldown.put(entityId, currentTime); 
            return new ReactionResult("潮湿", ColorPaletteValue.COLOR_REACTION_WET, entityPos);
        }
        
        if (!isInWater && wasInWater != null && wasInWater) {
            return new ReactionResult("潮湿", ColorPaletteValue.COLOR_REACTION_WET, entityPos);
        }
        
        if (!isInWater && mc.level != null && mc.level.isRaining()) {
            if (mc.level.canSeeSky(entity.blockPosition())) {
                Long lastRainTime = entityRainCooldown.get(entityId);
                if (lastRainTime == null || currentTime - lastRainTime >= RAIN_COOLDOWN_MS) {
                    entityRainCooldown.put(entityId, currentTime);
                    return new ReactionResult("潮湿", ColorPaletteValue.COLOR_REACTION_WET, entityPos);
                }
            }
        }
        
        return null;
    }
    
    public static ReactionResult checkEntityBurningState(Entity entity, Vec3 playerPos) {
        if (!ConfigManager.fancyDmgSplashGenshinReaction) return null;
        if (entity == null) return null;
        
        if (!shouldTriggerReaction(entity)) return null;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        
        long currentTime = System.currentTimeMillis();
        UUID entityId = entity.getUUID();
        Vec3 entityPos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
        
        boolean isPlayer = entity == mc.player;
        if (!isPlayer) {
            double distance = entityPos.distanceTo(playerPos);
            if (distance > 10.0) {
                return null;
            }
        }
        
        boolean isOnFire = entity.isOnFire();
        Boolean wasOnFire = entityWasOnFire.get(entityId);
        entityWasOnFire.put(entityId, isOnFire);
        
        if (isOnFire && (wasOnFire == null || !wasOnFire)) {
            entityFireCooldown.put(entityId, currentTime); 
            return new ReactionResult("燃烧", ColorPaletteValue.COLOR_REACTION_BURNING, entityPos);
        }
        
        if (isOnFire) {
            Long lastFireTime = entityFireCooldown.get(entityId);
            if (lastFireTime == null || currentTime - lastFireTime >= FIRE_COOLDOWN_MS) {
                entityFireCooldown.put(entityId, currentTime);
                return new ReactionResult("燃烧", ColorPaletteValue.COLOR_REACTION_BURNING, entityPos);
            }
        }
        
        return null;
    }
    
    public static ReactionResult checkImmuneReaction(Vec3 targetPos, boolean hasDamage) {
        if (!ConfigManager.fancyDmgSplashGenshinReaction) return null;
        
        long currentTime = System.currentTimeMillis();
        
        if (!hasDamage) {
            if (currentTime - lastImmuneReactionTime >= IMMUNE_COOLDOWN_MS) {
                lastImmuneReactionTime = currentTime;
                return new ReactionResult("免疫", ColorPaletteValue.COLOR_REACTION_IMMUNE, targetPos);
            }
        }
        
        return null;
    }
    
    public static ReactionResult checkPlayerImmuneItem(Vec3 playerPos) {
        if (!ConfigManager.fancyDmgSplashGenshinReaction) return null;
        
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastImmuneReactionTime >= IMMUNE_COOLDOWN_MS) {
            lastImmuneReactionTime = currentTime;
            return new ReactionResult("免疫", ColorPaletteValue.COLOR_REACTION_IMMUNE, playerPos);
        }
        
        return null;
    }
    
    public static ReactionResult checkHealReaction(LivingEntity entity) {
        if (!ConfigManager.fancyDmgSplashGenshinReaction) return null;
        if (entity == null) return null;
        
        UUID entityId = entity.getUUID();
        float currentHealth = entity.getHealth();
        
        Float previousHealth = entityHealthMap.get(entityId);
        entityHealthMap.put(entityId, currentHealth);
        
        if (previousHealth != null && currentHealth > previousHealth) {
            float healAmount = currentHealth - previousHealth;
            if (healAmount >= 0.5f) { 
                Vec3 pos = new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ());
                return new ReactionResult("治疗", ColorPaletteValue.COLOR_REACTION_HEALING, pos);
            }
        }
        
        return null;
    }
    
    public static void updateEntityHealth(LivingEntity entity) {
        if (entity == null) return;
        entityHealthMap.put(entity.getUUID(), entity.getHealth());
    }
    
    public static void cleanupEntityRecords(UUID entityId) {
        entityHealthMap.remove(entityId);
        entityRainCooldown.remove(entityId);
        entityWasInWater.remove(entityId);
        entityInRange.remove(entityId);
        entityWasOnFire.remove(entityId);
        entityFireCooldown.remove(entityId);
    }

    public static boolean isUsingWitherCloak() {
        return witherCloakActive;
    }
 
    public static void setWitherCloakActive(boolean active) {
        witherCloakActive = active;
    }
   
    public static ReactionResult checkWitherCloakImmune() {
        if (!ConfigManager.fancyDmgSplashGenshinReaction) return null;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;
        
        long currentTime = System.currentTimeMillis();
        
        if (isUsingWitherCloak()) {
            if (currentTime - lastWitherCloakImmuneTime >= WITHER_CLOAK_COOLDOWN_MS) {
                lastWitherCloakImmuneTime = currentTime;
                Vec3 playerPos = new Vec3(mc.player.getX(), mc.player.getY() + mc.player.getBbHeight() * 0.5, mc.player.getZ());
                return new ReactionResult("免疫", ColorPaletteValue.COLOR_REACTION_IMMUNE, playerPos);
            }
        }
        
        return null;
    }
    
    public static void clearHistory() {
        recentDamages.clear();
        reactionCounters.clear();
        frozenState = false;
        frozenTargetPos = null;
        quickenState = false;
        quickenTargetPos = null;
        bloomState = false;
        bloomTargetPos = null;
        entityHealthMap.clear();
        entityRainCooldown.clear();
        entityWasInWater.clear();
        entityInRange.clear();
        entityWasOnFire.clear();
        entityFireCooldown.clear();
        lastWetReactionTime = 0;
        lastImmuneReactionTime = 0;
        lastWitherCloakImmuneTime = 0;
        witherCloakActive = false;
    }
    
    private static class DamageRecord {
        final int color;
        final Vec3 pos;
        final long time;
        
        DamageRecord(int color, Vec3 pos, long time) {
            this.color = color;
            this.pos = pos;
            this.time = time;
        }
    }
    
    public static class ReactionResult {
        public final String name;
        public final int color;
        public final Vec3 position;
        
        public ReactionResult(String name, int color) {
            this.name = name;
            this.color = color;
            this.position = null;
        }
        
        public ReactionResult(String name, int color, Vec3 position) {
            this.name = name;
            this.color = color;
            this.position = position;
        }
    }
}

