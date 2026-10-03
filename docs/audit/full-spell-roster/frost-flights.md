# Authored frost and water projectile bodies

All thirty runtime frost effects now carry individually authored bodies on actual RuneBolt entities.
Water belongs to this runtime family but retains water membranes, channels, hooks and vapor instead
of receiving generic ice. Recipes follow the actual position/velocity and retain the shared
Power/Frugal/Pierce width controls; Pierce also elongates the body.

| Effect | Moving body |
| --- | --- |
| Absolute Zero | Four cold fronts pinch a dark frozen point. |
| Avalanche | Snow-capped rocks tumble through loose powder. |
| Black Ice | Dark core travels between fractured ice blades. |
| Blizzard | Wind carries a staggered snow front and pale cloud. |
| Bubble | Six wet seams surround a clear air pocket. |
| Chill | Curling breath carries a fine frozen lower edge. |
| Coldsnap | Opposed biting jaws snap around a detached crystal. |
| Cryostasis | Ice capsule carries a living, paused time pulse. |
| Current | Three flow lanes converge into a forward thrust. |
| Drowning Word | Wet channels fill a broken breath funnel. |
| Flash Freeze | Water becomes crystal between opposed cold blades. |
| Freeze | Opposed plates slide about a cold latch. |
| Frost | Branches grow from an advancing contact spine. |
| Frostbite | Ice teeth retain a bleeding central score. |
| Frostbloom | Ice petals open around a living seed. |
| Frostward | Curved rim shelters a heavy lower crystal. |
| Glacier | Stone bears an asymmetric ice-armored shoulder. |
| Hail | Charged hailstones carry a conducting fork. |
| Hoarfrost | Rime fern carries alternating forks along a slender stem. |
| Icepath | Three flat lozenges advance along a cold track. |
| Icicle | Long needle carries three facets at its shoulder. |
| Mirrorfrost | Tilted reflective pane carries an arcane light. |
| Rime Causeway | Ascending ice courses retain their supporting wind. |
| Rime Seal | Turning inset score travels inside opposed corner brackets. |
| Tidal Lift | Three water sources travel through an arched aqueduct. |
| Tidebreath | Water lung lobes breathe around a trailing exhalation. |
| Tidecall | Two breakers curl toward a foaming meeting point. |
| Tidehook | Hooked water barb pulls a short doubled tether. |
| Tidewrit | Three wall courses retain a folded, misting upper lip. |
| Undertow | Twisting downward bore carries sediment beneath it. |

## Integration

The shared FlightBodies coverage contract now includes fire and frost. Fully authored groups
replace the generic comet and duplicate server travel particles, keeping their travel voice.
Unsupported, omitted or foreign members retain fallback. Dispatch remains exact by namespace
and family, so adding frost coverage cannot route frost IDs into fire recipes.

This changes flight presentation only. Existing costs, speed, collision, reflection, effect
mechanics, cue/preparation, impact and aftermath remain in charge. Supporting materials survive
Full and Minimal recipes, including charge/hail, rock/glacier, wind/blizzard, void/black ice,
life/frostbloom, blood/frostbite, arcane/mirror/seal, time/life/cryostasis and sediment/undertow.

## Verification scope

FrostFlightTest uses sixty paid Survival Bolt casts across Full/Minimal, verifies runtime roster
equality, exact client identity, real movement, nearby authored five-tick material, bounded and
distinct emission recipes, supporting materials, particle-clear recovery, removed-ID retirement
and actual framebuffer differences. Proximity includes two emission ticks of travel plus a body
offset, allowing network movement to arrive after the latest emission. Kindling's fire ownership
fixture is irrelevant to this family; any future innate still receives its matching ownership.

FrostFlightVariantsTest adds paid Arc/Icicle, Arc/Hail, Pierce/Frost, Frugal/Tidehook, combined
Ember/Frost and unsupported Frost/Shock groups. It verifies actual falling versus straight server
motion, style bits, exact metadata, authored particles and covered/fallback comet selection.
Both authored schools appear in the covered mixed group. Matched empty frames accompany each
variant camera orientation. These are examples, not all delivery/modifier/rank/fusion combinations.

The main roster supplies thirty early/later pairs and an empty reference. Variant early frames
include substantial preparation; later frames separate more of the flight. The controlled stage
does not establish close side-view beauty, natural combat readability, shaders or multiplayer.
Full release/impact choreography, every shape, homing/reflected/linked identity, dynamic/addon
runes and sustained profiling remain open. No new ability/fusion/item/creature/lore count.
The larger living-world goal remains active.

Final evidence: roster 119s, variants 38s, fire regression 108s; 946 passing unit tests; 114 guide pages; 79 original captures, with all thirty roster later frames and twelve variant spell frames inspected. Review/JAR: `artifacts/review/frost-flights`.
