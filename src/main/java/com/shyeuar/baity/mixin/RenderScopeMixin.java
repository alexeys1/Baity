package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shyeuar.baity.mixin.accessor.CameraRenderStateAccessor;
import com.shyeuar.baity.render.RenderScope;
import com.shyeuar.baity.render.interfaces.EntityRenderStateInterface;
import com.shyeuar.baity.utils.NoSwimPoseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class RenderScopeMixin {

	@Mixin(LivingEntityRenderState.class)
	public static class LivingEntityRenderStateMixin implements EntityRenderStateInterface {
		@Unique
		private boolean baity$worldCameraContext = true;
	@Unique
	private boolean baity$hidePaperDollArmor = false;
	@Unique
	private int baity$entityId = -1;

		@Override
		public boolean baity$isWorldCameraContext() {
			return this.baity$worldCameraContext;
		}

		@Override
		public void baity$setWorldCameraContext(boolean worldCameraContext) {
			this.baity$worldCameraContext = worldCameraContext;
		}

		@Override
		public boolean baity$shouldHidePaperDollArmor() {
			return this.baity$hidePaperDollArmor;
		}

		@Override
		public void baity$setHidePaperDollArmor(boolean hidePaperDollArmor) {
			this.baity$hidePaperDollArmor = hidePaperDollArmor;
		}

		@Override
		public int baity$getEntityId() {
			return this.baity$entityId;
		}

		@Override
		public void baity$setEntityId(int entityId) {
			this.baity$entityId = entityId;
		}
	}

	@Mixin(LivingEntityRenderer.class)
	public static class LivingEntityRendererScopeMixin {

		@Inject(
			method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("HEAD")
		)
		private void baity$beginRenderScope(
			LivingEntityRenderState livingEntityRenderState,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			CameraRenderState cameraRenderState,
			CallbackInfo ci
		) {
			if (cameraRenderState instanceof CameraRenderStateAccessor cameraAccessor && !RenderScope.isWorldRenderPhase()) {
				cameraAccessor.baity$setWorldCamera(false);
			}
			boolean worldContext = RenderScope.isWorldEntityRender(cameraRenderState);
			if (livingEntityRenderState instanceof EntityRenderStateInterface context) {
				context.baity$setWorldCameraContext(worldContext);
			}
			if (livingEntityRenderState instanceof AvatarRenderState avatarState) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null && NoSwimPoseUtils.isSelfPlayerById(avatarState.id)) {
					if (worldContext && NoSwimPoseUtils.shouldForceStandingModelAppearance(avatarState)) {
						NoSwimPoseUtils.clearSwimRenderState(avatarState);
					} else if (!worldContext) {
						if (RenderScope.isPaperDollRender()) {
							if (NoSwimPoseUtils.isFeatureActive()) {
								NoSwimPoseUtils.clearSwimRenderState(avatarState);
							}
						} else {
							NoSwimPoseUtils.restoreSwimRenderStateFromEntity(avatarState, mc.player);
						}
					}
				}
			}
			RenderScope.enter(cameraRenderState);
		}

		@Inject(
			method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
			at = @At("RETURN")
		)
		private void baity$endRenderScope(
			LivingEntityRenderState livingEntityRenderState,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			CameraRenderState cameraRenderState,
			CallbackInfo ci
		) {
			RenderScope.exit();
		}
	}
}
