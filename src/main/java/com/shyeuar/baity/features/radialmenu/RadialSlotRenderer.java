package com.shyeuar.baity.features.radialmenu;

import com.shyeuar.baity.features.radialmenu.data.RadialMenuModels;
import com.shyeuar.baity.gui.owo.RadialMenuComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class RadialSlotRenderer {

    private RadialSlotRenderer() {
    }

    public static void drawSlotIcon(GuiGraphicsExtractor graphics, Font font, RadialMenuModels.RadialSlot slot,
                                    float centerX, float centerY) {
        if (slot == null) {
            return;
        }

        if (slot.icon != null && !slot.icon.isBlank()) {
            Identifier texture = RadialIconLibrary.resolveFileTexture(slot.icon);
            if (texture != null) {
                int iconSize = RadialMenuComponent.WARP_ICON_BASE_SIZE;
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                        Math.round(centerX - iconSize / 2f), Math.round(centerY - iconSize / 2f),
                        0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
                return;
            }
            ItemStack stack = RadialIconLibrary.resolveItemStack(slot.icon);
            if (!stack.isEmpty()) {
                drawItemIcon(graphics, stack, centerX, centerY);
                return;
            }
        }

        if (slot.unicodeIcon != null && !slot.unicodeIcon.isEmpty()) {
            RadialMenuComponent.drawUnicodeSymbol(graphics, font, slot.unicodeIcon, centerX, centerY,
                    RadialMenuComponent.ICON_BASE_SCALE);
        }
    }

    public static void drawIconPreview(GuiGraphicsExtractor graphics, Font font, String icon,
                                       float centerX, float centerY) {
        RadialMenuModels.RadialSlot preview = new RadialMenuModels.RadialSlot();
        preview.icon = icon == null ? "" : icon;
        drawSlotIcon(graphics, font, preview, centerX, centerY);
    }

    public static void drawSlotLabels(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY,
                                      double startAngle, double anglePerSection, int sectionCount,
                                      java.util.List<RadialMenuModels.RadialSlot> slots, int hoveredIndex) {
        for (int i = 0; i < sectionCount; i++) {
            RadialMenuModels.RadialSlot slot = i < slots.size() ? slots.get(i) : null;
            String label = slot == null || slot.displayName == null ? "" : slot.displayName.trim();
            if (label.isEmpty()) {
                continue;
            }
            float[] labelPos = RadialMenuComponent.sectorLabelPosition(
                    centerX, centerY, startAngle, anglePerSection, i, RadialMenuComponent.OUTER_RADIUS + 25, font, label);
            if (i == hoveredIndex) {
                RadialMenuComponent.drawRadialLabel(graphics, font, label, labelPos[0], labelPos[1]);
            } else {
                RadialMenuComponent.drawLabel(graphics, font, label, labelPos[0], labelPos[1], RadialMenuComponent.textSecondary());
            }
        }
    }

    private static void drawItemIcon(GuiGraphicsExtractor graphics, ItemStack stack, float centerX, float centerY) {
        int size = RadialMenuComponent.WARP_ICON_BASE_SIZE;
        float scale = size / 16f;
        int drawX = Math.round(centerX - 8);
        int drawY = Math.round(centerY - 8);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.scale(scale, scale);
        pose.translate(-centerX, -centerY);
        graphics.fakeItem(stack, drawX, drawY);
        pose.popMatrix();
    }
}
