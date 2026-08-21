package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.features.PrismBreak;
import com.shyeuar.baity.mixin.accessor.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class PrismBreakMixin {

    @Mixin(LevelRenderer.class)
    public static abstract class SubmitMixin {

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

    @Mixin(ClientLevel.class)
    public static abstract class DestroyProgressMixin {

        @Inject(method = "destroyBlockProgress", at = @At("HEAD"), cancellable = true)
        private void baity$keepServerBreakProgress(int id, BlockPos pos, int progress, CallbackInfo ci) {
            if (progress >= 0 || !PrismBreak.isActive()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || id != mc.player.getId()) {
                return;
            }
            if (mc.gameMode == null || !mc.gameMode.isDestroying()) {
                return;
            }
            BlockPos destroying = ((MultiPlayerGameModeAccessor) mc.gameMode).baity$getDestroyBlockPos();
            if (destroying != null && destroying.equals(pos)) {
                ci.cancel();
            }
        }
    }
}
