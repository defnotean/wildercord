# Articulated opening style forms

This bounded, default-off presentation slice adds the original 20-joint backend to
**Kindling Draw (player activation 3)** and **Frostbite (player activation 4)**.
They remain existing first-form sword strings, `swing swing low`; neither is a new
shared key, new art, or new damage mechanic. The other 38 style arts do not acquire
this articulated body choreography in this change. This is not full animation or
release acceptance.

Later work gave the remaining style arts their own Classic and articulated windups; this note only records the first slice.

## Semantics and unchanged authority

Kindling Draw's existing performer cuts a low 120-degree arc, ignites its victims,
and releases a separate advancing ground-fire line. Its new body pose gathers at
the rear hip, opens the elbow through a low draw, and follows through with a wider
counterbalancing guard. Frostbite's existing performer cuts a low tilted arc,
slows and crusts its victims, and freezes on the third crust. Its new pose presents
a compact low point, makes one measured cut, and folds back into closed guard.
The point is a chamber, not an additional thrust or damage event.

Both consume the original server `Performed` identity and accepted **6-tick windup,
12-tick recovery**, with one ACTIVE tick at the accepted release. The authored feet
remain planted; pelvis offsets move only the model. Server targeting, arc/reach,
cost, payment, cooldown, damage, cancellation, committed aim, party protection and
independently scheduled effects are untouched. The original rigid fallback clips
are untouched.

The only production file changed is `ArticulatedCombatPose.java`. It adds two
motions and their player admission/selection. It does not change sampling,
interpolation, socket transforms, rig/skin layout, rendering adapters, deferred
submission, materials, outlines, or compatibility gates. Unsupported equipment,
unknown layers and protected states retain the existing complete fallback.
All existing opt-in gates remain unchanged.

## Regression and offline evidence

`ArticulatedOpeningStylePoseTest` checks actual style IDs/windows, unsupported
moves, planted soles and rigid links, finite and immutable palettes, mirrored
body/view poses, wrist bounds, continuous phase/idle edges, visible grip/guard
placement, and distinct low-draw versus closed-guard semantics. It also compares
all local and world body/view floats for IDs 0–2, both hands, every 0.125 tick,
against independently captured SHA-256 fingerprints from immutable base
`ae9d57d8f3f631aeaf67ad11b2dc643da764d379` on Java 25. This includes the full-weight
sockets and complete windup/recovery windows.

`ArticulatedOpeningStyleArmorTest` checks stock leggings/boots collar orientation
and the .55-pixel funded Aura-shell topology across both new forms, hands, widths
and body regions. The production armor and shell tools separately inspect actual
baked geometry. An initial Kindling knee-collar reversal was corrected by reducing
its authored hip drop; no mesh, shell or previously accepted pose was changed.

Reproduce the source-driven offline checks in fresh output directories:

```sh
python3 tools/preview_articulated_opening_styles.py --out /tmp/opening-style-preview
python3 tools/check_articulated_armor.py --opening-styles --out /tmp/opening-style-armor
python3 tools/preview_articulated_shared_armor.py --opening-styles --out /tmp/opening-style-armor-preview
```

`ExportArticulatedPose`, `ExportArticulatedSharedArmor`,
`CheckArticulatedArmorGeometry`, `CheckArticulatedArmorView`,
`CheckArticulatedAuraShell` and `CheckArticulatedAuraShellClearance` accept the
additive `--opening-styles` domain. Existing default and `--shared-player` domains
are retained. The tools use production pose/geometry and already verified official
runtime dependencies. Their outputs record source hashes; nothing is native game
footage.

For the reviewed source, the body/FP skin+sword sweep has 145 frames per clip/hand,
both skin widths, no independent hand/forearm intersections, no canonical reticle
coverage, and at least .312977 blocks of opaque near-plane clearance in the bounded
free-look grid. The 192 conservative HUD-reserve samples cover 854×480/GUI2,
1280×720/GUI3, 1280×960/GUI4 and 1920×810/GUI3 at ages 0, 4, 6, 9.5, 16 and 18.
Every sampled grip stays above the reserved HUD band; minimum visible bare
sword-hand/guard-hand pixel counts are recorded in the exact-source report. The
canonical reticle remains clear; the peripheral 5-percent inspection box reaches
3/112 covered pixels during the transition, so zero whole-box coverage is not claimed.

The stock armor audit has zero reversed triangles across 1,020,800 triangles,
zero exposed skin/overlap samples, and .217794 pixels minimum equipment-slot
separation. Its 93,960 camera placements retain at least .242627 blocks of
near-plane clearance, with zero canonical reticle coverage. Six armor contact
sheets show the full four-slot body, FP armor arms, and bounded free-look stress
for both hands and synthetic skin widths. The images omit live materials, trim,
glint, native lighting and the actual translucent funded shell pass.

The actual .55-pixel funded-shell audits separately pass 2,320 immutable/palette
snapshots and 40 negative cases, with all 2,756,160 skin/overlay samples contained
and all 705,280 triangles outward across the full body and FP age/hand/width domain.
They use freshly compiled pose/geometry and the hashed source-identical cached
`AuraShellLayer` class for its official source bake, with pristine runtime jars.

### First-person semantic review and rejected candidate

The initial numeric-safe version left both blades almost upright in first person.
Fine source projection confirmed that difference was insufficient. The final
Kindling-only camera grip attitudes now gather outward, draw low across the lower
view, then retract toward the sword-hand side before returning to idle. At release,
its projected blade angle differs from Frostbite by about 58.6 degrees; the opaque
tip moves about 395 pixels horizontally versus 53 pixels at 854×480. The additional
fine-release sheet shows six samples from 3.875 through 6 ticks in both hands.
These are source geometry measurements, not hit/contact or native pixel evidence.

An intermediate low-follow candidate crossed the reticle during recovery at ages
12–13. That rejected sweep is retained as diagnostics and excluded from accepted
evidence. The corrected outward recovery passes the entire 0.125-tick pixel sweep,
including the return to idle. All body palettes for both new forms and Frostbite's
entire body/view palette remain exact-equal to the initial checkpoint; only
Kindling's first-person palette changed. Full armor partly conceals its early
foreshortened blade, while release shows the low crosscut. That native readability
tradeoff must still be inspected in motion.

The final pose source SHA-256 is
`079ecb4d1e66faafff1cfcfba6e5a4711216aa68e020d917f555afe0ff91e05c`.
The reconstructed Java 25 verifier compiled 780 main, 252 client, 144 test and 346
GameTest sources with unchanged source hashes, and all 1,256 JUnit tests passed.
All 268 Python tests and changed-tool syntax checks passed. Neither Loom nor the
native client was launched by those checks. Independent source/visual review found
no actionable issue in this bounded slice, subject to the native limitations below.

## Native helper and deliberately separate receipt scope

`ArticulatedOpeningStyleChecks.body(context)` and `.hud(context)` are now called
from the existing body and first-person focused suites after independent review.
The shared descriptor, catalog and workflow still contain the same four suites.
The new helpers must run before they can establish native acceptance.

Each helper plans **36 separate screenshot requests**, for **72 total**, at
1280×720 with GUI scale 3 and the HUD visible:

- Both forms, both main hands, body and first person
- Bare Glow with shell down: WINDUP, ACTIVE and RECOVERY
- Full enchanted netherite with funded Form shell: all three phases
- Armored Glow and bare funded Form: ACTIVE only
- Full enchanted netherite, funded Form, shell adapter disabled: ACTIVE fallback

Thus WINDUP and RECOVERY screenshots for the two crossed equipment/shell
combinations are intentionally omitted; their continuous source geometry remains
covered offline. Synthetic wide/slim socket, phase, entry/expiry and fallback
probes are clearly labeled and do not claim both live skin widths were observed.
Screenshots label the actual live skin.

Every trial starts a fresh real input string and verifies exactly one actual art
payment, the accepted identity/windows/aim, ordinary string rest, and exactly one
deferred performer completion. The scheduler's separate END_SERVER_TICK clock
allows only a one-tick comparison tolerance around the accepted windup; it does
not change production timing. Funded trials retain a normal resource pool.
Shell-down baseline trials use the first form's real Glow stage. The disabled
shell adapter checks the entire body and first-person fallback. Temporary hooks,
properties, input, window, HUD, camera and hand settings restore in `finally`.

The prefix is **`articulated_opening_style_`**, separate from the existing
`articulated_shared_` 120-trial readback plan. The opening slice leaves that plan
and wrapper unchanged; a separate capture correction strengthens the shared
wrapper's phase gate. A helper's requested/pre-capture phase is never a claim about the
actual rendered/readback phase: logs explicitly say `renderedPhase=unknown`,
`imageReceiptBinding=not_in_scope` and `exactImpactPixelCoverage=unverified`.
Binding these 72 images to actual render/readback phases requires a separate
explicit receipt plan owned by the lead.

The fixed waits alone are about 8.5 seconds per trial at 20 TPS (roughly ten
minutes across both helpers), before screenshot/render synchronization. Based on
existing suite timings, the additional native work is provisionally estimated
at 15–21 minutes; it must be measured on the first real run. This representative
matrix avoids the original 104-trial proposal approaching the current job budget.
Neither the existing 60-minute job limit nor its 120 shared trials is changed.

## Remaining acceptance

Native execution has not been performed for this new slice. Offline images and
geometry do not verify input delivery, Mixin application, deferred material and
outline ownership, GPU transparency, resource packs, native HUD pixels, actual
live skin widths, screenshot phase, or the native elapsed-time estimate.
The helper is integrated into the existing body/HUD suites, using the same explicit 0.5 render delta as its pre-capture state. It must still be executed, and its native artifacts reviewed before
claiming these first forms are release-ready. All release gates elsewhere remain
independently required.
