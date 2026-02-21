# Third-Party Notices / 第三方声明

This project uses or references the following third-party resources:

本项目使用或引用了以下第三方资源：

---

## SkyHanni-REPO Data / SkyHanni-REPO 数据

This mod fetches enchantment data from the SkyHanni-REPO repository:
- Repository: https://github.com/hannibal002/SkyHanni-REPO
- License: LGPL-2.1
- Usage: Data only (JSON format), no code is directly used
- Storage: On successful remote fetch, data is saved to `baity/skyhanni-repo/Enchants.json` (baity folder is at game root, same level as config). If remote fails, users must manually download and place the file. See `baity/EnchantChroma_Data_Setup_Guide.txt` for instructions.

The enchantment tier determination logic is inspired by SkyHanni's implementation, but has been rewritten and adapted for this project.

本模组从 SkyHanni-REPO 仓库获取附魔数据：
- 仓库地址：https://github.com/hannibal002/SkyHanni-REPO
- 许可证：LGPL-2.1
- 使用方式：仅使用数据（JSON 格式），未直接使用任何代码
- 存储：远程获取成功时，数据保存至 `baity/skyhanni-repo/Enchants.json`（baity 文件夹在游戏根目录下，与 config 同级）。若远程失败，用户需手动下载并放置文件。操作说明见 `baity/EnchantChroma_Data_Setup_Guide.txt`。

附魔等级判定逻辑参考了 SkyHanni 的实现思路，但已在本项目中独立重写并适配。

---

---

## Rhizome Mod / Rhizome 模组

The custom texture feature for the FishHookTimer functionality was inspired by the Rhizome mod project.
- Mod page: https://modrinth.com/mod/rhizome
- License: All Rights Reserved (ARR)
- Usage: Inspiration only — no code or assets were directly used or copied

The resource pack texture customization concept (allowing users to customize the fishing timer UI via resource packs) was inspired by Rhizome's implementation. 

FishHookTimer 功能的自定义纹理特性由 Rhizome 模组项目启发。
- 模组页面：https://modrinth.com/mod/rhizome
- 许可证：All Rights Reserved (ARR)
- 使用方式：仅作为启发 — 未直接使用或复制任何代码或资源

资源包纹理自定义的概念（允许用户通过资源包自定义钓鱼计时器UI）由 Rhizome 的实现启发。

---

## Acknowledgments / 致谢

We would like to thank the following projects for inspiration:
- [SkyHanni Mod](https://github.com/hannibal002/SkyHanni) — For enchantment tier classification logic
- [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) — For providing enchantment data
- [Rhizome Mod](https://modrinth.com/mod/rhizome) — For the resource pack texture customization concept for fishing timer

All code in this project has been independently implemented and adapted to fit our architecture.

感谢以下项目带来的启发：
- [SkyHanni Mod](https://github.com/hannibal002/SkyHanni) — 附魔等级分类逻辑
- [SkyHanni-REPO](https://github.com/hannibal002/SkyHanni-REPO) — 附魔数据来源
- [Rhizome Mod](https://modrinth.com/mod/rhizome) — 钓鱼计时器资源包纹理自定义概念

本项目中的相关代码均为独立实现，并已适配本项目的架构。
