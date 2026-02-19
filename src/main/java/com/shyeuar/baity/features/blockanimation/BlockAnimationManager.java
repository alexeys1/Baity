package com.shyeuar.baity.features.blockanimation;

import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;


@Environment(EnvType.CLIENT)
public final class BlockAnimationManager {
    private BlockAnimationManager() {}
    
    private static boolean isSwinging = false;
    private static InteractionHand swingHand = InteractionHand.MAIN_HAND;
    private static int swingTime = 0;
    private static final int SWING_DURATION = 6; 
   
    public static void register() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (client.player == null) return;
            
            updateSwing();
        });
    }
  
    public static void startSwing(InteractionHand hand) {
        if (!BlockAnimationUtils.isFeatureActive()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;
        
        isSwinging = true;
        swingHand = hand;
        swingTime = 0;
    }
  
    public static void updateSwing() {
        if (!isSwinging) return;
        
        swingTime++;
        if (swingTime >= SWING_DURATION) {
            isSwinging = false;
            swingTime = 0;
        }
    }
  
    public static float getSwingProgress(float partialTicks) {
        if (!isSwinging) return 0.0F;
        return (float)(swingTime + partialTicks) / SWING_DURATION;
    }
   
    public static boolean isSwinging() {
        return isSwinging;
    }
   
    public static InteractionHand getSwingHand() {
        return swingHand;
    }
}

