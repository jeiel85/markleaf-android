<#
.SYNOPSIS
  `:app:exportReleaseToBuildDrive` 가 이번 버전 산출물을 실제로 남겼는지 검증한다.
.DESCRIPTION
  기대 버전은 `app/build.gradle.kts` 의 versionName / versionCode 다. 태그를 밀기 전에
  `D:\Build` 에 이번 버전의 세 파일이 있는지 확인한다.

  이 검사가 필요한 이유는 두 가지다.

  1. export 는 사람이 손으로 돌리는 단계라 통째로 빠질 수 있다. v2.27.2 부터
     v2.29.0 까지 여섯 릴리스가 AAB 도 mapping 도 없이 지나갔고, 그 사실은
     GitHub 릴리스 자산을 정리하다가(#244) 뒤늦게 드러났다.
  2. mapping 은 없어도 export 가 성공한다 — `writeReleaseArtifacts` 는 AAB 가 없으면
     예외를 던지지만 mapping 은 `logger.warn` 만 남기고 건너뛴다(minify 가 꺼져 있으면
     조용히 그렇게 된다). AAB 가 있다고 mapping 도 있다고 볼 수 없다.

  mapping 은 D064 이후 GitHub Release 에 붙지 않고 30일 아티팩트로만 남으므로,
  여기서 놓치면 그 버전은 영구히 역난독화할 수 없게 된다.

  `D:\Build` 는 여러 프로젝트가 함께 쓰는 디렉터리다. `markleaf-v<semver>-vc<code>`
  stem 에 해당하는 파일만 본다.
.EXAMPLE
  pwsh scripts/verify-release-export.ps1
.EXAMPLE
  pwsh scripts/verify-release-export.ps1 -ExportDir E:\Build -ExpectedVersion 2.29.0 -ExpectedVersionCode 118
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ExportDir = "D:\Build",
    [string]$ExpectedVersion = "",
    [int]$ExpectedVersionCode = 0
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

# app/build.gradle.kts 의 noteLocales 와 같은 목록이어야 한다. 릴리스 노트 TXT 는
# 로케일마다 <tag> ... </tag> 블록을 연달아 담는다(writeReleaseArtifacts).
$StoreLocales = @("ko-KR", "en-US", "ja-JP", "zh-CN", "de-DE", "fr-FR", "es-ES")

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

$stem = "markleaf-v$ExpectedVersion-vc$ExpectedVersionCode"
Write-Host "기대 버전: v$ExpectedVersion (versionCode $ExpectedVersionCode)"
Write-Host "내보내기 위치: $ExportDir"
Write-Host "파일 stem: $stem"

if (-not (Test-Path -LiteralPath $ExportDir)) {
    Write-Host "`n  FAIL  내보내기 디렉터리가 없습니다: $ExportDir" -ForegroundColor Red
    Write-Host "`n실패 1건. .\gradlew.bat :app:exportReleaseToBuildDrive 를 먼저 실행하세요 — 검사를 고치지 마세요." -ForegroundColor Red
    exit 2
}

# ---- 1. 세 산출물 존재 + 비어 있지 않음 ----
Write-Host "`n릴리스 산출물"
$expected = [ordered]@{
    'AAB'      = "$stem.aab"
    'mapping'  = "$stem.mapping.txt"
    '릴리스 노트' = "$stem-release-notes.txt"
}

$notesPath = $null
foreach ($label in $expected.Keys) {
    $name = $expected[$label]
    $path = Join-Path $ExportDir $name
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure ("  FAIL  {0,-12} {1} 이(가) 없습니다." -f $label, $name)
        continue
    }
    $size = (Get-Item -LiteralPath $path).Length
    if ($size -le 0) {
        Add-Failure ("  FAIL  {0,-12} {1} 이(가) 비어 있습니다." -f $label, $name)
        continue
    }
    if ($label -eq '릴리스 노트') { $notesPath = $path }
    # 노트 TXT 는 수 KB라 MB 로 찍으면 0.0 으로 보여 비어 있는 것처럼 읽힌다.
    $human = if ($size -ge 1MB) { "{0:N1} MB" -f ($size / 1MB) } else { "{0:N0} KB" -f ($size / 1KB) }
    Write-Host ("  OK    {0,-12} {1} ({2})" -f $label, $name, $human) -ForegroundColor Green
}

# ---- 2. 릴리스 노트 TXT 가 여섯 로케일 블록을 모두 담고 있는지 ----
# 노트 파일은 있는데 로케일이 빠진 채 만들어지는 경우는 Gradle task 가 막지만,
# 손으로 편집하거나 오래된 버전의 파일이 남아 있는 경우까지 막지는 못한다.
if ($notesPath) {
    Write-Host "`n릴리스 노트 로케일 ($($StoreLocales.Count)개)"
    $notesText = Get-Content -Raw -Encoding utf8 -LiteralPath $notesPath
    $missingLocales = @()
    foreach ($locale in $StoreLocales) {
        if ($notesText -notmatch [regex]::Escape("<$locale>") -or $notesText -notmatch [regex]::Escape("</$locale>")) {
            $missingLocales += $locale
        }
    }
    if ($missingLocales.Count -gt 0) {
        Add-Failure ("  FAIL  블록이 없는 로케일: {0}" -f ($missingLocales -join ', '))
    } else {
        Write-Host ("  OK    여섯 로케일 블록이 모두 있습니다.") -ForegroundColor Green
    }
}

if ($fail -ne 0) {
    Write-Host "`n실패 $fail 건. .\gradlew.bat :app:exportReleaseToBuildDrive 를 실행해 산출물을 남기세요 — 검사를 고치지 마세요." -ForegroundColor Red
    Write-Host "mapping 이 빠졌다면 minify 가 켜진 상태로 bundleRelease 가 돌았는지 확인하세요." -ForegroundColor Red
    exit 2
}
Write-Host "`n모든 내보내기 검사를 통과했습니다 (v$ExpectedVersion / vc$ExpectedVersionCode)." -ForegroundColor Green
