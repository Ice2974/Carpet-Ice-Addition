# Phase 11 验证记录：单一 canonical Mixin registry

> **验收状态：代码与自动验证完成**（2026-09-07，功能 commit `f6511da`）；Level 3 游戏内人工测试尚未执行，见 §7。

## 1. 范围与冻结项

- 唯一 canonical 数据源为 `gradle/mixins/registry.json`；11 份平台手写 `src/main/resources/*.mixins.json` 删除。
- 现有 11 个 `mixin_config` 值、runtime config 文件名、11 个 `fabric.mod.json` 与 `.github/workflows/publish.yml` 不变。
- Mixin Java、Loom、依赖、mapping、preprocess 版本图、P9/P10 verifier 语义不变。
- `sourceSets.main.resources.srcDirs` 终态仍严格为 `[platform local, root]`。generated config 不注册为 resource srcDir。
- 不使用 resource preprocess、`IMixinConfigPlugin` 或运行时版本判断。

## 2. P11-0 行为门禁

在 sterile detached worktree 对 1.21.1（remap）、1.21.11（core）和 26.2（plain）放入未跟踪 root probe resource：

- non-core 的 `preprocessResources` 会产生条件求值后的 probe，但 Phase 10 `afterEvaluate` 恢复的 resource srcDirs 使 `processResources`、runtime JAR 与 sources JAR 实际消费原始 root resource，生成层结果被绕过。
- core 1.21.11 按插件实现没有 `preprocessResources`，直接消费 root resource；这是预期路径，不是 probe 失败。
- 因三类平台不能以 resource preprocess 形成一致生成链，Phase 11 采用独立 build-time generator。probe、临时 worktree 与临时输出均已清理。

## 3. Canonical、predicate 与 generator

Canonical schema 登记固定字段、entry、side、可选 `onlyMc` / `minMc` / `maxMc`、`basePrecedence` 和 `orderOverrides`。predicate 值只能是 `settings.json` 已登记版本名；generator 通过各版本项目由 preprocess 注入的 `project.extra["mcVersion"]` 建立 symbol → numeric code 映射，不解析版本字符串。未知 symbol、互斥/倒置/空 predicate、未命中任何平台注册项全部 fail closed。

每个平台注册独立 `generateMixinConfig`：

- inputs：canonical manifest、`settings.json`、平台版本名、当前 `mcVersion`、完整已登记 symbol→`mcVersion` 映射、`java_release`、`mixin_config`。
- output：`versions/<v>/build/generated/mixinConfig/<mixin_config>` 单一文件。
- UTF-8、LF、固定字段顺序、lexical topological tie-break、无 timestamp；`clean` 后可完全重建，输入不变时任务为 `UP-TO-DATE`。
- `processResources` 与 `sourcesJar` 各自以 task output 显式消费该文件；不新增 sourceDir。

迁移 inventory 动态统计为 70 个 unique entry、7 种 membership signature。这两个数字仅记录本次迁移事实，不进入长期 generator/verifier invariant；长期闭环以 canonical 展开结果与实际产物为准。

## 4. Precedence replacement 语义

`basePrecedence` 对已审计的同 target 行为相关 pair 建立 DAG；无约束节点以 lexical tie-break 确定顺序。`orderOverrides` 只能反转 base 中显式登记 `auditedOverrides` 的同一 unordered pair。

应用 override 时先按 pair key 替换 base edge，再构造 effective graph；不是在 base graph 上追加反向 edge。验证器逐平台注册并检查：

- base 中同一 pair 只能登记一个方向；
- override pair/platform 必须已审计，且恰为 base 方向的反向；
- audited 登记与实际 override 集合全等；
- effective graph 同 pair 最终只有一个方向并且无环。

唯一 override 为 1.21.10：

```text
base:       MobVillagerConversionMixin -> PhantomNeutralPhantomsMixin
1.21.10:   PhantomNeutralPhantomsMixin -> MobVillagerConversionMixin
```

生成结果实测 1.21.10 为 Phantom 在 Conversion 前，其余 10 个平台为 Conversion 在 Phantom 前。

## 5. Mixin 顺序证据与 verifier

依赖解析结果与本地 resolved artifact SHA-256：

| 平台范围 | resolved artifact | binary SHA-256 | sources SHA-256 |
|---|---|---|---|
| 1.21.x、26.1.2 | `net.fabricmc:sponge-mixin:0.17.0+mixin.0.8.7` | `E7889FCD185E4199052DCBCF0FE2128581CBF8630AEA7FEB37429667B1BA49A9` | `89F5EC3068C1AFCB3988C71F1E8A54C8DEECCF967023E0040FA21BB484253884` |
| 26.2 | `net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7` | `9E90EFEC71D2BAD5B96C9089F019D14A8603227D3C5F408D12F53FAE89D99D41` | `ABF52887326291AC76E3172A3A5EB425A7CC18DB6BA89C14348887B7B0DF10F3` |

对上述 resolved sources 的审计结果：`MixinConfig.prepareMixins()` 按 JSON 数组顺序创建 `MixinInfo`；`MixinInfo` 获得递增 intrinsic order，`compareTo()` 先比 priority 再比该 order；`MixinProcessor` 对单 target 以 `TreeSet<MixinInfo>` 排序。因此 migration proof 锁到实际解析 artifact，而不是 SpongePowered/Mixin 当前 master。

`verifyMixinConfigs` 现在逐平台检查：canonical schema/predicate/precedence、generated 单文件及确定性文本、runtime/sources JAR 唯一 config 与 generator 字节一致、entry ↔ runtime top-level Mixin class 双向全等、side/compatibility/fixed fields、`fabric.mod.json` 唯一引用、两层 resource srcDirs 不变，以及 root/11 个平台 runtime resource tree 不得出现手写 `*.mixins.json`。

`verifyJarEquivalence` 保留普通 class/resource 的原严格路径；Mixin JSON 字节不同时，只允许：固定字段完全相同、`mixins`/`client` 各自 membership 完全相同，并从实际 current runtime class 的 `@Mixin` annotation 提取 target/priority，所有同 target 且同 priority pair 的 baseline/current 相对顺序完全相同。未新增 wildcard、blanket exemption 或 baseline 重建。

## 6. 自动验证

| 验证 | 结果 |
|---|---|
| `gradlew clean build :1.21.11:test verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs verifyClassRenameMapping selfTestRenameEquivalence --stacktrace` | **BUILD SUCCESSFUL**；124 actionable tasks（90 executed、34 from cache）；11 个 clean 执行 |
| `:1.21.11:test` | 3 个 XML suite，46 tests，46 PASS，0 failure/error/skipped |
| 其余 10 个平台 `test` | 全部 `NO-SOURCE` |
| `verifyMixinConfigs` | 11/11 canonical/generated/runtime/sources/classes/fabric.mod 闭环通过 |
| `verifyJarEquivalence -PbaselineDir=D:/Project/Carpet-Ice-Addition-P6-baseline-final` | **11/11 PASS**；同 target/priority constrained pair 每平台动态为 14–17 对 |
| `gradlew projects` | root + 11 版本项目 = 12 projects |
| `:1.21.11:generateMixinConfig` 二次执行 | `UP-TO-DATE` |
| `git diff --check` | 通过 |

构建宿主的 Windows JDK AF_UNIX selector 存在项目配置前 loopback 故障；验证时仅通过系统临时目录中的 JVM agent 把 JDK selector 内部 pipe 切到其既有 TCP fallback。未修改仓库、`JAVA_HOME` 或 Gradle 配置，临时 agent 在任务结束前删除。

## 7. Level 3 人工测试步骤与验收标准

Codex 不启动 Minecraft。人工验收使用本次 `clean build` 生成的 11 个 runtime JAR，并为每个平台使用与其 `gradle.properties` 匹配的 Minecraft、Fabric Loader、Fabric API 与 Carpet 环境；每个平台使用隔离实例/存档，避免跨版本缓存污染。

1. **11 平台 dedicated server 加载**：依次覆盖 1.21.1、1.21.3、1.21.4、1.21.5、1.21.6、1.21.8、1.21.9、1.21.10、1.21.11、26.1.2、26.2。启动到可接受连接后正常停止。验收：无 config not found、JSON parse、Mixin class not found、target resolution、injection failure、invalid side 或 duplicate config 错误；日志显示模组正常初始化。
2. **11 平台 client 加载**：每个版本各启动一次安装本模组的客户端并进入测试世界/服务器。验收：无 client Mixin 加载错误；1.21.1–1.21.5 的两个 client entry 与其余版本的单 client entry 均不发生 class/target 缺失。
3. **注册面冒烟**：每个平台执行 `/carpet list`，确认 Ice 分类与预期规则可见；确认 `/killitem`、`/machineStatus` 命令树和 `/log villagerEvents` 订阅入口存在。验收：入口、权限与文本无缺失或异常。
4. **完整规则矩阵**：按 `refactor-acceptance-checklist.md` §3 对全部规则逐平台执行 false/default 与启用态测试；仅适用 1.21.1 的规则只在 1.21.1 验证，`mobsSpawnWithoutSpears` 在 1.21.11/26.x 验证，其余规则覆盖全部适用平台。验收：行为、反馈文本、持久化与重启后状态符合清单，无新增 Mixin 冲突。
5. **同 target 重点回归**：每个平台至少覆盖 villager events/trading/golem optimization、phantom warning/record world event、villager conversion/neutral phantoms、两项 silk-touch mining、illegal-text/machine-status rollback、ice-like-magma/safe-scaffolding、thorns/airborne penalty、custom-name trading/golem、neutral phantoms/tamed-mob protection；1.21.11/26.x 另覆盖 zombie villager events + mobs-without-spears。验收：组合开启时两侧功能同时有效，日志无 overwrite/redirect/inject 冲突。
6. **1.21.10 precedence 专项**：同时启用 villager conversion 与 neutral phantoms 相关规则，分别触发两条路径并进行保存/重载。验收：两项行为均正常，Phantom NBT 状态可保存/恢复，Mob conversion 无异常；日志无 cycle 或 Mixin apply error。相邻版本 1.21.9、1.21.11 做同场景对照。
7. **服务端-only 场景**：以未安装本模组的客户端连接安装本模组的 dedicated server，执行规则、命令与 logger 冒烟。验收：玩家可见文本正常，客户端不因缺少本模组资源或翻译键断开。
8. **发布前收口**：保存 11 平台的启动日志、版本/依赖清单和逐项结果；确认测试 JAR 文件名与发布预期一致。验收：11/11 server + 11/11 client 加载通过，适用规则矩阵无失败项，才可将 Phase 11 Level 3 标记完成。

## 8. 待人工确认项

- Phase 11 Level 3（§7）尚未执行。
- Phase 10 Level 3 文档仍标记“尚未执行”；本轮未收到可据以回填为通过的人工证据，因此未改写该历史状态。
