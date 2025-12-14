package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.BlockAnimationUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

        /**
         * 拦截 player.isUsingItem() 调用，如果玩家正在"格挡"则返回 true
         */
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

        /**
         * 拦截 player.getItemUseTimeLeft() 调用，如果玩家正在"格挡"则返回 > 0 的值
         */
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

        /**
         * 拦截 player.getActiveHand() 调用，如果玩家正在"格挡"则返回持剑的手
         */
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

        /**
         * 拦截 item.getUseAction() 调用，如果玩家正在"格挡"且持剑则返回 BLOCK
         */
        @WrapOperation(
            method = RENDER_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;")
        )
        private UseAction blockAnimation$wrapGetUseAction(ItemStack stack, Operation<UseAction> original) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.player != null && BlockAnimationUtils.isEntityBlocking(mc.player)) {
                if (BlockAnimationUtils.isSword(stack.getItem())) {
                    return UseAction.BLOCK;
                }
            }
            return original.call(stack);
        }
    }
}
