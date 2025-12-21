package com.shyeuar.baity.features;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import com.shyeuar.baity.utils.TickSchedulerUtils;
import com.shyeuar.baity.utils.MessageUtils;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

@Environment(EnvType.CLIENT)
public class Reminder {
    
    private static Reminder instance;
    
    private static final ItemStack COOKIE_DISPLAY_ICON = new ItemStack(Items.COOKIE);
    private static final ItemStack GOD_POTION_DISPLAY_ICON = new ItemStack(Items.POTION);
    
    private static final Pattern GOD_POTION_PATTERN = Pattern.compile(
        "You have a God Potion active! (\\d+) (Days?|Hours?|Minutes?|Mins?|Min) Use '/effects' to see the effects!"
    );
    
    private static final long MEOW_COOLDOWN = 2000;
    private static final float MEOW_VOLUME = 1.5F;
    private static final float MEOW_PITCH = 1.0F;
    
    private boolean cookieAlreadyNotified = false;
    private boolean godPotionAlreadyNotified = false;
    private boolean previouslyInSkyBlock = false;
    
    private boolean meowAlertRegistered = false;
    private long lastMeowTimestamp = 0;
    
    private int cookieSchedulerId = -1;
    private int godPotionSchedulerId = -1;
    
    public static Reminder getInstance() {
        if (instance == null) {
            instance = new Reminder();
        }
        return instance;
    }
    
    public static void init() {
        Reminder reminder = getInstance();
        if (reminder != null) {
            reminder.startCookieScheduler();
            reminder.startGodPotionScheduler();
            reminder.registerMeowAlert();
        }
    }
    
    private void startCookieScheduler() {
        cookieSchedulerId = TickSchedulerUtils.getInstance().runRepeating(this::tickCookieReminder, 5, TimeUnit.SECONDS);
    }
    
    private void startGodPotionScheduler() {
        godPotionSchedulerId = TickSchedulerUtils.getInstance().runRepeating(() -> {
            boolean currentlyInSkyBlock = isInSkyBlock();
            if (currentlyInSkyBlock && !previouslyInSkyBlock) {
                cookieAlreadyNotified = false;
                godPotionAlreadyNotified = false;
            }
            previouslyInSkyBlock = currentlyInSkyBlock;
            
            if (isInSkyBlock() && !godPotionAlreadyNotified) {
                tickGodPotionReminder();
            }
        }, 10, TimeUnit.SECONDS);
    }
    
    private void registerMeowAlert() {
        if (meowAlertRegistered) return;
        
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (sender == null) return;
            
            com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
            if (reminderModule == null || !reminderModule.isEnabled()) return;
            
            boolean meowEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "meowalert", false);
            if (!meowEnabled) return;
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            
            String playerName = client.player.getName().getString();
            String messageContent = message.getString();
            
            if (messageContainsName(messageContent, playerName)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMeowTimestamp > MEOW_COOLDOWN) {
                    lastMeowTimestamp = currentTime;
                    playMeowSound(client.player);
                }
            }
        });
        meowAlertRegistered = true;
    }
    
    private boolean messageContainsName(String message, String name) {
        if (message == null || name == null) return false;
        return message.toLowerCase().contains(name.toLowerCase());
    }
    
    private void playMeowSound(net.minecraft.client.network.ClientPlayerEntity player) {
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_CAT_AMBIENT, MEOW_VOLUME * 5.0f, MEOW_PITCH);
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, MEOW_VOLUME * 5.0f, 5.0f);
    }
    
    private void tickCookieReminder() {
        boolean currentlyInSkyBlock = isInSkyBlock();
        if (currentlyInSkyBlock && !previouslyInSkyBlock) {
            cookieAlreadyNotified = false;
            godPotionAlreadyNotified = false;
        }
        previouslyInSkyBlock = currentlyInSkyBlock;
        
        if (!isCookieReminderActive()) return;
        if (cookieAlreadyNotified) return;

        String tabFooter = getTabFooterText();
        if (tabFooter == null || !tabFooter.contains("Cookie Buff")) return;

        if (tabFooter.contains("Not active! Obtain booster cookies from the community")) {
            cookieAlreadyNotified = true;
            sendCookieNotification();
        }
    }
    
    private void tickGodPotionReminder() {
        if (!isGodPotionReminderActive()) return;
        if (godPotionAlreadyNotified) return;

        String tabFooter = getTabFooterText();
        if (tabFooter == null || !tabFooter.contains("God Potion")) return;

        Matcher matcher = GOD_POTION_PATTERN.matcher(tabFooter);
        if (matcher.find()) {
            int timeValue = Integer.parseInt(matcher.group(1));
            String timeUnit = matcher.group(2).toLowerCase();
            int remainingMinutes = convertToMinutes(timeValue, timeUnit);
            
            if (remainingMinutes <= 30) {
                godPotionAlreadyNotified = true;
                sendGodPotionNotification(remainingMinutes);
            }
        }
    }
    
    private boolean isCookieReminderActive() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null || !reminderModule.isEnabled()) return false;
        boolean subEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "cookie buff reminder", false);
        return isInSkyBlock() && subEnabled;
    }
    
    private boolean isGodPotionReminderActive() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null || !reminderModule.isEnabled()) return false;
        boolean subEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "god potion reminder", false);
        return isInSkyBlock() && subEnabled;
    }
    
    private void sendCookieNotification() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        MutableText prefix = MessageUtils.createBaityPrefix();
        MutableText message = Text.literal("You don't have a").formatted(Formatting.RED, Formatting.BOLD)
                .append(Text.literal(" Booster Cookie ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD))
                .append(Text.literal("active!").formatted(Formatting.RED, Formatting.BOLD));
        
        MessageUtils.sendCustomMessage(prefix.append(message));
        client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_BLAZE_DEATH, 1.0f, 0.75f);
        showCookieAnimation(client, client.player);
    }
    
    private void sendGodPotionNotification(int remainingMinutes) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        MutableText prefix = MessageUtils.createBaityPrefix();
        MutableText message = Text.literal("Your ").formatted(Formatting.YELLOW, Formatting.BOLD)
                .append(Text.literal("God Potion ").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("will expire in ").formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal(formatMinutes(remainingMinutes)).formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal("!").formatted(Formatting.YELLOW, Formatting.BOLD));
        
        MessageUtils.sendCustomMessage(prefix.append(message));
        client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_BLAZE_DEATH, 1.0f, 0.75f);
        showGodPotionAnimation(client, client.player);
    }
    
    private int convertToMinutes(int value, String unit) {
        switch (unit) {
            case "day":
            case "days":
                return value * 24 * 60;
            case "hour":
            case "hours":
                return value * 60;
            case "minute":
            case "minutes":
            case "min":
            case "mins":
                return value;
            default:
                return Integer.MAX_VALUE;
        }
    }
    
    private String formatMinutes(int minutes) {
        if (minutes >= 60) {
            int hours = minutes / 60;
            int remaining = minutes % 60;
            return remaining == 0 ? hours + "h" : hours + "h " + remaining + "m";
        }
        return minutes + "m";
    }
    
    private void showCookieAnimation(MinecraftClient client, net.minecraft.client.network.ClientPlayerEntity player) {
        if (client.world == null) return;
        client.gameRenderer.showFloatingItem(COOKIE_DISPLAY_ICON);
        client.particleManager.addEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
    }
    
    private void showGodPotionAnimation(MinecraftClient client, net.minecraft.client.network.ClientPlayerEntity player) {
        if (client.world == null) return;
        client.gameRenderer.showFloatingItem(GOD_POTION_DISPLAY_ICON);
        client.particleManager.addEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
    }
    
    private boolean isInSkyBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return false;
        
        if (client.isInSingleplayer()) {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        }
        
        if (client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address.toLowerCase();
            return serverAddress.contains("hypixel");
        }
        
        return false;
    }
    
    private String getTabFooterText() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null || client.inGameHud.getPlayerListHud() == null) return null;
        
        try {
            net.minecraft.text.Text footer = ((com.shyeuar.baity.mixin.PlayerListHudAccessor) client.inGameHud.getPlayerListHud()).getFooter();
            return footer != null ? footer.getString() : null;
        } catch (Exception e) {
            try {
                if (client.getNetworkHandler() != null) {
                    var playerList = client.getNetworkHandler().getPlayerList();
                    for (var entry : playerList) {
                        if (entry.getDisplayName() != null) {
                            String name = entry.getDisplayName().getString();
                            if (name.contains("Cookie Buff")) {
                                return name;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        return null;
    }
    
    public static void updateSettings() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null) return;
        
        Reminder reminder = getInstance();
        if (reminder == null) return;
        
        boolean cookieEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "cookie buff reminder", false);
        boolean godPotionEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "god potion reminder", false);
        boolean meowEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "meowalert", false);
        
        reminder.setCookieReminderEnabled(cookieEnabled);
        reminder.setGodPotionReminderEnabled(godPotionEnabled);
        reminder.setMeowAlertEnabled(meowEnabled);
    }
    
    public boolean isCookieReminderEnabled() {
        return com.shyeuar.baity.config.ConfigManager.cookieBuffReminderEnabled;
    }
    
    public void setCookieReminderEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.cookieBuffReminderEnabled = enabled;
        if (!enabled) {
            cookieAlreadyNotified = false;
            if (cookieSchedulerId != -1) {
                TickSchedulerUtils.getInstance().cancelTask(cookieSchedulerId);
                cookieSchedulerId = -1;
            }
        } else {
            if (cookieSchedulerId == -1) {
                cookieSchedulerId = TickSchedulerUtils.getInstance().runRepeating(this::tickCookieReminder, 5, TimeUnit.SECONDS);
            }
        }
    }
    
    public boolean isGodPotionReminderEnabled() {
        return com.shyeuar.baity.config.ConfigManager.godPotionReminderEnabled;
    }
    
    public void setGodPotionReminderEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.godPotionReminderEnabled = enabled;
        if (!enabled) {
            godPotionAlreadyNotified = false;
            if (godPotionSchedulerId != -1) {
                TickSchedulerUtils.getInstance().cancelTask(godPotionSchedulerId);
                godPotionSchedulerId = -1;
            }
        } else {
            if (godPotionSchedulerId == -1) {
                godPotionSchedulerId = TickSchedulerUtils.getInstance().runRepeating(() -> {
                    if (isInSkyBlock() && !godPotionAlreadyNotified) {
                        tickGodPotionReminder();
                    }
                }, 10, TimeUnit.SECONDS);
            }
        }
    }
    
    public boolean isMeowAlertEnabled() {
        return com.shyeuar.baity.config.ConfigManager.meowAlertEnabled;
    }
    
    public void setMeowAlertEnabled(boolean enabled) {
        com.shyeuar.baity.config.ConfigManager.meowAlertEnabled = enabled;
    }
}
