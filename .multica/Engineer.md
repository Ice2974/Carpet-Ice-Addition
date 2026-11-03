## 项目定位与必读资料

Carpet Ice Addition 是基于 Minecraft Carpet Mod 的 Carpet Extension，使用 Fabric Loader、Mixin、Loom 和 preprocess 构建体系。不要将其当作普通 Fabric Mod 进行架构重构。

开始任何项目任务前，必须阅读：

1. 仓库根目录 `AGENTS.md`。
2. 本次任务涉及的源码、配置、测试及 `docs/` 文档。
3. 与本次改动相关的构建脚本、版本配置和现有实现。

`AGENTS.md` 是具体目录边界、代码规范、Git / Worktree、构建验证命令和交付约束的权威来源。本文件只补充项目专项约束，不重复维护精确命令。项目本地 `.multica/` 配置默认不属于仓库交付内容，未经用户明确要求不得修改或提交。

## 项目原则

- **纯服务端兼容**：不要求客户端安装 Carpet Ice Addition。不得引入强制客户端握手、登录校验、客户端专属协议或必需资源。客户端增强只能是可选的，必须保证无 Mod 客户端正常连接和基本使用。
- **最小必要修改**：不做无关重构、格式化、依赖升级或构建体系调整。
- **多版本一致性**：默认覆盖 `settings.json` 中全部支持平台，除非正式任务明确限定范围。
- **行为稳定**：避免破坏 Carpet API、其他模组、存档、生命周期、状态清理、红石、生电机器、刷怪、区块加载和服务器性能。
- **证据优先**：以当前源码、构建配置、日志和测试为准；推断必须标明，未执行的验证不得声称通过。

## 多版本源码与构建架构

- `settings.json` 是支持 Minecraft 平台的唯一注册表；新增平台或检查覆盖范围时必须读取当前文件，不得记忆或猜测版本列表。
- `versions/mainProject` 声明核心平台版本。
- 根 `src/main/java` 是共享 Java 主源码树，按项目 preprocess 版本图变换；小范围差异使用既有宏，结构性差异使用平台 override。
- 根 `src/main/resources` 是共享资源主干。
- `versions/<MC 版本>` 存放平台 override、平台资源和版本配置。遵循现有 source set 和资源优先级。
- `gradle/mixins/registry.json` 是唯一人工维护的 Mixin registry，包含 membership、predicate 和 precedence；有效 Mixin 配置由构建生成。不得随意添加手写 Mixin 配置或绕开 registry。
- `common.gradle`、`build.gradle`、`settings.gradle` 和各版本 `gradle.properties` 共同构成项目构建体系。不得以普通 Fabric 模组模板替换或简化现有多版本架构。

以上为架构概览；任何具体修改必须先阅读 `AGENTS.md` 和相关构建配置，不能只凭本摘要操作。

## 功能开发专项要求

- 新增或调整规则、命令、logger 时，检查对应注册入口、翻译、语言文件、中英文文档及受影响平台 override。
- 玩家可见文本必须考虑纯服务端场景：原版客户端不一定加载本模组语言文件。涉及本模组自定义翻译键时，遵循项目现有服务端翻译工具和双重注册机制，不直接假设客户端可解析自定义翻译键。
- 高频 logger / 事件路径应先判断是否有订阅者或启用状态，再执行昂贵的文本构造、世界扫描或实体遍历。
- 需要修改 Mixin 时，检查 registry membership、predicate、precedence、生成配置及跨版本注入点差异。
- 需要第三方移植、参考或重写时，检查 `THIRD_PARTY_NOTICES.md` 和 README 致谢，不能擅自弱化许可证或来源声明。
- 配置位置应符合项目现有约定：世界 / 存档状态与全局配置分开；读取或写入失败必须有可诊断行为。

## 验证与交付

- 按 `AGENTS.md` 的分级验证要求执行适用的构建、测试、Mixin、资源、元数据及发布脚本校验。
- 代码级自动验证、自动运行时验证和人工游戏内验收必须分开记录。
- 环境允许时，执行多版本服务端冒烟测试；涉及客户端兼容性时，验证未安装本模组的客户端能否连接及使用基本功能。
- 无法自动验证的红石 / 生电机器、刷怪塔、多玩家和客户端交互应提供明确的人工验收步骤。
- 不得将编译成功等同于功能正确，也不得把未执行的测试标记为通过。
- 遵循 `AGENTS.md` 的 Worktree、提交和 PR 规范；提交信息使用项目要求的中文 Conventional Commits。