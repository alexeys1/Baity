package com.shyeuar.baity.mixin;

import com.shyeuar.baity.render.BaityModernUiFloatingTextTypes;
import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class ModernUiFloatingTextMixin {

    private static final int MODE_SDF_FILL = 1;

    @Mixin(targets = "icyllis.modernui.mc.text.ModernTextRenderer", remap = false)
    @Pseudo
    public static class ChooseModeMixin {

        @Inject(method = "chooseMode", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
        private void baity$floatingWorldTextMode(
                Matrix4fc ctm,
                Font.DisplayMode displayMode,
                CallbackInfoReturnable<Integer> cir
        ) {
            if (RenderScope.isFloatingWorldText() && displayMode == Font.DisplayMode.SEE_THROUGH) {
                cir.setReturnValue(MODE_SDF_FILL);
            }
        }
    }

    @Mixin(targets = "icyllis.modernui.mc.text.TextRenderType", remap = false)
    @Pseudo
    public static class GetOrCreateMixin {

        @Inject(
                method = "getOrCreate(Lnet/minecraft/resources/Identifier;I)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                at = @At("HEAD"),
                cancellable = true,
                remap = false,
                require = 0
        )
        private static void baity$floatingWorldTextRenderType(
                Identifier texture,
                int mode,
                CallbackInfoReturnable<RenderType> cir
        ) {
            if (mode == MODE_SDF_FILL && RenderScope.isFloatingWorldText()) {
                cir.setReturnValue(BaityModernUiFloatingTextTypes.getOrCreate(texture));
            }
        }
    }
}
