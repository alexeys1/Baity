# Custom Soulcry Sound / 自定义 Soulcry 音效

## Preface / 前言

After SkyBlock was upgraded to newer Minecraft versions, Hypixel admins removed the Atomsplit Katana-**Soulcry** ability sounds. Baity restores this experience using custom sounds loaded from a resource pack.

On first launch, Baity creates a resource-pack **template** under `config/baity/`. Zip that folder yourself, place the zip in `resourcepacks`, and enable it in **Options → Resource Packs** to use the default sounds.



在 SkyBlock 升级到更高版本后，Hypixel 管理员移除了 Atomsplit katana-**Soulcry** 技能的官方音效。Baity 通过资源包加载自定义音效来恢复这一体验。

模组首次启动时会在 `config/baity/` 下生成资源包**模板**。请自行将该文件夹打包为 zip，放入 `resourcepacks` 并在 **选项 → 资源包** 中启用，即可使用默认音效。

---



## Overview / 概述

| Item / 项目 | Value / 值 |

|---|---|

| Module / 模块 | **Sounds** (parent toggle only / 仅受主模块开关控制) |

| GUI entry / 界面入口 | **custom soulcry sound** → **Manage** (opens this guide / 打开本说明) |

| Template folder / 模板文件夹 | `<MC>/config/baity/baity-custom-soulcry-sounds/` |

| Open sound ID / 开启音效 ID | `baity:atomsplit_soulcry_open` |

| Close sound ID / 关闭音效 ID | `baity:atomsplit_soulcry_close` |



When the **Sounds** module is disabled, Soulcry playback stops even if the resource pack stays enabled. Re-enable **Sounds** to resume playback.

关闭 **Sounds** 主模块后，即使资源包仍处于启用状态，Soulcry 音效也不会播放。重新开启 **Sounds** 后即可恢复。

---



## Quick Start / 快速开始

1. Enable the **Sounds** module in Baity ClickGUI.  

   在 Baity 配置界面中开启 **Sounds** 模块。


2. Launch the game once so Baity can create the template folder (first launch only, when the folder does not exist).  

   启动游戏一次，让模组生成模板文件夹（仅当该文件夹尚不存在时）。


3. Zip the **contents** of `baity-custom-soulcry-sounds` (not the parent folder itself), name it e.g. `custom soulcry sound.zip`, and put it in `resourcepacks`.  

   将 `baity-custom-soulcry-sounds` **内部的文件**打包为 zip（zip 内不要多套一层文件夹），例如命名为 `custom soulcry sound.zip`，放入 `resourcepacks`。


4. Open **Options → Resource Packs** and enable your zip.  

   打开 **选项 → 资源包** 并启用该 zip。


5. Hold an **Atomsplit Katana** and use **Soulcry**.  

   手持 **Atomsplit Katana** 并使用 **Soulcry**。

---



## Template folder / 模板文件夹

Path / 路径:

```
<MC>/config/baity/baity-custom-soulcry-sounds/

├── pack.mcmeta

├── pack.png

└── assets/baity/

    ├── sounds.json

    └── sounds/

        ├── atomsplit_soulcry_open.ogg

        └── atomsplit_soulcry_close.ogg

```



**File naming:** MUST BE **atomsplit_soulcry_open.ogg** and **atomsplit_soulcry_close.ogg**.

**文件名规则：** 必须使用 **atomsplit_soulcry_open.ogg** 和 **atomsplit_soulcry_close.ogg**。



Baity creates this folder **only when** it does not exist yet. Edits inside are **not** overwritten on later launches. Delete the whole folder to regenerate defaults.

模组**仅当**该文件夹**不存在时**才会生成模板；其中的修改**不会**在后续启动时被覆盖。删除整个文件夹可让模组重新生成默认内容。



Normally you only replace the two `.ogg` files and leave `sounds.json` unchanged.

通常只需替换两个 `.ogg` 文件，无需改动 `sounds.json`。



---

## Install the pack / 安装资源包


Baity does **not** place anything in `<MC>/resourcepacks/` for you. You must zip and install manually:

Baity **不会**在 `<MC>/resourcepacks/` 中自动生成资源包，需要手动打包安装：



1. Open `config/baity/baity-custom-soulcry-sounds/`.

2. Select **all files inside** (`pack.mcmeta`, `pack.png`, `assets/`, …).

3. Compress to a `.zip` (the zip root must contain `pack.mcmeta`, not a subfolder named `baity-custom-soulcry-sounds`).

4. Move the zip to `<MC>/resourcepacks/`.

5. Enable it in **Options → Resource Packs**.



1. 打开 `config/baity/baity-custom-soulcry-sounds/`。

2. 选中**文件夹内的全部内容**（`pack.mcmeta`、`pack.png`、`assets/` 等）。

3. 压缩为 `.zip`（zip 根目录应直接包含 `pack.mcmeta`，不要再套一层 `baity-custom-soulcry-sounds` 文件夹）。

4. 将 zip 放入 `<MC>/resourcepacks/`。

5. 在 **选项 → 资源包** 中启用。



After editing the template, re-zip and replace the old zip in `resourcepacks`, or edit the installed pack directly.

修改模板后请重新打包并替换 `resourcepacks` 中的旧 zip，或直接编辑已安装的资源包。

---

## Troubleshooting / 故障排除



| Problem / 问题 | Check / 检查 |

|---|---|

| No sound / 没有声音 | **Sounds** module enabled; resource pack enabled in game; Master volume > 0 |

| Wrong sound / 音效不对 | Replace `.ogg` in template, re-zip, re-enable pack |

| Pack not listed / 资源包未出现 | Zip structure correct (`pack.mcmeta` at zip root); file is in `resourcepacks` |

| Template missing / 没有模板 | Delete `baity-custom-soulcry-sounds` and relaunch once |