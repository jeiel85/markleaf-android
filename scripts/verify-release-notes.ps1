<#
.SYNOPSIS
  릴리스 노트 자산(fastlane changelog, CHANGELOG 두 판)의 정합성을 검증한다.
.DESCRIPTION
  기대 버전은 `app/build.gradle.kts` 의 versionName / versionCode 다. 검사는 세 가지다.

  1. fastlane changelog 존재 — 여섯 스토어 로케일 전부에
     `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt` 가 있어야 한다.
  2. fastlane changelog 길이 — 로케일당 Play Console 상한인 500자 이하여야 한다.
     같은 불변식이 `:app:exportReleaseToBuildDrive` 에도 있지만 그건 릴리스 커밋이
     이미 만들어진 뒤 릴리스 시점에 돈다. de-DE / fr-FR 이 526 / 525자로 작성된 것을
     손으로 세어서야 발견한 적이 있어(#167), 같은 검사를 CI 로 앞당긴다.
  3. CHANGELOG.md ↔ CHANGELOG.ko.md 동기화 — 영어판이 GitHub 릴리즈 노트의 원본이고
     한국어판은 번역이라, 한쪽에만 있는 버전 섹션은 아무 것도 실패시키지 않은 채
     남는다(#167). 버전 섹션의 순서와 날짜가 두 파일에서 같은지, 그리고 이번 버전
     섹션이 양쪽에 있는지 확인한다. 제목은 번역이므로 비교하지 않는다.

  길이는 `writeReleaseArtifacts` 와 같은 방식으로 센다(trim 후 문자 수).
.EXAMPLE
  pwsh scripts/verify-release-notes.ps1
.EXAMPLE
  pwsh scripts/verify-release-notes.ps1 -ExpectedVersion 2.27.0 -ExpectedVersionCode 110
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ExpectedVersion = "",
    [int]$ExpectedVersionCode = 0
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# Play Console 은 릴리스 노트를 로케일당 500자로 제한한다.
# app/build.gradle.kts 의 playLimit 과 같은 값이어야 한다.
$PlayNoteLimit = 500

# 상한의 90%부터는 경고를 낸다(통과는 시킨다). v2.26.2 가 494/492자로 통과한 뒤
# 다음 릴리스의 한 단어 수정이 예고 없이 FAIL 로 뒤집힐 뻔했다(#184) — 여유가
# 얼마 안 남았다는 신호를 상한에 닿기 전에 주기 위한 것이다.
$PlayNoteWarnAt = [int][math]::Floor($PlayNoteLimit * 0.9)

# app/build.gradle.kts 의 noteLocales 와 같은 목록이어야 한다.
$StoreLocales = @("ko-KR", "en-US", "ja-JP", "de-DE", "fr-FR", "es-ES")

function Resolve-UnderRoot([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return (Join-Path $RepoRoot $Path)
}

$fail = 0
function Add-Failure([string]$Message) {
    Write-Host $Message -ForegroundColor Red
    $script:fail++
}

# ---- 기대 버전: app/build.gradle.kts ----
if (-not $ExpectedVersion -or $ExpectedVersionCode -le 0) {
    $gradlePath = Resolve-UnderRoot "app/build.gradle.kts"
    if (-not (Test-Path -LiteralPath $gradlePath)) {
        Write-Error "기대 버전을 읽을 app/build.gradle.kts 를 찾지 못했습니다: $gradlePath"
        exit 1
    }
    $gradleText = Get-Content -Raw -Encoding utf8 -LiteralPath $gradlePath

    if (-not $ExpectedVersion) {
        $nameMatch = [regex]::Match($gradleText, 'versionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"')
        if (-not $nameMatch.Success) {
            Write-Error "app/build.gradle.kts 에서 versionName 을 찾지 못했습니다."
            exit 1
        }
        $ExpectedVersion = $nameMatch.Groups[1].Value
    }
    if ($ExpectedVersionCode -le 0) {
        $codeMatch = [regex]::Match($gradleText, 'versionCode\s*=\s*([0-9]+)')
        if (-not $codeMatch.Success) {
            Write-Error "app/build.gradle.kts 에서 versionCode 를 찾지 못했습니다."
            exit 1
        }
        $ExpectedVersionCode = [int]$codeMatch.Groups[1].Value
    }
}

Write-Host "기대 버전: v$ExpectedVersion (versionCode $ExpectedVersionCode)"

# ---- 1·2. fastlane changelog: 존재 + 500자 상한 ----
Write-Host "`nfastlane changelog ($($StoreLocales.Count)개 로케일, ${ExpectedVersionCode}.txt)"
foreach ($locale in $StoreLocales) {
    $relative = "fastlane/metadata/android/$locale/changelogs/$ExpectedVersionCode.txt"
    $path = Resolve-UnderRoot $relative
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure ("  FAIL  {0,-6} {1} 이(가) 없습니다." -f $locale, $relative)
        continue
    }

    $length = (Get-Content -Raw -Encoding utf8 -LiteralPath $path).Trim().Length
    if ($length -gt $PlayNoteLimit) {
        Add-Failure ("  FAIL  {0,-6} {1,4}자 — Play 상한 {2}자를 {3}자 초과" -f $locale, $length, $PlayNoteLimit, ($length - $PlayNoteLimit))
    } elseif ($length -ge $PlayNoteWarnAt) {
        Write-Host ("  WARN  {0,-6} {1,4}자 / {2} — 여유 {3}자뿐, 다음 수정에서 넘칠 수 있습니다" -f $locale, $length, $PlayNoteLimit, ($PlayNoteLimit - $length)) -ForegroundColor Yellow
    } else {
        Write-Host ("  OK    {0,-6} {1,4}자 / {2}" -f $locale, $length, $PlayNoteLimit) -ForegroundColor Green
    }
}

# ---- 3. CHANGELOG.md ↔ CHANGELOG.ko.md ----
Write-Host "`nCHANGELOG 동기화"
$changelogPaths = [ordered]@{
    'CHANGELOG.md'    = Resolve-UnderRoot "CHANGELOG.md"
    'CHANGELOG.ko.md' = Resolve-UnderRoot "CHANGELOG.ko.md"
}

$sections = [ordered]@{}
$readable = $true
foreach ($name in $changelogPaths.Keys) {
    $path = $changelogPaths[$name]
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure "  FAIL  $name 이(가) 없습니다."
        $readable = $false
        continue
    }
    # 헤딩 형식은 '## vX.Y.Z - 제목 - YYYY-MM-DD' 다(docs/RELEASE.md).
    # 제목은 번역이라 비교하지 않고 버전과 날짜만 뽑는다. 날짜 뒤에 '(Release Failed)'
    # 같은 꼬리표가 붙은 항목이 있어 줄 끝에 앵커를 걸지 않는다.
    $sections[$name] = @(
        @(Get-Content -Encoding utf8 -LiteralPath $path) |
            Where-Object { $_ -match '^## v[0-9]+\.[0-9]+\.[0-9]+' } |
            ForEach-Object {
                [pscustomobject]@{
                    Version = [regex]::Match($_, '^## v([0-9]+\.[0-9]+\.[0-9]+)').Groups[1].Value
                    Date    = $(
                        $dateMatch = [regex]::Match($_, '([0-9]{4}-[0-9]{2}-[0-9]{2})')
                        if ($dateMatch.Success) { $dateMatch.Groups[1].Value } else { "" }
                    )
                }
            }
    )
}

if ($readable) {
    $en = $sections['CHANGELOG.md']
    $ko = $sections['CHANGELOG.ko.md']

    if ($en.Count -ne $ko.Count) {
        $enVersions = @($en | ForEach-Object { $_.Version })
        $koVersions = @($ko | ForEach-Object { $_.Version })
        $enOnly = @($enVersions | Where-Object { $koVersions -notcontains $_ })
        $koOnly = @($koVersions | Where-Object { $enVersions -notcontains $_ })
        Add-Failure "  FAIL  버전 섹션 수가 다릅니다 — CHANGELOG.md $($en.Count)개, CHANGELOG.ko.md $($ko.Count)개."
        if ($enOnly.Count -gt 0) { Add-Failure "        영어판에만: $($enOnly -join ', ')" }
        if ($koOnly.Count -gt 0) { Add-Failure "        한국어판에만: $($koOnly -join ', ')" }
    } else {
        $drift = @()
        for ($i = 0; $i -lt $en.Count; $i++) {
            if ($en[$i].Version -ne $ko[$i].Version) {
                $drift += "#$($i + 1): 영어판 v$($en[$i].Version) / 한국어판 v$($ko[$i].Version)"
            } elseif ($en[$i].Date -ne $ko[$i].Date) {
                $drift += "v$($en[$i].Version): 날짜 영어판 $($en[$i].Date) / 한국어판 $($ko[$i].Date)"
            }
        }
        if ($drift.Count -gt 0) {
            Add-Failure "  FAIL  버전 섹션이 어긋납니다:"
            foreach ($item in $drift) { Add-Failure "        $item" }
        } else {
            Write-Host ("  OK    두 판의 버전 섹션 {0}개가 순서·날짜까지 일치합니다." -f $en.Count) -ForegroundColor Green
        }
    }

    foreach ($name in $changelogPaths.Keys) {
        if (@($sections[$name] | Where-Object { $_.Version -eq $ExpectedVersion }).Count -eq 0) {
            Add-Failure "  FAIL  $name 에 이번 버전(v$ExpectedVersion) 섹션이 없습니다."
        }
    }
}

if ($fail -ne 0) {
    Write-Host "`n실패 $fail 건. 릴리스 노트를 갱신하세요 — 검사를 고치지 마세요." -ForegroundColor Red
    exit 2
}
Write-Host "`n모든 릴리스 노트 검사를 통과했습니다 (v$ExpectedVersion / vc$ExpectedVersionCode)." -ForegroundColor Green
