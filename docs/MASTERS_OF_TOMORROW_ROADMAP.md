# Masters of Tomorrow: development roadmap

Updated 6 October 2026. This is the plan for the expanded update, not a claim that it is release-ready. The feature branch is still in a draft pull request. The [release checklist](reviews/RELEASE_READINESS.md) records evidence and remaining checks; the [living-world roadmap](LIVING_WORLD_ROADMAP.md) retains earlier exploration and ecosystem commitments.

## What is already on the development branch

The current foundation includes three Master schools with consent-based trials, finite Aura, readable attack commitments, shared cast-punish pursuit, and an Ember, Gale and Stone signature. Party protection, first-clear records, the 100-crystal absorption cap and progression through Circle XX are implemented. Mana Skin now rebates a bounded fraction of actual nonlethal health damage rather than damage before armor.

Three shared player arts and sixteen existing style forms have authored body/hand timelines in this source checkpoint. Only part of that catalog has the articulated body backend and complete native visual evidence. Thirty-four of the fifty existing style arts still lack their new body choreography. The latest Collapse and Red Rain forms still need integrated native acceptance. New choreography is not counted as a new gameplay ability.

The branch also contains opt-in articulated combat, supported armor and funded Aura-shell presentation, staged in-game presentation preferences, and a bounded operator-approved pavilion upgrade for existing worlds. Each has its own remaining compatibility and runtime checks. The pavilion does not yet retrofit the full dungeon, boss or encounter catalog.

Earlier focused native checkpoints have passed. Recent published checkpoints still expose native failures, and the expanded local candidate remains unrun. The normal build and unit tests do not make the whole release green. These limits are tracked openly in the release checklist.

## Priority 1: a dependable playable foundation

- Fix and rerun the settings UI and field-callback fixtures without weakening their assertions. Confirm the real dedicated-server lifecycle tests after the approved disposable-server EULA setup.
- Resolve the actual wildlife navigation/arrival and presentation failures using observed causes. Preserve natural behavior, existing bounds and the full aggregate test order.
- Complete actual body, weapon, first-person, armor and effects evidence for the current authored forms. Keep Classic presentation as the default while supported opt-in paths are validated.
- Keep all four aggregate native shards and the separately labeled focused groups. Every release claim must refer to the exact candidate.

## Priority 2: make player progression rewarding

This is the next gameplay vertical slice alongside the stability work. Circle VIII should mark a new way to play, and the climb to XX should add meaningful choices. Maximum Aura should offer decisions beyond higher output.

- Audit every existing unlock, where a Survival player learns it, and whether the controls and tradeoffs are visible.
- Deliver one complete mage progression path and one complete Aura path first: in-world discovery, a short teaching encounter, an earned capability, loadout choice, practice opportunity, feedback and an encounter that rewards using it.
- Add bounded high-tier options with costs, limited preparation or equipment slots, commitment and recovery. Avoid filling later circles with only mana, regeneration and damage multipliers.
- Test old-save progression, already-qualified players, respec/reset behavior where supported, party interactions and the lowest legitimate access route.

The exact first mechanics are under source audit. Proposed ideas are not unlocks until the real learning and use paths work in-game.

## Priority 3: expand magic through distinct uses

- Add substantial authored spell packs spanning pressure, defense, control, traversal, utility and cooperation.
- Expand spell shaping and combinations through new tactical rules and clear costs. A different color, stronger number or permutation of existing runes does not count as a new ability.
- Give each addition a use case, discovery method, understandable tooltip, recognizable sound/animation and counterplay. Check how it interacts with terrain, claims, parties, summons and delayed ownership.
- Organize the existing large rune catalog so players can find useful combinations without relying on commands or external code knowledge.

The catalog expansion will be delivered in tested packs, with new abilities, upgrades, recipes and cosmetic variants counted separately.

## Priority 4: expand Aura movement and sword expression

- Add learned footwork, evasion, gap closing, stance choices, parry follow-ups, spell-cut counters and aerial options connected to sword styles.
- Preserve deliberate commitment: movement has swept collision, terrain and hazard checks; cancelled travel has a defined stop and recovery; a landing is distinguished from water, timeout or aborted flight.
- Finish the remaining style body forms in suitable families: fixed releases, targeted forms, counters, travel, teleports, airborne moves, charges and sustained fields.
- Give movement and follow-ups readable first-person hands as well as full third-person choreography. Keep input discoverable and rebindable, with stable-camera and reduced-flash choices.

New movements must change positioning decisions without replacing every dodge, cover choice or punish window with unlimited mobility.

## Priority 5: bosses with large, distinct repertoires

The requested long-range goal is hundreds of genuinely different fighting moves and many legal combinations. It is not met. The current three-school union has eight named attack actions; each school can use six. School tuning variants, individual sub-hits and permutations do not increase that unique-action count.

- Build an immutable attack catalog with stable IDs and explicit school, timing, resource, warning and animation contracts. The first passive catalog and compatibility layer are implemented with parity tests; live selection and execution do not use them yet.
- Add bounded school-specific transition graphs, encounter-seeded variation and recent-move avoidance. Record accepted observations for deterministic replays; a seed alone cannot reproduce player behavior.
- Preserve complete recovery and punish windows between attacks. No graph may bypass a due guard, Aura cost, cooldown, terrain check or committed aim, and no boss reads a future player input.
- Expand with small original packs whose attacks differ mechanically and visually, then broaden the repertoire after native counterplay and animation review.
- Track unique authored attacks, variants, transition edges and observed combinations separately. Finite catalogs cannot guarantee that no long fight ever repeats a sequence.

The first pack is being designed around the identities of Ember, Gale and Stone. Increasing health or recoloring the same swing is not a substitute for this goal.

## Priority 6: lore that changes what players discover and do

- Audit existing canon before extending the schools' histories, rivalries, magic traditions and ruined training grounds.
- Tie new lessons and abilities to NPC dialogue, exploration clues, short discovery quests and encounter rewards.
- Deliver lore in the game through readable conversations, journals, locations and repeatable hints, with persistent discovery records where needed.
- Make rewards and clues useful for both newly created worlds and existing characters. Keep optional background readable without burying combat instructions.

The first player-progression paths should demonstrate this approach before a much larger quest and lore expansion is counted complete.

## Priority 7: bring supported content to existing worlds safely

- Expand the current preview/region-approval/journal framework with individually reviewed structure and encounter adapters.
- Keep permanent placement subject to explicit operator approval of the exact region and preview. Defer unloaded chunks, preserve player edits, and retain claims/error refusal and rollback rules.
- Add separate boss/entity duplicate prevention and saved content-version records. Do not regenerate whole explored chunks or assume old worlds have complete edit provenance.
- Test restart, failed saves, changed claims, later player edits and recovery in disposable worlds before adding larger templates.

A small pavilion adapter is a foundation; a complete old-world content upgrade remains unfinished.

## Priority 8: presentation, usability and performance

- Finish supported articulated moves, hand sockets, first-person composition, armor materials, shell effects and whole-model compatibility fallback.
- Validate both handednesses and live skin widths, HUD scales, free look, equip/swing transitions, menus, resource reloads, outlines and supported shader/resource configurations.
- Make ordinary enablement, persistence, reset, Classic fallback, stable camera and reduced flash understandable in-game.
- Measure sustained eight-player server tick cost, client frame time, particles, packets and retained state. Bound effects and target searches; do not infer performance from a few screenshots.

## Priority 9: candidate integration and release decision

- Freeze a candidate with the agreed content scope and complete ordinary-play access.
- Pass normal build, generated assets, unit tests, packaging, every required native suite and all four aggregate shards on that exact candidate.
- Run real multiplayer, latency, representative endgame sustain, save migration and human gameplay sessions. Automated scripts do not establish that the fights feel good.
- Verify the distributable JAR, dependencies, licenses, version, upgrade instructions, rollback guidance and release notes.
- Present remaining compatibility limits and the readiness result before deciding to merge or publish a release.

There are no invented completion dates. Work advances through usable, reviewed gameplay slices while runtime regressions are repaired in parallel. The requested magic, movement, boss and lore expansions remain open until their actual in-game delivery and acceptance are complete.
