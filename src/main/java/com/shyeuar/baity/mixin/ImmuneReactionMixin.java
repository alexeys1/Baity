package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.fancydmgsplash.ElementalReactionDetector;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplash;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ImmuneReactionMixin {

    @Mixin(ChatComponent.class)
    public static class ChatDetectorMixin {
        @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
        private void baity$onChatMessage(Component message, CallbackInfo ci) {
            if (message == null) return;

            String text = message.getString();

            if (text.contains("Creeper Veil Activated!")) {
                ElementalReactionDetector.setWitherCloakActive(true);
            } else if (text.contains("Creeper Veil De-activated!") || 
                       text.contains("Not enough mana! Creeper Veil De-activated!")) {
                ElementalReactionDetector.setWitherCloakActive(false);
                com.shyeuar.baity.features.FancyCreeperVeil.lastDeactivate = System.currentTimeMillis();
            }
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
            if (!com.shyeuar.baity.config.ConfigManager.fancyDmgSplashGenshinReaction) return;

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
