<#
.SYNOPSIS
  config/locales.tsv 를 읽어 로케일 목록을 돌려주는 공용 헬퍼. 직접 실행하지 않고
  다른 스크립트에서 dot-source 한다.
.DESCRIPTION
  릴리스·랜딩 검사 스크립트가 각자 로케일 목록을 들고 있었고, 그래서 #294 와 #329
  가 각각 목록 절반만 갱신한 채 통과했다. 목록은 config/locales.tsv 한 곳에 있고
  이 함수가 그것을 읽는다. 파일명 규칙(소스 언어는 코드가 안 붙고 나머지는 붙는다)
  도 여기 한 번만 적는다 — 규칙을 쓰는 곳마다 다시 적으면 목록을 합친 의미가 없다.
.EXAMPLE
  . (Join-Path $PSScriptRoot 'locales.ps1')
  $locales = Get-MarkleafLocales -RepoRoot $RepoRoot
  $locales.Store        # ko-KR, en-US, ...  (릴리스 노트 블록 순서)
  $locales.LandingFile  # index.html, index.ko.html, ...
#>

function Get-MarkleafLocales {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [string]$ManifestPath = "config/locales.tsv"
    )

    $path = if ([System.IO.Path]::IsPathRooted($ManifestPath)) {
        $ManifestPath
    } else {
        Join-Path $RepoRoot $ManifestPath
    }
    if (-not (Test-Path -LiteralPath $path)) {
        throw "로케일 목록을 찾지 못했습니다: $path"
    }

    $locales = @()
    $lineNumber = 0
    foreach ($raw in Get-Content -Encoding utf8 -LiteralPath $path) {
        $lineNumber++
        $line = ($raw -split '#', 2)[0].Trim()
        if (-not $line) { continue }

        $fields = @($line -split '\s+')
        if ($fields.Count -ne 4) {
            throw "$path ${lineNumber}행: 열이 4개여야 합니다 (code store source starter) — '$raw'"
        }
        foreach ($index in 2, 3) {
            if ($fields[$index] -notin @('yes', 'no')) {
                throw "$path ${lineNumber}행: '$($fields[$index])' 는 yes/no 여야 합니다 — '$raw'"
            }
        }

        $code = $fields[0]
        $isSource = $fields[2] -eq 'yes'
        # 소스 언어의 파일에는 코드가 붙지 않는다: values/, docs/index.html,
        # README.md. 나머지는 전부 코드가 붙는다.
        $suffix = if ($isSource) { "" } else { ".$code" }
        $locales += [pscustomobject]@{
            Code            = $code
            Store           = $fields[1]
            IsSource        = $isSource
            HasStarterNotes = $fields[3] -eq 'yes'
            ResDir          = if ($isSource) { "values" } else { "values-$code" }
            RawDir          = if ($isSource) { "raw" } else { "raw-$code" }
            LandingFile     = "index$suffix.html"
            PrivacyFile     = "privacy$suffix.html"
            ReadmeFile      = "README$suffix.md"
        }
    }

    if ($locales.Count -eq 0) {
        throw "$path 에 로케일 행이 없습니다."
    }
    # code 와 store 둘 다 유일해야 한다. store 가 겹치면 두 언어가 같은 fastlane
    # 디렉터리로 검사를 통과하고, 릴리스 노트 TXT 는 같은 블록을 두 번 쓴다 —
    # 새 언어가 자기 스토어 메타데이터 없이 초록으로 나가는 경로다.
    foreach ($field in 'Code', 'Store') {
        $duplicates = @($locales | Group-Object $field | Where-Object { $_.Count -gt 1 })
        if ($duplicates.Count -gt 0) {
            throw "$path 에 중복된 $field 가 있습니다: $(($duplicates | ForEach-Object { $_.Name }) -join ', ')"
        }
    }
    $sources = @($locales | Where-Object { $_.IsSource })
    if ($sources.Count -ne 1) {
        throw "$path 의 source 열은 정확히 한 행에서만 yes 여야 합니다 (지금 $($sources.Count)개)."
    }

    return $locales
}
