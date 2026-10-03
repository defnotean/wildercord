# Life preparation and projectile choreography

## Runtime roster (29)

Heal, Grow, Regrowth, Cleanse, Venom, Nourish, Harvest, Reversal, Restore, Bramble, Haven, Glimmer, Fortune, Bloom, Soulbond, Second Wind, Lifebloom, Root Bulwark, Bloomstep, Stitchtime, Vinelash, Remedy, Ancient Seed, Moonpetal, Sporebloom, Glowvine, Rootsnare, Drowse, Ashen Mercy.

Fortune is innate; foreign ownership must reject its paid cast, then owned Fortune must pass. Bloom/Soulbond/Second Wind/Lifebloom are elemental fusions. Root Bulwark/Bloomstep/Stitchtime are signatures. Their support ingredients are respectively Earth/Arcane/Time/Life and Earth/Void/Time. Support survives Minimal quality.

## Authorship

All 29 have explicit preparation and travelling body switch cases. Leaf flutter, seed tumble, petal drift, buoyant spores, tightening fibres, falling sap, pulsing cell walls and stable woody thorns have distinct sprites and motion. Eight material silhouettes each have two authored texture states. Healing closes cell plates; cultivation germinates a seed; Regrowth branches its stem; Cleanse sheds pale petals; Venom carries a puncturing thorn ahead of a dripping sac. Nourish gathers unequal grain; Harvest travels as a bound sheaf. Reversal preserves a closed reserve bud. Restore stitches fibres between tissue pieces. Bramble presents outward thorns, Haven carries a leaf canopy, Glimmer spreads separate lichen tiles, Fortune sheds a seed from a clover silhouette. Bloom grows petals around an earth seed; Soulbond retains two tissue nodes linked by living fibres; Second Wind lifts a seed between differently drifting leaves; Lifebloom pulses a living bulb with separated petals. Root Bulwark carries upright trunks, Bloomstep retains petals around a void opening, Stitchtime alternates seams alongside time. Vinelash snakes along the travel direction. Remedy has medicinal leaf, sap and a distinct blossom. Ancient Seed carries a large seed with asymmetric shoot. Moonpetal sends a staggered pale petal fan. Sporebloom sheds granular spores under its cap. Glowvine bears unequal berries along a hanging stem. Rootsnare clamps living branches. Drowse floats softly descending pollen behind one curled petal. Ashen Mercy closes scorched leaf scraps across a living patch, sheds spent affliction as sap, and carries a separate residual heat seam. Its Fire + Life ingredients remain visible in Minimal.

The shared pen only emits individual materials. No whole-effect template is recoloured or shape-swapped. The particle implementation uses world lighting. Prepared Self must remain at the caster and must never generate a flying bolt. Harmful, helpful, world and movement outcomes stay under existing server gameplay authority.

## Native acceptance for the initial 28-rune family (2026-10-03)

| Focused suite | Result | Duration | Screenshots |
| --- | --- | --- | --- |
| LifeRecipeTest | Passed | 27s | no world screenshots required |
| LifeFormationTest | Passed | 1m03s | 31 |
| LifeFlightTest | Passed | 1m46s | 113 |
| LifeDeliveryTest | Passed | 34s | 6 |
| LifeFlightVariantsTest | Passed | 31s | 18 |

Logs and screenshots are preserved under `artifacts/review/life-choreography/{recipe,formation,flight,delivery,variants}`. Initial harness failures are preserved separately: a leftover Earth-count assertion expected 32, an expired Self effect was subjected to a projectile pixel assertion, and an undead Husk rejected the vanilla Poison marker despite taking actual venom damage. Each was corrected in the test setup; gameplay gates and effects were not relaxed. Hostile Venom acceptance uses a living Pillager and checks actual health loss plus Poison. Bramble acceptance includes a real incoming mob attack and reflected damage. Heal checks actual increased health; Grow checks actual wheat age; Bloomstep checks safe arrival from an actual Bolt impact.

All 28 runtime effects were cast through paid Survival Bolt in Full and Minimal quality, including foreign Fortune refusal followed by owned Fortune acceptance. Formation packets checked rear circles, no generic luminous projectile body, bounded front material, and every style's stream-codec round trip. All preparation and moving-body traces are distinct and change over time. Fusion ingredients remain in Minimal. Particle emission resumes after clearing the renderer and removed projectile IDs retire. Alternate paid Arc, Pierce and Frugal routes retain their motion/metadata. Covered Heal/Shock preserves both authored materials; uncovered Heal/Umbra retains the legacy fallback. World light, Reduced Flash and vertical/stationary directions are checked.

Directly inspected native Ancient Seed, Venom and Haven preparation, plus Vinelash and Drowse travel. Flight close views isolate retained production particles for readability and are diagnostics; ordinary player screenshots are preserved separately. Life materials are textured particle billboards, not 3D plant meshes. Recipe emission bounds are not a frame-time benchmark; no measured performance improvement is claimed.

This completes preparation and moving projectile bodies for the existing 28 Life effects. Individually authored impacts, persistent outcome presentation and original per-spell sound identity remain separate work. Existing effects still apply through the authoritative server delivery paths. New signature effects must expand the exact runtime roster, recipes and native acceptance before coverage is claimed.

## Ashen Mercy expansion accepted (Life 29)

The complete fusion group adds Ashen Mercy as the 29th runtime Life effect. Its independent preparation and moving-body recipes, exact FlightBodies coverage and Fire+Life ingredient assertions are implemented. The full 29 recipe suite passed in 19s, formation passed in 1m04s with 32 screenshots, and paid Survival flight passed in 1m52s with 117 screenshots. All 29 were verified in Full and Minimal quality; the added heat ingredient is explicitly asserted in Minimal. Evidence is preserved under `artifacts/review/life29-choreography/{recipe,formation,flight}`. Initial 28 acceptance above remains valid as historical evidence; the later reruns establish complete current 29 roster coverage. Actual Ashen Mercy gameplay, impact and original cue evidence belongs to the separate fusion suites.
