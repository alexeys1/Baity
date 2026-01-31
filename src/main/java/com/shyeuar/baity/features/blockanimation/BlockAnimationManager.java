package com.shyeuar.baity.features.blockanimation;

import com.shyeuar.baity.utils.BlockAnimationUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class BlockAnimationManager {
    private BlockAnimationManager() {}
    
    private static final Map<BlockPos, Integer> breakingProgress = new HashMap<>();
    private static BlockPos currentBreakingPos = null;
    private static int breakingTicks = 0;
    
    private static boolean isSwinging = false;
    private static InteractionHand swingHand = InteractionHand.MAIN_HAND;
    private static int swingTime = 0;
    private static final int SWING_DURATION = 6; 
   
    public static void register() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!BlockAnimationUtils.isFeatureActive()) return;
            if (client.player == null) return;
            
            if (BlockAnimationUtils.isPlayerBlockingWithSword(client.player)) {
                continueBreaking();
            } else {
                stopBreaking();
            }
            
            updateSwing();
        });
    }
  
    public static void startBreaking(BlockPos pos, @SuppressWarnings("unused") Direction direction) {
        if (!BlockAnimationUtils.isFeatureActive()) return;
        if (!BlockAnimationUtils.isPlayerRightClicking()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) return;
        
        currentBreakingPos = pos;
        breakingTicks = 0;
        breakingProgress.put(pos, 0);
    }

    public static void continueBreaking() {
        if (currentBreakingPos == null) return;
        if (!BlockAnimationUtils.isFeatureActive()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            stopBreaking();
            return;
        }
        
        if (!BlockAnimationUtils.isPlayerBlockingWithSword(mc.player)) {
            stopBreaking();
            return;
        }
        
        if (!mc.options.keyAttack.isDown()) {
            stopBreaking();
            return;
        }
        
        breakingTicks++;
       
        int progress = Math.min(10, breakingTicks); 
        breakingProgress.put(currentBreakingPos, progress);
    }
   
    public static void stopBreaking() {
        if (currentBreakingPos != null) {
            breakingProgress.remove(currentBreakingPos);
            currentBreakingPos = null;
            breakingTicks = 0;
        }
    }
   
    public static int getBreakingProgress(BlockPos pos) {
        return breakingProgress.getOrDefault(pos, -1);
    }
    
    public static boolean isBreaking(BlockPos pos) {
        return breakingProgress.containsKey(pos);
    }
    
    public static BlockPos getCurrentBreakingPos() {
        return currentBreakingPos;
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

