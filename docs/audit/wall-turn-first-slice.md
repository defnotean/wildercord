# Wall Turn: isolated first slice

Base: `38d16ca013dae54008a44546c65c809cf933e0be`. This document describes local source and authored acceptance tests, not a published build or a completed native playtest.

## Player loop

A Sovereign with the existing persistent Gale Master clear can sneak-use a wandering teacher with an empty main hand. The weapon-in-hand trial invitation and ordinary teacher duel remain available. The three-page *The Scout's Unfinished Step*, by Iven Reed, continues the Returned Step account without requiring that memorial. The teacher offers an explicitly labelled charcoal illustration of brace, kick and landing; the NPC does not perform a physical demonstration in the world.

Accepting the offer permanently learns the form. An optional original-cover written book is given if a main-inventory slot is free. A full inventory never drops the only copy: all three pages remain readable on the Master-form page and in the Grimoire. Prior legitimate Gale clear records qualify without a second fight, and the breathing method is unchanged.

The Master-form page is linked from the Aura screen. The ordinary Cord key opens the entry screen even without a Cord; Tab and Enter reach its native Aura badge, then the native Master-form label. Both retain their existing painting with visible focus marks and native narration. The page has one equipped form slot, independent of written-technique capacity. Equipping and emptying require safe solid support, no combat, no active movement/recovery, and an expired shared rest. The slot does not reset that rest. Native focusable, narratable buttons support both mouse and keyboard; the learning-source action opens concrete requirements and the control/commitment explanation. The same help remains available after learning. The form screen selects its intended Next, Accept or Equip focus in the post-init focus callback, including page rebuilds and resizes.

The dedicated rebindable key defaults to C in Survival. Minecraft 26.3 Quick Actions is G (SDL key 10); C is SDL key 6. C also serves as the Creative save-hotbar modifier with a numbered slot. The client suppresses Master-form activation in Creative, the server refuses flight-enabled bodies, and KeyMapping updates every binding for a key, preserving that Creative chord. F3+C is handled by the debug chord path before ordinary click dispatch. The native binding checks keep Quick Actions in their normal conflict census and exempt only the verified disjoint Creative chord. Airborne beside a visible wall, a fresh press accepts a brace for at most ten server ticks. Release the key, then press freshly on a later tick to commit the direction from observed movement intent; with no movement, kick away from the wall. Sneak or a menu cancels. The accepted kick travels over eight real server steps, at most four blocks horizontally and 1.25 blocks above its starting height, returning to that height before ordinary falling resumes.

The accepted brace pays 20 Aura once, including while awakened, and reserves 120 overworld ticks of shared rest. Refused contact pays nothing. An interrupted or obstructed paid brace receives no refund. Only genuine safe support observed on two ticks rearms the airborne use; that confirmation begins ten ticks of art recovery. Pending descent and recovery deadlines are persisted, independently of the transient movement owner. Descent commitment is capped at the existing six-second form rest, followed by ten recovery ticks; expiry never grants another wall contact. Flight/vehicle takeover or body/world retirement converts pending descent to the same bounded recovery without reducing an existing recovery deadline or paid rest. Ordinary walking is never disabled. Practice completion follows the actual kick and safe landing and grants no XP or combat reward.

## Authority and geometry

`MasterForms.Progress` persists learned/equipped/rest/airborne-use/practice and bounded descent/recovery deadlines separately from transient sessions. A session belongs to the exact connected `ServerPlayer` body. Each body/world session gets a new epoch, a monotonic input sequence and a held-edge latch. C2S form packets carry only action, epoch and sequence. Teacher acceptance carries only an already-issued offer nonce; its captured body, teacher, level, range, sightline, expiry and progression are rechecked by the server.

Every accepted movement stretch checks a conservative union of complete body boxes in 0.125-block pieces. It checks loaded cells before reading terrain, body collision, both world-border corners, fluids/fire and other harmful contact blocks, and each cell's exact dungeon-room membership. The new movement-only ward query reads only resident FULL chunks with getChunkNow, including referenced structure starts. Unresolved references are explicit UNKNOWN and refuse movement rather than comparing equal. It does not call the legacy StructureManager lookup, and existing block/structure rules retain that lookup unchanged. The server finds the wall using horizontal collision rays and an unobstructed eye-to-contact ray. Neither a packet nor an enemy supplies the wall, endpoint, cost, damage or learned state. Newly added cover interrupts the next actual stretch.

The movement saves accumulated fall distance, restores it after synchronized server teleports, and adds actual controlled descent. It never calls `resetFallDistance`, creates invulnerability, inflicts damage, chooses an enemy, or moves another entity. Aborting leaves the airborne use and rest consumed.

The original-body receipt is retired by genuine death, respawn, disconnect and world changes. A same-call dimension round trip cannot revive it. Real held-slot/item changes cancel synchronously through the existing inventory hook, including away-and-back changes. Accepted damage uses the existing accepted/absorbed-versus-rejected/full-blocked interruption rule. Stance breaks and admitted control effects cancel synchronously. Each movement step rechecks weapon identity, method, life, world, control, flight, vehicle, collision and competing ownership.

`MasterFormMovement` supplies a bounded ownership lease for existing Aura Step, ArtKit dash/blink/launch, shared native movement helpers, and the complete callback lifetimes of spell Dash, Grapple and suspension. Same-body world round trips do not discard a still-live legacy movement lease. Sustained third-party movement must register a server-side blocker with `registerBlocker`; unknown external movement is not claimed to be compatible. Registration exceptions conservatively refuse Wall Turn.

## Exact combat-commitment contract

- `ownsMotion` is true only for the physical brace and kick. `committed` is the separately persisted, bounded Aura-art commitment through descent and recovery.
- Fixed Master arts, sword-string arts and Aura-key techniques, including Step, wait while `committed` is true. The gap after the final kick or a cancelled brace cannot admit another such art before recovery.
- Ordinary sword swings remain available. An actual swing accepted by vanilla, or a validated physical hit, retires active movement without refunding it; malformed/rejected animation attempts do not.
- Ordinary Cord casts and held charges remain available during descent/recovery. A fully admitted cast, or a valid charge admission, retires only active physical movement. Invalid slots, invalid casts and other rejected requests do not cancel it.
- Ordinary offhand spell-scroll and Imbue use keep their existing admission and payment. Once admitted, they retire active motion. No new blanket offhand or spell lock is added.
- Starting Wall Turn refuses an existing held charge, another committed Master art, or a registered movement owner. Equip changes also refuse those shared commitments.
- Generic accepted interruption now includes form cancellation in `Charging.interrupt`. Incoming roots, FROZEN/AIRBORNE marks, stance breaks and other admitted control hooks latch physical cancellation before a same-call cleanse can remove the visible status.
- Death, respawn, logout and world changes retire transient movement and input epochs. A pending art commitment becomes ten ticks of persistent recovery; an existing landing recovery deadline is retained. The one-use flag and paid six-second rest remain, so retirement is not a reset route.

## Presentation boundary

Server events describe the actual brace, accepted kick steps, fall transition, landing and abort, with ordered epoch/serial identifiers and server time. They do not enter `MastersStyleRules`, and no cosmetic fixed-release timer decides an endpoint or a landing.

The authored palette uses the original Classic player body, existing skin and original first-person hand/grip compositor. The opt-in articulated backend explicitly yields to this Classic fallback during these new poses, so its idle segmented arms cannot hide the movement pose. **This slice does not add an articulated Wall Turn joint palette.** Existing armor, skin and weapon compatibility remains under the established fallback behavior. A newer accepted combat art takes presentation priority over the short outgoing movement transition.

There is no new flash, camera shake or particle dependency. Reduced/quiet presentation retains the body phase, explicit HUD state and bounded brace progress bar. The lesson includes the current rebinding-aware form control, cost, rest and limits.

`tools/ExportWallTurnPoses.java` projects the actual pure Classic palette with the shared torso pivots and hand transforms. The resulting SVG is offline pose evidence, not proof of native rendering, movement or two-client agreement.

## Verification and remaining release gates

Authored pure checks: progression and finite price, bounded height/distance, packet replay/held-repeat/menu latching, distinct body/hand phase poses, bounded mirrored hand transforms and original hip pivots.

Authored registered native suites:

- `dev.wildercord.aura.WallTurnLessonTest`: native keyboard entry through the real Cord binding and Aura badge without a Cord, narrated locked source help, retained parents, Next/Accept/Equip focus after page changes and resize, old eligible Survival progression, actual teacher offer and keyboard UI acceptance with a full inventory, mouse and keyboard readback, equip, rebinding, real jump and C controls, measured payment, kick and safe landing, retained practice without XP.
- `dev.wildercord.aura.WallTurnCommitmentTest`: unloaded referenced starts without chunk loading, invalid/admitted ordinary spells and charges, real offhand scroll/sword actions, same-call admitted root/mark removal, full native dash/grapple leases, recovery across world retirement and bounded impossible landing.
- `dev.wildercord.aura.WallTurnSafetyTest`: missing contact, duplicate/held packets, saved fall risk, same-call weapon switch-back, rejected versus absorbed damage, a newly placed obstruction, real body collision, hazardous fluid, border, ward, dimension round trip and native respawn replacement.

These suites are registered for the existing `-PfocusedSuite` path. They do not yet constitute a complete native release gate. Native execution, live skin/hand submissions and screenshots at required sizes/scales, real disconnect/reconnect and reload, two clients under different latency, a boss ground-warning escape and an attack that still catches the exposed brace remain required. Existing movement/casting regression suites must also run on the final combined source.

The shared `SpellCaster`, `Charging`, scroll/Imbue and accepted-swing boundaries above are implemented here. `Effects.push` retires form motion only after its existing party/Anchor/resistance admission and for an actual nonzero impulse. The Relay current-Cast veto remains ahead of those checks.

## Combined Relay admission, 6 October 2026

The combined tree starts at `5edb81ca1365e2df68905689afc682cb772eabbc` and carries all three reviewed Wall Turn commits. Relay warning/recovery excludes Wall Turn before Aura payment. Only an accepted paid Wall Turn retires an uncommitted Relay focus; its mana price and eight-second rest remain consumed. An admitted first or second Relay input retires only a physical brace/kick, preserving form rest, airborne-use consumption, landing recovery and accumulated fall risk. Relay remains an ordinary spell during form descent/landing recovery.

`ActionAdmission` is a synchronous original-body receipt, keyed by player UUID so replacement bodies cannot re-enter the same callback. It spans validation/payment callbacks and is always closed by try-with-resources; it is not a timed gameplay lock. Reentrant form, Relay, ordinary cast/charge and Aura-art input is rejected. Genuine damage, equipment and lifecycle retirement still run synchronously. First Relay placement reserves mana, rest and its receipt before payment/progression callbacks; retirement cannot resurrect the receipt afterward. The form reserves its cost/rest before spend hooks and retires the prior focus only after final validity checks.

Registered movement predicates are also callback boundaries. Local eligibility, aborted/original-owner validity and exact session/active receipts are checked after those predicates return. A predicate that cancels while returning false cannot produce an orphan brace, kick or step, or consume the prior paid Relay focus. Tick, landing and equip handling likewise refuse a retired session; a competing lease installed by a predicate is observed before payment. Native registered-predicate fixtures cover preliminary lease creation, paid brace cancellation, the fresh kick and the actual server tick, retaining the appropriate costs/rests and fall state.

Rejected slots, invalid stored spells, validation vetoes and insufficient payments preserve the prior paid state. Ordinary Cord casts recheck body, Cord/row, control and current resources after BEFORE_CAST before cancelling either prior action. Ordinary held charging keeps its existing admission policy, including a SELF-only shape group with no payload; once CHARGE is actually installed it retires motion immediately. An invalid second Relay lane or mismatched request slot leaves a still-valid paid focus available for another fresh input until its original expiry; actual selection/edit, cancellation, loss of validity and lifecycle events retain their existing retirement behavior. No path refunds or shortens either ability's cooldown.

`WallRelayChecks`, called by the existing `WallTurnCommitmentTest`, exercises actual paid switches, rejection/resource/callback cases, duplicate edges, warning/recovery exclusion, true landing recovery, fall-state/cooldown preservation and original-body lifecycle. `RelayCircleTest` now sends a real rejected editor reply through MasterFormsScreen → AuraScreen → the exact retained Cord editor. The Masters catalog preserves its original 27 entries and appends the three Wall Turn suites, for 30 in the existing CI job. These native fixtures are authored and compiled evidence until the lead executes native CI; no isolated compilation proves gameplay or rendering acceptance.

Asset regeneration produced byte changes in 1,842 existing PNGs from the available encoder. All decoded RGBA pixels matched the base exactly; those unrelated bytes were restored. The new cover/model and changed language remain generated by the registered helper.

## Initial local verification, 6 October 2026

The coordinated reconstructed JDK 25 gate `wall-turn-final-20261006-0345` compiled 794 main, 256 client, 156 unit-test and 368 native-test Java sources. All 1,410 JUnit tests passed, including seven new Wall Turn checks. Its source fingerprint remained unchanged. This gate does not run Loom, a Mixin annotation processor or the native game.

A second complete `tools/generate_assets.py` pass had no unexpected content changes or new files; 1,842 existing PNG encoder-only differences again had identical decoded pixels and were restored. `git diff --check` passed. The offline SVG was generated by the included Java exporter and visually inspected as a raster projection. Native behavior and presentation remain unrun, with the release gates above still open.

## Native scroll-fixture correction, 6 October 2026

The first actual Wall diagnostic on `64902f05` returned successfully from the lesson and safety suites, then stopped in the combined commitment fixture at its alleged rejected SELF-only scroll. The compiler preserves an explicit SELF group and emits a missing-payload warning; that is a nonempty compiled spell. Production scroll use therefore accepts it, retires the uncommitted Relay focus and consumes the scroll. The former rejection fixture had the wrong input.

The corrected fixture proves real empty-input and unattached-modifier rejection, plus a separate admitted SELF-only case. Each case checks the paid focus and attached Cord/main-hand identities immediately after offhand assignment, then records the actual use result, consumption, focus state and unchanged paid Relay cooldowns. Bounded native diagnostics distinguish the assignment boundary from the use boundary. A pure compiler regression pins all three admission categories. Production compiler, scroll, Relay and inventory behavior remain unchanged. These corrected native assertions require the next exact-source game run.
