## 脚手架防误触 (safeScaffoldingBreak)

玩家只有在主手拿着脚手架或者主手为空手时才能破坏脚手架。如果玩家尝试持其他物品破坏脚手架，系统将阻止该操作，并在 **Action Bar** 上显示提示。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`

## 合成器输出阻塞时停止合成 (crafterStopsWhenOutputBlocked)

当合成器输出目标容器无法完整接收本次产物时，停止本次合成，避免物品喷出。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 唱片世界事件时序修复 (recordWorldEventFix)

修复唱片快速放入并取出后音乐仍继续播放、或多张唱片音频重叠的问题。详见 MC-112245。
（此规则最初移植自Carpet-Fixes，现已按本模组需求调整）

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`

## 刷怪笼忽略隐身玩家 (spawnersIgnoreInvisiblePlayers)

普通刷怪笼、试炼刷怪笼和不祥试炼刷怪笼在检测附近玩家时忽略隐身玩家。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 禁用海带自然生长 (disableKelpNaturalGrowth)

禁用海带由随机刻触发的自然生长，不影响骨粉催熟。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 可采集紫水晶母岩 (canMineBuddingAmethyst)

使用带有精准采集附魔的合适工具可采集紫水晶母岩。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 假人Tab栏名称前缀 (botTabListNamePrefix)

为 Tab 栏中的假人名称添加前缀，支持使用 `&` 作为颜色/格式代码前缀。
（此规则最初移植自Carpet-TCTC-Addition，现已按本模组需求调整）

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, `[Bot] `
- 分类: `ICE`

## 假人Tab栏名称后缀 (botTabListNameSuffix)

为 Tab 栏中的假人名称添加后缀，支持使用 `&` 作为颜色/格式代码前缀。
（此规则最初移植自Carpet-TCTC-Addition，现已按本模组需求调整）

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, ` [Fake]`
- 分类: `ICE`

## 假人免疫荆棘反伤 (fakePlayerIgnoreThornsDamage)

假人在攻击带有荆棘附魔的生物或玩家时，不会受到荆棘造成的反伤。真人玩家保持原版行为。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 禁止伤害已驯服生物 (disablePetAttacking)

禁用玩家对已驯服生物造成伤害，防止误伤。仅拦截玩家来源伤害；未驯服生物与非玩家来源伤害保持原版行为。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`
