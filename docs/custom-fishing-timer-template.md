# Custom Fishing Timer Template / 自定义钓鱼计时器材质包

## Preface / 前言

Baity **FishHookTimer** draws a custom HUD bar while fishing on SkyBlock. Textures and per-frame sounds are loaded from a **resource pack** (namespace `fishtimer`), not from files inside the mod jar at runtime.

The initial idea for this feature originated from the module **rhizome**. However, as the author seems to have stopped updating, the subsequent **porting** was carried out. The default resource templates that followed also came from the author of this mod.

On first launch, Baity creates a template folder under `config/baity/`. Zip its contents, place the zip in `resourcepacks`, and enable it to use the default look and sounds.



Baity 的 **FishHookTimer** 在 SkyBlock 钓鱼时显示自定义 HUD 条。材质与逐帧音效通过**资源包**（命名空间 `fishtimer`）加载，而非运行时从模组 jar 内读取。

该功能创意最初来源于模组**rhizome**，不过由于作者似乎停止更新因此进行的**移植**，后面的默认资源模板也来源于该模组作者。

模组首次启动时会在 `config/baity/` 下生成模板文件夹。将其内容打包为 zip、放入 `resourcepacks` 并启用，即可使用默认外观与音效。

### Default template credit / 默认模板来源

The bundled default assets are adapted from the community pack **[Fishing Timer Pack Template](https://modrinth.com/resourcepack/fishing-timer-pack-template)** on Modrinth (Rhizome mod fishing timer). Baity remaps them to the `fishtimer` namespace required by this mod.

内置默认素材改编自 Modrinth 上的 **[Fishing Timer Pack Template](https://modrinth.com/resourcepack/fishing-timer-pack-template)**（Rhizome 钓鱼计时器资源包），并已改为本模组使用的 `fishtimer` 命名空间。

---

## Overview / 概述

| Item / 项目 | Value / 值 |
|---|---|
| Module / 模块 | **FishHookTimer** (parent toggle only / 仅受主模块开关控制) |
| GUI entry / 界面入口 | **custom timer template** → **Manage** |
| Template folder / 模板文件夹 | `<MC>/config/baity/baity-custom-fishing-timer/` |
| Texture path / 材质路径 | `assets/fishtimer/textures/skyblock/fishing_timer_bar.png` |
| Sounds / 音效 | `assets/fishtimer/sounds/fishing_timer_0.ogg` … `fishing_timer_11.ogg` |

When **FishHookTimer** is disabled, the HUD and frame sounds do not run even if the resource pack stays enabled.

关闭 **FishHookTimer** 主模块后，即使资源包仍启用，HUD 与帧音效也不会工作。

---

## Quick Start / 快速开始

1. Enable **FishHookTimer** in Baity ClickGUI (optionally enable **hide default timer**).  
   在 Baity 中开启 **FishHookTimer**（可按需开启 **hide default timer**）。

2. Launch the game once so Baity can create the template (only when the folder does not exist).  
   启动游戏一次以生成模板（仅当文件夹尚不存在时）。

3. Zip the **contents** of `baity-custom-fishing-timer`, put the zip in `resourcepacks`, and enable it.  
   将 `baity-custom-fishing-timer` **内部文件**打包为 zip，放入 `resourcepacks` 并启用。

4. Fish on SkyBlock with a supported rod; the custom bar appears when Hypixel’s timer armor stand is active.  
   在 SkyBlock 使用支持的鱼竿钓鱼；当 Hypixel 计时器盔甲架出现时显示自定义条。

---

## Template folder / 模板文件夹

```
<MC>/config/baity/baity-custom-fishing-timer/
├── pack.mcmeta
├── pack.png
└── assets/fishtimer/
    ├── sounds.json
    ├── textures/skyblock/fishing_timer_bar.png
    └── sounds/
        ├── fishing_timer_0.ogg
        ├── fishing_timer_1.ogg
        …
        └── fishing_timer_11.ogg
```

Baity creates this folder **only when** it does not exist. Later launches **do not** overwrite your edits. Delete the whole folder to regenerate defaults.

模组**仅当**文件夹不存在时创建模板；之后**不会**覆盖你的修改。删除整个文件夹可重新生成默认内容。

---

## Install the pack / 安装资源包

Baity does **not** write into `<MC>/resourcepacks/` automatically.

1. Open `config/baity/baity-custom-fishing-timer/`.
2. Select all files inside (`pack.mcmeta`, `pack.png`, `assets/`, …).
3. Zip them so `pack.mcmeta` is at the **root** of the zip (no extra wrapper folder).
4. Move the zip to `resourcepacks/`.
5. Enable it under **Options → Resource Packs**.

Baity **不会**自动在 `resourcepacks` 生成资源包，需按上述步骤手动打包安装。

---

## Texture: `fishing_timer_bar.png` / 材质规格

Vertical sprite sheet, **12 frames**, namespace path:

`assets/fishtimer/textures/skyblock/fishing_timer_bar.png`

| Property / 属性 | Value / 值 |
|---|---|
| Sheet size / 总尺寸 | 128 × 395 px |
| Frame size / 每帧 | 128 × 32 px |
| Row stride / 行间距 | 33 px (1 px gap between frames) |
| Frame index / 帧编号 | 0–11 |

**Vertical order (top → bottom) / 从上到下：**

| Frame | Y (px) | When shown / 显示时机 |
|---|---|---|
| 11 | 0–31 | Cast / start 抛竿开始 |
| 10 | 33–64 | Countdown 倒计时 |
| … | … | … |
| 1 | 330–361 | Countdown 倒计时 |
| 0 | 363–394 | Bite (`!!!`) 咬钩 |

The mod blits `frame = 11 - tick` during countdown and **frame 0** on bite.

倒计时期间显示第 11→0 帧，咬钩时固定为第 0 帧。

---

## Sounds (optional) / 音效（可选）

Fixed names (do not rename unless you change `sounds.json` and mod code):

| File | Plays when / 播放时机 |
|---|---|
| `fishing_timer_11.ogg` | Frame 11 (start) |
| `fishing_timer_10.ogg` … `fishing_timer_1.ogg` | Matching frames |
| `fishing_timer_0.ogg` | Frame 0 (bite) |

Place files in `assets/fishtimer/sounds/`. `sounds.json` in the template maps `fishing_timer_N` → `fishtimer:fishing_timer_N`.

Missing `.ogg` files are skipped silently for that frame.

缺失的 `.ogg` 不会报错，该帧 simply 不播放音效。

**Format / 格式:** OGG Vorbis; mono or stereo; 22050 Hz or 44100 Hz recommended.

---

## Customization tips / 自定义建议

- Replace only `fishing_timer_bar.png` to change visuals; keep dimensions and layout.
- Replace individual `fishing_timer_N.ogg` files to change sounds per stage.
- After edits, re-zip and replace the pack in `resourcepacks`, or edit the installed pack in place.

仅替换 `fishing_timer_bar.png` 可改外观，请保持尺寸与帧布局。修改音效后重新打包并启用资源包。

---

## Troubleshooting / 故障排除

| Problem / 问题 | Check / 检查 |
|---|---|
| Text fallback (`§e§l` numbers) instead of bar / 显示文字而非条 | Resource pack enabled; texture path correct |
| No frame sounds / 没有帧音效 | Pack enabled; `.ogg` names 0–11; FishHookTimer module on |
| Pack not listed / 资源包未出现 | Zip root contains `pack.mcmeta`; file in `resourcepacks` |
| No template / 没有模板 | Delete `baity-custom-fishing-timer` and relaunch once |