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

## 2026-04-29 - Loop 2

Selected task:
- Add app navigation skeleton

Files changed:
- app/build.gradle.kts (added Navigation Compose dependency)
- app/src/main/java/com/markleaf/notes/MainActivity.kt (updated to use NavHost)
- app/src/main/java/com/markleaf/notes/navigation/NavRoutes.kt (new)
- app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt (new)
- app/src/main/java/com/markleaf/notes/navigation/Placeholders.kt (new)

Commands run:
- ./gradlew assembleDebug

Build/test result:
- BUILD SUCCESSFUL in 33s
- 34 actionable tasks: 7 executed, 27 up-to-date
- No android.permission.INTERNET found

Commit:
- bf76448: chore: add compose navigation skeleton

Notes:
- Added Navigation Compose 2.7.0 dependency
- Created navigation routes for Notes, Editor, Tags, Search, Trash, Settings
- Set up NavHost with placeholder composables for each screen
- MainActivity now uses NavController and MarkleafNavHost
- Placeholder screens just display their name centered on screen

Next task:
- [ ] Add empty Notes List screen
- [ ] Add placeholder Editor screen
- [ ] Add placeholder Tags screen
- [ ] Add placeholder Search screen
- [ ] Add placeholder Trash screen
- [ ] Add placeholder Settings screen

Risks or blockers:
- None encountered in this loop
