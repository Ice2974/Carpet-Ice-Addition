# Engineer 项目交付约定

## 技术规范入口

- 本项目的技术架构、目录边界、编码约束、纯服务端兼容、多版本实现、资源、翻译、Mixin 和验证要求，统一以仓库根目录 `AGENTS.md` 为准。
- 同一 Squad 任务的成员职责、串行实施与审查、Worktree 生命周期、CI 门禁和返工流程，以 Engineering Squad Instructions 为准。

## Worktree 修改前检查

在开始修改前，核实当前实际工作目录、分支、工作区状态和 Worktree 归属，至少检查：

```powershell
git rev-parse --show-toplevel
git branch --show-current
git status --short --branch
git worktree list
```

不要根据预期分支名推断当前分支；发现无法解释的工作区修改时，先调查，不要覆盖或丢弃。

## 本项目 Git 与 PR 约定

- PR 目标分支为 `main`。
- 提交信息使用中文 Conventional Commits，格式为 `type: 中文描述`；规则等项目内部标识符保留原文，不翻译、不改写。
- 常用类型：`feat`、`fix`、`refactor`、`perf`、`test`、`docs`、`build`、`ci`、`chore`、`revert`。
- 示例：
  - `feat: 新增规则 craftableCoralBlock`
  - `fix: 修复配方冲突扫描异常`
  - `refactor: 简化配方注册流程`
  - `test: 补充配方冲突回归测试`
  - `docs: 更新规则说明`
- PR 标题、描述和说明使用中文，规则内部名、类名等技术标识符保留英文。
