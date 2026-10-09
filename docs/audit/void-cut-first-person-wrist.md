# Void Cut first-person wrist readability

The original HUD-on `masters_style_void_cut_first_active` capture from native run
37496328238, head `dff8c58f`, submitted one sword but measured 137 cyan pixels and
zero readable-component span. The PNG shows a nearly edge-on dark vertical strip.
The preserved source files have these SHA-256 hashes:

- PNG: `92763c4aa60c6e2e1247d568c8792cea0ebea3c3a43eedca0339e2ebcd851d17`
- JSON: `6ef7825733be9b70a3b6bc820eccbca5071ca20e516bc31da6b7a44beaef85f8`

Replaying its actual `handProjection` with the official 26.3 diamond-sword sprite
places the camera -0.03503 blocks from the sprite mid-plane, barely outside its
0.02125-block half-thickness. A broad face is technically visible, but severely
foreshortened. Its source raster footprint contains only 478 cyan pixels above
the HUD. Opaque geometry clears the near plane at 0.78952 blocks and does not
enter the aim corridor. The failure is consistent with the wrist orientation,
not a missing sword, near-plane clipping, or a blade entirely hidden by the HUD.

The phase request was logged at age 6.5, but the rendered hand in the receipt has
a follow-through interpolation fraction of 0.152537. The exact rendered age was
not recorded: the nominal phase log precedes inspection and the separate render
call. There is no held/frozen-pose path here, and the filename does not independently
prove the exact rendered phase. The regression test preserves
that captured hand and the actual camera rotation preceding it instead of substituting a pose
sampled at the requested age. Corrected wrist components use the captured blend.

The production change affects only ID9's first-person impact yaw (-25° to -5°)
and roll (14° to -10°). Its position and pitch, the chamber and follow endpoint,
all body joints, accepted aim, grip pivot, server clocks, and effects are unchanged.
Existing interpolation carries the wrist into and out of the corrected impact.
A yaw-only candidate was rejected because it introduced aim-corridor overlap at
extreme free-look angles. The paired yaw/roll correction keeps the blade outward.

The corrected exact native-matrix replay predicts about 5,805 cyan footprint
pixels above the HUD, 0.79126-block near clearance, and no aim-corridor overlap.
These are source geometry predictions, not new native pixels or acceptance.
`MastersVoidCutCompositionTest` additionally checks:

- Front-facing upper-blade cyan area and span above four viewport/GUI layouts,
  quarter-tick samples from ages 3.5 through 12.5, both hands, pitch offsets 0/-3/-12,
  settled input and the observed native residual attack/height curve
- Near-plane clearance and no opaque face or extrusion-wall overlap with the
  64-pixel aim corridor over a 5° grid covering yaw ±55° and pitch ±40°, both hands,
  both input-height contexts, and quarter ticks through the changed cut/recoil
- Only the reviewed yaw/roll trajectory changes; the remaining hand components,
  grip, endpoints, and timeline continuity are preserved

Free look can still turn a moving blade edge-on. The all-look checks establish
near-plane and aim clearance during the changed segment; readable broad-face
coverage is asserted for the accepted-facing contexts. Untouched later recovery
free-look behavior is not redefined by this repair.

The historical fingerprint remains exactly
`9f809f05118af49e7619a1471ad3b3a6d56e2fdb1a04e16f8966ec94e2eb0cea`.
No broad golden was regenerated. For ID9 only, the verifier reconstructs the
original yaw/roll during the affected interpolation interval before hashing.
Every other field and all other IDs retain their existing protection, including
ID9's chamber/follow endpoints and every body/articulated value. The dedicated
composition test explicitly verifies the replacement wrist trajectory.

Focused Java 25 verification passed 43 tests, including the new geometry suite,
existing first-person and Red Rain/ground-field composition, style animation,
view motion, and the unchanged historical fingerprint. The serialized aggregate
compiler and fresh unchanged native capture gates remain required separately.
