# Phase 4 验证记录：Mojmap mappings 统一 + 因 mappings 产生的双胞胎消除 + 定向去重

> 关联文档：[refactor-target-architecture.md](refactor-target-architecture.md)（目标与路线，Phase 4 执行结果已回填）、[refactor-phase3-verification.md](refactor-phase3-verification.md)（上一阶段，基线 `af07f26`）、[refactor-acceptance-checklist.md](refactor-acceptance-checklist.md)（验收标准）。
>
> 最终目标（2026-09-05 锁定）：**完整迁移到 Fallen_breath 风格源码架构**（根 src 主源码树 + per-version override + preprocess 版本图 + 单一 Mojmap）。Phase 4 完成其中「单一 Mojmap」部分，**不等于完整迁移完成**；后续见 Phase 5（本文 §9）。

## 1. 范围与结论

Phase 4 实施内容（与批准计划一致）：

| 项 | 内容 | 状态 |
|---|---|---|
| P4-0 | 基线冻结与测定、双胞胎权威清单、migrateMappings 评估 | implementation complete |
| P4-1 | 构建层过渡分支 + mc1214 试点切换 + intermediary 语义等价防线 + 变异测试 | implementation complete，acceptance accepted（L1-5，2026-09-05） |
| P4-2 | 其余 8 个 1.21.x 平台逐个切换（每平台独立绿 commit）+ 收尾删除 Yarn 档位与过渡分支 | implementation complete，acceptance accepted（11 平台 L1-5，2026-09-05） |
| P4-3 | 零差异双胞胎合并（含 mc26x 受限合并）、入口类错误上报逐字抽取、命令家族版本边界助手合并 | implementation complete，acceptance 待 Level 3 |
| P4-4 | 最终基线、静态核算、本文档与 docs/ 修正、target 文档最终目标/Phase 5 重写 | implementation complete，acceptance 待 Level 3 |

净效果：**11 个平台全部统一 officialMojangMappings（layered），Yarn 依赖彻底移除**；1.21.x 侧由 18 个 mojmap 档位承载（14 个 1:1 镜像原 Yarn 拓扑 + 4 个 Mojmap 名漂移分叉档）；mc26x 95→82 文件（13 个零差异双胞胎移入全平台档）。物理 java 257→267（Mojmap 名漂移导致的档位拆分略增副本，换取单一命名空间），唯一类名 147→149（新增 2 个版本边界助手类；`FeatureCompatibilityReporter` 在 common，不计入 versions/）。

## 2. 提交清单（分支 `phase4-mojmap`，13 commit，每个正式 commit 均全绿）

| commit | 步骤 | 摘要 |
|---|---|---|
| `e480181` | P4-1 | mc1214 试点：migrateMappings 迁移 98 文件、common.gradle 过渡分支（yarn_mappings 键存在与否）、intermediary 语义等价防线、变异测试 |
| `b37d478` | P4-2a | mc1213 切换（98/98 与试点一致，档位共享改名） |
| `6d6e5ac` | P4-2b | mc1211 切换（26 平台自有文件原地迁移；档位按 Yarn 拓扑拆分防同 FQCN 重复） |
| `bc5ce83` | P4-2c(1) | mc1215 切换（档位策略修正为严格 1:1 镜像 Yarn） |
| `33bbb00` | P4-2c(2) | mc1216 切换（TrialSpawnerData→TrialSpawnerStateData 首例内容分叉） |
| `c5ad7a9` | P4-2c(3) | mc1218 切换 |
| `205320b` | P4-2d(1) | mc1219 切换（KillItemCommand getCommandSenderWorld()→level() 分叉） |
| `b7c3758` | P4-2d(2) | mc12110 切换 |
| `6af8a57` | P4-2d(3) | mc12111 切换（24 文件 Mojmap 名漂移分叉，旧变体下沉 …-12110 系档） |
| `3f4f647` | P4-2e | 收尾：删 14 个 Yarn 档 java、common.gradle 终态无条件 officialMojangMappings |
| `8bad710` | P4-3b | 零差异双胞胎合并（纯档位移动，jar 不变；新建全平台档 mojmap-unified） |
| `c20fc8e` | P4-3c | 入口类错误上报逐字抽取至 common `FeatureCompatibilityReporter` |
| `d080b9d` | P4-3d | 命令家族版本边界助手合并（PlayerWorlds / MachineStatusTextEvents，含矩阵证明） |

P4-0 无代码变更（基线与清单在仓库外 `D:\Project\Carpet-Ice-Addition-P4-baseline\`，含 P4-0-report.md 与迁移驱动脚本）。

## 3. 基线与防线

### 3.1 基线 ladder

| 基线 | 目录 | 用途 |
|---|---|---|
| P4-0 基线 | `D:\Project\Carpet-Ice-Addition-P4-baseline` | Phase 4 动工前 11 平台 22 jar（fa2c1eb，与 af07f26 产物等价已交叉验证）；P4-1~P4-3b 全程对照 |
| Phase 4 最终基线 | `D:\Project\Carpet-Ice-Addition-P4-baseline-final` | P4-3d 后快照，自检 11/11；后续 Phase 5 对照起点 |

### 3.2 intermediary 语义等价防线（verifyJarEquivalence 扩展，P4-1）

**实证前提**：本项目 loom 构建不产出 refmap——注解目标在 remapJar 时直接重映射进 class 常量池（jar 无 refmap 文件、mixin json 无 refmap 指针，试点实测）。因此命名空间迁移的语义验证对象是 class 文件内的 `(class|method|field)_NNNN` intermediary 引用集合：同名 class 在两侧 jar 中集合必须完全一致，覆盖 mixin 注解目标（@Mixin/@Inject/@At/@Shadow/@Accessor 字符串）与全部编译期 MC 成员调用。**不做任何 blanket 忽略。**

- 变异测试：回退 BeaconBlockEntityMixin 的 getLightBlock 修复后，防线精确报出 `method_26193` 缺失（预期 FAIL ✓）。
- 实战捕获：迁移漏改 3 处（mc1214×1、mc1211×1，含双参重载 variant）＋ P4-2d 档位清单缺漏 1 处（由 verifyMixinConfigs dangling 断言捕获）。

### 3.3 与 P4-0 基线的预期差异清单（P4-3c/3d 的 jar 结构变化）

- 全部 11 平台 jar 新增 `com/ice2974/carpeticeaddition/FeatureCompatibilityReporter.class`（空 intermediary 引用集）。
- 1.21.1-1.21.10 平台新增 `PlayerWorlds.class`、`MachineStatusTextEvents.class`；KillItemCommand / MachineStatusCommand / MachineStatusRollbackWarningHandler 的部分 intermediary 引用精确迁移至助手类（mc1211：method_5770→PlayerWorlds，class_2559/class_5247/field_11750/field_24342→MachineStatusTextEvents；mc1216：method_51469、class_10609/class_10613 同理）。
- 其余维度（zip 条目清单除上述新增外、fabric.mod.json 语义、mixin json 字节、pack_format）零差异。

## 4. 关键技术发现

1. **loom 1.13.6 `migrateMappings` 可直接用于 Yarn→Mojmap**：`--mappings "net.minecraft:mappings:<MC版本>"`（字节码核实解析为 officialMojangMappings）；import/泛型/@Mixin 目标/`method=` 完整 descriptor 字符串均正确重映射，javadoc 保留。限制：只处理平台默认 `src/main/java`（档位源码需暂存暂迁，方法论见 P4-0-report.md）；输出 `<platform>/remappedSrc` 严禁入库。
2. **两类系统性缺口**（每平台预修 + 语义防线兜底）：
   - 子类 override 方法名不随父类改名传播（`MultiTickTask.shouldRun/run` → `Behavior.checkExtraStartConditions/start`，逐版本 tiny 实证）；
   - 个别 `@At target` 字符串不重映射且正确名逐版本不同（`getOpacity` → `getLightBlock`，1.21.1 为双参重载、1.21.3+ 为无参、26.x 为 `getLightDampening`）。
3. **Mojmap 名漂移（Yarn 名稳定）造成 3 处内容分叉**（档位据此拆分，全部实证于对应版本 layered tiny）：`TrialSpawnerData→TrialSpawnerStateData`（1.21.6）、`Entity.getCommandSenderWorld()→level()`（1.21.9）、1.21.11 批量改名（24 文件，villager/AI 族）。
4. **档位策略**：合并宽档会破坏平台可见性（mc1215 实证：合并档使旧 ClickEvent 变体错误可见，重复类 + HoverEvent 抽象化编译错误），正确形态为 **1:1 镜像 Yarn 档位拓扑**，内容级归一（零差异）再以纯移动合并。

## 5. P4-3 去重明细

| 组 | 处置 |
|---|---|
| 12 个 `mojmap-mc121x~mc26x:0` + `RuleMessageThrottle` 三方 `:0` | 合入新建全平台档 `mojmap-unified`（13 文件，11 平台 include）——Phase 5 根 src 的雏形 |
| `KillItemCommand`（mojmap-mc1216-1218 ~ mojmap-mc1219-12110 `:0`） | 合入 `mojmap-mc1216-12110` |
| `VillagerDimension121` / `VillagerDeathSide121`（1211-1218 ~ 1219-12110 `:0`） | 合入 `mojmap-mc1211-12110` |
| `SafeScaffoldingBreakMixin`（1211-1218 ~ 1219-12111 `:0`） | 合入 `mojmap-mc121x` |
| 入口类错误上报（4 份 × 约 120 行 if-else） | 逐字抽取至 common `FeatureCompatibilityReporter`（四份抽取产物字节一致；判断顺序/异常优先级/输出/fallback 语义不变，未改写 Map）；入口类保留同名静态委托，调用点零改动 |
| `KillItemCommand`（1014 行 ×2，1 行差异） | 统一入 `mojmap-mc1211-12110`，差异行提取 `PlayerWorlds.serverLevel`（mojmap-mc1211-12115 档 `getCommandSenderWorld()` / mojmap-mc1216-12110 档 `level()`）；1.21.11 与 26.x 保持自有整份 |
| `MachineStatusCommand` / `MachineStatusRollbackWarningHandler`（1.21.5 ClickEvent API 边界） | 统一入 `mojmap-mc1211-12110`，构造提取 `MachineStatusTextEvents.runCommand/showText`（mojmap-mc1211-12114 档 Action 枚举形态 / mojmap-mc1215-12110 档 RunCommand/ShowText 形态）；RollbackWarningHandler 的 1.21.11 副本入 mojmap-mc12111（自包含） |

**shared_tiers 矩阵证明（约束 7）**——每平台恰一份同 FQCN 助手：

| 平台 | PlayerWorlds | MachineStatusTextEvents |
|---|---|---|
| mc1211/1213/1214/1215 | mojmap-mc1211-1215 | mojmap-mc1211-1214（1211/1213/1214）/ mojmap-mc1215-12110（1215） |
| mc1216/1218/1219/12110 | mojmap-mc1216-12110 | mojmap-mc1215-12110 |
| mc12111 / mc261 / mc262 | 不引用（自有整份实现） | 不引用（自有整份实现） |

javac 重复类编译错误为机械兜底；全周期未触发。

**保留不合并（diff 实证为真实版本差异）**：mc1211 平台 19 个同名类（2-19 行 API 差异）、`EndPortalBlockCustomEndPlatformPositionMixin` 26.1/26.2（1 行真实差异；任何重排均使文件数不降反升）、`DisableIllegalTextCharacterCheckMixin` 等「1.21.9+ ≡ 26.x 但 ≠ 1.21.1-1.21.8」无自然档位可归一的组（留待 Phase 5 根 src + override 形态自然解决）、其余 2-500+ 行差异组。

## 6. 静态核算（P4-4a）

| 项 | 结果 | 对照 |
|---|---|---|
| 规则注册矩阵 | mc1211=40、mc1213–mc12110=38（×7）、mc12111/mc261/mc262=39（×3） | 验收清单 §3.1 一致 ✓ |
| 翻译键计数 | zh_cn=192、en_us=151 | 基线一致 ✓ |
| mixin 配置完整性 | verifyMixinConfigs 11/11（条目数与 Phase 3 相同） | ✓ |
| 全量构建 | build + verifyCraftableCoralBlocksJars + verifyFabricModJson + verifyMixinConfigs + :common:test 全绿 | ✓ |
| 最终基线自检 | verifyJarEquivalence 11/11 | ✓ |

## 7. 人工项清单

| 项 | 内容 | 执行人 | 日期 | 结果 |
|---|---|---|---|---|
| L1-5 冒烟（P4-1/P4-2） | 11 平台 dev 启动无 mixin/注册错误、`/carpet` 可用（Mojmap 切换后） | Ice2974 | 2026.9.5 | 通过 |
| Level 3 完整回归（P4-3/P4-4） | 全 11 平台按验收清单 §3：/carpet list 矩阵、§3.3 版本特定分支（重点：/killitem 全子命令 2-7、/machineStatus 全子命令 2-8、machineStatusRollbackWarning 2-12——命令家族结构重构后）、§3.2 规则翻译显示、§3.4 资源与翻译 | Ice2974 | 待执行 | 待执行 |

## 8. 已知限制

- `verifyJarEquivalence` 的 intermediary 比较对「引用从一个类迁移到新助手类」的结构性重构报差异（按设计），此类变更以最终基线 ratchet 承接（见 §3.1/§3.3）。
- Yarn 档位目录名仍出现在部分 properties 注释中（历史出处说明，非功能引用）；`extra_resource_dirs=mc1213-12111` 指向的目录现为纯资源档（java 已删）。
- 1.21.11 平台的 mojmap 档（mojmap-mc12111）承载 24+2 文件，为最大单版本档——Phase 5 根 src + per-version override 收敛的主要对象。

## 9. Phase 5 展望（独立立项，非 Phase 4 承诺）

目标：**preprocess 版本图 + 根 src 主源码树 + per-version override 收敛**，完成 Fallen_breath 风格源码架构的完整迁移。

- 前置门禁：`com.github.Fallen-Breath:preprocessor`（JitPack commit 锁定）供应链确认 + `THIRD_PARTY_NOTICES.md` 登记（Fallen-Breath/preprocessor、TIS 架构来源致谢）。
- **门禁不通过时，「完整迁移」整体状态标记 blocked 并等待人工决策，不视为可永久跳过的可选优化**（最终目标已锁定，见 target 文档修订）。
- 素材：`mojmap-unified` 档为根 src 雏形；1 行 / 3 行差异家族（P4-0-report 清单）为 `#if MC` 宏候选；结构性分叉保留 override。
