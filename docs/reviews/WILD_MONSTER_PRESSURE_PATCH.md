# Bounded ordinary-monster pressure patch

Source work completed 2026-10-05. This is implementation and regression coverage, not a measured difficulty multiplier, playtest, or whole-roster rebalance.

## Changed scope

- `src/main/java/dev/wildercord/monster/MonsterPressureRules.java`: pure bounded difficulty/cadence/navigation decisions.
- `src/main/java/dev/wildercord/monster/Gloomstalker.java`: consumes those rules; safeguards release and contact damage.
- `src/main/java/dev/wildercord/monster/BogWitchFrog.java`: consumes those rules; adds a dedicated bounded approach goal and safeguards release.
- `src/test/java/dev/wildercord/monster/MonsterPressureRulesTest.java`: ten pure regression cases.
- `src/gametest/java/dev/wildercord/monster/WildMonsterPressureTest.java`: native entity/goal/navigation fixture. The suite is registered for independent verification.

No health, damage, armor, spawn weights, configuration, strongest-player scaling, world difficulty, or other species changed. Easy keeps its existing cadence and attack priority; Peaceful hostile-spawn/despawn behavior is untouched. Safety revalidation applies at every difficulty.

## Exact cadence (ticks, inclusive)

| Behavior | Legacy / Easy | Normal | Hard |
| --- | --- | --- | --- |
| Gloom initial stalk | 40–79 | 26–45 | 20–35 |
| Gloom stalk after retreat | 50–99 | 28–47 | 22–37 |
| Gloom retreat deadline | 50–79 | 32–47 | 26–39 |
| Frog bubble start-to-start reservation | 80–129 | 60–89 | 50–74 |
| Gloom pursuit navigation multiplier | 1.10 | 1.20 | 1.25 |
| Frog dedicated pursuit multiplier | Disabled (melee stays 1.0) | 1.10 | 1.15 |

Gloom's fourteen-tick crouch, thirty-tick missed-pounce sprawl, ordinary twenty-tick swipe interval, frog's eighteen-tick swelling, ten-tick mouth tell, twenty-tick gulp, tongue cooldown, damage and health are unchanged. Retreat may end before its deadline when navigation completes, as before. The listed retreat interval is not a guaranteed recovery floor; missed-pounce sprawl remains the real guaranteed recovery.

## Concrete decisions fixed

1. Gloom previously circled at 9.5–10 blocks even though it could not begin its pounce beyond 9.5. On Normal/Hard it pursues outside eight blocks or while sight/vertical access is unsuitable, retaining its six-block orbit when a pounce is useful. Easy's old ten-block chase threshold remains.
2. A Gloom pounce now checks legal target, current sight, 9.5-block maximum distance, at most two blocks upward and three downward at release. Entering cover or leaving reach cancels the leap and reserves twenty ticks before another tell. Navigation can resume during that delay. It does not teleport or increase leap velocity.
3. Gloom contact pounce damage and its close swipe require current sight; the contact also rechecks legal targets. Hitting the crouched Gloom still interrupts it and starts retreat.
4. The frog selects its ready tongue first on Normal/Hard at the existing 2.8–6-block initiation range. Its mouth tell, shield counter, and cooldown remain. Outside twelve blocks or behind cover it uses ordinary ground navigation at a capped multiplier instead of relying exclusively on a sight-dependent melee goal.
5. After all eighteen swelling ticks, the frog rechecks legal target, sight and the original 4–16-block bubble band. Taking cover or leaving that band cancels the projectile, without refunding the reserved cooldown.
6. Pursuit routes are requested at most every eight active ticks. A failed or partial route waits twenty ticks before another request. Navigation may follow a partial ground path, but no forced relocation or attack through missing sight is introduced. Path computation remains vanilla; this is a query cadence bound, not a performance benchmark or proof every obstacle is solvable.

## Verification status and limits

- Source whitespace check passed.
- Ten pure JUnit cases are ready for the independent verifier: legacy Easy endpoints; Normal/Hard exact bounds; sampling endpoints; difficulty clamp; old orbit dead band; cover/ledge decisions; release checks; tongue priority; bubble cancellation; fixed tells/recovery and path retry bounds.
- Native fixture is ready for registration and compilation. It exercises actual registered entities and goal/path selection, covered release cancellation, reserved cooldowns, full tell/recovery values, and Easy behavior. It intentionally advances private phase deadlines and invokes AI methods with autonomous ticking disabled, so it is a deterministic runtime fixture, not a live combat benchmark.
- This source audit does not establish a Gradle build or native gameplay result. Updated independent verification outcomes are recorded in MASTERS_VERIFICATION.md.

## Remaining balance and safety work

- Live beginner / midgame / Protection IV netherite playtests on Normal and Hard; walking and sprinting backpedal, terrain, roofs, narrow doors, airborne players, melee dodge/parry, spell interruption and groups. Measure actual encounter pressure before advertising a difficulty multiple.
- The other four ordinary monster species and the broader boss/elite ecosystem are outside this patch. Advanced progression still needs recognizable appropriately rewarded encounters rather than all spawns matching the strongest nearby player.
- BogBubble's pre-existing radial splash and lingering vanilla area-effect cloud have their own cover semantics. Release-time sight now respects a hiding primary target; this patch does not claim all already-airborne splash or lingering poison is blocked by walls.
- The unchanged frog can still eat nearby prey during downtime; that distraction and healing need native encounter measurements before tuning.
- Gloom still selects dark retreat destinations with the existing bounded six-candidate search. Complex terrain may yield partial or unreachable routes; no pathfinding rewrite or universal anti-kiting guarantee is claimed.
