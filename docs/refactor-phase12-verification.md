# Phase 12：最终架构审计与方案 C 实施记录

> 状态：发布解析与文档收口已实施；最终验收尚未关闭。主工作区与 sterile 的 Gradle 调用在项目配置前被本机 JDK loopback 故障阻断，不能将历史 P6 11/11 或 Phase 11 的测试结果写成本轮新验证。CI 与最终验证状态见 §6。

## 1. 基点与最终架构

审计基点为 `main @ 46c9df6700e66040c7b19991c804cc916586974a`，工作区干净；[Build #63](https://github.com/Ice2974/Carpet-Ice-Addition/actions/runs/34136835341) 对应同一 SHA，success。用户已确认 Phase 11 Level 3 完成；不补造未提供的原始日志。

| 项目 | 源码事实 |
|---|---|
| registry / projects | `settings.json` 的 11 个真实版本；root + 11 直接子项目，无 alias；目录集合双向校验 |
| mainProject / graph | `versions/mainProject = 1.21.11`；11 nodes / 10 edges，`strictExtraMappings=true` |
| Java | 根 `src/main/java` 唯一跨版本 owner；non-core 本地 `versions/<v>/src/main/java` 为 override/addition；core 无本地 main Java |
| resources | `[versions/<v>/src/main/resources, src/main/resources]`；EXCLUDE + collision 集合断言，只有 1.21.1 的 10 个已登记配方覆盖 |
| Mixin | canonical `gradle/mixins/registry.json`；support `gradle/mixin-registry.gradle`；common 注册 generator，输出 `build/generated/mixinConfig/<mixin_config>`，显式 task output 接线 |
| tests | `:1.21.11:test`；3 suites / 46 tests；其余 10 test 本地源码为空 |
| Loom / Gradle | `fabric-loom:1.13.6` / `net.fabricmc.fabric-loom:1.15.1`；Wrapper 9.2.1；parallel=false |
| preprocessor | `Fallen-Breath/preprocessor@c5abb4fb12aad2590c852c1bc6c8d5758606ec0b` |
| mappings | 47 class rename；10 graph mapping 文件（9 空，1.21.11→26.1.2 两条成员映射） |
| baseline | `D:/Project/Carpet-Ice-Addition-P6-baseline-final`，11 平台 flat legacy 目录，永久只读 |

无 `common/`、`versions/shared/`、第三 shared tier、活动 `extra_resource_dirs` / `preprocess_enabled` / `shared_tiers`、per-version thin build.gradle 或 tracked `*.mixins.json`。生成配置不在 resource srcDirs。现有六个 ext/support 数据项均找到消费者，没有确认可删除的自定义 property/task。

Fallen-Breath 一致性：root/preprocess/real-version/override/shared resources 属于 Equivalent；双 Loom family、完整 SHA、严格 mapping、canonical Mixin registry、碰撞及 artifact verifiers 属于 Project-specific intentional difference；runtime filename、baseline resolver、historical publish fallback 属于 Legacy compatibility retained intentionally。当前态旧文档属于 Stale，已纠正；第三方来源法律充分性不作自动推断。

## 2. Legacy 与长期消费者

| 候选 | 定义 → 消费者 | 裁决 |
|---|---|---|
| `legacyBaselineDirNames` / `platform-mcXXXX` | root build → P6 runtime/sources baseline 查找 | 长期保留，非 dead metadata |
| `mixin_config` | per-version properties → generator output/input、fabric/Mixin verifiers；legacy publish 分支 | runtime compatibility identity，正式长期保留 |
| `carpet-ice-addition-mcXXXX.mixins.json` | generated/runtime/sources → Fabric Loader、P6/P11 verifier | 不改名；不是当前 project/source/preprocess/publish 平台标识 |
| historical registry `mcXXXX` | 旧 tag settings → legacy layout → 旧 JAR Mixin-code 归属 | 显式历史兼容分支，保留 |
| common/shared/Phase 标记 | 历史文档、ownership 证明、迁移出处注释 | 不是活动源码层；不按 grep 删除 |
| `bridge/Mc<ver>Bridge` | target 历史描述，当前实现已不存在 | 修正文档当前态含义 |
| `MIXIN_CONFIG_PATH`、TODO、FIXME | 自有 tracked 范围未发现对应清理对象 | 不制造清理 commit |

Java 中 Minecraft descriptor 的 `common/` 片段不是仓库目录。settings 是唯一平台 registry；graph、baseline map、collision map 与 predicates 是拓扑/验证/差异数据，不是第二个平台注册来源。没有删除任何 verifier：P6 rename/channel A/B、P9 27 owners / 42 entries 与 scoped canonicalizer、P10-R1 directory-entry 过滤、P11 target/priority-aware 顺序等价保持。

## 3. Helper / Bridge / Compatibility inventory

下列具名候选均有直接 Java 消费者；没有 Gradle 单独打包或反射加载这些 Helper 类型的旁路。现有 runtime JAR 中除两个 PvP helper 的互斥版本范围外，13 个具名候选均存在于全部平台。

| 候选 | owner → 主要消费者 / 职责 |
|---|---|
| BotTabListNameHelper | root → 入口设置回调、TAB Mixin；BOT 名称构造/刷新 |
| RealPlayerHelper | root → BOT/TAB/thorns/scaffolding；真实/假人判定 |
| CrafterOutputBlockHelper | root → CrafterBlockMixin；输出容纳能力 |
| IceLikeMagmaBlocksHelper | root → PlayerBreakIceLikeMagmaBlocksMixin；规则判定 |
| ItemFrameInteractionHelper | root → 各版本 ItemFrameMixin；交互、付费状态、退款 |
| CustomEndPlatformPositionHelper | root → EndPlatformSettings、EndPortal Mixins；坐标解析和失败处理 |
| CraftableCoralBlocksRecipeBookHelper | root + 1.21.1/26.x override → ConflictDetector/DataPackController；配方书同步和真实 API 差异 |
| PhantomSpawnWarningHelper | root + 26.x override → warning Mixin；gamerule 宏及 Carpet 加速反射探测 |
| LegacyPvpRuleHelper | root `MC<12105` → 1.21.1/1.21.3/1.21.4 tame-mob Mixins；旧 server API |
| PvpRuleHelper | root `MC>=12105` → 其余 8 平台 tame-mob Mixin；多版本 PvP API 宏 |
| VillagerTradingOptimizationRuleHelper | root → 入口设置回调；刷新村民 Brain |
| FeatureCompatibilityReporter | root → 各入口的功能异常路径；去重与安全跳过 |
| VillagerEventsCompatibility | root → logger runtime/identity/conversion/lightning/zombie/villager Mixins；错误隔离及会话节流 |

职责型桥接也保留：CraftableCoralCraftingRefresher/Dispatcher 由 RecipeBookHelper 和 CraftingMenu Mixin 消费；IronGolem Access/SkipMarked/Hooks 由 AI Mixins 消费；TradingOptimizationAccess 连接 trading/sensor/brain 与 RuleHelper；NeutralPhantomsRetaliationTracker 连接 LivingEntity/Phantom；VillagerEventState/ConversionScope121 支撑转换观察；ServerCommonNetworkHandlerAccessor 被 rollback warning Mixin 使用且在 canonical 登记。KillItemCommand 的 click/hover 反射 bindings 支撑旧 class/新 interface；VanillaLanguageService 的版本字符串校验与缓存 fallback 是 runtime 职责，不是 build registry。

没有已证实可删的迁移 Helper。TickSpeed 可选反射名称逐一命中情况未完成依赖级审查；保留而不猜测删除。删除上述机制可能改变游戏行为，另立任务。

## 4. 方案比较与最终选择

用户选择并收紧方案 C：仅解耦 actual publish identity。方案 A 能最小化修改但保留发布耦合；方案 B 会扩大 runtime path/fabric/verifier 的迁移证明和 Level 3 范围，本阶段不实施。

生产实现内嵌 `publish.yml`，不依赖 checkout 后的历史树存在新脚本：

1. `resolve_platform_layout` 验证 settings 非空/唯一/格式，明确判定 actual 或 legacy；混合、未知或错误直接失败。
2. actual 使用 entry→真实目录→`release_minecraft_range` + `mod_version`→精确 filename，不推导人工 platform code，也不消费任何 Mixin 字段。
3. legacy 才读取 `mixin_config` 与 JAR fabric mixins 的 mcXXXX；actual 错误不能进入 fallback。
4. `select_platforms` 保持 trim、去空、去重、未知拒绝及请求顺序。
5. `attribute_asset`/`collect_assets` 验证 release 的真实 build/libs 和 dispatch 的目标 Release assets；actual 精确命名双向对应，id/version/minecraft dependency 匹配，候选唯一，sources 排除。选中平台必须齐全；scope 外缺资产不阻断子集。
6. Bash 下游只消费已验证的 `platform_dir` / `jar`；manifest/platform matrix 不再重新解析 Mixin 来识别当前平台。

label 是 common.gradle 既有命名的派生视图；生产 release 检查真实 Gradle 输出、dispatch 检查真实资产，拒绝漂移/冲突，不增加手写 actual→mcXXXX 表。runtime config filename 与 `mixin_config` 是最终长期例外，没有后续改名 TODO。

生产 marker 各恰一对；harness 对缺失、重复、逆序 fail closed。测试机械提取整个 resolver，包含 layout、selection、dispatch attribution、release collection 和 CLI；另外提取既有 range expansion，不复制生产实现。`scripts/fixtures/publish-3.0.0.json` 是 tag 3.0.0 / `7395a9eae8ed616ffa6f6f5a2d9998efcb0680b2` 的原始 settings/properties 测试快照，不是第二 registry；历史资产测试为合成 JAR，不冒称真实 dispatch E2E。

## 5. 提交边界与长期门禁

- P12-A 跳过：没有有意义的 dead Java/property/task cleanup。
- P12-B：`b8cc9d4`，resolver + harness + Build 中调用 harness；不包含 release Gradle gate alignment。
- P12 gate alignment：`6664f94`，独立提交，前置证明为 Build #64 全新 Ubuntu checkout 成功；不包含 resolver 改动，不接入 P6 baseline。
- P12-C：AGENTS、target、acceptance、Phase 12 记录，以及 Phase 3/9 历史待办的完成指向；不重写历史事实。
- P12-D：仅按新验证证据更新结果；有阻断时不宣布完成。

Build workflow 显式执行 build、core test、coral/fabric/Mixin/rename/self-test，加生产 resolver harness。当前 release 显式 Gradle 命令与 Build 相同；dispatch 继续只读取既有 Release 资产，不执行 Gradle；历史 release 事件仍运行 tag 自己的 workflow。P6 equivalence、projects、sterile 仍是本地门禁，不能说 CI 已覆盖。docs-only push 被忽略，最终 HEAD 需手动 Build。verifyJarEquivalence 读取 sources，因此先 build；本阶段不修改它的 wiring。

## 6. 本轮验证证据与限制

| 验证 | 当前结果 |
|---|---|
| `python scripts/verify_publish_resolver.py` | 16 tests PASS；生产模块/CLI、actual 全平台、legacy fixture、marker 负例、selection、metadata、命名冲突、缺失/多资产、sources 排除、range expansion；现有真实 Gradle JAR filename/metadata 11/11 PASS |
| workflow YAML parse + Bash `-n` | 两个 workflow 的 run steps 通过；表达式在语法检查时替换为占位 token，不执行外部发布 |
| `git diff --check` | 通过 |
| `.\gradlew.bat -q projects` | 配置前失败：`Unable to establish loopback connection` |
| 主工作区全套 clean build / tests / verifiers / P6 / projects | 已调用组合命令；在同一配置前故障失败，任务结果不得记 PASS |
| sterile 检出 `b8cc9d4` | 新 detached worktree，Git 干净，未复制 build 输出；同一 Gradle 组合命令在配置前失败 |
| P6 baseline | 保持只读；本轮没有新的完整 11/11 verifier 结果 |
| [GitHub Build #64](https://github.com/Ice2974/Carpet-Ice-Addition/actions/runs/34184226519) @ `b8cc9d4` | **success**；新 Ubuntu checkout 全平台 build/core test、coral/fabric/Mixin/rename/self-test、16 个 harness tests 与真实 Gradle filename/metadata 11/11 通过 |

### release gate alignment 的独立性证明

Build #64 日志先 `git init` 新工作目录，随后以与 release 相同的 JDK 25 + Gradle setup 执行完整命令；没有 `baselineDir` 参数或本机目录。标准依赖/Gradle cache 不作为未跟踪源码输入；该验证不声称关闭本地 sterile/P6 门禁。

| 新增显式 gate | 代码输入/依赖 | Build #64 证据 |
|---|---|---|
| `:1.21.11:test` | core 本地测试源、JUnit 与本次构建 classpath | `> Task :1.21.11:test` 执行成功，其他 10 平台 NO-SOURCE |
| `verifyMixinConfigs` | canonical、generator、runtime/sources task outputs、fabric metadata | 11/11 闭环 OK |
| `verifyClassRenameMapping` | tracked root/per-version Java、canonical、功能性文本与 rename mapping | 明确打印 OK |
| `selfTestRenameEquivalence` | 仓库 mapping、自建样例、构建依赖提供的 ASM | 明确打印 OK，不读取外部 baseline |

以上门禁已有 Build CI 使用，Phase 12 只将同一命令接入当前 release；`verifyJarEquivalence` 的 baseline 读取只在其自身 doLast 中，不被上述 gate 调用。

全套尝试命令（主工作区及 sterile 相同）：

```powershell
.\gradlew.bat clean build :1.21.11:test verifyCraftableCoralBlocksJars verifyFabricModJson verifyMixinConfigs verifyClassRenameMapping selfTestRenameEquivalence verifyJarEquivalence -PbaselineDir=D:/Project/Carpet-Ice-Addition-P6-baseline-final projects --stacktrace
```

异常链为 `SocketConnection → WEPollSelectorImpl → PipeImpl → UnixDomainSockets.connect0: Invalid argument: connect`。一次临时 IPv4 启动选项和一次 no-daemon/空 JVM args 诊断均未消除故障；未修改 JAVA_HOME、依赖、仓库 Gradle 配置或重建临时 JVM agent。日志保存在本机临时目录 `cia-p12-verification`；不能把 CI build 代替此处未完成的 sterile/P6 验收。

审计阶段对已有 artifact 的只读观察：11 runtime/sources 各恰一 config，generated/runtime 引用和字节匹配；与 P6 比较，实际文件集合 11/11 一致、1.21.x class 字节一致、26.x 各 42 class 字节不同，资源差异仅 Mixin JSON。这不是本轮完整 verifier 执行。已有 XML 为 33+8+5=46 PASS；同样不冒称本轮新运行。

## 7. 人工验收、来源声明与完成条件

方案 C 不修改 Java、resources、fabric reference、generator、Gradle verifier 或依赖。满足 runtime 文件内容不变、P6 11/11、最终 CI 通过、bootstrap path 不变后，继承用户已确认的 Phase 11 Level 3；不机械重做 11 平台游戏测试。当前本地/sterile 验证阻断未关闭，尚不宣布 Phase 1–12 全部验收完成。

THIRD_PARTY_NOTICES 已覆盖固定 preprocessor SHA/GPL-3.0、TIS 架构参考与 AMS 实例；本阶段不引入第三方派生 Java，无改 LICENSE 的依据。原有功能来源/许可证声明不删除、不弱化。未完成逐行 clean-room/法律充分性审查，不声称已证明所有派生关系。

待人工确认/外部状态项：

- 本机 JDK loopback 环境恢复后，主工作区与最终 tracked-only 检出复跑全套 Gradle/P6/projects，补新鲜结果；禁止放宽 verifier 或重建 baseline 来通过。
- 真实 Publish dispatch 是否执行由用户发布决策；静态 harness 不等于外部发布 E2E。缺少 registry 的更早 tag 不在本次兼容承诺内。
- 可选反射 API 命中审查、第三方来源法律审查另立任务，不通过 Phase 12 自动删除代码/声明。

关闭条件：11/12 projects 与两层 ownership、graph/dependencies 冻结、canonical/generated/JAR 闭环、actual identity 解耦与 legacy 显式分支、全部既有 verifier 和新 harness、主工作区/sterile P6 11/11、46 tests 与 non-core NO-SOURCE、最终 HEAD CI、新鲜验证记录及 Level 3 继承条件均具备证据。
