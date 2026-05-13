package com.shyeuar.baity.mixin;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.LocateUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ClientPacketListener.class)
public class NonStarredMobTagsMixin {

    @Unique
    private static final Set<String> BLOOD_NAME_WHITELIST = Set.of(
        "Putrid", "Reaper", "Vader", "Frost", "Cannibal", "Revoker", "Tear", "Mr. Dead", "Skull",
        "Walker", "Psycho", "Ooze", "Freak", "Flamer", "Mute", "Leech", "Parasite",
        "Bonzo", "Scarf", "Spirit Bear", "Livid",
        "L.A.S.R.", "The Diamond Giant", "Jolly Pink Giant", "Bigfoot"
    );

    @Unique
    private static final Set<String> IGNORED_MOB_NAMES;

    @Unique
    private static final Pattern NAME_TAG_PATTERN =
        Pattern.compile("^(?:\\[Lv\\d+] )?[^\\sA-Za-z]* ?([A-Za-z ]+) [\\dkMB.,/]+❤$");

    static {
        Set<String> baseNames = Set.of(
            "Mimic", "Crypt Undead",
            "Prince Alexander", "Prince Bernhard", "Prince Christian", "Prince Friedrich",
            "Prince Marius", "Prince Nicholas", "Prince Pieter", "Prince Valentin",
            "hypixel", "sfarnham", "aPunch", "Jayavarmen", "Don Pireso", "codename_B",
            "WilliamTiger", "TheMGRF", "Ob111", "Sylent", "Bloozing", "Nitroholic",
            "Minikloon", "Relenter", "Externalizable", "Plancke", "ChiLynn", "skyerzz",
            "Magicboys", "Cecer", "Likaos", "Linfoot", "Dctr", "_onah", "falloutowns",
            "LadyBleu", "Revengeee", "Bembo", "flameboy101", "JamieTheGeek", "Judg3",
            "Rezzus", "AgentK", "Thorlon", "Vinny", "fudgiethewhale", "DistrictGecko",
            "Dueces", "Cheesey", "BlocksKey", "DEADORKAI", "Plummel", "AdamWho",
            "Winghide", "MistressEldrid",
            "Blaze", "King Midas", "Deathmite",
            "Akia", "Ilene", "Kari", "Lelani", "Steve", "Synestra", "Tyene",
            "Ussaea", "Yve", "Zana", "Trisha", "Nymira"
        );

        java.util.HashSet<String> all = new java.util.HashSet<>(baseNames);
        all.addAll(BLOOD_NAME_WHITELIST);
        IGNORED_MOB_NAMES = Set.copyOf(all);
    }

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void baity$hideNonStarredFromPacket(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        Module module = ModuleManager.getModuleByName("Culling");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.cullingHideNonStarredNametag) return;

        Minecraft mc = Minecraft.getInstance();
        if (!LocateUtils.isInDungeonRun(mc)) return;
        ClientLevel level = mc.level;
        if (level == null) return;

        Entity entity = level.getEntity(packet.id());
        if (!(entity instanceof ArmorStand armorStand)) return;

        Component comp = armorStand.getCustomName();
        if (comp == null) comp = armorStand.getDisplayName();
        if (comp == null) return;

        String raw = comp.getString();
        if (com.shyeuar.baity.features.fishing.FishHookTimer.isFishHookTimerStandName(comp)) return;
        Matcher matcher = NAME_TAG_PATTERN.matcher(raw);
        if (!matcher.matches()) return;

        String mobName = matcher.group(1);
        if (mobName == null || mobName.isEmpty()) return;
        if (IGNORED_MOB_NAMES.contains(mobName)) return;

        int space = mobName.indexOf(' ');
        if (space >= 0 && space + 1 < mobName.length()) {
            String suffix = mobName.substring(space + 1);
            if (BLOOD_NAME_WHITELIST.contains(suffix)) return;
        }

        armorStand.remove(Entity.RemovalReason.DISCARDED);
    }

}

