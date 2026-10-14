## Command List

| Command | Related Rule | Description |
| - | - | - |
| `/killitem` | `commandKillItem` | Clears item entities and manages the cleanup blacklist and named-item cleanup setting. |
| `/machineStatus` | `commandMachineStatus` | Checks the current status of technical machines. The baseline block state when each machine is shut down must be saved manually. |
| `/weakload` | `commandWeakload` | Marks chunks to stay weak loaded, keeping scheduled ticks and block entities running without entity ticking. |

## /killitem

### Related Rule

`commandKillItem`

### Command Syntax

| Command | Description |
| - | - |
| `/killitem range <radius>` | Clears item entities within the specified radius around the player executing the command in the current dimension |
| `/killitem dimension <dimension>` | Clears item entities in the specified dimension |
| `/killitem all` | Clears item entities in all loaded dimensions |
| `/killitem detail <resultId> <page>` | Shows cached cleanup details by page, usually triggered by `[Expand]`, `[Previous]`, or `[Next]` in chat, and does not clear items again |
| `/killitem config blacklist` | Shows the current item blacklist |
| `/killitem config blacklist add <item>` | Adds the specified item to the blacklist |
| `/killitem config blacklist remove <item>` | Removes the specified item from the blacklist |
| `/killitem config blacklist clear` | Clears the blacklist |
| `/killitem config clearNamedItems` | Shows whether named item entities will be cleared |
| `/killitem config clearNamedItems <true\|false>` | Sets whether named item entities will be cleared |

### Parameters

| Parameter | Description | Example |
| - | - | - |
| `<radius>` | Cleanup radius, only used by `/killitem range` | `64` |
| `<dimension>` | Dimension registry ID | `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end` |
| `<resultId>` | Cleanup detail cache ID, provided automatically by the clickable cleanup result buttons | `a1b2c3...` |
| `<page>` | Cleanup detail page number; values below 1 use page 1, and values beyond the end use the last page | `1` |
| `<item>` | Item registry ID | `minecraft:diamond`, `minecraft:netherite_ingot` |
| `<true\|false>` | Boolean toggle | `true`, `false` |

Cleanup results are shown as a multi-line summary with entity count, item count, and up to 5 item detail groups. If there are more than 5 groups, player chat feedback shows `[Expand]`; clicking it runs `/killitem detail <resultId> <page>` to view cached paged details and does not rerun `/killitem all`, `/killitem dimension`, or `/killitem range`.

Cleanup detail caches expire after about 10 minutes, after which the cleanup command must be run again. Console, command block, and RCON cleanup executions only show a plain truncated summary and do not provide clickable expansion.

## /machineStatus

### Related Rule

`commandMachineStatus`
`machineStatusRollbackWarning`

### Command Syntax

| Command | Description |
| - | - |
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
| - | - | - |
| `<dimension>` | Dimension registry ID | `minecraft:overworld`, `minecraft:the_nether`, `minecraft:the_end` |
| `<pos>` | Target block position | `100 64 -20` |
| `<name>` | Unique machine name. Names matching Brigadier's unquoted single-token rules can be entered directly, including `-`, `_`, `.`, and `+`; Chinese, spaces, or any other content that cannot be parsed as an unquoted single token must be wrapped in double quotes | `ironFarm`, `farm-1.2+test`, `"刷铁机"`, `"Piglin Trading Hall"`, `"Nether Portal Switch"` |

### Status Meanings

| Status | Description |
| - | - |
| `Invalid` | The dimension is missing, the saved block state cannot be parsed, the block type changed, or the current block can no longer be compared safely |
| `Running` | The target chunk is loaded, the current block state can be read, the block type is unchanged, and the current state differs from the saved shutdown state |
| `Stopped` | The target chunk is loaded and the current block state exactly matches the saved shutdown state |
| `Unloaded` | The dimension exists but the target chunk is not loaded; the command must not force-load it |

## /weakload

### Related Rule

`commandWeakload`

### Command Syntax

| Command | Description |
| - | - |
| `/weakload add <from> [<to>]` | Marks the specified chunk (or chunk area) as weak loaded |
| `/weakload remove <from> [<to>]` | Removes weak loading for the specified chunk (or chunk area) |
| `/weakload remove all` | Removes all weak loaded chunks in the current dimension |
| `/weakload query [<pos>]` | Queries chunks marked as weak loaded by this command in the current dimension |

### Parameters

| Parameter | Description | Example |
| - | - | - |
| `<from>` | Starting block column coordinates (X and Z only); when `<to>` is omitted, only the containing chunk is affected | `100 -200` |
| `<to>` | Ending block column coordinates, forming a rectangular chunk area together with `<from>` | `200 -100` |
| `<pos>` | Block column coordinates to query; when omitted, all weak loaded chunks in the current dimension are listed | `0 0` |

### Behavior Notes

- The command syntax, coordinate interpretation, and area limits (at most 256 chunks per invocation, coordinates must be within -30000000 to 29999999) match vanilla `/forceload`; the command operates on the dimension of the command source.
- Stronger loading sources such as players and `/forceload` naturally provide a higher loading level following vanilla rules, and the chunk falls back to weak loading once the stronger source is gone.
- Weak load records and tickets are independent from vanilla `/forceload`: removing one kind of loading never affects the other, and `/forceload remove all` does not remove weak loading.
- Weak load records are stored in `carpet-ice-addition/weakload.json` under the world directory and are restored automatically after a server restart or re-entering a singleplayer world; records of temporarily missing dimensions are kept and never deleted by changes to other dimensions.
- If the data file is corrupted, the original file is renamed to `weakload.json.corrupt` before continuing with empty records; if the rename fails, the data enters protection mode and rejects further changes until the world files are fixed and the server restarted.
