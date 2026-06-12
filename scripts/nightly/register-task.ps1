<#
.SYNOPSIS
    Register the "Markleaf nightly issue bot" Windows scheduled task.
    Daily at 03:00, ONLY when the PC is already on (a missed run because the PC
    was off/asleep is simply skipped — never caught up later).

.NOTES
    Run once to install. Re-run to update. Use unregister-task.ps1 to remove.
    Requires no admin: it registers under the current user and runs only while
    that user is logged on (so `claude` and `gh` inherit your credentials).
#>
[CmdletBinding()]
param(
    [string] $Time = '03:00',
    [string] $TaskName = 'Markleaf nightly issue bot'
)

$ErrorActionPreference = 'Stop'
$ScriptDir = $PSScriptRoot
$Bot = Join-Path $ScriptDir 'nightly-issue-bot.ps1'
if (-not (Test-Path $Bot)) { throw "Cannot find $Bot" }

# Prefer PowerShell 7 (pwsh) if installed, else Windows PowerShell 5.1.
$pwsh = (Get-Command pwsh -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell -ErrorAction SilentlyContinue).Source }
if (-not $pwsh) { throw "No PowerShell host found." }

$action = New-ScheduledTaskAction `
    -Execute $pwsh `
    -Argument ('-NoProfile -NonInteractive -ExecutionPolicy Bypass -WindowStyle Hidden -File "{0}"' -f $Bot) `
    -WorkingDirectory (Resolve-Path (Join-Path $ScriptDir '..\..')).Path

$trigger = New-ScheduledTaskTrigger -Daily -At $Time

# StartWhenAvailable=$false  -> if the PC was off at 03:00, do NOT run late.
# DisallowStartIfOnBatteries=$false -> still run on a laptop on battery.
# WakeToRun left OFF -> a sleeping PC is treated as "off" and skipped.
$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable:$false `
    -DontStopOnIdleEnd `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2) `
    -MultipleInstances IgnoreNew

# Run only when this user is logged on, in the interactive session, so the
# claude CLI and gh use the same credentials you use by hand. No stored
# password required.
$principal = New-ScheduledTaskPrincipal -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger `
    -Settings $settings -Principal $principal -Force | Out-Null

Write-Host "Registered scheduled task '$TaskName' — daily at $Time (only when the PC is on)."
Write-Host "Host: $pwsh"
Write-Host "Script: $Bot"
Write-Host ""
Write-Host "Test now (safe, no side effects):"
Write-Host "  Start-ScheduledTask -TaskName '$TaskName'   # runs LIVE — only after you've tested -DryRun"
Write-Host "Inspect:  Get-ScheduledTaskInfo -TaskName '$TaskName'"
