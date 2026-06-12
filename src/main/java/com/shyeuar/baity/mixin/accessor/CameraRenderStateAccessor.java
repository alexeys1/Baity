package com.shyeuar.baity.mixin.accessor;

import com.shyeuar.baity.render.interfaces.CameraRenderStateInterface;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CameraRenderState.class)
public interface CameraRenderStateAccessor extends CameraRenderStateInterface {
}
