package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.shyeuar.baity.mixin.accessor.CameraAccessor;
import com.shyeuar.baity.mixin.accessor.CameraRenderStateAccessor;
import com.shyeuar.baity.utils.NoSwimPoseUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	private Camera mainCamera;
	
	@Inject(method = "extractCamera", at = @At("TAIL"))
	private void baity$populateCameraRenderState(CallbackInfo ci, @Local CameraRenderState cameraRenderState) {
		CameraRenderStateAccessor cameraAccessor = (CameraRenderStateAccessor) cameraRenderState;
		cameraAccessor.baity$setId(this.mainCamera.entity().getId());
		cameraAccessor.baity$setPartialTickTime(this.mainCamera.getPartialTickTime());
		cameraAccessor.baity$setOldEyeHeight(((CameraAccessor) this.mainCamera).baity$getOldEyeHeight());
		cameraAccessor.baity$setEyeHeight(((CameraAccessor) this.mainCamera).baity$getEyeHeight());
		cameraAccessor.baity$setWorldCamera(true);

		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null
			&& this.mainCamera.entity() == mc.player
			&& NoSwimPoseUtils.shouldApplyEyeHeightChange()) {
			float standingEyeHeight = NoSwimPoseUtils.STANDING_EYE_HEIGHT;
			cameraAccessor.baity$setEyeHeight(standingEyeHeight);
			cameraAccessor.baity$setOldEyeHeight(standingEyeHeight);
		}
	}

	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void baity$beginWorldRenderPhase(CallbackInfo ci) {
		com.shyeuar.baity.render.RenderScope.enterWorldRenderPhase();
	}

	@Inject(method = "renderLevel", at = @At("RETURN"))
	private void baity$endWorldRenderPhase(CallbackInfo ci) {
		com.shyeuar.baity.render.RenderScope.exitWorldRenderPhase();
	}
}
