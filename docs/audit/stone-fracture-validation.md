# Stone Fracture validation

This is an isolated content slice based on `2d13121a`, reviewed on 2026-10-05. It appends synchronized move ID 8 and leaves the first seven IDs, encounter consent, party limits, source ownership, shared damage receipts, Mana Skin and Heart Circles unchanged.

## Accepted mechanic

- Eight exposed plant ticks, twelve fixed-facing frontal brace ticks, then twelve separately warned reply ticks
- One 28-Aura payment including the brace; 180-tick cooldown; forty exposed recovery ticks after a hit, miss or cancellation
- One captured participant in a fixed six-block-long, 1.4-block-wide lane, within 1.8 vertical blocks
- 28 base damage through Stone's existing 1.1 multiplier, matching Stone pursuit's 30.8 budget; no party damage increase
- Brace-only rear health/absorption punishment, ordinary axe and stance breaks, solid cover, sidestep, backstep, held guard, parry and Foresight
- Original planted cross-body brace, overhead gather, downward point release and recovery on the rigid body/weapon rig; optional articulated Master rendering remains Sweep-only

## Checks completed

The focused rules and animation run passed 22 tests. Generated-asset verification passed all 5,500 paths, comparing decoded PNG pixels and exact non-image bytes. Unrelated platform-dependent PNG encoding changes were restored after verification.

An independent static reviewer found no production, pure-test or documentation defects. Native-fixture review caught a short-lived threat stimulus: slowing the bolt did not extend its original 34-tick lifetime through a forty-tick recovery. The fixture now verifies the first live threat, launches a second native threat on recovery tick 24, verifies their live overlap and discards the first. A separate API check corrected absorption setup to set and verify the native maximum before awarding absorption hearts. Neither repair changes production or weakens the recovery and damage assertions.

After both fixture repairs, the independent reviewer reported no remaining findings. The final frozen Java 25 gate, `stone-fracture-final-20261005-1755`, compiled 778 main, 247 client, 136 test and 308 gametest sources and passed all 1,202 JUnit tests. All 3,399 source fingerprints were unchanged across the gate. It finished at 2026-10-05 17:55:45 UTC. The verifier used original publisher Minecraft jars at JUnit runtime; compile-only Fabric access metadata does not imply a Loom or native-client pass.

## Native gate still required

`StoneFractureChecks`, registered in `SwordMasterTrialTest`, authors thirty scenarios, four declined admissions and natural resource exhaustion. It advances actual AI through the opening Thrust, Fracture admission and all committed phases without private attack-state injection or manual AI advancement. The fixture covers eight-player audience isolation, damage provenance, no-knockback front/rear receipts, actual physical knockback, rear hits before/during/after brace, absorption, axe, stance, both interrupt boundaries, cover changes, parry, held guard, Foresight, target loss, paused AI and paid recovery under an incoming threat. Exact cooldown bounds are pure-rule assertions; the natural cancellation followup proves sequence, guard and resource behavior without claiming an independently observed cooldown expiry.

The default 0.65 knockback resistance is preserved. An ordinary frontal melee hit can physically displace Stone and cancel Fracture on the following tick, while rear brace damage cancels immediately even without knockback. The fixture tests this distinction instead of suppressing real motion. Native gameplay and balance must establish whether that tradeoff produces the intended pressure.

No local Loom build, native client run, visual capture or multiplayer balance acceptance is claimed. Native execution remains a separate GitHub gate. Compilation and authored scenarios are not evidence that those scenarios passed in a running client.

## Subsequent native functional result

On 2026-10-05, public commit `d9a831e962fa5a55b81c5aae5a4760d4e4ab804a` executed the complete `StoneFractureChecks` helper through the actual ordered `SwordMasterTrialTest` entrypoint in [run 37353975204, focused job 111911570017](https://github.com/defnotean/wildercord/actions/runs/37353975204/job/111911570017). All 30 defense/lifecycle scenarios, declined admissions and resource checks returned before the subsequent hit-receipt and presentation suites. The native AI selected and progressed the form; tests did not inject its private attack state.

The overall focused job remains failed: a later Rising Cinders cancellation screenshot was captured while ordinary vanilla hand motion was still active. That unrelated failure does not erase the completed functional assertions, and their completion does not make the full job green. Actual Stone/Gale spectator phase captures are a separate pending visual gate; human and multiplayer balance acceptance remain open.
