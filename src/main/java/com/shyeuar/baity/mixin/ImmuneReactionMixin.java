package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.ElementalReactionDetector;
import com.shyeuar.baity.features.FancyDmgSplash;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class ImmuneReactionMixin {

    /**
     * 监听聊天消息，检测 Wither Cloak 激活/失效
     */
    @Mixin(ChatHud.class)
    public static class ChatDetectorMixin {
        @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
        private void baity$onChatMessage(Text message, CallbackInfo ci) {
            if (message == null) return;

            String text = message.getString();

            if (text.contains("Creeper Veil Activated!")) {
                ElementalReactionDetector.setWitherCloakActive(true);
            } else if (text.contains("Creeper Veil De-activated!")) {
                ElementalReactionDetector.setWitherCloakActive(false);
            }
        }
    }

    /**
     * 定期检测 Wither Cloak 状态并触发免疫反应
     */
    @Mixin(ClientPlayerEntity.class)
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

            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
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
