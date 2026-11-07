# 更新记录

本项目的版本号与 `gradle.properties` 中的 `mod_version` 保持一致，遵循语义化版本。

## [1.0.3] - 2026-11-07

> 当前版本尚未创建 `v1.0.3` tag / GitHub Release。

### 新增

- 电路蚀刻器、晶能聚合器改为扫描对应配方目录，新增 JSON 即自动加载，不再维护 Java 文件名清单。
- AE2-Supergiant 并发处理器配方（电路蚀刻器 + 晶能聚合器）。
- AE2 Additions 量子电路板 / 量子处理器配方（压印器 + 电路蚀刻器 + 晶能聚合器）。
- appflux 能量处理器配方、充能红石水晶配方。
- AE2 熵变配方：AE2 石英块 → 充能赛特斯石英块；AE2LT 过载水晶块 → 充能过载水晶块（需安装 AE2LT）。
- 恢复接口、样板供应器等 GUI 的菜单背景贴图。
- 构建产物文件名带版本号。

### 修复

- appflux 红石水晶粉配方重复/错误：红石水晶与高纯红石水晶现在分别使用正确输入。
- AE2S 恩特罗粉、陨石粉配方（原来错误指向 ExtendedAE）。
- 恩特罗种子、恩特罗母岩、拓展机械框架配方由 ExtendedAE 迁移到 AE2S（`ae2:entro_*`、`ae2:machine_frame`）。
- 恩特罗种子配方改用对应的下界石英粉。
- CraftTweaker 运行时 API 缺失导致开发客户端启动崩溃。
- 中文“恩洛特水晶种子”笔误修正为“恩特罗水晶种子”。

## [1.0.2] - 2026-11-07

### 修复

- 正式安装包中 `AEBaseTileMixin` 只匹配 MCP `update`，导致 AE2S 的 `AEBaseTile` / `TileCableBus` 加载失败；现在同时匹配 MCP `update()V` 与 SRG `func_73660_a()V`。
- 键鼠输入改用 Minecraft / Forge 公开入口与 `MouseEvent`，移除对 LWJGLX / GLFW 的直接引用。
- 新增构建检查，阻止正式 JAR 再次包含直接输入后端引用。
- 调整 Mixin / CraftTweaker 依赖，避免旧 ASM / Gson 污染运行时。
- 支持通过 `-Pae2sDevJar=` 指定本地 AE2-Supergiant dev jar。

### 其他

- 新增 `tools/` 运行时回归工具与 `verification/1.0.2/` 验收记录。

## [1.0.0] - 2026-11-06

首个公开移植版本：

- Crystal Science 机器、产线与配套升级。
- AE2 网络集成：接口、样板供应器、存储、终端与自动化组件。
- 谐振系列物品：工具、样板、链接器、记忆卡与升级。
- AE2 指南书条目、JEI 配方查看、CraftTweaker 脚本支持。
- 创建 `v1.0.0` tag 与 GitHub Release。
