package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.fancydmgsplash.ElementalReactionDetector;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplash;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ImmuneReactionMixin {

    @Mixin(ChatComponent.class)
    public static class ChatDetectorMixin {
        @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD")
        )
        private void baity$onChatMessage(
            Component message,
            MessageSignature signature,
            GuiMessageSource source,
            GuiMessageTag tag,
            CallbackInfo ci
        ) {
            ElementalReactionDetector.handleWitherCloakChat(message);
        }
    }

    @Mixin(LocalPlayer.class)
    public static class PlayerTickMixin {
        @Unique
        private long lastWitherCloakCheckTime = 0;

        @Unique
        private static final long WITHER_CLOAK_CHECK_INTERVAL = 200;

        @Inject(method = "tick", at = @At("HEAD"))
        private void baity$checkWitherCloakImmune(CallbackInfo ci) {
            Module m = ModuleManager.getModuleByName("FancyDmgSplash");
            if (m == null || !m.isEnabled()) return;
            if (!com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplashSettings.isGenshinReactionEnabled()) return;

            Minecraft mc = Minecraft.getInstance();
            LocalPlayer self = (LocalPlayer) (Object) this;
            if (mc.player != self) return;

            long currentTime = System.currentTimeMillis();

            if (currentTime - lastWitherCloakCheckTime >= WITHER_CLOAK_CHECK_INTERVAL) {
                lastWitherCloakCheckTime = currentTime;

                ElementalReactionDetector.ReactionResult result = ElementalReactionDetector.checkWitherCloakImmune();
                if (result != null) {
                    FancyDmgSplash.addWitherCloakImmuneReaction(result);
                }
            }
        }
    }
}
