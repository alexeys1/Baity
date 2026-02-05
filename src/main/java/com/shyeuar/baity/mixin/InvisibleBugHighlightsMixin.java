package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.highlights.InvisibleBugDetector;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class InvisibleBugHighlightsMixin {

    @Mixin(ClientPacketListener.class)
    public static class ParticlePacketMixin {

        private static final Minecraft MC = Minecraft.getInstance();

        @Inject(
            method = "handleParticleEvent",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                shift = At.Shift.AFTER
            )
        )
        private void baity$detectInvisibleBugParticle(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
            Module module = ModuleManager.getModuleByName("Highlights");
            if (module == null || !module.isEnabled()) return;
            if (!ConfigManager.highlightsInvisibleBugEnabled) return;
            
            if (MC.level == null || MC.player == null) return;
            
            if (packet.getParticle().getType() != ParticleTypes.CRIT) return;
            
            Vec3 particleLocation = new Vec3(packet.getX(), packet.getY(), packet.getZ());
            
            InvisibleBugDetector.detectParticleAtLocation(particleLocation);
        }
    }

    @Mixin(Entity.class)
    public static class EntityRemovalMixin {

        @Inject(method = "remove", at = @At("HEAD"))
        private void baity$onEntityRemoved(CallbackInfo ci) {
            Entity self = (Entity) (Object) this;
            if (self instanceof ArmorStand) {
                InvisibleBugDetector.removeEntity((ArmorStand) self);
            }
        }
    }

}
