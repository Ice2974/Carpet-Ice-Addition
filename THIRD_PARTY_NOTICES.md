# Third-Party Notices

This project contains features inspired by or originally ported from third-party Carpet extensions.
Unless otherwise stated, the current implementations have been adapted or rewritten for Carpet Ice Addition.

## Fallen-Breath/preprocessor

- Related build tooling preprocess multi-version source architecture (Phase 5)
- Original project Fallen-Breath/preprocessor, a fork of ReplayMod/preprocess
- Source https://github.com/Fallen-Breath/preprocessor (JitPack `com.github.Fallen-Breath:preprocessor`, pinned to full commit `c5abb4fb12aad2590c852c1bc6c8d5758606ec0b`)
- License GPL-3.0
- Notes Build-time only Gradle plugin that preprocesses the root source tree per Minecraft version (`//#if MC` macros, version graph with mapping edges). It is not compiled into, linked with, or distributed in any release artifact, and does not affect the licensing of this project's own code.
- Notes The multi-version source architecture (root source tree + per-version overrides + preprocess version graph) is modeled after Fallen-Breath's Carpet TIS Addition project, with Carpet AMS Addition (same architecture family) used as a concrete reference during Phase 5 planning. Our version graph, mapping edges, platform layout, and verification gates are designed for Carpet Ice Addition's own 11-platform structure.

## Carpet-Fixes

- Related rule `recordWorldEventFix`
- Original project Carpet-Fixes
- License MIT
- Notes The rule targets the same Minecraft bug, but the current implementation uses Carpet Ice Addition's own event timing logic.

## Carpet-TCTC-Addition

- Related rules `botTabListNamePrefix`, `botTabListNameSuffix`, `disableIllegalTextCharacterCheck`
- Original project Carpet-TCTC-Addition
- License LGPL-3.0
- Notes The feature behavior was inspired by the original project and has been rewritten for Carpet Ice Addition.
- Notes The behavior and shared-character-check interception approach of `disableIllegalTextCharacterCheck` were referenced from a similar rule in the original project. Its implementation was written independently for Carpet Ice Addition's architecture and supported versions.

## DoormatCarpetExtension

- Related rule `disablePlayerAttackingTamedMobs`
- Original project DoormatCarpetExtension
- License LGPL-3.0
- Notes The rule behavior was inspired by the original project and has been rewritten and adjusted for Carpet Ice Addition.

## carpet-redcraft-addons

- Related rules `itemFrameInvisible`, `itemFrameFixed`
- Original project carpet-redcraft-addons
- Related original rule `betterItemFrames`
- License AGPL-3.0
- Notes The feature design references the `betterItemFrames` rule from carpet-redcraft-addons. The current implementation has been independently rewritten and adjusted for Carpet Ice Addition's target versions and layering structure.
