package com.shyeuar.baity.config;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static boolean smolpeopleMode = false;
    public static boolean blockAnimationMode = false;
    public static boolean blockAnimationInteractAnimations = true;
    public static boolean blockAnimationNoReequipWhenUsing = true;
    public static boolean blockAnimationSlowdown = false;
    public static boolean crosshairMode = true;
    public static boolean guiEnabled = true;
    public static int guiKeyCode = 345;
    public static boolean playerEspEnabled = false;
    public static boolean playerEspShowDistance = true;
    public static boolean playerEspShowOwnNametag = false;
    public static boolean pepCatEnabled = true;
    public static boolean reminderEnabled = false;
    public static boolean cookieBuffReminderEnabled = true;
    public static boolean godPotionReminderEnabled = true;
    public static boolean meowAlertEnabled = true;
    
    public static boolean customHandHoldingEnabled = false;
    public static double swingDuration = 6;
    public static double customHandHoldingPosX = 0;
    public static double customHandHoldingPosY = 0;
    public static double customHandHoldingPosZ = 0;
    public static double customHandHoldingRotX = 0;
    public static double customHandHoldingRotY = 0;
    public static double customHandHoldingRotZ = 0;
    public static double customHandHoldingScale = 1.0; 
    public static boolean customHandHoldingNoSwing = false;
    
    public static boolean radialMenuEnabled = true;
    public static int radialMenuKeybind = 4;
    
    public static boolean fancyDmgSplashEnabled = true;
    public static boolean fancyDmgSplashGenshinReaction = false;
    public static int fancyDmgSplashColorPalette = 0;
    
    public static boolean antiSwimEnabled = true;
    public static boolean antiSwimDisablePose = true;
    public static boolean antiSwimDisableEyeHeight = true; 
    
    public static boolean cullingEnabled = false;
    public static boolean cullingHideDyingMob = false;
    public static boolean cullingHideNonStarredNametag = false;
    public static boolean cullingRemoveUnderwaterFog = false;
    
    public static boolean skinLayer3DEnabled = false;
    
    public static boolean noHurtCamEnabled = false;
    
    public static boolean mufflerEnabled = false;
    public static boolean mufflerMuteEndermanScream = true;
    
    private static final String BAITY_DIR = "baity";
    private static final String CONFIG_FILE = "baity/config.txt";

    private static final Map<String, SettingField> CONFIG_FIELDS = new HashMap<>();
    
    static {
        registerField("SmolPeople", Boolean.class, 
            c -> ConfigManager.smolpeopleMode, 
            (c, v) -> ConfigManager.smolpeopleMode = (Boolean) v);
        registerField("BlockAnimation", Boolean.class,
            c -> ConfigManager.blockAnimationMode,
            (c, v) -> ConfigManager.blockAnimationMode = (Boolean) v);
        registerField("BlockAnimationInteractAnimations", Boolean.class,
            c -> ConfigManager.blockAnimationInteractAnimations,
            (c, v) -> ConfigManager.blockAnimationInteractAnimations = (Boolean) v);
        registerField("BlockAnimationNoReequipWhenUsing", Boolean.class,
            c -> ConfigManager.blockAnimationNoReequipWhenUsing,
            (c, v) -> ConfigManager.blockAnimationNoReequipWhenUsing = (Boolean) v);
        registerField("BlockAnimationSlowdown", Boolean.class,
            c -> ConfigManager.blockAnimationSlowdown,
            (c, v) -> ConfigManager.blockAnimationSlowdown = (Boolean) v);
        registerField("Crosshair", Boolean.class,
            c -> ConfigManager.crosshairMode,
            (c, v) -> ConfigManager.crosshairMode = (Boolean) v);
        registerField("ClickGUI", Boolean.class,
            c -> ConfigManager.guiEnabled,
            (c, v) -> ConfigManager.guiEnabled = (Boolean) v);
        registerField("GuiKeyCode", Integer.class,
            c -> ConfigManager.guiKeyCode,
            (c, v) -> ConfigManager.guiKeyCode = (Integer) v);
        registerField("PlayerESP", Boolean.class,
            c -> ConfigManager.playerEspEnabled,
            (c, v) -> ConfigManager.playerEspEnabled = (Boolean) v);
        registerField("  ShowDistance", Boolean.class,
            c -> ConfigManager.playerEspShowDistance,
            (c, v) -> ConfigManager.playerEspShowDistance = (Boolean) v);
        registerField("  ShowOwnNametag", Boolean.class,
            c -> ConfigManager.playerEspShowOwnNametag,
            (c, v) -> ConfigManager.playerEspShowOwnNametag = (Boolean) v);
        registerField("PepCat", Boolean.class,
            c -> ConfigManager.pepCatEnabled,
            (c, v) -> ConfigManager.pepCatEnabled = (Boolean) v);
        registerField("Reminder", Boolean.class,
            c -> ConfigManager.reminderEnabled,
            (c, v) -> ConfigManager.reminderEnabled = (Boolean) v);
        registerField("CookieBuffReminder", Boolean.class,
            c -> ConfigManager.cookieBuffReminderEnabled,
            (c, v) -> ConfigManager.cookieBuffReminderEnabled = (Boolean) v);
        registerField("GodPotionReminder", Boolean.class,
            c -> ConfigManager.godPotionReminderEnabled,
            (c, v) -> ConfigManager.godPotionReminderEnabled = (Boolean) v);
        registerField("MeowAlert", Boolean.class,
            c -> ConfigManager.meowAlertEnabled,
            (c, v) -> ConfigManager.meowAlertEnabled = (Boolean) v);
        registerField("CustomHandHolding", Boolean.class,
            c -> ConfigManager.customHandHoldingEnabled,
            (c, v) -> ConfigManager.customHandHoldingEnabled = (Boolean) v);
        registerField("SwingDuration", Double.class,
            c -> ConfigManager.swingDuration,
            (c, v) -> ConfigManager.swingDuration = (Double) v);
        registerField("CustomHandHoldingPosX", Double.class,
            c -> ConfigManager.customHandHoldingPosX,
            (c, v) -> ConfigManager.customHandHoldingPosX = (Double) v);
        registerField("CustomHandHoldingPosY", Double.class,
            c -> ConfigManager.customHandHoldingPosY,
            (c, v) -> ConfigManager.customHandHoldingPosY = (Double) v);
        registerField("CustomHandHoldingPosZ", Double.class,
            c -> ConfigManager.customHandHoldingPosZ,
            (c, v) -> ConfigManager.customHandHoldingPosZ = (Double) v);
        registerField("CustomHandHoldingRotX", Double.class,
            c -> ConfigManager.customHandHoldingRotX,
            (c, v) -> ConfigManager.customHandHoldingRotX = (Double) v);
        registerField("CustomHandHoldingRotY", Double.class,
            c -> ConfigManager.customHandHoldingRotY,
            (c, v) -> ConfigManager.customHandHoldingRotY = (Double) v);
        registerField("CustomHandHoldingRotZ", Double.class,
            c -> ConfigManager.customHandHoldingRotZ,
            (c, v) -> ConfigManager.customHandHoldingRotZ = (Double) v);
        registerField("CustomHandHoldingScale", Double.class,
            c -> ConfigManager.customHandHoldingScale,
            (c, v) -> ConfigManager.customHandHoldingScale = (Double) v);
        registerField("CustomHandHoldingNoSwing", Boolean.class,
            c -> ConfigManager.customHandHoldingNoSwing,
            (c, v) -> ConfigManager.customHandHoldingNoSwing = (Boolean) v);
        registerField("RadialMenu", Boolean.class,
            c -> ConfigManager.radialMenuEnabled,
            (c, v) -> ConfigManager.radialMenuEnabled = (Boolean) v);
        registerField("RadialMenuKeybind", Integer.class,
            c -> ConfigManager.radialMenuKeybind,
            (c, v) -> ConfigManager.radialMenuKeybind = (Integer) v);
        registerField("FancyDmgSplash", Boolean.class,
            c -> ConfigManager.fancyDmgSplashEnabled,
            (c, v) -> ConfigManager.fancyDmgSplashEnabled = (Boolean) v);
        registerField("FancyDmgSplashGenshinReaction", Boolean.class,
            c -> ConfigManager.fancyDmgSplashGenshinReaction,
            (c, v) -> ConfigManager.fancyDmgSplashGenshinReaction = (Boolean) v);
        registerField("FancyDmgSplashColorPalette", Integer.class,
            c -> ConfigManager.fancyDmgSplashColorPalette,
            (c, v) -> ConfigManager.fancyDmgSplashColorPalette = (Integer) v);
        registerField("AntiSwim", Boolean.class,
            c -> ConfigManager.antiSwimEnabled,
            (c, v) -> ConfigManager.antiSwimEnabled = (Boolean) v);
        registerField("DisablePose", Boolean.class,
            c -> ConfigManager.antiSwimDisablePose,
            (c, v) -> ConfigManager.antiSwimDisablePose = (Boolean) v);
        registerField("DisableEyeHeight", Boolean.class,
            c -> ConfigManager.antiSwimDisableEyeHeight,
            (c, v) -> ConfigManager.antiSwimDisableEyeHeight = (Boolean) v);
        registerField("Culling", Boolean.class,
            c -> ConfigManager.cullingEnabled,
            (c, v) -> ConfigManager.cullingEnabled = (Boolean) v);
        registerField("CullingHideDyingMob", Boolean.class,
            c -> ConfigManager.cullingHideDyingMob,
            (c, v) -> ConfigManager.cullingHideDyingMob = (Boolean) v);
        registerField("CullingHideNonStarredNametag", Boolean.class,
            c -> ConfigManager.cullingHideNonStarredNametag,
            (c, v) -> ConfigManager.cullingHideNonStarredNametag = (Boolean) v);
        registerField("CullingRemoveUnderwaterFog", Boolean.class,
            c -> ConfigManager.cullingRemoveUnderwaterFog,
            (c, v) -> ConfigManager.cullingRemoveUnderwaterFog = (Boolean) v);
        registerField("3DSkins", Boolean.class,
            c -> ConfigManager.skinLayer3DEnabled,
            (c, v) -> ConfigManager.skinLayer3DEnabled = (Boolean) v);
        registerField("NoHurtCam", Boolean.class,
            c -> ConfigManager.noHurtCamEnabled,
            (c, v) -> ConfigManager.noHurtCamEnabled = (Boolean) v);
        registerField("Muffler", Boolean.class,
            c -> ConfigManager.mufflerEnabled,
            (c, v) -> ConfigManager.mufflerEnabled = (Boolean) v);
        registerField("MufflerMuteEndermanScream", Boolean.class,
            c -> ConfigManager.mufflerMuteEndermanScream,
            (c, v) -> ConfigManager.mufflerMuteEndermanScream = (Boolean) v);
    }
    
    private static void registerField(String key, Class<?> type,
                                    java.util.function.Function<ConfigManager, Object> getter,
                                    java.util.function.BiConsumer<ConfigManager, Object> setter) {
        CONFIG_FIELDS.put(key, new SettingField(key, getter, setter, type));
    }

    public static void saveConfig() {
        try {
            java.nio.file.Path baityDir = java.nio.file.Paths.get(BAITY_DIR);
            if (!java.nio.file.Files.exists(baityDir)) {
                java.nio.file.Files.createDirectories(baityDir);
            }
            
            java.nio.file.Path configPath = java.nio.file.Paths.get(CONFIG_FILE);
            StringBuilder config = new StringBuilder();
            
            ConfigManager instance = null; 
            for (Map.Entry<String, SettingField> entry : CONFIG_FIELDS.entrySet()) {
                String key = entry.getKey();
                SettingField field = entry.getValue();
                Object value = field.getValue(instance);
                config.append(key).append(":").append(value).append("\n");
            }
            
            java.nio.file.Files.write(configPath, config.toString().getBytes());
        } catch (java.io.IOException e) {
            System.err.println("Failed to save Baity config: " + e.getMessage());
        }
    }

    public static void loadConfig() {
        try {
            java.nio.file.Path baityDir = java.nio.file.Paths.get(BAITY_DIR);
            if (!java.nio.file.Files.exists(baityDir)) {
                java.nio.file.Files.createDirectories(baityDir);
            }
            
            java.nio.file.Path oldConfigPath = java.nio.file.Paths.get("baity_config.txt");
            java.nio.file.Path newConfigPath = java.nio.file.Paths.get(CONFIG_FILE);
            if (java.nio.file.Files.exists(oldConfigPath) && !java.nio.file.Files.exists(newConfigPath)) {
                java.nio.file.Files.move(oldConfigPath, newConfigPath);
                System.out.println("[Baity] Migrated config to new location: " + CONFIG_FILE);
            }
            
            if (java.nio.file.Files.exists(newConfigPath)) {
                String content = java.nio.file.Files.readString(newConfigPath).trim();
                String[] lines = content.split("\n");
                
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    ConfigManager instance = null; 
                    for (Map.Entry<String, SettingField> entry : CONFIG_FIELDS.entrySet()) {
                        String key = entry.getKey();
                        if (line.startsWith(key + ":")) {
                            SettingField field = entry.getValue();
                            String valueStr = line.substring(key.length() + 1);
                            Object value = parseValue(valueStr, field.getType());
                            field.setValue(instance, value);
                            break;
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to load Baity config: " + e.getMessage());
        }
    }
    
    private static Object parseValue(String valueStr, Class<?> type) {
        if (type == Boolean.class) {
            return Boolean.parseBoolean(valueStr);
        } else if (type == Integer.class) {
            return Integer.parseInt(valueStr);
        } else if (type == Double.class) {
            return Double.parseDouble(valueStr);
        } else if (type == String.class) {
            return valueStr;
        }
        return valueStr;
    }
}
