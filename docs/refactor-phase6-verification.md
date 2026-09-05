# Phase 6 验证记录：项目自有类名统一（显式 mapping 等价验证框架）

> Phase 6 于 main 分支执行（基点 5953a8e，Phase 5 终态）。目标：消除仅因项目内部历史命名（Phase 4 迁 Mojmap 前的遗留拼写、版本后缀）形成的类名分叉，统一方向为「`@Mixin` 目标类的最新 Mojmap 拼写」，并重构 `verifyJarEquivalence` 使 rename 在显式声明的前提下可被证明等价。

## 0. 已确认决策（人工锁定，全程有效）

- 统一方向：保留 Mojmap target 风格——项目类名 = `@Mixin` 目标类简单名（+ 用途后缀）；mc26x 平台 override 名即为目标现名，P6-2～P6-5 保持 mc26x 零改动。
- `verifyJarEquivalence` 的唯一差异豁免通道 = `versions/class-rename-mapping.txt` 显式 mapping；禁止 wildcard / `!allow:` 字面量 / blanket ignore；rename 之外的常量、方法、字段、指令、字符串字面量、注释或资源差异必须失败并人工分析（fail closed）。
- channel A（sourcesJar 源码规范化等价）优先，channel B（asm classfile 规范化等价）兜底；不自建宽松 comparator。A 的 canonicalization 仅覆盖 code 段整词类名改写（Java 类型 / self-reference 语义），字符串字面量 / char / text block / 注释逐字符原样比较——任何 literal / 注释差异都必须失败。
- per-platform bijective：mapping 应用到某平台 baseline 条目后必须一一对应，同平台不得产生映射碰撞；不同平台的互斥历史名允许映射到同一目的地。
- P5-baseline-final 永久只读；如工具链漂移需同工具链 P5 对照，只允许以 Phase 5 HEAD `5953a8e472b16e6007ab6158cc1cc68f4bb16eb7` clean build 建立明确标记的辅助 baseline，禁止从含 Phase 6 改动的工作区重建。P6-baseline-final 只能在 Phase 6 完整验收（含 Level 3 人工回归）后建立。
- `PvpRuleHelper` / `LegacyPvpRuleHelper` 本期不合并（避免扩大为 API wrapper 重构），列入后续清尾候选。

## 1. 分步记录

| 步骤 | commit | 内容 |
|---|---|---|
| P6-1 | d710919 | 验证框架：`versions/class-rename-mapping.txt`（空表起步）；`verifyJarEquivalence` 重写（baseline 双布局兼容：`versions/platform-*/build/libs` 与 flat `platform-*/build/libs`；mapping 感知 bijective 条目对齐 + 碰撞检测；sources jar mapping 感知覆盖检查；renamed-pair channel A/B；mixins.json 字节一致或 mapping 感知解析等价；intermediary refs 对位比较）；新增 `verifyClassRenameMapping`（mapping 语法 / 终态 / active Java 类型与文件 / mixin 配置 / 功能性文本分层检查，docs / references / README 历史名豁免）与 `selfTestRenameEquivalence`（fail-closed 语义自测）；CI 接入两个新任务。空 mapping 下 11 平台全量验证对照 P5-baseline-final 全绿。 |
| P6-2 | ea73d4d | golem 优化族 13 对改名（按 `@Mixin` 目标配对：CompositeTask→GateBehavior、MultiTickTask→Behavior、SingleTickTask→OneShot、FindPointOfInterestTask→AcquirePoi、HideWhenBellRingsTask→ReactToBell、LoseJobOnSiteLossTask→ResetProfession、StartRaidTask→SetRaidStatus、TakeJobSiteTask→YieldJobSite、UpdateJobSiteTask→AssignProfessionFromJobSite、WorkStationCompetitionTask→PoiCompetitorScan、VillagerTaskListProvider→VillagerGoalPackages、VillagerEntity→Villager IronGolemOptimization、WalkTowardsJobSiteTask→GoToPotentialJobSite）+ mc1211 附加 3（GoToWorkTask→AssignProfessionFromJobSite、WalkTowardJobSiteTask→GoToPotentialJobSite、WalkToNearestVisibleWantedItemTask→GoToWantedItem）+ 平台 override 同步（mc1211×4、mc1213/1214×1）；9 份 JSON 同步；mapping +16。 |
| P6-3 | 4c95bbc | 实体名族 9 对：ItemFrameEntity→ItemFrame、WanderingTraderEntity→WanderingTrader、PhantomEntity→Phantom（NeutralPhantoms）、PhantomFindTargetGoal→PhantomAttackPlayerTargetGoal（NeutralPhantoms）、MobEntity→Mob（VillagerConversion）、VillagerEntity/ZombieEntity→Villager/Zombie（VillagerEvents / Lightning）、VillagerEntityTrading→VillagerTradingOptimization；平台 override mc1211×6、mc1213×3、mc1214×3、mc1215×2；9 份 JSON 同步；mapping +9。 |
| P6-4 | 205e59d | 杂项族 9 对：PiglinEntity/ZombieEntity MobsSpawnWithoutSpears→去 Entity、FlowableFluidFreeze→FlowingFluidFreeze、LavaFluid/WaterFluid TickRate→TickDelay、CraftingScreenHandler→CraftingMenu（CraftableCoralBlocks）、ServerPlayNetworkHandler→ServerGamePacketListenerImpl（两条）、ItemUsage→ItemUtils（PortableInfiniteWater）；mc1211 override×2；9 份 JSON 同步（spears 两条仅 mc12111）；mapping +9。 |
| P6-5 | 9c9ae11 | BlockItem 原子 consolidation：根两份变体合并为单一 `BlockItemEasyWaterloggedBlockPlacementMixin`（外层 `//#if MC<260000`，蒸发判定一行 `//#if MC>=12111` 宏分支，`EnvironmentAttributes` import 随门控内联）；删除 `…Mc12111Mixin`；mc12111 JSON 条目切换；mapping +1（累计 35）。该对 channel A（core 平台 sources jar 载原始 main 态文本，门控结构差异）不等价为预期，由 channel B 证明——B 通道为此增强：SourceFile 与 LineNumberTable 属编译期调试元数据（consolidation 引入门控注释行导致行号表漂移），两侧统一剔除后比较结构与指令；selfTest 增补行号漂移场景。 |
| P6-6 | dd8c314 | villagerevents 6 对去版本后缀（逐对 diff 确认职责一致后执行）：VillagerEventsLogger121/26、VillagerEventsRuntime121/26、VillagerEventSnapshot121/26、VillagerIdentity121/26、TextRenderer121/26、VillagerEventState26→VillagerEventState；根 5 文件 + mc261/mc262 各 6 文件 + mc1211/1213/1214 Identity override；引用同步（根与 mc1211 的 Mod / 事件 mixin、mc26x Mod / mixin）；villagerevents 不在 mixin JSON，无 JSON 变更；mapping +11（累计 46）。`VillagerDeathSide121` / `VillagerDimension121` / `VillagerEventConversionScope121` 为 1.21.x 独有单例（无 26.x 对应物，不在审批的 6 对范围内），名称保留。 |
| P6-7 | （本 commit） | 文档同步：验收清单附录 A / §3.3 / 附录 C 现势化、target-architecture §3.1 示例与 Phase 6 执行结果、baseline §6.3/§6.4 注记；本记录；全仓旧名扫描。 |

## 2. 等价验证新口径（P6-1 起）

- **baseline 双布局**：`-PbaselineDir` 同时支持 `versions/platform-*/build/libs`（P4 系快照）与 `platform-*/build/libs`（P5 系 flat 快照）；baseline 永久只读。
- **条目集合**：`com/ice2974/**.class` 条目按显式 mapping 做 bijective 对齐（期望条目数 != baseline 条目数即映射碰撞失败）；其余条目严格同名。sources jar 做 mapping 感知双向覆盖检查（门控空文件被同路径 override 吸收时允许合并，条目数不做强相等）。
- **renamed-pair 证明**：channel A = baseline/new sources jar 对应 `.java` 的源码规范化等价（Java 源分段器切分 code / lineComment / blockComment / string / char / textBlock，仅 code 段整词改写 mapping 中的类名，其余逐字符比较；首个差异位置与上下文随失败输出）；channel B = Gradle 发行版自带 asm（`asm-9.8` / `asm-commons-9.8`，零新增依赖）`SimpleRemapper` + `ClassRemapper` 规范化 internal name、统一剔除 SourceFile / LineNumberTable 后重建字节做严格比较（字符串字面量不被重映射，literal 差异即失败）。A/B 均不可用或均不等价 → 失败（fail closed）。
- **mixins.json**：优先字节级一致；字节不一致时按 mapping 做解析级等价（数组顺序保留，mapping 外差异失败）。
- **intermediary refs**（P4-1 防线保留）：renamed class 与 baseline 对位条目比较引用集合；plain 形态（26.x）空集通过，renamed-pair 语义由 channel A/B 承担。
- **verifyClassRenameMapping**：mapping 语法 / old 重复 / old==new / 简名交叠 / 链式映射断言；old 源文件与类型声明必须已不存在、new 源文件与类型声明必须存在；mixin 配置不得引用 old 简名；active .java 的 code 段 / 字面量段出现 old 简名失败（注释段仅报告，历史说明允许）；gradle / CI / 资源文本出现 old 简名或 FQCN 失败；docs / references / README / AGENTS / 构建输出排除在扫描外。

## 3. 自动验证矩阵（P6 每个正式 commit 均执行）

| 验证 | 结果 |
|---|---|
| `git diff --check` | 干净 |
| `gradlew build`（11 平台全量编译 + jar + `:common:test`） | 通过 |
| `verifyCraftableCoralBlocksJars` / `verifyFabricModJson` / `verifyMixinConfigs` | 11/11 通过 |
| `verifyClassRenameMapping` | 通过（mapping 终态 46 条） |
| `selfTestRenameEquivalence` | 通过（channel A fail-closed 语义 + channel B 行号漂移场景） |
| `verifyJarEquivalence -PbaselineDir=D:/Project/Carpet-Ice-Addition-P5-baseline-final` | 11/11 通过；renamed-pair 全部经 channel A 源码规范化等价证明（除 P6-5 BlockItem 对经 channel B classfile 规范化等价证明）；P6-6 起 mc26x 平台 rename 亦全部 channel A 证明 |

P6-2 期间的负向验证：先执行改名、后补 mapping 前的差异必然使条目集合检查失败（missing/extra 条目逐条列出），确认无 wildcard 放行；`.minecraft` 本地运行时目录曾被文本扫描误读（非源码），已加入扫描排除。

## 4. Level 3 人工游戏内测试方案（Phase 6 专属，不启动自动测试）

原则：改名不改变任何注入目标与行为，验证重点为「全平台 Mixin bootstrap 正常 + 各改名 mixin 所属规则行为不回归」。

- **全量测试平台（4 个）**：mc1211（override 最多 + LowVersion + mc1211 专属改名：AssignProfessionFromJobSite / GoToPotentialJobSite / GoToWantedItem / AcquirePoi 形态）、mc1218（1.21.x 中位、仅根 src 形态）、mc12111（core 原位编译 + HighVersion + BlockItem 统一类 + spears）、mc261（plain 形态 + 26.x override 改名 + villagerevents 统一）；mc262 在 mc261 通过后仅做启动 + Mixin bootstrap + 规则数量矩阵冒烟（与 mc261 同为 plain 且源码仅 EndPortal 差异）。
- **启动项**：每个平台 dedicated server 启动无 mixin 错误（`required=true` + `defaultRequire=1` 使任何改名遗漏在启动期 fail-fast）；`/carpet list` 规则数量符合清单 §3.1 矩阵。
- **改名 mixin 代表性场景（按族抽验，依据：同族改名共享同一验证面）**：
  - golem 族（13 类）：`ironGolemSpawningOptimization` 开启后村民标记 / 铁傀儡生成跳过路径生效，重点 mc1211（三个 1211 专属形态）与 mc261（Behavior / OneShot / GateBehavior 新名生效）。
  - 实体名族：`itemFrameInvisible` / `itemFrameFixed`（展示框隐形与固定）、`namedWanderingTraderPersistence`（流动商贩持久化）、`neutralPhantoms` + `phantomSpawnWarning`（幻翼仇恨与警告）、`villagerTradingOptimization`（交易所村民认领工作站）、四个 villager-events / conversion mixin（`villagerEvents` logger 死亡 / 僵尸化 / 女巫化输出）。
  - 杂项族：`easyWaterloggedBlockPlacement`（水桶副手放置含水方块；岩浆 / 高温维度判定分档——mc12111 走 WATER_EVAPORATES、1.21.x 走 ultraWarm、26.x 走 26.x override）、`waterFluidTickDelay` / `lavaFluidTickDelay`（freeze 与数值两态）、`craftableCoralBlocks`（合成界面刷新）、`portableInfiniteWater`、`mobsSpawnWithoutSpears`（mc12111 / mc26x）、`machineStatusRollbackWarning`（回档警告）、`disableIllegalTextCharacterCheck`（网络处理器分支）。
  - villagerevents 族（P6-6 全部平台受影响）：mc1211 / mc12111 / mc261 三档 `/log villagerEvents` 订阅输出、语言翻译回退、无订阅者高频短路。
- **client 侧**：mc1211 + mc261 各启动一次集成客户端（书编辑 / 剪贴板非法字符、TAB 列表名）确认 client mixin 正常；本项目玩家可见文本均为服务端 literal，改名不涉及翻译键。

## 5. 待人工确认项

- Level 3 人工回归（§4）尚未执行——通过后方可建立 P6-baseline-final 并进入发布流程。
- P6-baseline-final 建立步骤（验收通过后执行，仓库外目录）：以 P6 收尾 commit 干净工作区全量 `gradlew build`，将 11 个 `versions/platform-*/build/libs` 产物（jar + sources jar）按 `platform-*/build/libs` flat 布局复制到 `D:\Project\Carpet-Ice-Addition-P6-baseline-final`，并在目录内记录 commit SHA 与构建命令。
- `VillagerDeathSide121` / `VillagerDimension121` / `VillagerEventConversionScope121`（1.21.x 独有单例）与 `PvpRuleHelper` / `LegacyPvpRuleHelper`（API wrapper 性质）不在本期范围，列为后续清尾候选。
- 文档中 `Yarn / Mojmap 双胞胎`等历史标签按「历史记录保留旧名」约定仅在基线文档以注记形式存档，未篡改历史验证记录（phase2/3/4/5 验证记录未改动）。
