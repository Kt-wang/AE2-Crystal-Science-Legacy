# 1.0.2 键鼠输入热修复验收

2026-11-06。包含 `AEBaseTileMixin` 正式环境启动修复与键鼠输入兼容修复。

## 原因及修改

输入崩溃的调用链为 `MirrorLinkerListener.onKeyInput` → `org.lwjglx.input.Keyboard.isKeyDown`
→ `GLFW.glfwGetKey` → `NullPointerException`。绕过加载器的公开输入 API、
直接调用 `org.lwjglx` 运行时实现，会让输入代码绑定到特定后端。
[Cleanroom 官方移植文档](https://cleanroommc.com/wiki/cleanroom-mod-development/porting)
要求模组避免直接调用该运行时包。

1.0.2 的处理方式：

- 镜像连接器检查世界、玩家、界面、命中目标、手持物和潜行状态后，通过 Minecraft
  `GuiScreen.isCtrlKeyDown()` 检测修饰键。
- 频段 GUI 的鼠标调用使用公开的 `org.lwjgl.input.Mouse`，由加载器匹配实际输入后端。
- 谐振物品通过 Forge `MouseEvent.getDwheel()` 读取当前滚轮事件；发送槽位切换包后取消
  原版的快捷栏切换。去掉对全局滚轮状态的消费和 tick 末尾恢复快捷栏的补偿。
- LWJGL 2 `2.9.4-nightly-20150209` 为 `compileOnly`，不打包任何输入实现。
- `verifyPublicInputApi` 检查正式安装 JAR 的所有类；出现直接 `org/lwjglx/` 或
  `org/lwjgl/glfw/` 引用时使构建失败。该检查挂接到 `check` / `build`。

## 实际安装包验证

Minecraft 1.12.2、Java 25.0.5、官方 AE2S / Supergiant 1.1.0。
所有场景使用正式 SRG 模组 JAR；0.4.0 正式版仍为最低兼容验收基线。
安装包、前置、加载器和测试工具的 SHA-256 及启动退出状态见 [artifacts.json](artifacts.json)。
本目录的 `summary.json` 从完整验收输出中提取相关字段；逐次调用、完整启动日志及本机启动命令未纳入 Git。

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| 0.4.0 正式版客户端与集成服务器 | PASS | [报告](ae2csl-102-input-040-r1/summary.json) |
| 0.4.0 正式版客户端 + JEI 4.35.1 / CrT 4.1.20.716 | PASS | [报告](ae2csl-102-input-040-optional-r1/summary.json) |
| 0.4.0 正式版专服 | PASS | [报告](ae2csl-102-server-040-r1/summary.json) |
| 0.7.14-alpha 的 SDL 客户端与集成服务器 | PASS | [报告](ae2csl-102-input-sdl-r1/summary.json) |
| 1.0.1 + 旧 LWJGLX 的 SDL 负对照 | 旧输入异常，如预期 | [记录](ae2csl-101-input-sdl-negative-r4/summary.json) |

客户端测试在实际客户端线程上通过 Forge 事件总线触发键鼠事件，覆盖无玩家时的输入、
空手、手持镜像连接器并命中方块、物品栏界面、非潜行滚轮、潜行双向滚轮以及鼠标输入
同时转发 `KeyInputEvent` 的情况。轮询实际公开鼠标 API，检查两种供应器和记忆卡模型。
潜行滚轮通过真实客户端按键绑定配合服务器网络处理，确认记忆卡槽位 `0 → 1 → 0`，
快捷栏始终为 0。事件和网络响应摘要保留在各场景 `summary.json` 的 `inputTests` 中。

原供应器 tick 测试也继续通过：普通与扩展部件在 5 次宿主 tick 中分别运行 5 次逻辑调用，
客户端不运行服务端供应器逻辑；客户端放置方块实体并经过真实世界 tick，OpenGL 错误为 0。
专服额外使用仓库已有的 `Cleanroom-0.4.0-Server-Compatibility-1.0.0.jar`，处理加载器
自身的 Kirino 客户端类发现问题。

负对照额外放入旧 `lwjglxx-1.1.19.jar` 与 GLFW Java 库，以模拟仍残留旧输入实现的环境。
它在 `MirrorLinkerListener` 调用旧键盘类时触发 `NoSuchFieldError: FORCE_WAYLAND`，
发生在旧实现初始化阶段；该对照复现了直接依赖旧输入实现的故障，但不是截图中
`glfwGetKey` 空指针的逐帧复现。用户原整合包未在本地运行。

未模拟物理键盘持续按住 Ctrl，也未覆盖完整远程生产线或合成调度；此处的事件测试
覆盖本次崩溃入口和滚轮换槽路径。未验证其他 AE2S 版本、原版 AE2 或 AE2UEL。

## 复现

任意独立项目目录中均可构建隔离的运行时测试模组（需 Java 25 与供 AT 使用的 Java 8）：

```bash
./gradlew -I tools/runtime-regression.gradle build runtimeRegressionJar
```

将正式模组、AE2S 1.1.0 与 `build/verification/ae2csl-runtime-regression.jar` 安装到测试实例，
启动客户端并进入存档，检查工作目录中生成的 `ae2csl-runtime-regression.json` 与
`ae2csl-input-preflight.json`；专服另需处理上文的加载器兼容问题。
测试插件只生成报告，不会自行关闭游戏。输入测试请求格式见 [测试工具说明](../../tools/README.md)。

可选的自动化驱动用于已有 `mcp-testbridge`、`cleanroom-040-compat` 与加载器启动工具的
本地 mods 工作区；这些工具不作为模组运行依赖分发：

```bash
python3 tools/verify_runtime.py client --name input-040-new --bridge-port 18881 --display :0
python3 tools/verify_runtime.py server --name input-server-new --bridge-port 18882
python3 tools/verify_runtime.py client --name input-sdl-new --bridge-port 18884 --display :0 --loader-version 0.7.14-alpha
```

`--extra` 可重复传入实际可选模组 JAR。默认使用工作区的 0.4.0 安装器验证工具；
SDL 场景使用现有 `MMCE-Complement/verification/launch.py` 安装器验证工具。
每个场景必须使用新名称，防止旧报告误判。独立测试 JAR 不进入正式模组。

正式 JAR SHA-256：

```text
d80b232223e2060a1fead014d84a1130d916fbeb101899c5d7ae168405977559
```
