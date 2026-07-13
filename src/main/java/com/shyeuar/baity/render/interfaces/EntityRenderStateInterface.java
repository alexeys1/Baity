package com.shyeuar.baity.render.interfaces;

import net.minecraft.world.entity.EntityDimensions;

public interface EntityRenderStateInterface {

	default EntityDimensions baity$getStandingDimensions() {
		return null;
	}

	default void baity$setStandingDimensions(EntityDimensions dimensions) {
	}

	default boolean baity$isWorldCameraContext() {
		return true;
	}

	default void baity$setWorldCameraContext(boolean worldCameraContext) {
	}

	default boolean baity$shouldHidePaperDollArmor() {
		return false;
	}

	default void baity$setHidePaperDollArmor(boolean hidePaperDollArmor) {
	}
}
