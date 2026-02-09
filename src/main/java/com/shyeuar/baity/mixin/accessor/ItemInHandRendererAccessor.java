package com.shyeuar.baity.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Invoker("applyItemArmAttackTransform")
    void baity$callApplyItemArmAttackTransform(PoseStack poseStack, HumanoidArm hand, float swingProgress);
    
    @Invoker("applyItemArmTransform")
    void baity$callApplyItemArmTransform(PoseStack poseStack, HumanoidArm hand, float equippedProgress);
    
    @Accessor("mainHandHeight")
    float baity$getMainHandHeight();
    
    @Accessor("mainHandHeight")
    void baity$setMainHandHeight(float height);
    
    @Accessor("offHandHeight")
    float baity$getOffHandHeight();
    
    @Accessor("offHandHeight")
    void baity$setOffHandHeight(float height);
    
    @Accessor("mainHandItem")
    net.minecraft.world.item.ItemStack baity$getMainHandItem();
    
    @Accessor("mainHandItem")
    void baity$setMainHandItem(net.minecraft.world.item.ItemStack item);
    
    @Accessor("offHandItem")
    net.minecraft.world.item.ItemStack baity$getOffHandItem();
    
    @Accessor("offHandItem")
    void baity$setOffHandItem(net.minecraft.world.item.ItemStack item);
}