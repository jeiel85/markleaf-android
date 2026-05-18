## 2026-05-18 - F-Droid / Build Readiness Cleanup

- Selected task: `[Commercial P0-3] Room schema export + migration regression test`, plus repository cleanup items that can be completed locally before F-Droid submission.
- Work:
  - `AppDatabase` changed to `exportSchema = true`.
  - `app/build.gradle.kts` now sets KSP Room args (`room.schemaLocation`, incremental mode, expandProjection) and exposes `app/schemas` to androidTest assets.
  - Generated and committed `app/schemas/com.markleaf.notes.data.local.AppDatabase/12.json`.
  - Added `AppDatabaseMigrationTest`, which creates a legacy v4 DB and verifies migration to v12 preserves notes/tags, rebuilds FTS, and creates `note_links`, `attachments`, `sortOrder`, and `lastImportedAt` surfaces.
  - Added root `LICENSE` (Apache 2.0) to match README and F-Droid expectations.
  - Removed tracked `local.properties`; it remains ignored locally and should not participate in reproducible builds.
  - Added English/Korean fastlane short/full descriptions for reuse in F-Droid/store metadata.
  - Added `android.suppressUnsupportedCompileSdk=35` with a comment because API 35 targeting is intentional on the current AGP baseline.
- Verification:
  - `./gradlew.bat :app:kspDebugKotlin :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL
  - `./gradlew.bat --no-daemon test` → BUILD SUCCESSFUL
  - `./gradlew.bat --no-daemon :app:lintRelease :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL
  - `rg "android.permission.INTERNET" -n app/src` → no matches
  - APK outputs: debug APK 17,847,351 bytes; release APK 1,759,320 bytes; androidTest APK 1,864,036 bytes.
  - `./gradlew.bat --no-daemon :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.markleaf.notes.data.local.AppDatabaseMigrationTest'` did not run tests because the connected device already has a production-signed `com.markleaf.notes` installed. Debug install failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; the app was not uninstalled to avoid deleting user data.
- Follow-up:
  - F-Droid submission itself still needs the external procedural steps: upstream tag push, metadata PR, screenshots/feature graphics if desired, and final signed/reproducible-build review.

## 2026-05-14 - v2.15.0 cut (Commercial P0-1/P0-2/P0-4 묶음 출시)

- Selected task: v2.15.0 chore 출시 — Phase 22 P0-1 (backup 정책) / P0-2 (R8 + CI gates) / P0-4 (privacy 문서 v2.x화) 세 작업을 한 chore 릴리즈로 묶음. 신기능 0.
- Work:
  - `app/build.gradle.kts` — versionCode 84 → 85, versionName 2.14.0 → 2.15.0.
  - `CHANGELOG.md` — 기존 두 "## Unreleased" 섹션을 단일 `## v2.15.0 - Play 정식 출시 준비: 자동 백업 제외 + 빌드 최적화 - 2026-05-14` 로 통합. Tag 릴리즈 자동화가 `## v<버전> - ...` 헤더에서 release-notes 본문을 awk 로 잘라 GitHub Release notes 로 사용하므로 헤더 포맷 유지.
  - `fastlane/metadata/android/ko-KR/changelogs/85.txt` (NEW, 261자) + `fastlane/metadata/android/en-US/changelogs/85.txt` (NEW, 398자) — Play Console "What's new" 입력용 로컬라이즈 릴리즈 노트. 둘 다 500자 제한 안. 사용자가 Play Console 업로드 시 그대로 복사하거나 fastlane supply 로 자동 동기화 가능.
  - `README.md` 현재 버전 배지를 v2.14.0 → v2.15.0.
  - `docs/NOCLOUD_CERTIFICATION.md` 버전 라인 2.14.0+ → 2.15.0+.
  - `.gitignore` — `/dist/` 추가 (로컬 staging 디렉터리; CI가 tag push 시 동일 산출물 생성).
  - `.agent/tasks.md` — Commercial P0-4 체크 (P0-1 작업으로 docs 모두 v2.x 화 + "MVP draft" 잔존 0 확인).
  - 로컬 서명 AAB/APK/mapping.txt 생성 후 `dist/v2.15.0/markleaf-v2.15.0.{aab,apk,mapping.txt}` 로 스테이징 (gitignored). 사용자가 Play Console 비공개 테스트에 그대로 업로드 가능.
- Context:
  - v2.14.0(84) 가 비공개 테스트에 올라가 있는 상태. v2.15.0(85) 는 그 위에 R8 + backup 정책 + 문서 정밀화를 얹는 chore. *신기능 0* 인 만큼 closed test 사용자의 회귀 검증 부담이 작고, R8-shrunk APK 의 첫 실기기 smoke 로서 적절한 step.
  - 릴리즈 노트의 사용자 가시 변화는 두 가지 (자동 백업 제외 + 앱 크기 감소). 나머지(CI gate, proguard 규칙, lint 픽스) 는 개발자/기술 변화라 Play Console 노트에는 포함하지 않음.
- Verification:
  - `./gradlew :app:test` → BUILD SUCCESSFUL
  - `./gradlew :app:lintRelease` → BUILD SUCCESSFUL
  - `./gradlew :app:assembleRelease` → signed APK 1.7 MB. apksigner 검증 결과 SHA-256 = `0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a` 로 production cert 일치.
  - `./gradlew :app:bundleRelease` → signed AAB 4.0 MB.
  - `rg "android.permission.INTERNET" -n app/src` → no matches.
- Follow-up:
  - Tag `v2.15.0` push → CI 가 동일 산출물 + mapping 을 GitHub Release 자산으로 생성 → 사용자 비공개 테스트 업로드.
  - 비공개 테스트 실기기에서 Compose 라이브 프리뷰 / Room SQL / Coil 이미지 / commonmark 렌더 / SAF 폴더 미러 / 시스템 공유 시트 / FileProvider 공유 / AppWidget 흐름 manual smoke. R8 가 무언가를 strip 했으면 ANR / NoClassDefFoundError 로 표면화.
  - 다음 사이클: Phase 22 Commercial P0-3 (Room schema export + migration regression test).

## 2026-05-14 - Commercial P0-2: Release Hardening (R8 + CI gates)

- Selected task: `[Commercial P0-2] Release hardening` — Phase 22 의 다음 unchecked 항목.
- Work:
  - **R8 + resource shrink 활성화.** `app/build.gradle.kts` 의 release 블록에서 `isMinifyEnabled = true`, `isShrinkResources = true`, `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`.
  - **`app/proguard-rules.pro` 신규.** Conservative keep rules: Room entity 클래스/멤버, `AppSettings`, `SyncFrontmatter`/`NoteFolderMirror`, `QuickNoteWidget`, kotlinx.coroutines volatile 필드. `android.util.Log.d/v/i` 는 release 에서 R8 가 inline fold. 각 rule 마다 *왜 필요한지* 주석 — 미래에 의존성 업그레이드로 coupling 이 사라지면 제거할 수 있도록.
  - **벤치마크 변형도 자동으로 R8 상속** (`initWith(release)`). Macrobenchmark 가 더 release-like 한 APK 를 측정.
  - **`.github/workflows/android-build.yml` build job 에 hard gate 추가**:
    - `./gradlew :app:lintRelease` — failure 시 `lint-results-release.html` 을 artifact 로 업로드.
    - `./gradlew :app:assembleRelease` — R8 가 valid APK 를 만들어내는지 매 빌드 검증, 크기 > 0 확인.
    - `mapping.txt` 를 `markleaf-r8-mapping` artifact 로 업로드.
  - **Tag 릴리즈 잡** 에 `markleaf-vX.Y.Z.mapping.txt` 도 GitHub Release 자산으로 첨부 — Play Console / 외부 크래시 리포트에서 stack trace deobfuscation 가능.
  - **`EditorLiveSnapshotTest.kt`** 의 `remember(scheme) { MarkdownSyntaxVisualTransformation(...) }` 한 줄에 `@Suppress("RememberReturnType")` — test 소스 셋 경계 너머의 생성자 반환 타입을 lint 가 해석 못 하는 false positive. 한 줄에만 침묵.
  - **`docs/RELEASE.md`** 에 R8/mapping/CI gate 섹션 추가.
  - **`.agent/decisions.md`** D047 추가 — R8 enablement 결정 + minimal proguard 정책 + mapping artifact 결정.
  - **CHANGELOG / .agent/tasks.md / .agent/progress.md** 갱신.
- Context:
  - 상용화 게이트의 핵심: release 빌드가 단순히 "컴파일은 된다" 수준이 아니라 production-quality (R8 통과 + lint 통과 + 크래시 deobfuscatable) 인지 매 빌드 보장.
  - APK 크기 12 MB → 1.7 MB (87% 감소) 는 부수적 효과지만 사용자 다운로드 / 업데이트 비용 측면에서 큰 win. Play Store 의 instant delivery threshold 와 무관하지만 비공개 테스트 사용자 경험에 직접 반영.
  - launch-smoke 는 `continue-on-error: true` 그대로 유지. release-APK runtime smoke (R8 가 런타임에 무언가를 strip 했는지 검증) 는 별도 사이클 — 현 launch-smoke 의 emulator 플레이크가 release path 까지 confound 하지 않도록.
- Verification:
  - `./gradlew :app:test` → BUILD SUCCESSFUL
  - `./gradlew :app:lintRelease` → BUILD SUCCESSFUL (warning 만 남고 error 0)
  - `./gradlew :app:assembleRelease` → 1.7 MB APK
  - `./gradlew :app:bundleRelease` → 4.0 MB AAB
  - `app/build/outputs/mapping/release/mapping.txt` 32 MB 생성
- Follow-up:
  - R8-shrunk APK 의 emulator/실기기 런타임 smoke test (Compose 슬롯, Room SQL, Coil 이미지 로딩, commonmark 렌더, SAF, FileProvider, AppWidget 흐름 전수 확인).
  - 의존성 업그레이드 사이클 (lintRelease 에서 발견된 lifecycle/activity/room/coil/test 라이브러리 신버전).
  - Phase 22 Commercial P0-3: Room schema export + migration regression test.

## 2026-05-14 - Commercial P0-1: Android Backup / Data Extraction 정책 확정

- Selected task: `[Commercial P0-1] Android Backup / Data Extraction 정책 확정` — Phase 22 의 첫 unchecked 항목.
- Work:
  - `AndroidManifest.xml` 의 `<application>` 에 `android:allowBackup="false"` 설정. Android Auto Backup(Google Drive) 과 Android 12+ Device-to-Device transfer 양쪽 모두에서 Markleaf 데이터 제외.
  - 벤치마크 변형의 `tools:replace="android:allowBackup"` 오버라이드 제거 — main 의 `false` 를 그대로 상속해 정책 일관성 유지. `<profileable shell="true" />` 는 그대로 유지.
  - `docs/PRIVACY.md` 전면 재작성 — "MVP Privacy Policy Draft" 폐기, v2.x 기능 기준으로 "Markleaf 자체 네트워크 0 + 사용자 명시 행동 시 OS 경로로만 이동" 을 정밀하게 구분. 시스템 백업/D2D 제외 정책 명시.
  - `docs/SECURITY.md` 갱신 — `allowBackup="false"` 결정 근거 + `dataExtractionRules` 미선택 사유 + 사용자 주도 데이터 이동 경로(외부 링크 ACTION_VIEW 위임 포함) 정리.
  - `docs/NOCLOUD_CERTIFICATION.md` — "Backup / Data Extraction" 섹션 신설, "What Can Leave the Device" 를 명시적 사용자 행동 기준으로 재서술, 외부 링크는 OS 위임이라는 사실 추가.
  - `README.md` — "100% No-Cloud" 카피를 "Markleaf 자체는 네트워크에 나가지 않음 + 사용자 선택 경로로만 이동" 의 정밀 표현으로 교체. `allowBackup="false"` 도 강점 목록에 포함.
  - `CHANGELOG.md` Unreleased 섹션 추가.
  - `.agent/decisions.md` 에 D046 추가 — `allowBackup="false"` vs `dataExtractionRules` 비교와 결론.
  - `.agent/tasks.md` 에서 Commercial P0-1 항목 체크.
- Context:
  - Markleaf 의 핵심 약속은 "사용자가 직접 export/share 하기 전까지 데이터가 기기 밖으로 나가지 않는다". 일부 사용자에게 활성화돼 있을 수 있는 OS 차원의 Google 드라이브 자동 백업이 무언의 충돌을 일으킬 여지를 차단하는 게 목적.
  - `dataExtractionRules` 는 부분적 제외에 유용하지만 *전체 제외* 라면 `allowBackup="false"` 가 더 단순/명시적이라 후자를 택함. minSdk=26 환경에서 legacy + new 메커니즘을 동시에 관리하는 표면적도 줄어듦.
  - 다중 기기 사용자는 v2.1 SAF 폴더 미러 동기화로 *사용자가 선택한 폴더* 를 통해 이동 가능 — 이 경로는 사용자의 명시적 SAF 다이얼로그를 거치므로 정책 충돌 없음.
- Verification:
  - `./gradlew test` → BUILD SUCCESSFUL
  - `./gradlew assembleDebug` → BUILD SUCCESSFUL
  - `rg "android.permission.INTERNET" -n app/src` → no matches (정책 유지)
  - `rg "allowBackup|dataExtractionRules" -n app/src/main` → `AndroidManifest.xml` 의 `android:allowBackup="false"` 단일 매치
- Follow-up:
  - Phase 22 Commercial P0-2 (Release hardening: R8/shrink + release lint/build/bundle/signing/smoke gate) 다음.
  - 향후 사용자 설정만 별도 백업하는 정책으로 바꾸면 `dataExtractionRules` 도입을 재검토.

## 2026-05-14 - Commercial Readiness Planning

- Work: 상용화 관점의 냉정 평가를 다음 세션에서 바로 이어갈 수 있는 실행 계획으로 정리.
- Changed files:
  - `docs/COMMERCIAL_READINESS_PLAN.md` — v2.14.0 기준 상용화 준비도 평가, P0/P1/P2 개선 설계안, 수정 후보 파일, 검증 명령 정리.
  - `.agent/tasks.md` — Phase 22 Commercial Readiness backlog 추가.
  - `HISTORY.md` — 본 기록 추가.
- Context:
  - 기능 구현은 이미 비공개 테스트 앱 수준을 넘었지만, 정식 출시 전에는 데이터 보호, release hardening, Room migration safety, privacy/security 문서 현재화, sync 장애 가시성을 우선해야 한다.
  - 다음 권장 시작 작업은 `[Commercial P0-1] Android Backup / Data Extraction 정책 확정`.
- Verification:
  - Documentation-only change; Gradle build not run.

## 2026-05-13 - GitHub Issue 기준 문서 일괄 정리

- Work: GitHub 오픈 이슈 12건을 기준으로 프로젝트 문서 전체를 재정렬. 손상된 HISTORY.md 6000줄 정리.
- Changed files:
  - `.agent/tasks.md` — Phase 21 (GitHub Open Issues) 신설. 이슈별 상태 추적 (완료 3건, 보류 3건, 미구현 6건)
  - `docs/ROADMAP.md` — v2.x 완료 항목 정리 + GitHub Open Issues 향후 작업 섹션으로 교체
  - `HISTORY.md` — 손상된 중복 항목(약 6000줄) 제거
  - `.agent/decisions.md` — Pending Decisions → Resolved 로 이동 (DI, minSdk, Markdown lib, FTS, 이미지, tablet, flavor)
  - `docs/NOCLOUD_CERTIFICATION.md` — 버전 2.14.0+ 갱신, 권한 목록 VIBRATE 단독으로 수정
  - `GEMINI.md` — AGENTS.md 참조 중심으로 간소화
- Context:
  - GitHub 오픈 이슈 12건: #27(완료), #37(미구현), #38(미구현), #39(미구현), #51(보류), #52(미구현), #53(보류), #54(보류), #55(미구현), #57(미구현), #65(완료), #76(완료)
  - 완료 3건(#27, #65, #76)은 GH close 필요, 보류 3건은 외부 조건, 미구현 6건은 backlog

## 2026-05-08 (밤) - v2.1.0
- Work: 가치관 점검에서 사용자가 "결국 다중 기기 지원은 맞고 가치관 뒤집기보다 확장에 가까움"이라 말한 직후 합의된 v1.9 → v2.0 → v2.1 3단계의 마지막. CloudKit식 vendor lock 대신 SAF 폴더 미러 모델(Option D).
- Changed files:
  - data/settings/AppSettings.kt — `syncFolderUri: String?`, `syncLastSyncedAt: Long?` 필드 추가
  - data/settings/AppSettingsRepository.kt — `SYNC_FOLDER_URI`(stringPref) + `SYNC_LAST_SYNCED_AT`(longPref) 키, `setSyncFolderUri(uri)` / `setSyncLastSyncedAt(epochMillis)` setter. 폴더 끌 때 `lastSyncedAt`도 함께 정리.
  - data/sync/SyncFrontmatter.kt (NEW, ~110 LOC) — YAML frontmatter encoder/decoder. 표준 키(`markleaf_id`/`created_at`/`updated_at`/`pinned`/`archived`) + 알 수 없는 키들의 opaque 보존(round-trip 친화). 자체 YAML 파서 없이 라인 단위 `key: value` 처리, ISO instant 포맷.
  - data/sync/NoteFolderMirror.kt (NEW, ~170 LOC) — SAF DocumentFile 기반 read/write. `writeNote(context, uri, note)` (idempotent, id-marker 기반 파일 매칭) + `importChanges(context, uri, existing, applyUpdate, applyCreate)` (수동 reconcile, 새 파일은 신규 노트로 import, 기존은 file 타임스탬프가 더 새로울 때만 업데이트). `ImportResult(updated, created, skipped, errors)` 반환.
  - feature/editor/EditorScreen.kt — 기존 1초 디바운스 자동 저장 LaunchedEffect 안에 `appSettings.syncFolderUri` 가 있을 때 `withContext(IO) { NoteFolderMirror.writeNote(...) }` 호출 추가. `Dispatchers.IO`/`withContext` import 추가.
  - feature/settings/SettingsScreen.kt — `syncFolderLauncher` (`OpenDocumentTree` SAF launcher, persistable URI permission take + 첫 미러링 seed) 추가. 새 `SyncSection` 컴포저블 — 미선택 상태 / 선택된 상태 둘 다 명시적 카피. "지금 동기화" 버튼은 DB와 폴더 reconcile 후 결과 Toast(`updated/created/skipped`). `humanReadableTreePath()` 헬퍼로 SAF tree URI를 사람 친화 경로로 표시. `formatRelative()` 헬퍼로 마지막 sync 시각 표시.
  - res/values{,-ko,-es}/strings.xml — sync_title, sync_explainer, sync_recommended_locations, sync_status_unset/folder_format/last_synced_format/last_synced_never, sync_behavior_summary, sync_pick_folder, sync_change_folder, sync_now, sync_stop, sync_seeded_format, sync_done_format, sync_stopped (총 14키 × 3언어). ResourceParityTest 통과.
  - test/data/sync/SyncFrontmatterTest.kt (NEW) — 8개 단위 테스트: encode shape, full round-trip, frontmatter 없는 파일, 닫지 않은 frontmatter, 알 수 없는 키 보존, quoted values, pinned 키 부재 vs explicit false 등.
  - app/build.gradle.kts (versionCode 64, versionName 2.1.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - 사용자 명시 요청: "이 기능에 대한 명확한 설명이 필요" → Settings UI에서 *동작 원리*를 3-4줄로 풀어서 설명, 권장 폴더 위치를 구체적으로 보여주고, 동작 4줄(자동 저장 / 지금 동기화 / 충돌 / 삭제 미동기화)을 status row 아래 항상 노출.
  - 검토에서 의도적으로 좁힌 스코프: (a) 노트 *삭제* 동기화는 v2.1.0에서 제외 — 가장 위험한 작업, 잘못되면 데이터 손실 시나리오. (b) 앱 시작 시 자동 reconcile 미구현 — 백그라운드 silent overwrite 회피. 사용자는 "지금 동기화" 명시적 트리거. 두 결정 다 §2.6 안전 기본값에 정렬.
  - File-id 매칭: 파일명에 `id<id8자>` suffix를 박아 다음 save 때 같은 파일을 빠르게 찾음 (frontmatter 전부 파싱하는 O(n²) 회피). slug 부분이 사용자 친화 + suffix가 머신 친화 = `hello-world-idabcdef12.md`.
  - SAF persistent URI permission: `takePersistableUriPermission(uri, READ|WRITE)`. 이게 없으면 앱 재시작 시 URI가 만료됨 — v1.6 export-all은 일회성이라 필요 없었지만 v2.1은 영구 필요.
  - 충돌 슬랙 2초: 일부 클라우드 sync 앱이 파일 타임스탬프를 1초 단위로 반올림하는 경우 false-positive "file newer" 트리거 회피.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — SyncFrontmatterTest 8개 그린, ResourceParityTest 그린, 기존 단위 테스트 모두 그린
  - `./gradlew assembleDebugAndroidTest` 통과
  - `./gradlew verifyRoborazziDebug` 통과 (UI 변경이 settings에 국한되어 기존 14+4 골든 영향 없음)

## 2026-05-08 (밤) - v2.5.1 + tablet bench
- Work: 사용자가 v1.5에서 Material You 기본화 이후 leaf-style 그린이 사라진 것을 지적 → 테마 선택 설정 추가, 그린 기본 복귀. 동시에 Lenovo TB320FC (API 35) 태블릿에서 Macrobenchmark 첫 실측.
- Theme picker:
  - `AppSettings.colorPalette: ColorPalette { MARKLEAF_GREEN, MATERIAL_YOU }` (DataStore key `color_palette`)
  - `MarkleafTheme.dynamicColor` 기본값 `true → false` (정적 그린 기본)
  - `MainActivity` 가 settings flow를 collect 해 ColorPalette → dynamicColor 변환, 변경 즉시 리컴포즈
  - Settings → 새 "테마" 섹션: 두 옵션 OutlinedButton/Button 토글 패턴
  - strings × 3 lang
- Macrobenchmark on TB320FC (API 35):
  - profileinstaller 1.3.1 → 1.4.0 (API 35 지원), benchmark-macro-junit4 1.2.4 → 1.3.0
  - benchmark build type: `isDebuggable = true → false` (Macrobenchmark는 debuggable 거부)
  - `app/src/benchmark/AndroidManifest.xml` (NEW) — `<profileable shell="true" />` 만 추가, 다른 build variant에 영향 없음
  - benchmark/AndroidManifest.xml — WRITE_EXTERNAL_STORAGE 제거 (API 30+에서 GrantPermissionRule 실패 원인)
  - 결과:
    - **Cold start: median 326ms** (min 317 / max 360) — §2.1 *빠름 우선* 기준 즉각
    - **Warm start: median 113ms** — 거의 무즉각
    - **Hot start: median 57ms** — 사용자 인지 한계 이하
    - ScrollBenchmark는 fresh install이라 노트 4개뿐, fling 시 frame 데이터 부족으로 실패 — 데이터 시드된 빌드에서 재실행 필요 (v2.5.x 백로그)
- Verification:
  - `./gradlew assembleDebug + test + verifyRoborazziDebug` 통과
  - 태블릿에 v2.5.1 debug APK 설치 완료 (사용자 스모크 테스트용)

## 2026-05-08 (밤) - v2.0.0
- Work: Bear-급 인라인 rich rendering. 사용자가 "Bear의 핵심 체감 차이는 라이브 프리뷰" 라고 명시한 갈증을 직접 응답. 라이브러리 교체는 §2.9 정신에 맞춰 보류.
- Changed files:
  - core/markdown/MarkdownSyntaxHighlighter.kt — 헤딩 분기 재구성: marker 길이별로 contentStart 계산해 (H1: 24sp Bold, H2: 20sp SemiBold, H3: 18sp SemiBold) 콘텐츠 범위에만 fontSize+fontWeight 적용. 마커(`#`)는 muted color + Normal weight 로 retreat. BOLD_REGEX 매치 콘텐츠 weight를 SemiBold → Bold로 강화. 새 헬퍼 `muteMarkerStyle(colors)` 가 모든 inline marker(`**`, `_`, `~~`, 백틱, `[`, `](`, `)`)를 color=syntax + weight=Normal + style=Normal + decoration=None 으로 일괄 reset → 마커가 콘텐츠보다 시각적으로 retreat하는 Bear 패턴 구현.
  - test/core/markdown/MarkdownSyntaxHighlighterTest.kt — 5개 단위 테스트 추가: H1/H2/H3 fontSize, bold가 FontWeight.Bold (not SemiBold), 마커가 Normal weight로 reset되는지.
  - test/core/markdown/preview/EditorLiveSnapshotTest.kt (NEW) — 라이브 *에디터* 모드 시각 골든 4개. BasicTextField + MarkdownSyntaxVisualTransformation을 실제 사용 환경 그대로 캡처. 헤딩(라이트/다크) + 인라인 emphasis + 혼합 문서.
  - test/snapshots/roborazzi/editor_live_*.png (NEW, 4 files) — 위 테스트의 골든.
  - app/build.gradle.kts (versionCode 63, versionName 2.0.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - Bear의 NSTextStorage + NSAttributedString 패턴을 Compose의 BasicTextField + AnnotatedString 등가로 구현. 글자 인덱스는 그대로 보존(VisualTransformation의 OffsetMapping.Identity), 시각만 변형.
  - Phase B (commonmark-java 라이브러리 교체) 보류 결정 근거: 사용자 가치 0 (사용자 화면에 변화 없음), 추측 기반 리팩터, 실제 필요(위키링크/하이라이트 등 확장 도입)가 생길 때 진행이 §2.9에 맞음. 코드 변경량을 최소화하면서 Phase A (사용자 가치 100%)만 출시.
  - 사용자가 보여준 mixed_document_light 골든의 라이브 변형 결과 — H1 "Title"이 visibly 24sp Bold green, `**bold word**` 가 진짜 Bold tertiary, `*emphasis*` 가 italic, 마커들 muted gray retreat. *체감상 가장 큰 변화* 라고 약속한 것 그대로.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — MarkdownSyntaxHighlighterTest 13개 (기존 8 + 신규 5) 그린, EditorLiveSnapshotTest 4개 그린, 전체 단위 테스트 그린
  - `./gradlew assembleDebugAndroidTest` 통과
  - `./gradlew recordRoborazziDebug` 4개 새 PNG 생성, `./gradlew verifyRoborazziDebug` 모두 통과 (changeThreshold 0.05f)
  - 기존 14개 preview-pane 골든은 변경 없음 (VisualTransformation은 에디터의 BasicTextField에만 영향)

## 2026-05-08 (밤) - v1.9.0
- Work: Phase 18 — 시각 회귀 그물망. Bear-급 라이브 프리뷰로 가기 전 안전 인프라 사이클. 사용자 화면 변화 0.
- Changed files:
  - build.gradle.kts (root) — `id("io.github.takahirom.roborazzi") version "1.20.0" apply false`
  - app/build.gradle.kts — Roborazzi 플러그인 적용 + testImpl(`roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule` 1.20.0). compose ui-test-junit4/manifest를 test에도 추가 (기존엔 androidTest only). androidx.test.ext:junit testImpl 추가.
  - core/markdown/preview/MarkdownPreviewList.kt (NEW, ~250 LOC) — `EditorScreen`의 미리보기 LazyColumn 분기 + InlineMarkdownText / CalloutBox / FrontmatterBlock / FootnoteDefRow / MarkdownCodeBlock 모두 이쪽으로 이동. 공개 API: `MarkdownPreviewList(lines, modifier, contentPadding)` 와 단일 `PreviewLineRenderer(line)` 둘.
  - feature/editor/EditorScreen.kt — 미리보기 LazyColumn 통째 삭제, `MarkdownPreviewList(...)` 호출로 대체. 이로 인해 사용처 사라진 import 다수 (background, RoundedCornerShape, ClickableText, HorizontalDivider, SpanStyle, buildAnnotatedString, withStyle, FontFamily, FontStyle, BaselineShift, TextDecoration, sp, LazyColumn, items, clip) 정리.
  - test/core/markdown/preview/MarkdownPreviewSnapshotTest.kt (NEW) — 14개 시각 골든. 각 PreviewLineType + 콜아웃 3종 + 프론트매터 + 각주 + 혼합 문서 (라이트/다크 변형 포함).
  - test/snapshots/roborazzi/*.png (NEW, 14 files) — 골든 이미지. mdpi 360x640 에서 Windows native graphics로 첫 record.
  - .github/workflows/android-build.yml — `Verify Roborazzi snapshots` 스텝 추가 (`./gradlew verifyRoborazziDebug`). 실패 시 diff artifact 업로드.
  - app/build.gradle.kts (versionCode 62, versionName 1.9.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - 가치관 점검 후 사용자가 명시적으로 동의한 v1.9 → v2.0 → v2.1 순서의 첫 단계. v2.0(인라인 rich rendering, CommonMark 도입)과 v2.1(SAF 폴더 미러 sync) 이전에 시각 회귀 차단망 먼저 깔기.
  - Roborazzi 1.20은 Robolectric 4.10+ + Kotlin 1.9 + AGP 8.x + JDK 17과 맞물려 동작. 우리는 모두 충족.
  - `@Config(qualifiers = "w360dp-h640dp-mdpi")` — 처음 시도한 BCP47 locale 포함 qualifier(`en-rUS`)가 Robolectric Qualifiers.parse()에 거부당함. locale 빼고 화면 사이즈+밀도만 남기니 통과.
  - `changeThreshold = 0.05f`: Windows native 폰트 렌더링과 Ubuntu CI Linux native 폰트 렌더링 사이의 hint/안티앨리어싱 픽셀 미세 차이 흡수. 구조적 회귀(요소 누락/색상 변경/레이아웃 어긋남)는 5%를 가볍게 넘기므로 잡힘. 추후 record를 CI에서 다시 돌려 동일 OS 골든을 만들면 0.005f 정도로 타이트하게 조일 예정.
  - MarkdownPreviewList 추출은 자연스러운 부산물 — 같은 코드를 에디터와 테스트가 공유하므로 갈라질 위험이 0이 됨. v2.0에서 인라인 rich rendering 도입 시 이 단일 진입점만 수정하면 양쪽 다 갱신됨.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — 기존 단위 테스트 모두 그린, Roborazzi 14개 새 테스트도 그린 (record 직후 verify)
  - `./gradlew assembleDebugAndroidTest` 통과
  - `./gradlew recordRoborazziDebug` 14개 PNG 생성, `./gradlew verifyRoborazziDebug` 모두 통과 (changeThreshold 0.05f 안)
  - 사용자에게 보여준 mixed_document_light 골든 시각 확인 — frontmatter 모노스페이스 박스, primary 색상 H1, callout(Tip) 연두 배경+icon, 위첨자 각주 ref, 하단 각주 def 모두 의도대로 그려짐.

## 2026-05-08 (밤) - v1.8.0
- Work: v1.5–1.7 사이클이 누적시킨 상단바·설정 chrome density를 줄이는 정리 사이클. 새 기능 0, 모든 액션 유지하되 시각 밀도만 하향.
- Changed files:
  - feature/notes/NotesListScreen.kt — TopAppBar 액션 정리. archive/trash/settings IconButton 3개를 `Icons.Default.MoreVert` IconButton + DropdownMenu(보관함/휴지통/설정) 한 개로 합침. search/tags는 primary 유지. `overflowExpanded: Boolean` state 추가.
  - feature/editor/EditorScreen.kt — TopAppBar 액션 정리. focus, trash IconButton을 새 ⋮ overflow DropdownMenu(집중 모드 / 휴지통으로 이동)로 이동. search(find-in-note), preview/edit 토글, share-menu 셋은 primary 유지. focus 모드 항목은 미리보기 모드일 때만 표시(쓰기 화면에서만 의미 있음). `overflowExpanded: Boolean` state 추가.
  - feature/settings/SettingsScreen.kt — `settings_security` 섹션 제거. `screenshot_protection` 스위치를 `settings_privacy` 섹션 안으로 이동(privacy_local_first 다음, privacy dashboard 버튼 위).
  - res/values{,-ko,-es}/strings.xml — `more_options` (3 lang) 추가, `settings_security` (3 lang) 삭제. ResourceParityTest 통과.
  - app/build.gradle.kts (versionCode 61, versionName 1.8.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - v1.7.0 직후 §2 가치관 점검에서 발견된 "gradual chrome accretion" 신호에 대한 직접적인 응답 사이클. 5단계 흐름(열기→쓰기→정리→찾기→내보내기)에 새 기능을 더하지 않고, 기존 항목을 사용 빈도에 따라 primary / overflow로 재정렬한 것.
  - Material 3 TopAppBar는 trailing actions의 명시적 상한을 두지 않지만 일반적으로 ≤3개 권장. 우리는 이번에 그 권장에 정렬됨.
  - DropdownMenu는 v1.6 share-menu, v1.7 archive-row long-press 등에서 이미 사용 중인 패턴이라 코드 추가가 일관된 idiom.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — UI를 만지지 않은 단위 테스트들 모두 그린 (`ResourceParityTest`가 새 키 / 삭제된 키를 3개 언어 동기 검증)
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08 (밤) - v1.7.0
- Work: Phase 16 (Spec Closure) — `Note.archived` 필드(v1.0부터 dead) 살리는 보관함 UI 도입 + AGENT_SPEC §7 접근성 라인 검증.
- Changed files:
  - data/local/dao/NoteDao.kt — `setArchived(noteId, archived)`, `observeArchivedNotes()` 추가. `observeNotes()`/`searchNotesFts`/`searchNotesLike`/`getNoteByTitle`/`getMaxSortOrder` 모두 `AND archived = 0` 추가해 보관 노트가 메인/검색에 노출되지 않도록 함.
  - domain/repository/NoteRepository.kt — `setArchived` + `observeArchivedNotes` 인터페이스 시그니처 추가
  - data/repository/LocalNoteRepository.kt — 위 두 메서드 구현
  - ui/viewmodel/NotesViewModel.kt — `setArchived(noteId, archived)` 추가
  - ui/viewmodel/ArchiveViewModel.kt (NEW) — TrashViewModel과 동일한 패턴: 아카이브 flow 관찰 + `unarchive(noteId)` + `moveToTrash(noteId)`
  - ui/viewmodel/MarkleafViewModelFactory.kt — ArchiveViewModel 케이스 추가
  - feature/archive/ArchiveScreen.kt (NEW) — TrashScreen과 비슷한 구조이되 Restore/Delete 버튼 대신 long-press DropdownMenu(보관 해제 / 휴지통 이동). 클릭 시 에디터 진입.
  - navigation/NavRoutes.kt — `ARCHIVE = "archive"`
  - navigation/MarkleafNavHost.kt — `composable(NavRoutes.ARCHIVE) { … ArchiveScreen(…) }` + 두 NotesListScreen 호출처에 `onArchiveClick = { navController.navigate(NavRoutes.ARCHIVE) }` 전달
  - feature/notes/NotesListScreen.kt — TopAppBar에 `Icons.Default.Inventory2` 보관함 아이콘 (Tags 다음, Trash 앞), long-press 드롭다운에 "보관" 항목 (Pin과 Trash 사이). 새 `onArchiveClick`/`onArchive` 콜백.
  - res/values{,-ko,-es}/strings.xml — `archive`, `unarchive`, `archive_empty`, `archive_empty_hint` 4개 키 3언어 동시 추가 (ResourceParityTest 통과)
  - app/build.gradle.kts (versionCode 60, versionName 1.7.0)
  - test/data/repository/LocalNoteRepositoryTest.kt — 3개 테스트 추가 (setArchived hides+exposes, setArchived false restores, searchNotes excludes archived)
  - test/ui/viewmodel/MarkleafViewModelFactoryTest.kt — `createsArchiveViewModel` + FakeNoteRepository에 setArchived/observeArchivedNotes override
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - DB 스키마 변경 없음. `archived` 컬럼은 v1.0부터 NoteEntity에 존재했고 모든 기존 행은 default 0이라 마이그레이션 없이 안전.
  - 인덱스: 기존 `(trashed, pinned, sortOrder)` 인덱스의 leading prefix(`trashed`)는 새 쿼리(`WHERE trashed = 0 AND archived = 0 …`)에서도 활용 가능. 메인 노트가 수만 건이 아닌 한 추가 인덱스 불필요.
  - 접근성 audit는 코드 변경보다 검증 위주: `IconButton(`이 들어간 5개 파일 모두 stringResource 라벨 부여, `contentDescription = null` 케이스는 모두 텍스트 라벨 동반 데코레이티브 아이콘. 색상 대비는 정적 팔레트(LightColorScheme/DarkColorScheme) 기준 surfaceVariant↔onSurfaceVariant ~11:1 / ~6:1, primary↔primaryContainer ~5.5:1 / ~5:1로 WCAG AA 4.5:1 통과.
  - 다국어 확대(JP/FR/DE)는 검증할 native 화자 부재로 v1.7.0에선 보류. ResourceParityTest 인프라는 그대로 작동하므로 향후 추가 시 안전.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — 신규 4개 포함 모든 단위 테스트 그린 (`LocalNoteRepositoryTest` 7건, `MarkleafViewModelFactoryTest` 4건 등)
  - `./gradlew assembleDebugAndroidTest` 통과
- AGENT_SPEC §7 *반드시 포함* 16/16, *있으면 좋은* 7/7 모두 닫힘. v1.7.0이 사실상 MVP 종료.

## 2026-05-08 (밤) - v1.6.0
- Work: Phase 15 (Markdown Expressiveness) 4종 + 별도 검토에서 발견된 §7 *반드시 포함* 누락 항목(단일/전체 .md 내보내기 UI) 동시 해결.
- Changed files:
  - core/markdown/SimpleMarkdownPreview.kt — `CalloutKind` enum 추가 (NOTE/TIP/IMPORTANT/WARNING/CAUTION + `WARN`/`DANGER` 별칭 매핑), `PreviewLineType.CALLOUT/FRONTMATTER/FOOTNOTE_DEF` 추가, `PreviewInlineType.FOOTNOTE_REF` 추가. `parse()` 진입부에서 leading `---` … `---` 만 frontmatter로 처리하고 본문 중간의 `---`는 기존 HORIZONTAL_RULE 처리 유지. 콜아웃은 `> [!TYPE]` 헤드라인 다음 연속된 `>` 라인을 모아 단일 PreviewLine로 합침. 각주 정의는 라인 단위 정규식, 각주 참조는 inline 정규식(definition와 충돌하지 않도록 `(?!:)` 부정 lookahead 사용).
  - core/markdown/MarkdownEditActions.kt — `indent()` / `outdent()` 추가. 둘 다 `selectionLineRange()` 헬퍼로 선택 영역에 닿는 모든 줄을 잡아 일괄 처리. outdent는 2-space, tab, 1-space 순으로 단계적 제거.
  - feature/editor/EditorScreen.kt — BasicTextField에 `Modifier.onPreviewKeyEvent { Tab/Shift+Tab → indent/outdent }` 부착. 기존 Share IconButton을 DropdownMenu로 묶어 "공유 시트로 보내기" + ".md 파일로 저장" 두 항목으로 확장. 후자는 `ActivityResultContracts.CreateDocument("text/markdown")` 런처 + `ExportUtil.generateMarkdownContent/generateFileName` 호출. 미리보기 LazyColumn에 `PreviewLineType.CALLOUT/FRONTMATTER/FOOTNOTE_DEF` 케이스 추가. `InlineMarkdownText`에 `FOOTNOTE_REF` (BaselineShift.Superscript + 11sp + primary color) 케이스 추가.
  - feature/settings/SettingsScreen.kt — 새 "데이터" 섹션 + "전체 노트 내보내기…" 버튼. `ActivityResultContracts.OpenDocumentTree()` 런처가 `LocalNoteRepository.observeNotes().first()` 로 현재 노트 스냅샷을 잡아 `ExportAllNotes.exportAllNotes(context, folderUri, notes)` 호출. 휴지통 노트는 제외. 결과 Toast로 "노트 N개를 내보냈습니다" 표시.
  - res/values{,-ko,-es}/strings.xml — share_via_system, export_as_file, export_success, export_failed, export_all_notes, export_all_notes_description, export_all_done_format, settings_data, callout_note/tip/important/warning/caution + `share_note` 라벨을 "공유 / 내보내기"로 수정 (3개 언어 동시).
  - app/build.gradle.kts (versionCode 59, versionName 1.6.0)
  - test/core/markdown/SimpleMarkdownPreviewTest.kt — 6개 테스트 추가 (frontmatter, mid-document `---` 보존, callout block, callout alias, footnote def, footnote ref).
  - test/core/markdown/MarkdownEditActionsTest.kt — 5개 테스트 추가 (indent single, indent multi-line, outdent 2-space, outdent tab, outdent no-op).
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - SimpleMarkdownPreview.kt 라인 수: v1.5.0 종료 시 178 LOC → v1.6.0 ~210 LOC. lightweight-bias 메모의 ~300 LOC 한계 안에 안전하게 들어옴.
  - 전체 노트 내보내기에서 SettingsScreen이 NotesViewModel 없이 LocalNoteRepository를 직접 인스턴스화하는 건 TagsScreen/SearchScreen과 동일한 기존 패턴(스펙 §9의 엄격 해석에는 어긋나지만 v1.0부터의 단순화 컨벤션).
  - 단일 노트 export 메뉴 위치: TopAppBar 액션이 v1.5에서 5개로 늘어나 더 추가하면 비좁아지므로, 기존 Share IconButton을 DropdownMenu host로 바꿔 "공유" + "내보내기" 두 동작을 하나의 버튼 아래에 묶음. 사용자가 자주 헷갈릴 수 있어 라벨에 ellipsis(…)를 붙여 "선택할 게 더 있다"는 신호.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — 신규 11개 테스트 포함 모든 단위 테스트 그린
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08 (밤) - v1.5.0
- Work: Phase 14 — 안드로이드 정상 시민 마감을 위한 5종 묶음 (Material You / 예측 뒤로가기 / 단일 노트 시스템 공유 / 외부 공유 텍스트 수신 / FLAG_SECURE 토글).
- Changed files:
  - ui/theme/Theme.kt — `MarkleafTheme.dynamicColor` 기본값 `false` → `true` (S+에서만 dynamicLight/DarkColorScheme 사용, 그 외에는 기존 정적 색상 폴백)
  - AndroidManifest.xml — `<application android:enableOnBackInvokedCallback="true">`, MainActivity에 `<intent-filter ACTION_SEND text/plain>`, 그리고 `androidx.core.content.FileProvider` provider 등록 (`${applicationId}.fileprovider` authority)
  - res/xml/file_paths.xml (NEW) — `cache-path name="shared_notes"` (`ShareNoteUtil`이 이미 사용 중인 cache 디렉터리)
  - data/settings/AppSettings.kt — `screenshotProtection: Boolean = false` 필드 추가
  - data/settings/AppSettingsRepository.kt — `SCREENSHOT_PROTECTION` boolean 키 + `setScreenshotProtection(enabled)`
  - MainActivity.kt — `extractSharedText()` 헬퍼 (EXTRA_SUBJECT를 `# 제목` H1으로, EXTRA_TEXT를 본문으로 결합), `repeatOnLifecycle(STARTED)` 안에서 settings flow 관찰 → `window.addFlags(FLAG_SECURE)` / `clearFlags`. `onNewIntent`에서 SEND가 새로 들어오면 `setIntent(intent) + recreate()`.
  - navigation/MarkleafNavHost.kt — `sharedText: String? = null` param, NOTES composable LaunchedEffect로 `viewModel.createNote(sharedText)` 호출 후 에디터로 navigate.
  - ui/viewmodel/NotesViewModel.kt — `createNote(initialContent: String = "")` 시그니처. 본문이 있으면 `TitleExtractor.extractTitle/generateExcerpt`로 제목/요약을 채움.
  - feature/editor/EditorScreen.kt — TopAppBar actions 영역에 `Icons.Default.Share` IconButton 추가, 클릭 시 현재 편집 중 텍스트로 Note를 만들어 `ShareNoteUtil.shareNote`. (저장 보다 빠르게 공유 누른 경우에도 입력 중 본문이 그대로 시트에 들어가도록 in-memory copy)
  - feature/settings/SettingsScreen.kt — 새 `settings_security` 섹션의 `screenshot_protection` 토글
  - res/values{,-ko,-es}/strings.xml — `share_note`, `settings_security`, `screenshot_protection`, `screenshot_protection_description` (3개 언어 동시 추가, ResourceParityTest 통과 보장)
  - app/build.gradle.kts (versionCode 58, versionName 1.5.0)
  - CHANGELOG.md, .agent/tasks.md
- Context:
  - 다섯 항목 모두 ≤30 LOC급 작은 변경이라 한 사이클로 묶어도 디프 검토가 어렵지 않음. 각 항목은 독립적으로 켜고 끌 수 있음.
  - `ShareNoteUtil`은 v0.x부터 존재했지만 UI에 wired된 적이 없어 사실상 dead code였음. AndroidManifest에 `FileProvider`도 빠져 있어 wire-up 시 FileProvider도 함께 등록해야 했음 — `xml/file_paths.xml`은 `cacheDir/shared_notes` 한 줄로 끝.
  - SEND 수신 처리는 `onCreate` + `onNewIntent` 둘 다에서 처리. 이미 실행 중인 인스턴스로 SEND가 오면 `setIntent + recreate()`로 새 노트가 시드됨.
  - FLAG_SECURE는 `repeatOnLifecycle(STARTED)`에서 settings flow를 관찰. `distinctUntilChanged`로 동일값 emit 무시. STARTED 위에서만 active → 백그라운드에서 토글되어도 안전.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — `ResourceParityTest`가 새 3개 키를 ko/es에서도 검증, 기존 단위 테스트는 영향 없음

## 2026-05-08 (밤) - v1.4.2
- Work: v1.4.1에서 부분만 고쳐졌던 태블릿 접힌 레일의 상태바 겹침을 마저 정리.
- Changed files:
  - navigation/MarkleafNavHost.kt — `CollapsedNoteListRail`에서 `systemBarsPadding`을 안쪽 Box → 바깥 Surface로 옮기고, Box는 `fillMaxSize`만 유지
  - app/build.gradle.kts (versionCode 57, versionName 1.4.2)
  - CHANGELOG.md
- Context:
  - 사용자 보고 (Lenovo Y700 2nd gen): 메인 콘텐츠는 상태바 밑으로 내려왔지만 56dp 회색 레일의 배경이 여전히 상태바 시계 영역까지 올라가 겹침.
  - 원인: v1.4.1에서 padding을 inner Box에 줘 아이콘 위치만 인셋만큼 밀려났을 뿐, 바깥 Surface의 background 페인트 영역은 그대로 top-to-bottom이었음. Surface가 그리는 회색 fill이 상태바 뒤까지 보여서 시각적 충돌 발생.
  - 해결: `systemBarsPadding`은 padding을 받은 노드의 *그리는 영역* 자체를 줄이므로 Surface에 적용하면 surfaceVariant fill이 상태바 아래에서만 시작됨. 위쪽은 부모 Row의 `editorPaneColor`(= `MaterialTheme.colorScheme.background`)가 보여 에디터 페인 TopAppBar 영역과 동일한 톤으로 자연스럽게 이어짐.
- Verification:
  - `./gradlew assembleDebug` 통과
  - 기존 테스트는 영향 없음 (테스트 재실행 생략)

## 2026-05-08 (밤) - v1.4.1
- Work: Lenovo Y700 2nd gen 같은 노치 없는 태블릿에서 노트 본문이 알림바 밑으로 들어가던 문제 해결.
- Changed files:
  - MainActivity.kt — `super.onCreate()` 직전에 `enableEdgeToEdge()` 호출 (androidx.activity 1.8.2 helper)
  - feature/tags/TagsScreen.kt — Surface root에 `systemBarsPadding()`
  - feature/search/SearchScreen.kt — Surface root에 `systemBarsPadding()`
  - feature/trash/TrashScreen.kt — Surface root에 `systemBarsPadding()`
  - navigation/MarkleafNavHost.kt — `CollapsedNoteListRail` Box에 `systemBarsPadding()`
  - app/build.gradle.kts (versionCode 56, versionName 1.4.1)
  - CHANGELOG.md
- Context:
  - 사용자 보고: Galaxy S24 (노치 있음, Android 15 추정)는 시스템 차원 edge-to-edge 강제로 인해 M3 Scaffold + TopAppBar의 자동 인셋 처리가 동작 → 정상.
  - Lenovo Y700 2nd gen (노치 없음, Android 14 이하 추정)에서는 시스템이 강제하지 않아 우리 앱이 명시적으로 enableEdgeToEdge를 켜지 않으면 status bar 처리가 모호한 상태(반투명 + no padding)가 되어 콘텐츠가 알림 영역 밑으로 그려짐.
  - 해결의 통합 포인트는 `enableEdgeToEdge()` 한 줄 — 모든 안드로이드 버전에서 동일하게 transparent 시스템 바 + WindowInsets 제공. M3 Scaffold/TopAppBar는 그 인셋을 알아서 padding으로 변환.
  - Scaffold 없는 화면(Tags/Search/Trash)에는 root Surface에 systemBarsPadding을 직접 추가 — 같은 통일성을 단순한 modifier 한 줄로 확보.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release unit) 통과 — 기존 테스트 모두 영향 없음

## 2026-05-08 (밤) - v1.4.0
- Work: Bear급 사용 경험으로 가는 다음 단계 — 정리와 글쓰기 습관에 직결되는 세 가지 기능 묶음.
- Changed files:
  - util/TagParser.kt — `isValidTagName`을 segment-단위 검증으로 바꿔 `/` 중첩 허용 (각 세그먼트는 `[a-zA-Z가-힣_][a-zA-Z가-힣0-9_-]*`)
  - feature/tags/TagsScreen.kt — `buildHierarchicalRows` 도입, 깊이별 들여쓰기와 색상 구분 (root: primary, child: secondary)
  - feature/editor/EditorScreen.kt — TopAppBar에 집중/검색 아이콘 추가, FindBar 컴포저블, focus 토글 시 툴바·통계·하이라이팅 일괄 숨김, `findAllRanges` 헬퍼 + LaunchedEffect로 selection 이동
  - res/values{,-ko,-es}/strings.xml — focus_mode / exit_focus_mode / find_in_note / find_in_note_hint / find_previous_match / find_next_match / close
  - test/util/TagParserTest.kt — 5개 시나리오 추가 (slash 중첩, 깊은 중첩, trailing slash 거부, 빈 중간 세그먼트 거부, 한글 중첩)
  - test/feature/editor/FindRangesTest.kt — 7개 시나리오 (빈 입력, 단일/다중 매치, 대소문자 무시, 미존재, 비겹침)
  - app/build.gradle.kts (versionCode 55, versionName 1.4.0)
  - CHANGELOG.md / HISTORY.md / .agent/tasks.md / .agent/progress.md
- Context:
  - 사용자: *"Bear급 사용 경험으로 가고 싶다"*. 직전 v1.3.0의 toolbar+smart-Enter 이후, 다음 한 사이클로 임팩트가 가장 큰 묶음으로 (1) 태그 계층화 — 정리 구조 도약, (2) 집중 모드 — 글쓰기 습관, (3) 노트 안 찾기 — 긴 노트 필수기능.
  - 셋 다 서로 독립적이고 각기 화면 한두 곳만 손대므로 한 사이클에 깔끔히 끝남.
  - DB 스키마 변경 없음. 태그 검증 정규식만 변경되어 기존 태그도 모두 통과.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release) 통과 — TagParser 5건 + FindRanges 7건 신규 통과
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08 (저녁) - v1.3.1
- Work: 사용자 보고로 시작 노트가 여전히 위키 링크와 ZIP 백업을 안내한다는 점을 발견 — 온보딩 콘텐츠와 구현을 동기화.
- Changed files:
  - app/src/main/res/raw/starter_notes.md (4 notes 재작성)
  - app/src/main/res/raw-ko/starter_notes.md
  - app/src/main/res/raw-es/starter_notes.md
  - data/onboarding/StarterNotesSeeder.kt (인라인 EN 폴백 동기화)
  - test/data/onboarding/StarterNotesSeederTest.kt (어서션 갱신 — `[[` / "ZIP backup" 부재 + "Smart Enter" 등장 확인)
  - app/build.gradle.kts (versionCode 54, versionName 1.3.1)
  - CHANGELOG.md
- Context:
  - 사용자 시나리오: v1.3.0 설치 후 새로 받은 샘플 노트가 `[[Local-first backup and export]]`를 따라가라고 안내하지만 실제 앱에는 위키 링크 기능이 없어 클릭해도 아무 일도 일어나지 않음.
  - 또한 *"설정에서 ZIP 백업 만들기"* 도 안내하나 v1.2.0에서 제거됨.
  - 새 시작 노트 4개: Welcome / Markdown(toolbar + Smart Enter 안내) / Tags(핀, 그룹화) / Privacy(.md 내보내기 + 휴지통).
- Verification:
  - `./gradlew test` 통과 — StarterNotesSeederTest 새 어서션 포함

## 2026-05-08 (오후)
- Work: v1.2.0 가벼움 회귀로 비워둔 토대 위에 Bear급 글쓰기 경험을 한 단계 끌어올림 (v1.3.0).
- Changed files:
  - core/markdown/MarkdownEditActions.kt — heading 순환, bullet/ordered/blockquote 토글, horizontalRule, codeBlock, applyAutoContinuation 추가
  - feature/editor/EditorScreen.kt — 툴바 4개 → 12개 (그룹 구분선 포함), 자동 이어쓰기 onValueChange 통합, 통계 footer (단어/글자/분)
  - feature/notes/NotesListScreen.kt — long-press → DropdownMenu(고정 토글 + 휴지통), 고정/오늘/어제/지난 7일/이전 섹션 그룹화, 핀 아이콘 표시
  - data/local/dao/NoteDao.kt + repository/LocalNoteRepository.kt + domain/repository/NoteRepository.kt — setPinned 와이어
  - ui/viewmodel/NotesViewModel.kt — setPinned launch helper
  - res/values{,-ko,-es}/strings.xml — heading/bullet/ordered/blockquote/code_block/horizontal_rule/strikethrough/inline_code/editor_stats_format/pin/unpin/section_* 추가
  - test/core/markdown/MarkdownEditActionsTest.kt — heading 순환, list/quote 토글, codeBlock, hr, autoContinuation(bullet/ordered/checklist/blockquote/empty-prefix-end/plain) 시나리오 추가
  - test/ui/viewmodel/MarkleafViewModelFactoryTest.kt — Fake setPinned override
  - app/build.gradle.kts (versionCode 53, versionName 1.3.0)
  - CHANGELOG.md / HISTORY.md / .agent/tasks.md / .agent/progress.md
- Context:
  - 사용자: *"퀵 버튼이 너무 비어 보인다"* + *"Bear급 사용 경험으로 가고 싶다"*. v1.2.0의 토대 위에서 가장 임팩트 큰 작업 묶음으로 선정.
  - 마크다운 파서는 손파서를 유지 (300줄 미만). 새 기능은 모두 파서를 건드리지 않고 에디터 액션 + UI 레이어에서 끝남.
  - DB 스키마 변경 없음. `pinned` 필드는 v1.0부터 있었지만 UI 토글이 없어 사실상 데드 필드였음.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release unit) 통과 — 새 12개 액션/자동 이어쓰기 케이스 포함
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08
- Work: 가벼운 마크다운 앱 가치관에 맞춰 무거운 부가기능을 일괄 정리하고 누락된 노트 삭제 진입점을 복구.
- Changed files:
  - feature/notes/NotesListScreen.kt (NoteCountDashboard 제거, 드래그 재정렬 제거, long-press 휴지통 진입)
  - feature/editor/EditorScreen.kt (휴지통 버튼 추가, 버전 히스토리/백링크/이미지 첨부/위키 링크/체크리스트 진행률/표·수식 미리보기 제거)
  - feature/settings/SettingsScreen.kt (ZIP 백업, 툴바 토글, 권한 섹션 제거)
  - feature/search/SearchScreen.kt (Quick Open Links 섹션 제거)
  - core/markdown/SimpleMarkdownPreview.kt (TABLE/MATH/IMAGE/LINK/INLINE_MATH 제거)
  - core/markdown/MarkdownEditActions.kt (wikiLink/image 액션 제거)
  - core/markdown/MarkdownSyntaxHighlighter.kt (WIKI_LINK/TABLE 정규식 제거)
  - data/local/AppDatabase.kt (v9 migration: note_snapshots / note_links / attachments DROP)
  - data/local/dao 및 entity (Snapshot/Link/Attachment 관련 파일 삭제)
  - data/repository/LocalNoteRepository.kt + domain/repository/NoteRepository.kt (스냅샷/백링크 메서드 제거)
  - data/settings/AppSettings(+Repository).kt (ToolbarConfig 제거)
  - util/BackupUtil.kt, util/PermissionUtils.kt, feature/notes/ChecklistProgressIndicator.kt, core/markdown/ChecklistParser.kt, domain/model/NoteSnapshot.kt 삭제
  - AndroidManifest.xml (POST_NOTIFICATIONS, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_EXTERNAL_STORAGE 제거)
  - app/build.gradle.kts (Coil 의존성 제거)
  - res/values{,-ko,-es}/strings.xml (사용하지 않는 키 제거 + move_to_trash 계열 추가)
  - test 코드 정리 (제거된 기능 관련 테스트 삭제 또는 갈음)
  - CHANGELOG.md / HISTORY.md
- Context:
  - 사용자 보고: 휴지통 화면은 있는데 그곳으로 노트를 보낼 진입점이 UI 어디에도 없음.
  - 원인: NotesListScreen에서 long-press를 `detectDragGesturesAfterLongPress`가 가로채서 `onMoveToTrash` 콜백이 죽은 코드가 됐고, 에디터에는 삭제 버튼 자체가 없었음.
  - 함께 처리: 사용자 요청 *"가벼움/편의성에 부합하지 않는 기능은 과감히 제거"* 에 따라, AGENT_SPEC §7의 MVP 제외 항목(테이블, 수식, WYSIWYG, 복잡한 데이터)과 §2.3 "노트는 특정 앱에 갇히지 않아야" 원칙에 어긋나는 ZIP 백업, §6.3 "권한을 가능한 요청하지 않는다"와 충돌하는 미디어/알림 권한, 그리고 자동 저장+휴지통이 이미 안전망이라 중복인 버전 히스토리, "second-brain" 성격이 강한 위키 링크/백링크를 함께 제거.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release unit test) 통과
  - `./gradlew assembleDebugAndroidTest` 통과 (인스트루멘테이션 컴파일까지만 확인; 실제 디바이스 실행은 보류)

## 2026-05-07
- Work: Play Console 제출 마감본 버전업 및 릴리즈 파이프라인 정리.
- Changed files:
  - app/build.gradle.kts (versionCode 51, versionName 1.1.21)
  - CHANGELOG.md (v1.1.21 섹션 추가)
  - HISTORY.md (이 기록 추가)
- Context:
  - Play Console 제출 마감 단계에서 릴리즈 버전 단조 증가를 유지.
  - 태그 릴리즈 CI signed AAB artifact 수집 경로 및 스토어 그래픽/정책 입력 준비를 완료.

## 2026-05-07
- Work: Play Store 출시 준비를 위한 target API 정책 대응 및 AAB 검증.
- Changed files:
  - app/build.gradle.kts (`compileSdk/targetSdk` 35, `versionCode` 50, `versionName` 1.1.20)
  - CHANGELOG.md (v1.1.20 섹션 추가)
- Context:
  - 2025-08-31 이후 Google Play 신규/업데이트 제출 기준(API 35+)에 맞춰 배포 차단 가능성을 사전 제거.
  - 로컬 signed release AAB 빌드로 제출 산출물 생성 경로를 재검증.

## 2026-05-06
- Work: v1.1.19 버전업 및 GitHub Actions 성공 모니터링.
- Changed files:
  - app/build.gradle.kts (versionCode 49, versionName 1.1.19)
  - CHANGELOG.md (v1.1.19 섹션 추가)
  - HISTORY.md (이 기록 추가)
- Context:
  - 최신 릴리즈 태그(v1.1.18) 이후 다음 단위 릴리즈 검증을 위해 단조 증가 버전을 적용.
  - 태그 푸시 후 GitHub Actions 실행 상태를 gh run watch로 추적해 성공 여부를 확인.
## 2026-05-06
- Work: v1.1.18 릴리즈 준비 및 GitHub Actions 권한 복구.
- Changed files:
  - .github/workflows/android-build.yml (permissions: contents: write 적용)
  - app/build.gradle.kts (versionCode 48, versionName 1.1.18)
  - CHANGELOG.md (v1.1.18 섹션 추가 및 누락 기능 통합)
  - HISTORY.md (이 기록 추가)
- Context:
  - v1.1.17 릴리즈 시도가 GitHub Actions의 GITHUB_TOKEN 권한 부족(contents: read)으로 인해 실패했음을 확인.
  - 워크플로우 파일에 contents: write 권한을 부여하여 gh release create가 가능하도록 수정.
  - 실패한 v1.1.17을 건너뛰고 v1.1.18로 상향하여 모든 최신 기능과 권한 수정을 포함한 새로운 릴리즈를 발행함.
- Included Major Features (Cumulative):
  - No-Cloud Certification documentation (#74)
  - In-app Privacy Dashboard (#73)
  - Checklist progress visualization (#69)
  - Home screen Quick Note widget (#60)
  - Drag and drop note reordering (#61)
  - Issue backlog refinement & batch registration (103 issues)

## 2026-05-04
- Work: Play Store Top 10 진입을 위한 50가지 개선 전략 수립 및 GitHub 이슈 등록.
- Changed files:
  - HISTORY.md (전략 목록 추가)
  - GitHub Issues (50개 신규 등록)
- Context:
  - 앱의 경쟁력을 강화하고 사용자 경험, 보안, 성장을 극대화하기 위한 종합 로드맵 수립.
  - UI/UX(15), 기능(15), 보안(10), 성장/ASO(10) 총 50개 항목 도출.

### Markleaf Play Store Top 10 개선 전략 (50선)
1. Material You 다이내믹 컬러 지원
2. 예측 뒤로 가기 제스처 지원 (Android 13+)
3. 리치 에디터 애니메이션 고도화
4. 사용자 지정 폰트 지원 (나눔스퀘어, JetBrains Mono 등)
5. 드래그 앤 드롭 노트 재정렬
6. 햅틱 피드백 최적화
7. 엣지 투 엣지(Edge-to-Edge) 디자인 적용
8. 마크다운 미리보기 전환 애니메이션 개선
9. 그리드/리스트 뷰 전환 옵션 제공
10. 폴더/노트북 아이콘 커스터마이징
11. 멀티 윈도우/분할 화면 최적화
12. 편집기 툴바 사용자 지정 기능
13. 상단/하단 빠른 스크롤 기능
14. 집중 모드(Focus Mode) UI 구현
15. 상태바 노트 카운트 대시보드
16. SQLite FTS5 통합 검색 고도화
17. 위키 링크([[노트 링크]]) 지원
18. 로컬 전용 이미지 첨부 기능
19. 문서 템플릿(회의록, 일기 등) 지원
20. 생체 인식(지문/안면) 앱 잠금
21. 고품질 PDF 내보내기 기능
22. 노트 수정 이력(스냅샷) 관리
23. 홈 화면 위젯 (최근 노트, 빠른 작성)
24. 앱 숏컷 (아이콘 롱프레스 바로가기)
25. 온디바이스 OCR (이미지 텍스트 추출)
26. 음성 인식 마크다운 입력
27. 노트 내 그리기/수기 스케치 통합
28. 체크리스트 진행률 시각화
29. 코드 구문 강조 (Syntax Highlighting) 지원
30. LaTeX 수식 지원
31. SQLCipher 기반 DB 전체 암호화
32. 긴급 상황 패닉 트리거 (앱 즉시 잠금)
33. 암호화된 로컬 백업 파일 생성
34. 권한 최소화 및 개인정보 보호 감사
35. 민감 노트 스크린샷 방지 옵션
36. 프라이빗 "시크릿" 노트 모드
37. 이미지 메타데이터(EXIF/GPS) 자동 제거
38. 자동 로컬 백업 스케줄러
39. 인앱 프라이버시 대시보드
40. No-Cloud 보증 인증 문서화
41. 다국어 지원 확대 (JP, FR, DE 등)
42. 전략적 인앱 리뷰 요청 UI
43. 고전환 스토어 스크린샷 리디자인
44. 전문 브랜딩 피처 그래픽 업데이트
45. 오픈소스 강점 및 투명성 강조
46. "이미지로 공유" 카드 생성 기능
47. 인터랙티브 온보딩 가이드
48. 커뮤니티 마크다운 템플릿 갤러리
49. WCAG 기준 접근성 최적화
50. 로컬 성능 모니터링 (비추적 방식)
