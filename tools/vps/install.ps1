# Copies the ops scripts from this folder onto the live server. Run it on the VPS from a copy of tools/vps:
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File install.ps1
#
# The scripts it replaces are kept in <Root>\ops_backup\<time> first. start-wildercord.bat is never copied:
# the running restart loop re-reads it, so changing it under the loop would break the live server.
param([string]$Root = 'C:\wildercord')
$ErrorActionPreference = 'Stop'
$scripts = @('rcon.ps1', 'backup_world.ps1', 'apply_update.ps1', 'watchdog.ps1', 'deploy.ps1')

$backup = Join-Path $Root ("ops_backup\" + (Get-Date -Format 'yyyyMMdd_HHmmss'))
New-Item -ItemType Directory -Force $backup | Out-Null
foreach ($s in $scripts) {
    $src = Join-Path $PSScriptRoot $s
    $errors = $null
    $null = [System.Management.Automation.Language.Parser]::ParseFile($src, [ref]$null, [ref]$errors)
    if ($errors) { throw "$s does not parse: $($errors[0].Message)" }
}
foreach ($s in $scripts) {
    $dst = Join-Path $Root $s
    if (Test-Path -LiteralPath $dst) { Copy-Item -LiteralPath $dst -Destination $backup }
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $s) -Destination $dst -Force
    $a = (Get-FileHash -LiteralPath (Join-Path $PSScriptRoot $s)).Hash
    if ((Get-FileHash -LiteralPath $dst).Hash -ne $a) { throw "$dst did not copy correctly" }
    Write-Output "installed $s"
}
Write-Output "previous scripts kept in $backup"
