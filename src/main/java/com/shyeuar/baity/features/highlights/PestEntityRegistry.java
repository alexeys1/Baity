package com.shyeuar.baity.features.highlights;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class PestEntityRegistry {

    public static final String TEXTURE_BEETLE =
            "ewogICJ0aW1lc3RhbXAiIDogMTcyMzE3OTc4OTkzNCwKICAicHJvZmlsZUlkIiA6ICJlMjc5NjliODYyNWY0NDg1YjkyNmM5NTBhMDljMWMwMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLRVZJTktFTE9LRSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83MGExZTgzNmJmMTk2OGIyZWFhNDgzNzIyN2ExOTIwNGYxNzI5NWQ4NzBlZTllNzU0YmQ2YjZkNjBkZGJlZDNjIgogICAgfQogIH0KfQ==";
    public static final String TEXTURE_CRICKET =
            "ewogICJ0aW1lc3RhbXAiIDogMTcyMzE3OTgxMTI2NCwKICAicHJvZmlsZUlkIiA6ICJjZjc4YzFkZjE3ZTI0Y2Q5YTIxYmU4NWQ0NDk5ZWE4ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNYXR0c0FybW9yU3RhbmRzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EyNGM2OWY5NmNlNTU2MjIxZTE5NWM4ZWYyYmZhZDcxZWJmN2Y5NWY1YWU5MTRhNDg0YThkMGVjMjE2NzI2NzQiCiAgICB9CiAgfQp9";
    public static final String TEXTURE_DRAGONFLY =
            "ewogICJ0aW1lc3RhbXAiIDogMTc2MDQ1MDQxODQzNywKICAicHJvZmlsZUlkIiA6ICIwNjY5Y2E1MGYyZWU0NTQxODhlYWQ3YTM3NTkzNDRlMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcjR6eWNsb3duVFYiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjU0YWZmNGMwYjJkY2UzYTY3MjM0OWNjMGVlOWU2ZjNhOWRlZWJlNGIzNTU2ZTg0NjExZWNhMjUwYTc4MjFiZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    public static final String TEXTURE_EARTHWORM =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5NzQ3MDQ1OTc0NywKICAicHJvZmlsZUlkIiA6ICIyNTBlNzc5MjZkNDM0ZDIyYWM2MTQ4N2EyY2M3YzAwNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJMdW5hMTIxMDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQwM2JhNDAyN2EzMzNkOGQyZmQzMmFiNTlkMWNmZGJhYTdkOTA4ZDgwZDIzODFkYjJhNjljYmU2NTQ1MGFkOCIKICAgIH0KICB9Cn0=";
    public static final String TEXTURE_EARTHWORM_TAIL =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5NzQ3MDQ3ODAzMCwKICAicHJvZmlsZUlkIiA6ICI0NmY3N2NjNmQ2MjU0NjEzYjc2NmYyZDRmMDM2MzZhNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaXNzV29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZDQwYWE1MDkwNTIzNWI2MjhlNzM3OWViMzFmYTQ1Y2Q0MWI1MDNmMDk3MjFkYjNjNDM3ZmNlZTM5MjA3ZGZjIgogICAgfQogIH0KfQ==";
    public static final String TEXTURE_FIELD_MOUSE =
            "ewogICJ0aW1lc3RhbXAiIDogMTcyNzkwNDc5NzQ1OSwKICAicHJvZmlsZUlkIiA6ICI0MmIwOTMyZDUwMWI0MWQ1YTM4YjEwOTcxYTYwYmYxMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJBaXJib2x0MDc4IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2YzNzllMDkyNTI4MTczMTRiZDBiNjk0ZjdkNTNiNDhhZjJjN2ZhODQ5OTEwOTgwMmE0MWJiMjk0ZDJmOTNlM2UiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    public static final String TEXTURE_FIREFLY =
            "ewogICJ0aW1lc3RhbXAiIDogMTc2MDQ1MDQyMjEzNiwKICAicHJvZmlsZUlkIiA6ICIzNDY4Y2VjMWFlOTY0YWRmYWQyNjEzMGEwZGQ0NjRkYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJzdXJlZWxta18iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNlNzllOTBhZGYzNDcxOGYzMTNlYzI0ZDZjNjEzNWI2OWIzNzg4YzYxODQ5ODQ0NmNjYzgzY2E2NDBjMGIxNCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    public static final String TEXTURE_FIREFLY_FLASH =
            "ewogICJ0aW1lc3RhbXAiIDogMTc2MDQ1MDQyMzg4OSwKICAicHJvZmlsZUlkIiA6ICIyY2Y2MzExZjUyMTM0NTE2YTEyNTY3NWUwMzk3NmU2MSIsCiAgInByb2ZpbGVOYW1lIiA6ICJmaWdodHN0b2NrIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzNlNTI3ODJkN2YyYWFlZThhZjViYTI5MjhmZWM3ODg1ZTk0ODc5MzM0YzIyOTZiYzllN2UyZGJjNTQxOGU1OGYiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    public static final String TEXTURE_FLY =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5Njk0NTA2MzI4MSwKICAicHJvZmlsZUlkIiA6ICJjN2FmMWNkNjNiNTE0Y2YzOGY4NWQ2ZDUxNzhjYThlNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJtb25zdGVyZ2FtZXIzMTUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5MGU3Nzc4MjZhNTI0NjEzNjhlMjZkMWIyZTE5YmZhMWJhNTgyZDYwMjQ4M2U1NDVmNDEyNGQwZjczMTg0MiIKICAgIH0KICB9Cn0=";
    public static final String TEXTURE_LOCUST =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5NzU1NzA3NzAzNywKICAicHJvZmlsZUlkIiA6ICI0YjJlMGM1ODliZjU0ZTk1OWM1ZmJlMzg5MjQ1MzQzZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJfTmVvdHJvbl8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGIyNGE0ODJhMzJkYjFlYTc4ZmI5ODA2MGIwYzJmYTRhMzczY2JkMThhNjhlZGRkZWI3NDE5NDU1YTU5Y2RhOSIKICAgIH0KICB9Cn0=";
    public static final String TEXTURE_MITE =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQxOTcyNSwKICAicHJvZmlsZUlkIiA6ICJkYjYzNWE3MWI4N2U0MzQ5YThhYTgwOTMwOWFhODA3NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJFbmdlbHMxNzQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmU2YmFmNjQzMWE5ZGFhMmNhNjA0ZDVhM2MyNmU5YTc2MWQ1OTUyZjA4MTcxNzRhNGZlMGI3NjQ2MTZlMjFmZiIKICAgIH0KICB9Cn0=";
    public static final String TEXTURE_MOSQUITO =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5Njk0NTAyOTQ2MSwKICAicHJvZmlsZUlkIiA6ICI3NTE0NDQ4MTkxZTY0NTQ2OGM5NzM5YTZlMzk1N2JlYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGFua3NNb2phbmciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTJhOWZlMDViYzY2M2VmY2QxMmU1NmEzY2NjNWVjMDM1YmY1NzdiNzg3MDg1NDhiNmY0ZmZjZjFkMzBlY2NmZSIKICAgIH0KICB9Cn0=";
    public static final String TEXTURE_MOTH =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5Njg3MDQwNTk1NCwKICAicHJvZmlsZUlkIiA6ICJiMTUyZDlhZTE1MTM0OWNmOWM2NmI0Y2RjMTA5NTZjOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaXNxdW90aCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82NTQ4NWM0YjM0ZTViNTQ3MGJlOTRkZTEwMGU2MWY3ODE2ZjgxYmM1YTExZGZkZjBlY2NmODkwMTcyZGE1ZDBhIgogICAgfQogIH0KfQ==";
    public static final String TEXTURE_PRAYING_MANTIS =
            "ewogICJ0aW1lc3RhbXAiIDogMTc2MDQ1MDQxOTYxMiwKICAicHJvZmlsZUlkIiA6ICI0OWIzODUyNDdhMWY0NTM3YjBmN2MwZTFmMTVjMTc2NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJiY2QyMDMzYzYzZWM0YmY4IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzFlMDRiYjYzNjdjYWE0ZTg4ZjVmZDBlZTgwZjA3NDVkMTM3YTYwNjAyMjNkYmJjNDJhMTY0NzFmZGY2NGJiODMiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    public static final String TEXTURE_RAT =
            "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
    public static final String TEXTURE_SLUG =
            "ewogICJ0aW1lc3RhbXAiIDogMTY5NzQ3MDQ0MzA4MiwKICAicHJvZmlsZUlkIiA6ICJkOGNkMTNjZGRmNGU0Y2IzODJmYWZiYWIwOGIyNzQ4OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJaYWNoeVphY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2E3OWQwZmQ2NzdiNTQ1MzA5NjExMTdlZjg0YWRjMjA2ZTJjYzUwNDVjMTM0NGQ2MWQ3NzZiZjhhYzJmZTFiYSIKICAgIH0KICB9Cn0=";
    public static final String TEXTURE_LUNAR_MOTH =
            "ewogICJ0aW1lc3RhbXAiIDogMTc3MjE1OTE4MzkzMSwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iZWU0ZmNjNWFhYzI3YmRiYjBiZTIxMGRmMDhiMDViY2E1YWViYzg5YmYyODIxZjYwOGE1NWZkMmNmMDQzNGJlIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";

    private static final Set<String> KNOWN_PEST_HEAD_VALUES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    TEXTURE_BEETLE,
                    TEXTURE_CRICKET,
                    TEXTURE_DRAGONFLY,
                    TEXTURE_EARTHWORM,
                    TEXTURE_EARTHWORM_TAIL,
                    TEXTURE_FIELD_MOUSE,
                    TEXTURE_FIREFLY,
                    TEXTURE_FIREFLY_FLASH,
                    TEXTURE_FLY,
                    TEXTURE_LOCUST,
                    TEXTURE_MITE,
                    TEXTURE_MOSQUITO,
                    TEXTURE_MOTH,
                    TEXTURE_PRAYING_MANTIS,
                    TEXTURE_RAT,
                    TEXTURE_SLUG,
                    TEXTURE_LUNAR_MOTH
            )));

    public static final String EARTHWORM_SEGMENT_TEXTURE = TEXTURE_EARTHWORM;

    private static final ConcurrentHashMap<Integer, String> trackedHeadTextureByEntityId = new ConcurrentHashMap<>();

    static {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    private PestEntityRegistry() {}

    public static void clear() {
        trackedHeadTextureByEntityId.clear();
        gardenPestRescanTickCounter = 0;
    }

    public static Map<Integer, String> snapshotTrackedPests() {
        return new HashMap<>(trackedHeadTextureByEntityId);
    }

    public static void afterClientEntityData(int entityId) {
        tryRegisterPestArmorStand(entityId);
    }

    public static void onClientEntityLoad(Entity entity) {
        if (entity instanceof ArmorStand) {
            tryRegisterPestArmorStand(entity.getId());
        }
    }

    public static void pruneStale() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            trackedHeadTextureByEntityId.clear();
            return;
        }
        trackedHeadTextureByEntityId.entrySet().removeIf(entry -> {
            Entity e = mc.level.getEntity(entry.getKey());
            return !(e instanceof ArmorStand as && as.isAlive());
        });
    }

    private static boolean pestHighlightActive() {
        var module = ModuleManager.getModuleByName("Highlights");
        if (module == null || !module.isEnabled()) {
            return false;
        }
        return ConfigManager.highlightsPestEnabled;
    }

    private static int gardenPestRescanTickCounter;

    public static void tickThrottledRescanGardenPests() {
        if (!pestHighlightActive()) {
            return;
        }
        if (++gardenPestRescanTickCounter < 20) {
            return;
        }
        gardenPestRescanTickCounter = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !LocateUtils.isOwnGarden(mc)) {
            return;
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof ArmorStand as && as.isAlive()) {
                tryRegisterPestArmorStand(as.getId());
            }
        }
    }

    static void tryRegisterPestArmorStand(int entityId) {
        if (!pestHighlightActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !LocateUtils.isOwnGarden(mc)) {
            return;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (!(entity instanceof ArmorStand stand) || !stand.isAlive()) {
            trackedHeadTextureByEntityId.remove(entityId);
            return;
        }
        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty() || !head.is(Items.PLAYER_HEAD)) {
            trackedHeadTextureByEntityId.remove(entityId);
            return;
        }
        String textureValue = headTexturePropertyValue(head);
        if (textureValue != null && KNOWN_PEST_HEAD_VALUES.contains(textureValue)) {
            String previous = trackedHeadTextureByEntityId.put(stand.getId(), textureValue);
            if (previous == null) {
                TabJacobContestCrop.requestImmediateRescan();
            }
        } else {
            trackedHeadTextureByEntityId.remove(entityId);
        }
    }

    private static String headTexturePropertyValue(ItemStack stack) {
        ResolvableProfile resolvable = stack.get(DataComponents.PROFILE);
        if (resolvable == null) {
            return null;
        }
        GameProfile profile = resolvable.partialProfile();
        if (profile == null) {
            return null;
        }
        PropertyMap map = profile.properties();
        if (map == null || map.isEmpty()) {
            return null;
        }
        var textures = map.get("textures");
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        return textures.stream()
                .filter(Objects::nonNull)
                .map(Property::value)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }
}
