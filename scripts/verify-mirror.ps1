<#
.SYNOPSIS
  markleaf-android 미러 백업 검증. 로컬 ref를 GitLab과 대조하고,
  요청 시(GitHub 복구 후) GitHub와도 대조한다.
.DESCRIPTION
  GitHub가 flagged/접근 불가인 동안에는 기본적으로 GitHub를 건드리지 않는다.
  -IncludeGitHub 스위치를 줄 때만 GitHub ref를 읽으며, 접근 불가 시 조용히 건너뛴다.
.EXAMPLE
  pwsh scripts/verify-mirror.ps1
.EXAMPLE
  pwsh scripts/verify-mirror.ps1 -IncludeGitHub
#>
[CmdletBinding()]
param(
    [string]$GitLabRemote = "gitlab",
    [string]$GitHubRemote = "github",
    [switch]$IncludeGitHub
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Get-RemoteRefs([string]$Remote) {
    $lines = & git ls-remote --heads --tags $Remote 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    $map = @{}
    foreach ($line in $lines) {
        $parts = $line -split "\s+"
        if ($parts.Count -ge 2 -and -not $parts[1].EndsWith("^{}")) {
            $map[$parts[1]] = $parts[0]
        }
    }
    return $map
}

function Get-LocalRefs {
    $map = @{}
    foreach ($line in & git show-ref --heads --tags) {
        $parts = $line -split "\s+"
        if ($parts.Count -ge 2) { $map[$parts[1]] = $parts[0] }
    }
    return $map
}

function Compare-RefSet($a, $aName, $b, $bName) {
    $keys = @($a.Keys + $b.Keys | Sort-Object -Unique)
    $diff = 0
    foreach ($k in $keys) {
        $av = if ($a.ContainsKey($k)) { $a[$k] } else { "<missing>" }
        $bv = if ($b.ContainsKey($k)) { $b[$k] } else { "<missing>" }
        if ($av -ne $bv) {
            $diff++
            Write-Host "DIFF $k`n  $aName : $av`n  $bName : $bv" -ForegroundColor Yellow
        }
    }
    return $diff
}

$local  = Get-LocalRefs
$gitlab = Get-RemoteRefs $GitLabRemote
if ($null -eq $gitlab) { Write-Error "GitLab을 '$GitLabRemote' 원격으로 읽을 수 없습니다."; exit 1 }

Write-Host "Local refs: $($local.Count)  |  GitLab refs: $($gitlab.Count)"
$d = Compare-RefSet $local "local" $gitlab "gitlab"
if ($d -eq 0) { Write-Host "OK: GitLab이 로컬과 일치합니다 ($($local.Count) refs)." -ForegroundColor Green }
else          { Write-Host "$d 건의 로컬 vs GitLab 차이." -ForegroundColor Red }

if ($IncludeGitHub) {
    $github = Get-RemoteRefs $GitHubRemote
    if ($null -eq $github) {
        Write-Host "GitHub 접근 불가(아직 flagged?) — 건너뜀." -ForegroundColor DarkYellow
    } else {
        $d2 = Compare-RefSet $github "github" $gitlab "gitlab"
        if ($d2 -eq 0) { Write-Host "OK: GitHub가 GitLab과 일치합니다." -ForegroundColor Green }
        else           { Write-Host "$d2 건의 GitHub vs GitLab 차이." -ForegroundColor Red }
    }
}

if ($d -ne 0) { exit 2 }
