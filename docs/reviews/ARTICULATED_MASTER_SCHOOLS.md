# Opt-in articulated Gale and Stone forms

This bounded presentation slice extends the existing original 20-joint backend to Master attack IDs 7 (Gale Crosswind Reprise) and 8 (Stone Fracture). It remains behind `-Dwildercord.articulated=true`, which defaults off. These are NPC forms; no player art, first-person composition, camera, input, gameplay state, damage, cost, roster, warning, movement or release deadline is changed.

## Choreography and ownership

Gale gathers into a compact high carry, clears the leading and trailing feet during the existing four-tick sidestep, and plants before its warned point reply. Its chest counter-turns against the pelvis; its elbow extends into the forward reply, then returns through a separate recovery key. Small body-local ankle offsets depict the step without adding to the server entity's travel.

Stone settles into a low held brace, raises the sword for its separate overhead warning, and closes the spine and elbow into a downward-forward fracture. Both ankles retain their authored targets from the completed plant through the reply and follow-through. The head, shoulders, forearms, wrists, knees and feet keep separate transforms. Blade attitude is carried mostly on the child socket so the visible wrist does not fold sharply into its forearm.

The sampler imports the existing `GaleRepriseRules` and `StoneFractureRules` timing constants. Accepted ID, start, tell, active and recovery still come from `SwordMaster`. The existing Spellcut and Master sweep paths, including both hands and the first-person palettes, have byte-identical exported samples against the base commit.

Only Gale's accepted `[8,12)` step sets `scriptedFootwork`. Vanilla's smoothed walk speed and client position interpolation can remain nonzero after landing. The immutable extracted frame therefore also records synchronized horizontal velocity. A known stationary Gale clip can retain its planted segmented pose through that residual easing; an unknown or nonzero velocity does not grant this exception. This does not permit ordinary locomotion for other clips. Native seam captures must verify this behavior; the source-level distinction alone cannot establish packet timing.

Existing complete-body equipment, cape, unknown-layer, posture and ownership fallbacks are retained. Model geometry, original clothing adapters, armor adapters, item attachments, hit-stop activation identity and immutable pose palettes are unchanged. Original rigid Gale/Stone choreography remains the fallback.

## Verification and evidence boundaries

Focused pure tests cover unchanged IDs and phase windows; the exact step boundary; alternating Gale foot clearance; the Stone brace and overhead grip; planted soles; rigid limb lengths; reflected hands; finite immutable matrices; continuous extreme valid timings; and bounded wrist folding. Existing Spellcut/sweep pose tests also pass.

`python3 tools/preview_articulated_schools.py --out <fresh-directory> --verification <verified-dependency-directory>` emits source-driven sheets for both hands plus a report. It uses the actual exported rig polygons, school textures and native item-display matrices. Its semantic checks require a forward Gale blade, raised Stone overhead blade and downward-forward Stone release blade. The sheets omit Master clothing accessories, native lighting, live baseline/head look and entity travel. They are not Minecraft footage or native acceptance. The existing default pose exporter still emits exactly the original Spellcut/sweep clip set; school export is an explicit `--schools` mode.

The existing `ArticulatedCombatPresentationTest` now contains a separate synthetic school regression matrix and calls the natural school trial capture helper in opt-in mode. No new suite, CI job or aggregate selection is added. Synthetic checks cover both hands, repeated model passes, exact attachment matrices, original clothing attachment identity, whole-body fallback/recovery, disabled preview, expiry, unknown forms and the Gale landing boundary. The existing strict 0.001-block vanilla grip-convergence check is unchanged.

Fifteen requested live captures use ordinary unpaused server AI, a consenting Fabric FakePlayer challenger and the real client as spectator. Ten cover each school's named beats; five additional Gale frames cover ages 11 through 15. Each fresh trial is naturally admitted and retains its actual paid costs, start, movement, warning and single release. The capture probe records the actual segmented model passes, all 20 named transforms, original-part visibility, and native held-item entry/return matrices. Expected socket/item transforms are explicitly derived from that receipt. A model probe at method RETURN covers the early segmented setup return as well as rigid fallback.

Articulated body beats use a fixed closer observer position and 50-degree FOV selected before the natural trial starts. Reply-warning beats keep the wider 60-degree view to show the full lane. Native visible model polygons and resolved held-item extents must stay inside the viewport with a 3% margin, and close body geometry must occupy at least 180 vertical pixels. Sidecars retain the actual submit/projection matrices and identify partial warning coverage in close views. These bounds do not prove self-occlusion or visual quality; inspection of the unmodified PNG remains required. Original rigid-fallback framing and full-lane checks stay unchanged.

The new filename prefix is `articulated_npc_`; existing `masters_npc_` rigid captures retain their names and original fields. Additive metadata identifies expected backend, articulated activation and phase, velocity versus interpolated travel, model receipts and hand receipts. A failed native render is evidence of failure, never accepted coverage.

Native execution, visual inspection of actual school silhouettes/clothing/grips, landing continuity, and complete repository acceptance remain required before any release-ready or default-on claim. Earlier actual Spellcut captures were inspected as existing-rig reference only; they do not validate these new NPC forms.

## Frozen source gate, 2026-10-05 18:56 UTC

Gate `articulated-schools-20261005-1856` compiled 779 main, 250 client, 138 unit-test and 316 native-test source files. All 1,222 JUnit tests passed with zero skipped, aborted or failed tests. All 3,414 source/configuration fingerprints remained unchanged. This independent official-dependency gate did not launch Loom, mixins, the game or a GPU.

Final source-driven preview checks passed for 24 selected samples and six canonical blade-direction probes. Independent source review found and closed the post-step walk-easing/interpolation seam and corrected the initial blade attitudes. Actual native school footage, packet timing and rendered clothing/grip acceptance remain pending.

## Integrated follow-up gate, 2026-10-05 19:31 UTC

Gate `school-lifecycle-arrival-integrated-20261005-1930` compiled 780 main, 250 client, 139 unit-test and 320 native-test files, then passed all 1,233 JUnit tests. All 3,422 source/configuration fingerprints remained unchanged. This includes the closer native capture probes and the separately reviewed lifecycle/party/fixture fixes. It still does not establish native segmented school rendering; the next actual articulated suite must produce and validate those frames.

The final combined rerun `school-lifecycle-final-20261005-1935` also includes the independently reviewed ordinary-Husk admission correction. It passed the same four source sets and all 1,233 JUnit tests with all 3,422 hashes unchanged. The final Python tooling suite passed all 161 tests, including close/wide capture metadata validation. Native execution is still the next gate.
