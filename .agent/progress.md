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
- (pending) feat: add placeholder editor screen

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
