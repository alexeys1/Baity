package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.AutoSprint;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class AutoSprintMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void baity$autoSprint(CallbackInfo ci) {
        Input input = this.keyPresses;
        if (!AutoSprint.shouldAutoSprint(input.forward())) {
            return;
        }
        this.keyPresses = new Input(
            input.forward(),
            input.backward(),
            input.left(),
            input.right(),
            input.jump(),
            input.shift(),
            true
        );
    }
}
