# Crimson Moon reconstruction

This work starts from public commit `160ff12556754428edc686ef261d4e5255e7363a`. The earlier unpublished timeline, motion, capture-source and evidence files were lost when the execution environment was replaced. Their historical test counts and preview hashes do not validate this reconstruction. The public base already contains Moon's independently released original-owner guards; that code is left intact.

## Approved gameplay contract

Presentation ID 19 uses ten ticks of windup and twenty of recovery, with `ACTIVE_CONE`. This is an existing Crimson Sovereign Final, requiring full/full/full/low, not another ability. Base cost/rest remain 40 Aura and 600 ticks with existing modifiers. Acceptance pays Aura/rest once; cancellation keeps those costs and physical recovery. Health toll and completion/momentum consumption occur only at release, including an empty cone. Final conditions are admission conditions and are not checked again after payment.

The windup/recovery and strict release cone are deliberate gameplay counterplay changes. Accepted horizontal facing stays fixed while current feet at release supply the origin. The existing 180-degree, six-block arc (including target half-width and vertical allowance) chooses the nearest ten legal visible targets. Owner LOS applies before the cap. An old last-struck target cannot bypass that cone. Released identities and distance bands remain fixed; later range/cover changes do not re-query targets.

Existing mechanics remain: toll `max(0, min(maxHealth * .25, currentHealth - 2))`; direct damage at release `1 + min(4, floor(distance / 1.2))`; 2.5 weapon factor; six 0.1-factor wound beats ten ticks apart with the existing 1.5 moving multiplier; one shared Hits ledger and Drink bucket. Drink uses 50% of actual health loss, capped at ten plus the common mending allowance, including valid lethal damage. Five ground crescents expand outward at +1…+5, radius 1.2…6. They do not return. Current party/team/duel/trial admission and Effects/Scheduler provenance remain intact.

## Original motion and fresh checks

The Classic keys express a low broad stance, outside-hip chamber, one rising-edge sweep, far-side follow-through and free-hand counterbalance. The twenty-joint articulated body has independent camera keys. Chamber is 6.5, physical release 10, follow-through 14 and neutral 30. `ACTIVE` is only `[10,11)`; direct damage follows at ages 11–15. Wounds add no physical swing. The reconstructed follow-through is newly authored and requires fresh visual review.

The fresh standalone Java 25 checks exercise both hands and widths, centered sockets, planted soles, rigid/immutable matrices, continuous phase edges, skin/sleeve near-plane clearance, HUD reserve and stock armor/funded-shell winding. Their JUnit wrappers are included. They do not prove Minecraft baked geometry, actual item pixels, material quality, native input or server lifecycle behavior.

A fresh independent compile of the public baseline gives the same 0–18 Classic/articulated body/view sample fingerprint as the modified source:
`837c4514b76867d30ab236e916a6262055206ae011866e8c5b2180c35b51fd08`.
The old fingerprint algorithm and old 1,498-test result are not reused as new evidence. Fresh focused compilation passes all three changed production files, both changed game-test files and nine changed/new test files. All 47 focused JUnit tests pass. The full frozen-source gate and native execution remain pending.

The Classic chamber hand was then moved inward, raised and eased after a fresh opaque-blade regression caught clipping in the 4:3, GUI-scale-4 HUD reserve. A Moon-only test preserves the existing ground-field clipping and visible-edge-span policy across the whole clock, both hands and all four viewport layouts.

The new `CrimsonMoonTimelineTest` is registered in the full native descriptor and compiles under Java 25. Its eleven scenarios drive three fully charged native Attack/Punch pairs followed by a low pair into the real checked request path. Peak momentum is explicitly declared fixture setup; no performer replacement, ledger-stroke injection, cooldown reset or physical-lock clearing occurs. Each prior individual rest expires through real ticks. It checks unseen/duplicate requests, once-paid price/rest, release-only toll and momentum/completion, cancellation, closed eligibility after payment, all five direct delay bands and six wound beats, moving wounds, current feet and locked facing, the actual stale struck victim, LOS before the nearest-ten cap, immutable released target snapshots, lawful lethal direct/wound drink, common mending, the shared player damage cap and observed single-hit player stance wear. That player case exhausts damage in its first hit; it does not prove cumulative clipping of stance wear at fifteen. A legitimate native multiple-hit stance-budget witness remains open, with pure shared-budget tests providing only separate mechanism coverage. The existing independent owner-lifetime suite still covers body/world/callback/allegiance retirement. These are compiled native test sources; none has been run natively in this rebuilt environment.

The legacy art gallery now performs its three Moon toll probes through real windup and recovery. It retains the original four-victim wound/economy checks and full-health→75%, 3→2 and 1.5 unchanged thresholds. `GameRules.NATURAL_HEALTH_REGENERATION` is disabled and asserted false so saturation cannot hide the delayed toll. Its prior isolated individual-cooldown reset remains explicit and is not evidence of actual client input.

## Fresh offline mesh preview

`tools/preview_articulated_crimson_moon.py` exports both hands and original wide/slim production meshes with the optional `--crimson-moon` exporter mode. The existing exporter modes retain their prior selection. The new mode samples actual Moon body/view matrices through the official ModelPart implementation across its complete 30-tick clock, then draws four source keyframes from first person and two opposite oblique views.

The first fresh run passes 38,560 matrix and 3,856 socket comparisons (maximum error 0.00000114), 96 unchanged-policy HUD samples and 78,084 opaque-geometry near-plane placements. The nearest tested opaque point remains at camera z = -0.370589, behind the -0.05 near plane. This is reproducible offline geometry evidence, with source/dependency/executable/output hashes in its report. It is not a game screenshot, GPU/deferred-pass receipt, armored/funded-shell render or multiplayer proof. Right-handed third-person release partially overlaps the torso in the sampled projections; actual native alternate observer angles must establish its silhouette.

## Unclosed visual-proof requirements

No former unapproved receipt implementation is being treated as recovered or passing. Before a new Moon visual receipt can be accepted it must:

1. Numerically compare actual deferred body/view joints and submitted item matrices with the intended source palette, including the observed vanilla baseline. Merely storing a hash does not prove geometry, and a skipped Classic transform must fail.
2. Bind every body, view and item pass to the complete extracted activation, move, hand, aim and frame identity. Matching pose values alone cannot admit stale or substituted frames.
3. Validate the exact intended backend/eligibility inputs: actual sword/offhand, skin, equipment and funded shell; whole fallback must specifically prove the requested shell-adapter fallback, not an unrelated incompatibility.

Native acquisition must use actual Final input and original connected skins, wait for cosmetic holds naturally, and bind the returned PNG to callback pixels and the actual source phase. No time freeze, retime, pose injection or fake observer can substitute for it. The shared/opening 192 gate remains untouched.

The shared multiplayer supervisor is owned by the integration lead. The future bounded mandatory Moon case is `articulated_netherite_funded_right_release`, across actual wide/slim profiles and front/reverse oblique observer angles, requiring genuine owner FP and separate TCP observer receipts for one accepted action. This bounded proof cannot stand in for a broader owner/phase/hand/fallback matrix. No Moon adapter should be enabled before its source and native receipt contract pass fresh review.
