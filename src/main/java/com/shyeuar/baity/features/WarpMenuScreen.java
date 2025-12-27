package com.shyeuar.baity.features;

import com.shyeuar.baity.utils.RadialMenuRendererUtils;
import com.shyeuar.baity.utils.SoundUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static com.shyeuar.baity.utils.RadialMenuRendererUtils.*;

@Environment(EnvType.CLIENT)
public class WarpMenuScreen extends Screen {

    private static double savedMouseX = -1;
    private static double savedMouseY = -1;

    public static void setInitialMousePosition(double x, double y) {
        savedMouseX = x;
        savedMouseY = y;
    }

    private int hoveredSection = -1;
    private final WarpCategory currentCategory;
    private final Screen parentScreen;

    public enum WarpCategory {
        MAIN(null, "Warp Menu", "\u2302", List.of()),

        BASIC("Basic", "Basic", "\u2302", List.of(
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

        OTHERS("Others", "Others", "\u2605", List.of(
            new WarpDestination("Dungeon Hub", "warp dh", "dungeon_hub"),
            new WarpDestination("Wizard Tower (Rift)", "warp rift", "wizard_tower_rift"),
            new WarpDestination("Jerry's Workshop", "warp jerry", "jerrys_workshop"),
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
            return Identifier.of("baity", "textures/gui/warp/" + iconId + ".png");
        }
    }

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
        super(Text.literal("Warp Menu"));
        this.currentCategory = category;
        this.parentScreen = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (savedMouseX >= 0 && savedMouseY >= 0 && this.client != null) {
            GLFW.glfwSetCursorPos(this.client.getWindow().getHandle(), savedMouseX, savedMouseY);
            savedMouseX = -1;
            savedMouseY = -1;
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

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
            hoveredSection = RadialMenuRendererUtils.getSectionFromAngle(degrees, sectionCount);
        }

        double anglePerSection = 360.0 / sectionCount;
        double startAngle = getStartAngle(sectionCount);

        for (int i = 0; i < sectionCount; i++) {
            double sectionStartAngle = startAngle + i * anglePerSection;
            double sectionEndAngle = sectionStartAngle + anglePerSection;
            boolean isHovered = (hoveredSection == i);
            int sectionColor = isHovered ? SECTION_HOVER_COLOR : SECTION_COLOR;
            drawArcSection(context, centerX, centerY, INNER_RADIUS, OUTER_RADIUS,
                    sectionStartAngle, sectionEndAngle, sectionColor);
        }

        for (int i = 0; i < sectionCount; i++) {
            double lineAngle = Math.toRadians(startAngle + i * anglePerSection);
            drawRadialLine(context, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, lineAngle, BORDER_COLOR);
        }

        for (int i = 0; i < sectionCount; i++) {
            double sectionStartAngle = startAngle + i * anglePerSection;
            double sectionEndAngle = sectionStartAngle + anglePerSection;
            boolean isHovered = (hoveredSection == i);

            double midAngle = Math.toRadians((sectionStartAngle + sectionEndAngle) / 2);

            if (currentCategory == null) {
                int iconRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
                int iconX = centerX + (int) (Math.cos(midAngle) * iconRadius);
                int iconY = centerY + (int) (Math.sin(midAngle) * iconRadius);

                int textColor = isHovered ? 0xFFFFFF00 : 0xFFFFFFFF;
                String icon = getIcon(sections[i]);
                
                float scale = 3.0f;
                var matrices = context.getMatrices();
                matrices.pushMatrix();
                matrices.translate(iconX, iconY);
                matrices.scale(scale, scale);
                context.drawText(this.textRenderer, icon, -this.textRenderer.getWidth(icon) / 2, -this.textRenderer.fontHeight / 2, textColor, true);
                matrices.popMatrix();
            } else {
                WarpDestination dest = (WarpDestination) sections[i];
                
                int iconRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
                int iconX = centerX + (int) (Math.cos(midAngle) * iconRadius);
                int iconY = centerY + (int) (Math.sin(midAngle) * iconRadius);
                
                int iconSize = 24;
                context.drawTexture(RenderPipelines.GUI_TEXTURED, dest.getIconTexture(), 
                    iconX - iconSize / 2, iconY - iconSize / 2, 
                    0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
                
                String labelText = dest.name;
                int labelWidth = this.textRenderer.getWidth(labelText);

                int labelRadius = OUTER_RADIUS + 25;
                int labelX = centerX + (int) (Math.cos(midAngle) * labelRadius) - labelWidth / 2;
                int labelY = centerY + (int) (Math.sin(midAngle) * labelRadius) - 4;

                int labelColor = isHovered ? 0xFFFFFF00 : 0xFFFFFFFF;
                context.drawText(this.textRenderer, labelText, labelX, labelY, labelColor, true);
            }
        }

        drawFilledCircle(context, centerX, centerY, INNER_RADIUS + 2, CENTER_COLOR);

        if (currentCategory == null && hoveredSection >= 0 && hoveredSection < sectionCount) {
            WarpCategory hoveredCat = MAIN_CATEGORIES[hoveredSection];
            String labelText = hoveredCat.displayName;
            int labelWidth = this.textRenderer.getWidth(labelText);

            double sectionStartAngle = startAngle + hoveredSection * anglePerSection;
            double sectionEndAngle = sectionStartAngle + anglePerSection;
            double midAngle = Math.toRadians((sectionStartAngle + sectionEndAngle) / 2);

            int labelRadius = OUTER_RADIUS + 15;
            int labelX = centerX + (int) (Math.cos(midAngle) * labelRadius) - labelWidth / 2;
            int labelY = centerY + (int) (Math.sin(midAngle) * labelRadius) - 4;

            context.drawText(this.textRenderer, labelText, labelX, labelY, 0xFFFFFF00, true);
        }

        String centerIcon;
        int centerColor;
        float centerScale;
        if (currentCategory == null) {
            centerIcon = "\u274C";
            centerColor = hoveringExit ? 0xFFFF4444 : 0xFFFFFFFF;
            centerScale = 2.0f; 
        } else {
            centerIcon = "\u21A9"; 
            centerColor = hoveringExit ? 0xFFFFFF00 : 0xFFFFFFFF;
            centerScale = 2.0f;
        }
        
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, centerY);
        matrices.scale(centerScale, centerScale);
        int centerTextWidth = this.textRenderer.getWidth(centerIcon);
        context.drawText(this.textRenderer, centerIcon, -centerTextWidth / 2, -this.textRenderer.fontHeight / 2, centerColor, true);
        matrices.popMatrix();

        String title = currentCategory == null ? "Warp Menu" : currentCategory.displayName;
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, (this.width - titleWidth) / 2, 20, 0xFFFFFF, true);
    }

    private Object[] getSections() {
        if (currentCategory == null) {
            return MAIN_CATEGORIES;
        } else {
            return currentCategory.destinations.toArray();
        }
    }

    private String getIcon(Object section) {
        if (section instanceof WarpCategory cat) {
            return cat.icon;
        } else if (section instanceof WarpDestination dest) {
            return dest.name;
        }
        return "?";
    }

    @Override
    public boolean mouseClicked(Click click, boolean isInsideWindow) {
        if (click.button() == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            double dx = click.x() - centerX;
            double dy = click.y() - centerY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= INNER_RADIUS + 2) {
                SoundUtils.playWoodenButton();
                if (parentScreen != null) {
                    MinecraftClient.getInstance().setScreen(parentScreen);
                } else {
                    this.close();
                }
                return true;
            }

            if (hoveredSection >= 0) {
                Object[] sections = getSections();
                if (hoveredSection < sections.length) {
                    Object selected = sections[hoveredSection];
                    SoundUtils.playWoodenButton();
                    if (selected instanceof WarpCategory cat) {
                        MinecraftClient.getInstance().setScreen(new WarpMenuScreen(cat, this));
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.networkHandler.sendChatCommand(command);
        }
        this.close();
    }
}
