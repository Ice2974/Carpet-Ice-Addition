# AGENTS.md

本文件是给 Codex / Agent 使用的仓库工作规则，只记录协作方式、修改边界和验证要求，不维护项目当前状态、规则清单、版本清单或发布信息。

## 信息来源

- 项目事实以 `docs/` 和当前源码为准，`AGENTS.md` 只提供工作规则。
- 修改前先阅读本次任务相关的源码、配置和 `docs/` 文档，不要只根据文件名猜测实现。
- `references/` 是参考资料目录；只有任务明确要求参考第三方源码、外部资料或指定参考实现时才查看，不主动扫描、引用或修改。
- 如果 `AGENTS.md` 与 `docs/` 或源码冲突，以 `docs/` 和源码为准，并在回复中说明冲突。

## 基本原则

- 优先做最小必要修改，不做与任务无关的重构、格式化、依赖升级或发布配置调整。
- 不要根据 `AGENTS.md` 推断当前支持版本、规则数量或功能状态。
- 不能验证的内容要明确说明，不能伪装为已测试或已确认。
- 对实现范围、跨版本差异、第三方来源、兼容性或验证结果不确定时，写入回复的“待人工确认项”。
- 单次任务提示词只补充本次任务特有要求；未重复说明的通用约束仍以本文件为准。

## 目录边界

- `common/`：规则定义、翻译、公共配置管理，以及不依赖具体 Minecraft 版本签名差异的工具逻辑。
- `src/main/java`（仓库根）：主源码树——面向 mainProject（platform-mc12111）的「main 态」源码 + `//#if MC` 预处理宏，经 preprocess 版本图变换为各平台编译输入；只允许 java，不放 resources。
- `versions/shared/`：纯资源档目录（珊瑚配方资源包），不是独立 Gradle 子项目，不直接产出 jar；只能由平台模块通过 `extra_resource_dirs` 数据键引入，不再承载 Java 源码。
- `versions/platform-*`：per-version override 源码（同路径整文件替换根 src 变换输出 / 异路径附加）、平台资源（mixin json、fabric.mod.json、资源包副本）与 per-version `gradle.properties`。
- `docs/`：规则、命令、记录器、开发说明、发布说明等项目资料。
- `THIRD_PARTY_NOTICES.md`：第三方来源、许可证、致谢、移植 / 参考 / 重写说明的维护文件；不替代 `LICENSE`，也不作为项目功能状态来源。
- `references/`：本地参考资料目录，不作为项目事实来源，不修改其中内容。

## 固定项目标识

以下为项目长期固定命名，除非任务明确要求重命名，否则不要修改：

| 标识类型 | 固定值 |
| - | - |
| Display Name / 项目名称 | `Carpet Ice Addition` |
| Mod ID | `carpet-ice-addition` |
| Java Package | `com.ice2974.carpeticeaddition` |
| Maven Group | `com.ice2974` |
| Archives Base Name | `carpet-ice-addition`（平台 jar 附加 `-mcXXXX` 后缀） |
| 主入口类（各平台共用） | `com.ice2974.carpeticeaddition.CarpetIceAdditionMod` |
| 规则分类内部名 | `CarpetIceAddition`（对应翻译键 `carpet.category.CarpetIceAddition`，显示值 `Ice`） |
| 资源 namespace | `carpet-ice-addition`（assets 路径 `assets/carpet-ice-addition/`） |
| Mixin package | `com.ice2974.carpeticeaddition.mixins` |
| Mixin 配置命名 | `carpet-ice-addition-mcXXXX.mixins.json`（`mcXXXX` 为平台版本后缀） |
| GitHub 仓库 | `Ice2974/Carpet-Ice-Addition` |

约束：

- `gradle.properties` 中的 `mod_id`、`mod_name`、`maven_group`、`archives_base_name` 是上述标识的单一来源，修改时必须同步所有 `fabric.mod.json`、Java 常量、语言文件和 mixin 配置。
- 各平台 `CarpetIceAdditionMod.java` 中的 `MOD_ID` 和 `MOD_NAME` 常量必须与 `gradle.properties` 保持一致。
- `compatibilityLevel`（`JAVA_21` / `JAVA_25`）随平台版本变化，不视为固定标识，不要为统一而强行对齐。
- 规则分类内部名 `CarpetIceAddition` 与翻译键 `carpet.category.CarpetIceAddition` 绑定，修改一处必须同步另一处及语言文件。

## 根源码树与跨版本规则

- 源码架构（Phase 5 起）：根 `src/main/java` 主源码树（mainProject = platform-mc12111）+ 根 `build.gradle` preprocess 版本图（插件 `com.replaymod.preprocess` 经 JitPack 全 SHA 锁定解析到 Fallen-Breath/preprocessor）+ `versions/platform-*/src/main/java` per-version override；`versions/shared/` 只承载纯资源档。
- 根 src 必须保持「main 态」：纯文本可直接按 1.21.11 编译；所有非 1.21.11 内容必须以 `//$$ ` 前缀的注释态出现在条件分支内；预处理指令行（`//#if` / `//#elseif` / `//#else` / `//#endif` / `//#disable-remap` 等）不加 `$$` 前缀。
- 宏只用于单处 ≤10 行、不改 Mixin 注入 descriptor 的小差异；结构性分叉（注入目标结构 / 方法签名 / AI 拓扑 / `@At` 字符串差异）必须用平台 override 文件表达，不强行塞进宏。
- override 语义：非 core 平台的本地 `src/main/java` 按同路径整文件替换根 src 变换输出、异路径附加；override 文件直接编译，不参与宏求值与边重映射，内部不使用预处理指令；core（platform-mc12111）没有 override 层，本地 src 不参与编译。
- 版本图边 mapping 文件（`versions/mapping-<a>-<b>.txt`）只在 automatic mapping 无法表达时添加条目（外部库 rename、成员移动、remap↔plain 边）；1.21.x remap 边通常保持 0 字节。修改 mapping 后必须全量编译验证受影响平台。
- 严禁把根 src 或 `versions/shared/` 注册为独立 Gradle 子项目；Java source root 必须停在标准的 src/main/java，禁止把 com/... package 目录直接注册为 source root。
- 跨小版本差异优先通过宏、平台 override 或版本图 mapping 表达，不优先使用运行期字符串版本号判断来偷渡兼容。
- 依赖版本不匹配时，优先依赖 `fabric.mod.json` 的 `depends` 在启动阶段 fail-fast，不做运行期静默降级。
- 平台模块的 mixins 配置默认保持 `required=true`，除非任务明确要求调整。
- `org.gradle.parallel=false` 是 preprocess 体系下 Gradle 9 跨项目解析独占锁问题的既有解，未经全量验证不要改回 `true`。

## 版本注册表与构建配置

- `settings.json` 是支持版本清单的唯一来源；`settings.gradle` 据此动态生成平台子项目（根直接子项目 `:platform-mcXXXX`，磁盘目录仍为 `versions/platform-*`），并断言注册表与 `versions/platform-*` 目录一致。
- 新增 Minecraft 平台 = 在 `settings.json` 按版本升序登记 + 新建 `versions/platform-*/`（per-version `gradle.properties`、src）+ 根 `build.gradle` 版本图 `createNode` 登记节点并与相邻版本 `link`（含新建对应 `versions/mapping-*.txt`），不要手写 include 清单；平台不写独立 `build.gradle`，`settings.gradle` 按 per-version `gradle.properties` 的 `loom_plugin` 完整 `id:version` 自动选择共享 family 构建入口（`build-remap.gradle` / `build-plain.gradle`，未知值 fail closed）。
- 平台版本数据（minecraft、loader、fabric-api、carpet、pack_format 等）放在 `versions/platform-*/gradle.properties`，不要向根 `gradle.properties` 回填平台前缀键。
- 平台共通构建逻辑在根 `common.gradle`（由共享 family 构建入口在 plugins 块之后 apply from 引入），差异由 per-version 数据键驱动；修改共通逻辑时必须同时验证两种 loom 形态（Mojmap layered remap 与免混淆 plain）。

## 规则 / 命令 / 记录器修改

新增或调整功能时，按类型同步检查：

| 类型 | 至少检查 | 必须同步文档 |
| - | - | - |
| 规则 | 规则定义类、翻译提供类、中英文语言文件、相关 Mixin / Helper / 配置类、入口类（根 src 注册宏边界）、mixin json、受影响平台 override | `docs/rules.md`、`docs/rules_en.md` |
| 命令 | 命令注册入口、命令实现类、权限判断、命令树刷新逻辑、反馈/错误文本、配置持久化逻辑 | `docs/commands.md`、`docs/commands_en.md` |
| 记录器 | `registerLoggers()` 接入点、logger 注册/显示/辅助类、事件触发点、Mixin、HUD 更新入口、内部名、默认 option、可选 options、订阅状态快速判断 | `docs/loggers.md`、`docs/loggers_en.md` |

约定：

- 中文规则名使用 `.name`；中英文规则介绍使用 `.desc`；英文规则名默认直接显示内部名，不额外新增英文 `.name`。
- 游戏内规则介绍（`.desc`）句末不带句号；`docs/` 文档中的规则介绍句末需要句号。
- 规则文档中的规则介绍需要与游戏内规则介绍保持一致；来源说明需要中英文同步保留或同步调整。
- 新增或调整带有第三方来源说明的规则、命令或记录器时，需要同步检查 `THIRD_PARTY_NOTICES.md`，不要只修改 `docs/` 文档。
- 命令文档顶部应包含 `命令列表` / `Command List`，新增或调整命令时同时更新列表和对应命令详情。
- logger 指 Carpet `/log` 体系下由玩家订阅的游戏内信息输出，不是 SLF4J / Log4j 服务端日志；不要新增独立的 `/log` 命令。
- logger 可以依赖规则，但不要默认把“规则开关”和“logger 订阅”混为一件事；如存在联动，需在文档中说明。
- 高频 logger 或高频事件路径必须先判断是否存在订阅者或对应加速字段，再组装文本、扫描世界或遍历实体。

## 玩家可见文本

本项目按纯服务端模组使用场景处理玩家可见文本；玩家客户端可能未安装本模组，因此不能依赖客户端加载本模组语言文件。

- 面向玩家的命令反馈、错误提示、规则触发提示、回档警告、记录器输出等，如果使用本模组自定义翻译键，不要直接向玩家发送 `Text.translatable(...)` 或 `Component.translatable(...)`。
- 服务端应先通过项目既有服务端翻译工具解析翻译键，再发送 literal / plain 文本给客户端；优先复用 `common/src/main/java/com/ice2974/carpeticeaddition/translation/TranslationFormatUtil.java` 或同目录既有工具。
- 中英文语言文件仍是玩家可见文本来源，不要把长文本硬编码到命令类、Mixin 或平台专属实现中。
- 需要颜色、点击事件、hover 文本等样式时，先在服务端完成翻译和参数格式化，再在对应版本文本组件上应用样式。
- 只有 vanilla / Minecraft 客户端必然存在的翻译键，才可以考虑继续使用客户端侧 translatable；本模组自定义键默认走服务端翻译。

## 许可证与第三方来源声明

- `LICENSE` 只描述本项目自身授权；`THIRD_PARTY_NOTICES.md` 用于维护第三方项目来源、许可证、致谢，以及“参考 / 移植 / 重写”说明。
- 新增、调整或删除从第三方项目参考、移植、重写的规则、命令、记录器、Mixin、Helper、配方或配置逻辑时，必须同步检查 `THIRD_PARTY_NOTICES.md` 是否需要更新。
- 代码、语言文件、`docs/`、`README*` 与 `THIRD_PARTY_NOTICES.md` 中的第三方来源表述必须一致；涉及中英文文档时，需要同步更新中英文描述。
- 修改第三方来源说明时，不要擅自弱化许可证信息，不要把“复制 / 改写 / 移植”改写成“灵感参考”，除非已完成相应 clean-room 重写或有明确审查结论。
- 若移除第三方派生实现、完成独立重写或更换实现方案，需要同步调整 `THIRD_PARTY_NOTICES.md` 和相关规则文档中的来源说明，保留必要致谢。
- 若无法确认来源许可证、派生关系、NOTICE 是否足够或是否需要保留原许可证文本，不要删除来源声明，写入“待人工确认项”。

## 配置、文档与版本号

- 运行时配置应按作用范围选择存放位置：只对当前存档生效、内容依赖世界状态或坐标的配置，放在当前世界 / 存档目录下；全局性、通用性、与具体存档无关的配置，放在全局 `config/` 目录下。
- 配置读取失败时记录日志并回退默认值；写入失败时记录日志，并向命令执行者返回明确错误。
- 文档只维护当前状态，不追加无关历史流水账；新增或调整内容的格式应与旧内容一致。
- 修改 Mod 版本号时，需要更新 `gradle.properties`
- 旧版本号若属于历史记录、示例、兼容范围或 release notes，且不能确定是否应替换，不要擅自改写，写入“待人工确认项”。

## 构建与验证

- 默认使用仓库内 Gradle Wrapper 执行编译、构建和验证；Windows 下优先使用 `.\gradlew.bat`。
- Java 版本由 Gradle Java Toolchain 自动选择，不要在任务中手动切换 `JAVA_HOME`，也不要把本机 JDK 绝对路径写入仓库级 `gradle.properties`、`build.gradle` 或其他提交文件。
- 本机已通过用户级 Gradle 配置提供本地 JDK。构建前如需确认 Java Toolchain 状态，可运行：

```powershell
.\gradlew.bat -q javaToolchains
```

- 只修改 Markdown 文档时，至少运行 `git diff --check`。
- 修改源码、资源、构建脚本、Mixin、平台入口或 sourceSets 时，先运行 `git diff --check`，再运行受影响模块的 `compileJava`。
- 涉及 `common/`、根 `src/`、`versions/shared/` 或跨平台公共逻辑时，优先验证所有当前支持平台的编译。
- 如果因环境限制无法运行验证命令，需要在回复中明确说明未验证内容、原因和风险。
- 不要把未运行的游戏内测试、多人测试或启动测试写成已通过。

## 日志与测试边界

- 服务端日志不要刷屏，高频路径不要每 tick 输出。
- 用户操作失败、配置读取失败、配置写入失败、命令执行失败、兼容性阻断等关键路径可以记录 debug / warn。
- 不要在正常玩家操作中大量输出 info。
- 不要把玩家隐私、服务器敏感路径或无关环境信息写入日志。
- Agent 可以编译项目、生成测试清单、补充排查日志，但不负责最终人工游戏内验收。

## 默认不要修改

除非任务明确要求，默认不要修改：

- `README.md`
- `README_en.md`
- `.gitignore`
- `LICENSE`
- `THIRD_PARTY_NOTICES.md`（但任务涉及许可证、第三方来源、移植 / 参考说明或 clean-room 重写时，需要按维护约定同步检查）
- `references/`
- `AGENTS.md`
- 发布平台元数据
- 与当前任务无关的构建脚本

## 回复要求

完成任务后，回复中说明：

- 修改了哪些文件
- 为什么这样改
- 运行了哪些验证命令
- 哪些内容未验证，以及原因
- 待人工确认项；如果没有，就写“无”
- 是否发现与 `docs/` 或现有源码不一致的地方
