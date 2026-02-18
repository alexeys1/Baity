package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public class NoTextShadowMixin {

    @Inject(
        method = "getShadowColor(Lnet/minecraft/network/chat/Style;I)I",
        at = @At("HEAD"),
        cancellable = true
    )
    private void baity$noTextShadow$forceNoShadow(Style style, int baseColor, CallbackInfoReturnable<Integer> cir) {
        if (ConfigManager.noTextShadowEnabled) {
            cir.setReturnValue(0);
        }
    }
}

