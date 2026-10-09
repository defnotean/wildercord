# Unmoved and Null Parry presentation review

This is an original local presentation slice for two existing counter arts. It does not
establish native gameplay, owner/observer rendering, or release readiness. The separate
mechanics slice owns paid admission, the Unmoved defensive lifetime, and Null Parry's
pulse-before-primary ordering.

## Choreography

- **Unmoved, presentation ID 24, 6/16 ticks:** a broad, low Stone brace; a short level point
  driven from the ribs by elbow extension; withdrawal into the loaded stance. Articulated
  pelvis, spine and chest have separate small turns, both feet are planted through the
  full-weight sequence, and both shoulders/elbows are independently posed. The original
  rigid fallback has its own six-joint choreography and an ID-24-only hilt pitch for the
  thrust. First-person keys expose the blade's face while keeping both hands peripheral.
- **Null Parry, presentation ID 25, 4/14 ticks:** receive outside the dominant shoulder on
  an oblique edge, sweep inward across a compact pocket, then fold the elbow and close.
  It has separate body and camera-space keys. The existing pulse does not create an extra
  physical attack or animation reset. The Classic camera wrist stays open at release so
  the blade's face remains readable.

These are model-space poses, not movement commands. No camera steering, FOV adjustment,
entity displacement, target selection or new action input is added. No old key is reused
or recolored. Mirroring uses the existing complete joint/socket reflection. Client
admission already delegates to the presentation support methods; no new client whitelist
or weakened compatibility rule is required.

## Preservation and reproducibility

Before either shared sampler was edited, `freeze_brace_null_baseline.py` compiled only
production Git objects from `dcf242d9cd1fd8a65226f28545ab353506dc3696` in an isolated source
root. The immutable fixture freezes **18,482 rows** for player IDs 0–23, both hands,
Classic body/view and articulated world/view. Each row has four independent SHA-256
palettes covering all joints, bind/local/world transforms, both sockets, Classic pivots
and hand composition. Signed zero is retained. Invalid/expired ages and timings,
fractional ages, boundary neighborhoods, alternate windows and unsupported old IDs are
included. Regeneration with `--check` must match the frozen bytes exactly.

- Compressed fixture SHA-256: `57fe71c19d79e02c1faf7a36aaad16bfccfd032994c55b8424fa526b969b2226`
- Uncompressed fixture SHA-256: `89d7669ea689c5a381c4fd612d16026e340b95b5b4f445db154b1b775214a8b1`
- The manifest records the full original source closure, encoder and Java runtime.

`check_brace_null_mutations.py` makes independent source copies. Four old-ID-22 scalar
mutations must fail their respective Classic-body, Classic-view, articulated-world and
articulated-view palettes. An old unsupported-ID-5 admission mutation must fail fallback
preservation. An Unmoved-only socket offset must fail the new hierarchy invariant while
all old-reference tests continue to pass. Nothing mutates the checkout or fixture.

## Offline verification tools

Set `JAVA_HOME` to the verified Java 25 installation and pass the original standalone
dependency cache with `--verification`. Every runner requires a fresh output directory.

- `check_brace_null_poses.py`: new pose, armor, composition and preservation tests plus
  existing player/NPC pose suites; 191 tests passed on this slice.
- `check_brace_null_geometry.py --compiled <verified-main-client-output>`: fresh compile
  of the pose/rig/armor/shell implementations, original-runtime bakes, unchanged bounded
  armor/view/shell audit thresholds. Supporting class provenance and the compile-only
  fixture are checked; transformed Minecraft classes are never loaded at runtime.
- `preview_articulated_brace_null.py`: actual exported rig polygons and source palettes,
  both hands/widths; chamber, active, follow and recovery; native-sized conservative HUD
  bands, cross-arm SAT, canonical reticle and weighted free-look near-plane checks.
- `preview_classic_brace_null.py`: original vanilla rigid body polygons using Java-exported
  production pivots/head bounds/hilt tilt, plus first-person opaque sword projections.
- `preview_brace_null_armor.py`: production armor polygons over source skin/sword, actual
  shell wireframes, exact arm/socket palettes and bounded free-look/vertical-view sheets.

Original-runtime numeric checks cover 4,312,224 skin samples, 1,133,440 armor triangles,
3,060,288 shell containment samples and 783,104 shell triangles. They report no exposed
sample or reversed triangle. The bare composition run covers 104,328 weighted view
placements and 192 native-sized HUD layouts, with no canonical reticle occlusion or
cross-arm base-cuboid intersection. Finite samples are not a continuous collision proof.

During authoring the original-runtime gate caught a chamber elbow inversion missed by
simpler cuboid fixtures. The authored pose was corrected, without relaxing any threshold.
Interim failed outputs remain separate from the final evidence.

## Cutting-edge readability revision

Review rejected the earlier whole-item-pixel count as a blade-readability claim. The
Unmoved camera-space ACTIVE grip pitch is now 0.73 rather than 1.13; third-person keys,
ordinary gameplay and the old-ID golden reference are unchanged. Both readability
runners use the untouched software-rendered diamond pixels and isolate the sword's
raster item ID before applying the native cyan classifier, four-connected components,
minimum eight pixels per component and unchanged 16-GUI-pixel cutting-edge span.
Cyan skin, cuffs, armor and background pixels cannot contribute.

`check_brace_null_blade_readability.py` passes 960 bare native-sized/HUD samples across
both arts, hands, widths, chamber/ACTIVE/follow/recovery and bounded free look, including
three samples inside each one-tick ACTIVE interval. Minimum connected span is 26.5 GUI
pixels. `check_brace_null_armor_readability.py` passes 160 armored ACTIVE samples; minimum
span is 17.5 GUI pixels, at Unmoved's downward free look in 854×480 GUI scale 2. These are
finite offline source-raster checks; native GPU lighting and continuous readability still
need acceptance. The margin on that worst armored view is explicitly narrower.

## Additive native fixture preparation (not run)

The whole `WildercordMastersArtsPresentationTest` iterates the complete style
roster and will attempt these two real earned-counter inputs after mechanics integration.
It remains intact. The pair-only addition wraps its native phase captures in the existing
bounded wall-clock HitStop wait, retaining the exact accepted owner/action and game tick.
Old capture names use their previous path with no extra context scheduling.

`BraceNullCaptureProbe` now binds the native screenshot's run UUID/sequence/name, owner
UUID/entity, art, accepted activation, hand and exact render-state identity. It derives
actual consumed age from the post-HitStop `ageInTicks` clock, verified against pristine
`EntityRenderer` bytecode, and requires the exact corresponding immutable Classic pose
and hilt attitude. ACTIVE is strictly `[windup, windup + 1)`. Third-person evidence must
come from the submitted model/state pair inside actual deferred `ModelFeatureRenderer`
geometry preparation, after `PlayerModel.setupAnim`; attachment-only setup calls cannot
satisfy it. First-person evidence observes the real native main-hand item submission.
The JSON records expected and consumed rigid palettes, the untouched native body baseline
and bind transforms, expected and actual native item-submit matrices and grip points,
the pre-art hand-entry matrix, and SHA-256 of the exact written PNG. A separate
`BraceNullTransformOracle` computes body and hand expectations without invoking the
production adapters or their pivot/head/view/swing helpers. The observed pre-art body
baseline comes from the real deferred call; the observed hand-entry stack precedes
production HEAD transforms. Missing baselines and model/state/stack substitutions fail
closed. Raw phase metadata alone cannot satisfy either consumed-geometry assertion.

`check_brace_null_native_fixture.py` compiles this additive source, runs the unchanged
native-pixel-policy CPU harness, performs 89 adversarial phase-contract assertions and
verifies the official age-clock bytecode. Pure
adversarial contract tests reject wrong actor/art/hand/action/state/screenshot identity,
frozen windup, a recovery tick mislabeled ACTIVE, wrong pose and wrong hilt angle.
Compilation and these CPU checks are not evidence that mixins applied or a client rendered.
`check_brace_null_probe_adversary.py` compiles the actual probe with explicitly minimal
Minecraft caller shapes, the real samplers and original-runtime PoseStack/JOML. It passes
39 independent controls and rejects 384 malformed/no-op/idle body, hand and displayed-item cases,
including the reviewed all-999 palette, missing matrix and correct-body-with-no-third-item
false accepts. World-item negatives include omitted/reversed Unmoved hilt adjustment,
idle/wrong-handed output, borrowed stack/item/model, missing/empty displayed quads,
wrong baked display matrices, detached/duplicate callbacks and caught duplicate failures. These are CPU
caller-contract tests, not native renderer evidence. Three control cases prove that old
capture names and cancelled-neutral names remain outside the pair receipt.

The receipt now requires both body and an actual third-person held-item draw. A separate
item-layer scope binds the exact submitted model, original attachment baseline, native
outer root, actor/state/action, actual baked skin width, selected arm, item render state,
pose stack and collector. Its independent world-hand oracle reproduces vanilla's arm
translation/rotation and the authored hilt hinge. Correct body metadata cannot substitute
for the real hilt transform.

For both first- and third-person paths, a second observation is inside the original
`ItemStackRenderState.submit`, at the real `LayerRenderState` invocation of
`SubmitNodeCollector.submitItem`. It records finite emitted quad positions and requires
one nonempty draw with the exact item identity and expected display context. The actual
stock handheld display fields and local transform are validated against the official
model; an independent matrix composition must match all 16 submitted values and the
actual displayed model-local hilt point. Missing, duplicate, detached or substituted
callbacks are terminal, including when their immediate assertion is caught. PNG evidence
cannot complete with body-only or pre-item-entry-only observations.

134 additional original-runtime CPU checks compare the world-hand oracle with pristine
`PlayerModel.translateToHand` for both baked widths and hands, and compare displayed
matrices/hilts with pristine `ItemTransform.apply`. They include omitted-hilt and no-op
item-transform negatives. These use the signed original dependency bytes, never the
compile-only transformed jar at runtime.

The new narrow receipt is Classic and owner-only; it does not fill articulated or real
observer coverage. Existing articulated
shared-player/opening helpers explicitly cover IDs 1/2 and 3/4, not IDs 24/25.

Required follow-on native coverage must bind exact accepted owner/action identity through
raw extraction, submission, deferred body/view/armor/shell palettes and unmodified frame
readback. ACTIVE is the one-tick interval beginning at windup, not a two-tick filename
window. Additive counter-specific receipt scope must not reuse or weaken existing shared,
opening, Stone Hinge, Gale or Witness gates. A real second client must prove observer
receipt/rendering independently; owner third-person is not an observer substitute.

Native owner/observer captures, continuous in-game motion, live idle/equip transitions,
GPU materials, trim/glint, transparency, shell lighting, resource-pack fallback, full
Loom/client suites and final integrated mechanics acceptance remain **pending**. No
native launch, publication, release, or deferred feature enablement is implied here.


### Exact connected-fixture gap

`ConnectedCastReceiptTest` currently preserves 46 ordered cases. Its existing
`NextCounterPairedCases` cover Quietus and Nullcatch projectile mechanics, not these
sword-art presentations. The peer observes host outcome readiness, health, charge and
projectile replication after server completion. Those observations cannot establish a
4/6-tick windup or one-tick rendered ACTIVE phase.

An explicit next extension would append eight cases, preserving the old 46-case prefix:
Unmoved/Null Parry × Classic/articulated × right/left. Each needs real owner first-person
and separate connected-observer third-person receipts at WINDUP, ACTIVE, follow and
recovery. Add a pre-input peer-armed handshake, retain real guard/catch/ordinary attack,
correlate the accepted action and both connections, and bind independent raw extraction,
deferred palettes, framebuffer copy and PNG hashes in each process. Do not substitute
an outcome message, synthetic camera actor, request label or owner third-person frame.

The required coordinated files are the new pair-capture helper and its genuine peer
protocol, `ConnectedCastReceiptTest`'s host/peer branches and case union,
`cast-receipt-native-contract.json` (46 → 54), both ordered rosters in
`tools/native/export_two_client_launch.init.gradle` and `tools/native/launch_two_clients.py`,
and `tools/test_two_client_supervisor.py`'s exact-union and adversarial evidence tests.
Keep the original case prefix, two-JVM limit, 2 GiB heaps and 900-second timeout; a full
native run must establish that the enlarged workload actually fits. Actual skin/profile
coverage must be declared and independently verified rather than inferred from offline
wide/slim sheets. None of that connected extension is implemented in this local slice.
