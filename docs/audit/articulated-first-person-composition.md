# Articulated first-person composition: local preflight

Status: **native acceptance pending**. This is a bounded presentation-only candidate over
`819f40dbcea84e682619fe1b8c4aa7488318c775`. The default-off articulated and armor switches remain off.
No public upload, native CI invocation, or remote write is part of this evidence.

## Why these changes

The real HUD-on `22cb4171` captures (`articulated_live_right_first_frame_0/4/7.png` and their
left-hand counterparts) were inspected before editing. The later samples put most of the grip,
wrist and forearm behind the hotbar/lower screen edge, and the recovery blade approaches the
reticle. Those filenames are loop samples, not proofs of exact accepted impact/recovery ages.

Only the independent `VIEW_BIND`, `VIEW_CHAMBER`, `VIEW_IMPACT` and `VIEW_FOLLOW` keys changed:

- Raise the actual dominant hand/wrist and a portion of its forearm above the HUD reserve.
- Keep the guard hand readable and intentionally lifted on the other side of the target.
- Move the impact/follow grip slightly outward and apply a 0.18-radian (10.3-degree) local grip
  twist so the blade face remains legible through recovery.
- Keep the camera-space origin, depth placements, arm lengths, sword scale, world/body palettes,
  gameplay geometry, accepted timing, movement, input, cooldowns, damage and reach unchanged.

No HUD or hand asset was hidden. No camera/FOV setting was changed. The existing slim-pivot
baseline correction and held-item/socket hierarchy are untouched.

## Source-only before/after evidence

These figures are software-rasterized production geometry, **not native game pixel measurements**.
The conservative full-width HUD reserve is 42 GUI pixels tall, not a simulated Minecraft HUD.
At 854×480, GUI 2, native hand projection 70°, wide/right:

| Authored age | Grip Y before | Grip Y after | Dominant hand pixels above reserve, before→after | Dominant forearm before→after | Guard hand before→after | Guard forearm before→after |
|---|---:|---:|---:|---:|---:|---:|
| Idle 0 | 410.69 | 372.39 | 877→1346 | 931→3120 | 1724→2316 | 0→3580 |
| Chamber 2.625 | 330.96 | 321.30 | 2258→2200 | 6604→6668 | 1520→2195 | 6→3720 |
| Impact 4 | 361.84 | 334.81 | 2120→2059 | 4412→6203 | 433→2064 | 0→3191 |
| Follow 7 | 469.91 | 369.81 | 0→3327 | 0→4193 | 0→1430 | 0→2028 |

The 96-view preflight covers six authored ages × both hands × wide/slim × four framebuffer/UI
pairs: 854×480/2, 1280×720/3, 1280×960/4, and 1920×810/3. Every view retains visible pixels
of both hands and forearms above the conservative reserve. Every grip anchor is above it.
Minimum visible counts across this matrix: dominant hand 1231, dominant forearm 2807, guard
hand 1424, guard forearm 2028. All world/body palette arrays, weights and phases match the
base export exactly, including both Spellcut and Master SWEEP clips.

The full 129-age source-geometry audit reports:

- Skin/sword: 41,796 weighted free-look placements (±25° yaw, ±20° pitch), no near-plane breach.
  Minimum margin beyond the 0.05-block near plane: 0.0720 blocks wide, 0.0870 slim.
- Armor: separately rerun on the same final pose/transforms and official netherite texture;
  41,796 placements, minimum near-plane margin 0.2434 blocks.
- Zero canonical reticle or central target-rectangle coverage for skin/sword and armor at
  native hand FOV 70. The 90/110 source stress projections are hypothetical, not world-FOV tests.
- No cross-hand/forearm base-cuboid intersections. Same-wrist hinge overlaps remain separately
  reported; they are not misclassified as independent-limb collisions. The first-person renderer
  submits arms only, so there is no first-person head mesh. Third-person head/body poses are
  unchanged, not newly certified free of every existing overlap.

The armor audit is separate from the depth-resolved skin/sword pixel pass. Native combined
armor/skin/sword sorting, trim, glint, lighting, sleeve layers and resource reload still require
runtime review. The software item uses the official iron sword sprite; the native fixture uses
a diamond sword. These are deliberately not conflated.

## Checks executed locally

- Reconstructed official-dependency verifier: 769 main, 247 client, 130 test, 297 gametest sources
  compiled; **1,174/1,174 JUnit tests passed**, 3,370 source fingerprints unchanged.
- Added pure tests: continuous first-person palettes across idle/clip boundaries; canonical
  native-FOV dominant/guard anchor clearance through 641 timeline samples and both hands.
- Strict pristine-runtime grip regression: 288 cases across both widths/hands and multiple
  vanilla arm transforms; maximum error 6.17e-7 blocks against the unchanged 0.001-block limit.
- Final skin/sword source geometry, arm-pair intersection and armor checks passed as above.
- Loom, Minecraft client launch, native mixin execution and new HUD screenshots: **not run**.

## Native fixture and integration

`ArticulatedFirstPersonCompositionTest` is a standalone native fixture. It keeps the real HUD
visible, uses actual framebuffer/UI dimensions, tests both hands with bare skin and an actual
Protection IV netherite chestplate, and captures idle-before, accepted-cast samples and
idle-after. It requires an observed recovery sample, logs the actual live skin model and
pre-capture accepted age/phase, and restores the original fullscreen/window settings. The logged pre-capture age/phase
is context, not an assertion that screenshot readback occurred at an exact phase boundary. Exact ACTIVE
framebuffer coverage remains unverified by this sampling fixture.

Separate native-model/submit probes cover both wide/slim baked rigs, entry/exit grip convergence,
one main submission, no duplicate offhand draw, caller-pose restoration, incomplete equip,
item mismatch, ordinary swing and unsupported armor fallback. Synthetic probes never rename
live screenshots as another skin width. Both live skin widths still need native screenshot
coverage before that part of the acceptance matrix can be claimed.

The integration lead owns registering the new fixture in the gametest entrypoints, suite
catalog and existing articulated CI selection (three articulated suites after registration).
Registration is not native acceptance. No new CI job is needed. Before accepting visually,
inspect the unchanged full-frame screenshots for actual visible wrist/forearm above the real
HUD, a purposeful guard hand, a clean target/reticle, blade-face readability, and native
armor/skin/sword behavior at every viewport. Free-look source geometry is preflight only;
manual/native free-look and backend-transition footage remain part of visual acceptance.

## Reproduction

Using the already-verified local official dependency cache:

1. Export the unmodified base with its `tools/preview_articulated_combat.py --out BEFORE --visual-only`.
2. Run `python tools/check_articulated_composition.py --baseline BEFORE --out NEW_COMPOSITION`.
3. Run `python tools/preview_articulated_combat.py --out NEW_COMPOSITION --reuse-export --skip-gif`.
4. Run `python tools/check_articulated_armor.py --out NEW_ARMOR`.
5. Run the normal project compile/unit gate, plus the strict `CheckArticulatedGrip` helper with
   the pristine official runtime jars. The reconstructed verifier is an additional local gate,
   not a substitute for Loom or native CI.
6. After fixture registration, run the existing native articulated suite and review its new
   actual HUD-on captures. Keep failure, pending and unrun results distinct.
