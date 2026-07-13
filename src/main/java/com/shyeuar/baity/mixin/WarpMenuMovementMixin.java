package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.radialmenu.DynamicRadialMenuScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class WarpMenuMovementMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void allowMovementInWarpMenu(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof DynamicRadialMenuScreen) {
            long window = client.getWindow().handle();
            Options options = client.options;

            boolean forward = isKeyPressed(window, options.keyUp);
            boolean back = isKeyPressed(window, options.keyDown);
            boolean left = isKeyPressed(window, options.keyLeft);
            boolean right = isKeyPressed(window, options.keyRight);
            boolean jump = isKeyPressed(window, options.keyJump);
            boolean sneak = isKeyPressed(window, options.keyShift);
            boolean sprint = isKeyPressed(window, options.keySprint);

            if (options.toggleSprint().get() && forward && !back) {
                sprint = true;
            }

            this.keyPresses = new Input(forward, back, left, right, jump, sneak, sprint);

            float forwardMovement = getMovementMultiplier(forward, back);
            float sidewaysMovement = getMovementMultiplier(left, right);
            this.moveVector = new Vec2(sidewaysMovement, forwardMovement).normalized();
        }
    }

    private static boolean isKeyPressed(long window, KeyMapping keyBinding) {
        int keyCode = keyBinding.getDefaultKey().getValue();
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }

    private static float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }
}
