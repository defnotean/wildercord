# Installs a Wildercord jar waiting in C:\wildercord\staging, but only while nobody is playing, so an
# update never kicks anyone. Called by watchdog.ps1 every 2 minutes; does nothing if staging is empty.
#
# A staged jar must come with <jar>.sha256 holding its SHA-256 (the first 64 hex characters count, so
# `sha256sum` output works). Upload the .sha256 first: a jar without one, or whose hash doesn't match (a partial
# upload, the wrong file), is moved to staging\rejected and never installed. A jar written in the last minute is
# left alone until its upload has surely finished.
param(
    [scriptblock]$Logger = { param($m) Write-Output $m },
    [string]$Root = 'C:\wildercord'
)
$staging = Join-Path $Root 'staging'

# Why a staged jar can't be installed, or $null if it can.
function Get-StagedJarProblem([IO.FileInfo]$jar) {
    $sidecar = "$($jar.FullName).sha256"
    if (-not (Test-Path -LiteralPath $sidecar)) { return 'no .sha256 beside it' }
    $expected = ([string](Get-Content -LiteralPath $sidecar -Raw)).Trim()
    if ($expected -notmatch '^([A-Fa-f0-9]{64})') { return '.sha256 does not start with a SHA-256' }
    $actual = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash
    if ($actual -ne $Matches[1].ToUpperInvariant()) { return "hash $actual does not match .sha256" }
    return $null
}

function Get-ServerJava {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*fabric-server-launch*' }
}

function Install-StagedJar([IO.FileInfo]$staged) {
    $problem = Get-StagedJarProblem $staged
    if ($problem) {
        $rejected = Join-Path $staging 'rejected'
        New-Item -ItemType Directory -Force $rejected | Out-Null
        Move-Item -LiteralPath $staged.FullName -Destination $rejected -Force
        if (Test-Path -LiteralPath "$($staged.FullName).sha256") { Move-Item -LiteralPath "$($staged.FullName).sha256" -Destination $rejected -Force }
        & $Logger "rejected $($staged.Name): $problem"
        return
    }

    $online = $null
    try {
        $out = & (Join-Path $Root 'rcon.ps1') -Command 'list' -Root $Root
        if ("$out" -match 'are (\d+) of a max') { $online = [int]$Matches[1] }
    } catch { }
    if ($online -ne 0) { return }

    & $Logger "server empty: installing $($staged.Name)"
    # Stop the server cleanly (saving the world). Its restart loop waits 10 seconds after it exits, which is
    # when the loop is ended so the server doesn't come straight back on the old jar.
    try { & (Join-Path $Root 'rcon.ps1') -Command 'stop' -Root $Root | Out-Null } catch { }
    for ($i = 0; $i -lt 90; $i++) {
        if (-not (Get-ServerJava)) { break }
        Start-Sleep -Seconds 1
    }
    schtasks /end /tn MinecraftServer | Out-Null
    Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" | Where-Object { $_.CommandLine -like '*start-wildercord*' } |
        ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    $java = Get-ServerJava
    if ($java) {
        & $Logger 'server did not stop within 90 seconds: killing it'
        $java | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        Start-Sleep -Seconds 3
    }

    $old = Join-Path $Root 'old_mods'
    New-Item -ItemType Directory -Force $old | Out-Null
    Get-ChildItem (Join-Path $Root 'mods\wildercord-*.jar') | ForEach-Object { Move-Item $_.FullName $old -Force }
    Move-Item $staged.FullName (Join-Path $Root 'mods') -Force
    Move-Item "$($staged.FullName).sha256" $old -Force
    # Any older jar still waiting would otherwise be installed next time, going back a version.
    Get-ChildItem (Join-Path $staging 'wildercord-*.jar*') -ErrorAction SilentlyContinue | ForEach-Object { Move-Item $_.FullName $old -Force }
    schtasks /run /tn MinecraftServer | Out-Null
    & $Logger "installed $($staged.Name) and restarted the server"
}

$staged = Get-ChildItem (Join-Path $staging 'wildercord-*.jar') -ErrorAction SilentlyContinue |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddMinutes(-1) } |
    Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $staged) { return }

# deploy.ps1 holds this lock while it installs by hand; never install underneath it.
$lock = New-Object System.Threading.Mutex($false, 'Global\WildercordDeploy')
$held = $false
try {
    try { $held = $lock.WaitOne(0) } catch [System.Threading.AbandonedMutexException] { $held = $true }
    if ($held) { Install-StagedJar $staged }
} finally {
    if ($held) { $lock.ReleaseMutex() }
    $lock.Dispose()
}
