# Original articulated combat proof

This opt-in development slice is a segmented renderer, **not smooth weighted skinning**. It does not add an Epic Fight, GeckoLib or PAL dependency and contains no Epic Fight code or assets. Native play/render acceptance remains pending until the dedicated client suite actually runs.

## Try the bounded slice

Add the client JVM option `-Dwildercord.articulated=true` to a disposable development profile. Default is off. Use a standard or slim 64x64 skin, an empty offhand, no armor/cape/worn backpack or casting gear, a vanilla sword and a standing pose. At Aura Edge or above, press the current Spellcut binding (fresh default U). The original accepted ID 0 still has 4 windup ticks and 12 recovery ticks. Compare first person and third person. SwordMaster SWEEP (ID 1) uses its existing 18-tick tell, one active tick and 19 recovery ticks.

Stable camera is the preview default. It suppresses Wildercord camera shake/nudge/FOV kicks and vanilla hurt/walk bob while the feature is enabled; user look input and ordinary configured FOV remain free. `-Dwildercord.articulated.stableCamera=false` restores the existing camera-effect settings. Existing Impact Full/Soft/Off controls cosmetic hit-stop independently. Rendering never rotates the player's view, blocks input, or extends a gameplay deadline.

## What is really articulated

- 20 parented channels: pelvis, spine, chest, head; shoulder, upper arm, forearm, hand and item socket on each side; thigh, shin and foot on each side
- Separate skin-textured cuboids visibly rotate at elbows, wrists, knees and ankles. Torso slices counter-turn above the pelvis. The feet use two-link IK with flat planted soles and unchanged bone lengths.
- Standard/slim arm widths, independent left-limb UV islands and hat/jacket/sleeve/trouser visibility follow the extracted skin state. Exterior side UVs are sliced from the full original limb rectangle, rather than repeating a full limb on each segment.
- The first-person viewmodel submits actual upper-arm, forearm and hand geometry for both arms, using vanilla's translucent skin pass and no self-outline. Its separately authored palette shares the accepted clip phase and canonical hand/item socket, not the third-person camera composition.
- The Master retains its original hood, mantle, cloak and sheathed scabbard, attached to the articulated head/chest/pelvis. The drawn blade uses the hand socket.
- Cord wrist beads and aura eyes use wrist/head socket adapters. No client root translation changes entity coordinates, hitboxes, reach or server movement.

## Ownership and compatibility boundary

The full existing renderer stays installed as the fallback. Only Spellcut and Master SWEEP have articulated attack clips. The compatible first-person sword view keeps its own resting arms between Spellcuts, including walking, while third-person locomotion keeps the original body. Ordinary sword swings and equip transitions retain vanilla first-person animation; their transitions to the preview idle view still need native visual acceptance. Other moves, crouching, riding, swimming, flying, death, invisible entities, active item use, non-vanilla swords, offhand items, armor, a visible cape, carried parrots/arrows/stingers, worn backpack/casting gear, aura shell/afterimages, or an unknown player/Master render layer retain the whole old backend. Unsupported equipment is never silently hidden. This temporary naked/standing proof is not ready to replace the user's enchanted-netherite combat presentation.

Before default-on, the next mandatory compatibility gate is vanilla armor, trims and glint, including enchanted netherite in both views. Modded armor, changed resource-pack item geometry, shaders, dynamic layer injection and broad mod interoperability are unverified.

Third-person entry/exit incorporates the actual vanilla held-arm rotation/position and weight-eased head limits, rather than fading only toward an unrelated custom bind. The native fixture checks those seam transforms at both timeline edges. Each model owns its own geometry. Resource reload recreates renderer-owned body and first-person models; no texture, GPU handle or live entity is cached in a pose. Deferred model submission re-applies the immutable captured palette. The existing hit-stop now captures the rigid and articulated custom palettes together with the entity pose, while cancellation, replacement activation, death, entity removal and world clear release old holds.

## Verification and reproduction

- Pure pose tests cover hierarchy, bind pose, finite matrices, handedness, accepted boundaries, shortest-arc interpolation, planted soles, fixed bone lengths, visible bends and separate first-person composition.
- The native suite is `dev.wildercord.client.combat.ArticulatedCombatPresentationTest`, selectable with `./gradlew runClientGameTest -PciSuite=articulated`. It checks actual mixin-owned models, skin variants, wrist/item transforms, fallback/cancellation/model reuse and hit-stop palette ownership. It also captures live Spellcut input in both views and hands. Synthetic bridge assertions are explicitly identified separately from actual server-driven captures.
- `tools/ExportArticulatedPose.java` exports the production pure matrices; `tools/ExportArticulatedGeometry.java` and `tools/preview_articulated_combat.py` provide source-matched offline geometry inspection. Those outputs must be labelled offline previews, never Minecraft screenshots.
- Local native launch still has the documented Loom Unix-domain-socket probe restriction; the toolchain/security settings are unchanged. Source compilation and offline previews are not a native pass.

Native framebuffer validation of semi-transparent sleeves, outline behavior, resource reload, equipment/cape/parrot transitions, live Master combat/clothing and perspective swaps remains pending. The pure-palette body previews omit the vanilla breathing/held-item baseline during partial-weight blends; full-weight chamber/impact/follow are exact production geometry. The frozen source gate and exact offline findings are recorded below. No new release-readiness or full animation-coverage claim is made by this slice.

## Frozen source gate: 2026-10-05 12:16 UTC

Independent Java 25 verifier tag `articulated-slice-20261005-1216` passed at 12:16:36 UTC:

- 756 main, 242 client, 126 unit-test and 292 native-test source files compiled
- 1,140/1,140 JUnit tests passed, zero skipped/aborted/failed; 13 tests cover the new pure pose model
- All 3,340 source/config fingerprints remained unchanged; source manifest SHA-256 `224abcbb5532119df9f2054699aa3e85e85a3c4300d69c72dae1369b6f4008fd`
- 22 Python CI-tooling regression tests passed; whitespace checks passed
- Loom launch, native fixture execution and native screenshots were **not run** by this gate

Independent read-only review identified and guided corrections for camera-plane penetration, cross-arm intersections, overly folded wrists, lateral hilt drift, cross-backend arm/head discontinuities, ordinary-swing/equip suppression, translucent skin/outline mismatches, unknown Master layers and expired hit-stop retention. Frozen-source re-review found corresponding fixes for those material source findings. The remaining native acceptance limits above are not closed by compilation.

## Combined integration gate: 2026-10-05 12:25 UTC

After integration with the current combat branch and independent review of its dedicated CI/curator support, `articulated-integrated-20261005-1224` passed at **12:25:21 UTC**: all **756 main**, **242 client**, **126 unit-test** and **292 native-test** sources compile, and **1,140/1,140 JUnit tests pass** with no skips or failures. All **3,340 source/config fingerprints** remained unchanged. The **49 Python CI/curator tests** pass. The separate one-suite articulated native job preserves the sixteen-suite Masters job and all four aggregate shards. Full evidence remains intact; curated owner-camera samples are bounded to 14 MB and explicitly cannot establish a gameplay pass.

The renderer is still default-off, with unsupported equipment and layers using the complete fallback. Native execution of this integrated renderer, its new mixins and its screenshot acceptance remains pending.

## Final source-driven offline geometry evidence

The preview exporter uses actual production `ModelPart.Cube` polygons, official 26.3 `ModelPart` transformations and official sword display transforms. Final source fingerprints for the pose, rig and viewmodel agree with the inspected output.

- 45,040 joint transforms and 4,504 socket transforms agree with the pure matrices, maximum error about 0.00000120.
- All 240 tested exterior skin-side seam vertices preserve their UVs exactly; bind-pose partition gaps are zero.
- 41,796 full-mesh placements across both arm widths/hands, every eighth-tick frame and a 9×9 weighted free-look grid remain in front of the camera near plane. Minimum margin: 0.08183 blocks.
- The crosshair center remains clear in all 1,548 canonical-aim raster frames. At the native 70° hand projection, the central test rectangle has up to 25% wide / 37.5% slim coverage, while the sword remains visible. This is geometric visibility, not proof of live combat readability.
- Both clips have zero sampled opposite-arm hand/forearm intersections. The closed cuboid wrist hinge retains a small adjacent-segment wedge, at most about 0.56 pixels penetration / 2.81 cubic pixels. This is a limitation of the segmented geometry, not smooth skinning.
- Hilt-to-hand-center drift is at most 0.00000101 model pixels in 826 scoped samples; the fixed rounded vanilla-display hilt coordinate differs by about 0.000376 pixels.

Minecraft 26.3 uses a separate hand projection: `Camera.calculateHudFov` starts at 70° and applies death/fluid modifiers; `GameRenderer.render3dHud` uses that field and a 0.05 near plane. The preview's 90°/110° views are explicitly hypothetical projection stress tests, not the user's world-FOV settings. The preview also excludes the Master's attached clothing and native render layers. Its slowed animation is labeled 0.5× for inspection, not gameplay speed.

To reproduce the offline set with the existing verified dependency cache, run `python tools/preview_articulated_combat.py --out <output-directory> --verification <verifier-directory>`. Source geometry, PNG contact sheets, a slowed GIF, a verification JSON and their method/limits are emitted separately; no preview output is presented as game footage.
