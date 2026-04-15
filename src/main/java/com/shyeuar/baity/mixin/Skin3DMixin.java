package com.shyeuar.baity.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shyeuar.baity.features.Skin3DRenderer;
import com.shyeuar.baity.features.Skin3DRenderer.OffsetProvider;
import com.shyeuar.baity.features.Skin3DRenderer.SkinData;
import com.shyeuar.baity.features.Skin3DRenderer.VoxelMesh;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class Skin3DMixin {

    @Mixin(PlayerModel.class)
    public static abstract class PlayerModelMixin extends HumanoidModel<AvatarRenderState>
            implements Skin3DRenderer.PlayerModelMarker {

        @Shadow public ModelPart leftSleeve;
        @Shadow public ModelPart rightSleeve;
        @Shadow public ModelPart leftPants;
        @Shadow public ModelPart rightPants;
        @Shadow public ModelPart jacket;

        @Unique
        private boolean baity$ignored = false;

        protected PlayerModelMixin(ModelPart root) {
            super(root);
        }

        @Override
        public void baity$setIgnored(boolean ignored) {
            this.baity$ignored = ignored;
        }

        @Override
        public boolean baity$isIgnored() {
            return this.baity$ignored;
        }

        @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
                at = @At("TAIL"))
        private void baity$setupSkin3D(AvatarRenderState state, CallbackInfo ci) {
            if (baity$ignored) {
                return;
            }

            if (!baity$isMainPlayerModel()) {
                return;
            }

            Skin3DRenderer.clearInjectedMesh(this.hat);
            Skin3DRenderer.clearInjectedMesh(jacket);
            Skin3DRenderer.clearInjectedMesh(leftSleeve);
            Skin3DRenderer.clearInjectedMesh(rightSleeve);
            Skin3DRenderer.clearInjectedMesh(leftPants);
            Skin3DRenderer.clearInjectedMesh(rightPants);

            if (!Skin3DRenderer.isEnabled()) {
                return;
            }

            if (!Skin3DRenderer.inRange(state)) {
                return;
            }

            SkinData data = Skin3DRenderer.getOrCreateSkinData(state);
            if (data == null) {
                return;
            }

            boolean slim = state.skin != null &&
                    state.skin.model() == PlayerModelType.SLIM;

            if (state.showHat) {
                Skin3DRenderer.injectMesh(this.hat, data.head, OffsetProvider.HEAD);
            }
            if (state.showJacket) {
                Skin3DRenderer.injectMesh(jacket, data.body, OffsetProvider.BODY);
            }
            if (state.showLeftSleeve) {
                Skin3DRenderer.injectMesh(leftSleeve, data.leftArm,
                        slim ? OffsetProvider.LEFT_ARM_SLIM : OffsetProvider.LEFT_ARM);
            }
            if (state.showRightSleeve) {
                Skin3DRenderer.injectMesh(rightSleeve, data.rightArm,
                        slim ? OffsetProvider.RIGHT_ARM_SLIM : OffsetProvider.RIGHT_ARM);
            }
            if (state.showLeftPants) {
                Skin3DRenderer.injectMesh(leftPants, data.leftLeg, OffsetProvider.LEFT_LEG);
            }
            if (state.showRightPants) {
                Skin3DRenderer.injectMesh(rightPants, data.rightLeg, OffsetProvider.RIGHT_LEG);
            }
        }

        @Unique
        private boolean baity$isMainPlayerModel() {
           
            return Skin3DRenderer.isRegisteredMainModel((Object) this);
        }
    }

    @Mixin(AvatarRenderer.class)
    public static abstract class EquipmentMixin
            extends net.minecraft.client.renderer.entity.LivingEntityRenderer<
                    net.minecraft.client.player.AbstractClientPlayer,
                    AvatarRenderState,
                    PlayerModel> {

        protected EquipmentMixin() {
            super(null, null, 0);
        }

        @Inject(method = "<init>", at = @At("TAIL"))
        private void baity$registerMainModel(CallbackInfo ci) {
            Skin3DRenderer.registerMainModel(this.getModel());
        }

        @Inject(method = "renderRightHand", at = @At("HEAD"))
        private void baity$renderFirstPersonRightArm(
                PoseStack matrices,
                net.minecraft.client.renderer.SubmitNodeCollector queue,
                int light,
            net.minecraft.resources.Identifier skinTexture,
                boolean sleeveVisible,
                CallbackInfo ci
        ) {
            baity$setupFirstPersonArm(false, sleeveVisible);
        }

        @Inject(method = "renderLeftHand", at = @At("HEAD"))
        private void baity$renderFirstPersonLeftArm(
                PoseStack matrices,
                net.minecraft.client.renderer.SubmitNodeCollector queue,
                int light,
            net.minecraft.resources.Identifier skinTexture,
                boolean sleeveVisible,
                CallbackInfo ci
        ) {
            baity$setupFirstPersonArm(true, sleeveVisible);
        }

        @Unique
        private void baity$setupFirstPersonArm(boolean isLeftArm, boolean sleeveVisible) {
            if (!Skin3DRenderer.isEnabled()) {
                return;
            }

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            SkinData data = Skin3DRenderer.getOrCreateSkinDataForPlayer(mc.player);
            if (data == null) {
                return;
            }

            PlayerModel model = this.getModel();
            boolean slim = data.slim;
            ModelPart sleeve;
            VoxelMesh mesh;
            OffsetProvider offset;

            if (isLeftArm) {
                sleeve = model.leftSleeve;
                mesh = data.leftArm;
                offset = slim ? OffsetProvider.FIRSTPERSON_LEFT_ARM_SLIM : OffsetProvider.FIRSTPERSON_LEFT_ARM;
            } else {
                sleeve = model.rightSleeve;
                mesh = data.rightArm;
                offset = slim ? OffsetProvider.FIRSTPERSON_RIGHT_ARM_SLIM : OffsetProvider.FIRSTPERSON_RIGHT_ARM;
            }

            Skin3DRenderer.clearInjectedMesh(sleeve);
            if (sleeveVisible && mesh != null) {
                Skin3DRenderer.injectMesh(sleeve, mesh, offset);
            }
        }
    }

    @Mixin(Deadmau5EarsLayer.class)
    public static class Deadmau5EarsMixin {

        @Shadow
        @Final
        private HumanoidModel<AvatarRenderState> model;

        @Inject(method = "<init>", at = @At("TAIL"))
        private void baity$markEarsModelAsIgnored(CallbackInfo ci) {
            if (model instanceof Skin3DRenderer.PlayerModelMarker marker) {
                marker.baity$setIgnored(true);
            }
        }
    }

    @Mixin(value = ModelPart.class, priority = 300)
    public static abstract class ModelPartMixin implements Skin3DRenderer.MeshInjector {

        @Shadow public boolean visible;

        @Shadow public abstract void translateAndRotate(PoseStack matrices);

        @Unique
        private VoxelMesh baity$injectedMesh = null;

        @Unique
        private OffsetProvider baity$offsetProvider = null;

        @Unique
        private boolean baity$renderingEnabled = false;

        @Override
        public void baity$setInjectedMesh(VoxelMesh mesh, OffsetProvider offset) {
            this.baity$injectedMesh = mesh;
            this.baity$offsetProvider = offset;
            this.baity$renderingEnabled = (mesh != null);
        }

        @Override
        public VoxelMesh baity$getInjectedMesh() {
            return this.baity$injectedMesh;
        }

        @Override
        public OffsetProvider baity$getOffsetProvider() {
            return this.baity$offsetProvider;
        }

        @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$renderInjectedMesh4(PoseStack matrices, VertexConsumer vertices,
                                               int light, int overlay, CallbackInfo ci) {
            baity$doRender(matrices, vertices, light, overlay, -1, ci);
        }

        @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$renderInjectedMesh5(PoseStack matrices, VertexConsumer vertices,
                                               int light, int overlay, int color, CallbackInfo ci) {
            baity$doRender(matrices, vertices, light, overlay, color, ci);
        }

        @Unique
        private void baity$doRender(PoseStack matrices, VertexConsumer vertices,
                                    int light, int overlay, int color, CallbackInfo ci) {
            if (!visible || !baity$renderingEnabled || baity$injectedMesh == null || baity$offsetProvider == null) {
                return;
            }

            if (!Skin3DRenderer.isEnabled()) {
                return;
            }

            matrices.pushPose();
            translateAndRotate(matrices);
            baity$offsetProvider.applyOffset(matrices, baity$injectedMesh);
            baity$injectedMesh.render(matrices, vertices, light, overlay, color);
            matrices.popPose();

            baity$renderingEnabled = false;
            ci.cancel();
        }
    }
}
