# Second-form fixed releases: Rising Cinders and Blossom Fall

Date: 2026-10-05. This is a bounded source, unit and offline presentation review. Native gameplay and rendering remain release gates.

## Content

- Animation ID 13, Rising Cinders: eight-tick paid windup, sixteen-tick recovery. An outside-hip scoop unfolds into a high diagonal release; the original enemies rise, not the player. The cinder rain remains twelve ticks after release.
- Animation ID 14, Blossom Fall: eight-tick paid windup, eighteen-tick recovery. An over-shoulder gather falls into a planted stance. A grip-local turn directs the third-person tip toward the ground. The existing healing/slowing petal field retains its eighty-tick lifetime.
- Both keep the original leap/low string, Flow unlock, eight-Aura base price, eighty-tick individual cooldown, damage factors, targeting dimensions and effect counts. Existing discounts and rest modifiers still apply.
- IDs 0–12 retain their meanings and timings. Client action requests still accept only IDs 0–2; the new IDs are server-confirmed presentation IDs.

Each fixed-release profile now declares a non-null target policy. The shipping cone profiles re-query at the active frame rather than pulling an old string victim into the committed cone. An unprofiled art still retains its observed victim. A future target/counter profile must explicitly select and validate retained targeting; moving, landing-driven and channelled arts still need their own actual phase callbacks.

Acceptance pays once and starts the original individual rest. Repeated requests cannot pay again. Damage, weapon selection changes, method changes, spectator state and world changes cancel an unreleased hit while preserving its paid recovery. The existing shared player-identity/lifecycle checks still apply. Once released, rain and fields are independent of the remaining body pose, weapon and method. Their effects recheck current harm/help admission. Cinder rain additionally rejects a removed, replaced or disconnected owner body and a removed/cross-world source.

## Verification actually run

The frozen independent Java 25 gate `second-forms-polished-20261005` completed at **11:28:38 UTC**:

- 753 main, 236 client, 124 unit-test and 290 native-test Java sources compiled
- **1,120 / 1,120 JUnit tests passed**, none skipped, aborted or failed
- All 3,325 Java/JSON/Gradle/properties source fingerprints stayed unchanged
- **22 / 22 Python CI-tooling tests passed**
- All 267 unique registered native-test entrypoints resolve to source files
- Generated-asset consistency passed across 5,500 paths; original PNG bytes were retained after checking decoded pixels
- Player-guide source/export validation passed across 132 pages (`tools/player_docs.py --check`); no rendered Jekyll site was available for the separate HTML checker
- Whitespace checks passed

This independent gate uses the declared dependencies and official Minecraft jars. It is not a Loom build, native launch, multiplayer pass or distributable-artifact gate.

The new native lifecycle suite exercises both actual performers: payment/repeat rejection, exact advertised release tick, no windup damage/fields, committed aim despite free look, old-target cone exclusion, cancellation, post-release interruption/weapon/method loss, live party changes during rain/field pulses, field expiry, owner-world departure and a lifted source changing worlds. The existing presentation suite now plays the real leap/low input and samples both hands, both player-skin rig sizes, first-person transforms, grip invariance, free look, cancellation and reset. These tests **compile but have not run in a native client**.

## Offline presentation evidence

Source-sampled body and first-person contact sheets were reviewed for chamber, release, follow-through, recovery and neutral frames. This inspection found and corrected a low/offscreen Blossom first-person release and an insufficiently downward third-person finish. The final source-driven sheets show the two cuts' opposite paths and visible sampled first-person releases/recovery.

A quarter-tick sweep across both hands, five free-look yaw offsets (−180°, −90°, 0°, 90°, 180°) and three pitch offsets (−90°, 0°, 90°) projected 2,036,160 sword-surface vertices using the official vanilla diamond-sword pixels and display transforms. None crossed the −0.05-block camera near plane; the nearest sampled vertex was at approximately −0.318 blocks. The archived evidence includes sampler CSV, renderer code and source hashes.

These are **offline projections, not game screenshots**. They omit lighting, shaders, walking, bob, VFX, skin textures and equipment layers. The rigid-limb body model has no elbow or knee flex; exact sole contact and limb/item intersection clearance are not certified by the contact sheets. Native owner/observer agreement, armor layers, resource-pack item models, latency, camera comfort and actual screenshots remain necessary before calling this release-ready.
