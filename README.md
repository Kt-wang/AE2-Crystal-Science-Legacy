# AE2 Crystal Science: Legacy（AE2 水晶科技：Legacy）

**Applied Energistics: Crystal Science 的 Minecraft 1.12.2 / Cleanroom 非官方移植版。**

本移植基于上游 1.21.1 版本，运行在
[AE2-Supergiant](https://github.com/FormlessDragon/Applied-Energistics-2-Supergiant)
（AE2 的 1.12.2 Cleanroom 重构分支）之上。

> 本项目与上游 AECS 作者、Applied Energistics 2 团队均无隶属或背书关系。

## 基本信息

| 项目 | 值 |
|---|---|
| 版本 | **1.0.3** |
| Minecraft | 1.12.2 |
| 加载器 | Cleanroom Loader **0.4.0 正式版** |
| 前置 | AE2-Supergiant **1.1.0**（实际验收版本） |
| 可选 | JEI 4.35.1、CraftTweaker 4.1.20.716 |
| Mod ID | `ae2csl` |
| 包名 | `ae2csl.*` |

## 内容概览

- 完整的 Crystal Science 机器、产线与配套升级。
- 与 AE2 网络集成：接口、样板供应器、存储、终端与自动化组件。
- 谐振系列物品：工具、样板、链接器、记忆卡与升级。
- AE2 指南书条目（中 / 英 / 日）、JEI 配方查看、CraftTweaker 脚本支持。

> 具体机器、配方与用法请以游戏内 AE2 指南书和 JEI 为准。

## 安装

1. 准备 Minecraft 1.12.2 + Cleanroom Loader 0.4.0 正式版实例，使用 Java 25。
2. 将 `AE2-Crystal-Science-Legacy-1.0.3.jar` 与 **AE2-Supergiant** 放入 `mods/`；更新时移除旧版 Crystal Science JAR。
3. 可选：加入 JEI（配方查看）或 CraftTweaker（脚本）。

## 构建

需要 **JDK 25**，使用仓库自带的 Gradle Wrapper：

```bash
./gradlew build      # 产物：build/libs/AE2-Crystal-Science-Legacy-1.0.3.jar
./gradlew runClient  # 开发客户端
./gradlew runServer  # 开发服务端
```

Unimined 的 Access Transformer 构建工具还需要 JDK 8；Gradle 可通过 Foojay 自动获取，
也可用 `-Porg.gradle.java.installations.paths=/path/to/jdk8,/path/to/jdk25` 指定本地工具链。
若已有 AE2S 官方 1.1.0 `-dev.jar`，可用
`-Pae2sDevJar=/path/to/AppliedEnergistics2-Supergiant-1.1.0-dev.jar` 替代 JitPack 编译依赖。

## 1.0.2 修复

修复输入事件中的底层键盘实现兼容问题。镜像连接器通过 Minecraft 的 Ctrl 检测入口工作，
并在检测按键前检查世界、玩家、界面和手持物；所有频段界面的鼠标调用都使用公开的
LWJGL 2 API，由 Cleanroom 路由到实际输入后端。LWJGL 2 只参与编译，不打包进模组。

谐振物品从 Forge `MouseEvent` 读取当前滚轮增量，并取消原版快捷栏切换，
保持“潜行 + 滚轮换槽”的行为。构建会检查正式 JAR，阻止再次引入对 `org.lwjglx`
或 GLFW 的直接引用。遵循 [Cleanroom 官方移植约定](https://cleanroommc.com/wiki/cleanroom-mod-development/porting)。

输入事件与客户端、专服验收记录见 [verification/1.0.2/README.md](verification/1.0.2/README.md)。

## 保留的启动修复

修复正式安装包启动时 `AEBaseTileMixin` 找不到 `update`、继而导致 AE2S 的
`AEBaseTile` / `TileCableBus` 无法加载的崩溃。注入同时匹配 MCP `update()V`
和 SRG `func_73660_a()V`，并要求恰好执行一次，保留谐振样板供应器的服务端 tick。
编译依赖使用 Cleanroom 0.4.0 的 Mixin API，并阻止 CraftTweaker
间接带入旧 ASM / Gson。

启动故障原因与当前验收索引见 [verification/README.md](verification/README.md)。
专用服务器验收同时安装了仓库中的
`Cleanroom-0.4.0-Server-Compatibility-1.0.0.jar`，用于处理加载器自身的客户端类发现问题；
该补丁独立于本次 Crystal Science 修复。未验证原版 AE2 或 AE2UEL。

## 原作者与致谢

本移植版的所有原始内容均来自以下项目与作者，感谢他们的工作：

**Applied Energistics: Crystal Science（上游）**
- 代码：Frostbite-time、Lounode
- 贴图：MHanHanBing
- 3D 模型：TedXenon
- 仓库：<https://github.com/Frostbite-time/AE2-Crystal-Science>

**Applied Energistics 2**
- 代码：AlgorithmX2 et al.（2013–2020）
- 贴图 / 模型：Ridanisaurus Rid、AlgorithmX2 et al.
- 仓库：<https://github.com/AppliedEnergistics/Applied-Energistics-2>

**AE2-Supergiant**
- 1.12.2 Cleanroom 分支：FormlessDragon
- 仓库：<https://github.com/FormlessDragon/Applied-Energistics-2-Supergiant>

**1.12.2 移植与后续修改**
- KleinBlue

完整的版权、修改说明与第三方声明见 [LICENSE](LICENSE)。

## 许可

沿用上游 AE2CS 的多许可证结构：

- **源代码**：[LGPL-3.0-or-later](LICENSES/LGPL-3.0.txt)
- **贴图与模型**：[CC BY-NC-SA 3.0](LICENSES/CC-BY-NC-SA-3.0.txt)
- **版权与修改说明**：[LICENSE](LICENSE)

贴图与模型不可用于商业用途；转载或修改需保留署名，并以相同许可共享。
