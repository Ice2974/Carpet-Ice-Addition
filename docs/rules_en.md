## safeScaffoldingBreak

Players can only break scaffolding when holding scaffolding or with an empty main hand. If they try to break scaffolding while holding another item, the action is blocked and a warning is shown in the **Action Bar**.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`

## crafterStopsWhenOutputBlocked

Stops crafting when the output container cannot fully accept the crafted result, preventing item spillage.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## recordWorldEventFix

Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, and prevents overlapping disc audio. See MC-112245.
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

Allows collecting budding amethyst with a suitable Silk Touch tool.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## botTabListNamePrefix

Adds a prefix to fake player names in the TabList. Supports `&` color/format codes.
(This rule was initially ported from Carpet-TCTC-Addition and has since been adapted for this mod's needs.)

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, `[Bot] `
- Categories: `ICE`

## botTabListNameSuffix

Adds a suffix to fake player names in the TabList. Supports `&` color/format codes.
(This rule was initially ported from Carpet-TCTC-Addition and has since been adapted for this mod's needs.)

- Type: `String`
- Default: `#none`
- Suggested values: `#none`, ` [Fake]`
- Categories: `ICE`

## fakePlayerIgnoreThornsDamage

Bots do not take reflected damage caused by Thorns when attacking entities or players with Thorns. Real players keep vanilla behavior.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## disablePetAttacking

Disables players harming tamed mobs to prevent accidental harm. Only player-sourced damage is blocked; untamed mobs and non-player damage sources keep vanilla behavior.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`
