package com.shyeuar.baity.features.highlights;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
        
        for (LivingEntity existing : invisbugEntities) {
            if (existing != null && existing.isAlive()) {
                Vec3 entityPos = existing.position();
                if (entityPos.distanceTo(particleLocation) < DETECTION_DISTANCE) {
                    return;
                }
            }
        }
        
        AABB searchBox = new AABB(
            particleLocation.x - DETECTION_DISTANCE,
            particleLocation.y - DETECTION_DISTANCE,
            particleLocation.z - DETECTION_DISTANCE,
            particleLocation.x + DETECTION_DISTANCE,
            particleLocation.y + DETECTION_DISTANCE,
            particleLocation.z + DETECTION_DISTANCE
        );
        
        ArmorStand nearestArmorStand = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (ArmorStand armorStand : MC.level.getEntitiesOfClass(ArmorStand.class, searchBox)) {
            if (armorStand == null || !armorStand.isAlive()) continue;
            
            Vec3 entityPos = armorStand.position();
            double distance = entityPos.distanceTo(particleLocation);
            
            if (distance < nearestDistance && isDefaultArmorStandCandidate(armorStand)) {
                nearestDistance = distance;
                nearestArmorStand = armorStand;
            }
        }
        
        final ArmorStand finalNearestArmorStand = nearestArmorStand;
        if (finalNearestArmorStand != null) {
            MC.execute(() -> invisbugEntities.add(finalNearestArmorStand));
        }
    }
    
    private static boolean isDefaultArmorStandCandidate(ArmorStand armorStand) {
        if (armorStand == null || !armorStand.isAlive()) return false;
        
        Component nameComponent = armorStand.getName();
        String nameText = nameComponent.getString();
        String defaultArmorStandName = net.minecraft.client.resources.language.I18n.get("entity.minecraft.armor_stand");
        if (!nameText.equals(defaultArmorStandName) || armorStand.hasCustomName()) {
            return false;
        }
        
        return hasEmptyInventory(armorStand);
    }
    
    private static boolean hasEmptyInventory(ArmorStand armorStand) {
        ItemStack mainHand = armorStand.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack offHand = armorStand.getItemBySlot(EquipmentSlot.OFFHAND);
        ItemStack head = armorStand.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = armorStand.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = armorStand.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack feet = armorStand.getItemBySlot(EquipmentSlot.FEET);
        
        return mainHand.isEmpty() && offHand.isEmpty() && 
               head.isEmpty() && chest.isEmpty() && 
               legs.isEmpty() && feet.isEmpty();
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

