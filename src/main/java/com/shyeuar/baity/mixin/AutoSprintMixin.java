package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.AutoSprint;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class AutoSprintMixin {

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void baity$autoSprint(CallbackInfoReturnable<Boolean> cir) {
        AutoSprint.handleSprintKeyIsDown((KeyMapping) (Object) this, cir);
    }
}
