<#
.SYNOPSIS
  연결된 기기/에뮬레이터에서 계측 테스트를 실행한다.
.DESCRIPTION
  CI 는 계측 테스트를 돌리지 않는다(#235). GitHub 호스티드 러너의 에뮬레이터가
  ddmlib 의 property fetch 에 응답하지 못해 네 번 연속 실패했고, 그 타임아웃은
  adb 쪽에서 기다려서 해결되는 종류가 아니다. 그래서 이 검증은 **로컬 수동 게이트**로
  옮겼다. 릴리스 전에 반드시 한 번 돌린다 — docs/RELEASE.md 의 "Instrumented tests"
  절을 볼 것.

  `com.markleaf.notes.ui` 는 제외한다. 그 클래스들은 크래시(#235)로 오래 실행되지
  못하는 동안 검증 대상 UI 가 바뀌어 24 건이 실패한다(#239). 허용 목록이 아니라
  제외로 거는 이유는, 다른 패키지에 테스트를 추가했을 때 필터를 넓혀줄 때까지
  조용히 건너뛰지 않게 하기 위해서다.

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
    [string]$ExcludePackage = "com.markleaf.notes.ui"
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
Write-Host "제외: $ExcludePackage  (#239)" -ForegroundColor DarkGray
Write-Host ""

Push-Location $RepoRoot
try {
    $env:ANDROID_SERIAL = $Serial
    & (Join-Path $RepoRoot "gradlew.bat") `
        ":app:connectedDebugAndroidTest" `
        "-Pandroid.testInstrumentationRunnerArguments.notPackage=$ExcludePackage" `
        "--console=plain"
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
