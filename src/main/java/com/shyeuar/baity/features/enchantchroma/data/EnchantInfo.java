package com.shyeuar.baity.features.enchantchroma.data;

import com.google.gson.annotations.Expose;

public class EnchantInfo {
    @Expose
    public String nbtName = "";

    @Expose
    public String loreName = "";

    @Expose
    public int goodLevel = 0;

    @Expose
    public int maxLevel = 0;

    public boolean isUltimate() {
        return false;
    }
}
