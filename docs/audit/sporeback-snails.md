# Sporeback Snails: first fungal habitat relationship

## Scope

One original peaceful magical wildlife creature, **Sporeback Snail**, with a layered offset
spiral shell, lilac slug foot, two articulated antennae, feeding posture, retraction and dew crown.
Two functional items: **Mycelial Dew** and **Fungal Poultice**. One original three-page lore text:
Mara's **The Patient Spiral**. Spawn eggs and lore books are not credited as functional gear.
This adds no ability, signature fusion, dungeon or completed investigation. It starts the fungal
habitat foundation; it does not complete a connected ecosystem.

## Gameplay

Natural biome pools: lush and dripstone caves. Placement requires Y below48, dark covered moss
or clay footing, an empty fluid cell, the configured wildlife/per-species switch and multiplier,
and fewer than two living snails within24blocks. No breeding or unique death loot.

A real mushroom approach followed by two seconds of uninterrupted close browsing prepares one
dew reserve without breaking or replacing the mushroom. The reserve and a one-minute forage
rest are saved. Gathering rejects spectator/dead players before touching reserve/discovery clocks, and requires crouching with an empty hand, an unfrightened snail, a bead
and the saved two-minute gather deadline. Exactly one dew is given; the first successful gather
also awards the journal using a persistent per-player discovery key. Magic cannot mint dew.

Life gives a finite spore breath; a separate ten-second response rest prevents immediate renewal.
Fire response and ordinary harm retract the antennae for six seconds, blocking gathering.
Bright conditions invite a bounded search for shade. Both the chosen destination and actual
footing must have real low light and nearby overhead cover before a shelter pose is accepted.

Dew, paper and a brown mushroom craft one poultice. Actual item use gives thirty seconds of
Night Vision with six seconds of Slowness. Existing Night Vision prevents consumption or
refresh. Ordinary vanilla effect persistence applies; the item provides no immunity.

## Bounds and implementation

The stable anchored search visits at most eight nearby columns/24cells per second, with at most
two native path calls per slice. Journey time is260ticks, three movement retries, and an exhausted
search rests120ticks. Loaded-cell checks precede world queries. No entity ledger, ticking block,
terrain destruction, population growth or automated item generation is introduced.

Main: SporebackRules, SporebackSnail, SporebackContent. Client: SporebackModel and
SporebackRenderer. Original exact UV art and detailed item materials: tools/sporeback_art.py.
Five authored shell/friction/breath/dew sound cues: tools/feel/sporeback.py. Player chapter:
wiki/world/sporeback-snails.md. Central common/client registration, config schema, generator
and test entry are integrated by the root task.

## Native evidence

Focused SporebackSnailTest initially passed in74seconds; the final expanded suite passed in64seconds with an integrated server and actual client.

- Real navigation to a living mushroom, close browse completion and exactly retained plant.
- Loaded crafting recipe and actual poultice use packet, consumption, sight/slowness and refusal.
- Authoritative spectator and remote custom-call refusal preserves reserve/deadline/discovery; standing gather refusal, crouched empty-hand client interaction, exactly one dew and journal.
- Native lush/dripstone pool entries; accepted dark moss habitat and dry-stone refusal.
- Life response through native CastEngine hit handling; repeated response retains clock/no dew.
- Direct Fire response hook, closed shell and finite expiry. This is not a paid Fire spell cast.
- Exact gather/response deadlines, spent reserve and discovery/journal across a full saved-world
  shutdown and reopen. Two-snail cap and native empty death-resource loot.
- A separate exposed live visitor is observed150ticks; any shelter pose must satisfy actual
  footing darkness/overhead cover. This conditional invariant does not prove successful shade travel.

First run failed a journal restart assertion because the test fixture replaced the journal in
its selected empty-hand slot when providing the poultice. The fixture now preserves that book
before hand replacement; the corrected full suite passed. No production persistence bug was found.

Evidence: artifacts/review/parallel-living-world/sporeback/final-native.log and final/ PNGs;
native.log retains the first successful74second run, first-run-fixture-inventory.log the original
fixture failure. The first camera pitch put the low head/body partly under the HUD. Final
35degree/diagonal originals were inspected: whole lilac foot, mushroom approach, layered shell,
antennae and dew crown now fit above the HUD. The second frame catches early physical retraction
and the poultice inventory icon. These are controlled moss/roof fixtures, not natural cave photographs.

## Outstanding acceptance

Detailed held-item/field-journal visual inspection; actual paid Fire
and Life casts, ordinary nonlethal harm interaction, successful varied-terrain shade travel,
true-reserve restart case, natural cave encounter frequency across multiple seeds, wider fungal
relationships, actual two-client/dedicated-server play, shaders/reduced settings and measured
sustained performance remain unverified. Search caps are implementation constraints, not a
performance benchmark. Full living-world goal remains active.
