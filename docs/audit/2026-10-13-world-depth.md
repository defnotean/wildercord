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
| `cast/boss/` and boss classes in `cast/`; `world/dungeons/` | Archive, elemental dungeons, Root Guardian, Storm Conductor and newer regional encounters | Recent Root/Storm movesets exist on main. Do not duplicate them; regression-test them and build a different next boss. |
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
| POL-01: sixteen-school journal | In progress | Mapping defect reproduced in source; implementation and tests next. |
| Full development goal | Active | Remaining backlog above; not a release-readiness claim. |
