package com.shyeuar.baity.mixin.accessor;

import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(FogRenderer.class)
public interface FogRendererAccessor {
    
    @Accessor("FOG_ENVIRONMENTS")
    static List<FogEnvironment> baity$getFogEnvironments() {
        throw new AssertionError("Mixin accessor");
    }
}
