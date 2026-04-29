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

## 2026-04-29 - Loop 1

Selected task:
- Initialize Android project with Kotlin and Jetpack Compose

Files changed:
- build.gradle.kts (project-level)
- settings.gradle.kts
- gradle.properties
- local.properties
- gradlew
- gradlew.bat
- gradle/wrapper/gradle-wrapper.properties
- gradle/wrapper/gradle-wrapper.jar
- app/build.gradle.kts
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/markleaf/notes/MainActivity.kt
- app/src/main/java/com/markleaf/notes/ui/theme/Theme.kt
- app/src/main/res/values/strings.xml
- app/src/main/res/values/styles.xml
- .gitignore

Commands run:
- ./gradlew assembleDebug

Build/test result:
- BUILD SUCCESSFUL in 50s
- 34 actionable tasks executed
- No android.permission.INTERNET found

Commit:
- cbc4146: chore: initialize android project with kotlin and compose

Notes:
- Used Kotlin 1.9.22, Compose BOM 2024.02.02, Compose compiler 1.5.8
- Min SDK 26, Target SDK 34
- Used Material 3 theme with custom color scheme
- Basic package structure created: com.markleaf.notes with app, ui.theme subpackages
- Removed AppCompat dependency since using Compose-only approach

Next task:
- Set `applicationId` to `com.markleaf.notes` (already done during initialization)
- Configure Gradle Kotlin DSL (already done)
- Add Material 3 theme structure (already done)
- Add basic package structure (already done)
- Next: Add app navigation skeleton

Risks or blockers:
- None encountered in this loop
