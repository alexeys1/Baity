package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;

public class CullingMixin {
    @Mixin(net.minecraft.client.renderer.entity.ArmorStandRenderer.class)
    public static class HideNonStarredMixin {
        
        @Unique
        private static final Set<String> DUNGEON_MOB_NAMES = Set.of(
            "Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton",
            "Skeletor", "Sniper", "Super Archer", "Spider", "Fels", "Withermancer"
        );
        
        @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$hideNonStarredNametag(net.minecraft.client.renderer.entity.state.ArmorStandRenderState state,
                PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector queue,
                net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
            
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingHideNonStarredNametag) return;
            if (!checkInDungeon()) return;
            
            if (state.nameTag == null) return;
            
            String nameText = state.nameTag.getString();
            
            if (!nameText.contains("✯ ") && nameText.contains("❤") && containsDungeonMobName(nameText)) {
                ci.cancel();
            }
        }
        
        @Unique
        private static boolean containsDungeonMobName(String text) {
            for (String mobName : DUNGEON_MOB_NAMES) {
                if (text.contains(mobName)) {
                    return true;
                }
            }
            return false;
        }
        
        @Unique
        private static boolean checkInDungeon() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null) return false;
            for (var entry : mc.getConnection().getOnlinePlayers()) {
                if (entry.getTabListDisplayName() != null) {
                    String text = entry.getTabListDisplayName().getString().trim();
                    if (text.startsWith("Dungeon:") || (text.startsWith("Area:") && text.contains("Catacombs"))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
   
    @Mixin(value = FogRenderer.class, priority = 1500)
    public static class RemoveWaterFogMixin {
        
        @Shadow
        @Final
        private static List<FogEnvironment> FOG_ENVIRONMENTS;
        
        @Inject(method = "setupFog(Lnet/minecraft/client/Camera;IZLnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
                at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogData;renderDistanceEnd:F", shift = At.Shift.AFTER, ordinal = 0))
        private void baity$removeWaterFog(Camera camera, int viewDistance, boolean thick, 
                DeltaTracker tickCounter, float skyDarkness, ClientLevel world, 
                CallbackInfoReturnable<Vector4f> cir, @Local FogData fogData) {
            
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingRemoveUnderwaterFog) return;
            
            FogType submersionType = camera.getFluidInCamera();
            Entity entity = camera.getEntity();
            
            if (submersionType != FogType.WATER) return;
            
            for (int i = 0; i < FOG_ENVIRONMENTS.size(); i++) {
                FogEnvironment modifier = FOG_ENVIRONMENTS.get(i);
                if (modifier.isApplicable(submersionType, entity)) {
                    fogData.environmentalStart = Float.MAX_VALUE;
                    fogData.environmentalEnd = Float.MAX_VALUE;
                    fogData.renderDistanceStart = Float.MAX_VALUE;
                    fogData.renderDistanceEnd = Float.MAX_VALUE;
                    break;
                }
            }
        }
    }
    
    @Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
    public static class HideDyingMobMixin {
        
        @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$hideDyingMob(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,
                PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector queue,
                net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingHideDyingMob) return;
            
            if (state.deathTime > 0) {
                ci.cancel();
            }
        }
    }
    
    @Mixin(GameRenderer.class)
    public static class NoHurtCamMixin {
        
        @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
        private void baity$cancelHurtCam(PoseStack matrices, float tickProgress, CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("NoHurtCam");
            if (m != null && m.isEnabled()) {
                ci.cancel();
            }
        }
    }
}
