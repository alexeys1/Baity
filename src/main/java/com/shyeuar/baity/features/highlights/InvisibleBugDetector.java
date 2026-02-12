package com.shyeuar.baity.features.highlights;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class InvisibleBugDetector {
    
    private static final double DETECTION_DISTANCE = 5.0;
    
    private static final Set<LivingEntity> invisbugEntities = new CopyOnWriteArraySet<>();
    
    private static final Minecraft MC = Minecraft.getInstance();
    
    public static void detectParticleAtLocation(Vec3 particleLocation) {
        if (particleLocation == null) return;
        if (MC.level == null || MC.player == null) return;
        
        if (!hasInvisibugMarker(particleLocation)) {
            return;
        }
        
        for (LivingEntity existing : invisbugEntities) {
            if (existing != null && existing.isAlive()) {
                Vec3 entityPos = existing.position();
                if (entityPos.distanceTo(particleLocation) < DETECTION_DISTANCE) {
                    return;
                }
            }
        }
        
        AABB searchBox = new AABB(
            particleLocation.x - 0.5,
            particleLocation.y - 1.0,
            particleLocation.z - 0.5,
            particleLocation.x + 0.5,
            particleLocation.y + 1.0,
            particleLocation.z + 0.5
        );
        
        ArmorStand markerArmorStand = null;
        
        for (ArmorStand armorStand : MC.level.getEntitiesOfClass(ArmorStand.class, searchBox)) {
            if (armorStand == null || !armorStand.isAlive()) continue;
            
            if (isInvisibugMarker(armorStand)) {
                markerArmorStand = armorStand;
                break;
            }
        }
        
        final ArmorStand finalMarkerArmorStand = markerArmorStand;
        if (finalMarkerArmorStand != null) {
            MC.execute(() -> invisbugEntities.add(finalMarkerArmorStand));
        }
    }
    
    private static boolean hasInvisibugMarker(Vec3 pos) {
        AABB searchBox = new AABB(
            pos.x - 0.5,
            pos.y - 1.0,
            pos.z - 0.5,
            pos.x + 0.5,
            pos.y + 1.0,
            pos.z + 0.5
        );
        
        java.util.List<ArmorStand> markers = new java.util.ArrayList<>();
        for (ArmorStand armorStand : MC.level.getEntitiesOfClass(ArmorStand.class, searchBox)) {
            if (armorStand != null && armorStand.isAlive() && isInvisibugMarker(armorStand)) {
                markers.add(armorStand);
            }
        }
        
        return markers.size() == 1;
    }
    
    private static boolean isInvisibugMarker(ArmorStand armorStand) {
        if (armorStand == null || !armorStand.isAlive()) return false;
        
        if (!armorStand.isMarker()) return false;
        
        if (armorStand.hasCustomName()) return false;
        
        ItemStack mainHand = armorStand.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHand.isEmpty()) return false;
        
        return true;
    }
  
    public static Set<LivingEntity> getCurrentInvisbugEntities() {
        invisbugEntities.removeIf(entity -> entity == null || !entity.isAlive());
        return new CopyOnWriteArraySet<>(invisbugEntities);
    }
    
    public static void removeEntity(ArmorStand entity) {
        invisbugEntities.remove(entity);
    }
    
    public static void clear() {
        invisbugEntities.clear();
    }
}

