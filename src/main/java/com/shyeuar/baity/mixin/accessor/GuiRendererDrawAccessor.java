package com.shyeuar.baity.mixin.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.render.GuiRenderer$Draw")
public interface GuiRendererDrawAccessor {

    @Accessor("pipeline")
    RenderPipeline baity$pipeline();

    @Accessor("indexCount")
    int baity$indexCount();
}
