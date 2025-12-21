package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.BlockAnimationUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public abstract class BlockAnimationMixin {

    @Mixin(HeldItemRenderer.class)
    public static abstract class MixinHeldItemRenderer {

        private static final String RENDER_METHOD = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";

        @WrapOperation(
            method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isUsingItem()Z")
        )
        private boolean blockAnimation$wrapIsUsingItem(AbstractClientPlayerEntity player, Operation<Boolean> original) {
            if (player != null && BlockAnimationUtils.isEntityBlocking(player)) {
                return true;
            }
            return original.call(player);
        }

        @WrapOperation(
            method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getItemUseTimeLeft()I")
        )
        private int blockAnimation$wrapGetItemUseTimeLeft(AbstractClientPlayerEntity player, Operation<Integer> original) {
            if (player != null && BlockAnimationUtils.isEntityBlocking(player)) {
                ItemStack mainHand = player.getMainHandStack();
                ItemStack offHand = player.getOffHandStack();
                if (BlockAnimationUtils.isSword(mainHand.getItem()) || BlockAnimationUtils.isSword(offHand.getItem())) {
                    return 20;
                }
            }
            return original.call(player);
        }

        @WrapOperation(
            method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getActiveHand()Lnet/minecraft/util/Hand;")
        )
        private Hand blockAnimation$wrapGetActiveHand(AbstractClientPlayerEntity player, Operation<Hand> original) {
            if (player != null && BlockAnimationUtils.isEntityBlocking(player)) {
                Hand blockingHand = BlockAnimationUtils.getBlockingHand(player);
                if (blockingHand != null) {
                    return blockingHand;
                }
            }
            return original.call(player);
        }

        @WrapOperation(
            method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;")
        )
        private UseAction blockAnimation$wrapGetUseAction(ItemStack stack, Operation<UseAction> original) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null && BlockAnimationUtils.isEntityBlocking(mc.player)) {
                if (BlockAnimationUtils.isSword(stack.getItem())) {
                    return UseAction.BLOCK;
                }
            }
            return original.call(stack);
        }
    }
}
