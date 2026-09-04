# Phase 3 源码结构优化验证记录

> 本文档是 [refactor-target-architecture.md](refactor-target-architecture.md) Phase 3 的产出物，记录各步骤的基线、等价性证据与验证结果。对照数据见 [refactor-baseline.md](refactor-baseline.md)（下称"基线"，仅历史参考），验收条目见 [refactor-acceptance-checklist.md](refactor-acceptance-checklist.md)（下称"验收清单"）。

## 1. 状态总览

| 状态 | 值 | 说明 |
|---|---|---|
| Phase 2 门禁 | 人工确认通过（2026-09-04，用户声明） | `refactor-phase2-verification.md` 尚未同步更新 acceptance status，待人工补记 |
| Phase 3 基线 | 已建立（P3-0，§2） | Phase 1/2 等价基线自此仅作历史参考，不作 Phase 3 验收基准 |
| P3-1 implementation status | **complete（2026-09-04）** | 代码改动落地，§3 自动化验证全部通过 |
| P3-1 acceptance status | **blocked-on-manual-items** | §4 人工项（L2 定向 + L1-5 冒烟）未执行 |

## 2. P3-0：Phase 3 基线快照

| 项 | 值 |
|---|---|
| 基线来源 | `main` @ `7395a9e`（Phase 2 收尾提交），工作区干净 |
| 快照目录 | `D:\Project\Carpet-Ice-Addition-P3-baseline`（仓库外） |
| 内容 | 11 平台 `build/libs` 全量产物（22 个 jar：11 主包 + 11 sources）+ mc1213/mc1214 迁移前后 FQCN 清单（`manifest-*-pre.txt` / `manifest-*-post.txt`） |
| 生成方法 | `.\gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson :common:test` 后 `cp --parents versions/platform-*/build/libs/*.jar <快照目录>/` |
| 自检 | 生成后立即 `verifyJarEquivalence -PbaselineDir=<快照目录>`：11/11 通过（证明快照结构与产物完整） |

基线管理约定（ratchet）：零行为变化步骤对当前基线必须零差异；有意改变 jar 条目的步骤（重命名/删除类）在人工验收通过后立即以新 HEAD 重建基线。

## 3. P3-1：mc1213/mc1214 注释等价副本合并

### 3.1 变更内容

| 类 | platform-mc1213 副本 | platform-mc1214 副本 | 处置 |
|---|---|---|---|
| `rules/VillagerTradingOptimizationTasks` | javadoc 标注 "MC 1.21.2 ~ 1.21.3" | javadoc 标注 "MC 1.21.4" | git mv 入 `shared/mc1213-1214`，javadoc 合并为 "MC 1.21.2 ~ 1.21.4"，删除 mc1214 副本 |
| `rules/NearbyJobSiteAcquireTask` | 同上 | 同上 | 同上 |
| `mixins/VillagerEntityTradingOptimizationMixin` | 同上 | 同上 | 同上 |

- 迁移前实测：三对副本除 1 行 javadoc 版本范围描述外**逐字节等价**（`diff --strip-trailing-cr` 逐对核实）；迁移后档位单份与两份原副本各仅差该 javadoc 行。
- mc1214 副本删除与迁移同 commit：拆开会出现两份同 FQCN 同时在源码集内的中间态，compileJava 直接失败（duplicate class），无法保持"每个 commit 可构建"。
- `FindPointOfInterestTaskIronGolemOptimizationMixin`（mc1213/mc1214 间的真实结构分叉，5 参 vs 6 参 BiPredicate 重载）**不在本步范围**，两平台副本原样保留。

### 3.2 等价性证据（对应本阶段强制约束）

| 约束 | 证据 |
|---|---|
| sourceSet 加载顺序等价 | 本步对 `gradle.properties` / `build.gradle` / `common.gradle` / `settings.json` 零改动（`git diff` 为空）；`shared_tiers` 声明与顺序逐字不变，仅文件物理位置从平台目录移入两平台均已引入的 `mc1213-1214` 档 |
| 解析后源码集等价 | mc1213、mc1214 迁移前后合并源码根 FQCN 清单 `diff` 完全一致（各 100 项、零重复；清单存于基线快照目录） |
| jar class/resource 集合 | `verifyJarEquivalence` 对 P3-0 基线 11/11 通过：zip 条目清单、fabric.mod.json 逐键语义、mixin json 字节、pack.mcmeta pack_format 全部零差异 |
| mixin 注册路径 | 类 FQCN 与 mixin json 均未改动（json 字节一致由 L1-6 覆盖）；另显式核验：mc1213/mc1214 jar 内三个类条目存在且路径为原 FQCN，`carpet-ice-addition-mc1213.mixins.json` 仍列出 `VillagerEntityTradingOptimizationMixin` |
| 禁止合并结构不同 Mixin / 条件宏 / 行为修改 | 本步仅移动 javadoc 差异的等价副本；无宏、无任何可执行代码变化（对两份原副本的 diff 各仅 1 行 javadoc） |

### 3.3 自动化验证（全部通过，2026-09-04）

| 验证项 | 命令 | 结果 |
|---|---|---|
| 全平台构建 + 既有验证 | `.\gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson :common:test --stacktrace` | 通过（11 平台全绿，common 单测通过） |
| jar 内容级等价 | `.\gradlew.bat verifyJarEquivalence -PbaselineDir=<P3-0 基线>` | 11/11 平台零差异 |
| 工作区卫生 | `git diff --check` / `git diff --cached --check` | 通过 |

### 3.4 回滚

revert 本步单 commit 即可（git 记录为 3 个纯重命名 + 3 个删除）。

## 4. 人工项清单（未执行，Agent 不得代验）

| 项 | 内容 | 执行人 | 日期 | 结果 |
|---|---|---|---|---|
| L2 定向 | mc1213 与 mc1214：验收清单 2-2 villagerTradingOptimization（认站/补货/职业生命周期）、2-3 ironGolemSpawningOptimization | Ice2974 | 2026.9.4 | 通过 |
| L1-5 冒烟 | mc1213、mc1214 dev 实例启动无 mixin/注册错误，`/carpet` 可用 | Ice2974 | 2026.9.4 | 通过 |

本步未触及规则/命令/logger 语义、语言文件与文档，`docs/rules*.md`、`docs/commands*.md`、`docs/loggers*.md` 无需变更。

## 5. Phase 3 强制约束遵从记录

1. 每步骤先建新基线：P3-0 已执行（§2）；后续步骤按 ratchet 约定刷新。
2. Phase 1/2 基线仅历史参考：P3-1 起全部等价判定改用 P3-0 基线。
3. 源码移动三要件（sourceSet 顺序等价 / jar 集合确认 / mixin 注册路径）：见 §3.2 证据表。
4. 禁止为减文件数合并结构不同 Mixin、用宏替代覆盖、改规则/命令/logger 行为：P3-1 范围仅限 javadoc 等价副本。
5. 删除独立 commit：结构性删除（Bridge ×11、空档 ×2 等 P3-4 内容）将独立成 commit；本步的副本删除与迁移不可分割（§3.1）。
