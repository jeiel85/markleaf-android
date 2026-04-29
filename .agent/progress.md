# Markleaf Progress

이 파일은 랄프 루프의 진행 기록입니다.  
각 루프가 끝날 때마다 에이전트는 이 파일에 기록을 추가합니다.

---

## Initial State - 2026-04-29

- Repository: `https://github.com/jeiel85/markleaf-android.git`
- Application ID: `com.markleaf.notes`
- Source of truth: `docs/AGENT_SPEC.md`
- MVP policy:
  - No `android.permission.INTERNET`
  - No API integration
  - No login/account
  - No analytics
  - No ads
  - No tracking
  - No Firebase
  - No proprietary SDK
  - F-Droid-friendly direction

## 2026-04-29 - Loop 3

Selected task:
- Add empty Notes List screen

Files changed:
- app/src/main/java/com/markleaf/notes/feature/notes/NotesListScreen.kt (new)
- app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt (updated to use real NotesListScreen)
- app/src/main/res/drawable/ic_add.xml (new)

Commands run:
- ./gradlew assembleDebug

Build/test result:
- BUILD SUCCESSFUL in 34s
- 34 actionable tasks: 7 executed, 27 up-to-date
- No android.permission.INTERNET found

Commit:
- 3a182fd: feat: add empty notes list screen

Notes:
- Created NotesListScreen with empty state display
- Added Scaffold with TopAppBar showing "Notes"
- Added FAB with add icon for creating new notes
- Empty state shows "No notes yet" and "Tap + to create your first note"
- Updated navigation to use real NotesListScreen instead of placeholder

Next task:
- [ ] Add placeholder Editor screen
- [ ] Add placeholder Tags screen
- [ ] Add placeholder Search screen
- [ ] Add placeholder Trash screen
- [ ] Add placeholder Settings screen

Risks or blockers:
- None encountered in this loop

---

## 2026-04-29 - Loop 4

Selected task:
- Add placeholder Editor screen

Files changed:
- app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt (new)
- app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt (updated to add Editor route)

Commands run:
- ./gradlew assembleDebug

Build/test result:
- BUILD SUCCESSFUL in 31s
- 34 actionable tasks: 7 executed, 27 up-to-date
- No android.permission.INTERNET found
- Warning: Parameter 'onBack' is never used (expected for placeholder)

Commit:
- 661d31f: feat: add placeholder editor screen

Notes:
- Created EditorScreen.kt in feature/editor package with Scaffold and BasicTextField
- Added Editor route to MarkleafNavHost with noteId parameter support
- Editor is a placeholder with minimal UI (TopAppBar + BasicTextField)
- Navigation callbacks left as TODO for future implementation

Next task:
- [ ] Add placeholder Tags screen
- [ ] Add placeholder Search screen
- [ ] Add placeholder Trash screen
- [ ] Add placeholder Settings screen

Risks or blockers:
- None encountered in this loop

---

## 2026-04-29 - Loop 5

Selected task:
- Add placeholder Tags, Search, Trash, Settings screens (Phase 1 Tasks 33-36)

Files changed:
- app/src/main/java/com/markleaf/notes/feature/tags/TagsScreen.kt (new)
- app/src/main/java/com/markleaf/notes/feature/search/SearchScreen.kt (new)
- app/src/main/java/com/markleaf/notes/feature/trash/TrashScreen.kt (new)
- app/src/main/java/com/markleaf/notes/feature/settings/SettingsScreen.kt (new)
- app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt (updated to use all real screens)
- app/src/main/java/com/markleaf/notes/navigation/Placeholders.kt (deleted)
- app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt (renamed function from EditorScreenPlaceholder to EditorScreen)

Commands run:
- ./gradlew assembleDebug

Build/test result:
- BUILD SUCCESSFUL in 1m 6s
- 34 actionable tasks: 9 executed, 25 up-to-date
- No android.permission.INTERNET found

Commit:
- (pending) feat: add placeholder screens for tags, search, trash, settings

Notes:
- Created TagsScreen, SearchScreen, TrashScreen, SettingsScreen in respective feature packages
- All screens follow same pattern: Scaffold with TopAppBar + centered placeholder text
- Removed unnecessary Placeholders.kt file from navigation package
- Fixed EditorScreen function name mismatch
- All navigation routes now point to real composables instead of placeholders

Phase 1 tasks completed:
- [x] Add placeholder Editor screen (Task 32)
- [x] Add placeholder Tags screen (Task 33)
- [x] Add placeholder Search screen (Task 34)
- [x] Add placeholder Trash screen (Task 35)
- [x] Add placeholder Settings screen (Task 36)

Next phase:
- Phase 2: Implement Notes List screen with real data
- Create Note data model
- Set up Room database
- Implement note list with real notes

Risks or blockers:
- None encountered in this loop

---

## 2026-04-29 - Phase 3 TagParser

Selected task:
- Add tag parser utility with Korean support

Files changed:
- app/src/main/java/com/markleaf/notes/util/TagParser.kt (new)
- app/src/test/java/com/markleaf/notes/util/TagParserTest.kt (new)

Commands run:
- ./gradlew test

Build/test result:
- BUILD SUCCESSFUL in 33s
- 52 actionable tasks: 7 executed, 45 up-to-date
- All 18 tests passed (including 10 TagParser tests)

Commit:
- (pending) feat: add tag parser with Korean support and tests

Notes:
- Created TagParser utility that extracts #tags from note content
- Handles Korean tags correctly
- Avoids parsing Markdown headings (e.g., # Heading) as tags
- Avoids parsing URL fragments (e.g., https://example.com#fragment) as tags
- Excludes empty tag names
- Includes normalizeTagName function for tag normalization
- Created comprehensive test suite with 10 test cases

Next task:
- [ ] Reindex tags on note save

Risks or blockers:
- None encountered in this loop

---

## 2026-04-29 - Reindex tags on note save

Selected task:
- Reindex tags on note save (Phase 3)

Files changed:
- app/src/main/java/com/markleaf/notes/domain/model/Tag.kt (new)
- app/src/main/java/com/markleaf/notes/domain/repository/TagRepository.kt (new)
- app/src/main/java/com/markleaf/notes/data/repository/LocalTagRepository.kt (new)

Commands run:
- ./gradlew assembleDebug

Build/test result:
- BUILD SUCCESSFUL in 38s
- 36 actionable tasks: 6 executed, 30 up-to-date
- No android.permission.INTERNET found

Commit:
- (pending) feat: add tag domain model and reindex tags on note save

Notes:
- Created Tag domain model (id: Long, name, normalizedName, createdAt)
- Created TagRepository interface with reindexTagsForNote, getTagsForNote, observeTagsForNote, getAllTags, observeAllTags, getTagByName
- Implemented LocalTagRepository with Room DAO integration
- ReindexTagsForNote parses tags from note content, clears old tags, creates new tags, and creates cross-references

Next task:
- [ ] Add tag list screen

Risks or blockers:
- None encountered in this loop
