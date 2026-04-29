# Markleaf Tasks

이 파일은 랄프 루프에서 사용할 작업 목록입니다.  
에이전트는 매 루프마다 가장 위의 unchecked task 하나만 선택해 구현합니다.

---

## Phase 0 - Repository Preparation

- [x] Add `docs/AGENT_SPEC.md`
- [x] Add `AGENTS.md`
- [x] Add `.agent/tasks.md`
- [x] Add `.agent/progress.md`
- [x] Add `.agent/decisions.md`
- [x] Add `.agent/RALPH_PROMPT.md`
- [x] Add root `README.md`
- [x] Add `docs/ARCHITECTURE.md`
- [x] Add `docs/ROADMAP.md`
- [x] Add `docs/BRANDING.md`

---

## Phase 1 - Project Foundation

- [x] Initialize Android project with Kotlin and Jetpack Compose
- [x] Set `applicationId` to `com.markleaf.notes`
- [x] Configure Gradle Kotlin DSL
- [x] Add Material 3 theme structure
- [x] Add basic package structure
- [x] Add app navigation skeleton
- [x] Add empty Notes List screen
- [x] Add placeholder Editor screen
- [x] Add placeholder Tags screen
- [x] Add placeholder Search screen
- [x] Add placeholder Trash screen
- [x] Add placeholder Settings screen
- [ ] Add GitHub Actions Android build workflow
- [ ] Verify no `android.permission.INTERNET` exists
- [ ] Run initial Gradle build

---

## Phase 2 - Local Notes

- [x] Add Room dependencies
- [x] Add Note domain model
- [x] Add NoteEntity
- [x] Add NoteDao
- [x] Add AppDatabase
- [x] Add entity-domain mapper
- [x] Add NoteRepository interface
- [x] Add LocalNoteRepository implementation
- [x] Add observe notes flow
- [x] Add create note flow
- [x] Add edit note flow
- [x] Add auto-save with debounce
- [x] Add title extraction utility
- [x] Add excerpt generation utility
- [ ] Add tests for title extraction
- [ ] Verify notes persist after app restart

---

## Phase 3 - Tags

- [ ] Add Tag domain model
- [ ] Add TagEntity
- [ ] Add NoteTagCrossRef
- [ ] Add TagDao
- [ ] Add tag parser utility
- [ ] Add Korean tag parser tests
- [ ] Avoid parsing Markdown headings as tags
- [ ] Avoid parsing URL fragments as tags
- [ ] Reindex tags on note save
- [ ] Add tag list screen
- [ ] Add tag count display
- [ ] Add filter notes by selected tag

---

## Phase 4 - Search and Trash

- [ ] Add search UI
- [ ] Add debounced search state
- [ ] Add Room LIKE search for title, excerpt, and Markdown content
- [ ] Add search result screen
- [ ] Add move to trash behavior
- [ ] Add trash screen data source
- [ ] Add restore from trash
- [ ] Add delete forever confirmation
- [ ] Add empty states
- [ ] Add tests for trash repository behavior

---

## Phase 5 - Export and Polish

- [ ] Add slug generation utility
- [ ] Add tests for slug generation
- [ ] Add single note Markdown export
- [ ] Add export all notes with Android Storage Access Framework
- [ ] Add share note action
- [ ] Add settings screen content
- [ ] Add app version display
- [ ] Polish typography and spacing
- [ ] Verify no `android.permission.INTERNET` exists
- [ ] Review dependencies for F-Droid friendliness
- [ ] Run final `./gradlew test`
- [ ] Run final `./gradlew assembleDebug`

---

## Later Versions

- [ ] Evaluate Markdown preview renderer
- [ ] Evaluate SQLite FTS
- [ ] Evaluate image attachments
- [ ] Evaluate `[[note links]]`
- [ ] Evaluate tablet two-pane layout
- [ ] Evaluate optional backup strategy
- [ ] Evaluate whether any network feature is necessary
