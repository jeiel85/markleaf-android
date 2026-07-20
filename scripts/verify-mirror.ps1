<#
.SYNOPSIS
  markleaf-android 미러 백업 검증. 미러 대상 ref를 로컬·GitLab과 대조하고,
  요청 시 GitHub와도 대조한다.
.DESCRIPTION
  비교 범위는 `refs/heads/main` + `refs/tags/v*`뿐이다
  (docs/mirror-runbook.md "동기화 범위"). 기능 브랜치는 의도적으로 미러하지
  않으므로 양쪽 어디에 있든 비교에서 제외한다 — 포함하면 정상 작업 사본에서도
  항상 차이가 잡혀 게이트가 무의미해진다.

  GitHub가 flagged/접근 불가인 동안에는 기본적으로 GitHub를 건드리지 않는다.
  -IncludeGitHub 스위치를 줄 때만 GitHub ref를 읽는다.

  -MirrorOnly는 로컬 비교를 통째로 건너뛰고 GitHub vs GitLab만 본다. CI 러너처럼
  로컬 ref가 작업 사본이 아니라 체크아웃 설정의 부산물인 환경을 위한 것이다
  (shallow checkout은 태그가 없어 로컬 비교가 항상 어긋난다). GitHub를 읽지 않고는
  미러를 판정할 수 없으므로 -MirrorOnly는 -IncludeGitHub를 함축한다 — 스위치 하나만
  줘도 완결된 호출이다.

  종료 코드:
    0  요청한 검사가 모두 통과
    1  실행 오류 (GitLab ref를 읽을 수 없음)
    2  로컬이 GitLab과 불일치 — 로컬 최신화 필요. 미러 자체는 정상
       (-MirrorOnly에서는 로컬을 보지 않으므로 발생하지 않는다)
    3  GitHub와 GitLab의 미러 ref가 갈라짐 — 이 게이트가 잡으려는 실패
    4  GitHub를 읽지 못해 미러 일치를 판정하지 못함
  둘 이상 해당하면 3 > 4 > 2 순으로 심각한 쪽을 반환한다.
.EXAMPLE
  pwsh scripts/verify-mirror.ps1
.EXAMPLE
  pwsh scripts/verify-mirror.ps1 -IncludeGitHub
.EXAMPLE
  # CI: 로컬 ref가 없는 러너에서 미러 정합성만 본다
  pwsh scripts/verify-mirror.ps1 -MirrorOnly
#>
[CmdletBinding()]
param(
    [string]$GitLabRemote = "gitlab",
    [string]$GitHubRemote = "github",
    [switch]$IncludeGitHub,
    [switch]$MirrorOnly
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# 미러 대상 = 런북 "동기화 범위"의 main + 릴리스 태그. 그 외는 전부 범위 밖이다.
function Test-MirrorRef([string]$Ref) {
    return ($Ref -eq "refs/heads/main") -or ($Ref -like "refs/tags/v*")
}

function Select-MirrorRefs($Map) {
    $out = @{}
    foreach ($k in $Map.Keys) {
        if (Test-MirrorRef $k) { $out[$k] = $Map[$k] }
    }
    return $out
}

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

# 미러를 판정하려면 GitHub를 읽어야 하므로 -MirrorOnly는 -IncludeGitHub를 함축한다.
# (함축하지 않으면 -MirrorOnly 단독 호출이 아무것도 비교하지 않고 0으로 통과한다.)
$checkGitHub = $IncludeGitHub -or $MirrorOnly

$gitlabAll = Get-RemoteRefs $GitLabRemote
if ($null -eq $gitlabAll) {
    Write-Error "GitLab을 '$GitLabRemote' 원격으로 읽을 수 없습니다." -ErrorAction Continue
    exit 1
}
$gitlab = Select-MirrorRefs $gitlabAll

Write-Host "미러 범위: refs/heads/main + refs/tags/v*  (범위 내/전체)"

$localDiff = 0
if ($MirrorOnly) {
    Write-Host "  GitLab $($gitlab.Count)/$($gitlabAll.Count)  |  로컬 비교 생략 (-MirrorOnly)"
} else {
    $localAll = Get-LocalRefs
    $local    = Select-MirrorRefs $localAll
    Write-Host "  로컬 $($local.Count)/$($localAll.Count)  |  GitLab $($gitlab.Count)/$($gitlabAll.Count)"

    $localDiff = Compare-RefSet $local "local" $gitlab "gitlab"
    if ($localDiff -eq 0) { Write-Host "OK: GitLab이 로컬과 일치합니다 ($($local.Count) refs)." -ForegroundColor Green }
    else                  { Write-Host "$localDiff 건의 로컬 vs GitLab 차이 — 로컬 최신화 필요." -ForegroundColor Red }
}

$mirrorDiff    = 0
$mirrorUnknown = $false

if ($checkGitHub) {
    $githubAll = Get-RemoteRefs $GitHubRemote
    if ($null -eq $githubAll) {
        $mirrorUnknown = $true
        Write-Host "GitHub 접근 불가(아직 flagged?) — 미러 일치를 판정하지 못했습니다." -ForegroundColor Red
    } else {
        $github = Select-MirrorRefs $githubAll
        Write-Host "  GitHub $($github.Count)/$($githubAll.Count)"
        $mirrorDiff = Compare-RefSet $github "github" $gitlab "gitlab"
        if ($mirrorDiff -eq 0) { Write-Host "OK: GitHub가 GitLab과 일치합니다 ($($github.Count) refs)." -ForegroundColor Green }
        else                   { Write-Host "$mirrorDiff 건의 GitHub vs GitLab 미러 차이 — 이력이 갈라졌습니다." -ForegroundColor Red }
    }
}

if ($mirrorDiff -ne 0) { exit 3 }
if ($mirrorUnknown)    { exit 4 }
if ($localDiff -ne 0)  { exit 2 }
exit 0
