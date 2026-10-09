# Mana Skin native before/after verification

Verified on 2026-10-05. These are executed Minecraft/Fabric tests, not a whole-release pass.

## Correctness result

Mana Skin now restores 20% of the current hit’s actual nonlethal red-health loss after armor, enchantments, Resistance and absorption. The price remains two mana per health actually restored. It does not heal earlier wounds, heal through a full absorption hit, or rebate lethal-to-zero damage subsequently handled by a separate death save.

The native Protection IV control used a real attributed `minecraft:player_attack` source, explicitly checked not to bypass armor or enchantments. Both players had armor 20/toughness 12. Without Skin, the wound was 4.27392 HP; with Skin it was 3.419136 HP, exactly 80%, costing 1.7095642 mana within float precision. The earlier fixture used vanilla generic damage, which bypasses armor; that source was corrected without reducing the wound/rebate assertions.

The complete native seam helper returned successfully before the benchmark: bare and pre-existing wounds, full/partial absorption, Resistance, insufficient/threshold mana, minimum wound, Gash healing suppression, rejected hit, missing Cord, real P4 armor, lethal/death-save separation, nested callbacks, exception restoration, self-heal attribution and the pre-rebate low-health watcher. This follows the actual ordered helper invocation; not every individual assertion prints a separate line.

## Controlled survival benchmark

The benchmark source is byte-identical in both runs: SHA-256 `8b9be5b2900c0a98059ff4d88acb44a931ed39d30b580a6b8e8b89874bb83169`.

- Fresh disposable world, seed 98432026; Normal difficulty.
- Genuine connected Survival player with 20 HP, all four Protection IV netherite pieces, Circle 20 and Echo Cord.
- 600 starting mana and 18 mana/second production regeneration, no meditation/Ley/well boost, crystals 0.
- No natural health regeneration, casting, guard or player movement input; native knockback remains active.
- Naturally ticking enrolled Gale Master with fixed initial RNG seed; no manual attack scheduling, AI pause or refill during observation.
- 400-tick maximum, ending early on death. This is a stationary unguarded benchmark, not a measurement of skilled combat, maximum possible builds or coordinated multiplayer.

| Actual hit tick | Master raw damage | Old Skin health after hit | Corrected Skin health after hit |
| --- | ---: | ---: | ---: |
|41|36|20|14.940416|
|86|26|20|11.885312|
|171|36|20|6.825728|
|345|36|20|1.766144|
|394|42|20|0, death|

Both versions recorded six attack starts/releases and the same five accepted hit ticks. The old formula erased every post-armor wound and survived the 20-second window at full health, with 592.2 mana remaining, minimum 583.2, 70.400085 spent and 62.600098 regenerated. The corrected formula died at 19.7 seconds with 600 mana remaining; it spent and regenerated 9.116882 mana, with minimum 597.4702. Available mana no longer converts a 20% wound rebate into complete damage negation. No Master damage statistic was increased for this comparison.

## Exact native sources and limits

- Baseline `b5bd9d2d3a0ca2d9850e5346599edc9b62fe8843`, [run 37346996028, full shard 4](https://github.com/defnotean/wildercord/actions/runs/37346996028/job/111887955413): benchmark completed at 17:51:37 UTC, then the shard failed later on the old Kindling Draw visibility assertion.
- The same baseline run’s [focused job](https://github.com/defnotean/wildercord/actions/runs/37346996028/job/111887955236) independently recorded identical benchmark results at 17:17:42 UTC. It later stopped producing output during unrelated combat checks and was cancelled at its existing 60-minute boundary. The benchmark did not stall.
- Corrected `e0dfa2980bb0d166ecd6145d216ef5f27f5ff728`, [run 37351823046, focused job](https://github.com/defnotean/wildercord/actions/runs/37351823046/job/111904271385): native P4 control at 17:56:12 UTC; benchmark death result at 17:56:47 UTC. Later combat and ordinary art phases completed, then the job failed a cancelled-neutral capture that still contained the initiating vanilla swing/item dip.

These results establish the bounded Mana Skin correction and the measured unguarded comparison. They do not establish full aggregate CI, every school/gear combination, multiplayer fairness, human enjoyment or release readiness. Existing release gates remain open.
