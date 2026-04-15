package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("eyeHeight")
    float baity$getEyeHeight();
    
    @Accessor("eyeHeightOld")
    float baity$getOldEyeHeight();

    @Accessor("position")
    Vec3 baity$getPosition();

    @Accessor("entity")
    Entity baity$getEntity();
}
