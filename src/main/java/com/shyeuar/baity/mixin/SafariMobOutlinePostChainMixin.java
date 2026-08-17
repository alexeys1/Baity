package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.highlights.SafariHighlights;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public abstract class SafariMobOutlinePostChainMixin {

    @Inject(method = "submitEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V", at = @At("HEAD"))
    private void baity$enableSafariMobOutlinePostChain(
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            SubmitNodeCollector submitNodeCollector,
            CallbackInfo ci) {
        if (SafariHighlights.isSafariMobOutlineActive()) {
            levelRenderState.shouldShowEntityOutlines = true;
        }
    }
}