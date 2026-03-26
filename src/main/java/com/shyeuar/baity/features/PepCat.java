package com.shyeuar.baity.features;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.MessageUtils;

@Environment(EnvType.CLIENT)
public class PepCat {
    private static boolean hasRegistered = false;
    private static float lastHealth = -1.0f; 
    private static boolean wasInWorld = false;
    private static long lastDeathTime = 0; 
    private static int lastRandomMessageIndex = -1;
    public static void init() {
        if (!hasRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                Module pepCatModule = ModuleManager.getModuleByName("PepCat");
                if (pepCatModule != null && pepCatModule.isEnabled() && ConfigManager.pepCatEnabled) {
                    LocalPlayer player = client.player;
                    if (player != null) {
                        float currentHealth = player.getHealth();
                        boolean isInWorld = client.level != null && client.player != null;
                        
                        if (wasInWorld && isInWorld) {
                            if (lastHealth < 0) {
                                lastHealth = currentHealth;
                            }
                            else if (lastHealth > 0 && currentHealth <= 0) {
                                long currentTime = System.currentTimeMillis();
                                if (currentTime - lastDeathTime > 5000) {
                                    lastDeathTime = currentTime;
                                    onPlayerDeath(player);
                                }
                            }
                        }
                        
                        wasInWorld = isInWorld;
                        lastHealth = currentHealth;
                    } else {
                        wasInWorld = false;
                        lastHealth = -1.0f; 
                    }
                }
            });
            
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                Module pepCatModule = ModuleManager.getModuleByName("PepCat");
                if (pepCatModule != null && pepCatModule.isEnabled() && ConfigManager.pepCatEnabled) {
                    if (isCurrentPlayerDeathMessage(message) && !overlay) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastDeathTime > 5000) {
                            lastDeathTime = currentTime;
                            LocalPlayer player = Minecraft.getInstance().player;
                            if (player != null) {
                                onPlayerDeath(player);
                            }
                        }
                    }
                }
            });
            
            hasRegistered = true;
        }
    }
    
    private static final String[] ENGLISH_SECOND_PERSON_DEATH_SUBSTRINGS = new String[]{
        "you died",
        "you were killed by",
        "you were slain by",
        "you were blown up by",
        "you fell into the void",
        "you fell to your death",
        "you starved to death",
        "you burned to death",
        "you were burnt to a crisp",
        "you drowned",
        "you hit the ground too hard",
        "you were shot by",
        "you were slain",
        "you were killed"
    };

    private static boolean isCurrentPlayerDeathMessage(Component message) {
        String messageText = message.getString();

        if (messageText.indexOf(':') >= 0) {
            return false;
        }

        String lower = messageText.toLowerCase(java.util.Locale.ROOT);
        for (String pattern : ENGLISH_SECOND_PERSON_DEATH_SUBSTRINGS) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    private static void onPlayerDeath(LocalPlayer player) {
        playTotemAnimation(player);
        sendEncouragementMessage(player);
    }
    
    private static void playTotemAnimation(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.level != null) {
            if (client.level instanceof net.minecraft.client.multiplayer.ClientLevel) {
                net.minecraft.client.multiplayer.ClientLevel clientWorld = (net.minecraft.client.multiplayer.ClientLevel) client.level;
                clientWorld.playSound(
                    player, 
                    player.getX(), player.getY(), player.getZ(),
                    com.shyeuar.baity.client.Baity.LAUGHTER_SOUND,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    1.0f,
                    1.0f  
                );
            } else {
                net.minecraft.client.resources.sounds.SimpleSoundInstance soundInstance = net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    com.shyeuar.baity.client.Baity.LAUGHTER_SOUND, 1.0f, 1.0f);
                client.getSoundManager().play(soundInstance);
            }
            
            ItemStack catItem = createCustomCatItem();
            client.gameRenderer.displayItemActivation(catItem);
            client.particleEngine.createTrackingEmitter(client.player, ParticleTypes.OMINOUS_SPAWNING, 10);
        }
    }
    
    private static ItemStack createCustomCatItem() {
        return new ItemStack(com.shyeuar.baity.items.CustomTotemItem.CUSTOM_TOTEM);
    }
    
    
    private static void sendEncouragementMessage(LocalPlayer player) {
        int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(4);
        if (idx == lastRandomMessageIndex && 4 > 1) {
            idx = (idx + 1) % 4;
        }
        lastRandomMessageIndex = idx;

        String text;
        String emoji;
        switch (idx) {
            case 0 -> {
                text = "阎王爷翻了翻生死簿，备注栏写了个\"菜\"";
                emoji = "( ͡° ͜ʖ ͡°)";
            }
            case 1 -> {
                text = "这次复活，建议换个脑子试试";
                emoji = "⌐■_■";
            }
            case 2 -> {
                text = "路边的乌鸦都在讨论你刚才的走位";
                emoji = "→_→";
            }
            default -> {
                text = "您的操作已加入\"反面教材\"合集";
                emoji = "(￣▽￣)";
            }
        }

        MutableComponent fullMessage = MessageUtils.createBaityPrefix()
            .append(MessageUtils.createColoredText(text, 0x00FFFF))
            .append(MessageUtils.createColoredText(emoji, 0xFF80FF));

        MessageUtils.sendCustomMessage(fullMessage);
    }
}
