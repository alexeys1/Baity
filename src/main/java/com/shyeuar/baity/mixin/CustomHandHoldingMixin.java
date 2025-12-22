package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class CustomHandHoldingMixin {

    @Mixin(LivingEntity.class)
    public static abstract class SwingDurationMixin {

        @Inject(method = "getHandSwingDuration", at = @At("HEAD"), cancellable = true)
        private void baity$customSwingDuration(CallbackInfoReturnable<Integer> cir) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            LivingEntity self = (LivingEntity) (Object) this;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || self != mc.player) return;

            int duration = (int) ConfigManager.swingDuration;
            cir.setReturnValue(duration);
        }
    }

    @Mixin(PlayerEntity.class)
    public static abstract class AttackCooldownMixin {

        @Inject(method = "getAttackCooldownProgress", at = @At("HEAD"), cancellable = true)
        private void baity$removeAttackCooldownAnimation(float tickDelta, CallbackInfoReturnable<Float> cir) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            PlayerEntity self = (PlayerEntity) (Object) this;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || self != mc.player) return;

            cir.setReturnValue(1.0f);
        }
    }

    @Mixin(HeldItemRenderer.class)
    public static abstract class HeldItemOffsetMixin {

        @Unique
        private static final float OFFSET_X = 1.0f;
        @Unique
        private static final float OFFSET_Y = -0.4f;
        @Unique
        private static final float OFFSET_Z = -1.0f;

        private static final String RENDER_METHOD = "renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V";

        @Inject(method = RENDER_METHOD, at = @At("HEAD"))
        private void baity$adjustHeldItemOffset(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
            Module customHandHoldingModule = ModuleManager.getModuleByName("CustomHandHolding");
            if (customHandHoldingModule == null || !customHandHoldingModule.isEnabled()) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || player != mc.player) return;
            if (item.isEmpty()) return;

            int handSide = (hand == Hand.MAIN_HAND) == (player.getMainArm() == Arm.RIGHT) ? 1 : -1;
            matrices.translate(OFFSET_X * handSide, OFFSET_Y, OFFSET_Z);
        }
    }
}
