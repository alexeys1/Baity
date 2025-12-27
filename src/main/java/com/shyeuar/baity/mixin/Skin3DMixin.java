package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.Skin3DRenderer;
import com.shyeuar.baity.features.Skin3DRenderer.OffsetProvider;
import com.shyeuar.baity.features.Skin3DRenderer.SkinData;
import com.shyeuar.baity.features.Skin3DRenderer.VoxelMesh;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerSkinType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class Skin3DMixin {

    @Mixin(PlayerEntityModel.class)
    public static abstract class PlayerModelMixin extends BipedEntityModel<PlayerEntityRenderState>
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

        @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
                at = @At("TAIL"))
        private void baity$setupSkin3D(PlayerEntityRenderState state, CallbackInfo ci) {
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

            boolean slim = state.skinTextures != null &&
                    state.skinTextures.model() == PlayerSkinType.SLIM;

            if (state.hatVisible) {
                Skin3DRenderer.injectMesh(this.hat, data.head, OffsetProvider.HEAD);
            }
            if (state.jacketVisible) {
                Skin3DRenderer.injectMesh(jacket, data.body, OffsetProvider.BODY);
            }
            if (state.leftSleeveVisible) {
                Skin3DRenderer.injectMesh(leftSleeve, data.leftArm,
                        slim ? OffsetProvider.LEFT_ARM_SLIM : OffsetProvider.LEFT_ARM);
            }
            if (state.rightSleeveVisible) {
                Skin3DRenderer.injectMesh(rightSleeve, data.rightArm,
                        slim ? OffsetProvider.RIGHT_ARM_SLIM : OffsetProvider.RIGHT_ARM);
            }
            if (state.leftPantsLegVisible) {
                Skin3DRenderer.injectMesh(leftPants, data.leftLeg, OffsetProvider.LEFT_LEG);
            }
            if (state.rightPantsLegVisible) {
                Skin3DRenderer.injectMesh(rightPants, data.rightLeg, OffsetProvider.RIGHT_LEG);
            }
        }

        @Unique
        private boolean baity$isMainPlayerModel() {
           
            return Skin3DRenderer.isRegisteredMainModel((Object) this);
        }
    }

    @Mixin(PlayerEntityRenderer.class)
    public static abstract class EquipmentMixin
            extends net.minecraft.client.render.entity.LivingEntityRenderer<
                    net.minecraft.client.network.AbstractClientPlayerEntity,
                    PlayerEntityRenderState,
                    PlayerEntityModel> {

        protected EquipmentMixin() {
            super(null, null, 0);
        }

        @Inject(method = "<init>", at = @At("TAIL"))
        private void baity$registerMainModel(CallbackInfo ci) {
            Skin3DRenderer.registerMainModel(this.getModel());
        }

        @Inject(method = "renderRightArm", at = @At("HEAD"))
        private void baity$renderFirstPersonRightArm(
                MatrixStack matrices,
                net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
                int light,
                net.minecraft.util.Identifier skinTexture,
                boolean sleeveVisible,
                CallbackInfo ci
        ) {
            baity$setupFirstPersonArm(false, sleeveVisible);
        }

        @Inject(method = "renderLeftArm", at = @At("HEAD"))
        private void baity$renderFirstPersonLeftArm(
                MatrixStack matrices,
                net.minecraft.client.render.command.OrderedRenderCommandQueue queue,
                int light,
                net.minecraft.util.Identifier skinTexture,
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

            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player == null) {
                return;
            }

            SkinData data = Skin3DRenderer.getOrCreateSkinDataForPlayer(mc.player);
            if (data == null) {
                return;
            }

            PlayerEntityModel model = this.getModel();
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

    @Mixin(Deadmau5FeatureRenderer.class)
    public static class Deadmau5EarsMixin {

        @Shadow
        @Final
        private BipedEntityModel<PlayerEntityRenderState> model;

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

        @Shadow public abstract void applyTransform(MatrixStack matrices);

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

        @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$renderInjectedMesh4(MatrixStack matrices, VertexConsumer vertices,
                                               int light, int overlay, CallbackInfo ci) {
            baity$doRender(matrices, vertices, light, overlay, -1, ci);
        }

        @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                at = @At("HEAD"), cancellable = true)
        private void baity$renderInjectedMesh5(MatrixStack matrices, VertexConsumer vertices,
                                               int light, int overlay, int color, CallbackInfo ci) {
            baity$doRender(matrices, vertices, light, overlay, color, ci);
        }

        @Unique
        private void baity$doRender(MatrixStack matrices, VertexConsumer vertices,
                                    int light, int overlay, int color, CallbackInfo ci) {
            if (!visible || !baity$renderingEnabled || baity$injectedMesh == null || baity$offsetProvider == null) {
                return;
            }

            if (!Skin3DRenderer.isEnabled()) {
                return;
            }

            matrices.push();
            applyTransform(matrices);
            baity$offsetProvider.applyOffset(matrices, baity$injectedMesh);
            baity$injectedMesh.render(matrices, vertices, light, overlay, color);
            matrices.pop();

            baity$renderingEnabled = false;
            ci.cancel();
        }
    }
}
