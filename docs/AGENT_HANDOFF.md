# WilderCord development handoff

Prepared October 13, 2026 after the owner requested a pause. This document is
intended to work without the preceding chat. Read it together with the current
user request and the actual Git/process state; paths, PIDs and results below are
a dated snapshot, not live process discovery.

## 1. State at handoff

| Item | State |
| --- | --- |
| Project | WilderCord, a Minecraft Java **26.3** mod, Fabric Loader **0.19.5**, Java **25** |
| Repository | <https://github.com/defnotean/wildercord> |
| Development branch | `living-world-expansion` (renamed from `defnotean/world-depth-audit`) |
| Review | Draft [PR #2](https://github.com/defnotean/wildercord/pull/2); not merged or released |
| Local workspace | `C:\Users\Eating\Desktop\WilderCord` |
| Work baseline | `0a18358929180f1c4bc5cc4b827f7f92f05571c4` |
| Last content candidate | `710dc620`, pushed to GitHub (`0476c86b` verified on `pandora-vps`; `710dc620` ready for VPS sync once `sshd` is restarted) |
| Immediately preceding checkpoint | `0476c86bc6ec1476062ead27c8ed11b872716201` (verified build, Masters 47-class gate, articulated 6-class suite, and Siltcrest stabilization) |
| Development goal | **Waiting for `sshd` restart on `pandora-vps`** (owner away from home; resume VPS 4-shard run once `sshd` is restarted) |
| Scheduled continuation | **PAUSED** while `pandora-vps` (`192.168.1.110:22`) is wedged in pre-auth |
| GitHub Actions | Repository-wide **disabled**; build/docs workflows also disabled |
| Owned VPS work | Batches 1–5, plus Batch 6 (`132` Python gate tests, release packaging audit, 6-class `articulated` suite, 47-class `required-masters-aggregate` gate, `SiltcrestPresentationTest` stabilization, and heap-dump root-cause diagnosis of `IntegratedServer` retention) verified on `WIN-I2GBVRRD872` |
| Live Minecraft server | Left running and unchanged (`PID 4724` in `C:\wildercord`) |

The owner originally asked for a repository-wide audit, a detailed expansion list,
and sustained development of bosses, buildings, NPCs, custom biomes, legends, lore
and polish, with regular GitHub pushes. They required **all future tests on
the VPS, no GitHub CI**, and later explicitly resumed work one batch at a time.

Candidate `0476c86b` passes the owned VPS build (`4ce8196d-c56f-4f2b-a43a-9e0c4040466a`,
`2,755` JUnit tests, `9,081` generated asset paths, `197` guide pages), all `132`
Python protocol/gate unit tests, release packaging audit (`29,214,149` bytes), the
complete 6-class `articulated` suite (`65705560-fbcc-4759-9adb-446f8892aa90`), the
complete 47-class `required-masters-aggregate` gate (`batch 1791952000`:
`43fc14bd-645a-4f84-9ead-8ecc334c1fce`, `713a7f5f-5e2e-47aa-af05-4039c15d6b62`,
`9750c316-30db-4819-a5cc-a7b5e98a6a4d`), and focused `SiltcrestPresentationTest`
(`020fca8f-0345-46bd-8e0a-f88cca13559b`). Commit `710dc620` (`ServerDisconnects` +
`ExcisePlayerRemovalMixin`) resolves the exact root cause of both the `IntegratedServer`
memory leak (`ServerNetworkingImpl.PLAY.trackedAddons` retaining closed singleplayer
worlds when `RootCarry`'s `DISCONNECT` listener mutated `ServerPlayer` attachments on
the `Render thread` inside `Connection.channelInactive` and tripped
`PlayerAttachmentThreadDiagnosticsMixin` before `AbstractNetworkAddon.endSession()`)
and the historical off-thread attachment save race.

## 2. Where the full context lives

- [Progress inventory](audit/2026-10-13-progress.md): detailed list of everything
  implemented, repaired, tested and left unfinished during this effort.
- [World-depth audit](audit/2026-10-13-world-depth.md): baseline inventory, confirmed
  integration gaps, prioritized BIO/BLD/BOSS/NPC/lore roadmap and dated evidence.
  Its early findings are historical; consult later milestone entries for fixes.
- [Architecture](ARCHITECTURE.md): spell compiler, runtime, player state, Aura,
  rendering, networking and the generation pipeline.
- [Lore canon](LORE_CANON.md): human schools, Masters, Tideward, Marchkeepers,
  Belowkeepers and named people. Do not rewrite their identities casually.
- [VPS testing](testing/vps.md): owned execution, source provenance, interrupted
  processes, evidence and complete-group commands.
- [Full client plan](FULL_CLIENT_SHARD_PLAN.md): exact class membership, order,
  source hashes and the distinction between focused and complete acceptance.
- [Pause receipt](audit/evidence/2026-10-13-paused-vps-run.json): durable summary of
  the intentionally interrupted native run and its class-return boundaries.

`docs/audit/` and `docs/LIVING_WORLD_ROADMAP.md` contain many older milestones.
They are useful history, not evidence that their tests ran on today's HEAD.
`wiki/` contains authored player pages; `gitbook/` is their generated export.
The public guide is not automatically updating while Actions is disabled.

## 3. Repository map and working conventions

| Area | Purpose / useful starting point |
| --- | --- |
| `src/main/java/dev/wildercord/Wildercord.java` | Main registration and initialization |
| `spell/` | Pure Java rune grammar, compiler, numbers, reactions, attunements; keep Minecraft imports out |
| `cast/` | Server casting, admission/payment, spell lifetime, world effects and event callbacks |
| `aura/`, `aura/world/` | Breathing schools, techniques, Masters, physical combat, trials and highland beasts |
| `world/biome/` | Reedlands and Windscar climate integration; Reedland bank placement |
| `mixin/ReedlandPlacementMixin.java`, `ReedlandSurfaceMixin.java` | Both regional biome adapters now use these originally named hooks |
| `world/sites/{farm,masters,wilds,travel,water,mine,highland}/` | Site registration, procedural pieces, authentic markers, loot and discovery |
| `world/ferryman/`, `world/cartographer/` | Reed-Crowned Ferryman and Glass Cartographer rules, encounter state and content registration |
| `wildlife/`, `monster/` | Existing ecosystems, animals, resources, rare threats and spawn predicates |
| `lore/` | Owner journal, serialized discovery IDs, conversations and ordered quest rules |
| `player/`, `net/`, `api/`, `config/` | Saved attachments, authority, packets, integration hooks and config |
| `src/client/java/dev/wildercord/client/` | Renderers, animation, UI, presentation and effects |
| `src/main/resources/` | Generated data/art plus loader and mixin configuration |
| `src/test/java/` | JUnit rule/contract tests |
| `src/gametest/java/`, `src/gametest/resources/fabric.mod.json` | Real native-client tests and their exact registered entrypoints |
| `tools/generate_assets.py` and `tools/*_art.py`, `sites_*.py` | Source of generated language, biome/structure data, recipes, textures and loot |
| `tools/player_docs.py` | Validate authored wiki and export GitBook; `--check` rejects stale exports |
| `tools/run_vps_checks.py` | Owned Windows VPS build/focused/suite/shard runner |
| `tools/client_shard_plan.{py,json}` | Explicit native scheduling contract shared with Gradle |
| `profiles/`, packaging tools | Development JAR/profile verification; not release authorization |

Minecraft 26.3 changed several data/API formats. Work from the actual mapped
sources and current local patterns. In particular, biomes use environment
attributes for natural spawn settings; old biome JSON examples are unsuitable.
The local read-only mapped jars are under
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-common-7e9a32a5b8/26.3/`.
The corresponding `*-sources.jar` is available. Their presence is not permission
to run a new local build: the owner moved builds, generation and tests to the VPS.

Production authority stays on the server. Preserve ownership, claim/build
permissions, paid budgets, finite state lifetimes, exact persistence semantics
and physical combat's distinct presentation. Registrations and permanent event
listeners must not retain closed worlds through fixture entities or players.

## 4. What was implemented during this effort

### Connected Reedlands region — native evidence available

- **Luminous Reedlands**, the first custom registered Overworld biome: a damp
  partition of ordinary swamp climate, inherited swamp surfaces, distinct colours,
  wetland fauna, natural Moonreed and persistent owner discovery.
- **Tideward Reedkeeper Landing**, a stilt house and living garden with fishing
  supplies, a lectern, authentic site discovery and a resident trading villager.
- **The First Crossing** legend, original Reedkeeper stories, then progress-aware
  return conversations for the ledger, lighthouse, completed peaceful journey
  and Ferryman victory. Original stories come first; repeated thanks give no loot.
- **The Drowned Road**, three ordered chapters connecting the resident, boathouse
  passenger ledger and actual lighthouse lever/lamp. Earlier visits count. Total
  rewards: 90 XP, five Blank Runes and one Aura Shard, once per player.
- Discovery support for all ten water sites, including the two new sites.
- **The Last Crossing**, a 35-by-35 basin with four pools, broad dry paths,
  supported foundations, four shore exits, book, supplies and authentic bell.
- **Reed-Crowned Ferryman**, an optional original boss with a reed raft, mantle,
  crown, oar, anchor and emissive lantern. Three telegraphed attacks, Minimal-mode
  geometry, a half-health pattern change, cancellation/reset/abandonment behavior,
  saved bell identity and copied-site/refarming refusal. Eligible surviving
  contributors receive 80 XP, a journal note and a compass to that crossing once.

Key sources: `world/biome/ReedlandClimate.java`, `ReedlandBanks.java`,
`world/sites/water/{ReedkeeperLandingPiece,ReedkeeperResident,LastCrossingPiece,WaterSites}.java`,
`world/ferryman/`, `client/ferryman/`, `lore/LoreQuestRules.java`,
`tools/regional_biomes.py`, `tools/sites_water.py` and the Ferryman art module.
Native coverage: `ReedlandBiomeTest`, `ReedkeeperLandingChecks`, `DrownedRoadChecks`,
`FerrymanEncounterTest` and `LastCrossingTerrainChecks`.

### Lore, combat and presentation repairs

- Scout's Last Cairn investigation: three chapters connect watchtowers, standing
  stones and authentic Sleeping Blade history. The Last Oath can be read without
  claiming the sword; copied markers cannot grant it.
- All sixteen Master journal identities now map correctly. Older placeholder
  entries normalize without replaying rewards. Authored boss language survives
  regeneration: 157 values retained and ten obsolete labels removed.
- Masters retire paid actions when their last challenger leaves. Returned severed
  Aura can reach its original Master within the existing paid damage allowance.
- Excise retires paid holds before player-removal serialization; off-thread
  retirement is marshalled to the server thread. Dedicated reconnect evidence
  preserves the same recovery deadline. A broader historical save race is open.
- Corrected Heart/Overflow and ward/Resistance/Brace guidance, all 417 Master
  technique timing rows, and visible advancement rewards. See the progress report
  for exact values and scopes; these were not wholesale balance changes.
- Fixed concrete test-fixture errors: elite scaling, regeneration interference,
  lifecycle ordering, stale timing assumptions, damage-path probes and retained
  world/entity references. Ordinary Arts targets suppress both independent elite
  systems; the complete fifty-art suite passes without changing combat rules.
- Corrected the connected articulated first-person item transform assumption.
  Original geometry, provenance and camera checks remain. Archived paired
  captures revalidate, but a fresh connected VPS matrix is still required.

### Connected Windscar Highlands region — native evidence available (`a85023dd`)

- **Windscar Highlands (`BIO-02`)**: partitions the dry windswept-hill humidity band
  at or below `-0.25`, preserving all other climate coordinates and biome entries.
  Inherits highland stone surfaces, retains ordered vanilla features/ores with sparse
  trees and an authored palette, includes Windreed decoration, Stonehorns, Galeclaws,
  Tussock Pikas and Skyrays, and shares `peaks`' Summit Wind attunement at `Y >= 200`.
- **Highland Cairn Lodge (`BLD-03`) & Cairn Keeper (`NPC-03`)**: a 21-by-21 stone-and-spruce
  ridge shelter with a dry-stone wind-break wall, cultivated leeward Windreed terrace,
  eastern Cairn Beacon Lamp and lever, four-page *The Paired Cairns* book, supply chest,
  and a persistent trading Cairn Keeper villager (`cairn_keeper`) with four original
  stories plus four progress-aware return conversations (`watch_orders`, `cairn_light`,
  `light_return`, `cartographer_return`).
- **Paired Cairns / Ridge Watch / Lamp on the Ridge (`LOR-03`)**: three ordered journal
  chapters (`paired_cairns` -> `ridge_orders` -> `cairn_beacon`) connecting the Cairn
  Keeper's stories, Marchkeeper watchtower visits, and lighting the authentic Cairn
  Beacon Lamp (`learned:scout_wind_mark`).
- **Highland Survey Ring & The Glass Cartographer (`BOS-03`)**: a 35-by-35 open stone
  meridian court with an entrance shelter, two-page *The Glass Meridian* book, supply
  chest, and authentic `SurveyAstrolabeBlock`. Using the astrolabe in Survival summons
  the pedestal-anchored `GlassCartographer` with three world-geometry telegraphed
  attacks (`SWEEP`, `PLUMB`, `GRID`) readable under `ParticleStatus.MINIMAL`, a Phase 2
  rotated meridian grid at `<= 50%` health, target-loss retirement, quiet abandonment
  reset, chunk-replay preservation, and a one-time `Compass of the High Meridian`
  (`high_meridian_compass`) + `80 XP` + `learned:glass_cartographer` reward.

### Connected Belowkeeper Hollows region — native evidence available (`3778d1eb`)

- **Belowkeeper Hollows (`BIO-04`)**: partitions the continental subterranean band of
  `minecraft:dripstone_caves` (`continentalness >= 0.40`), preserving all other climate
  coordinates and biome entries. Combines dripstone formations with lush moss/clay
  cave shelves while keeping `generation.features` strictly ordered with vanilla cave
  feature steps (`FeatureSorter` verified acyclic across all 70 Overworld biomes).
  Includes `glowcap_patch`, `breathmark_site`, `belowkeeper_drainhouse`, Sporeback
  Snails, Mossveil Dormice, Rootmolt Striders, and shares `deep_caves`' Belowkeeper
  Stillness attunement at `Y <= 0` (`skyLight == 0`).
- **Belowkeeper Listening Station (`BLD-02`) & Belowkeeper Listener (`NPC-02`)**: a
  21-by-21 vaulted deepslate-and-oxidized-copper acoustic chamber with a damp moss/clay
  Glowcap basin, overhead Fungal Nursery, Acoustic Resonance Shutter Lamp and lever,
  four-page *What the Empty Bell Kept* lectern book, supply chest, and a persistent
  librarian Belowkeeper Listener villager (`belowkeeper_listener`) with four original
  stories plus four progress-aware return conversations (`deep_records`, `station_gauge`,
  `gauge_return`, `bell_return`).
- **What the Empty Bell Kept / Pressure Records / Resonance Shutter (`LOR-02`)**: three
  ordered journal chapters (`empty_bell_kept` -> `pressure_records` -> `resonance_shutter`,
  `90 XP`, `5 Blank Runes`, `1 Aura Shard` once per player) connecting the Listener's
  stories, Deepslate Vault (`place:mine_deep_vault`) discovery, and lighting the
  authentic Acoustic Resonance Shutter Lamp (`learned:belowkeeper_shutter`).
- **Belowkeeper Bell Sanctum & The Hollow Bell (`BOS-01`)**: a 35-by-35 vaulted
  deepslate-and-copper resonance court with an entrance vestibule, two-page *The Quiet
  Bell* lectern book, supply chest, and authentic `ListeningBelfryBlock`. Using the
  belfry in Survival summons the plinth-anchored `HollowBell` with three world-geometry
  telegraphed acoustic attacks (`TOLL` annular pressure ring with a safe inner eye,
  `CLAPPER` locked drop circle, `CHORD` damped acoustic shutter lane) readable under
  `ParticleStatus.MINIMAL`, a Phase 2 rotated resonance chord at `<= 50%` health,
  target-loss retirement, quiet abandonment reset, chunk-replay preservation, and a
  one-time `Compass of the Quiet Bell` (`quiet_bell_compass`) + `80 XP` +
  `learned:hollow_bell` reward.

### Connected Emberwood Reach region — native evidence available (`3920b5c3`)

- **Emberwood Reach (`BIO-03`)**: partitions the humid band of `minecraft:wooded_badlands`
  (`humidity >= 0.25`), preserving all other climate coordinates and biome entries.
  Blends podzol and coarse-dirt ash firebreaks with warm grassland in
  `ReedlandSurfaceMixin` while keeping `generation.features` strictly ordered with
  vanilla feature steps (`FeatureSorter` verified acyclic across all 71 Overworld
  biomes). Includes `cinder_fern_patch`, Cinderfoxes, Salamanders, Charcoal Moths,
  Cinder Bailiffs, and shares `badlands`'s Sunscorch attunement at noon under open sky.
- **Emberwood Charcoal Yard (`BLD-04`) & Emberwood Kiln Keeper (`NPC-04`)**: a
  21-by-21 brick-and-dark-oak burner's yard with a coarse-dirt firebreak, cultivated
  Cinder Fern ash bed, recessed hearth behind an iron-bar grate, Kiln Draft Lamp and
  lever, four-page *Eleven Winters at the Gate* lectern book, supply chest, and a
  persistent weaponsmith Kiln Keeper villager (`kiln_keeper`) with four original
  stories plus four progress-aware return conversations (`forge_ledger`, `kiln_draft`,
  `draft_return`, `widow_return`).
- **Eleven Winters at the Gate / The Southern Forge Ledgers / The Kiln Draft Lamp (`LOR-04`)**:
  three ordered journal chapters (`eleven_winters` -> `forge_records` -> `kiln_draft_lamp`,
  `90 XP`, `5 Blank Runes`, `1 Aura Shard` once per player) connecting the Kiln Keeper's
  stories, Mountain Forge Hall (`place:mine_forge_hall`) discovery, and lighting the
  authentic Kiln Draft Lamp (`learned:emberwood_draft`).
- **Emberwood Kiln Court & The Kiln Widow (`BOS-04`)**: a 35-by-35 brick-and-terracotta
  kiln court with an entrance gatehouse, four corner draft vents, two-page *The Cooled
  Kiln* lectern book, supply chest, and authentic `KilnHeartstoneBlock`. Using the
  heartstone in Survival summons the plinth-anchored `KilnWidow` (registered in
  `Spirits.isBoss`) with three world-geometry telegraphed kiln attacks (`DRAFT` safe
  cooling-eye ring, `SLAG` locked mortar drop circle, `SWEEP` cooled-brick shelter lane)
  readable under `ParticleStatus.MINIMAL`, a Phase 2 rotated flue-sweep lane at `<= 50%`
  health, target-loss retirement, quiet abandonment reset, chunk-replay preservation,
  and a one-time `Compass of the Cooled Kiln` (`cooled_kiln_compass`) + `80 XP` +
  `learned:kiln_widow` reward.

### Cross-region world-depth audit, lore canon & existing boss regression — native evidence available (`0d63abfa`)

- **Lore Canon Reconciliation (`POL-02`)**: updated `docs/LORE_CANON.md` with Emberwood
  Reach, Emberwood Charcoal Yards, Emberwood Kiln Keepers, The Kiln Widow, and the
  3-chapter Eleven Winters at the Gate / The Southern Forge Ledgers / The Kiln Draft
  Lamp journal route.
- **Cross-Region World-Depth Audit (`BIO-05`, `POL-03..08`, `LOR-06`)**: added
  `RegionalWorldDepthAuditTest` verifying 24-permutation climate adapter commutativity
  and idempotence across all four regional biomes, shared parent attunement rules (`15`
  rules), unique structure salts and valid spacing across all eight regional sites,
  finite 3-chapter quest rewards (`90 XP`, `5 Blank Runes`, `1 Aura Shard` once per
  route), complete journal entry and resident dialogue translation coverage, and
  regional boss rule invariants.
- **Existing Boss Regression Tour (`BOS-05`)**: verified native `NewEncountersTest`
  (`RootGuardian` + `StormConductor`) and `WildercordDungeonsTest` (`CinderWarden` +
  `StarEater` + `TideScribe`) on `pandora-vps` after all four regional expansions.

### Batch 6 native gates, Siltcrest stabilization & `IntegratedServer` leak resolution (`0476c86b` / `710dc620`)

- **Complete 47-Class `required-masters-aggregate` Gate (`batch 1791952000`)**: all three
  required Masters suites (`masters-core` 44 classes, `masters-march-mechanics` 1 class,
  `masters-march-visuals` 2 classes) passed on `pandora-vps` and passed
  `tools/masters_required_ci.py` (`focusedClientGate: "passed"`,
  `artifacts/review/world-depth/vps-native/masters-aggregate-1791952000.json`).
- **Complete 6-Class `articulated` Suite**: passed on `pandora-vps` in `41m 22s`
  (`65705560-fbcc-4759-9adb-446f8892aa90`).
- **Python Protocol/Gate Unit Tests & Release Packaging Audit**: all `132` unit tests in
  `tools/test_*.py` passed on `pandora-vps`, and `tools/build_profiles.py` +
  `tools/audit_packages.py` passed (`wildercord-0.13.0-alpha+mc26.3.jar` at `29,214,149`
  bytes, `within_32_mib_raw_file_limit: true`).
- **`SiltcrestPresentationTest` Stabilization (`654b8106` & `0476c86b`)**:
  `BitternShelterGoal.findShelter` now completes the full 6-block radius sweep on each
  activation tick, and `SiltcrestPresentationWitness.classifyRefusal` recognizes
  `REACH_ONLY_STRIKE`. Verified in focused mode (`020fca8f-0345-46bd-8e0a-f88cca13559b`)
  and inside `shard 1/4` (class `38 / 87`, `38.36s`).
- **`IntegratedServer` Memory Leak & Off-Thread Disconnect Race Fix (`710dc620`)**:
  forensic heap-dump (`C:\wildercord-ci\dump.hprof`, `538 MB`) and stack-trace analysis
  established that when a singleplayer world closes on the client `Render thread`,
  Netty's `LocalChannel` synchronously calls `Connection.channelInactive` ->
  `ServerPlayNetworkAddon.handleDisconnect()` -> `ServerPlayConnectionEvents.DISCONNECT`
  on the `Render thread` (`!server.isSameThread()`). Listener `[22]`
  (`RootCarry.java:91`) called `h.player.removeAttached(SELECT)` off-thread, causing
  `PlayerAttachmentThreadDiagnosticsMixin` to throw `AssertionError` before
  `AbstractNetworkAddon.handleDisconnect()` could call `this.endSession()` (while Netty's
  `Connection.exceptionCaught` silently swallowed the exception because `!channel.isOpen()`).
  That left every closed world's `ServerPlayNetworkAddon` in
  `ServerNetworkingImpl.PLAY.trackedAddons`, retaining ~45 MB per closed `IntegratedServer`
  until `shard 1/4` hit `OutOfMemoryError` at class `69 / 87` (and was also the exact cause
  of the historical off-thread attachment `ConcurrentModificationException` during player
  saves). `dev.wildercord.net.ServerDisconnects` and `ExcisePlayerRemovalMixin` marshal
  all 59 WilderCord `DISCONNECT` listeners onto the owning `MinecraftServer` thread and
  flush pending off-thread disconnects at `SERVER_STOPPING` and at the head of
  `PlayerList.remove`, `saveAll`, and `removeAll`.

## 5. Evidence index — do not merge scopes

These are actual VPS receipts. Full `result.json`, logs and screenshots are
retained remotely in `C:\wildercord-ci\repo\artifacts\review\vps\<UUID>` and copied
locally under `artifacts/review/world-depth/vps-native/<UUID>`. Those large local
artifacts are ignored by Git. A fresh clone has this index, not all raw evidence.

| Check | Candidate | Result | Receipt UUID |
| --- | --- | --- | --- |
| Owned build, native compilation, 2,755 JUnit cases, 9,081 generated paths, 197 guide pages | `0476c86b` | Pass; no JUnit failures/errors/skips | `4ce8196d-c56f-4f2b-a43a-9e0c4040466a` |
| Focused `SiltcrestPresentationTest` (shelter sweep + reach-only refusal) | `0476c86b` | Pass, 2m 08s | `020fca8f-0345-46bd-8e0a-f88cca13559b` |
| Complete 6-class `articulated` suite | `708172ec` | Pass, 41m 22s | `65705560-fbcc-4759-9adb-446f8892aa90` |
| Complete `masters-core` part (44 classes, `batch 1791952000`) | `708172ec` | Pass, 31m 39s | `43fc14bd-645a-4f84-9ead-8ecc334c1fce` |
| Complete `masters-march-mechanics` part (1 class, `batch 1791952000`) | `708172ec` | Pass, 7m 11s | `713a7f5f-5e2e-47aa-af05-4039c15d6b62` |
| Complete `masters-march-visuals` part (2 classes, `batch 1791952000`) | `708172ec` | Pass, 4m 56s | `9750c316-30db-4819-a5cc-a7b5e98a6a4d` |
| Combined 47-class `required-masters-aggregate` (`batch 1791952000`) | `708172ec` | Pass (`focusedClientGate: "passed"`) | `masters-aggregate-1791952000.json` |
| Owned build, native compilation, 2,755 JUnit cases, 9,081 generated paths, 197 guide pages | `0d63abfa` | Pass; no JUnit failures/errors/skips | `2d1da2fc-6fcf-4c7a-bbdd-907bd5ac6d39` |
| Root Guardian & Storm Conductor native boss regression (`NewEncountersTest`) | `0d63abfa` | Pass, 2m 00s; `root_guardian_bound.png`, `root_guardian_exposed.png`, `storm_conductor_grounded.png` | `11b2b5d7-a88c-4b74-90b2-8c6cf195313a` |
| Cinder Warden, Star-Eater & Tide Scribe dungeon boss regression (`WildercordDungeonsTest`) | `0d63abfa` | Pass, 2m 28s | `f170838d-2325-4341-bedc-6e1da5a5fbcf` |
| Owned build, native compilation, 2,749 JUnit cases, 9,081 generated paths, 197 guide pages | `3920b5c3` | Pass; no JUnit failures/errors/skips | `e9508d34-d240-40e8-bf9c-cf850a58b05b` |
| Emberwood Reach terrain, Charcoal Yard, Kiln Keeper, and Eleven Winters 3-chapter route | `3920b5c3` | Pass, 3m 03s; `emberwood_reach_terrain.png` + `emberwood_charcoal_yard.png` | `f6a12b04-7dfa-4b66-b4cc-cd8d3be0031f` |
| Full Kiln Widow encounter, Kiln Court chunk replay, Minimal-particle tells, and victory return dialogue | `3920b5c3` | Pass, 3m 21s; 4 encounter screenshots | `347b60af-3d00-4a1c-9812-e55f868f92ee` |
| Cinder Bailiff & Emberwood wildlife ecology regression | `3920b5c3` | Pass, 2m 19s | `48972ce8-41c4-45dc-8198-ef3bbaac3e02` |
| Lore Journal screen, sixteen Master victories, and ordered regional routes | `3920b5c3` | Pass, 2m 04s | `7df2fbe5-f697-4a5d-b75d-06aec16bdf99` |
| Owned build, native compilation, 2,740 JUnit cases, 9,052 generated paths, 195 guide pages | `3778d1eb` | Pass; no JUnit failures/errors/skips | `12e79d1a-6067-41ec-bfa2-1d58b9e10713` |
| Belowkeeper Hollows terrain, Listening Station, Belowkeeper Listener, and Empty Bell 3-chapter route | `3778d1eb` | Pass, 2m 51s; `belowkeeper_hollows_terrain.png` + `belowkeeper_listening_station.png` | `de852d21-9301-4664-8487-8ee4196f1598` |
| Full Hollow Bell encounter, Bell Sanctum chunk replay, Minimal-particle tells, and victory return dialogue | `3778d1eb` | Pass, 3m 01s; 4 encounter screenshots | `3a335b67-98ce-46a0-b5f5-e85f431dbc0a` |
| Lore Journal screen, sixteen Master victories, and ordered regional routes | `3778d1eb` | Pass, 1m 44s | `30b0373e-9130-425f-befe-e84b9d32aaad` |
| Natural Fungal terrain and Glowcap ecosystem regression | `3778d1eb` | Pass, 1m 54s | `e5fb6e34-2e6c-4472-824a-f8be354efe9b` |
| Owned build, native compilation, 2,731 JUnit cases, generated assets, 192 guide pages | `a85023dd` | Pass; no JUnit failures/errors/skips | `7d0cac44-4930-4853-b984-4852b5c2bca2` |
| Windscar Highlands terrain, Highland Cairn Lodge, Cairn Keeper, and Paired Cairns 3-chapter route | `a85023dd` | Pass, 2m 52s; unobstructed `windscar_highlands_terrain.png` + `highland_cairn_lodge.png` | `38bfd658-92a9-459f-a2ed-b4aca4ceaf0b` |
| Full Glass Cartographer encounter, Survey Ring chunk replay, Minimal-particle tells, and victory return dialogue | `a85023dd` | Pass, 3m 33s; 4 encounter screenshots | `9f996124-3ec3-455e-8530-0c4620652799` |
| Lore Journal screen, sixteen Master victories, and Scout's Last Cairn route | `a85023dd` | Pass, 1m 45s | `c75d4e71-7757-4b49-9ff7-2cf7100328cc` |
| Initial Windscar candidate build (2,726 JUnit cases) | `195e4c22` | Pass | `67a759ec-352f-4b80-9681-afc1770f1cfd` |
| Initial Windscar terrain + harvest native check | `195e4c22` | Pass, 1m 58s | `6824ce51-1e4c-468c-a8c4-caa1211ca8f8` |
| Reedlands regression after Windscar climate/surface integration | `195e4c22` | Pass, 3m 9s | `01a81321-d5a9-4785-a227-ef072d654d6b` |
| Owned build, native compilation, 2,721 JUnit cases, 9,005 generated paths, 191 guide pages | `9de25fbe` | Pass; no JUnit failures/errors/skips | `f2753cde-619e-48fc-b264-29dfabe96475` |
| Same build checks for return-dialogue content | `64b0cfbe` | Pass | `d4d1e9ca-88e6-4f80-b571-dcd80d046e62` |
| Reedlands/landing/Drowned Road, including return dialogue | `64b0cfbe` | Pass, 3m 9s | `6212756c-8828-40b3-b560-9c58be1fd66c` |
| Full Ferryman encounter and victory dialogue | `64b0cfbe` | Pass, 3m 25s | `86dbe112-9234-46be-a008-78a6f2727c62` |
| Full unfiltered Arts: fifty arts plus registry/strings and Aura page | `fc403c21` | Pass, 12m 6s; 115 screenshots | `ec4d309d-c8f9-4006-a3f3-377e65658cde` |
| Trail signatures, actual ownership/transactions and reopen | `9de25fbe` | Pass, 1m 39s | `c926245a-49ac-410e-9df0-39f16613b95c` |
| Watchweft history, five original scenarios | `3f0af5fb` | Pass, 2m 44s | `113c6799-bf45-4e1d-ae1d-b70535999236` |
| Complete `masters-march-mechanics` part with its exact seed | `48fb5cc8` | Pass, 8m 36s | `140a81dc-5084-4d5f-a61c-860c4fc8a79e` |
| Next Counter | `ad8b967c054a9492958a3b8b3bccdaf43b48a847` | Pass, 3m 11s | `d0d876e2-4645-4a79-8ff1-e68e62cede96` |
| Next Support | `ad8b967c054a9492958a3b8b3bccdaf43b48a847` | Pass, 1m 38s | `d38dfd8f-0b4d-4e60-bb66-cbfb441ce3b1` |
| Full group 1/4, 86 original classes | `76a1f823` | **Interrupted by owner; incomplete** | `4b900e09-4ead-4654-9006-4cb0eeb096e2` |

Older native results in the audit include Sword Master trial, ordinary planner
(38 scenarios), Kiln (35), articulated Aura shell (36 combinations), lore, Sleeping
Blade, physical magic, Relay, progression and dedicated Excise reconnect. They
occurred at their recorded checkpoints, including before the VPS-only direction.
Do not call them fresh VPS passes on the newest candidate. Development packaging
was verified at an older checkpoint; no current release artifact is approved.

The Reedlands census at `64b0cfbe` found 9,394 regional columns, 2,552 water columns
and six natural Moonreeds in 49 requested chunks. This proves that seed and
bounded sample. It does not prove population density, multi-seed distribution,
unaided survival balance or cooperative play. Registered spawn pools and a
locator-admitted building likewise do not establish natural encounter frequency.

## 6. The deliberately stopped native run

Run `4b900e09-4ead-4654-9006-4cb0eeb096e2` started at
`2026-10-13T19:24:43Z`, source `76a1f823`, full group `1/4`, batch `1791919483`,
attempt `1`, timeout 10,800 seconds. On the owner's explicit pause, the verified
owned runner PID **31496** was stopped at `19:53:43Z`. Its Windows Job Object
closed the descendant native client PID **33372**. Both were verified absent;
the separate live-server PID **24072** was still running. PIDs are historical.

The log contains **63 returned class boundaries**. `BloomstepArrivalCaptureTest`
had started when the run was stopped. This is **not** a group pass, a gameplay
failure, or proof that the old cumulative heap issue is fixed. The original
`result.json` still says `running` because the process was terminated before its
final writer; the neighboring `interruption.json` and absent processes explain
that stale value. Preserve the original result rather than fabricating a pass.

The wrapper preserved the native log, heap diagnostic log and **143 screenshots**
in the receipt. They have been copied locally. Heap-on-OOM diagnostics had been
enabled on the owned JVM without changing its approximately 4 GiB limit; no OOM
dump was produced before this pause. A non-collecting heap-info snapshot near
27 minutes reported about 3.2 GiB used. That is diagnostic context, not a leak
diagnosis. Do not resume a killed process by trusting a saved session/PID.

An earlier superseded startup receipt, `df9ad375-66c5-41d4-840d-7cf9e328e903`, was
also intentionally stopped to include the Watchweft cleanup before this run.
Its interruption record is historical, not another currently active job.

## 7. VPS environment and safe resumption

| Setting | Value |
| --- | --- |
| SSH alias | `pandora-vps` (configured for the owner's machine) |
| Expected Windows computer | `WIN-I2GBVRRD872` |
| Main test checkout | `C:\wildercord-ci\repo`, clean at `76a1f823` when stopped |
| Java | `C:\java\jdk-25.0.4.1+1` |
| Python environment | `C:\wildercord-ci\venv` — Python 3.12, Pillow 12.2, NumPy 2.4.6 |
| Gradle cache | `C:\wildercord-ci\gradle`, Gradle 9.7.1 |
| Live server/world | `C:\wildercord` — excluded from development tests |
| Existing accepted EULA input | `C:\wildercord\eula.txt`; read/hash only, never write acceptance |
| Graphics/audio | Actual AMD OpenGL client; OpenAL null-output mixer supported |

The separate `C:\wildercord-ci\windscar-authoring` worktree is **dirty by design**:
it was used to author generated resources, guide exports and run low-memory
protocol checks while the original native checkout remained fixed. It contains
intermediate source copies and is not the authoritative current candidate. Its
outputs are already incorporated into `57fff8a9`. Inspect it before cleanup; do
not blindly reset it or mistake it for the clean owned test checkout. Temporary
authoring scripts/zips also sit under `C:\wildercord-ci`; they are convenience
artifacts, not the project runner or a live deployment.

After the user explicitly resumes, inspect Git and actual processes first. A
new agent on another machine may not have the SSH alias or credentials; request
that access if missing instead of falling back to GitHub CI or local tests.
No credentials are stored in this handoff.

Run these commands **on the VPS**, after confirming no owned job is active and
the main checkout is clean. Fetch and check out the exact desired commit; use
the actual SHA selected from the development branch, including subsequent fixes.
Do not reset a dirty checkout or update it beneath a native client.

```powershell
Set-Location C:\wildercord-ci\repo
git status --short
git fetch origin defnotean/world-depth-audit
# Inspect the fetched SHA, then: git checkout --detach <exact-candidate-SHA>

$env:JAVA_HOME='C:\java\jdk-25.0.4.1+1'
$env:GRADLE_USER_HOME='C:\wildercord-ci\gradle'
$env:PATH="C:\wildercord-ci\venv\Scripts;$env:JAVA_HOME\bin;$env:PATH"
$env:PYTHONUTF8='1'
$candidate=(git rev-parse HEAD).Trim()

python tools/run_vps_checks.py build --head $candidate --computer WIN-I2GBVRRD872
python tools/run_vps_checks.py focused --head $candidate --computer WIN-I2GBVRRD872 --suite dev.wildercord.world.biome.WindscarBiomeTest --accepted-eula C:\wildercord\eula.txt
# Preserve screenshots before another native run replaces the output directory.
python tools/run_vps_checks.py focused --head $candidate --computer WIN-I2GBVRRD872 --suite dev.wildercord.world.biome.ReedlandBiomeTest --accepted-eula C:\wildercord\eula.txt
```

Execute dependent steps sequentially and stop to diagnose a failure. The above
is a resumption recipe, not permission to start tests while the owner has paused.
The runner's build mode covers build, JUnit, native-source compilation, exact
generator parity and guide validation. It creates a fresh receipt per launch,
verifies a clean exact SHA/computer and refuses inherited filters or fake GitHub
environment. Do not bypass those checks to get a green-looking outcome.

For Windows SSH, an encoded PowerShell command works. Long encoded commands can
exceed Windows' command-line limit; upload a small script instead. Windows
PowerShell 5 can treat ordinary native stderr as an error under
`$ErrorActionPreference='Stop'`. For logged Python subprocesses, redirect stderr
inside Python and propagate the real child exit code. Do not confuse unittest's
normal stderr progress with a failed test or accidentally swallow a real failure.

## 8. Native acceptance contract

Current plan: `full-client-explicit-v8-emberwood`, **349** classes in groups
**87 / 87 / 88 / 87**. Earlier plans (`v4` with 343 classes, `v5` with 344
classes, `v6` with 345 classes, and `v7` with 347 classes) are frozen in
`tools/tests/full-client-plan-v4-343.json`,
`tools/tests/full-client-plan-v5-344.json`,
`tools/tests/full-client-plan-v6-345.json`, and
`tools/tests/full-client-plan-v7-347.json`. `EmberwoodBiomeTest` is appended
to group 3 and `KilnWidowEncounterTest` is appended to group 4; all 347
previous classes keep their exact shard membership and relative order.

Focused native execution requires the exact class's ordered setup/run/cleanup/
returned markers, a fresh launch UUID, exact processed descriptor, successful
owned child exit and build completion. It always leaves full acceptance unverified.
The full gate needs **all four** complete groups from the same source, computer,
batch and attempt, then a passing aggregate. Do not combine the stopped v4 run
with later groups or reuse focused/archived evidence as full-group evidence.

The three required Masters parts remain **44 / 1 / 2** classes, and the articulated
group has six. Their acceptance is separate. Windows full manifests use schema
4 and required Masters manifests schema 2, explicitly `executor: windows-vps`.
The historical `workflowSha` field must equal the actual VPS candidate SHA; it
does not mean GitHub ran the tests. See the VPS guide for exact aggregate flags.

The runner creates a Windows Job Object before launching descendants, closes it
on exit/interruption, shares the checkout lock, bounds evidence/logs and enforces
the declared timeout. Stop only a verified owned runner if interruption is needed.
Never issue a blanket Java kill. A stale `result.json` marked running is not proof
of a live process; use process ownership plus interruption/completion records.

## 9. Exact next work once `sshd` on `pandora-vps` is restarted

1. **Restart `sshd` on `pandora-vps` (`192.168.1.110`)**:
   - Run `Restart-Service sshd` on `WIN-I2GBVRRD872` (`192.168.1.110`) to clear the wedged pre-auth `sshd.exe` child pipes.
2. **Sync `710dc620` (or latest `HEAD`), delete `C:\wildercord-ci\dump.hprof`, and run the 4-shard full-client matrix (`1/4`–`4/4`) on `pandora-vps`**:
   ```powershell
   Remove-Item -Force -ErrorAction SilentlyContinue C:\wildercord-ci\dump.hprof
   Set-Location C:\wildercord-ci\repo
   git fetch origin living-world-expansion
   git checkout --detach origin/living-world-expansion
   git clean -fd -e build/vps-runs

   $env:JAVA_HOME='C:\java\jdk-25.0.4.1+1'
   $env:GRADLE_USER_HOME='C:\wildercord-ci\gradle'
   $env:PATH="C:\wildercord-ci\venv\Scripts;$env:JAVA_HOME\bin;$env:PATH"
   $env:PYTHONUTF8='1'
   $candidate=(git rev-parse HEAD).Trim()
   $batch=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString()

   python tools/run_vps_checks.py build --head $candidate --computer WIN-I2GBVRRD872
   python tools/run_vps_checks.py shard --shard 1/4 --batch $batch --head $candidate --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt --timeout 10800
   python tools/run_vps_checks.py shard --shard 2/4 --batch $batch --head $candidate --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt --timeout 10800
   python tools/run_vps_checks.py shard --shard 3/4 --batch $batch --head $candidate --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt --timeout 10800
   python tools/run_vps_checks.py shard --shard 4/4 --batch $batch --head $candidate --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt --timeout 10800
   ```
3. **Aggregate all four shard manifests with `tools/full_client_ci.py` and record the final receipts.**

## 10. Resolved & remaining risks

- **Resolved (`710dc620`, pending VPS verification once `sshd` restarts)**:
  Both the cumulative `IntegratedServer` heap retention (`~45 MB` per closed world via
  `ServerNetworkingImpl.PLAY.trackedAddons`) and the historical off-thread attachment
  `ConcurrentModificationException` during player save traced to the same root cause:
  `Connection.channelInactive` invoking `ServerPlayNetworkAddon.handleDisconnect()` ->
  `ServerPlayConnectionEvents.DISCONNECT` on the client `Render thread`, where
  `RootCarry.java:91` mutated `ServerPlayer` attachments off-thread and triggered
  `PlayerAttachmentThreadDiagnosticsMixin` (`AssertionError`), skipping
  `AbstractNetworkAddon.endSession()`. `ServerDisconnects` and
  `ExcisePlayerRemovalMixin` now marshal all 59 WilderCord `DISCONNECT` listeners onto
  the owning `MinecraftServer` thread and flush pending off-thread disconnects before
  player saves and shutdown.
- **Resolved (`654b8106` & `0476c86b`, verified in `020fca8f-0345-46bd-8e0a-f88cca13559b` and inside `shard 1/4`)**:
  `SiltcrestPresentationTest` shelter search sweep and reach-only strike refusal
  classification.
- The original Arts failure receipt `94575240-8a03-409b-8850-022bb8c326ed` is retained.
  It exposed random elite conversion in an ordinary fixture. The fixed full pass
  is `ec4d309d-…`; preserve both rather than erasing failed evidence.
- No new release, PR merge or live deployment has been performed. Old downloads
  and public guide pages may therefore differ from the development branch.

For a complete change history, inspect
`git log 0a18358929180f1c4bc5cc4b827f7f92f05571c4..HEAD` and the progress inventory.
Keep this entry point current when work resumes, and clearly separate the new
evidence from this paused snapshot.
