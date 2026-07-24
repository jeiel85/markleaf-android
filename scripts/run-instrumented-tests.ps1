<#
.SYNOPSIS
  연결된 기기/에뮬레이터에서 계측 테스트를 실행한다.
.DESCRIPTION
  CI 도 이제 계측 테스트를 돌린다 — `instrumented-tests` job 이 AGP 관리형 기기
  (`:app:ciAtdApi30DebugAndroidTest`)로 실행한다(#235). `connectedDebugAndroidTest`
  가 GitHub 러너에서 실패했던 것은 ddmlib 의 property fetch 타임아웃 때문인데,
  관리형 기기는 그 탐색 경로를 지나가지 않는다.

  이 스크립트는 그 자동 게이트를 대체하지 않고, 손에 있는 실기기나 AVD 에서
  같은 스위트를 돌려 보기 위한 것이다. 릴리스 전 확인용으로도 계속 쓴다 —
  docs/RELEASE.md 의 "Instrumented tests" 절을 볼 것.

  제외 패키지는 기본값이 없다. `com.markleaf.notes.ui` 를 빼고 돌리던 시절은
  ComprehensiveFeatureTest 가 현재 UI 와 어긋나 24 건 실패하던 때의 이야기이고,
  그 클래스는 #239 에서 삭제됐다. 필요하면 -ExcludePackage 로 직접 건다.

  gradlew.bat 을 쓴다. POSIX `gradlew` 는 인자를 하나로 합쳐버린다(#241).
.EXAMPLE
  pwsh scripts/run-instrumented-tests.ps1
.EXAMPLE
  pwsh scripts/run-instrumented-tests.ps1 -Serial emulator-5554
#>
[CmdletBinding()]
param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$Serial = "",
    [string]$ExcludePackage = ""
)

$ErrorActionPreference = "Stop"

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adb = (Get-Command adb -ErrorAction SilentlyContinue)?.Source
}
if (-not $adb) {
    Write-Error "adb 를 찾을 수 없습니다. Android SDK platform-tools 를 설치하거나 PATH 에 추가하세요."
}

# 기기 선택 — 명시된 시리얼이 없으면 'device' 상태인 첫 항목.
if (-not $Serial) {
    $Serial = (& $adb devices |
        Select-String -Pattern '^(\S+)\s+device$' |
        ForEach-Object { $_.Matches[0].Groups[1].Value } |
        Select-Object -First 1)
}
if (-not $Serial) {
    Write-Host ""
    Write-Host "연결된 기기가 없습니다." -ForegroundColor Red
    Write-Host "  실기기: USB 연결 후 `adb devices` 로 'device' 상태 확인"
    Write-Host "  에뮬레이터: emulator -list-avds 로 AVD 확인 후 emulator -avd <name>"
    exit 1
}

# boot_completed 만으로는 부족한 경우가 있어 API 레벨까지 확인한다. Gradle 이
# 기기를 고를 때 실제로 묻는 값이고, 여기서 비어 있으면 아래 실행은
# "0 of which were compatible" 로 끝난다.
$sdk = (& $adb -s $Serial shell getprop ro.build.version.sdk 2>$null) -replace '\s',''
if (-not $sdk) {
    Write-Host "기기 $Serial 가 API 레벨을 보고하지 않습니다 (부팅 중이거나 응답 불가)." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "기기: $Serial (API $sdk)" -ForegroundColor Cyan
if ($ExcludePackage) {
    Write-Host "제외: $ExcludePackage" -ForegroundColor DarkGray
} else {
    Write-Host "제외 없음 — 계측 스위트 전체" -ForegroundColor DarkGray
}
Write-Host ""

Push-Location $RepoRoot
try {
    $env:ANDROID_SERIAL = $Serial
    $gradleArgs = @(":app:connectedDebugAndroidTest")
    if ($ExcludePackage) {
        $gradleArgs += "-Pandroid.testInstrumentationRunnerArguments.notPackage=$ExcludePackage"
    }
    $gradleArgs += "--console=plain"
    & (Join-Path $RepoRoot "gradlew.bat") @gradleArgs
    $code = $LASTEXITCODE
}
finally {
    Pop-Location
}

Write-Host ""
if ($code -eq 0) {
    Write-Host "계측 테스트 통과." -ForegroundColor Green
} else {
    Write-Host "계측 테스트 실패. 리포트: app/build/reports/androidTests/connected/debug/index.html" -ForegroundColor Red
}
exit $code
