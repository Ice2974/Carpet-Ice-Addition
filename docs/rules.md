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

## 唱片世界事件时序修复 (recordWorldEventFix)

修复了将唱片快速放入唱片机后又迅速取出时，音乐仍可能继续播放，且多个唱片音频可能重叠的问题，详见 MC-112245。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`

## 刷怪笼忽略隐身玩家 (spawnersIgnoreInvisiblePlayers)

开启后，隐身玩家不会触发普通刷怪笼、试炼刷怪笼和不祥试炼刷怪笼。若附近存在可见玩家，仍可正常触发对应刷怪流程。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`
