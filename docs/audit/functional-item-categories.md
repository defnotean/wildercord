# Functional item category audit and proposed follow-ups

## Current accepted ledger — 2026-10-04

The historical proposals below retain their original scope. Current accepted additions are 27: relics2 (Sleeping Blade, Mara's Empty Bell), equipment4 (Cave Breather, Rootbound Greaves, Reedwater Waders, Dewglass Spectacles), tools5 (Ridge Whistle, Draft Kite, Dewglass Lens, Reed Rattle, Bank Surveyor's Line), placeables7 (the six historical entries plus Cinder Fern), and materials9. At least six relics, four equipment and one tool remain to meet category minima, yielding 38 total. The accepted Tideward mechanics are shallow supported walking, crouched actual warning reading and private sampled crossing guidance; proposed wet-slow immunity, peripheral-view reduction, companion route commands and Tide Needle are not accepted features. See tideward-milestone.md and living-world-remaining.md for current native evidence and scope.

Classification against baseline `14efc61e`. The Glowcap additions passed their eighth full acquisition/crafting/restart run, seventh natural-terrain run and sixth navigation-bounds run. Existing baseline equipment and spawn eggs are excluded; lore books have their own target. The earlier 17 evidenced additions match root's classification:

| Category | Accepted goal additions | Count / target |
|---|---|---|
| Relics | Sleeping Blade | 1  / 8 |
| Equipment/accessories | Cave Breather | 1 / 8 |
| Exploration/support tools | Ridge Whistle, Draft Kite, Dewglass Lens, Reed Rattle | 4  / 6 |
| Placeable utilities | Windreed, Moonreed, Marshlight, Reed Refuge, Glowcap Cutting, Fungal Nursery | 6 / 6 |
| Consumables/used materials | Stonehorn Plate, Galeclaw Plume, Bastion Poultice, Windreed Braid, Dusk Pearl, Moonreed Floss, Mycelial Dew, Fungal Poultice, Dried Glowcap Gills | 9 / 8 |

The accepted Nursery gameplay adds Glowcap cutting and Fungal Nursery to placeables, Cave Breather to offhand equipment, and gills to used materials. This establishes **21 functional items** = 1 relic + 1 equipment + 4 tools + 6 placeables + 9 consumables/materials. Three written books are additional lore, not functional-item credits. Clear original native captures and the combined checked review JAR are recorded in `life-fieldcraft-milestone.md`.

This leaves seven relics, seven equipment/accessories and two tools to fill the stated category minimums: 16 more items, yielding 37 total because gills is a ninth material. Recommend treating 36 as a minimum and delivering 37 meaningful items; do not relabel a consumable to force arithmetic. The goal is a substantial minimum; 37 meaningful items can satisfy the category breadth without filler.

## Coherent next milestone proposals (not implementation/count credit)

### Ember wardens / The Cinder Bailiff

Original hostile woodland creature, telegraphed ash rake and recoverable charged lunge, ember ecosystem interaction with charred grass/living fern, non-destructive counters using Water/Life and physical attacks. Relic **Last-Coal Censer** stores one extinguished hazard in finite wear to fuel one explicit area interruption with self smoke/visibility tradeoff; accessory **Bailiff's Ash Mantle** reduces the next qualifying burn tick after deliberate stillness, movement breaks preparation and shared rest prevents stacking. Acquisition through ash-witness investigation, not unlimited enemy farm. These would add one hostile creature, one relic, one equipment and one investigation only after complete native proof.

### Belowkeeper / Stolen Third Breath

Complete second fungal threat relationship with original fungal arthropod that seeks mature caps, telegraphs root-line grab, releases victims on physical hit/counter, no persistent terrain destruction or resources per kill. Relic **Mara's Empty Bell** can interrupt one nearby creature windup after an audible committed ring; it costs filter charge and leaves the carrier briefly vulnerable. Equipment **Rootbound Greaves** trade mobility for resistance to one knockback during grounded preparation; no universal immunity or infinite DoT shield. Restored artifacts and two distinct roles through a real fungal ruin location and investigation. Creature/model/art/worldgen/telegraph/loot/persistence/performance required.

### Cooperative Tideward expedition equipment

Two functional equipment choices: **Reedwater Waders** temporarily resist wet slow/rooting while preventing sprint and spending durability; **Dewglass Spectacles** reveal already-loaded ecological warning silhouettes only while crouched, reducing peripheral view and requiring periodic lens replacement. Relic **Tamsin's Tide Needle** exchanges one bounded visible water patch's shape, respecting building/claims and water budgets, with clear previews and a commitment rest. Support tool **Bank Surveyor's Line** marks a short safe crossing for companions from already-loaded footing without terrain edits; anchors expire and cannot grant quest resources. Include real non-colour cues, accessibility toggle and authoritatively bounded queries.

Remaining additions should predominantly equipment/relics. New plants/materials can be dependencies without claiming category progress until they have useful acquisition/tradeoff paths; no need to create extra filler crafting ingredients.


## Source and evidence references

Existing accepted increments are recorded in [the living-world roadmap](../LIVING_WORLD_ROADMAP.md), [Sporeback acceptance](sporeback-snails.md), and [the outstanding-work audit](living-world-remaining.md). Main category sources are `aura/world/SleepingBlades.java`, `aura/world/AuraBeasts.java`, `wildlife/HighlandContent.java`, `wildlife/WetlandContent.java`, `wildlife/WetlandGarden.java`, `wildlife/WetlandShelters.java`, `wildlife/ReedRattle.java` and `wildlife/SporebackContent.java` beneath `src/main/java/dev/wildercord`.

Category sources and actual recipes are `wildlife/FungalGarden.java`, `tools/fungal_art.py` and generated `data/wildercord/recipe/{fungal_nursery,cave_breather}.json`. Native evidence is recorded in `glowcap-nursery-native.md`. This ledger is a category audit, not completion evidence for the larger item target or connected ecosystem.
