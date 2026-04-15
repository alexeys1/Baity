package com.shyeuar.baity.client;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.ClickGui;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.gui.smol.SmolFriendsScreen;
import com.shyeuar.baity.managers.KeybindManager;
import com.shyeuar.baity.managers.ModuleInitializer;
import com.shyeuar.baity.sync.BaityPresenceSync;
import com.shyeuar.baity.utils.KeyMappingUtils;
import com.shyeuar.baity.items.CustomTotemItem;
import com.shyeuar.baity.features.fancydmgsplash.FancyDmgSplash;
import com.shyeuar.baity.features.smolpeople.SmolFriendManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import com.shyeuar.baity.features.radialmenu.RadialMenu;
import com.shyeuar.baity.utils.SoundUtils;

@Environment(EnvType.CLIENT)
public class Baity implements ClientModInitializer {
    
    private static long lastKeyPressTime = 0;
    public static boolean openGuiNextTick = false;
    public static boolean openSmolFriendsNextTick = false;

    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        com.shyeuar.baity.config.BaityConfigDir.init();
        CustomTotemItem.register();
        
        ConfigManager.loadConfig();
        SmolFriendManager.reloadFromConfig();

        if (ModuleManager.getModules().isEmpty()) {
            ModuleManager.init();
        }
        
        ModuleInitializer.initializeModules();
        BaityPresenceSync.init();
        
        com.shyeuar.baity.features.fishing.FishHookTimer.init();
        com.shyeuar.baity.features.chat.ChatChannelSwitcher.init();
        
        registerCustomSounds();
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long windowHandle = client.getWindow().handle();
            ConfigManager.flushPendingSave();
            
            if (openGuiNextTick) {
                openGuiNextTick = false;
                Minecraft.getInstance().setScreen(new ClickGui());
                return;
            }

            if (openSmolFriendsNextTick) {
                openSmolFriendsNextTick = false;
                Minecraft.getInstance().setScreen(new SmolFriendsScreen(null));
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
            BaityPresenceSync.tick();
            
            com.shyeuar.baity.features.fishing.FishHookTimer.getInstance().tick();
        });
        
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            com.shyeuar.baity.sync.SyncCommands.register(dispatcher)
        );

        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.NametagRenderer());
        WorldRenderEvents.AFTER_ENTITIES.register(new FancyDmgSplash());
        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.highlights.ShulkerHighlights());
        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.highlights.InvisibleBugHighlights());
        
        WorldRenderEvents.AFTER_ENTITIES.register(new com.shyeuar.baity.features.FancyCreeperVeil());
        
        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            var timer = com.shyeuar.baity.features.fishing.FishHookTimer.getInstance();
            if (timer.shouldRender()) timer.render(guiGraphics, 0.0f);
        });
    }
    
    public static final net.minecraft.sounds.SoundEvent LAUGHTER_SOUND = registerSoundEvent("sounds.laughter");
    
    private static net.minecraft.sounds.SoundEvent registerSoundEvent(String name) {
        net.minecraft.resources.Identifier identifier = net.minecraft.resources.Identifier.fromNamespaceAndPath("baity", name);
        return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT, identifier, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(identifier));
    }
    
    private void registerCustomSounds() {
        SoundUtils.registerSounds();
    }

}
