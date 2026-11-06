# 启动与输入修复验证

正式安装包启动失败的原报告：[mclo.gs/3ne0HtI](https://mclo.gs/3ne0HtI)。

AE2S 的正式安装包将实现 Minecraft `ITickable.update()` 的方法改名为
`func_73660_a()`。原 `AEBaseTileMixin` 使用 `remap=false` 却只查找 `update`，
导致 `InvalidInjectionException`，后续表现为 `AEBaseTile` / `TileCableBus`
无法加载。修复同时匹配 MCP 与 SRG 方法，并要求恰好注入一次。

[1.0.2 验收记录](1.0.2/README.md)包含 Cleanroom **0.4.0 正式版**、Java 25、
官方 AE2S / Supergiant 1.1.0 的客户端、专服、部件 tick、键鼠输入及
JEI / CraftTweaker 共存验证，以及 0.7.14-alpha SDL 输入补充测试。
[artifacts.json](1.0.2/artifacts.json)记录安装包摘要与实际启动结果。

[运行时回归工具](../tools/README.md)可单独编译为测试模组，在真实安装环境中检查
Mixin、供应器 tick、键盘事件和滚轮换槽。测试模组不打入正式 JAR。
