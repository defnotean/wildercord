param(
    [Parameter(Mandatory=$true)][ValidateRange(1,2147483647)][int]$GameProcessId,
    [ValidateRange(10,600)][int]$Seconds=60,
    [string]$JdkDirectory='C:\Program Files\Java\jdk-25'
)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$process=Get-Process -Id $GameProcessId
if($process.ProcessName -notin @('java','javaw')){throw 'Choose the Minecraft Java process id.'}
$outputDirectory=Join-Path $taskRoot 'artifacts/profiling'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$capture=Join-Path $outputDirectory ('gameplay-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.jfr')
$diagnosticTool=Join-Path $JdkDirectory 'bin/jcmd.exe'
& $diagnosticTool $GameProcessId JFR.start 'name=wildercord_gameplay' 'settings=profile' "duration=${Seconds}s" ('filename="{0}"' -f $capture)
if($LASTEXITCODE -ne 0){throw 'Java Flight Recorder could not start.'}
Write-Output "Recording locally for $Seconds seconds. Reproduce the target gameplay scene now."
Write-Output "Capture: $capture"
