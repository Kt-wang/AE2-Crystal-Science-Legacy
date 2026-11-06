---
navigation:
  parent: index.md
  title: 谐振连接器
  icon: ae2csl:resonating_linker
  position: 151
item_ids:
  - ae2csl:resonating_linker
---

# 谐振连接器

<ItemImage id="ae2csl:resonating_linker" scale="3" />

**谐振连接器** 用于就地编辑[谐振样板供应器](resonating_pattern_provider.md)的**默认输入目标**。

## 用法

1. **右键谐振样板供应器**（方块或部件）完成绑定。
2. **右键任意方块面**，把它标记为当前默认输入槽的目标；再次右键同一面取消标记。
3. **潜行 + 滚轮**切换默认输入槽，聊天栏会提示槽位序号与当前目标。

默认目标作用于：谐振样板中**未单独标记**的输入，以及普通处理样板（按输入下标映射）。

绑定信息包含维度、坐标与部件面，重新进入世界后依然有效；绑定另一个供应器即可切换目标。

## 合成

<RecipeFor id="ae2csl:resonating_linker" />
