# Basinfill: small persistent water holes

Added after public 0.9.1-alpha.1 as a development review increment.

## Implementation

- Rank-I frost/water WORLD rune, six base mana; normal shape/cost/cooldown/knowledge rules apply.
- Blank Rune + Clay Ball + Water Bucket; normal container remainder returns the bucket.
- Terrain impact selects the adjacent empty/water cell; Touch or Bolt aimed at a basin floor is the ordinary recipe.
- Bounded breadth-first discovery: at most sixteen connected same-height cells, each within three horizontal blocks of the impact seed. Floor and sides must have sturdy watertight faces.
- Reject deeper/open/oversized vessels, occupied blocks, evaporating environments, unloaded/out-of-height/out-of-border cells and temporary constructs. Build settings, Adventure restrictions and native protection event veto apply; floor permission is also checked.
- Validate all cells before editing, reserve the full block allowance atomically, fill only air/non-source water, and share one basin allowance across the paid cast's copies, links and pulses.
- Ordinary permanent water follows vanilla rules if players later modify banks; this is not a fluid containment block or a deep-reservoir spell.
- Authored droplet/vessel rune artwork, pouring-thread preparation, cupped parcel projectile and per-cell settling drops. Dedicated synthesized frost_basin_pour cue: staggered liquid threads, low gurgle, descending drops. No borrowed Bubble cue remains.

## Evidence

BasinfillTest passed in 35 seconds: a real paid Survival Touch cast; exact source-water placement;
maximum16, oversized20, deep/open/Adventure/plant refusals; no partial water; exhausted block
budget; shared paid pulse refusal; loaded native crafting output and returned bucket; a real
protection-event veto; Nether refusal; ordinary elapsed fluid ticks and a complete saved-world
restart retaining source water. Three actual-game captures are preserved in
artifacts/review/basinfill and the filled pool illustrates the player guide.

Final combined build passed in16seconds:963 unit tests, zero failures/errors/skips. Generated
check passes4874paths;115guidepages validate/export. Checked review JAR has clean ZIP CRC;
4872 resources and1633 main/client classes byte-match the tested outputs. SHA256:
`f6450e2abffc4c7b11e6959cc50d22b9c54bfca1174afea321a319679eee015d`.

## Limits and remaining goal work

Controlled fixture screenshots do not show natural generation or remote multiplayer. The
native suite exercises paid Touch; authored Bolt formation/flight is implemented but this
suite is not a full Bolt/beam/rank/link/Minimal/shader presentation review. Unloaded/border
and temporary-support rejection are code checks, not separately exercised native cases here.
Source water can be collected like any ordinary water. Base mana is not total spell price.

This adds one new ability toward the living-world target; it does not also count as new
functional equipment, a signature fusion, creature, ecosystem or lore text. The full goal
remains open; see living-world-remaining.md. The review JAR includes development changes and
has not been uploaded as a new public release.
