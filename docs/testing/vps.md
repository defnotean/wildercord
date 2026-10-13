# VPS testing

The owner moved all testing from GitHub Actions to the existing Windows VPS on
October 13, 2026. Repository Actions is disabled (`enabled=false`), and existing
builds were cancelled. Do not reenable or dispatch a GitHub workflow. The YAML
under `.github/workflows` records the former setup and its acceptance selections;
retaining it does not mean those jobs are active. Player Guide auto-publication
through Actions is stopped as well.

## Workspace and source

- SSH alias: `pandora-vps` (the user's configured connection).
- Expected computer: `WIN-I2GBVRRD872`.
- Test checkout: `C:\wildercord-ci\repo`.
- Java: `C:\java\jdk-25.0.4.1+1`.
- Python environment: `C:\wildercord-ci\venv` (Python 3.12 and pinned requirements).
- Gradle cache: `C:\wildercord-ci\gradle`.
- Live Minecraft server: `C:\wildercord`; never use its world as a test fixture.

The checkout is detached at the exact candidate SHA. Inspect running jobs and
checkout status before fetching/checking out another candidate. A local Git bundle
can transfer an unpublished committed candidate for testing before its GitHub push.
Do not reset a dirty checkout or replace sources under a running client.

## Build and focused native checks

Run from the test checkout on the VPS after setting these process-local variables:

```powershell
$env:JAVA_HOME='C:\java\jdk-25.0.4.1+1'
$env:GRADLE_USER_HOME='C:\wildercord-ci\gradle'
$env:PATH="C:\wildercord-ci\venv\Scripts;$env:JAVA_HOME\bin;$env:PATH"
$env:PYTHONUTF8='1'
$candidate=(git rev-parse HEAD).Trim()

python tools/run_vps_checks.py build --head $candidate --computer WIN-I2GBVRRD872
python tools/run_vps_checks.py focused --head $candidate --computer WIN-I2GBVRRD872 --suite dev.wildercord.cast.NextCounterTest --accepted-eula C:\wildercord\eula.txt
```

The EULA input must already contain the owner's acceptance. The runner reads it
and records its hash; it does not change that file or impersonate a GitHub CI
environment. No accepted EULA is committed to the repository.

Build mode runs the full build, compiles native test sources, checks generated
asset parity and checks the guide. Its JUnit summary rejects missing reports,
failures, errors and skips. Focused mode accepts exactly one registered class and
requires its ordered setup/run/cleanup/return markers with this run's nonce,
the exact processed descriptor and a successful process/build completion. A skip,
foreign nonce, wrong class or partial log cannot pass. Every result remains
explicitly `fullClientGate: unverified`.

The runner refuses the wrong computer, dirty/wrong candidate, inherited test
filters, injected JVM options and GitHub environment variables. It shares the
existing two-client supervisor's checkout lock. Commands run sequentially with
two Gradle workers. An owned Windows job is created before launching children;
its handle closes when the runner exits or is interrupted, so its descendants
cannot leave an orphan native client behind. This uses the Windows job inheritance
and kill-on-close contract described in [Microsoft's job-object documentation](https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects).

Evidence is retained in a new `artifacts/review/vps/<launch UUID>/` directory:
result JSON, each command's complete log, SHA, process identity, timestamps and
validation issues. The default total deadline is one hour, with an explicit
maximum of three hours. Both failed graphics backends end the run promptly.
OpenAL uses its real null-output mixer when no backend was explicitly configured.
Screenshots remain in the ordinary client output directory; preserve needed
images before the next Gradle native run replaces that directory.

Run the runner's evidence and real process-ownership tests on the VPS:

```powershell
python -m unittest discover -s tools -p test_vps_checks.py -v
```

## Complete native groups

`suite` mode runs each original complete required Masters part or the six-class
`articulated` group. `shard` mode runs one of the four unchanged full-client
groups and enables the required complete animation gallery. Use one positive
batch ID and attempt for every group of the exact candidate being aggregated:

```powershell
$batch=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString()
python tools/run_vps_checks.py suite --suite masters-march-mechanics --batch $batch --head $candidate --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt --timeout 3600
python tools/run_vps_checks.py shard --shard 1/4 --batch $batch --head $candidate --computer WIN-I2GBVRRD872 --accepted-eula C:\wildercord\eula.txt --timeout 10800
```

Each launch retains `manifest.json` alongside its log and result. Full-client
VPS manifests use schema 4; required Masters parts use schema 2. Their explicit
`executor: windows-vps` and `computer` identify the execution provider. The
retained `workflowSha` protocol field identifies the committed test recipe and
must equal the candidate source SHA. No GitHub environment is synthesized.
Every class must complete in this launch's ordered boundaries, with a distinct
launch nonce, matching processed descriptor and actual successful child exit.

The existing `full_client_ci.py` and `masters_required_ci.py` aggregate commands
accept `--executor windows-vps --computer WIN-I2GBVRRD872`. Supply the exact
candidate as both `--head-sha` and `--workflow-sha`, the common `--run-id` batch
and `--run-attempt`, and every retained manifest. The full aggregate additionally
requires `--shards-result success --download-result success`; on the VPS these
mean every required run succeeded and every retained input was retrieved, not
that anything was uploaded to GitHub. Mixed providers, computers, candidates,
batches, attempts, missing/duplicate groups and reused launches cannot pass.
One group by itself still leaves full-client acceptance unverified.

## Acceptance still requiring execution or migration

The focused runner does not replace the four full-client groups, the three
required Masters parts, articulated coverage, connected wide/slim and Moon
matrices, packaging checks or manual visual acceptance. Their rosters, deadlines,
geometry comparisons and evidence requirements remain in their existing tools.
The former Linux launcher remains available as historical tooling; the Windows
runner handles the complete groups above. The paired-matrix alarm still contains
Linux-specific operations. Connected orchestration and a fresh full VPS execution
are still required; do not label a focused pass as their substitute. Moon owner
launches retain an explicit VPS receipt and fresh nonce; migration of the separate
historical image-package provenance validator remains pending. The bounded completed-
evidence readers now work on Windows as well as Linux. Windows opens the reparse
point itself, rejects it, and denies concurrent writers/deletion while reading;
the existing byte limits, stable-file checks and strict JSON validation remain.

The first remote checkpoint at `550fa619` passed build/native compilation,
generated assets and guide validation. That bootstrap run's logs are directly
under `C:\wildercord-ci` and predate this runner.

The runner's seven evidence/ownership tests pass on the actual VPS, including
normal and interrupted process cleanup while an unrelated sibling stays alive.
`NextCounterTest` passes at `ad8b967c` in 3m 11s (including initial native setup),
and `NextSupportTest` passes in 1m 38s. The VPS initializes the AMD OpenGL driver
and real OpenAL null mixer without needing an interactive login.

At `39d05075`, `FerrymanEncounterTest` passes in 2m 47s through the new completed-
evidence reader. The tooling test run has 69 passes and one explicit skip: the
Gradle/Groovy parity test searched the default cache instead of the configured
`GRADLE_USER_HOME`. At `73c34d4a` its independent VPS rerun passes in 15.442s.
The four new evidence-reader tests pass, including actual symlinks and concurrent
writer/deletion refusal. No full native matrix pass is established.

The owned build at `73c34d4a` passes with 2,721 JUnit tests, zero failures/errors/
skips, 9,005 matching generated paths and 191 validated guide pages. The unfiltered
`WildercordArtsTest` fails on that VPS checkpoint: Glacier Mirror/Skate/Winters
Hush failures precede unavailable stage/husk errors in later scenes. The complete
failed log is retained; those gameplay failures are not converted into acceptance.

The full `masters-march-mechanics` selection passes at `48fb5cc8` in 8m 36s,
receipt `140a81dc-5084-4d5f-a61c-860c4fc8a79e`, with a schema-2 passed part manifest,
the actual seed and no verification issues. It alone does not accept the combined
Masters gate. The nine new VPS protocol/child-process tests, sixteen full-client
tests and fifteen required-Masters tests also pass on the VPS, with zero skips.
