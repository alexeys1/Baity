package com.shyeuar.baity.features.enchantchroma.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class EnchantDatabase {
    @Expose
    @SerializedName("NORMAL")
    public Map<String, EnchantInfo> normal = new HashMap<>();

    @Expose
    @SerializedName("ULTIMATE")
    public Map<String, EnchantInfo> ultimate = new HashMap<>();

    @Expose
    @SerializedName("STACKING")
    public Map<String, EnchantInfo> stacking = new HashMap<>();

    public EnchantInfo findEnchant(String loreName) {
        String normalized = normalizeKey(loreName);
        
        EnchantInfo result = searchInMap(normal, normalized);
        if (result != null) return result;
        
        result = searchInMap(ultimate, normalized);
        if (result != null) return result;
        
        return searchInMap(stacking, normalized);
    }

    public boolean hasData() {
        return isMapNotEmpty(normal) || isMapNotEmpty(ultimate) || isMapNotEmpty(stacking);
    }

    private String normalizeKey(String key) {
        return key.toLowerCase().trim();
    }

    private EnchantInfo searchInMap(Map<String, EnchantInfo> map, String key) {
        return map != null ? map.get(key) : null;
    }

    private boolean isMapNotEmpty(Map<String, EnchantInfo> map) {
        return map != null && !map.isEmpty();
    }
}
