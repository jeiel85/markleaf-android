<#
.SYNOPSIS
    Remove the "Markleaf nightly issue bot" scheduled task.
#>
[CmdletBinding()]
param([string] $TaskName = 'Markleaf nightly issue bot')

$ErrorActionPreference = 'Stop'
if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
    Write-Host "Removed scheduled task '$TaskName'."
} else {
    Write-Host "No scheduled task named '$TaskName' found."
}
