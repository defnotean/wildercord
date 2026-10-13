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
| Last content candidate | `57fff8a91d6df3bc9c5c49e252a7617212a543b7`, pushed to GitHub |
| Immediately preceding checkpoint | `76a1f82330df245189cb5c63c6203dce87c2d589` |
| Development goal | **Paused by the owner**, not completed |
| Scheduled continuation | **PAUSED**, `continue-wildercord-world-expansion-on-vps` |
| GitHub Actions | Repository-wide **disabled**; build/docs workflows also disabled |
| Owned VPS work | Stopped; runner/native test PIDs absent and zero task runners verified |
| Live Minecraft server | Left running and unchanged |

The owner originally asked for a repository-wide audit, a detailed expansion list,
and sustained development of bosses, buildings, NPCs, custom biomes, legends, lore
and polish, with regular GitHub pushes. They then required **all future tests on
the VPS, no GitHub CI**, and a thorough work inventory. Their latest instruction
is to **stop work and leave substantial project context**. Do not resume because
of the older continuous-work request; wait for a new request to continue.

The last content commit contains an implemented Windscar candidate and its tests.
It is saved for review, but **its build, new JUnit cases, native compilation,
generator parity and actual Windscar gameplay checks are still pending**. The
docs added with this handoff do not turn that candidate into a verified build.

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
| `world/sites/{farm,masters,wilds,travel,water,mine}/` | Site registration, procedural pieces, authentic markers, loot and discovery |
| `world/ferryman/` | Reed-Crowned Ferryman rules, encounter state and content registration |
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

### Windscar candidate — saved, runtime verification pending

`57fff8a9` adds **Windscar Highlands**, using the dry windswept-hill humidity band
at or below -0.25. It preserves all other climate coordinates and other biome
entries. The surface adapter inherits highland stone; data retains ordered
vanilla features and ores with sparse trees and an authored landscape palette.

Existing Windreed decoration, Stonehorns, Galeclaws, Tussock Pikas and Skyrays
include the region with their original habitat/rarity/config limits. Watchtowers,
standing stones and Wind Gate tags admit it. The owner receives a persistent
Marchkeeper field note. The wiki has a new region chapter and connected guides.

Windscar retains Summit Wind at height 200 or higher, sharing `peaks`' daily rest.
Reedlands now retains rain-only Mire, sharing `swamp`'s rest. Neither creates a
second daily allowance merely by crossing a regional boundary.

Added tests: four `WindscarClimateTest` methods, one regional case in
`WorldRunesTest`, and the full `WindscarBiomeTest`. The native test declares its
fixed seed, 8,192-block noise search and 81-chunk census before measurement. It
requires actual region selection, stone/grass/elevation, natural Windreed, a
Survival client harvest, no repeat yield, spectator refusal, owner sync and a
saved-world reopen. It has not yet executed. Do not enlarge its census or weaken
assertions simply because a first run fails; diagnose the actual generation.

VPS authoring evidence for this candidate: 17 explicit-plan tests, 16 full-protocol
tests including executable Groovy parity, 28 client-protocol tests and 15 Masters
protocol tests pass (**76**, no skips). The full asset generator completed and
**192** guide pages validated/exported. This was a separate authoring checkout,
not an owned candidate build. Five new Java tests, native compilation, exact
asset parity, actual terrain, screenshots and the Reedlands regression are pending.

## 5. Evidence index — do not merge scopes

These are actual VPS receipts. Full `result.json`, logs and screenshots are
retained remotely in `C:\wildercord-ci\repo\artifacts\review\vps\<UUID>` and copied
locally under `artifacts/review/world-depth/vps-native/<UUID>`. Those large local
artifacts are ignored by Git. A fresh clone has this index, not all raw evidence.

| Check | Candidate | Result | Receipt UUID |
| --- | --- | --- | --- |
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

Current plan: `full-client-explicit-v5-windscar`, **344** classes in groups
**86 / 86 / 87 / 85**. The earlier v4 plan has 343 classes and is frozen in
`tools/tests/full-client-plan-v4-343.json`. Windscar is appended to group 4; the
three earlier groups and every original class's order/membership are preserved.

Focused native execution requires the exact class's ordered setup/run/cleanup/
returned markers, a fresh launch UUID, exact processed descriptor, successful
owned child exit and build completion. It always leaves full acceptance unverified.
The full gate needs **all four** complete groups from the same source, computer,
batch and attempt, then a passing aggregate. Do not combine the stopped v4 run
with later v5 groups or reuse focused/archived evidence as full-group evidence.

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

1. **Validate the Windscar candidate.** Run the owned build and repair real
   compilation/parity failures. Run `WindscarBiomeTest`, inspect the actual
   terrain screenshot and acquisition outcome, then rerun Reedlands because the
   shared placement/surface hooks and attunement integration changed. Record
   source/receipt/counts honestly and update the progress report and draft PR.
2. **Finish native reliability and complete coverage.** The stopped first group
   did not reach the previous heap-failure point. Investigate cumulative heap
   exhaustion, then perform fresh complete groups at one chosen exact candidate.
   Do not keep restarting expensive runs for incidental documentation edits.
3. **Finish connected VPS verification.** Wide/slim counter and Moon matrices
   need fresh native VPS execution. `tools/native/run_paired_matrix.py` still has
   POSIX-specific alarm behavior; migrate that ownership/deadline path without
   weakening geometry, identity, frame, lifecycle or timeout checks. Existing
   Windows evidence readers and standalone owned launch support are already in.
4. **Continue the connected regional roadmap.** Windscar still needs measured
   traversal, multi-seed distribution, regional investigation, Cairn Lodge and
   the proposed Glass Cartographer. Named Iona/Tamsin services, Belowkeeper and
   Emberwood biomes, Hollow Bell/Kiln Widow and related settlements are future
   work. Existing ecosystems do not count as newly implemented custom biomes.
5. **Measure the whole experience.** Natural fauna/structure frequency, actual
   two-client attribution/rewards, unaided survival journeys, supported visual
   configurations, old-world safety and sustained performance remain open.

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
