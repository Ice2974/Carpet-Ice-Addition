# Phase 5 验证记录：Fallen-Breath 风格源码架构收敛（preprocess 版本图 + 根 src + per-version override）

> 状态：**Phase 5 全部完成并冻结（2026-09-05）**——P5-0～P5-8 逐段记录见 §1-§12；Level 3 人工回归通过；dormant 删除完成（§12.1）；基线快照 `D:\Project\Carpet-Ice-Addition-P5-baseline-final` 建立（§12.2）。
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

## 8. P5-1 基础设施 + 扁平化（commit 2337cf3，2026-09-05）

修改：settings.gradle（pluginManagement + JitPack + resolutionStrategy + include 扁平化 `:platform-mcXXXX`）、根 build.gradle（plugins 块 + 11 节点 10 边版本图 + `strictExtraMappings=true` + 路径字面量）、gradle.properties（`preprocess_version` 全 SHA）、versions/mainProject（`platform-mc12111`）+ 10 个 0 字节 mapping 占位、common.gradle（`preprocess_enabled` 条件接入预留 + afterEvaluate 资源恢复 + 根 src 无 resources 断言 + shared_tiers 空断言）、THIRD_PARTY_NOTICES.md（preprocessor + TIS/AMS 登记）。

验证：全量 build + 4 verify + jar 等价 11/11 全绿；插件确认经 JitPack 解析入 Gradle 缓存（`com.github.Fallen-Breath/preprocessor`）。全平台开关 off，行为零变化。

## 9. P5-1b 行为学实验（scratch worktree `D:\Project\Carpet-Ice-Addition-P5-lab`，不入主线）

实验方式：lab worktree 中完整原型化 P5-2（core flip）与 P5-3 首步（mc12110 flip）。root src = mc12111 完整编译集 100 文件（8 平台 + 92 档位逐字节复制）；lab 内对 mc12111/mc12110 置 `preprocess_enabled=true` + `shared_tiers=`。**最终态：clean build 后双翻转平台 + 其余 9 平台，4 verify + jar 等价 11/11 全绿。**

### 9.1 逐项结论

| 项 | 结论 |
| - | - |
| (a) 任务与 sourceSet 改写 | ✓ core java sourceSet = [根 src] 原位编译；非 core = [本地 src/main/java（overwrite 层）， build/preprocessed 生成目录] |
| (b) 资源恢复 | ✓ afterEvaluate 恢复后全部资源语义等价（verifyFabricModJson / verifyCraftableCoralBlocksJars / jar 等价共同证明）；core 资源同径恢复 |
| (c) overwrite 语义 | ✓ **本地 overwrite 文件不经宏处理、不经重映射、直接编译**；同路径时生成侧跳过该文件；异路径文件作为附加源编译（同名 FQCN 不冲突由 javac 保证） |
| (d) 整文件 guard | ✓ 输出文件保留、全部内容行变 `//$$ ` 注释、无类型声明、不产 class（zip 条目集不受影响）。**核心纪律：root src 必须以「main 态」书写**——core 对 root src 原位编译、不运行宏引擎，凡对 1.21.11 不成立的代码行必须以 `//$$` 前缀注释态存放（等价于引擎对 MC=12111 求值后的输出态） |
| (e) 字符串字面量 | **推翻 P5-0 §3.4 假设**：preprocessor 的源重映射器**会重映射 mixin 注解字符串**（实证：mc12110 生成源码中 `@Redirect target` 类路径 `npc/villager/Villager` 自动变为 `npc/Villager`）。裸方法名在上游侧遇多重载时报歧义错（`convertTo` 双重载），解法 = 行级 `//#disable-remap`（该行原样输出，两版本方法名相同时输出文本与旧档位一致）或补全 descriptor。@At 字符串族的 override 需求收窄至「descriptor 真变（方法增删参）」者 |
| (f) automatic mapping | ✓ remap↔remap 边经 loom OMM 完全生效：1.21.11 批量 rename（Villager/Zombie/WanderingTrader/GameRules/ResourceLocation→Identifier/AbstractHorse 包移动族）零手工条目，生成源码正确换名并编译 |
| (g) strictExtraMappings | ✓ `=true` + 0 字节 1.21.x 边正常（strict 路径无抱怨） |
| (h) cleanupUnnecessaryMappings | ⏸ 未实测（需真实 mapping 内容，留 P5-4 附带观察） |
| (i) clean build 依赖顺序 | ✓ `clean` 后从零构建（无 build/preprocessed 缓存）全绿；verifyMixinConfigs 以编译产物枚举后对翻转平台正确 |
| (j) core 项目行为 | ✓ 本地 src/main/java 完全不参与 sourceSet（lab 中 dormant 8 文件保留、无重复类错误、断言③不计入）；core 资源经恢复逻辑正常打包 |
| (k) mainProject | ✓ `../mainProject` 解析 + 内容 `platform-mc12111` 匹配成功（core 被正确识别，无节点查找错误） |
| (l) evaluationDependsOn | ✓ 变换链（mc12110 → core 输出）正常 |

### 9.2 新发现（重要）

1. **`org.gradle.parallel=true` 与 preprocess 冲突**：非 core 平台的 preprocessCode 在执行期解析上游项目 compileClasspath，Gradle 9 抛「attempted without an exclusive lock」。`--no-parallel` 下正常。**处置：P5-3 起（首个非 core flip）将根 gradle.properties 的 `org.gradle.parallel` 改为 false（或全部命令加 --no-parallel），CI 命令不变**；构建时间影响可接受（workers.max=2 本已限流）。core-only flip（P5-2）不受影响。
2. **仓库行尾约定 = CRLF blob**：本会话曾误判为 LF 并触发一次全库 renormalize（已 `reset --hard` 撤销）与一次 LF blob 误提交（已 amend 回 CRLF）。后续所有源文件写入保持 CRLF；lab worktree 需以 `--no-checkout` + `core.autocrlf=false` 重建以避免 smudge 伪影（Phase 2 已知伪影复核成立）。
3. **worktree 空目录伪影**：mc261/mc262 的 `src/main/resources/data/carpet-ice-addition/`（未跟踪空目录）不随 checkout 携带，lab 需手工补齐才能过 zip 条目比对；主线不受影响。该空目录为历史遗留，P5-7 清理时一并评估。
4. **E2 期间在 lab 施加并验证的 P5-3 式处理**（mc12110 步骤完整原型，全部达到等价）：mc12111 独有 mixin ×3 的整文件 guard（`MC>=12111` 裸文本）；GameRules/EnvironmentAttributes 宏 ×4 家族；PlayerWorlds / MachineStatusTextEvents / BlockItem base 以「main 态 //$$」入 root + guard（`MC<12111` 等）；KillItemCommand / MachineStatusCommand / MachineStatusRollbackWarningHandler 的 helper 间接性宏（维持引用集等价）；MobEntityVillagerConversionMixin 的 `//#disable-remap` ×2（convertTo 歧义）。

## 10. P5-1c verifyMixinConfigs 产物化改造（commit f8d0be7）

断言③反向枚举从「srcDirs 下 .java 文件」改为「jar 内 mixins 包 top-level class（排除 `$` 内部类）」。实证链条：lab 中旧口径对整文件 guard 空输出 .java 误报（mc12110 报 3 个未注册类）→ 改造后误报消除；主线旧体系一致性证明：改造前后 11 平台校验计数逐平台一致（67/64/64/64/63/63/63/63/65/64/64）。json→class 防悬空断言不变；双向等强。

## 11. P5-2～P5-5 实施记录（2026-09-05，全部完成）

### 11.1 提交序列（分支 phase5-preprocess）

| commit | 阶段 | 内容 |
| - | - | - |
| 8564a88 | P5-2 | core flip：root src = mc12111 完整编译集 100 文件；mc12111 开关 on + shared_tiers 空；8 平台文件 dormant；等价 11/11 |
| f8245c2 | P5-3a | mc12110 flip：3 个 1.21.11 独有 mixin guard、GameRules/EnvironmentAttributes 宏 ×4、helper 间接性宏 ×3、PlayerWorlds/MachineStatusTextEvents/BlockItem base 以 main 态入 root、disable-remap ×2（convertTo 歧义）、入口类 High/Low 注册宏（修复自类引用盲区）；parallel=false |
| 8d72461 | P5-3b | mc1219 flip：1219/12110 边零差异，纯属性翻转（P5-0 判定实证） |
| 595a985 | P5-3c | mc1218 flip：DisableIllegalText 旧形态 override + PvpRuleHelper 整文件 guard(MC>=12105) + 三分支宏 |
| ea24a20 | P5-3d | mc1216 flip：同 mc1218 集，override 复制 + 纯属性翻转 |
| 95521ba | P5-3e | mc1215 flip：BookEditScreen 入 root(guard MC<12106)；发现并修复「player.level() 同名多版本双重解析」（SafeScaffolding/RuleMessageThrottle 两处 disable-remap）；TrialSpawner rename 经 automatic 实证 |
| f8583e5 | P5-3f | mc1214 flip：9 个 <=1.21.4 形态 override + LegacyPvpRuleHelper 入 root(guard MC<12105)；MachineStatusTextEvents else 分支首次激活 |
| 4c8c5be | P5-3g | mc1213 flip：与 mc1214 同集（仅 FindPointOfInterest 平台自有变体不同） |
| f98097a | P5-3h | mc1211 flip：root 3 个 1.21.3+ 类补 guard；平台既有 26 文件转 active override + 3 个档位独占变体复制；BlockItem base guard 修正为 MC<12111（zip 比对检出 P5-0 归因笔误） |
| e098b27 | P5-4c | mc261 flip：37 纯 + 6 复合 MC<260000 guard；mapping-mc12111-mc261.txt 填充（displayClientMessage→sendSystemMessage、getDayTime→getOverworldClockTime，AMS 短式）；50 个 26.x override 复制（38 独有 FQCN + 12 结构性同 FQCN）；5 处 26.x C 宏（main 态方向） |
| 751b235 | P5-4d | mc262 flip：mc261 同批 50 文件双份复制；本地 EndPortal(26.2 形态)保留；mapping-mc262-mc261.txt 保持 0 字节 |

### 11.2 P5-5 收敛核对（2026-09-05）

- 属性审计：11 平台 shared_tiers 全空、preprocess_enabled 全 true。
- **全仓 `clean` 后从零重建**：build + 4 verify + verifyJarEquivalence(P4-baseline-final) 全绿（33 项 OK；
  全部 preprocess 变换、宏求值、edge 重映射、override 组装、资源恢复经冷启动验证）。
- FQCN 唯一性：编译层由「同路径 override 替换语义 + core 不读平台目录 + guard 空输出」与 javac 重复类检查
  双重保证（clean 构建通过即无重复）；源文件层的同路径配对（82 对）均为设计内 override 对，非重复。
- 旧档位状态：versions/shared 全部 Java 档（19 个）与 platform-mc12111 的 8 个文件进入 **dormant**
  （零引用、不参与任何编译），等待 P5-7（Level 3 通过后单独 commit 删除）；纯资源档 mc1213-12111 仍在役。

### 11.3 遗留与移交

- ~~P5-6（Level 3 人工回归）~~ **已于 2026-09-05 由人工验证完成**（Ice2974 确认通过），P5-7/P5-8 放行。
- P5-7（删 dormant，单独 commit）与 P5-8（文档冻结 + P5-baseline-final 快照）随后执行，记录见 §12。

## 12. P5-6 人工回归与收尾（2026-09-05）

- **P5-6 Level 3 人工回归：通过**（人工确认）。覆盖范围按计划 §10 矩阵：全部 11 平台加载与规则注册矩阵
  （40 / 38×7 / 39×3，含入口类 High/Low 注册宏边界）、/killitem 与 /machineStatus 命令族、
  machineStatusRollbackWarning、边界平台（mc1211 / mc1214-1215 / mc1215-1216 / mc1218-1219 /
  mc12111 / mc261 / mc262）高风险 Mixin 行为。
- 门禁放行：满足删除纪律（§0.2-2）——全部 dormant 副本（19 个 Java 档、platform-mc12111 的 8 文件）
  在 P5-6 前已证明不参与任何平台编译（shared_tiers 全空 + 全仓冷重建 + 等价 11/11）。
- 待人工确认项更新：§7-1（注入字符串宏豁免）实际未启用——所有 descriptor 差异族按默认走 override 落地
  （DisableIllegalTextCharacterCheck 6 平台复制等），无需豁免裁决；§7-4 WardenEntity 26 边以 override 落地。

### 12.1 P5-7 dormant 删除（commit `21bbd78`，2026-09-05）

- 删除对象（全部满足 §0.2 删除纪律，Level 3 前已证明不参与任何平台编译）：
  - `versions/shared/` 19 个 Java 档（mojmap-\* 17 档 + mc26x，合计 227 文件）与 4 个无引用空骨架档；
  - `platform-mc12111/src/main/java` 8 个 dormant 副本（P5-2 起 core 编译集 = 根 src，本地目录不参与 sourceSet）；
  - `common.gradle` 移除 `shared_tiers` Java 档叠加机制（含 preprocess 前的 shared_tiers 断言），保留
    `extra_resource_dirs` 资源档机制与 preprocess 接入；11 平台 `gradle.properties` 删除 `shared_tiers` 空键与 flip 注释。
- 保留：纯资源档 `versions/shared/mc1213-12111`（10 个珊瑚配方，8 平台 `extra_resource_dirs` 仍在役；
  `git ls-files` 复核 versions/shared 仅剩该档）。
- 净变化：247 文件、+27 / −18750 行。
- 删后验证：`clean` 全量 build + verifyCraftableCoralBlocksJars + verifyFabricModJson + verifyMixinConfigs +
  verifyJarEquivalence（P4-baseline-final）全绿——mixin config 11 / intermediary refs 11 / L1-6 equivalence 11
  （33 OK）；`:common:test` 通过（构建缓存命中，输入未变）；`git diff --check` 干净。

### 12.2 P5-8 文档冻结 + P5-baseline-final 快照（2026-09-05）

- `refactor-target-architecture.md`：Phase 5 标记完成——头部目标声明改为「完整迁移达成」；§6 新增
  「Phase 5 执行结果」（扁平化 `:platform-mcXXXX`、main=1.21.11、根 src 105 文件与 override 分布、
  mapping 策略实证、Phase 6 类名统一展望）；§5.2 `shared_tiers` 行加移除注记；§8 备选对比与
  §9 待确认项 2 / 8 关闭。
- `refactor-acceptance-checklist.md`：陈旧引用修正——L1-5 与 §2 前言去 Yarn 口径；§3.3 与附录 B 的
  /killitem、/machineStatus、itemFrame 实现分布改为「根 src + mc261 / mc262 override」现实；
  附录 A 说明注明「份数」为 P4 基线口径。L1-1 验证命令无变化（符合计划预期）。
- `AGENTS.md`：目录边界改写（根 src 主源码树 / versions/shared 纯资源化 / platform-\* override 职责）；
  「shared 与跨版本规则」整节改写为「根源码树与跨版本规则」（main 态纪律、宏上限、override 语义、
  mapping 管理、新增平台流程、`parallel=false` 原因）；版本注册表段更新（扁平化、Mojmap layered remap
  口径、版本图接线）；规则同步表与全平台验证范围同步。
- 基线快照：`D:\Project\Carpet-Ice-Addition-P5-baseline-final` 建立（11 runtime + 11 sources jar，
  与 P4-baseline-final 同构），jar 取自 P5-7 删后验证构建；作为后续阶段（Phase 6 类名统一等）的对照锚点。
- Phase 5 终态对计划 §11 最终验收标准：①11 平台全部经「根 src + 版本图变换 + 平台 override」编译、
  子项目为根直接子项目；②versions/shared 仅剩纯资源档、shared_tiers 机制与全部 dormant 副本移除；
  ③verifyJarEquivalence 对 P4-baseline-final 11/11 零适配零忽略（verifyMixinConfigs 产物化改造经旧体系
  一致性证明，§10）；④全部 verify + `:common:test` 绿（CI 待合并后由 GitHub Actions 覆盖）；⑤Level 3
  通过并记录（§12）；⑥THIRD_PARTY_NOTICES 登记完整（P5-1）；⑦文档冻结（本节）；⑧P5-baseline-final
  快照建立（本节）。

---

### 本次 P5-0 变更文件

| 文件 | 变更 |
| - | - |
| `docs/refactor-phase5-verification.md` | 新建（本档） |
| `docs/refactor-phase4-verification.md` | §4-3 边界表述修正（getCommandSenderWorld/level 双方法裁定，指向本档 §4.1） |

仓库外产物：`D:\Project\Carpet-Ice-Addition-P5-workspace\inventory\{p5_inventory.py, p5_semantic.py, p5_adjacent_diffs.py, p5_mapping_probe.py, out/*}`。
