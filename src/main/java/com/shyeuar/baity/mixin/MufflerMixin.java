package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MufflerMixin {

    private static final ResourceLocation ENDERMAN_SCREAM = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.enderman.scream");
    private static final ResourceLocation ENDERMAN_STARE = ResourceLocation.fromNamespaceAndPath("minecraft", "entity.enderman.stare");

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At("HEAD"), cancellable = true)
    private void baity$muteSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        Module m = ModuleManager.getModuleByName("Muffler");
        if (m == null || !m.isEnabled()) return;

        ResourceLocation soundId = sound.getLocation();
        
        if (ModuleUtils.getOptionBoolean(m, "mute enderman scream", true)) {
            if (soundId.equals(ENDERMAN_SCREAM) || soundId.equals(ENDERMAN_STARE)) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }
        }
        
        if (ModuleUtils.getOptionBoolean(m, "mute phantom", true)) {
            if (isInGalatea() && soundId.getPath().startsWith("entity.phantom")) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            }
        }
    }
    
    private static boolean isInGalatea() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        return true;
    }
}
