> 文档对应版本：`v2.5.0`

## 脚手架防误拆 (safeScaffoldingBreak)

只有主手持脚手架或主手为空时，玩家才能破坏脚手架，防止误拆。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`

## 可隐形展示框 (invisibleItemFrames)

允许玩家用幻翼膜右键已放入物品的物品展示框或荧光物品展示框，使其隐形。\
（功能设计参考了 carpet-redcraft-addons 的 betterItemFrames 规则，当前实现已按本模组目标版本与分层结构独立重写。）

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 可固定展示框 (fixedItemFrames)

允许玩家用玻璃板右键已放入物品的物品展示框或荧光物品展示框，使其固定；固定后的展示框可用斧头右键解除固定。\
（功能设计参考了 carpet-redcraft-addons 的 betterItemFrames 规则，当前实现已按本模组目标版本与分层结构独立重写。）

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

修复了将唱片快速放入唱片机后又迅速取出时，音乐仍可能继续播放，且多个唱片音频可能重叠的问题。详见 MC-112245。\
（最初移植自 Carpet-Fixes，现已按本模组需求调整。）

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

禁用海带由随机刻触发的自然生长。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 禁用紫水晶生长 (disableAmethystGrowth)

紫水晶母岩不会再通过随机刻生成紫水晶芽，或推进紫水晶芽生长。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 可采集紫水晶母岩 (silkTouchBuddingAmethyst)

紫水晶母岩可使用带有精准采集附魔的合适工具采集。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 可采集霜冰 (silkTouchFrostedIce)

使用任意带有精准采集附魔的工具破坏霜冰时，会掉落 1 个普通冰。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 霜冰合适工具补齐 (frostedIceProperToolFix)

将镐定为破坏霜冰的合适工具，使其破坏速度按冰处理。

注：真人玩家若要获得完整的挖掘速度体验（破坏进度条与实际一致），需要客户端完整安装 Carpet 和本模组；客户端未完整安装 Carpet 和本模组时，真人玩家客户端的破坏动画可能与服务端判定不完全同步。

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
（功能设计参考了 Carpet-TCTC-Addition 的同类规则，当前实现已按本模组需求重写。）

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, `[Bot]`
- 分类: `ICE`

## 假人Tab栏名称后缀 (botTabListNameSuffix)

为 Tab 栏中的假人添加后缀，使用 `&` 来表示文字颜色。\
（功能设计参考了 Carpet-TCTC-Addition 的同类规则，当前实现已按本模组需求重写。）

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, `[Fake]`
- 分类: `ICE`

## 掉落物清理指令 (commandKillItem)

注册 /killitem 指令，允许按半径或全局清理掉落物，并支持黑名单与命名掉落物保护配置。

- 类型: `String`
- 默认值: `ops`
- 可选值: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- 分类: `ICE`, `COMMAND`

## 机器状态检查命令 (commandMachineStatus)

注册 /machineStatus 指令，用于保存生电机器关机时的目标方块状态，并查询当前哪些机器未处于保存的关机状态。

- 类型: `String`
- 默认值: `ops`
- 可选值: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- 分类: `ICE`, `COMMAND`, `SURVIVAL`

## 回档前机器状态警告 (machineStatusRollbackWarning)

玩家输入已适配的回档命令时，检查已保存的机器状态；如果存在未处于保存关机状态的机器，则向该玩家发送警告提示，不会拦截或修改回档命令。

默认适配的备份 Mod：Quick Backup Multi（`/qb`, `/quickbackupmulti`）。\
默认适配的 MCDR 插件：Quick Backup Multi（`!!qb`）、Prime Backup（`!!pb`）、Chunk Backup（`!!cb`）。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`

## 假人免疫荆棘反伤 (fakePlayerIgnoreThornsDamage)

假人在攻击带有荆棘附魔的生物或玩家时，不会受到荆棘造成的反伤。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 禁止伤害已驯服生物 (disablePlayerAttackingTamedMobs)

若服务器 PVP 开启，则玩家不能伤害自己驯服的生物；若服务器 PVP 关闭，则玩家不能伤害任何玩家拥有的已驯服生物。\
影响范围包括猫、狼、鹦鹉、鹦鹉螺、僵尸鹦鹉螺、马、驴、骡、僵尸马、骷髅马、驼羊、行商驼羊。\
(功能设计参考了 DoormatCarpetExtension 的同类规则，当前实现已按本模组需求重写)

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

## 中立幻翼 (neutralPhantoms)

幻翼不会主动攻击玩家，但在被玩家攻击后会进行反击。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 轻松含水放置 (easyWaterloggedBlockPlacement)

允许玩家在副手持有水桶时，将支持含水状态的方块直接放置为含水状态。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## 自定义末地平台位置 (customEndPlatformPosition)

允许自定义实体通过末地传送门进入末地时生成的末地黑曜石平台中心位置，并同步调整进入末地后的落点。

- 类型: `String`
- 默认值: `vanilla`
- 参考值: `vanilla`, `-100,49,0`
- 实际可接受格式: `vanilla` 或 `x,y,z`
- 分类: `ICE`, `FEATURE`

## 禁止自然生成持矛生物 (mobsSpawnWithoutSpears) `MC>=1.21.11`

僵尸、僵尸村民、尸壳、僵尸猪灵和猪灵在自然生成时不会手持长矛。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

## Carpet单人退出崩溃修复 (carpetSingleplayerExitCrashFix) `MC<=1.21.1`

修复客户端进入过 Carpet 服务器后，再退出单人世界可能触发的 Carpet 崩溃。

- 类型: `boolean`
- 默认值: `true`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`, `CLIENT`
