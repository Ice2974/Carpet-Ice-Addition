> Doc Version: `v2.6.0`

## safeScaffoldingBreak

Require holding scaffolding or an empty main hand to break scaffolding.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`

## invisibleItemFrames

Allows players to right-click item frames or glow item frames that already contain an item with a phantom membrane to make them invisible.\
(The feature design references carpet-redcraft-addons' betterItemFrames rule, while the current implementation has been independently rewritten for this mod's target versions and layering.)

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## fixedItemFrames

Allows players to right-click item frames or glow item frames that already contain an item with a glass pane to make them fixed; fixed frames can be unfixed by right-clicking them with an axe.\
(The feature design references carpet-redcraft-addons' betterItemFrames rule, while the current implementation has been independently rewritten for this mod's target versions and layering.)

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## crafterStopsWhenOutputBlocked

Prevents a crafter from crafting when the container it faces cannot fully accept the main recipe result. Recipe remainders keep their vanilla behavior.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## craftableCoralBlocks

Allows 9 coral fans of the same type to be crafted into the corresponding coral block, including dead coral variants.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## recordWorldEventFix

Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, which may also cause overlapping disc audio. See MC-112245.\
(This rule was initially ported from Carpet-Fixes and has since been adapted for this mod's needs.)

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`

## spawnersIgnoreInvisiblePlayers

Normal spawners, trial spawners, and ominous trial spawners ignore invisible players when checking nearby players.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## disableKelpNaturalGrowth

Disables kelp natural growth from random ticks.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## disableAmethystGrowth

Budding amethyst blocks will no longer generate amethyst buds or advance the growth of amethyst buds during random ticks.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## silkTouchBuddingAmethyst

Budding amethyst can be collected by using a suitable tool with the Silk Touch enchantment.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## silkTouchFrostedIce

Frosted ice drops 1 regular ice when broken by a player using any tool with the Silk Touch enchantment.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## frostedIceProperToolFix

Makes pickaxes the proper tools for breaking frosted ice, so that pickaxe mining speed is applied as if it were ice.

Note: For real players to get the full mining-speed experience (break progress matching the server), the client must have both Carpet and this mod fully installed. If the client does not fully install Carpet and this mod, the real player's client-side break animation may not fully match the server's break timing.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## iceLikeMagmaBlocks

Allows magma blocks broken by players with tools without Silk Touch to generate a lava source using ice-like support checks when the block below is a valid solid block or liquid block.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## disableNyliumDecay

Crimson nylium and warped nylium will not decay into netherrack when covered by a block above.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## botTabListNamePrefix

Add a prefix to bots in the Tab list, using `&` to represent text colors.\
(The feature design references a similar rule from Carpet-TCTC-Addition, and the current implementation has been rewritten for this mod's needs.)

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Bot]`
- Categories: `ICE`

## botTabListNameSuffix

Add a suffix to bots in the Tab list, using `&` to represent text colors.\
(The feature design references a similar rule from Carpet-TCTC-Addition, and the current implementation has been rewritten for this mod's needs.)

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Fake]`
- Categories: `ICE`

## commandKillItem

Registers the /killitem command for clearing item entities by radius or globally, with blacklist and named-item protection settings.

- Type: `String`
- Default: `ops`
- Possible values: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- Categories: `ICE`, `COMMAND`

## commandMachineStatus

Registers the /machineStatus command to save the target block state when a technical machine is shut down, and to check which machines are not currently in their saved shutdown state.

- Type: `String`
- Default: `ops`
- Possible values: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- Categories: `ICE`, `COMMAND`, `SURVIVAL`

## machineStatusRollbackWarning

Checks saved machine states when a player enters a supported rollback command, and warns that player if any machines are not in their saved shutdown state. This rule does not block or modify rollback commands.

Supported backup mod by default: Quick Backup Multi (`/qb`, `/quickbackupmulti`).\
Supported MCDR plugins by default: Quick Backup Multi (`!!qb`), Prime Backup (`!!pb`), Chunk Backup (`!!cb`).

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`

## fakePlayerIgnoreThornsDamage

Bots will not take reflected damage caused by Thorns when attacking entities or players equipped with Thorns.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## disablePlayerAttackingTamedMobs

Players cannot damage their own tamed mobs while server PVP is enabled, and cannot damage any player-owned tamed mobs while server PVP is disabled.\
Affected mobs include cats, wolves, parrots, nautiluses, zombie nautiluses, horses, donkeys, mules, zombie horses, skeleton horses, llamas, and trader llamas.\
(The feature design references a similar rule from DoormatCarpetExtension, and the current implementation has been rewritten for this mod's needs.)

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`

## phantomSpawnWarning

Warns players at the start of the night when they reach the vanilla insomnia time threshold for phantom spawning.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`

## neutralPhantoms

Makes phantoms neutral toward players: they will not attack players first, but will fight back when attacked.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## easyWaterloggedBlockPlacement

Allows players holding a water bucket in the offhand to directly place waterloggable blocks in a waterlogged state.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## customEndPlatformPosition

Allows customizing the center position of the End obsidian platform generated when entities enter the End, while keeping the arrival position in sync.

- Type: `String`
- Default: `vanilla`
- Suggested values: `vanilla`, `-100,49,0`
- Accepted format: `vanilla` or `x,y,z`
- Categories: `ICE`, `FEATURE`

## mobsSpawnWithoutSpears `MC>=1.21.11`

Prevents naturally spawned zombies, zombie villagers, husks, zombified piglins, and piglins from holding spears.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## carpetSingleplayerExitCrashFix `MC<=1.21.1`

Fixes a Carpet crash that can happen when leaving a singleplayer world after previously joining a Carpet server.

- Type: `boolean`
- Default: `true`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`, `CLIENT`
