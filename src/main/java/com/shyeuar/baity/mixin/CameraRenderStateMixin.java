package com.shyeuar.baity.mixin;

import com.shyeuar.baity.render.interfaces.CameraRenderStateInterface;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CameraRenderState.class)
public abstract class CameraRenderStateMixin implements CameraRenderStateInterface {
	@Unique
	private int baity$id = -1;
	
	@Unique
	private float baity$partialTickTime = 0.0F;
	
	@Unique
	private float baity$oldEyeHeight = 0.0F;
	
	@Unique
	private float baity$eyeHeight = 0.0F;

	@Unique
	private boolean baity$worldCamera = false;

	@Unique
	private int baity$nameTagEntityId = -1;

	@Override
	public boolean baity$isWorldCamera() {
		return this.baity$worldCamera;
	}

	@Override
	public void baity$setWorldCamera(boolean worldCamera) {
		this.baity$worldCamera = worldCamera;
	}
	
	@Override
	public int baity$getId() {
		return this.baity$id;
	}
	
	@Override
	public void baity$setId(int id) {
		this.baity$id = id;
	}
	
	@Override
	public float baity$getPartialTickTime() {
		return this.baity$partialTickTime;
	}
	
	@Override
	public void baity$setPartialTickTime(float partialTickTime) {
		this.baity$partialTickTime = partialTickTime;
	}
	
	@Override
	public float baity$getOldEyeHeight() {
		return this.baity$oldEyeHeight;
	}
	
	@Override
	public void baity$setOldEyeHeight(float oldEyeHeight) {
		this.baity$oldEyeHeight = oldEyeHeight;
	}
	
	@Override
	public float baity$getEyeHeight() {
		return this.baity$eyeHeight;
	}
	
	@Override
	public void baity$setEyeHeight(float eyeHeight) {
		this.baity$eyeHeight = eyeHeight;
	}

	@Override
	public int baity$getNameTagEntityId() {
		return this.baity$nameTagEntityId;
	}

	@Override
	public void baity$setNameTagEntityId(int entityId) {
		this.baity$nameTagEntityId = entityId;
	}
}
