# Wildercord: a living, handcrafted magical adventure

Active user goal, started 2026-10-02. The full objective remains open until the requirements below are implemented and verified. This roadmap is a scope and evidence ledger, not evidence of completion.

## Baseline audit

Inspected main at `14efc61e`: Aura steps 1–10 exist; step 10 is merged, despite a stale status table now corrected. Step 11 (world) and 12 (mage/swordsman) remain incomplete. Wildlife currently has six registered species with biome/pool/crowding rules. DungeonStructure currently dispatches eight dungeon types. Runes.java contains 351 RuneDef declarations; this is a source inventory count, not a claim that all 351 presentations work. Existing FieldGuide, RuneResearch, relic, companion, equipment and generation systems should be extended.

The earlier test evidence is useful but does not verify the expanded goal. Every ability needs a presentation review; every new creature needs behavior and habitat evidence. No broad visual or balance claim is established by this initial code audit.

## Content targets

These targets are additions to the baseline, subject to documented design adjustments that preserve the full goal.

- 12 new creatures: four peaceful/magical wildlife, three hostile/territorial creatures, two Aura-resistant beasts, two bosses, one useful companion. Each needs custom identity, habitat, behavior, model/animation, sound, rewards, bestiary and tests.
- Four connected ecosystems: luminous wetlands, wind-carved highlands, ember woodlands, subterranean fungal ruins. Each needs resources, shelter/foraging behavior, ecological relationships, bounded spawning, and meaningful magic interactions.
- Six distinct new dungeon/encounter locations, plus old battlefields and the sleeping blade. Include the sword tomb, its intent gates and guardian. Deliberate biome distribution, rarity, alternate routes and useful return visits.
- 36 new functional items: eight relics, eight equipment/accessories, six exploration/support tools, six placeable utilities/decorations, eight consumables/material uses. Distinct tradeoffs, acquisition and recipes; no filler recolours.
- At least 12 new magic/Aura abilities and 18 authored signature fusions. Audit all existing abilities and supported pairings, including formation, launch, impact, sound, mechanics and counters. Every supported pairing must resolve intentionally.
- 24 discoverable lore texts/entries, four optional connected investigation/quest chains, illustrated bestiary/journal integration and consistent regional/faction histories.
- Finish all remaining Aura step 11/12 requirements: terrain training, battlefields, sword tomb, sleeping blade, Aura beasts, village tournaments, resonant strikes, rune-etched blades and Unity.

## Milestones and current state

| Milestone | Scope | State | Required evidence |
| --- | --- | --- | --- |
| A | Audit/player journey; finish Aura world and cooperation | Started: terrain training; Marchkeeper battlefields | Runtime terrain checks/trials; all step 11/12 encounters/items; solo/cooperative balance; pictures/JAR |
| B | Visual identity and full ability roster audit | Pending | Per-ability matrix; first/third-person captures; reduced effects/shaders; fusion behavior |
| C | Four ecosystems and twelve creatures | Pending | Habitat/AI/spawning/persistence tests; custom art and animation; observation gameplay |
| D | Locations, items, acquisition and counterplay | Pending | Generation/loot/recipe tests; combat matchups; equipment/inventory verification |
| E | Lore, quests, field journal and guide design | Pending | Rendered book review; discoverability; useful clues; text/nav checks |
| F | Early/mid/late progression and integration polish | Pending | Playable journey; multiple builds; existing-world upgrade; multiplayer/performance checks |

Each milestone must integrate code, generated art, sounds, recipes and documentation, capture actual gameplay, produce a tested JAR, record limits and commit/push completed work. Public releases/CurseForge uploads require an explicit user request.

## Visual and gameplay invariants

Casting circles stay behind the caster; spell-specific formation and discharge stay readable. Fusions interact mechanically and visually. Aura uses physical expression and no magic circles. Cinnamon retains her custom model, configurable owner, immortality and functioning interactions. Powerful attacks have practical counters; first contact must not consistently decide combat. Important information has non-colour cues and adjustable visual/sound intensity.

Use existing systems when useful, but preserve authored identities. No uncontrolled populations, destructive ecosystem loops, unbounded effect/cache growth, mandatory daily chores or progression based mainly on repetition. Lore connects to creatures, resources, locations and choices.

## Completion audit

For every numbered goal requirement and every target above, record the implementation paths and authoritative evidence. Verify actual behavior, persistence, packaged assets, gameplay readability and performance where applicable. Distinguish source inventory, unit checks, integrated client tests and real multi-client evidence. Missing evidence means incomplete. A tested foundation feature does not complete a milestone or the whole goal.

## Increment ledger

- Existing expedition client regression also passes after the battlefield addition. The physical terrain art writer now uses explicit UTF-8/LF output to prevent Windows regeneration drift.
- Terrain training: pushed at `828c3fc2`, 895 passing unit tests and dedicated runtime/Aura regression suites. Review package: artifacts/review/living-world-training.
- Marchkeeper battlefields: three outdoor landmark layouts, three lore entries (nine pages), three discovered technique intents, custom memorial/book art and four memory sounds. Full build: 899 passing unit tests. Dedicated runtime suite passes all three layouts in four orientations, provenance save/load, actual player interaction/interruption, technique teaching, finite rewards, real book reading and registered structure placement on normal terrain. Assets: 4,621 generated paths verified; guide: 103 pages validated. Review package: artifacts/review/living-world-battlefields. Natural spawn frequency across seeds and a real two-client session remain unmeasured. This does not complete Aura step 11, the six additional dungeon locations, or the larger lore target.
