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
