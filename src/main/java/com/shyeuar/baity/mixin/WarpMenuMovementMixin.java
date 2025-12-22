package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.WarpMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class WarpMenuMovementMixin extends Input {

    @Inject(method = "tick", at = @At("TAIL"))
    private void allowMovementInWarpMenu(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof WarpMenuScreen) {
            long window = client.getWindow().getHandle();
            GameOptions options = client.options;

            boolean forward = isKeyPressed(window, options.forwardKey);
            boolean back = isKeyPressed(window, options.backKey);
            boolean left = isKeyPressed(window, options.leftKey);
            boolean right = isKeyPressed(window, options.rightKey);
            boolean jump = isKeyPressed(window, options.jumpKey);
            boolean sneak = isKeyPressed(window, options.sneakKey);
            boolean sprint = isKeyPressed(window, options.sprintKey);

            if (options.getSprintToggled().getValue() && forward && !back) {
                sprint = true;
            }

            this.playerInput = new PlayerInput(forward, back, left, right, jump, sneak, sprint);

            float forwardMovement = getMovementMultiplier(forward, back);
            float sidewaysMovement = getMovementMultiplier(left, right);
            this.movementVector = new Vec2f(sidewaysMovement, forwardMovement).normalize();
        }
    }

    private static boolean isKeyPressed(long window, KeyBinding keyBinding) {
        int keyCode = keyBinding.getDefaultKey().getCode();
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }

    private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }
}
