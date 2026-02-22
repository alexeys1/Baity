package com.shyeuar.baity.features.enchantchroma.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@Environment(EnvType.CLIENT)
public class EnchantLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/EnchantChroma/RepoManager");

    private static final String REPO_USER = "hannibal002";
    private static final String REPO_NAME = "SkyHanni-REPO";
    private static final String REPO_BRANCH = "main";
    private static final String DATA_PATH = "constants/Enchants.json";

    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    private static final AtomicReference<EnchantDatabase> CACHED = new AtomicReference<>(new EnchantDatabase());
    private static volatile boolean initialized = false;

    private static final String GUIDE_FILENAME = "EnchantChroma_Data_Setup_Guide.txt";

    private static final String GUIDE_CONTENT = """
        ============================================================
        EnchantChroma Data Setup Guide / 附魔染色数据配置指南
        ============================================================

        If your EnchantChroma feature is not working, please check whether
        Enchants.json exists in the following locations (in priority order):
        1. config/skyhanni/repo/constants/Enchants.json
        2. config/baity/skyhanni-repo/Enchants.json
        If it does not exist, please follow the steps below to obtain and place it.

        如果您的 EnchantChroma 功能无法生效，请检查以下位置（按优先级顺序）
        是否存在 Enchants.json 文件：
        1. config/skyhanni/repo/constants/Enchants.json
        2. config/baity/skyhanni-repo/Enchants.json
        如果没有，请按以下流程操作。

        ------------------------------------------------------------
        Step 1: Find the data source / 第一步：找到数据来源
        ------------------------------------------------------------

        The enchant data comes from SkyHanni-REPO (NOT the SkyHanni mod repo):
        附魔数据来自 SkyHanni-REPO（不是 SkyHanni 模组仓库）：

        Repository / 仓库: https://github.com/hannibal002/SkyHanni-REPO
        File path / 文件路径: constants/Enchants.json

        ------------------------------------------------------------
        Step 2: Download Enchants.json / 第二步：下载 Enchants.json
        ------------------------------------------------------------

        Method A - Direct link / 方法A - 直接链接:
        https://raw.githubusercontent.com/hannibal002/SkyHanni-REPO/main/constants/Enchants.json

        Right-click the link -> "Save link as..." -> Save as Enchants.json
        右键链接 -> 「链接另存为」 -> 保存为 Enchants.json

        Method B - From GitHub / 方法B - 从 GitHub 下载:
        1. Open https://github.com/hannibal002/SkyHanni-REPO
        2. Go to constants -> Enchants.json
        3. Click the Raw button (top right)
        4. Right-click page -> "Save as..." -> Save as Enchants.json

        1. 打开 https://github.com/hannibal002/SkyHanni-REPO
        2. 进入 constants -> Enchants.json
        3. 点击右上角 Raw 按钮
        4. 右键页面 -> 「另存为」 -> 保存为 Enchants.json

        ------------------------------------------------------------
        Step 3: Place the file / 第三步：放置文件
        ------------------------------------------------------------

        Priority order for Enchants.json / Enchants.json 优先级顺序:
        1. config/skyhanni/repo/constants/Enchants.json (SkyHanni mod repo location)
        2. config/baity/skyhanni-repo/Enchants.json (Baity mod repo location)

        If you have SkyHanni mod installed, place it in:
        如果您安装了 SkyHanni 模组，请放入：
        <Minecraft>/config/skyhanni/repo/constants/Enchants.json

        Otherwise, place it in:
        否则，请放入：
        <Minecraft>/config/baity/skyhanni-repo/Enchants.json

        Example (Windows): C:\\Users\\<You>\\AppData\\Roaming\\.minecraft\\config\\baity\\skyhanni-repo\\Enchants.json
        示例 (Windows): C:\\Users\\<用户名>\\AppData\\Roaming\\.minecraft\\config\\baity\\skyhanni-repo\\Enchants.json

        ------------------------------------------------------------
        Step 4: Restart the game / 第四步：重启游戏
        ------------------------------------------------------------

        Restart Minecraft for the changes to take effect.
        Note: Data is fetched once per game session. To retry remote fetch, restart the game.
        重启 Minecraft 使更改生效。
        注意：数据每次游戏启动时仅拉取一次。若要重新尝试远程拉取，请重启游戏。

        ============================================================
        """;

    public static void init() {
        if (initialized) return;
        initialized = true;
        ensureDirectoriesExist();
        ensureGuideExists();
        loadAsync();
    }

    private static void loadAsync() {
        Thread t = new Thread(() -> {
            EnchantDatabase data = null;
            String source = null;

            try {
                String rawJson = fetchFromRemoteRaw();
                data = GSON.fromJson(rawJson, EnchantDatabase.class);
                if (data != null && data.hasData()) {
                    saveToLocal(rawJson);
                    source = "remote";
                }
            } catch (Exception e) {
                LOGGER.warn("[RepoManager] Remote fetch failed, loading local copy if available");
            }

            if (data == null || !data.hasData()) {
                data = loadFromLocal();
                if (data != null && data.hasData()) {
                    source = "local";
                }
            }

            if (data != null && data.hasData()) {
                CACHED.set(data);
                if (source != null) {
                    LOGGER.info("[RepoManager] Loaded enchant data from {}", source);
                }
            }
        }, "EnchantChroma-RepoLoader");
        t.setDaemon(true);
        t.start();
    }

    private static void ensureDirectoriesExist() {
        try {
            Files.createDirectories(getBaityRepoDir());
        } catch (Exception e) {
            LOGGER.warn("[RepoManager] Could not create skyhanni-repo directory: {}", e.getMessage());
        }
    }

    private static void ensureGuideExists() {
        try {
            Path baityDir = getBaityDir();
            Path guidePath = baityDir.resolve(GUIDE_FILENAME);
            if (!Files.isRegularFile(guidePath)) {
                Files.createDirectories(baityDir);
                Files.writeString(guidePath, GUIDE_CONTENT, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.warn("[RepoManager] Could not create guide: {}", e.getMessage());
        }
    }

    private static Path getBaityDir() {
        return com.shyeuar.baity.config.BaityConfigDir.getBaityConfigDir();
    }

    private static Path getSkyHanniRepoPath() {
        Path configDir = com.shyeuar.baity.config.BaityConfigDir.getConfigDir();
        return configDir.resolve("skyhanni").resolve("repo").resolve("constants").resolve("Enchants.json");
    }

    private static Path getBaityRepoDir() {
        return getBaityDir().resolve("skyhanni-repo");
    }

    private static Path getBaityEnchantsPath() {
        return getBaityRepoDir().resolve("Enchants.json");
    }

    private static String fetchFromRemoteRaw() throws IOException {
        String rawUrl = "https://raw.githubusercontent.com/" + REPO_USER + "/" + REPO_NAME + "/" + REPO_BRANCH + "/" + DATA_PATH;
        try (InputStream in = URI.create(rawUrl).toURL().openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void saveToLocal(String json) {
        try {
            Path path = getBaityEnchantsPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("[RepoManager] Could not save to local: {}", e.getMessage());
        }
    }

    private static EnchantDatabase loadFromLocal() {
        Path skyHanniPath = getSkyHanniRepoPath();
        if (Files.isRegularFile(skyHanniPath)) {
            EnchantDatabase data = loadFromPath(skyHanniPath);
            if (data != null && data.hasData()) {
                LOGGER.debug("[RepoManager] Loaded from SkyHanni repo: {}", skyHanniPath);
                return data;
            }
        }

        Path baityPath = getBaityEnchantsPath();
        if (Files.isRegularFile(baityPath)) {
            EnchantDatabase data = loadFromPath(baityPath);
            if (data != null && data.hasData()) {
                LOGGER.debug("[RepoManager] Loaded from baity repo: {}", baityPath);
                return data;
            }
        }

        return null;
    }

    private static EnchantDatabase loadFromPath(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.trim().isEmpty()) {
                LOGGER.warn("[RepoManager] Local file is empty: {}", path);
                return null;
            }
            
            EnchantDatabase data = GSON.fromJson(json, EnchantDatabase.class);
            if (data != null && data.hasData()) {
                LOGGER.debug("[RepoManager] Successfully loaded local file. Data entries: normal={}, ultimate={}, stacking={}", 
                    data.normal != null ? data.normal.size() : 0,
                    data.ultimate != null ? data.ultimate.size() : 0,
                    data.stacking != null ? data.stacking.size() : 0);
                return data;
            } else {
                LOGGER.warn("[RepoManager] Local file loaded but has no valid data: {}", path);
                return data;
            }
        } catch (Exception e) {
            LOGGER.error("[RepoManager] Error loading local file: {}", e.getMessage(), e);
            return null;
        }
    }

    public static EnchantDatabase getData() {
        EnchantDatabase local = loadFromLocal();
        if (local != null && local.hasData()) {
            CACHED.set(local);
            return local;
        }
        
        EnchantDatabase cached = CACHED.get();
        return cached != null ? cached : new EnchantDatabase();
    }

    public static Path getEnchantsFileLocation() {
        Path skyHanniPath = getSkyHanniRepoPath();
        if (Files.isRegularFile(skyHanniPath)) {
            return skyHanniPath;
        }
        return getBaityEnchantsPath();
    }

    public static void forceReload() {
        EnchantDatabase local = loadFromLocal();
        if (local != null && local.hasData()) {
            CACHED.set(local);
            LOGGER.info("[RepoManager] Force reloaded enchant data from local file");
        } else {
            LOGGER.warn("[RepoManager] Force reload failed: no valid local file found");
        }
    }
}
