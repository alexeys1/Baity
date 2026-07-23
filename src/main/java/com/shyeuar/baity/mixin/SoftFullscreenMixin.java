package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.platform.Window;
import com.shyeuar.baity.features.SoftFullscreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class SoftFullscreenMixin {

    @Inject(method = "toggleFullScreen", at = @At("HEAD"))
    private void baity$softFullscreenBeforeToggle(CallbackInfo ci) {
        SoftFullscreen.onBeforeToggleFullScreen(Minecraft.getInstance());
    }
}
