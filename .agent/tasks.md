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

## Phase 10 - Device UI Verification
- [x] Refresh AndroidX test runtime and isolate Compose UI tests with an in-memory app harness
- [ ] Resolve Lenovo TB320FC Compose test host lifecycle issue where the host Activity is backgrounded before Compose hierarchy registration

---

## Phase 8 - Post v1.0.0 & Future (Backlog)
- [x] Implement fixed Signing Keystore (Prevent Update Conflict)
- [ ] Implement WebDAV sync (Optional)
- [ ] Implement Google Drive backup (Optional)
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

## Phase 15 - Markdown Expressiveness (Planned, target v1.6.0)

목적: 손파서가 ~150줄로 여유 있을 때 가성비 높은 문법을 추가. *Bear/Obsidian 호환성* 도 부수적으로 ↑.

- [ ] 콜아웃 `> [!NOTE]` / `[!WARN]` / `[!TIP]` (#3 of 50) — `SimpleMarkdownPreview.parseLine`에서 `BLOCKQUOTE` 분기에 첫 줄 prefix 매칭 추가. 새 `PreviewLineType.CALLOUT_*` 또는 `extra` 필드에 종류 저장. 미리보기에서 색상 박스로 렌더링.
- [ ] 프론트매터 YAML 인식 (#10 of 50) — `parse()` 시작 부분에서 본문이 `---\n`으로 시작하면 다음 `---`까지를 `PreviewLineType.FRONTMATTER`로 묶고 미리보기에서는 축소 표시 또는 별도 메타 영역으로. `MarkdownSyntaxHighlighter`도 같은 영역을 덤덤하게 처리.
- [ ] 각주 `[^1]` (#1 of 50) — inline 세그먼트 `PreviewInlineType.FOOTNOTE_REF` 추가, 미리보기 하단에 `[^1]: ...` 정의 모아 별도 섹션. 본문 ref ↔ 정의 사이 클릭 점프는 v1.7+로 미룸.
- [ ] Tab/Shift+Tab 들여쓰기 — 에디터 `BasicTextField`에 `Modifier.onPreviewKeyEvent { ... }` 또는 입력 핸들러로 Tab 키를 가로채 현재 줄 시작에 `  ` (2 spaces) 추가/제거. 선택 영역이 여러 줄이면 모두 적용.

손파서가 이 4개로 ~250줄 근처가 될 텐데, 그래도 한계는 ~300줄 (`feedback_lightweight_bias` 메모 참고). 그 이상 나가면 `org.jetbrains:markdown` 라이브러리 도입 고려.

---

## Phase 16 - Spec Closure (Planned, target v1.7.0)

목적: AGENT_SPEC §7 *반드시 포함* + *있으면 좋은* 항목 중 마지막 미완 항목들을 닫음.

- [ ] 아카이브 UI — `Note.archived` 필드는 v1.0부터 존재. 길게 누르기 메뉴에 "아카이브"/"아카이브 해제" 추가, `archived = true`인 노트는 메인 목록에서 제외. 별도 *아카이브* 화면 (Tags / Trash 같은 레벨의 목적지)에서 조회 + 복구.
- [ ] 접근성 정비 (#76) — TalkBack 시나리오 1회 실행, contentDescription 누락 노드 보완, 모든 클릭 가능 요소 48dp 이상 검증, 색상 대비 WCAG AA 검증 (특히 surfaceVariant 텍스트). 큰 코드 변화 없이 라벨/사이즈 위주.
- [ ] 다국어 확대 (#51, 검증 가능한 언어만) — `ResourceParityTest`가 누락 막아주므로 안전. JP/FR/DE 중 본인이 검증 가능한 것만.
- [ ] 스토어 그래픽 / 스크린샷 (#53, #54) — 코드 외 작업. v1.5/v1.6 결과를 새 스크린샷으로 갱신.

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
