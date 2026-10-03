# Verified server update: 0.9.1-alpha.1

On 2026-10-03 the user requested the server update while players left temporarily. An SSH/RCON check confirmed zero players before starting the existing WildercordUpdate091 scheduled task. Its second empty-server check passed; the server flushed and stopped cleanly before the offline backup.

The task reported installed at 2026-10-03T13:23:53-07:00. Backup: `C:\wildercord_backups\pre-0911-20261003-132206`. Installed file: `C:\wildercord\mods\wildercord-0.9.1-alpha.1+mc26.3.jar`.

SHA256: `cdd7741d6a48f4a12cd2d1f04e9b61e7b202ed68f024b42fb297020d1283fd1a`, independently checked on the installed JAR and matching the published GitHub release asset. The loader identified `wildercord 0.9.1-alpha.1+mc26.3`; startup logged Done at13:23:52; RCON responded and reported one connected player afterward. The one-time update task removed itself after its health checks.

This is the published release, not the later Earth/fieldcraft source commit or the currently uncommitted Life/six-signature/Glowcap work. Those newer milestones remain separate review/release work.

A Netty disconnect-packet encoder error appeared after startup; RCON and a connected player remained healthy. This observation does not establish the origin of that individual connection error or prove multi-client gameplay compatibility. No forced Java termination was used.
