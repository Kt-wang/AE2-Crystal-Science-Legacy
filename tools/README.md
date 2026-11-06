# 运行时回归工具

普通 `./gradlew build` 会检查发布 JAR 中没有直接 `org.lwjglx` / GLFW 引用。
以下工具用于真实安装环境的 Mixin、tick 和输入检查，测试插件不打入正式模组。

## 独立安装检查

```bash
./gradlew -I tools/runtime-regression.gradle build runtimeRegressionJar
```

将正式 `AE2-Crystal-Science-Legacy-1.0.2.jar`、官方 AE2S 1.1.0 和
`build/verification/ae2csl-runtime-regression.jar` 放入测试实例 `mods/`。
使用 Java 25、Cleanroom 0.4.0 正式版启动并进入世界。专服必须先处理加载器自身
的 Kirino 客户端发现问题。测试插件不附带这个加载器补丁。

工作目录会生成：

- `ae2csl-runtime-regression.json`：公共 Mixin 类加载、正式 SRG tick 方法、两种供应器
  夹具各 5 次逻辑调用、空线缆 / 普通方块实体 / 客户端隔离。
- `ae2csl-input-preflight.json`：客户端线程通过 Forge 事件总线触发的键盘事件结果。

默认要求实际 Cleanroom 版本为 `0.4.0`。测试其他版本时明确添加 JVM 参数
`-Dae2csl.verification.cleanroom=0.7.14-alpha`；这不替代正式 0.4.0 的基线验收。

## 输入请求

进入世界后，将请求写入工作目录的 `ae2csl-input-request.json`，用递增 `id`
区分请求。插件在真实客户端线程处理，并原子写入 `ae2csl-input-response.json`：

```json
{"id": 1, "action": "key", "hitBlock": true}
```

支持以下操作：

| action | 参数 | 行为 |
| --- | --- | --- |
| `key` | `hitBlock`（可选） | 连续触发 8 次 Forge 键盘输入事件；可临时使用方块命中目标 |
| `wheel` | `delta`（如 `-120` 或 `120`） | 触发 Forge 鼠标滚轮，并转发键盘与旧鼠标输入事件 |
| `pollMouse` | 无 | 通过公开 LWJGL 2 API 读取鼠标状态，检查运行时重定向 |
| `inspect` | 无 | 返回快捷栏槽位与当前谐振记忆卡槽位 |

手持记忆卡并潜行时，正反滚轮应使内部槽位先变化再恢复，快捷栏不变。
这些请求不模拟物理键盘按住 Ctrl 的状态。

## 可选工作区自动化

`verify_runtime.py` 是本地 mods 工作区的自动化驱动，期望当前项目位于
`<workspace>/1.12.2/ae2-crystal-science-legacy`，并已有以下外部工具及产物：

- `<workspace>/tools/verify_ae2_addons.py` 和官方 0.4.0 安装器缓存。
- `<workspace>/.dev/ae2s-1.1.0/AppliedEnergistics2-Supergiant-1.1.0.jar`。
- 相邻 `mcp-testbridge`、`cleanroom-040-compat` 项目的构建产物。
- SDL 场景另用相邻 `MMCE-Complement/verification/launch.py` 的已锁定安装器。

这些外部工作区工具未包含在本仓库中；独立克隆可使用前述测试 JAR 和输入请求检查。
工作区驱动使用新场景目录，保存加载器、JAR 摘要、输入与 tick 报告，并请求测试桥正常退出。
