package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.highlights.PestEntityRegistry;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class PestEntityDataMixin {

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void baity$afterEntityDataForPestRegistry(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        PestEntityRegistry.afterClientEntityData(packet.id());
    }

    @Inject(method = "handleSetEquipment", at = @At("TAIL"))
    private void baity$afterEquipmentForPestRegistry(ClientboundSetEquipmentPacket packet, CallbackInfo ci) {
        PestEntityRegistry.afterClientEntityData(packet.getEntity());
    }
}