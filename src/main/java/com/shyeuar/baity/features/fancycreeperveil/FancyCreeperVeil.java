package com.shyeuar.baity.features.fancycreeperveil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class FancyCreeperVeil implements WorldRenderEvents.AfterEntities {
    
    private static final Minecraft MC = Minecraft.getInstance();
    private static final ResourceLocation WITHER_CLOAK_SHIELD = ResourceLocation.fromNamespaceAndPath("baity", "textures/wither_cloak_shield.png");
    
    public static boolean isCloakActive = false;
    public static long lastCreeperRender = 0;
    public static long lastDeactivate = System.currentTimeMillis();
    
    private static final double SHIELD_WIDTH = 0.8;
    private static final double SHIELD_HEIGHT = 2.0;
    private static final double ACCURACY = 4.0;
    private static final int SHIELD_COUNT = 6;
    private static final double SHIELD_SPEED = 2.0;
    private static final double SHIELD_DISTANCE = 1.2;
    
    public static void init() {
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            Module module = ModuleManager.getModuleByName("FancyCreeperVeil");
            if (module == null || !module.isEnabled()) return;
            if (!ConfigManager.fancyCreeperVeilEnabled) return;
            
            String text = message.getString();
            
            if (text.startsWith("Creeper Veil ")) {
                if (isCloakActive && !text.equals("Creeper Veil Activated!")) {
                    isCloakActive = false;
                    lastDeactivate = System.currentTimeMillis();
                } else {
                    isCloakActive = true;
                }
            } else if (text.startsWith("Not enough mana! Creeper Veil De-activated!")) {
                isCloakActive = false;
                lastDeactivate = System.currentTimeMillis();
            }
        });
    }
    
    @Override
    public void afterEntities(WorldRenderContext context) {
        Module module = ModuleManager.getModuleByName("FancyCreeperVeil");
        if (module == null || !module.isEnabled()) return;
        if (!ConfigManager.fancyCreeperVeilEnabled) return;
        
        if (MC.level == null || MC.player == null) return;
        
        if (isCloakActive) {
            if (System.currentTimeMillis() - lastCreeperRender >= 2000) {
                isCloakActive = false;
                lastDeactivate = System.currentTimeMillis();
                lastCreeperRender = 0;
                return;
            }
        }
        
        if (!isCloakActive) return;
        
        PoseStack matrices = context.matrices();
        MultiBufferSource buffers = context.consumers();
        if (matrices == null || buffers == null) return;
        
        Vec3 cameraPos = context.worldState().cameraRenderState.pos;
        Vec3 playerPos = MC.player.position();
        
        matrices.pushPose();
        matrices.translate(playerPos.x - cameraPos.x, playerPos.y - cameraPos.y, playerPos.z - cameraPos.z);
        
        for (int i = 0; i < SHIELD_COUNT; i++) {
            double angle = (int) (((System.currentTimeMillis() / 30.0 * SHIELD_SPEED * -0.5 * ACCURACY) % (360 * ACCURACY)) / ACCURACY);
            angle += (360.0 / SHIELD_COUNT) * i;
            angle %= 360;
            
            double angleRad = Math.toRadians(angle);
            double posX = Math.cos(angleRad) * SHIELD_DISTANCE;
            double posZ = Math.sin(angleRad) * SHIELD_DISTANCE;
            
            matrices.pushPose();
            matrices.translate(posX, 0, posZ);
            matrices.mulPose(Axis.YP.rotationDegrees((float) -angle));
            
            renderShield(matrices, buffers);
            
            matrices.popPose();
        }
        
        matrices.popPose();
    }
    
    private void renderShield(PoseStack matrices, MultiBufferSource buffers) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(WITHER_CLOAK_SHIELD));
        PoseStack.Pose pose = matrices.last();
        Matrix4f matrix = pose.pose();
        
        float width = (float) SHIELD_WIDTH;
        float height = (float) SHIELD_HEIGHT;
        float halfWidth = width / 2.0f;
        
        consumer.addVertex(matrix, -halfWidth, height, 0)
            .setColor(1.0f, 1.0f, 1.0f, 1.0f)
            .setUv(0, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);
        
        consumer.addVertex(matrix, halfWidth, height, 0)
            .setColor(1.0f, 1.0f, 1.0f, 1.0f)
            .setUv(1, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);
        
        consumer.addVertex(matrix, halfWidth, 0, 0)
            .setColor(1.0f, 1.0f, 1.0f, 1.0f)
            .setUv(1, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);
        
        consumer.addVertex(matrix, -halfWidth, 0, 0)
            .setColor(1.0f, 1.0f, 1.0f, 1.0f)
            .setUv(0, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0, 0, 1);
    }
    
    public static void onWorldUnload() {
        isCloakActive = false;
    }
}

