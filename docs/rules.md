## 脚手架防误触 (safeScaffoldingBreak)

玩家只有在主手拿着脚手架或者主手为空手时才能破坏脚手架。如果玩家尝试持其他物品破坏脚手架，系统将阻止该操作，并在 **Action Bar** 上显示提示。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `ICE`, `SURVIVAL`, `FEATURE`

## 合成器输出阻塞时停止合成 (crafterStopsWhenOutputBlocked)

当合成器输出目标容器无法接收产物时，停止合成并取消原料消耗。此功能可以防止合成器因目标容器无法接收全部产物而导致物品溢出。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `ICE`, `FEATURE`