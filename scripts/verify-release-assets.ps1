<#
.SYNOPSIS
  GitHub Release 에 붙는 자산 목록이 워크플로·스테이징 스텝·문서 세 곳에서
  일치하는지 검증한다.
.DESCRIPTION
  같은 사실이 세 벌 존재한다. 실행되는 것은 하나뿐이고, 나머지 둘은 조용히 낡는다.

  1. `gh release create` 에 넘어가는 파일 인자 (실제로 업로드되는 것)
  2. `Prepare release asset` 스텝이 만들어 두는 파일 (인자가 가리키는 대상)
  3. `docs/RELEASE.md` · `AGENTS.md` 의 문서 표기

  D062(AAB 제외)와 D064(mapping 제외)로 목록이 세 개 → 한 개로 줄어드는 동안 이
  목록은 두 번 어긋났고, #244 에서 같은 세 곳을 손으로 다시 맞춰야 했다. 세 번째
  드리프트를 막는 것이 없었다 — 이 스크립트가 그것이다.

  두 번째 문제도 같이 닫는다. `gh release create` 의 파일 인자는 **태그 푸시에서만**
  실행된다. release job 은 PR 에서 skip 되므로, #244 처럼 산출물을 만드는 스텝을
  지우면서 인자를 남겨 두면 PR CI 는 통과하고 **태그가 공개된 뒤에** 실패한다.
  이 검사는 build job 에서 도므로 그 실패가 PR 로 앞당겨진다.

  문서 쪽은 산문을 파싱하지 않는다. 기계가 읽을 마커 한 줄을 두고 그것을 비교한다:

      <!-- release-assets: markleaf-vX.Y.Z.apk -->

  마커는 자기를 설명하는 산문 바로 옆에 둔다. 목록을 고치는 사람이 마커를 못 보고
  지나가기 어렵고, 지나가면 이 검사가 잡는다.
.EXAMPLE
  pwsh scripts/verify-release-assets.ps1
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$WorkflowPath = Join-Path $RepoRoot ".github/workflows/android-build.yml"
# 마커를 두어야 하는 문서. 새 문서가 목록을 다시 적기 시작하면 여기에 추가한다.
$DocPaths = @("docs/RELEASE.md", "AGENTS.md")
# `gh release create` 에서 값을 하나 먹는 플래그. 그 값은 파일 인자가 아니다.
# 불리언 플래그(`--draft`, `--prerelease`, `--verify-tag`, `--latest`, `--generate-notes`)는
# 여기 넣으면 안 된다 — 뒤따르는 파일 인자를 값으로 오인해 건너뛰고, 실제로는 업로드되는
# 파일을 "문서에 없음"으로 잡는 거짓 실패가 된다. 목록에 없는 플래그는 값을 먹지 않는
# 것으로 취급하므로, 불리언은 그냥 빼 두면 된다.
$ValueFlags = @("--repo", "-R", "--title", "-t", "--notes", "-n",
                "--notes-file", "-F", "--target", "--discussion-category",
                "--notes-start-tag")
# 태그 이름은 실행 시점에 정해지므로 문서와 비교하려면 자리표시자로 정규화한다.
$TagPlaceholder = "vX.Y.Z"

$fail = 0
function Add-Failure([string]$Message) {
    Write-Host $Message -ForegroundColor Red
    $script:fail++
}

function Get-StepBlock([string]$Text, [string]$StepName) {
    # `- name: <StepName>` 부터 같은 들여쓰기의 다음 `- name:` 직전까지.
    $pattern = "(?ms)^(\s*)- name: " + [regex]::Escape($StepName) + "\s*$.*?(?=^\1- name: |\Z)"
    $m = [regex]::Match($Text, $pattern)
    if (-not $m.Success) { return $null }
    return $m.Value
}

function ConvertTo-Placeholder([string]$Name) {
    # "markleaf-${GITHUB_REF_NAME}.apk" -> "markleaf-vX.Y.Z.apk"
    return ($Name -replace '\$\{GITHUB_REF_NAME\}', $TagPlaceholder `
                  -replace '\$GITHUB_REF_NAME', $TagPlaceholder).Trim('"', "'")
}

if (-not (Test-Path -LiteralPath $WorkflowPath)) {
    Write-Error "워크플로를 찾지 못했습니다: $WorkflowPath"
    exit 1
}
$workflow = Get-Content -Raw -Encoding utf8 -LiteralPath $WorkflowPath

# ---- 1. gh release create 의 파일 인자 ----
$createBlock = Get-StepBlock $workflow "Create GitHub release"
if (-not $createBlock) {
    Write-Error "'Create GitHub release' 스텝을 찾지 못했습니다. 스텝 이름이 바뀌었다면 이 스크립트도 같이 고치세요."
    exit 1
}

# 줄 끝 `\` 연속을 한 줄로 편다.
$flattened = ($createBlock -replace '\\\s*\r?\n\s*', ' ')
$cmdMatch = [regex]::Match($flattened, 'gh release create\s+(?<args>.+)')
if (-not $cmdMatch.Success) {
    Write-Error "'Create GitHub release' 스텝 안에서 `gh release create` 호출을 찾지 못했습니다."
    exit 1
}

# 큰따옴표로 묶인 덩어리를 하나의 토큰으로 유지하며 자른다.
$tokens = @([regex]::Matches($cmdMatch.Groups['args'].Value, '"[^"]*"|\S+') |
    ForEach-Object { $_.Value })

$uploadArgs = @()
$skipNext = $false
$sawTag = $false
foreach ($t in $tokens) {
    if ($skipNext) { $skipNext = $false; continue }
    if ($t.StartsWith('--')) {
        if ($ValueFlags -contains $t) { $skipNext = $true }
        continue
    }
    if (-not $sawTag) { $sawTag = $true; continue }  # 첫 위치 인자는 태그다
    $uploadArgs += (ConvertTo-Placeholder $t)
}

Write-Host "gh release create 파일 인자 ($($uploadArgs.Count)개)"
foreach ($a in $uploadArgs) { Write-Host "  $a" }
if ($uploadArgs.Count -eq 0) {
    Add-Failure "  FAIL  파일 인자가 하나도 없습니다 — Release 가 빈 채로 발행됩니다."
}

# ---- 2. Prepare release asset 이 실제로 만드는 파일 ----
$prepBlock = Get-StepBlock $workflow "Prepare release asset"
if (-not $prepBlock) {
    Write-Error "'Prepare release asset' 스텝을 찾지 못했습니다."
    exit 1
}
$staged = @([regex]::Matches($prepBlock, '(?m)^\s*(?:cp|mv)\s+\S+\s+("(?<dst>[^"]+)"|(?<dst>\S+))\s*$') |
    ForEach-Object { ConvertTo-Placeholder $_.Groups['dst'].Value })

Write-Host "`nPrepare release asset 이 만드는 파일 ($($staged.Count)개)"
foreach ($s in $staged) { Write-Host "  $s" }

foreach ($a in $uploadArgs) {
    if ($staged -notcontains $a) {
        Add-Failure "  FAIL  '$a' 를 업로드하는데 그 파일을 만드는 스텝이 없습니다 — 태그 시점에 실패합니다."
    }
}
foreach ($s in $staged) {
    if ($uploadArgs -notcontains $s) {
        Add-Failure "  FAIL  '$s' 를 만들어 두고 업로드하지 않습니다 — 죽은 스테이징이거나 빠뜨린 인자입니다."
    }
}

# ---- 3. 문서 마커 ----
Write-Host "`n문서 마커 ($($DocPaths.Count)개 파일)"
$markerPattern = '<!--\s*release-assets:\s*(?<list>[^>]+?)\s*-->'
foreach ($rel in $DocPaths) {
    $path = Join-Path $RepoRoot $rel
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Failure ("  FAIL  {0,-18} 파일이 없습니다." -f $rel)
        continue
    }
    $text = Get-Content -Raw -Encoding utf8 -LiteralPath $path
    $matches = @([regex]::Matches($text, $markerPattern))
    if ($matches.Count -eq 0) {
        Add-Failure ("  FAIL  {0,-18} release-assets 마커가 없습니다. 자산 목록을 적은 문단 옆에 `<!-- release-assets: ... -->` 를 두세요." -f $rel)
        continue
    }
    $ok = $true
    foreach ($m in $matches) {
        $documented = @($m.Groups['list'].Value -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
        $missing = @($uploadArgs | Where-Object { $documented -notcontains $_ })
        $extra = @($documented | Where-Object { $uploadArgs -notcontains $_ })
        if ($missing.Count -or $extra.Count) {
            $detail = @()
            if ($missing.Count) { $detail += "문서에 없음: $($missing -join ', ')" }
            if ($extra.Count) { $detail += "문서에만 있음: $($extra -join ', ')" }
            Add-Failure ("  FAIL  {0,-18} 워크플로와 다릅니다 — {1}" -f $rel, ($detail -join ' / '))
            $ok = $false
        }
    }
    if ($ok) {
        Write-Host ("  OK    {0,-18} 마커 {1}개 모두 일치" -f $rel, $matches.Count) -ForegroundColor Green
    }
}

if ($fail -ne 0) {
    Write-Host "`n실패 $fail 건. 세 복사본을 일치시키세요 — 검사를 고치지 마세요." -ForegroundColor Red
    Write-Host "자산 목록을 의도적으로 바꾸는 중이라면 워크플로·스테이징 스텝·문서 마커를 함께 고쳐야 합니다 (D062, D064)." -ForegroundColor Red
    exit 2
}
Write-Host "`n릴리스 자산 목록이 워크플로·스테이징·문서에서 일치합니다 ($($uploadArgs -join ', '))." -ForegroundColor Green
