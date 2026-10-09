# Bounded native CI self-JVM snapshots (2026-10-05)

The existing external diagnostic helper deliberately follows only the launcher's
verified private process group/session. Gradle 9.7.1's single-use daemon can start a
new session, leaving the actual Minecraft JVM outside that boundary. This change
does not widen process discovery. A helper in the gametest source set observes
only the JVM in which Fabric's pinned client-test runner is already executing.

## Activation and bounds

- Only the exact non-sensitive selectors `CI=true` and the existing `GITHUB_JOB`
  IDs activate snapshots. `masters-native` and `articulated-native` use 30 and 45
  minutes; `game-tests` uses 90 and 150 minutes. Missing or unknown values disable
  the helper. It never enumerates the environment or prints either selector.
- A monotonic clock starts at Fabric runner startup, before the test thread is
  launched. Deadlines do not reset between scenes. Each is attempted once, even
  if collection or writing fails. The current 60/180-minute workflow timeouts
  stay unchanged. These timers do not cover pre-runner startup hangs; unusually
  long startup can also leave less time before the existing job cancellation.
- Each invocation creates at most two files of at most 64 KiB each, inside a
  unique `logs/ci-diagnostics/jvm-*/` directory beneath the native client's run
  directory. Existing workflow evidence uploads already preserve that directory.
  No files are created until a snapshot is due.
- The helper records the captured runner test/render thread IDs first, then at
  most two exact `Server thread` matches. Server discovery inspects metadata
  without stacks for at most 256 sorted self-JVM thread IDs. It captures at most
  four stacks of at most 64 frames each. Other worker stacks are not collected.
- Output contains fixed roles, numeric IDs, thread states, bounded Java symbols
  and line numbers, elapsed time, and the current suite/phase. It omits raw thread
  names, exception messages, lock objects, environment, VM properties, arguments,
  locals, world/entity data and heap contents. It does not launch a process or
  attach to another JVM.

## Lifecycle and interpretation

Fabric client gametest 6.0.7's `start()` hook captures the render-thread ID; its
`lambda$start$0` hook captures the test-thread ID. Existing setup/run/cleanup/end
markers also update immutable diagnostic strings. Normal runner return and the
pinned catch/rethrow path stop the watcher. The latter preserves the original
throwable, including when diagnostic code itself fails with an `Error`.

The watcher is a daemon with no inherited thread-local values or context class
loader. It holds no test entrypoint, client, server, world or entity references.
Cleanup clears its test ID and scene state, interrupts only its own daemon, and
never joins it. A blocked diagnostic write cannot block test teardown or JVM
exit. After the second attempt the watcher also stops itself. There are no
shutdown hooks, changed game timeouts, modified assertions or altered verdicts.

Snapshots are observations, not success/failure evidence. A missing thread may
have ended; a missing snapshot can reflect activation, startup, cancellation,
JVM scheduling or diagnostic I/O limits. This does not establish the cause of
any earlier native CI stall or prove that an actual stalled game can always
service a JVM stack request.

## Verification

`JAVA_HOME=<Java 25 JDK> python -m unittest discover -s tools -p 'test_*ci*.py' -v`
passes 43 tests. The new standalone Java harness exercises the actual helper:
strict activation, both schedules, monotonic wraparound, once-only attempts,
scene transitions, real blocked-thread stacks, unrelated-thread exclusion,
thread/depth/byte/file bounds, ended threads, I/O and Error isolation, original
throwable identity, daemon ownership, no inherited caller state, and immediate
cleanup even when the diagnostic sink ignores interruption. The existing native
launcher/manifest/process-boundary regressions still pass.

The helper and mixin compile with Java 25 against checksum-verified official
Minecraft 26.3/Fabric dependencies. Pinned runner bytecode was inspected for the
startup, normal-return and catch/rethrow injection sites. This focused compile
does not run Loom, the Mixin annotation processor, Minecraft, or native CI.
Independent review and the lead's serialized complete verifier remain separate
acceptance gates.
