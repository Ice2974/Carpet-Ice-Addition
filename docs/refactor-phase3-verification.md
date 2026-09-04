# Phase 3 源码结构优化验证记录

> 本文档是 [refactor-target-architecture.md](refactor-target-architecture.md) Phase 3 的产出物，记录各步骤的基线、等价性证据与验证结果。对照数据见 [refactor-baseline.md](refactor-baseline.md)（下称"基线"，仅历史参考），验收条目见 [refactor-acceptance-checklist.md](refactor-acceptance-checklist.md)（下称"验收清单"）。

## 1. 状态总览

| 状态 | 值 | 说明 |
|---|---|---|
| Phase 2 门禁 | **accepted（2026-09-04，人工验收）** | `refactor-phase2-verification.md` 已更新 |
| Phase 3 基线 | 已建立（P3-0，§2） | Phase 1/2 等价基线自此仅作历史参考，不作 Phase 3 验收基准 |
| P3-1 implementation status | **complete（2026-09-04）** | 代码改动落地，§3 自动化验证全部通过 |
| P3-1 acceptance status | **accepted（2026-09-04）** | §8 人工项（L2 定向 + L1-5 冒烟）已由人工执行并通过 |
| P3-2 implementation status | **complete（2026-09-04）** | 代码改动落地，§4 自动化验证全部通过；jar 条目差异白名单核对一致 |
| P3-2 acceptance status | **accepted（2026-09-04）** | §8 人工项（mc261/mc262 L2 定向 + L1-5 冒烟）已执行并通过；P3-2 后基线已按 ratchet 以 `32d0f26` 重建（见 §5.1） |
| P3-3 implementation status | **complete（2026-09-04）** | `verifyMixinConfigs` 任务落地，§5 自动化验证全部通过；纯增量校验，零源码 / json / 产物变化 |
| P3-3 acceptance status | **accepted（自动化验收）** | 无人工项；变异自测证明三类检测均有效（§5.3） |
| P3-4 implementation status | **complete（2026-09-04）** | Bridge ×11 删除（`d904d6f`，独立 commit）+ 空档 ×2 工作区删除（git 不跟踪，无 commit，§6.6）；自动化验证全绿 |
| P3-4 acceptance status | **accepted（2026-09-04）** | §8 人工项（任一平台 L1-5 冒烟）通过 |
| P3-5 implementation status | **complete（2026-09-05）** | Level 3 自动化验收 + 文档同步 + 状态冻结（§7）；本轮零源码变更 |
| P3-5 acceptance status | **accepted（2026-09-05，人工验收）** | 验收清单 Level 3 游戏内完整回归（§3.1–§3.5 人工部分）已执行并通过（§8）；**Phase 3 全部步骤完成**，最终基线见 §10.2 |

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

## 5. P3-3：verifyMixinConfigs 校验任务

### 5.1 基线刷新（P3-2 后，先行执行）

P3-2 人工验收通过（`32d0f26`）后核查发现基线目录仍为 P3-0（`7395a9e`）产物——对旧基线运行 `verifyJarEquivalence` 在 mc261/mc262 失败，差异恰为 §4.3 白名单（11 删 11 增命令类更名）。按 ratchet 约定以当前 HEAD 全量构建重建基线并自检：`verifyJarEquivalence` 11/11 通过。本轮起等价判定基线 = `32d0f26` 产物。

### 5.2 任务定义（根 build.gradle，与既有验证任务并列注册并挂接 dependsOn）

针对全部 11 平台逐项断言（数据源：per-version `mixin_config` / `java_release` 键 + jar 内产物 + 平台合并源码集 `sourceSets.main.java.srcDirs`）：

| # | 断言 | 防护目标 |
|---|---|---|
| 1 | `src/main/resources/<mixin_config>` 存在、jar 内打包、JSON 合法 | 配置缺失 / 漏打包 |
| 2 | `mixins` + `client` 每个条目在 jar 内存在对应 `mixins/<条目>.class` | 悬空条目（验收清单 §3.5 前半） |
| 3 | 平台合并源码集 mixins 包全部类恰为条目并集（双向） | 漏注条目（§3.5 后半；mixins 包只允许注册进 json 的 mixin 类） |
| 4 | `package` == 项目固定包、`required` == true、`compatibilityLevel` == `JAVA_<java_release>`、`injectors.defaultRequire` == 1 | 项目级不变量（基线 §1.2） |
| 5 | 条目无重复、`mixins` 与 `client` 无交集 | 配置书写错误 |

明确边界：不合并 json、不模板生成、不改 json 内容、不移动 Mixin 文件、不校验 client/main 归属数组的语义正确性（静态不可判定，由 L1-5 游戏内加载兜底）。

断言 ③（mixin package Java 类 ↔ mixin json 条目双向一致）的设计约束：

- **当前成立前提**：mixins 包内全部 Java 类型均作为 Mixin 注册进 json，不存在 mixin helper / utility 类型（P3-3 实施前预检 ×11 平台已确认该前提成立）。
- **未来演进触发**：若架构演进引入 mixin helper、accessor support class、annotation wrapper 或其他非注册辅助类，则"包内全部类必须注册"的严格断言不再成立，需重新评估断言范围。候选调整方向：按注解扫描（仅 `@Mixin` / `@Accessor` 标注类型参与比对）、按命名规则过滤、按目录划分检查范围（mixins 包内再分 `impl` / `support` 子包）。
- 调整属独立的后续变更：需同步修改任务实现与本节说明，不得在引入辅助类的同一次改动中静默放松断言。

### 5.3 自动化验证（全部通过，2026-09-04）

| 验证项 | 方法 | 结果 |
|---|---|---|
| 实施前现状预检 | bash 三集合比对（json 条目 ↔ 源码集 mixin 类 ↔ jar 内 class）×11 平台 | 全部一致，无历史问题 |
| 任务全绿 | `.\gradlew.bat verifyMixinConfigs` | 11/11（条目数与基线 §1.2 表逐平台一致：65+2 / 62+2 / 62+1 / 64+1 / 63+1） |
| 变异 A（悬空） | 向 mc1211 json 临时注入 `NoSuchDanglingMixinEntry` | 按预期 FAIL：`dangling mixin entries ... [NoSuchDanglingMixinEntry]` |
| 变异 B（漏注） | 从 mc261 json 临时删除 `AmethystNaturalGrowthMixin` 条目 | 按预期 FAIL：`mixin classes in source set but missing from ... [AmethystNaturalGrowthMixin]` |
| 变异 C（不变量） | mc262 json `compatibilityLevel` 改为 JAVA_21 | 按预期 FAIL：`compatibilityLevel JAVA_21 != JAVA_25` |
| 全量回归 | `build verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs :common:test verifyJarEquivalence`（P3-2 后基线） | 全绿，等价 11/11（任务为纯增量，产物零变化） |
| 工作区卫生 | `git diff --check` / `git diff --cached --check` | 通过 |
| CI 接入（P3-3 收尾） | `.github/workflows/build.yml` 命令串追加 `verifyMixinConfigs`（原有任务与顺序不变；`verifyJarEquivalence` 依赖仓库外基线，不纳入 CI） | 本地等价命令验证通过（§5.3 全量回归行） |

### 5.4 变异测试的环境伪影记录

变异还原使用 `git checkout --` 时，`core.autocrlf=true` 将 mc1211 / mc261 / mc262 三份 mixin json 的工作区行尾 smudge 为 CRLF（原状态 LF，`git ls-files --eol` 显示 `i/lf w/crlf`），重打包后导致 `verifyJarEquivalence` 对 jar 内 json 字节比对失败——与 Phase 2 记录（refactor-phase2-verification.md §3 伪影 1）同源。处置：`git show HEAD:<path> > <path>` 直接以 index 原始字节重写（`checkout --` 对 clean 后内容一致的文件是 no-op，不可用），恢复 `w/lf` 后全量验证通过。结论：凡变异测试触碰资源文件，还原必须绕过 smudge。

### 5.5 回滚

revert 本步单 commit 即可（仅根 build.gradle 增量 + 本文档）。

## 6. P3-4：Bridge 与空档清理

### 6.1 删除内容与理由

| 对象 | 内容 | 删除理由 |
|---|---|---|
| Bridge ×11 | `versions/platform-*/.../bridge/Mc<ver>Bridge.java`（每平台 1 个，仅含 `platformId()` 桩方法与私有构造器） | 历史迁移遗留死代码；基线 §3.3"薄桥接模式已覆盖残余差异"的描述与实测不符（实测为纯桩、零引用），经人工确认删除 |
| 空档 ×2 | `versions/shared/mc1211-1219/`、`versions/shared/mc12110-12111/`（仅空目录骨架） | 0 文件、无任何 `shared_tiers` / `extra_resource_dirs` 引用，档位碎片化的纯残留 |

### 6.2 删除前检查结果（全部通过）

Bridge（对全部 11 个类逐一执行）：

| 检查项 | 方法 | 结果 |
|---|---|---|
| Java 调用 / import | 全仓 grep `Bridge`（*.java，排除 build/bin） | 仅 11 个类自身定义匹配 |
| 包名引用 | grep `carpeticeaddition.bridge`（定义文件外） | 0 处 |
| 字符串 / 反射 / ServiceLoader | grep 类名与包名于全部文本文件（json/gradle/properties/yml/cmd/bat/toml/cfg） | 0 处（不存在任何字符串形态引用，反射与 ServiceLoader 无线索） |
| mixin json | 11 份 json 条目扫描 | 0 条 |
| resource / META-INF | resources 目录 | 无相关文件 |
| Gradle sourceSet / 构建引用 | settings.gradle / build.gradle / common.gradle / 全部 gradle.properties | 0 处 |

空档：

| 检查项 | 结果 |
|---|---|
| 文件（tracked / untracked） | 0 / 0 |
| `shared_tiers` / `extra_resource_dirs` 引用 | 11 平台均无 |
| 对平台 sourceSet 顺序的影响 | 无（不在任何叠加清单内，删除后全平台构建不变式保持） |

### 6.3 jar 条目差异白名单（对 32d0f26 基线，11 平台逐平台核对）

每平台恰好消失 2 个条目，零新增（11 平台合计新增条目数 = 0）：

```
com/ice2974/carpeticeaddition/bridge/Mc<ver>Bridge.class   （每平台 1 个）
com/ice2974/carpeticeaddition/bridge/                      （空目录条目，随类删除消失）
```

无功能 class、mixin class、fabric.mod.json、资源条目变化。

### 6.4 自动化验证（全部通过，2026-09-04）

| 验证项 | 结果 |
|---|---|
| `build verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs :common:test`（Bridge 删除后 + 空档删除后各跑一次） | 11 平台全绿 |
| jar 条目差异分析 | == §6.3 白名单 |
| `git diff --check` / `git diff --cached --check` | 通过 |

### 6.5 P3-4 阶段基线（新目录，不覆盖历史基线）

| 项 | 值 |
|---|---|
| 目录 | `D:\Project\Carpet-Ice-Addition-P3-baseline-P3-4`（仓库外） |
| 来源 | `main` @ `d904d6f`（Bridge 删除 commit）+ 空档工作区删除，工作区干净 |
| 内容 | 11 平台 22 个 jar（主包 + sources） |
| 自检 | `verifyJarEquivalence -PbaselineDir=<该目录>`：11/11 通过 |
| 历史基线处置 | `Carpet-Ice-Addition-P3-baseline`（32d0f26）保留不动，作为 P3-4 差异分析的对照 |

### 6.6 空档删除无独立 commit 的原因

两个空档目录 0 文件、git 完全不跟踪（`git ls-files` 为空）；git 无法记录空目录的存在与删除，因此"删除空档"只能是工作区操作（已执行，`versions/shared/` 现为 15 个有效档位），不产生 commit。fresh clone 的检出结果与本操作后的工作区天然一致，无回滚需求。

### 6.7 回滚

revert `d904d6f` 即可恢复 11 个 Bridge 类（空档无需回滚，见 §6.6）。

## 7. P3-5：收尾验收与状态冻结（2026-09-05）

本轮零源码变更（仅文档），不合并、不删除、不引入 preprocess / `#if` / mappings 变化。

### 7.1 Level 3 自动化验收结果（全部通过）

| 验收清单条目 | 方法 | 结果 |
|---|---|---|
| 全平台构建 + common 单测 | `build verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs :common:test` | 11 平台全绿 |
| 版本矩阵（11 平台 jar 命名） | 当前产物清单 vs 基线 §3 表 | 11 个 jar 文件名逐字符一致（mc1.21-1.21.1 … mc26.2） |
| fabric.mod.json 语义 | `verifyFabricModJson`（含 mixins 引用、loader 依赖断言） | 11/11 |
| mixin 配置完整性（§3.5） | `verifyMixinConfigs`（json ↔ class ↔ 源码集双向） | 11/11（65+2 / 62+2 / 62+1×4 / 64+1 / 63+1×2） |
| 规则注册矩阵（§3.1） | 源码 @Rule 静态核算：common 34 + LowVersion 2（仅 mc1211）+ HighVersion 1（mc12111/261/262）+ 双胞胎档 4（FluidSettings 2 + EndPlatform 1 + CraftableCoralBlocks 1） | mc1211=40、mc1213–mc12110=38（×7）、mc12111/mc261/mc262=39（×3），与 §3.1 矩阵一致 |
| 命令注册入口 | 四种入口形态（mc1211 / mc1213-12110 档 / mc12111 / mc26x 档）`registerCommands` 均调用 `KillItemCommand.register` + `MachineStatusCommand.register` | 一致 |
| 命令权限门 | `CommandHelper.canUseCommand(source, commandKillItem / commandMachineStatus)` 存在于各实现 | 权限逻辑未变化 |
| 资源包（§3.4） | `verifyCraftableCoralBlocksJars`（pack_format × 10 配方） | 11/11 |
| 翻译完整性（§3.4） | lang 键计数 vs 基线 §6.6 | zh_cn=192、en_us=151，与基线一致（Phase 3 未触碰语言文件；logger 10 键仅存于硬编码 Map 维持现状基线） |
| 产物等价 | `verifyJarEquivalence` 对 P3-4 基线（`d904d6f`） | 11/11 零差异（当前 HEAD 与 P3-4 基线仅差文档提交） |

### 7.2 Phase 3 最终统计（实测，7395a9e → 当前 HEAD）

| 指标 | P3 前 | P3 后 | 变化来源 |
|---|---|---|---|
| `versions/` 物理 java 文件 | 276 | **257**（-19） | P3-1 -3、P3-2 -5、P3-4 -11 |
| 唯一类名 | 164 | **147**（-17） | P3-2 消除 6 个 `Mc26X` 后缀名、P3-4 删 11 个 Bridge |
| 多副本类名 | 72 | **71** | `ServerGamePacketListenerImplMachineStatusRollbackWarningMixin` 收敛为 mc26x 单份 |
| 冗余物理副本 | 112 | **110** | P3-1 -3；P3-2 消重复 -2 但三个共享命令名并入使副本 +3、净 +1 |
| shared 档位 | 17（含 2 空档） | **15** | 空档删除 |
| mc26x 档文件 | 90 | **95** | 承载三命令 + 回档 Mixin + 入口类 |
| mc1213-1214 档文件 | 3 | **6** | 承载三份注释等价合并类 |
| 平台自有 java（部分） | mc261/mc262 各 7 | **各 1**（EndPortal 覆盖） | P3-2 + P3-4 |
| mixin json | 11 份 | **11 份**（保留现状） | 新增 `verifyMixinConfigs` 防线并接入 CI |

余下 ~110 份冗余副本为结构性分叉（Yarn↔Mojmap 双胞胎 + 1.21.x API 边界覆盖），Phase 3 约束下不可合并，归 Phase 4 评估（§7.4）。

### 7.3 Phase 3 总状态

| 步骤 | implementation | acceptance |
|---|---|---|
| P3-0 基线 | complete | —（基础设施） |
| P3-1 注释等价副本合并 | complete | accepted（人工 L2 + L1-5） |
| P3-2 26.x 命名链收敛 | complete | accepted（人工 L2 + L1-5） |
| P3-3 verifyMixinConfigs | complete | accepted（自动化验收 + 变异自测） |
| P3-4 Bridge ×11 + 空档 ×2 清理 | complete | accepted（人工 L1-5 冒烟） |
| P3-5 收尾 | complete | **accepted（2026-09-05）**（Level 3 游戏内回归，§8） |

### 7.4 Phase 4 / 后续评估项（不在 Phase 3 实施）

1. **mappings 统一（Mojmap 或其他单一命名空间）**：消除 ~45 对 Yarn↔Mojmap 双胞胎的唯一途径；约 150 个 Yarn 源文件重写 + 全量 Level 3 回归；触发判据维持目标文档 §6（双胞胎漏改事故 ≥2 次或 preprocess 收益论证需要）。
2. **preprocess / `#if MC`**：前置 = mappings 统一 + `com.github.Fallen-Breath:preprocessor`（JitPack commit 锁定）供应链确认 + `THIRD_PARTY_NOTICES.md` 登记。适合宏化的已测定清单：1 行 mappings 差异家族（Yarn 侧 KillItemCommand 两档、VillagerDimension121、EndPortal 26.x）与少量行差异家族（PvpRuleHelper 等）；量化上限建议维持目标文档 R2（单处 ≤10 行、不改注入 descriptor）。
3. **root src + per-version 覆盖目录收敛**：依赖 preprocess 生成源码树（Gradle sourceSet 无同 FQCN 遮蔽能力，P3 规划期已论证）；无 preprocess 时该形态文件数不降反升，不建议单独实施。
4. **`EndPortalBlockCustomEndPlatformPositionMixin` 26.x 覆盖形态**：mc261/mc262 间 1 行真实 API 差异，可评估迁 mc26x + 平台覆盖（净文件数不变，仅形态统一），收益小，建议随 Phase 4 一并处理。
5. **翻译三源治理**（JSON ×2 + 硬编码 Map）：现状基线缺陷（logger 10 键缺失于 JSON）维持原样，出现缺键事故时立项。
6. **mc1211 平台资源空目录残留**（`data/` 等，Phase 2 §3 已记录）：环境事实，可选清理。

## 8. 人工项清单

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

P3-3 无人工项（纯增量校验，验收清单 §3.5 的 mixin 完整性自本步起由 `verifyMixinConfigs` 自动化覆盖；client 归属数组语义仍由 L1-5 加载兜底）。

### P3-4（已执行，2026-09-04）

| 项 | 内容 | 执行人 | 日期 | 结果 |
|---|---|---|---|---|
| L1-5 冒烟 | 任选 1 平台（建议 mc262：plain 形态 + 删除前 Bridge 与入口同目录层级）dev 实例启动无 mixin/注册错误，`/carpet` 可用——确认 Bridge 删除不影响 mod 加载 | Ice2974 | 2026.9.4 | 通过 |

### P3-5（已执行，2026-09-05）

| 项 | 内容 | 执行人 | 日期 | 结果 |
|---|---|---|---|---|
| Level 3 完整回归 | 验收清单 §3 人工部分：全部 11 平台 `/carpet list` 条目数对照 §3.1 矩阵（40 / 38×7 / 39×3）、§3.3 版本特定分支（LowVersion / HighVersion / killitem 三实现 / machineStatus 三实现 / itemFrame 五分叉 / client mixin 归属）、§3.2 规则逐条翻译显示、§3.4 资源与翻译行为冒烟 | Ice2974 | 2026.9.5 | 通过 |

说明：§3.5 mixin 完整性已由 `verifyMixinConfigs` 自动化覆盖（P3-3）；§3.4 的 `:common:test` 单测与 pack_format 已自动化。人工项聚焦游戏内行为回归。Phase 3 各步骤的定向 L2（2-2/2-3、2-7/2-8/2-12）已在 P3-1/P3-2 验收中覆盖，Level 3 为全量收尾。

## 9. Phase 3 强制约束遵从记录

1. 每步骤先建新基线：P3-0 已执行（§2）；P3-2 条目变更步骤已在人工验收后以 `32d0f26` 重建（§5.1）；P3-4 条目变更步骤的阶段基线（`d904d6f`，§6.5）按用户指令于自动化验证通过后建立（独立目录，未覆盖历史基线），人工冒烟随后补验通过（§7）。
2. Phase 1/2 基线仅历史参考：P3-1 起全部等价判定改用 Phase 3 基线。
3. 源码移动三要件（sourceSet 顺序等价 / jar 集合确认 / mixin 注册路径）：P3-1 见 §3.2，P3-2 见 §4.2。
4. 禁止为减文件数合并结构不同 Mixin、用宏替代覆盖、改规则/命令/logger 行为：P3-2 仅收敛非命名差异为 0 行的重复链与入口类（2 行注释差异）；`EndPortalBlockCustomEndPlatformPositionMixin` 存在 1 行真实 API 差异，保留平台覆盖未合并。
5. 删除独立 commit：结构性删除（Bridge ×11、空档 ×2 等 P3-4 内容）将独立成 commit；P3-1/P3-2 的副本删除与迁移不可分割（§3.1 / §4.1）。
6. mixin json 保留现状：P3-3 只建立校验防线，不合并、不生成、不改任何 json 内容；实施前预检确认当前 11 平台 json 与源码集 / 产物完全一致（无历史问题需处置）。
7. 删除独立 commit：P3-4 的 Bridge ×11 删除为独立 commit（`d904d6f`）；空档 ×2 因 git 不跟踪空目录无法成 commit（§6.6），以工作区删除 + 本文档记录替代。
8. P3-5 不做结构变更：本轮零源码修改；发现的结构优化机会全部记入 §7.4（Phase 4 / 后续评估项），未在本轮实施。

## 10. Phase 3 后独立修复记录

### 10.1 资源包 metadata 兼容修复（2026-09-05，bugfix，非 P3 步骤）

- **问题**：pack_format > 81 的平台上客户端启动必现 ERROR（每次 2 条）：`Couldn't load carpet-ice-addition:craftable_coral_blocks pack metadata: Pack declares support for version newer than 81, but is missing mandatory fields min_format and max_format`（1.21.11 实例日志自 2026-09-01 起实测，非 Phase 3 回归——pack.mcmeta 在 Phase 1–3 全程字节不变）。
- **变更**：为 5 个受影响平台（mc1219 / mc12110 = 88、mc12111 = 94、mc261 = 101、mc262 = 107）的 `resourcepacks/craftable_coral_blocks/pack.mcmeta` 增加 `"min_format":<pack_format>,"max_format":<pack_format>`，保留原 `pack_format` 字段；≤81 的 6 个平台未动。
- **验证**：全平台构建 + 三验证任务 + common 单测全绿；`verifyJarEquivalence` 对 P3-4 基线 11/11 通过（该任务对 pack.mcmeta 仅比对解析后 pack_format 值，属已记录能力边界）；另做 jar 内字节比对——5 个变更平台 diff 恰为新增字段、6 个未动平台字节一致，条目清单无变化。
- **人工验证（已通过，2026-09-05）**：受影响平台启动确认 metadata ERROR 消失，验收清单 2-1（craftableCoralBlocks 开关 + 10 配方 + 冲突锁定）通过（Ice2974 执行）。
- **基线影响**：5 平台 jar 内 pack.mcmeta 字节变化，人工验证通过后已按 ratchet 以 `af07f26` 建立最终阶段基线（§10.2）；P3-4 基线（`d904d6f`）保留不动。

### 10.2 Phase 3 最终基线（状态冻结）

| 项 | 值 |
|---|---|
| 目录 | `D:\Project\Carpet-Ice-Addition-P3-baseline-final`（仓库外） |
| 来源 | `main` @ `af07f26`（含 §10.1 资源包 metadata 修复），工作区干净 |
| 内容 | 11 平台 22 个 jar（主包 + sources） |
| 自检 | `verifyJarEquivalence -PbaselineDir=<该目录>`：11/11 通过 |
| 历史基线 | `…-P3-baseline`（`32d0f26`）与 `…-P3-baseline-P3-4`（`d904d6f`）均保留不动，作为各阶段差异分析对照 |

Phase 3 至此全部完成（P3-0 ~ P3-5 implementation + acceptance 均 accepted）；后续结构优化见 §7.4（Phase 4 待评估项）。
