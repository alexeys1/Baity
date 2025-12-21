package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.BlockAnimationUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    
    /**
     * 第三人称防砍动画 - 修改玩家模型手臂姿势
     * 当玩家在第三人称视角下触发防砍时，让手臂呈现格挡姿势
     */
    @Mixin(PlayerEntityModel.class)
    public static abstract class ThirdPersonBlockAnimationMixin {
        
        @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V", at = @At("TAIL"))
        private void baity$applyBlockingPose(PlayerEntityRenderState state, CallbackInfo ci) {
            MinecraftClient mc = MinecraftClient.getInstance();
            
            // 只对本地玩家生效
            if (mc.player == null || state.id != mc.player.getId()) return;
            
            // 检测是否正在防砍
            if (!BlockAnimationUtils.isEntityBlocking(mc.player)) return;
            
            PlayerEntityModel model = (PlayerEntityModel) (Object) this;
            Hand blockingHand = BlockAnimationUtils.getBlockingHand(mc.player);
            
            // 计算行走时的手臂摆动（1/8 原版速度）
            float armSwing = 0f;
            if (state.limbSwingAmplitude > 0) {
                float limbAngle = state.limbSwingAnimationProgress;
                // 原版摆动公式，乘以 0.125 得到 1/8 速度
                armSwing = (float) (Math.cos(limbAngle * 0.6662f + Math.PI) * 1.4f * state.limbSwingAmplitude * 0.125f);
            }
            
            if (blockingHand == Hand.MAIN_HAND) {
                // 主手持剑防砍
                if (mc.player.getMainArm() == net.minecraft.util.Arm.RIGHT) {
                    applyBlockingPoseToArm(model.rightArm, armSwing, true);
                } else {
                    applyBlockingPoseToArm(model.leftArm, armSwing, false);
                }
            } else if (blockingHand == Hand.OFF_HAND) {
                // 副手持剑防砍
                if (mc.player.getMainArm() == net.minecraft.util.Arm.RIGHT) {
                    applyBlockingPoseToArm(model.leftArm, armSwing, false);
                } else {
                    applyBlockingPoseToArm(model.rightArm, armSwing, true);
                }
            }
        }
        
        /**
         * 应用格挡姿势到指定手臂
         * @param arm 手臂模型部件
         * @param armSwing 行走摆动值
         * @param isRightArm 是否是右臂（用于镜像yaw）
         */
        private void applyBlockingPoseToArm(net.minecraft.client.model.ModelPart arm, float armSwing, boolean isRightArm) {
            // pitch: 负值 = 手臂向前抬起
            arm.pitch = -0.9F + armSwing;
            // yaw: 右臂负值向内收，左臂正值向内收
            arm.yaw = isRightArm ? -0.5F : 0.5F;
            // roll: 不翻转
            arm.roll = 0.0F;
        }
    }
}
