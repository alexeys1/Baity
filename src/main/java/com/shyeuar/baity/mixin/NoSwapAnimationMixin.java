package com.shyeuar.baity.mixin;

import com.shyeuar.baity.mixin.accessor.ItemInHandRendererAccessor;
import com.shyeuar.baity.utils.HeldItemTweaksUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class NoSwapAnimationMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void baity$removeSwapAnimation(CallbackInfo ci) {
        if (!HeldItemTweaksUtils.isNoItemswapAnimationActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemInHandRenderer renderer = (ItemInHandRenderer) (Object) this;
        ItemInHandRendererAccessor accessor = (ItemInHandRendererAccessor) renderer;

        accessor.baity$setMainHandItem(mc.player.getMainHandItem());
        accessor.baity$setOffHandItem(mc.player.getOffhandItem());
        accessor.baity$setMainHandHeight(1.0f);
        accessor.baity$setOffHandHeight(1.0f);
    }
}
