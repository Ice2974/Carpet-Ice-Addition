> Doc Version: `v2.2.0`

## Command List

| Command | Related Rule | Description |
|---|---|---|
| `/killitem` | `commandKillItem` | Clears item entities and manages the cleanup blacklist and named-item cleanup setting. |
| `/machineStatus` | `commandMachineStatus` | Checks the current status of technical machines. The baseline block state when each machine is shut down must be saved manually. |

---

## /killitem

### Related Rule

`commandKillItem`

### Command Syntax

| Command | Description |
|---|---|
| `/killitem range <radius>` | Clears item entities within the specified radius around the player executing the command in the current dimension |
| `/killitem dimension <dimension>` | Clears item entities in the specified dimension |
| `/killitem all` | Clears item entities in all loaded dimensions |
| `/killitem config blacklist` | Shows the current item blacklist |
| `/killitem config blacklist add <item>` | Adds the specified item to the blacklist |
| `/killitem config blacklist remove <item>` | Removes the specified item from the blacklist |
| `/killitem config blacklist clear` | Clears the blacklist |
| `/killitem config clearNamedItems` | Shows whether named item entities will be cleared |
| `/killitem config clearNamedItems <true\|false>` | Sets whether named item entities will be cleared |

### Parameters

| Parameter | Description | Example |
|---|---|---|
| `<radius>` | Cleanup radius, only used by `/killitem range` | `64` |
| `<dimension>` | Dimension registry ID | `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end` |
| `<item>` | Item registry ID | `minecraft:diamond`, `minecraft:netherite_ingot` |
| `<true\|false>` | Boolean toggle | `true`, `false` |

---

## /machineStatus

### Related Rule

`commandMachineStatus`

### Command Syntax

| Command | Description |
|---|---|
| `/machineStatus add <dimension> <pos> <name>` | Adds a machine record and saves the current block state at the target position as the shutdown state |
| `/machineStatus remove <name>` | Removes the specified machine record |
| `/machineStatus rename <name> <newName>` | Renames the specified machine record |
| `/machineStatus update <name>` | Updates the saved shutdown state with the current block state at the recorded position |
| `/machineStatus move <name> <dimension> <pos>` | Moves the machine target to a new position and saves that block's current state as the new shutdown state |
| `/machineStatus list` | Lists all recorded machines with their current status |
| `/machineStatus list running` | Lists only machines whose current block state differs from the saved shutdown state while keeping the same block type |
| `/machineStatus list stopped` | Lists only machines whose current block state exactly matches the saved shutdown state |
| `/machineStatus list invalid` | Lists only machines with invalid status, such as missing dimensions, invalid saved states, or changed block types |
| `/machineStatus list unloaded` | Lists only machines whose target chunk is currently not loaded |
| `/machineStatus info <name>` | Shows detailed information for the specified machine |

### Parameters

| Parameter | Description | Example |
|---|---|---|
| `<dimension>` | Dimension registry ID | `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end` |
| `<pos>` | Target block position | `100 64 -20` |
| `<name>` | Unique machine name. Names without spaces can be entered directly, including Chinese names; if the name contains spaces or other content that should be preserved exactly, wrap it in double quotes | `ironFarm`, `刷铁机`, `"Piglin Trading Hall"`, `"Nether Portal Switch"` |

### Status Meanings

| Status | Description |
|---|---|
| `Invalid` | The dimension is missing, the saved block state cannot be parsed, the block type changed, or the current block can no longer be compared safely |
| `Running` | The target chunk is loaded, the current block state can be read, the block type is unchanged, and the current state differs from the saved shutdown state |
| `Stopped` | The target chunk is loaded and the current block state exactly matches the saved shutdown state |
| `Unloaded` | The dimension exists but the target chunk is not loaded; the command must not force-load it |

---

## Machine Status Rollback Warning

### Related Rule

`machineStatusRollbackWarning`

### Behavior

When enabled, entering a supported rollback command makes the server reuse the internal `/machineStatus` status-checking logic, collect saved machines that are currently `Running`, and warn only the player who triggered that input.

- It does not execute the `/machineStatus` command itself
- It does not block, cancel, or modify the original rollback command or chat message
- It does not modify backup mods, MCDR plugins, or saved `/machineStatus` data
- It only warns for `Running` machines; `Unloaded` and `Invalid` are ignored by default

### Supported Inputs

| Source | Default matching examples |
|---|---|
| Minecraft slash commands | `/qb back 1`, `/qb restore 1`, `/quickbackupmulti back 1`, `/quickbackupmulti restore 1` |
| Player chat messages | `!!qb back 1`, `!!pb back 1`, `!!cb back 1` |

### Config File

Global config path: `config/carpet-ice-addition/machine_status_rollback_warning.json`

Default content:

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

Notes:

- The default config file is created automatically when the feature first checks an input and the file does not exist
- Restart the server after editing this config; the current implementation does not hot-reload it
- Invalid regex entries are skipped and logged as warnings
- `/machineStatus` machine data still stays in the current world save directory at `carpet-ice-addition/machine_status.json`
