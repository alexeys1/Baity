package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

public interface ItemStackRenderStateAccessor {
    @Mixin(ItemStackRenderState.class)
    interface ItemStackRenderStateAccessorImpl {
        @Invoker("firstLayer")
        ItemStackRenderState.LayerRenderState baity$callFirstLayer();
    }
    
    @Mixin(ItemStackRenderState.LayerRenderState.class)
    interface LayerRenderStateAccessor {
        @Accessor("itemTransform")
        ItemTransform baity$getTransform();
    }
}

