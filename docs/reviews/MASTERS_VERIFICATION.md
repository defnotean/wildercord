# Masters of Tomorrow: development verification

Base: `ebb2887f7d7becccf339918c00a9741721606fb1` (main).

Development version: `0.11.0-masters-dev`.

## Opt-in articulated integration: 2026-10-05 12:25 UTC

An original segmented player/Master rig and separate first-person viewmodel are now included as a **default-off development proof**, preserving the existing combat authority and full unsupported-equipment fallback. The combined independent gate passes all four source sets and **1,140/1,140 JUnit tests**, with **3,340 unchanged source/config fingerprints**; **49 Python CI-tooling tests** also pass. A separate one-suite native job and bounded real-frame evidence are included. Native rendering, armor, fallback transitions and compatibility remain acceptance gates. See [the articulated scope and exact limits](ARTICULATED_COMBAT_SLICE.md).

## Native CI checkpoint: 2026-10-05 11:34 UTC

[Run 126](https://github.com/defnotean/wildercord/actions/runs/37302625183), for published checkpoint `b72ebc2aa00650f202655dab89bf4590a500d9be`, passed its normal Build job including generated assets, Gradle compilation/unit tests and packaging. Native gameplay remains incomplete. Its focused job completed the Master trial and advanced to the presentation fixture, then failed the strict default-key conflict assertion: Minecraft 26.3 uses G for Quick Actions. This content checkpoint changes fresh defaults to U/Y/J and preserves that strict native assertion; existing saved bindings are not overwritten. The correction still needs its next native run.

The preceding `8f7b69b0` focused run reached the end of the Master trial fixture, including an observed **8.104321-health thrust against a 20-health player in full Protection IV netherite** and its guard/casting probes. It then failed abandonment cleanup. The fixture had moved the player beyond its supplied floor and the log records a fall death; that hazard is corrected without claiming it proves the whole failure cause. Departures now stay outside the arena on solid ground, and the same 220-server-tick deadline reports player, roster, entity and chunk-ticking state.

Full shards independently exposed a stochastic Siltcrest scenario, Rimehare flight distance, and supplied Cinder receiver health assumptions. The new fixture checkpoint admits sustained native fish movement without steering it, keeps the original strict escape/refusal assertions, isolates supplied Cinder witnesses from unrelated random Runebound health bonuses, and adds read-only Rimehare path/threat diagnostics while retaining its final-distance assertion. These four changed suites passed focused Java compilation and independent review; the next native results remain decisive. The current full-shard Resonant Strikes failure is being investigated separately.

## Reviewed content checkpoint: Ember and second forms

This checkpoint adds the Ember Master's two-beat Cinder Wake and authoritative releases/original fallback-rig motion for Rising Cinders and Blossom Fall. The focused native catalog now has **16 suites**; the new afterburn probes are included in the existing Master trial suite. Existing C2S shared-art IDs remain 0–2, and the two presentation IDs append as 13/14. Thirty-eight existing method arts still lack full body choreography.

Each slice received an independent source review. The Ember review found and fixed a paused-AI warning lifecycle defect before publication, and later checked the chamber-only head-clearance correction. The second-form review checked payment, target policy, cancellation, delayed ownership, preserved original effects and new-ID-only presentation changes. Offline source-sampled previews were inspected; they do not establish native rendering quality. The new articulated-rig direction is a separate next architecture step; these original rigid-limb motions remain a fallback and timing reference.

The combined frozen Java 25 gate `content-integrated-20261005-1138` passed at **11:39:14 UTC**: **755 main**, **236 client**, **125 unit-test** and **291 native-test** source files compile, and **1,127/1,127 JUnit tests pass** with zero skipped, aborted or failed tests. All **3,329 source/config fingerprints** remained unchanged. The combined generated-assets check covers **5,500 paths**, guide-source/export validation covers **132 pages**, and **22 Python CI-tooling tests** pass. This is independent source/unit verification; native content and the corrected defaults still require the next focused run.

## Earlier CI follow-up: 2026-10-05

[Draft PR #1](https://github.com/defnotean/wildercord/pull/1) now runs the normal repository workflow. On published commit `eba91238eee564eebf5addfb72a6b2f3929eb9a5`, the [Build job](https://github.com/defnotean/wildercord/actions/runs/37292944774/job/111707410625) **passed**: generated assets, Gradle build/unit tests, pinned launcher profiles and artifact uploads. This supersedes the earlier normal-build blocker for GitHub CI; it does not change what the earlier local environments could run.

The actual client also starts and produces gameplay screenshots. Its first full run exposed native assertions in Siltcrest fish-escape observation, Cinder Bailiff withdrawal, and residue expiration after chunk return. Shard 2 later failed advancement ordering; the supplied higher-tier Cord had already granted the lower-tier advancement before its test. Commit `8f7b69b0` restores the intended assertion order without changing advancement rules. A native startup or a successful build is not a complete gameplay pass.

The bounded corrective changes are:

- Isolate the Cinder supplied zombie from unrelated random Runebound conversion, assert initial current/max health of 20, and preserve the final no-damage assertion.
- Observe Siltcrest's actual committed quarry before the bird's synchronous final-tick resolution, retain the exact cancellation/refusal requirements, and log each unforced trial. Eight trials and the strict displacement threshold remain.
- Wake parked residues when resident chunks become accessible again, using Fabric's full-status-change event and deferred non-loading chunk checks. Retain inaccessible records, bound fades to 64 per dimension per sweep, and verify actual readiness before the native test reads blocks. Accessibility checks are separately bounded across queued wake-ups and scheduled entries.
- Add a separate 15-suite Masters native job, preserving all four complete shards and their gallery settings. One catalog controls selection; launcher and processed-descriptor evidence must agree. Focused results explicitly cannot establish a full-client-gate pass.

Independent review found no blocking issue in these patches. A freshly reconstructed Java 25 verifier passed all four source sets and **1,116/1,116 JUnit tests** at 10:28:36 UTC, with optional Iris absent from the test runtime and all 3,324 selected source/config fingerprints unchanged. **22 Python CI-tooling regression tests** also pass. The corrective native behavior and focused job still require their next GitHub run; no native pass is claimed from these local checks.

## Baseline

- Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3
- Java 25, Gradle 9.7.1, Fabric Loom 1.18.2
- Pristine baseline main, client, unit-test and client-game-test source sets compile with Java 25 against the official unobfuscated Minecraft jars and declared dependencies.
- Pristine baseline: 1,008 of 1,008 JUnit tests pass under the independent runner.
- Normal Gradle configuration on the cloud executor is blocked by Loom's Unix-domain socket capability probe throwing `SocketException: Operation not permitted`. This occurs before source compilation. The toolchain was not patched and no security setting was weakened.

The independent compile classpath applies Fabric API's declared class-tweaker metadata. Runtime tests use the pristine official jars. This is useful compile and unit-test evidence, but it is not equivalent to a successful Loom build, a remapped release artifact or native gameplay verification.

## Original integrated source gate (historical checkpoint)

At **2026-10-05 04:09:23 UTC**, verification tag `final-20261005-0409` passed a fresh compile of all **753 main**, **236 client**, **124 unit-test** and **289 native-test** Java source files. All **1,114 of 1,114 JUnit tests passed**, with zero skipped, aborted or failed tests. All **3,276 Java/JSON source fingerprints stayed unchanged** throughout the gate.

The final gate includes the ten first-form timelines, server stroke ledger, Survival introductions and first clears, Master body/weapon animations, two-species pressure, and the later Crescent/protection/lifecycle fixes. The final source review found no unresolved defect in its targeted combat, consent, ownership and animation-timing paths. That bounded review does not prove the absence of every bug.

Generated assets also pass consistency checks across **5,500 paths**, and the player guide passes all **132 pages**. All **266 registered native-test entrypoints** resolve to source files without duplicates. Whitespace checks pass. The final gate caught one held-Shield native fixture using the wrong RuneBolt launch overload; only its factory call was corrected, and the complete gate was rerun.

The final Java/JSON fingerprint-map digest is `378ae2cb78a076226e5bec70f78aa812615fd3d717705fcbf905a695c5b13e22` (SHA-256 of the sorted JSON map).

## Foundation build status (earlier immutable checkpoint)

At 2026-10-05 02:57:17 UTC, the complete updated independent gate passed: 747 main, 234 client, 118 unit-test and 280 client-game-test source files compiled successfully. All 1,052 JUnit tests passed, with zero skipped, aborted or failed tests. Fingerprints of 3,253 Java/JSON files were unchanged throughout the gate. Compilation identified and fixed an incorrect Crescent callback return, fixture overload errors and an outdated player-model overlay assumption.

Standalone focused checks already passed:

- 19 party-ledger JUnit tests, including randomized transitions
- 10 crystal-bound, shared-art eligibility/timing and original animation-sampler JUnit tests
- An additional party-ledger stress harness with 100,000 transitions and 3,143,794 assertions
- Generated-assets consistency across 5,500 paths
- Player-guide validation across 132 pages
- `git diff --check`

PNG compression-only changes were removed after comparing decoded pixels; unrelated artwork was not changed.

## Native suites authored and compiled

The original fifteen added suites are registered and compile. GitHub now executes native clients and has reached their Master combat probes, but the focused suite as a whole has **not passed**. Later suites cannot be treated as executed merely because they are listed here:

- PartyMutationSafetyTest and PartyOfflineProjectileTest: direct/delayed damage, harmful/helpful statuses, source restoration, summons, fire/frost/movement, live and retained projectile ownership
- WildercordHeartCirclesTest: circles 9–20, denied/interrupted formation, old-save compatibility, bounds, synchronization and tooltip captures
- WildercordMastersArtsTest and MastersStyleTimelineTest: payment, no windup damage, active-frame/recovery timing, committed aim, follow-up cancellation and weapon-change/no-op behavior
- SwordStringAuthorityTest: server-observed marks, packet order, shared jitter budget, proof consumption and replay rejection
- SwordMasterTrialTest, DuelistMasterAccessTest, MasterVictoriesTest and MasterAntiAirTest: Survival access, consent, roster limits, bystanders, armor/guard/interrupt benchmarks, first-clear persistence, elevated aim and cleanup
- CrescentCoverTest and CrescentAudienceTest: swept and partial cover, initial offsets, repeated parries, originating-trial audience, budget exclusion, clash continuation, party/duel admission and protected recoil
- WildMonsterPressureTest: actual goal/path selection, release cover, cadence, tells and miss recovery
- WildercordMastersArtsPresentationTest: actual keys and ten style strings, body/first-person frames, bindings/help, scales, cancellation and turned/vertical aim
- MasterModelPresentationTest: baked NPC model channels, handedness, anchors, soles, hilt/offhand separation and cancellation reset; synthetic model frames are identified explicitly

The authorized Windows desktop baseline also failed before source compilation: Loom reported `RmStartSession` Windows error 29 and an access-denied cache transformation. No client launched and no screenshots were produced. GitHub has since verified native startup and produced gameplay captures; complete feature/mixin coverage, gameplay, latency behavior and presentation remain release gates. An authored or compiled assertion is not a successful runtime test.

## Review findings addressed in the verified source

- Party relationships are rechecked at the impact and mutation boundary, not only at initial target selection.
- Scheduled effects retain attribution; source-only Aura contexts remain distinct from actual spells for sparring and affinity logic.
- Trial-provenance projectiles cannot spread collision splash to nonparticipants. Held/perfect Shields, retained offline-owner IDs and beneficial-payload exceptions have source-level checks and compiled native regressions.
- Committed shared moves block manual cast, scroll, imbued-item and ordinary attack entrypoints. Already-launched delayed spells are not blanket-cancelled.
- Active frames revalidate player identity, world, weapon and eligibility; accepted damage/interruption cancels the pending hit.
- In Minecraft 26.3 skin overlays are children of their limbs. They inherit parent animation; copying parent transforms would double the motion.

## Follow-ups completed after the foundation checkpoint

- Controlled Master returns now respect held Spell Shields as well as perfect parries. Crescent flight also preserves trial ownership through returns and clashes, sweeps actual collision shapes, checks target cover before budget/damage, and excludes protected observers from clash pushes.
- All ten first-form style arts now use the authoritative timeline, including payment, active frame, recovery and interruption. Physical follow-up cuts are distinct from already-released afterimages and fields.

## Not yet established

- A release-ready artifact with all native gameplay and presentation gates passed (the normal GitHub Gradle build now passes)
- Complete added-mixin coverage and real multiplayer behavior (native startup itself now works in GitHub CI)
- Post-Protection-IV encounter difficulty and performance under several simultaneous casters
- Complete animation/asset coverage for all fifty existing style arts
- Broad rebalance of ordinary creatures and dungeon bosses against the expanded endgame resource ceiling

The final independent source/unit gate includes Survival teacher introductions, permanent first-clear lessons, dedicated Master move poses and elevated aim, bounded anti-air attacks and spell-interrupt immunity, two-species Normal/Hard pressure, server-observed stroke evidence, and latched held-slot changes. Their integration compiles and the full unit suite passes; partial automated native combat probes have run, while complete gameplay acceptance and human playtesting remain pending.

## Offline animation review

Source-driven rig/held-item projections and pure sampler checks cover the three new actions, ten first forms and Master poses. This is offline geometry evidence, not game screenshots. The frozen review records one minor remaining Thorn setup arm/torso intersection (about 29 cubic model-pixels in a brief blended pose), plus deliberate brief windup/recoil viewport exits. Camera near-plane checks passed the documented handedness and aim samples. That original local motion archive and its media were lost when the cloud workspace reset; the persisted source report records its historical findings, but no currently available media artifact is implied. New animation changes require fresh source-matched previews and native capture. Native shader, equipment-layer and multiplayer presentation still need a real-client pass.

## Experimental development JAR

At the original checkpoint, an optional JAR was manually assembled from the independently compiled production main/client classes and repository resources. That local artifact was lost in the workspace reset; the following is its historical packaging record, not a current downloadable release. Normal GitHub builds now produce their own artifacts. It contains 1,933 Wildercord classes and 7,437 archive entries. Static packaging checks verify expanded version metadata, both production entrypoints, registered main/client mixin classes and the declared Minecraft/Fabric/Java dependencies. No Minecraft or third-party runtime classes, nested dependency JARs, unit tests or native-test classes are bundled.

Artifact: `wildercord-0.11.0-masters-dev+mc26.3-EXPERIMENTAL.jar` (20,324,718 bytes).

SHA-256: `aaa0b7eae7f6e6b73ed4bfe6b67b693072cc4d6d86f52ff8ee1fd951e09183fc`.

This particular artifact is **manually assembled**, not produced by the later successful normal CI build, and has no Mixin annotation-processing or runtime-launch verification. Use only for a backed-up disposable test world. It is not approved for production or important worlds. Nothing was installed into a user's game. The feature source is now published through draft PR #1; main remains unchanged. Source is authoritative.

## Next acceptance gates

1. Repeat the normal Gradle build/unit checks for each corrective commit, then pass the focused native suites and all four complete gameplay shards.
2. Verify client/server startup and every added mixin, then real two-client party, reflected-projectile, roster and delayed-effect scenarios.
3. Tune full Protection-IV/netherite combat, coordinated spell pressure, networking jitter and eight-player performance with real play sessions.
4. Review the actual client at multiple UI scales, both hands, armor/skin layers, Iris/shaders and owner/observer viewpoints. Inspect all windup/impact/cancel/recovery frames.
5. Continue bounded, independently reviewed style and Master content in parallel with native repairs; each published slice needs explicit compile, unit, source and native acceptance status. Complete the remaining style body timelines and broader creature/dungeon balance before claiming a finished overhaul.
