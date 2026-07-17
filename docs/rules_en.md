> Doc Version: `v2.8.0`

## General mechanics

### Crafting recipe conflict lock

Crafting-related rules register built-in recipes from this mod. If another datapack or mod provides a crafting recipe with the same output as one of those built-in recipes, the corresponding rule is automatically locked to `false` at runtime.

When locked:
- the built-in recipe from this mod stops applying;
- the external datapack/mod recipe keeps working;
- `/carpet <ruleName>` shows `false`;
- `/carpet <ruleName> true` is rejected;
- all online players receive a notice;
- `carpet.conf` is not modified;
- removing the conflicting datapack and running `/reload` releases the lock and restores the previous configured value.

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

Add a prefix to bots in the Tab list, using `&` to represent section symbols.\
(The feature design references a similar rule from Carpet-TCTC-Addition, and the current implementation has been rewritten for this mod's needs.)

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Bot]`
- Categories: `ICE`

## botTabListNameSuffix

Add a suffix to bots in the Tab list, using `&` to represent section symbols.\
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

## disableIllegalChatCharacterCheck

Skips the vanilla chat character validation, allowing characters that are normally rejected, such as the section sign.

This rule affects every vanilla interface and processing path that calls the shared vanilla chat-character validation method, including chat, commands, signs, books, anvils, and other screens that reuse vanilla text-input components. Interface-specific length, format, syntax, and business predicates remain active, as do server-side text filtering, packet limits, and chat-signature validation. Third-party mods with custom text boxes that do not call the shared method are outside the guaranteed scope.

When only the client has this mod and connects to a vanilla server, sending `/carpet disableIllegalChatCharacterCheck true` can switch the rule temporarily for the current client process. The command is still sent to the server, so a vanilla server will normally report an unknown command, and it may still filter or reject the resulting text or disconnect the client. No reliable local switch is guaranteed when connected to a Carpet server that does not have this extension.

Whether sign formatting codes survive from editing to persistent storage also depends on the installation setup and the rule value synchronized by the server:

| Installation | Sign behavior |
| --- | --- |
| Client only | When the rule is `true` in the client process, local typing and pasting can be relaxed; a server without this mod still removes formatting codes when saving signs, as in vanilla. |
| Server only | When the server rule is `true`, the server can preserve raw sign text containing section signs that a client actually submits; a vanilla client normally cannot enter these characters through the regular text-input UI. |
| Client and server | The rule value synchronized by the server overrides a temporary value for the same rule in the client process. When the synchronized value is `true`, the complete path from client editing and packet submission to server persistence can retain the text. |

The server stores the raw literal string containing section signs in `SignText`; the client interprets supported sequences as formatting codes while rendering it. This does not mean that the server creates a structured color `Style`. Server-side text filtering, packet limits, interface-specific length and line-width limits, waxing and edit permissions, front/back selection, and the other vanilla checks for books and chat remain active. This rule adds no server-side book-formatting bypass.

(The rule behavior and shared-character-check interception approach reference the rule of the same name in Carpet-TCTC-Addition; the implementation for this project's architecture and supported versions was written independently.)

- Type: `boolean`
- Default: `false`
- Reference values: `true`, `false`
- Categories: `ICE`, `CLIENT`
