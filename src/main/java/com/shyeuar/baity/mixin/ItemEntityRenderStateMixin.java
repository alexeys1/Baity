package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.droppeditem.ItemEntityRenderStateExtension;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemEntityRenderStateExtension {

    @Unique
    private ItemStack baity$itemStack = ItemStack.EMPTY;

    @Unique
    private float baity$dropScale = 1.0f;

    @Override
    public ItemStack baity$getItemStack() {
        return baity$itemStack;
    }

    @Override
    public void baity$setItemStack(ItemStack stack) {
        baity$itemStack = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public float baity$getDropScale() {
        return baity$dropScale;
    }

    @Override
    public void baity$setDropScale(float scale) {
        baity$dropScale = scale;
    }
}