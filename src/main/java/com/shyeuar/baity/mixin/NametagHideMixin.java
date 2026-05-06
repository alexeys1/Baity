package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class NametagHideMixin {
    @Inject(method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void baity$hideOriginalNameTag(AvatarRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
        Module m = ModuleManager.getModuleByName("Nametag");
        if (m == null || !m.isEnabled()) {
            return; 
        }

        boolean defaultNametag = ModuleUtils.getOptionBoolean(m, "default nametag", false);
        if (defaultNametag) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        boolean isSelf = mc.player.getId() == state.id;
        net.minecraft.world.entity.Entity entity = mc.level.getEntity(state.id);
        if (entity instanceof Player player) {
            if (com.shyeuar.baity.utils.AntiBotUtils.isBot(player)) {
                ci.cancel();
                return;
            }
        }

        boolean showOwnNametag = ModuleUtils.getOptionBoolean(m, "show own nametag", false);
        if (isSelf) {
            if (showOwnNametag) {
                ci.cancel();  
            }
            return;
        }

        ci.cancel();
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void baity$disableVanillaNameTag(Avatar avatar, double d, CallbackInfoReturnable<Boolean> cir) {
        Module m = ModuleManager.getModuleByName("Nametag");
        if (m == null || !m.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean defaultNametag = ModuleUtils.getOptionBoolean(m, "default nametag", false);
        if (defaultNametag) {
            if (avatar == mc.player) cir.setReturnValue(true);
            return;
        }

        if (avatar == mc.player) {
            boolean showOwnNametag = ModuleUtils.getOptionBoolean(m, "show own nametag", false);
            if (showOwnNametag) {
                cir.setReturnValue(false);
            }
            return;
        }

        cir.setReturnValue(false);
    }

    @Redirect(
        method = "shouldShowName",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Avatar;isInvisibleTo(Lnet/minecraft/world/entity/player/Player;)Z"
        ),
        require = 0
    )
    private boolean baity$ignoreInvisibilityForDefaultNametag(Avatar avatar, Player viewer) {
        Module m = ModuleManager.getModuleByName("Nametag");
        if (m == null || !m.isEnabled()) return avatar.isInvisibleTo(viewer);
        boolean defaultNametag = ModuleUtils.getOptionBoolean(m, "default nametag", false);
        if (!defaultNametag) return avatar.isInvisibleTo(viewer);
        return false;
    }

}
