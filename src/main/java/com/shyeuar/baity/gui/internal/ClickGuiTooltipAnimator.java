package com.shyeuar.baity.gui.internal;

import com.shyeuar.baity.gui.animation.TooltipSizeAnimator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ClickGuiTooltipAnimator {

    private final TooltipSizeAnimator sizeAnimator = new TooltipSizeAnimator();

    public TooltipSizeAnimator.Frame update(int signature, float targetWidth, float targetHeight) {
        return sizeAnimator.update(signature, targetWidth, targetHeight);
    }

    public void endFrame(boolean tooltipVisible) {
        sizeAnimator.endFrame(tooltipVisible);
    }

    public void reset() {
        sizeAnimator.reset();
    }
}
