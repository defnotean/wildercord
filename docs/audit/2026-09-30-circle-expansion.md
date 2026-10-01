# Casting circle expansion

This later addition extends the completed physical magic release. All changes remain uncommitted. Player instructions and exact numbers are in [circle disciplines](../features/circle-disciplines.md).

## Implemented

- Twelve craftable circle modifier runes: Needle, Bloom, Gyre, Anchor, Reservoir, Crucible, Confluence, Pilgrim, Vigil, Mercy, Tempest and Eclipse. The roster is now **350 named runes: 39 shapes, 258 effects, 36 modifiers and 17 links; 199 craftable runes**.
- Twelve explicitly authored animated mechanisms: closing iris, unfolding petals, three-arm turbine, locked lattice, filling basins, breathing furnace, braided satellites, rolling compass, watchful eye, sheltering crescents, storm forks and moving eclipse.
- Plain shapes choose a mechanism without receiving a gameplay bonus. A selected discipline overrides it. Rune script, illustrated roundels, elemental colours and special secret centrepieces remain available.
- Charged and release circles use the mechanisms; caster circles retain the live rear attachment. Linked disciplines also open at their actual trigger point. A later group's discipline cannot replace the opening group's circle.
- The Cord preview and world renderer share `CircleGeometry`, so the selectable mechanism is visible in the editor.
- Original hand-drawn item glyphs, emblems and circuit band motifs for all twelve runes, with tier decoration and animated Tier III icons. Generated language entries, item models and recipes are included. The editor has a Circle disciplines category, and its readout describes every tradeoff.
- Gameplay choices affect shape coverage, flying speed, effect duration, mana, supportive power, actual distinct fused elements, movement, crouching, water/rain and night. Flight speed does not extend range or change an instant beam into a projectile. Duration does not add strikes to a repeating shape.
- Conditions are sampled once at group release, and paid child effects share the original cast identity and budgets. Effect-local views keep circle power/duration bonuses out of linked groups. The first discipline per group counts; extra circles are warned about, ignored and not charged.
- Knots preserve disciplines. Existing physical terrain lifetime caps and spell budgets still apply.

## Verification

- `logs/circle-final-verification.log`: the final real client circle suite **passed in 1m 22s**. Real damage and healing changes, actual Haste duration, stance/movement snapshots, actual water exposure, night/day activation, shared budget identity, group isolation, Knot preservation, linked spell effects and the runtime readout's short complete lines were checked. The unit gate passed in the preceding build.
- Unit suite: **474 tests across 56 suites, zero failures/errors/skips**. Ten circle tests cover all **468 shape/discipline pairs**, stacking refusal, prices, role/condition tradeoffs, actual distinct woven ingredients, Knot/link isolation, twelve distinct finite bounded mechanisms and all ten ingredient colours.
- `logs/circle-magic-regression.log`: **passed in 3m 12s** with all 39 shapes and rapid turning, producing 190 captures. The final circle suite also verifies all twelve mechanisms after sharing their geometry with the editor.
- **52 final circle screenshots** are archived in `artifacts/review/circles/`: four frames per discipline, three editor previews and one linked impact. The shape regression archive is `artifacts/review/circle_magic/`.

The damage/link fixtures are named before spawning so they cannot randomly awaken as Runebound, and damage assertions measure health lost rather than assuming a starting health. The fixture failure is preserved in `logs/circle-readout-fixture-failure.log`.

The earlier complete 59-entrypoint run remains historical evidence; the descriptor now has 60 entrypoints, and the added circle suite was run separately. The full long suite was not rerun for this addition. This does not establish exhaustive multiplayer balance or uncapped performance for the new circles.

## Final release gates

- `logs/circle-shader-verification.log`: enabled Iris/Sodium shader fixture passed in **53 seconds**, with twelve shader/plain captures archived.
- Two asset regenerations matched all **3,698 resource hashes**, including all 350 unique rune rings and emblems.
- The merged review gallery contains **718 searchable captures**, twelve circle icon comparisons and the earlier equipment/material galleries.
- The final jar and three launcher profiles are rebuilt after the readout change; packaging checks verify pinned dependencies, archive CRCs and exact embedded jar bytes. The build and pack logs are `logs/circle-final-build.log` and `logs/circle-distribution-profiles.log`.
- The final build passed in **four seconds**. `artifacts/review/circle-verification.json` records the focused gate results, current unit totals, resource hashes, archive counts and pack hashes. The whitespace gate is clean.
