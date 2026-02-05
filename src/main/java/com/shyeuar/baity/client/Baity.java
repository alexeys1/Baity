package com.shyeuar.baity.client;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.ClickGui;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.managers.KeybindManager;
import com.shyeuar.baity.managers.ModuleInitializer;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.items.CustomTotemItem;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplash;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import com.shyeuar.baity.features.radialmenu.RadialMenu;
import com.shyeuar.baity.utils.SoundUtils;

@Environment(EnvType.CLIENT)
public class Baity implements ClientModInitializer {
    
    private static long lastKeyPressTime = 0;
    public static boolean openGuiNextTick = false;

    @Override
    public void onInitializeClient() {
        CustomTotemItem.register();
        
        ConfigManager.loadConfig();

        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }
        
        ModuleInitializer.initializeModules();
        
        com.shyeuar.baity.features.blockanimation.BlockAnimationManager.register();
        
        registerCustomSounds();
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long windowHandle = client.getWindow().handle();
            
            if (openGuiNextTick) {
                openGuiNextTick = false;
                Minecraft.getInstance().setScreen(new ClickGui());
                return;
            }
            
            if (client.screen == null && ConfigManager.guiKeyCode != 0) {
                boolean currentGuiKeyState = KeyMappingUtils.isKeyPressed(windowHandle, ConfigManager.guiKeyCode);
                if (currentGuiKeyState) {
                    if (System.currentTimeMillis() - lastKeyPressTime > 200) {
                        Minecraft.getInstance().setScreen(new ClickGui());
                        lastKeyPressTime = System.currentTimeMillis();
                    }
                }
            }
            
            KeybindManager.handleModuleKeybinds(client, windowHandle);
            
            RadialMenu.tick(client);
        });
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("baity")
            .executes(context -> {
                openGuiNextTick = true;
                return 1;
            })));

        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.NametagRenderer());
        WorldRenderEvents.AFTER_ENTITIES.register(new FancyDmgSplash());
        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.highlights.ShulkerHighlights());
        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.highlights.InvisibleBugHighlights());
        
        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.FancyCreeperVeil());
    }
    
    public static final net.minecraft.sounds.SoundEvent LAUGHTER_SOUND = registerSoundEvent("sounds.laughter");
    
    private static net.minecraft.sounds.SoundEvent registerSoundEvent(String name) {
        net.minecraft.resources.ResourceLocation identifier = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("baity", name);
        return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT, identifier, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(identifier));
    }
    
    private void registerCustomSounds() {
        SoundUtils.registerSounds();
    }

}
