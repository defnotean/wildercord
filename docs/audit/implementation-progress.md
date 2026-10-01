# Audit implementation

This tracks the implementation authorized on September 30, 2026. The first table and evidence sections record the initial audit pass. Later physical-magic expansion supersedes its roster, build and screenshot counts; see the expansion section at the end for the latest state. A row is complete only after its code and relevant checks are finished.

| Work | State |
|---|---|
| Generated localization and metadata | Implemented; 3,517 resource hashes identical across consecutive generator runs; 460 unit tests pass |
| Cinnamon persistence, safe spawn, rescue and personality | Enhanced client suite passed; resting/toy screenshots captured |
| Cast cancellation, dimension and aim consistency | Lifecycle checks implemented for release, echoes, scrolls and wild/secret casts; full regression gate passed; final turning capture reviewed |
| Per-shape formations, fused ingredients and camera clearance | Final 39-shape suite passed; first-render emission and attached rear glyph implemented; release and 180-degree turn captures reviewed |
| Client visual events, quality settings, limits and counters | Implemented with local frame and server tick sampling; three presets measured with zero AFK-throttled frames; local JFR CPU/allocation capture analysed |
| Cached plans and spatial query improvements | Cache isolation/rank/eviction unit tests pass; companion repair scans reduced |
| Explicit test skips and release gates | Explicit optional skips, full-suite manifest, resource/build/client CI gates and profile packaging implemented; remote CI has not run |
| Practice room, stress scenarios and performance profiles | Entry/return/reset/24 targets passed; three visual presets, pinned `.mrpack` distribution builder and frame sampler implemented; fresh launcher import not yet exercised |
| Defensive foci, mantle, elemental armour and control balance | Foci, three sets and mantle implemented; damage/guard/debt/impulse tests passed; typed fire damage and repeated-silence cap/recovery checks passed in latest integrated prefix |
| Fusion discovery preview and book | Effect/mana/material hover preview and learned exact-pair Grimoire implemented; real left-click fusion and native notebook UI passed |
| Rootbound and Storm Spire encounter upgrades | Custom bosses, models, loot and advancements implemented; focused encounters and full dungeon regression suites passed |
| Clockwork crypt, greenhouse and sky ruin | All nine new layouts, rotated controls, loot and alternate-route checks passed; final fast Survival fall recovery passed; natural many-seed placement remains unverified |
| Dungeon variants and relics | Three variants per new expedition and three explicit off-hand handling sidegrades implemented; charm tradeoff checks passed |
| Cooperative rituals and solo alternatives | Real block/effect tests passed for solo and contributing teammate completion, reward consumption and cooldown |
| Research, build library and magical home projects | Native notebook save/load/delete mouse test passed; research/library atomicity and rewards passed; all home utilities, fused ingredients and solo/cooperative rituals passed |
| Familiar roles, event aftermath and optional trials | Four selectable roles, saved echoes and three trials implemented; focused role/echo/precision tests passed |
| Equipment artwork and screenshots | New focus/toy/armour/boss artwork generated; four armour captures and 20-icon comparison available; final gallery has 656 searchable captures plus the full animation gallery |
| Actual mouse constants and interaction regression | Minecraft 26.3 migration mismatch corrected; native notebook, Fusion Altar and Cord left/right-click regression suites passed; full client gate passed |
| Final regeneration, builds and integrated verification | 3,517-file regeneration stable; final 460-test build passed; full client gate and final expedition/magic/shader checks passed |

The original audit remains a record of findings before this implementation. New checks and screenshots are recorded here as work completes.

## Evidence

The full client log `logs/implementation-complete-suite.log` ends in **BUILD SUCCESSFUL in 44m 40s**. With the authored animation gallery enabled, the 58-entrypoint descriptor has **55 passed and 3 skipped** suites: the additional fire/blood close-up gallery, optional staged showcase, and baseline shader suite. Suite counts are not counts of individual assertions. `artifacts/review/test-manifest.json` records the descriptor, declared optional flags, and unit report totals.

Final source changes to the rear-circle timing/attachment and sky recovery/art were verified after that full run:

* `logs/expeditions-final-verification.log`: passed in 40 seconds; nine layouts, mechanisms, route/loot checks and fast Survival fall recovery.
* `logs/magic-final-verification.log`: passed in 3m 13s; all 39 shapes and a 180-degree turning capture, with 190 screenshots archived.
* `logs/shader-final-verification.log`: passed in 53 seconds with Iris/Sodium and the test shader enabled; 12 shader/plain comparisons archived.
* `logs/implementation-final-build.log`: final release build and game-test compilation passed in 10 seconds; 460 unit tests in 54 suites, zero failures/errors/skips.
* `logs/distribution-profiles-final.log`: all three `.mrpack` archives built from the final release jar; manifest, dependency consistency, CRC and bundled jar checks passed.

Earlier focused successful logs remain available for Cinnamon, foci, practice, encounters, armour, research/library, home rituals, content systems and real mouse interactions. All changes remain uncommitted.

## Review and remaining verification limits

`artifacts/review/index.html` contains 656 searchable current captures, twenty equipment icon comparisons, and a link to the full labelled animation gallery. Final focused captures supersede matching images from the full run. Historical archives remain on disk. `build/libs/wildercord-0.6.1-alpha+mc26.3.jar` and the three packs in `artifacts/profiles/` contain the final code.

Remote CI, a fresh launcher import, arbitrary shader packs, an uncapped low-end hardware comparison, natural placement across many seeds, and real 2/4/8-client dedicated-server balance/performance sessions remain unverified. The local gates do not establish perfect fairness for every spell combination or exhaustive travel/camera conditions. Measured performance and its limits are in `2026-09-30-performance-measurements.md`.

Game-test launches clear their screenshot output directory. Review images are therefore copied into ignored `artifacts/review/` before subsequent launches. The original audit records the findings before implementation, not the current completion state.

## Physical magic and original material expansion

The later scope adds three physical base runes, six signature fusions, original sprites and motion for twelve material styles, live rear casting beats, exact weaving of up to eight effects, paid innate imprinting, owner-only Survival soul weaving and the same-cast burst spellguard fix. The current roster is 338 named runes, 297 shapes/effects and 77 named fusions, including 22 signatures. The implementation and verification details are in `2026-09-30-physical-magic-expansion.md`; player instructions are in `../features/physical-magic.md`.

Focused terrain/altar/defence verification passed in 55 seconds; 464 unit tests in 55 suites pass; all 39 shapes and turning passed in 3m 12s; enabled Iris/Sodium comparisons passed in 52 seconds. Owned-innate and foreign-Phantom checks passed in 25 seconds. The final integrated gate passed in **45m 6s**, with **56 passed and 3 explicitly skipped** client suites; enabled shaders passed separately.

Final regeneration matches across 3,623 resource hashes. The release jar and game-test compilation passed in four seconds, and all three pinned `.mrpack` profiles were rebuilt and validated from that jar. The final run's 840 PNGs are archived in `artifacts/review/expanded_complete/`, including 186 labelled release/impact timing frames. The merged index has **666 searchable captures**, 20 equipment icons, nine physical rune icons and twelve original material sprites. Ten focused terrain images, 190 updated shape images and 12 shader/plain comparisons are also archived. `artifacts/review/test-manifest.json` records the complete gate and declared optional skips. The whitespace gate is clean.

The updated dev client is running with the visible `Minecraft* 26.3` window and Iris/Sodium installed. No shaderpack is selected in this dev session; the enabled shader fixture was verified separately. The local review server returns HTTP 200 at `http://127.0.0.1:8874/artifacts/review/index.html`. Opening that gallery in the Codex browser panel was queued. No compilation or asset regeneration will run against the active dev client.

## Latest casting circle expansion

Twelve craftable circle disciplines and twelve authored animated mechanisms are implemented, with shared world/editor geometry, material-coloured circuits, linked impact circles, group-isolated bonuses and release snapshots. There are now **350 named runes, 36 modifiers and 199 craftable runes**. Exact tradeoffs and recipes are in `../features/circle-disciplines.md`; changes and limits are in `2026-09-30-circle-expansion.md`.

**474 unit tests in 56 suites pass**, including all 468 shape/discipline compilations. The final circle client suite passed in **1m 22s**, including real combat/support/status changes, conditions, group/Knot isolation, linked spells and the runtime readout. The 39-shape/turning regression passed in **3m 12s**, and enabled Iris/Sodium comparisons passed in **53 seconds**. Two regenerations matched **3,698 resource hashes**. The final release build passed in four seconds and the three launcher packs were rebuilt and validated.

The gallery has **718 searchable captures**, including **52 circle frames**, three editor previews, the linked impact and 190 updated shape captures; twelve circle icons are included. Focused results and pack hashes are in `artifacts/review/circle-verification.json`. The earlier full gate remains historical; the current 60-entrypoint descriptor was not run in full for this addition.

The updated dev client has been restarted with Iris/Sodium and the visible `Minecraft* 26.3` window. The local gallery returns HTTP 200 and its Codex browser display is queued. All changes remain uncommitted, and no further compilation or regeneration will run against the active client.
