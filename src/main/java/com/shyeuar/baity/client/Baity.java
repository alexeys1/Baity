package com.shyeuar.baity.client;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.ClickGui;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.managers.KeybindManager;
import com.shyeuar.baity.managers.ModuleInitializer;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.items.CustomTotemItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import com.shyeuar.baity.features.RadialMenu;
import com.shyeuar.baity.utils.SoundUtils;

@Environment(EnvType.CLIENT)
public class Baity implements ClientModInitializer {
    
    private static long lastKeyPressTime = 0;
    public static boolean openGuiNextTick = false;

    @SuppressWarnings("deprecation")
    @Override
    public void onInitializeClient() {
        CustomTotemItem.register();
        
        ConfigManager.loadConfig();

        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }
        
        ModuleInitializer.initializeModules();
        
        registerCustomSounds();
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long windowHandle = client.getWindow().getHandle();
            
            if (openGuiNextTick) {
                openGuiNextTick = false;
                MinecraftClient.getInstance().setScreen(new ClickGui());
                return;
            }
            
            if (client.currentScreen == null && ConfigManager.guiKeyCode != 0) {
                boolean currentGuiKeyState = KeyMappingUtils.isKeyPressed(windowHandle, ConfigManager.guiKeyCode);
                if (currentGuiKeyState) {
                    if (System.currentTimeMillis() - lastKeyPressTime > 200) {
                        MinecraftClient.getInstance().setScreen(new ClickGui());
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

        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.PlayerESPRenderer());
        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.FancyDmgSplash());
        
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            RadialMenu.render(context, MinecraftClient.getInstance());
        });
    }
    
    public static final net.minecraft.sound.SoundEvent LAUGHTER_SOUND = registerSoundEvent("sounds.laughter");
    
    private static net.minecraft.sound.SoundEvent registerSoundEvent(String name) {
        net.minecraft.util.Identifier identifier = net.minecraft.util.Identifier.of("baity", name);
        return net.minecraft.registry.Registry.register(net.minecraft.registry.Registries.SOUND_EVENT, identifier, net.minecraft.sound.SoundEvent.of(identifier));
    }
    
    private void registerCustomSounds() {
        SoundUtils.registerSounds();
    }

}
