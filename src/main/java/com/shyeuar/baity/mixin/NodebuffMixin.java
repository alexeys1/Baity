package com.shyeuar.baity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.mixin.accessor.FogRendererAccessor;
import com.shyeuar.baity.utils.ModuleUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class NodebuffMixin {
    
    @Mixin(GameRenderer.class)
    public static class GameRendererMixin {

        @Shadow @Final private FogRenderer fogRenderer;

        @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotate(FLorg/joml/Vector3fc;)Lorg/joml/Matrix4f;", ordinal = 0), require = 1)
        private Matrix4f baity$preventNauseaRotation(Matrix4f matrix, float angle, Vector3fc axis, Operation<Matrix4f> original) {
            if (shouldRemoveNausea()) {
                return matrix;
            }
            return original.call(matrix, angle, axis);
        }

        @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotate(FLorg/joml/Vector3fc;)Lorg/joml/Matrix4f;", ordinal = 1), require = 1)
        private Matrix4f baity$preventNauseaRotationSecond(Matrix4f matrix, float angle, Vector3fc axis, Operation<Matrix4f> original) {
            if (shouldRemoveNausea()) {
                return matrix;
            }
            return original.call(matrix, angle, axis);
        }

        @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;scale(FFF)Lorg/joml/Matrix4f;"), require = 1)
        private Matrix4f baity$preventNauseaScaling(Matrix4f matrix, float x, float y, float z, Operation<Matrix4f> original) {
            if (shouldRemoveNausea()) {
                return matrix;
            }
            return original.call(matrix, x, y, z);
        }

        @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), require = 1)
        private void baity$clearBlindnessFog(DeltaTracker deltaTracker, CallbackInfo ci) {
            if (shouldRemoveBlindness()) {
                FogRendererAccessor.baity$getFogEnvironments().removeIf(env -> env instanceof BlindnessFogEnvironment);
            }
        }

        private static boolean shouldRemoveNausea() {
            Module module = ModuleManager.getModuleByName("Nodebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove nausea", true);
        }

        private static boolean shouldRemoveBlindness() {
            Module module = ModuleManager.getModuleByName("Nodebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove blindness", true);
        }
    }

    @Mixin(Hud.class)
    public static class HudMixin {

        @Inject(method = "extractConfusionOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V", at = @At("HEAD"), cancellable = true, require = 1)
        private void baity$cancelNauseaOverlay(GuiGraphicsExtractor guiGraphics, float intensity, CallbackInfo ci) {
            if (shouldRemoveNausea()) {
                ci.cancel();
            }
        }

        private static boolean shouldRemoveNausea() {
            Module module = ModuleManager.getModuleByName("Nodebuff");
            return module != null && module.isEnabled() && ModuleUtils.getOptionBoolean(module, "remove nausea", true);
        }
    }
}
