package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.moderntooltip.TooltipAnimation;
import com.shyeuar.baity.gui.ClickGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ModernTooltipScreenMixin {

    @Inject(
            method = "extractRenderStateWithTooltipAndSubtitles",
            at = @At("RETURN")
    )
    private void baity$modernTooltipScreenRenderEnd(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof ClickGui) {
            return;
        }
        TooltipAnimation.onScreenRenderEnd();
    }
}
