# Masters of Tomorrow release checklist

This is a release gate, not a declaration of readiness. Keep the pull request in draft until the open checks below have evidence against the final release candidate. A source review, unit pass, or earlier commit's native pass cannot substitute for that candidate's results.

## Verified checkpoints (2026-10-05, 19:29 UTC)

- Published `66f09e6e` passed the normal build, generated assets, packaging audit, all **18 focused update suites** and all **three articulated native suites** in [run 37357354405](https://github.com/defnotean/wildercord/actions/runs/37357354405). The focused selection includes the existing-world save/reopen suite, combat/progression/party checks and actual first-person/third-person presentation. This is exact-commit focused evidence, not an aggregate pass.
- That run captured the real Gale and Stone school warning/release/recovery poses against a consenting server-side fake challenger, observed by a real spectator client. These are original rigid-fallback renders from separate natural trials per frame, not continuous footage or proof of the newer articulated-school backend. Closer articulated body views and separate full-lane views are authored and await their own native run.
- Mana Skin's complete correctness helper passed after using a physical damage source that respects armor. The byte-identical Circle20/Protection IV benchmark measured the same natural Gale attack cadence before and after the correction: the old rebate kept the player at 20 health through 400 ticks; the corrected rebate left the player dead at tick 394. Damage values were unchanged. See [the exact native measurements](../audit/mana-skin-native-verification.md) for the controlled setup and limits; this is one unattended solo benchmark, not a coordinated-party balance conclusion.
- Full aggregate CI remains red on `66f09e6e`. Shard 1 failed the Siltcrest setup's sustained-swimming/bank admission; shard 3 failed Bolt Step's late endpoint distance after its actual three arrivals; both have narrowly scoped fixture repairs awaiting native reruns. Shard 4 failed the non-player Driving Cut damage/lock assertion even though the same SHA's focused selection passed; the failed instance remains unidentified. Source review found that this ordinary-target fixture could randomly promote its Husk and refill health during admission; the local follow-up explicitly preserves and verifies its ordinary baseline and records release/lock measurements. Shard 2 is still running at this observation. Keep these outcomes distinct from earlier commits.
- The current local follow-up adds original articulated Gale/Stone school poses, native Master unload cleanup, bounded party-name retention, closer capture receipts and test-JVM stall diagnostics. These changes are source-reviewed and have focused or combined independent checks, but their new native contracts remain pending. All full shards and focused selections stay enabled.

## Scope and playable behavior

- [ ] All requested content intended for this release is implemented and accessible through ordinary play, with complete descriptions and controls.
- [ ] All three Master schools have sufficiently distinct decisions, attacks and readable counterplay. Ember has its delayed Cinder Wake, Gale its committed Crosswind Reprise, and Stone its frontal brace followed by Fracture Reply. Their focused automated cases pass on the checkpoint above; final-candidate multiplayer and human gameplay acceptance remain open.
- [ ] Normal full charges can be punished from a valid early opening. Fast/instant casts, early release, cover, sidestep and full guard remain real counters.
- [ ] Interruption consistently distinguishes an accepted damaging hit from a blocked/rejected hit. Verify absorption, Mana Skin, lifesaves, exact charge identity and the shared immunity window. The separately reviewed consistency implementation extends the same receipt to close-range BREAK_CAST and player Driving Cut. Their expanded native cases passed at earlier checkpoints; the final candidate must retain those results. Driving Cut preserves its idle-player seal and CastLock recovery, while a held spell additionally requires unchanged charge identity and shared interrupt-immunity admission.
- [ ] Crystal absorption at 100, Circle 20 progression, rewards, existing saves and beginner progression work together without unbounded power or resource exploits. The Mana Skin checkpoint has complete focused-native correctness and one before/after sustain benchmark, including lethal-to-zero/lifesave separation; the final candidate and broader multiplayer sustain matrix still need acceptance.
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

- [ ] Every intended supported move and state has reviewed body, weapon, first-person and armor behavior. The original articulated Spellcut/Master-sweep proof does not cover all remaining style arts; the authored Gale/Stone articulated extension still needs native visual acceptance.
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
