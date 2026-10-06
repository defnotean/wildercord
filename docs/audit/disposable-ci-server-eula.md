# Disposable CI dedicated-server startup

The owner explicitly approved recording Minecraft EULA acceptance on 2026-10-06
for disposable CI servers used to test Wildercord. The agreement is the official
[Minecraft EULA](https://www.minecraft.net/en-us/eula). This setup is scoped to
automated test servers; it does not configure a public server or paid service.

The four native-test workflow paths explicitly set
`CI_MINECRAFT_EULA_ACCEPTED=true`. The Fabric test configuration enables its
supported `eula` setting only when that flag and `GITHUB_ACTIONS=true` are both
present. A local run therefore retains its own EULA choice. No production server,
user installation or test assertion writes an acceptance file.

With this setting, Loom's `acceptGameTestEula` task creates the disposable
`build/run/clientGameTest/eula.txt` containing `eula=true`, after the test run
directory is cleared. This is the vendor-supported provisioning mechanism for
in-process dedicated servers. No EULA check is bypassed or patched.

## Observed failure and preserved coverage

At source `3d34b031ed5a3696b79321645f53a716f822c0de`, job `112051018341` in
run `37395703968` entered `HailfallReleasedOwnerTest` at 01:09:23 UTC. Its
dedicated startup reported a missing `eula.txt` and exited at the EULA guard.
Fabric subsequently timed out waiting for the dedicated-server instance after
ten seconds. The original `configureTests` block had `eula = false`.

The pinned Fabric client gametest API 6.0.7+4be74c3f5d starts vanilla
`net.minecraft.server.Main` on another thread of the same JVM. Its setup writes
`server.properties`, not an EULA acceptance file. Vanilla returns before creating
the dedicated server when acceptance is absent, so Fabric's startup future is
never completed. This happens before socket connection or any Hailfall assertion.
The same prerequisite applies to the repository's other `createServer` fixtures.

The native startup messages share the client's normal log and captured workflow
output; this path has no separate child-process startup log. The workflow already
preserves those logs. The original startup timeout, real TCP disconnect/reconnect,
respawn, dimension transitions, damage checks and all acceptance assertions remain
unchanged. A fresh native run is required to establish that the fixture now reaches
and passes them.
