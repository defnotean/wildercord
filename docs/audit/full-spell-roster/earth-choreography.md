# Earth spell preparation and flight

## Scope

This milestone authors **all 32 built-in runtime Earth effects**. It includes the six Earth elemental fusion results, the Thunderquake signature fusion, Stoneform's innate ownership, ordinary combat/support spells and excavation tools. Native tests compare the authored list directly with `Runes.all()` filtered by `EFFECT` and `earth`; a newly registered Earth effect requires adding its choreography.

The work covers preparation and the physical body that follows a real moving projectile. It does **not** claim that every impact, delivery shape or sound has been independently re-authored. Existing release timing, effect execution, terrain edits, ownership and shape placement remain governed by the production casting code.

## Materials and rendering

`EarthOption` carries material, color, size, lifetime, drift and angular momentum. `EarthParticle` uses **world lighting and a translucent layer**, rather than a luminous line or an emissive core. Original 32-pixel textures distinguish faceted rock, chipped stratified slabs, separate sediment grains, ragged dust, branching dark faults, ivory bone spurs, bark-covered roots and shaded geode crystals. Each has a second chipped/exposed face.

Fragments tumble with gravity, heavy slabs settle more slowly, dust spreads and loses lateral momentum, roots settle without tumble, crystals rotate gently and fractures hold their scored orientation. Reduced Flash softens the earth particles' opacity. Preparations last eight ticks and moving body particles five ticks. All styles remain within the existing quality budgets; Minimal reduces sampling and secondary debris while retaining defining materials and fusion ingredients.

Earth-only projectile preparation replaces the former generic luminous body with the authored material assembly. Rear magic circles stay behind the caster. Self and targeted placement use the existing authoritative shape assembly anchor. Mixed spells retain independently authored ingredients and uncovered groups retain the fallback body.

## Individual recipes

| Rune | Preparation | Moving body |
| --- | --- | --- |
| Shield | Load-bearing slabs key into an arch | Joined shoulders travel edge first with an open seam |
| Break | A widening fault splits unequal stone halves | Halves tumble outward behind a leading fault |
| Stoneskin | Armour courses close from top to bottom | Overlapping scales shed a lower chip |
| Root | Woody forks grow toward a soil-covered clamp | A curling pointed fork drags soil behind |
| Tremor | Three ground slabs heave in opposing phases | Fault tiles rise and fall out of phase |
| Excavate | A square face loosens separate cutting plugs | A rectangular cut trails freshly freed plugs |
| Aftershock | A first lifted plate receives a delayed lower answer | Unequal front and rear tiles rebound at different times |
| Weigh | A dense counterweight sinks from its sockets | The weight drags two sediment falls beneath it |
| Shackle | Earth anchors draw rough stone tethers inward | Closing anchor jaws pull a jagged rear tether |
| Rampart | Offset parapet courses rise over packed dirt | Crenellated blocks advance on a compacted foot |
| Brace | Diagonal buttresses lean into a keystone | A tilted buttress shelters dust in its wake |
| Chisel | An advancing mineral wedge scores a stone plate | A point leads scraped chips and a bare score |
| Tunnel | Hollow shoulders excavate retreating courses | An open bore carries separate dust collars |
| Vein | Mineral branches draw ore grains into their junction | A central nugget splits into two travelling mineral forks |
| Fell | Bark collars shear around a dropping stump | Broken woody segments cant from a split stump |
| Pelt | Three unequal pebbles gather into a throwing sequence | Staggered pebbles move and tumble independently |
| Stoneform | Broad body plates close around a cracked spine | A dense spine carries a fractured shoulder shell |
| Magma | A cooled shell opens around molten seams | Fire squeezes between rocky jaws and leaves lower cinders |
| Sinkhole | Sediment runs inward around an open void | Inward grit descends into a hollow trailing sink |
| Geode | An asymmetric raw casing exposes crystal teeth | An exposed crystal fan travels inside its split casing |
| Fossilize | A root fossil is buried course by course as time arrests its rind | The fossil holds still while a slower mineral rind closes |
| Bonespur | Unequal ivory spurs extrude from a bloody stone socket | Three barbs lead the socket with blood at their roots |
| Monolith | Heavy stone courses lock vertically | A solid column advances as one mass and sheds base crumbs |
| Strata Rise | Diagonal terraces rise in sequential sediment layers | Courses advance in a repeating rising sequence |
| Thunderquake | Separate fault lips compress before timed thunder | Displaced fault jaws answer each other with small discharges |
| Infest | Segmented stone grubs disturb a crooked seam | A wriggling burrow train travels along a fractured wake |
| Sandstorm | A diagonal grain sheet rolls over turbulent dust | A broad grit front crosses two differently drifting dust banks |
| Tusk Charge | Heavy curved tusks lower into the route | Paired tusks rake above a displaced earthen clod |
| Mire | Saturated clods slump and leak trapped moisture | A drooping mud clod sheds water from its low side |
| Stalactite | Suspended dripstone elongates downward and sheds crust | A cracked point leads independently tumbling chips |
| Basalt Surge | Upright basalt teeth rise in a forward pressure chain | Teeth leap in a forward-timed sequence |
| Prospect | Raw stone opens a mineral window with separate ore answers | A struck nodule carries a separate mineral echo |

## Verification plan and results

- `EarthFormationTest`: exact 32-rune runtime coverage; unique recipes and evolving preparation beats; finite bounded geometry; no `LightOption`; fusion material presence in Minimal; physical-material wire round trips; actual Full and Minimal formation packets with rear-circle placement; visible screenshots; paid Pelt and Self Stoneskin casts.
- `EarthFlightTest`: every rune cast with paid Survival Bolt in Full and Minimal, including foreign innate refusal followed by the owned innate; synchronized identity; actual flight; physical body nearby; retained support ingredients; differing motion phases; continued emission after clearing particles; retired entity IDs; full roster screenshots and diagnostic close views of retained production particles.
- `EarthFlightVariantsTest`: paid Arc/Pelt, Arc/Stalactite, Pierce/Chisel, Frugal/Prospect, Earth/Storm mixed group and uncovered Earth/Umbra fallback; physical world lighting; Reduced Flash opacity; vertical and stationary directions.

All three focused native client suites passed on 2026-10-03: `EarthFormationTest` in 1m17s, `EarthFlightTest` in 1m55s, and `EarthFlightVariantsTest` in 31s. Logs and screenshots are preserved under `artifacts/review/parallel-living-world/earth/{formation,flight,variants}`. Formation produced 35 screenshots, full paid flight produced 129 screenshots, and alternate deliveries produced 18 screenshots. Representative Geode/Pelt preparation and Stalactite/Magma travel images were inspected directly. The close flight views deliberately isolate retained production particles for readability; they are diagnostics, not an unedited player cast.

Native acceptance covered every runtime Earth effect in both quality settings, actual paid projectile travel, the foreign/owned Stoneform distinction, Self Stoneskin, fusion ingredients, Arc gravity, Pierce/Frugal flags, covered Earth/Storm mixing, uncovered Earth/Umbra fallback, world lighting, Reduced Flash and vertical/stationary directions. Recipe comparisons prove distinct evolving traces and bounded emission counts; they are not a frame-time benchmark. The native run recorded one transient server catch-up warning during repeated screenshot capture, so no measured performance improvement is claimed. Earth impacts, persistent world-result animation and sound identity remain separate work.
