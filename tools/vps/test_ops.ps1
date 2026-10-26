# Tests for the ops scripts, run on the VPS from a copy of tools/vps (never from C:\wildercord):
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File test_ops.ps1
#
# Everything runs against throwaway roots under %TEMP% with a fake rcon.ps1, so nothing reaches the live server,
# its scheduled tasks or its world. The one exception is -LiveRcon, which sends a read-only `list` to the server.
param([switch]$LiveRcon)
$ErrorActionPreference = 'Stop'
$failures = 0

function Check([bool]$ok, [string]$what) {
    if ($ok) { Write-Output "PASS $what" } else { Write-Output "FAIL $what"; $script:failures++ }
}

function New-Root {
    $root = Join-Path $env:TEMP ("wc-ops-test-" + [guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Force "$root\staging", "$root\mods", "$root\logs", "$root\world\region" | Out-Null
    Set-Content "$root\server.properties" "level-name=world`nrcon.port=1`n"
    return $root
}

# A fake rcon.ps1 that records each command and answers `list` with the given player count, or fails (server down).
function Set-FakeRcon([string]$root, [int]$players) {
    $body = if ($players -lt 0) { 'throw "connection refused"' }
            else { "if (`$Command -eq 'list') { 'There are $players of a max of 20 players online: ' } elseif (`$Command -eq 'save-all flush') { 'Saved the game' } else { '' }" }
    Set-Content "$root\rcon.ps1" "param([string]`$Command, [string]`$Root)`nAdd-Content (Join-Path `$Root 'rcon-calls.txt') `$Command`n$body"
}

function New-StagedJar([string]$root, [string]$name, [string]$sidecar, [int]$ageMinutes) {
    $jar = Join-Path $root "staging\$name"
    Set-Content $jar "jar $name $(Get-Random)"
    if ($sidecar -eq 'right') { Set-Content "$jar.sha256" "$((Get-FileHash $jar).Hash.ToLower())  $name" }
    elseif ($sidecar -eq 'wrong') { Set-Content "$jar.sha256" ('0' * 64) }
    (Get-Item $jar).LastWriteTime = (Get-Date).AddMinutes(-$ageMinutes)
    return $jar
}

# Each run is its own process, as under the watchdog, so a lock held here really blocks it.
function Invoke-ApplyUpdate([string]$root) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'apply_update.ps1') -Root $root
}

# 1. Every script parses.
foreach ($f in Get-ChildItem $PSScriptRoot -Filter *.ps1) {
    $errors = $null
    $null = [System.Management.Automation.Language.Parser]::ParseFile($f.FullName, [ref]$null, [ref]$errors)
    Check (-not $errors) "$($f.Name) parses"
}

$roots = @()
try {
    # 2. apply_update: a jar without a .sha256 is rejected, not installed.
    $r = New-Root; $roots += $r; Set-FakeRcon $r 0
    $jar = New-StagedJar $r 'wildercord-9.0.0.jar' 'none' 5
    $out = Invoke-ApplyUpdate $r
    Check ((Test-Path "$r\staging\rejected\wildercord-9.0.0.jar") -and -not (Test-Path $jar)) 'jar without .sha256 is moved to rejected'
    Check ("$out" -match 'no \.sha256') 'rejection says why'
    Check (-not (Test-Path "$r\rcon-calls.txt")) 'rejected jar never contacts the server'

    # 3. apply_update: a jar whose hash doesn't match is rejected along with its .sha256.
    $r = New-Root; $roots += $r; Set-FakeRcon $r 0
    $jar = New-StagedJar $r 'wildercord-9.0.0.jar' 'wrong' 5
    $out = Invoke-ApplyUpdate $r
    Check ((Test-Path "$r\staging\rejected\wildercord-9.0.0.jar.sha256") -and -not (Test-Path $jar)) 'mismatched jar and .sha256 are moved to rejected'
    Check ("$out" -match 'does not match') 'mismatch is logged'

    # 4. apply_update: a jar still being uploaded (written in the last minute) is left alone.
    $r = New-Root; $roots += $r; Set-FakeRcon $r 0
    $jar = New-StagedJar $r 'wildercord-9.0.0.jar' 'none' 0
    $null = Invoke-ApplyUpdate $r
    Check ((Test-Path $jar) -and -not (Test-Path "$r\staging\rejected")) 'fresh jar is not judged yet'

    # 5. apply_update: a good jar waits while players are online.
    $r = New-Root; $roots += $r; Set-FakeRcon $r 3
    $jar = New-StagedJar $r 'wildercord-9.0.0.jar' 'right' 5
    $null = Invoke-ApplyUpdate $r
    $calls = @(Get-Content "$r\rcon-calls.txt" -ErrorAction SilentlyContinue)
    Check ((Test-Path $jar) -and ($calls -join ',') -eq 'list') 'good jar waits while 3 players are online'

    # 6. apply_update: nothing happens while deploy.ps1 holds the lock.
    $r = New-Root; $roots += $r; Set-FakeRcon $r 3
    $jar = New-StagedJar $r 'wildercord-9.0.0.jar' 'wrong' 5
    $lock = New-Object System.Threading.Mutex($false, 'Global\WildercordDeploy')
    $null = $lock.WaitOne(0)
    try { $null = Invoke-ApplyUpdate $r } finally { $lock.ReleaseMutex(); $lock.Dispose() }
    Check ((Test-Path $jar) -and -not (Test-Path "$r\staging\rejected")) 'auto-install waits while a deploy holds the lock'

    # 7. backup_world with the server down: a whole snapshot with the world, its level.dat and server.properties.
    $r = New-Root; $roots += $r; Set-FakeRcon $r -1
    Set-Content "$r\world\level.dat" 'level'
    Set-Content "$r\world\region\r.0.0.mca" 'region one'
    $dest = "$r\snapshots"
    $null = & (Join-Path $PSScriptRoot 'backup_world.ps1') -Root $r -Dest $dest -Keep 2
    $snaps = @(Get-ChildItem $dest -Directory)
    Check ($snaps.Count -eq 1 -and (Test-Path "$($snaps[0].FullName)\world\region\r.0.0.mca") -and (Test-Path "$($snaps[0].FullName)\server.properties")) 'backup copies the world and server files'
    Check ((Get-Content "$r\logs\backup.log" -Raw) -match 'BACKUP_OK .* server=down') 'backup logs BACKUP_OK'

    # 8. backup_world with the server up: flushes, pauses saving, and always turns saving back on.
    Set-FakeRcon $r 0
    Remove-Item "$r\rcon-calls.txt"
    Rename-Item $snaps[0].FullName '20000101_0000'
    $null = & (Join-Path $PSScriptRoot 'backup_world.ps1') -Root $r -Dest $dest -Keep 2
    $calls = @(Get-Content "$r\rcon-calls.txt")
    Check (($calls -join ',') -eq 'list,save-off,save-all flush,save-on') 'backup runs save-off, save-all flush, save-on'

    # 9. Rotation: with Keep=2 the oldest snapshot is refreshed in place, so changed files are mirrored and
    # deleted ones disappear, and a leftover .partial is reused instead of piling up.
    Rename-Item (Get-ChildItem $dest -Directory | Sort-Object Name | Select-Object -Last 1).FullName '20000102_0000'
    Copy-Item "$dest\20000102_0000" "$dest\20000103_0000.partial" -Recurse
    Remove-Item "$r\world\region\r.0.0.mca"
    Set-Content "$r\world\region\r.1.0.mca" 'region two'
    $null = & (Join-Path $PSScriptRoot 'backup_world.ps1') -Root $r -Dest $dest -Keep 2
    $names = @(Get-ChildItem $dest -Directory | Sort-Object Name | ForEach-Object Name)
    $newest = Join-Path $dest $names[-1]
    Check ($names.Count -eq 2 -and $names[0] -eq '20000102_0000' -and $names -notlike '*.partial') "rotation keeps 2 whole snapshots ($($names -join ', '))"
    Check ((Test-Path "$newest\world\region\r.1.0.mca") -and -not (Test-Path "$newest\world\region\r.0.0.mca")) 'refreshed snapshot mirrors the current world'
} finally {
    foreach ($r in $roots) { Remove-Item -LiteralPath $r -Recurse -Force -ErrorAction SilentlyContinue }
}

# 10. The watchdog's status ping finishes the exchange: a fake server answers the status request and
# records whether the client went on to ping. Hanging up early made the real server log a stack trace.
$ast = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'watchdog.ps1'), [ref]$null, [ref]$null)
foreach ($f in $ast.FindAll({ param($n) $n -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -in 'Write-VarInt', 'Read-VarInt', 'Test-Minecraft' }, $false)) {
    . ([scriptblock]::Create($f.Extent.Text))
}
$pinged = Join-Path $env:TEMP "wc-ping-$PID.txt"
$portFile = "$pinged.port"
Remove-Item $pinged, $portFile -ErrorAction SilentlyContinue
$server = Start-Job -ArgumentList $portFile, $pinged -ScriptBlock {
    param($portFile, $pinged)
    $l = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, 0)
    $l.Start()
    Set-Content $portFile $l.LocalEndpoint.Port
    $c = $l.AcceptTcpClient()
    $s = $c.GetStream()
    $s.ReadTimeout = 5000
    function Skip-Packet { $len = 0; $shift = 0; do { $b = $s.ReadByte(); $len = $len -bor (($b -band 0x7F) -shl $shift); $shift += 7 } while ($b -band 0x80); $buf = New-Object byte[] $len; $r = 0; while ($r -lt $len) { $r += $s.Read($buf, $r, $len - $r) }; $buf }
    $null = Skip-Packet
    $null = Skip-Packet
    $json = [System.Text.Encoding]::UTF8.GetBytes('{"version":{"name":"test","protocol":775}}')
    $body = [byte[]](@(0, $json.Length) + $json)
    $s.Write([byte[]](@($body.Length) + $body), 0, $body.Length + 1)
    try {
        $ping = Skip-Packet
        if ($ping[0] -eq 1) { Set-Content $pinged 'ping'; $s.Write([byte[]](@($ping.Length) + $ping), 0, $ping.Length + 1) }
    } catch { }
    Start-Sleep -Milliseconds 200
    $c.Close(); $l.Stop()
}
for ($i = 0; $i -lt 60 -and -not (Test-Path $portFile); $i++) { Start-Sleep -Milliseconds 250 }
$ok = (Test-Path $portFile) -and (Test-Minecraft '127.0.0.1' ([int](Get-Content $portFile)))
$null = Wait-Job $server -Timeout 15
Remove-Job $server -Force
Check ($ok -and (Test-Path $pinged)) "status ping reads the response and pings back (answered=$ok, pinged=$(Test-Path $pinged))"
Remove-Item $pinged, $portFile -ErrorAction SilentlyContinue

# 11. The real rcon.ps1 against the live server: a read-only `list`.
if ($LiveRcon) {
    $out = & (Join-Path $PSScriptRoot 'rcon.ps1') -Command 'list'
    Check ("$out" -match '^There are \d+ of a max') "live rcon list: $out"
    $livePort = [int]((Get-Content 'C:\wildercord\server.properties' | Where-Object { $_ -like 'server-port=*' }) -replace 'server-port=', '')
    Check (Test-Minecraft '127.0.0.1' $livePort) "live status ping on port $livePort"
}

Write-Output "$failures failure(s)"
exit [int]($failures -gt 0)
