package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class DroppedItem2DMixin {

    @Shadow private RandomSource random;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void baity$makeDroppedItem2D(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState, CallbackInfo ci) {
        if (!isModuleEnabled()) return;
        if (state.item.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        net.minecraft.client.Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null) return;
        poseStack.pushPose();
        AABB box = state.item.getModelBoundingBox();
        float baseOffset = -((float) box.minY) + 0.0625F;
        float bob = (float) Math.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
        poseStack.translate(0.0F, bob + baseOffset, 0.0F);
        float yaw = camera.yRot();
        float pitch = camera.xRot();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.scale(1.0F, 1.0F, 0.01F);
        ItemEntityRenderer.submitMultipleFromCount(poseStack, submitNodeCollector, state.lightCoords, state, this.random, box);
        poseStack.popPose();
        ci.cancel();
    }

    private static boolean isModuleEnabled() {
        Module module = ModuleManager.getModuleByName("2DdroppedItem");
        return module != null && module.isEnabled();
    }
}

