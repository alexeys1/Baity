package com.shyeuar.baity.features.fishing;

import com.shyeuar.baity.config.ConfigManager;
import com.shyeuar.baity.gui.hud.HudElement;
import com.shyeuar.baity.gui.hud.HudManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class FishHookTimer implements HudElement {

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)");
    private static final String BITE_MARKER = "!!!";
    private static final String ASSET_NAMESPACE = "fishtimer";
    private static final String BAR_TEXTURE_PATH = "textures/skyblock/fishing_timer_bar.png";
    private static final int FRAME_W = 128;
    private static final int FRAME_H = 32;
    private static final int FRAME_STRIDE = 33;
    private static final int SHEET_W = 128;
    private static final int SHEET_H = 395;

    private static FishHookTimer instance;
    private static final net.minecraft.sounds.SoundEvent[] FRAME_SOUNDS = new net.minecraft.sounds.SoundEvent[12];

    private boolean selected;
    private boolean clicked;
    private int currentTick = -1;
    private boolean biteMode;
    private int lastSoundFrame = -1;
    
    private FishHookTimer() {}
    
    public static FishHookTimer getInstance() {
        if (instance == null) {
            instance = new FishHookTimer();
            HudManager.getInstance().register(instance);
        }
        return instance;
    }
    
    public static void init() {
        getInstance();
        ensureGuideExists();
        registerSounds();
    }
    
    private static void registerSounds() {
        for (int i = 0; i < 12; i++) {
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(ASSET_NAMESPACE, "fishing_timer_" + i);
            FRAME_SOUNDS[i] = net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT,
                id,
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id)
            );
        }
    }
    
    private static void ensureGuideExists() {
        try {
            java.nio.file.Path baityDir = com.shyeuar.baity.config.BaityConfigDir.getBaityConfigDir();
            java.nio.file.Path guidePath = baityDir.resolve("FishHookTimer_DIY_UI_Setup_Guide.txt");
            if (!java.nio.file.Files.isRegularFile(guidePath)) {
                java.nio.file.Files.createDirectories(baityDir);
                java.nio.file.Files.writeString(guidePath, GUIDE_CONTENT, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
    }
    
    private static final String GUIDE_CONTENT = """
        ============================================================
        FishHookTimer DIY UI Setup Guide / 钓鱼计时器自定义UI配置指南
        ============================================================
        
        Structure: assets/fishtimer/textures/skyblock/fishing_timer_bar.png
        结构：assets/fishtimer/textures/skyblock/fishing_timer_bar.png
        
        ------------------------------------------------------------
        Step 1: Resource Pack Structure / 第一步：资源包结构
        ------------------------------------------------------------
        
        <ResourcePack>/
        └── assets/
            └── fishtimer/
                ├── textures/
                │   └── skyblock/
                │       └── fishing_timer_bar.png
                └── sounds/
                    ├── fishing_timer_0.ogg
                    ├── fishing_timer_1.ogg
                    ├── ... (fishing_timer_2.ogg to fishing_timer_11.ogg)
                    └── fishing_timer_11.ogg
        
        ------------------------------------------------------------
        Step 2: Texture Image Specifications / 第二步：纹理图片规格
        ------------------------------------------------------------
        
        The fishing_timer_bar.png is a vertical sprite sheet containing 12 animation frames.
        Each frame represents a different stage of the countdown timer.
        
        fishing_timer_bar.png 是一张垂直排列的精灵图，包含12个动画帧。
        每一帧代表倒计时器的不同阶段。
        
        Image Layout / 图片布局:
        - Total dimensions: 128 pixels wide × 395 pixels tall
        - Frame size: Each frame is 128 pixels wide × 32 pixels tall
        - Frame spacing: 33 pixels vertically (1 pixel gap between each frame)
        - Total frames: 12 frames, numbered from 0 to 11
        
        - 总尺寸：128像素宽 × 395像素高
        - 每帧大小：128像素宽 × 32像素高
        - 帧间距：垂直方向33像素（每帧之间有1像素间隙）
        - 总帧数：12帧，编号从0到11
        
        Vertical Frame Layout (from top to bottom) / 垂直帧布局（从上到下）:
        Frame 11 (top row)    - Y coordinates: 0 to 31
        Frame 10              - Y coordinates: 33 to 64
        Frame 9               - Y coordinates: 66 to 97
        Frame 8               - Y coordinates: 99 to 130
        Frame 7               - Y coordinates: 132 to 163
        Frame 6               - Y coordinates: 165 to 196
        Frame 5               - Y coordinates: 198 to 229
        Frame 4               - Y coordinates: 231 to 262
        Frame 3               - Y coordinates: 264 to 295
        Frame 2               - Y coordinates: 297 to 328
        Frame 1               - Y coordinates: 330 to 361
        Frame 0 (bottom row)  - Y coordinates: 363 to 394
        
        第11帧（顶部行）- Y坐标：0到31
        第10帧          - Y坐标：33到64
        第9帧           - Y坐标：66到97
        第8帧           - Y坐标：99到130
        第7帧           - Y coordinates: 132 to 163
        第6帧           - Y坐标：165到196
        第5帧           - Y坐标：198到229
        第4帧           - Y坐标：231到262
        第3帧           - Y坐标：264到295
        第2帧           - Y坐标：297到328
        第1帧           - Y坐标：330到361
        第0帧（底部行）  - Y坐标：363到394
        
        Frame Content Description / 帧内容描述:
        Each frame can contain visual elements such as:
        - Progress indicators (circles, bars, or other shapes)
        - Character sprites or icons
        - Text or numbers
        - Decorative elements
        
        The frames are displayed sequentially during the countdown, with Frame 11 shown
        at the start and Frame 0 shown when the fish bites (!!! state).
        
        每一帧可以包含以下视觉元素：
        - 进度指示器（圆形、条形或其他形状）
        - 角色精灵图或图标
        - 文本或数字
        - 装饰元素
        
        帧在倒计时期间按顺序显示，第11帧在开始时显示，第0帧在鱼咬钩时显示（!!!状态）。
        
        ------------------------------------------------------------
        Step 3: Sound Effects (Optional) / 第三步：音效（可选）
        ------------------------------------------------------------
        
        The mod provides sounds.json. Add ogg files in your resource pack at:
        模组已提供 sounds.json，在资源包中放置 ogg 文件即可：
        assets/fishtimer/sounds/
        
        您可以通过在 assets/fishtimer/sounds/ 目录下创建音效文件来为每一帧添加音效。
        
        Sound File Naming / 音效文件命名:
        - Format: fishing_timer_<frame_number>.ogg
        - Frame numbers: 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
        - Total files: 12 sound files (one for each frame)
        
        - 格式：fishing_timer_<帧编号>.ogg
        - 帧编号：0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
        - 总文件数：12个音效文件（每帧一个）
        
        Example file names / 示例文件名:
        fishing_timer_0.ogg   (plays when frame 0 is displayed - bite state)
        fishing_timer_1.ogg   (plays when frame 1 is displayed)
        fishing_timer_2.ogg   (plays when frame 2 is displayed)
        ... (continues for all 12 frames)
        fishing_timer_11.ogg  (plays when frame 11 is displayed - start)
        
        fishing_timer_0.ogg   （第0帧显示时播放 - 咬钩状态）
        fishing_timer_1.ogg   （第1帧显示时播放）
        fishing_timer_2.ogg   （第2帧显示时播放）
        ... （所有12帧都继续）
        fishing_timer_11.ogg  （第11帧显示时播放 - 开始）
        
        Sound File Requirements / 音效文件要求:
        - Format: OGG Vorbis (.ogg)
        - Sample rate: Recommended 44100 Hz or 22050 Hz
        - Bit depth: 16-bit recommended
        - Channels: Mono or Stereo (both supported)
        
        - 格式：OGG Vorbis (.ogg)
        - 采样率：推荐44100 Hz或22050 Hz
        - 位深度：推荐16位
        - 声道：单声道或立体声（两者都支持）
        
        Note: Sound effects are optional. If a sound file is missing for a frame,
        no sound will play for that frame.
        
        注意：音效是可选的。如果某一帧的音效文件缺失，该帧将不会播放音效。
        
        ------------------------------------------------------------
        Step 4: Enable Resource Pack / 第四步：启用资源包
        ------------------------------------------------------------
        
        1. Place your resource pack in the resourcepacks folder
        2. Open Minecraft Options -> Resource Packs
        3. Move your pack to the "Selected" column
        4. The timer UI will automatically use your custom texture and sounds
        
        1. 将资源包放入resourcepacks文件夹
        2. 打开Minecraft选项 -> 资源包
        3. 将您的资源包移动到"已选择"列
        4. 计时器UI将自动使用您的自定义纹理和音效
        
        ============================================================
        """;
    
    public void tick() {
        if (!ConfigManager.fishHookTimerEnabled) {
            clearTimerState();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clearTimerState();
            return;
        }
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        if (!main.is(Items.FISHING_ROD) && !off.is(Items.FISHING_ROD)) {
            clearTimerState();
            return;
        }
        FishingHook hook = findOwnHook(mc);
        if (hook == null) {
            clearTimerState();
            return;
        }
        List<ArmorStand> stands = mc.level.getEntitiesOfClass(
            ArmorStand.class,
            hook.getBoundingBox().inflate(5.0),
            s -> s.isCustomNameVisible() && s.hasCustomName()
        );
        boolean found = false;
        for (ArmorStand s : stands) {
            String nameStr = s.getName().getString();
            if (nameStr == null || nameStr.isEmpty()) nameStr = s.getDisplayName().getString();
            if (BITE_MARKER.equals(nameStr)) {
                currentTick = 0;
                biteMode = true;
                found = true;
                break;
            }
            var m = NUMERIC_PATTERN.matcher(nameStr);
            if (m.find()) {
                try {
                    double v = Double.parseDouble(m.group(1));
                    int t = (int)(v * 10.0);
                    if (t >= 1 && t <= 40) {
                        currentTick = t;
                        biteMode = false;
                        found = true;
                        break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (!found) clearTimerState();
    }

    private FishingHook findOwnHook(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;
        return mc.level.getEntitiesOfClass(
            FishingHook.class,
            mc.player.getBoundingBox().inflate(50.0),
            h -> h.getOwner() == mc.player
        ).stream().findFirst().orElse(null);
    }

    private void clearTimerState() {
        currentTick = -1;
        biteMode = false;
        lastSoundFrame = -1;
    }

    private void tryPlayFrameSound(int frameIndex) {
        if (frameIndex < 0 || frameIndex > 11) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        
        if (FRAME_SOUNDS[frameIndex] == null) return;
        
        mc.level.playSound(
            mc.player,
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            FRAME_SOUNDS[frameIndex],
            SoundSource.PLAYERS,
            1.0f,
            1.0f
        );
    }
    
    public static boolean isFishingTimerArmorStand(String nameStr) {
        if (nameStr == null || nameStr.isEmpty()) return false;
        if (BITE_MARKER.equals(nameStr)) return true;
        var m = NUMERIC_PATTERN.matcher(nameStr);
        if (!m.find()) return false;
        try {
            double v = Double.parseDouble(m.group(1));
            int t = (int)(v * 10.0);
            return t >= 1 && t <= 40;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public String getId() {
        return "fishHookTimer";
    }
    
    @Override
    public String getDisplayName() {
        return "FishHookTimer";
    }
    
    @Override
    public double getX() {
        return ConfigManager.fishHookTimerX;
    }
    
    @Override
    public void setX(double x) {
        ConfigManager.fishHookTimerX = x;
    }
    
    @Override
    public double getY() {
        return ConfigManager.fishHookTimerY;
    }
    
    @Override
    public void setY(double y) {
        ConfigManager.fishHookTimerY = y;
    }
    
    @Override
    public float getScale() {
        return ConfigManager.fishHookTimerScale;
    }
    
    @Override
    public void setScale(float scale) {
        ConfigManager.fishHookTimerScale = Math.max(0.1f, Math.min(10.0f, scale));
    }
    
    @Override
    public double getDefaultX() {
        return FishHookTimerConfig.DEFAULT_X;
    }
    
    @Override
    public double getDefaultY() {
        return FishHookTimerConfig.DEFAULT_Y;
    }
    
    @Override
    public float getDefaultScale() {
        return FishHookTimerConfig.DEFAULT_SCALE;
    }
    
    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    @Override
    public boolean isClicked() {
        return clicked;
    }
    
    @Override
    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }
    
    @Override
    public int getWidth() {
        return FRAME_W;
    }

    @Override
    public int getHeight() {
        return FRAME_H;
    }

    @Override
    public boolean shouldRender() {
        if (!ConfigManager.fishHookTimerEnabled || currentTick < 0) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        if (!main.is(Items.FISHING_ROD) && !off.is(Items.FISHING_ROD)) return false;
        FishingHook hook = findOwnHook(mc);
        if (hook == null) return false;
        List<ArmorStand> stands = mc.level.getEntitiesOfClass(
            ArmorStand.class,
            hook.getBoundingBox().inflate(5.0),
            s -> s.isCustomNameVisible() && s.hasCustomName()
        );
        for (ArmorStand s : stands) {
            String nameStr = s.getName().getString();
            if (nameStr == null || nameStr.isEmpty()) nameStr = s.getDisplayName().getString();
            if (isFishingTimerArmorStand(nameStr)) return true;
        }
        return false;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, float partialTicks) {
        if (!ConfigManager.fishHookTimerEnabled || currentTick < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        int w = (int)(FRAME_W * getScale());
        int h = (int)(FRAME_H * getScale());
        int px = getAbsX(w);
        int py = getAbsY(h);
        boolean useTexture = false;
        try {
            var texId = Identifier.fromNamespaceAndPath(ASSET_NAMESPACE, BAR_TEXTURE_PATH);
            if (mc.getResourceManager().getResource(texId).isPresent()) useTexture = true;
        } catch (Exception ignored) {}
        if (useTexture) {
            int frame = biteMode ? 0 : Math.min(currentTick, 11);
            if (frame != lastSoundFrame) {
                tryPlayFrameSound(frame);
                lastSoundFrame = frame;
            }
            var matrices = guiGraphics.pose();
            matrices.pushMatrix();
            matrices.translate((float)px, (float)py);
            matrices.scale(getScale(), getScale());
            guiGraphics.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath(ASSET_NAMESPACE, BAR_TEXTURE_PATH),
                0, 0, 0, FRAME_STRIDE * (11 - frame),
                FRAME_W, FRAME_H, SHEET_W, SHEET_H
            );
            matrices.popMatrix();
        } else {
            String txt = biteMode ? "§c§l!!!" : String.format("§e§l%.1f", currentTick / 10.0);
            float scale = (float)w / (float)mc.font.width(txt) * 0.27f;
            var matrices = guiGraphics.pose();
            matrices.pushMatrix();
            matrices.translate((float)(px + w / 2.0), (float)(py + h / 2.0));
            matrices.scale(scale, scale);
            int tw = mc.font.width(txt);
            guiGraphics.drawString(mc.font, txt, -tw / 2, -mc.font.lineHeight / 2, 0xFFFFFFFF, false);
            matrices.popMatrix();
        }
    }
}
