package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.keybinds.ContainerGuiUtils;
import com.shyeuar.baity.features.keybinds.Keybinds;
import com.shyeuar.baity.features.keybinds.KeybindsMenuType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class KeybindsSlotClickMixin {
    @Shadow
    public abstract net.minecraft.network.chat.Component getTitle();

    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void baity$preventUnequip(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        if (slot == null || actionType != ContainerInput.PICKUP) {
            return;
        }
        KeybindsMenuType menuType = KeybindsMenuType.fromTitle(getTitle());
        if (menuType != KeybindsMenuType.WARDROBE && menuType != KeybindsMenuType.EQUIPMENT) {
            return;
        }
        ItemStack stack = slot.getItem();
        if (!ContainerGuiUtils.isEquippedSetButton(stack)) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        long windowHandle = client.getWindow().handle();
        if (Keybinds.shouldPreventUnequip(menuType, stack, windowHandle)) {
            ci.cancel();
        }
    }
}
