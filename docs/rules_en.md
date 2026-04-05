## safeScaffoldingBreak

The player can only break scaffolding if they are holding scaffolding or if their main hand is empty. If the player tries to break scaffolding while holding another item, the action will be prevented, and a warning will appear in the **Action Bar**.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`, `FEATURE`

## crafterStopsWhenOutputBlocked

Stops crafting when the output container cannot fully accept the crafted result, avoiding item spillage. This prevents the crafting process from continuing if the container is full or unable to accept the crafted items.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## recordWorldEventFix

Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, which may also cause overlapping disc audio. See MC-112245.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`

## spawnersIgnoreInvisiblePlayers

Invisible players do not activate normal spawners, trial spawners, or ominous trial spawners. If visible players are nearby, spawning can still be activated normally.

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

## botTabListNamePrefix

Adds a prefix to fake player names in the TabList, using `&` to represent text color.

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Bot] `
- Categories: `ICE`

## botTabListNameSuffix

Adds a suffix to fake player names in the TabList, using `&` to represent text color.

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, ` [Fake]`
- Categories: `ICE`
