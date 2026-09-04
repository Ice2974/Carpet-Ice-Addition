# Phase 3 源码结构优化验证记录

> 本文档是 [refactor-target-architecture.md](refactor-target-architecture.md) Phase 3 的产出物，记录各步骤的基线、等价性证据与验证结果。对照数据见 [refactor-baseline.md](refactor-baseline.md)（下称"基线"，仅历史参考），验收条目见 [refactor-acceptance-checklist.md](refactor-acceptance-checklist.md)（下称"验收清单"）。

## 1. 状态总览

| 状态 | 值 | 说明 |
|---|---|---|
| Phase 2 门禁 | **accepted（2026-09-04，人工验收）** | `refactor-phase2-verification.md` 已更新 |
| Phase 3 基线 | 已建立（P3-0，§2） | Phase 1/2 等价基线自此仅作历史参考，不作 Phase 3 验收基准 |
| P3-1 implementation status | **complete（2026-09-04）** | 代码改动落地，§3 自动化验证全部通过 |
| P3-1 acceptance status | **accepted（2026-09-04）** | §5 人工项（L2 定向 + L1-5 冒烟）已由人工执行并通过 |
| P3-2 implementation status | **complete（2026-09-04）** | 代码改动落地，§4 自动化验证全部通过；jar 条目差异白名单核对一致 |
| P3-2 acceptance status | **blocked-on-manual-items** | §5 人工项（mc261/mc262 L2 定向 + L1-5 冒烟）已完成 |

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

## 4. P3-2：26.x 命名链收敛

### 4.1 变更内容

mc261 与 mc262 之间已确认的"纯命名差异"重复链统一为 `shared/mc26x` 单份，类名去除版本后缀（命名方案：无后缀，版本隔离由源码目录 / source set 表达）：

| 迁移前（mc261 + mc262 各一份） | 迁移后（mc26x 单份） | 非命名差异（迁移前实测） |
|---|---|---|
| `command/KillItemCommandMc261/2` | `command/KillItemCommand` | **0 行** |
| `command/MachineStatusCommandMc261/2` | `command/MachineStatusCommand` | **0 行** |
| `command/MachineStatusRollbackWarningHandlerMc261/2` | `command/MachineStatusRollbackWarningHandler` | **0 行**（全部为类名自引用与对 MachineStatusCommandMc26x 的引用） |
| `mixins/ServerGamePacketListenerImplMachineStatusRollbackWarningMixin` | 同名迁入 mc26x | **0 行**（全部为 Handler 类名引用） |
| `CarpetIceAdditionMod`（入口类） | 同名迁入 mc26x | **2 行注释**（"26.1 / 26.2 检测器构造 SlotDisplayContext…"版本字样，中性化为"检测器构造…"；可执行代码零差异） |

- 共 10 个物理文件收敛为 5 个（mc261 副本 git mv 保历史，mc262 副本删除）；31 处 `Mc261` 引用经内容替换去除。
- mc262 副本删除与迁移同 commit：与 P3-1 同理，拆开会产生同 FQCN 双包含或引用断裂的中间态，无法保持逐 commit 可构建。
- 与 Yarn 侧同名类的共存：`KillItemCommand`（mc121x-killitem / mc1216-1218 档）与 `MachineStatusCommand`、`MachineStatusRollbackWarningHandler`（mc1211-1214 / mc1215-12111 档）与 26.x source set 无交集（`shared_tiers=mc26x`），满足"每平台每 FQCN 恰一份"不变式。
- 明确不动：`EndPortalBlockCustomEndPlatformPositionMixin`（mc261/mc262 间 1 行真实 API 差异 `getBottomCenter()` ↔ `Vec3.atBottomCenterOf()`，保留平台覆盖）；`bridge/Mc261Bridge` / `Mc262Bridge`（P3-4 范围）。

### 4.2 等价性证据（对应本阶段强制约束）

| 约束 | 证据 |
|---|---|
| sourceSet 加载顺序等价 | 本步对 `gradle.properties` / `build.gradle` / `common.gradle` / `settings.json` 零改动；mc261/mc262 的 `shared_tiers=mc26x` 声明不变，文件物理位置从平台目录移入两平台均已引入的 mc26x 档 |
| jar class/resource 集合 | 与 P3-0 基线逐平台比对条目清单：9 个 1.21.x 平台**零差异**；mc261/mc262 差异恰为预期白名单（§4.3），无任何资源 / mixin json / metadata 条目变化；jar 内 `fabric.mod.json` 与 `carpet-ice-addition-mc26X.mixins.json` 与基线**字节一致** |
| mixin 注册路径 | mixin json 零改动（字节一致）；迁移的 `ServerGamePacketListenerImplMachineStatusRollbackWarningMixin` 类名与 FQCN 不变；另显式核验：mc261/mc262 jar 内 mixin json 全部条目（mixins + client）均能解析到 jar 内实际类条目 |
| 不改变外部行为 | 命令名、参数树、权限判断（`CommandHelper.canUseCommand` × 规则门）、logger 名称、rule 名称、翻译键、fabric.mod.json entrypoints 全部未触碰；变更仅限类名与源码位置 |

### 4.3 jar 条目差异白名单（对 P3-0 基线，mc261 与 mc262 各 11 删 11 增，逐一核对一致）

```
command/KillItemCommandMc26X.class + 4 内部类（$CachedKillItemResult/$CachedSummaryEntry/$ClearResult/$SummaryEntry）
  → command/KillItemCommand.class + 同名 4 内部类
command/MachineStatusCommandMc26X.class + 4 内部类（$1/$MachineRuntimeStatus/$MachineStatusIssueReason/$MachineWithStatus）
  → command/MachineStatusCommand.class + 同名 4 内部类
command/MachineStatusRollbackWarningHandlerMc26X.class
  → command/MachineStatusRollbackWarningHandler.class
```

`CarpetIceAdditionMod.class`、`ServerGamePacketListenerImplMachineStatusRollbackWarningMixin.class` 及全部资源条目不变。

### 4.4 自动化验证（全部通过，2026-09-04）

| 验证项 | 命令 | 结果 |
|---|---|---|
| 全平台构建 + 既有验证 | `.\gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson :common:test --stacktrace` | 通过（11 平台全绿，common 单测通过） |
| jar 条目差异分析 | 逐平台 `diff <(unzip -Z1 基线 jar) <(unzip -Z1 当前 jar)` | 9 平台零差异；mc261/mc262 差异 == §4.3 白名单 |
| metadata 字节比对 | `cmp` jar 内 fabric.mod.json 与 mixins.json（vs 基线） | mc261/mc262 全部字节一致 |
| mixin 条目完整性 | jar 内 mixin json 全条目 ↔ jar 内类条目 | mc261/mc262 全部可解析 |
| 残留搜索 | 全仓 grep `KillItemCommandMc26` / `MachineStatusCommandMc26` / `MachineStatusRollbackWarningHandlerMc26` | 零残留 |
| 工作区卫生 | `git diff --check` / `git diff --cached --check` | 通过 |

### 4.5 回滚

revert 本步单 commit 即可（git 记录为 5 个重命名 + 5 个删除）。

## 5. 人工项清单

### P3-1（已执行，2026-09-04）

| 项 | 内容 | 执行人 | 日期 | 结果 |
|---|---|---|---|---|
| L2 定向 | mc1213 与 mc1214：验收清单 2-2 villagerTradingOptimization（认站/补货/职业生命周期）、2-3 ironGolemSpawningOptimization | Ice2974 | 2026.9.4 | 通过 |
| L1-5 冒烟 | mc1213、mc1214 dev 实例启动无 mixin/注册错误，`/carpet` 可用 | Ice2974 | 2026.9.4 | 通过 |

### P3-2（已执行，2026-09-04）

| 项 | 内容 | 执行人 | 日期 | 结果 |
|---|---|---|---|---|
| L2 定向 | mc261 与 mc262：验收清单 2-7 /killitem 全子命令、2-8 /machineStatus 全子命令、2-12 machineStatusRollbackWarning（含 `config/machine_status_rollback_warning.json` 读写与回退） | Ice2974 | 2026.9.4 | 通过 |
| L1-5 冒烟 | mc261、mc262 dev 实例启动无 mixin/注册错误，`/carpet` 可用，`/log villagerEvents` 可订阅（入口类迁移后注册行为核验） | Ice2974 | 2026.9.4 | 通过 |

P3-2 未触及命令参数树、权限、翻译键与语言文件，`docs/commands*.md`、`docs/rules*.md`、`docs/loggers*.md` 无需变更；`docs/refactor-acceptance-checklist.md` §3.3 与附录 B 的实现分布描述已同步（26.x 统一为 `shared/mc26x`）。

## 6. Phase 3 强制约束遵从记录

1. 每步骤先建新基线：P3-0 已执行（§2）；P3-2 为条目变更步骤，其人工验收（§5）通过后按 ratchet 以新 HEAD 重建基线。
2. Phase 1/2 基线仅历史参考：P3-1 起全部等价判定改用 P3-0 基线。
3. 源码移动三要件（sourceSet 顺序等价 / jar 集合确认 / mixin 注册路径）：P3-1 见 §3.2，P3-2 见 §4.2。
4. 禁止为减文件数合并结构不同 Mixin、用宏替代覆盖、改规则/命令/logger 行为：P3-2 仅收敛非命名差异为 0 行的重复链与入口类（2 行注释差异）；`EndPortalBlockCustomEndPlatformPositionMixin` 存在 1 行真实 API 差异，保留平台覆盖未合并。
5. 删除独立 commit：结构性删除（Bridge ×11、空档 ×2 等 P3-4 内容）将独立成 commit；P3-1/P3-2 的副本删除与迁移不可分割（§3.1 / §4.1）。
