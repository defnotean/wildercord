# Masters of Tomorrow: development roadmap

Updated 8 October 2026. This is the plan for the expanded update, not a claim that it is release-ready. The feature branch is still in a draft pull request. The [release checklist](reviews/RELEASE_READINESS.md) records evidence and remaining checks; the [living-world roadmap](LIVING_WORLD_ROADMAP.md) retains earlier exploration and ecosystem commitments.

## What is already on the development branch

The current foundation includes three Master schools with consent-based trials, finite Aura, readable attack commitments, shared cast-punish pursuit, and an Ember, Gale and Stone signature. Party protection, first-clear records, the 100-crystal absorption cap and progression through Circle XX are implemented. Mana Skin now rebates a bounded fraction of actual nonlethal health damage rather than damage before armor.

All three shared player arts and all fifty existing style forms now have authored Classic body/hand timelines and an authored articulated backend, including the ten opening forms (Crackle through Blossom Fall). This is separate from native visual acceptance, which is still pending for the newest forms. The six authored earned counters have ordinary native input, observer and visual acceptance gates; Mirror/Riposte and Unmoved/Null have independently reviewed source and bounded offline geometry evidence; current-source native execution remains pending. The newest counter slice is local and unpublished. Crimson Moon's Final-input and released-lifetime suites have passed on published checkpoints, and one funded owner-first-person release passes on dff8c58f. Four genuine wide/slim front/reverse observer release views pass on `17dd0598` with exact clock, image and causal damage receipts. This covers one selected funded-netherite right-hand release, while the complete visual catalog and final polish remain open. New choreography is not counted as a new gameplay ability.

The branch also contains opt-in articulated combat, supported armor and funded Aura-shell presentation, staged in-game presentation preferences, and a bounded operator-approved pavilion upgrade for existing worlds. Each has its own remaining compatibility and runtime checks. The pavilion does not yet retrofit the full dungeon, boss or encounter catalog.

Earlier focused native checkpoints have passed. On `ac1711`, build, articulated, connected combat, aggregate shard 1 and March mechanics pass, but core and aggregate shard 3 expose a Moon continuation regression and the March opponent escape check fails. The normal build and unit tests do not make the whole release green. These limits are tracked openly in the release checklist.

## Priority 1: a dependable playable foundation

- Retain the newly passed native settings, field-callback and Hail owner/acceptance scenes on the final candidate. Finish the later ground-field and phase-capture checks without weakening assertions; earlier scene passes do not make a whole job green.
- Resolve the actual wildlife navigation/arrival and presentation failures using observed causes. Preserve natural behavior, existing bounds and the full aggregate test order.
- Complete actual body, weapon, first-person, armor and effects evidence for the current authored forms. Keep Classic presentation as the default while supported opt-in paths are validated.
- Keep all four aggregate native shards and the separately labeled focused groups. The update gate now requires three bounded parts with an exact 44/1/2 class union (47 total): core, March mechanics, and March visuals. Each part remains mandatory; only all three matching candidate/run/attempt results establish focused acceptance. Every release claim must refer to the exact candidate.

## Priority 2: make player progression rewarding

This is the next gameplay vertical slice alongside the stability work. Circle VIII should mark a new way to play, and the climb to XX should add meaningful choices. Maximum Aura should offer decisions beyond higher output.

- Audit every existing unlock, where a Survival player learns it, and whether the controls and tradeoffs are visible.
- Deliver one complete mage progression path and one complete Aura path first: in-world discovery, a short teaching encounter, an earned capability, loadout choice, practice opportunity, feedback and an encounter that rewards using it.
- Add bounded high-tier options with costs, limited preparation or equipment slots, commitment and recovery. Avoid filling later circles with only mana, regeneration and damage multipliers.
- Test old-save progression, already-qualified players, respec/reset behavior where supported, party interactions and the lowest legitimate access route.

The first authored player capabilities are **Relay Circle** and **Wall Turn**. Both are integrated after independent source review. All four Relay lesson, lifetime, defence and impact suites return successfully on published `dff8c58f`, after the inactive-listener correction. All three Wall Turn Lesson, Safety and Commitment diagnostic suites pass, including reciprocal Relay switching. These are scoped results, with final-candidate, multiplayer and visual checks still open. Relay gives an eligible Circle VIII Archivist a paid focus and a second deliberate cast input, with restricted spell grammar and two sightlines. Wall Turn gives a Sovereign with a recorded Gale clear one equipped movement form: an exposed wall brace, outward kick and real landing recovery. Each has an original retrievable lesson and must work for already-qualified characters without repeated progression. Wall Turn now has its own segmented articulated joint palette and one continuous brace, kick, fall and landing clip, unit-tested for continuity and armour. A focused native smoke (`ProgressionVowWallTurnNativeTest`) confirms that a real brace and kick produce the weighted articulated frame. It also found and fixed a client clock skew that hid every kick step in both Classic and articulated presentation. Screenshot, third-person and observer visual acceptance remain open.

Progress on the rest of this priority:

- **Audit:** done; see `docs/audit/PROGRESSION_AUDIT.md`. It closed two small gaps: the player wiki now covers Master's Arts and Master forms, and the wiki Heart Circle table names each circle's gift.
- **Later circles:** every circle from VIII to XX now grants exactly one new thing. VIII gives Archmage, Relay and the Masters invitation; X, XII, XIV, XVI and XVIII give lessons. The seven circles that were multiplier-only (IX, XI, XIII, XV, XVII, XIX and XX) each ask for one bounded **Circle Vow** between two sides (`docs/design/circle-vows.md`).
- **Respec:** a vow can be released for 5 levels and chosen again. Spell traits, techniques and form slots keep their existing bounded changes.
- **Tests:** unit tests cover vow rules, old-save loading of every progression record, and party rules for forms and lessons. The native smoke covers old-save load, a paid respec through the real `/vow` command, restart persistence and the Wall Turn frame.
- **Still open:** the vow balance numbers and their chat-and-`/vow` interface are owner decisions. Multiplayer acceptance and the complete mage and Aura vertical slices are not done.

## Priority 3: expand magic through distinct uses

- Add substantial authored spell packs spanning pressure, defense, control, traversal, utility and cooperation.
- Expand spell shaping and combinations through new tactical rules and clear costs. A different color, stronger number or permutation of existing runes does not count as a new ability.
- Give each addition a use case, discovery method, understandable tooltip, recognizable sound/animation and counterplay. Check how it interacts with terrain, claims, parties, summons and delayed ownership.
- Organize the existing large rune catalog so players can find useful combinations without relying on commands or external code knowledge.

The catalog expansion will be delivered in tested packs, with new abilities, upgrades, recipes and cosmetic variants counted separately.

Reweave's whole native field-feasibility class passes on `6c750309` and `5cea20d1`. The reviewed Circle XII player slice is now implemented: an old-save-accessible Ebb Ledger lesson for a Low Tide holder, one 40-base-mana Harm field with shared rest, and one warned disc-to-lane conversion that preserves expiry, remaining beats and the original target budget. Its complete native player class passes on `3f8b54e1`, including real lesson/editor/rebound-key use and reconnect/respawn checks. Full restart, broader multiplayer and visual acceptance remain required; this counts as one new capability.

The source now implements **Excise at Circle XVI**: a retrievable Rootbound lesson and a paid, interruptible commitment to remove one hostile deployed Zone emitter. The reviewed contract preserves sibling fields and previously inflicted poison or other aftereffects. Its whole ordinary-player class returns on `31ba951b` in 72.42 seconds, covering the real lesson and held-input scenarios, per-emitter cut, sibling preservation and its authored lifecycle checks. It is mandatory in the core part of the expanded 47-class gate; final-candidate acceptance remains required. Genuine player-owner permissions, observer visuals and process restart remain open; this counts as one authored capability, not completion of the magic expansion.

**Status (8 October 2026, uncommitted):**

- **Done:** the rune count is now 756. The new packs are mostly not for fighting: farming, fishing, mining and crafting, exploring and travel, support, hearth and passive runes. There are also 17 links and 36 modifiers, with ten conditions such as If Night, If Underground and If In Fields, plus field and kin shapes. No two rune recipes share an ingredient set, and no two runes share a name or a complete animation.
- **Catalog:** the Rune Catalog (Ctrl+B, or the Catalog link in the Cord screen) searches runes. It filters by family, element, use and what the player knows, and lists "goes well with" companions built from the compiler's own rules. Search never reveals text the player cannot read yet.
- **Verified:** unit tests (2086 on the merged tree) and the shapes and support packs' native suites.
- **Open:** in-game review of the Catalog layout at each GUI scale. The use tags come from keywords in the rune text, so a few are noisy.

## Priority 4: expand Aura movement and sword expression

- Add learned footwork, evasion, gap closing, stance choices, parry follow-ups, spell-cut counters and aerial options connected to sword styles.
- Preserve deliberate commitment: movement has swept collision, terrain and hazard checks; cancelled travel has a defined stop and recovery; a landing is distinguished from water, timeout or aborted flight.
- Every style form now has authored Classic and articulated choreography. Native visual acceptance and in-game polish of the newest forms remain open.
- Give movement and follow-ups readable first-person hands as well as full third-person choreography. Keep input discoverable and rebindable, with stable-camera and reduced-flash choices.

New movements must change positioning decisions without replacing every dodge, cover choice or punish window with unlimited mobility.

The moves pack is implemented in development. **Parry follow-ups:** a perfect guard against a living attacker opens a 16-tick moment. Pressing the form key in that moment answers with a learned follow-up: W gives Ember Riposte, A or D gives Gale Shove, and no key gives Stone Break. Each one pays Aura, shows a set tell, owes recovery, and is taught by its school's teacher through a lesson book. The follow-ups do not use the slot. **Spell Cut** extends `SpellCutRules` with a Survival counter. A swing with a hostile bolt in reach severs it for 14 Aura. A near miss pays 6 Aura and owes a longer recovery, and a swing with no spell near costs nothing. It is learned from the Ember teacher. **Air Step** (Gale) and **Plunging Strike** (Stone, Sovereign) are aerial slot forms. They use the same one-slot rules as Wall Turn and Cinder Lunge. Every step is swept against blocks, fluids, hazards and wards. Each one ends as a landing, a splash or a stall, each with its own recovery. Neither clears fall damage, and a released form's own fall damage counts as its landing, not an interrupt. **First-person hands:** classic first-person poses now cover Cinder, Reed and every new form and phase. Wall Turn and Stone Hinge keep their existing hands. The poses honour the stable-camera and reduced-flash choices. **Input:** Relay, Excise and Reweave no longer lose a press and release that both fall between two ticks (a sub-tick tap). A unit test shows the old loss and the fix. A focused native class (`MovesPackTest`) passes in single player. It covers the lessons and teachers, Air Step landing, Plunge landing on a foe, a Plunge splash, all three follow-ups with their timeout, the Spell Cut miss and sever, and a first-person capture under stable camera and reduced flash. Still open: the real perfect-guard path in a live fight (the test opens the moment directly), observers and multiplayer, a dedicated server, restart, and visual polish of the third-person choreography for the new forms.

Stone Hinge's native damage/impulse precursor passes on `5cea20d1`. Its proposed melee knockback redirect must preserve actual damage, vertical physics and ordinary knockback on refusal. Independent review rejects the first server-move/velocity-packet approach because it cannot synchronize the already-moved position to the owning client. A genuine-owner negative control on `0aed34e6` confirms zero owner lateral movement and loss of the server-only offset. A separate, reviewed velocity-deflection comparison preserves the native impulse magnitude, Y and packet dispatch. All fifteen connected-owner cases return on `6d2a7ce8`. The corrected peer diagnostic on `ac1711` preserves sixteen complete owner/server/tracker/peer case chains; the natural-Master case fails before completion and the later delay profiles remain unexecuted. The next reviewed diagnostics retain the original motion assertions and preserve the first failure before teardown. The ordinary form now exists in development: a Stone teacher teaches it at Sovereign after a recorded Stone clear, it shares the one Master-form slot with Wall Turn, and a grounded form-key press with one held strafe pays 20 Aura for a 6-tick plant and 12-tick catch that turns only the first frontal hostile melee impulse by the reviewed velocity method. Refusal keeps native damage and knockback. A focused native single-player test covers learn, equip and use; peer, latency and dedicated-server acceptance remain open, as do the NOT_PROVEN movement and admission gates in the design notes. Until those checks pass it is gated behind the server switch `aura.experimental_stone_hinge` (off by default): off, the teacher does not offer it, equip and use are refused, and a learned form stays learned but cannot be equipped.

## Priority 5: bosses with large, distinct repertoires

The requested long-range goal is hundreds of genuinely different fighting moves and many legal combinations. It is not met. The previously accepted three-school union has nine named attack actions, including Ember's Kiln Ring; Ember has seven available actions, while Gale and Stone each have six. School tuning variants, individual sub-hits and permutations do not increase that unique-action count.

Stone Fault March ships live in every Stone Master fight, announced by its own hint banner: three ordered, fixed ground bands with inward, lateral and jump counterplay, one attempt per participant, finite Aura and a full exposed recovery. Its original Classic and articulated motion and three native suites are authored. Its whole mechanics class returns on `31ba951b` in 37.95 minutes, but the diagnostic reaches its existing time cap during presentation before opponent-view checks begin. Its native full-visual and opponent-view acceptance runs are still owed; until they pass, the formally accepted count stays nine, and a regression there is fixed in place rather than by removing the move.

**Combat retune and the technique repertoire.** Masters now fight at a faster tempo. Ordinary cuts have 12–14 tick tells and 14–16 tick recoveries, down from 18–22 and 20–24. The pause between exchanges is 40 ticks instead of 60. Ember and Gale guard after every third attack instead of every second. Movement and approach speeds are higher, and signature recoveries and cooldowns are shorter, although Kiln Ring and Fault March keep their authored clips. After the plain opening, most ordinary melee slots become one of 100 named techniques (34 Ember, 33 Gale, 33 Stone). Each technique is a two-to-four-strike chain of authored poses on wire id 11. Every strike is warned and aimed separately, with a minimum 5-tick tell, and the whole chain is capped at 1.75 strike-equivalents of damage. Each strike shape has its own answer: jump a low cut, crouch under a high cut, sidestep a lane, or leave a whirl's 3.75-block radius. Techniques are counted as authored combinations of shared strike primitives, not as unique signature actions.

- Build an immutable attack catalog with stable IDs and explicit school, timing, resource, warning and animation contracts. The immutable catalog and compatibility layer are implemented with parity tests. The live adapter is currently limited to ordinary attack choices; it does not yet execute a large authored pack or unrestricted signature combinations.
- The bounded school-specific graph now has a source-reviewed live adapter for eligible ordinary attack choices, with seeded history, recent-move avoidance and complete paid recovery. Existing priority responses and signatures retain their own admission. Its bounded native ordinary-planner suite passes on dff8c58f, including the authored transition/cancellation checks. Real coordinated-party and human counterplay acceptance remain open; a seed alone cannot reproduce player behavior.
- Preserve complete recovery and punish windows between attacks. No graph may bypass a due guard, Aura cost, cooldown, terrain check or committed aim, and no boss reads a future player input.
- Expand with small original packs whose attacks differ mechanically and visually, then broaden the repertoire after native counterplay and animation review.
- Track unique authored attacks, variants, transition edges and observed combinations separately. Finite catalogs cannot guarantee that no long fight ever repeats a sequence.

The first pack develops the identities of Ember, Gale and Stone. Ember Kiln Ring now passes all three dedicated native mechanics, presentation and opponent-view suites on `160ff125`. Its fixed annulus offers inward, outward, cover and timed-jump answers, with visible inner and outer boundaries. Continuous gameplay, final-candidate compatibility and the whole release remain open. Increasing health or recoloring the same swing is not a substitute for this goal.

**Status (8 October 2026, uncommitted):** every breathing method now has a Sword Master: 16 schools and 417 named techniques. The new Masters are Rime, Thunder, Verdant and Hollow; Starlit, Hourglass and Crimson; Tide, Iron and Dune; and Echo, Dawn and Venom. Each new Master has a signature attack with a stated answer and a first-clear lesson. Tide, Iron, Dune, Echo, Dawn and Venom are new breathing methods, with five arts each.

- **Verified:** the unit tests and every pack's own native trial on the merged tree.
- **Open:**
  - Articulated (full-body) poses for the Tide, Iron and Dune signatures, which fall back to Classic.
  - Native runs of Whirlpool, Bulwark and Sandveil.
  - The shared BIND reward (Hollow, Hourglass, Dune, Venom): an owner decision.
  - The full aggregate shards on the final candidate.

## Priority 6: lore that changes what players discover and do

- Audit existing canon before extending the schools' histories, rivalries, magic traditions and ruined training grounds.
- Tie new lessons and abilities to NPC dialogue, exploration clues, short discovery quests and encounter rewards.
- Deliver lore in the game through readable conversations, journals, locations and repeatable hints, with persistent discovery records where needed.
- Make rewards and clues useful for both newly created worlds and existing characters. Keep optional background readable without burying combat instructions.

The first player-progression paths should demonstrate this approach before a much larger quest and lore expansion is counted complete.

**Status (lore pack, uncommitted):**

- **Done**
  - `docs/LORE_CANON.md` audits the shipped canon. It also sets the agreed canon for Tide, Iron, Dune, Echo, Dawn, Venom and a Master for every method.
  - A per-player Lore Journal (`dev.wildercord.lore`):
    - Saved, kept through death and synced only to its owner. Old saves read as empty.
    - Fills in on its own from progress the player already has.
    - Opens on a rebindable key (H), with Leads, Places, People, Learned and Talk tabs.
  - Six short discovery leads: Scout's Wall, Hessa's Gate, Keeper's Openings, Warden's Threshold, Different Hands and Masters' Ledger.
    - Each has a trigger, a clue, a goal, a hint you can read again, and a one-time reward.
    - The reward is vanilla XP plus Aura Shards and/or Blank Runes, and Aura XP for players who know a breath.
  - Duelists and Sword Masters have short greeting, hint and parting lines for all 16 methods, plus shared lines for any other method.
    - The lines are kept in the journal. Combat lessons are unchanged.
- **Verified**
  - Unit tests for the quest state machine and for journal persistence.
  - One native run of `LoreJournalGameTest`, which completes one lead end to end and opens the journal on H.
- **Open**
  - Natural-play checks of the other five leads and the teacher lines.
  - A multiplayer sync review.
  - Master lines for the other schools appear only once those Masters ship.
  - The larger quest expansion.

## Priority 7: bring supported content to existing worlds safely

- Expand the current preview/region-approval/journal framework with individually reviewed structure and encounter adapters.
- Keep permanent placement subject to explicit operator approval of the exact region and preview. Defer unloaded chunks, preserve player edits, and retain claims/error refusal and rollback rules.
- Add separate boss/entity duplicate prevention and saved content-version records. Do not regenerate whole explored chunks or assume old worlds have complete edit provenance.
- Test restart, failed saves, changed claims, later player edits and recovery in disposable worlds before adding larger templates.

Status:

- Every worldgen structure and feature now has an individual, tested catalog decision.
- Four bounded adapters exist: the pavilion, plus the Sleeping Blade rest, Battlefield memorial and Sword Tomb duel ring encounter sites.
- The three encounter families refuse a duplicate in the same spread region. The check uses journal manifests, saved natural and upgrade content-version records, a live-Gravekeeper check and the generator's own placement.
- An encounter anchor is marked authentic only after every write is observed.
- A native disposable-world test covers claims, restart, later edits, duplicates and rollback.
- By decision, large dungeons and the full natural structures stay world-generation-only. Master training grounds, Duelists and tournaments are runtime-only.

Remaining gaps:

- The generator check is not verified natively, because test worlds do not place the structure sets.
- Under presets that lack the set, an old natural site in a chunk that has not reloaded is unknown.

See `docs/SAFE_WORLD_UPGRADES.md`.

## Priority 8: presentation, usability and performance

- Finish supported articulated moves, hand sockets, first-person composition, armor materials, shell effects and whole-model compatibility fallback.
- Validate both handednesses and live skin widths, HUD scales, free look, equip/swing transitions, menus, resource reloads, outlines and supported shader/resource configurations.
- Make ordinary enablement, persistence, reset, Classic fallback, stable camera and reduced flash understandable in-game.
- Measure sustained eight-player server tick cost, client frame time, particles, packets and retained state. Bound effects and target searches; do not infer performance from a few screenshots.

**Status (8 October 2026, uncommitted):**

- **Performance harness:** `PerformanceHarnessTest` runs 8 fake players plus an observer for 600 measured ticks. It records tick time, entities, particles and packets, and fails on per-player state that is still kept after players leave.
  - Its first run found 17 such leaks, all fixed or pruned. One run measured p50 1.4 ms and p95 3.1 ms.
- **HUD layout:** `HudHandednessLayoutTest` checks every HUD and screen at GUI scales 1-4 and auto, in both handednesses. It found four overlaps (Stance, Aura and Spell HUDs, and the crossroads banner at scale 4), now fixed.
- **Verified:** both tests pass on the merged tree. See `docs/testing/performance.md`.
- **Open:** window sizes other than 1280x960, shader packs, and tighter time budgets once CI numbers exist.

## Priority 9: candidate integration and release decision

- Freeze a candidate with the agreed content scope and complete ordinary-play access.
- Pass normal build, generated assets, unit tests, packaging, every required native suite and all four aggregate shards on that exact candidate.
- Run real multiplayer, latency, representative endgame sustain, save migration and human gameplay sessions. Automated scripts do not establish that the fights feel good.
- Verify the distributable JAR, dependencies, licenses, version, upgrade instructions, rollback guidance and release notes.
- Present remaining compatibility limits and the readiness result before deciding to merge or publish a release.

There are no invented completion dates. Work advances through usable, reviewed gameplay slices while runtime regressions are repaired in parallel. The requested magic, movement, boss and lore expansions remain open until their actual in-game delivery and acceptance are complete.
