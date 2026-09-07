# Phase 10 验证记录：root resources 收敛 + `common/` / `versions/shared` 资源档完全退出

> Phase 10 于 main 分支执行（基点 `7eaf266`，Phase 9 终态 + AGENTS.md docs-only 修补）。目标：`common/src/main/resources`（icon + 中英 lang）与 `versions/shared/mc1213-12111`（10 个 modern 珊瑚配方）全部迁入根 `src/main/resources`，`common/` 与 `versions/shared/` 目录及 `extra_resource_dirs` 数据键整体退出，平台资源 srcDirs 终态收敛为 [平台本地， 根]，runtime JAR / 元数据 / 资源包行为与 P6-baseline-final 保持 **11/11 内容级等价**。
>
> **验收状态：代码与自动验证完成**（2026-09-07；P10-0a 独立 fixture + P10-0b 真仓瞬时 probe 行为前提实证 + P10-A / P10-B 原子切换 + 全部验证器 + verifyJarEquivalence 11/11，见 §2 / §5；Level 3 游戏内人工测试尚未执行，见 §6 / §8）。

## 0. 约束与口径

- 冻结项不变：依赖、preprocessor JitPack 全 SHA、版本图拓扑（11 节点 / 10 边）、`mainProject=1.21.11`、loom 家族闭环、P6-baseline-final（永久只读，不重建）、`org.gradle.parallel=false`、47 条 class rename mapping、Java verifier 豁免范围（不新增任何 classfile 规范化）。
- 迁移方式：资源文件**逐字节** git mv 迁入（git 历史以 100% 相似度 rename 记录）；1.21.1 的 10 个 old-schema 配方保留平台本地原位（终态唯一依赖平台本地同路径覆盖的平台，schema 分叉完整保留）。
- 等价口径：runtime JAR 与 P6-baseline-final 11/11 内容级等价（既有专项语义 invariant 口径不变）；本 Phase 额外执行 L2 资源专项检查（含 sources artifact，见 §5）。
- **26.x 构建输出树约束**：26.1.2 / 26.2 运行时 jar 中的 `data/`、`data/carpet-ice-addition/` 两个空目录条目源自构建输出树（`build/resources/main`）中旧时代残留的空目录，被 plain 家族 `jar` 任务持续打包；P6 baseline jar 同样含这 2 个条目。对这些平台执行 `clean` 后再构建会使条目消失并导致 `verifyJarEquivalence` 非 class 条目集合比较失败——不得 clean 后等价验证（1.21.x remap 家族的 remapJar 不写纯目录条目，不受影响；P5→P9 多轮增量重建已实证该残留跨 processResources 重跑存活）。

## 1. 提交记录

| commit | 内容 |
|---|---|
| `2ce9a69` | P10-A：common/ 纯资源档退出（icon/lang 迁入根 src/main/resources）+ 资源 srcDirs 接线切换 + Phase 5「根 src 仅 java」断言退役 + processResources / sourcesJar 显式 EXCLUDE + 配置期资源碰撞不变式（全平台预期空集）+ 根资源所有权不变式 + verifyClassRenameMapping 移除 common 扫描项。 |
| `f7636e8` | P10-B：versions/shared 与 extra_resource_dirs 完全退出（10 个 modern 配方迁入根 + 26.x 本地副本删除 + settings.gradle 目录断言收紧 + 8 份平台 gradle.properties 数据键移除 + 碰撞预期演进为 1.21.1 = 10 条 recipe 路径 + shared 功能性文本扫描块删除）。 |
| `b4e8a5c` | P10-C：本文档 + target-architecture §5/§6/§8 + AGENTS.md 现势化（目录边界 / 源码架构 / 注册表断言 / 资源 wiring 规则 / 验证范围）。 |

## 2. P10-0 行为前提实证

### 2.1 P10-0a 独立 Gradle fixture（系统临时目录，零仓库触碰）

最小 `java` 插件工程，两个资源目录含同相对路径 `probe/p10.json`（不同字节），以 `-PprobeOrder` × `-PprobeStrategy` 矩阵驱动（每次 `clean` 隔离，规避 up-to-date 假象）：

| 运行 | srcDirs 顺序 | strategy | 结果 |
|---|---|---|---|
| 1 | [platform, root] | EXCLUDE | `probe/p10.json` = platform 字节，runtime jar 恰 1 条目（first-wins） |
| 2 | [root, platform] | EXCLUDE | = root 字节（**胜者严格跟随 srcDirs 顺序**） |
| 3 | [platform, root] | 默认（不设置） | **BUILD FAILED**：`Entry probe/p10.json is a duplicate but no duplicate handling strategy has been set`（Gradle 9.2.1 默认对重复条目 fail-fast，非静默 INCLUDE） |
| 1b | [platform, root] | EXCLUDE，跑 sourcesJar | `sourcesJar`（Zip 型任务，直接消费 resources srcDirs、不经 processResources 输出）遇重复同样默认 fail-fast；显式 EXCLUDE 后恰 1 条目 |

结论：候选行为（[平台本地， 根] 顺序 + processResources / sourcesJar 显式 EXCLUDE）语义与预期一致；EXCLUDE 的 first-wins 是确定性的但**不是 fail-closed**（静默忽略后续同路径项），碰撞集合的 fail-closed 裁决由碰撞不变式承担（§3）。

### 2.2 P10-0b 真仓瞬时 probe（不提交，完成后零残留）

- 基线记录：probe 前 `git status --porcelain` = 空、`git diff` = 空、HEAD `7eaf266`、无 stash；probe 清理后逐项比对一致。
- 临时 wiring（git restore 恢复）：根 `src/main/resources` 以平台本地之后的次序接入全部平台 srcDirs + `processResources` / `sourcesJar` 显式 EXCLUDE + Phase 5「根 src 仅 java」断言临时移除；**不安装**碰撞不变式（允许 probe 文件制造受控碰撞）。
- probe 文件：`probe/p10.json` 分别置于根（root 标记）与 `versions/1.21.1`（remap 代表 = 终态唯一依赖平台本地 same-path override 的平台，non-core preprocess 路径）、`versions/26.2`（plain 代表）本地。
- 观测（**不 clean**）：

| 平台 / 任务 | probe 条目 | 内容 | 结论 |
|---|---|---|---|
| `:1.21.1:remapJar` | 恰 1 | platform-1.21.1 | 平台本地 same-path 胜出（remap / non-core） |
| `:1.21.1:remapSourcesJar` | 恰 1 | platform-1.21.1 | sourcesJar EXCLUDE first-wins 同语义 |
| `:26.2:jar` | 恰 1 | platform-26.2 | plain 家族同样平台胜出；`data/` 空目录条目保持 2（残留存活动证） |
| `:26.2:sourcesJar` | 恰 1 | platform-26.2 | 同上 |
| `:1.21.11:jar`（core 附加检查） | 恰 1 | root | 根资源向 core 平台传播（loom interim dev jar 观测） |

- 清理：probe 文件删除 + `git restore common.gradle` + 三平台重跑使 stale 输出移除生效（jars probe 条目归零）→ `git status --porcelain` / `git diff` 与基线逐字节一致。
- 偏离协议未被触发（无一项与预期 / Javadoc 相悖）。

## 3. 两道新增配置期防线（common.gradle afterEvaluate，任何 Gradle 调用自动执行）

- **资源碰撞不变式**：对本平台生效资源 srcDirs 求文件相对路径的多所有者交集，交集必须恰等根 `build.gradle` 注入的 `expectedRootResourceCollisions[本平台]`。实现约束（按修订计划落实）：expected map keySet 必须与版本注册表**全等**（未来新增平台缺键不会被默认当作空集放行）；相对路径统一以 `/` 规范化（Windows 与 CI/Linux 一致）；1.21.1 的预期集合由 `craftableCoralPackRoot` + `craftableCoralRecipePaths` 单一来源派生（不维护第二份配方名单；pack root 提为共享 def，`verifyCraftableCoralBlocksJars` 与不变式共用同一字面量）。注意 ext 注入点位于 `craftableCoralRecipePaths` 定义之后（脚本顺序依赖）。
- **根资源所有权不变式**（长期）：根 `src/main/resources` 不得包含 `fabric.mod.json` 与 `resourcepacks/craftable_coral_blocks/pack.mcmeta`（平台 / 资源包元数据保持平台本地所有权）；**根 `*.mixins.json` 不设禁令**（Phase 11 单一 Mixin 配置体系届时再评估，不做无条件长期禁令）。
- 负向测试（两阶段各一次）：临时在 26.2 本地创建与根同路径的 `assets/carpet-ice-addition/lang/en_us.json` → 配置期精确失败（`actual=[assets/carpet-ice-addition/lang/en_us.json] expected=[]`）→ 删除后复验通过。正向：P10-B 终态 1.21.1 的 10 条真实碰撞被预期集合精确放行（全量构建含 `:1.21.1` 全任务链通过）。

## 4. 迁移清单

- **P10-A（common/ 退出）**：`common/src/main/resources/assets/carpet-ice-addition/icon.png`、`lang/en_us.json`、`lang/zh_cn.json` → 根同路径（3 文件，100% rename）；`common/` 整体删除（含 P9 遗留 untracked 空目录 `common/src/test/java/...`、`common/build/*`）。
- **P10-B（shared / extra_resource_dirs 退出）**：`versions/shared/mc1213-12111/src/main/resources/resourcepacks/craftable_coral_blocks/data/carpet-ice-addition/recipe/*.json` 10 文件 → 根同路径（100% rename）；`versions/26.1.2`、`versions/26.2` 本地 10 配方副本删除（原与 shared 字节恒等）；`versions/shared/` 整体删除（含 P9 遗留 untracked 空目录 `mc1213-12111/src/main/java/...`、空 `data/carpet-ice-addition/`）；8 份平台 `gradle.properties`（1.21.3–1.21.11）删除 `extra_resource_dirs=mc1213-12111`；`settings.gradle` 目录断言 `∪ {shared}` 收紧为恰等注册表。
- **脚本层**：`common.gradle`（srcDirs 终态 [平台本地， 根] + EXCLUDE + 两道不变式 + tiers 机制移除）；根 `build.gradle`（`craftableCoralPackRoot` 提取 + `expectedRootResourceCollisions` 注入 + verifyClassRenameMapping 移除 common / shared 目录扫描）。

## 5. 自动验证矩阵（P10-B 提交前全绿）

| 验证 | 结果 |
|---|---|
| `git diff --check` | P10-A / P10-B 提交前均干净 |
| `gradlew build :1.21.11:test verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs verifyClassRenameMapping selfTestRenameEquivalence` | P10-A、P10-B 各全量通过（**BUILD SUCCESSFUL**） |
| `:1.21.11:test` | 3 类 46 测试 PASS（未动） |
| `verifyJarEquivalence -PbaselineDir=D:/Project/Carpet-Ice-Addition-P6-baseline-final` | **11/11 runtime JAR 内容级等价**（P10-A、P10-B 各复验；transferred ownership 模型行为不变） |
| `gradlew projects` | **12 projects**（root + 11 版本项目） |
| 碰撞不变式负向测试 | 两阶段各 1 次，配置期精确报碰撞路径并 FAIL；清除后复验 PASS（§3） |
| L2 资源专项（11 jar） | icon.png / lang(en,zh) 各恰 1 条目；全 jar **0 重复条目名**；icon 字节 SHA 与根源文件一致（`9d8751f7…`） |
| L2 sources artifact 专项（P10-B 新增） | 1.21.1 sources jar：10 配方各恰 1 条目、字节 = 本地 old-schema（10/10）；26.2 sources jar：10 配方各恰 1 条目、字节 = 根 modern（10/10） |
| L2 schema 分叉锁定 | 运行时 jar brain 配方（schema 分叉代表）SHA：1.21.1 = `b39d6292…`（old-schema，= 本地原文件）；26.2 = `3c1d867f…`（modern，= 根/shared 原字节） |
| 26.x 空目录条目 | `data/` + `data/carpet-ice-addition/` 保持 2 条目，与 P6 baseline 一致（全程未 clean，见 §0） |

## 6. 人工验证

- **Level 3 游戏内人工测试：尚未执行**（待人工确认项）。本 Phase 无 Java 行为改动，Level 3 重点应为资源承载面：内置资源包 `craftable_coral_blocks` 在 1.21.1（old-schema）与 26.x（modern）的配方可用性、mod 图标 / 双语规则文本显示（`/carpet` 分类 Ice 的规则名与介绍）。
- 行为载体证明链：runtime JAR 与 P6-baseline-final 11/11 内容级等价（§5）+ L2 资源专项逐字节锁定；迁移仅改变资源的源码树归属，不改变 jar 内字节。

## 7. 实施发现（新增记录）

- **26.x 运行时 jar 的空目录条目是构建输出树残留的持续复现**（§0）：所有平台的 `build/resources/main` 都残留旧时代 `data/carpet-ice-addition/` 空目录（历史 datapack 配方移除后的孤儿目录），plain 家族 `jar` 直接打包输出树 → 空目录条目进 jar；remap 家族 remapJar 重打包丢弃纯目录条目。P6 baseline 同分布（26.x 有、1.21.x 无），等价成立的前提是该残留不被 clean。
- **srcDirs 顺序变化不构成 Gradle up-to-date 输入变化**：fixture 中仅调换 srcDirs 顺序时 `processResources` 保持 up-to-date、输出为旧顺序产物（首轮实证的"root_first 仍 platform 胜出"反常即此假象）；行为实验必须 `clean` 隔离。真仓 probe 依赖 probe 文件为新增输入天然失效该假象。
- **`tasks.named('sourcesJar')` 在 `java { withSourcesJar() }` 之前不可用**（配置期任务未注册）：显式 EXCLUDE 接线用惰性 `tasks.configureEach` 名称匹配表达。
- **preprocess 插件对 core 平台的资源改写被 afterEvaluate restore 覆盖后，根资源目录对 core / non-core 行为一致**（probe 的 1.21.11 观测；根资源以 [平台本地， 根] 次序接入全部平台）。

## 8. 待人工确认项

- **Level 3 游戏内人工测试**（§6）尚未执行：重点为 1.21.1 old-schema 与 26.x modern 内置资源包配方、mod 图标与双语规则文本显示。
- **push 后观察 GitHub Actions Build 结果**：本地验证矩阵全绿，但 CI 在 push 前无法观测；如失败按既有约束上报，不以放宽断言适配。
- **26.x 输出树残留约束**（§0）如未来需要 clean 后等价验证，需先人工决策处理方式（例如为 baseline 重建开专项授权），本 Phase 未做任何处理。
