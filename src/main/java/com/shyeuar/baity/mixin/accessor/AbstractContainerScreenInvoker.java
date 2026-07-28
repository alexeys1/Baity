package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenInvoker {
    @Invoker("extractSlot")
    void baity$invokeExtractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY);
}
