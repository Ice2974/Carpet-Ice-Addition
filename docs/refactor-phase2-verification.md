# Phase 2 等价验证记录（构建架构收尾与迁移后稳定化）

> 本文档是 [refactor-target-architecture.md](refactor-target-architecture.md) Phase 2 的产出物，记录 Phase 1 构建体系迁移后的等价验证结果。对照数据见 [refactor-baseline.md](refactor-baseline.md)（下称"基线"），验收条目见 [refactor-acceptance-checklist.md](refactor-acceptance-checklist.md)（下称"验收清单"）。

## 1. 状态总览（双轨）

| 状态 | 值 | 说明 |
|---|---|---|
| implementation status | complete（2026-09-04） | 全部代码 / 配置 / 文档改动落地，自动化验证通过 |
| acceptance status | accepted（2026-09-04） | Level 2 / Level 3 人工验收、IDE 导入、dispatch 演练均已完成 |

进入 Phase 3 的唯一依据：本表 acceptance status 变更为 accepted。在此之前不得宣布 Phase 2 完成。

## 2. 自动化验证结果（全部通过，2026-09-04）

| 验证项 | 命令 / 方法 | 结果 |
|---|---|---|
| L1-1 全平台构建 + 既有验证 | `.\gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson --stacktrace` | 通过（11 平台全绿） |
| L1-3 / L1-4 增强（verifyFabricModJson 新断言） | 同上任务内新增断言：mixins 引用 == `[mixin_config]`、mixin 配置文件存在于 jar、`depends.fabricloader == ">=loader_version"` | 通过（11 平台全绿） |
| **L1-6 jar 内容级等价对照** | `.\gradlew.bat verifyJarEquivalence -PbaselineDir=<基线快照目录>`（基线重建方法见 §3） | **11/11 平台通过**：zip 条目清单、fabric.mod.json 解析后逐键语义、全部 `*.mixins.json` 字节一致、pack.mcmeta 的 pack_format 一致；sources jar 两侧存在且同名 |
| L1-2 jar 命名 | L1-6 定位逻辑隐含（基线 jar 按同名定位成功 = 两侧文件名一致，label 段与基线 §3 表逐字符一致） | 通过 |
| common 单测（验收清单 3.4） | `.\gradlew.bat :common:test` | 通过 |
| maven-publish 收敛验证 | `.\gradlew.bat publishToMavenLocal --dry-run`（根级，覆盖全部子项目）+ mc1211 / mc262 单独 dry-run | 11/11 平台存在 `mavenJava` publication 与 `publishMavenJavaPublicationToMavenLocal` 任务；两种 loom 形态任务图正确（remap：remapJar/remapSourcesJar → publish；plain：jar/sourcesJar → publish）；dry-run 未实际写入 Maven Local |
| publish.yml 解析段等价验证（本地） | jq 1.7.1 对 `settings.json`：真实文件校验通过、提取顺序与注册表一致；空数组 / 非字符串 / 非法形态 / 重复 / 非数组 / 缺键 / 非法 JSON 共 6 类变异副本全部被拒 | 通过 |
| publish.yml 展开脚本等价验证（本地） | 从 workflow 原样提取 python，拉取 Modrinth game_version 标签（909 条），逐平台运行 | 11 平台展开结果与 `*_release_minecraft_range` 一致（如 mc1211 → 1.21, 1.21.1；mc261 → 26.1, 26.1.1, 26.1.2）；不存在的平台码正确报错退出 |
| 工作区卫生 | `git diff --check` | 通过 |

## 3. 基线快照重建方法（L1-6 对照基准，可复现）

基线 = **0270fc3（Phase 1 动工前提交）的等价工作区全量构建产物**，经 `git worktree` 重建（决策记录见 §4）。Release 2.13.1 资产仅作历史行为辅助参考，不作为 L1-6 基线（其源码为 2.13.1，与 3.0.0 无内容级可比性）。

重建步骤（Windows，仓库外目录 `<WT>`）：

```bash
git worktree add <WT> 0270fc3
cd <WT>
# 伪影修复 1：autocrlf=true 的新检出于本地会将 LF 源文件 smudge 为 CRLF，
# 而本机主仓工作区为混合行尾状态。先恢复 index 原始内容（LF）：
git -c core.autocrlf=false checkout -- .
# 伪影修复 1 续：再把主仓中工作区为 CRLF 的 src 文件在 <WT> 恢复 CRLF，
# 使两侧 src 资源逐字节一致（清单来自主仓 `git ls-files --eol` 的 w/crlf 项）：
#   （对每个 w/crlf 的 versions/*/src/*、common/src/* 文件执行 CRLF 转换）
# 伪影修复 2：补建主仓存在但 git 不跟踪的 src 空目录（清单来自主仓 `find … -type d -empty`），
# 26.x 免混淆形态的 jar 任务会为空目录生成目录条目（data/ 等）：
#   （对每个空目录执行 mkdir -p）
./gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson
```

重建中遇到并处理的两个伪影（均为主仓工作区状态无法被纯 git 检出复现所致，与 Phase 1 迁移无关）：

1. **行尾 smudge**：worktree 全新检出把 414 个 LF 文本文件转成 CRLF，导致基线 jar 内 mixin json 与主仓 jar（LF）字节不一致。按上法修复后，两侧 jar 资源文件逐字节一致。主仓另有 43 个混合行尾的 `.java` 文件（重建后为 LF），javac 产物不携带源码行尾，对 jar 内容无影响。
2. **未跟踪空目录**：主仓 src 树内 23 个空目录（如 `platform-mc261/src/main/resources/data/carpet-ice-addition/`，即基线 §9 已记录的残留）不被 git 跟踪，worktree 检出后不存在；26.x plain 形态的 jar 会为它们生成 `data/`、`data/carpet-ice-addition/` 目录条目（Yarn remap 形态的 remapJar 不保留目录条目，故 1.21.x 平台不受影响）。补建空目录后两侧条目集一致。迁移前的真实构建同样会包含这些条目，因此补建是对迁移前行为的忠实复刻，而非对现状的修饰。

基线快照目录保留至 Phase 2 验收结束后删除（`git worktree remove <WT>`）。

## 4. 决议记录（落地 refactor-target-architecture §9 挂起项）

| 事项 | 决议 |
|---|---|
| 注册表文件名（§9-4） | 定为 `settings.json`（TIS 命名） |
| loom_plugin 数据化形态（§9-6） | 接受：per-version 属性声明插件 id:version，common.gradle 断言其与薄包装 plugins 块一致 |
| publish.yml 联动（§9-7 / R9） | 在 Phase 2 完成：平台清单改读 `settings.json`（JSON 层校验通过后才提取，保留注册表顺序，不 sort/dedup，空列表 / 非字符串 / 形如非 `mc[0-9]+$` / 重复均 fail-fast）；Modrinth 展开脚本改读 per-version `gradle.properties`（去前缀键）。幂等预检 / 上传 / 后验逻辑零改动 |
| 旧 jar 对照基线留存（§9-5） | 采用 git worktree @0270fc3 重建（方法见 §3）；Release 2.13.1 仅作历史参考 |
| AGENTS.md 同步（§9-8） | 已完成：新增「版本注册表与构建配置」一节（注册表唯一来源、新增平台流程、版本数据归属 per-version、共通逻辑双形态验证要求） |
| maven-publish 收敛 | 插件改由 common.gradle 统一 `pluginManager.apply`，11 份薄包装各删 1 行；行为等价经 dry-run 验证（§2） |
| 薄包装形态定位 | Phase 2 后、当前 Yarn / 26.x 双构建形态下的稳定形态；Phase 4 若 mappings / Loom 架构发生变化，可重新评估（不表述为永久最终形态） |
| fabricloader 一致性断言定位 | `depends.fabricloader == ">=${loader_version}"` 是本项目当前数据模型的**项目级 invariant**（模板硬编码值与 per-version `loader_version` 恒等），**不是 Fabric Loader 的通用必然关系**；未来若某平台最低运行依赖需与构建 loader 版本分离，应先调整 per-version 数据模型，再同步放宽该断言 |

## 5. 发布门禁（3.0.0 正式 Release / tag 创建前必须满足）

不依赖 GitHub Actions 的 workflow 引用行为作为安全前提。

| # | 门禁 | 状态 |
|---|---|---|
| 1 | 发布分支（打 tag 所依据的 commit）已包含修复后的 publish.yml | 已完成 |
| 2 | 已完成 workflow_dispatch 演练，或人工确认等价验证充分 | 已完成 |

## 6. 人工项清单（未执行，Agent 不得代验）

| 项 | 内容 | 执行人 | 日期 | 结果 |
|---|---|---|---|---|
| L1-5【人工】mod 加载冒烟 | mc1211 / mc1215 或 mc12110 / mc12111 / mc262 四类形态：启动无 mixin / 注册错误，`/carpet` 可用 | Ice2974 | 2026.9.4 | 通过 |
| L1-7【人工】注册行为 | 各平台 `/carpet list` 条目数符合验收清单 §3.1 矩阵（40 / 38×7 / 39×3），`/log villagerEvents` 可订阅 | Ice2974 | 2026.9.4 | 通过 |
| Level 2【人工】核心功能 | 验收清单 §2 全部 12 项（2-1 ~ 2-12） | Ice2974 | 2026.9.4 | 通过 |
| Level 3【人工】完整回归 | 验收清单 §3（规则逐条 / 版本分支 / 资源翻译 / mixin 完整性） | Ice2974 | 2026.9.4 | 通过 |
| IDE 导入（R6） | VSCode / IntelliJ 导入新架构工程正常 | Ice2974 | 2026.9.4 | 通过 |
| dispatch 演练 | 见 §5 门禁 2 | Ice2974 | 2026.9.4 | 通过 |

## 7. 已知限制与未验证项

- publish.yml 的 GitHub Actions 端到端执行未验证（无法本地运行）；本地已覆盖解析段与展开脚本的全部行为分支（含失败分支）。风险由 §5 门禁兜底。
- `verifyJarEquivalence` 的 class 文件不做字节级比对（编译时间戳噪声，沿用目标文档 R5 决策），以条目清单 + Level 2/3 行为验证兜底。
- 主仓工作区行尾呈混合状态（`core.autocrlf=true` 历史形成）；等价验证以 jar 资源字节一致为准，该环境事实本身不在本次处理范围。
