> Doc Version: `v1.3.0`

## safeScaffoldingBreak

Require holding scaffolding or an empty main hand to break scaffolding.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`

## crafterStopsWhenOutputBlocked

Prevent the crafter from crafting when its output points to a valid auto-accepting container that cannot fully accept the crafted result and recipe remainders.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## recordWorldEventFix

Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, which may also cause overlapping disc audio. See MC-112245.
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

Disables kelp natural growth from random ticks while keeping bonemeal growth unchanged.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## canMineBuddingAmethyst

Budding amethyst can be collected with a Silk Touch tool.

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

Add a prefix to the Bot in the TabList, using `&` to represent text color.
(This rule was initially ported from Carpet-TCTC-Addition and has since been adapted for this mod's needs.)

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Bot] `
- Categories: `ICE`

## botTabListNameSuffix

Add a suffix to the Bot in the TabList, using `&` to represent text color.
(This rule was initially ported from Carpet-TCTC-Addition and has since been adapted for this mod's needs.)

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, ` [Fake]`
- Categories: `ICE`

## fakePlayerIgnoreThornsDamage

Bots will not take reflected damage caused by Thorns when attacking entities or players equipped with Thorns.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## disablePlayerAttackingTamedMobs

Prevents players from damaging mobs they have tamed to avoid accidental harm.
Affected mobs include cats, wolves, parrots, nautiluses, zombie nautiluses, horses, donkeys, mules, zombie horses, skeleton horses, llamas, and trader llamas.
(This rule was initially ported from DoormatCarpetExtension and has since been adapted for this mod's needs.)

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
