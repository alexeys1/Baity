package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.Keybinds;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class KeybindsMixin {

    @Mixin(AbstractContainerScreen.class)
    public abstract static class ContainerScreenMixin {

        @Inject(
                method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
                at = @At("HEAD"),
                cancellable = true
        )
        private void baity$preventUnequipSlotClick(
                Slot slot,
                int slotId,
                int button,
                ContainerInput actionType,
                CallbackInfo ci
        ) {
            if (slot == null || !Keybinds.isUnequipBlockedAction(actionType)) {
                return;
            }
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            if (Keybinds.shouldBlockUnequipContainerInput(screen.getMenu().containerId, slot.index)) {
                ci.cancel();
            }
        }
    }

    @Mixin(MultiPlayerGameMode.class)
    public abstract static class GameModeMixin {

        @Inject(
                method = "handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V",
                at = @At("HEAD"),
                cancellable = true
        )
        private void baity$preventUnequipContainerInput(
                int containerId,
                int slotIndex,
                int mouseButton,
                ContainerInput actionType,
                Player player,
                CallbackInfo ci
        ) {
            if (!Keybinds.isUnequipBlockedAction(actionType)) {
                return;
            }
            if (Keybinds.shouldBlockUnequipContainerInput(containerId, slotIndex)) {
                ci.cancel();
            }
        }
    }
}
