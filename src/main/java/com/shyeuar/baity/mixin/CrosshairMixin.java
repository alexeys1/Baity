package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class CrosshairMixin {
    @ModifyExpressionValue(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;"))
    private CameraType baity$forceCrosshairInThirdPersonRear(CameraType original) {
        com.shyeuar.baity.gui.module.Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
        if (smolPeopleModule == null || !smolPeopleModule.isEnabled()) return original;
        
        boolean crosshairMode = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(smolPeopleModule, "crosshair", false);
        if (crosshairMode && original == CameraType.THIRD_PERSON_BACK) {
            return CameraType.FIRST_PERSON;
        }
        return original;
    }
}
