package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.FancyDmgSplash;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.render.entity.ArmorStandEntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ArmorStandEntityRenderer.class)
public class SkyblockDamageMixin {
    
   
    @Unique
    private static final Pattern DAMAGE_PATTERN = Pattern.compile("[✧✯]?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?[kKmMbB]?[⚔+✧❤♞☄✷ﬗ✯]*)");
    
    @Unique
    private static final Set<Integer> processedEntities = new HashSet<>();
    
    @Unique
    private static long lastCleanupTime = 0;
   
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/ArmorStandEntity;Lnet/minecraft/client/render/entity/state/ArmorStandEntityRenderState;F)V", 
            at = @At("TAIL"))
    private void baity$onUpdateRenderState(ArmorStandEntity armorStand, ArmorStandEntityRenderState state, 
                                            float tickDelta, CallbackInfo ci) {
        
        if (!armorStand.hasCustomName() || armorStand.getCustomName() == null) {
            return;
        }
        
        String customName = armorStand.getCustomName().getString();
        if (customName == null || customName.isEmpty()) {
            return;
        }
        
        Module m = ModuleManager.getModuleByName("FancyDmgSplash");
        if (m == null || !m.isEnabled()) {
            return;
        }
        
        Matcher matcher = DAMAGE_PATTERN.matcher(customName);
        if (!matcher.matches()) {
            return;
        }
        
        state.invisible = true;
        state.displayName = null;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > 5000) {
            processedEntities.clear();
            lastCleanupTime = currentTime;
        }
        
        if (!processedEntities.contains(armorStand.getId())) {
            processedEntities.add(armorStand.getId());
            
            try {
                double damage = parseDamageValue(customName);
                Vec3d targetPos = new Vec3d(armorStand.getX(), armorStand.getY(), armorStand.getZ());
                
                Text originalText = armorStand.getCustomName();
                
                FancyDmgSplash.addDamageNumber(damage, targetPos, originalText);
                
            } catch (NumberFormatException ignored) {
            }
        }
    }
    
    @Unique
    private static double parseDamageValue(String text) {
        String cleaned = text.replaceAll("[^\\d.,kKmMbB]", "");
        
        cleaned = cleaned.replace(",", "");
        
        double multiplier = 1.0;
        
        if (cleaned.toLowerCase().endsWith("b")) {
            multiplier = 1_000_000_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.toLowerCase().endsWith("m")) {
            multiplier = 1_000_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        } else if (cleaned.toLowerCase().endsWith("k")) {
            multiplier = 1_000.0;
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        
        return Double.parseDouble(cleaned) * multiplier;
    }
}
