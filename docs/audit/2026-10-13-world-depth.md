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
| POL-09: generator preserves boss guidance | Implemented and locally verified | All 157 existing authored values preserved; ten obsolete labels suppressed; 8,987 generated paths stable; two Python regression checks added. Existing CI runs generation parity and boss JUnit assertions. |
| Sleeping Blade history access | Implemented and locally verified | Native journal test verifies authentic-only discovery, one book per visitor and no ownership/draw from reading. Full existing focused `SleepingBladeTest` also passes, including generation, interrupted draws, unique ownership and guard tradeoffs. |
| Full development goal | Active | Remaining backlog above; not a release-readiness claim. |

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
