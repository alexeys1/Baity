package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.render.RenderScope;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class PaperDollArmorMixin {

    @Mixin(HumanoidArmorLayer.class)
    public static class HumanoidArmorLayerMixin {
        @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$hidePaperDollArmor(
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                int packedLight,
                HumanoidRenderState state,
                float limbSwing,
                float limbSwingAmount,
                CallbackInfo ci
        ) {
            if (RenderScope.shouldHidePaperDollArmor(state)) {
                ci.cancel();
            }
        }
    }

    @Mixin(CustomHeadLayer.class)
    public static class CustomHeadLayerMixin {
        @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
        )
        private void baity$hidePaperDollHeadEquipment(
                PoseStack poseStack,
                SubmitNodeCollector submitNodeCollector,
                int packedLight,
                LivingEntityRenderState state,
                float limbSwing,
                float limbSwingAmount,
                CallbackInfo ci
        ) {
            if (RenderScope.shouldHidePaperDollArmor(state)) {
                ci.cancel();
            }
        }
    }
}
