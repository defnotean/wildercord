# World depth audit and development backlog

Started 2026-10-13 at `0a18358929180f1c4bc5cc4b827f7f92f05571c4` (Minecraft 26.3, Fabric, Java 25).
Requested outcome: a detailed magical adventure with polished combat, bosses, buildings, NPCs,
custom biomes, legends and connected lore. Work continues on `defnotean/world-depth-audit`.

## Audit scope and evidence

This is a repository-wide **structural and content audit**, with source review of the initialization,
world registrations, encounter catalogues, lore/quest persistence, networking boundaries, and test/build
pipeline. It is not a claim to have played every encounter or reviewed every line of approximately
300,000 production Java lines. Runtime, visual, multiplayer, generation-frequency and balance checks
remain explicit work items. Old milestone reports are historical evidence, not fresh passes.

Baseline `gradlew.bat build --console=plain`: **passed** on 2026-10-13. This compiles the main/client
code, runs JUnit and builds the JAR; it does not run the 341 client suites. The checkout was clean and
equal to `origin/main` before this work. GitHub authentication and fetching work.

| Inventory at baseline | Count | Meaning |
| --- | ---: | --- |
| Main Java files / lines | 1,221 / 244,425 | All files under `src/main/java` |
| Client Java files / lines | 366 / 56,171 | Rendering, screens, client integration |
| Unit-test source files | 346 | Includes helpers; not a test-case count |
| Game-test source files | 621 | Includes helpers; not a suite count |
| Registered client suite entrypoints | 341 | Source game-test descriptor |
| Structure definitions / structure sets | 68 / 68 | Includes 48 sites across six families |
| Custom biome definitions | **0** | No `data/wildercord/worldgen/biome` JSON files |
| Recipes / loot tables / advancements | 767 / 211 / 813 | Files in the mod's data namespace |

Counts are source inventories, not quality or completion scores. Existing ecosystem features and
biome selectors do not constitute registered custom biomes.

## What is already substantial

| Area and source anchors | Existing foundation | Audit conclusion / next verification |
| --- | --- | --- |
| `spell/`, `cast/`, `client/fx/` | Rune grammar, fusion, reaction, charge, effect lifecycle, receipts and presentation | Deep repertoire; finish lifecycle/readability coverage and supported combination checks before adding indiscriminate runes. |
| `aura/`, `aura/world/` | Sixteen schools, teachers, masters, named techniques, counters, progression, cooperative trials | Extend earned identity and recovery/counterplay. Keep Aura physical and spell circles distinct. |
| Boss classes in `cast/`; `world/dungeons/` | Archive, elemental dungeons, Root Guardian, Storm Conductor and newer regional encounters | Recent Root/Storm movesets exist on main. Do not duplicate them; regression-test them and build a different next boss. |
| `world/sites/{farm,masters,wilds,travel,water,mine}` | Eight sites per family, procedural construction, loot and discovery | Plenty of buildings; stronger terrain fitting, resident purpose, regional connections and return visits have higher value than empty copies. |
| `wildlife/`, `monster/` | Wetland, highland, ember and fungal systems; wildlife, predators, mounts and roaming threats | Connect ecology to exploration and optional investigations. Measure natural density and mixed-ecosystem cost. |
| `town/`, `runesmith/`, `guild/`, `party/`, `duel/` | Inns, rooms, bounties, raids, caravans, hamlet standing, crafting/trade and social systems | Give residents regional voices and useful services; exercise saved state and attribution with two clients. |
| `lore/`, `client/lore/`, `tools/lore_journal_text.py` | Owner-synced journal, six discovery leads, sixteen authored teacher/master voices | Confirmed integration drift: only three schools are mapped by `LoreJournal.schoolMethod`. Fix and migrate old records first. |
| `player/`, `gear/`, `content/`, `cooking/`, `ritual/` | Heart Paths, ascension, codex, equipment, recipes, meals, rituals | Build progression journeys that expose these systems at the right moment. Verify useful tradeoffs and avoid compulsory grind. |
| `net/`, `mixin/`, `api/`, `config/` | Bounded spell packets, server authority, add-on hooks, configuration | Existing guardrails must apply to every new NPC action, summon, reward and world edit. Review new boundaries per feature. |
| `world/upgrade/` | Planned world changes, journal, claims and recovery tooling | Additive content belongs in new chunks by default. Any old-world retrofit needs preview, bounds, claims and recovery evidence. |
| `travel/`, `backpack/`, `loadout/`, `pet/`, `familiar/` | Travel, storage, saved builds and companions | Verify inventory preservation, safe landings, ownership and cleanup as content is integrated. Preserve Cinnamon's existing identity and behavior. |
| `tools/`, `.github/workflows/`, `profiles/` | Generators, packaging audit, pinned profiles, full client shards and specialized native gates | Keep honest distinction between build pass, focused native pass and all-suite release evidence. |

## Confirmed gaps and risks

1. **P1 — Thirteen Masters lose their identity in journal victories.**
   `LoreJournal.schoolMethod` maps Ember/Gale/Stone and returns `school_<n>` for everything else.
   Both first-clear dialogue and victory recovery use it. Those IDs miss the sixteen authored voices.
   Repair the mapping, normalize old victory entries without replaying rewards, and test all schools.
2. **P1 — Custom biome request is unimplemented.** There are biome-targeted features and structures,
   but no mod biome definitions. A biome needs registry data, actual placement in new worlds,
   climate/terrain fit, surface/underground composition, spawn tags, resources and tests. A JSON file
   alone is not a completed biome.
3. **P2 — Lore is narrower than the world.** Six leads mostly cover introductory Aura and reactions.
   Expand them into optional regional investigations using real discoveries, with late-arrival recovery,
   persistent progress, one-time rewards and useful clues. Reuse the journal.
4. **P2 — Documentation drift hides existing content.** `LORE_CANON.md` calls six breaths future canon
   and lists three shipped Masters; source registers sixteen. `docs/design/0.13-roadmap.md` contains
   an old "38 commits unpushed" snapshot that no longer describes this clean main checkout. Preserve
   historical reports as dated reports, but bring current reference material into agreement with code.
5. **P2 — Site collision/footprint work remains.** The release checklist records cross-family overlaps
   and trial pits extending outside bounds. Reproduce on current source and natural terrain before
   changing generation. Test negative terrain cases as well as beautiful examples.
6. **P2 — Runtime coverage remains expensive and incomplete for this candidate.** There are 341
   registered suites plus separate native gates. A successful local build establishes no new claim
   about their current results, two-player behavior or visual quality.
7. **P2 — Older roadmap completion claims conflict.** `LIVING_WORLD_ROADMAP.md`, Masters roadmap,
   release reports and the 0.13 snapshot have different baselines. Use this ledger for new work and
   cite implementation plus evidence when reconciling them; never mark a target done from a name count.
8. **P0 — Asset generation loses recent boss guidance.** Running the generator exposed 145 missing
   warning/message keys and 12 changed values, including nine detailed bestiary entries, at the
   baseline. It also resurrected ten retired labels. The ordinary build had passed because it used
   committed resources. Preserve all 157 authored values in an owned generator module, remove the
   retired labels, and exercise generation parity plus the existing boss language assertions.
9. **P2 — Sleeping Blade discovery is gated behind drawing.** Its use callback consumes the
   interaction before the journal's observer runs; its Grimoire history previously arrived only
   when drawn or already claimed. Reading an authentic site's history should be accessible to an
   explorer without changing the ownership or draw rules. A copied stone must not grant history.

## Prioritized implementation list

Priority 0 protects saves and playability; priority 1 makes coherent playable content; priority 2
deepens it after its foundations work. Names below are **proposals**, not shipped canon or features.

| ID | Priority | Work | Concrete acceptance |
| --- | --- | --- | --- |
| POL-01 | 0 | Repair all sixteen Master journal identities and old placeholder records | Correct voice/name for every first clear; old saves normalize once; quests/rewards preserved; unit and native journal checks. |
| POL-02 | 0 | Reconcile current lore reference and player guide | All sixteen schools match source; historical claims labeled; no promises of absent content. |
| POL-03 | 0 | Structure bounds and terrain-fit sweep | Each piece's actual edits fit its declared footprint; entrances passable; no cross-chunk order artifacts on a fixed seed matrix. |
| POL-04 | 0 | Encounter restart/abandonment matrix | Bosses, NPC escorts, temporary arenas, rewards and hazards survive or retire correctly on death, logout, dimension change, unload and restart. |
| POL-05 | 0 | First-hour progression route | A fresh survival character can find a Cord, learn casting or Aura, discover shelter, read a lead and earn a useful reward without commands. |
| POL-06 | 1 | Boss readability and accessibility pass | Distinct motion/audio/ground tells and recovery; reduced particles, safe colors, first/third person and ordinary terrain checked. |
| POL-07 | 1 | Mixed-world performance route | Profile travel through crowded settlements, wetlands and combat; bounded searches/spawns and no retained world/player references after exit. |
| POL-08 | 1 | Acquisition and reward audit | Every new resource has an obtainable source and useful sink; finite reward claims; no gear-invalidating power escalation. |
| BIO-01 | 1 | **Luminous Reedlands** custom biome | Water shelves, dry paths, reed islands, existing newts/crabs/bitterns, moonreed and survey shelters; native registry/placement and seed sampling. |
| BIO-02 | 1 | **Windscar Highlands** custom biome | Walkable saddles, exposed spires, stonehorn/galeclaw habitat, lee-side shelter and readable travel routes; no unavoidable fall traps. |
| BIO-03 | 1 | **Emberwood Reach** custom biome | Ash pockets, living fire-adapted woodland, cinderfox routes and charcoal work sites; no uncontrolled fire spread. |
| BIO-04 | 1 | **Belowkeeper Hollows** underground biome | Fungal chambers, wet/dry shelves, nursery/drainhouse connections, restrained luminous palette and safe entrances. |
| BIO-05 | 2 | Biome transitions and rare variants | Deliberate edges, climate compatibility, regional spawn/structure tags and rarity measurements across multiple seeds. |
| BOS-01 | 1 | **The Hollow Bell**, Belowkeeper encounter | A suspended root-and-bronze bell body; pressure channels and audible numbered tolls; interruptible resonance and generous exposed intervals; own model, arena and loot. |
| BOS-02 | 1 | **The Reed-Crowned Ferryman**, Tideward encounter | A reed raft guardian that rearranges safe crossings; anchor lines, rising water tells and rescuable routes; ground fighters always retain counterplay. |
| BOS-03 | 2 | **The Glass Cartographer**, highland encounter | A lens-bearing survey construct that projects paths before shattering them; break sightlines, turn reflectors and exploit a stationary calibration recovery. |
| BOS-04 | 2 | **The Kiln Widow**, Emberwood encounter | A many-legged ceramic kiln guardian; vent management, cooling windows and visible clay cracks; no unavoidable heat damage or recolored existing moveset. |
| BOS-05 | 1 | Existing boss regression tour | Exercise Root/Storm's new moves, phase transitions, multiplayer targets and arena exit; verify reward attribution and reusable relics. |
| BLD-01 | 1 | **Tideward Survey House** | Stilted map room, dry public shelter, Iona/Tamsin clues and a small repair service; four approach directions and flood-safe entrance. |
| BLD-02 | 1 | **Belowkeeper Listening Station** | Pressure gauges, readable bell inscriptions, working route to fungal sites and safe camp; introduces the Hollow Bell counter before its fight. |
| BLD-03 | 1 | **Highland Cairn Lodge** | Windward/lee architecture, route maps, traveler beds and a scout's wall; use existing inn/room behavior where suitable. |
| BLD-04 | 2 | **Emberwood Charcoal Yard** | Cooled kiln, wood storage, firebreaks and a useful material trade; environmental story connects fire-adapted wildlife and the Kiln Widow. |
| BLD-05 | 2 | Existing site return visits | Repairs, rotating regional supplies and optional research hand-ins; no mandatory daily chores or endless reward farms. |
| NPC-01 | 1 | Make **Iona and Tamsin** discoverable surveyors | Existing canon, distinct voices, clue-aware conversation, safe schedule, persistent identity and protected one-time rewards. |
| NPC-02 | 1 | **Mara's apprentice**, Belowkeeper maintenance NPC | An explicitly new person continuing Mara's work; teaches pressure/bell observations, sells practical supplies and responds to investigation progress. |
| NPC-03 | 2 | **Cairn keeper**, highland route NPC | Reads nearby routes, reacts to a found scout inscription and offers rescue supplies; no omniscient spoilers. |
| NPC-04 | 2 | Regional inn and caravan voices | Lines keyed to discovered regions/rumors and current threats; finite cooldown, owner-only journal recording, no repeated chat spam. |
| NPC-05 | 1 | Resident persistence and trade lifecycle | Save/reload, death/replacement policy, unreachable beds, raids, trading during departure and two-player interactions tested. |
| LOR-01 | 1 | **The Drowned Road** investigation | Survey clue → safe crossing observation → drowned-site evidence → optional Ferryman discovery; every stage visible and replay-safe. |
| LOR-02 | 1 | **What the Empty Bell Kept** investigation | Nursery observations → listening station → pressure records → Hollow Bell; distinguish maintenance failure from invented divine lore. |
| LOR-03 | 1 | **The Scout's Last Cairn** investigation | Existing memorial → highland lodge → three authored route clues → optional cartographer encounter; reward knowledge and a travel utility. |
| LOR-04 | 2 | **Eleven Winters at the Gate** investigation | Hessa's established kiln history → charcoal-yard evidence → aftermath sites → Kiln Widow; preserve the existing Ember teacher's account. |
| LOR-05 | 1 | Discoverable regional legends | Short inscriptions, damaged maps and eyewitness versions; concrete exploration clues, at least two delivery routes, no required wiki. |
| LOR-06 | 2 | Journal organization at scale | Region/people cross-links, readable completed investigations and stable entry IDs; keyboard/small-window review and localization completeness. |
| SYS-01 | 0 | Respect existing worlds | New generation only by default; no automatic terrain rewrite; explicit upgrade plan for supported additions and test old-save load. |
| SYS-02 | 1 | Regional tools and building materials | Survey markers, acoustic shutters, cooled kiln bricks, cairn lamps; each has acquisition, a purpose, recipe, art and documented limits. |
| SYS-03 | 1 | Optional expedition preparation | Region meals, shelter supplies and flexible counters that support both magic and Aura; no single compulsory rune or build. |
| SYS-04 | 2 | Regional ambience | Bounded ambient sounds/particles, weather/day differences and environmental storytelling; obey effect/sound accessibility settings. |
| QA-01 | 0 | Repeatable evidence and GitHub checkpoints | Small coherent commits, generated assets, unit/build checks, focused native evidence and pushes; draft PR preserves full CI status. |
| QA-02 | 1 | Cooperative content tour | Two clients discover/complete a lead, enter/leave a boss and trade with an NPC; no stolen credit, duplicate payment or leaked private progress. |
| QA-03 | 1 | Packaging and release candidate checks | Generated asset parity, distributable packaging, all required client gates and exact candidate SHA recorded; alpha publication is separate from saving code. |
| POL-09 | 0 | Preserve current boss text in the generator | All 157 authored values preserved, ten retired names excluded, repeat generation stable, boss language tests pass. |

## Delivery sequence

1. Save this audit; repair confirmed journal drift and verify recovery from older saves.
2. Add a small connected investigation through existing sites to establish reusable, tested quest progression.
3. Build one complete regional slice: Reedlands biome, survey house, surveyors, Drowned Road clues,
   regional utility and Ferryman. Land independently useful pieces with their own evidence.
4. Repeat the proven integration pattern for Hollows/Hollow Bell, Highlands/Cartographer and
   Emberwood/Kiln Widow. Each encounter has distinct mechanics, silhouettes and rewards.
5. Run the progression, old-world, cooperative, generation and performance tours; reconcile the
   living-world and Masters completion ledgers against actual source and fresh evidence.

No milestone is complete just because its names, JSON, model, rules or test fixture exist. A boss
needs ordinary gameplay admission, tells, counters, persistence, assets, rewards and native checks;
a biome must actually generate; a legend must be discoverable and lead somewhere useful.

## Checkpoint ledger

| Checkpoint | Status | Evidence / remaining work |
| --- | --- | --- |
| Baseline structural/content audit | Complete at stated scope | Inventory above; source paths; baseline build passed. Full runtime audit remains open. |
| POL-01: sixteen-school journal | Implemented and locally verified | Stable IDs, old-save normalization, four regression tests; 2,705 unit tests pass; focused native journal suite passes (32s launch, ~12s scene); rendered People tab inspected. |
| POL-02: current lore reference | Reference corrected | Sixteen breaths/Masters and migration described in `LORE_CANON.md`; old 0.13 Git status labeled historical. Wider player-guide audit remains open. |
| LOR-03 foundation: Scout's Last Cairn | Implemented and locally verified | Three connected chapters after Scout's Wall; two new book pages; ordinary tower/ring discovery and blade-stone interaction exercised by `ScoutLegendChecks` inside the registered journal suite. The proposed highland biome/lodge/Cartographer extension remains unbuilt. |
| LOR-01 foundation: Drowned Road | Implemented and locally verified | Three ordered chapters, Reedkeeper account, boathouse ledger and working lighthouse action. Real conversation/use packets, dark/copied/unrelated/spectator negatives, exact rewards, owner UI and save/reopen pass. The separate optional Ferryman encounter is now implemented; a further drowned-site investigation remains proposed. |
| POL-09: generator preserves boss guidance | Implemented and locally verified | All 157 existing authored values preserved; ten obsolete labels suppressed; 8,987 generated paths stable; two Python regression checks added. Existing CI runs generation parity and boss JUnit assertions. |
| Sleeping Blade history access | Implemented and locally verified | Native journal test verifies authentic-only discovery, one book per visitor and no ownership/draw from reading. Full existing focused `SleepingBladeTest` also passes, including generation, interrupted draws, unique ownership and guard tradeoffs. |
| BIO-01 foundation: Luminous Reedlands | Implemented and locally verified | Actual climate placement, surface/spawn/structure tags, natural Moonreed, owner-only journal discovery and save/reopen. Fixed 49-chunk census and existing wetland checks pass. Broader frequency and fauna-density measurements remain open. |
| BLD-01 / NPC foundation: Reedkeeper Landing | Implemented and locally verified | New house, ordinary fisher trades, four saved conversations, attributed First Crossing book, functional garden and discovery advancement. Four facings, real locator-selected terrain, chunk-order replay and resident save/reopen pass. Named Iona/Tamsin NPCs and the proposed repair service remain unbuilt. |
| POL-04: Master abandonment and returned Aura | Implemented and locally verified | Paid attacks retire when the last challenger leaves; severed Aura can return to its original Master within its payment allowance. Full native trial and Stone March checks pass; wider encounter lifecycle audit remains open. |
| Player-guide fidelity | Expanded and checked | All 417 Master technique timings and all 155 visible advancements/reward totals checked against runtime data; fractional Heart bonuses and ward caps corrected. 190 guide pages and 8,995 generated paths validated at the landing checkpoint. |
| Native matrix repairs | Ongoing | Focused high-stage Aura and two-scenario Mana Skin checks pass; six retained fixture references cleared. Later Excise disconnect, Siltcrest shelter, connected counter receipt and long-run save/heap failures remain under investigation. |
| Full development goal | Active | Remaining backlog above; not a release-readiness claim. |
| Reed-Crowned Ferryman / Last Crossing | Implemented and locally verified | Distinct raft, reed crown, anchor, oar and emissive lantern; authentic opt-in bell, three marked attacks, half-health pattern, finite abandonment, participant compass/XP, replay-safe identity and saved completion. Full native encounter and registered locator on real Reedlands terrain pass. Cooperative client tour and random placement frequency remain open. |

### POL-01 verification

- `gradlew.bat build compileGametestJava --console=plain --offline`: passed; 2,705 JUnit tests,
  zero failures, errors or skips.
- `gradlew.bat runClientGameTest "-PfocusedSuite=dev.wildercord.lore.LoreJournalGameTest" "-Pwildercord.eulaAccepted=true" --offline --console=plain`:
  passed. Reused the existing accepted local game-test EULA. Checked all sixteen first-clear hooks,
  owner synchronization, translated People text, old Tide victory recovery and no repeat reward.
- Screenshot `lore_journal_sixteen_masters.png` inspected: readable names and scrolling list;
  all sixteen rows are asserted in native screen content. Evidence copied under the ignored
  `artifacts/review/world-depth/master-journal/` before another native run can replace it.
- This focused run calls the first-clear journal hook; it does not claim to defeat all sixteen
  Masters or certify their combat. Full client CI and release gates remain separate.

### Scout legend and generator verification

- Three additional quest-state tests cover ordered prerequisites, no same-poll cascade, earlier
  discoveries, interrupted-state recovery and a valid acyclic chapter graph. The existing text
  tests cover the new quest lines and both book pages. All **2,708 JUnit tests pass**.
- Expanded native `LoreJournalGameTest`: passed in 52 seconds including startup (~25-second
  scene). Two real structure pieces are built chunk by chunk, their authored books checked and
  starts registered; the normal structure observer discovers them. The final step invokes the
  ordinary block interaction chain, rejects an unauthentic stone, gives the Last Oath exactly once,
  and confirms that reading neither starts a draw nor claims the blade. Completed journal text and
  the screenshot `lore_scout_last_cairn.png` were inspected.
- The native fixture supplies the memorial discovery and teacher conversation through their
  production hooks. It does not claim a full unaided survival playthrough or naturally seeded site
  distribution. Tower/ring books already saved in older chunks are not rewritten; their discovery
  entries still advance the story, and earlier blade discoveries also count.
- `python tools/check_generated_assets.py`: **8,987 paths pass**, including pixel equality for PNGs
  and byte equality for other files. A semantic comparison with the prior commit confirms no
  existing language value changed or disappeared; only the 17 new story entries were added.
- `python -m unittest discover -s tools -p test_boss_encounter_text.py -v`: both checks pass. The
  first repair run also exposed the generator restoring the retired Lantern Bearer `slam` label;
  the existing boss test caught it. The final generator now excludes all ten retired labels.
- `python tools/player_docs.py --check`: 189 pages pass. Exporting also brought the existing
  wiki boss descriptions into the previously stale GitBook export, including the lair-boss page.
- Three pinned launcher profiles built from the local JAR, and `tools/audit_packages.py` passed
  with explicit JAR/profile paths. The report under `artifacts/review/world-depth/` records local
  checkout provenance; this is packaging evidence, not a published release or all-client pass.
- Existing focused `SleepingBladeTest`: **passed** in 1m 22s (~55-second scene). Its first run
  revealed that the fixture equipped an iron sword over the newly delivered history book. The
  fixture now asserts early discovery and stores that book in a free inventory slot before
  equipping the test weapon. The original exact-one-book/blade, draw-interruption, serialized
  ownership, later-visitor, guard, book-screen and natural-generation checks remain intact.
- Packaging audit's own 44 Python tests also pass. No claim is made yet for all 341 registered
  client suites or the separate multiplayer gates on this candidate.
- GitHub's available push credential rejected a proposed workflow edit for lack of workflow
  scope. The unpushed commit was amended to retain the existing workflow. Its asset-parity check
  and boss JUnit tests already cover generator drift; the two additional Python checks are kept
  and were run locally. No credential or repository permission was changed.

### Next work

Keep the full goal active. Check the draft PR's CI outcomes, then implement the first actual custom
biome with registry/placement and seed evidence. Prefer one complete regional slice over adding four
biome names without generation. New bosses, new resident NPCs, and new buildings from the proposal
table remain open. Preserve the existing boss movesets and the newly repaired generator on every pass.

### CI environment follow-up

The first full checkpoint (`6dc32560`, run `38401206245`) failed its generated-file step before
comparison: `tools/serpent_art.py` imports NumPy, while CI installed only Pillow. The existing local
NumPy installation masked that baseline provisioning defect. `tools/requirements.txt` now pins the
verified local versions (Pillow 12.2.0, NumPy 2.4.6); the build and three native image-verification
installation steps use that same requirements file. This is a dependency correction, not permission
to ignore asset differences. The new run must still pass pixel/byte parity and all relevant gates.

### Native CI follow-up: current rules and remaining failures

GitHub run `38401906989` at `08f51410` passed its build, generated-asset parity and
packaging job after the dependency correction. It did **not** pass the whole native
matrix. Failures include ward expectations, the burst-survival fixture, Stone March
recovery, Master opening-cost assumptions, connected Resistance prevention, and one
player-save preflight. Each remains a separate finding until its relevant run passes.

- The Void Ward failure reproduced locally: the fixture expected Resistance IV on a
  player, while the existing `DefenceCaps` mixin limits lasting grants to Resistance II.
  Corrected the fixture and Brace/Shulkershell descriptions without changing combat
  balance. The focused ward test now also applies a real ten-point drowning hit,
  bypassing prior Anchor armour, and verifies six health lost. Native pass: 40 seconds.
- The Physical Magic burst fixture used a default cheap cast, whose current paid
  damage allowance runs out before it can reach the death guard. An explicit synthetic
  30-mana allowance lets all eight four-point hits enter the same cast. The test now
  asserts all eight land and the same player survives on two health. The full focused
  Physical Magic test passed in 56 seconds, retaining its terrain and crafting checks.
- The complete focused Relay suite passed locally in 2m 10s, including paused and
  unpaused save/reopen and socket reconnection. The CI save warning hides its exception;
  a test-only observer now exposes that cause without changing save behavior. No
  cross-platform save fix is claimed. Three pure-JDK/source-boundary tests pass after
  explicitly reading UTF-8 on Windows.
- Unit results remain 2,708 passed. Regenerated resources retain parity across all
  8,987 paths; the 189-page guide export passes. Other native repairs are in progress.
- Running the wider wiki generator revealed another open audit item: its Master
  technique timing model has hard-coded recovery values and omits the current opening
  floor. The unrelated generated technique-table changes were not included in this
  checkpoint; that model needs comparison against the actual Java techniques before
  its output can be trusted.

The first new-biome slice is paused behind these native regressions. Minecraft 26.3
uses data-driven material rules; old surface-rule integration examples are unsuitable.
The Reedlands proposal still requires actual registry, climate-placement, surface and
seed evidence, and remains explicitly unimplemented.

### Published Master timing repair

The Master technique generator now reads the current opening minimum and recovery
formula, and uses Java-compatible half-up rounding. Both published guides have been
regenerated for all 417 techniques. A JUnit check compares every named row in both
outputs with the runtime impact and recovery values, rather than trusting the
Python calculation. All 2,709 unit tests pass; the 189-page guide export also passes.
No attack timing or damage balance changed in this documentation repair.

Further native diagnosis found an actual Master cancellation bug: a last enrolled
fighter leaving the arena bypasses attack ticking and leaves the windup set. The
local repair passed all 40 Stone March damage/cancellation cases and its eight-player
trial. The final admission fixture needs natural Aura replenishment before it can
isolate rest as its only admission gate; the complete rerun is still pending.

The save observer also exposed the prior opaque failure: a ConcurrentModificationException
in Fabric's attachment map collection while saving the player during disconnect.
This is not a missing rest codec. Off-thread mutation diagnosis is in progress;
no save fix or full native pass is claimed.

The test build now rejects server-player attachment writes from other threads at
their source, before mutation, to identify any writer racing the save. This hook is
absent from the shipped mod. Focused WatchweftPaidTest passed with the diagnostic
loaded, including actual paid casting, a vanilla hostile approaching, and exact
saved-rest retention after reopening. The intermittent CI fault did not reproduce
in this standalone Windows run. A full CI run with the writer diagnostic is pending.

### Native fixture checkpoint: progression and presentation

Four focused native suites now pass after correcting stale fixture assumptions:

- `MasterAntiAirTest`: 39 seconds. Exact payments exclude the existing multiplayer
  trickle; cancellation checks the current paid recovery boundary, elevation lock
  and cover rather than an obsolete longer Crescent timeline.
- `AuraProgressionTest`: 1m 4s. Grants and boundaries use the registered thresholds;
  the practice cap, real waterfall stillness, owner sync and save/reopen remain checked.
- `ReweavePlayableTest`: 1m 12s. Refused retrieval displays its existing refusal UI
  with no study, page or session nonce. Expiry, dimension and cracked-circle cases
  still require the lesson to close. A read-only screen accessor exposes refusal.
- `MasterModelPresentationTest`: 1m 11s. Gale and Stone body captures retain their
  actual attack-frame assertions and derive payments from current costs.

These changes do not tune combat or progression. The complete local Stone March
rerun also passed in 7m 39s, including all 40 scenarios, the eight-player trial and
admission priority after real breathing replenished its spent Aura. Its gameplay
repair and the remaining trial/receipt fixtures are still being reviewed for a
separate checkpoint. The full CI matrix remains incomplete and is not green.

### Heart progression and stillness checkpoint

- `WildercordHeartCirclesTest` passed in 41 seconds after its fixtures were aligned
  with the existing 1.5% per-circle power and lasting player Resistance cap. Its
  Mana Skin probes separately verify the short dodge exception and capped lasting
  Resistance, including actual health lost and mana paid. The suite retains all
  twenty circles, cracked circles, owner sync and serialized save/load checks.
  The native UI check reads the translated tooltip and locates its responsive badge;
  inspected captures show 28.5% at nineteen circles, 30% at twenty and 15% Overflow.
- `WildercordAuraTest` passed in 2m 4s. The prior stillness failure was a fixture
  interaction: earlier spell probes had readied a Heart Circle; it formed during
  the crouch, then the innate-awakening screen released the key. The isolated Aura
  stillness scenario now starts without condensed mana for that unrelated circle.
  Its full crouch duration, actual place of power and earned Flow remain required.
- Player-facing drift was also present: circle chat rounded 1.5% to 2%, the guide's
  table doubled every power bonus, and Overflow text promised 30% instead of its
  existing 15%. The shared readout now preserves fractional values. Both guide
  tables and the two Overflow language entries are checked against the Java values.
- Full local build and native-source compilation pass. The unit suite passes with
  2,713 tests, including three local climate-partition tests for the unregistered
  Reedlands foundation. Asset parity retains all 8,987 paths and guide validation
  retains all 189 pages. Reedlands registration and natural terrain proof remain
  work in progress; it is not yet an available biome at this checkpoint.

The long Master trial rerun passed the returned-bolt, abandonment, lifecycle, Ember,
pursuit and Gale Reprise sections, then found a Stone Fracture fixture using magic
as a substitute for a physical hit. Magic immunity correctly rejected that probe.
Its isolated receipt now uses player-attack damage with evasion spent and temporary
knockback resistance; the separate normal-knockback case keeps the real resistance.
The complete trial rerun is still required before claiming the suite passes.

CI run `38407882192` completed with failures. Its first shard passed Watchweft's
paid/save scenario with the attachment diagnostic loaded, then exhausted its
approximately 4 GiB Java heap during chunk serialization in `LifeOutcomeEdgesTest`
after roughly forty minutes. No off-thread attachment writer was reported. This
does not resolve the intermittent attachment save race; the memory failure is a
separate open investigation, and increasing the heap alone is not a demonstrated fix.


### Verified Master trial repairs

`SwordMasterTrialTest` now passes in full (10m 5s). Its real damage paths cover
consenting enrollment and bystanders, spellbane, returned severed Aura, abandonment,
lifecycle, Ember Afterburn, pursuit, Gale Reprise, Stone Fracture, Master wound
receipts and Break Cast receipt consistency. The earlier full Stone March suite
also passed (7m 39s). The returned bolt retains its original cast allowance and
cannot recover the original spell payload. An empty arena cancels paid actions
without refunding them, then removes the abandoned Master after the existing delay.

The fixtures isolate existing multiplayer Aura trickle where exact payment matters,
wait for real guard recovery and natural breathing, and grant the actual short
Resistance dodge exception only at the tested hit. Lasting Resistance and ordinary
spell immunity retain their current caps. Connected-client receipt coverage still
requires its separate CI run; the full native matrix is not claimed green.

### First playable custom biome: Luminous Reedlands

The first regional biome is now registered in the ordinary Overworld preset.
It partitions the humid end of existing swamp climate entries without replacing
noise settings. Its swamp surface rules retain shallow pools; authored water,
grass and foliage colours distinguish the banks. Existing wetland fauna, Moonreed
pollination and the Stilt Smokehouse connect it to playable systems immediately.
The automatically discovered journal note introduces the Tideward's first-crossing
legend and survives owner synchronization and a real save/reopen.

The fixed native survey uses seed `-7620530482425397421`, a bounded noise-only
search and exactly 49 generated chunks around `(-2768, 64, 3296)`. Before regional
colonies were added, its 9,394 regional columns contained water but zero Moonreed.
The implementation now supplements an admitted regional patch with at most four
plants found in 64 bounded source-chunk columns. It retains every habitat gate,
does not replace occupied blocks and consumes no extra random draws. The same
survey then found six naturally generated plants. The native test does not place
plants or force biome cells; its final run passed in 43 seconds, including actual
entry discovery, owner sync, saved biome identity and the saved natural plant.
The inspected daylight capture is included in the player guide. Fauna registration
is asserted, but this capture does not prove naturally spawned creature density.

The existing placement-contract and positive-terrain suites passed in 31 and 32
seconds. The strict 81-chunk absence fixture initially differed at one of 120
attempts: `(-1552, 67, -794)` was occupied rather than empty dry ground. A control
without the regional mixins passed, and two subsequent unchanged region-enabled
runs also matched all 120 original receipts (51 and 31 seconds). The cause of the
intermittent occupied candidate is unresolved. The expected receipt, seed, search
area and zero-reed requirement remain unchanged. Bounded test-only diagnostics now
record occupied block identities and the complete decoration feature order.

The full native roster deliberately grows to 342 classes, split 86/86/86/84.
Reedlands joins the existing wetland group; every prior class retains its group
and relative order. Fifteen explicit-plan adversarial tests and fourteen selector/
Gradle-contract tests pass on Windows. POSIX runtime evidence validation remains
Linux-only and is not claimed as a local pass. The guide exports and checks 190
pages; generated assets retain exact non-image bytes and identical PNG pixels
across all 8,990 paths. The remaining three regional biomes, new bosses and new
resident settlements are still proposed work.

The final local build and native-source compilation passed in 1m 3s. All 2,714
JUnit tests passed with zero failures, errors or skips. Performance, balanced and
cinematic packs were rebuilt from the explicit current JAR; their packaging audit
passed, as did all 44 packaging-audit unit tests. This is not release approval or
evidence that the unresolved full native matrix passes.

### Continuing CI findings

Run `38415643853` passes the build and both March parts. Its third shard reaches
`WildercordAuraMasteryTest`, then fails a carried-spell health expectation (1.12
health remained) and a thunderstorm/ley-crossing breakthrough (stage 3, no stillness).
These require investigation against current rules. The connected matrix reports
a counter item-draw hand/camera/hilt-basis mismatch; all four Moon views pass.
Neither result establishes the complete connected receipt contract.

Its Masters core also exposed the same obsolete lesson-screen assumption already
corrected for Reweave. Excise's two refused retrieval checks now require the visible
refusal with no active study, nonce or advanced page; existing expiry, dimension
and cracked-circle checks still require closure. The local dedicated-server rerun
stopped before gameplay because the local EULA opt-in was absent. Verification of
this change remains assigned to the existing owner-approved CI server setup.

### Tideward landing, resident and first-crossing legend

The Reedlands now has an authored regional building: a raised survey house with
mud-brick walls, a dark prismarine roof, covered workbench, seating, fishing chest,
bed and open Moonreed garden. Its villager resident is a persistent apprentice
fisher with ordinary trades. First contact records the Reedkeeper under People;
sneak-use reveals four journal conversations, with a per-player pause between
lines. The four-page attributed book connects Iona's existing wetland survey to
the Tideward flood crossing and the Reed-Crowned Ferryman legend. This introduces
the Ferryman as lore; it does not add the proposed boss encounter.

The registered locator admits a real site in the existing fixed 49-chunk census:
seed `-7620530482425397421`, chunk `[-176, 204]`, bounds
`(-2816, 56, 3264)` through `(-2798, 73, 3281)`. The inspected guide screenshot
places that authored piece on its locator-selected terrain after the untouched
biome survey. It is not evidence of random-spread frequency. The original terrain
census still finds exactly six naturally decorated Moonreeds before site placement.

The complete extended `ReedlandBiomeTest` passes in 1m 30s. Four separately clipped
orientations preserve entrances, real soil/water/sky for all garden buds, the
four-page book and fishing trades. Replaying every chunk through a serialized and
restored piece keeps exactly one resident. Actual client interaction packets
verify every conversation, the cooldown, translated saved speaker and merchant
screen. A real save/reopen preserves the resident UUID and heard stories. Earlier
fixture failures came from inspecting distant, non-ticking/unloaded entities;
the four bounded sites now reach entity-ticking status before inspection and stay
loaded during the conversation checks. No resident-count assertion was removed.

The player guide now describes 49 sites. The advancement page was also missing
nine Aura and three cooking entries. With the landing added, its actual displayed
roster is 155: 109 tasks, 32 goals, 14 challenges, four hidden entries, 14,630 XP,
48 Blank Runes and 29 Mana Crystals. A new source-to-guide check verifies every
named row and these totals in both editions. All 8,995 generated asset paths retain
parity; the guide still exports 190 pages.

The first Reedlands CI run found a remaining test anchor that treated the current
full roster as the old frozen 339-class roster. It now excludes only the explicit
later Reedlands addition while retaining the original 339-class hash and every
stale-evidence rejection. The portable anchor and selector checks pass (29 tests).
The POSIX evidence-reader portion remains a Linux CI check, not a Windows pass.
Other new CI diagnostics remain open: Siltcrest daytime shelter navigation and
Mana Skin production mana regeneration. They are not attributed to the new biome
without a reproducer, and no broad native acceptance is claimed.

The repository-wide test run caught a duplicate placement salt on the new landing.
Its corrected salt, `2026101509`, is unique among all 69 registered structure sets.
The complete build and native-source compilation then passed in 1m 21s, with all
2,715 JUnit tests passing and no failures, errors or skips. The final native
Reedlands/landing rerun passed in 1m 10s after that correction. Existing placement
counts, strict natural-plant observations and resident persistence assertions
remain intact.

The final asset comparison passes across all 8,995 paths, and the 190-page guide
passes export checks. Performance, balanced and cinematic profiles built from the
explicit current JAR pass the packaging audit. This checkpoint is ready for
development review; the unresolved full native matrix still prevents a release
readiness claim.

### Mana Skin sustain benchmark correction

The CI failure reproduced in a fresh local world. Production threat-20 tempering
doubles the Gale Master's opening crescent from 36 to 72 before native defences.
The properly equipped stationary player dies on tick 25, so Mana Skin has no
nonlethal wound to rebate and the original full mana pool never has room to
regenerate. This is not a failure of production mana regeneration.

The original 600-mana reference remains intact. A second, independent 300-mana
scenario now supplies available capacity and must observe real regeneration,
under the same strict cadence, native clock and source checks. Both scenarios
pass together in 44 seconds. The second gains 22.5 mana in five production ticks
before the same lethal first hit. No damage, armour, tempering, health, Mana Skin
or regeneration rules changed. The detailed review in
`docs/reviews/MANA_SKIN_RESOLVED_DAMAGE.md` now distinguishes the original review
environment from this fresh native evidence. Full CI confirmation remains pending.

### High-stage Aura and test-world retention

The complete `WildercordAuraMasteryTest` passes in 2m 44s. Its carried-spell probe
now explicitly funds a 60-mana damage allowance through `Cast.damagePrice`, keeping
the original two-rune spell and lethal spellguard assertions. Changing the rune
count metadata did not fund that allowance; an intermediate rerun caught that
mistake. The tempest setup clears mana condensed by earlier spellblade cases,
preventing an unrelated first-Circle awakening screen from releasing the held
meditation input. The clear-sky negative and full thunderstorm breakthrough remain
unchanged. No progression, damage or spellguard rule changed.

Source review also found six cached native-test instances retaining entities from
closed worlds: Rootmolt ecology, Drainhouse acquisition, Belowkeeper equipment,
Dewglass spectacles, Siltcrest presentation and Relay defence. Their outer cleanup
now clears those specific fields/arrays on success and failure. The Rootmolt
ecology suite passes in 49 seconds with its actual competing consumers, finite meal
deadline and full restart. Dewglass spectacles passes in 29 seconds, including
its owner-only observation and restart checks. This removes concrete retention paths; it does not
establish the cause or resolution of the earlier four-GiB CI heap failure.

GitHub run `38424793282` passes build and March mechanics and now reaches the
Excise gameplay checks beyond the repaired roster preflight and refusal screen.
Its later active-disconnect assertion fails. That check retains its exact clock,
body-replacement and recovery-deadline requirements and now reports each observed
value plus the old body's pending state. A gameplay fix is not claimed without
those diagnostics; dedicated-server verification still uses the owner-approved CI
environment.

The focused Siltcrest presentation rerun passes in 1m 6s at both Full and Minimal
settings, retaining its original hunt, phase, shelter-navigation and pixel checks.
The 420-tick shelter window is unchanged. A future failure now also reports the
actual cover predicate, shelter clock, running goals and existing navigation path;
it does not request diagnostic paths or manipulate the bird. This local pass does
not resolve the intermittent CI shelter failure.

### The Drowned Road: a playable coastal continuation

The Reedkeeper's conversations now open three ordered chapters: **The Drowned
Road**, **The Second Count** and **A Light for the Last Boat**. The player hears
the complete Ferryman account, visits a coastal pier boathouse and stands beside
the working lamp in a lighthouse. Newly generated boathouses hold a three-page
passenger ledger beside their slip; a fourth Keeper's Log page connects the lamp
to the second passenger count. The existing lever supplies the final action.
An already tended lamp counts for another visitor, and earlier discoveries are
retained without skipping chapter announcements. Rewards total 90 vanilla XP,
five Blank Runes and one Aura Shard per player, with no combat or Aura gate.

All nine water sites now record their actual structure visits under Places. One
structure lookup in the existing journal poll supplies these discoveries and
checks the authored lighthouse lamp within six blocks. It performs no block-volume
search or terrain edits. Copied/unregistered buildings, unrelated lamps, darkness
and spectator visits do not count. Older registered structures remain eligible;
their furnishings and books are not overwritten.

The expanded native `ReedlandBiomeTest` passes in **1m 31s**, including its original
fixed natural-terrain census, four landing orientations, real resident interaction
packets, an east-facing boathouse, a west-facing lighthouse, ordinary journal polls
and a real client lever packet. It verifies all four negative lamp cases, exact
item rewards, final-chapter XP, translated owner UI, repeated-poll idempotence and
actual save/reopen without repayment. The first run failed a new test precondition:
placing its unrelated power source before the test lamp did not send the needed
neighbour update. Reversing those two fixture placements powers the negative
control; no production condition or assertion was relaxed. The helper adds no
entrypoint and changes no roster hash. This remains authored-piece integration,
not an unaided survival route or a natural structure-frequency measurement.

The build and all **2,716 JUnit tests** pass, with zero failures, errors or skips.
The unit addition checks earlier coastal visits, the specific final conversation,
ordered announcements and one-time completion. Both player-guide editions and
the lore canon describe the route and old-world behavior.

Current CI also narrows the open Excise disconnect issue: at frozen clock 704,
the old body has recovery deadline 716 and no pending hold, while the new body
has no saved recovery deadline. Cancellation computes the expected deadline;
save/lifecycle ordering requires further investigation. This is not a passing
dedicated-server result or a claimed fix.

### Existing water sites and naturally tempered knights

The existing `WaterSitesGameTest` passes in **1m 10s** after the coastal story
changes, including the original structure locators, doors, walkable pier, slip,
books and loot. All three profile packs built from the explicit current JAR pass
the packaging audit, and generated parity remains 8,995 paths.

The next third-shard failure was `WildercordAuraWorldTest`'s Fallen Knight case.
It compared final health to rank base health even though the real ENTITY_LOAD
handler tempers the spawned knight. A native diagnostic measured rank 1, base
40, threat 9 and final health 76. The subsequent slash measured 7.8 damage,
which is the normal rank-two 6 damage at threat 6's 1.3 multiplier. The fixture
now verifies rank base, expected threat, the exact scaled maximum including any
elite modifier, and the exact scaled slash. The rest of its encounter checks,
including telegraph, dodge and reflected crescent, remain in place.

An unchanged local run also showed the random sixteen spawn attempts can all
miss this small artificial room. The spawn call now receives a fixed seed of 1;
its original production attempt count, radius, darkness, distance and haunt
predicates are unchanged. The complete corrected Aura World suite passes in
**1m 57s**. No knight health, damage, spawn rate or production RNG changed.

Excise now has bounded test-only tracing immediately before player serialization
and before/after retirement, including body identity, thread and authoritative
deadlines. This adds evidence without changing persistence or cancelling a hold.
The diagnostics load successfully in the native client; the actual dedicated
disconnect sequence still requires the existing CI environment.

The latest Siltcrest failure is a separate, earlier observation limit: Full
quality completes its natural hunt, while Minimal quality records three lawful
escapes at ticks 67, 748 and 980, then reaches its original 1,100-tick allowance.
Each escape has an out-of-range committed quarry and retains the production
cooldown. The full receipts remain in CI. No larger observation window, forced
quarry, reset appetite or creature behavior change is being substituted for this
intermittent presentation result.

## Excise disconnect serialization evidence and repair

The `91919e2` GitHub run's core job (`115414318700`) records the real failure at
the serialization boundary: body `2141889297`, overworld tick **694**, has
`pending=true`, `recovery=0`, `rest=934` at `save_begin`. After reconnect, the old
body has recovery **706**, while both the newly loaded attachment and the saved
value remain **0**. Earlier explicit retirement receipts run on the server thread.
This establishes that this transport's removal saved the paid hold before its
cancellation deadline was written; it does not establish which networking callback
failed to deliver retirement early enough.

`ExcisePlayerRemovalMixin` now retires the body's held Excise at the head of
`PlayerList.remove`, immediately before vanilla's player save. Retirement already
removes a focus once and preserves an existing deadline, so an earlier disconnect
callback cannot charge, refund, or extend it again. The separate Fabric disconnect
callback now schedules attachment mutations onto the server thread when invoked
from another thread. The original frozen-clock reconnect assertion and test-only
save-order receipts remain intact. Compilation passes; dedicated reconnect acceptance
still requires the next GitHub run because local dedicated-server EULA opt-in is
absent. No full green CI claim is made.

## Reed-Crowned Ferryman and the Last Crossing

The first regional boss slice is implemented. `FerrymanRules` owns the exact
server/client hit geometry: a 6.5-block locked oar sector, a 2.25-block marked
anchor circle and two wake lanes leaving a continuous 3.5-block safe crossing.
Their tells are 32/44/56 ticks and their recoveries 24/32/40. The server announces
and resolves each attack once; losing the announced target retires its pending
impact. At half health, subsequent moves change order and wake orientation without
changing the current tell. Ten quiet seconds restore an abandoned guardian.

An original articulated model has reed pontoons, a woven mantle, uneven crown,
long oar, hanging iron anchor and an emissive lantern. The client draws the locked
marks as geometry, independently of particle settings. Native review replaced
low-contrast deck stripes with visible crosses and distinct safe-lane borders.
The full and Minimal captures cover first- and third-person views.

The Last Crossing is a 35-by-35 basin with four shallow pools, two broad dry
crossings and four open shore exits. An authentic Crossing Bell explicitly starts
the fight; the structure's registered piece and exact bell position are checked.
The bell saves its guardian UUID and completion. Repeated use, chunk-clipped
replay and world reopening preserve that identity. Reload retires unfinished
attacks; a saved corpse is not healed. The piece preserves bell/container cells
while clearing its interior, preventing replay from resetting completion.

The real terrain survey initially found zero admissions in its declared 49
candidates. Test-only receipts showed suitable shore profiles over eight- to
nine-block-deep water, beyond the initial six-block foundation. The production
piece now contains a twelve-block foundation within its 21-block height and probes
every interior support column. The same survey then admitted candidate two,
chunk `[-176, 204]`, seed `-7620530482425397421`, bounds
`(-2816, 50, 3264)..(-2782, 70, 3298)`. Every checked support meets solid ground.
The test explicitly selects the normal preset and checks its actual generator and
seed. The published terrain screenshot shows only the authored piece placed at
that locator's choice, without a terrain fixture. This proves admission and fit,
not random-spread placement frequency across a world.

Contributing survivors still inside the arena receive 80 XP, one named compass
bound to that exact crossing, and a persistent field note. The compass uses a
vanilla untracked lodestone target so it does not require an installed lodestone.
The saved completed bell prevents repeat rewards. Participation is limited to 64
identities and resets with an abandoned or reopened attempt. The coastal Drowned
Road investigation remains independent and peaceful.

Verification at this checkpoint:

- `build compileGametestJava`: **PASS**, 2,721 JUnit tests, zero errors/failures/skips.
- Registered `FerrymanEncounterTest`: **PASS, 1m 50s**, including four orientations,
  reversed chunk replay, an unregistered bell refusal, the actual survival bell
  packet, all three normal moves and evasions, half-health change, target departure,
  finite abandonment, active/completed replay, two saved-world reopens, exact
  contributor reward and persistent completion.
- The first encounter run passed its attacks but exposed test packet sequencing:
  moving immediately after sending the second bell click outran server handling.
  The test now observes successful admission before moving; production admission
  and observation limits remain intact.
- All **9,005** generated paths match (PNG pixels, exact bytes elsewhere).
- All **191** guide pages and their GitBook export validate. Advancements now total
  156 visible, 110 tasks, 32 goals, 14 challenges, four hidden and 14,650 XP;
  deterministic item totals remain 48 Blank Runes and 29 Mana Crystals.
- Thirty portable selector/plan tests plus the independent Gradle/source parity
  check pass. The explicit plan is now 343 entries in 86/86/87/84 groups. The former
  342-entry plan is frozen, and a new assertion proves every prior group retains
  every member in its original order. No runtime failure is excused by scheduling.

Remaining acceptance: connected multiplayer participation/late arrival tour,
random generation frequency, and the wider full-client matrix. In CI `91919e2`,
Aura World now proceeds, but the following `WildercordArtsTest` reports multiple
string-admission and effect failures. Its original assertions remain unchanged.
The connected counter job still fails, and save/heap investigation remains open.
The Excise before-save repair is confirmed by the subsequent dedicated CI run;
no green full-matrix or release-readiness claim is made.

### Excise CI confirmation

Run `38434875114`, core job `115426148706`, passes **ExcisePlayableTest** in
74.087 seconds. At tick 695, body `656679764` retires a pending hold, writes recovery
707, and then enters `save_begin` with that exact value. The new body `1019110246`
has recovery 707 at tick 696. The original frozen-clock reconnect, death, respawn,
refusal and payment assertions all return successfully. The same core job later
fails in `MasterOrdinaryPlannerTest`: `Observe exact ordinary frame 3: 146 vs 145`.
That separate scheduling observation is the next core investigation; its exact
frame assertion has not been relaxed.

The Ferryman checkpoint's explicit JAR and all three profile archives pass package
inventory, dependency-pin and scope checks (`ferryman-packaging.json`). The raw JAR
is 29,010,471 bytes, SHA-256
`27814af1a25e71465ea6babc467e2face0d12c1da6756957042ce4c5c0ec22e7`.
Package validation is separate from full gameplay acceptance.

### Ordinary planner native timing and roster checks

The unchanged local `MasterOrdinaryPlannerTest` reproduced the core CI failure
in 30 seconds. It requested frame 3 after visiting frame 4, then requested the
last recovery frame after observing a later frame. Ordinary Sweep/Thrust/Crescent
currently have three/four-tick tells and three/four-tick recoveries, so those
observation sequences could never succeed. No production timings were changed.
The fixture now probes the first harmless warning tick, checks the last harmless
tick before applying a dodge/parry, and observes damage and full exposed recovery
on the final reserved frame. Exact game-time equality, exact damage/Aura budgets,
accepted history, ownership and duplicate-callback assertions remain enforced.

That exposed a separate fixture assumption: moving a non-target challenger out
of range does not withdraw their enrollment. A new outside-arena case proves the
paused challenger takes no hit and retains enrollment/history; the roster-removal
case now uses an actual spectator transition and proves removal and phrase reset.
The registered fixture releases its closed world/entity references in `finally`.

All **38 native scenarios** pass in **2m 9s**
(`ordinary-planner-roster-boundaries.log`), covering all three original schools,
three health phases, solo/eight-player trials, counters, reentry, lifecycle,
stale proposal and expired callback checks. This is focused native acceptance;
full CI remains pending. Additional CI evidence identifies the next issues:
Ember Kiln's fixed-payment assertion, funded-shell idle visibility, and connected
counter receipts whose host item display context differs between classic and
articulated paths. Both connected JVMs returned successfully at `6e6c805`; the
independent receipt validator still rejects those captures, so they are not
accepted visual evidence.

### Connected counter item-basis validator repair

The `6e6c805` connected artifact contains successful exits from both JVMs in each
54-case wide/slim run. The independent curator rejected the articulated owner's
item draw because it assumed every first-person camera uses a FIRST_PERSON item
display transform. `ArticulatedViewModel` actually submits the unchanged item
state from `AvatarRenderState` at the hand socket, which intentionally uses the
THIRD_PERSON transform. Classic first-person rendering uses FIRST_PERSON; both
remote backends use THIRD_PERSON. The native receipts match all four paths.

The validator now derives that exact expected item basis from the already-bound
role/backend while independently retaining the camera/view checks. It still
requires the exact original stock diamond rotation, translation, scale, local
matrix, hand, hilt anchor, emitted quads and all original geometry/provenance
comparisons. No runtime rendering, receipt bytes or capture phase changed.
Regression cases reject camera-basis confusion, either wrong hand, and a correct
context label carrying the other transform. Synthetic properties use explicit LF
bytes so the existing LF/CR/CRLF and malformed-Java-parser probes run on Windows.

All **22 `CounterEvidenceTests` pass** in 95.217 seconds. Rechecking the original
downloaded `38434875114` artifacts passes the complete terminal/life/counter witness
validation for both profiles (**128 counter phase captures**, 54 cases per profile).
`counter-archived-revalidation.json` explicitly records that these are archived
receipts, the original run failed, and no new native run or exhaustive pixel review
is claimed. Two wide/right/active frames were inspected visually; that spot check
does not replace review of the entire matrix. A fresh CI run remains required.

### Ember Kiln exact-payment fixture

An unchanged focused run reproduced CI's first Kiln failure in 29 seconds. The
eight-player fixture expected a fixed 76 Aura after the opening Wake while
ordinary crowd regeneration was active. It now uses the existing `fixAura()`
test control, as the planner's exact-debit fixture does, to isolate paid costs.
The next run reached the interrupt/recovery boundary and exposed an obsolete
16-Aura ordinary-attack expectation: the current cost is 12. Exact balances now
assert 36 after Wake + ring + ordinary attack and 64 after Wake + a refused ring
followed by its ordinary fallback. The cooldown assertion already used the live
130-tick rule; its stale 180-tick message is corrected. No production costs,
cooldowns, recovery windows or damage values were changed.

All **35 native scenarios pass in 5m 27s**
(`ember-kiln-current-balances.log`): the full original counter/ownership matrix,
phase variants, natural grounding/travel, lethal party pulse and four blocked
admissions. Exact frame equality and original hit/recovery assertions remain.
The fixture also clears its retained world, entities, ward, pulse and grounding
records after close. A separate CI shard at `6e6c805` still exhausted heap in
`LifeOutcomeEdgesTest` after 38 minutes; no full-matrix memory fix is claimed.

### Funded Aura shell: active rendering and ordinary idle fade

The existing articulated presentation fixture incorrectly required a visible shell
on every funded frame, including idle frames after the ordinary flare expires.
Production already separates available Aura Armour protection from visible use.
The fixture now requires the genuine shell at every active capture, exactly one
shell while use/flare is active, and zero shell submissions at rest. Body ownership,
armour material/trim/glint, hand socket, sword, cancellation, deferred geometry and
resource-reload assertions remain. An explicit idle check verifies server-side
Aura Armour protection is still available after the visible shell disappears.

`ArticulatedAuraShellPresentationTest` passes in **8m 4s**, with all **36** actual
input/camera/hand/armour/move combinations. The screenshots are preserved under
`artifacts/review/world-depth/aura-shell/`. One first-person frame was inspected;
this does not claim exhaustive pixel review of all captures. Both guide editions
explain the existing idle fade. No production rendering or protection value changed.
The remaining nine counter profile/selection tests also pass (23.916 seconds).

### User-directed testing migration: VPS only

The owner directed all future testing to the VPS and requested an end to GitHub
CI. On October 13, GitHub Actions was disabled for the entire repository through
its repository settings (`enabled=false`). Build and Player Guide were also
disabled individually. Runs `38441765274`, `38440285130` and `38434875114` are
confirmed completed/cancelled. Historical results are preserved. Old references
above to a required next GitHub run describe the earlier workflow; new acceptance
work must execute on the VPS instead. The archived YAML remains historical setup,
not an enabled execution path. Guide auto-publication is consequently stopped too.

SSH access to the configured `pandora-vps` is confirmed. It is the existing Windows
host with the live server at `C:\wildercord`, Java 25, Python 3.12 and eight logical
processors. A separate checkout is being prepared at `C:\wildercord-ci\repo` for
builds and tests. Its setup is not yet a passing VPS build/native result. Native
graphics/session availability and replacement full-suite orchestration still need
verification. The live server, saves and installed release are unchanged.

The broad development goal remains active, and a 15-minute chat heartbeat records
the VPS-only requirement, coherent commits/pushes, the unfinished roadmap and the
need to inspect running jobs before starting another. See the accompanying
`2026-10-13-progress.md` for the requested readable progress inventory.

### First VPS build, native gameplay and evidence migration

The exact `550fa619` candidate passes its first VPS build and native-source
compilation (5m 18s Gradle, initial tool setup separate), generated-asset parity
and guide validation. Tests run in `C:\wildercord-ci\repo`, with a dedicated
Python environment and Gradle cache. The live `C:\wildercord` installation is
not changed. GitHub Actions remains disabled.

`tools/run_vps_checks.py` now owns sequential Windows build/focused-native runs,
requires the expected computer and exact clean source commit, rejects inherited
test filters/JVM injection, uses the shared checkout lock and retains per-launch
JSON/logs. An owned Windows job contains only the runner and its descendants.
Seven VPS tests pass, including real normal/forced-exit cleanup while an unrelated
sibling remains alive. Native success requires the actual processed descriptor,
four ordered lifecycle stages with a fresh nonce, no skip and a successful process
exit. A focused receipt always leaves full-client acceptance unverified.

The pending `NextCounterTest` and `NextSupportTest` field cleanup is now verified
on the VPS at `ad8b967c`: native passes in **3m 11s** and **1m 38s**, including
their real save/reopen checks. The initial native run includes first-time setup.
AMD OpenGL and OpenAL's no-output mixer initialize successfully in the SSH session.
The former retained static entity/Cast references are released; the wider heap
failure still needs its long-run reproduction.

Completed-evidence reading now preserves the Linux checks and adds an actual
Windows handle path. It opens final reparse points without following them, rejects
links/devices/directories and denies writer/deletion sharing. Bounds and stable
file checks remain; the full and required-Masters parsers reuse it. Four actual
Windows file tests pass. The wider tooling run has **69 passing tests and one
explicit Gradle/Groovy-runtime discovery skip**, not 70 passes. Its cache lookup
now honors `GRADLE_USER_HOME`; the executable parity rerun remains pending here.

`FerrymanEncounterTest` also passes on the VPS at `39d05075` in **2m 47s**, through
the new evidence reader. Result UUIDs are `d0d876e2-4645-4a79-8ff1-e68e62cede96`
(Counter), `d38dfd8f-0b4d-4e60-bb66-cbfb441ce3b1` (Support), and
`bd6e55ee-a6bf-40c7-8def-27120654e2cb` (Ferryman). Full-suite orchestration,
connected matrices and their fresh candidate acceptance remain migration work;
none of these focused successes substitutes for them.

### VPS native group protocol and Arts fixture diagnosis

The exact `73c34d4a` owned VPS build passes all **2,721** JUnit tests with no
failures/errors/skips, **9,005** generated paths and **191** guide pages. The
previously skipped Gradle/Groovy parity check independently passes in 15.442s.
Its retained build receipt is `a5cc9bba-2260-49f0-b6dc-12a1811ae966`.

`48fb5cc8` adds complete Windows group execution and explicit VPS provenance:
the original four full-client groups, three required Masters parts and six-class
articulated roster remain intact. Per-launch manifests bind the source, computer,
batch/attempt, actual child exit and class lifecycle nonce. Aggregation refuses
mixed providers, candidates, computers, batches, duplicate parts and reused
launches. Nine new protocol/actual-child tests, all sixteen existing full-client
protocol tests (including executable Gradle parity), and all fifteen required-part
tests pass on the VPS: **40 tests, zero skips**. A real Stone March group run is
in progress; no full aggregate is accepted from these tooling tests.

The unfiltered `WildercordArtsTest` fails at `73c34d4a` in **5m 27s**, receipt
`94575240-8a03-409b-8850-022bb8c326ed`. The first errors involve Glacier Mirror,
Skate and Winters Hush; the native log then records `Player0 was slain by Husk`
before later scenes lose their targets. Earlier cleanup kills named Splitting
and Vampiric fixtures. Source inspection shows the ordinary-target helper marked
Runebound rolls but did not mark the independent `Tempering` system; Splitting
death spawns untagged children that the next fixture cleanup cannot select.

The helper now explicitly admits ordinary, untempered, non-Runebound targets and
asserts their exact authored health after world admission. Active AI and native
damage/movement/freeze/thaw behavior remain. A dead original player fails at the
reset boundary instead of generating dozens of misleading later errors. Individual
art start/pass/failure observations make the original fifty-art run diagnosable.
This repair still needs its new native VPS run; no production combat values changed.

The README's inactive Actions badge is removed and its testing instructions now
point to VPS evidence. Contributor guidance also records the new execution policy.
Historical failures are retained, and no artificial green GitHub status is added.

### Later fixture retention review

`RuneEtchingsTest.foe` and both `VoidFlightVariantsTest` delivery targets held
entities after their world context closed. Both now release those references in
`finally`, including an assertion failure path; their native assertions and
presentation settings restoration remain unchanged. Void variants occur after
`LifeOutcomeEdgesTest` in shard one's frozen order; Rune Etchings runs separately
in shard two. They cannot explain the earlier failure at that class. They were
additional retention sources for a longer run. The same review found the
`ResonantStrikesTest` fake player/Cast and the `MovesPackTest` and
`LessonPackPlayableTest` entity fields retained their later closed worlds. Those
are now released in their outer `finally` blocks too. Compile verification and a
fresh full run remain pending for these small cleanups; the original heap failure
is still open.

### Complete VPS Arts rerun

The exact `fc403c21` candidate passes the complete unfiltered `WildercordArtsTest`
on the VPS in **12m 6s** (owned wall time 12m 7s), receipt
`ec4d309d-c8f9-4006-a3f3-377e65658cde`. All fifty individual art scenes, the
registry/string contract and the Aura page pass: 52 successful observations,
zero failed observations, all four owned class lifecycle stages, no skip and
actual child exit zero. Native screenshots and the complete successful log are
retained alongside the earlier failed receipt. This establishes the ordinary
fixture correction at that candidate; it does not establish the full 343-class
gate or solve the independent long-run heap issue.
