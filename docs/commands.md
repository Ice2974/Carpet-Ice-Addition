> 文档对应版本：`v2.2.0`

## 命令列表

| 命令 | 相关规则 | 说明 |
|---|---|---|
| `/killitem` | `commandKillItem` | 清理掉落物，并管理清理黑名单与命名掉落物配置。 |
| `/machineStatus` | `commandMachineStatus` | 查询生电机器当前状态，需要手动保存机器关机时的基准方块状态。 |

---

## /killitem

### 相关规则

`commandKillItem`

### 指令语法

| 指令 | 作用 |
|---|---|
| `/killitem range <radius>` | 以执行命令的玩家为中心，清理当前维度指定半径内的掉落物 |
| `/killitem dimension <dimension>` | 清理指定维度中的掉落物 |
| `/killitem all` | 清理所有已加载维度中的掉落物 |
| `/killitem config blacklist` | 显示当前物品黑名单 |
| `/killitem config blacklist add <item>` | 将指定物品加入黑名单 |
| `/killitem config blacklist remove <item>` | 将指定物品移出黑名单 |
| `/killitem config blacklist clear` | 清空黑名单 |
| `/killitem config clearNamedItems` | 显示是否清理命名掉落物 |
| `/killitem config clearNamedItems <true\|false>` | 设置是否清理命名掉落物 |

### 参数说明

| 参数 | 说明 | 示例 |
|---|---|---|
| `<radius>` | 清理半径，只对 `/killitem range` 生效 | `64` |
| `<dimension>` | 维度注册表 ID | `minecraft:overworld`、`minecraft:the_nether`、`minecraft:the_end` |
| `<item>` | 物品注册表 ID | `minecraft:diamond`、`minecraft:netherite_ingot` |
| `<true\|false>` | 布尔值开关 | `true`、`false` |

---

## /machineStatus

### 相关规则

`commandMachineStatus`

### 指令语法

| 指令 | 作用 |
|---|---|
| `/machineStatus add <dimension> <pos> <name>` | 新增机器记录，并将目标方块当前位置的当前状态保存为关机状态 |
| `/machineStatus remove <name>` | 删除指定机器记录 |
| `/machineStatus rename <name> <newName>` | 重命名指定机器记录 |
| `/machineStatus update <name>` | 使用记录位置当前的方块状态更新保存的关机状态 |
| `/machineStatus move <name> <dimension> <pos>` | 将机器目标移动到新位置，并把该方块当前状态保存为新的关机状态 |
| `/machineStatus list` | 列出所有机器及其当前状态 |
| `/machineStatus list running` | 只列出当前方块状态与保存关机状态不同、但方块类型未变化的机器 |
| `/machineStatus list stopped` | 只列出当前方块状态与保存关机状态完全一致的机器 |
| `/machineStatus list invalid` | 只列出状态异常的机器，例如维度不存在、保存状态无效或方块类型变化 |
| `/machineStatus list unloaded` | 只列出目标区块当前未加载的机器 |
| `/machineStatus info <name>` | 查看指定机器的详细信息 |

### 参数说明

| 参数 | 说明 | 示例 |
|---|---|---|
| `<dimension>` | 维度注册表 ID | `minecraft:overworld`、`minecraft:the_nether`、`minecraft:the_end` |
| `<pos>` | 目标方块坐标 | `100 64 -20` |
| `<name>` | 必须唯一的机器名称。无空格的名称可直接输入，中文名称同样支持直接输入；包含空格或其他需要保留原样的内容时，请使用双引号包裹 | `刷铁机`、`ironFarm`、`"猪灵交易所"`、`"地狱门 开关"` |

### 状态说明

| 状态 | 说明 |
|---|---|
| `异常` | 维度不存在、保存的方块状态无法解析、目标方块类型变化，或当前状态已无法安全比较 |
| `运行中` | 目标区块已加载、可以读取当前方块状态、方块类型未变化，但当前状态与保存的关机状态不同 |
| `关机` | 目标区块已加载，且当前方块状态与保存的关机状态完全一致 |
| `未加载` | 维度存在，但目标区块当前未加载；命令不会为了检查状态而强制加载区块 |

---

## 回档前机器状态警告

### 相关规则

`machineStatusRollbackWarning`

### 行为说明

启用后，当玩家输入已适配的回档命令时，服务器会直接复用 `/machineStatus` 的内部状态检查逻辑，检查已保存机器中哪些处于“运行中”状态，并只向触发该输入的玩家发送警告。

- 不会执行 `/machineStatus` 命令本身
- 不会拦截、取消或修改原始回档命令/聊天消息
- 不会修改备份 mod、MCDR 插件或 `/machineStatus` 已保存数据
- 只提示“运行中”机器；`未加载` 和 `异常` 默认不提示

### 已适配输入

| 来源 | 默认匹配示例 |
|---|---|
| Minecraft 斜杠命令 | `/qb back 1`、`/qb restore 1`、`/quickbackupmulti back 1`、`/quickbackupmulti restore 1` |
| 玩家聊天消息 | `!!qb back 1`、`!!pb back 1`、`!!cb back 1` |

### 配置文件

全局配置文件路径：`config/carpet-ice-addition/machine_status_rollback_warning.json`

默认内容：

```json
{
  "rollbackCommandPatterns": [
    "^/?qb\\s+(back|restore)\\b.*",
    "^/?quickbackupmulti\\s+(back|restore)\\b.*",
    "^!!qb\\s+back\\b.*",
    "^!!pb\\s+back\\b.*",
    "^!!cb\\s+back\\b.*"
  ]
}
```

约定：

- 首次命中检测时，如果配置文件不存在，会自动生成默认配置
- 修改该配置后需要重启服务器，当前实现不支持热加载
- 无效正则会被跳过，并在服务器日志中记录 warning
- `/machineStatus` 的机器数据仍保存在当前世界存档目录下的 `carpet-ice-addition/machine_status.json`
