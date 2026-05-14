# Markleaf Tasks

이 파일은 랄프 루프에서 사용할 작업 목록입니다.  
에이전트는 매 루프마다 가장 위의 unchecked task 하나만 선택해 구현합니다.

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
- [ ] [#51] 다국어 지원 확대 (JP, FR, DE) — native 화자 부재로 v1.7.0에서 보류. ResourceParityTest 인프라 작동 중.
- [ ] [#53] 스토어 스크린샷 리디자인 — 코드 외 작업. Play Console 등록 시점에 맞춰 진행.
- [ ] [#54] 전문 브랜드 피처 그래픽 업데이트 — 코드 외 작업. Play Store 등록 시점에 맞춰 진행.

### 신규 기능 (미구현)
- [ ] [#37] 생체 인식(지문/안면) 앱 잠금 — BiometricPrompt 기반 잠금/해제 흐름
- [ ] [#38] 고품질 PDF 내보내기 — PDF 렌더링 파이프라인 및 폰트/레이아웃 유지
- [ ] [#39] 홈 화면 위젯 (최근 노트, 빠른 작성) — AppWidgetProvider로 최근 노트/빠른 작성 액션 제공
- [ ] [#52] 전략적 인앱 리뷰 요청 UI — 사용 패턴 기반 리뷰 요청 조건과 UI
- [ ] [#55] 오픈소스 투명성 강조 — 설정 화면에 GitHub 링크 및 라이선스 고지 강화
- [ ] [#57] 인터랙티브 온보딩 가이드 — 단계별 오버레이/튜토리얼 플로우

## Phase 22 - Commercial Readiness (Backlog, 2026-05-14)

목적: MVP 이후 기능 확장이 충분히 진행된 Markleaf를 Play 정식 출시와 장기 사용자 데이터 보관에 견딜 수 있는 상용 수준으로 마감한다. 새 기능보다 데이터 보호, 릴리즈 게이트, 개인정보 문서, 동기화 장애 가시성을 우선한다.

상세 설계 문서:
- `docs/COMMERCIAL_READINESS_PLAN.md`

P0 - 정식 출시 전 필수:
- [x] [Commercial P0-1] Android Backup / Data Extraction 정책 확정 — `android:allowBackup="false"`로 Android Auto Backup / D2D transfer 모두에서 Markleaf 데이터 제외. `dataExtractionRules`는 *전체 제외* 케이스에 과한 표면적이라 미도입(D046). Privacy/Security/NoCloud/README 모두 v2.x 기준으로 갱신.
- [ ] [Commercial P0-2] Release hardening — release R8/resource shrink 검토, release lint/build/bundle/signing/smoke gate 강화
- [ ] [Commercial P0-3] Room schema export + migration regression test — `exportSchema=true`, schema JSON 커밋, v4→v12 주요 migration 검증
- [ ] [Commercial P0-4] Privacy / Security 문서 현재화 — MVP draft 문구 제거, v2.x 이미지/SAF sync/share/external link 동작까지 반영

P1 - 상용 신뢰도 강화:
- [ ] [Commercial P1-1] 이미지 첨부 EXIF 제거 — 첨부 저장 시 위치/기기 메타데이터 제거 및 테스트 추가
- [ ] [Commercial P1-2] 생체 인식 앱 잠금 — BiometricPrompt 기반 잠금/해제 흐름과 설정 토글
- [ ] [Commercial P1-3] Sync Center / Conflict Center — Toast로 사라지는 동기화 결과를 지속 상태와 충돌 목록으로 노출

P2 - 전환율/경쟁력:
- [ ] [Commercial P2-1] 고품질 PDF 내보내기 — Markdown preview 기반 PDF 렌더링
- [ ] [Commercial P2-2] 첫 실행 온보딩 개선 — No account / local markdown / export & folder sync 철학을 짧게 전달
- [ ] [Commercial P2-3] Store packaging — 스크린샷, feature graphic, Play privacy copy, F-Droid metadata 정리

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
- [ ] 다국어 확대 (#51) — JP/FR/DE 검증할 native 화자 부재로 v1.7에선 미진행. ResourceParityTest 인프라는 작동 중이라 향후 추가 안전.
- [ ] 스토어 그래픽 / 스크린샷 (#53, #54) — 코드 외 작업이라 별도 사이클.

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
