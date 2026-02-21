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
        Enchants.json exists in the baity/skyhanni-repo folder. If it does
        not exist, please follow the steps below to obtain and place it.

        如果您的 EnchantChroma 功能无法生效，请检查 baity/skyhanni-repo
        文件夹下是否存在 Enchants.json 文件。如果没有，请按以下流程操作。

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

        Put Enchants.json in: <Minecraft>/baity/skyhanni-repo/Enchants.json
        将 Enchants.json 放入：<Minecraft>/baity/skyhanni-repo/Enchants.json

        (baity folder is at game root, same level as config folder)
        （baity 文件夹在游戏根目录下，与 config 文件夹同级）

        Example (Windows): C:\\Users\\<You>\\AppData\\Roaming\\.minecraft\\baity\\skyhanni-repo\\Enchants.json
        示例 (Windows): C:\\Users\\<用户名>\\AppData\\Roaming\\.minecraft\\baity\\skyhanni-repo\\Enchants.json

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
                if (data != null && data.hasData()) source = "local";
            }

            if (data != null && data.hasData()) {
                CACHED.set(data);
            }
        }, "EnchantChroma-RepoLoader");
        t.setDaemon(true);
        t.start();
    }

    private static void ensureDirectoriesExist() {
        try {
            Files.createDirectories(getRepoDir());
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
        return FabricLoader.getInstance().getGameDir().resolve("baity");
    }

    private static Path getRepoDir() {
        return getBaityDir().resolve("skyhanni-repo");
    }

    private static Path getEnchantsPath() {
        return getRepoDir().resolve("Enchants.json");
    }

    private static String fetchFromRemoteRaw() throws IOException {
        String rawUrl = "https://raw.githubusercontent.com/" + REPO_USER + "/" + REPO_NAME + "/" + REPO_BRANCH + "/" + DATA_PATH;
        try (InputStream in = URI.create(rawUrl).toURL().openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void saveToLocal(String json) {
        try {
            Path path = getEnchantsPath();
            Files.writeString(path, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("[RepoManager] Could not save to local: {}", e.getMessage());
        }
    }

    private static EnchantDatabase loadFromLocal() {
        Path path = getEnchantsPath();
        if (!Files.isRegularFile(path)) return null;
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            return GSON.fromJson(json, EnchantDatabase.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static EnchantDatabase getData() {
        EnchantDatabase cached = CACHED.get();
        if (cached == null || !cached.hasData()) {
            EnchantDatabase local = loadFromLocal();
            if (local != null && local.hasData()) {
                CACHED.set(local);
                return local;
            }
        }
        return cached;
    }

    public static Path getEnchantsFileLocation() {
        return getEnchantsPath();
    }
}
