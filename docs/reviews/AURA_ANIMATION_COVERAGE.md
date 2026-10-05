# Aura animation coverage audit

Date: 2026-10-05. This is a source audit, not a claim of native rendering verification.

## Current boundary

- The three new shared Master's Arts have original body and first-person keyframes, driven by the accepted server timeline. Their native presentation suite is authored but has not run yet.
- The 50 existing breathing-method arts already have custom trails/world effects. Their input strings use ordinary vanilla swings, with counter/step prerequisites where applicable. The ten first-slot arts now have original player-body and first-person weapon keyframes in the client catalog; the accepted server timeline is integrated in source, with final independent compilation and unit checks passing; native visual verification is still pending. Rising Cinders and Blossom Fall now add two authored fixed-release second forms (IDs 13–14); their native gameplay and presentation checks are authored, not yet run. The other thirty-eight style arts have no authored player-body/weapon choreography.
- The existing casting-pose mixin handles spell shapes, not these sword arts. Colour, particles, slash ribbons, afterimages, knockback and teleports are not equivalent to an authored joint animation.
- PlayerModel is Minecraft's rigid-limb rig. The implementation can turn/lean the torso, rotate both arms/legs, adjust pivots, mirror handedness and transform the held weapon. It cannot bend elbows/knees or reproduce Epic Fight's skeletal rig without a separate original rig and renderer.

## New shared arts

| Art | Input | Existing/new VFX | Authored body + weapon | Timeline authority | Native status |
| --- | --- | --- | --- | --- | --- |
| Spellcut | U, rebindable | Cross trail + impact feedback | High chamber, diagonal cut, hip turn and follow-through | 4-tick windup, 12-tick recovery; server impact | Pending native run |
| Rising Break | Y, rebindable | Rising trail + lift feedback | Low stance, rising blade, opened shoulders and high recovery | 8-tick windup, 18-tick recovery; server impact | Pending native run |
| Driving Cut | J, rebindable | Thrust trail + cast-break feedback | Drawn-back point, committed forward stance, recoil | 6-tick windup, 14-tick recovery; server impact | Pending native run |

## Existing style coverage

“Vanilla” below means the ordinary swings that feed the unchanged sword-string reader. The ten first-slot catalogs now contain authored poses, but their server timelines are integrated in source and they are not considered complete until native presentation passes. The final column describes phase ownership to verify. First-slot moves and the two selected fixed-release second forms use the shared commitment pipeline; the other slots still need their real choreography hooks.

| Style / art | Current swing input | Existing custom VFX strokes | Authored body / weapon | Phase hooks / required verification |
| --- | --- | --- | --- | --- |
| Ember: Kindling Draw | Vanilla string I | DRAW | Authored; runtime pending | Accepted draw/impact; keep advancing fire-line ticks separate |
| Ember: Rising Cinders | Vanilla string II | RISING | Authored; runtime pending | 8-tick committed rising cut, 16-tick recovery; rain retains its independent 12-tick release delay |
| Ember: Backdraft | Vanilla string III, counter | THRUST | None | Counter brace → thrust; returned flame release |
| Ember: Wildfire Rush | Vanilla string IV, step | THRUST | None | Actual rush start/travel/stop, then settle |
| Ember: Sunfall | Vanilla string V | FALLING, RISING | None | Actual rise → airborne hold → landing slam (landing callback) |
| Rime: Frostbite | Vanilla string I | CUT | Authored; runtime pending | Accepted short cut; third-crust freeze is hit feedback |
| Rime: Hailfall | Vanilla string II | RISING | None | Accepted upward cast-cut; cloud release separate |
| Rime: Glacier Mirror | Vanilla string III, counter | SWEEP | None | Counter brace → wide sweep; mirror persistence separate |
| Rime: Skate | Vanilla string IV, step | LOW | None | Actual glide start/travel/stop; never animate through a blocked glide |
| Rime: Winter's Hush | Vanilla string V | SWEEP | None | Accepted sweep; delayed shatter release |
| Thunder: Crackle | Vanilla string I | CUT, THRUST | Authored; runtime pending | Three actual hit ticks, alternating cuts then thrust |
| Thunder: Skyfall | Vanilla string II | RISING | None | Raised blade/hold; actual delayed lightning-release tick |
| Thunder: Static Riposte | Vanilla string III, counter | THRUST | None | Counter brace → thrust; chain arcs separate |
| Thunder: Bolt Step | Vanilla string IV, step | CROSS, CUT | None | Each actual blink endpoint and its cut; abort on missing targets |
| Thunder: Heaven's Spear | Vanilla string V | THRUST | None | Actual charge duration → committed thrust → settle |
| Gale: Cutting Breeze | Vanilla string I | DRAW | Authored; runtime pending | Accepted low draw; projectile crescent travel separate |
| Gale: Updraft | Vanilla string II | RISING | None | Actual rise and landing callbacks; airborne cut held between |
| Gale: Eye of the Storm | Vanilla string III, counter | SPIN | None | Counter turn → spin; storm persistence separate |
| Gale: Tailwind | Vanilla string IV, step | THRUST | None | Actual dash start/travel/stop |
| Gale: Hundred Winds | Vanilla string V | SPIN | None | Actual field duration and repeated blade releases |
| Stone: Rockbreaker | Vanilla string I | FALLING | Authored; runtime pending | Accepted heavy overhead impact and long recoil |
| Stone: Avalanche | Vanilla string II | FALLING | None | Actual airborne rise → landing impact (landing callback) |
| Stone: Unmoved | Vanilla string III, counter | THRUST | None | Braced counter → short thrust; return to grounded guard |
| Stone: Landslide | Vanilla string IV, step | THRUST | None | Actual charge start/travel/stop; keep braced torso |
| Stone: Mountain Splitter | Vanilla string V | FALLING | None | Accepted overhead split; advancing ground-strike ticks separate |
| Verdant: Thorn Lash | Vanilla string I | DRAW | Authored; runtime pending | Accepted draw; delayed lashes extend on their own hit ticks |
| Verdant: Blossom Fall | Vanilla string II | FALLING | Authored; runtime pending | 8-tick committed falling/planting cut, 18-tick recovery; field retains its independent 80-tick lifetime |
| Verdant: Rooted Parry | Vanilla string III, counter | RISING | None | Counter brace → rising reply; rooted duration separate |
| Verdant: Wild Growth | Vanilla string IV, step | LOW | None | Actual movement path start/stop; low crossing cut |
| Verdant: Grove's Heart | Vanilla string V | FALLING | None | Accepted planting strike; grove pulse lifecycle separate |
| Hollow: Void Cut | Vanilla string I | CUT | Authored; runtime pending | Accepted lateral cut; delayed void wake separate |
| Hollow: Collapse | Vanilla string II | FALLING | None | Accepted downward pull-cut and recovery |
| Hollow: Null Parry | Vanilla string III, counter | SWEEP | None | Counter brace → clearing sweep; silence feedback separate |
| Hollow: Rift Step | Vanilla string IV, step | CUT | None | Actual teleport endpoint and flank cut; blocked-rift cancellation |
| Hollow: Event Horizon | Vanilla string V | CROSS | None | Cross-cut gather; actual field/crush phase callback |
| Starlit: Star Needle | Vanilla string I | THRUST | Authored; runtime pending | Accepted precise thrust; needle flights separate |
| Starlit: Meteor Shower | Vanilla string II | RISING | None | Accepted raised blade; staggered meteor releases separate |
| Starlit: Constellation Guard | Vanilla string III, counter | CROSS | None | Crossed counter guard; actual delayed star-release ticks |
| Starlit: Comet Dash | Vanilla string IV, step | THRUST | None | Actual dash stop → held delay → burst-release callback |
| Starlit: Nova | Vanilla string V | SPIN | None | Actual gathering duration → turning release → recovery |
| Hourglass: Echo Cut | Vanilla string I | CUT | Authored; runtime pending | Accepted first cut and actual delayed echo beat |
| Hourglass: Rewind Leap | Vanilla string II | FALLING | None | Actual initial fall/leap and delayed rewind transition |
| Hourglass: Stopped Moment | Vanilla string III, counter | THRUST | None | Counter brace → frozen-point thrust; actual release callback |
| Hourglass: Blur | Vanilla string IV, step | DRAW | None | Actual movement burst; low draw and settle |
| Hourglass: Thousand Moments | Vanilla string V | SPIN | None | Actual held field → repeated cuts → release callback |
| Crimson: Bloodletting | Vanilla string I | CUT | Authored; runtime pending | Accepted low lateral cut and recoil |
| Crimson: Red Rain | Vanilla string II | FALLING | None | Accepted falling cut; blood-rain phase separate |
| Crimson: Sanguine Parry | Vanilla string III, counter | CROSS | None | Crossed counter → cut; delayed return separate |
| Crimson: Frenzy | Vanilla string IV, step | THRUST | None | Accepted forward attack/buff start; sustained frenzy is not one endless swing |
| Crimson: Crimson Moon | Vanilla string V | SWEEP | None | Accepted broad sweep; outward/returning moon phases separate |

## Bounded implementation and remaining gates

1. Keep the three shared art IDs stable. Do not report the 50 style arts as animated merely because these shared actions work.
2. The bounded first-form implementation reuses the server-authored Performed channel (animation ID, start tick, windup, recovery, accepted yaw and actual attack pitch). It begins after validation. Cancellation, death, world change and stale-packet behavior still require native regression checks. No target, hit or movement claim comes from the client.
3. The ten first-slot attacks now commit payment/recovery before their delayed performer and revalidate before impact. The two fixed-release second forms now do the same; other style slots retain their existing scheduling. A late post-performer event is not described as a real windup.
4. The authored ten-first-slot catalog covers: Kindling Draw, Frostbite, Crackle, Cutting Breeze, Rockbreaker, Thorn Lash, Void Cut, Star Needle, Echo Cut and Bloodletting. These have distinct posture, travel, rhythm and recoil, with original keyframes rather than palette substitutions. Their source integration compiles; native rendering and gameplay still await the gates below.
5. Rising Cinders and Blossom Fall now have distinct grounded rising and falling/planting releases, keeping their original damage, price, rest and independently scheduled rain/field lifetime. The catalog requires an explicit target policy for each art: cone releases re-query at impact, while a future target/counter profile must deliberately retain and validate its string victim. Extend the remaining second-slot launchers/slams and third-slot counter replies. Movement, teleport, landing-driven and sustained final arts need their real phase callbacks before high-fidelity animation can be synchronized.
6. Reuse original motion families where honest: low draw, lateral cut, thrust, rising cut, overhead slam, brace/riposte, travelling stance, turning/channelled release. Style-specific timing and pose accents distinguish each art; multiple-hit arts must use actual hit beats.
7. Native acceptance gates: owner and observer see the same accepted art; no playback for refusal; cancellation stops the pose; left/right hands mirror correctly; slim/standard skins and outer layers agree; first-person blade stays readable; pause/menu/world changes leave no stale animation; inspect screenshots/clips at windup, impact, follow-through and settled recovery.

## Source map

- Registry: src/main/java/dev/wildercord/aura/arts/MethodArts.java and the ten *Arts.java method classes.
- Existing input/acceptance: src/client/java/dev/wildercord/client/SwordStringsClient.java and src/main/java/dev/wildercord/aura/SwordStrings.java.
- Existing VFX protocol/playback: src/main/java/dev/wildercord/aura/AuraFx.java and src/client/java/dev/wildercord/client/AuraFxClient.java.
- New authoritative shared-art protocol: src/main/java/dev/wildercord/aura/MastersArts.java.
- New pure keyframes and hip-pivot math: src/main/java/dev/wildercord/aura/MastersArtAnimation.java; ten style forms: MastersStyleAnimation.java; shared server timing catalog: MastersStyleRules.java.
- New rendering: src/client/java/dev/wildercord/client/MastersArtPose.java and MastersArtsClient.java.
- Native suite: src/gametest/java/dev/wildercord/gametest/WildercordMastersArtsPresentationTest.java.
- Reference used for the architectural distinction only: [Epic Fight API: Getting started](https://github.com/Antikythera-Studios/epic-fight.github.io/blob/main/docs/API/Starting.en.md). No dependency, rig, animation or asset was copied.

## Additional verification completed in the cloud

- Sixteen focused JUnit tests pass for the three shared arts, ten first forms, phase continuity, repeated hit beats, distinct poses, input-ID separation, mirrored hip pivots, free-look heading, weight-eased neck limits, camera-grip transforms and third-person blade pitch.
- Independent offline diagnostics use the actual Java samplers, vanilla rigid-limb geometry and sword display transforms. They exposed and guided fixes for camera-origin clipping, inward guard-arm intersections, vertical-looking thrusts and low-cut readability. These images are explicitly not in-game footage.
- The first-person renderer consumes the immutable captured avatar state. It rotates about the vanilla grip point, bounds large aim offsets, keeps thrust points and low cuts readable, and shows a directional cue without turning the camera. Model neck limits blend with the art so vertical look does not snap at entry or recovery.
- Player armor uses PlayerModel in the official 26.3 bytecode. Skin overlays are child model parts and inherit the motion, rather than receiving a duplicate transform.
- Native owner/observer gameplay, mixin application, shaders, item-resource variations and final screenshots remain unverified.
