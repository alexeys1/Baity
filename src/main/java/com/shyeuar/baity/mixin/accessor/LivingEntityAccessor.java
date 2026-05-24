package com.shyeuar.baity.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("getCurrentSwingDuration")
    int baity$getCurrentSwingDuration();

    @Accessor("swimAmount")
    void baity$setSwimAmountField(float swimAmount);

    @Accessor("swimAmountO")
    void baity$setSwimAmountOField(float swimAmountO);
}

