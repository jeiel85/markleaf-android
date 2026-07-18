<#
.SYNOPSIS
  다국어 랜딩 페이지(docs/index*.html)의 릴리스 버전 표기가 언어 간에 일치하는지 검증한다.
.DESCRIPTION
  각 언어 랜딩 페이지에는 "현재 릴리스"를 가리키는 버전 표기가 세 곳 있다:
  JSON-LD 의 softwareVersion, hero 의 release-line, trust-ledger 의 <strong>.
  이 세 값은 여섯 언어(en/ko/ja/de/es/fr) 전부에서 동일해야 한다.

  스크린샷을 가리키는 hero figcaption 버전은 릴리스 버전과 별개다(스크린샷을
  다시 찍기 전까지 이전 버전으로 남는다). 릴리스 버전과 달라도 정상이지만,
  여섯 언어 사이에서는 서로 일치해야 한다(모두 같은 스크린샷을 쓰므로).

  어느 그룹이든 언어 간에 어긋나면 어긋난 값을 보고하고 exit 2 로 종료한다.
  릴리스 직전(태그 푸시 전)에 실행해 언어별 버전 표기 누락을 잡는다.
.EXAMPLE
  pwsh scripts/verify-landing-versions.ps1
.EXAMPLE
  pwsh scripts/verify-landing-versions.ps1 -DocsDir docs
#>
[CmdletBinding()]
param(
    [string]$DocsDir = "docs"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$files = @("index.html", "index.ko.html", "index.ja.html", "index.de.html", "index.es.html", "index.fr.html")

function Get-Version([string]$Text, [string]$Pattern, [string]$Label, [string]$File) {
    $m = [regex]::Match($Text, $Pattern)
    if (-not $m.Success) {
        Write-Host "WARN  ${File}: '$Label' 버전 표기를 찾지 못했습니다." -ForegroundColor Yellow
        return $null
    }
    return $m.Groups[1].Value
}

$rows = @()
$missingFiles = 0
foreach ($f in $files) {
    $path = Join-Path $DocsDir $f
    if (-not (Test-Path -LiteralPath $path)) {
        Write-Host "WARN  $path 이(가) 없습니다 — 건너뜀." -ForegroundColor Yellow
        $missingFiles++
        continue
    }
    $text = Get-Content -Raw -Encoding utf8 -LiteralPath $path

    $rows += [pscustomobject]@{
        File            = $f
        SoftwareVersion = Get-Version $text '"softwareVersion":\s*"([0-9]+\.[0-9]+\.[0-9]+)"' "softwareVersion" $f
        ReleaseLine     = Get-Version $text 'class="release-line">[^<]*?v([0-9]+\.[0-9]+\.[0-9]+)' "release-line" $f
        TrustLedger     = Get-Version $text '<strong>v([0-9]+\.[0-9]+\.[0-9]+)</strong>' "trust-ledger" $f
        Screenshot      = Get-Version $text '<figcaption>[^<]*v([0-9]+\.[0-9]+\.[0-9]+)' "figcaption" $f
    }
}

if ($rows.Count -eq 0) { Write-Error "검사할 랜딩 페이지가 없습니다."; exit 1 }

Write-Host "Landing pages: $($rows.Count)"
foreach ($r in $rows) {
    Write-Host ("  {0,-16} release[sw={1} line={2} trust={3}]  screenshot={4}" -f `
        $r.File, $r.SoftwareVersion, $r.ReleaseLine, $r.TrustLedger, $r.Screenshot)
}

# 릴리스 버전: 모든 파일의 sw/line/trust 가 단일 값이어야 한다.
$releaseValues = foreach ($r in $rows) { $r.SoftwareVersion; $r.ReleaseLine; $r.TrustLedger }
$releaseUnique = @($releaseValues | Where-Object { $_ } | Sort-Object -Unique)

# 스크린샷 버전: 모든 파일의 figcaption 이 단일 값이어야 한다(릴리스 버전과는 별개).
$screenshotUnique = @($rows | ForEach-Object { $_.Screenshot } | Where-Object { $_ } | Sort-Object -Unique)

$fail = 0

if ($releaseUnique.Count -eq 1) {
    Write-Host "OK: 릴리스 버전이 모든 언어에서 일치합니다 (v$($releaseUnique[0]))." -ForegroundColor Green
} elseif ($releaseUnique.Count -eq 0) {
    Write-Host "MISMATCH: 릴리스 버전 표기를 하나도 찾지 못했습니다." -ForegroundColor Red
    $fail++
} else {
    Write-Host "MISMATCH: 릴리스 버전이 언어 간에 어긋납니다 -> $($releaseUnique -join ', ')" -ForegroundColor Red
    $fail++
}

if ($screenshotUnique.Count -le 1) {
    $shot = if ($screenshotUnique.Count -eq 1) { "v$($screenshotUnique[0])" } else { "표기 없음" }
    Write-Host "OK: 스크린샷(figcaption) 버전이 일치합니다 ($shot)." -ForegroundColor Green
} else {
    Write-Host "MISMATCH: 스크린샷(figcaption) 버전이 언어 간에 어긋납니다 -> $($screenshotUnique -join ', ')" -ForegroundColor Red
    $fail++
}

if ($missingFiles -gt 0) {
    Write-Host "참고: 랜딩 파일 $missingFiles 개를 읽지 못했습니다." -ForegroundColor DarkYellow
}

if ($fail -ne 0) { exit 2 }
Write-Host "모든 랜딩 버전 검사를 통과했습니다." -ForegroundColor Green
