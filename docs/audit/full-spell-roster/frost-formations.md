# Authored frost and water preparations

All thirty built-in effects whose runtime element is frost have explicit two-beat preparation
recipes. Water effects belong to that runtime family but use water droplets, membranes, flow lanes
and vapor. Cold effects grow angular ice, cracks, plates, teeth and branching crusts. The shape
continues to select delivery; recipes use the existing group-aware caster/aimed/fixed assembly.

| Effect | Authored preparation |
|---|---|
| Chill | Low curling breath with a gathering fine crust. |
| Frost | Branching contact spine grows a crystalline crust. |
| Freeze | Opposed ice plates slide inward and latch around a hollow center. |
| Tidebreath | Two water lung lobes expand around a rising breath. |
| Icepath | Three stepping lozenges crystallize forward. |
| Bubble | Wet membrane rounds out along two perpendicular seams. |
| Frostward | Ice hood shelters an open warm center. |
| Icicle | Three facets weld a forward needle with a dripping base. |
| Coldsnap | Brittle fans spring apart along a sharp fracture. |
| Mirrorfrost | Glints plane a mirror and turn an incoming ray back. |
| Hail | Five staggered pellets descend through a crackling storm lane. |
| Glacier | Ice columns lock together over stone footings. |
| Blizzard | Snow lanes spiral through a crosswind. |
| Frostbloom | Ice petals unfurl around a living green bud. |
| Black Ice | Dark facets crack along a pale cutting edge. |
| Rime Seal | Forked floor lattice develops around an arcane center. |
| Cryostasis | Capsule closes around a mending pulse and opposed time ticks. |
| Frostbite | Three paired teeth creep into a blood-and-ice core. |
| Absolute Zero | Four cold fronts pinch a dark frozen point. |
| Tidal Lift | Three source beads rise along a curved aqueduct. |
| Rime Causeway | Wind supports three ascending ice courses. |
| Avalanche | Hanging slabs descend through snow and rock. |
| Tidecall | Opposed breakers curl toward their meeting point. |
| Undertow | Downward water twist gathers dark sediment beneath it. |
| Hoarfrost | Creeping fern of rime grows lateral forks. |
| Drowning Word | Broken breath divides into inward-filling water channels. |
| Tidewrit | Three water courses rise into a moving wall with a folding lip. |
| Tidehook | Water hook reaches out while its tether coils behind. |
| Current | Three flow lanes align into a shared forward thrust. |
| Flash Freeze | Water beads become ice between opposed closing frost blades. |

## Existing behavior and presentation

Runtime definitions, `FrostFeels` and `FusedFrostVfx` establish the effects' roles and existing
material traditions. Other impact presentation also dispatches through `ExplorerVfx`, `SignatureVfx`
and the base VFX handlers. Existing cues, delivery mechanics, impact handlers and aftermath remain
unchanged. This increment replaces the generic frost preparation layer only when
an authored frost effect is present. Other supporting element layers remain. Exact built-in IDs
dispatch the art; an add-on with a similarly named effect does not silently receive it.

Fused recipes visibly include their ingredients: storm/hail, earth/glacier, wind/blizzard,
life/frostbloom, void/black ice, arcane/rime seal, time/cryostasis and blood/frostbite. Existing
fusion mechanics remain responsible for their gameplay interaction. These are improvements to
existing abilities, not new abilities or new fusion results.

## Verification scope

The native suite covers encoded packets for all thirty effects in Full and Minimal modes, actual
material particles, bounded front positions and rear circles. Recipe traces verify finite,
nonduplicate, evolving beats and retention of both materials for nine mixed recipes in Minimal.
The Full captures compare the aim-area framebuffer region with an empty native stage. A paid
Survival Bolt/Frost and paid Self/Tidebreath verify production dispatch; the latter also checks
the actual Water Breathing status and caster-attached particles.

Packet fixtures use Bolt as a controlled display carrier; they do not execute all thirty complete
effects. Actual source sprites, lifetime, renderer and event/emission budgets are used. The paid
Self capture uses third person and is not compared with the first-person empty stage.

Full launch/travel/impact choreography, every delivery pairing, linked combinations, third-person
roster, add-on/dynamic runes, quality combinations, shaders, real multiplayer and profiling remain
open. The full living-world objective remains active. Review captures/JAR: `artifacts/review/frost-formations`.
