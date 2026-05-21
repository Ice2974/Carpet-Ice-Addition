> 文档对应版本：`v2.1.0`

## 命令列表

| 命令 | 相关规则 | 说明 |
|---|---|---|
| `/killitem` | `commandKillItem` | 清理掉落物，并管理清理黑名单与命名掉落物配置。 |

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
