package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundSystem.class)
public class MufflerMixin {

    private static final Identifier ENDERMAN_SCREAM = Identifier.of("minecraft", "entity.enderman.scream");
    private static final Identifier ENDERMAN_STARE = Identifier.of("minecraft", "entity.enderman.stare");

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void baity$muteSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        Module m = ModuleManager.getModuleByName("Muffler");
        if (m == null || !m.isEnabled()) return;

        Identifier soundId = sound.getId();
        
        if (ModuleUtils.getOptionBoolean(m, "mute enderman scream", true)) {
            if (soundId.equals(ENDERMAN_SCREAM) || soundId.equals(ENDERMAN_STARE)) {
                cir.setReturnValue(SoundSystem.PlayResult.NOT_STARTED);
            }
        }
    }
}
