package com.shyeuar.baity.features;

import com.shyeuar.baity.config.DevConfig;
import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.module.ModuleManager;
import com.shyeuar.baity.utils.ModuleUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class PlayerESPRenderer implements WorldRenderEvents.AfterEntities {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    private static long lastTimeUpdate = 0;
    private static double cachedSinValue = 0.0;
    
    @Override
    public void afterEntities(WorldRenderContext context) {
        Module m = ModuleManager.getModuleByName("PlayerESP");
        if (m == null || !m.isEnabled()) {
            return; 
        }
        
        if (mc.world == null || mc.player == null) return;
        
        // 从 CameraRenderState 获取相机位置，从 Camera 获取旋转信息
        Vec3d cameraPos = context.worldState().cameraRenderState.pos;
        Camera camera = mc.gameRenderer.getCamera();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
        float cameraYaw = camera.getYaw();
        float cameraPitch = camera.getPitch();
        
        // 使用 context 提供的 matrices
        MatrixStack matrices = context.matrices();
        if (matrices == null) {
            matrices = new MatrixStack();
            matrices.multiply(new org.joml.Quaternionf().rotationXYZ(
                (float) Math.toRadians(cameraPitch),
                (float) Math.toRadians(cameraYaw + 180.0f),
                0.0f
            ));
        }
        
        updateCache();
        
        com.shyeuar.baity.utils.AntiBotUtils.updatePlayerMap();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (com.shyeuar.baity.utils.AntiBotUtils.isBot(player)) {
                continue;
            }
            
            boolean showOwnNametag = ModuleUtils.getOptionBoolean(m, "show own nametag", false);
            if (player == mc.player) {
                if (mc.options.getPerspective().isFirstPerson()) {
                    continue;
                }
                if (!showOwnNametag) {
                    continue;
                }
            }
            
            Vec3d lerpedPos = player.getLerpedPos(tickDelta);
            double x = lerpedPos.x - cameraPos.x;
            double y = lerpedPos.y - cameraPos.y;
            double z = lerpedPos.z - cameraPos.z;

            matrices.push();
            try {
                matrices.translate(x, y, z);
                renderPlayerName(matrices, player, cameraYaw, cameraPitch, m);
            } finally {
                matrices.pop();
            }
        }
    }
    
    private void renderPlayerName(MatrixStack matrices, PlayerEntity player, float cameraYaw, float cameraPitch, Module module) {
        matrices.push();
        try {
            float heightOffset = player.getHeight() + 0.5f;
            
            Module smolPeopleModule = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName("SmolPeople");
            if (smolPeopleModule != null && smolPeopleModule.isEnabled()) {
                boolean showOwnNametag = ModuleUtils.getOptionBoolean(module, "show own nametag", false);
                if (player == mc.player && showOwnNametag) {
                    heightOffset -= 0.4f;
                }
            }
            
            matrices.translate(0, heightOffset, 0);
            matrices.multiply(new org.joml.Quaternionf().rotationY(-cameraYaw * 0.017453292F));
            matrices.multiply(new org.joml.Quaternionf().rotationX(cameraPitch * 0.017453292F));

            assert mc.player != null;
            double distance = mc.player.distanceTo(player);
            float baseScale = (float) Math.max(0.03, Math.min(distance * 0.0025, 0.12));
            float breathingScale = (float) (baseScale * (1.0 + cachedSinValue * 0.3)); 
            matrices.scale(-breathingScale, -breathingScale, breathingScale);
        
        String baseName = player.getDisplayName() != null ? player.getDisplayName().getString() : player.getName().getString();
        boolean isDeveloper = DevConfig.isDeveloper(player);
        boolean showDistance = ModuleUtils.getOptionBoolean(module, "show distance", true);
        
        TextRenderer textRenderer = mc.textRenderer;
        int nameColor = 0xFF69B4;
        int distanceColor = 0x00FFFF;
        
        int totalWidth = textRenderer.getWidth(baseName);
        if (isDeveloper) {
            totalWidth += textRenderer.getWidth(DevConfig.DEV_PREFIX) + 2; 
        }
        
        int currentX;
        if (isDeveloper) {
            int nameWidth = textRenderer.getWidth(baseName);
            int prefixWidth = textRenderer.getWidth(DevConfig.DEV_PREFIX) + 2;
            currentX = -nameWidth / 2 - prefixWidth; 
        } else {
            currentX = -totalWidth / 2;
        }
        
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        
        // 添加 alpha 通道到颜色
        int nameColorWithAlpha = 0xFFFF69B4;
        int distanceColorWithAlpha = 0xFF00FFFF;
        int devColorWithAlpha = DevConfig.DEV_PREFIX_COLOR | 0xFF000000;
        
        if (isDeveloper) {
            textRenderer.draw(DevConfig.DEV_PREFIX, currentX, 0, devColorWithAlpha, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
            currentX += textRenderer.getWidth(DevConfig.DEV_PREFIX) + 2;
        }
        
        textRenderer.draw(baseName, currentX, 0, nameColorWithAlpha, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        
        if (showDistance) {
            double dist = mc.player != null ? mc.player.distanceTo(player) : 0.0;
            String distanceText = " [" + (int)Math.round(dist) + "]";
            int distanceX;
            if (isDeveloper) {
                distanceX = currentX + textRenderer.getWidth(baseName) + 2;
            } else {
                distanceX = totalWidth / 2 + 2;
            }
            textRenderer.draw(distanceText, distanceX, 0, distanceColorWithAlpha, false, matrices.peek().getPositionMatrix(), immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
        }
        
            immediate.draw();
        } finally {
            matrices.pop();
        }
    }
    

    private static void updateCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime != lastTimeUpdate) {
            cachedSinValue = Math.sin(currentTime * 0.001) * 0.08; 
            lastTimeUpdate = currentTime;
        }
    }
}
