## 通用机制说明

### 合成配方冲突锁定

合成类规则通过对应的内置数据包提供本模组自带配方。规则启用时选中该数据包，规则关闭时取消选中并触发 vanilla 资源 reload。若其他数据包或模组提供了与这些自带配方同产物的合成配方，对应规则会在运行时自动锁定为 `false`。

锁定后：

- 本模组内置数据包被取消选中；
- 外部数据包 / 模组配方继续生效；
- `/carpet <规则名>` 查看时显示为 `false`；
- `/carpet <规则名> true` 会被拒绝；
- 所有在线玩家会收到提示；
- `carpet.conf` 不会被修改；
- 移除冲突数据包后执行 `/reload` 可解除锁定，并按原配置恢复；资源 reload 成功后会重新同步在线玩家的配方书。

### 客户端分类

带有 `CLIENT` 分类的规则涉及客户端行为，需要客户端也完整安装 Carpet 和本模组才会完全生效；仅在服务端安装时，这些规则只能部分生效。

## 规则

### 脚手架防误拆 (safeScaffoldingBreak)

只有主手持脚手架或主手为空时，玩家才能破坏脚手架，防止误拆。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 可隐形展示框 (itemFrameInvisible)

允许玩家用幻翼膜右键已放入物品的物品展示框或荧光物品展示框，使其隐形。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 可固定展示框 (itemFrameFixed)

允许玩家用玻璃板右键已放入物品的物品展示框或荧光物品展示框，使其固定；固定后的展示框可用斧头右键解除固定。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 合成器输出阻塞时停止合成 (crafterStopsWhenOutputBlocked)

当合成器面朝容器无法完整接收主产物时，阻止本次合成；合成余物保持原版处理。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 可合成珊瑚块 (craftableCoralBlocks)

允许使用 9 个同种类珊瑚扇合成对应的珊瑚块，死珊瑚扇同样生效。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`

### 唱片世界事件时序修复 (recordWorldEventFix)

修复了将唱片快速放入唱片机后又迅速取出时，音乐仍可能继续播放，且多个唱片音频可能重叠的问题。详见 MC-112245。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`

### 刷怪笼忽略隐身玩家 (spawnersIgnoreInvisiblePlayers)

普通刷怪笼、试炼刷怪笼和不祥试炼刷怪笼在判定附近玩家时会忽略隐身玩家。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 禁用海带自然生长 (disableKelpNaturalGrowth)

禁用海带由随机刻触发的自然生长。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 禁用紫水晶生长 (disableAmethystGrowth)

紫水晶母岩不会再通过随机刻生成紫水晶芽，或推进紫水晶芽生长。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 可采集紫水晶母岩 (silkTouchBuddingAmethyst)

紫水晶母岩可使用带有精准采集附魔的合适工具采集。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`

### 可采集霜冰 (silkTouchFrostedIce)

使用任意带有精准采集附魔的工具破坏霜冰时，会掉落 1 个普通冰。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`

### 霜冰合适工具补齐 (frostedIceProperToolFix)

将镐定为破坏霜冰的合适工具，使其破坏速度按冰处理。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`, `SURVIVAL`, `CLIENT`

### 信标合适工具补齐 (beaconProperToolFix)

使镐成为挖掘信标的合适工具。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`, `SURVIVAL`, `CLIENT`

### 类冰岩浆块 (iceLikeMagmaBlocks)

允许玩家使用不带精准采集的工具破坏岩浆块时，根据类似冰块的判定逻辑，在下方为有效固体方块或液体方块时生成岩浆源。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`

### 禁用菌岩退化 (disableNyliumDecay)

绯红菌岩和诡异菌岩在上方被方块遮挡时不会退化为下界岩。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 羊毛抑制幽匿蔓延 (woolSuppressesSculkSpread)

当幽匿催发体上方放置羊毛时，阻止其产生幽匿系列方块，同时保留附近生物死亡时阻止经验球生成的特性。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 监守者不敌视玩家 (wardenNotHostileToPlayers)

阻止监守者对玩家产生敌意并将玩家设为攻击目标，但不会阻止其感知玩家产生的振动。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`

### 假人Tab栏名称前缀 (botTabListNamePrefix)

为 Tab 栏中的假人添加前缀，可使用 `&` 代替分节符。

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, `[Bot]`
- 分类: `ICE`, `BOT`

### 假人Tab栏名称后缀 (botTabListNameSuffix)

为 Tab 栏中的假人添加后缀，可使用 `&` 代替分节符。

- 类型: `String`
- 默认值: `#none`
- 参考值: `#none`, `[Fake]`
- 分类: `ICE`, `BOT`

### 掉落物清理指令 (commandKillItem)

注册 /killitem 指令，允许按半径或全局清理掉落物，并支持黑名单与命名掉落物保护配置。

- 类型: `String`
- 默认值: `ops`
- 可选值: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- 分类: `ICE`, `COMMAND`

### 机器状态检查命令 (commandMachineStatus)

注册 /machineStatus 指令，用于保存生电机器关机时的目标方块状态，并查询当前哪些机器未处于保存的关机状态。

- 类型: `String`
- 默认值: `ops`
- 可选值: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- 分类: `ICE`, `COMMAND`

### 回档前机器状态警告 (machineStatusRollbackWarning)

玩家输入已适配的回档命令时，检查已保存的机器状态；如果存在未处于保存关机状态的机器，则向该玩家发送警告提示，不会拦截或修改回档命令。

默认适配的备份 Mod：Quick Backup Multi（`/qb`, `/quickbackupmulti`）。\
默认适配的 MCDR 插件：Quick Backup Multi（`!!qb`）、Prime Backup（`!!pb`）、Chunk Backup（`!!cb`）。\
可在 `<world>/carpet-ice-addition/machine_status.json` 中配置需要检测的回档命令。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`

### 假人免疫荆棘反伤 (fakePlayerIgnoreThornsDamage)

假人在攻击带有荆棘附魔的生物或玩家时，不会受到荆棘造成的反伤。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`, `BOT`

### 禁止伤害已驯服生物 (disablePlayerAttackingTamedMobs)

若服务器 PVP 开启，则玩家不能伤害自己驯服的生物；若服务器 PVP 关闭，则玩家不能伤害任何玩家拥有的已驯服生物。\
影响范围包括猫、狼、鹦鹉、鹦鹉螺、僵尸鹦鹉螺、马、驴、骡、僵尸马、骷髅马、驼羊、行商驼羊。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 幻翼生成预警 (phantomSpawnWarning)

玩家达到原版幻翼生成的失眠时间阈值时，会在夜晚开始时提醒玩家及时睡觉。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `SURVIVAL`

### 中立幻翼 (neutralPhantoms)

幻翼不会主动攻击玩家，但在被玩家攻击后会进行反击。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`

### 轻松含水放置 (easyWaterloggedBlockPlacement)

允许玩家在副手持有水桶时，将支持含水状态的方块直接放置为含水状态（水会蒸发的维度除外）。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 便携无限水 (portableInfiniteWater)

当玩家主手和副手均持有原版水桶时，使用水桶不会消耗其中的水。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`

### 禁用空中挖掘惩罚 (disableAirborneMiningPenalty)

使玩家未站在地面上时不再受到挖掘速度惩罚。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `SURVIVAL`, `CLIENT`

### 自定义末地平台位置 (customEndPlatformPosition)

允许自定义实体通过末地传送门进入末地时生成的末地黑曜石平台中心位置，并同步调整进入末地后的落点。

- 类型: `String`
- 默认值: `vanilla`
- 参考值: `vanilla`, `-100,49,0`
- 实际可接受格式: `vanilla` 或 `x,y,z`
- 分类: `ICE`, `FEATURE`

### 信标忽略遮挡 (beaconIgnoresObstruction)

使信标无视上方方块的遮挡并正常工作。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `CLIENT`

### 命名游商不消失 (namedWanderingTraderPersistence)

被命名的流浪商人不会自然消失。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### 禁止自然生成持矛生物 (mobsSpawnWithoutSpears) `MC>=1.21.11`

僵尸、僵尸村民、尸壳、僵尸猪灵和猪灵在自然生成时不会手持长矛。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`

### Carpet单人退出崩溃修复 (carpetSingleplayerExitCrashFix) `MC<=1.21.1`

修复客户端进入过 Carpet 服务器后，再退出单人世界可能触发的 Carpet 崩溃。

- 类型: `boolean`
- 默认值: `true`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`, `CLIENT`

### 切石机 Ctrl+Q 修复 (ctrlQStonecuttingFix) `MC<=1.21.1`

允许在切石机输出槽使用 Ctrl+Q 一次丢出整组产物。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `BUGFIX`

### 禁用非法字符检查 (disableIllegalTextCharacterCheck)

跳过原版的文本字符合法性检查，允许使用原版通常会拦截的字符，例如分节符。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `true`, `false`
- 分类: `ICE`, `FEATURE`, `CLIENT`

### 水流体刻延迟 (waterFluidTickDelay)

自定义水的流动延迟，单位为游戏刻。设置为 `freeze` 时冻结水的流动相关计划流体刻。

- 类型: `String`
- 默认值: `5`
- 建议值: `freeze`, `5`
- 实际可接受值: `freeze` 或 `1` 至 `72000` 的整数
- 分类: `ICE`, `FEATURE`

### 岩浆流体刻延迟 (lavaFluidTickDelay)

自定义岩浆的流动延迟，单位为游戏刻。ultrawarm 维度中的延迟为设置值的三分之一且最低为 1；设置为 `freeze` 时冻结岩浆流动，但不影响随机刻。

- 类型: `String`
- 默认值: `30`
- 建议值: `freeze`, `30`
- 实际可接受值: `freeze` 或 `1` 至 `72000` 的整数
- 分类: `ICE`, `FEATURE`

### 村民交易优化 (villagerTradingOptimization)

当村民被命名为 trade 时，将其 AI 精简为面向固定式村民交易所的最小行为集：仅保留原版补货、日程切换、防溺水与工作站有效性校验，交易、职业升级与新货单不受影响，但需先配好职业与工作站并把村民固定在工作站旁。

本规则面向已搭建完成的固定式交易所：村民应在命名前已获得最终职业并绑定工作站，且被固定在工作站约 1.73 格范围内（推荐贴身或相邻放置）。优化期间村民不再自主走动或寻路，不再寻找、竞争或更换工作站，不再开门、面向玩家、闲逛、社交、恐慌、睡觉、繁殖、传播流言、参与召唤铁傀儡或捡拾物品，也不会自行走向工作站；补货保留原版工作行为，每日补货次数、两次补货间隔与跨日重置等原版限制全部保留，已有 gossip 与声誉对交易价格的影响继续生效。工作站被拆除或失效后，村民会停止补货并保留职业，但不会自行重新认领工作站；需要调整职业、更换工作站或把村民移回站位时，应先改名或关闭规则让原版 AI 恢复。村民仍可通过外力、船、矿车、水流等方式移动（不会主动寻路上载具），落水时仍会尝试跳跃自救。名称匹配区分大小写，需精确等于小写 `trade`。改名、移除名称或关闭规则后，村民会通过原版 Brain 重建恢复完整 AI，不写入任何依赖本模组的存档数据。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `OPTIMIZATION`

### 铁傀儡生成优化 (ironGolemSpawningOptimization)

将村民命名为 iron_golem 即声明其位于搭建完成的现代恐吓式刷铁机：关闭闲逛张望、社交闲聊、钟聚、繁殖、职业与交易展示等与刷铁无关的行为，并抑制寻找工作站、寻找聚会点与主动寻路捡取物品；保留恐慌触发、按日程睡眠与原版铁傀儡生成算法，不保证 gossip 型刷铁；维护、运输或重新布置村民与床前应先改名解除优化。

本规则面向 MC 1.21+ 的现代恐吓式（僵尸惊吓）刷铁机，命名即声明该村民已位于搭建完成、可正常运行的铁塔；不保证依赖村民 gossip / 社交触发生成的旧式架构继续工作（命名村民的闲聊与聚会行为被整表关闭，gossip 路径的生成触发不再生效）。保留的生成链：CORE 恐慌触发（含恐慌期间每 100 tick 的原版生成判定）、PANIC 收尾、REST 认床走床睡眠并刷新 LAST_SLEPT、日程切换与原版铁傀儡检测 / 冷却；不修改原版生成条件、概率、冷却与生成位置判定。被关闭的行为：WORK / PLAY / IDLE / MEET 除日程切换（priority 99）外的全部（含闲逛、张望、社交闲聊、繁殖、跳床、英雄赠礼、交易展示、聚会走位等）、REST / PANIC / 袭击活动共用的张望组合、寻找工作站与聚会点的 48 格 POI 扫描、职业链、跟随交易玩家、主动寻路捡取物品，以及袭击与响钟入口（对应入口任务被跳过后不再进入 PRE_RAID / RAID / HIDE 活动；命名时已处于这些活动的村民会经原版退出任务自然离开）。认床（HOME POI 获取）与 REST 的 HOME 缺失回退保持原版，命名期间床仍可认领与重认领；床被破坏时按原版机制失效。名称匹配比较纯文本内容、区分大小写，需精确等于小写 `iron_golem`，颜色、粗体等样式不影响匹配。改名、移除名称或关闭规则后，被跳过的任务自下一次启动判定起恢复完整原版行为（进行中的无关行为会自然结束当前周期），已有原版记忆保留并继续使用；命名期间关闭的 gossip 等机会不会回溯补发，恢复后按原版自身周期重新建立；不写入任何依赖本模组的存档数据。运输村民、重新认床、维修或重新布置铁塔结构前，应先将村民改名解除优化。

- 类型: `boolean`
- 默认值: `false`
- 可选值: `false`, `true`
- 分类: `ICE`, `FEATURE`, `OPTIMIZATION`
