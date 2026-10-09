# Native singleplayer close handshake

This is a GameTest-only candidate for the shutdown race in Fabric client gametest
6.0.7+4be74c3f5d with Minecraft 26.3. It has no production descriptor or source hook.
Native acceptance is required before treating it as fixed.

## Evidence and boundary

The public `0aed34e6` Masters run stalled in `RelayLessonChecks.run` at its real
singleplayer resource close, before its dedicated socket reconnect. At 30, 45 and
75 minutes, the test and server were waiting for Fabric's TEST phase while the
render thread waited in `IntegratedServer.halt` for a queued server task.

The pinned bytecode establishes that the render thread runs Fabric's deferred
disconnect after entering TICK. Vanilla `halt(false)` then calls
`executeBlocking(Runnable)` before the disconnect busy-wait containing Fabric's
existing phase pump. If the server already finished its task pass and arrived at
TEST, the render thread cannot reach that barrier. Clearing a screen does not
remove this ordering race.

[Fabric issue #5623](https://github.com/FabricMC/fabric-api/issues/5623), checked on
2026-10-06, independently reports the same cycle for Fabric API 0.161.0+26.3. Its
timed server-hold workaround is not used here.

## Candidate behavior

- The original `TestSingleplayerContextImpl.close()` opens a scope for that exact
  context's server and render thread. Try-with-resources disarms it on every
  returning or throwing path.
- The required single call site is `IntegratedServer.halt(Z)V` invoking
  `IntegratedServer.executeBlocking(Runnable)V`.
- An unrelated call retains the original operation. A matching close can claim
  only one native task and only during TICK. Reentry and a late claimed callback
  fail before another submission.
- The adapter submits the original Runnable once and retains its returned future.
  While incomplete, it invokes Fabric's existing private `Minecraft.postRunTasks`
  through a lower-priority required invoker. That method performs the original
  TEST barrier, client-task dispatch and return to TICK.
- The adapter joins that future without catching or replacing its exception.
  An already reported crash is propagated if the native task cannot finish.
  Vanilla halt, saving, shutdown and Fabric close continue normally afterward.

There is no timeout escape, artificial completion, retried task, copied phase loop,
manual barrier arrival/deregistration, forced server-stop flag or skipped closure.
The bridge is intentionally pinned to Fabric's added method and must resolve at
native mixin application; standalone javac cannot prove that application.

## Required acceptance

`tools/test_native_ci_singleplayer_close.py` compiles and runs the actual pure-JDK
gate with deterministic scheduler controls. These cover the three-party parked
ordering, immediate/delayed/exceptional completion, submission and pump failures,
recorded crashes, reentry, late callback, scope identity, server/thread mismatch,
wrong-phase refusal, and primary/suppressed exception identity. Only the isolated
test process has bounded waits and failure cleanup; the game adapter has neither.
The Python checks also reject production source/descriptor references and changes
to the exact native call boundary.

`NativeSingleplayerCloseChecks.verify` runs at the start of the existing Relay
suite. It requires paused and unpaused worlds to stop their original server
threads, reopen the same actual save with a different server, and retain a world
block and player inventory. The unpaused case closes while a deliberate body
failure unwinds. A receipt requires one completed native task and an inactive
scope. The existing `RelayLessonChecks` then performs its real socket close and
reconnect with its original saved-study assertions.

Required native evidence is both `WILDERCORD_NATIVE_SINGLEPLAYER_CLOSE paused=`
receipts, the `relayLessonAndSocketReconnect=true` receipt, and a successful
original suite result. Source controls, javac and JUnit are not substitutes.

A native fatal-task exception requires a separate expected-failure process and
receipt. That acceptance is pending; no control clears Fabric's global failure
state to make a failing run pass. The deliberate body exception above is caught
by the test itself and never claims that a native process crash recovered.
