<#
.SYNOPSIS
  공개 표면(언어별 랜딩 페이지 + README)의 릴리스 버전 표기가 실제 릴리스 버전과
  일치하는지 검증한다.
.DESCRIPTION
  기대 버전은 `app/build.gradle.kts` 의 versionName 이다. 최신 릴리스 태그가 아니라
  versionName 을 기준으로 삼는 이유는 이 검사가 태그 푸시 "전"에 도는 것이기 때문이다
  (docs/RELEASE.md). 태그는 versionName 에서 파생되므로 릴리스를 준비하는 시점에는
  아직 이전 버전을 가리킨다.

  검사 대상은 두 종류다.

  1. 다국어 랜딩 페이지(docs/index*.html) — "현재 릴리스"를 가리키는 표기가 세 곳 있다:
     JSON-LD 의 softwareVersion, hero 의 release-line, trust-ledger 의 <strong>.
  2. 다국어 README(README*.md) — 릴리스 링크(releases/tag/vX.Y.Z 또는 -/releases/vX.Y.Z)가
     들어 있는 줄. 그 줄 안의 모든 vX.Y.Z 표기를 검사하므로 링크 URL 과 링크 텍스트가
     따로 낡는 경우도 잡힌다. 로드맵의 과거 마일스톤("v1.0.0 stable release" 등)은
     릴리스 링크가 없는 줄이라 대상에서 빠진다.

  스크린샷을 가리키는 hero figcaption 버전은 릴리스 버전과 별개다(스크린샷을 다시 찍기
  전까지 이전 버전으로 남는다). 릴리스 버전과 달라도 정상이지만, 모든 언어 사이에서는
  서로 일치해야 한다(모두 같은 스크린샷을 쓰므로).

  대상 파일이 없거나 버전 표기를 찾지 못하면 실패로 처리한다. 이전 판은 경고만 하고
  넘어간 데다 언어끼리만 비교해서, 모든 언어가 "똑같이" 낡으면 통과했다 — v2.25.0 과
  v2.26.0 두 번 연속으로 누락이 이 검사를 통과했다(#167).
.EXAMPLE
  pwsh scripts/verify-landing-versions.ps1
.EXAMPLE
  pwsh scripts/verify-landing-versions.ps1 -ExpectedVersion 2.27.0
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$DocsDir = "docs",
    [string]$ExpectedVersion = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# 언어 목록과 파일명 규칙은 config/locales.tsv 에서 온다. 이 스크립트만 네 벌을
# 들고 있었고, 그 중 하나라도 빠뜨린 언어는 아무 검사도 받지 않았다(#262).
# 어떤 언어가 어떤 파일을 가져야 하는지는 verify-locales.ps1 이 본다.
. (Join-Path $PSScriptRoot 'locales.ps1')

function Resolve-UnderRoot([string]$Path) {
    if ([System.IO.Path]::IsPathRooted($Path)) { return $Path }
    return (Join-Path $RepoRoot $Path)
}

$fail = 0
function Add-Failure([string]$Message) {
    Write-Host $Message -ForegroundColor Red
    $script:fail++
}

# ---- 기대 버전: app/build.gradle.kts 의 versionName ----
if (-not $ExpectedVersion) {
    $gradlePath = Resolve-UnderRoot "app/build.gradle.kts"
    if (-not (Test-Path -LiteralPath $gradlePath)) {
        Write-Error "기대 버전을 읽을 app/build.gradle.kts 를 찾지 못했습니다: $gradlePath"
        exit 1
    }
    $gradleMatch = [regex]::Match(
        (Get-Content -Raw -Encoding utf8 -LiteralPath $gradlePath),
        'versionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"')
    if (-not $gradleMatch.Success) {
        Write-Error "app/build.gradle.kts 에서 versionName 을 찾지 못했습니다."
        exit 1
    }
    $ExpectedVersion = $gradleMatch.Groups[1].Value
}

Write-Host "기대 버전: v$ExpectedVersion (app/build.gradle.kts versionName)"

# ---- 랜딩 페이지 ----
$docsPath = Resolve-UnderRoot $DocsDir
$locales = Get-MarkleafLocales -RepoRoot $RepoRoot
$landingFiles = @($locales | ForEach-Object { $_.LandingFile })
$releasePatterns = [ordered]@{
    'softwareVersion' = '"softwareVersion":\s*"([0-9]+\.[0-9]+\.[0-9]+)"'
    'release-line'    = 'class="release-line">[^<]*?v([0-9]+\.[0-9]+\.[0-9]+)'
    'trust-ledger'    = '<strong>v([0-9]+\.[0-9]+\.[0-9]+)</strong>'
}

Write-Host "`n랜딩 페이지 ($($landingFiles.Count)개, $DocsDir/)"
$screenshotVersions = @()
foreach ($file in $landingFiles) {
    $path = Join-Path $docsPath $file
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure ("  FAIL  {0,-16} 파일이 없습니다." -f $file)
        continue
    }
    $text = Get-Content -Raw -Encoding utf8 -LiteralPath $path

    $ok = $true
    foreach ($label in $releasePatterns.Keys) {
        $match = [regex]::Match($text, $releasePatterns[$label])
        if (-not $match.Success) {
            Add-Failure ("  FAIL  {0,-16} '{1}' 버전 표기를 찾지 못했습니다." -f $file, $label)
            $ok = $false
        } elseif ($match.Groups[1].Value -ne $ExpectedVersion) {
            Add-Failure ("  FAIL  {0,-16} '{1}' = v{2} (기대 v{3})" -f $file, $label, $match.Groups[1].Value, $ExpectedVersion)
            $ok = $false
        }
    }

    $screenshot = [regex]::Match($text, '<figcaption>[^<]*v([0-9]+\.[0-9]+\.[0-9]+)')
    if (-not $screenshot.Success) {
        Add-Failure ("  FAIL  {0,-16} figcaption 스크린샷 버전 표기를 찾지 못했습니다." -f $file)
        $ok = $false
    } else {
        $screenshotVersions += $screenshot.Groups[1].Value
    }

    if ($ok) {
        Write-Host ("  OK    {0,-16} release=v{1}  screenshot=v{2}" -f $file, $ExpectedVersion, $screenshot.Groups[1].Value) -ForegroundColor Green
    }
}

# ---- README ----
$readmeFiles = @($locales | ForEach-Object { $_.ReadmeFile })
$releaseLinkPattern = '(?:releases/tag/|-/releases/)v[0-9]+\.[0-9]+\.[0-9]+'

Write-Host "`nREADME ($($readmeFiles.Count)개)"
foreach ($file in $readmeFiles) {
    $path = Resolve-UnderRoot $file
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure ("  FAIL  {0,-16} 파일이 없습니다." -f $file)
        continue
    }
    $lines = @(Get-Content -Encoding utf8 -LiteralPath $path)

    $linkLines = 0
    $stale = @()
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -notmatch $releaseLinkPattern) { continue }
        $linkLines++
        foreach ($match in [regex]::Matches($lines[$i], 'v([0-9]+\.[0-9]+\.[0-9]+)')) {
            if ($match.Groups[1].Value -ne $ExpectedVersion) {
                $stale += "L$($i + 1) v$($match.Groups[1].Value)"
            }
        }
    }

    if ($linkLines -eq 0) {
        Add-Failure ("  FAIL  {0,-16} 릴리스 링크(releases/tag/vX.Y.Z)가 있는 줄을 찾지 못했습니다." -f $file)
    } elseif ($stale.Count -gt 0) {
        # 한 줄에 같은 버전이 링크 텍스트와 URL 로 여러 번 나오므로 줄 단위로 묶는다.
        $staleUnique = @($stale | Sort-Object -Unique)
        Add-Failure ("  FAIL  {0,-16} 기대 v{1} 과 다른 표기 -> {2}" -f $file, $ExpectedVersion, ($staleUnique -join ', '))
    } else {
        Write-Host ("  OK    {0,-16} 릴리스 링크 {1}줄 모두 v{2}" -f $file, $linkLines, $ExpectedVersion) -ForegroundColor Green
    }
}

# ---- trust-ledger 의 릴리스 날짜: CHANGELOG 와 일치하는지 ----
#
# 이 자리에는 원래 "Includes Quiet Formatting" 같은 기능 이름이 있었다. 버전 표기
# 세 곳은 검사를 받는데 그 옆의 <span> 은 아무도 보지 않아서, 두 릴리스 지난 기능을
# 모든 언어 페이지에서 계속 광고하고 있었다(#262). 기능 이름은 릴리스마다 낡는 데다
# 언어별로 번역돼 있어 기계가 대조하기 어렵다 — 날짜는 낡지 않고, 값이 모든
# 언어에서 동일하며, CHANGELOG 에 원본이 있다.
Write-Host "`n릴리스 날짜 (trust-ledger <span> vs CHANGELOG)"
$changelogPath = Resolve-UnderRoot "CHANGELOG.md"
$expectedDate = ""
if (-not (Test-Path -LiteralPath $changelogPath)) {
    Add-Failure "  FAIL  CHANGELOG.md 를 찾지 못했습니다."
} else {
    $headingPattern = '(?m)^## v' + [regex]::Escape($ExpectedVersion) + '\b.*?-\s*(\d{4}-\d{2}-\d{2})\s*$'
    $headingMatch = [regex]::Match((Get-Content -Raw -Encoding utf8 -LiteralPath $changelogPath), $headingPattern)
    if (-not $headingMatch.Success) {
        Add-Failure "  FAIL  CHANGELOG.md 에 v$ExpectedVersion 섹션의 날짜가 없습니다."
    } else {
        $expectedDate = $headingMatch.Groups[1].Value
        Write-Host "  기대 날짜: $expectedDate (CHANGELOG.md)"
    }
}
if ($expectedDate) {
    foreach ($file in $landingFiles) {
        $path = Join-Path $docsPath $file
        if (-not (Test-Path -LiteralPath $path)) { continue }  # 위에서 이미 실패로 잡았다
        $text = Get-Content -Raw -Encoding utf8 -LiteralPath $path
        # trust-ledger 블록: <strong>vX.Y.Z</strong> 바로 뒤의 <span>.
        $spanMatch = [regex]::Match($text, '<strong>v[0-9]+\.[0-9]+\.[0-9]+</strong>\s*(?:\r?\n\s*)?<span>(?<span>[^<]*)</span>')
        if (-not $spanMatch.Success) {
            Add-Failure ("  FAIL  {0,-16} 릴리스 <strong> 뒤의 <span> 을 찾지 못했습니다." -f $file)
        } elseif ($spanMatch.Groups['span'].Value -notmatch [regex]::Escape($expectedDate)) {
            Add-Failure ("  FAIL  {0,-16} '{1}' 에 릴리스 날짜 {2} 가 없습니다." -f $file, $spanMatch.Groups['span'].Value.Trim(), $expectedDate)
        } else {
            Write-Host ("  OK    {0,-16} {1}" -f $file, $spanMatch.Groups['span'].Value.Trim()) -ForegroundColor Green
        }
    }
}

# ---- 언어별 데모 클립: 각 표면이 자기 언어 자산을 가리키는지 ----
#
# README.ko.md 가 markleaf-tablet-en.gif 를 가리켜도 렌더링은 멀쩡하므로 조용히
# 나간다 — 버전 표기에서 이 검사가 막아 주는 것과 같은 종류의 구멍이다 (#258).
# 언어 코드는 파일명 규칙에서 끌어온다: index.html/README.md 는 en, 나머지는 접미사.
# 두 표면이 서로 다른 자산을 쓴다. 랜딩은 클릭-투-플레이 `<video>`(mp4 + poster
# webp)로 바뀌었고 — 자동재생 GIF 는 `prefers-reduced-motion` 을 존중할 방법이
# 없고 방문당 ~930KB 였다 — README 는 GitHub 가 마크다운을 그대로 렌더하므로
# `<video>` 를 쓸 수 없어 GIF 그대로다. 그래서 기대 확장자가 표면마다 다르다.
$assetPairs = @()
$assetPairs += @($locales | ForEach-Object { @{ File = "$DocsDir/$($_.LandingFile)"; Lang = $_.Code; Kind = "video" } })
$assetPairs += @($locales | ForEach-Object { @{ File = $_.ReadmeFile; Lang = $_.Code; Kind = "gif" } })

Write-Host "`n데모 클립 ($($assetPairs.Count)개 표면)"
foreach ($pair in $assetPairs) {
    $path = Resolve-UnderRoot $pair.File
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure ("  FAIL  {0,-20} 파일이 없습니다." -f $pair.File)
        continue
    }
    $text = Get-Content -Raw -Encoding utf8 -LiteralPath $path
    $expected = if ($pair.Kind -eq "video") {
        @("markleaf-tablet-$($pair.Lang).mp4", "markleaf-tablet-$($pair.Lang)-still.webp")
    } else {
        @("markleaf-tablet-$($pair.Lang).gif")
    }

    # 이 표면이 참조하는 태블릿 자산을 전부 모아, 자기 언어 것만 있는지 본다.
    $referenced = @(
        [regex]::Matches($text, 'markleaf-tablet-[a-z]{2}(?:-still)?\.(?:gif|mp4|webp)') |
            ForEach-Object { $_.Value } | Sort-Object -Unique
    )

    $missing = @($expected | Where-Object { $referenced -notcontains $_ })
    $unexpected = @($referenced | Where-Object { $expected -notcontains $_ })
    $absent = @($expected | Where-Object { -not (Test-Path -LiteralPath (Join-Path $docsPath "assets/$_")) })

    if ($missing.Count -or $unexpected.Count) {
        $detail = @()
        if ($missing.Count) { $detail += "빠짐: $($missing -join ', ')" }
        if ($unexpected.Count) { $detail += "다른 언어/형식: $($unexpected -join ', ')" }
        Add-Failure ("  FAIL  {0,-20} {1}" -f $pair.File, ($detail -join ' / '))
    } elseif ($absent.Count) {
        # 에셋 자체가 있어야 참조가 의미를 갖는다.
        Add-Failure ("  FAIL  {0,-20} 참조하는데 docs/assets 에 없는 파일: {1}" -f $pair.File, ($absent -join ', '))
    } else {
        Write-Host ("  OK    {0,-20} {1}" -f $pair.File, ($expected -join ' + ')) -ForegroundColor Green
    }
}

# ---- 스크린샷(figcaption): 릴리스 버전과 별개, 언어 간 일치만 확인 ----
$screenshotUnique = @($screenshotVersions | Sort-Object -Unique)
if ($screenshotUnique.Count -le 1) {
    $shown = if ($screenshotUnique.Count -eq 1) { "v$($screenshotUnique[0])" } else { "표기 없음" }
    Write-Host "`nOK: 스크린샷(figcaption) 버전이 $($landingFiles.Count)개 언어에서 일치합니다 ($shown)." -ForegroundColor Green
} else {
    Add-Failure "`nMISMATCH: 스크린샷(figcaption) 버전이 언어 간에 어긋납니다 -> $($screenshotUnique -join ', ')"
}

if ($fail -ne 0) {
    Write-Host "`n실패 $fail 건. 낡은 표기를 갱신하세요 — 검사를 고치지 마세요." -ForegroundColor Red
    exit 2
}
Write-Host "`n모든 공개 표면 버전 검사를 통과했습니다 (v$ExpectedVersion)." -ForegroundColor Green
