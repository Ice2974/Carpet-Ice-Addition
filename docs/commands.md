> 文档对应版本：`v2.3.0`

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

清理结果会完整统计实体数和物品数；如果物品明细分组过多，反馈中的明细列表可能截断显示。

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
