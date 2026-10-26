# Wildercord watchdog: run every 2 minutes by the WildercordWatchdog scheduled task (as SYSTEM).
# Keeps the Minecraft server and the playit agent running, and checks that players can actually reach
# the server through its public address; if they can't twice in a row, it restarts the agent.
$ErrorActionPreference = 'Continue'
$root = 'C:\wildercord'
$log = Join-Path $root 'logs\watchdog.log'
$stateFile = Join-Path $root 'watchdog-state.json'

function Log($message) {
    Add-Content -Path $log -Value ("{0} {1}" -f (Get-Date -Format 's'), $message)
    # Keep the log short.
    if ((Get-Item $log).Length -gt 1MB) {
        Get-Content $log -Tail 2000 | Set-Content "$log.tmp"
        Move-Item "$log.tmp" $log -Force
    }
}

function Write-VarInt([System.IO.Stream]$s, [int]$v) {
    do {
        $b = $v -band 0x7F
        $v = $v -shr 7
        if ($v -ne 0) { $b = $b -bor 0x80 }
        $s.WriteByte([byte]$b)
    } while ($v -ne 0)
}

function Read-VarInt([System.IO.Stream]$s) {
    $n = 0
    for ($i = 0; $i -lt 5; $i++) {
        $b = $s.ReadByte()
        if ($b -lt 0) { throw 'closed' }
        $n = $n -bor (($b -band 0x7F) -shl (7 * $i))
        if (($b -band 0x80) -eq 0) { return $n }
    }
    throw 'bad varint'
}

# A Minecraft server-list ping: true if a server answers with its status.
function Test-Minecraft([string]$server, [int]$port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $task = $client.ConnectAsync($server, $port)
        if (-not $task.Wait(8000)) { return $false }
        $stream = $client.GetStream()
        $stream.ReadTimeout = 8000
        $name = [System.Text.Encoding]::UTF8.GetBytes($server)
        $packet = New-Object System.IO.MemoryStream
        Write-VarInt $packet 0
        Write-VarInt $packet 775
        Write-VarInt $packet $name.Length
        $packet.Write($name, 0, $name.Length)
        $packet.WriteByte([byte](($port -shr 8) -band 0xFF))
        $packet.WriteByte([byte]($port -band 0xFF))
        Write-VarInt $packet 1
        $bytes = $packet.ToArray()
        Write-VarInt $stream $bytes.Length
        $stream.Write($bytes, 0, $bytes.Length)
        Write-VarInt $stream 1
        Write-VarInt $stream 0
        # Finish the whole status exchange (response, ping, pong). Hanging up after the packet id makes the
        # server log an EncoderException stack trace on every check.
        $length = Read-VarInt $stream
        if ($length -le 0 -or $length -gt 1048576) { return $false }
        $body = New-Object byte[] $length
        $read = 0
        while ($read -lt $length) {
            $n = $stream.Read($body, $read, $length - $read)
            if ($n -le 0) { return $false }
            $read += $n
        }
        if ($body[0] -ne 0) { return $false }
        $ping = New-Object byte[] 10
        $ping[0] = 9
        $ping[1] = 1
        $stream.Write($ping, 0, $ping.Length)
        $null = Read-VarInt $stream
        return (Read-VarInt $stream) -eq 1
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

$state = @{ failures = 0; lastRestart = '2000-01-01T00:00:00' }
if (Test-Path $stateFile) {
    try {
        $saved = Get-Content $stateFile -Raw | ConvertFrom-Json
        $state.failures = [int]$saved.failures
        $state.lastRestart = [string]$saved.lastRestart
    } catch { }
}

# 0. A Wildercord update waiting in staging: installed only while nobody is playing.
& (Join-Path $root 'apply_update.ps1') -Logger ${function:Log}

# 1. The Minecraft server: its task runs a restart loop; make sure the loop itself is alive.
$java = Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*fabric-server-launch*' }
$loop = Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" | Where-Object { $_.CommandLine -like '*start-wildercord*' }
if (-not $java -and -not $loop) {
    Log 'Minecraft server loop not running: starting the MinecraftServer task'
    schtasks /run /tn MinecraftServer | Out-Null
}

# 2. The playit agent's service.
$svc = Get-Service playit -ErrorAction SilentlyContinue
if ($svc -and $svc.Status -ne 'Running') {
    Log "playit service was $($svc.Status): starting it"
    Start-Service playit -ErrorAction SilentlyContinue
}

# 3. Can players reach it? Only worth asking while the server answers locally.
if (Test-Minecraft '127.0.0.1' 25565) {
    $target = $null
    try {
        $srv = Resolve-DnsName -Name '_minecraft._tcp.mc.defnotean.ca' -Type SRV -DnsOnly -ErrorAction Stop | Where-Object { $_.Type -eq 'SRV' } | Select-Object -First 1
        if ($srv) { $target = @($srv.NameTarget, [int]$srv.Port) }
    } catch { }
    if (-not $target) { $target = @('carolyn-chemist.tun.ply.gg', 47092) }
    if (Test-Minecraft $target[0] $target[1]) {
        if ($state.failures -gt 0) { Log "public address $($target[0]):$($target[1]) reachable again" }
        $state.failures = 0
    } else {
        $state.failures++
        Log "public address $($target[0]):$($target[1]) not answering ($($state.failures) in a row)"
        $sinceRestart = (Get-Date) - [datetime]$state.lastRestart
        if ($state.failures -ge 2 -and $sinceRestart.TotalMinutes -ge 10) {
            Log 'restarting the playit agent'
            Restart-Service playit -Force -ErrorAction SilentlyContinue
            $state.lastRestart = (Get-Date -Format 's')
        }
    }
}

$state | ConvertTo-Json | Set-Content $stateFile
