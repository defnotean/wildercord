# Installs a Wildercord jar on the live server by hand, now rather than whenever the server is next empty.
# Replaces the per-release deploy-0120.ps1 / deploy-0130.ps1, whose strict process checks failed a healthy 0.13
# deploy and left the watchdog switched off.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File C:\wildercord\deploy.ps1 `
#       -Jar C:\wildercord-deploy\wildercord-0.14.0-alpha+mc26.3.jar -ExpectedSha256 <hash>
#
# It refuses while anyone is online. Steps: check the jar's hash; pause the watchdog; save and stop the server and
# wait for it to exit (it is never killed: if it won't stop, nothing is changed); end the restart loop; take a
# snapshot with backup_world.ps1; swap the jar into mods (the old one goes to old_mods); start the server and wait
# up to 5 minutes for the new build to load and answer RCON. Whatever happens, the watchdog is switched back on
# and a stopped server is started again. Result: logs\deploy.log and logs\deploy-status.json.
param(
    [Parameter(Mandatory = $true)][string]$Jar,
    [Parameter(Mandatory = $true)][string]$ExpectedSha256,
    [string]$Root = 'C:\wildercord',
    [int]$HealthSeconds = 300
)
$ErrorActionPreference = 'Stop'
$log = Join-Path $Root 'logs\deploy.log'
$statusFile = Join-Path $Root 'logs\deploy-status.json'
$rcon = Join-Path $Root 'rcon.ps1'
$name = Split-Path $Jar -Leaf
if ($name -notmatch '^wildercord-(.+)\.jar$') { throw "$name is not a wildercord-<version>.jar" }
$version = $Matches[1]

function Log($message) {
    Add-Content -LiteralPath $log -Value ("{0} {1}" -f (Get-Date -Format s), $message)
    Write-Output $message
}

function Report($state, $message) {
    @{ state = $state; message = $message; version = $version; time = (Get-Date -Format o) } |
        ConvertTo-Json | Set-Content -LiteralPath $statusFile -Encoding UTF8
    Log "$state ${version}: $message"
}

function Rcon([string]$command) {
    try { return [string](& $rcon -Command $command -Root $Root) } catch { return $null }
}

function Get-ServerJava {
    @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*fabric-server-launch*' })
}

function Get-ServerLoop {
    @(Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" | Where-Object { $_.CommandLine -like '*start-wildercord*' })
}

function Get-Sha256([string]$path) { (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash }

# One deploy at a time, and apply_update.ps1 (the watchdog's auto-install) takes the same lock.
$lock = New-Object System.Threading.Mutex($false, 'Global\WildercordDeploy')
try { if (-not $lock.WaitOne(0)) { throw 'another deploy or auto-install is running' } } catch [System.Threading.AbandonedMutexException] { }

$watchdogWasOn = $false
$started = $false
try {
    $expected = $ExpectedSha256.Trim().ToUpperInvariant()
    if ((Get-Sha256 $Jar) -ne $expected) { throw "$Jar does not match the expected SHA-256" }

    $list = Rcon 'list'
    if ($list -and $list -notmatch 'are 0 of a max') { throw "players are online ($list); deploy when the server is empty" }

    $watchdogWasOn = (Get-ScheduledTask -TaskName WildercordWatchdog).State -ne 'Disabled'
    if ($watchdogWasOn) { Disable-ScheduledTask -TaskName WildercordWatchdog | Out-Null }
    Report 'running' 'watchdog paused; stopping the server'

    if (Get-ServerJava) {
        Rcon 'save-all flush' | Out-Null
        Rcon 'stop' | Out-Null
        for ($i = 0; $i -lt 90 -and (Get-ServerJava); $i++) { Start-Sleep -Seconds 1 }
        if (Get-ServerJava) { throw 'the server did not stop within 90 seconds; nothing was changed' }
    }
    schtasks /end /tn MinecraftServer | Out-Null
    Get-ServerLoop | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }

    $backup = & (Join-Path $Root 'backup_world.ps1') -Root $Root | Select-Object -Last 1
    Log "backup: $backup"

    $mods = Join-Path $Root 'mods'
    $old = Join-Path $Root 'old_mods'
    New-Item -ItemType Directory -Force $old | Out-Null
    Get-ChildItem -LiteralPath $mods -Filter 'wildercord-*.jar' | ForEach-Object { Move-Item -LiteralPath $_.FullName -Destination $old -Force }
    Copy-Item -LiteralPath $Jar -Destination $mods
    if ((Get-Sha256 (Join-Path $mods $name)) -ne $expected) { throw "the copy in mods does not match the expected SHA-256" }

    $launched = Get-Date
    schtasks /run /tn MinecraftServer | Out-Null
    $started = $true
    $latest = Join-Path $Root 'logs\latest.log'
    $healthy = $false
    for ($i = 0; $i -lt $HealthSeconds / 5 -and -not $healthy; $i++) {
        Start-Sleep -Seconds 5
        if (-not (Get-ServerJava)) { continue }
        if (-not (Test-Path -LiteralPath $latest) -or (Get-Item -LiteralPath $latest).LastWriteTime -lt $launched) { continue }
        $text = Get-Content -LiteralPath $latest -Raw
        $healthy = $text -like "*wildercord $version*" -and $text -like '*Done (*' -and "$(Rcon 'list')" -match 'are \d+ of a max'
    }
    if (-not $healthy) {
        throw "the server did not load $version and answer RCON within $HealthSeconds seconds; the previous jar is in $old"
    }
    Report 'installed' "loaded and answering RCON after $([int]((Get-Date) - $launched).TotalSeconds)s"
} catch {
    Report 'failed' $_.Exception.Message
    throw
} finally {
    if (-not $started -and -not (Get-ServerJava) -and -not (Get-ServerLoop)) { schtasks /run /tn MinecraftServer | Out-Null }
    if ($watchdogWasOn) { Enable-ScheduledTask -TaskName WildercordWatchdog | Out-Null }
    $lock.ReleaseMutex()
    $lock.Dispose()
}
