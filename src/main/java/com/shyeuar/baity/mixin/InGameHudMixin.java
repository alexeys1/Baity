package com.shyeuar.baity.mixin;

import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void baity$beginHudRenderPhase(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderScope.enterHudRenderPhase();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void baity$endHudRenderPhase(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderScope.exitHudRenderPhase();
    }
}
