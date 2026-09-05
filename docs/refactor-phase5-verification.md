# Phase 5 验证记录：Fallen-Breath 风格源码架构收敛（preprocess 版本图 + 根 src + per-version override）

> 状态：**P5-0 完成（2026-09-05）**，P5-1 起待实施。
> 计划基线：Phase 5 实施计划 v3（已经人工审查通过），关键决策见 §0。
> 工作区：`D:\Project\Carpet-Ice-Addition-P5-workspace\inventory\`（脚本与产物，不入仓库）。

## 0. 范围、已确认决策与删除纪律

### 0.1 已确认决策（人工锁定）

| 决策 | 内容 |
| - | - |
| 主版本（mainProject） | **1.21.11**（root src 以 1.21.11 Mojmap 命名为基准；core 子项目 = `platform-mc12111`） |
| 26.x 结构性分叉 | **双份复制**进 `platform-mc261/src` 与 `platform-mc262/src`（纯 Fallen-Breath 模型，shared Java 档机制最终完全移除） |
| 自有类名统一 | **Phase 5 不统一**（保持 jar zip 条目集合与 P4 基线完全一致，verifyJarEquivalence 零适配；统一留 Phase 6） |
| Gradle project path | **扁平化为根直接子项目** `:platform-mcXXXX`（磁盘仍 `versions/platform-*`；preprocessor 经 `project.parent` 取根扩展，`:versions:platform-*` 的 parent 是 `:versions` 不兼容） |
| preprocess 供应链 | JitPack `com.github.Fallen-Breath:preprocessor`，固定全 SHA `c5abb4fb12aad2590c852c1bc6c8d5758606ec0b` |
| strictExtraMappings | 保持 `true` 服务 1.21.x remap↔remap 边；remap↔plain 边（26.x）在锁定源码中恒走 `LegacyMapping.readMappingSet`，不受该开关影响；仅当 1.21.x strict 模式自身与当前 Loom 不兼容（P5-1b 实证）才考虑回退 |

### 0.2 删除纪律（含人工补充约束，全程有效）

1. **旧源码一律 dormant 保留至 Level 3 通过后**（P5-7 单独 commit 删除）：迁移期间任何旧 Java 不物理删除——shared 档位文件仅随 flip 撤销引用；`platform-mc12111/src/main/java` 的 8 个文件在 core 启用 preprocess 后不参与 sourceSet，作为 dormant legacy copy 原样保留。
2. **非 core 平台的 `src/main/java` 文件只要位于该目录，就是 active preprocess overwrite**，不得当作 dormant 副本处理：
   - 仍承担 per-version override 的文件必须永久保留（P5-7 不删）；
   - 若某非 core 文件计划由 root replacement 接管，**必须在 Level 3 前移除本地副本**、让 root 成为实际编译输入（该移除发生在对应 flip commit 内，由 jar 等价即时证明替代物语义）；
   - P5-7 只能删除**已在 P5-6 前证明不参与实际编译**的旧副本（shared 档、core dormant 副本等）。
3. Level 3（P5-6）时的编译输入配置必须是最终配置——不允许「Level 3 测的是本地 shadow、P5-7 删后才切到未经测试的 root 版本」。

### 0.3 阶段总览

P5-0 基线冻结 + 分类清单（本档 §1-§7）→ P5-1 基础设施 + 扁平化（全平台开关 off）→ P5-1b 行为学实验（scratch worktree）→ P5-1c verifyMixinConfigs 改造 → P5-2 core flip → P5-3 1.21.x 链下翻 ×8 → P5-4 26.x 双平台 flip → P5-5 收敛核对 → P5-6 Level 1+2+3 → P5-7 删除 dormant → P5-8 文档冻结 + P5-baseline-final。

## 1. P5-0a 基线确认

- HEAD `d1ff3a0`（main，工作区干净）；`D:\Project\Carpet-Ice-Addition-P4-baseline-final` 含全部 11 平台 runtime + sources jar。
- 命令：`.\gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs verifyJarEquivalence -PbaselineDir=D:\Project\Carpet-Ice-Addition-P4-baseline-final`
- 结果：**BUILD SUCCESSFUL；4 个 verify 全部通过；jar 等价 11/11**（逐 jar zip 条目集合 / fabric.mod.json 语义 / mixin json 逐字节 / pack_format / 逐 class intermediary 引用集全部一致，含 1.21.1-1.21.11 的 157 类与 26.x 的 151 类）。HEAD 与 Phase 4 最终基线产物等价确认。

## 2. P5-0b 源码清单（inventory 实测）

脚本：`p5_inventory.py` / `p5_semantic.py` / `p5_adjacent_diffs.py`（产物在 `inventory/out/`：`per-platform-compile-set.tsv`、`fqcn-variants.tsv`、`semantic-variants.tsv`、`variant-diffs.tsv`、`adjacent-variant-diffs.txt`、`mixin-target-map.tsv`、`name-divergent-candidates.tsv`、`json-entries.tsv`、`summary.txt`）。

| 指标 | 值 |
| - | - |
| 平台数 / 编译槽位总数 | 11 / 1099（每平台 96-104） |
| 唯一 FQCN | **149** |
| md5 单变体 / 多变体 | 80 / 69 |
| **语义单变体**（剥注释/排序 import 后同文） | **90**（其中全 11 平台 23 个 = mojmap-unified 13 + 新发现 10） |
| **语义多变体**（真需 edge/宏/override） | **59** |
| md5 多变体但语义等价（仅注释/import 序/空白） | 10 |
| mixin json 条目矩阵 | 65+2 / 62+2×3 / 62+1×4 / 64+1 / 63+1×2，与各平台 mixins 包类集合**双向一致（断言③复刻 11/11 OK）** |
| 名称分叉族（@Mixin 目标相同、自有类名不同） | 27 个 mixin 目标组 + 1 个 stem 候选（villagerevents State/State26） |

## 3. P5-0c 源码分类矩阵（A-E 落位）

分类依据：语义变体分组 + 逐族相邻变体 diff 审读（`adjacent-variant-diffs.txt`，1638 行）+ mappings 实证（§4）。

### 3.1 A 类（root src 直通，无需任何变换）

- **A1 全平台语义等价（23）**：mojmap-unified 13 + 新发现 10（语义哈希确认全平台同文）。
- **A2 子集平台语义等价（67）**：单一变体但仅版本子集适用（其适用范围即 guard 条件，见 3.5）。
- **A3 跨边仅局部名/参数名/注释差异**（语义多变体但代码结构等价，root 统一后变换产物与旧档位文本差异仅限标识符拼写/注释，**字节码与 intermediary 引用集不变**）：`SculkCatalystListenerMixin`、`WardenAngerManagerMixin`、`SensorTradingOptimizationMixin`、`IronGolemVillagerOptimizationHooks`、`CraftingRefresherDispatcher`、`VillagerTradingOptimizationRuleHelper`、`CraftableCoralBlocksValidator`、`EntitySetCustomNameIronGolemOptimizationMixin`、`LivingEntityNeutralPhantomsMixin`（26 边参数名）、`NameTagItemDuplicateNamingFixMixin`（26 边 javadoc）、`WardenEntityMixin`（1.21.8/1.21.9 边仅注释）、`CraftableCoralBlocksConflictDetector`（26 边局部名）、`ServerWorldPhantomSpawnWarningMixin`（26 边 import 序）等。
  - 证据示例：`FluidTickDelayValidator` 26.x 副本仅多一行 javadoc `(26.x Mojang mappings)`；`SensorTradingOptimizationMixin` 26 边滤除注释后 diff 为空。

### 3.2 B 类（automatic mapping 承担，1.21.x 边保持 0 字节 extra）

loom 缓存 tiny 实证**同 intermediary 纯改名**（§4）：

- **1.21.11 批量（≈20 族）**：`npc.Villager→npc.villager.Villager`（class_1646）、`monster.Zombie→monster.zombie.Zombie`（class_1642）、`ZombieVillager→monster.zombie`（class_1641）、`GameRules→level.gamerules`（class_1928）、`ResourceLocation→Identifier`（class_2960，波及 KillItemCommand/MachineStatusCommand/ConflictDetector/RecipeBookHelper/DataPackController/VillagerDimension121/VillagerIdentity121 等）、`animal.horse→animal.equine.AbstractHorse`（class_1496）、`npc.WanderingTrader→npc.wanderingtrader`、`ResourceLocationArgument→IdentifierArgument`、`ResourceKey::location→identifier`。受影响家族：LoseJobOnSiteLossTask/TakeJobSiteTask/UpdateJobSiteTask/VillagerEntityIronGolem/VillagerEntityVillagerEvents/VillagerTaskListProvider/WorkStationCompetitionTask/VillagerDeathSide121/VillagerEventsRuntime121/VillagerDimension121/VillagerIdentity121/WanderingTraderEntity/MobEntityVillagerConversion/VillagerEntityTradingOptimization/VillagerTradingOptimizationTasks/DisablePlayerAttackingTamedMobs(1211 边除外)/CraftableCoralBlocks×4 等的 12110→12111 边差异（各 2-12 行 diff 全部为上述 rename）。
- **1.21.5→1.21.6 边**：`Player.serverLevel()→Player.level()`（method_51469 同 id，见 §4）→ `PlayerBreakIceLikeMagmaBlocksMixin`（6 行差异全此一处）**B 类**。
- **26.x 边（explicit edge mapping 条目，预计仅 2-3 个成员）**：`Player.displayClientMessage(Component,boolean)→sendSystemMessage(...)`（SafeScaffoldingBreak / PhantomSpawnWarningHelper / MachineStatusRollbackWarningHandler）、`getDayTime()→getOverworldClockTime()`（PhantomSpawnWarningHelper）。其余 26.x 同 FQCN 差异族均为 A3 或 C。

### 3.3 C 类（宏，单处 ≤10 行、不改 Mixin injection descriptor）

| 家族 | 边界 | 宏内容（行数） |
| - | - | - |
| `PlayerWorlds` | 1.21.5/1.21.6 | body：`getCommandSenderWorld()` ↔ `level()`（1 行；**不能用 automatic**，见 §4：method_5770/37908 为两个独立方法） |
| `KillItemCommand` | 1.21.10/1.21.11 | 调用切换：`PlayerWorlds.serverLevel(player)` ↔ `(ServerLevel) player.level()`（2 行；保持 ≤1.21.10 的 helper 间接性以维持引用集等价） |
| `MachineStatusCommand` / `MachineStatusRollbackWarningHandler` | 1.21.10/1.21.11 | ClickEvent 构造切换：`MachineStatusTextEvents.runCommand/showText` ↔ `new ClickEvent.RunCommand / new HoverEvent.ShowText`（2-4 行；同上保持 helper 间接性；26.x 边由 displayClientMessage→sendSystemMessage edge 条目覆盖） |
| `MachineStatusTextEvents` | 1.21.4/1.21.5 | `new ClickEvent(Action.RUN_COMMAND,cmd)` ↔ `new ClickEvent.RunCommand(cmd)`（6 行，既有版本边界助手） |
| `BeaconProperToolFixMixin` / `FrostedIceProperToolFixMixin` | 12111→26.1.2 | `state.is(Blocks.X)` ↔ `state.getBlock()==Blocks.X`（各 1 行） |
| `CrafterBlockMixin` | 12111→26.1.2 | `assemble(input, registryAccess())` ↔ `assemble(input)`（1 行） |
| `LavaFluidTickRateMixin` | 1.21.10/1.21.11 | `dimensionType().ultraWarm()` ↔ `EnvironmentAttributes.FAST_LAVA`（4 行） |
| `BlockItemEasyWaterloggedBlockPlacementMixin` | 12110→26.1.2 | `ultraWarm()` ↔ `EnvironmentAttributes.WATER_EVAPORATES` + `is(Items.X)` ↔ `getItem()!=`（4 行） |
| `GameRules` 读取包装族（`PhantomEntityNeutralPhantomsMixin`/`PhantomSpawnWarningHelper`/`PvpRuleHelper` 的 1.21.10/1.21.11 边） | 1.21.10/1.21.11 | `getBoolean(RULE_X)` ↔ `Boolean.TRUE.equals(get(X))`（3-4 行；GameRules 移包本身是 B） |
| `IronGolemVillagerSkipClasses` | 12111→26.1.2 | 否决类集合差异（`FollowCustomerTask` ↔ `LookAndFollowTradingPlayerSink`+`GoToPotentialJobSite`，3 行） |
| `CraftableCoralBlocksRecipeBookHelper` | 12111→26.1.2 | `player.resetRecipes` ↔ `player.getRecipeBook().removeRecipes(recipes, player)`（2 行 ×2 处） |
| `CraftableCoralBlocksDataPackController` | 12111→26.1.2 | null 判定合并（3 行） |
| 入口类 `CarpetIceAdditionMod` | 1.21.10/1.21.11 | `CarpetIceAdditionHighVersionSettings` 注册行（2 行）；1.21.1/26.x 边超限 → D |

### 3.4 D 类（per-version override，同 FQCN 同路径替换）

- **mc1211（结构性，1.21.1 旧 API 形态）**：既有平台 26 文件 + 从 `mojmap-mc1211-*` 档迁入的独占变体。代表：`BeaconBlockEntityMixin`（双参 getOpacity descriptor）、`CraftingScreenHandlerCraftableCoralBlocksMixin`（@Shadow→getInputGridSlots）、`DisablePlayerAttackingTamedMobsMixin`（@Mixin Entity→LivingEntity + 方法签名）、`FakePlayerIgnoreThornsDamageMixin`（@Desc→hurtServer）、`ItemFrameEntityMixin`（hurt→hurtServer ×3 注入串）、`LivingEntityNeutralPhantomsMixin`/`MobEntityVillagerConversionMixin`（注入 target 变化）、`VillagerEntityLightningVillagerEventsMixin`（91 行 witch 转换结构）、`ZombieEntityVillagerEventsMixin`（convertTo 形态）、`CraftableCoralBlocksConflictDetector`（69 行 RecipeDisplay）、`FindPointOfInterestTaskIronGolemOptimizationMixin`（5 参 descriptor）、`NameTagItemDuplicateNamingFixMixin`（CONSUME 语义）、`FlowableFluidFreezeMixin`、`PhantomEntityNeutralPhantomsMixin`、`GoToWorkTask/…/WalkToward…` 等。
- **mc1213/mc1214（≤1.21.4 变体）**：`JukeboxManagerRecordWorldEventMixin`（@At target Player→Entity 串）、`VillagerEntityTradingOptimizationMixin`（@At descriptor ×10 Holder 化）、`PhantomEntityNeutralPhantomsMixin`（NBT API）、`ItemFrameEntityMixin`（nbt.getBoolean 返回类型）、`VillagerIdentity121`（15 行）、`NearbyJobSiteAcquireTask`（14 行）、`VillagerTradingOptimizationTasks`（9 行，若判 C 则宏）、`DisablePlayerAttackingTamedMobsMixin`（34 行 EntityReference）等——从 `mojmap-mc1213-1214`/`mojmap-mc1211-1214` 档复制入两平台 src。
- **mc1215（ValueInput/ValueOutput 前形态）**：`ItemFrameEntityMixin`、`PhantomEntityNeutralPhantomsMixin`（既有平台文件）。
- **26.x 双份（platform-mc261/src + platform-mc262/src）**：结构性大分叉 `KillItemCommand`（491 行）、`MachineStatusCommand`（97 行）、`NearbyJobSiteAcquireTask`（73 行）、`VillagerTradingOptimizationTasks`（34 行）、入口类（22 行）；**名称分叉族 ≈38 个类**（`ItemFrameMixin`、`WanderingTraderMixin`、`CraftingMenuCraftableCoralBlocksMixin`、`FlowingFluidFreezeMixin`、`ItemUtilsPortableInfiniteWaterMixin`、`WaterFluidTickDelayMixin`/`LavaFluidTickDelayMixin`、`MobVillagerConversionMixin`、`PhantomNeutralPhantomsMixin`(+26 独有 `PhantomAttackPlayerTargetGoal…`)、`Piglin/Zombie…Spears`、villager 族 ×4、AI Behavior 族 ×14、`ServerGamePacketListenerImpl…` ×2、`BrainProviderTradingOptimizationMixin`、villagerevents26 族 ×6 等，从 `mc26x` 档复制）；**@At 字符串差异** `BeaconBlockEntityMixin`（getLightDampening）与 `EndPortalBlockCustomEndPlatformPositionMixin`（mc261/mc262 各自既有份保留，1 行 receiver 差异不宏化）。

### 3.5 E 类（整文件 guard，版本独占且适用范围为多平台连续区间）

- `MC<260000`（1.21.x 全线、26.x 排除）：1.21.x 侧名称分叉族 root 文件（对应 3.4 的 26.x 副本）——`ItemUsagePortableInfiniteWaterMixin`、`ServerPlayNetworkHandler…` ×2、`WaterFluidTickRateMixin`、`LavaFluidTickRateMixin`、`CraftingScreenHandlerCraftableCoralBlocksMixin`、`FlowableFluidFreezeMixin`、`MobEntityVillagerConversionMixin`、`PhantomEntityNeutralPhantomsMixin`、`ItemFrameEntityMixin`、`PiglinEntity/ZombieEntity…Spears`（12111）、villager 族 ×4、AI 任务族（`MultiTick/Composite/SingleTick/HideWhenBellRings/StartRaid/FindPointOfInterest/UpdateJobSite/WalkTowards×2/LoseJobOnSiteLoss/TakeJobSite/WorkStationCompetition/VillagerTaskListProvider`）、villagerevents121 族、`ServerCommonNetworkHandlerAccessor`、`PhantomFindTargetGoalNeutralPhantomsMixin` 等。
- `MC<12106`（1.21.1-1.21.5）：`BookEditScreenDisableIllegalTextCharacterCheckMixin`（client）。
- `MC<12105`（1.21.1-1.21.4）：`LegacyPvpRuleHelper`（3 平台）。
- 单平台独占（mc1211 的 `CarpetServerOnServerClosedNullFix`/`ScreenHandlerCtrlQ`/`ServerWorldAccessVillagerEvents`、mc12111 的 `BlockItemEasyWaterloggedBlockPlacementMc12111Mixin`/`ZombieEntity/PiglinEntity…Spears`）→ **D override 而非 guard**。

### 3.6 关键结构性结论

1. **「1.21.9+ ≡ 26.x ≠ 1.21.1-1.21.8」组**的真相：`DisableIllegalTextCharacterCheckMixin`（`@Inject method` `(C)Z→(I)Z` + char→int）等差异为**注入字符串/参数类型变化**——按 R2 不得宏化 → 1.21.1-1.21.8 六平台 override（从 `mojmap-mc1211-1218` 档复制）。**列入待人工确认项 §7-2**（若允许 ≤4 行机械字符串宏可免 6 份复制）。
2. `VillagerEntityLightningVillagerEventsMixin`/`ZombieEntityVillagerEventsMixin` 的 12110→12111 边差异含 `@Redirect target` 类路径串（`npc/Villager`→`npc/villager/Villager`）：字符串不重映射 → mc1211-12110 侧需 override（8 个平台复制）或字符串宏。**同 §7-2 一并裁决**。
3. `KillItemCommand` 12111 变体内联了 `player.level()` 直调（≤1.21.10 走 PlayerWorlds）→ 宏保持间接性（3.3），否则 KillItemCommand.class 的引用集相对基线漂移。
4. 26.x explicit edge mapping 预计**仅 2-3 个成员条目**（displayClientMessage→sendSystemMessage、getDayTime→getOverworldClockTime；P5-4 以机械提取+编译复核定稿）。

## 4. P5-0d 边界实证裁定（loom 缓存 layered tiny，official/intermediary/named 三列）

数据源：`D:\DevTools\Gradle\.gradle\caches\fabric-loom\<版本>\loom.mappings.*\mappings.tiny`；脚本 `p5_mapping_probe.py` + 定向 awk。

### 4.1 Entity 世界访问器（文档冲突裁定）

| intermediary | named 名 | 生命周期 |
| - | - | - |
| method_5770 | `getCommandSenderWorld` | 1.21.1-1.21.5 存在，**1.21.6 移除** |
| method_37908 | `level` | 1.21.1-1.21.8 存在于 Entity（与上者并存过），**1.21.9 起离开 Entity**（迁至 `ServerPlayer` 等自身声明，新 intermediary id，source 名仍为 `level`） |

**裁定**：二者是两个独立方法，不存在「getCommandSenderWorld 改名为 level」。
- Phase 4 文档「1.21.9」说法的来源 = method_37908 在 1.21.9 迁移 owner；PlayerWorlds 档位边界（1211-1215 / 1216-12110）对应 method_5770 的生命周期（1.21.6 移除）。**两个说法各描述一个方法**；`docs/refactor-phase4-verification.md` §4-3 已按此修正（本次 P5-0 一并提交）。
- Phase 5 影响：`PlayerWorlds` 为 **C 宏家族**（不可用 automatic mapping——5770/37908 intermediary 不连续）；edge 1.21.5↔1.21.6 与 1.21.8↔1.21.9 **均无需 rename 条目**（source 名 `level` 全程不变）；两档 PlayerWorlds 的 javadoc 记述同样不准确，待 P5-3 统一入 root 时修正。
- 基线引用集注意：1.21.1-1.21.5 基线 jar 调 method_5770（经 PlayerWorlds）、1.21.6-1.21.8 调 method_37908——root 统一后必须保持同样的间接性与方法选择，否则 verifyJarEquivalence 引用集比对失败。

### 4.2 其余边界（intermediary 连续性验证）

| 成员 | intermediary | 边界 | 结论 |
| - | - | - | - |
| `Player.serverLevel()→level()` | method_51469（同 id） | 1.21.5→1.21.6 | 纯改名 → **B automatic**（PlayerBreakIceLikeMagmaBlocksMixin 6 行差异全此一处） |
| `Villager` 包移动 | class_1646 | 1.21.10→1.21.11 | 同 id → automatic |
| `Zombie` / `ZombieVillager` | class_1642 / class_1641 | 1.21.11 | 同 id → automatic |
| `GameRules` 移包 | class_1928 | 1.21.11 | 同 id → automatic（其 `RULE_X`→`X` 字段与 `getBoolean`→`get` 方法族待 P5-3 编译复核，形态差异部分归 C 宏） |
| `ResourceLocation→Identifier` | class_2960 | 1.21.11 | 同 id → automatic（`ResourceLocationArgument→IdentifierArgument`、`ResourceKey::location→identifier` 同批） |
| `AbstractHorse` 移包 | class_1496 | 1.21.11 | 同 id → automatic |
| `WanderingTrader` 移包 | （1.21.11 = class_3989 于 `npc.wanderingtrader`；1.21.10 侧同 id 位于 `npc`） | 1.21.11 | 同 id → automatic |

## 5. P5-0e carpet API 判定

59 个语义多变体家族的全部相邻变体 diff 审读中，**未出现任何 `carpet.*` 标识符的跨版本变化**（引用面：`CarpetServer.settingsManager/parseSettingsClass/minecraft_server`、`carpet.api.settings.CarpetRule/Validator` 等在各版本间文本一致）。结论：**无 carpet API rename 流入 root，无需为 carpet 编写 extra mapping**；该结论随 P5-3/P5-4 编译期 fail-fast 复核。

## 6. P5-0f JitPack 坐标 dry-check

`https://jitpack.io/com/github/Fallen-Breath/preprocessor/c5abb4fb12aad2590c852c1bc6c8d5758606ec0b/`：
- `preprocessor-<全SHA>.jar` → HTTP 200；`preprocessor-<全SHA>.pom` → HTTP 200（POM 确认 groupId `com.github.Fallen-Breath` / artifactId `preprocessor` / version 全 SHA，依赖经 `com.github.Fallen-Breath.preprocessor:preprocessor` 传递）；`build.log` → HTTP 200（构建成功）。
- 结论：**供应链坐标可解析**，全 SHA 锁定有效。CI 侧可达性随 P5-1 后的 build.yml 实跑验证。

## 7. P5-0 待人工确认项

1. **注入字符串宏的 R2 边界解释**（§3.6-1/2）：`DisableIllegalTextCharacterCheckMixin`（`(C)Z→(I)Z`，6 行，波及 1.21.1-1.21.8 六平台 override 复制）与 `VillagerEntityLightning`/`ZombieEntityVillagerEvents`（`@Redirect target` 类路径串，波及 1.21.1-1.21.10 八平台复制）——是否允许「≤4 行、机械字符串替换、由 intermediary 引用集校验兜底」的 descriptor 简单宏作为 R2「复杂 descriptor」禁令的显式豁免？默认（不豁免）：按 D 类多平台复制 override。
2. `VillagerTradingOptimizationTasks`（1211→1213 边 9 行，`Swim→Swim<>` 泛型 + 注释）等边界 4-10 行族，P5-3 时按「宏 vs override」逐族定稿；本档不预锁。
3. JitPack 在 GitHub Actions runner 的可达性（本地已证，CI 随 P5-1 实跑）。
4. `WardenEntityMixin` 26 边 4 行差异的定类（A3 局部名 or B edge 成员）——P5-4 机械提取时裁定。

## 8. 后续阶段记录（占位）

- P5-1（基础设施 + 扁平化）：待实施。
- P5-1b / P5-1c / P5-2 / P5-3 / P5-4 / P5-5 / P5-6 / P5-7 / P5-8：待实施，实施后在此追加记录。

---

### 本次 P5-0 变更文件

| 文件 | 变更 |
| - | - |
| `docs/refactor-phase5-verification.md` | 新建（本档） |
| `docs/refactor-phase4-verification.md` | §4-3 边界表述修正（getCommandSenderWorld/level 双方法裁定，指向本档 §4.1） |

仓库外产物：`D:\Project\Carpet-Ice-Addition-P5-workspace\inventory\{p5_inventory.py, p5_semantic.py, p5_adjacent_diffs.py, p5_mapping_probe.py, out/*}`。
