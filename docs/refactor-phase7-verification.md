# Phase 7 验证记录：构建骨架收敛（preprocess_enabled 移除 + 共享 family 构建入口 + 薄包装删除）

> Phase 7 于 main 分支执行（基点 fb10d2d，Phase 6 终态）。目标：在冻结全部依赖与数据模型的前提下收敛构建脚本骨架——删除实证无回滚价值的 `preprocess_enabled` 开关，以参考实现同款的 `buildFileName` 机制用两份共享 family 构建入口替代 11 份逐平台薄包装 `build.gradle`。**不改变任何外部行为**：项目路径、projectDir、buildDir、任务路径、发布 jar 命名与产物路径、CI 与 publish 流程、P6-baseline-final 基线布局全部不变。
>
> **验收状态：完成**（2026-09-06；Level 1 全量矩阵 + 结构 preflight + Level 2 死引用检查逐 commit 全绿；Level 3 边界判断见 §4）。

## 0. 已确认约束（人工锁定，全程有效）

- 冻结项：数据模型全部键（loom_plugin / jar_task / java_release / java_toolchain / pack_format / minecraft_version / release_minecraft_range / minecraft_dependency / fabric_version / fabric_dependency / carpet_version / carpet_dependency / carpet_repo）、preprocessor JitPack 全 SHA（`c5abb4fb…`）、mainProject = platform-mc12111 语义、verifyJarEquivalence 证明能力（fail closed）、`org.gradle.parallel=false`。
- 禁止项（Phase 8+ 范围，本相一律不做）：平台目录 / Gradle 项目改名、preprocess 节点身份变更、common 模块删除或迁移、根 `src/main/resources`、`versions/shared` 删除、`extra_resource_dirs` 语义变更、mixin json 统一、任何依赖升级。
- settings.gradle 选择 family 入口时不得假设 Settings 阶段可读子项目 gradle.properties 普通属性（`providers.gradleProperty()` 只覆盖根 / 用户 / 系统属性）：必须显式解析 `versions/platform-*/gradle.properties` 取 `loom_plugin`（缺文件 / 缺键 / 重复键 / 无分隔符 / 未知值一律 fail closed）；per-version `gradle.properties` 继续是平台构建数据唯一来源，不新建第二套 family 注册表。
- `loom_plugin` → 入口映射按完整 `id:version` 精确匹配（当前仅允许 `fabric-loom:1.13.6` → `build-remap.gradle`、`net.fabricmc.fabric-loom:1.15.1` → `build-plain.gradle`）；不把 `PluginDescriptor.getVersion()` 作为主线依赖，common.gradle 保留既有 id 级运行期断言。
- 若 `buildFileName` 指向仓库根共享脚本引起 projectDir / buildDir / mainProject 回退路径 / 任务路径 / 产物路径变化：不允许改 CI / publish 路径"适配"，必须修正构建骨架保持外部行为不变（本相实证未发生任何此类变化，见 §2）。

## 1. 分步记录

| 步骤 | commit | 内容 |
|---|---|---|
| P7-A | 4dd44b9 | 移除 `preprocess_enabled`：common.gradle 头注释可选键表与条件分支删除，preprocess 接入（插件 apply + afterEvaluate 资源恢复 + 根 src 无 resources 断言）改为无条件；11 份 per-version `gradle.properties` 删除 `preprocess_enabled=true`（mc1214 连同其 P5-3 历史注释）。死开关实证：全仓消费点仅 common.gradle 条件分支；11 平台全 true；false 分支需要旧共享 Java 组织（P5-7 已删除）才可能产出可用构建——core 平台本地无 Java、非 core 平台仅 override 子集、资源恢复被跳过，false 只能产出废 jar；CI / verify 任务 / publish.yml / 根 build.gradle 零消费。 |
| P7-B | 3ba59c3 | settings.gradle 新增 family 入口选择：逐平台显式解析 per-version `gradle.properties`（ISO-8859-1 逐行解析，重复键 / 缺分隔符 / 缺键 fail closed）取 `loom_plugin`，按完整 `id:version` 精确匹配 `build-remap.gradle`（9 平台）/ `build-plain.gradle`（2 平台）并设 `ProjectDescriptor.buildFileName`（按 ProjectDescriptor 语义相对 projectDir 解析 `../../`）；两份入口文件的 plugins 块与原薄包装逐字一致，另加 `jar_task` 家族一致性断言；common.gradle 注释同步（family 入口表述）。薄包装转 inert（保留至 P7-C 删除）。结构 preflight + 全量矩阵全绿（§2 / §3）。 |
| P7-C | （本 commit） | `git rm` 11 份 `versions/platform-*/build.gradle`；AGENTS.md 同步（新增平台配方去掉薄包装并补 family 入口说明、数据键表去掉 preprocess_enabled、共通构建逻辑表述）；target-architecture §2.A / §6 Phase 7 / §9-6 现势化；新建本文档。 |

## 2. 结构 preflight（P7-B 与 P7-C 之间执行；删除薄包装前的结构实证）

临时 init 脚本（不入库）在 projectsEvaluated 输出全部 11 平台骨架字段，与切换前对照：

| 字段 | 切换前 | 切换后 | 结论 |
|---|---|---|---|
| project path | `:platform-mcXXXX` | 不变 | 一致 |
| projectDir | `versions/platform-mcXXXX` | 不变 | 一致 |
| buildDir | `versions/platform-mcXXXX/build` | 不变 | 一致 |
| buildFile | `versions/platform-mcXXXX/build.gradle` | 仓库根 `build-remap.gradle`（9 平台）/ `build-plain.gradle`（2 平台） | 按设计变化（Phase 7 目标本身），其余不变式不受影响 |
| loom 插件 | remap 9 平台 Fabric Loom 1.13.6 / plain 2 平台 1.15.1（配置期 banner） | 不变 | 一致（common.gradle id 级断言 11/11 通过） |
| jar 任务形态 | remap 家族 remapJar + remapSourcesJar；plain 家族 jar + sourcesJar | 不变 | 一致 |
| 发布 jar 命名 | `Carpet-Ice-Addition-v3.0.0-mc<releaseMinecraftLabel>.jar` | 不变（preflight 逐平台打印 archive 一致） | 一致 |
| preprocess | 11/11 已应用；rootNode = platform-mc12111 | 不变 | 一致（插件 `../mainProject` 回退路径随 projectDir 不变） |
| 复用同一物理脚本 | —（逐平台独立文件） | `build-remap.gradle` ×9、`build-plain.gradle` ×2 | 多 project 复用同一 family 入口安全（全量构建 + verify + jar 等价通过） |

## 3. 自动验证矩阵（P7 每个正式 commit 均执行）

| 验证 | 结果 |
|---|---|
| `git diff --check` | 每次提交前干净 |
| `gradlew build`（11 平台 + `:common:test`） | P7-A / B / C 三次全通过（105 actionable tasks） |
| `verifyCraftableCoralBlocksJars` / `verifyFabricModJson` / `verifyMixinConfigs` | 11/11 通过（三次） |
| `verifyClassRenameMapping` | 通过（mapping 终态 47 条不变） |
| `selfTestRenameEquivalence` | 通过 |
| `verifyJarEquivalence -PbaselineDir=D:/Project/Carpet-Ice-Addition-P6-baseline-final` | 11/11 通过（三次），逐平台 byteIdentical = total（151–161）、普通资源 34–36 条字节一致、47 条 mapping 全部 inert——构建骨架收敛对产物零影响 |
| Level 2 死引用检查 | `git grep preprocess_enabled`：P7-C 后仅 `docs/refactor-phase5-verification.md` 历史记录与本文档（历史 / 移除说明），全部 gradle / properties / .github / Java 零残留。`git ls-files 'versions/platform-*/build.gradle'`：P7-C 后为空。CI / publish.yml 无平台 build.gradle 路径引用（仅产物路径 `versions/platform-*/build/libs`，未变） |
| Level 2 项目清单 | 根 + `:common` + 11 平台共 12 个项目；settings.json 注册表 ↔ `versions/platform-*` 目录一致性断言通过 |

## 4. 人工验证判断与边界

- 不要求重跑全平台 Level 3 人工游戏内回归：P7 三个 commit 的发布产物经 `verifyJarEquivalence` 与 P6-baseline-final 逐条目 / 逐字节证明等价（Level 3 所验证的行为载体未变），Level 3 结论继续由 Phase 6 验收（2026-09-06）承载。
- 可选轻量冒烟（非验收必需）：mc1211（remap 最老）与 mc262（plain 最新）各启动一次 dedicated server，确认 Mixin bootstrap 无错、`/carpet list` 规则数量与 docs 清单一致。
- IDE 导入（VSCode Buildship / IntelliJ）未验证：动态 `buildFileName` 属 Gradle 标准机制，但 IDE 对项目目录外 buildFile 的呈现方式未测试（见 §5）。

## 5. 待人工确认项

- IDE 导入呈现（见 §4）：如发现 IDE 误将共享入口当作平台专属脚本或丢失 source root 关联，属 IDE 适配层面问题，不据此回改构建骨架语义。
- 游戏内运行与 CI 冷缓存（全新 runner 首次构建）未在本机验证：无理由预期差异（依赖解析路径与任务图不变）；如 CI 首跑异常，按 §0 约束上报人工决策，不通过放宽断言"适配"。
