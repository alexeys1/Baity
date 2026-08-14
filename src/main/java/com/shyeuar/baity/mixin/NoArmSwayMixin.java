package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.utils.HeldItemTweaksUtils;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandRenderer.class)
public class NoArmSwayMixin {

    @WrapWithCondition(
            method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"
            )
    )
    private boolean baity$skipHandViewBobSync(PoseStack poseStack, Quaternionfc quaternion) {
        return !HeldItemTweaksUtils.isNoArmSwayActive();
    }
}
