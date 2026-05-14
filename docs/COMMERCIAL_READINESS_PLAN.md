# Markleaf Commercial Readiness Plan

이 문서는 Markleaf를 MVP 이후의 "기능 구현 완료 앱"에서 Play 정식 출시와 장기 사용자 데이터 보관에 견딜 수 있는 상용 수준 앱으로 끌어올리기 위한 실행 계획이다.

작성 기준일: 2026-05-14  
현재 기준 버전: v2.14.0  
핵심 판단: 기능 완성도는 높지만, 상용화 전에는 데이터 보호, 릴리즈 게이트, 개인정보 문서, 동기화 장애 가시성을 먼저 닫아야 한다.

## 1. 냉정 평가

현재 Markleaf는 비공개 테스트 앱 또는 고품질 오픈소스 앱으로는 충분히 설득력이 있다. Markdown 편집, CommonMark/GFM 미리보기, 위키링크, 백링크, 이미지 첨부, SAF 폴더 미러 동기화, FTS 검색, 태블릿 레이아웃, Roborazzi, Macrobenchmark, 서명 릴리즈 자동화까지 이미 들어가 있다.

다만 상용화 관점에서는 "기능 수"보다 다음 네 가지가 더 중요하다.

- 사용자의 노트 데이터가 업데이트, 백업, 동기화, 삭제 과정에서 손실되지 않는다는 확신
- 개인정보/보안 문구가 실제 구현과 정확히 일치한다는 신뢰
- 릴리즈 빌드가 debug 수준이 아니라 production gate를 통과한다는 보증
- 오류, 충돌, 동기화 상태가 Toast로 사라지지 않고 사용자가 나중에 확인할 수 있다는 점

상용화 준비도 점수:

- 제품 기능 완성도: 8/10
- 로컬 우선 차별성: 9/10
- 상용 배포 안정성: 6.5/10
- 개인정보/보안 신뢰성: 6/10
- 장기 유지보수성: 7/10
- 스토어 출시 준비도: 6.5/10

## 2. P0 - 정식 출시 전 반드시 닫을 항목

### P0-1. Android Backup / Data Extraction 정책 확정

문제:

- 현재 `app/src/main/AndroidManifest.xml`의 `<application>`에 `android:allowBackup="true"`가 설정되어 있다.
- Markleaf는 로컬 우선/프라이버시 앱이므로, 시스템 백업에 DB와 첨부 파일이 포함되는지 여부를 의도적으로 설계해야 한다.
- "사용자가 직접 export/share 하기 전까지 데이터가 기기 밖으로 나가지 않는다"는 메시지와 자동 OS 백업 가능성이 충돌할 수 있다.

권장 설계:

- 보수적 기본값: `android:allowBackup="false"`로 전환.
- 또는 Android 12+ `dataExtractionRules`를 추가해 Room DB, DataStore, app-private attachments를 cloud backup에서 제외.
- Play Store 설명과 Privacy Policy에는 "Android 시스템 백업 포함 여부"를 명시한다.

실제 수정 후보:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/data_extraction_rules.xml` 신규 파일, 선택 시
- `docs/PRIVACY.md`
- `docs/SECURITY.md`
- `README.md`
- `app/src/main/res/values*/strings.xml`, 설정 화면에 설명을 넣는 경우

검증:

- `./gradlew test`
- `./gradlew assembleDebug`
- `rg "allowBackup|dataExtractionRules" -n app/src/main`
- `rg "android.permission.INTERNET" -n app/src` 결과 없음 유지

### P0-2. Release Hardening

문제:

- 현재 release build에서 `isMinifyEnabled = false`다.
- CI의 emulator launch smoke는 `continue-on-error: true`라 실제 release gate로는 약하다.
- 정식 출시 전에는 release APK/AAB에 대한 명확한 품질 게이트가 필요하다.

권장 설계:

- `release` 빌드:
  - `isMinifyEnabled = true`
  - `isShrinkResources = true`
  - 필요한 ProGuard/R8 keep rule만 최소 추가
- CI:
  - tag release 또는 main release-prep에서는 `lintRelease`, `test`, `verifyRoborazziDebug`, `assembleRelease`, `bundleRelease` 실행
  - release APK/AAB 존재 및 크기 > 0 확인
  - signing certificate 검증 유지
  - release 경로에서는 launch smoke를 hard fail로 전환

실제 수정 후보:

- `app/build.gradle.kts`
- `app/proguard-rules.pro` 신규 또는 기존 파일 확인
- `.github/workflows/android-build.yml`
- `docs/RELEASE.md`

주의:

- R8 적용 후 Compose, Room, commonmark, Coil, DocumentFile, FileProvider, AppWidget, ActivityResult 흐름을 실제 기기/에뮬레이터에서 smoke test 해야 한다.

검증:

- `./gradlew test`
- `./gradlew lintRelease`
- `./gradlew assembleRelease`
- `./gradlew bundleRelease`
- signed release 환경이 없으면 debug/unsigned release 한계와 함께 보고

### P0-3. Room Schema Export + Migration Test

문제:

- 현재 `AppDatabase`는 `exportSchema = false`다.
- 상용 노트 앱은 업데이트 중 데이터 손실이 가장 큰 리스크다.
- v4 -> v12까지 migration이 있지만 schema JSON과 migration regression net이 약하다.

권장 설계:

- `exportSchema = true` 전환.
- `room.schemaLocation`을 `app/schemas`로 지정.
- schema JSON 커밋.
- 주요 버전 경로 migration test 추가.
- 최소 검증 시나리오:
  - 오래된 `notes`, `tags`, `note_tag_cross_ref` 유지
  - v9에서 제거된 테이블 이후 v10/v11 재도입 경로 확인
  - `lastImportedAt` 추가 후 기존 노트 보존
  - FTS rebuild 후 검색 가능

실제 수정 후보:

- `app/build.gradle.kts`
- `app/src/main/java/com/markleaf/notes/data/local/AppDatabase.kt`
- `app/src/androidTest/java/...` 또는 `app/src/test/java/...` migration test
- `app/schemas/...` 신규 생성

검증:

- `./gradlew test`
- `./gradlew assembleDebugAndroidTest`
- 가능하면 `connectedDebugAndroidTest`에서 migration test 실행

### P0-4. Privacy / Security 문서 현재화

문제:

- `docs/PRIVACY.md`가 아직 "MVP Privacy Policy Draft" 기준이다.
- 현재 앱에는 이미지 첨부, SAF 폴더 미러 동기화, 외부 링크 열기, 공유, Markdown export가 있다.
- README의 "100% No-Cloud" 문구는 강점이지만, 사용자 주도 공유/폴더 동기화/브라우저 열기와 함께 더 정밀한 표현이 필요하다.

권장 문구 방향:

- Markleaf 자체에는 `android.permission.INTERNET`이 없다.
- Markleaf 서버가 없으므로 Markleaf가 노트를 자체 서버에 업로드하지 않는다.
- 사용자가 export, share, 외부 링크 열기, 다른 앱이 동기화하는 폴더 선택을 수행하면 사용자 선택 앱/OS 경로를 통해 데이터가 기기 밖으로 이동할 수 있다.
- 노트, 태그, 첨부, 메타데이터는 사용자의 명시 행동 전까지 로컬에 남는다.

실제 수정 후보:

- `docs/PRIVACY.md`
- `docs/SECURITY.md`
- `docs/NOCLOUD_CERTIFICATION.md`
- `README.md`
- GitHub Pages `docs/privacy.html`
- Play Console privacy copy, repo 밖 작업

검증:

- 구현과 문구 불일치 검색:
  - `rg "MVP|Draft|100% No-Cloud|never leaves" docs README.md`
  - `rg "ACTION_VIEW|OpenDocument|CreateDocument|OpenDocumentTree|ACTION_SEND" app/src/main/java`

## 3. P1 - 상용 신뢰도 강화

### P1-1. EXIF 제거

문제:

- 이미지 첨부는 app-private storage로 복사되지만, 원본 이미지의 EXIF 위치/기기 정보가 유지될 수 있다.
- 백로그에는 EXIF 제거가 적혀 있으나 `AttachmentManager`에는 구현되어 있지 않다.

권장 설계:

- `androidx.exifinterface:exifinterface` 의존성 추가.
- 이미지 복사 후 JPEG/PNG/WebP 등 처리 가능한 포맷에 대해 민감 EXIF tag 제거.
- 제거 실패 시 첨부 자체는 막지 않되, debug log나 UI 상태로 알릴지 판단.
- "첨부 이미지는 앱 내부 저장소로 복사되며 가능한 메타데이터를 제거한다" 문구 추가.

실제 수정 후보:

- `app/build.gradle.kts`
- `app/src/main/java/com/markleaf/notes/util/AttachmentManager.kt`
- `app/src/test/java/com/markleaf/notes/util/...` 테스트
- `docs/PRIVACY.md`

검증:

- EXIF 포함 샘플 이미지를 test asset으로 두고 복사 후 GPS tag 제거 확인.
- `./gradlew test`

### P1-2. 생체 인식 앱 잠금

문제:

- 민감 노트 앱으로 상용화하려면 앱 진입 잠금은 사용자가 기대하는 수준의 보안 옵션이다.
- 스크린샷 차단만으로는 기기 탈취/공유 기기 사용 시 보호가 부족하다.

권장 설계:

- `androidx.biometric:biometric` 도입 가능성 검토. F-Droid 친화성 확인 필요.
- `AppSettings.appLockEnabled: Boolean`
- `MainActivity` 또는 dedicated `AppLockGate` composable에서 `ON_RESUME` 기준 잠금 상태 확인.
- 잠금 상태에서는 노트 UI를 렌더링하지 않는다.
- 인증 실패/취소 시 빈 잠금 화면 유지.
- PIN fallback은 Android BiometricPrompt의 device credential 허용 여부를 검토.

실제 수정 후보:

- `app/build.gradle.kts`
- `app/src/main/java/com/markleaf/notes/data/settings/AppSettings.kt`
- `AppSettingsRepository.kt`
- `MainActivity.kt`
- `feature/settings/SettingsScreen.kt`
- 신규 `feature/security/AppLockGate.kt`
- strings 3개 locale parity

검증:

- unit: settings round trip
- Robolectric 한계가 있으므로 실제 기기 manual smoke 필요
- lock enabled 상태에서 앱 switch/resume 시 내용 노출 여부 확인

### P1-3. Sync Center / Conflict Center

문제:

- 현재 sync 결과는 Toast 중심이다.
- 자동 reconcile, 수동 sync, conflict duplicate 생성, mirror write 실패를 사용자가 나중에 추적하기 어렵다.

권장 설계:

- DataStore 또는 Room에 최근 sync event 저장.
- 최소 모델:
  - timestamp
  - direction: import/export/delete/mirror_attachments
  - updated/created/skipped/errors/conflicts
  - message
- Settings의 Sync section에서 최근 상태와 오류를 지속 표시.
- conflict duplicate title만 만들지 말고 충돌 목록 진입점을 제공.

실제 수정 후보:

- 신규 `data/sync/SyncEvent.kt`
- `AppSettings`에 last sync summary를 저장하거나 Room 테이블 추가
- `SettingsScreen.kt`
- `NoteFolderMirror.kt`
- `MainActivity.kt`
- `EditorScreen.kt`
- `TrashScreen.kt`

검증:

- conflict logic unit test 확장
- sync result UI snapshot 또는 Robolectric compose test

## 4. P2 - 경쟁력과 전환율

### P2-1. 고품질 PDF 내보내기

목표:

- 사용자가 노트를 외부에 제출/공유할 수 있는 완성물로 만들기.

권장 설계:

- 1차: Android `PdfDocument` 기반으로 Markdown preview line model을 직접 렌더링.
- 2차: Print Framework 사용 검토.
- 외부 서버, WebView remote asset, network 금지.

주의:

- 한글 줄바꿈, 코드 블록, 표, 이미지, 페이지 나눔이 핵심 품질 포인트.

### P2-2. Onboarding 개선

목표:

- starter notes만으로는 첫 사용자의 제품 철학 전달이 약할 수 있다.

권장 설계:

- 첫 실행 3단계:
  - No account
  - Local markdown
  - Export/sync by your folder
- "건너뛰기" 명확히 제공.
- Starter notes는 유지하되, onboarding 후 예시 노트로 자연스럽게 연결.

### P2-3. Store Packaging

목표:

- Play 정식 출시 전 전환율과 신뢰도를 끌어올림.

작업:

- 스토어 스크린샷 리디자인
- feature graphic 최종화
- privacy policy URL 최신화
- No-Cloud 문구 정밀화
- GitHub Release asset과 Play AAB 버전 일치 확인

## 5. 다음 세션 추천 시작 순서

다음 세션에서는 새 기능을 추가하지 말고 아래 순서로 시작한다.

1. `AGENTS.md`, `docs/AGENT_SPEC.md`, `.agent/tasks.md`, `.agent/progress.md`, `.agent/decisions.md` 읽기.
2. 이 문서 `docs/COMMERCIAL_READINESS_PLAN.md` 읽기.
3. `.agent/tasks.md`의 "Phase 22 - Commercial Readiness"에서 가장 위의 unchecked task 하나만 선택.
4. 첫 권장 task는 `[Commercial P0-1] Android Backup / Data Extraction 정책 확정`.
5. 구현 후 `./gradlew test`, `./gradlew assembleDebug`, `rg "android.permission.INTERNET" -n app/src` 실행.
6. 변경이 사용자/정책 영향이면 `CHANGELOG.md`, `HISTORY.md`, `docs/PRIVACY.md`를 함께 갱신.

## 6. 상용화 판단 기준

정식 출시 준비 완료로 판단하려면 최소한 다음이 참이어야 한다.

- `android.permission.INTERNET` 없음.
- 개인정보 문서가 v2.x 실제 기능을 설명한다.
- Android backup/data extraction 정책이 명시되어 있다.
- release build가 R8/shrink 또는 그에 준하는 production gate를 통과한다.
- Room schema export와 migration regression test가 있다.
- sync/import/export 실패가 사용자가 확인 가능한 방식으로 남는다.
- Play Store용 privacy copy, screenshot, feature graphic이 현재 앱과 일치한다.

