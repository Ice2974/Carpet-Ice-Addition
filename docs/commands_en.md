> Doc Version: `v1.9.0`

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