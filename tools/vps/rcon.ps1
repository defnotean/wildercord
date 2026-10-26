# Sends one command to the Wildercord server over RCON and prints its reply. The port and password come from
# server.properties, so no secret lives in this script or in git.
param(
    [Parameter(Mandatory = $true)][string]$Command,
    [string]$Root = 'C:\wildercord'
)
$ErrorActionPreference = 'Stop'

$props = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $Root 'server.properties')) {
    if ($line -match '^\s*([^#=]+?)\s*=(.*)$') { $props[$Matches[1]] = $Matches[2].Trim() }
}
$port = if ($props['rcon.port']) { [int]$props['rcon.port'] } else { 25575 }
$password = $props['rcon.password']
if (-not $password) { throw 'rcon.password is not set in server.properties' }

function Read-RconPacket($sock) {
    $hdr = New-Object byte[] 4
    $null = $sock.Receive($hdr, 4, [System.Net.Sockets.SocketFlags]::None)
    $len = [BitConverter]::ToInt32($hdr, 0)
    $body = New-Object byte[] $len
    $got = 0
    while ($got -lt $len) {
        $n = $sock.Receive($body, $got, $len - $got, [System.Net.Sockets.SocketFlags]::None)
        if ($n -le 0) { break }
        $got += $n
    }
    return , $body
}

function Send-RconPacket($sock, [int]$reqId, [int]$type, [string]$payload) {
    $pl = [System.Text.Encoding]::UTF8.GetBytes($payload)
    $ms = New-Object System.IO.MemoryStream
    $b = [BitConverter]::GetBytes(4 + 4 + $pl.Length + 2); $ms.Write($b, 0, 4)
    $b = [BitConverter]::GetBytes($reqId); $ms.Write($b, 0, 4)
    $b = [BitConverter]::GetBytes($type); $ms.Write($b, 0, 4)
    $ms.Write($pl, 0, $pl.Length)
    $ms.WriteByte(0); $ms.WriteByte(0)
    $sock.Send($ms.ToArray()) | Out-Null
}

$c = New-Object System.Net.Sockets.TcpClient
try {
    $c.Connect('127.0.0.1', $port)
    $s = $c.Client
    $s.ReceiveTimeout = 120000
    Send-RconPacket $s 1 3 $password
    $r = Read-RconPacket $s
    if ([BitConverter]::ToInt32($r, 0) -eq -1) { throw 'RCON auth failed' }
    Send-RconPacket $s 2 2 $Command
    $r = Read-RconPacket $s
    # The body is request id (4 bytes), type (4 bytes), then the text. The old helper skipped 12 bytes and lost
    # the reply's first four characters ("e are 0 of a max ...").
    Write-Output ([System.Text.Encoding]::UTF8.GetString($r, 8, $r.Length - 8).TrimEnd([char]0))
} finally {
    $c.Close()
}
