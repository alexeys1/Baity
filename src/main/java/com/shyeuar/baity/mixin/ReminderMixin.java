package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.Reminder;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ReminderMixin {

    @Mixin(ClientPacketListener.class)
    public static class SystemChatMixin {

        @Inject(method = "handleSystemChat", at = @At("HEAD"))
        private void baity$reminderOnSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
            Reminder.onSystemChat(packet.content(), packet.overlay());
        }
    }
}
