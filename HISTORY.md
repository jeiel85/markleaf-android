# HISTORY

## 2026-04-30
- Work: 릴리즈 노트 규칙을 `CHANGELOG.md` 기준으로 강제하도록 수정.
- Changed files:
  - `.github/workflows/android-build.yml` (release notes extraction from `CHANGELOG.md`)
  - `app/build.gradle.kts` (updated to v1.0.4 / versionCode 5)
  - `CHANGELOG.md` (added v1.0.4 and backfilled v1.0.2)
  - `HISTORY.md` (updated)
  - `.agent/decisions.md` (release note policy added)
  - `.agent/progress.md` (updated)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat assembleDebug`
  - `./gradlew.bat assembleRelease`
  - Updated existing `v1.0.2` and `v1.0.3` GitHub Release notes from `CHANGELOG.md`
- Result: 릴리즈 본문이 GitHub auto-generated notes 대신 프로젝트 changelog를 따르도록 정리 완료

## 2026-04-30
- Work: 릴리즈 규칙 위반 확인 및 수정. `v1.0.2`에 debug APK가 함께 첨부된 원인을 별도 `Release APK` workflow로 확인.
- Changed files:
  - `.github/workflows/release-apk.yml` (removed)
  - `.github/workflows/android-build.yml` (release asset naming and signed APK upload path corrected)
  - `app/build.gradle.kts` (updated to v1.0.3 / versionCode 4)
  - `CHANGELOG.md` (added v1.0.3)
  - `HISTORY.md` (updated)
  - `.agent/decisions.md` (release asset policy corrected)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat assembleDebug`
  - `./gradlew.bat assembleRelease`
  - Removed incorrect `app-debug.apk` asset from `v1.0.2`
- Result: 릴리즈에는 signed release APK만 첨부하도록 정리 완료

## 2026-04-30
- Work: v1.0.0 정식 출시 작업 완료. 태블릿 레이아웃, 백링크, 백업/복구, Material You 지원 및 최종 마감.
- Changed files:
  - `app/build.gradle.kts` (updated to v1.0.0)
  - `README.md` (updated roadmap)
  - `CHANGELOG.md` (added v1.0.0)
  - `HISTORY.md` (updated)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (updated with backlinks UI)
  - `app/src/main/java/com/markleaf/notes/util/BackupUtil.kt` (created)
  - `app/src/main/java/com/markleaf/notes/feature/settings/SettingsScreen.kt` (updated with backup UI)
- Verification:
  - 전 기능 통합 테스트 완료
  - 태블릿 레이아웃 및 다이내믹 컬러 동작 확인
  - ZIP 백업 파일 무결성 및 복구 테스트 통과
- Result: v1.0.0 정식 배포 준비 완료

