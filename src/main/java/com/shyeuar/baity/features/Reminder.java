package com.shyeuar.baity.features;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.DurationParseUtils;
import com.shyeuar.baity.utils.LocateUtils;
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

    private static final int GOD_POTION_WARN_MINUTES = 30;
    private static final int COOKIE_SB_POLL_SECONDS = 30;

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

    private boolean cookieAlreadyNotified = false;
    private boolean godPotionAlreadyNotified = false;
    private boolean previouslyInSkyBlock = false;

    private int skyBlockPresenceTaskId = -1;
    private int cookieSbPollTaskId = -1;
    private int godPotionNotifyTaskId = -1;
    private int katNotifyTaskId = -1;

    public static Reminder getInstance() {
        if (instance == null) {
            instance = new Reminder();
        }
        return instance;
    }

    public static void init() {
        Reminder reminder = getInstance();
        if (reminder != null) {
            reminder.startSkyBlockPresenceWatcher();
            reminder.rescheduleKatNotification();
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

    private void startSkyBlockPresenceWatcher() {
        if (skyBlockPresenceTaskId != -1) {
            return;
        }
        skyBlockPresenceTaskId = TickSchedulerUtils.getInstance().runRepeating(this::onSkyBlockPresenceTick, 2, TimeUnit.SECONDS);
    }

    private void onSkyBlockPresenceTick() {
        boolean currentlyInSkyBlock = isInSkyBlock();
        if (currentlyInSkyBlock && !previouslyInSkyBlock) {
            cookieAlreadyNotified = false;
            godPotionAlreadyNotified = false;
            onSkyBlockEnter();
        } else if (!currentlyInSkyBlock && previouslyInSkyBlock) {
            onSkyBlockLeave();
        }
        previouslyInSkyBlock = currentlyInSkyBlock;
    }

    private void onSkyBlockEnter() {
        onKatSkyBlockConnect();
        tryCheckCookieReminder();
        startCookieSbPoll();
        rescheduleGodPotionNotification();
    }

    private void onSkyBlockLeave() {
        cancelCookieSbPoll();
        cancelGodPotionNotification();
    }

    private void startCookieSbPoll() {
        cancelCookieSbPoll();
        if (!isCookieReminderEnabled()) {
            return;
        }
        cookieSbPollTaskId = TickSchedulerUtils.getInstance().runRepeating(() -> {
            if (!isInSkyBlock()) {
                cancelCookieSbPoll();
                return;
            }
            tryCheckCookieReminder();
        }, COOKIE_SB_POLL_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelCookieSbPoll() {
        if (cookieSbPollTaskId != -1) {
            TickSchedulerUtils.getInstance().cancelTask(cookieSbPollTaskId);
            cookieSbPollTaskId = -1;
        }
    }

    private void tryCheckCookieReminder() {
        if (!isCookieReminderActive()) {
            return;
        }
        if (cookieAlreadyNotified) {
            return;
        }

        String tabFooter = getTabFooterText();
        if (tabFooter == null || !tabFooter.contains("Cookie Buff")) {
            return;
        }

        if (tabFooter.contains("Not active! Obtain booster cookies from the community")) {
            cookieAlreadyNotified = true;
            sendCookieNotification();
        }
    }

    private void cancelGodPotionNotification() {
        if (godPotionNotifyTaskId != -1) {
            TickSchedulerUtils.getInstance().cancelTask(godPotionNotifyTaskId);
            godPotionNotifyTaskId = -1;
        }
    }

    private Integer parseGodPotionRemainingMinutes() {
        String tabFooter = getTabFooterText();
        if (tabFooter == null || !tabFooter.contains("God Potion")) {
            return null;
        }

        Matcher matcher = GOD_POTION_PATTERN.matcher(tabFooter);
        if (!matcher.find()) {
            return null;
        }

        int timeValue = Integer.parseInt(matcher.group(1));
        String timeUnit = matcher.group(2).toLowerCase();
        return convertToMinutes(timeValue, timeUnit);
    }

    private void rescheduleGodPotionNotification() {
        cancelGodPotionNotification();
        if (!isGodPotionReminderActive()) {
            return;
        }
        if (godPotionAlreadyNotified) {
            return;
        }

        Integer remainingMinutes = parseGodPotionRemainingMinutes();
        if (remainingMinutes == null) {
            return;
        }

        if (remainingMinutes <= GOD_POTION_WARN_MINUTES) {
            godPotionAlreadyNotified = true;
            sendGodPotionNotification(remainingMinutes);
            return;
        }

        long delayMs = (long) (remainingMinutes - GOD_POTION_WARN_MINUTES) * 60_000L;
        godPotionNotifyTaskId = TickSchedulerUtils.getInstance().runLaterMillis(() -> {
            godPotionNotifyTaskId = -1;
            if (!isGodPotionReminderActive() || godPotionAlreadyNotified) {
                return;
            }

            Integer currentRemaining = parseGodPotionRemainingMinutes();
            if (currentRemaining == null) {
                return;
            }
            if (currentRemaining <= GOD_POTION_WARN_MINUTES) {
                godPotionAlreadyNotified = true;
                sendGodPotionNotification(currentRemaining);
            } else {
                rescheduleGodPotionNotification();
            }
        }, delayMs);
    }

    private void cancelKatNotification() {
        if (katNotifyTaskId != -1) {
            TickSchedulerUtils.getInstance().cancelTask(katNotifyTaskId);
            katNotifyTaskId = -1;
        }
    }

    private void rescheduleKatNotification() {
        cancelKatNotification();
        if (ConfigManager.reminderKatReadyAtMs <= 0L) {
            return;
        }
        if (ConfigManager.reminderKatPetName == null || ConfigManager.reminderKatPetName.isEmpty()) {
            return;
        }

        long delayMs = ConfigManager.reminderKatReadyAtMs - System.currentTimeMillis();
        if (delayMs <= 0L) {
            return;
        }

        katNotifyTaskId = TickSchedulerUtils.getInstance().runLaterMillis(() -> {
            katNotifyTaskId = -1;
            if (ConfigManager.reminderKatReadyAtMs > System.currentTimeMillis()) {
                rescheduleKatNotification();
                return;
            }
            trySendKatNotification();
        }, delayMs);
    }

    private void trySendKatNotification() {
        if (!isKatReminderActive() || !isKatUpgradeReady()) {
            return;
        }
        sendKatNotification();
    }

    private void onKatSkyBlockConnect() {
        if (ConfigManager.reminderKatReadyAtMs <= 0L) {
            return;
        }
        if (ConfigManager.reminderKatReadyAtMs > System.currentTimeMillis()) {
            rescheduleKatNotification();
            return;
        }
        TickSchedulerUtils.getInstance().runLater(this::trySendKatNotification, 10);
    }

    private void handleKatChat(String raw) {
        if (!isReminderModuleEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!LocateUtils.isHub(client)) {
            return;
        }
        if (raw == null || !raw.contains("[NPC] Kat:")) {
            return;
        }

        String text = LocateUtils.toPlainText(raw);
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
        ConfigManager.saveConfig();
    }

    private void setKatReadyAtFromDuration(String durationText) {
        long seconds = DurationParseUtils.parseLongDurationToSeconds(durationText);
        if (seconds <= 0L) {
            return;
        }
        ConfigManager.reminderKatReadyAtMs = System.currentTimeMillis() + seconds * 1000L;
        ConfigManager.saveConfig();
        rescheduleKatNotification();
    }

    private void reduceKatReadyAt(long millis) {
        if (ConfigManager.reminderKatReadyAtMs <= 0L) {
            return;
        }
        ConfigManager.reminderKatReadyAtMs -= millis;
        if (ConfigManager.reminderKatReadyAtMs > System.currentTimeMillis()) {
            ConfigManager.saveConfig();
            rescheduleKatNotification();
            return;
        }
        ConfigManager.reminderKatReadyAtMs = System.currentTimeMillis();
        ConfigManager.saveConfig();
        cancelKatNotification();
        trySendKatNotification();
    }

    private void clearKatUpgrade() {
        ConfigManager.reminderKatPetName = "";
        ConfigManager.reminderKatReadyAtMs = 0L;
        ConfigManager.saveConfig();
        cancelKatNotification();
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
            cancelCookieSbPoll();
        } else if (isInSkyBlock()) {
            cookieAlreadyNotified = false;
            tryCheckCookieReminder();
            startCookieSbPoll();
        }
    }

    public boolean isGodPotionReminderEnabled() {
        return ConfigManager.reminderGodPotionEnabled;
    }

    public void setGodPotionReminderEnabled(boolean enabled) {
        ConfigManager.reminderGodPotionEnabled = enabled;
        if (!enabled) {
            godPotionAlreadyNotified = false;
            cancelGodPotionNotification();
        } else if (isInSkyBlock()) {
            godPotionAlreadyNotified = false;
            rescheduleGodPotionNotification();
        }
    }

    public boolean isKatReminderEnabled() {
        return ConfigManager.reminderKatEnabled;
    }

    public void setKatReminderEnabled(boolean enabled) {
        ConfigManager.reminderKatEnabled = enabled;
        if (!enabled) {
            cancelKatNotification();
        } else {
            rescheduleKatNotification();
            if (isInSkyBlock()) {
                onKatSkyBlockConnect();
            }
        }
    }
}
