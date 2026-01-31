package com.shyeuar.baity.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

@Environment(EnvType.CLIENT)
public class CustomTotemItem {
    
    public static final Item CUSTOM_TOTEM;
    
    static {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("baity", "custom_totem");
        ResourceKey<Item> registryKey = ResourceKey.create(Registries.ITEM, id);
        CUSTOM_TOTEM = Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().setId(registryKey)));
    }
    
    public static void register() {
        System.out.println("custom totem item registered: " + CUSTOM_TOTEM);
    }
}

