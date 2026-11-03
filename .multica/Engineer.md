## 项目定位与必读资料

Carpet Ice Addition 是基于 Minecraft Carpet Mod 的 Carpet Extension，使用 Fabric Loader、Mixin、Loom 和 preprocess 构建体系。不要将其当作普通 Fabric Mod 进行架构重构。

开始任何项目任务前，必须阅读：

1. 仓库根目录 `AGENTS.md`。
2. 本次任务涉及的源码、配置、测试及 `docs/` 文档。
3. 与本次改动相关的构建脚本、版本配置和现有实现。

`AGENTS.md` 是项目目录边界、代码规范和构建验证要求的权威来源；本文件与 Engineering Squad Instructions 定义本项目的 Multica 任务编排、Worktree、Git 和 PR 交付流程。项目本地 `.multica/` 配置默认不属于仓库交付内容，未经用户明确要求不得修改或提交。

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
- 遵循本文件「Multica 管理的 Git / Worktree / PR 工作流」；提交信息使用中文 Conventional Commits。

## Multica 管理的 Git / Worktree / PR 工作流

本节约束通过 Multica Engineering Squad 执行的任务；普通本地开发或其他工作流应遵循用户明确要求及仓库适用规范。

### Worktree 管理

- Worktree 由 Multica 任务编排环境创建、管理和清理。Engineer 必须在当前任务分配的 Worktree 内工作，不得直接修改原始项目工作目录。
- 开始修改前，实际读取以下命令的输出：
  ```powershell
  git rev-parse --show-toplevel
  git branch --show-current
  git status --short --branch
  git worktree list
  ```
- 分支名称必须以实际 `git branch --show-current` 结果为准，不得假设固定命名。
- 不得自行创建、重命名、删除或切换离开当前任务分支；不得手动执行 `git worktree add` / `git worktree remove` 管理任务 Worktree。
- 同一任务恢复执行时，优先续接原 Worktree 和原分支，不得重新创建分支或丢弃已有工作。
- 开始新任务时，核实任务分支基准及 `origin/main` 状态。发现分支过期、冲突或无法解释的工作区修改时，先安全调查（`git log`、`git diff`、`git status`、`git stash list`、`git reflog` 等），不得擅自 `git reset`、`git clean` 或强制覆盖。

### Squad 协作与并行

- 同一正式任务内，代码实施与审查严格串行：Engineer 完成修改和提交后，再交由 Reviewer 审查。
- Engineer 是唯一负责生产代码修改的成员；Reviewer 只读审查，不修改生产代码、不提交、不 Push。
- 不同独立任务允许并行。
- 当多个任务涉及相同文件、共享构建配置（如 `common.gradle`、`settings.gradle`、`build.gradle`）、Mixin registry 或相互依赖的功能时，由 Architect 协调为串行，避免交叉覆盖。
- 不得因为 Worktree 相互隔离，就认定不同任务之间不存在代码冲突。

### 提交规范

- 采用逻辑里程碑提交：每完成一个完整、可验证的逻辑节点提交一次，不要求每个文件或每次小修改单独提交。
- 提交前检查 `git diff`、`git diff --staged`，并执行当前改动类型适用的验证。
- 不要把一个任务的全部修改无差别合并成一个最终提交。
- 不得依赖 Worktree 收尾时的自动提交兜底。每次 Engineer 执行结束前，应显式提交完整工作，并检查 `git status` 确认工作区状态符合预期。
- 提交信息遵循 Conventional Commits，使用中文描述，专有名词及内部标识符保留英文，格式为 `type: 中文描述`。
- 常用类型：`feat`、`fix`、`refactor`、`perf`、`test`、`docs`、`build`、`ci`、`chore`、`revert`。
- 示例：
  - `feat: 新增规则 craftableCoralBlock`
  - `fix: 修复配方冲突扫描异常`
  - `refactor: 简化配方注册流程`
  - `test: 补充配方冲突回归测试`
  - `docs: 更新规则说明`

### PR 规范

- PR 目标分支固定为 `main`。
- 使用当前任务分支作为 PR head，不另建 `feat/...` 分支。
- 完成初始实现和适用的本地自动化验证后，Push 当前任务分支并创建 Draft PR。
- PR 标题、描述和说明使用中文，保留规则内部名、类名等英文技术标识符。
- PR 描述必须包含修改摘要、重要实现、测试结果、未执行的验证和待人工验收事项。
- 返工时更新同一个 PR，不另建重复 PR。
- 所有必要 CI 和审查门禁通过后，才允许把 Draft PR 转为 Ready for Review。
- 审查必须针对明确的 base SHA 和 head SHA；head SHA 变化后，旧审查不能直接视为对新代码有效。
- 不得擅自合并 PR、启用 Auto Merge 或修改仓库保护规则。

### 权限边界与禁止操作

当前只采用行为约束，不假设本地 GitHub 凭据具备技术级隔离。

严格禁止：
- 直接执行 `git push origin main`。
- 使用 `gh pr merge`、GitHub REST API、GraphQL 或其他接口合并 PR。
- 启用 PR Auto Merge。
- 强制推送覆盖他人提交。
- 擅自删除 `main` 或其他受保护分支。
- 修改 GitHub 仓库权限、Branch Protection 或 Rulesets。

允许的操作：
- 创建和更新当前任务分支。
- Push 当前任务分支。
- 创建、更新 Draft PR。
- 所有门禁通过后，将 Draft PR 转为 Ready for Review。

最终 PR 合并决定权始终属于用户。
