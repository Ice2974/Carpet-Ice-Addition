> 文档对应版本：`v1.9.0`

## 脚手架防误拆 (safeScaffoldingBreak)

只有主手持脚手架或主手为空时，玩家才能破坏脚手架，防止误拆。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`

## 可隐形展示框 (invisibleItemFrames)

允许玩家用幻翼膜右键已放入物品的物品展示框或荧光物品展示框，使其隐形。
(此规则最初移植自 carpet-redcraft-addons，并由原 betterItemFrames 规则拆分而来。)

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 可固定展示框 (fixedItemFrames)

允许玩家用玻璃板右键已放入物品的物品展示框或荧光物品展示框，使其固定；固定后的展示框可用斧头右键解除固定。
(此规则最初移植自 carpet-redcraft-addons，并由原 betterItemFrames 规则拆分而来。)

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 合成器输出阻塞时停止合成 (crafterStopsWhenOutputBlocked)

当合成器面朝容器无法完整接收主产物时，阻止本次合成；合成余物保持原版处理。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 可合成珊瑚块 (craftableCoralBlocks)

允许使用 9 个同种类珊瑚扇合成对应的珊瑚块，死珊瑚扇同样生效。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 唱片世界事件时序修复 (recordWorldEventFix)

修复了将唱片快速放入唱片机后又迅速取出时，音乐仍可能继续播放，且多个唱片音频可能重叠的问题。详见 MC-112245。
(此规则最初移植自 Carpet-Fixes，现已按本模组需求调整。)

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`

## 刷怪笼忽略隐身玩家 (spawnersIgnoreInvisiblePlayers)

普通刷怪笼、试炼刷怪笼和不祥试炼刷怪笼在判定附近玩家时会忽略隐身玩家。

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

## 类冰岩浆块 (iceLikeMagmaBlocks)

允许玩家使用不带精准采集的工具破坏岩浆块时，根据类似冰块的判定逻辑，在下方为有效固体方块或液体方块时生成岩浆源。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 禁用菌岩退化 (disableNyliumDecay)

绯红菌岩和诡异菌岩在上方被方块遮挡时不会退化为下界岩。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 假人Tab栏名称前缀 (botTabListNamePrefix)

为 Tab 栏中的假人添加前缀，使用 `&` 来表示文字颜色。
(此规则最初移植自 Carpet-TCTC-Addition，现已按本模组需求调整。)

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, `[Bot] `
- 分类: `ICE`

## 假人Tab栏名称后缀 (botTabListNameSuffix)

为 Tab 栏中的假人添加后缀，使用 `&` 来表示文字颜色。
(此规则最初移植自 Carpet-TCTC-Addition，现已按本模组需求调整。)

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, ` [Fake]`
- 分类: `ICE`

## 掉落物清理指令 (commandKillItem)

注册 /killitem 指令，允许按半径或全局清理掉落物，并支持黑名单与命名掉落物保护配置。

- 类型: `String`
- 默认值: `ops`
- 可选值: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- 分类: `ICE`, `COMMAND`

## 假人免疫荆棘反伤 (fakePlayerIgnoreThornsDamage)

假人在攻击带有荆棘附魔的生物或玩家时，不会受到荆棘造成的反伤。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 禁止伤害已驯服生物 (disablePlayerAttackingTamedMobs)

玩家不能伤害自己驯服的生物，防止误伤。
影响范围包括猫、狼、鹦鹉、鹦鹉螺、僵尸鹦鹉螺、马、驴、骡、僵尸马、骷髅马、驼羊、行商驼羊。
(此规则最初移植自 DoormatCarpetExtension，现已按本模组需求调整。)

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`

## 幻翼生成预警 (phantomSpawnWarning)

玩家达到原版幻翼生成的失眠时间阈值时，会在夜晚开始时提醒玩家及时睡觉。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`

## 轻松放置含水方块 (easyWaterloggedBlockPlacement)

允许玩家在副手持有水桶时，将支持含水状态的方块直接放置为含水状态。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 禁止自然生成持矛僵尸猪灵 (zombifiedPiglinsSpawnWithoutSpears)

僵尸猪灵在自然生成时不会手持长矛。\
(此规则仅适用于 Minecraft 1.21.11 及以上版本。)

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`
