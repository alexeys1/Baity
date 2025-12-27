package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogModifier;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
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

public class CullingMixin {
    @Mixin(net.minecraft.client.render.entity.ArmorStandEntityRenderer.class)
    public static class HideNonStarredMixin {
        
        @Unique
        private static final Set<String> DUNGEON_MOB_NAMES = Set.of(
            "Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton",
            "Skeletor", "Sniper", "Super Archer", "Spider", "Fels", "Withermancer"
        );
        
        @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ArmorStandEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$hideNonStarredNametag(net.minecraft.client.render.entity.state.ArmorStandEntityRenderState state,
                MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
                net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo ci) {
            
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingHideNonStarredNametag) return;
            if (!checkInDungeon()) return;
            
            if (state.displayName == null) return;
            
            String nameText = state.displayName.getString();
            
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
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getNetworkHandler() == null) return false;
            for (var entry : mc.getNetworkHandler().getPlayerList()) {
                if (entry.getDisplayName() != null) {
                    String text = entry.getDisplayName().getString().trim();
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
        private static List<FogModifier> FOG_MODIFIERS;
        
        @Inject(method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
                at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/fog/FogData;renderDistanceEnd:F", shift = At.Shift.AFTER, ordinal = 0))
        private void baity$removeWaterFog(Camera camera, int viewDistance, boolean thick, 
                RenderTickCounter tickCounter, float skyDarkness, ClientWorld world, 
                CallbackInfoReturnable<Vector4f> cir, @Local FogData fogData) {
            
            Module m = ModuleManager.getModuleByName("Culling");
            if (m == null || !m.isEnabled()) return;
            if (!ConfigManager.cullingRemoveUnderwaterFog) return;
            
            CameraSubmersionType submersionType = camera.getSubmersionType();
            Entity entity = camera.getFocusedEntity();
            
            if (submersionType != CameraSubmersionType.WATER) return;
            
            for (FogModifier modifier : FOG_MODIFIERS) {
                if (modifier.shouldApply(submersionType, entity)) {
                    fogData.environmentalStart = Float.MAX_VALUE;
                    fogData.environmentalEnd = Float.MAX_VALUE;
                    fogData.renderDistanceStart = Float.MAX_VALUE;
                    fogData.renderDistanceEnd = Float.MAX_VALUE;
                    fogData.skyEnd = Float.MAX_VALUE;
                    fogData.cloudEnd = Float.MAX_VALUE;
                    return;
                }
            }
        }
    }
    
    @Mixin(net.minecraft.client.render.entity.LivingEntityRenderer.class)
    public static class HideDyingMobMixin {
        
        @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$hideDyingMob(net.minecraft.client.render.entity.state.LivingEntityRenderState state,
                MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
                net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo ci) {
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
        
        @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
        private void baity$cancelHurtCam(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("NoHurtCam");
            if (m != null && m.isEnabled()) {
                ci.cancel();
            }
        }
    }
}
