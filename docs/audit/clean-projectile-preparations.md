# Clean elemental projectile preparations

Pure authored Fire, Frost and Storm projectile groups now suppress the generic shape orb and line, using each exact preparation roster and requiring every effect to be covered by that family. The projectile delivery list remains Bolt, Arc, Orb, Spark, Comet, Ricochet, Cluster and Wisp. Self, beam, rain and other deliveries keep their shape staging; uncovered mixed groups keep their existing fallback.

The focused native suite `CleanProjectileFormationTest` uses real Survival mana-paid Bolt casts of Ember, Chill and Jolt in Full and Minimal, checks rear glyphs and front material assemblies, rejects generic orb/ray particles inside the front body volume, and checks an actual mixed Ember + Umbra cast retains the generic orb/ray. Jolt deliberately retains its authored conducting ARC stroke: removing every LightParticle would erase part of its custom recipe.

Native verification: `runClientGameTest -PfocusedSuite=dev.wildercord.client.fx.CleanProjectileFormationTest` passed in 41 seconds. All eight real paid casts (three pure families plus mixed fallback, in Full and Minimal) passed particle, placement, mana and screenshot assertions. The exact-step screenshots retain preparation material at the early release boundary; this is not a full animation capture.

Five screenshots (background, Ember, Chill, Jolt, mixed fallback) and the passing log are preserved under `artifacts/review/clean-projectile-preparations`. The three pure representatives were directly inspected: Ember keeps a crooked cinder hook, Chill its low breath with a frosted lower edge, and Jolt its interrupted conducting clamp. The first two test failures are retained as `first-failed.log` and `second-diagnostic.log`; they identified rear glyph strokes outside the front-body assertion scope. The corrected suite checks those rear strokes independently without changing their production behavior.

The test restores the prior quality, camera, HUD visibility and window dimensions in `finally`.

The paid-cast path also emits short rear glyph light strokes behind the caster. These are checked separately from the front projectile body; the regression leaves those glyph strokes intact.

## Complete affected preparation rosters

After the Canvas suppression change, the full Fire, Frost and Storm preparation
suites were rerun against the combined development sources. Fire passed in 65s
with 31 original screenshots, Frost in 70s with 35, and Storm in 51s with 23.
All 29 Fire, 32 Frost and 20 Storm runtime effects retain authored material
particles, rear circles and bounded front assemblies in Full and Minimal,
with visible-pixel assertions on the Full screenshots. Each suite also checks
a real paid representative cast. Fire's
packet fixture now derives actual fusion ingredients from VisualElements,
matching the other family suites, instead of hardcoding Fire alone.

Evidence is preserved under this review directory's `FireFormationTest`,
`FrostFormationTest` and `StormFormationTest` subdirectories. These preparation
checks cover the concrete regression risk from removing the generic front
layer; they do not establish every impact, sound or alternate delivery.
