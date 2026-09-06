# Markleaf Tasks

이 파일은 랄프 루프에서 사용할 작업 목록입니다.  
에이전트는 매 루프마다 가장 위의 unchecked task 하나만 선택해 구현합니다.

---

## GitHub Issue #363 - External File Timestamps (Done, 2026-09-06)

- [x] 외부 보고자에게 감사·트리아지 댓글을 남기고 `Open file…` 및 공유 파일 가져오기 경로를 재현
- [x] 원본 제공자 메타데이터를 읽기 전에 캡처하고 Markleaf frontmatter 시각을 우선하는 수정 구현
- [x] PR #364 리뷰·필수 CI·계측 테스트를 통과시키고 머지
- [x] v2.37.1 / versionCode 136 릴리스 문서·8개 로케일·`D:\Build` 산출물 검증 준비

---

## User-requested Project Direction Refresh (Done, 2026-07-28)

- [x] 공개 저장소·릴리스·CI·배포 채널을 점검하고, 최신 설치 경로와 3/6개월 안정화 게이트를
  `README*.md`와 `docs/ROADMAP.md`에 반영

## User-requested Public Surface Renewal (Done, 2026-07-15)

- [x] GitHub Pages 랜딩을 Quiet proof 방향으로 재구성하고 실제 앱 화면, no-INTERNET 증거, F-Droid 우선 설치 경로, 반응형·접근성·SEO를 검증

---

## Phase 29 - Quiet Editor (Done, 2026-07-16)

목적: v2.22까지 쌓인 기능을 상시 chrome으로 노출하지 않고, 글이 중심이 되는 편집 화면으로
재구성합니다. 전체 계약과 후속 단계는 `docs/ROADMAP.md`의 *v2.23+ — Bear-class 제품
응집도*를 따릅니다.

- [x] `DESIGN.md`에 compact formatting entry, selection-context actions, expanded style panel의 상태·포커스·접근성 계약 고정
- [x] 상시 가로 스크롤 툴바를 작은 진입점과 문맥 패널로 재구성하되 기존 액션·단축키·Quick Insert 보존
- [x] Quiet Editor 포맷팅 단계를 v2.23.0 / versionCode 105로 릴리스하고 양쪽 배포 자산 검증
- [x] 에디터 상단 앱 바와 통계·목차·백링크 Info surface의 정보 구조 정리
- [x] 제목 중복 excerpt, Android plurals, vector empty state, tablet sparse Tags/Settings surface 마감
- [x] phone/tablet 수동 QA, TalkBack, 6개 언어 parity, Roborazzi, test/lint/build 검증

---

## Phase 30 - Living Markdown (Planned, after Phase 29)

목적: 저장 데이터는 순수 Markdown으로 유지하면서 편집 화면 자체를 읽기 좋은 문서 표면으로
발전시킵니다. full WYSIWYG/block editor는 범위 밖입니다.

- [ ] Compose cursor/selection/IME/offset/performance feasibility harness 구축
- [ ] 활성 줄·선택 영역은 마커를 보이고 비활성 영역은 마커가 물러나는 syntax reveal 구현
- [ ] 편집/미리보기 체크리스트 직접 조작의 동작과 접근성 일치
- [ ] 저장 원문을 바꾸지 않는 heading/list folding 구현
- [ ] 표준 GFM Markdown에 행·열을 조작하는 구조화된 table actions 구현
- [ ] 한글·일본어 IME, RTL 후보 텍스트, TalkBack, hardware keyboard, large-note 회귀 검증

---

## Phase 31 - Smart Library (Planned, after Phase 30)

목적: 태그·검색·스마트 컬렉션을 phone/tablet 공통 탐색 구조로 통합합니다.

- [ ] phone navigation sheet와 tablet tag rail의 공통 information architecture 구현
- [ ] All Notes / Today / Todos / Attachments / Untagged / Conflicts 로컬 스마트 컬렉션 구현
- [ ] 태그 고정, 태그 트리 검색·접기, 정확한 note counts 구현
- [ ] 저장된 검색과 Quick Switcher/Search 결과 규칙 통합
- [ ] 목록 정렬·preview density·attachment thumbnail 설정과 10k+ 노트 성능 검증

---

## Phase 32 - Capture Everywhere (Planned, after Phase 31)

목적: Android의 명시적 공유·선택 동작으로만 외부 내용을 로컬 Markdown 노트에 빠르게 담습니다.

- [ ] `ACTION_PROCESS_TEXT` 선택 텍스트를 새 노트 또는 기존 노트에 추가
- [ ] 공유 앱이 제공한 `EXTRA_HTML_TEXT`만 기기 안에서 Markdown으로 변환
- [ ] 여러 이미지 공유와 출처 URL/제목 보존 규칙 구현
- [ ] 새 노트·검색·고정 태그 런처 바로가기 구현
- [ ] no-INTERNET, MIME/크기 경계, attachment/export/mirror round-trip 검증

---

## Phase 33 - Personal Writing (Planned, after Phase 32)

목적: Markleaf identity를 유지하는 소수의 palette와 제한된 typography preset을 제공합니다.

- [ ] Markleaf Paper / Forest Night / Graphite 후보 palette의 light/dark 접근성 계약 검토
- [ ] 본문 크기·줄 간격·문단 간격 preset 구현
- [ ] system sans/serif, Material You, 6개 언어, large-font 조합 검증
- [ ] phone/tablet Roborazzi와 APK 크기·렌더링 성능 회귀 검증

---

## Phase 28 - Public GitLab Releases (Done)

- [x] Publish the GitLab mirror as a public repository
- [x] Add independent GitLab branch/MR verification and protected tag signing
- [x] Store GitLab Release assets permanently in the Generic Package Registry
- [x] Preserve the local Play hand-off contract at `D:\Build`
- [x] Backfill and publicly verify all four v2.22.0 GitLab Release assets
- [x] Cross-link the GitHub repository and GitLab mirror in all README locales
- [x] Verify both remotes are writable and document ordered dual-push synchronization

---

## Phase 27 - Slash Quick Insert & v2.22.0 Release (Done)

- [x] Add line-scoped `/` command detection and deterministic Markdown insertion
- [x] Add localized Quick Insert panel with touch and hardware-keyboard selection
- [x] Preserve toolbar, focus, autosave, wikilink/tag autocomplete, and SAF image flow
- [x] Add unit, Compose, resource parity, and instrumented editor coverage
- [x] Prepare v2.22.0 / versionCode 104 release metadata and six locale notes
- [x] Verify and export signed release artifacts to `D:\Build`
- [x] Publish and verify GitLab/GitHub release refs and F-Droid handoff

---

## Public App Info Refresh for v2.19.1 (Done)

- [x] Update README release links across supported README locales
- [x] Update GitHub Pages landing current-version strip and GitHub Releases link
- [x] Update No-Cloud certification version floor
- [x] Update F-Droid metadata draft current version/build entry

---

## GitHub Issue #144 - Dash Tag Filter (Done)

- [x] Triage issue #144 and thank the external reporter
- [x] Fix tag taps so `#old-notes` and other indexed tags filter through the tag index instead of the FTS query parser
- [x] Add regression coverage for dash-tag filtering
- [x] Prepare v2.19.1 release metadata

---

## Phase 26 - Beautiful Sample Notebook Onboarding (Done)

목적: 설명형 온보딩을 늘리는 대신, Markleaf 기능을 실제로 활용한 아름다운 샘플 노트북을 첫 설치 시 제공해 사용자가 3분 안에 쓰임을 체감하게 합니다.

- [x] **1. Starter Notes v2 Content**
  - [x] 기존 4개 안내 노트를 6개 샘플 노트북으로 확장
  - [x] Welcome, Markdown showcase, daily journal, project brief, tags/search/backlinks, local folder mirror 흐름으로 구성
  - [x] 각 지원 로케일(raw, ko, es, de, ja, fr)에 같은 6개 구조 제공
- [x] **2. Real Feature Demonstration**
  - [x] 이미지, 콜아웃, 표, 코드 블록, 각주, 체크리스트, 위키링크, 계층 태그, 검색 키워드, 폴더 미러 설명 포함
  - [x] 번들 feature graphic을 starter attachment로 복사해 미리보기에서 실제 이미지가 렌더링되도록 구성
  - [x] starter note 생성 시 태그뿐 아니라 위키링크도 색인해 backlinks가 바로 작동하도록 개선
- [x] **3. Tests & Resource Parity**
  - [x] starter note 개수, 이미지 참조, 콜아웃/표/각주/위키링크 포함 여부 테스트
  - [x] seed 시 태그 색인, 링크 색인, starter attachment 복사 검증
  - [x] 로케일별 starter note separator 개수 검증을 6개 노트 기준으로 갱신

---

## Phase 25 - Final Writing Feel Polish (Done)

목적: 새 기능을 추가하지 않고, 에디터의 손끝 감각을 마지막으로 다듬습니다. 커서 포커스 복귀, 미리보기 전환 감각, 편집/미리보기 캔버스 정렬, 툴바 밀도처럼 사용자가 매일 느끼는 작은 마찰만 줄입니다.

- [x] **1. Editor Focus Continuity**
  - [x] 새 노트 진입 시 곧바로 쓸 수 있도록 에디터 포커스를 요청
  - [x] 미리보기에서 편집으로 돌아올 때 커서 포커스를 자연스럽게 복귀
  - [x] 포맷팅 툴바, 위키링크 자동완성, 찾기/바꾸기 조작 후 편집 포커스를 유지
- [x] **2. Preview Transition & Canvas Alignment**
  - [x] 편집/미리보기 전환을 `Crossfade`로 부드럽게 전환
  - [x] 미리보기 좌우 패딩을 편집 캔버스와 같은 `20.dp` 기준선으로 정렬
- [x] **3. Toolbar Density Polish**
  - [x] 터치 타겟은 유지하면서 툴바의 위쪽 여백과 그룹 디바이더 좌우 여백을 줄여 더 가볍게 조정
- [x] **4. Verification & Stabilize**
  - [x] 로컬 유닛 테스트 (`./gradlew testDebugUnitTest`) 구동 및 통과 확인
  - [x] 앱 빌드 (`./gradlew assembleDebug`) 성공 확인

---

## Phase 24 - Writing Canvas & Empty State Premium Polish (Done)

목적: 기능 추가보다는 에디터의 캔버스 감각(타이포그래피, 줄간격, 패딩)과 빈 상태(Empty State), 노트 목록의 시각적 리듬(수정 시간 표시, 여백 튜닝)을 Bear-class 수준으로 고급스럽게 개선합니다.

- [x] **1. Theme & Typography Updates**
  - [x] `Theme.kt`에서 `bodyLarge` 텍스트 스타일의 `lineHeight`를 `26.sp`로 확장하여 시각적으로 시원하게 튜닝
- [x] **2. Editor Screen Typography & Padding Adjustments**
  - [x] `EditorScreen.kt` 내 `BasicTextField`에 `MaterialTheme.typography.bodyLarge`를 명시적으로 부여하고 적절한 텍스트 색상 연동
  - [x] 에디터 본문 캔버스의 가로/세로 패딩 튜닝 (`horizontal = 20.dp, vertical = 12.dp`)
- [x] **3. Editor Screen Empty State Refinement**
  - [x] `EditorScreen.kt`의 `decorationBox` 빈 상태 아이콘 및 문구 정렬 튜닝 (가로/세로 비율 및 여백 최적화)
- [x] **4. Notes List Row Metadata (Date) Integration**
  - [x] `NotesListScreen.kt` 내 `NoteRow` 컴포저블 하단에 `note.updatedAt` 기준의 마지막 수정 날짜/시간 포맷 표시
  - [x] 날짜를 표시할 헬퍼 함수 (`formatUpdatedTime`) 작성
- [x] **5. Notes List Section Headers & Card Visual Rhythm Polish**
  - [x] `NotesListScreen.kt` 내 `SectionHeader` 및 `NoteRow`들 간의 세로 간격, 코너 둥글기, 하이라이트 여백 조정
- [x] **6. Notes List Empty State Refinement**
  - [x] `NotesListScreen.kt` 내의 빈 상태 뷰 레이아웃 튜닝 (버튼 디자인, 텍스트 정렬 및 여백 프리미엄 리터칭)
- [x] **7. Verification & Stabilize**
  - [x] 로컬 유닛 테스트 (`./gradlew test`) 구동 및 통과 확인
  - [x] 앱 빌드 (`./gradlew assembleDebug`) 성공 확인

---

## Phase 0 - Repository Preparation (Done)
- [x] All preparation tasks completed.

---

## Phase 1 - Project Foundation (Done)
- [x] All foundation tasks completed.

---

## Phase 2 - Local Notes (Done)
- [x] All local note tasks completed.

---

## Phase 3 - Tags (Done)
- [x] All tag tasks completed.

---

## Phase 4 - Search and Trash (Done)
- [x] All search and trash tasks completed.

---

## Phase 5 - Export and Polish (Done)
- [x] All export and polish tasks completed.

---

## Phase 6 - Branting & v0.x Features (Done)
- [x] Create app icon (Adaptive Icon)
- [x] Create branding page (GitHub Pages)
- [x] Implement Dark Mode support
- [x] Implement Markdown Preview
- [x] Implement SQLite FTS Search (v0.3.0)
- [x] Implement Image Attachments (v0.3.0)
- [x] Implement Note Links [[Link]] (v0.4.0)

---

## Phase 7 - Final Road to v1.0.0 (Done)
- [x] [#8] Implement Tablet Two-Pane Layout
- [x] [#10] Implement Backlinks section in note detail
- [x] [#9] Implement Enhanced Backup/Restore (ZIP export/import)
- [x] [#11] Polish UI/UX and Material You support
- [x] [#12] Final release stabilization for F-Droid
- [x] Update README.md and documentation for v1.0.0

---

## Phase 10 - Device UI Verification (Closed at v2.5.3, 2026-05-09)
- [x] Refresh AndroidX test runtime and isolate Compose UI tests with an in-memory app harness
- [x] ~~Resolve Lenovo TB320FC Compose test host lifecycle issue~~ — **superseded by v1.9 Roborazzi visual regression net.** Robolectric-based snapshot tests give us per-PR visual diff coverage without depending on emulator/device Compose UI test reliability. Closing this rather than chasing the original instrumentation flake.

---

## Phase 8 - Post v1.0.0 & Future (Backlog)
- [x] Implement fixed Signing Keystore (Prevent Update Conflict)
- [x] ~~Implement WebDAV sync (Optional)~~ — **subsumed by v2.1 SAF folder mirror** (any cloud/WebDAV client that syncs the chosen folder works without us writing protocol code).
- [x] ~~Implement Google Drive backup (Optional)~~ — **subsumed by v2.1 SAF folder mirror** (point Markleaf at a Google Drive folder on the device).
- [x] Advanced Markdown support (Tables, KaTeX-style math notation preview)
- [x] Note version history (Snapshots)
- [x] Performance optimization for 10k+ notes
- [x] Multi-language support (i18n)

---

## Phase 9 - Bear-Class Product Polish (Backlog)
- [x] [#15] Add first-run starter notes onboarding
- [x] [#17] Improve Markdown link preview and settings navigation
- [x] Improve editor Markdown toolbar (#18)
- [x] Clarify editor link toolbar buttons with distinct affordances and tooltips
- [x] Publish editor link toolbar clarification as v1.1.15
- [x] Tablet note list collapse and constrained editor width (#19)
- [x] Settings foundation for Markdown visibility and line width (#20)
- [x] Live Markdown editor inline syntax highlighting (#21)
- [x] English default and Korean localization (#22)
- [x] Tablet two-pane visual separation polish (#23)
- [x] Improve note list and editor empty states
- [x] Add quick-open search for notes, tags, and links
- [x] Audit theme application and improve note list title contrast
- [x] Improve tag screen counts and navigation
- [x] Improve backlinks with context snippets
- [x] Improve export and backup status messages
- [x] Add large dataset performance checks

---

## Phase 14 - Platform Polish (Done, 2026-05-08, v1.5.0)

목적: 안드로이드 정상 시민 마감. 플랫폼 표준에 정확히 맞춰 *"잘 만든 안드로이드 앱"* 인상으로 끌어올리는 묶음. 각 항목은 작고 독립적이라 한 사이클에 묶어 진행.

- [x] Material You 다이내믹 컬러 (#59) — `MarkleafTheme.dynamicColor` 기본값을 `true`로 전환. `Build.VERSION.SDK_INT >= S`에서만 `dynamicLight/DarkColorScheme(context)` 사용, 그 외에는 기존 그린 정적 색상이 폴백.
- [x] 예측 뒤로가기 제스처 (#27) — `<application android:enableOnBackInvokedCallback="true">`. NavHost가 자동으로 OnBackInvoked dispatcher와 연동됨.
- [x] 단일 노트 시스템 공유 — 에디터 TopAppBar에 `Icons.Default.Share` IconButton, 클릭 시 입력 중인 본문/제목으로 Note를 합성해 `ShareNoteUtil.shareNote` 호출. AndroidManifest에 누락돼 있던 `androidx.core.content.FileProvider`도 `${applicationId}.fileprovider` authority로 함께 등록 + `xml/file_paths.xml` 추가.
- [x] 공유 시트로 받은 텍스트 → 새 노트 — `MainActivity`에 `<intent-filter ACTION_SEND text/plain>`, `extractSharedText()`가 `EXTRA_SUBJECT`를 `# 제목` H1으로 변환해 `EXTRA_TEXT`와 결합. NavHost에 `sharedText` param 추가, NOTES composable의 LaunchedEffect가 `viewModel.createNote(sharedText)`로 시드 후 에디터 진입. `onNewIntent`에서 SEND 재진입은 `setIntent + recreate()`.
- [x] FLAG_SECURE 토글 (#47) — `AppSettings.screenshotProtection: Boolean = false` + `AppSettingsRepository.setScreenshotProtection`. Settings 화면에 "화면 보안" 섹션 + 스위치. `MainActivity`가 `repeatOnLifecycle(STARTED)`에서 settings flow를 관찰, `distinctUntilChanged`로 dedup 후 `window.addFlags(FLAG_SECURE)` / `clearFlags`. 기본값은 꺼짐.

---

## Phase 15 - Markdown Expressiveness (Done, 2026-05-08, v1.6.0)

목적: 손파서가 ~150줄로 여유 있을 때 가성비 높은 문법을 추가. *Bear/Obsidian 호환성* 도 부수적으로 ↑.

- [x] 콜아웃 `> [!NOTE]` / `[!WARN]` / `[!TIP]` / `[!IMPORTANT]` / `[!CAUTION]` — 헤드라인 다음 연속된 `>` 라인을 단일 `PreviewLineType.CALLOUT` 블록으로 합침. 종류는 `extra`에 저장하고 `CalloutKind.parse()`로 별칭(WARN/DANGER) 처리. 미리보기 색상 박스 + 아이콘.
- [x] 프론트매터 YAML 인식 — `parse()` 진입 시 `rawLines[0].trim() == "---"` 이면 다음 `---`까지를 `PreviewLineType.FRONTMATTER`로 묶음. 본문 중간의 `---`는 기존대로 가로선.
- [x] 각주 `[^1]` / `[^1]: ...` — 라인 단위 `FOOTNOTE_DEF` + 인라인 `FOOTNOTE_REF`(부정 lookahead로 def와 충돌 회피). 미리보기에서 ref는 위첨자, def는 본문 하단 별도 행. ref↔def 클릭 점프는 v1.7+ 보류.
- [x] Tab/Shift+Tab 들여쓰기 — `MarkdownEditActions.indent`/`outdent` + 에디터 BasicTextField `Modifier.onPreviewKeyEvent`. 다중 줄 선택 시 일괄 처리.

추가로 §7 *반드시 포함* 누락 검출 작업 동시 처리:
- [x] **단일 노트 .md 저장** — 에디터 Share 버튼을 DropdownMenu로 확장 (공유 시트 / .md 파일로 저장). 후자는 SAF `ACTION_CREATE_DOCUMENT`.
- [x] **전체 노트 일괄 내보내기** — 설정 화면 "데이터" 섹션 + SAF `ACTION_OPEN_DOCUMENT_TREE` → `ExportAllNotes.exportAllNotes`.

SimpleMarkdownPreview.kt LOC: 178 → ~210 (한계 ~300 안에 안전).

---

## Phase 20 - SAF Folder Mirror Sync (Done, 2026-05-08, v2.1.0)

목적: §2.2 로컬 우선 정신 유지하면서 다중 기기 지원. 우리 서버 0, INTERNET 권한 0. 사용자가 폴더 위치를 정하면 OS/타사 앱이 sync를 담당.

- [x] AppSettings에 `syncFolderUri` + `syncLastSyncedAt` 추가, DataStore round-trip
- [x] `SyncFrontmatter` — `.md` 헤더 encode/decode (markleaf_id, ISO timestamps, pinned, archived, opaque unknown keys 보존)
- [x] `NoteFolderMirror.writeNote()` — SAF DocumentFile, idempotent, id-marker 파일 매칭
- [x] `NoteFolderMirror.importChanges()` — 수동 reconcile, 충돌 시 file lastModified vs updatedAt 비교(2초 슬랙)
- [x] EditorScreen 자동 저장 직후 폴더 미러 (디바운스 1초)
- [x] Settings *Sync* 섹션 — 동작 원리 명시, 권장 폴더 위치, 폴더 선택/변경/끄기/지금 동기화 4 액션
- [x] Strings 3언어 동기 (14키), Roborazzi 골든 영향 없음
- [x] 8개 frontmatter codec 단위 테스트

의도적으로 v2.1.0에서 빼고 v2.1.x로 미룬 것:
- 노트 *삭제* 동기화 (DB→파일 / 파일→DB) — 데이터 손실 위험 가장 큼
- 앱 시작 시 자동 reconcile — silent overwrite 회피
- 충돌 시 양 버전 보존 UI

---

## Phase 21 - GitHub Open Issues (Backlog)

GitHub에 열려 있는 이슈를 기준으로 한 미구현 작업 목록.
각 항목의 상세 스펙은 `docs/ISSUE_BACKLOG_DETAIL.md` 를 참조.

### 완료되었으나 GitHub에서 Open 상태인 항목 (Close 필요)
- [x] [#27] 예측 뒤로 가기 제스처 — v1.5.0에서 `<application android:enableOnBackInvokedCallback="true">` 등록 완료
- [x] [#65] SQLite FTS5 통합 검색 — v0.3.0에서 FTS4/FTS5 도입 완료, Phase 8에서 검증 완료
- [x] [#76] WCAG 기준 접근성 최적화 — v1.7.0에서 TalkBack/contentDescription, 터치 타겟, 색상 대비 모두 검증 완료

### 보류 (외부 조건 필요)
- [x] [#51] 다국어 지원 확대 (JP, FR 완료, DE 완료) — JP/FR 번역 및 독일어(DE) 번역/스타터 노트 최종 검수 및 보강 완료.
- [x] [#53] 스토어 스크린샷 리디자인 — v2.x UI 기반 프리미엄 스토어 스크린샷 목업 생성 완료.
- [x] [#54] 전문 브랜드 피처 그래픽 업데이트 — 1024x500 규격의 프리미엄 브랜드 피처 그래픽 이미지 생성 완료.

### 신규 기능 (완료 및 정리)
- [x] [#37] 생체 인식(지문/안면) 앱 잠금 — `androidx.biometric` 기반 BiometricPrompt 잠금/해제 구현 완료 (`[Commercial P1-2]`)
- [x] [#38] 고품질 PDF 내보내기 — Markdown 렌더링 기반 A4 여백 튜닝 및 PDF 인쇄 파이프라인 구현 완료 (`[Commercial P2-1]`)
- [x] [#39] 홈 화면 위젯 (최근 노트, 빠른 작성) — ListView 기반 컬렉션 위젯 및 퀵 링크 에디터 다이렉트 진입 구현 완료 (v2.16.0)
- [x] [#52] 전략적 인앱 리뷰 요청 UI — OSS 및 로컬 퍼스트 프라이버시 철학과 어긋나므로 의도적으로 기각 및 트래커 정리 완료
- [x] [#55] 오픈소스 투명성 강조 — 설정 화면 Open Source 섹션 (GitHub, 라이선스, F-Droid 링크) 구현 완료 (v2.16.0)
- [x] [#57] 인터랙티브 온보딩 가이드 — 4단계 WelcomeOnboardingSheet 모달 바텀 시트 구현 완료 (v2.16.0)

## Phase 23 - Smart Formatting Toggle & Wrapping (Done)

목적: iOS의 Bear 앱과 같은 프리미엄 경험을 제공하기 위해 마크다운 포맷팅 편집 액션(Bold, Italic, Strikethrough, Inline Code)을 스마트 토글 및 언랩(Toggle/Unwrap)과 지능형 주변 단어 감싸기(Smart Word Wrapping) 방식으로 고도화합니다.

- [x] [Smart Formatting] MarkdownEditActions.wrapSelection 리팩토링 및 스마트 토글 구현
- [x] [Smart Formatting] MarkdownEditActions.findWordAtCursor 지능형 단어 탐색 구현
- [x] [Smart Formatting] MarkdownEditActionsTest.kt에 토글/언랩 및 단어 감싸기 단위 테스트 추가 및 검증
- [x] [Smart Formatting] 실제 에디터 화면(EditorScreen)에서 수동 동작 검증 및 연동 상태 확인

---

## Phase 22 - Commercial Readiness (Backlog, 2026-05-14)

목적: MVP 이후 기능 확장이 충분히 진행된 Markleaf를 Play 정식 출시와 장기 사용자 데이터 보관에 견딜 수 있는 상용 수준으로 마감한다. 새 기능보다 데이터 보호, 릴리즈 게이트, 개인정보 문서, 동기화 장애 가시성을 우선한다.

상세 설계 문서:
- `docs/COMMERCIAL_READINESS_PLAN.md`

P0 - 정식 출시 전 필수:
- [x] [Commercial P0-1] Android Backup / Data Extraction 정책 확정 — `android:allowBackup="false"`로 Android Auto Backup / D2D transfer 모두에서 Markleaf 데이터 제외. `dataExtractionRules`는 *전체 제외* 케이스에 과한 표면적이라 미도입(D046). Privacy/Security/NoCloud/README 모두 v2.x 기준으로 갱신.
- [x] [Commercial P0-2] Release hardening — R8 + resource shrink 활성화(`isMinifyEnabled=true`, `isShrinkResources=true`, minimal `proguard-rules.pro`); `lintRelease` + `assembleRelease` CI hard gate; tag 릴리즈에 `mapping.txt` 자산 첨부(D047). APK 12 MB → 1.7 MB. release-APK runtime smoke 는 후속 사이클.
- [x] [Commercial P0-3] Room schema export + migration regression test — `exportSchema=true`, KSP `room.schemaLocation=app/schemas`, v12 schema JSON 커밋, v4 레거시 DB → v12 migration regression test 추가. F-Droid 빌드 재현성을 위해 추적 중이던 `local.properties` 제거.
- [x] [Commercial P0-4] Privacy / Security 문서 현재화 — P0-1 작업에서 `docs/PRIVACY.md`(MVP draft 폐기), `docs/SECURITY.md`, `docs/NOCLOUD_CERTIFICATION.md`, `README.md` 모두 v2.x 기준으로 재작성. 이미지/SAF sync/share/external link 동작 정확히 구분(D046). 잔존 "MVP draft" 문구는 검증 결과 없음.

P1 - 상용 신뢰도 강화:
- [x] [Commercial P1-1] 이미지 첨부 EXIF 제거 — 첨부 저장 시 위치/기기 메타데이터 제거 및 테스트 추가
- [x] [Commercial P1-2] 생체 인식 앱 잠금 — BiometricPrompt 기반 잠금/해제 흐름과 설정 토글
- [x] [Commercial P1-3] Sync Center / Conflict Center — Toast로 사라지는 동기화 결과를 지속 상태와 충돌 목록으로 노출

P2 - 전환율/경쟁력:
- [x] [Commercial P2-1] 고품질 PDF 내보내기 — Markdown preview 기반 PDF 렌더링
- [x] [Commercial P2-2] 첫 실행 온보딩 개선 — No account / local markdown / export & folder sync 철학을 짧게 전달
- [x] [Commercial P2-3] Store packaging — 스크린샷, feature graphic, Play privacy copy, F-Droid metadata 정리

## Phase 19 - Inline Rich Rendering (Done, 2026-05-08, v2.0.0)

목적: Bear의 핵심 체감 차이(헤딩이 입력 즉시 *진짜로* 커지고, 굵게가 진짜로 굵어지는 라이브 프리뷰)를 Compose VisualTransformation으로 구현. 가치관 점검에서 사용자가 명시한 갈증의 직접 응답.

- [x] `MarkdownSyntaxHighlighter` 헤딩 분기 재구성 — 마커 길이별 fontSize(24/20/18sp) + fontWeight(Bold/SemiBold/SemiBold) 콘텐츠 범위에만 적용
- [x] `BOLD_REGEX` 매치를 SemiBold → Bold 로 강화
- [x] `muteMarkerStyle()` 헬퍼 — 모든 inline 마커(`**`/`*`/`_`/`~~`/백틱/`[`/`](`/`)`)를 color=syntax + Normal weight + Normal style + no decoration 로 reset → Bear 패턴의 marker retreat
- [x] 5개 단위 테스트 + 4개 editor-live Roborazzi 골든

보류: Phase B (commonmark-java 라이브러리 교체) — 추측 기반 인프라 리팩터, §2.9 위반 위험. 위키링크/하이라이트 등 실제 확장 도입 결정이 나면 그때 사이클로.

다음 (예정): v2.1 — SAF 폴더 미러 동기화 (Option D)

## Phase 18 - Roborazzi Visual Regression Net (Done, 2026-05-08, v1.9.0)

목적: Bear-급 라이브 프리뷰로 가는 v2.0 / v2.1 전에, 라이브 프리뷰 *시각 회귀* 를 GitHub CI에서 잡을 수 있는 그물망 도입.

- [x] Roborazzi 1.20.0 + Compose UI test 의존성 (testImpl only, APK 영향 0)
- [x] `MarkdownPreviewList` 컴포저블을 EditorScreen에서 분리 (테스트 가능 단위)
- [x] 14개 시각 골든 — 각 PreviewLineType + 콜아웃 3종 + frontmatter + 각주 + 혼합 문서 × 라이트/다크
- [x] CI 워크플로에 `verifyRoborazziDebug` + 실패 시 diff artifact 업로드
- [x] `changeThreshold = 0.05f` (Windows record ↔ Linux verify 폰트 미세 차이 흡수, 추후 동일 OS golden 만들면 더 타이트하게)

다음: v2.0 — 인라인 rich rendering (`VisualTransformation` 색상 → 색상+폰트크기+굵기) + 손파서 → CommonMark 라이브러리.

## Phase 17 - Chrome Consolidation (Done, 2026-05-08, v1.8.0)

목적: v1.5–1.7가 누적한 상단바·설정 chrome density를 §2.5(단순하지만 허전하지 않게) 기준으로 정리. 새 기능 0.

- [x] NotesListScreen TopAppBar — 5 actions → search/tags primary + ⋮ overflow (archive/trash/settings)
- [x] EditorScreen TopAppBar — 5 actions → search/preview-toggle/share primary + ⋮ overflow (focus/trash). focus는 미리보기 모드에선 숨김 (쓰기에서만 의미).
- [x] SettingsScreen — `settings_security` 단일-스위치 섹션을 `settings_privacy` 섹션으로 흡수. 5 sections → 4.
- [x] strings: `more_options` 추가, `settings_security` 삭제 (3 lang).

§2.5 임계점에서 한 발 물러섬. 다음 사이클에 새 항목이 추가되더라도 한동안 여유 있음.

## Phase 16 - Spec Closure (Done, 2026-05-08, v1.7.0)

목적: AGENT_SPEC §7 *반드시 포함* + *있으면 좋은* 항목 중 마지막 미완 항목들을 닫음.

- [x] 아카이브 UI — `Note.archived` 필드 살리기. `NoteDao.observeArchivedNotes` + `setArchived`, 메인/검색 쿼리에 `AND archived = 0` 추가. `ArchiveViewModel`(TrashViewModel 패턴) + `feature/archive/ArchiveScreen` (long-press 드롭다운: 보관 해제 / 휴지통). NotesListScreen TopAppBar에 보관함 아이콘 + long-press 메뉴에 "보관" 항목.
- [x] 접근성 검증 — IconButton 5개 파일 전수 검증 (모두 stringResource 라벨), `contentDescription = null` 케이스는 의도된 데코레이티브 아이콘만. 라이트/다크 색상 팔레트 surfaceVariant↔onSurfaceVariant / primary↔primaryContainer 모두 WCAG AA 4.5:1 통과 확인. Material 3 IconButton/DropdownMenuItem 기본 터치 영역(>=48dp).
- [x] 다국어 확대 (#51) — JP/FR 번역에 이어 DE(독일어) 로컬라이제이션 검증 및 누락 없는 100% key-parity 보강 완료.
- [x] 스토어 그래픽 / 스크린샷 (#53, #54) — 상용 릴리즈 게이트 통과용 프리미엄 스토어 그래픽/스크린샷 생성 완료.

§7 *반드시 포함* 16/16, *있으면 좋은* 7/7 — MVP 마감.

---

## Phase 13 - Organization And Writing Habits (Done, 2026-05-08)

목적: Bear급 사용 경험으로 가는 다음 단계 — 정리/글쓰기 습관에 직결되는 핵심 3종.

- [x] Tag hierarchy: allow `#parent/child/grand` (each segment validated like a normal tag name) and render the Tags screen as an indented tree
- [x] Focus mode: hide toolbar, stats footer, syntax highlighting, preview/trash buttons; expose a single exit toggle in the top bar
- [x] Find in note: search icon opens an inline FindBar with case-insensitive matching, prev/next navigation, and current/total counter; selection auto-scrolls to the active match
- [x] Tests: TagParser nesting + FindRanges
- [x] Localized strings (en/ko/es)

## Phase 12 - Writing Tool Evolution (Done, 2026-05-08)

목적: v1.2.0 가벼움 회귀로 비운 토대 위에 Bear급 글쓰기 경험으로 한 단계 도약.

- [x] Extend the editor toolbar to 12 actions (heading cycle, bullet, ordered, blockquote, code, code block, hr, strikethrough, inline code...) with grouped dividers
- [x] Smart auto-continuation on Enter for bullet / ordered / checklist / blockquote (with empty-prefix line auto-end and ordered-list increment)
- [x] Pin toggle UI via long-press dropdown on the notes list; surface a pin glyph on the row
- [x] Group the notes list into Pinned / Today / Yesterday / Past 7 days / Older
- [x] Show real-time word / character / reading-minute stats in the editor footer
- [x] Wire setPinned through NoteRepository / DAO / ViewModel without schema change
- [x] Tests for the new edit actions and auto-continuation cases

## Phase 11 - Lightweight Realignment (Done, 2026-05-08)

목적: 사용자 피드백("휴지통은 있는데 거기로 보낼 길이 없다", "가벼움/편의성에 부합하지 않는 기능은 과감히 제거하라")에 따라 AGENT_SPEC §2 (속도/단순함/데이터 소유권)와 §7 (MVP 제외) 원칙으로 회귀.

- [x] Restore the missing trash entry: editor top bar trash button + list long-press confirmation
- [x] Remove the notes-list dashboard card (total/pinned/tags counts)
- [x] Remove drag-to-reorder (it hijacked long-press and broke the trash flow)
- [x] Remove version history (snapshots) feature — auto-save + trash already cover undo
- [x] Remove backlinks / wiki link feature (`[[Title]]` indexing, panel, Quick Open links)
- [x] Remove image attachments (Coil dep, AttachmentEntity, media permissions)
- [x] Remove ZIP backup/restore (Markdown export remains)
- [x] Remove math + table preview rendering (still typeable as raw text)
- [x] Remove toolbar config switches; keep bold/italic/checkbox/markdownLink always-on
- [x] Remove unused notification permission and storage permissions
- [x] Drop `note_snapshots`, `note_links`, `attachments` tables via DB v9 migration
- [x] Remove ChecklistProgressIndicator and unused ChecklistParser
- [x] Update CHANGELOG / HISTORY / unit + UI tests
- [x] Verify with `./gradlew assembleDebug`, `test`, `assembleDebugAndroidTest`
