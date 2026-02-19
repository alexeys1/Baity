package com.shyeuar.baity.mixin;

import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplash;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ClientPacketListener.class)
public class FancyDmgSplashMixin {

    @Unique
    private static final Pattern DAMAGE_PATTERN =
        Pattern.compile("[✧✯]?(\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?[kKmMbB]?[⚔+✧❤♞☄✷ﬗ✯]*)");

    @Unique
    private static final Set<Integer> DAMAGE_PROCESSED_IDS = new HashSet<>();

    @Unique
    private static long DAMAGE_LAST_CLEANUP_MS = 0L;

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void baity$fancyDmgSplashFromPacket(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        Module module = ModuleManager.getModuleByName("FancyDmgSplash");
        if (module == null || !module.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Entity entity = level.getEntity(packet.id());
        if (!(entity instanceof ArmorStand armorStand)) return;
        if (!armorStand.hasCustomName() || armorStand.getCustomName() == null) return;

        String customName = armorStand.getCustomName().getString();
        if (customName == null || customName.isEmpty()) return;

        Matcher matcher = DAMAGE_PATTERN.matcher(customName);
        if (!matcher.matches()) return;

        long now = System.currentTimeMillis();
        if (now - DAMAGE_LAST_CLEANUP_MS > 5000L) {
            DAMAGE_PROCESSED_IDS.clear();
            DAMAGE_LAST_CLEANUP_MS = now;
        }

        if (!DAMAGE_PROCESSED_IDS.add(armorStand.getId())) return;

        double damage;
        try {
            damage = parseDamageValue(customName);
        } catch (NumberFormatException ignored) {
            return;
        }

        Vec3 targetPos = new Vec3(armorStand.getX(), armorStand.getY(), armorStand.getZ());

        Component originalText = armorStand.getCustomName();
        boolean useCompactDamage =
            com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(module, "compact damage number", true);

        Component formattedText = originalText;
        if (useCompactDamage) {
            boolean isCritical = customName.contains("✧") || customName.contains("✯");
            if (damage >= 1000 || isCritical) {
                formattedText = FancyDmgSplash.applyCompactFormatting(originalText, damage);
            }
        }

        FancyDmgSplash.addDamageNumber(damage, targetPos, formattedText);
        armorStand.remove(Entity.RemovalReason.DISCARDED);
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

