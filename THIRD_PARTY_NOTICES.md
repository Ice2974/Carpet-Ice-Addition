# Third-Party Notices

This file records the third-party build tooling and architecture provenance used by Carpet Ice Addition, and acknowledges the projects that inspired its feature designs.

The license of Carpet Ice Addition's own code is described in [LICENSE](LICENSE). Licenses mentioned in this file belong to the respective third-party projects; they do not change the licensing of Carpet Ice Addition's own code.

## Build Tooling and Architecture Provenance

### Fallen-Breath/preprocessor

- Related build-time dependency: multi-version source preprocessing (Phase 5)
- Upstream project Fallen-Breath/preprocessor, a fork of ReplayMod/preprocess, https://github.com/Fallen-Breath/preprocessor
- Consumed via JitPack (`com.github.Fallen-Breath:preprocessor`, pinned to full commit `c5abb4fb12aad2590c852c1bc6c8d5758606ec0b`) as a Gradle build plugin
- Original project license: GPL-3.0-or-later
- Notes Build-time only Gradle plugin that preprocesses the root source tree per Minecraft version (`//#if MC` macros, version graph with mapping edges). It is not compiled into, linked with, or distributed in any release artifact. Its license belongs to the tool itself and does not relicense Carpet Ice Addition's own code.

### Fallen-Breath/fabric-mod-template

- Related architecture provenance: multi-version preprocess build architecture (Phase 5)
- Upstream project Fallen-Breath/fabric-mod-template, https://github.com/Fallen-Breath/fabric-mod-template
- Original project license: LGPL-3.0 (applies to the template itself)
- Notes The multi-version source architecture used by this project (root source tree + per-version overrides + preprocess version graph) is modeled after the architecture family introduced by fabric-mod-template, which is also the base of Carpet TIS Addition and Carpet AMS Addition. The current build implementation (settings.json-driven platform registry, shared build family entries, mixin config generation, resource collision invariants) was constructed for Carpet Ice Addition's own 11-platform structure with substantial modifications, and no template files are shipped in this repository. The template's license does not apply to Carpet Ice Addition's source code or release artifacts.

## Inspirations and Acknowledgements

The following projects inspired feature designs, rule behaviors, or architecture ideas in Carpet Ice Addition. The corresponding implementations were written independently for Carpet Ice Addition and do not incorporate source code from those projects. Upstream licenses are recorded as information about those projects, not as the license of Carpet Ice Addition's implementations.

### Carpet-Fixes

- Related rule `recordWorldEventFix`
- Upstream project Carpet-Fixes, https://github.com/FxMorin/carpet-fixes
- Original project license: MIT (Copyright (c) 2020 Fx Morin)
- Notes The rule targets the same Minecraft bug. The feature design was inspired by Carpet-Fixes; the Carpet Ice Addition implementation was written independently and uses its own event timing logic.

### Carpet-TCTC-Addition

- Related rules `botTabListNamePrefix`, `botTabListNameSuffix`, `disableIllegalTextCharacterCheck`
- Upstream project Carpet-TCTC-Addition, https://github.com/The-Cat-Town-Craft/Carpet-TCTC-Addition
- Original project license: LGPL-3.0
- Notes The feature behaviors, including the shared-character-check interception approach of `disableIllegalTextCharacterCheck`, were inspired by the original project. The Carpet Ice Addition implementations were written independently for its architecture and supported versions.

### DoormatCarpetExtension

- Related rule `disablePlayerAttackingTamedMobs`
- Upstream project DoormatCarpetExtension, https://github.com/axialeaa/DoormatCarpetExtension
- Original project license: LGPL-3.0
- Notes The rule behavior was inspired by the original project. The Carpet Ice Addition implementation was written independently and adjusted for Carpet Ice Addition.

### carpet-redcraft-addons

- Related rules `itemFrameInvisible`, `itemFrameFixed`
- Upstream project carpet-redcraft-addons, https://github.com/MultiCoreNetwork/carpet-redcraft-addons
- Related original rule `betterItemFrames`
- Original project license: AGPL-3.0
- Notes The feature design was inspired by the `betterItemFrames` rule (a scarpet-script based implementation) of carpet-redcraft-addons. The Carpet Ice Addition implementation was written independently as a Java mixin-based implementation for its target versions and layering structure, and does not incorporate source code from that project.

### Carpet TIS Addition

- Related architecture inspiration: multi-version preprocess source architecture (Phase 5)
- Upstream project Carpet TIS Addition, https://github.com/TISUnion/Carpet-TIS-Addition
- Original project license: LGPL-3.0
- Notes The multi-version source architecture of this project is modeled after Carpet TIS Addition. The Carpet Ice Addition build implementation was written independently for its own 11-platform structure.

### Carpet AMS Addition

- Related architecture inspiration: concrete reference during Phase 5 planning
- Upstream project Carpet AMS Addition, https://github.com/Minecraft-AMS/Carpet-AMS-Addition
- Original project license: LGPL-3.0
- Notes Carpet AMS Addition (same architecture family) was used as a concrete architecture reference during Phase 5 planning. The Carpet Ice Addition build implementation was written independently for its own platform structure.
