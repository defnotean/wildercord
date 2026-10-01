# New expeditions and handling relics

The generator now registers eight dungeons, including three new expeditions. New structures appear in newly generated terrain; existing chunks are not replaced.

| Expedition | Regions | Central mechanic | Guaranteed vault relic |
|---|---|---|---|
| Clockwork Crypt | deserts and badlands | A control stores harmful spells, pauses the trap, then releases stored damage toward the gallery after a visible warning | Keeper's Hourglass |
| Living Greenhouse | flower, birch and dark forests | Three life spells or bone-meal uses cleanse the heart and restore a moss bridge; shears choose a one-time mana harvest instead | Living Seedpod |
| Moving Sky Ruin | windswept hills, windswept forests and meadows | Three collision platforms move together; physical use or wind magic shifts them, carries riders and recovers falls | Sky Feather |

Clock damage is capped at 12 and strikes eligible monsters, while the automatic trap announces its four-damage strike for two seconds. The garden's cleanse and harvest rewards are mutually exclusive. Sky movements preflight every stone and landing space, so an obstruction pauses the entire group. No mechanism loads remote chunks merely to run its puzzle.

The three new layouts each have a stable location-derived variant. Crypt variants open cross-galleries and an additional passage; greenhouse variants add a back entrance and raised work benches; sky variants add one or two optional outer paths reached by scaffolding. Ordinary routes remain available. The structure pieces include hallway and vault loot, guards, distinct control panels, and vault warding.

## Relic tradeoffs

These reusable charms affect spells while held in the off-hand, occupying the hand that could otherwise hold another casting item. Each uses its own hourglass, seedpod or feather artwork.

* **Keeper's Hourglass:** +30% spell duration, −15% spell power.
* **Living Seedpod:** −10% mana cost, −15% spell power, +10% cooldown.
* **Sky Feather:** −15% cooldown, +15% mana cost.

These modify existing spell bonus calculations; they do not increase the Cord's rune tier or socket capacity. Rootbound Relic and Stormglass Relic remain the Root Guardian's and Storm Conductor's reusable tactical trophies.

ExpeditionsTest constructs real rotated pieces, checks their controls and loot containers, applies real clock/garden spells and verifies platform movement and fall recovery. ContentSystemsTest checks all three charm benefits and drawbacks. A direct piece test does not prove natural terrain placement for every possible world seed.
