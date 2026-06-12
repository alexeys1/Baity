package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.FancyCreeperVeil;
import com.shyeuar.baity.features.fancydmgsplash.ElementalReactionDetector;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.config.ConfigManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(CreeperPowerLayer.class)
public class FancyCreeperVeilMixin {

    private static final Minecraft MC = Minecraft.getInstance();

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void baity$hideWitherCloakCreeperCharge(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        EntityRenderState entityRenderState,
        float f,
        float g,
        CallbackInfo ci
    ) {
        Module module = ModuleManager.getModuleByName("FancyCreeperVeil");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.fancyCreeperVeilEnabled) return;
        
        if (MC.player == null || MC.level == null) return;
        if (!(entityRenderState instanceof CreeperRenderState creeperRenderState)) return;
        if (!creeperRenderState.isPowered) return;

        long now = System.currentTimeMillis();
        boolean cloakActive = ElementalReactionDetector.isUsingWitherCloak() || now - FancyCreeperVeil.lastDeactivate < 300;
        if (!cloakActive) return;

        Vec3 renderPos = new Vec3(entityRenderState.x, entityRenderState.y, entityRenderState.z);
        double radius = 1.0;
        AABB searchBox = new AABB(
            renderPos.x - radius, renderPos.y - 2.0, renderPos.z - radius,
            renderPos.x + radius, renderPos.y + 2.0, renderPos.z + radius
        );

        Creeper candidate = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Creeper creeper : MC.level.getEntitiesOfClass(Creeper.class, searchBox)) {
            if (!creeper.isAlive()) continue;
            double dSq = creeper.position().distanceToSqr(renderPos);
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                candidate = creeper;
            }
        }

        if (candidate == null) return;

        boolean isWitherCloakCreeper =
            candidate.isInvisible()
                && candidate.getMaxHealth() == 20.0f
                && candidate.distanceTo(MC.player) < 10.5f;

        if (!isWitherCloakCreeper) return;

        FancyCreeperVeil.lastCreeperRender = now;
        ci.cancel();
    }
    
}
