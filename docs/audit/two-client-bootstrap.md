# Actual two-client bootstrap (2026-10-04)

Accepted scoped connection gate on source baseline4b0a4a93 plus the optional native harness. This does not complete Aura, multiplayer balance, authenticated online play, remote human testing or the living-world goal.

## What ran

Two independent Minecraft client JVMs, host PID24508 and peer PID32592, used separate game directories, working directories, offline names and UUIDs. Fabric created a real dedicated server in the host JVM, bound only to127.0.0.1, with capacity2. Host UUIDde9c2fe8-3188-30b6-a721-9ed8940474eb and peer UUID315e5fc2-3894-3190-83b4-ab47825b72ac occupied the same actual server player list. The fixture required distinct loopback TCP remote sockets and positive ports; no fabricated guest entity was used.

Both clients moved through Fabric's held-key input API. Each actual server player moved, and each client's renderer observed the other's changed position. The peer genuinely disconnected, reducing the server player list from2 to1. Both owned JVMs exited0; the supervisor reported PASS in30.88s. The two native screenshots were preserved unchanged and directly viewed. Their stone platform is a supplied movement fixture, not natural-world or Aura presentation evidence.

Artifacts: artifacts/review/two-clients/connection-result.json, screenshot-manifest.json and two original PNGs. Detailed original logs/IPC remain under artifacts/drafts/aura-closure/two-clients/run-input. Those runtime artifacts are not staged.

## Reproduce

Use Java25 and the repository's usual Gradle setup. Serialize with all other Gradle/native/generator operations:

```powershell
.\gradlew.bat -I tools/native/export_two_client_launch.init.gradle exportTwoClientLaunch -PtwoClientSuite -PtwoClientLaunch=artifacts/two-clients-launch.json
python tools/native/launch_two_clients.py artifacts/two-clients-launch.json --output artifacts/two-clients-run --accepted-eula C:/path/to/existing/accepted/eula.txt
```

The output directory must be new. The exporter derives the exact resolved Loom classpath/Java/main/JVM arguments rather than guessing dependency paths. The explicit property selects only this native fixture in generated test resources. It is excluded from the ordinary descriptor and default CI so an unsupervised run cannot wait for a peer.

The launcher copies an existing accepted EULA only when that explicit source file contains eula=true. This run copied the already accepted C:/wildercord/eula.txt from the user's VPS over existing SSH. It did not fabricate acceptance or change the VPS. Server/data/logs are confined to the fresh host and peer directories; the supervisor's finite360s deadline terminates only its own two child processes.

## Startup corrections and limits

The initial dedicated start lacked the accepted EULA file and failed. Fabric's dedicated connect helper uses the configured port: configuring0 bound a real socket but left getPort/connect at0. The fixture now probes an available loopback port, closes that probe, then requests that explicit positive port. A race with another local program can still fail startup; no retry or success is fabricated.

An immediate server-position assertion ran ahead of the peer's packets; it now waits for actual admitted movement under the finite barrier. Direct key state did not move the peer; the mapped Fabric held-key input API passed. No player position writes are used as movement proof. The supplied server teleports only establish the documented starting positions.

Next: actual two-client Unity cooperation/cancellation, foreign-owner interactions, contention, reconnect, death and dimension transitions; full saved-server reopen and representative mixed combat/performance. No credits for those unrun gates.
