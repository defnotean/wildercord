# Original articulated shared player arts

This bounded continuation starts from reviewed local renderer checkpoint `ce8eb5cb59aba3b7ead486e83f25fa7c9536b84c`. It extends the existing default-off articulated renderer to the already implemented **player** activation IDs 1 (Rising Break) and 2 (Driving Cut). NPC attack ID 1 remains the independently sampled Master SWEEP. No server implementation, protocol, input mapping, accepted yaw/pitch, cost, damage, reach or gameplay deadline is changed.

## Original motion and ownership

Rising Break compresses its hips and knees into a low chamber, unfolds into an upward diagonal release and finishes high. Driving Cut retracts its point beside the ribs, extends the pelvis/chest/elbow toward its planted lead foot, then recoils. Every limb retains the existing rigid segment lengths; the two ankle plants stay fixed throughout full-weight commitment. These are authored local mesh shifts, never entity travel, hitboxes or a new movement mechanic.

Both moves have independently composed first-person arms. The hand hierarchy, subtle wrist flexion and child socket keep the grip attached while the authored blade attitude changes. Driving Cut's view blade is slightly raised during the forward extension so its foreshortened point remains readable. The first-person origin, native 70-degree hand projection, existing accepted-aim clamp and conditional near-plane clearance remain unchanged. Canonical grip and guard anchors remain above a conservative 42-GUI-pixel HUD band for 854×480/GUI2, 1280×720/GUI3, 1280×960/GUI4 and 1920×810/GUI3.

The existing `wildercord.articulated`, armor and armor-arms switches remain default-off. Existing armor compatibility, primary-body ownership, deferred immutable palettes, hit-stop activation identity, model reload behavior and whole unsupported-equipment/layer/posture fallback are retained. Ordinary locomotion is still the original body's responsibility. No new asset is copied from another combat mod.

The new player dispatcher delegates ID 0 directly to the untouched Spellcut sampler. The original default exporter and `--schools` exports are byte-identical to the exact baseline over every sampled body/view joint and both hands: Spellcut/Master SWEEP is 6,055,272 bytes with SHA-256 `5aeee1bc1418e22d68c51adba4be3eafc85c731091c7d64c89132465ca401b46`; Gale/Stone is 12,585,603 bytes with SHA-256 `d6048ebb68318d08429745078415aa23d7946644c60cd8e14fd3d02d5a0ea035`.

## Clocks and local verification

The player sampler reads the accepted window without adding a tick: Rising Break is 8 windup + 18 recovery ticks, Driving Cut 6 + 14. The first recovery tick is the single release/active phase, matching existing player semantics. Expiry returns the exact inactive palette. Cancellation or a replacement activation cannot revive the previous hit-stop palette.

`ArticulatedSharedPlayerPoseTest` checks the separate player/NPC namespaces, exact phase/expiry/invalid-input behavior, distinctive lift/extension/recoil, flat soles, fixed plants, rigid lengths, wrist limits, finite/immutable matrices, handed reflection, body/view phase and idle seams, and canonical wrist/guard HUD anchors. Existing Spellcut and school pure tests also remain enabled. Independent all-source compile/JUnit evidence and the separate bare-skin and actual-netherite geometry results are recorded below; none substitutes for a native run.

## Offline reproduction

Run `python3 tools/preview_articulated_shared_player.py --out <fresh-output-directory> --verification <official-cache-directory>`. It exports actual production pose matrices and rig polygons, then makes four contact sheets explicitly labelled **OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE**. It audits all sampled first-person opaque geometry against the near plane over the existing accepted-aim grid and rasterizes canonical-aim reticle visibility. Each report records source hashes and verifies that the sources stayed unchanged. `--visual-only` skips those numerical audits and cannot establish their pass.

The offline images omit native armor layers, lighting, deferred rendering and live third-person held-arm/head/breathing baseline composition. The item display matrix comes from pristine Minecraft's native `ItemTransform.apply`, while opaque item edge polygons are reconstructed from the stock sword sprite. Native screenshots must be inspected separately.

## Native capture contract, pending execution

The existing `ArticulatedCombatPresentationTest` and `ArticulatedFirstPersonCompositionTest` call the new additive `ArticulatedSharedPlayerChecks` helper. There is no extra CI job or test catalog entry, and existing Spellcut/NPC fixture bodies and capture helpers are unchanged. Only the new trials prepare stage 4, because the real server requires it for Rising Break and Driving Cut.

Each new phase trial presses the actual registered key, waits for the exact accepted player activation ID and requested windup/active/recovery phase, verifies the unchanged payload windows/aim, then captures the native framebuffer with the HUD visible. Separate fresh activations per requested phase avoid one screenshot's readback latency skipping the next phase. Third-person coverage uses both hands, bare skin and a full enchanted netherite set at 1280×720/GUI3; HUD coverage uses both hands, bare skin and enchanted netherite chestplate arms across all four supported viewports. Actual live skin width is recorded rather than inferred from the selected hand. Synthetic native model probes separately test wide/slim hand sockets, positive-weight convergence to the actual vanilla held-grip at both edges, whole posture/layer/armor/offhand/locomotion fallback, held-palette replacement and cancellation.

Names are distinct from Spellcut and school evidence:

- `articulated_shared_rising_break_third_1280x720_gui3_<skin|netherite>_<left|right>_requested_<windup|active|recovery>.png`
- `articulated_shared_driving_cut_third_1280x720_gui3_<skin|netherite>_<left|right>_requested_<windup|active|recovery>.png`
- `articulated_shared_<rising_break|driving_cut>_hud_<width>x<height>_gui<scale>_<skin|netherite>_<left|right>_requested_<windup|active|recovery>.png`

There are 24 new third-person phase trials and 96 new HUD phase trials. Their existing rest waits total at least 12,600 unpaused cooldown ticks, equivalent to 10.5 minutes at 20 TPS, plus world/input/expiry/capture overhead. This is simulated-time accounting, not a measured native wall-clock duration. The existing 60-minute native job is unchanged. `ARTICULATED_SHARED_SAMPLE` log records include exact accepted move, activation start tick, windup/recovery, actual skin, pre-capture age/phase, requested phase and `renderedPhase=unknown`. Filename phase suffixes are explicitly `requested_*`, never actual pixel phase labels. Curation must not infer rendered phase from the filename or the pre-capture receipt. A pre-capture phase receipt does not guarantee the framebuffer remained at that exact phase during readback, so logs explicitly retain `exactImpactPixelCoverage=unverified` and `nativePixelReviewRequired=true`. Missing pre-capture phase admissions fail rather than being renamed or claimed as covered. Native results and visual acceptance are pending; local compiler/geometry evidence never claims gameplay or GPU success.

## Initial local gate at checkpoint `2d7d8916`, 2026-10-05

The exact-source independent Java 25 gate `articulated-shared-player-requested-phase-20261005-2033` completed at **20:33:58 UTC**. All **780 main / 250 client / 140 unit-test / 321 native-test source files** compile; all **1,241 JUnit tests** pass with no skips, aborted tests or failures. All **3,425 source/configuration fingerprints** remain unchanged through the gate. This includes the full-netherite capture matrix, strict positive-weight native grip seams and final requested-phase naming.

All **169 Python tooling tests** pass with the official Java 25 directory on `PATH`. An earlier attempt without that directory failed the tooling test that explicitly requires Java; it was an environment failure, not a claimed pass.

The final offline export verifies unchanged source hashes and actual rig polygon parity. Across **119,880** sampled first-person placements (two moves, both hands/skin widths and the existing accepted-aim grid), the closest opaque vertex stays at least **0.302609 blocks** beyond the 0.05-block near plane. Canonical 256×144 raster checks show zero reticle or central inspection-rectangle coverage. The least visible sword occupies 309 pixels in Driving Cut and 786 pixels in Rising Break at that audit resolution; those are offline readability diagnostics, not a quality threshold or native proof. Independent source review additionally found zero sampled opposite-hand/forearm intersections across **11,840** base-cuboid pairs; it excludes inflated overlays, armor, same-wrist hinge wedges and continuous-time guarantees.

Independent review found no blocking source issue after the full armor/seam additions and the final requested-phase provenance correction. Actual Minecraft execution, shader/material/glint/trim behavior, armor enclosure through the new motion and native framebuffer visual acceptance remain pending. None of these local results launches Loom, mixins, a game client or a GPU.

## Actual armor domain and bounded correction

The initial **119,880-placement** preview audit measured bare skin/overlays and the sword. It did **not** validate netherite enclosure or enhanced armor arms; the preexisting Spellcut armor pass was never treated as evidence for the new clips. A subsequent explicit `--shared-player` domain in the actual armor geometry tools uncovered **3,664 reversed body-leg triangles** in the initial new motion. Skin/overlay enclosure and nested equipment-slot containment passed, but the existing positive-orientation gate correctly rejected those folds. The intermediate reduction still failed with 88 reversed triangles; neither result was accepted or hidden.

The correction changes only four coordinates in the new motion keys: Rising's chamber pelvis drop is 1.10 pixels rather than 2.05; its impact pelvis drop is 0.29 rather than 0.95 and forward offset is −0.05 rather than −0.35; Driving's impact pelvis drop is 1.25 rather than 1.60. Fixed foot plants, arm/weapon/view keys, accepted windows and all old clips remain unchanged. The global armor deformer, renderer and skin geometry remain byte-identical. Rising retains its upward hip release and high blade finish, and Driving retains its forward hand extension and recoil. No server movement or mechanic changed.

`ArticulatedSharedPlayerArmorTest` now checks positive triangle orientation through both shared arts, both hands, both leg regions and stock-sized leggings/boots collars at 1/8-tick intervals. It demonstrably fails on preserved checkpoint `2d7d8916` and passes the correction. It is a focused pure regression using the stock 4×12×4 leg extents/pivots and 0.4/0.9 inflation; the independent pristine-runtime-bake audit supplies the wider geometry evidence rather than pretending this unit fixture is a native render.

Run `python3 tools/check_articulated_armor.py --shared-player --out <fresh-output-directory> --verification <official-cache-directory>` to reproduce that wider audit. It uses actual `EntityModelSet.vanilla()` armor-slot bakes and production `ArticulatedArmorGeometry.setupAnim` deformation. Across **209 Rising / 161 Driving poses per hand and skin width**, including both endpoints, it checks all four body slots and the separate first-person chestplate-arm mesh. The final results are:

- **4,955,040 skin/overlay surface samples** enclosed; zero outside
- **1,731,600 nested-shell samples** enclosed; zero outside
- **1,302,400 deformed triangles**, zero reversed; minimum orientation cosine **0.052403**
- Minimum sampled leggings/outer-slot separation **0.172047 pixels**
- **119,880 armor-arm camera placements**, minimum near-plane margin **0.233326 blocks**, zero sampled canonical center or reticle coverage

The existing equipment-input and immutable-palette regressions also pass separately (99 input checks and 2,580 palette captures). Those checks retain their original domain and are not presented as new-move native evidence. Running the armor tool without `--shared-player` reruns its original Spellcut domain; that full default audit also passes unchanged thresholds. All audited source hashes remain unchanged through each final run.

Actual-armor contact sheets can be reproduced with `python3 tools/preview_articulated_shared_armor.py --out <fresh-output-directory> --verification <official-cache-directory>`. Its companion Java exporter reads production-deformed `ModelPart` polygons directly. Six clearly labelled offline sheets cover both moves, wide/slim skins, both hands and four keyframes: full four-slot netherite over the body, enhanced first-person armor arms, and separate bounded free-look stress. Stock humanoid/leggings texture alpha is retained. The 96 bounded image cells show visible armor and sword with no sampled reticle hit. These images use approximate offline shading and omit native material/glint/trim/deferred rendering. Driving's blade becomes nearly edge-on at the extreme free-look chamber, so native readability remains a visual acceptance gate.

All **740** exported new-player first-person frame palettes/timelines remain value-identical to checkpoint `2d7d8916`; the four first-person armor PNGs are byte-identical before/after the pelvis correction. The default Spellcut/Master SWEEP and Gale/Stone exports were compared again to the original `ce8eb5cb` baseline and retain the exact hashes recorded above.

The final frozen gate `articulated-shared-player-armor-fixed-20261005-2051` completed at **20:51:26 UTC**: **780 main / 250 client / 141 unit-test / 321 native-test sources** compile, all **1,242 JUnit tests** pass, and all **3,427 source/configuration fingerprints** remain unchanged. All **169 Python tests** also pass. Independent source/geometry review cleared the four-coordinate correction, strict regression, audit extension, actual-armor exporter and all six contact sheets.

These remain finite CPU geometry and offline raster samples. They do not prove every continuous pose, live held-arm/head baseline, arbitrary resource-pack geometry, opposite-equipment collision, trim/glint, GPU output or exact framebuffer phase. Actual accepted-input Minecraft captures, native armor/material compatibility and visual acceptance remain pending in the existing articulated suites.
