# Phase 8 验证记录：版本目录 / Gradle 项目 / preprocess 节点正名（mcXXXX → 实际 MC 版本）

> Phase 8 于 main 分支执行（基点 1a6787b，Phase 7 终态）。目标：将内部标识 `versions/platform-mcXXXX` / `:platform-mcXXXX` / preprocess 节点 `mcXXXX` 正名为实际 Minecraft 版本号（mc1211→1.21.1 … mc261→26.1.2、mc262→26.2），覆盖磁盘目录、Gradle 项目、preprocess 节点、mainProject、mapping 文件名、构建/验证路径、CI 路径与发布路径。**版本语义、图拓扑（11 节点 + 10 条边）、源码行为、依赖、发布 jar 命名与 Modrinth / GitHub Release 语义全部不变**。
>
> **验收状态：完成**（2026-09-06；P8-0 本地探针 + 结构验证 + CI 等价命令集全量 + verifyJarEquivalence 11/11 内容级等价 + 双树 resolver harness 全绿，见 §3；游戏内人工测试通过（2026-09-06 人工确认，见 §4）；GitHub Actions Build #56 通过（HEAD `6938e2e`，见 §3 / §4）。

## 0. 已确认约束（人工锁定，全程有效）

- 冻结项：依赖与数据模型全部键、preprocessor JitPack 全 SHA（`c5abb4fb…`）、mainProject 语义（core = 1.21.11，原 platform-mc12111）、版本图拓扑（11 节点 / 10 条链边 / 1.21.11↔26.1.2 remap↔plain 边）、`verifyJarEquivalence` fail-closed 证明能力、P6-baseline-final 基线（永久只读，legacy 目录名布局）、`org.gradle.parallel=false`、loom 家族闭环（`loom_plugin` 完整 `id:version` 选择 + P7-R1 坐标防漂移断言）。
- 禁止项（Phase 9+ 范围，本相一律不做）：common 模块迁移 / 删除、mixin json 统一、`versions/shared` 删除、`extra_resource_dirs` 语义变更、任何依赖 / Gradle / Loom / JDK / preprocessor SHA 升级、发布命名变更、`versions/class-rename-mapping.txt` 47 条 mapping 语义变更。
- 保留的 mcXXXX 标识（非本项目身份，明确不属正名范围）：mixin 配置命名 `carpet-ice-addition-mcXXXX.mixins.json`（AGENTS.md 固定标识，jar 侧平台码的唯一载体）、`versions/shared/mc1213-12111` 资源档目录名、各平台 `gradle.properties` 中「迁自 / 原 platform-mcXXXX」迁移历史注释、`build.gradle` `legacyBaselineDirNames` 映射值、`publish.yml` 布局解析层的 legacy 兼容分支、历史 docs 记录。
- Phase 8 验证结论表述边界：**不声称 sources jar 与 P6-baseline-final 字节等价**（sources jar 的 55 对差异继续登记为预期 comment-only 差异，见 §3）；Java 注释实名修改单独登记为「预期的 comment-only sources 差异」（55 对，全部位于注释段，见 §3）；runtime JAR 与 P6-baseline-final 为 **11/11 内容级等价**（本 Phase 涉及的项目 class 均 byte-identical、普通资源 byte-identical、其余特殊资源按现有专项语义 invariant 验证），不声称整个 JAR / ZIP 的 raw bytes 或 SHA-256 完全一致。

## 1. 分步记录

| 步骤 | commit | 内容 |
|---|---|---|
| P8-0 | （无 commit） | 本地 scratch Gradle 探针（`.tmp/p8-name-probe`，验证后删除）：以仓库 Gradle Wrapper（9.2.1）include `:1.21.11` / `:1.21.10` / `:26.2` / `:26.1.2` 并重映射 projectDir，`gradlew projects` 确认带点项目名通过 NameValidator、`project(':<版本>')` 查找与 projectDir 解析正常——不凭理论下结论的环境级实证。 |
| P8-1 | 8e86219 | 原子正名提交（256 文件，全部 rename 追踪）：11 个平台目录 `git mv`（`versions/platform-mcXXXX` → `versions/<版本>`）；10 个 `versions/mapping-mcXXXX-mcYYYY.txt` → `mapping-<高版本>-<低版本>.txt`；`versions/mainProject` → `1.21.11`。settings.json 注册表 11 条目实名；settings.gradle include / projectDir / buildFileName 实名 + 磁盘断言升级为「`versions/` 目录集合恰为注册表条目目录 ∪ {shared}」双向 fail closed（断言过滤候选条目为目录本身）；build.gradle `createNode` / `link` 全实名（Groovy 变量 `mc12101…mc260200` → `v1_21_1…v26_2`，版本码整数不变）、`platformProjects` 路径实名、`ext.versionProjectNames` 注入、`projectsEvaluated` 断言改为「根直接子项目除去 `:common`」集合比较（正名后无前缀模式可匹配）、`verifyClassRenameMapping` 平台目录改由注册表派生（另加注册表目录存在性断言）、`verifyJarEquivalence` 经 `legacyBaselineDirNames`（键集与注册表防漂移断言）定位 P6 baseline 的 legacy 目录名并现势化过期 P5-baseline-final 文本；common.gradle platform-only 三重断言（根直接子项目 + 注册表成员 + `projectDir == versions/<项目名>`）+ `archivesName = archives_base_name-<项目名>`（平台唯一性保持）+ jar `from :common` 改惰性闭包（见 §2）；common/build.gradle 路径 → `versions/1.21.1/gradle.properties`、变量 `mc1211CarpetVersion` → `baselineCarpetVersion`；build-remap / build-plain 注释现势化；根 gradle.properties 注释现势化；11 份平台 gradle.properties 首行实名（迁移历史注释保留）；publish.yml 布局解析层（见 §2）；Java 注释实名 13 文件 13 行（comment-only）。 |
| P8-2 | f515270 | AGENTS.md 现势化（目录边界 / 固定标识 Archives Base Name 后缀表述 / 源码架构 / override 语义 / 版本注册表与构建配置）；docs/refactor-target-architecture.md §5.1 注册表条目注记关闭 + §6 新增 Phase 8 执行结果 + §8 深度表补 Phase 8 行；docs/refactor-acceptance-checklist.md 平台简写全量实名（`shared/mc1213-12111` 档位名保留）；新建本文档。 |
| 测试确认 | fb5c5df | 游戏内人工测试通过（2026-09-06 人工确认）：本文档验收状态与 §4 人工确认登记、target-architecture §6 Phase 8 验收补记。 |
| P8-R1 | 6938e2e | IDE 导入加固：settings.gradle `versions/` 目录枚举由 `listFiles({…} as FilenameFilter)` 闭包 SAM 转换改为 `listFiles().findAll { it.isDirectory() }`（Groovy 侧无歧义过滤，消除 IDE Gradle/Groovy 环境求值差异来源）。背景：VSCode（Java 扩展 / Buildship）导入时报「disk 集含 mapping-\*.txt / mainProject 等文件」——与实施中间坏态逐字一致（提交态 `new File(parent, name).isDirectory()` 修复版在 CLI 全绿），不排除 IDE 自带 Gradle 对 FilenameFilter 闭包求值差异，故彻底移除该模式；正反向验证：CLI `projects`/`build`/verify 全绿，负向测试（临时杂散目录 `versions/stray-drift-probe`）触发同一断言失败后恢复通过。 |
| P8-R2 | （本 commit） | ① settings.gradle 新增注册表重复版本 Settings 阶段显式 fail fast（`groupBy` 检测重复条目，错误信息列出重复项；此前目录一致性比较经 Set 化会静默去重）；负向验证：临时在 settings.json 制造重复版本 → Settings 阶段以「settings.json 版本注册表存在重复项：[1.21.11]」失败 → 恢复后 `projects` 正常。② 本文档事实口径现势化：分步记录补记真实短 SHA（P8-1 `8e86219`、P8-2 `f515270`、测试确认 `fb5c5df`、P8-R1 `6938e2e`）；GitHub Actions Build #56 通过记录（HEAD `6938e2e`，替换「commit 未推送 / Build #54」过时表述）；verifyJarEquivalence 收紧为「11/11 runtime JAR 内容级等价」口径（不声称 raw bytes / SHA-256 一致）；历史补发布兼容表述收紧（真实 workflow_dispatch 端到端演练仍未执行）；target-architecture §6 Phase 8 同步最小修正。 |

## 2. 关键实证与适配（P8 新增发现）

### 2.1 配置顺序由 preprocess 图驱动（行为学发现 + 惰性化适配）

- 正名前（HEAD 1a6787b，init 脚本打印 afterEvaluate 完成序）：root → `:common` → `:platform-mc12111`（core）→ 链上其余平台 → 26.x；`common.gradle` 中 `tasks.named('jar') { from(project(':common').sourceSets…) }` 的 config action 恰在 `:common` 已配置后执行。
- 正名后项目名带点，`:1.21.11`（core）先行、`:common` 落于平台之后，eager 解引用 `:common.sourceSets` 立即失败（`Could not get unknown property 'sourceSets' for project ':common'`）。
- 适配：`from` 改惰性闭包 `from { project(':common').sourceSets.main.output.classesDirs }`——执行期再解析，行为不变、不再依赖配置顺序；`common.gradle` 三重断言对 `versionProjectNames` 缺失给出显式错误。settings.gradle 的 `gradle.ext` 不承载该数据（根 build.gradle 注入 + 「根项目先于子项目配置完成」经 afterEvaluate 序实证）。

### 2.2 publish.yml 布局解析层（历史 tag 补发布兼容，唯一生产实现）

- 现状机制实证：dispatch 补发布 = default 分支 workflow + `refs/tags/<tag>` checkout（tag 树提供 settings.json / 平台目录），release 事件 = tag 所指 commit 的旧 workflow。因此发布路径必须同时理解 pre-P8 tag（legacy `mcXXXX`，目录 `versions/platform-mcXXXX`）与 P8+ tag（actual 实际版本，目录 `versions/<版本>`）。
- 实现：`resolve_platform_layout()`（marker `P8-LAYOUT-RESOLVER-BEGIN/END` 包裹的唯一生产实现）——JSON 结构校验（非空 string 数组、逐条匹配 legacy / actual 形态、无重复）、逐条目解析目录与 `mixin_config` → jar 侧平台码映射（`platform_dir` / `platform_entry`，**`platform_dir` 值为完整仓库相对路径含 `versions/` 前缀，全部 Bash / Python 消费点禁止再拼接 `versions/`**）、同树混合形态 / 未知形态 / 目录缺失 / 平台码冲突 / mixin_config 无法解析一律 fail closed。dispatch 资产循环经 `platform_entry[mcXXXX]` 回注册表条目再落 `platform_dir`；expand_script argv 传完整目录（python 端 `os.path.join(<目录>, "gradle.properties")`）；manifest 循环 `lib_dir` 取自 `platform_dir`。
- legacy 仅作为历史 tag 兼容路径，不重新成为当前 main 的标识形态。
- 口径边界：本层证明的是「支持 P8+ actual 版本注册表树 + 支持包含 legacy `mcXXXX` settings.json 注册表的旧树」，legacy / actual 两种布局已通过从生产 resolver 机械提取的静态 harness（§2.3）；真实 workflow_dispatch 端到端演练仍未执行，不据此声称所有 pre-P8 历史 Release 均已证明可补发布。

### 2.3 双树静态 harness（与生产实现同一份代码）

- harness（本地执行，不入库；方法收录于本文档）：`awk` 按 marker 从 publish.yml 机械提取 `resolve_platform_layout` 与 expand_script python，**不维护复制版逻辑**；本地补 `jq` 适配层（Windows jq.exe 输出 CRLF，harness 以 `tr -d '\r'` 包装并透传退出码；CI ubuntu 无此问题）。
- legacy 树：`git worktree add` tag `3.0.0`（已验证该 tag 树 settings.json 为 legacy mcXXXX 注册表）→ 分类 legacy 11/11、11 目录存在、平台码映射 11/11、game_versions 展开非空（mc1211→["1.21","1.21.1"]、mc12111→["1.21.11"]、mc261→["26.1","26.1.1","26.1.2"]、mc262→["26.2"]）。
- actual 树（P8 工作区）：分类 actual 11/11、目录 11/11、平台码映射 11/11（mc1211→1.21.1、mc262→26.2 等）。
- 负例 fail closed ×5：混合 legacy/actual 形态、未知条目形态、平台 gradle.properties 缺失、jar 侧平台码冲突、mixin_config 无法解析——全部拒绝且错误信息语义正确。
- 真实 workflow_dispatch 演练（tag 3.0.0）未执行：静态 harness 已覆盖解析层与展开层生产代码；演练会触发公开 workflow 运行，留待人工按需执行（见 §5）。

### 2.4 中间产物命名变化（计划修订项 #3 已确认的口径）

- `archivesName` 由 `carpet-ice-addition-mcXXXX` 改为 `carpet-ice-addition-<版本>`（平台唯一性保持）：中间 jar / 本地 maven publication 坐标随之变化——属可观察变化，经计划修订确认接受；发布 jar 与 sources jar 命名由 `release_minecraft_range` 推导（`Carpet-Ice-Addition-v3.0.0-mc<label>.jar`），**逐字符不变**；publish.yml 资产模式 `Carpet-Ice-Addition-v*.jar` 与文件名断言不受影响。

## 3. 自动验证矩阵（P8-1 提交前全绿，P8-2 复跑结构项）

| 验证 | 结果 |
|---|---|
| `git diff --check` | P8-1 / P8-2 提交前均干净 |

### GitHub Actions 实际结果（2026-09-06 人工确认）

- Build #56：workflow `Build`，job `Build and verify all platforms`，HEAD `6938e2e`，result **success**。GitHub-hosted Ubuntu runner 上 checkout、JDK 25、Gradle setup、`gradlew build` 与全部 verify 任务均成功——覆盖 P8-1 `8e86219`、P8-2 `f515270`、测试确认 `fb5c5df`、P8-R1 `6938e2e` 的实名架构。
| P8-0 带点项目名探针 | 通过（`projects` 层级 / `project(':…')` / projectDir 重映射） |
| `gradlew projects`（settings.gradle 全部断言 + P7-R1 坐标防漂移 + family 选择） | 通过：root + `:common` + 11 个实名版本项目共 **13 个 projects / 12 个 subprojects**；projectDir 全部为 `versions/<版本>`；`rootNode: 1.21.11` 打印确认 preprocess 图读取实名 mainProject |
| `gradlew build verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs verifyClassRenameMapping selfTestRenameEquivalence --stacktrace`（CI 等价命令集） | **BUILD SUCCESSFUL**（104 actionable tasks）；`verifyMixinConfigs` 11/11（`configLabel` 以实名项目路径呈现）；`verifyClassRenameMapping OK`（47 条 mapping 终态不变）；`selfTestRenameEquivalence OK` |
| `verifyJarEquivalence -PbaselineDir=D:/Project/Carpet-Ice-Addition-P6-baseline-final` | **11/11 runtime JAR 内容级等价**：本 Phase 涉及的项目 class 均 byte-identical（total 157–161）、普通资源 34 条 byte-identical、其余特殊资源（fabric.mod.json 逐键语义 / *.mixins.json mapping 感知 / pack.mcmeta 字节 + pack_format）按现有专项语义 invariant 验证——不声称整个 JAR / ZIP 的 raw bytes 或 SHA-256 完全一致；13 文件注释实名对 classfile 零影响；baseline 定位经 `legacyBaselineDirNames`（快照仍为 legacy 目录名，只读未动） |
| sources jar 差异登记（预期差异，不作等价声明） | 逐平台逐条目比对：55 个 (平台, .java 条目) 差异对，全部位于注释段（抽查完整 diff 确认仅注释行文本变化；runtime jar 在等价验证器全部比对项中无差异）——13 个 comment-only 编辑文件的必然结果，单独登记为预期差异，**不声称与 P6 baseline sources jar 字节等价** |
| 双树 resolver harness（§2.3） | legacy 树（tag 3.0.0 worktree）11/11 + actual 树 11/11 + expand_script 双树 8 平台抽样非空 + 负例 fail closed ×5：**全部通过** |
| 死引用检查（含 Groovy identifier） | `platform-mc` / `:platform-` / `mapping-mc` / `mc12101…mc260200` / `mc1211CarpetVersion` 全仓清点（gradle / properties / json / yml / java / txt）：仅剩白名单项——`legacyBaselineDirNames` 映射值、publish.yml legacy 解析分支、mixin 固定命名（mixin_config / fabric.mod.json / mixin json 文件名）、`shared/mc1213-12111` 档位名、gradle.properties 迁移历史注释、common/build.gradle 与 gradle.properties 首行的「正名前目录」说明、历史 docs（P8-2 现势化范围外的 phase1-7 记录） |
| Level 2 项目清单 | 13 projects / 12 subprojects（修正旧口径）；settings.json 注册表 ↔ `versions/<版本>` 目录双向断言通过 |

## 4. 人工验证判断与边界

- **游戏内人工测试通过（2026-09-06 人工确认）**：带点项目名 / 实名目录产物在游戏内加载运行正常，Phase 8 人工验证闭环完成（执行范围以人工执行记录为准）。
- Level 3 全平台人工游戏内回归不重跑：P8 发布产物经 `verifyJarEquivalence` 与 P6-baseline-final 逐条目内容级证明运行时等价（§3 口径；Level 3 所验证的行为载体未变），Level 3 结论继续由 Phase 6 验收（2026-09-06）承载；本轮游戏内人工测试（上条）在此基础上完成 Phase 8 的游戏内验证闭环。
- ~~可选轻量冒烟（非验收必需）：1.21.1（remap 最老）、1.21.11（core）、26.2（plain 最新）各启动一次 dedicated server……~~ 已由 2026-09-06 人工游戏内测试覆盖并通过。
- IDE 导入：VSCode 工作区导入报错已解决（2026-09-06 人工确认报错消失）——P8-R1 移除 FilenameFilter 闭包 SAM 依赖后重新导入正常；IntelliJ 对带点项目名（`:1.21.11` 等）的呈现仍未测试。
- GitHub Actions **Build #56 已通过**（workflow `Build`，job `Build and verify all platforms`，HEAD `6938e2e`，result success，2026-09-06 人工确认，见 §3）——实名架构在 GitHub-hosted Ubuntu runner 上完整构建与全部 verify 任务成功；P8-R2 起的 commit 随 push 常规观察 CI，如失败按 §0 约束上报人工决策，不通过放宽断言"适配"。
- 真实 workflow_dispatch 补发布演练（tag 3.0.0）未执行：公开 CI 运行 + 潜在 Modrinth 侧动作需人工决策；静态 harness 已覆盖解析层 / 展开层生产代码路径（见 §2.3）。

## 5. 待人工确认项

- ~~推送 P8-1 / P8-2 并观察 GitHub Actions Build（当前最后已知绿：Build #54 @ 1a6787b）~~ **已关闭**：Build #56 @ `6938e2e` 通过（2026-09-06 人工确认，见 §3 / §4）；P8-R2 起的 commit 随 push 常规观察 CI。
- ~~IDE 导入冒烟：带点项目名在 VSCode Buildship / IntelliJ 中的呈现与 source root 关联；P8-R1 已消除 settings.gradle 的 FilenameFilter 闭包 SAM 依赖（VSCode 导入报错的最可疑来源），需在修复后的 commit 上重新执行一次 IDE 导入确认诊断消除。~~ **VSCode 侧已关闭**（2026-09-06 人工确认重新导入后报错消失）；IntelliJ 呈现仍未测试，如使用 IntelliJ 导入时发现问题再上报。
- 真实 dispatch 演练（可选）：对 tag 3.0.0 执行一次 workflow_dispatch（预期 Modrinth 幂等预检 should_upload=false、gh release upload 步骤为 release 事件专属不会执行）作为 legacy 兼容的端到端确认。
- 下一次正式 Release（release 事件）时 publish.yml 布局解析层将首次在 CI 上以 actual 形态执行；dispatch legacy 路径（checkout pre-P8 tag）预计仅历史补发布场景触发。
