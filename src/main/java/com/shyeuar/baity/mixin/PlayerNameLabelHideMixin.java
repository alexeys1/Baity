package com.shyeuar.baity.mixin;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerEntityRenderer.class)
public class PlayerNameLabelHideMixin {

    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void baity$hideOriginalNameTag(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        Module m = ModuleManager.getModuleByName("PlayerESP");
        if (m == null || !m.isEnabled()) {
            return; 
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        
        if (state.playerName == null) return;
        String playerName = state.playerName.getString();
        
        PlayerEntity player = null;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p.getName().getString().equals(playerName)) {
                player = p;
                break;
            }
        }
        if (player == null) return;

        if (com.shyeuar.baity.utils.AntiBotUtils.isBot(player)) {
            ci.cancel();
            return;
        }

        boolean showOwnNametag = ModuleUtils.getOptionBoolean(m, "show own nametag", false);
        if (player == mc.player) {
            if (showOwnNametag) {
                ci.cancel();
                return;
            }
            // 如果未启用show own nametag，让原版标签正常渲染（原版行为）
            return;
        }

        ci.cancel();
    }

}
