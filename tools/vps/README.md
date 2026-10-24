# Live-server ops scripts

The scripts that run the live server on `pandora-vps` (`C:\wildercord`). They used to exist only on the VPS; this
folder is now their source. Change them here, test them on the VPS, then install them.

| Script | Runs | Does |
| --- | --- | --- |
| `backup_world.ps1` | `MinecraftBackup` task, 04:00, as SYSTEM | `save-off`, `save-all flush`, robocopy `/MIR` into the oldest of 3 folder snapshots in `C:\wildercord_backups\snapshots`, `save-on`. Idle priority, unbuffered I/O. Logs `BACKUP_OK` to `logs\backup.log`. |
| `watchdog.ps1` | `WildercordWatchdog` task, every 2 minutes, as SYSTEM | Keeps the server loop and the playit agent alive; calls `apply_update.ps1`. |
| `apply_update.ps1` | from the watchdog | Installs `staging\wildercord-*.jar` once the server is empty, only with a matching `<jar>.sha256`; otherwise moves it to `staging\rejected`. |
| `deploy.ps1` | by hand | Installs a jar now (empty server only): hash check, watchdog paused, clean stop, snapshot, jar swap, health check, watchdog back on. |
| `rcon.ps1` | everything above | One RCON command; port and password come from `server.properties`. |
| `install.ps1` | by hand | Copies the scripts above into `C:\wildercord`, keeping the old ones in `ops_backup\<time>`. |
| `test_ops.ps1` | by hand | Tests against throwaway roots with a fake RCON; `-LiveRcon` adds a read-only `list` to the server. |
| `start-wildercord.bat` | `MinecraftServer` task | Reference copy of the restart loop. Not installed: the running loop re-reads it. |

## Shipping a build

Either stage it for the next empty moment (upload the `.sha256` first; the watchdog ignores a jar for a minute after
it was written):

```powershell
certutil -hashfile wildercord-X+mc26.3.jar SHA256   # or: sha256sum wildercord-X+mc26.3.jar > wildercord-X+mc26.3.jar.sha256
# copy wildercord-X+mc26.3.jar.sha256, then wildercord-X+mc26.3.jar, into C:\wildercord\staging
```

or deploy it now while nobody is on:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:\wildercord\deploy.ps1 -Jar <path to jar> -ExpectedSha256 <hash>
```

## Changing the scripts

```powershell
scp -r tools/vps pandora-vps:C:/wildercord-ci/ops
ssh pandora-vps "powershell -NoProfile -ExecutionPolicy Bypass -File C:\wildercord-ci\ops\test_ops.ps1 -LiveRcon"
ssh pandora-vps "powershell -NoProfile -ExecutionPolicy Bypass -File C:\wildercord-ci\ops\install.ps1"
```

Delete `C:\wildercord-ci\ops` afterwards. Never commit the RCON password; `rcon.ps1` reads it from the server.
