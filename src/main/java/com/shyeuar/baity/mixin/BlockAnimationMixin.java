package com.shyeuar.baity.mixin;

import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public abstract class BlockAnimationMixin {

    @Mixin(value = ItemInHandRenderer.class, priority = 500)
    public static abstract class ItemInHandRendererMixin {
        @Shadow
        @Final
        private Minecraft minecraft;

        @Invoker("applyItemArmAttackTransform")
        public abstract void baity$callApplyItemArmAttackTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, HumanoidArm hand, float swingProgress);

        @Invoker("applyItemArmTransform")
        public abstract void baity$callApplyItemArmTransform(com.mojang.blaze3d.vertex.PoseStack poseStack, HumanoidArm hand, float equippedProgress);

        @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
        private void baity$itemUsed(InteractionHand interactionHand, CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (!BlockAnimationUtils.isNoReequipWhenUsingEnabled()) return;

            if (this.minecraft.player != null && this.minecraft.player.isUsingItem() 
                    && this.minecraft.player.getUsedItemHand() == interactionHand) {
                if (BlockAnimationUtils.isPlayerBlockingWithSword(this.minecraft.player)) {
                    callback.cancel();
                }
            }
        }

        @WrapOperation(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z")
        )
        private boolean baity$wrapIsUsingItem(net.minecraft.client.player.AbstractClientPlayer player, Operation<Boolean> original) {
            if (player != null && BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                return true;
            }
            return original.call(player);
        }

        @WrapOperation(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I")
        )
        private int baity$wrapGetItemUseTimeLeft(net.minecraft.client.player.AbstractClientPlayer player, Operation<Integer> original) {
            if (player != null && BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                return BlockAnimationUtils.DEFAULT_ITEM_USE_DURATION;
            }
            return original.call(player);
        }

        @WrapOperation(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;")
        )
        private InteractionHand baity$wrapGetActiveHand(net.minecraft.client.player.AbstractClientPlayer player, Operation<InteractionHand> original) {
            if (player != null && BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(player);
                if (blockingHand != null) {
                    return blockingHand;
                }
            }
            return original.call(player);
        }

        @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
        private void baity$renderArmWithItem(net.minecraft.client.player.AbstractClientPlayer player, float partialTicks, float pitch, 
                InteractionHand interactionHand, float swingProgress, net.minecraft.world.item.ItemStack stack, float equippedProgress, 
                com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector, 
                int combinedLight, CallbackInfo callback) {
            
            ItemInHandRenderer itemInHandRenderer = (ItemInHandRenderer) (Object) this;
            com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.RenderResult result = 
                    com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.renderFirstPerson(
                            itemInHandRenderer,
                            interactionHand,
                            player,
                            interactionHand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite(),
                            stack,
                            poseStack,
                            submitNodeCollector,
                            combinedLight,
                            partialTicks,
                            pitch,
                            swingProgress,
                            equippedProgress
                    );
            
            if (result == com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.RenderResult.INTERRUPT) {
                callback.cancel();
            }
        }
    }

    @Mixin(net.minecraft.world.entity.player.Player.class)
    public static abstract class PlayerMixin {
        @Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)
        private void baity$removeAttackCooldownAnimationWhileBlocking(float tickDelta, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
            if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            if (!BlockAnimationUtils.isFeatureActive()) return;
            
            net.minecraft.world.entity.player.Player self = (net.minecraft.world.entity.player.Player) (Object) this;
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(self)) return;

            cir.setReturnValue(1.0f);
        }
    }

    @Mixin(HumanoidModel.class)
    public static abstract class HumanoidModelMixin<T extends HumanoidRenderState> extends net.minecraft.client.model.EntityModel<T> {
        @Shadow
        public ModelPart rightArm;
        @Shadow
        public ModelPart leftArm;

        protected HumanoidModelMixin(ModelPart root) {
            super(root);
        }

        @Inject(method = "setupAnim",
                at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/client/model/HumanoidModel;setupAttackAnimation(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;F)V"))
        private void baity$setupAnim(T renderState, CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (!(renderState instanceof AvatarRenderState)) return;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            AvatarRenderState avatarState = (AvatarRenderState) renderState;
            if (avatarState.id != mc.player.getId()) return;
            
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;

            InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(mc.player);
            if (blockingHand == null) return;
            
            InteractionHand interactionHand =
                    renderState.mainArm == HumanoidArm.RIGHT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            
            if (blockingHand == interactionHand) {
                this.rightArm.xRot = this.rightArm.xRot - Mth.PI * 2.0F / 10.0F;
            } else {
                this.leftArm.xRot = this.leftArm.xRot - Mth.PI * 2.0F / 10.0F;
            }
        }
    }

    @Mixin(Minecraft.class)
    public static abstract class MinecraftMixin {
        @Shadow
        @Final
        private static org.slf4j.Logger LOGGER;
        @Shadow
        @Final
        public net.minecraft.client.Options options;
        @Shadow
        public net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode;
        @Shadow
        public net.minecraft.client.multiplayer.ClientLevel level;
        @Shadow
        public net.minecraft.client.player.LocalPlayer player;
        @Shadow
        protected int missTime;
        @Shadow
        public net.minecraft.world.phys.HitResult hitResult;

        @Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
        private void baity$handleKeybinds(CallbackInfo callback) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (this.player == null || !this.player.isUsingItem()) return;
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(this.player)) return;

            while (this.options.keyAttack.consumeClick()) {
                this.baity$startBlockAttack();
            }
        }

        @Unique
        private void baity$startBlockAttack() {
            if (this.missTime <= 0) {
                if (this.hitResult == null) {
                    LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
                    if (this.gameMode.hasMissTime()) {
                        this.missTime = 10;
                    }
                } else if (this.player.getItemInHand(InteractionHand.MAIN_HAND).isItemEnabled(this.level.enabledFeatures()) 
                        && !this.player.isHandsBusy()) {
                    if (this.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        net.minecraft.world.phys.BlockHitResult blockhitresult = (net.minecraft.world.phys.BlockHitResult) this.hitResult;
                        net.minecraft.core.BlockPos blockpos = blockhitresult.getBlockPos();
                        if (!this.level.isEmptyBlock(blockpos)) {
                            com.shyeuar.baity.features.blockanimation.BlockAnimationManager.startBreaking(blockpos, blockhitresult.getDirection());
                            return;
                        }
 
                        baity$triggerClientSwing();
                    }
                }
            }
        }
        
        @Unique
        private void baity$triggerClientSwing() {
            com.shyeuar.baity.features.blockanimation.BlockAnimationManager.startSwing(InteractionHand.MAIN_HAND);
        }

        @Redirect(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
        private boolean baity$continueAttack(net.minecraft.client.player.LocalPlayer player) {
            if (!BlockAnimationUtils.isFeatureActive()) return player.isUsingItem();
            if (BlockAnimationUtils.isPlayerBlockingWithSword(player)) {
                return false;
            }
            return player.isUsingItem();
        }

        @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
        private boolean baity$startUseItem(net.minecraft.client.multiplayer.MultiPlayerGameMode gameMode) {
            if (!BlockAnimationUtils.isFeatureActive()) return gameMode.isDestroying();
            if (this.player != null && BlockAnimationUtils.isPlayerBlockingWithSword(this.player)) {
                return false; 
            }
            return gameMode.isDestroying();
        }
    }
    
    @Mixin(net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer.class)
    public static abstract class PlayerItemInHandLayerMixin<S extends AvatarRenderState, M extends EntityModel<S> & ArmedModel<S>> extends ItemInHandLayer<S, M> {
        
        public PlayerItemInHandLayerMixin(net.minecraft.client.renderer.entity.RenderLayerParent<S, M> renderLayerParent) {
            super(renderLayerParent);
        }
        
        @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
        private void baity$submitArmWithItem(S renderState,
                net.minecraft.client.renderer.item.ItemStackRenderState itemStackRenderState,
                HumanoidArm humanoidArm,
                com.mojang.blaze3d.vertex.PoseStack poseStack,
                net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
                int packedLight,
                CallbackInfo callback) {
            
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (itemStackRenderState.isEmpty()) return;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            
            if (renderState.id != mc.player.getId()) return;
            
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;
            
            InteractionHand interactionHand =
                    humanoidArm == renderState.mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(mc.player);
            if (blockingHand != interactionHand) return;
            
            
            @SuppressWarnings("unchecked")
            ArmedModel<AvatarRenderState> model = 
                    (ArmedModel<AvatarRenderState>) this.getParentModel();
            
            com.shyeuar.baity.features.blockanimation.BlockAnimationRenderer.renderThirdPerson(
                    (AvatarRenderState) renderState,
                    model,
                    itemStackRenderState,
                    humanoidArm,
                    poseStack,
                    submitNodeCollector,
                    packedLight);
            
            callback.cancel();
        }
    }
    
    @Mixin(net.minecraft.client.renderer.LevelRenderer.class)
    public static abstract class LevelRendererMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void baity$injectClientBreakingProgress(net.minecraft.client.Camera camera, CallbackInfo ci) {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;
            
            net.minecraft.core.BlockPos breakingPos = com.shyeuar.baity.features.blockanimation.BlockAnimationManager.getCurrentBreakingPos();
            if (breakingPos != null) {
                int progress = com.shyeuar.baity.features.blockanimation.BlockAnimationManager.getBreakingProgress(breakingPos);
                if (progress >= 0 && progress < 10) {
                    int playerId = mc.player.getId();
                    ((net.minecraft.client.renderer.LevelRenderer)(Object)this).destroyBlockProgress(playerId, breakingPos, progress);
                } else if (progress >= 10) {
                    int playerId = mc.player.getId();
                    ((net.minecraft.client.renderer.LevelRenderer)(Object)this).destroyBlockProgress(playerId, breakingPos, -1);
                }
            }
        }
    }
}