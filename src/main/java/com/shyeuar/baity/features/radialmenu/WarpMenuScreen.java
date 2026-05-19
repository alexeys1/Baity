package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.gui.internal.ClickGuiState;
import com.shyeuar.baity.gui.owo.RadialMenuComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Environment(EnvType.CLIENT)
public class WarpMenuScreen extends Screen {

    public enum WarpCategory {
        MAIN(null, "Warp Menu", "\u2302", List.of()),

        BASIC("Basic", "Basic", "\u2B50", List.of(
            new WarpDestination("Hub", "hub", "hub"),
            new WarpDestination("Community Center", "warp elizabeth", "community_center"),
            new WarpDestination("Museum", "warp museum", "museum"),
            new WarpDestination("Wizard Tower", "warp tower", "wizard_tower"),
            new WarpDestination("Private Island", "is", "private_island"),
            new WarpDestination("Castle", "warp castle", "castle"),
            new WarpDestination("Sirius' Shack", "warp da", "sirius_shack"),
            new WarpDestination("Crypts", "warp crypt", "crypts")
        )),

        PARK_BARN("Park & Barn", "Park & Barn", "\u2618", List.of(
            new WarpDestination("The Park", "warp park", "the_park"),
            new WarpDestination("Jungle", "warp jungle", "jungle"),
            new WarpDestination("Howling Cave", "warp howl", "howling_cave"),
            new WarpDestination("Murkwater Loch", "warp murkwater", "murkwater_loch"),
            new WarpDestination("Galatea", "warp galatea", "galatea"),
            new WarpDestination("The Barn", "warp barn", "the_barn"),
            new WarpDestination("Mushroom Desert", "warp desert", "mushroom_desert"),
            new WarpDestination("Trapper's Den", "warp trap", "trappers_den")
        )),

        MINING("Mining", "Mining", "\u26CF", List.of(
            new WarpDestination("Gold Mine", "warp gold", "gold_mine"),
            new WarpDestination("Dwarven Mines", "warp mines", "dwarven_mines"),
            new WarpDestination("The Forge", "warp forge", "the_forge"),
            new WarpDestination("Dwarven Base Camp", "warp gt", "dwarven_base_camp"),
            new WarpDestination("Crystal Hollows", "warp ch", "crystal_hollows"),
            new WarpDestination("Crystal Nucleus", "warp cn", "crystal_nucleus"),
            new WarpDestination("Deep Caverns", "warp deep", "deep_caverns")
        )),

        COMBAT("Combat", "Combat", "\u2694", List.of(
            new WarpDestination("Spider's Den", "warp spider", "spiders_den"),
            new WarpDestination("Spider Mound", "warp top", "spider_mound"),
            new WarpDestination("Arachne's Sanctuary", "warp arachne", "arachnes_sanctuary"),
            new WarpDestination("The End", "warp end", "the_end"),
            new WarpDestination("Dragon's Nest", "warp drag", "dragons_nest"),
            new WarpDestination("Void Sepulture", "warp void", "void_sepulture")
        )),

        CRIMSON("Crimson", "Crimson Isle", "\u2620", List.of(
            new WarpDestination("Crimson Isle", "warp isle", "crimson_isle"),
            new WarpDestination("Forgotten Skull", "warp kuudra", "forgotten_skull"),
            new WarpDestination("The Wasteland", "warp wasteland", "the_wasteland"),
            new WarpDestination("Dragontail", "warp dragontail", "dragontail"),
            new WarpDestination("Scarleton", "warp scarleton", "scarleton"),
            new WarpDestination("Smoldering Tomb", "warp smold", "smoldering_tomb")
        )),

        OTHERS("Others", "Others", "\uD83C\uDFB2", List.of(
            new WarpDestination("Dungeon Hub", "warp dh", "dungeon_hub"),
            new WarpDestination("Wizard Tower (Rift)", "warp rift", "wizard_tower_rift"),
            new WarpDestination("Jerry's Workshop", "warp jerry", "jerrys_workshop"),
            new WarpDestination("Lotus Atoll", "warp lotus", "lotus_atoll"),
            new WarpDestination("Backwater Bayou", "warp bayou", "backwater_bayou"),
            new WarpDestination("Garden", "warp garden", "garden")
        ));

        final String id;
        final String displayName;
        final String icon;
        final List<WarpDestination> destinations;

        WarpCategory(String id, String displayName, String icon, List<WarpDestination> destinations) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.destinations = destinations;
        }
    }

    public static class WarpDestination {
        final String name;
        final String command;
        final String iconId;

        WarpDestination(String name, String command, String iconId) {
            this.name = name;
            this.command = command;
            this.iconId = iconId;
        }
        
        public Identifier getIconTexture() {
            return Identifier.fromNamespaceAndPath("baity", "textures/gui/warp/" + iconId + ".png");
        }
    }

    private static double savedMouseX = -1;
    private static double savedMouseY = -1;

    public static void setInitialMousePosition(double x, double y) {
        savedMouseX = x;
        savedMouseY = y;
    }

    private int hoveredSection = -1;
    private final WarpCategory currentCategory;
    private final Screen parentScreen;
    
    private static final int OUTER_RADIUS = RadialMenuComponent.OUTER_RADIUS;
    private static final int INNER_RADIUS = RadialMenuComponent.INNER_RADIUS;

    private static final WarpCategory[] MAIN_CATEGORIES = {
        WarpCategory.BASIC,
        WarpCategory.PARK_BARN,
        WarpCategory.MINING,
        WarpCategory.OTHERS,
        WarpCategory.CRIMSON,
        WarpCategory.COMBAT
    };

    public WarpMenuScreen() {
        this(null, null);
    }

    public WarpMenuScreen(WarpCategory category, Screen parent) {
        super(Component.literal("Warp Menu"));
        this.currentCategory = category;
        this.parentScreen = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (savedMouseX >= 0 && savedMouseY >= 0 && this.minecraft != null) {
            GLFW.glfwSetCursorPos(this.minecraft.getWindow().handle(), savedMouseX, savedMouseY);
            savedMouseX = -1;
            savedMouseY = -1;
        }
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        float ratio = ClickGuiState.fixedScaleRatio(this.minecraft);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        mouseX = Math.round(ClickGuiState.fixedCoord(mouseX, centerX, ratio));
        mouseY = Math.round(ClickGuiState.fixedCoord(mouseY, centerY, ratio));
        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(ratio, ratio);
        pose.translate(-centerX, -centerY);
        try {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        boolean hoveringExit = distance <= INNER_RADIUS + 2;

        Object[] sections = getSections();
        int sectionCount = sections.length;

        hoveredSection = -1;
        if (!hoveringExit && distance > INNER_RADIUS && distance < OUTER_RADIUS + 20) {
            double angle = Math.atan2(dy, dx);
            double degrees = Math.toDegrees(angle);
            if (degrees < 0) degrees += 360;
            hoveredSection = RadialMenuComponent.getSectionFromAngle(degrees, sectionCount);
        }

        double anglePerSection = 360.0 / sectionCount;
        double startAngle = RadialMenuComponent.getStartAngle(sectionCount);

        final var owo = OwoUIGraphics.of(context);
        RadialMenuComponent.drawWheel(owo, centerX, centerY);
        RadialMenuComponent.drawSectorDividers(owo, centerX, centerY, sectionCount, startAngle, anglePerSection);

        if (hoveredSection >= 0 && hoveredSection < sectionCount) {
            double sectionStartAngle = startAngle + hoveredSection * anglePerSection;
            RadialMenuComponent.drawHoveredSector(owo, centerX, centerY, sectionStartAngle, sectionStartAngle + anglePerSection);
        }

        for (int i = 0; i < sectionCount; i++) {
            boolean isHovered = (hoveredSection == i);
            float[] iconPos = RadialMenuComponent.sectorCenter(
                    centerX, centerY, startAngle, anglePerSection, i, INNER_RADIUS, OUTER_RADIUS);

            if (currentCategory == null) {
                WarpCategory cat = (WarpCategory) sections[i];
                RadialMenuComponent.drawUnicodeSymbol(context, this.font, cat.icon,
                        iconPos[0], iconPos[1], RadialMenuComponent.ICON_BASE_SCALE);
            } else {
                WarpDestination dest = (WarpDestination) sections[i];
                int iconSize = RadialMenuComponent.WARP_ICON_BASE_SIZE;
                context.blit(RenderPipelines.GUI_TEXTURED, dest.getIconTexture(),
                        Math.round(iconPos[0] - iconSize / 2f), Math.round(iconPos[1] - iconSize / 2f),
                        0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);

                String labelText = dest.name;
                float[] labelPos = RadialMenuComponent.sectorLabelPosition(
                        centerX, centerY, startAngle, anglePerSection, i, OUTER_RADIUS + 25, this.font, labelText);

                if (isHovered) {
                    RadialMenuComponent.drawRadialLabel(context, this.font, labelText, labelPos[0], labelPos[1]);
                } else {
                    RadialMenuComponent.drawLabel(context, this.font, labelText, labelPos[0], labelPos[1],
                            RadialMenuComponent.textSecondary());
                }
            }
        }

        if (currentCategory == null && hoveredSection >= 0 && hoveredSection < sectionCount) {
            WarpCategory hoveredCat = MAIN_CATEGORIES[hoveredSection];
            String labelText = hoveredCat.displayName;
            float[] labelPos = RadialMenuComponent.sectorLabelPosition(
                    centerX, centerY, startAngle, anglePerSection, hoveredSection, OUTER_RADIUS + 15, this.font, labelText);
            RadialMenuComponent.drawRadialLabel(context, this.font, labelText, labelPos[0], labelPos[1]);
        }

        RadialMenuComponent.CenterStyle centerStyle = currentCategory == null
                ? RadialMenuComponent.CenterStyle.EXIT
                : RadialMenuComponent.CenterStyle.BACK;
        RadialMenuComponent.drawCenter(owo, centerX, centerY, centerStyle);
        } finally {
            pose.popMatrix();
        }
    }

    private Object[] getSections() {
        if (currentCategory == null) {
            return MAIN_CATEGORIES;
        } else {
            return currentCategory.destinations.toArray();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        if (click.button() == 0) {
            float ratio = ClickGuiState.fixedScaleRatio(this.minecraft);
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            double dx = ClickGuiState.fixedCoord((float) click.x(), centerX, ratio) - centerX;
            double dy = ClickGuiState.fixedCoord((float) click.y(), centerY, ratio) - centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= INNER_RADIUS + 2) {
                com.shyeuar.baity.utils.SoundUtils.playWoodenButton();
                if (parentScreen != null) {
                    Minecraft.getInstance().setScreen(parentScreen);
                } else {
                    this.onClose();
                }
                return true;
            }

            if (hoveredSection >= 0) {
                Object[] sections = getSections();
                if (hoveredSection < sections.length) {
                    Object selected = sections[hoveredSection];
                    com.shyeuar.baity.utils.SoundUtils.playWoodenButton();
                    if (selected instanceof WarpCategory cat) {
                        Minecraft.getInstance().setScreen(new WarpMenuScreen(cat, this));
                    } else if (selected instanceof WarpDestination dest) {
                        executeWarp(dest.command);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(click, isInsideWindow);
    }

    private void executeWarp(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.connection.sendCommand(command);
        }
        this.onClose();
    }
}


