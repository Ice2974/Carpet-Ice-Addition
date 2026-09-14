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

### Category definitions

`CLIENT`: The rule involves client-side behavior and requires both Carpet and this mod to be fully installed on the client to take full effect; with a server-only installation, these rules only partially take effect.

## Rules

### safeScaffoldingBreak

Require holding scaffolding or an empty main hand to break scaffolding, preventing accidental dismantling.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### itemFrameInvisible

Allows players to right-click item frames or glow item frames that already contain an item with a phantom membrane to make them invisible.

> Invisible item frames remain invisible after the rule is disabled

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### itemFrameFixed

Allows players to right-click item frames or glow item frames that already contain an item with a glass pane to make them fixed; fixed frames can be unfixed by right-clicking them with an axe.

> Fixed item frames remain fixed after the rule is disabled

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

Disable kelp natural growth from random ticks.

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

Drops 1 regular ice when broken using any tool with the Silk Touch enchantment.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### frostedIceProperToolFix

Makes pickaxes the proper tools for breaking frosted ice.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`, `SURVIVAL`, `CLIENT`

### beaconProperToolFix

Makes pickaxes the proper tools for mining beacons.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`, `SURVIVAL`, `CLIENT`

### iceLikeMagmaBlocks

When players break magma blocks with tools without Silk Touch, the magma block does not drop, and a lava source is generated using ice-like support checks when the block below provides valid solid support or liquid support.

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

### woolSuppressesSculkSpread

Prevents a sculk catalyst from generating sculk blocks when covered by wool, while preserving its ability to suppress experience drops from nearby mob deaths.

> Wool can only stop the catalyst from initiating new spread events; it cannot freeze spread events that are already in progress

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### wardenNotHostileToPlayers

Prevents wardens from becoming hostile toward players or targeting them for attack without preventing them from detecting player-caused vibrations.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

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

Controls the permission of the /machineStatus command, which saves the target block state when a technical machine is shut down, and checks which machines are not currently in their saved shutdown state.

- Type: `String`
- Default: `ops`
- Possible values: `false`, `true`, `ops`, `0`, `1`, `2`, `3`, `4`
- Categories: `ICE`, `COMMAND`

### machineStatusRollbackWarning

Checks saved machine states when a player enters a supported rollback command, and warns that player if any machines are not in their saved shutdown state.

> Supported backup mod by default: Quick Backup Multi (`/qb`, `/quickbackupmulti`).
>
> Supported MCDR plugins by default: Quick Backup Multi (`!!qb`), Prime Backup (`!!pb`), Chunk Backup (`!!cb`).
>
> Rollback commands to detect can be configured in the global `config/carpet-ice-addition/machine_status_rollback_warning.json`.

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

Players cannot damage their own tamed mobs while PVP is enabled, and cannot damage any player-owned tamed mobs while PVP is disabled.

> Affected mobs include cats, wolves, parrots, nautiluses, zombie nautiluses, horses, donkeys, mules, zombie horses, skeleton horses, llamas, and trader llamas.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### enhancedTrident

Allows tridents to hit multiple entities during a movement and regain the ability to deal damage when moved again after stopping.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### phantomSpawnWarning

Warns players to sleep in time at the start of the night when they reach the vanilla insomnia time threshold for phantom spawning.

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

Allows customizing the center position of the End obsidian platform generated when entities enter the End through the End portal, while keeping the arrival position in sync.

- Type: `String`
- Default: `vanilla`
- Suggested values: `vanilla`, `-100,49,0`
- Accepted format: `vanilla` or `x,y,z`
- Categories: `ICE`, `FEATURE`

### beaconIgnoresObstruction

Allows beacons to function normally regardless of blocks above them.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `CLIENT`

### namedWanderingTraderPersistence

Prevents named wandering traders from naturally despawning.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
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

### ctrlQStonecuttingFix `MC<=1.21.1`

Allows dropping an entire stack from the stonecutter output slot using Ctrl+Q.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`

### disableIllegalTextCharacterCheck

Skips vanilla text character validation, allowing characters that are normally rejected, such as section signs.

- Type: `boolean`
- Default: `false`
- Possible values: `true`, `false`
- Categories: `ICE`, `FEATURE`, `CLIENT`

### waterFluidTickDelay

Customizes the water flow delay in game ticks.

> `vanilla`: does not take over, leaving the delay to vanilla and other mods\
> `freeze`: freezes the fluid, stopping its flow

- Type: `String`
- Default: `vanilla`
- Suggested values: `freeze`, `vanilla`
- Accepted values: `vanilla`, `freeze`, or an integer from `1` through `72000`
- Categories: `ICE`, `FEATURE`

### lavaFluidTickDelay

Customizes the lava flow delay in game ticks.

> `vanilla`: does not take over, leaving the delay to vanilla and other mods\
> `freeze`: freezes the fluid, stopping its flow

- Type: `String`
- Default: `vanilla`
- Suggested values: `freeze`, `vanilla`
- Accepted values: `vanilla`, `freeze`, or an integer from `1` through `72000`
- Categories: `ICE`, `FEATURE`

### villagerTradingOptimization

Naming a villager trade trims its AI to the minimum behavior set required by a fixed trading hall.

> CORE keeps only drowning avoidance, workstation validity checks, and nearby workstation claiming without pathfinding (a workstation is claimed only when it lies within the restock check distance of about 1.73 blocks; once claimed, villagers can still bind the workstation, gain a profession, and reset to unemployed as usual)\
> WORK keeps only the restocking behavior, with hard restock semantics such as daily restock counts, intervals, and cross-day resets matching vanilla. Villagers never pathfind or move toward workstations, and the task lists of all other activities (meeting, resting, idling, panicking, raiding, playing, etc.) are cleared.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `OPTIMIZATION`

### nameTagDuplicateNamingFix

Prevents a name tag from being consumed when its name is identical to the entity's current custom name.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `BUGFIX`

### ironGolemSpawningOptimization

Naming a villager iron_golem trims its AI to the minimum behavior set required by a scare-based iron farm.

> The IDLE / MEET / PLAY / WORK activity task lists are skipped entirely (including gossip exchanges, following / watching trading players, etc.); villagers no longer run the 48-block POI scans and pathfinding for job sites or meeting points, while bed claiming (HOME) stays vanilla. Named villagers remain stationary during the day, keeping only the priority-99 schedule switching; at night they enter REST sleep according to the schedule, or trigger iron golem spawning through CORE's PANIC.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `OPTIMIZATION`

### enchantedGoldenAppleEffectRollback

Restores the effects of eating an enchanted golden apple to those from Java Edition 1.8: Regeneration V for 30 seconds, Absorption I for 2 minutes, Resistance I for 5 minutes, and Fire Resistance I for 5 minutes.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`, `SURVIVAL`

### bedrockImpalingPort

Makes Impaling behave like Bedrock Edition, dealing additional damage to any target in water or rain.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`

### betterTridentDespawnCondition

Tridents can only despawn after being stuck in a block and remaining stationary for 1200 game ticks.

- Type: `boolean`
- Default: `false`
- Possible values: `false`, `true`
- Categories: `ICE`, `FEATURE`
