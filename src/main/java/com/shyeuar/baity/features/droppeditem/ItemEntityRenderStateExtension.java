package com.shyeuar.baity.features.droppeditem;

import net.minecraft.world.item.ItemStack;

public interface ItemEntityRenderStateExtension {
    ItemStack baity$getItemStack();

    void baity$setItemStack(ItemStack stack);

    float baity$getDropScale();

    void baity$setDropScale(float scale);
}