# 重构前基线（Fallen-Breath 多版本架构迁移）

> 本文档是迁移动工前的项目基线快照，作为 Phase 1 构建体系迁移与 Phase 2 等价验证的对照依据。只描述当前状态，不包含目标设计。目标架构与路线图见 [refactor-target-architecture.md](refactor-target-architecture.md)，验收标准见 [refactor-acceptance-checklist.md](refactor-acceptance-checklist.md)。

- 基线快照：分支 `main` @ `0270fc3`（chore: 将mod版本更新至 3.0.0），工作区干净。
- Gradle Wrapper 9.2.1；`gradle.properties`：`org.gradle.jvmargs=-Xmx4G -XX:MaxMetaspaceSize=1G`、`parallel=true`、`workers.max=2`。
- `mod_version=3.0.0`（尚未打 tag，最新 tag 为 `2.13.1`，发布流水线会拒绝 tag 与版本不一致的发布）。
- 项目固定标识（Mod ID、包名、Mixin 命名等）以 `AGENTS.md` 的固定项目标识表为准，本文不重复维护。

## 1. 模块关系

### 1.1 关系图

```
carpet-ice-addition（root）
│   build.gradle：无 plugins / subprojects 块，仅 allprojects 公共配置
│   （group、version、仓库）+ platformProjects 列表 + 两个根验证任务
│
├── common —— 纯 java-library（无 Loom），Java 21
│     ├─ 依赖（全部 compileOnly）：fabric-carpet 1.21.1 版构建、
│     │   fabric-loader 0.18.4、gson 2.11.0、slf4j-api 2.0.17
│     ├─ 测试：JUnit Jupiter 5.10.2
│     └─ 产物 common.jar（固定无版本命名，规避 IDE 缓存失效），不单独分发
│
└── versions
    ├── platform-mc1211 … platform-mc262 —— 11 个 Loom 子项目，各自产出发布 jar
    │     每份 build.gradle 约 140 行，结构高度克隆（差异仅属性前缀与 srcDir 清单）
    └── shared —— 17 个版本档位目录，非 Gradle 模块，仅被 platform 经 srcDir 引用
          ├── mc121x / mc26x —— Yarn 与 Mojmap 两个映射体系各自的基线层
          └── 15 个窄区间档位（含 2 个空档：mc1211-1219、mc12110-12111）
```

`settings.gradle`：显式 `include 'common'` + 11 条 `include 'versions:platform-*'`（附冗余的 projectDir 赋值）。`versions/shared` 未被 include，符合 `AGENTS.md` 的禁止项。

### 1.2 支持版本矩阵

| 平台模块 | MC | 发布区间 | minecraft 依赖 | yarn | loader | fabric-api 构建 / 依赖 | carpet 构建 / 依赖 | Java | loom 插件 | pack_format | mixin 条目 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| platform-mc1211 | 1.21.1 | 1.21~1.21.1 | >=1.21 <=1.21.1 | 1.21.1+build.3 | 0.18.4 | 0.102.1+1.21.1 / >=0.102.0+1.21 | 1.21-1.4.147+v240613 / >=1.4.147 | 21 | fabric-loom 1.13.6 | 48 | 65 + 2 client |
| platform-mc1213 | 1.21.3 | 1.21.2~1.21.3 | >=1.21.2 <=1.21.3 | 1.21.3+build.2 | 0.18.4 | 0.114.1+1.21.3 / >=0.106.1+1.21.3 | 1.21.2-1.4.158+v241022 / >=1.4.158 | 21 | 同上 | 57 | 62 + 2 client |
| platform-mc1214 | 1.21.4 | 1.21.4 | >=1.21.4 <=1.21.4 | 1.21.4+build.8 | 0.18.4 | 0.110.5+1.21.4 / >=0.107.0+1.21.4 | 1.21.4-1.4.161+v241203 / >=1.4.161 | 21 | 同上 | 61 | 62 + 2 client |
| platform-mc1215 | 1.21.5 | 1.21.5 | >=1.21.5 <=1.21.5 | 1.21.5+build.1 | 0.18.4 | 0.114.4+1.21.5 / >=0.114.1+1.21.5 | 1.21.5-1.4.169+v250325 / >=1.4.169 | 21 | 同上 | 71 | 62 + 2 client |
| platform-mc1216 | 1.21.6 | 1.21.6 | >=1.21.6 <=1.21.6 | 1.21.6+build.1 | 0.18.4 | 0.128.0+1.21.6 / >=0.127.1+1.21.6 | 1.21.6-1.4.176+v250617 / >=1.4.176 | 21 | 同上 | 80 | 62 + 1 client |
| platform-mc1218 | 1.21.8 | 1.21.7~1.21.8 | >=1.21.7 <=1.21.8 | 1.21.8+build.1 | 0.18.4 | 0.133.4+1.21.8 / >=0.129.0+1.21.7 | 1.21.7-1.4.177+v250630 / >=1.4.177 | 21 | 同上 | 81 | 62 + 1 client |
| platform-mc1219 | 1.21.9 | 1.21.9 | >=1.21.9 <=1.21.9 | 1.21.9+build.1 | 0.18.4 | 0.133.14+1.21.9 / >=0.130.0+1.21.9 | 1.21.9-1.4.185+v250930 / >=1.4.185 | 21 | 同上 | 88 | 62 + 1 client |
| platform-mc12110 | 1.21.10 | 1.21.10 | >=1.21.10 <=1.21.10 | 1.21.10+build.3 | 0.18.4 | 0.138.4+1.21.10 / >=0.134.1+1.21.10 | 1.21.10-1.4.188+v251016 / >=1.4.188 | 21 | 同上 | 88 | 62 + 1 client |
| platform-mc12111 | 1.21.11 | 1.21.11 | >=1.21.11 <=1.21.11 | 1.21.11+build.4 | 0.18.4 | 0.140.2+1.21.11 / >=0.140.2+1.21.11 | 1.4.194（Modrinth maven）/ >=1.4.194 | 21 | 同上 | 94 | 64 + 1 client |
| platform-mc261 | 26.1.2 | 26.1~26.1.2 | >=26.1 <=26.1.2 | 无（免混淆） | 0.18.6 | 0.146.0+26.1.2 / >=0.142.1+26.1 | 26.1-beta-9+v260311 / >=26.1-beta-1 | 25 | net.fabricmc.fabric-loom 1.15.1 | 101 | 63 + 1 client |
| platform-mc262 | 26.2 | 26.2 | >=26.2 <=26.2 | 无（免混淆） | 0.19.3 | 0.154.0+26.2 / >=0.154.0+26.2 | 26.2（Modrinth maven）/ >=26.2 | 25 | 同上 | 107 | 63 + 1 client |

备注：

- 1.21.x 全部使用 Yarn v2 mappings，经 `remapJar` / `remapSourcesJar` 改名发布产物；26.x 免混淆、无 `mappings` 配置，依赖用普通 `implementation`，经 `jar` / `sourcesJar` 改名发布产物。两套任务名不同是构建脚本双形态的根源。
- carpet 依赖：9 个 1.21.x 平台来自 masa maven（`carpet:fabric-carpet:...`），mc12111 与 mc262 来自 Modrinth maven（`maven.modrinth:carpet:...`）。
- Java 声明方式不一致：1.21.x 用 `source/targetCompatibility = 21` + `options.release = 21`（无 toolchain 块）；仅 26.x 有 `toolchain.languageVersion = 25`。CI 统一用 temurin JDK 25 运行 Gradle。
- mixin 配置：每平台一份 `carpet-ice-addition-mc<平台>.mixins.json`，`package = com.ice2974.carpeticeaddition.mixins`，`injectors.defaultRequire = 1`，`compatibilityLevel` 为 `JAVA_21`（1.21.x）或 `JAVA_25`（26.x）。

### 1.3 各平台 sourceSets 叠加关系（Phase 1 必须逐字保留）

java srcDir 声明顺序（平台自身 `src/main/java` 为隐式默认，下表为追加的 shared 档位，按声明先后排列）：

| 平台 | 追加 java srcDir（顺序即声明顺序） |
|---|---|
| mc1211 | mc121x, mc1211-12110, mc121x-killitem, mc1211-1218, mc1211-1215, mc1211-1214 |
| mc1213 | mc121x, mc1211-12110, mc121x-killitem, mc1211-1218, mc1211-1215, mc1211-1214, mc1213-12111, mc1213-12110, mc1213-1214 |
| mc1214 | 同 mc1213 |
| mc1215 | mc121x, mc1211-12110, mc121x-killitem, mc1211-1218, mc1211-1215, mc1213-12111, mc1213-12110, mc1215-12111, mc1215-1218 |
| mc1216 | mc121x, mc1211-12110, mc1211-1218, mc1213-12111, mc1213-12110, mc1215-12111, mc1215-1218, mc1216-1218 |
| mc1218 | 同 mc1216 |
| mc1219 | mc121x, mc1211-12110, mc121x-killitem, mc1219-12111, mc1213-12111, mc1213-12110, mc1215-12111, mc1219-12110 |
| mc12110 | 同 mc1219 |
| mc12111 | mc121x, mc121x-killitem, mc1219-12111, mc1213-12111, mc1215-12111 |
| mc261 | mc26x |
| mc262 | mc26x |

resources srcDir：全部平台追加 `common/src/main/resources`；1.21.x 除 mc1211 外再追加 `versions/shared/mc1213-12111/src/main/resources`（珊瑚配方资源）；各平台自身 `src/main/resources` 持有 fabric.mod.json 模板、mixin json、`resourcepacks/craftable_coral_blocks/pack.mcmeta`。

珊瑚块配方（10 个 `coral_block_from_*.json`）来源分布：mc1211 自有平台资源；mc1213–mc12111 共用 `shared/mc1213-12111` 资源；mc261 / mc262 各自平台资源内携带一份。

叠加不变式（迁移期间必须保持）：

1. 每个平台的合并源码集中，任一 FQCN 只出现一份。档位按类粒度切分，不依赖目录优先级遮蔽。
2. srcDir 声明顺序即档位叠加清单。Phase 1 只允许把这份清单"等价搬移"为数据（如 per-version 属性），不允许调整顺序或增删条目。
3. common 的编译产物最终内联进每个平台 jar，资源经 srcDir 进入各平台资源集。

### 1.4 common 的三重消费方式

1. `implementation project(':common')` —— 编译类路径。
2. `jar { from(project(':common').sourceSets.main.output.classesDirs) }` —— common 类内联进每个发布 jar。
3. `resources.srcDir common/src/main/resources` —— assets（icon、语言文件）进入各平台资源集。

### 1.5 shared 档位规模

| 档位 | java 文件数 | 资源 | 档位 | java 文件数 | 资源 |
|---|---|---|---|---|---|
| mc121x | 59 | 无 | mc1211-1214 | 5 | 无 |
| mc26x | 90 | 无 | mc1216-1218 | 4 | 无 |
| mc1213-12111 | 15 | 10 珊瑚配方 | mc1211-12110 | 3 | 无 |
| mc1215-12111 | 9 | 无 | mc1213-1214 | 3 | 无 |
| mc1219-12111 | 8 | 无 | mc1211-1215 | 2 | 无 |
| mc1211-1218 | 6 | 无 | mc1219-12110 | 2 | 无 |
| mc121x-killitem | 1 | 无 | mc1213-12110 | 1 | 无 |
| mc1215-1218 | 1 | 无 | mc1211-1219 / mc12110-12111 | 0 | 空档 |

17 个档位目录结构统一为 `<档位>/src/main/java/com/ice2974/carpeticeaddition/{command,mixins,rules,settings,villagerevents}`；mc26x 子包结构略有差异。档位内无 mixin json 与 pack.mcmeta（全部在平台模块内）。

## 2. 构建流程

- 标准入口（CI 同款）：`.\gradlew.bat build verifyCraftableCoralBlocksJars verifyFabricModJson`。
- 根 `build.gradle`（无 plugins 块、无 subprojects 块）：
  - `allprojects`：group、version、仓库（fabricmc maven、masa maven、Modrinth maven（限 `maven.modrinth` group）、mavenCentral）。
  - `platformProjects` 列表与 `gradle.projectsEvaluated` 断言：保证列表与 `:versions:platform-*` 子项目一致（settings.gradle 与 build.gradle 双源，靠断言防漂移）。
  - `verifyCraftableCoralBlocksJars`：逐平台打开发布 jar 断言——恰 1 份 `resourcepacks/craftable_coral_blocks/pack.mcmeta` 且 pack_format 符合平台表；恰 10 个 `coral_block_from_*` 配方；主数据包无遗留 `data/carpet-ice-addition/recipe/coral_block_*`。
  - `verifyFabricModJson`：fabric.mod.json 存在、无 UTF-8 BOM、JSON 可解析、无未展开的 `${` 占位符。
  - 两个验证任务以 dependsOn 挂接到每个平台的 remapJar（1.21.x）/ jar（26.x）。
- 平台 `processResources`：8 个 `inputs.property` + 9 个 expand 键（version、id、name、minecraft_range、minecraft_dependency、fabric_version、fabric_dependency、carpet_version、carpet_dependency），11 份逐字重复仅前缀不同。
- 发布命名逻辑：约 45 行 Groovy（`compareMcVersions` / `toVersionParts` / `toPatchValue` / `releaseMinecraftLabel`）在 11 份平台 build.gradle 中逐字重复。
- `maven-publish`：各平台声明 `mavenJava(MavenPublication)` 但无 `repositories` —— 实际仅 `publishToMavenLocal` 可用，属残留配置。

## 3. jar 输出规则

发布 jar 名 = `Carpet-Ice-Addition-v${mod_version}-mc<label>.jar`（另产 `-sources.jar`）。label 由 `*_release_minecraft_range` 推导：

1. 区间按 `~` 或 `-` 拆分并升序排序；
2. 单值区间 → label 为原文；
3. 两端 major.minor 相同且 patch 差值 >= 2 → label 为 `X.Y.x`；
4. 否则 label 为 `lower-upper`。

当前 `mod_version=3.0.0` 下的期望发布产物：

| 平台 | 发布 jar |
|---|---|
| mc1211 | Carpet-Ice-Addition-v3.0.0-mc1.21-1.21.1.jar |
| mc1213 | Carpet-Ice-Addition-v3.0.0-mc1.21.2-1.21.3.jar |
| mc1214 | Carpet-Ice-Addition-v3.0.0-mc1.21.4.jar |
| mc1215 | Carpet-Ice-Addition-v3.0.0-mc1.21.5.jar |
| mc1216 | Carpet-Ice-Addition-v3.0.0-mc1.21.6.jar |
| mc1218 | Carpet-Ice-Addition-v3.0.0-mc1.21.7-1.21.8.jar |
| mc1219 | Carpet-Ice-Addition-v3.0.0-mc1.21.9.jar |
| mc12110 | Carpet-Ice-Addition-v3.0.0-mc1.21.10.jar |
| mc12111 | Carpet-Ice-Addition-v3.0.0-mc1.21.11.jar |
| mc261 | Carpet-Ice-Addition-v3.0.0-mc26.1.x.jar |
| mc262 | Carpet-Ice-Addition-v3.0.0-mc26.2.jar |

mod_version 变更时文件名整体替换版本段，label 部分不变。

## 4. 发布流程（CI）

- `build.yml`：push / PR 到 main（忽略 `**.md`、LICENSE）+ 手动触发；单 job（ubuntu + temurin JDK 25）执行 `./gradlew build verifyCraftableCoralBlocksJars verifyFabricModJson`。无产物上传，纯验证。
- `publish.yml`（约 960 行）：
  - 触发：GitHub Release published，或手动 dispatch（tag 必须指向已存在的非 draft Release；可选 platforms 子集）。
  - 版本校验：断言 Release tag == `gradle.properties` 的 `mod_version`；jar 文件名必须含 `-v${MOD_VERSION}-`；每平台恰 1 个非 sources jar。
  - 平台清单：从 `settings.gradle` grep 解析。
  - Modrinth 游戏版本展开：内联 Python 断言每平台 `*_minecraft_dependency` 区间与 `*_release_minecraft_range` 一致，并按 Modrinth release 列表展开闭区间；另从 Modrinth API 拉取 carpet、fabric-api 依赖项目 ID。
  - GitHub Release 资产上传（`gh release upload --clobber` + 逐文件存在性校验）。
  - Modrinth 上传：项目 `3ZWOd2ma`，`Kira-NT/mc-publish@v3.3.1`，loaders=fabric，依赖 carpet（必需）+ fabric-api（必需）；含完整幂等预检与后验（API 轮询、文件名/sha1/元数据全量比对、失败残留检查）。
  - secrets：`MODRINTH_TOKEN` 与 `github.token`；无 CurseForge 发布。
- 无 scripts/ 目录；本地开发部署用 `.minecraft/deploy.cmd`（构建后按映射表拷贝到本地多实例 mods 目录）。

## 5. 构建结果等价对照基准（Phase 1 / Phase 2 判定用）

Phase 1 完成后，以下内容必须与迁移前完全一致（判定时逐项对照）：

1. **jar 文件名**：§3 的 11 个文件名。
2. **fabric.mod.json 语义**：`depends` 值逐平台与 §1.2 表一致；`id=carpet-ice-addition`、`name=Carpet Ice Addition`、`version=${mod_version}`、license `PolyForm Noncommercial License 1.0.0`、`environment=*`、entrypoints 仅 `main: [...CarpetIceAdditionMod]`、`mixins` 引用各平台 mixin json。
3. **mixin 配置行为**：文件名、package、compatibilityLevel、`mixins` / `client` 条目数与 §1.2 表一致（`defaultRequire=1` 意味着任何条目缺失或类缺失都会在加载期失败，属隐性保护）。
4. **jar 内 class / resource 结构**：平台类 + common 类内联（统一包前缀 `com.ice2974.carpeticeaddition`）；`resourcepacks/craftable_coral_blocks/` 内容（pack.mcmeta + 10 配方）；`assets/carpet-ice-addition/{icon.png, lang/*}`。
5. **rule / command / logger 注册行为**：见验收清单文档的注册数量矩阵（40 / 38 / 39 / 39 / 39）。

## 6. 功能基线统计

### 6.1 规则（41 个）

- 分布：`CarpetIceAdditionSettings`（common，34 个，全平台）+ `CarpetIceAdditionLowVersionSettings`（2 个，仅 mc1211）+ `CarpetIceAdditionHighVersionSettings`（1 个，mc12111 / mc261 / mc262）+ shared 档位双胞胎 4 对（`customEndPlatformPosition`、`craftableCoralBlocks`、`waterFluidTickDelay`、`lavaFluidTickDelay`，mc121x 与 mc26x 各一份声明，名称默认值一致）。
- 每平台注册数：mc1211=40；mc1213–mc12110（7 个平台）=38；mc12111=39；mc261=39；mc262=39。
- 分类：全部含 `ICE`；细分 FEATURE / BUGFIX / SURVIVAL / COMMAND / CLIENT / BOT / OPTIMIZATION。
- 翻译键约定：中文 `carpet.rule.<name>.name` + `.desc`，英文仅 `.desc`（英文规则名直接显示内部名）；另有 `carpet.rule.craftableCoralBlocks.conflict.*` 3 键。
- 命令门规则 `commandKillItem` / `commandMachineStatus`：String 类型默认 `"ops"`，options `{false, true, ops, 0, 1, 2, 3, 4}`。
- 逐条规则表（默认值 / 分类 / 实现位置）见验收清单附录 A。

### 6.2 命令（2 个）

- `/killitem`（规则门 `commandKillItem`）：实现分布——`shared/mc121x-killitem`（1.21.1–1.21.5、1.21.9、1.21.10）、`shared/mc1216-1218`（1.21.6–1.21.8）、`platform-mc261` / `platform-mc262`（Mojmap 变体），共 4 处实现。
- `/machineStatus`（规则门 `commandMachineStatus`）：实现分布——`shared/mc1211-1214`、`shared/mc1215-12111`、`platform-mc261` / `platform-mc262`，共 3 处实现。
- 注册方式：各平台入口类覆写 `CarpetExtension.registerCommands()`。

### 6.3 logger（1 个）

- `villagerEvents`：options `{all, death, zombified, witch}`，默认 `all`，strict；`VillagerEventsLogger`（Phase 6 统一名称，基线期为 `VillagerEventsLogger121` / `VillagerEventsLogger26` 双胞胎）；订阅加速字段 `__villagerEvents`；服务端翻译兜底 `VanillaLanguageService`。

### 6.4 Mixin（105 个唯一类 / 173 个文件 / 68 份重复拷贝）

- 重复两大轴：Yarn↔Mojmap 双胞胎（mc121x ↔ mc26x，约 30 对；Phase 6 已统一项目类名至 `@Mixin` 目标 Mojmap 拼写，文件份分叉仍由 per-version override 承载）；1.21.x 窄档位分叉（最重：`PhantomNeutralPhantomsMixin` 6 份、`AcquirePoiIronGolemOptimizationMixin` 4 份，Phase 6 前旧名 `PhantomEntityNeutralPhantomsMixin` / `FindPointOfInterestTaskIronGolemOptimizationMixin`）。
- client 侧 mixin 仅 4 个类：`DisableIllegalTextCharacterCheckClipboardMixin`（全部平台）与 `BookEditScreenDisableIllegalTextCharacterCheckMixin`（仅 1.21.1–1.21.5），全部服务于 `disableIllegalTextCharacterCheck`。
- villager-events 独立 mixin 9 个（不挂任何规则）。
- `@Accessor` 1 个：`ServerCommonNetworkHandlerAccessor`（mc121x）。

### 6.5 入口类（5 份，约 95% 重复）

mc1211 / `shared/mc1213-12110`（mc1213–mc12110 共用）/ mc12111 / mc261 / mc262。各自注册：4 组 settings 类、命令、logger、翻译提供、rule observer、KillItem / MachineStatus 配置管理器生命周期、VillagerEventsRuntime、珊瑚数据包控制器、22 个 per-feature `AtomicBoolean` 兼容错误标志。

### 6.6 翻译三处维护点

- `common/src/main/resources/assets/carpet-ice-addition/lang/zh_cn.json`（192 键）、`en_us.json`（151 键）。
- `common/src/main/java/.../translation/CarpetIceAdditionTranslations.java` 硬编码 Map 是运行时真源（服务端翻译，纯服务端模组约定）。
- 已知不一致：logger 的 10 个 `logger.carpet-ice-addition.villager_events.*` 键仅存在于硬编码 Map，JSON 语言文件缺失。

## 7. 当前架构优点

1. 物理隔离：跨版本差异以独立源码文件表达，差异可见、冲突在编译期暴露。
2. 编译期 fail-fast：mixin `required=true` + 根验证任务 + `projectsEvaluated` 同步断言。
3. 无运行期版本字符串判断（符合 `AGENTS.md` 约定）。
4. 发布管线幂等且自校验（发布前后均对 Modrinth API 做全量核对）。
5. common 模块抽出 MC 无关逻辑并带单元测试。

## 8. 长期维护风险

1. 11 份约 140 行的克隆 build.gradle；45 行发布命名逻辑重复 11 份；已出现轻微漂移（如 1.21.x 用 `${project.mod_version}` 而 26.x 用 `${project.version}`）。
2. 根 `gradle.properties` 按平台前缀膨胀（每平台 7–8 键），全局属性与版本数据混杂。
3. 68 份 mixin 重复拷贝：同一特性改动需同步多处，漏改风险随版本数增长。
4. 双重分叉轴：Yarn↔Mojmap 双胞胎 + 1.21.x 窄档位分叉，叠加成本最高。
5. 档位碎片化：17 个档位（含 2 空档），每平台的叠加清单手工维护、互不相同。
6. 5 份入口类克隆。
7. 翻译三源同步（JSON × 2 + 硬编码 Map），且已存在缺同步实例。
8. 双 loom 插件 / 双 Java 目标 / 双任务名（remapJar vs jar）导致构建脚本双形态。
9. `settings.gradle` include 列表与根 `build.gradle` 的 `platformProjects` 双源，仅靠断言兜底。

## 9. 已知不一致与遗留（仅记录，本阶段不处理）

- 翻译 JSON 与硬编码 Map 不同步（logger 键缺失，见 §6.6）。
- 空 shared 档位：`mc1211-1219`、`mc12110-12111`。
- `platform-mc261/src/main/resources/data/` 空目录残留。
- 各平台 `maven-publish` 声明无 repositories 的残留配置。
- `mod_version=3.0.0` 尚未打 tag；发布流水线将拒绝 tag 不匹配的发布。
- 1.21.x 插件 id `fabric-loom` 与 26.x `net.fabricmc.fabric-loom` 不一致（历史演进形成，Phase 1 冻结不动）。
