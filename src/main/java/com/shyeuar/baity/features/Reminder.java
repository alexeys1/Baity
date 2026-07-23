package com.shyeuar.baity.features;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.TickSchedulerUtils;
import com.shyeuar.baity.utils.MessageUtils;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public class Reminder {

    private static Reminder instance;
    private static boolean chatRegistered;

    private static ItemStack cookieDisplayIcon;
    private static ItemStack godPotionDisplayIcon;
    private static ItemStack katDisplayIcon;

    private static ItemStack getCookieDisplayIcon() {
        if (cookieDisplayIcon == null) {
            cookieDisplayIcon = new ItemStack(Items.COOKIE);
        }
        return cookieDisplayIcon;
    }

    private static ItemStack getGodPotionDisplayIcon() {
        if (godPotionDisplayIcon == null) {
            godPotionDisplayIcon = new ItemStack(Items.POTION);
        }
        return godPotionDisplayIcon;
    }

    private static ItemStack getKatDisplayIcon() {
        if (katDisplayIcon == null) {
            katDisplayIcon = new ItemStack(Items.BONE);
        }
        return katDisplayIcon;
    }

    private static final Pattern GOD_POTION_PATTERN = Pattern.compile(
        "You have a God Potion active! (\\d+) (Days?|Hours?|Minutes?|Mins?|Min) Use '/effects' to see the effects!"
    );
    private static final Pattern KAT_GIVE_PATTERN = Pattern.compile(
        "^\\[NPC] Kat: I'll get your (.+?) upgraded to .+ in no time!$"
    );
    private static final Pattern KAT_REMIND_PATTERN = Pattern.compile(
        "^\\[NPC] Kat: I'm currently taking care of your (.+?)!$"
    );
    private static final Pattern KAT_DURATION_PATTERN = Pattern.compile(
        "^\\[NPC] Kat: Come back in (.+) to pick it up!$"
    );
    private static final Pattern KAT_DURATION_REMIND_PATTERN = Pattern.compile(
        "^\\[NPC] Kat: You can pick it up in (.+)\\.$"
    );
    private static final Pattern DURATION_PART_PATTERN = Pattern.compile(
        "(\\d+)\\s*(days?|d|hours?|h|minutes?|mins?|m|seconds?|secs?|s)\\b",
        Pattern.CASE_INSENSITIVE
    );

    private boolean cookieAlreadyNotified = false;
    private boolean godPotionAlreadyNotified = false;
    private boolean katAlreadyNotified = false;
    private boolean previouslyInSkyBlock = false;

    private int cookieSchedulerId = -1;
    private int godPotionSchedulerId = -1;
    private int katSchedulerId = -1;

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
            reminder.startKatScheduler();
            reminder.registerChatListener();
        }
    }

    private void registerChatListener() {
        if (chatRegistered) {
            return;
        }
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) {
                return;
            }
            getInstance().handleKatChat(message.getString());
        });
        chatRegistered = true;
    }

    private void startCookieScheduler() {
        cookieSchedulerId = TickSchedulerUtils.getInstance().runRepeating(this::tickCookieReminder, 5, TimeUnit.SECONDS);
    }
    private void startGodPotionScheduler() {
        godPotionSchedulerId = TickSchedulerUtils.getInstance().runRepeating(() -> {
            onSkyBlockPresenceTick();
            if (isInSkyBlock() && !godPotionAlreadyNotified) {
                tickGodPotionReminder();
            }
        }, 10, TimeUnit.SECONDS);
    }

    private void startKatScheduler() {
        katSchedulerId = TickSchedulerUtils.getInstance().runRepeating(this::tickKatReminder, 5, TimeUnit.SECONDS);
    }

    private void onSkyBlockPresenceTick() {
        boolean currentlyInSkyBlock = isInSkyBlock();
        if (currentlyInSkyBlock && !previouslyInSkyBlock) {
            cookieAlreadyNotified = false;
            godPotionAlreadyNotified = false;
            katAlreadyNotified = false;
        }
        previouslyInSkyBlock = currentlyInSkyBlock;
    }

    private void tickCookieReminder() {
        onSkyBlockPresenceTick();

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

    private void tickKatReminder() {
        onSkyBlockPresenceTick();

        if (!isKatReminderActive()) return;
        if (katAlreadyNotified) return;
        if (!isKatUpgradeReady()) return;

        katAlreadyNotified = true;
        sendKatNotification();
    }

    private void handleKatChat(String raw) {
        if (!isReminderModuleEnabled()) {
            return;
        }
        if (raw == null || !raw.contains("[NPC] Kat:")) {
            return;
        }

        String text = raw.replace('\u00A7', '&').replaceAll("&[0-9a-fk-or]", "");
        if (text.contains("[NPC] Kat: A flower? For me? How sweet!")) {
            reduceKatReadyAt(TimeUnit.DAYS.toMillis(1));
            return;
        }
        if (text.contains("[NPC] Kat: A bouquet? For me? How sweet!")) {
            reduceKatReadyAt(TimeUnit.DAYS.toMillis(5));
            return;
        }
        if (text.contains("[NPC] Kat: If you have any other pets you'd like to upgrade, you know where to find me!")) {
            clearKatUpgrade();
            return;
        }

        Matcher give = KAT_GIVE_PATTERN.matcher(text);
        if (give.find()) {
            setKatPetName(give.group(1).trim());
            return;
        }

        Matcher remind = KAT_REMIND_PATTERN.matcher(text);
        if (remind.find()) {
            setKatPetName(remind.group(1).trim());
            return;
        }

        Matcher duration = KAT_DURATION_PATTERN.matcher(text);
        if (duration.find()) {
            setKatReadyAtFromDuration(duration.group(1).trim());
            return;
        }

        Matcher durationRemind = KAT_DURATION_REMIND_PATTERN.matcher(text);
        if (durationRemind.find()) {
            setKatReadyAtFromDuration(durationRemind.group(1).trim());
        }
    }

    private void setKatPetName(String petName) {
        if (petName == null || petName.isEmpty()) {
            return;
        }
        ConfigManager.reminderKatPetName = petName;
        katAlreadyNotified = false;
        ConfigManager.saveConfig();
    }

    private void setKatReadyAtFromDuration(String durationText) {
        long seconds = parseDurationToSeconds(durationText);
        if (seconds <= 0L) {
            return;
        }
        ConfigManager.reminderKatReadyAtMs = System.currentTimeMillis() + seconds * 1000L;
        katAlreadyNotified = false;
        ConfigManager.saveConfig();
    }

    private void reduceKatReadyAt(long millis) {
        if (ConfigManager.reminderKatReadyAtMs <= 0L) {
            return;
        }
        ConfigManager.reminderKatReadyAtMs -= millis;
        katAlreadyNotified = false;
        if (ConfigManager.reminderKatReadyAtMs <= System.currentTimeMillis()) {
            ConfigManager.reminderKatReadyAtMs = System.currentTimeMillis();
        }
        ConfigManager.saveConfig();
    }

    private void clearKatUpgrade() {
        ConfigManager.reminderKatPetName = "";
        ConfigManager.reminderKatReadyAtMs = 0L;
        katAlreadyNotified = false;
        ConfigManager.saveConfig();
    }

    private boolean isKatUpgradeReady() {
        if (ConfigManager.reminderKatReadyAtMs <= 0L) {
            return false;
        }
        if (ConfigManager.reminderKatPetName == null || ConfigManager.reminderKatPetName.isEmpty()) {
            return false;
        }
        return System.currentTimeMillis() >= ConfigManager.reminderKatReadyAtMs;
    }

    private static long parseDurationToSeconds(String text) {
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        Matcher matcher = DURATION_PART_PATTERN.matcher(text);
        long total = 0L;
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            if (unit.startsWith("d")) {
                total += value * 24L * 3600L;
            } else if (unit.startsWith("h")) {
                total += value * 3600L;
            } else if (unit.startsWith("m")) {
                total += value * 60L;
            } else if (unit.startsWith("s")) {
                total += value;
            }
        }
        return matched ? total : 0L;
    }

    private boolean isReminderModuleEnabled() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        return reminderModule != null && reminderModule.isEnabled();
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

    private boolean isKatReminderActive() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null || !reminderModule.isEnabled()) return false;
        boolean subEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "kat reminder", false);
        return isInSkyBlock() && subEnabled;
    }

    private void sendCookieNotification() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        MutableComponent prefix = MessageUtils.createBaityPrefix();
        MutableComponent message = Component.literal("You don't have a").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                .append(Component.literal(" Booster Cookie ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .append(Component.literal("active!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

        MessageUtils.sendCustomMessage(prefix.append(message));
        client.player.playSound(net.minecraft.sounds.SoundEvents.BLAZE_DEATH, 1.0f, 0.75f);
        showCookieAnimation(client, client.player);
    }

    private void sendGodPotionNotification(int remainingMinutes) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        MutableComponent prefix = MessageUtils.createBaityPrefix();
        MutableComponent message = Component.literal("Your ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                .append(Component.literal("God Potion ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("will expire in ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(formatMinutes(remainingMinutes)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

        MessageUtils.sendCustomMessage(prefix.append(message));
        client.player.playSound(net.minecraft.sounds.SoundEvents.BLAZE_DEATH, 1.0f, 0.75f);
        showGodPotionAnimation(client, client.player);
    }

    private void sendKatNotification() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String petName = ConfigManager.reminderKatPetName;
        if (petName == null || petName.isEmpty()) {
            petName = "pet";
        }

        MutableComponent prefix = MessageUtils.createBaityPrefix();
        MutableComponent message = Component.literal("Your ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                .append(Component.literal(petName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" is ready at Kat!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

        MessageUtils.sendCustomMessage(prefix.append(message));
        client.player.playSound(net.minecraft.sounds.SoundEvents.BLAZE_DEATH, 1.0f, 0.75f);
        showKatAnimation(client, client.player);
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

    private void showCookieAnimation(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        if (client.level == null) return;
        client.gameRenderer.displayItemActivation(getCookieDisplayIcon());
        client.particleEngine.createTrackingEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
    }

    private void showGodPotionAnimation(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        if (client.level == null) return;
        client.gameRenderer.displayItemActivation(getGodPotionDisplayIcon());
        client.particleEngine.createTrackingEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
    }

    private void showKatAnimation(Minecraft client, net.minecraft.client.player.LocalPlayer player) {
        if (client.level == null) return;
        client.gameRenderer.displayItemActivation(getKatDisplayIcon());
        client.particleEngine.createTrackingEmitter(player, ParticleTypes.OMINOUS_SPAWNING, 10);
    }

    private boolean isInSkyBlock() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return false;

        if (client.isLocalServer()) {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        }

        return com.shyeuar.baity.utils.LocateUtils.inSkyBlock(client);
    }

    private String getTabFooterText() {
        return com.shyeuar.baity.utils.LocateUtils.getTabListFooterPlainBestEffort(Minecraft.getInstance());
    }

    public static void updateSettings() {
        com.shyeuar.baity.gui.module.Module reminderModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("Reminder");
        if (reminderModule == null) return;

        Reminder reminder = getInstance();
        if (reminder == null) return;

        boolean cookieEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "cookie buff reminder", false);
        boolean godPotionEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "god potion reminder", false);
        boolean katEnabled = com.shyeuar.baity.utils.ModuleUtils.getOptionBoolean(reminderModule, "kat reminder", false);

        reminder.setCookieReminderEnabled(cookieEnabled);
        reminder.setGodPotionReminderEnabled(godPotionEnabled);
        reminder.setKatReminderEnabled(katEnabled);
    }

    public boolean isCookieReminderEnabled() {
        return ConfigManager.reminderCookieBuffEnabled;
    }

    public void setCookieReminderEnabled(boolean enabled) {
        ConfigManager.reminderCookieBuffEnabled = enabled;
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
        return ConfigManager.reminderGodPotionEnabled;
    }

    public void setGodPotionReminderEnabled(boolean enabled) {
        ConfigManager.reminderGodPotionEnabled = enabled;
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

    public boolean isKatReminderEnabled() {
        return ConfigManager.reminderKatEnabled;
    }

    public void setKatReminderEnabled(boolean enabled) {
        ConfigManager.reminderKatEnabled = enabled;
        if (!enabled) {
            katAlreadyNotified = false;
            if (katSchedulerId != -1) {
                TickSchedulerUtils.getInstance().cancelTask(katSchedulerId);
                katSchedulerId = -1;
            }
        } else {
            if (katSchedulerId == -1) {
                katSchedulerId = TickSchedulerUtils.getInstance().runRepeating(this::tickKatReminder, 5, TimeUnit.SECONDS);
            }
        }
    }

}
