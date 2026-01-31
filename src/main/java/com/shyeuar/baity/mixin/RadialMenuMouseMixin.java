package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.RadialMenu;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class RadialMenuMouseMixin {

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(CallbackInfo ci) {
        if (RadialMenu.isOpen()) {
            ci.cancel();
        }
    }
}
