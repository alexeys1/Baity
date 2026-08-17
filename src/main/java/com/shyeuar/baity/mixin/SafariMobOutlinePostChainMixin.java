package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.highlights.SafariHighlights;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class SafariMobOutlinePostChainMixin {

    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void baity$enableSafariMobOutlinePostChain(
            Camera camera,
            Frustum frustum,
            DeltaTracker deltaTracker,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        if (SafariHighlights.isSafariMobOutlineActive()) {
            levelRenderState.haveGlowingEntities = true;
        }
    }
}
