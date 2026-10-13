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
| Development branch | `defnotean/world-depth-audit` |
| Review | Draft [PR #2](https://github.com/defnotean/wildercord/pull/2); not merged or released |
| Local workspace | `C:\Users\Eating\Desktop\WilderCord` |
| Work baseline | `0a18358929180f1c4bc5cc4b827f7f92f05571c4` |
| Last content candidate | `a85023dd32b2c9f4037aed81e39ef26f0cc2e7ec`, pushed to GitHub and verified on `pandora-vps` |
| Immediately preceding checkpoint | `195e4c22089260f513c8eb3c746c2e2aa057ff56` (`57fff8a9` + handoff docs) |
| Development goal | **Active** (resumed by owner on 2026-10-13) |
| Scheduled continuation | **ACTIVE**, 15-minute recurring development heartbeat |
| GitHub Actions | Repository-wide **disabled**; build/docs workflows also disabled |
| Owned VPS work | Batch 1 (`BIO-02`) and Batch 2 (`BLD-03`, `NPC-03`, `LOR-03`, `BOS-03`) verified on `WIN-I2GBVRRD872` |
| Live Minecraft server | Left running and unchanged (`PID 24072` in `C:\wildercord`) |

The owner originally asked for a repository-wide audit, a detailed expansion list,
and sustained development of bosses, buildings, NPCs, custom biomes, legends, lore
and polish, with regular GitHub pushes. They required **all future tests on
the VPS, no GitHub CI**, and later explicitly resumed work one batch at a time.

Candidate `a85023dd` completes the connected **Windscar Highlands** slice (`BIO-02`,
`BLD-03` Highland Cairn Lodge & Survey Ring, `NPC-03` Highland Cairn Keeper,
`LOR-03` 3-chapter Paired Cairns / Ridge Watch / Lamp on the Ridge journal route,
and `BOS-03` The Glass Cartographer) and passes the owned VPS build (`2,731` JUnit
tests, asset parity, and guide checks) plus focused native `WindscarBiomeTest`,
`CartographerEncounterTest`, and `LoreJournalGameTest`.

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

## 5. Evidence index — do not merge scopes

These are actual VPS receipts. Full `result.json`, logs and screenshots are
retained remotely in `C:\wildercord-ci\repo\artifacts\review\vps\<UUID>` and copied
locally under `artifacts/review/world-depth/vps-native/<UUID>`. Those large local
artifacts are ignored by Git. A fresh clone has this index, not all raw evidence.

| Check | Candidate | Result | Receipt UUID |
| --- | --- | --- | --- |
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

Current plan: `full-client-explicit-v6-cartographer`, **345** classes in groups
**86 / 86 / 87 / 86**. Earlier plans (`v4` with 343 classes and `v5` with 344
classes) are frozen in `tools/tests/full-client-plan-v4-343.json` and
`tools/tests/full-client-plan-v5-344.json`. `CartographerEncounterTest` is
appended to group 4; the three earlier groups and every original class's
order/membership are preserved.

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

## 9. Exact next work, when authorized

1. **Batch 3 — Belowkeeper Hollows Slice (`BIO-04` + `BLD-02` + `NPC-02` + `LOR-02` + `BOS-01`):**
   Implement the underground **Belowkeeper Hollows** custom biome (`BIO-04`),
   **Belowkeeper Listening Station** (`BLD-02`), **Belowkeeper Listener** resident
   and acoustic survey trade/dialogue (`NPC-02`), 3-chapter Belowkeeper journal
   investigation (`LOR-02`), and **The Hollow Bell** optional resonance encounter
   (`BOS-01`), then verify on `pandora-vps`.
2. **Batch 4 — Emberwood Reach Slice (`BIO-03` + `BLD-04` + `NPC-04` + `LOR-04` + `BOS-04`):**
   Implement **Emberwood Reach** (`BIO-03`), **Emberwood Charcoal Yard** (`BLD-04`),
   Kiln Keeper resident (`NPC-04`), Emberwood journal route (`LOR-04`), and **The
   Kiln Widow** (`BOS-04`), then verify on `pandora-vps`.
3. **Batch 5 — Return Visits, Progression, and Boss/Site Polish (`BLD-05`, `NPC-01`, `POL-03..08`, `BOS-05`).**
4. **Batch 6 — Full Native Gate & Paired Matrix Verification on `pandora-vps`.**

Keep the user's preference for substantial connected detail: useful residents,
clear encounter tells, return visits, meaningful resources, persistence and lore
that points to something a player can actually do. Read the canon before adding
named people. Do not duplicate already implemented Root Guardian/Storm Conductor
content. Do not mark this broad project finished because a milestone or budget ends.

## 10. Known unresolved risks

- A former long group exhausted an approximately 4 GiB heap near
  `LifeOutcomeEdgesTest` after roughly 38–40 minutes. Concrete fixture references
  were cleaned up, including Watchweft before that point. Some other cleaned
  fixtures run later or in other groups and cannot explain that original failure.
  The paused rerun is inconclusive. Capture actual retention evidence before
  claiming a root cause or raising the heap merely to hide it.
- Intermittent Siltcrest shelter/return observations and a broader historical
  attachment-save concurrent-modification issue remain open. The Excise lifecycle
  repair and focused ecology passes are real but narrower than those broad issues.
- The original Arts failure receipt `94575240-8a03-409b-8850-022bb8c326ed` is retained.
  It exposed random elite conversion in an ordinary fixture. The fixed full pass
  is `ec4d309d-…`; preserve both rather than erasing failed evidence.
- Windscar's chosen natural seed, census, first harvest foothold and screenshot
  have not been observed yet. They are explicit pending acceptance, not claims
  supported by the earlier Reedlands images.
- No new release, PR merge or live deployment has been performed. Old downloads
  and public guide pages may therefore differ from the development branch.

For a complete change history, inspect
`git log 0a18358929180f1c4bc5cc4b827f7f92f05571c4..HEAD` and the progress inventory.
Keep this entry point current when work resumes, and clearly separate the new
evidence from this paused snapshot.
