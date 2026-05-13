package com.shyeuar.baity.features.fishing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shyeuar.baity.utils.LocateUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
public final class HypixelFishingRodCatalog {

    private static final String PUBLIC_ITEMS_JSON =
        "https://raw.githubusercontent.com/hannibal002/SkyHanni-REPO/main/constants/Items.json";
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 8_000;

    private static final Set<String> LAVA_ROD_IDS = new HashSet<>();
    private static final Set<String> WATER_ROD_IDS = new HashSet<>();
    private static volatile boolean remoteFetchStarted;
    private static volatile boolean mainHandHoldsHypixelFishingRod;

    static {
        seedBuiltInIds();
    }

    private HypixelFishingRodCatalog() {}

    private static void seedBuiltInIds() {
        for (String id : new String[] {
            "STARTER_LAVA_ROD",
            "INFERNO_ROD",
            "MAGMA_ROD",
            "HELLFIRE_ROD",
            "POLISHED_TOPAZ_ROD",
        }) {
            LAVA_ROD_IDS.add(id);
        }
        for (String id : new String[] {
            "FISHING_ROD",
            "CHALLENGE_ROD",
            "CHAMP_ROD",
            "LEGEND_ROD",
            "ROD_OF_THE_SEA",
            "GIANT_FISHING_ROD",
            "BINGO_ROD",
        }) {
            WATER_ROD_IDS.add(id);
        }
    }

    public static void init() {
        if (remoteFetchStarted) {
            return;
        }
        remoteFetchStarted = true;
        Thread.ofVirtual().start(HypixelFishingRodCatalog::fetchRemoteCatalogAndApply);
    }

    public static void clientTickRefreshMainHandIfSkyblock(Minecraft mc) {
        if (mc == null || mc.player == null || mc.level == null) {
            return;
        }
        if (!LocateUtils.inSkyBlock(mc)) {
            return;
        }
        mainHandHoldsHypixelFishingRod = itemMatchesCatalogRod(mc.player.getMainHandItem());
    }

    public static boolean mainHandHoldsHypixelFishingRod() {
        return mainHandHoldsHypixelFishingRod;
    }

    public static synchronized boolean itemMatchesCatalogRod(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String id = readExtraAttributesItemId(stack);
        if (id.isEmpty()) {
            return false;
        }
        return LAVA_ROD_IDS.contains(id) || WATER_ROD_IDS.contains(id);
    }

    private static String readExtraAttributesItemId(ItemStack stack) {
        try {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) {
                return "";
            }
            CompoundTag root = customData.copyTag();
            if (root == null || !root.contains("id")) {
                return "";
            }
            return root.getString("id").orElse("");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static void fetchRemoteCatalogAndApply() {
        String body = httpGet(PUBLIC_ITEMS_JSON);
        if (body == null || body.isBlank()) {
            return;
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(body).getAsJsonObject();
        } catch (Throwable ignored) {
            return;
        }
        Set<String> newLava = new HashSet<>();
        Set<String> newWater = new HashSet<>();
        if (!parseIdArray(root.get("lava_fishing_rods"), newLava)
            || !parseIdArray(root.get("water_fishing_rods"), newWater)
            || newLava.isEmpty()
            || newWater.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> replaceCatalogIds(newLava, newWater));
        } else {
            replaceCatalogIds(newLava, newWater);
        }
    }

    private static boolean parseIdArray(JsonElement el, Set<String> out) {
        if (el == null || !el.isJsonArray()) {
            return false;
        }
        JsonArray arr = el.getAsJsonArray();
        for (JsonElement e : arr) {
            if (e != null && e.isJsonPrimitive()) {
                String s = e.getAsString();
                if (s != null && !s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return true;
    }

    private static synchronized void replaceCatalogIds(Set<String> lava, Set<String> water) {
        LAVA_ROD_IDS.clear();
        WATER_ROD_IDS.clear();
        LAVA_ROD_IDS.addAll(lava);
        WATER_ROD_IDS.addAll(water);
    }

    private static String httpGet(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) URI.create(url).toURL().openConnection();
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setRequestProperty("User-Agent", "Baity");
            int code = c.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }
}