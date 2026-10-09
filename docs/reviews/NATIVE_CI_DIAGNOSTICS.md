# Native CI diagnostics

`tools/run_client_ci.py` keeps the existing launch command, test selections, process-group
ownership, graphics-backend failure handling and exit code. The workflow still has 60-minute
focused jobs and 180-minute full-shard jobs. Diagnostics do not stop a slow test or turn an
unverified run into a pass.

A test-mod-only mixin traces the pinned Fabric client-gametest runner (6.0.7+4be74c3f5d,
from Fabric API 0.161.0+26.3). `WILDERCORD_NATIVE_SCENE` JSON records identify the exact
entrypoint at setup, invocation and final-state cleanup, followed by `returned` or `threw`.
The original calls and exception propagation are retained. A `returned` entry can be an
intentional optional skip and is **not** evidence of an assertion pass. `System.nanoTime()`
measures elapsed time from the first scene and from the current scene; no game ticks are needed.
All hooks require exactly one match against the pinned runner and live only in `src/gametest`.
Updating the Fabric dependency requires checking these hook sites again.

A separate launcher thread writes a `WILDERCORD_NATIVE_DIAGNOSTIC` heartbeat every 60 seconds,
including monotonic launcher elapsed time, the latest selected entrypoint and phase, its elapsed
time, and how long child output has been quiet. Before the first scene it reports
`gradle/startup`; after an end marker it reports `between-scenes/gradle`. Missing markers leave
the phase unknown to the launcher, rather than implying that a scene completed. An unchanged
phase or quiet output alone does not prove a deadlock.

The launcher takes at most two snapshots:

| Selection | Snapshot times after launcher start | Existing CI job limit |
| --- | --- | --- |
| Named focused suite | 30 and 45 minutes | 60 minutes |
| Full descriptor or shard | 90 and 150 minutes | 180 minutes |

Each snapshot visits at most four Java descendants. Discovery follows only `/proc` child
links from the launcher-owned process, including its worker threads. Every descendant must
still match its parent, the launcher's private process group/session, and process start identity.
An exited, reused, detached or inaccessible identity is skipped; discovery never broadens to a
global process list. Identity and ownership are checked again immediately before each attach.

The only diagnostic command is the target JDK's `jcmd PID Thread.print -l`. Each invocation is
limited to ten seconds and 256 KiB of output. A timeout, cancellation or size cap stops and reaps
only that diagnostic command. It does not signal or terminate the game or Gradle. Missing
`jcmd`, failed attach and absent owned Java targets are reported as unavailable evidence.
No command lines, environments, VM properties, heap or system-wide process dumps are collected.

Stack files are kept under a unique
`build/run/clientGameTest/logs/ci-diagnostics/run-*/` directory. The existing gameplay/focused
artifact steps already preserve this log tree; no extra jobs, services or artifact replacement
are introduced. Launcher records link each stack file to its threshold, PID, current scene and
attach outcome. The existing manifest remains authoritative for scope and job-result evidence.

## Validation

Run the existing selection/manifest tests and diagnostic regression tests together:

```sh
python -m unittest discover -s tools -p 'test_*ci*.py' -v
```

These exercise descendant/group/session and start-identity checks, nonleader forks, bounded
capture, unavailable diagnostics, monotonic phase tracking, silent child output, unchanged
launch/exit semantics and the rule that scene markers cannot establish a gameplay pass.
Compiling the test-only mixin checks its Java types; a native run is still needed to verify
actual Fabric application and gameplay behavior.
