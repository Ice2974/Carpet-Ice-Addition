## General mechanics

### Crafting recipe conflict lock

Crafting-related rules provide this mod's recipes through a corresponding built-in datapack. Enabling a rule selects that datapack; disabling it deselects the datapack and triggers a vanilla server resource reload. If another datapack or mod provides a crafting recipe with the same output as one of those built-in recipes, the corresponding rule is automatically locked to `false` at runtime.

When locked:

- the built-in datapack from this mod is deselected;
- the external datapack/mod recipe keeps working;
- `/carpet <ruleName>` shows `false`;
- `/carpet <ruleName> true` is rejected;
- all online players receive a notice;
- `carpet.conf` is not modified;
- removing the conflicting datapack and running `/reload` releases the lock and restores the previous configured value; online recipe books are synchronized after a successful resource reload.

### Client-category

Rules with the `CLIENT` category involve client-side behavior and only take full effect when both Carpet and this mod are fully installed on the client as well. With a server-only installation, these rules only partially take effect.

## Rules

### safeScaffoldingBreak

Require holding scaffolding or an empty main hand to break scaffolding.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### invisibleItemFrames

Allows players to right-click item frames or glow item frames that already contain an item with a phantom membrane to make them invisible.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### fixedItemFrames

Allows players to right-click item frames or glow item frames that already contain an item with a glass pane to make them fixed; fixed frames can be unfixed by right-clicking them with an axe.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### crafterStopsWhenOutputBlocked

Prevents a crafter from crafting when the container it faces cannot fully accept the main recipe result. Recipe remainders keep their vanilla behavior.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### craftableCoralBlocks

Allows 9 coral fans of the same type to be crafted into the corresponding coral block, including dead coral variants.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### recordWorldEventFix

Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, which may also cause overlapping disc audio. See MC-112245.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`

### spawnersIgnoreInvisiblePlayers

Normal spawners, trial spawners, and ominous trial spawners ignore invisible players when checking nearby players.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### disableKelpNaturalGrowth

Disables kelp natural growth from random ticks.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### disableAmethystGrowth

Budding amethyst blocks will no longer generate amethyst buds or advance the growth of amethyst buds during random ticks.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### silkTouchBuddingAmethyst

Budding amethyst can be collected by using a suitable tool with the Silk Touch enchantment.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### silkTouchFrostedIce

Frosted ice drops 1 regular ice when broken by a player using any tool with the Silk Touch enchantment.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### frostedIceProperToolFix

Makes pickaxes the proper tools for breaking frosted ice, so that pickaxe mining speed is applied as if it were ice.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`, `SURVIVAL`, `CLIENT`

### iceLikeMagmaBlocks

Allows magma blocks broken by players with tools without Silk Touch to generate a lava source using ice-like support checks when the block below is a valid solid block or liquid block.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### disableNyliumDecay

Crimson nylium and warped nylium will not decay into netherrack when covered by a block above.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### botTabListNamePrefix

Add a prefix to bots in the Tab list, using `&` to represent section symbols.

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Bot]`
- Categories: `ICE`, `BOT`

### botTabListNameSuffix

Add a suffix to bots in the Tab list, using `&` to represent section symbols.

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Fake]`
- Categories: `ICE`, `BOT`

### commandKillItem

Registers the /killitem command for clearing item entities by radius or globally, with blacklist and named-item protection settings.

- Type: `String`
- Default: `ops`
- Possible values: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- Categories: `ICE`, `COMMAND`

### commandMachineStatus

Registers the /machineStatus command to save the target block state when a technical machine is shut down, and to check which machines are not currently in their saved shutdown state.

- Type: `String`
- Default: `ops`
- Possible values: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- Categories: `ICE`, `COMMAND`

### machineStatusRollbackWarning

Checks saved machine states when a player enters a supported rollback command, and warns that player if any machines are not in their saved shutdown state. This rule does not block or modify rollback commands.

Supported backup mod by default: Quick Backup Multi (`/qb`, `/quickbackupmulti`).\
Supported MCDR plugins by default: Quick Backup Multi (`!!qb`), Prime Backup (`!!pb`), Chunk Backup (`!!cb`).

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`

### fakePlayerIgnoreThornsDamage

Bots will not take reflected damage caused by Thorns when attacking entities or players equipped with Thorns.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`, `BOT`

### disablePlayerAttackingTamedMobs

Players cannot damage their own tamed mobs while server PVP is enabled, and cannot damage any player-owned tamed mobs while server PVP is disabled.\
Affected mobs include cats, wolves, parrots, nautiluses, zombie nautiluses, horses, donkeys, mules, zombie horses, skeleton horses, llamas, and trader llamas.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### phantomSpawnWarning

Warns players at the start of the night when they reach the vanilla insomnia time threshold for phantom spawning.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`

### neutralPhantoms

Makes phantoms neutral toward players: they will not attack players first, but will fight back when attacked.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### easyWaterloggedBlockPlacement

Allows players holding a water bucket in the offhand to directly place waterloggable blocks in a waterlogged state, except in dimensions where water evaporates.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### portableInfiniteWater

Prevents water from being consumed when using a vanilla water bucket while holding one in both hands.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### disableAirborneMiningPenalty

Prevents players from receiving the mining speed penalty while airborne.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`, `CLIENT`

### customEndPlatformPosition

Allows customizing the center position of the End obsidian platform generated when entities enter the End, while keeping the arrival position in sync.

- Type: `String`
- Default: `vanilla`
- Suggested values: `vanilla`, `-100,49,0`
- Accepted format: `vanilla` or `x,y,z`
- Categories: `ICE`, `FEATURE`

### mobsSpawnWithoutSpears `MC>=1.21.11`

Prevents naturally spawned zombies, zombie villagers, husks, zombified piglins, and piglins from holding spears.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### carpetSingleplayerExitCrashFix `MC<=1.21.1`

Fixes a Carpet crash that can happen when leaving a singleplayer world after previously joining a Carpet server.

- Type: `boolean`
- Default: `true`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`, `CLIENT`

### disableIllegalTextCharacterCheck

Skips vanilla text character validation, allowing characters that are normally rejected, such as section signs.

- Type: `boolean`
- Default: `false`
- Possible values: `true`, `false`
- Categories: `ICE`, `FEATURE`, `CLIENT`

### waterFluidTickDelay

Customizes the water flow delay in game ticks. Setting it to `freeze` freezes scheduled ticks related to water flow.

- Type: `String`
- Default: `5`
- Suggested values: `freeze`, `5`
- Accepted values: `freeze` or an integer from `1` through `72000`
- Categories: `ICE`, `FEATURE`

### lavaFluidTickDelay

Customizes the lava flow delay in game ticks. In ultrawarm dimensions, the delay is divided by three with a minimum value of 1. Setting it to `freeze` freezes lava flow without affecting random ticks.

- Type: `String`
- Default: `30`
- Suggested values: `freeze`, `30`
- Accepted values: `freeze` or an integer from `1` through `72000`
- Categories: `ICE`, `FEATURE`
