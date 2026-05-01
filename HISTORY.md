# HISTORY

## 2026-05-01
- Work: 영어 기본 및 한국어 다국어 지원. GitHub Issue #22 등록 후 구현.
- Changed files:
  - `app/src/main/res/values/strings.xml` (default English strings and starter notes)
  - `app/src/main/res/values-ko/strings.xml` (Korean strings)
  - `app/src/main/res/raw/starter_notes.md` (default English starter notes)
  - `app/src/main/res/raw-ko/starter_notes.md` (Korean starter notes)
  - Compose screen files (localized visible labels, empty states, buttons, content descriptions)
  - `app/src/main/java/com/markleaf/notes/data/onboarding/StarterNotesSeeder.kt` (locale resource-backed starter notes)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.14)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `rg "android.permission.INTERNET" -n app\src`
  - Device install was not run because no ADB device was listed after build
- Result: English remains the default language, and Korean devices receive Korean UI and starter notes

## 2026-05-01
- Work: 라이브 Markdown 에디터 1단계 inline syntax highlighting. GitHub Issue #21 등록 후 구현.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/core/markdown/MarkdownSyntaxHighlighter.kt` (raw Markdown text highlighting)
  - `app/src/main/java/com/markleaf/notes/core/markdown/MarkdownSyntaxVisualTransformation.kt` (identity-offset editor transformation)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (Edit mode live highlighting)
  - `app/src/test/java/com/markleaf/notes/core/markdown/MarkdownSyntaxHighlighterTest.kt` (highlighter tests)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.13)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - Lenovo TB320FC Android 15 `v1.0.13` release APK install and launch check
  - `rg "android.permission.INTERNET" -n app\src`
- Result: Edit 화면에서 Markdown 원문을 보존하면서 heading, emphasis, link, checkbox 문법을 실시간으로 하이라이팅

## 2026-05-01
- Work: 설정 옵션 기반 추가. GitHub Issue #20 등록 후 구현.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/data/settings/AppSettings.kt` (settings model)
  - `app/src/main/java/com/markleaf/notes/data/settings/AppSettingsRepository.kt` (DataStore preferences repository)
  - `app/src/main/java/com/markleaf/notes/feature/settings/SettingsScreen.kt` (Markdown syntax and line width controls)
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (line width applied to tablet editor pane)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.12)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - Lenovo TB320FC Android 15 `v1.0.12` release APK install and launch check
  - `rg "android.permission.INTERNET" -n app\src`
- Result: Markdown 표시 방식과 line width 설정을 저장할 수 있고, line width가 태블릿 에디터 폭에 반영됨

## 2026-05-01
- Work: 태블릿 왼쪽 노트 목록 접기/펼치기. GitHub Issue #19 등록 후 구현.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (tablet collapse state, rail, constrained editor width)
  - `app/src/main/java/com/markleaf/notes/feature/notes/NotesListScreen.kt` (optional collapse action)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.11)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - Lenovo TB320FC Android 15 `v1.0.11` release APK install and launch check
  - `rg "android.permission.INTERNET" -n app\src`
- Result: 태블릿에서 왼쪽 목록을 접을 수 있고, 접힘 상태에서도 에디터 폭을 최대 800dp로 제한

## 2026-05-01
- Work: Markdown 편집 툴바 개선. GitHub Issue #18 등록 후 구현.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/core/markdown/MarkdownEditActions.kt` (toolbar insertion logic)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (TextFieldValue editor state and toolbar UI)
  - `app/src/test/java/com/markleaf/notes/core/markdown/MarkdownEditActionsTest.kt` (toolbar insertion tests)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.10)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - Lenovo TB320FC Android 15 `v1.0.10` release APK install and launch check
  - `rg "android.permission.INTERNET" -n app\src`
- Result: Bold, Italic, Checkbox, Markdown Link, Wiki Link, Image 액션을 에디터 툴바에서 사용할 수 있게 함

## 2026-05-01
- Work: Markdown 링크 Preview 처리와 설정 화면 보강. GitHub Issue #17 등록 후 구현.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreview.kt` (inline note/markdown link parsing)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (clickable inline link rendering)
  - `app/src/main/java/com/markleaf/notes/feature/settings/SettingsScreen.kt` (back button and settings sections)
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (settings back navigation)
  - `app/src/test/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreviewTest.kt` (inline link tests)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.9)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - Lenovo TB320FC Android 15 `v1.0.9` release APK install and launch check
  - `rg "android.permission.INTERNET" -n app\src`
- Result: Preview 모드의 문장 중간 노트 링크와 Markdown 링크 표시를 개선하고 설정 화면의 닫기/정보 구조를 보강

## 2026-05-01
- Work: Bear 벤치마크 기반 제품 갭을 정리하고 첫 실행 샘플 노트 온보딩을 추가. GitHub Issue #15 등록 후 구현.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/data/onboarding/StarterNotesSeeder.kt` (first-run starter notes)
  - `app/src/main/java/com/markleaf/notes/MainActivity.kt` (startup seeding)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/NoteDao.kt` (note count query)
  - `app/src/test/java/com/markleaf/notes/data/onboarding/StarterNotesSeederTest.kt` (seed tests)
  - `docs/BEAR_BENCHMARK_GAP.md` (Bear-class product gap review)
  - `docs/ROADMAP.md` and `.agent/tasks.md` (Phase 9 plan)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.8)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - Lenovo TB320FC Android 15 `v1.0.8` release APK install and launch check
  - `./gradlew.bat connectedDebugAndroidTest` was not completed because the signed release APK on the tablet rejected debug APK update due to signature mismatch
- Result: 신규 설치 사용자가 빈 화면 대신 Markdown/태그/링크/백업 예시 노트로 앱을 시작할 수 있게 함

## 2026-05-01
- Work: 안정성 및 MVP 스펙 보강. GitHub Issue #14 등록 후 저장/태그/내비게이션/테스트 라인을 수정.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (title/excerpt/tag/backlink save path)
  - `app/src/main/java/com/markleaf/notes/data/local/AppDatabase.kt` (v5 migration, destructive migration removal)
  - `app/src/main/java/com/markleaf/notes/data/local/entity/NoteTagCrossRef.kt` (string note IDs)
  - `app/src/main/java/com/markleaf/notes/feature/notes/NotesListScreen.kt` (top-level navigation actions)
  - `app/src/main/java/com/markleaf/notes/feature/tags/TagsScreen.kt` (local tag list)
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (route fixes)
  - `app/build.gradle.kts` (v1.0.7, test runner, androidTest dependencies)
  - repository and instrumentation tests
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat connectedDebugAndroidTest`
  - Lenovo TB320FC Android 15 `v1.0.7` release APK launch check
- Result: 앱 시작, 기본 작성/검색 진입/태그 저장/기기 테스트 라인을 안정화

## 2026-05-01
- Work: 앱 실행 직후 종료되는 시작 크래시 수정. GitHub Issue #13 등록 후 수정 진행.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/MainActivity.kt` (root repository/factory wiring)
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (explicit ViewModel factory usage)
  - `app/src/main/java/com/markleaf/notes/ui/viewmodel/MarkleafViewModelFactory.kt` (added)
  - `app/src/test/java/com/markleaf/notes/ui/viewmodel/MarkleafViewModelFactoryTest.kt` (added)
  - `app/build.gradle.kts` (updated to v1.0.6 / versionCode 7)
  - `CHANGELOG.md` (added v1.0.6)
  - `.agent/progress.md` (updated)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat assembleDebug`
  - `./gradlew.bat assembleRelease`
  - `rg "android.permission.INTERNET" -n app`
- Result: 앱 시작 경로의 ViewModel 생성 실패를 수정 완료

## 2026-04-30
- Work: 릴리즈 제목 규칙을 `vX.Y.Z - 한국어 제목 (English Title)` 형식으로 복구.
- Changed files:
  - `.github/workflows/android-build.yml` (release title extraction from `CHANGELOG.md`)
  - `app/build.gradle.kts` (updated to v1.0.5 / versionCode 6)
  - `CHANGELOG.md` (added v1.0.5 and normalized release headings)
  - `HISTORY.md` (updated)
  - `.agent/decisions.md` (release title policy added)
  - `.agent/progress.md` (updated)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat assembleDebug`
  - `./gradlew.bat assembleRelease`
  - Updated existing `v1.0.2`, `v1.0.3`, and `v1.0.4` GitHub Release titles
  - Renamed existing `v1.0.2` release asset to `markleaf-v1.0.2.apk`
- Result: GitHub Release 제목이 changelog heading을 따르도록 정리 완료

## 2026-04-30
- Work: 릴리즈 노트 본문을 한글 changelog 기준으로 보정.
- Changed files:
  - `CHANGELOG.md` (converted v1.0.2-v1.0.5 release notes to Korean)
  - `.agent/decisions.md` (added Korean release note body rule)
  - `.agent/progress.md` (updated)
  - `HISTORY.md` (updated)
- Verification:
  - Updated existing `v1.0.2`, `v1.0.3`, and `v1.0.4` GitHub Release notes to Korean
- Result: GitHub Release 본문은 한글 changelog 섹션을 사용하도록 정리

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

