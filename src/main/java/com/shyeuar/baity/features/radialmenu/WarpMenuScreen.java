package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.gui.theme.LinearTheme;
import io.wispforest.owo.ui.core.Color;
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
    
    private static final int OUTER_RADIUS = 80;
    private static final int INNER_RADIUS = 30;
    private static final int CENTER_RADIUS = 30;
    
    private static final int BG_COLOR = LinearTheme.BG_PRIMARY.getRGB(); // 深灰色 (18, 18, 20)
    
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
            hoveredSection = getSectionFromAngle(degrees, sectionCount);
        }

        double anglePerSection = 360.0 / sectionCount;
        double startAngle = getStartAngle(sectionCount);

        final var owo = OwoUIGraphics.of(context);
        final int segments = 220;

        final int baseOuter = OUTER_RADIUS;
        final int baseInner = OUTER_RADIUS - 7;

        int faceColor = BG_COLOR;
        int edgeColor = lerpArgb(BG_COLOR, LinearTheme.BG_SECONDARY.getRGB(), 0.35f);

        int edgeArgb = withAlpha(edgeColor, 0x99);
        int faceArgb = withAlpha(faceColor, 0x66);

        owo.drawCircle(centerX, centerY, segments, baseOuter, Color.ofArgb(edgeArgb));
        owo.drawCircle(centerX, centerY, segments, baseInner, Color.ofArgb(faceArgb));

        int aaInner = withAlpha(edgeColor, 0x60);
        int aaOuter = withAlpha(edgeColor, 0x00);
        drawRingSplit(owo, centerX, centerY, 0, 360, segments,
                baseOuter - 1, baseOuter + 0.75, Color.ofArgb(aaInner), Color.ofArgb(aaOuter));

        int innerBase = withAlpha(lerpArgb(faceColor, 0x00000000, 0.18f), 0x66);
        int innerEdge = withAlpha(lerpArgb(faceColor, LinearTheme.BG_SECONDARY.getRGB(), 0.4f), 0x88);

        int innerRadius = CENTER_RADIUS - 2;
        owo.drawCircle(centerX, centerY, segments, innerRadius, Color.ofArgb(innerBase));
        drawRingSplit(owo, centerX, centerY, 0, 360, segments,
                innerRadius - 1, innerRadius + 1, Color.ofArgb(innerEdge), Color.ofArgb(innerEdge));

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
                var matrices = context.pose();
                matrices.pushMatrix();
                matrices.translate(iconX, iconY);
                matrices.scale(scale, scale);
                context.drawString(this.font, icon, -this.font.width(icon) / 2, -this.font.lineHeight / 2, textColor, true);
                matrices.popMatrix();
            } else {
                WarpDestination dest = (WarpDestination) sections[i];

                int iconRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
                int iconX = centerX + (int) (Math.cos(midAngle) * iconRadius);
                int iconY = centerY + (int) (Math.sin(midAngle) * iconRadius);

                int iconSize = 24;
                context.blit(RenderPipelines.GUI_TEXTURED, dest.getIconTexture(),
                    iconX - iconSize / 2, iconY - iconSize / 2,
                    0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);

                String labelText = dest.name;
                int labelWidth = this.font.width(labelText);

                int labelRadius = OUTER_RADIUS + 25;
                int labelX = centerX + (int) (Math.cos(midAngle) * labelRadius) - labelWidth / 2;
                int labelY = centerY + (int) (Math.sin(midAngle) * labelRadius) - 4;

                int labelColor = isHovered ? 0xFFFFFF00 : 0xFFFFFFFF;
                context.drawString(this.font, labelText, labelX, labelY, labelColor, true);
            }
        }

        if (currentCategory == null && hoveredSection >= 0 && hoveredSection < sectionCount) {
            WarpCategory hoveredCat = MAIN_CATEGORIES[hoveredSection];
            String labelText = hoveredCat.displayName;
            int labelWidth = this.font.width(labelText);

            double sectionStartAngle = startAngle + hoveredSection * anglePerSection;
            double sectionEndAngle = sectionStartAngle + anglePerSection;
            double midAngle = Math.toRadians((sectionStartAngle + sectionEndAngle) / 2);

            int labelRadius = OUTER_RADIUS + 15;
            int labelX = centerX + (int) (Math.cos(midAngle) * labelRadius) - labelWidth / 2;
            int labelY = centerY + (int) (Math.sin(midAngle) * labelRadius) - 4;

            context.drawString(this.font, labelText, labelX, labelY, 0xFFFFFF00, true);
        }
        
        int iconOuterR = 7;
        int iconInnerR = 4;
        if (currentCategory == null) {
            owo.drawCircle(centerX, centerY, 48, iconOuterR, Color.ofArgb(0xCCFF4444));
            owo.drawCircle(centerX, centerY, 48, iconInnerR, Color.ofArgb(withAlpha(BG_COLOR, 0xFF)));
        } else {
            owo.drawCircle(centerX, centerY, 48, iconOuterR, Color.ofArgb(0xCCFFFF55));
            owo.drawCircle(centerX, centerY, 48, iconInnerR, Color.ofArgb(withAlpha(BG_COLOR, 0xFF)));
        }

        String title = currentCategory == null ? "Warp Menu" : currentCategory.displayName;
        int titleWidth = this.font.width(title);
        context.drawString(this.font, title, (this.width - titleWidth) / 2, 20, 0xFFFFFF, true);
    }

    private void drawRingSplit(OwoUIGraphics context, int centerX, int centerY,
                               double fromDeg, double toDeg, int segments,
                               double innerRadius, double outerRadius,
                               Color innerColor, Color outerColor) {
        double f = normalizeDeg(fromDeg);
        double t = normalizeDeg(toDeg);
        if (t <= f) t += 360d;

        if (t <= 360d) {
            context.drawRing(centerX, centerY, f, t, segments, innerRadius, outerRadius, innerColor, outerColor);
        } else {
            context.drawRing(centerX, centerY, f, 360d, segments, innerRadius, outerRadius, innerColor, outerColor);
            context.drawRing(centerX, centerY, 0d, t - 360d, segments, innerRadius, outerRadius, innerColor, outerColor);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int lerpArgb(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(lerp(aa, ba, t));
        int rr = Math.round(lerp(ar, br, t));
        int rg = Math.round(lerp(ag, bg, t));
        int rb = Math.round(lerp(ab, bb, t));
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static double normalizeDeg(double deg) {
        deg %= 360d;
        if (deg < 0) deg += 360d;
        return deg;
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

    private double getStartAngle(int sectionCount) {
        return -90 - (360.0 / sectionCount) / 2;
    }

    private int getSectionFromAngle(double degrees, int sectionCount) {
        double anglePerSection = 360.0 / sectionCount;
        double startAngle = -90 - anglePerSection / 2;
        if (startAngle < 0) startAngle += 360;

        double adjustedDegrees = degrees - startAngle;
        if (adjustedDegrees < 0) adjustedDegrees += 360;

        int section = (int) (adjustedDegrees / anglePerSection);
        return section % sectionCount;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean isInsideWindow) {
        if (click.button() == 0) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            double dx = click.x() - centerX;
            double dy = click.y() - centerY;
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


