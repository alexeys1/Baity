package com.shyeuar.baity.features.blockanimation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Quaternionf;

public final class BlockAnimationRenderer {
    private BlockAnimationRenderer() {}

    public enum RenderResult {
        PASS,
        INTERRUPT
    }
 
    public static RenderResult renderFirstPerson(ItemInHandRenderer itemInHandRenderer, 
            InteractionHand interactionHand, 
            AbstractClientPlayer player, 
            HumanoidArm humanoidArm, 
            net.minecraft.world.item.ItemStack itemStack, 
            PoseStack poseStack, 
            SubmitNodeCollector submitNodeCollector, 
            int combinedLight, 
            float partialTick, 
            float interpolatedPitch, 
            float swingProgress, 
            float equipProgress) {
        
        if (!BlockAnimationUtils.isFeatureActive()) return RenderResult.PASS;
        if (!BlockAnimationUtils.isPlayerBlockingWithSword(player)) return RenderResult.PASS;
        
        InteractionHand blockingHand = BlockAnimationUtils.getBlockingHand(player);
        if (blockingHand != interactionHand) return RenderResult.PASS;
        
        if (itemStack.isEmpty() || itemStack.has(net.minecraft.core.component.DataComponents.MAP_ID)) {
            return RenderResult.PASS; 
        }
        
        if (itemStack.is(net.minecraft.world.item.Items.CROSSBOW)) {
            return RenderResult.PASS;
        }
        
        poseStack.pushPose();
        
        boolean mainHand = interactionHand == InteractionHand.MAIN_HAND;
        HumanoidArm handSide = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean isHandSideRight = handSide == HumanoidArm.RIGHT;
        
        com.shyeuar.baity.mixin.accessor.ItemInHandRendererAccessor accessor = 
                (com.shyeuar.baity.mixin.accessor.ItemInHandRendererAccessor) itemInHandRenderer;
        accessor.baity$callApplyItemArmTransform(poseStack, handSide, equipProgress);
        
        com.shyeuar.baity.gui.module.Module customHandHoldingModule = 
                com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("CustomHandHolding");
        if (customHandHoldingModule != null && customHandHoldingModule.isEnabled()) {
            com.shyeuar.baity.features.customhandholding.CustomHandHoldingManager.getInstance()
                    .applyPositionAndRotation(poseStack, handSide);
        }
        
        float actualSwingProgress = swingProgress;
        if (com.shyeuar.baity.features.blockanimation.BlockAnimationManager.isSwinging() 
                && com.shyeuar.baity.features.blockanimation.BlockAnimationManager.getSwingHand() == interactionHand) {
            actualSwingProgress = com.shyeuar.baity.features.blockanimation.BlockAnimationManager.getSwingProgress(partialTick);
        }
        
        if (BlockAnimationUtils.isInteractAnimationsEnabled()) {
            accessor.baity$callApplyItemArmAttackTransform(poseStack, handSide, actualSwingProgress);
        }
        
        applyFirstPersonBlockTransform(poseStack, handSide);
       
        if (customHandHoldingModule != null && customHandHoldingModule.isEnabled()) {
            com.shyeuar.baity.features.customhandholding.CustomHandHoldingManager.getInstance()
                    .applyScale(poseStack);
        }
        
        itemInHandRenderer.renderItem(player,
                itemStack,
                isHandSideRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND :
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                submitNodeCollector,
                combinedLight);
        
        poseStack.popPose();
        
        return RenderResult.INTERRUPT;
    }
    
    private static void applyFirstPersonBlockTransform(PoseStack matrixStack, HumanoidArm hand) {
        applyBlockingTransformOnly(matrixStack, hand);
    }

    public static void applyBlockingTransformOnly(PoseStack matrixStack, HumanoidArm hand) {
        int direction = hand == HumanoidArm.RIGHT ? 1 : -1;
        matrixStack.translate(direction * -0.14142136F, 0.08F, 0.14142136F);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
        matrixStack.mulPose(Axis.YP.rotationDegrees(direction * 13.365F));
        matrixStack.mulPose(Axis.ZP.rotationDegrees(direction * 78.05F));
    }
   
    public static void renderThirdPerson(AvatarRenderState renderState, 
            ArmedModel<AvatarRenderState> model, 
            ItemStackRenderState itemStackRenderState, 
            HumanoidArm humanoidArm, 
            PoseStack poseStack, 
            SubmitNodeCollector submitNodeCollector, 
            int packedLight) {
       
        poseStack.pushPose();
        ArmedModel<AvatarRenderState> avatarModel = (ArmedModel<AvatarRenderState>) model;
        avatarModel.translateToHand(renderState, humanoidArm, poseStack);
        boolean leftHand = humanoidArm == HumanoidArm.LEFT;
        applyThirdPersonBlockTransform(poseStack, leftHand);
     
        com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.ItemStackRenderStateAccessorImpl stateAccessor = 
                (com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.ItemStackRenderStateAccessorImpl) (Object) itemStackRenderState;
        ItemStackRenderState.LayerRenderState firstLayer = stateAccessor.baity$callFirstLayer();
        if (firstLayer != null) {
            com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.LayerRenderStateAccessor layerAccessor = 
                    (com.shyeuar.baity.mixin.accessor.ItemStackRenderStateAccessor.LayerRenderStateAccessor) (Object) firstLayer;
            revertItemTransform(layerAccessor.baity$getTransform(), leftHand, poseStack);
        }
        itemStackRenderState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
    
    private static void applyThirdPersonBlockTransform(PoseStack poseStack, boolean leftHand) {
        poseStack.translate((leftHand ? 1.0F : -1.0F) / 16.0F, 0.4375F, 0.0625F);
        poseStack.translate(leftHand ? -0.035F : 0.05F, leftHand ? 0.045F : 0.0F, leftHand ? -0.135F : -0.1F);
        poseStack.mulPose(Axis.YP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -50.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees((leftHand ? -1.0F : 1.0F) * -60.0F));
        poseStack.translate(0.0F, 0.1875F, 0.0F);
       
        poseStack.scale(0.625F, 0.625F, 0.625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XN.rotationDegrees(-100.0F));
        poseStack.mulPose(Axis.YN.rotationDegrees(leftHand ? 35.0F : 45.0F));
        poseStack.translate(0.0F, -0.3F, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(50.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
        poseStack.translate(-0.9375F, -0.0625F, 0.0F);
        poseStack.translate(0.5F, 0.5F, 0.25F);
        poseStack.mulPose(Axis.YN.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, 0.28125F);
    }
    
    private static void revertItemTransform(ItemTransform itemTransform, boolean leftHand, PoseStack poseStack) {
        if (itemTransform != ItemTransform.NO_TRANSFORM) {
            float angleX = itemTransform.rotation().x();
            float angleY = leftHand ? -itemTransform.rotation().y() : itemTransform.rotation().y();
            float angleZ = leftHand ? -itemTransform.rotation().z() : itemTransform.rotation().z();
            Quaternionf quaternion = new Quaternionf().rotationXYZ(angleX * 0.017453292F,
                    angleY * 0.017453292F,
                    angleZ * 0.017453292F);
            quaternion.conjugate();
            poseStack.scale(1.0F / itemTransform.scale().x(),
                    1.0F / itemTransform.scale().y(),
                    1.0F / itemTransform.scale().z());
            poseStack.mulPose(quaternion);
            poseStack.translate((leftHand ? -1.0F : 1.0F) * -itemTransform.translation().x(),
                    -itemTransform.translation().y(),
                    -itemTransform.translation().z());
        }
    }
}

