package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.FancyCreeperVeil;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(CreeperPowerLayer.class)
public class FancyCreeperVeilMixin {

    private static final Minecraft MC = Minecraft.getInstance();

    @Inject(method = "isPowered(Lnet/minecraft/client/renderer/entity/state/CreeperRenderState;)Z", 
            at = @At("HEAD"), cancellable = true)
    private void baity$hideWitherCloakCreeperCharge(CreeperRenderState creeperRenderState, CallbackInfoReturnable<Boolean> cir) {
        Module module = ModuleManager.getModuleByName("FancyCreeperVeil");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.fancyCreeperVeilEnabled) return;
        
        if (MC.player == null || MC.level == null) return;
        if (!creeperRenderState.isPowered) return;
        
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
            MC.player.getX() - 10.0, MC.player.getY() - 10.0, MC.player.getZ() - 10.0,
            MC.player.getX() + 10.0, MC.player.getY() + 10.0, MC.player.getZ() + 10.0
        );
        
        net.minecraft.world.phys.Vec3 renderStatePos = new net.minecraft.world.phys.Vec3(
            creeperRenderState.x, creeperRenderState.y, creeperRenderState.z
        );
        
        net.minecraft.world.entity.monster.Creeper closestCreeper = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (net.minecraft.world.entity.monster.Creeper creeper : MC.level.getEntitiesOfClass(
                net.minecraft.world.entity.monster.Creeper.class, searchBox)) {
            if (!creeper.isAlive()) continue;
            
            boolean isWitherCloak = 
                creeper.isInvisible() &&
                creeper.getMaxHealth() == 20.0f &&
                creeper.distanceTo(MC.player) < 7.5 &&
                (com.shyeuar.baity.features.fancydmgsplash.ElementalReactionDetector.isUsingWitherCloak() ||
                 System.currentTimeMillis() - FancyCreeperVeil.lastDeactivate < 300);
            
            if (isWitherCloak) {
                float tickDelta = MC.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                net.minecraft.world.phys.Vec3 creeperInterpolatedPos = new net.minecraft.world.phys.Vec3(
                    net.minecraft.util.Mth.lerp(tickDelta, creeper.xOld, creeper.getX()),
                    net.minecraft.util.Mth.lerp(tickDelta, creeper.yOld, creeper.getY()),
                    net.minecraft.util.Mth.lerp(tickDelta, creeper.zOld, creeper.getZ())
                );
                
                double distanceToRenderState = creeperInterpolatedPos.distanceTo(renderStatePos);
                
                if (distanceToRenderState < 3.0 && distanceToRenderState < closestDistance) {
                    closestDistance = distanceToRenderState;
                    closestCreeper = creeper;
                }
            }
        }
        
        if (closestCreeper != null) {
            FancyCreeperVeil.lastCreeperRender = System.currentTimeMillis();
            cir.setReturnValue(false);
        }
    }
    
}
