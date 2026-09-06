# 重构验收清单（三级验收）

> 本清单定义 Fallen-Breath 多版本架构迁移过程中每一步的验收标准，配套 [refactor-baseline.md](refactor-baseline.md)（对照基准数据）与 [refactor-target-architecture.md](refactor-target-architecture.md)（阶段划分）。清单描述"必须验证什么"，具体修复不属于本文档职责。

约定：

- 标注【人工】的条目需要游戏内 / 客户端环境，由人工执行；Agent 不得将其标记为已通过。
- 未标注的条目原则上可由命令行自动化完成。
- "基线 §x" 均指 `refactor-baseline.md` 对应章节。

## 0. 验收分级与阶段映射

| 级别 | 范围 | 触发时机 |
|---|---|---|
| Level 1 架构验收 | Gradle 构建、jar 生成、fabric.mod.json、mod 加载 | 每个迁移步骤（Phase 1 每步、Phase 3/4 每步） |
| Level 2 核心功能验证 | 代表性规则 / 命令 / logger / client mixin 冒烟 | Phase 2 收尾、Phase 3/4 每个特性步 |
| Level 3 完整版本回归 | 全部规则 × 全部 11 个版本 + 发布验收 | Phase 2 收尾、Phase 3/4 收尾、每次正式发布前 |

| 迁移阶段 | 必须通过的级别 | 附加项 |
|---|---|---|
| Phase 1 构建体系迁移（每步 + 收尾） | Level 1 | jar 内容级等价对照（见 L1-6） |
| Phase 2 编译与行为等价验证 | Level 1 + 2 + 3 | 与动工前构建快照 / Release 2.13.1 资产对照 |
| Phase 3 源码结构优化（每步） | Level 1 + 2 | 受影响特性的定向验证 |
| Phase 3 / Phase 4 收尾 | Level 1 + 2 + 3 | — |
| 正式发布 | Level 3 | 发布验收（§4） |

## 1. Level 1 架构验收

- [ ] **L1-1 全平台构建**：`.\gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs --stacktrace` 全绿（Windows 下用 `.\gradlew.bat`；`verifyMixinConfigs` 为 P3-3 新增防线，随 CI 接入同步加入本命令）。
- [ ] **L1-2 jar 生成与命名**：11 个平台 jar 齐全，文件名与基线 §3 表一致（mod_version 变更时仅版本段变化；label 部分逐字符一致）。
- [ ] **L1-3 fabric.mod.json 语义**：逐平台与基线 §1.2 / §5 比对——`depends`（minecraft / fabric-api / carpet / fabricloader）、`version`、`id`、`name`、`license`、`environment`、entrypoints、`mixins` 引用全部一致；无未展开 `${`、无 BOM、JSON 合法（`verifyFabricModJson` 覆盖后半部分，前半部分需人工或脚本比对）。
- [ ] **L1-4 mixin 配置行为**：每平台 mixin json 的文件名、package、compatibilityLevel、`mixins` / `client` 条目数与基线 §1.2 一致；`defaultRequire=1` 下游戏加载成功即隐式证明所有条目类存在（配合 L1-5）。
- [ ] **L1-5 【人工】mod 加载冒烟**：dev 实例（`runServer` 或 `.minecraft/deploy.cmd` 部署）启动无 mixin / 注册错误；`/carpet` 打开规则列表；`/killitem`、`/machineStatus` 命令树存在且权限符合默认 `ops`。建议至少覆盖 4 类形态各 1 版本：1.21.1（remap + LowVersion + override 最多）、1.21.5 或 1.21.10（1.21.x 中位）、1.21.11（HighVersion + core 原位编译根 src）、26.2（免混淆 + Java 25）。
- [ ] **L1-6 jar 内容级等价对照（Phase 1 专项，自动化）**：与动工前构建快照逐平台比对——
  - zip 条目清单一致（class 文件路径与数量、resourcepacks/、assets/）；
  - fabric.mod.json 解析后逐键语义一致（允许因构建时间产生的字段差异为零——模板展开值应完全相同）；
  - mixin json 内容逐字节一致（其内容不参与模板展开）；
  - pack.mcmeta 的 pack_format 逐平台一致（基线 §1.2）；
  - class 文件不做字节级比对（编译时间戳噪声），以清单 + 后续 Level 2/3 行为验证兜底。
- [ ] **L1-7 【人工】mod 加载注册行为**（Phase 1 完成标准的一部分）：规则 / 命令 / logger 注册行为不变——各平台 `/carpet list` 条目数符合注册数量矩阵（§3 开头），`/log villagerEvents` 可订阅。

## 2. Level 2 核心功能验证（代表集）

选择原则：覆盖每种实现形态——1.21.x↔26.x 双胞胎规则、多路分叉规则、字符串参数规则、命令门规则、资源包规则、client mixin、logger。抽样版本建议：1.21.1、1.21.5、1.21.11、26.1.2、26.2（覆盖全部形态分支）。

全部条目均【人工】（游戏内操作）：

- [ ] **2-1 craftableCoralBlocks**（资源包 + 冲突锁 + 双胞胎）：开关规则后内置资源包 `craftable_coral_blocks` 生效 / 失效，10 个珊瑚块配方可 / 不可合成；与其他数据包配方冲突时触发冲突锁定并给出提示；规则切换时已打开的合成界面结果槽刷新。
- [ ] **2-2 villagerTradingOptimization**：开启后固定式交易所村民行为符合 docs/rules.md 描述（认领身边工作站、保留原版职业生命周期）；关闭后恢复原版。
- [ ] **2-3 ironGolemSpawningOptimization**：开启后铁傀儡生成走优化路径（村民标记 / 跳过逻辑），关闭恢复原版。
- [ ] **2-4 waterFluidTickDelay / lavaFluidTickDelay**（字符串参数 + 缓存刷新）：设为 `freeze` 后水 / 岩浆冻结不流动；设为数值后按延迟 tick；切换时缓存立即生效；非法值被 `FluidTickDelayValidator` 拒绝。
- [ ] **2-5 neutralPhantoms + phantomSpawnWarning**（6 份分叉的最重档）：开启后幻翼不主动仇恨玩家且被攻击会反击； phantomSpawnWarning 开启后幻翼生成前向玩家发送警告。
- [ ] **2-6 silkTouchBuddingAmethyst**：精准镐采掘紫水晶母岩掉落母岩本身；关闭时恢复原版（掉落碎片）。
- [ ] **2-7 /killitem 全子命令**（命令门规则 commandKillItem）：`range <半径>`、`dimension <维度>`、`all`、`detail <resultId> <page>`（分页与可点击按钮）、`config blacklist add|remove|clear`、`config clearNamedItems [bool]`；黑名单物品与命名物品不被清除；结果写入世界目录 `killitem.json` 且重载后保留。
- [ ] **2-8 /machineStatus 全子命令**（commandMachineStatus）：`add / remove / rename / update / move / list [running|stopped|invalid|unloaded] / info`；机器状态分类正确；`machine_status.json` 持久化跨重启有效。
- [ ] **2-9 botTabListNamePrefix / botTabListNameSuffix**（BOT 类 + 共用 mixin）：设置前缀 `[Bot]` / 后缀 `[Fake]` 后假人 TAB 列表名正确拼接；`#none` 时不变；`&` 颜色码转换生效。
- [ ] **2-10 villagerEvents logger**（唯一 logger）：`/log villagerEvents <all|death|zombified|witch>` 订阅输出正确（村民死亡 / 僵尸化 / 女巫化事件）；无订阅者时高频路径零开销（`__villagerEvents` 加速字段）。
- [ ] **2-11 disableIllegalTextCharacterCheck（client 侧）**【需客户端装 mod】：非法字符检查关闭后，书编辑 / 剪贴板路径接受 vanilla 拒绝的字符；服务端路径（网络处理器 mixin）同样放行。
- [ ] **2-12 machineStatusRollbackWarning**：触发回档命令（正则匹配 `MachineStatusRollbackCommandMatcher`）后按配置产生警告；`config/machine_status_rollback_warning.json` 读写正常、损坏时回退默认并留日志。

## 3. Level 3 完整版本回归

### 3.1 规则注册数量矩阵（可与 `/carpet list` 输出比对）

| 平台 | 应注册规则数 | 平台特有 |
|---|---|---|
| 1.21.1 | 40 | + carpetSingleplayerExitCrashFix、ctrlQStonecuttingFix（LowVersion） |
| 1.21.3 / 1.21.4 / 1.21.5 / 1.21.6 / 1.21.8 / 1.21.9 / 1.21.10 | 38 | 无 |
| 1.21.11 | 39 | + mobsSpawnWithoutSpears（HighVersion） |
| 26.1.2 / 26.2 | 39 | + mobsSpawnWithoutSpears（HighVersion） |

### 3.2 规则逐条核对

按附录 A 全表核对 41 条规则的：内部名存在、默认值一致、options 一致、分类正确（`/carpet <rule>` 显示与文档一致）、中文 `.name` / `.desc` 翻译正常（服务端翻译，不依赖客户端语言文件）、文档位置有效（docs/rules.md `### 中文描述 (内部名)` 小节；rules_en.md `### 内部名` 小节）。

### 3.3 版本特定分支清单（逐项必验）

- [ ] carpetSingleplayerExitCrashFix、ctrlQStonecuttingFix：仅 1.21.1 存在；其他平台不存在。
- [ ] mobsSpawnWithoutSpears：1.21.11 / 26.1.2 / 26.2 存在；1.21.1–1.21.10 不存在。
- [ ] /killitem：Phase 5 起为两个实现分支——根 src 单一实现（1.21.1–1.21.11，helper 间接宏覆盖跨版本差异）与 `26.1.2` / `26.2` 各自 override（26.x 形态）——两个分支都要冒烟。
- [ ] /machineStatus：同 /killitem 的两分支形态（根 src 实现承担 1.21.x 跨版本差异宏；26.1.2 / 26.2 override 承担 26.x 形态）——两个分支都要冒烟。
- [ ] itemFrameInvisible / itemFrameFixed：根 src `ItemFrameMixin`（1.21.6–1.21.11 生效形态）+ `1.21.1` / `1.21.3` / `1.21.4` / `1.21.5` override 变体；26.x 为各平台 src 的同名 `ItemFrameMixin`（1.21.x↔26.x 项目类名已由 Phase 6 统一）——对应版本全部冒烟（展示框隐形 / 固定交互）。
- [ ] BookEditScreen client mixin 仅存在于 1.21.1–1.21.5 平台的 mixin json `client` 数组；1.21.6+ 与 26.x 只有 Clipboard mixin。
- [ ] 26.x 命令实现（平台 override）与 1.21.x 根 src 实现同为 CommandSourceStack（Mojmap 命名空间，Phase 4 统一），注册与执行正常。
- [ ] 1.21.1 平台 jar 内包含其自有的 10 个珊瑚配方副本；1.21.3–1.21.11 由 shared/mc1213-12111 提供；26.1.2 / 26.2 各自携带（`verifyCraftableCoralBlocksJars` 已覆盖计数，此处为行为冒烟）。

### 3.4 资源与翻译

- [ ] 内置资源包：逐平台 pack_format 与基线 §1.2 一致；10 个 `coral_block_from_*` 配方可用；冲突锁定三键翻译（`carpet.rule.craftableCoralBlocks.conflict.*`）正常。
- [ ] 翻译完整性：zh_cn.json / en_us.json 与硬编码 Map `CarpetIceAdditionTranslations` 的键集合核对；logger 10 键当前仅存在于硬编码 Map——回归时按"现状基线"核对（即 JSON 缺失是已知现状，不是回归项），并保留待修复记录。
- [ ] `.\gradlew.bat :1.21.11:test`（FluidTickDelayUtil / IronGolemVillagerOptimizer / VanillaLanguageService 三组单测；Phase 9 起测试 owner 为 `:1.21.11:test`，其余平台 test 保持 NO-SOURCE）通过。

### 3.5 mixin 完整性

- [ ] 每平台 mixin json 无悬空条目（所有条目类在该平台编译产物中存在）且与该平台源码根的实际 mixin 类集合一致（重构若引起条目漂移在此暴露）。

## 4. 发布验收

- [ ] 期望产物：基线 §3 的 11 个 jar + 对应 `-sources.jar`。
- [ ] publish.yml 前置断言通过：Release tag == `mod_version`；每平台恰 1 个非 sources jar；文件名含 `-v${MOD_VERSION}-`。
- [ ] Modrinth：项目 `3ZWOd2ma`；loaders=fabric；依赖 carpet（必需）+ fabric-api（必需）；game_versions 为 `*_release_minecraft_range` 的闭区间展开；version_name 形如 `Carpet Ice Addition v${MOD_VERSION} for mc<label>`。
- [ ] 幂等性：publish 重跑（dispatch 模式）不产生重复版本，预检 / 后验逻辑正常跳过或补齐。

## 附录 A：规则总表（41 条）

说明：分类缩写——F=FEATURE，B=BUGFIX，S=SURVIVAL，C=CLIENT，CMD=COMMAND，BOT=BOT，O=OPTIMIZATION（全部另含 ICE，表中省略）。"份数"指该 mixin/实现的 .java 物理分叉拷贝数（**P4 基线口径**；Phase 5 起物理分叉收敛为「根 src 宏 + per-version override」，实际分布以根 `src/main/java` 与 `versions/<版本>/src/main/java` 为准；表中 Yarn / Mojmap 双子标签亦为家族命名习惯，现全部为 Mojmap 命名空间）。翻译键：中文 `carpet.rule.<内部名>.name` + `.desc`，英文仅 `.desc`（全表通用，不再逐行注明）。

| 内部名 | 默认值 / options | 分类 | 可用平台 | 实现要点 | client |
|---|---|---|---|---|---|
| safeScaffoldingBreak | false | F | 全部 | SafeScaffoldingBreakMixin×3 + RealPlayerHelper / RuleMessageThrottle | 否 |
| crafterStopsWhenOutputBlocked | false | F | 全部 | CrafterBlockMixin×2 + CrafterOutputBlockHelper | 否 |
| recordWorldEventFix | false | B | 全部 | JukeboxManagerRecordWorldEventMixin×3 + ServerWorldRecordWorldEventMixin×2 + DelayedJukeboxStartEventManager | 否 |
| spawnersIgnoreInvisiblePlayers | false | F | 全部 | MobSpawnerLogicMixin + TrialSpawnerDataMixin（各 ×2） | 否 |
| disableKelpNaturalGrowth | false | F | 全部 | KelpNaturalGrowthMixin×2 | 否 |
| disableAmethystGrowth | false | F | 全部 | AmethystNaturalGrowthMixin×2 | 否 |
| silkTouchBuddingAmethyst | false | F,S | 全部 | CanMineBuddingAmethystMixin×2 | 否 |
| silkTouchFrostedIce | false | F,S | 全部 | CanMineFrostedIceMixin×2 | 否 |
| frostedIceProperToolFix | false | B,S,C | 全部 | FrostedIceProperToolFixMixin×2（分类标 C 但 mixin 在通用列表） | 否 |
| beaconProperToolFix | false | B,S,C | 全部 | BeaconProperToolFixMixin×2（同上） | 否 |
| iceLikeMagmaBlocks | false | F,S | 全部 | PlayerBreakIceLikeMagmaBlocksMixin×4 + common IceLikeMagmaBlocksHelper | 否 |
| disableNyliumDecay | false | F | 全部 | NyliumDecayMixin×2 | 否 |
| woolSuppressesSculkSpread | false | F | 全部 | SculkCatalystListenerMixin×2（targets 内部类） | 否 |
| wardenNotHostileToPlayers | false | F,S | 全部 | WardenEntityMixin×3 + WardenAngerManagerMixin×2 | 否 |
| fakePlayerIgnoreThornsDamage | false | F,S,BOT | 全部 | FakePlayerIgnoreThornsDamageMixin×3 | 否 |
| itemFrameInvisible | false | F | 全部 | ItemFrameMixin（根 + 1.21.1/1.21.3/1.21.4/1.21.5 override；26.x 同名 override，Phase 6 统一）+ ItemFrameInteractionHelper | 否 |
| itemFrameFixed | false | F | 全部 | 与 itemFrameInvisible 共用同一 mixin / helper | 否 |
| easyWaterloggedBlockPlacement | false | F | 全部 | BlockItemEasyWaterloggedBlockPlacementMixin（P6-5 起单一统一类，1.21.11 蒸发判定经宏分支；26.x 同名 override） | 否 |
| portableInfiniteWater | false | F,S | 全部 | ItemUtilsPortableInfiniteWaterMixin（根；26.x 同名 override，Phase 6 统一） | 否 |
| disableAirborneMiningPenalty | false | F,S,C | 全部 | DisableAirborneMiningPenaltyMixin×2（分类标 C 但 mixin 在通用列表） | 否 |
| disableIllegalTextCharacterCheck | false | F,C | 全部 | 4 个 mixin：StringHelper 版×3、网络处理器版×2、Clipboard（client）、BookEditScreen（client，仅 1.21.1–1.21.5） | 是 |
| disablePlayerAttackingTamedMobs | false | F | 全部 | DisablePlayerAttackingTamedMobsMixin×4 + PvpRuleHelper / LegacyPvpRuleHelper | 否 |
| phantomSpawnWarning | false | S | 全部 | ServerWorldPhantomSpawnWarningMixin×2 + PhantomSpawnWarningHelper×3 | 否 |
| neutralPhantoms | false | F,S | 全部 | PhantomNeutralPhantomsMixin（根 + 1.21.1/1.21.3/1.21.4/1.21.5 override；26.x 同名 override）+ PhantomAttackPlayerTargetGoalNeutralPhantomsMixin（根；26.x 同名 override，Phase 6 统一）+ LivingEntityNeutralPhantomsMixin×3 | 否 |
| commandKillItem | "ops"（{false,true,ops,0..4}） | CMD | 全部 | 无 mixin；KillItemCommand 4 处实现（见 §3.3） | 否 |
| commandMachineStatus | "ops"（同上） | CMD | 全部 | 无 mixin；MachineStatusCommand 3 处实现（见 §3.3） | 否 |
| machineStatusRollbackWarning | false | — | 全部 | ServerGamePacketListenerImplMachineStatusRollbackWarningMixin（Phase 6 统一后单一名称：根 + 26.x 同名 override）+ ServerCommonNetworkHandlerAccessor + Handler 类 + 全局配置 json | 否 |
| botTabListNamePrefix | "#none"（{#none,[Bot]}，strict=false） | BOT | 全部 | PlayerListEntryTabListNameMixin×2（目标为 ServerPlayer TAB 名方法，服务端）+ BotTabListNameHelper | 否 |
| botTabListNameSuffix | "#none"（{#none,[Fake]}，strict=false） | BOT | 全部 | 与 Prefix 共用 mixin / helper | 否 |
| beaconIgnoresObstruction | false | F,C | 全部 | BeaconBlockEntityMixin×3（分类标 C 但 mixin 在通用列表） | 否 |
| namedWanderingTraderPersistence | false | F | 全部 | WanderingTraderMixin（根；26.x 同名 override，Phase 6 统一） | 否 |
| villagerTradingOptimization | false | F,O | 全部 | VillagerTradingOptimizationMixin（根 + 1.21.1/1.21.3/1.21.4 override；26.x 同名 override，Phase 6 统一）+ SensorTradingOptimization / EntitySetCustomNameTradingOptimization（1.21.x）+ BrainProviderTradingOptimization（26.x）；VillagerTradingOptimizationTasks 等 5 份 + RuleHelper + common Access | 否 |
| ironGolemSpawningOptimization | false | F,O | 全部 | Phase 6 统一后 13 类（Behavior / OneShot / GateBehavior / AcquirePoi / GoToWantedItem / AssignProfessionFromJobSite / ResetProfession / ReactToBell / SetRaidStatus / YieldJobSite / PoiCompetitorScan / VillagerGoalPackages / VillagerIronGolemOptimization）+ GoToPotentialJobSite 版（1.21.x）+ per-version override（AcquirePoi 于 1.21.1/1.21.3/1.21.4；AssignProfessionFromJobSite / GoToPotentialJobSite / GoToWantedItem 1211 形态于 1.21.1）；common Optimizer / SkipMarked / Access + Hooks | 否 |
| nameTagDuplicateNamingFix | false | B | 全部 | NameTagItemDuplicateNamingFixMixin×3 | 否 |
| mobsSpawnWithoutSpears | false | F | 1.21.11 / 26.1.2 / 26.2 | PiglinMobsSpawnWithoutSpearsMixin + ZombieMobsSpawnWithoutSpearsMixin（根；26.x 同名 override，Phase 6 统一） | 否 |
| carpetSingleplayerExitCrashFix | **true** | B,C | 仅 1.21.1 | CarpetServerOnServerClosedNullFixMixin（remap=false，目标 carpet.CarpetServer） | 否 |
| ctrlQStonecuttingFix | false | B | 仅 1.21.1 | ScreenHandlerCtrlQStonecuttingFixMixin | 否 |
| customEndPlatformPosition | "vanilla"（{vanilla,-100,49,0}，strict=false） | F | 全部 | EndPortalBlockCustomEndPlatformPositionMixin×4 + CustomEndPlatformPositionHelper + EndPlatformSettings | 否 |
| craftableCoralBlocks | false | F,S | 全部 | CraftingMenuCraftableCoralBlocksMixin（根 + 1.21.1 override；26.x 同名 override，Phase 6 统一）+ DataPackController / Validator / ConflictDetector 等 + 10 配方资源包 | 否 |
| waterFluidTickDelay | "5"（{freeze,5}，strict=false） | F | 全部 | WaterFluidTickDelayMixin（根；26.x 同名 override，Phase 6 统一）+ FlowingFluidFreezeMixin（根 + 1.21.1 override；26.x 同名 override）+ FluidSettings / Validator / Util | 否 |
| lavaFluidTickDelay | "30"（{freeze,30}，strict=false） | F | 全部 | LavaFluidTickDelayMixin（根；26.x 同名 override，Phase 6 统一）+ 共用 Freeze mixin | 否 |

规则文档：中文见 `docs/rules.md` 对应 `### 中文描述 (内部名)` 小节，英文见 `docs/rules_en.md` 对应 `### 内部名` 小节。

## 附录 B：命令规格

### /killitem（规则门 commandKillItem，默认 ops）

- 语法：`range <半径 1–1024>`、`dimension <维度>`、`all`、`detail <resultId> <页码>`、`config blacklist add|remove <物品>|clear`、`config clearNamedItems [bool]`。
- 行为：按物品分类汇总清除 ItemEntity；每玩家 10 分钟 detail 结果缓存（保留 5 份）；结果翻页与操作按钮（反射构造 ClickEvent / HoverEvent，跨版本兼容）；黑名单与命名物品保护。
- 持久化：世界目录 `killitem.json`（KillItemConfigManager）。
- 权限：`CommandHelper.canUseCommand` 按 commandKillItem 值判定。
- 实现分布：根 src（1.21.1–1.21.11，Phase 5 起）；`26.1.2` / `26.2` override（26.x 形态）。
- 文档：`docs/commands.md` / `docs/commands_en.md` 的 `/killitem` 小节。

### /machineStatus（规则门 commandMachineStatus，默认 ops）

- 语法：`add <维度> <坐标> <名称>`、`remove <名称>`、`rename <旧名> <新名>`、`update <名称>`、`move <名称> <维度> <坐标>`、`list [running|stopped|invalid|unloaded]`、`info <名称>`。
- 行为：记录目标方块关机状态快照并比较，分类为 RUNNING / STOPPED / INVALID / UNLOADED（MachineStatusKind）。
- 持久化：世界目录 `machine_status.json`（MachineStatusConfigManager）。
- 实现分布：根 src（1.21.1–1.21.11，Phase 5 起）；`26.1.2` / `26.2` override（26.x 形态）。
- 文档：`docs/commands.md` / `docs/commands_en.md` 的 `/machineStatus` 小节。

## 附录 C：logger 规格

- 内部名：`villagerEvents`；options `{all, death, zombified, witch}`，默认 `all`，strict。
- 注册：`VillagerEventsLogger`（Phase 6 统一名称；1.21.x 由根 src 编译、26.1.2 / 26.2 由平台 override 提供），入口类 `registerLoggers()` 接入。
- 行为：输出村民死亡 / 僵尸化 / 女巫化事件；服务端经 `VanillaLanguageService` 翻译；`VillagerEventsCompatibility` 做一次性错误节流（保证 logger 永不阻断实体 tick）。
- 高频路径：无订阅者时经 `__villagerEvents` 静态字段短路，不组装文本。
- 文档：`docs/loggers.md` / `docs/loggers_en.md`。
