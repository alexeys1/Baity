package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.enchantchroma.EnchantChromaConfig;
import com.shyeuar.baity.features.enchantchroma.EnchantChromaRenderer;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(BakedSheetGlyph.class)
public abstract class EnchantChromaMixin {

    @Shadow
    private float left;

    @Shadow
    private float right;

    @Shadow
    private float up;

    @Shadow
    private float down;

    @Shadow
    private float u0;

    @Shadow
    private float u1;

    @Shadow
    private float v0;

    @Shadow
    private float v1;

    @Shadow
    private float shearTop() {
        return 0.0f;
    }

    @Shadow
    private float shearBottom() {
        return 0.0f;
    }

    @Inject(
        method = "render(ZFFFLorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;IZI)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void baity$renderRainbow(boolean italic, float x, float y, float z, Matrix4f matrix, VertexConsumer vertexConsumer, int color, boolean bold, int light, CallbackInfo ci) {
        if (!EnchantChromaConfig.isEnabled()) {
            return;
        }

        if (color != EnchantChromaConfig.MARKER_COLOR && 
            color != EnchantChromaConfig.MARKER_COLOR_SHADOW) {
            return;
        }

        boolean shadowed = (color == EnchantChromaConfig.MARKER_COLOR_SHADOW);

        float k = x + this.left;
        float l = x + this.right;
        float m = y + this.up;
        float n = y + this.down;
        float o = italic ? this.shearTop() : 0.0F;
        float p = italic ? this.shearBottom() : 0.0F;
        float q = bold ? 0.1F : 0.0F;

        Vector2f topLeft = new Vector2f(k + o - q, m - q);
        Vector2f bottomLeft = new Vector2f(k + p - q, n + q);
        Vector2f bottomRight = new Vector2f(l + p + q, n + q);
        Vector2f topRight = new Vector2f(l + o + q, m - q);

        int[] colors = EnchantChromaRenderer.computeGradient(topLeft, bottomLeft, bottomRight, topRight, shadowed);

        vertexConsumer.addVertex(matrix, topLeft.x, topLeft.y, z).setColor(colors[0]).setUv(this.u0, this.v0).setLight(light);
        vertexConsumer.addVertex(matrix, bottomLeft.x, bottomLeft.y, z).setColor(colors[1]).setUv(this.u0, this.v1).setLight(light);
        vertexConsumer.addVertex(matrix, bottomRight.x, bottomRight.y, z).setColor(colors[2]).setUv(this.u1, this.v1).setLight(light);
        vertexConsumer.addVertex(matrix, topRight.x, topRight.y, z).setColor(colors[3]).setUv(this.u1, this.v0).setLight(light);

        ci.cancel();
    }
}
