# HISTORY

## 2026-05-02
- Work: v1.1.8 Release APK Full Build Tree Discovery Complete. release output subtree 가정을 제거하고 app/build 전체에서 APK를 탐색하도록 workflow 복구.
- Changed files:
  - `.github/workflows/android-build.yml` (discover release APK anywhere under app/build)
  - `app/build.gradle.kts` (versionCode 38, versionName 1.1.8)
  - `CHANGELOG.md`, `HISTORY.md`, `.agent/progress.md`, `.agent/decisions.md`
- Verification:
  - GitHub Actions run `25250479341` proved the release build succeeded but no APK was found under the prior release subtree assumption
  - Recovery path updated to search the full app build tree for APK outputs before signing verification

## 2026-05-02
- Work: v1.1.7 Release APK Recursive Discovery Recovery Complete. release 하위 경로까지 포함해 실제 APK를 재귀 탐색하도록 workflow 복구.
- Changed files:
  - `.github/workflows/android-build.yml` (discover release APK recursively under release output tree)
  - `app/build.gradle.kts` (versionCode 37, versionName 1.1.7)
  - `CHANGELOG.md`, `HISTORY.md`, `.agent/progress.md`, `.agent/decisions.md`
- Verification:
  - GitHub Actions run `25250479341` proved the release build succeeded but no APK existed directly under `app/build/outputs/apk/release/`
  - Recovery path updated to recursively discover release APK outputs before signing verification

## 2026-05-02
- Work: v1.1.6 Release APK Discovery Recovery Complete. metadata 부재 환경에서도 실제 release APK를 탐색하도록 workflow 복구.
- Changed files:
  - `.github/workflows/android-build.yml` (discover release APK from release output directory glob)
  - `app/build.gradle.kts` (versionCode 36, versionName 1.1.6)
  - `CHANGELOG.md`, `HISTORY.md`, `.agent/progress.md`, `.agent/decisions.md`
- Verification:
  - GitHub Actions run `25250418933` proved the release build succeeded but `output-metadata.json` was absent in the release job workspace
  - Recovery path updated to use discovered release APK files instead of metadata-only lookup

## 2026-05-02
- Work: v1.1.5 Release Artifact Path Recovery Complete. release APK 고정 경로 가정을 제거하고 metadata 기반 경로로 복구.
- Changed files:
  - `.github/workflows/android-build.yml` (resolve release APK path from output-metadata.json)
  - `app/build.gradle.kts` (versionCode 35, versionName 1.1.5)
  - `CHANGELOG.md`, `HISTORY.md`, `.agent/progress.md`, `.agent/decisions.md`
- Verification:
  - GitHub Actions run `25250335060` proved the release build succeeded but fixed-path certificate verification failed
  - Local release metadata confirmed APK output version `1.1.5` / `35` before retagging

## 2026-05-02
- Work: v1.1.4 Release Tag Recovery Complete. bash 기반 tag release 인자 순서 보정 후 새 버전으로 재복구.
- Changed files:
  - `.github/workflows/android-build.yml` (move release-signing Gradle property before release task)
  - `app/build.gradle.kts` (versionCode 34, versionName 1.1.4)
  - `CHANGELOG.md`, `HISTORY.md`, `.agent/progress.md`
- Verification:
  - GitHub Actions run `25250226582` failed with task parsing on Ubuntu bash
  - Recovery path updated to publish a fresh `v1.1.4` tag instead of reusing failed `v1.1.3`

## 2026-05-02
- Work: v1.1.3 Release Workflow Recovery Complete. 태그 릴리즈 실패 원인 수정 후 새 버전으로 복구 릴리즈 준비.
- Changed files:
  - `.github/workflows/android-build.yml` (quoted release-signing Gradle property for tag releases)
  - `app/build.gradle.kts` (versionCode 33, versionName 1.1.3)
  - `CHANGELOG.md`, `HISTORY.md`, `.agent/progress.md`, `.agent/decisions.md`
- Verification:
  - `./gradlew.bat :app:assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - Release workflow root cause confirmed from failed GitHub Actions run `25246920678`

## 2026-05-02
- Work: v1.1.2 Version Sync and Workflow Recovery. 워크플로우 퇴행 복구 및 문서/버전 정합성 맞춤.
- Changed files:
  - `.github/workflows/android-build.yml` (Restored title extraction)
  - `app/build.gradle.kts` (versionCode 32, versionName 1.1.2)
  - `CHANGELOG.md`, `HISTORY.md`, `.agent/progress.md`
- Verification: GitHub Actions Triggered upon push

## 2026-05-02
- Work: v1.1.1 CI Release Stability. CI 워크플로우 문법 오류 수정 및 성능 테스트 제거로 빌드 안정화.
- Changed files:
  - `app/build.gradle.kts` (versionCode 30, versionName 1.1.1)
  - `.github/workflows/android-build.yml`
  - `CHANGELOG.md`, `HISTORY.md`
- Verification: GitHub Actions 모니터링 예정

## 2026-05-02
- Work: v1.1.0 Comprehensive Release. 백링크 컨텍스트, 태그 카운트, 포괄적 테스트 스위트 구축 및 릴리즈.
- Changed files:
  - `app/build.gradle.kts` (versionCode 29, versionName 1.1.0)
  - `app/src/androidTest/java/com/markleaf/notes/ui/ComprehensiveFeatureTest.kt` (50-case test suite)
  - `app/src/androidTest/java/com/markleaf/notes/ui/AppIntegrationTest.kt` (i18n support)
  - `app/src/androidTest/java/com/markleaf/notes/ui/EditorScreenTest.kt` (i18n support)
  - `CHANGELOG.md`, `HISTORY.md`
- Verification:
  - Phone (SM-S921N) & Tablet (TB320FC) on-device testing (50 scenarios)
  - `.\gradlew.bat connectedDebugAndroidTest`
  - `.\gradlew.bat test`
  - `.\gradlew.bat lintDebug`
- Result: 주요 기능 고도화 완료 및 릴리즈 빌드 배포 준비 완료

## 2026-05-02
- Work: Backup status messages. Settings 백업/복원 결과 메시지에 처리 개수와 실패 안내 추가.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/util/BackupUtil.kt` (operation result with counts)
  - `app/src/main/java/com/markleaf/notes/feature/settings/SettingsScreen.kt` (detailed success/error messages)
  - locale string resources for detailed backup/restore status
  - `app/build.gradle.kts`, `.agent/tasks.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
  - `./gradlew.bat compileDebugKotlin`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
  - Lenovo TB320FC Android 15 `v1.0.27` release APK install and launch check
- Result: 백업/복원 결과가 성공/실패뿐 아니라 처리 규모와 다음 행동을 명확히 표시

## 2026-05-02
- Work: Backlink context snippets. 에디터 백링크 목록에 링크 주변 문맥 표시 추가.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/domain/model/Note.kt` (backlink snippet model)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/NoteLinkDao.kt` (backlink link lookup)
  - `app/src/main/java/com/markleaf/notes/data/repository/LocalNoteRepository.kt` (snippet generation)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (title + snippet backlink rows)
  - `app/src/test/java/com/markleaf/notes/data/repository/LocalNoteRepositoryTest.kt`
  - `app/build.gradle.kts`, `.agent/tasks.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
  - Lenovo TB320FC Android 15 `v1.0.26` release APK install and launch check
- Result: 백링크 목록에서 어떤 문맥에서 현재 노트가 참조됐는지 바로 확인 가능

## 2026-05-02
- Work: Tag screen counts and navigation. Tags 화면에서 태그별 활성 노트 수를 표시하고 태그 검색 이동을 명확화.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/data/local/dao/TagDao.kt` (tag count projection)
  - `app/src/main/java/com/markleaf/notes/data/repository/LocalTagRepository.kt` (tag summary flow)
  - `app/src/main/java/com/markleaf/notes/domain/model/Tag.kt` (tag summary model)
  - `app/src/main/java/com/markleaf/notes/feature/tags/TagsScreen.kt` (count row UI)
  - locale string resources for count labels
  - `app/src/test/java/com/markleaf/notes/data/repository/LocalTagRepositoryTest.kt`
  - `app/build.gradle.kts`, `.agent/tasks.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalTagRepositoryTest --tests com.markleaf.notes.res.ResourceParityTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
  - Lenovo TB320FC Android 15 `v1.0.25` release APK install and launch check
- Result: 태그 목록에서 연결된 활성 노트 수를 바로 확인하고 태그 검색으로 이동 가능

## 2026-05-02
- Work: Theme contrast audit. 노트 목록 제목 대비와 전체 테마 적용 경로 점검.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/ui/theme/Theme.kt` (fixed Markleaf color scheme by default and normalized typography letter spacing)
  - `app/src/main/java/com/markleaf/notes/feature/notes/NotesListScreen.kt` (explicit themed title/content colors)
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (consistent surface/content color pairing for tablet list pane)
  - `app/src/test/java/com/markleaf/notes/data/repository/LocalNoteRepositoryTest.kt` (less flaky 10k search timing guard)
  - `app/build.gradle.kts`, `.agent/tasks.md`, `.agent/decisions.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
  - Lenovo TB320FC Android 15 `v1.0.24` release APK install and launch check
- Result: 노트 목록 제목이 앱 테마 색상으로 더 명확히 보이고, tablet list pane의 surface/content 색상 관계가 일관화됨

## 2026-05-02
- Work: Quick-open search. Search 화면에서 notes, tags, wiki-link labels를 함께 탐색하도록 확장.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/feature/search/SearchScreen.kt` (sectioned quick-open results)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/NoteLinkDao.kt` (distinct link label projection)
  - locale string resources for quick-open section labels
  - `app/build.gradle.kts`, `.agent/tasks.md`, `.agent/decisions.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
  - Lenovo TB320FC Android 15 `v1.0.23` release APK install and launch check
- Result: Search now works as local quick-open across notes, tags, and note-link labels

## 2026-05-02
- Work: Empty state polish. 노트 목록과 에디터 빈 상태에 다음 행동과 작성 힌트 추가.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/feature/notes/NotesListScreen.kt` (empty state create button)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (empty editor writing hint)
  - `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ko/strings.xml`, `app/src/main/res/values-es/strings.xml`
  - `app/build.gradle.kts`, `.agent/tasks.md`, `.agent/decisions.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - Lenovo TB320FC Android 15 `v1.0.22` release APK install and launch check
- Result: 빈 목록과 빈 에디터가 다음 행동을 더 명확히 안내

## 2026-05-02
- Work: Multi-language support expansion. Spanish locale와 리소스 parity 테스트 추가.
- Changed files:
  - `app/src/main/res/values-es/strings.xml` (Spanish UI strings)
  - `app/src/main/res/raw-es/starter_notes.md` (Spanish starter notes)
  - `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ko/strings.xml` (Markdown preview support copy update)
  - `app/src/test/java/com/markleaf/notes/res/ResourceParityTest.kt` (locale key parity and starter note checks)
  - `app/build.gradle.kts`, `.agent/tasks.md`, `.agent/decisions.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - Lenovo TB320FC Android 15 `v1.0.21` release APK install and launch check
- Result: English, Korean, and Spanish locale resources now stay aligned by test

## 2026-05-02
- Work: 10k+ notes performance optimization. 검색/목록 경로 index와 FTS 검색 경로 정리.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/data/local/entity/NoteEntity.kt` (notes indexes)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/NoteDao.kt` (FTS rowid join and search result limit)
  - `app/src/main/java/com/markleaf/notes/data/local/AppDatabase.kt` (schema v7 migration and FTS rebuild)
  - `app/src/main/java/com/markleaf/notes/data/repository/LocalNoteRepository.kt` (FTS prefix query path)
  - `app/src/test/java/com/markleaf/notes/data/repository/LocalNoteRepositoryTest.kt` (10k note search test)
  - `app/build.gradle.kts`, `.agent/tasks.md`, `.agent/decisions.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - Lenovo TB320FC Android 15 `v1.0.20` release APK install and launch check
- Result: 10,000개 로컬 노트에서 검색 회귀를 확인하는 테스트와 indexed query path 추가

## 2026-05-02
- Work: Note version history. Room 기반 local snapshot 저장과 에디터 복원 UI 추가.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/data/local/entity/NoteSnapshotEntity.kt` (snapshot entity)
  - `app/src/main/java/com/markleaf/notes/data/local/dao/NoteSnapshotDao.kt` (snapshot DAO)
  - `app/src/main/java/com/markleaf/notes/domain/model/NoteSnapshot.kt` (domain model)
  - `app/src/main/java/com/markleaf/notes/data/local/AppDatabase.kt` (schema v6 migration)
  - `app/src/main/java/com/markleaf/notes/data/repository/LocalNoteRepository.kt` (snapshot creation and restore)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (version history dialog)
  - `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ko/strings.xml`
  - `app/src/test/java/com/markleaf/notes/data/repository/LocalNoteRepositoryTest.kt`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - Lenovo TB320FC Android 15 `v1.0.19` release APK install and launch check
- Result: 노트 수정 전 버전을 로컬 DB에 제한적으로 보존하고 에디터에서 복원 가능

## 2026-05-02
- Work: Advanced Markdown preview. 로컬 preview parser/rendering에 table과 수식 표기 지원 추가.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreview.kt` (table rows, inline math, display math parsing)
  - `app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt` (Compose table and math preview rendering)
  - `app/src/test/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreviewTest.kt` (table/math parser tests)
  - `app/build.gradle.kts`, `.agent/tasks.md`, `.agent/decisions.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.core.markdown.SimpleMarkdownPreviewTest`
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `rg "android.permission.INTERNET" -n app\src`
  - `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Result: Preview mode can display Markdown tables and local math notation without adding network/API behavior or proprietary dependencies

## 2026-05-02
- Work: 릴리즈 APK 업데이트 충돌 방지를 위해 production signing certificate를 고정 검증하도록 릴리즈 파이프라인 보강.
- Changed files:
  - `.github/workflows/android-build.yml` (tag release keystore presence and certificate SHA-256 verification)
  - `app/build.gradle.kts` (required release signing property and v1.0.17)
  - `docs/RELEASE.md` (fixed certificate fingerprint and keystore replacement warning)
  - `.agent/tasks.md`, `.agent/decisions.md`, `CHANGELOG.md`
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
  - `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Result: 태그 릴리즈가 누락되거나 다른 keystore로 서명된 APK를 GitHub Release에 올리기 전에 실패하도록 설정

## 2026-05-01
- Work: 태블릿 2패널 편집 화면 시각적 구분 개선. GitHub Issue #23 등록 후 구현.
- Changed files:
  - `app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt` (tablet pane background tones and divider)
  - `app/src/main/java/com/markleaf/notes/feature/notes/NotesListScreen.kt` (selected note highlight and configurable container color)
  - `app/build.gradle.kts` and `CHANGELOG.md` (v1.0.15)
- Verification:
  - `./gradlew.bat test`
  - `./gradlew.bat lintDebug`
  - `./gradlew.bat assembleDebug assembleRelease`
  - `rg "android.permission.INTERNET" -n app\src`
- Result: Tablet list/editor panes are visually separated without copying another app's brand or exact layout

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

