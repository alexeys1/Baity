package com.shyeuar.baity.gui.module;

import com.shyeuar.baity.gui.value.ModuleCategory;
import com.shyeuar.baity.gui.value.Option;
import com.shyeuar.baity.gui.value.ButtonValue;
import com.shyeuar.baity.gui.sync.ConfigSynchronizer;
import com.shyeuar.baity.gui.tooltip.TooltipManager;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.utils.MessageUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ModuleManager {
    private static final ArrayList<Module> modules = new ArrayList<>();
    
    public static void registerModule(Module module) {
        modules.add(module);
    }
    
    public static void init() {
        initTooltips();
        
        ModuleRegistry.registerModuleWithValues(
            "BlockAnimation", "BlockAnimation", ModuleCategory.FUN,
            () -> ConfigManager.blockAnimationMode,
            val -> ConfigManager.blockAnimationMode = val,
            new Option[]{
                new Option("slowdown", "slowdown", false, ModuleCategory.FUN)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "slowdown",
                    () -> ConfigManager.blockAnimationSlowdown,
                    val -> ConfigManager.blockAnimationSlowdown = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "CustomHandHolding", "CustomHandHolding", ModuleCategory.FUN,
            () -> ConfigManager.customHandHoldingEnabled,
            val -> ConfigManager.customHandHoldingEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new com.shyeuar.baity.gui.value.SliderValue("position x", "position x", 0, -2.5, 1.5, 0.05, ModuleCategory.FUN),
                new com.shyeuar.baity.gui.value.SliderValue("position y", "position y", 0, -1.5, 1.5, 0.05, ModuleCategory.FUN),
                new com.shyeuar.baity.gui.value.SliderValue("position z", "position z", 0, -1.5, 3.0, 0.05, ModuleCategory.FUN),
                ((com.shyeuar.baity.gui.value.SliderValue) new com.shyeuar.baity.gui.value.SliderValue("rotation x", "rotation x", 0, -180, 180, 1, ModuleCategory.FUN)).setNeedsSeparator(true),
                new com.shyeuar.baity.gui.value.SliderValue("rotation y", "rotation y", 0, -180, 180, 1, ModuleCategory.FUN),
                new com.shyeuar.baity.gui.value.SliderValue("rotation z", "rotation z", 0, -180, 180, 1, ModuleCategory.FUN),
                ((com.shyeuar.baity.gui.value.SliderValue) new com.shyeuar.baity.gui.value.SliderValue("scale", "size", 1, 0.1, 3.0, 0.05, ModuleCategory.FUN)).setNeedsSeparator(true),
                new com.shyeuar.baity.gui.value.SliderValue("swing duration", "swing duration", 6, 1, 20, 1, ModuleCategory.FUN),
                new com.shyeuar.baity.gui.value.Option("no swing", "no swing", false, ModuleCategory.FUN)
            },
                new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "position x",
                    () -> ConfigManager.customHandHoldingPosX,
                    val -> ConfigManager.customHandHoldingPosX = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "position y",
                    () -> ConfigManager.customHandHoldingPosY,
                    val -> ConfigManager.customHandHoldingPosY = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "position z",
                    () -> ConfigManager.customHandHoldingPosZ,
                    val -> ConfigManager.customHandHoldingPosZ = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rotation x",
                    () -> ConfigManager.customHandHoldingRotX,
                    val -> ConfigManager.customHandHoldingRotX = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rotation y",
                    () -> ConfigManager.customHandHoldingRotY,
                    val -> ConfigManager.customHandHoldingRotY = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "rotation z",
                    () -> ConfigManager.customHandHoldingRotZ,
                    val -> ConfigManager.customHandHoldingRotZ = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "scale",
                    () -> ConfigManager.customHandHoldingScale,
                    val -> ConfigManager.customHandHoldingScale = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "swing duration",
                    () -> ConfigManager.swingDuration,
                    val -> ConfigManager.swingDuration = ((Number) val).doubleValue()
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "no swing",
                    () -> ConfigManager.customHandHoldingNoSwing,
                    val -> ConfigManager.customHandHoldingNoSwing = (Boolean) val
                )
            }
        );
        
        Module pepCatModule = ModuleRegistry.registerSimpleModule(
            "PepCat", "PepCat", ModuleCategory.FUN,
            () -> ConfigManager.pepCatEnabled,
            val -> ConfigManager.pepCatEnabled = val
        );
        pepCatModule.setEnabled(true);
        
        ModuleRegistry.registerModuleWithValues(
            "SmolPeople", "SmolPeople", ModuleCategory.FUN,
            () -> ConfigManager.smolpeopleMode,
            val -> ConfigManager.smolpeopleMode = val,
            new Option[]{
                new Option("crosshair", "crosshair", true, ModuleCategory.FUN)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "crosshair",
                    () -> ConfigManager.crosshairMode,
                    val -> ConfigManager.crosshairMode = (Boolean) val
                )
            }
        );
        
        Module noSwimChangeModule = ModuleRegistry.registerModuleWithValues(
            "NoSwimChange", "NoSwimChange", ModuleCategory.QOL,
            () -> ConfigManager.noSwimChangeEnabled,
            val -> ConfigManager.noSwimChangeEnabled = val,
            new Option[]{
                new Option("disable swim pose", "disable swim pose", true, ModuleCategory.QOL),
                new Option("disable swim eye height", "disable swim eye height", true, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "disable swim pose",
                    () -> ConfigManager.noSwimChangeDisablePose,
                    val -> ConfigManager.noSwimChangeDisablePose = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "disable swim eye height",
                    () -> ConfigManager.noSwimChangeDisableEyeHeight,
                    val -> ConfigManager.noSwimChangeDisableEyeHeight = (Boolean) val
                )
            }
        );
        noSwimChangeModule.setEnabled(true);
        
        ModuleRegistry.registerModuleWithValues(
            "Muffler", "Muffler", ModuleCategory.QOL,
            () -> ConfigManager.mufflerEnabled,
            val -> ConfigManager.mufflerEnabled = val,
            new Option[]{
                new Option("mute enderman scream", "mute enderman scream", true, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "mute enderman scream",
                    () -> ConfigManager.mufflerMuteEndermanScream,
                    val -> ConfigManager.mufflerMuteEndermanScream = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "RadialMenu", "RadialMenu", ModuleCategory.QOL,
            () -> ConfigManager.radialMenuEnabled,
            val -> ConfigManager.radialMenuEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new ButtonValue("keybind", "keybind", 4, ModuleCategory.QOL, ButtonValue.ButtonValueType.KEYBIND, false)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "keybind",
                    () -> ConfigManager.radialMenuKeybind,
                    val -> ConfigManager.radialMenuKeybind = ((Number) val).intValue()
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Reminder", "Reminder", ModuleCategory.QOL,
            () -> ConfigManager.reminderEnabled,
            val -> ConfigManager.reminderEnabled = val,
            new Option[]{
                new Option("cookie buff reminder", "cookie buff reminder", true, ModuleCategory.QOL),
                new Option("god potion reminder", "god potion reminder", true, ModuleCategory.QOL),
                new Option("meowalert", "meowalert", true, ModuleCategory.QOL)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "cookie buff reminder",
                    () -> ConfigManager.cookieBuffReminderEnabled,
                    val -> ConfigManager.cookieBuffReminderEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "god potion reminder",
                    () -> ConfigManager.godPotionReminderEnabled,
                    val -> ConfigManager.godPotionReminderEnabled = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "meowalert",
                    () -> ConfigManager.meowAlertEnabled,
                    val -> ConfigManager.meowAlertEnabled = (Boolean) val,
                    () -> com.shyeuar.baity.features.Reminder.updateSettings()
                )
            }
        );
        
        Module clickGUI = new Module("ClickGUI", "ClickGUI", ModuleCategory.HUD);
        clickGUI.setEnabled(true);
        registerModule(clickGUI);
        ConfigSynchronizer.registerModuleConfig(
            "ClickGUI",
            () -> ConfigManager.guiEnabled,
            val -> ConfigManager.guiEnabled = val
        );
        

        ModuleRegistry.registerSimpleModule(
            "3DSkins", "3DSkins", ModuleCategory.RENDER,
            () -> ConfigManager.skinLayer3DEnabled,
            val -> ConfigManager.skinLayer3DEnabled = val
        );
        
        ModuleRegistry.registerModuleWithValues(
            "Culling", "Culling", ModuleCategory.RENDER,
            () -> ConfigManager.cullingEnabled,
            val -> ConfigManager.cullingEnabled = val,
            new Option[]{
                new Option("hide dying mob", "hide dying mob", false, ModuleCategory.RENDER),
                new Option("hide non-starred mob nametag", "hide non-starred mob nametag", false, ModuleCategory.RENDER),
                new Option("remove underwater fog", "remove underwater fog", false, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "hide dying mob",
                    () -> ConfigManager.cullingHideDyingMob,
                    val -> ConfigManager.cullingHideDyingMob = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "hide non-starred mob nametag",
                    () -> ConfigManager.cullingHideNonStarredNametag,
                    val -> ConfigManager.cullingHideNonStarredNametag = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "remove underwater fog",
                    () -> ConfigManager.cullingRemoveUnderwaterFog,
                    val -> ConfigManager.cullingRemoveUnderwaterFog = (Boolean) val
                )
            }
        );
        
        ModuleRegistry.registerModuleWithValues(
            "FancyDmgSplash", "FancyDmgSplash", ModuleCategory.RENDER,
            () -> ConfigManager.fancyDmgSplashEnabled,
            val -> ConfigManager.fancyDmgSplashEnabled = val,
            new com.shyeuar.baity.gui.value.Value[]{
                new Option("genshin elemental reaction", "genshin elemental reaction", false, ModuleCategory.RENDER),
                new com.shyeuar.baity.gui.value.ColorPaletteValue("color palette", "color palette", ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "genshin elemental reaction",
                    () -> ConfigManager.fancyDmgSplashGenshinReaction,
                    val -> ConfigManager.fancyDmgSplashGenshinReaction = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "color palette",
                    () -> ConfigManager.fancyDmgSplashColorPalette,
                    val -> ConfigManager.fancyDmgSplashColorPalette = ((Number) val).intValue()
                )
            }
        );
        
        ModuleRegistry.registerSimpleModule(
            "NoHurtCam", "NoHurtCam", ModuleCategory.RENDER,
            () -> ConfigManager.noHurtCamEnabled,
            val -> ConfigManager.noHurtCamEnabled = val
        );
        
        ModuleRegistry.registerModuleWithValues(
            "PlayerESP", "PlayerESP", ModuleCategory.RENDER,
            () -> ConfigManager.playerEspEnabled,
            val -> ConfigManager.playerEspEnabled = val,
            new Option[]{
                new Option("show distance", "show distance", true, ModuleCategory.RENDER),
                new Option("show own nametag", "show own nametag", false, ModuleCategory.RENDER)
            },
            new ModuleRegistry.ValueConfigInfo[]{
                new ModuleRegistry.ValueConfigInfo(
                    "show distance",
                    () -> ConfigManager.playerEspShowDistance,
                    val -> ConfigManager.playerEspShowDistance = (Boolean) val
                ),
                new ModuleRegistry.ValueConfigInfo(
                    "show own nametag",
                    () -> ConfigManager.playerEspShowOwnNametag,
                    val -> ConfigManager.playerEspShowOwnNametag = (Boolean) val
                )
            }
        );
    }
    
    private static void initTooltips() {
        TooltipManager.registerTooltip("SmolPeople", "Make your character smaller and cuter", 0xFFFFFF);
        TooltipManager.registerTooltip("BlockAnimation", "Restored the blocking animation of version 1.8", 0xFFFFFF);
        TooltipManager.registerTooltip("PepCat", "Play an animation and give pep talk when you died. It's a skill issue!", 0xFFFFFF);
        TooltipManager.registerTooltip("RadialMenu", "A roulette tool that invokes a shortcut command", 0xFFFFFF);
        TooltipManager.registerTooltip("NoSwimChange", "Only disables the swimming pose and eye height change on your client.", 0xFFFFFF);
        TooltipManager.registerTooltip("meowalert", 
            MessageUtils.createColoredText("play a ", 0xFFFFFF)
                .append(MessageUtils.createColoredText("ᯠ₋ ̫ ₋.ᯄ ੭", 0xFFC0CB))
                .append(MessageUtils.createColoredText("meow~", 0xFFC0CB))
                .append(MessageUtils.createColoredText(" when you are mentioned in chat", 0xFFFFFF)));
        TooltipManager.registerTooltip("Muffler", "Disable certain annoying sounds", 0xFFFFFF);
    }
    
    public static List<Module> getModules() {
        return modules;
    }
    
    public static List<Module> getModulesByCategory(ModuleCategory category) {
        return modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public static Module getModuleByName(String name) {
        return modules.stream()
                .filter(module -> module.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
