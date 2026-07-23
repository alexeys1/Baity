package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AvatarRenderer.class)
public interface AvatarRendererInvoker {
    @Invoker("shouldShowName")
    boolean baity$invokeShouldShowName(Avatar avatar, double distanceSquared);
}
