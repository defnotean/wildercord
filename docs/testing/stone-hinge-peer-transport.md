# Stone Hinge peer transport diagnostic

This is an isolated GameTest diagnostic. It does not enable an ordinary form, charge Aura, add an admission rule, replace a release gate, or prove whole-JVM egress isolation. Native execution of the new 29-case roster remains pending. Fake-socket tests and standalone Java compilation are not native transport acceptance.

## Fixed scope

One invocation exports one reviewed Java 25/Loom launch, then starts three fresh sequential host/peer pairs. At most two 2 GiB Java processes run at once. Every pair has a fresh nonce, IPC directory, listener ports and disposable dedicated server. The host owns the server and struck Survival player; the peer independently observes that current body. Both clients use normal ConnectScreen connections through their assigned literal `127.0.0.1` relay.

| Profile | Owner upstream/downstream | Observer upstream/downstream | Cases |
| --- | --- | --- | --- |
| transparent | 0 / 0 ms | 0 / 0 ms | Existing 15 owner cases, nonzero X/Z retention, natural Master release |
| symmetric | 50 / 50 ms | 50 / 50 ms | Ordinary, right, left, wall, ordinary fall, falling refusal |
| asymmetric | 100 / 25 ms | 25 / 100 ms | Same six cases |

The source contract is `src/gametest/resources/stone-hinge-peer-native-contract.json`. No caller can choose a subset, different delays, arbitrary entrypoint or extra retry. The 1,200-second total includes bounded export/preflight, three profiles of at most 300 seconds, final validation and cleanup. It fits inside the existing `native-diagnostic` job's unchanged 60-minute job and 50-minute native step ceilings. The existing paired 46-case and four-view Moon matrix is unchanged.

## Entry and prerequisites

The existing diagnostic request parser explicitly dispatches this supervisor for the fixed `stone-hinge-peer` case after checking the request's source SHA and current PR head. The generic registered-class selector remains unchanged. Collection independently validates the original evidence and the bounded staged copies; missing exports, partial profiles, stale files or mismatched hashes fail the diagnostic.

The supervisor is `tools/native/run_stone_hinge_peer.py`. It uses the existing accepted-EULA contract, Java/Fabric command validation, runtime fingerprints, supervisor lock and owned-process cleanup. It exports with:

```
./gradlew -I tools/native/export_two_client_launch.init.gradle exportTwoClientLaunch \
  -PtwoClientSuite=stone-hinge-peer -PtwoClientLaunch=<fresh ignored output>/stone-hinge-peer-launch.json \
  --no-daemon --stacktrace --console=plain
```

The fixed supervisor accepts only `--output`, `--total-timeout` (at most 1,200) and the existing local `--accepted-eula` file option. CI additionally requires the existing `native-diagnostic` job, exact checkout/run/attempt/event identity, and the existing explicitly accepted disposable-CI EULA flags. The existing virtual display and software rendering environment must already be supplied by that job. Do not add a service, firewall exception, external relay destination or separate CI job.

## Evidence and refusal conditions

The host publishes `server-ready.properties` from the actual retained OS-bound Netty listener, with the run nonce and owned host PID. Only after validating this receipt does the supervisor open relay backend connections and publish `relay-ready.properties`. Endpoint receipts prove both client-to-relay and relay-to-server socket tuples.

The passive movement chain is:

1. Original `ServerEntity.sendChanges` dispatch of exact encoded owner motion
2. Genuine owner packet application and subsequent `LocalPlayer.sendPosition` send
3. Native server acceptance of that same requested position
4. Original tracker position payload, tied to the accepted owner event
5. Independent peer decoding of matching packet bytes, an actual native interpolation step toward that target, and eventual convergence

Reports bind nonce, profile, case, UUID, entity ID, retained body generation and owned process. They preserve exact packet SHA-256 values and distinguish native quantization from same-wall-clock position equality. Native tick callbacks provide progress evidence; normal peer time synchronization can resynchronize game time and is recorded separately. No test-phase sleeps stand in for transport delay. Existing wound, absorption, vertical motion, fall, synchronization flags, duplicate and refusal assertions remain required.

The relay owns two listeners and at most four connected sockets. It forwards opaque ordered bytes without parsing game packets, fabricating acknowledgements, replaying or dropping content. Every direction has continuous read/write hashes and offsets, bounded FIFO queues, partial-write handling and monotonic residence measurements. Queue caps, connect/EOF/byte-age/backpressure deadlines, lifetime byte caps, wrong endpoints, extra clients, resets or release lateness above 250 ms invalidate the run.

The supervisor refuses stale/mismatched provenance, unreviewed command inputs, malformed or duplicate JSON keys, wrong role/PID/body/nonce, missing or reordered cases, failed/early child exit, changed runtime inputs, missing native chain links, motion correction, missing interpolation, queue/hash/offset mismatch, unclean EOF, deadline exhaustion and cleanup failure. Failure remains failure even when cleanup succeeds. Only retained owned processes are terminated, waited five seconds, killed if needed, and reaped; no process is selected by port.

Publish only root evidence files and the three bounded profile evidence directories. Each profile's logs, IPC and reports are capped at 16 MiB. Exclude `work/**`, which contains disposable game saves, caches and temporary runtime state. The aggregate matrix result remains failed unless all 29 cases, all four byte streams per profile, all six Java exits and cleanup pass.

During live supervision, Java can atomically rename a write-once IPC `.tmp` file after the evidence walk enumerates it. Only a missing temporary name belonging to the active profile's fixed IPC roster may be accounted for through its corresponding published destination, which must already be regular and unlinked. Its bytes and file count are included immediately. Existing temporary files, logs and all other evidence remain bounded; missing destinations, other disappearing paths, symlinks, non-regular files and traversal errors fail with the path and live/final phase. After both owned processes stop and logs close, enumeration and hashing are strict: no missing-entry exception applies, every required witness and exact final IPC name must exist, and the full artifact hash ledger is rechecked before acceptance.

The live IPC directory is opened relative to a retained profile-root descriptor with no-follow, directory-only and close-on-exec flags. Live traversal starts from that retained root descriptor, and every enumerated entry uses its walk directory's no-follow relative stat; it never reopens the root pathname to enumerate names. Both ordinary temporary metadata and the published fallback use this boundary. Root/IPC inode and device identities are rechecked before and after lookup and at scan completion; replacement fails without inspecting an outside destination. The walk and both owned descriptors close on success and every exception. These checks neither retry nor read receipt payloads during live size accounting.

## Verification status

The fixture, transport supervisor and diagnostic dispatcher passed separate source reviews. The integrated `c76bdf42` snapshot compiled all four source sets and passed 1,561 JUnit and 576 Python checks, including the relay, supervisor and collector's adversarial controls. These results do not establish runtime mixin injection, actual delayed Minecraft behavior or the 29-case native pass. Exact native diagnostic evidence remains required.
