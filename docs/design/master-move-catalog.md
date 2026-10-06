# Master move catalog and bounded transition proposal

Status: the lead approved only the passive eight-definition catalog and compatibility adapter on 2026-10-06. That slice is implemented; the planner, live wiring, new attacks and graph remain proposals. Baseline: `8de18a5d`. This document does not enable new behavior, claim native verification, or authorize publication. All work remains in the isolated `design/master-move-catalog` worktree.

## What exists

`MastersRules.Move` contains **eight named attack actions across all schools**. Five are shared: Sweep, Thrust, Crescent, Break Cast, and Pursuit Break. Ember adds Cinder Wake; Gale adds Crosswind Reprise; Stone adds Stone Fracture. Each school therefore exposes six actions, giving 18 school/action references but only eight distinct action IDs. Pursuit's three school tuning profiles are variants of one authored attack, not three new moves. Guard, bolt cutting/redirection, evasion, breathing, individual hits, party lanes, phase reordering, and attack combinations are not additional authored attacks.

| Wire ID | Action | Availability | Tell / recovery ticks | Aura |
| --- | --- | --- | --- | --- |
| 1 | Sweep | all | 18 / 20 | 16 |
| 2 | Thrust | all | 22 / 24 | 16 |
| 3 | Crescent | all | 20 / 24 | 16 |
| 4 | Break Cast | all | 20 / 24 | 16 |
| 5 | Cinder Wake | Ember | 24 / 56 | 24 |
| 6 | Pursuit Break | all | 22 / 30 | Ember 28, Gale 26, Stone 30 |
| 7 | Crosswind Reprise | Gale | 22 / 32 | 24 |
| 8 | Stone Fracture | Stone | 32 / 40 | 28 including brace |

Recovery values are the server's existing values. The rendered active hit occupies one of those ticks: the client currently receives active = 1 and visual recovery = recovery - 1. Cinder Wake's separately warned second hit occurs 26 ticks into its 56-tick recovery; it is a single two-beat action, not a second graph node. A new attack cannot begin merely because that first cut finished.

`SwordMaster.customServerAiStep` owns admission and priorities: lifecycle/roster cleanup; stagger cancellation; ongoing effects; committed attack; dodge; recovery/breathing; exhaustion; held guard; live-cast pursuit; due guard or Stone Fracture; Gale Reprise; projectile evasion; bounded approach/line of sight; ranged/height Crescent; nearby charging Break Cast; Ember signature; deterministic four-step pattern. Preserve this order in the first adapter. In particular, do not move reactive pursuit above recovery or let a graph bypass a due guard.

Resources are finite: Aura max 100, exhaustion threshold 28, breathing 60 ticks, guard cost 12, cut cost 8, redirect cost 20, dodge cost 18. Pursuit cooldowns are 140/100/160 ticks; Reprise 140; Fracture 180. Stone guards after every completed ordinary action, the other schools after every two. These rules are not difficulty knobs for this lane.

Offensive enrollment is narrower than ownership: `participant`, `canHarmParticipant`, `projected`, and `mayCut` currently allow enrolled, living, Survival/Adventure server players in this dimension and arena. Owned summons/projectiles resolve their enrolled owner for incoming harm/control through `MasterVictories.owner` and `acceptsHarmFrom`; they are not automatically outgoing targets. The adapter must preserve both boundaries. Future owned-entity targeting needs a separately reviewed guard change.

Ordinary attacks lock aim six ticks before release; Cinder Wake and the movement signatures accept fixed geometry before their first warning. Pursuit uses a server-authored charge aged at least six ticks and never reads private input, a future action, or a client's requested move. Its same-charge token is checked through impact. Movement uses native collision and safe supported paths; displacement, missed ticks, invalid roster or lifecycle changes cancel. Cover can shorten a warned lane but its removal cannot expand accepted reach. Existing `projected`, spell defenses, guard/parry callbacks, `MasterHitReceipt`, `CastHitRules`, and `Statuses.interrupt` own resolved damage and interruption immunity.

Presentation currently syncs integer attack ID, start tick, and aim pitch. IDs derive from enum ordinal + 1 and are duplicated explicitly in `MasterAnimationRules`. The rigid body/weapon sampler supports all eight; the articulated master sampler supports IDs 1, 7, and 8, with explicit rigid fallback elsewhere. `MasterRenderer` reads accepted server state and clears vanilla swings on cancellation. A catalog cannot quietly replace this with client move choice or equate a renderer stub with authored choreography.

## Distinct-attack acceptance bar and honest counts

A new authored attack needs all four: (1) a meaningful mechanical decision different from existing attacks, expressed in spatial geometry, movement, timing structure, conditional resolution, or resource interaction; (2) a recognizably different warning that tells the player what will happen; (3) a demonstrable counterplay choice and a real punish window; (4) original body and weapon choreography that agrees with the hit and warning. Review includes the strongest prior lookalike and explains the meaningful difference. Recoloring, mirroring, speed/range/damage tuning, extra party lanes, a renamed move, a school skin, or sequence permutations do not pass by themselves.

Track separate fields/counts: unique authored attack IDs; certified authored attacks; variants of those IDs; finite authored transition edges; observed combinations. An attack may have multiple hits. A combination is an ordered use of existing attacks with complete recovery, not a new attack ID. The current census of eight named actions is not retrospective certification of every evidence requirement below.

Scope ambiguity: “hundreds” could mean across the encounter roster, per school, or per individual boss. The foundation supports growth but does not resolve that ambition by inflating counts. An illustrative staged program across the existing three schools is 8 existing → 11 after the first pack → 20 → 32 → 50. Each stage is conditional on fresh mechanics and evidence. Going to 101 would require 32 certified signature attacks per school plus five shared; 200 requires 65 per school plus five shared. That is 70 available per school, not 200 per school. Reaching 100 available per school with five shared requires 290 unique attacks across the roster; 200 per school requires 590. These are arithmetic scope estimates, not delivery commitments. Do not add schools or inflate stats to reach a number.

No finite, eligibility-constrained catalog can guarantee never repeating through an arbitrarily long fight. Promise bounded anti-repeat preferences and varied legal sequencing, while retaining lawful fallback and visible recovery.

## First implementation slice: passive catalog and compatibility adapter

Use immutable Java data records first, outside runtime selection. This gives data-driven definitions and validation without introducing a scripting language, resource-reload attack authority, a JSON parser, or a general effect interpreter. A future validated resource format is optional and separately reviewed.

Proposed new pure classes in `dev.wildercord.aura.world`:

- `MasterMoveCatalog`: immutable `Definition` records containing stable namespaced `id`, fixed `wireId`, legacy `MastersRules.Move`, immutable schools, and a closed execution-family enum. Only the eight legacy definitions exist initially. Canonical ordering is wire ID, with namespaced ID tie-break/duplicate rejection. Count IDs once across schools. Return immutable collections and reject invalid IDs/duplicate wire IDs/school sets. Timing and resource tuning remain delegated; warnings, counterplay and presentation remain in the existing compiled handlers. A later authored catalog may add validated telegraph/counterplay/presentation contract keys after their representation is reviewed.
- `LegacyMasterMoves`: a deliberately small adapter providing lookup by enum/wire ID plus existing tell, active, visual recovery, cost and damage. It delegates tuning to current rule constants/methods; it does not introduce a second source of balance truth. Unknown wire IDs return no definition/idle, never another attack. Explicit map tests freeze IDs 1–8 even if future enum source order changes.
- `MasterMovePlanner`: an unconnected pure planning prototype, only after catalog review, taking a finite graph, seed, decision ordinal, current node, depth, four-ID history, and immutable eligible candidate IDs. It returns a plan proposal plus a next state; it cannot call entities, apply damage, execute commands, create tasks, or mutate the world. Runtime admission remains the server executor's responsibility.

The first production wiring, if approved separately, changes only lookups at `beginAttack` and animation timing/ID getters to call the compatibility adapter. Keep `MastersRules.Move`, all existing IDs, selection order, execution classes, targets, geometry, costs, and cancellation intact. Do not put the experimental planner on the live path. Before wiring, a parity matrix covers all three schools, all eight enum values, party sizes 1–8, valid/invalid wire IDs and timing boundaries. Unsupported school/action combinations remain catalog-ineligible; legacy low-level damage/timing lookups retain their old mathematical behavior for test compatibility.

## Later bounded style graph

The graph chooses the next fully warned action only when `SwordMaster` is free after its entire recovery and any due guard. It never owns or alters in-flight sub-beats. Each school's immutable graph has at most 16 nodes and 64 edges in v1; expansion requires an explicit bound change. Each node references a reviewed action or a neutral state, each edge has a stable ID, bounded positive integer weight (1–100), and a closed enum condition. No arbitrary expression, reflection, command, callback name, class loading, entity selector, or effect program can be supplied by data.

Use a maximum of two action nodes per phrase for Ember and Gale and one for Stone initially, retaining current guard cadence. Every edge crosses the source action's full recovery. Ending a phrase returns to neutral; a cycle in the graph cannot reset depth. A school signature's internal two-beat choreography counts as one action but still owns its complete recovery. Breathing, guard, obstruction, interruption, loss of target, or a changed valid roster end the phrase. Reevaluate eligibility after every recovery; never reserve or execute an unconditionally chained next hit. A graph suggestion is expendable if target, resources or terrain changed.

| School | Style nodes / allowed intent | What stays readable |
| --- | --- | --- |
| Ember | Neutral → broad pressure (Sweep) or marked commitment (Cinder Wake); after full recovery → narrow extension (Thrust) or space (Crescent); then neutral/guard | Shape pressure creates a positioning question. Cinder Wake owns both its original warnings; no new attack overlaps its pending wake. |
| Gale | Neutral → spacing (Crescent) or lateral commitment (Reprise when its existing cadence permits); after full recovery → point pressure (Thrust) or close pressure (Sweep); then neutral/guard | The side and landing are fixed before movement, and the reply keeps its own tell. No last-second tracking. |
| Stone | Neutral → point pressure (Thrust) or space (Crescent); full recovery → the due brace/Fracture when its existing cadence permits; then neutral | Every ordinary attack retains its due guard. Fracture includes its paid fixed brace and separately warned reply; never add perfect-guard retaliation to it. |

These are graph intentions, not enabled sequences. Live-cast pursuit and nearby Break Cast remain observable, eligibility-checked openings selected at the existing priority only from free neutral state. Specials retain sequence cadence and cooldowns; a graph edge does not grant permission. A future first pack adds only reviewed nodes and edges.

For seeded variation, create one encounter seed when the roster locks, retain it for the encounter, and record it with catalog/graph version in test receipts. Use a specified, tested integer mixing function over seed, school, successful decision ordinal and sorted edge IDs; do not use shared world RNG, wall time, collection insertion order, or runtime-dependent object hashes. Derive one bounded weighted choice over canonical eligible edges. A declined runtime admission does not advance the ordinal, consume Aura, change cooldowns or append history. A successfully started action advances once even if later interrupted; this prevents repeatedly retrying an interrupted move without counting it. Native replay must record the accepted observation/eligibility snapshots as well as the seed: a seed alone cannot reproduce a player's actions or terrain.

History is an immutable ring of the last four successfully started authored IDs, independent of variant. Exclude the immediately preceding ID if another legal candidate exists; downweight other recent IDs. Do not fabricate an ineligible candidate to avoid a repeat. If nothing remains, return neutral/approach/guard/breathe under existing rules; if the only lawful attack remains the same after neutral, repetition is allowed and recorded. No redraw loop, unlimited history, graph search, or lookahead into inputs.

## Runtime safety contract before activation

Keep one server-owned accepted attack at a time. Validate free state, current roster, finite sufficient Aura, all cooldowns, graph depth and executor-specific terrain/line of sight before paying once and accepting fixed geometry. Repeat lawful-target/ownership and lifecycle checks at every impact and at movement/effect ticks. Retain attack-instance hit ledgers, callback reentrancy protection, interruption immunity and existing damage pathways. Do not shorten recovery after parry, miss, cancel, dodge, graph transition, phase change or spell response; existing move-specific cancellation contracts remain the minimum baseline.

The catalog describes an allowlisted execution family; code owns actual geometry, safe-motion checks, timing, damage routing, bounded particle/target/hit/effect counts and cancellation. V1 contains only existing executors. Future families require code review and explicit caps, with no terrain editing, homing after lock, speed override, invulnerability, dynamic effect execution or client-authoritative hit. A warning must bound actual harm. Changed cover may reduce harm but never expand a captured warning. Missed scheduled beats expire harmlessly rather than appearing late.

## First authored pack proposal, after the foundation

Three candidate attacks, one per existing school, not yet accepted or implemented:

1. Ember **Kiln Ring**: a stationary, visibly annular sword ignition with a clearly marked safe inner pocket and outer escape edge. Its decision is moving inward versus outward, unlike the existing broad cut plus narrow wake. A distinct full-turn coil and low circular release show both radii. This needs a new bounded annulus executor and per-target cover clipping; no persistent fire or damaging terrain.
2. Gale **Vaulting Cut**: a short, fixed, collision-checked vertical arc to a premarked landing wedge, with one descending cut and a long planted recovery. The player can leave the landing, interrupt early takeoff, or use cover to cancel/block the route. This changes vertical movement and landing timing rather than reusing Reprise's lateral step. It requires a genuinely new safe arc executor, ceiling/edge failure tests and original takeoff/airborne/landing choreography. Omit it from a first release if native collision/counterplay evidence is weak.
3. Stone **Fault March**: one planted blade strike sends a small finite sequence of individually warned, advancing ground bands; unsupported ground and cover stop propagation, and stepping sideways or jumping a band is counterplay. The fixed advancing timing and grounded propagation distinguish it from Crescent's free flight and Fracture's brace/reply. A single attack-wide hit ledger prevents damage multiplication across bands. This needs bounded ground propagation, no block changes, and a clear stomp/blade-plant/recovery performance.

Do not count a candidate until its strongest lookalike review, mechanics and native evidence pass. Tune damage within the existing reviewed budget; these proposals are not permission to raise health, armor, speed, damage, resource regeneration, or reduce tells. If a prototype is only a variant in play, record it as a variant and keep the authored count unchanged.

Cross-system overlap already exists: `ShapeRunners.ring` supplies a hollow ring; player `StoneArts` has Avalanche's rolling ground shockwave and Mountain Splitter's progressive ground line; `GaleArts` has Updraft and Tailwind traversal. Therefore a ring, advancing stone, or aerial movement alone is not a new mechanical invention. These candidates require a separate authored NPC decision/telegraph/counterplay contract and original choreography, reviewed against those player mechanics as well as the eight existing boss attacks. They are not proposed as new player unlocks and are not counted now.

## Evidence and release gates

Foundation checks: immutable collection/duplicate/bound validation; exact enum→ID and ID→enum parity; school counts; legacy cost/timing/damage parity; deterministic candidate order; fixed-seed golden sequences; same seed/snapshots replay across fresh planners; declined admission leaves state unchanged; finite history/graph/depth; no eligible choice; one eligible repeating choice; cooldown/Aura/NaN rejection; interruption and terminal reset. Inert helpers passing unit tests are not native combat acceptance.

Before production adapter wiring: independent source review of the exact patch, successful relevant JUnit checks, build/generated-asset/package checks per repository gates, and fresh native Masters suites on the integrated source. Existing native `SwordMasterTrialTest`, anti-air, lifecycle, hit-receipt, pursuit, school-motion, party mutation and roster checks remain authoritative. Do not modify a test merely to fit a changed contract.

Before graph activation or each new authored attack: native solo and admitted-party runs with target departure/death/dimension change, outsiders and owned sources, duplicate ticks, skipped ticks, cancellation, cover insertion/removal, dodge, guard/parry, spell interruption/absorption/no-damage results, exact Aura debit/cooldown, every phase and a guaranteed post-attack punish. Include body and held-weapon sequences at warning, movement, impact and recovery from readable front/side views and the opponent's first-person view at supported FOVs; demonstrate a successful counter and a failed counter. If a move becomes player-accessible later, it also requires player first-person hand/weapon evidence. Capture exact-source render receipts, cancellation/idle fallback and reduced-presentation settings. Screenshots alone do not establish mechanics; pure rule tests alone do not establish rendering. No imported Epic Fight code or assets.

The lead's review of this proposal precedes production edits. Review of a later immutable source patch and fresh required gates precedes any integration. This lane performs no merge, release, deploy or remote write.

### Passive slice verification

Java 25 focused compilation passed for the two new helpers and seven JUnit classes. All 54 focused tests passed: the nine new catalog tests plus existing Masters, animation, pursuit, Ember, Gale and Stone rule tests. They cover the frozen IDs, eight-action/six-per-school census, immutable snapshots, duplicate and malformed input rejection, canonical ordering, unknown-ID idle behavior, resource/damage parity, and half-tick animation parity through every legacy tell/recovery boundary. Reports are outside the repository at `/workspace/shared/master_move_catalog_evidence/junit`.

This is focused pure-Java validation. Gradle/Loom, the aggregate build and native game suites have not run for this isolated patch. No runtime caller uses the new helpers, and no selection, executor, roster, damage, renderer, resources or native fixtures changed. Seeded replay and graph tests are requirements for the later planner, not claimed results of this slice.
