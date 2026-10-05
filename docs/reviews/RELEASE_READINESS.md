# Masters of Tomorrow release checklist

This is a release gate, not a declaration of readiness. Keep the pull request in draft until the open checks below have evidence against the final release candidate. A source review, unit pass, or earlier commit's native pass cannot substitute for that candidate's results.

## Verified checkpoints

- Published `6ac294e2`: normal build, all 16 then-current focused update suites, and the original articulated native suite passed in run 37316763054.
- Published `5737fca5`: normal build and all three articulated suites passed in run 37323834315. These include native armor/material, reload, hand-socket and HUD-composition assertions. Genuine owner-camera Spellcut samples show the revised wrist placement; not every captured view has been manually inspected.
- The 17-suite update job on `5737fca5` stopped at the pursuit fixture's combined grounding/roster assertion. The later native suites cannot be counted as passed. Full aggregate CI is not green.
- Independent Java 25 gates compile main, client, unit and native-test sources. The latest local combat contract/timing gate passes 1,176 unit tests. This is additional evidence, not native execution.

## Scope and playable behavior

- [ ] All requested content intended for this release is implemented and accessible through ordinary play, with complete descriptions and controls.
- [ ] All three Master schools have sufficiently distinct decisions, attacks and readable counterplay. The shared foundation and Ember follow-up are not the entire requested combat expansion.
- [ ] Normal full charges can be punished from a valid early opening. Fast/instant casts, early release, cover, sidestep and full guard remain real counters.
- [ ] Interruption consistently distinguishes an accepted damaging hit from a blocked/rejected hit. Verify absorption, Mana Skin, lifesaves, exact charge identity and the shared immunity window. The separately reviewed consistency implementation extends the same receipt to close-range BREAK_CAST and player Driving Cut; the exact candidate still needs native acceptance of those callers. Driving Cut preserves its idle-player seal and CastLock recovery, while a held spell additionally requires unchanged charge identity and shared interrupt-immunity admission.
- [ ] Crystal absorption at 100, Circle 20 progression, rewards, existing saves and beginner progression work together without unbounded power or resource exploits.
- [ ] Ordinary creatures and dungeon bosses are balanced against the new endgame ceiling. Two species' pressure changes are only part of that review.

## Automated release evidence

- [ ] Normal Gradle build, generated assets, unit tests, packaging and dependency checks pass on the exact candidate.
- [ ] All four aggregate native shards pass. Keep failures visible; do not substitute focused success or weaken assertions to obtain green status.
- [ ] The focused update and articulated groups pass with exact catalog counts and provenance. Fixture fixes require native reruns in the original relevant order.
- [ ] Every added Mixin applies in the actual client/server runtime, including the damage receipt and existing-world mutation guards.
- [ ] Native regressions cover cancellation, ownership, delayed effects, reflections, roster locking, bystanders and retained offline projectile owners.

## Multiplayer, difficulty and performance

- [ ] Real connected clients verify parties, invitations, duel exceptions, boss enrollment, disconnect/rejoin, dimension changes and hostile/helpful effects. Fake-player/server fixtures alone do not establish this.
- [ ] Controlled latency and packet-order tests preserve fairness and server authority without replay, free payment or animation/hit-direction divergence.
- [ ] Solo and coordinated groups fight with representative endgame gear, including Protection IV netherite, absorption, Mana Skin and high-circle spell sustain. Record outcomes, opportunities and failure causes; do not claim an unmeasured difficulty multiplier.
- [ ] Human play sessions confirm readable tells, satisfying first-person/third-person combat, usable controls and counterplay. Automated bots and scripted cases do not prove that a fight is fun.
- [ ] Profile server tick time, client frame time, particles, packet volume and retained maps during sustained eight-player combat and repeated trials. Agree numerical budgets before calling the result acceptable.

## Animation and presentation

- [ ] Every intended supported move and state has reviewed body, weapon, first-person and armor behavior. The original articulated Spellcut/Master-sweep proof does not cover all remaining style arts.
- [ ] Inspect genuine before/release/recovery captures, handedness, supported skin widths, UI scales, free look, equip/ordinary-swing transitions, menus and cancellation.
- [ ] Inspect enchanted/trimmed armor, outlines, transparency, reloads and supported resource/shader configurations. Unsupported equipment must retain the complete fallback.
- [ ] Curated images remain byte-identical, correctly labeled and bound to their commit/run; absent frames and unverified phase timing remain explicit.

## Saves, existing worlds and delivery

- [ ] Native `UpgradeRecoveryTest` passes, including a real save reopen, unloaded-chunk deferral, fresh authorization and ABA-safe rollback. Controlled save/reopen is not power-loss proof.
- [ ] Fault tests cover journal/chunk save ordering, failed persistence, policy changes, claims errors and later edits. Permanent placement stays disabled without exact operator approval; never use a valuable user world as a test fixture.
- [ ] The pavilion adapter's limits are clear. Large structures, dungeons, bosses and entity retrofits remain separate adapters requiring their own safety and duplicate-prevention evidence.
- [ ] Upgrade, backup, recovery and rollback documentation is usable; test migration from supported old releases and removal/reinstallation behavior in disposable saves.
- [ ] The final normal-build JAR has correct version/dependencies, resources and registrations, no bundled Minecraft/runtime classes or secrets, and a recorded hash. Install only into an explicitly approved test environment for acceptance.
- [ ] Final release notes distinguish completed features and remaining compatibility limits. Obtain the required decision before merging main or publishing a release; development authorization does not itself perform those actions.
