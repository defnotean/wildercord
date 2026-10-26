# Nightly world backup, run at 04:00 by the MinecraftBackup scheduled task (as SYSTEM).
#
# The old version zipped the ~13 GB world with Compress-Archive every night. That starved the server for long enough
# that vanilla's tick watchdog (max-tick-time) killed it between 04:03 and 04:56, every night from Oct 11. This one:
#   - flushes the world first (save-off, then save-all flush), so the copy has everything up to now;
#   - keeps $Keep plain folder snapshots and refreshes the oldest one in place with robocopy /MIR, so a night copies
#     only the region files that changed since that snapshot, not the whole world;
#   - runs at Idle priority (and so background I/O priority) with unbuffered copies (/J), so the server's thread and
#     the file cache are left alone.
# A snapshot is written as <stamp>.partial and renamed when complete, so a folder without the suffix is a whole one.
param(
    [string]$Root = 'C:\wildercord',
    [string]$Dest = 'C:\wildercord_backups\snapshots',
    [ValidateRange(1, 30)][int]$Keep = 3
)
$ErrorActionPreference = 'Stop'
(Get-Process -Id $PID).PriorityClass = 'Idle'
$log = Join-Path $Root 'logs\backup.log'
$rcon = Join-Path $Root 'rcon.ps1'

function Log($message) {
    Add-Content -LiteralPath $log -Value ("{0} {1}" -f (Get-Date -Format s), $message)
    Write-Output $message
}

function Rcon([string]$command) {
    try { return [string](& $rcon -Command $command -Root $Root) } catch { return $null }
}

$level = 'world'
foreach ($line in Get-Content -LiteralPath (Join-Path $Root 'server.properties')) {
    if ($line -match '^level-name=(.+)$') { $level = $Matches[1].Trim() }
}
$worlds = @($level, "${level}_nether", "${level}_the_end")
$files = @('server.properties', 'ops.json', 'whitelist.json', 'banned-players.json', 'banned-ips.json', 'usercache.json')

New-Item -ItemType Directory -Force -Path $Dest | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd_HHmm'
$target = Join-Path $Dest "$stamp.partial"
$started = Get-Date

# Reuse the oldest snapshot (or a partial one left by a failed night) once there are enough, so the mirror only
# has to copy what changed. Anything older than that is removed.
$existing = @(Get-ChildItem -LiteralPath $Dest -Directory | Sort-Object Name)
$partial = @($existing | Where-Object Name -like '*.partial')
$whole = @($existing | Where-Object Name -notlike '*.partial')
$older = @($whole | Select-Object -SkipLast ($Keep - 1))
$reuse = if ($partial) { $partial[0] } elseif ($older) { $older[0] } else { $null }
foreach ($old in $partial + $older) {
    if ($old.FullName -ne $reuse.FullName) { Remove-Item -LiteralPath $old.FullName -Recurse -Force }
}
if ($reuse) { Rename-Item -LiteralPath $reuse.FullName -NewName (Split-Path $target -Leaf) }
New-Item -ItemType Directory -Force -Path $target | Out-Null

$online = $null -ne (Rcon 'list')
try {
    if ($online) {
        Rcon 'save-off' | Out-Null
        $saved = Rcon 'save-all flush'
        if ("$saved" -notmatch 'Saved the game') { Log "warning: save-all flush replied '$saved'" }
    }
    foreach ($w in $worlds) {
        $src = Join-Path $Root $w
        $dst = Join-Path $target $w
        if (Test-Path -LiteralPath $src) {
            robocopy $src $dst /MIR /J /XF session.lock /R:2 /W:5 /NFL /NDL /NJH /NJS /NP | Out-Null
            if ($LASTEXITCODE -ge 8) { throw "robocopy failed for $src (exit $LASTEXITCODE)" }
        } elseif (Test-Path -LiteralPath $dst) {
            Remove-Item -LiteralPath $dst -Recurse -Force
        }
    }
    foreach ($f in $files) {
        $p = Join-Path $Root $f
        if (Test-Path -LiteralPath $p) { Copy-Item -LiteralPath $p -Destination $target -Force }
    }
} finally {
    if ($online) { Rcon 'save-on' | Out-Null }
}

if (-not (Test-Path -LiteralPath (Join-Path $target "$level\level.dat"))) { throw "snapshot has no $level\level.dat" }
$final = Join-Path $Dest $stamp
Rename-Item -LiteralPath $target -NewName $stamp
$size = (Get-ChildItem -LiteralPath $final -Recurse -File | Measure-Object Length -Sum).Sum
Log ("BACKUP_OK {0} bytes={1} seconds={2:N0} server={3}" -f $final, $size, ((Get-Date) - $started).TotalSeconds, $(if ($online) { 'up' } else { 'down' }))
