<#
.SYNOPSIS
  config/locales.tsv 의 언어 목록과 실제 표면(리소스·스토어·랜딩·README·문서)이
  양방향으로 일치하는지 검증한다.
.DESCRIPTION
  언어 목록이 일곱 군데에 복사돼 있었고, 그래서 새 언어가 절반만 들어간 채로
  통과했다 — 중국어(#294)와 크로아티아어(#329) 모두 손으로 세어서야 발견했다.
  목록은 이제 config/locales.tsv 한 곳에 있고, 테스트·Gradle·검사 스크립트가
  그것을 읽는다.

  목록 하나만으로는 절반만 막힌다. 목록에 있는데 파일이 없으면(선언만 하고 번역을
  안 붙였다) 그 언어는 여전히 조용히 나가고, 파일이 있는데 목록에 없으면(#294 의
  raw-zh 처럼) 아무 게이트도 그 파일을 보지 않는다. 그래서 표면마다 양방향으로
  본다: 선언된 언어의 파일이 있는가, 그리고 그 자리에 있는 파일이 전부 선언된
  언어의 것인가.

  검사하는 표면은 여섯 가지다.

  1. 앱 리소스 — app/src/main/res/values[-code]/strings.xml
  2. 첫 실행 노트 — res/raw[-code]/starter_notes.md (starter=yes 인 언어만)
  3. 스토어 메타데이터 — fastlane/metadata/android/<store>/
  4. 랜딩·개인정보 페이지 — docs/index[.code].html, docs/privacy[.code].html
     (개인정보 페이지는 버전 표기가 없어 verify-landing-versions.ps1 의 대상이
     아니었고, 그래서 어떤 검사도 존재 여부를 보지 않았다 — 링크 두 개가 죽는다.)
  5. README — README[.code].md
  6. 데모 클립 — docs/assets/markleaf-tablet-<code>.{gif,mp4} + -still.webp

  마지막으로 문서에 적힌 언어 "개수"도 본다. AGENTS.md·docs/RELEASE.md·
  docs/assets/README.md 는 개수를 산문으로 적어 두는데, 그건 목록에서 파생되지
  않으므로 언어가 늘어도 그대로 남는다.
.EXAMPLE
  pwsh scripts/verify-locales.ps1
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

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

# 선언된 경로가 실제로 있는지. $Expected 는 저장소 기준 상대 경로다.
function Test-Declared([string]$Label, [string[]]$Expected) {
    $missing = @($Expected | Where-Object { -not (Test-Path -LiteralPath (Resolve-UnderRoot $_)) })
    if ($missing.Count -gt 0) {
        Add-Failure ("  FAIL  {0,-14} 선언된 언어인데 없는 경로: {1}" -f $Label, ($missing -join ', '))
    } else {
        Write-Host ("  OK    {0,-14} {1}개" -f $Label, $Expected.Count) -ForegroundColor Green
    }
}

# 그 자리에 있는 것 중 선언되지 않은 것. $Pattern 은 언어별 항목만 골라내는
# 정규식이고, 첫 그룹이 언어 코드여야 한다. $DeclaredCodes 는 "이 자리에 있어도
# 되는" 코드다 — 코드가 붙는 자리(values-xx, index.xx.html …)에는 소스 언어가
# 오면 안 되므로 그 목록에서 빠진다.
function Test-Strays([string]$Label, [string]$ScanRoot, [string]$Pattern, [string[]]$DeclaredCodes, [string]$SourceCode = "", [switch]$Directory) {
    $root = Resolve-UnderRoot $ScanRoot
    if (-not (Test-Path -LiteralPath $root)) {
        Add-Failure ("  FAIL  {0,-14} 검사할 디렉터리가 없습니다: {1}" -f $Label, $ScanRoot)
        return
    }
    $entries = if ($Directory) {
        Get-ChildItem -LiteralPath $root -Directory
    } else {
        Get-ChildItem -LiteralPath $root -File
    }
    $strays = @()
    $sourceStrays = @()
    foreach ($entry in $entries) {
        $match = [regex]::Match($entry.Name, $Pattern)
        if (-not $match.Success) { continue }
        $code = $match.Groups[1].Value
        if ($DeclaredCodes -contains $code) { continue }
        if ($SourceCode -and $code -eq $SourceCode) {
            $sourceStrays += $entry.Name
        } else {
            $strays += "$($entry.Name) ($code)"
        }
    }
    if ($strays.Count -gt 0) {
        Add-Failure ("  FAIL  {0,-14} config/locales.tsv 에 없는 언어의 파일: {1}" -f $Label, ($strays -join ', '))
    }
    if ($sourceStrays.Count -gt 0) {
        # 소스 언어는 코드가 붙지 않는 자리(values/, index.html, README.md)에만
        # 있어야 한다. values-en 은 영어 기기에서 values/ 를 밀어내는데, 파리티
        # 테스트는 values/ 만 읽으므로 낡은 영어 리소스가 검사 밖으로 나간다.
        Add-Failure ("  FAIL  {0,-14} 소스 언어({1})는 코드 없는 자리에만 둡니다: {2}" -f $Label, $SourceCode, ($sourceStrays -join ', '))
    }
}

$locales = Get-MarkleafLocales -RepoRoot $RepoRoot
$codes = @($locales | ForEach-Object { $_.Code })
$sourceCode = ($locales | Where-Object { $_.IsSource }).Code
# 코드가 붙는 자리에 올 수 있는 언어. 소스 언어는 values/ · index.html · README.md
# 쪽이므로 여기서 빠진다 (values-en 은 파리티 테스트가 읽지 않는 자리다).
$suffixedCodes = @($locales | Where-Object { -not $_.IsSource } | ForEach-Object { $_.Code })
$stores = @($locales | ForEach-Object { $_.Store })
Write-Host "로케일 목록: $($locales.Count)개 (config/locales.tsv) — $($codes -join ', ')"

# ---- 1. 앱 리소스 ----
Write-Host "`n앱 리소스 (app/src/main/res)"
Test-Declared "strings.xml" @($locales | ForEach-Object { "app/src/main/res/$($_.ResDir)/strings.xml" })
# values-night 처럼 언어가 아닌 한정자는 두 글자 코드 모양이 아니라 저절로 빠진다.
Test-Strays "values-*" "app/src/main/res" '^values-([a-z]{2})(?:-r[A-Z]{2})?$' $suffixedCodes $sourceCode -Directory

# ---- 2. 첫 실행 노트 ----
Write-Host "`n첫 실행 노트 (res/raw)"
$withStarter = @($locales | Where-Object { $_.HasStarterNotes })
Test-Declared "starter_notes" @($withStarter | ForEach-Object { "app/src/main/res/$($_.RawDir)/starter_notes.md" })
# raw-zh 는 일부러 없다(#294). 반대로 raw-<code> 가 있는데 목록에서 starter=no
# 라면 목록이 낡은 것이므로 그것도 잡는다.
Test-Strays "raw-*" "app/src/main/res" '^raw-([a-z]{2})$' @($withStarter | Where-Object { -not $_.IsSource } | ForEach-Object { $_.Code }) $sourceCode -Directory

# ---- 3. 스토어 메타데이터 ----
Write-Host "`n스토어 메타데이터 (fastlane/metadata/android)"
Test-Declared "store locale" @($stores | ForEach-Object { "fastlane/metadata/android/$_" })
Test-Strays "store locale" "fastlane/metadata/android" '^([a-z]{2}-[A-Z]{2})$' $stores -Directory

# ---- 4. 랜딩·개인정보 페이지 ----
Write-Host "`n랜딩·개인정보 페이지 (docs)"
Test-Declared "index.html" @($locales | ForEach-Object { "docs/$($_.LandingFile)" })
Test-Declared "privacy.html" @($locales | ForEach-Object { "docs/$($_.PrivacyFile)" })
# 소스 언어의 페이지는 index.html 이므로, index.en.html 은 아무도 링크하지 않는
# 사본이 된다 — 코드가 붙은 자리에서는 소스 언어도 이물이다.
Test-Strays "index.*.html" "docs" '^index\.([a-z]{2})\.html$' $suffixedCodes $sourceCode
Test-Strays "privacy.*.html" "docs" '^privacy\.([a-z]{2})\.html$' $suffixedCodes $sourceCode

# ---- 5. README ----
Write-Host "`nREADME"
Test-Declared "README" @($locales | ForEach-Object { $_.ReadmeFile })
Test-Strays "README.*.md" "." '^README\.([a-z]{2})\.md$' $suffixedCodes $sourceCode

# ---- 6. 데모 클립 ----
Write-Host "`n데모 클립 (docs/assets)"
$clips = @()
foreach ($locale in $locales) {
    $clips += "docs/assets/markleaf-tablet-$($locale.Code).mp4"
    $clips += "docs/assets/markleaf-tablet-$($locale.Code).gif"
    $clips += "docs/assets/markleaf-tablet-$($locale.Code)-still.webp"
}
Test-Declared "tablet clip" $clips
Test-Strays "tablet clip" "docs/assets" '^markleaf-tablet-([a-z]{2})(?:-still)?\.(?:gif|mp4|webp)$' $codes

# ---- 7. 문서에 적힌 개수 ----
#
# 개수는 목록에서 파생되지 않는 유일한 표기라 언어가 늘어도 그대로 남는다.
# verify-release-export.ps1 의 "여섯 로케일" OK 줄이 두 번의 언어 추가를 지나
# 그대로 있었던 것과 같은 종류다.
Write-Host "`n문서에 적힌 언어 개수 ($($locales.Count)개여야 함)"
$numberWords = @{
    1 = 'one'; 2 = 'two'; 3 = 'three'; 4 = 'four'; 5 = 'five'; 6 = 'six'
    7 = 'seven'; 8 = 'eight'; 9 = 'nine'; 10 = 'ten'; 11 = 'eleven'; 12 = 'twelve'
}
$expectedWord = $numberWords[$locales.Count]
# "the languages" 같은 산문까지 후보로 잡지 않도록 수사(數詞)만 본다.
$numberWordAlternation = (($numberWords.Values | Sort-Object) -join '|')

# 파일 -> (라벨, 정규식, 기댓값) 목록. 정규식의 첫 그룹이 개수다.
$countChecks = @(
    @{ File = "AGENTS.md"; Label = "N개 로케일"; Pattern = '(\d+)개\s*(?:스토어\s+)?로케일'; Expected = "$($locales.Count)" }
    @{ File = "AGENTS.md"; Label = "N개 언어"; Pattern = '(\d+)개\s*언어'; Expected = "$($locales.Count)" }
    @{ File = "docs/RELEASE.md"; Label = "N languages"; Pattern = "\b($numberWordAlternation)\s+(?:languages|READMEs)\b"; Expected = $expectedWord }
    @{ File = "docs/assets/README.md"; Label = "클립 개수"; Pattern = 'markleaf-tablet-<lang>[^|]*\((\d+)\)'; Expected = "$($locales.Count)" }
)
foreach ($check in $countChecks) {
    $path = Resolve-UnderRoot $check.File
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure ("  FAIL  {0,-22} 파일이 없습니다." -f $check.File)
        continue
    }
    $text = Get-Content -Raw -Encoding utf8 -LiteralPath $path
    $found = @([regex]::Matches($text, $check.Pattern) | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
    if ($found.Count -eq 0) {
        Add-Failure ("  FAIL  {0,-22} {1} 표기를 찾지 못했습니다 — 표현이 바뀌었다면 이 검사의 정규식도 함께 고치세요." -f $check.File, $check.Label)
        continue
    }
    $wrong = @($found | Where-Object { $_ -ne $check.Expected })
    if ($wrong.Count -gt 0) {
        Add-Failure ("  FAIL  {0,-22} {1} 이(가) '{2}' 여야 하는데 '{3}' 로 적혀 있습니다." -f $check.File, $check.Label, $check.Expected, ($wrong -join ', '))
    } else {
        Write-Host ("  OK    {0,-22} {1} = {2}" -f $check.File, $check.Label, $check.Expected) -ForegroundColor Green
    }
}

# AGENTS.md 는 개수와 함께 코드 목록도 적는다: "8개 언어(en · ko · ...)".
# 개수만 맞고 목록이 낡는 경우가 있어 집합으로 비교한다.
$agentsPath = Resolve-UnderRoot "AGENTS.md"
if (Test-Path -LiteralPath $agentsPath) {
    $agentsText = Get-Content -Raw -Encoding utf8 -LiteralPath $agentsPath
    $listMatches = @([regex]::Matches($agentsText, '\(([a-z]{2}(?:\s*·\s*[a-z]{2})+)\)'))
    if ($listMatches.Count -eq 0) {
        Add-Failure "  FAIL  AGENTS.md            '(en · ko · …)' 언어 목록을 찾지 못했습니다."
    } else {
        $expectedList = ($codes | Sort-Object) -join ' '
        foreach ($match in $listMatches) {
            $listed = (($match.Groups[1].Value -split '·' | ForEach-Object { $_.Trim() }) | Sort-Object) -join ' '
            if ($listed -ne $expectedList) {
                Add-Failure ("  FAIL  AGENTS.md            언어 목록이 다릅니다: '{0}' (기대: {1})" -f $match.Groups[1].Value, ($codes -join ' · '))
            } else {
                Write-Host ("  OK    AGENTS.md            언어 목록 ({0})" -f $match.Groups[1].Value) -ForegroundColor Green
            }
        }
    }
}

if ($fail -ne 0) {
    Write-Host "`n실패 $fail 건. config/locales.tsv 와 표면을 맞추세요 — 검사를 고치지 마세요." -ForegroundColor Red
    Write-Host "언어를 추가하는 중이라면 config/locales.tsv 헤더에 어떤 파일이 필요한지 적혀 있습니다." -ForegroundColor Red
    exit 2
}
Write-Host "`n모든 로케일 표면 검사를 통과했습니다 ($($locales.Count)개 언어)." -ForegroundColor Green
