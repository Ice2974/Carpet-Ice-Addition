# Phase 9 验证记录：退出 `common` Java 子项目 + 单元测试归属重构

> Phase 9 于 main 分支执行（基点 `92df229`，Phase 8 终态）。目标：`common/src/main/java` 27 个 source owner 迁入根 `src/main/java`（preprocess main 态模型）；单元测试 owner 由 `:common:test` 切换为 `:1.21.11:test`；删除 `implementation project(':common')`、平台 jar 对 `:common` classesDirs 的 merge、`include 'common'` 与 `common/build.gradle`；`common/src/main/resources` 保留原位（仅改 `rootProject` 物理路径引用，Phase 10 处理迁移）；`verifyJarEquivalence` 演进为两层 ownership 模型并保持对 P6-baseline-final 的 **11/11 runtime JAR 内容级等价**。
>
> **验收状态：完成**（2026-09-06；P9-0 基线 + C1 verifier no-op 证明 + 双 owner 等强证明 + clean 11 平台构建与全部验证器 + verifyJarEquivalence 11/11 内容级等价，见 §4 / §5；Level 3 游戏内人工测试通过（2026-09-06 人工确认，见 §6））。

## 0. 约束与口径

- 冻结项不变：依赖（JUnit 5.10.2 / gson 2.11.0 / junit-platform-launcher 1.10.2 原值接线，无任何升级）、preprocessor JitPack 全 SHA、版本图拓扑（11 节点 / 10 边）、`mainProject=1.21.11`、loom 家族闭环、P6-baseline-final（永久只读）、`org.gradle.parallel=false`、47 条 class rename mapping。
- 迁移方式：27 个 Java 文件**逐字节**复制迁入（`diff -rq` 零 differ 行），git 历史以 rename 记录（Phase 9 提交的 diffstat 中 30 个文件为 100% 相似度 rename）。
- verifier 语义边界：两层 ownership 模型仅对 `ownershipTransferredClassEntries` 放行两类**预期编译器产物差异**（见 §2）；严格 channel B 与全部非 transferred 路径行为不变；sources 新增条目在对应 owner 的全部 runtime entries 完成等价证明前不被接受。
- 本 Phase 不声称 sources jar 与 P6-baseline-final 字节等价（baseline sources jar 本就不含 common 源文件；迁移后 sources jar 新增的 27 个 `.java` 条目按 ownership transfer 解释，runtime 层由 11/11 内容级等价承载）。

## 1. 提交记录

| commit | 内容 |
|---|---|
| `f4348ba` | P9-C1：`verifyJarEquivalence` 两层 ownership 演进（Layer 1 source owners / Layer 2 class entries / major invariant / scoped channel-B / sources 解释收口）+ `selfTestRenameEquivalence` 扩至 7 例；未迁移树 no-op 证明（11/11 PASS，ownership 集合为空）。 |
| `3f76129` | P9-C2：原子切换（27 source owner 迁移 + 3 测试迁移 + 测试接线 + 删除 `:common` Java 体系 + 资源路径 + activeJavaRoots / projectsEvaluated / build.yml / 根 `loader_version` 清理）+ transferred-entry scoped channel-B 规范化扩展（2026-09-06 人工授权，见 §2）。 |
| （C3） | 本文档 + target-architecture §6/§8 + acceptance-checklist + AGENTS.md 最小现势化。 |

> 实施过程注记：C2 曾尝试拆分「verifier 扩展 / 切换」两个 commit，本地实证中间态必红（`include 'common'` 与 `common/build.gradle` 删除不能分离，`projectsEvaluated` 断言与编译同时失败），按 v3 计划的原子性判断收敛为单一 C2 提交（中间态未推送，仅本地重写）。

## 2. verifier 两层 ownership 模型与 26.x 规范化授权

- **Layer 1 `ownershipTransferredSourceOwners`**（自动推导，fail closed）：unexplained sources 条目须同时满足 current sources 新增、baseline sources 无对应 source、根 `src/main/java` 存在对应文件、baseline/current runtime jar 有 mapping-aware class 对位；11 平台集合一致；规模必须为空（未迁移树 no-op）或恰为 27。
- **Layer 2 `ownershipTransferredClassEntries`**：每个 owner 纳入 `X.class` + `X$*.class`（nested record / enum / inner / synthetic），每 entry 独立确认通过 step 1 mapping-aware bijective 对位并独立证明；跨平台 entry 集合一致。
- **major invariant**：transferred entries 的 baseline major 必须为 65（旧 `:common` `--release 21`）、current major 必须严格等于 `java_release+44`（1.21.x=65，26.x=69）、minor 必须为 0（非 preview）且两侧相等。
- **scoped channel-B 规范化范围（2026-09-06 人工授权）**：① 预期 major 字节（65 → java_release+44）；② javac lambda 合成方法名序号（`lambda$<method>$<ordinal>` 末段纯数字序号剥离）。②的背景与实证：javac 在 JDK 21→25 间将 lambda 序号分配从「类全局计数」改为「按方法计数」——同一源文件 javac 21 产出 `lambda$new$0` + `lambda$start$1`、javac 25 产出 `lambda$new$0` + `lambda$start$0`（最小复现）；26.x `--release 25` 必须 javac 25，与旧 `:common`（javac 21）之间不存在可消除该差异的构建模型改法（`--release 21` 仍产出按方法计数命名且违反 major invariant）。仅 `VanillaLanguageService` 一个类命中该模式（该类仅有的另一 lambda 位于构造器/字段初始化，跨方法序号被全局计数推移）；声明与全部 Methodref / MethodHandle / NameAndType 引用经同一 remapper 入口一致改写；minor 与其余全部结构 / 指令 / 字面量差异继续 fail closed；**严格 channel B 不做任何 lambda 规范化**（selfTest 用例 7 锁定）。
- **selfTest 7 例**：① transfer + nested `$` + 仅合法 version 差异 PASS（byte-identical / channel-B 双路径）；② 非 ownership class 仅 version 差异 FAIL；③ transferred current major 与 java_release 不符 FAIL；④ literal/instruction 差异 FAIL；⑤ 非 ownership nested `$` class version 差异 FAIL；⑥ transferred lambda 序号差异 PASS；⑦ 同一 lambda 序号差异在非 ownership class 上 FAIL。

## 3. 迁移清单（Layer 1 = 27 source owners）

`FeatureCompatibilityReporter`；`command/`：KillItemConfigManager、MachineStatusConfigManager、MachineStatusKind、MachineStatusRollbackCommandMatcher、MachineStatusRollbackWarningConfig、MachineStatusStateUtil；`rules/`：CraftableCoralBlocksRecipes、CraftableCoralBlocksState、CraftableCoralCraftingRefresher、CustomEndPlatformPositionHelper、FluidTickDelayUtil、IceLikeMagmaBlocksHelper、IronGolemSkipMarked、IronGolemVillagerOptimizationAccess、IronGolemVillagerOptimizer、ItemFrameInteractionHelper、VillagerTradingOptimizationAccess；`settings/`：CarpetIceAdditionHighVersionSettings、CarpetIceAdditionLowVersionSettings、CarpetIceAdditionSettings；`translation/`：CarpetIceAdditionTranslations、TranslationFormatUtil；`util/`：RuleTextFormatUtil；`villagerevents/`：VanillaFormatString、VanillaLanguageService、VillagerEventsCompatibility。

**Layer 2 = 每平台 42 个 runtime class entries**（27 顶层 + 15 nested：`MachineStatusConfigManager$MachineRecord/State/ConfigFileData/MachineRecordData`、`KillItemConfigManager$Snapshot/State/ConfigFileData`、`MachineStatusRollbackWarningConfig$Snapshot/State/ConfigFileData`、`MachineStatusStateUtil$ParsedState`、`FluidTickDelayUtil$CachedDelayState`、`VanillaLanguageService$State`、`CustomEndPlatformPositionHelper$IntPosition`、`ItemFrameInteractionHelper$FrameCustomizationAction`），跨平台集合断言一致。

## 4. P9-0 基线与双 owner 等强证明（未提交试运行）

| 项 | 结果 |
|---|---|
| P9-0 工作区 | clean（HEAD `92df229`）；`gradlew projects` = 13 projects（root + `:common` + 11 版本项目） |
| P9-0 `:common:test` | 3 类 46 测试（FluidTickDelayUtilTest 33 / IronGolemVillagerOptimizerTest 8 / VanillaLanguageServiceTest 5）全 PASS；XML：`common/build/test-results/test/TEST-*.xml` |
| 双 owner 同跑 | `:common:test` 与 `:1.21.11:test` 各 3 类 46 测试 0 失败 0 错误，双侧 XML 类集合与计数一致 |
| classpath 诊断 | `:1.21.11` `testCompileClasspath` / `testRuntimeClasspath` 无 `project :common`、无 `common.jar`、无 common 输出（仅 netty-common 等无关第三方构件） |
| 11 平台 test | `:1.21.11:test` 46 测试；其余 10 平台 `test` 全部 NO-SOURCE（`compileTestJava` 稳态复验确认） |
| 双 owner 状态边界 | 不运行 jar / publication / `verifyJarEquivalence`（classesDirs merge 在双 owner 态会产生重复条目） |

## 5. 自动验证矩阵（C2 提交前全绿）

| 验证 | 结果 |
|---|---|
| `git diff --check` | C1 / C2 提交前均干净 |
| clean 11 平台 `build`（含 27 文件对 11 组 carpet/MC 组合的独立编译） | **BUILD SUCCESSFUL**（R1 编译风险清零；首轮即通过） |
| `:1.21.11:test` | 46 测试 PASS；其余 10 平台 test NO-SOURCE |
| `verifyCraftableCoralBlocksJars` / `verifyFabricModJson` | 通过（11/11） |
| `verifyMixinConfigs` | 通过（11/11，mixins/client 条目与编译产物双向一致） |
| `verifyClassRenameMapping` | OK（47 条 mapping 终态不变；active roots 已去除 common 目录，core 测试目录经平台循环自动纳入扫描） |
| `selfTestRenameEquivalence` | OK（含 P9 7 例） |
| `verifyJarEquivalence -PbaselineDir=D:/Project/Carpet-Ice-Addition-P6-baseline-final` | **11/11 runtime JAR 内容级等价**：9 个 1.21.x 平台 transferred entries **42/42 byte-identical**；26.1.2 / 26.2 transferred entries **42/42 scoped channel-B**（全部条目 major 65→69 invariant 通过；`VanillaLanguageService.class` 为唯一含 lambda 序号规范化的条目——javap 指令级 diff 确认 delta 仅合成方法名 + 常量池 3 处引用 + major，26.2 手工核验 delta 形状一致）；非 transferred 路径 byte-identical、普通资源 byte-identical、特殊资源按既有专项语义 invariant 验证；不声称 raw bytes / SHA-256 一致 |
| `gradlew projects` | **12 projects**（root + 11 版本项目）；`:common` 不存在 |
| CI 状态 | 本地 CI 等价命令集（build.yml 命令 + 全部 verify 任务）全绿；push 后随 GitHub Actions 常规观察，如失败按既有约束上报人工决策 |

## 6. 人工验证

- **Level 3 游戏内人工测试通过（2026-09-06 人工确认）**：全 11 平台 dedicated server + client / integrated-server 代表平台，覆盖迁移代码直接承载的行为面（`/log villagerEvents`（`VanillaLanguageService`，26.1.2 / 26.2 必测）、waterFluidTickDelay / lavaFluidTickDelay（`FluidTickDelayUtil`）、ironGolemSpawningOptimization（`IronGolemVillagerOptimizer`）、itemFrameInvisible / itemFrameFixed（`ItemFrameInteractionHelper`）、customEndPlatformPosition、`/killitem` 与 `/machineStatus` 配置读写（两个 ConfigManager + RollbackWarningConfig）等；执行范围以人工执行记录为准）。singleplayer / client-only 行为未以 dedicated server 结果替代。
- Level 3 的行为载体证明链：runtime JAR 与 P6-baseline-final 11/11 内容级等价（§5），迁移仅改变类的编译归属，不改变字节语义（除 §2 登记的两类编译器产物差异，均已人工授权并经游戏内验证）。

## 7. 实施发现（新增记录）

- **preprocess 插件会重写全部 sourceSet 的 java srcDirs，并把 core 平台的 test 源经版本图变换传播进各平台 `build/preprocessed/test`**：首次接线后 `:1.21.1:compileTestJava` 出现 junit 依赖缺失编译错误。修复：common.gradle `afterEvaluate` 将 test sourceSet 的 srcDirs 统一恢复为本平台本地 `src/test/java`（core 存在 = 唯一测试入口；非 core 不存在 = NO-SOURCE），与 main 资源恢复同时序语义（restore 晚于插件改写）。
- lambda 序号分配策略的 javac 版本差异（§2），及其在 27 个迁移类中仅命中 1 类的分布。

## 8. 待人工确认项

- ~~26.x transferred-entry scoped channel-B 规范化范围扩展（lambda 序号）~~ **已授权并关闭**（2026-09-06 人工授权 + 游戏内验证，见 §2）。
- push 后观察 GitHub Actions Build 结果（build.yml 已显式加入 `:1.21.11:test`）；如失败按既有约束上报，不以放宽断言适配。
- `common/src/main/resources` 的迁移（含根 src 资源语义是否调整）归 Phase 10。
