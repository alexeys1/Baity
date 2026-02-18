package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NameTagFeatureRenderer.Storage.class)
public class TagBackgroundMixin {

    @ModifyArg(
        method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeStorage$NameTagSubmit;<init>(Lorg/joml/Matrix4f;FFLnet/minecraft/network/chat/Component;IIID)V"
        ),
        index = 6
    )
    private int baity$tagBackground$backgroundColor(int originalBackgroundColor) {
        Module m = ModuleManager.getModuleByName("Nametag");
        if (m == null || !m.isEnabled()) {
            return originalBackgroundColor;
        }

        boolean enabled = ModuleUtils.getOptionBoolean(m, "transparentize other tags", true);
        if (!enabled) {
            return originalBackgroundColor;
        }

        return 0;
    }
}

