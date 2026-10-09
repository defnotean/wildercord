# Red Rain first-person chamber readability

The original native frame `masters_style_red_rain_left_turn_first_windup` from run
37476041308 (head `3d392`) submitted one sword but contained no cyan blade pixels.
The original 1280×720 HUD-on PNG shows a dark extruded edge, not a missing item or a
blade hidden by the HUD. Its observed hand state has attack phase .75, inverse arm
height .85689604, art weight .9505149, left hand, accepted-aim deltas -90°/-75°,
and hand pose (.06, .38, -.14, -62°, 30°, -38°).

Using its unchanged captured projection and the official 26.3 handheld display,
the camera is approximately -.01343 blocks from the sword sprite's mid-plane.
The generated sprite has surfaces at z=7.5/16 and 8.5/16 and display scale .68,
so its half-thickness is .02125 blocks. The camera lies between the broad-face
planes: neither opaque cyan face faces the camera. Only dark perimeter side walls
are visible. Loosening the cyan or readable-span assertions would hide a genuine
presentation defect.

The correction changes only Red Rain's first-person chamber hand: x .06→.22,
yaw 30°→48°, roll -38°→-20°. The hilt and sword move outward together. Its pitch,
y/z translation, body joints, accepted aim, camera bounds, vanilla grip pivot,
server timing, impact pose, and follow-through pose are unchanged. Existing smooth
interpolation carries the corrected chamber into the original release.

A static source search rejected orientation-only candidates that entered the
reticle at the opposite look extreme. The exact recorded left-hand replay now
preserves the native camera prefix before the authored hand and the amplitude-
faded swing after it. Its original attack phase is .75 and ownership is .9505149;
the recovered camera prefix has less than .00001 blocks of translation. Replacing
only the authored hand predicts, for that recorded 1280×720 context:

- 5,049.3 square screen pixels of projected upper-blade cyan face area above the HUD
- 218.1 screen pixels (109.1 GUI pixels) of upper-blade span
- No opaque face or extrusion-wall overlap with the 64-pixel aim corridor
- Nearest opaque depth .78405 blocks, compared with the .05 near plane

Earlier all-look numerical minima treated the complete residual camera/swing
matrix as if it followed the authored hand. Those minima are withdrawn because
the camera prefix does not commute with the hand. The corrected exact-frame
replay retains generous margins; the separate bounded-look tests continue to
check their explicitly modeled source contexts.

These are source geometry predictions, not new native pixels or acceptance.
The native capture assertions remain unchanged and must pass on a fresh run.

`MastersRedRainChamberCompositionTest` preserves the original failing projection
as a regression fixture. It first proves that neither old broad face is visible,
then replays the source correction over the actual native matrix in the order
`cameraPrefix × authoredHand × fadedSwing × itemDisplay`. It also covers
100,096 chamber-entry contexts: quarter-tick samples through the chamber, both
hands, four viewport/GUI layouts, a sampled 5° yaw/pitch grid, settled input throughout,
and the observed residual attack-height curve from the first native capture age
of 3. A genuinely lowered vanilla hand at zero art ownership is not claimed as a
readable-art frame. The checks require visible, front-facing opaque cyan area and
span above the HUD, near-plane clearance, and no opaque face or extrusion wall in
the aim corridor. Separate controls verify the hilt pivot, continuous endpoints,
and unchanged release/recovery hand keyframes. Existing ground-field continuous
and extreme-look composition checks remain in force.

The sampled all-look broad-face guarantee is intentionally for the chamber. The unchanged
moving cut and follow-through may pass edge-on as the wrist turns; this repair does
not redefine their existing native visibility or geometry requirements.

The original all-IDs legacy fingerprint intentionally changed with the corrected
ID18 hand. Its replacement protection is derived from immutable public
`160ff12556754428edc686ef261d4e5255e7363a`, first reproducing the original full hash
`837c4514b76867d30ab236e916a6262055206ae011866e8c5b2180c35b51fd08`.
Canonicalizing only ID18's classic hand x/yaw/roll before its impact produces
`9f809f05118af49e7619a1471ad3b3a6d56e2fdb1a04e16f8966ec94e2eb0cea`.
That Red Rain-only normalization preserves all fields of IDs0–17, ID18's unchanged
hand y/z/pitch at every sample, its body/articulated data, weight/clock, and complete
release/recovery. The reviewed chamber hand is checked explicitly alongside its
continuous entry and visibility tests. The later, separately reviewed
[Void Cut wrist correction](void-cut-first-person-wrist.md) retains this same hash
by restoring only its affected ID9 yaw/roll components before hashing; its dedicated
tests protect the replacement trajectory.
