## Safe Scaffolding Break (safeScaffoldingBreak)

The player can only break scaffolding if they are holding scaffolding or if their main hand is empty. If the player tries to break scaffolding while holding another item, the action will be prevented, and a warning will appear in the **Action Bar**.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `SURVIVAL`, `FEATURE`

## Crafter Stops When Output Blocked (crafterStopsWhenOutputBlocked)

Stops crafting when the output container cannot fully accept the crafted result, avoiding item spillage. This prevents the crafting process from continuing if the container is full or unable to accept the crafted items.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

## Record World Event Timing Fix (recordWorldEventFix)

Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, which may also cause overlapping disc audio. See MC-112245.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`
