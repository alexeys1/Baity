package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.prismbreak.PrismBreak;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class PrismBreakMixin {

    @Inject(method = "submitBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
    private void baity$prismBreak(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (PrismBreak.isActive()) {
            if (ConfigManager.prismBreakReplaceVanilla) {
                ci.cancel();
            }
            PrismBreak.render((LevelRenderer) (Object) this, levelRenderState);
        }
    }
}