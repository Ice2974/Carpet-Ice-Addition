# 目标架构草案与迁移路线（Fallen-Breath 多版本架构）

> 本文档是迁移的目标设计与路线图，不随实现自动更新。基线数据见 [refactor-baseline.md](refactor-baseline.md)（下称"基线"），验收标准见 [refactor-acceptance-checklist.md](refactor-acceptance-checklist.md)（下称"验收清单"）。
>
> 参考实例：本地 `references/Carpet-AMS-Addition-master`（只读，不修改）是 Carpet TIS Addition（Fallen-Breath）多版本架构的同构实现，本文第 1 章以其为解剖对象。
>
> **最终目标（2026-09-05 锁定）：完整迁移到 Fallen-Breath 风格源码架构**——根 src 主源码树 + per-version override + preprocess `#if MC` 版本图 + 单一 Mojmap 命名空间。Phase 4（Mojmap 统一）完成后**不等于**完整迁移完成；剩余收敛相为 Phase 5（§6）——**已于 2026-09-05 执行并验收（执行结果见 §6 Phase 5 执行结果，验证记录见 [refactor-phase5-verification.md](refactor-phase5-verification.md)）**，完整迁移达成。

## 0. 总约束（全部阶段适用）

1. **不删除旧架构**：迁移期间 platform / shared 结构保留；任何删除只发生在替代物通过 Level 3 验收之后，且单独成 commit。
2. **每阶段独立可回滚**：Phase 1 只动构建脚本层，源码零改动，回滚 = revert 对应提交；Phase 3/4 每步均需先建对照基线再动手。
3. **依赖冻结**：不主动升级 Gradle（9.2.1）、JDK（21 / 25）、Loom（1.13.6 / 1.15.1）、loader、fabric-api、carpet、Mixin 体系。若某项升级成为硬性前置，先停下作为待人工确认项。
4. **旧 jar 行为对照基线留存**：Release 2.13.1 资产 + Phase 1 动工前本地全量构建快照，作为行为对照（存放位置见 §9 待确认项）。

### 0.1 Phase 1 专项约束（补充确认，2026-09-04）

1. 不改变任何 sourceSets 的顺序和覆盖关系（基线 §1.3 的叠加清单逐字等价搬移，"数据化"不等于"重排"）。
2. 不优化 shared 目录、不合并 Mixin、不修改 mappings。
3. common.gradle 迁移允许保留 platform 层薄包装，不要求第一阶段完全删除 platform build.gradle。
4. Phase 1 完成标准：除构建脚本组织变化外，以下内容必须与迁移前完全一致——
   - jar 内 class / resource 结构；
   - fabric.mod.json 语义；
   - mixin 配置行为；
   - rule / command / logger 注册行为。
5. **熔断条款**：如果发现必须修改源码才能完成 Phase 1，停止迁移，记录原因（写入 §9 待人工确认项），不自行扩大范围。

## 1. Fallen-Breath 多版本架构解剖

以 `references/Carpet-AMS-Addition-master` 为实例（与 Carpet TIS Addition 同构）：

| 机制 | 实例位置 | 说明 |
|---|---|---|
| 版本注册表 | `minecraftVersions.json`（TIS 为 `settings.json`） | `{"versions": [...]}` 升序数组，是支持版本的唯一清单 |
| 动态子项目 | `settings.gradle` | JsonSlurper 读注册表，逐版本 `include(":$version")`、`projectDir=versions/$version`、`buildFileName=../../common.gradle`，无硬编码平台列表 |
| per-version 属性 | `versions/<v>/gradle.properties` | minecraft_version、mcVersion 整数码（如 12108 / 260200）、yarn、carpet_core_version、carpet / minecraft 依赖区间、game_versions |
| 唯一构建脚本 | `common.gradle` | 每个版本子项目共用：按 `mcVersion >= 26_00_00` 切换 `fabric-loom`（免 remap）/ `fabric-loom-remap`；按 mcVersion 阈值推 Java 版本；mixin json 的 `/*JAVA_VERSION*/` 占位替换；env（BUILD_RELEASE / JITPACK）驱动版本后缀与产物命名；发布配置 |
| preprocess 版本图 | 根 `build.gradle` 的 `preprocess { createNode('1.21.8', 1_21_08, ''); a.link(b, file('versions/mapping-A-B.txt')) }` | 节点=MC 版本（整数码），边=相邻版本 + tiny 格式额外映射文件；插件 id `com.replaymod.preprocess` 经 JitPack 解析到 `com.github.Fallen-Breath:preprocessor`（实例锁定 commit `c5abb4fb12`） |
| 源码组织 | 根 `src/`（354 文件）+ `versions/<v>/src/` | 单一源码树面向最新版本，`//#if MC>=12111` 风格宏表达小差异（36 个文件使用）；`versions/1.21.8/src` 仅 4 个手写覆盖文件（宏与边映射无法表达的差异），覆盖文件内无宏 |
| mappings 策略 | `common.gradle` | 混淆版本统一 `officialMojangMappings()`（layered）；26.x 免混淆直接编译 |
| CI / 发布 | `.github/workflows/`（matrix_prep + 可复用 build + release） | 注册表驱动矩阵；发布逐版本读取 `versions/<v>/gradle.properties` 元数据，`Kir-Antipov/mc-publish` 上传 Modrinth + CurseForge |

## 2. A / B / C 评估

### A. 建议直接采用（Phase 1 范围）

| 设计要素 | 理由 |
|---|---|
| 版本注册表 JSON | 支持 11 个版本的唯一清单，替代 settings.gradle 硬编码 + 根 properties 前缀堆叠 + build.gradle platformProjects 三处双源 |
| settings.gradle 动态生成子项目 | 新增版本 = 注册表 + 一个目录 + 一份 per-version properties，不再复制 140 行 build.gradle |
| per-version gradle.properties | 版本数据归版本目录，根 properties 回归全局属性 |
| 唯一 common.gradle | 消灭 11 份克隆；发布命名逻辑（45 行）只写一份 |
| platform 薄包装（本项目过渡形态） | 比参考实现更保守：允许平台 build.gradle 保留为"声明差异 + 应用共通脚本"的薄文件，不强删（用户补充约束） |

### B. 延后评估 / 改造后采用（Phase 3+，均非承诺）

| 设计要素 | 评估 |
|---|---|
| preprocess 版本图 | ~~可选优化工具~~ **Phase 5 必经步骤（最终目标已锁定完整迁移）**。前提是单一 mappings 命名空间（Phase 4 已完成）；引入前需过供应链确认（JitPack commit 锁定的第三方插件）与 `THIRD_PARTY_NOTICES.md` 登记，门禁不通过则完整迁移状态 blocked 等待人工决策（见 §6 Phase 5） |
| `#if MC` 宏 | 仅限"小差异"（几行内的条件分支）。**硬约束：大型 Mixin / 行为差异必须继续用版本覆盖文件表达**，不强行塞进宏；覆盖文件是本项目长期保留的一等公民机制 |
| mixin json 统一管理 | 不强制收敛为一个 json。两个候选形态：方案 A——单一模板 + 占位符按版本生成（参考实现的 `/*JAVA_VERSION*/` 思路扩展）；方案 B——保持 11 份 + 新增一致性校验任务（条目类必须存在于该平台编译产物、无悬空条目）。**已决策（2026-09-04，P3-3）：采用方案 B**，`verifyMixinConfigs` 已落地并接入 CI（悬空 / 漏注双向 + 不变量断言，见 refactor-phase3-verification.md §5） |
| 矩阵 CI | 延后。现有单作业构建 + publish.yml 幂等管线保留，仅把其元数据来源从根 properties 前缀改为 per-version properties |
| 产物聚合任务 | 参考 `buildAndGather` 思路时，必须叠加（而非替换）现有 `verifyCraftableCoralBlocksJars` / `verifyFabricModJson` |

### C. 不建议采用

| 设计要素 | 理由 |
|---|---|
| JitPack SNAPSHOT 依赖（实例中 `fabric-loom-remap 1.15-SNAPSHOT`、`fabric-loom 1.17-SNAPSHOT`） | 与本项目"依赖锁定 + fail-fast"原则冲突；Phase 1 依赖冻结 |
| license 头插件（hierynomus） | 本项目无 license header 体系，纯增量负担 |
| 私有 maven 发布 | 本项目分发渠道是 GitHub Release + Modrinth，无自建 maven 需求 |
| 把 maven.modrinth 作为 carpet 主依赖源 | 保留 masa maven 优先；现状（mc12111 / mc262 已用 Modrinth maven）Phase 1 不动 |
| 把所有版本差异塞进 `#if` 宏 | 可读性与冲突处理成本不可控；覆盖文件机制必须保留 |

## 3. Carpet Extension 专项分析

### 3.1 Mixin 大版本差异：三层工具分工

| 差异类型 | 表达工具 | 本项目现状示例 |
|---|---|---|
| 标识符重命名（类 / 方法 / 字段改名） | 版本图边映射（mapping 文件） | `ItemUsage` ↔ `ItemUtils`、`WanderingTraderEntity` ↔ `WanderingTrader` |
| 小逻辑分支（几行差异） | `#if` 宏（若 Phase 3 引入） | 注入点参数增减、返回值微调 |
| 结构性分叉（注入目标、方法体、AI 任务拓扑不同） | 版本覆盖文件（永久机制） | `PhantomNeutralPhantomsMixin` 6 份分叉、`AcquirePoiIronGolemOptimizationMixin` 4 份（Phase 6 统一命名后的现名）、`KillItemCommand` 三实现 |

本项目 68 份 mixin 重复中，估计只有一部分能被前两层消化；结构性分叉（尤其 ironGolem 的 21 份 Yarn 分叉）在覆盖文件模型下依然是独立文件，只是存放位置从 17 个 shared 档位收敛为 per-version 覆盖目录。

### 3.2 Yarn mappings 差异：与 Fallen 构建体系的解耦论证

- **Phase 1 的四件事（注册表 / 动态子项目 / common.gradle / per-version properties）不依赖 mappings 统一**。Yarn（1.21.x）与 Mojmap（26.x）双源码树在 Phase 1 之后原样保留，全部收益（消灭克隆脚本、版本数据归位）照常获得。
- preprocess 的源码变换要求跨版本一致的命名空间做锚点，参考实现选择全系 Mojmap。因此：preprocess 评估归入 Phase 3，mappings 统一归入 Phase 4，两者都不阻塞 Phase 1 / 2。
- Yarn↔Mojmap 双胞胎（约 30 对）只有在 Phase 4 完成后才可能消除；在此之前是保留成本。

### 3.3 Carpet API 差异

- fabric-carpet 自身代码不混淆，`CarpetServer` / `SettingsManager` / `CarpetExtension` 等 API 在各 MC 版本间基本稳定，mappings 选择不影响 carpet API 调用面。
- 现有 `bridge/Mc<ver>Bridge` 薄桥接模式已覆盖残余差异，Phase 1 保留。
- carpet 依赖版本与依赖区间已是 per-version 数据（基线 §1.2），Phase 1 仅改存放位置。

### 3.4 结论

Fallen-Breath 构建体系（注册表 + 动态子项目 + common.gradle + per-version properties）与 mappings 选择、源码组织**正交**，可以先行单独迁移并独立验收；源码体系（档位 / 双胞胎 / preprocess）是另一条独立时间线。这正是本次路线把"构建架构迁移"与"源码体系迁移"拆开的依据。

## 4. 目标目录树

### 4.1 Phase 1 过渡形态（源码组织完全不动）

```
carpet-ice-addition/
├── settings.gradle            # 改：读版本注册表动态 include（指向现有 versions/platform-*）
├── settings.json              # 新增：版本注册表（schema 见 §5；文件名待人工确认）
├── build.gradle               # 根：保留两个验证任务；platformProjects 手工双源改为注册表↔目录一致性断言
├── common.gradle              # 新增：全部平台共通构建逻辑，平台差异全部由 per-version 数据驱动
├── gradle.properties          # 缩减：仅全局属性（mod_version、maven_group、archives_base_name、
│                               #        mod_id、mod_name、loader_version、jvmargs 等）
├── common/                    # 完全不动
└── versions/
    ├── platform-mc1211/       # … platform-mc262：目录名与源码完全不动
    │   ├── gradle.properties  # 新增：原 mc1211_* 前缀属性去前缀落地 + 新增数据键（§5）
    │   ├── build.gradle       # 薄包装（允许保留）：应用共通脚本 + 可选的平台差异覆盖
    │   └── src/…              # 不动（mixin json、fabric.mod.json、pack.mcmeta、资源包原位）
    └── shared/                # 完全不动：17 个档位、叠加顺序、内容一律保持
```

### 4.2 Phase 3 / 4 演进方向示意（非承诺，逐项过人工确认后才可能发生）

```
carpet-ice-addition/
├── src/                       # （若 Phase 3 通过）单一源码树，面向最新版本基线
├── versions/
│   ├── <mc 版本>/
│   │   ├── gradle.properties
│   │   └── src/…              # 覆盖文件：结构性分叉的永久归宿
│   └── mapping-*.txt          # （若引入 preprocess）版本图边映射
└── common.gradle / settings.json / …
```

### 4.3 数据流

注册表（settings.json）→ settings.gradle 动态生成子项目 → 子项目读自身 gradle.properties（版本数据 + 档位序列 + 插件选择）→ common.gradle 统一执行 → 产物 + 根验证任务。

## 5. 版本注册表与 per-version 属性 schema

### 5.1 settings.json

```json
{
  "versions": ["mc1211", "mc1213", "mc1214", "mc1215", "mc1216", "mc1218",
               "mc1219", "mc12110", "mc12111", "mc261", "mc262"]
}
```

- 升序排列（与参考实现一致，末位即最新）。
- 过渡期条目为平台目录名（`platform-` 前缀省略）；Phase 3 若目录改名为纯版本号，注册表同步改名即可。
- 文件名沿用 TIS 的 `settings.json` 还是参考实现的 `minecraftVersions.json`，待人工确认（见 §9）。

### 5.2 versions/platform-mc<XXXX>/gradle.properties 字段

原有 `mcXXXX_` 前缀属性去前缀落地，另加少量数据键驱动 common.gradle 分支：

| 字段 | 来源 / 示例（mc1211） |
|---|---|
| minecraft_version | 原 `mc1211_minecraft_version` = 1.21.1 |
| release_minecraft_range | 原 = 1.21~1.21.1 |
| minecraft_dependency | 原 = >=1.21 <=1.21.1 |
| yarn_mappings | 原 = 1.21.1+build.3（26.x 平台无此键） |
| loader_version | 原 `loader_version` / `mcXXXX_loader_version` 按平台落地 |
| fabric_version / fabric_dependency | 原 |
| carpet_version / carpet_dependency | 原（含 Modrinth maven 标记，见下） |
| carpet_repo | 数据化标记：masa 或 modrinth（现 mc12111 / mc262 用 Modrinth maven） |
| loom_plugin | `fabric-loom:1.13.6` 或 `net.fabricmc.fabric-loom:1.15.1`（冻结不动） |
| jar_task | `remap`（1.21.x，改 remapJar/remapSourcesJar）或 `plain`（26.x，改 jar/sourcesJar） |
| java_release | 21 或 25 |
| pack_format | 48 … 107 |
| mixin_config | carpet-ice-addition-mc1211.mixins.json |
| shared_tiers | 有序列表，逐字来自基线 §1.3（如 `mc121x,mc1211-12110,mc121x-killitem,mc1211-1218,mc1211-1215,mc1211-1214`）。**（Phase 1 形态；Phase 5 起该键已随 Java 档位机制移除，见 §6 Phase 5 执行结果）** |
| extra_resource_dirs | common 之外追加的资源目录（1.21.x 除 mc1211 为 `shared/mc1213-12111/src/main/resources`；其余为空） |

原则：**每个字段值都能在现有构建脚本 / 根 properties 中逐字找到出处**；不允许在迁移中"顺手修正"任何值（含已知的 `mod_version` vs `version` 引用不一致——那是 Phase 1 之后单独的清理项）。

## 6. 分阶段路线图

### Phase 1：Fallen-Breath 构建体系迁移

**范围（仅此四件）**：版本注册表、settings.gradle 动态生成版本模块、common.gradle 统一构建逻辑、per-version gradle.properties。

**硬性约束**：§0.1 全部五条（sourceSets 顺序与覆盖关系不动；不优化 shared、不合并 Mixin、不改 mappings；允许薄包装；四项完成标准；熔断条款）。

建议步骤（每步独立成 commit，随时可 revert）：

1. **P1-1 生成 per-version properties**：为 11 个平台生成 `versions/platform-*/gradle.properties`（值逐字搬自根 properties 与 build.gradle），此步不接线、不影响构建；人工抽查字段值。
2. **P1-2 新增注册表 + 动态 settings.gradle**：`settings.json` + 改写 settings.gradle 循环生成 include（projectDir 指向现有 platform 目录）；root build.gradle 的 `platformProjects` 断言改为"注册表 ↔ versions/platform-* 目录"一致性断言；跑 Level 1。
3. **P1-3 抽取 common.gradle（单平台试点）**：先接 mc1214（形态最简单之一：Yarn + remap + 完整档位），平台 build.gradle 缩为薄包装；跑 Level 1 + L1-6 内容级对照（仅该平台）。
4. **P1-4 扩展到全部平台**：逐平台接入（Yarn 形态 9 个 + 免混淆形态 2 个，两种形态都要显式验证）；每接 2–3 个跑一次 Level 1。
5. **P1-5 收尾**：根 gradle.properties 清理平台前缀键；11 份发布命名逻辑确认只剩 common.gradle 一份；全量 Level 1 + L1-6。
6. **P1-6 记录**：若中途触发熔断条款，恢复到上一步可回滚点，把原因写入本文 §9。

**回滚**：任一步 revert 即可；P1-1 / P1-2 阶段旧 include 删除前的并行对照期由 git 历史承担。

**退出条件**：Level 1 全绿（含 L1-5 / L1-7 人工项）+ L1-6 内容级等价通过 + §0.1-4 四项完成标准逐项确认。

### Phase 2：新架构编译与行为等价验证

- 逐平台 jar 内容级对照（验收清单 L1-6 方法：zip 条目清单、fabric.mod.json 语义、mixin json 字节、pack.mcmeta）。
- 对照基线：Phase 1 动工前的本地全量构建快照 + Release 2.13.1 资产（历史行为参考）。
- 执行验收清单 Level 1 + 2 + 3 全量。
- 产出：等价验证记录（哪些项自动比对通过、哪些人工项由谁在何时验收）。

### Phase 3：源码结构优化（再评估，非承诺）

每项候选独立立项，执行前需人工确认；仅列评估框架：

| 候选 | 收益 | 成本 / 风险 | 回滚 | 触发判据（建议） |
|---|---|---|---|---|
| preprocess 版本图 + `#if` 宏 | 窄档位分叉（如 6 份 Phantom）收敛为单文件 | 需 Phase 4 mappings 统一前置；第三方插件供应链 | revert（源码层独立提交） | 新增一个 MC 版本需手工复制 > 5 个文件；或双胞胎 / 分叉类漏改事故 ≥ 2 次 |
| shared 档位收敛（17 → 覆盖目录） | 档位叠加清单消失，每版本一个目录 | 大规模文件搬移，git 历史 | revert | 同上，或档位继续增殖 > 20 |
| Mixin 合并（Yarn 侧 1.21.x 内部） | 68 份重复下降 | 每次合并都要逐版本验证注入点 | revert | 分叉类实际内容 diff 归一后仍相同的比例可测定时 |
| 入口类去重（5 → 1 + 数据） | 注册逻辑单点维护 | 入口类承载平台差异（bridge / logger / command 变体选择） | revert | 新增平台需复制 ~95% 相同入口类时 |
| mixin json 统一管理（方案 A / B） | 防悬空条目、防漂移 | 方案 A 引入生成器，方案 B 引入校验任务 | revert | 出现一次 json 与类集合不一致事故时 |
| 翻译治理（三源 → 单源生成） | 消灭 JSON 与硬编码 Map 缺同步 | 涉及玩家可见文本约定（AGENTS.md） | revert | 出现一次缺键事故时 |
| 空档清理（mc1211-1219、mc12110-12111 等） | 目录卫生 | 极低 | revert | 任意时点，可作为独立小项 |

### Phase 3 执行结果（2026-09-04 ~ 09-05，决策记录）

| 候选 | 处置 | 依据 |
|---|---|---|
| preprocess 版本图 + `#if` 宏 | **未引入**（归 Phase 4 后） | 前置条件（mappings 统一 + 供应链确认）未变；适合宏化的家族已测定并存档 |
| shared 档位收敛（17 → 覆盖目录） | **未实施**，仅删除 2 空档（17→15） | Gradle sourceSet 无同 FQCN 遮蔽能力，完整形态依赖 preprocess；无 preprocess 时文件数不降反升 |
| Mixin 合并（Yarn 侧内部） | **限定实施**：mc1213/mc1214 三份注释等价副本入 mc1213-1214 档（P3-1）；26.x 纯命名重复链收敛入 mc26x（P3-2） | 仅合并实测非命名差异为 0 的副本；1 行 mappings 差异与结构性分叉全部保留 |
| 入口类去重（5 → 1 + 数据） | **部分实施**：mc261/mc262 入口并入 mc26x（P3-2）；Yarn 侧维持 3 份（LowVersion/HighVersion 注册差异） | Yarn 侧合并需引入注册拆分架构，超出最小修改原则 |
| mixin json 统一管理 | **方案 B 落地**：`verifyMixinConfigs` + CI 接入（P3-3） | 见 §2.B |
| 翻译治理（三源 → 单源生成） | **未实施** | 维持现状基线，出现缺键事故再立项 |
| 空档清理 | **完成**（P3-4，连同 Bridge ×11 死代码删除） | 全仓零引用实证 |

净效果：`versions/` 物理 java 276 → 257、唯一类名 164 → 147、档位 17 → 15；余下冗余为结构性分叉，归 Phase 4。完整记录见 refactor-phase3-verification.md。

### Phase 4：mappings 迁移（Mojmap 统一）——**已完成（2026-09-05）**

- ~~独立于 Fallen 架构迁移的独立决策；**不是任何前置条件**。~~ **已执行**：最终目标锁定后由人工决策直接启动。
- ~~触发判据（建议）：Yarn↔Mojmap 双胞胎（约 30 对）的漏改事故频发，或 preprocess 收益论证需要。~~ 实测权威口径为 **58 对**（P4-0 清单，基线"约 30"与 phase3"~45"为口径不明的手工估算）。
- 原成本量级预估"约 150 个 Yarn 源文件重写 + 全量 Level 3 回归"与实际相符（loom `migrateMappings` 自动迁移 + 人工修复两类系统性缺口）。
- 执行记录与验收见 [refactor-phase4-verification.md](refactor-phase4-verification.md)；净效果：11 平台统一 Mojmap 源码命名空间（9 个 1.21.x remap 平台经 `officialMojangMappings()`（layered）；mc261/mc262 为免混淆 plain 形态，不配置 mappings），Yarn 依赖彻底移除，零差异双胞胎（13 组入全平台档 `mojmap-unified` + 3 组档位内归一）与命令家族/入口类定向去重落地。

### Phase 5：preprocess 版本图 + 根 src + per-version override 收敛——**已完成（2026-09-05）**

- 范围：引入 `com.github.Fallen-Breath:preprocessor`（JitPack commit 锁定）版本图与 `#if MC` 宏；源码树从 shared 档位收敛为根 src（面向最新版本）+ `versions/<v>/src` 覆盖文件；结构性分叉保留覆盖文件形态。
- ~~**前置门禁**：preprocessor 供应链确认 + `THIRD_PARTY_NOTICES.md` 登记……门禁无法通过时整体状态 blocked~~ **已通过**（P5-1 落地：JitPack `com.github.Fallen-Breath:preprocessor` 锁定 commit `c5abb4fb12…` + `THIRD_PARTY_NOTICES.md` 登记）。
- 素材沉淀：`mojmap-unified` 档为根 src 雏形；1-3 行差异家族（P4-0 报告清单）为宏候选；1.21.11 的 24 文件 Mojmap 名漂移档为 override 主要对象。
- 宏使用硬约束沿用 §2.B（`#if` 宏）：大型 Mixin / 行为差异必须继续用覆盖文件表达；单处 ≤10 行、不改注入 descriptor。

**Phase 5 执行结果（2026-09-05，分支 `phase5-preprocess`，完整记录见 [refactor-phase5-verification.md](refactor-phase5-verification.md)）**：

- **架构形态**：Gradle 子项目扁平化为根直接子项目 `:platform-mcXXXX`（磁盘目录仍在 `versions/platform-*`）；`versions/mainProject` = `platform-mc12111`（core 原位编译根 src，本地 src 不参与 sourceSet）；根 `build.gradle` 持有版本图（11 节点 + 10 条链边 + `strictExtraMappings=true`）。
- **源码组织**：根 src 105 文件（1.21.11「main 态」+ `//#if MC` 宏 + `//$$` 非主流注释态）；per-version override 承担结构性分叉（mc1211 29 份、mc1213 / mc1214 各 10 份、mc1215 3 份、mc1216 / mc1218 各 1 份、mc261 / mc262 各 51 份，26.x 名称分叉族双份复制）；`versions/shared/` 仅保留纯资源档 `mc1213-12111`（Java 档与 `shared_tiers` 机制已于 P5-7 删除）。
- **mapping 策略实证**：1.21.x remap↔remap 9 条边全部 0 字节（automatic mapping 经 loom 双侧树自动合成承担全部 Mojmap rename）；仅 remap↔plain 的 mc12111↔mc261 边需要显式 legacy mapping（AMS 短式 2 行）；`//#disable-remap` 用于裸方法名歧义与 `player.level()` 同名多版本双重解析两族。
- **等价验证**：每次 flip 均全量 build + 4 verify + `verifyJarEquivalence` 对 P4-baseline-final 11/11 零适配零忽略；全仓 clean 冷重建通过；Level 3 人工回归通过（2026-09-05）。
- **Phase 6 执行结果（2026-09-05，main 分支，完整记录见 [refactor-phase6-verification.md](refactor-phase6-verification.md)）**：项目自有类名统一完成——47 条显式 mapping / 47 个分叉族（golem 16 含 P6-R1 补齐的 GoToWantedItem + 实体名 9 + 杂项 9 + BlockItem consolidation 1 + villagerevents 6 对 11 行 mapping）按 `@Mixin` 目标类最新 Mojmap 拼写统一 FQCN（mc26x 平台 P6-2～P6-5 零改动）；`versions/class-rename-mapping.txt` 显式 mapping 成为 `verifyJarEquivalence` 的唯一差异豁免通道（全部项目 class partner 内容级等价经 byte-identical / channel A 源码规范化 / channel B asm classfile 规范化证明，普通资源另按路径字节级比较，无 wildcard / 字面量豁免）；新增 `verifyClassRenameMapping` 与 `selfTestRenameEquivalence` 并接入 CI；Level 3 人工回归通过（2026-09-06 人工确认），Phase 6 验收完成，P6-baseline-final 已建立。

## 7. 风险登记册

| # | 风险 | 等级 | 缓解 |
|---|---|---|---|
| R1 | preprocessor 为 JitPack commit 锁定的第三方 Gradle 插件，供应链与停维护风险 | 中（Phase 3 才面对） | 定位为可选优化；引入前人工确认 + `THIRD_PARTY_NOTICES.md` 登记；不引入也完整可用 |
| R2 | `#if` 宏滥用导致单文件多版本逻辑不可读 | 中 | 硬约束：结构性分叉必须覆盖文件；Phase 3 若引入，先定义"小差异"量化上限（如 ≤ 10 行 / 处） |
| R3 | common.gradle 需兼容两套 loom 插件形态（remapJar vs jar、modImplementation vs implementation、mappings 有无） | 高（Phase 1 核心） | 数据键（loom_plugin / jar_task / yarn_mappings 有无）驱动；两种形态各选平台显式验证；冻结插件版本 |
| R4 | 注册表与目录 / 属性的双源漂移 | 中 | settings.gradle 内置"注册表 ↔ versions/platform-* 目录 ↔ 子项目"一致性断言（延续现有 projectsEvaluated 断言思路） |
| R5 | jar 字节级不可比（时间戳 / 条目顺序噪声）导致等价误判 | 中 | L1-6 采用清单级 + 语义级对照，不做字节级 |
| R6 | Gradle 9.2.1 动态 include 与 IDE（VSCode Buildship / IntelliJ）兼容性 | 中 | P1-3 单平台试点期由人工确认 IDE 导入正常；common.jar 固定命名策略保留 |
| R7 | 迁移窗口与并行开发冲突 | 中 | Phase 1 尽量单分支短窗口；P1-1/P1-2 可先合入（无行为影响） |
| R8 | Phase 1 发现"必须改源码才能等价"（熔断） | 低 | §0.1-5 熔断条款：停下、记录、升级为人工决策 |
| R9 | 发布流水线（publish.yml）依赖 settings.gradle grep 解析平台清单 | 中（Phase 1 联动） | settings.gradle 改造后同步核对 publish.yml 的平台解析与 mod_version 读取逻辑是否仍然成立（属构建脚本联动，允许改 publish.yml 的解析段，但不改其行为语义） |

## 8. 备选深度对比（决策记录）

| 深度 | 内容 | 状态 |
|---|---|---|
| 仅 Phase 1 + 2（构建统一即止） | 消灭 11 份克隆 + 版本数据归位 + 注册表驱动 | ~~天然合法停点~~ 已被执行并超越 |
| + Phase 3 | preprocess / 档位收敛 / Mixin 合并等源码体系优化 | **已完成**（2026-09-05，按项独立决策执行，见 §6 执行结果） |
| + Phase 4 | mappings 统一 → 消除 Yarn↔Mojmap 双胞胎 | ~~仅当双胞胎维护成本失控~~ **已完成**（2026-09-05，最终目标锁定后启动，见 §6 Phase 4） |
| + Phase 5 | preprocess 版本图 + 根 src + per-version override 收敛 | ~~最终目标收敛相；受供应链门禁约束，门禁不通过则完整迁移状态 **blocked** 等待人工决策~~ **已完成**（2026-09-05，分支 `phase5-preprocess`，见 §6 Phase 5 执行结果） |

## 9. 待人工确认项汇总

1. ~~**Phase 3 / Phase 4 触发**：是否执行、何时执行（建议判据见 §6 各表）。~~ **已关闭**：Phase 3（2026-09-04~09-05）与 Phase 4（2026-09-05）均已执行并验收。
2. ~~**preprocessor 供应链（Phase 5 门禁）**：JitPack commit 锁定第三方插件的接受度；`THIRD_PARTY_NOTICES.md` 登记内容（Fallen-Breath/preprocessor、TIS 架构来源致谢）。门禁结论必须显式记录……~~ **已关闭**：门禁通过（P5-1 落地全 SHA commit 锁定 + `THIRD_PARTY_NOTICES.md` 登记），Phase 5 已执行并验收（2026-09-05）。
3. **mixin json 统一管理形态**：~~方案 A（单模板生成）vs 方案 B（多份 + 校验任务），Phase 3 决策~~ **已决策：方案 B（2026-09-04，见 §6 执行结果与 §2.B）**。
4. **注册表文件名**：`settings.json`（TIS 命名）vs `minecraftVersions.json`（参考实现命名）。
5. **旧 jar 对照基线留存位置**：建议 Phase 1 动工前本地 `gradlew build` 产物复制到仓库外目录（或 `.minecraft/` 部署实例），Release 2.13.1 资产作历史参考。
6. **loom 插件选择的数据化形态**：`loom_plugin` 属性 + common.gradle 按 id apply（版本冻结），是否接受。
7. **publish.yml 联动**（R9）：settings.gradle 改造后其平台解析段需要同步调整（仅解析方式，不改行为），是否纳入 Phase 1 范围一并处理。
8. ~~**AGENTS.md 同步**：Phase 1 落地后，`AGENTS.md` 中涉及 settings.gradle / shared 档位 / gradle.properties 的协作规则需同步改写（本轮不动，列为 Phase 1 收尾待办）。~~ **已关闭**（2026-09-05，Phase 5 收尾 P5-8a 一并改写为根 src + preprocess 版本图 + per-version override 架构口径）。
