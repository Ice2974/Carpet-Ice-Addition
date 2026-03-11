# Carpet Ice Addition

A server-first Fabric Carpet extension mod for technical survival gameplay.

## Version Baseline

- Minecraft: `1.21.8`
- Java: `21`
- Fabric Loader: `0.18.4`
- Fabric API: `0.133.4+1.21.8`
- Carpet: `1.4.177` (dependency artifact: `1.21.7-1.4.177+v250630`)

## Build

```bash
gradle build
```

Built jar location:

- `build/libs/carpet-ice-addition-0.1.0.jar`

## Run In Development

```bash
gradle runServer
```

Then check mod loading and rules in server console or in-game with `/carpet`.

## Included Rules (MVP)

### `safeScaffoldingBreak`

- Type: `boolean`
- Default: `false`
- Categories: `Ice`

English:

- Require holding scaffolding or an empty main hand to break scaffolding.

中文：

- 只有主手持脚手架或主手为空时，玩家才能破坏脚手架，防止误拆。

Behavior:

- Only targets scaffolding blocks.
- Allows breaking only when main hand is scaffolding or empty.
- Offhand does not matter.
- Applies in survival and creative.
- Applies only to real players (not fake players).
- Blocks before break is executed.
- Sends action bar warning on invalid attempt:
  - EN: `Hold scaffolding or empty your main hand to break scaffolding.`
  - ZH: `你必须手持脚手架或空手才能破坏脚手架`

### `crafterStopsWhenOutputBlocked`

- Type: `boolean`
- Default: `false`
- Categories: `Ice`

English:

- Prevent the crafter from crafting when its output points to a valid auto-accepting container that cannot fully accept the crafted result and recipe remainders.

中文：

- 当合成器前方是可自动接收产物的有效容器，且无法完整接收本次主产物与配方余留物时，取消本次合成，避免产物喷出。

Behavior:

- Injects before Crafter craft execution starts consuming materials.
- If front target is a valid auto-accepting inventory and cannot fully accept the crafted result plus all recipe remainders, cancels this craft tick.
- When canceled:
  - no crafting this tick,
  - no ingredient consumption,
  - no item entities spawned,
  - no partial output insertion.
- If front target is not a valid auto-accepting inventory, keeps vanilla behavior.
- Ender chest is not treated as target inventory (vanilla behavior kept).

## Manual Test Guide

### 1) Rule visibility and toggle

1. Start dev server.
2. Run `/carpet` and confirm both rules are listed.
3. Toggle each rule:
   - `/carpet safeScaffoldingBreak true`
   - `/carpet crafterStopsWhenOutputBlocked true`

### 2) `safeScaffoldingBreak`

1. Place scaffolding.
2. Hold any non-scaffolding item in main hand, try break.
3. Expected: break denied + action bar warning.
4. Empty main hand, try break.
5. Expected: break succeeds.
6. Hold scaffolding in main hand, try break.
7. Expected: break succeeds.

### 3) `crafterStopsWhenOutputBlocked`

1. Build a crafter with fixed recipe output (example: 9 redstone dust).
2. Place valid target inventory in output direction.
3. Fill target so it can only accept part of result (example: free space for 1 dust only).
4. Trigger crafter.
5. Expected: craft canceled; no input consumed; no dropped item.
6. Increase free space to fit full result stack.
7. Expected: craft proceeds normally.
8. Use a recipe that produces non-empty remainders (for example, recipe paths that return buckets or bottles).
9. Fill target so main result can fit but at least one remainder cannot fit completely.
10. Trigger crafter.
11. Expected: craft canceled; no input consumed; no dropped item entities.
12. Replace front block with non-inventory block.
13. Expected: vanilla crafter behavior preserved.

## Known Limits / Notes

- Fake player detection currently checks `instanceof carpet.patches.EntityPlayerMPFake` (coupled to Carpet fake-player implementation class).
- Output-fit simulation for crafter uses generic `Inventory` + `SidedInventory` insertion rules (`isValid`, `canInsert`) and slot stack limits, and evaluates crafted result plus all non-empty recipe remainders together.
- This MVP intentionally does not include commands, debug logger, client UI, scarpet extension, or extra libraries.

## Short Future Directions

- Add more technical-survival Carpet rules.
- Improve fake-player detection strategy if Carpet internals change.
- Add automated game tests for crafter edge cases.

## Current Assumptions

- For rule2, when front target is a valid auto-accepting inventory, crafting is allowed only if both crafted result and all non-empty recipe remainders can be fully inserted into that target inventory.
- Ender chest remains vanilla behavior and is not treated as a blocking target for rule2.


