package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class KeybindsSlotClickMixin {

    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void baity$preventUnequip(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        if (slot == null || actionType != ContainerInput.PICKUP) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        Keybinds.MenuType menuType = Keybinds.MenuType.fromTitle(screen.getTitle());
        if (menuType != Keybinds.MenuType.WARDROBE && menuType != Keybinds.MenuType.EQUIPMENT) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (!Keybinds.isEquippedSetButton(stack)) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        long windowHandle = client.getWindow().handle();
        if (Keybinds.shouldPreventUnequip(menuType, stack, windowHandle)) {
            ci.cancel();
        }
    }
}
