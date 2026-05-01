---
## 2026-05-01 - Issue #17 Markdown Link Preview and Settings Polish
Selected task:
- [#17] Improve Markdown link preview and settings navigation

What was implemented:
- Added inline parsing for `[[note links]]` inside normal body text
- Added inline parsing for `[label](target)` Markdown links
- Rendered inline links as clickable text in Preview mode
- Kept external web URLs non-opening to preserve MVP no-INTERNET behavior
- Rebuilt Settings with a top app bar back button and Data, Markdown, Privacy, and App sections
- Added backup/restore status messages

Files changed:
- app/src/main/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreview.kt
- app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt
- app/src/main/java/com/markleaf/notes/feature/settings/SettingsScreen.kt
- app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt
- app/src/test/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreviewTest.kt
- app/build.gradle.kts
- CHANGELOG.md
- HISTORY.md

Build/test result:
- `./gradlew.bat test` passed
- `./gradlew.bat lintDebug` passed
- `./gradlew.bat assembleDebug assembleRelease` passed
- Lenovo TB320FC Android 15 `v1.0.9` release APK install and launch check passed
- No `android.permission.INTERNET` declaration found in app source

---

## 2026-05-01 - Issue #15 Starter Notes Onboarding Implemented
Selected task:
- [#15] Add first-run starter notes onboarding

What was implemented:
- Added first-run starter notes seeding for empty installs
- Added four Korean starter notes covering Markdown, tags, wiki links, backup/export, and local-first privacy
- Added a local SharedPreferences guard so deleted starter notes are not recreated on every launch
- Added Bear-class product gap review and Phase 9 product polish roadmap
- Created GitHub Issue #16 for the remaining Phase 9 roadmap

Files changed:
- app/src/main/java/com/markleaf/notes/data/onboarding/StarterNotesSeeder.kt
- app/src/main/java/com/markleaf/notes/MainActivity.kt
- app/src/main/java/com/markleaf/notes/data/local/dao/NoteDao.kt
- app/src/test/java/com/markleaf/notes/data/onboarding/StarterNotesSeederTest.kt
- docs/BEAR_BENCHMARK_GAP.md
- docs/ROADMAP.md
- .agent/tasks.md
- app/build.gradle.kts
- CHANGELOG.md
- HISTORY.md

Build/test result:
- `./gradlew.bat test` passed
- `./gradlew.bat lintDebug` passed
- `./gradlew.bat assembleDebug assembleRelease` passed
- Lenovo TB320FC Android 15 release APK install and launch check passed
- `./gradlew.bat connectedDebugAndroidTest` not completed because the installed signed release APK rejected debug APK update due to signature mismatch

---

## 2026-05-01 - Issue #14 Stability and MVP Spec Hardening
Selected task:
- Stabilize implemented MVP behavior and fix spec gaps found in review

Issue:
- https://github.com/jeiel85/markleaf-android/issues/14

What was found:
- Editor saved only markdown content and updatedAt, leaving title/excerpt/tags stale
- Tag cross reference used `Long` note IDs while notes use `String` IDs
- Search, Tags, Trash, and Settings routes existed without top-level UI access
- Phone editor navigation built invalid route strings
- Database used destructive migration
- Android instrumentation runner was not configured, so device UI tests could not run
- Settings showed a hardcoded old version

What was implemented:
- Editor auto-save now updates title, excerpt, content, tags, and backlinks
- Tag cross-ref note IDs now use `String`
- Added Room migration from schema v4 to v5 and removed destructive migration
- Added top app bar actions for Search, Tags, Trash, and Settings
- Implemented basic Tags screen backed by stored tags
- Fixed editor route generation and query encoding
- Fixed androidTest runner/BOM dependencies and updated UI test selectors
- Added repository tests for tag reindexing and wiki-link backlinks
- Updated app version to `1.0.7` / `versionCode = 8`

Commands run:
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat connectedDebugAndroidTest`
- `rg "android.permission.INTERNET" -n app\src\main app\src\debug app\src\release`
- Lenovo TB320FC Android 15 release APK install/start logcat check

Build/test result:
- Unit tests passed
- Lint passed
- Debug and release APK builds passed
- 6 connected instrumentation tests passed on tablet
- `v1.0.7` release APK starts on tablet with no FATAL/ANR log
- No `android.permission.INTERNET` declaration found in app source

---
## 2026-05-01 - Issue #13 Startup Crash Fix
Selected task:
- Fix app exiting immediately on launch

Issue:
- https://github.com/jeiel85/markleaf-android/issues/13

What was found:
- `NotesViewModel`, `SearchViewModel`, and `TrashViewModel` require `NoteRepository`
- `MarkleafNavHost` used default `viewModel()` calls without a factory
- The default factory cannot construct these ViewModels, which can crash on the first Notes route
- `adb` was not available in this environment, so direct logcat verification was not possible

What was implemented:
- Added `MarkleafViewModelFactory`
- Wired `MainActivity` to create `AppDatabase`, `LocalNoteRepository`, and one ViewModel factory
- Passed the factory into `MarkleafNavHost`
- Updated Notes/Search/Trash routes to use the explicit factory
- Added factory regression tests for the three repository-backed ViewModels
- Updated app version to `1.0.6` / `versionCode = 7`

Files changed:
- app/src/main/java/com/markleaf/notes/MainActivity.kt
- app/src/main/java/com/markleaf/notes/navigation/MarkleafNavHost.kt
- app/src/main/java/com/markleaf/notes/ui/viewmodel/MarkleafViewModelFactory.kt
- app/src/test/java/com/markleaf/notes/ui/viewmodel/MarkleafViewModelFactoryTest.kt
- app/src/test/java/com/markleaf/notes/MainActivityLaunchTest.kt
- app/build.gradle.kts
- CHANGELOG.md
- HISTORY.md
- .agent/progress.md

Commands run:
- `gh issue create`
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`
- `rg "android.permission.INTERNET" -n app`

Build/test result:
- First test run exposed missing JVM Main dispatcher setup in the new test
- Latest `test` passed after adding test dispatcher setup
- `MainActivityLaunchTest` now verifies activity creation without crashing under Robolectric
- `assembleDebug` passed
- `assembleRelease` passed
- No `android.permission.INTERNET` declaration found in app source

---
---
## 2026-04-30 - Release Title Rule Fix
Selected task:
- Restore release title rule used by `v1.0.0`

What was found:
- `v1.0.0` used `v1.0.0 - 정식 출시 (First Major Release)`
- Current workflow used fixed titles like `Markleaf v1.0.4`

What was implemented:
- Updated release workflow to extract the GitHub Release title from the `CHANGELOG.md` version heading
- Normalized changelog headings to `## vX.Y.Z - 한국어 제목 (English Title) - YYYY-MM-DD`
- Updated app version to `1.0.5` / `versionCode = 6`
- Added release title source decision

Files changed:
- .github/workflows/android-build.yml
- app/build.gradle.kts
- CHANGELOG.md
- HISTORY.md
- .agent/progress.md
- .agent/decisions.md

Commands run:
- Updated existing `v1.0.2` GitHub Release notes to Korean
- Updated existing `v1.0.3` GitHub Release notes to Korean
- Updated existing `v1.0.4` GitHub Release notes to Korean

Build/test result:
- Existing release note bodies now use Korean `CHANGELOG.md` sections

---
## 2026-04-30 - Korean Release Notes Rule Fix
Selected task:
- Ensure GitHub Release notes are written in Korean

What was found:
- Release title format was documented, but release note body language was not explicit
- Existing post-1.0.0 release notes used English changelog text

What was implemented:
- Converted `v1.0.2` through `v1.0.5` changelog release notes to Korean
- Added Korean release note body rule to release decisions
- Updated existing `v1.0.2`, `v1.0.3`, and `v1.0.4` GitHub Release notes to Korean

Files changed:
- CHANGELOG.md
- HISTORY.md
- .agent/progress.md
- .agent/decisions.md

Commands run:
- Updated existing `v1.0.2`, `v1.0.3`, and `v1.0.4` GitHub Release titles
- Renamed existing `v1.0.2` release asset from `app-release.apk` to `markleaf-v1.0.2.apk`
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

Build/test result:
- Existing post-1.0.0 release titles now use `vX.Y.Z - 한국어 제목 (English Title)`
- Existing `v1.0.2` asset now uses `markleaf-v1.0.2.apk`
- `test` passed
- `assembleDebug` passed
- `assembleRelease` passed

---
## 2026-04-30 - Release Notes Rule Fix
Selected task:
- Fix release notes generation so GitHub Releases follow `CHANGELOG.md`

What was found:
- GitHub Releases used `--generate-notes`, producing only comparison links
- `v1.0.2` had no matching `CHANGELOG.md` entry
- Existing `v1.0.3` release body did not use the `CHANGELOG.md` section

What was implemented:
- Updated release workflow to extract `## vX.Y.Z` from `CHANGELOG.md`
- Replaced `--generate-notes` with `--notes-file release-notes.md`
- Updated app version to `1.0.4` / `versionCode = 5`
- Added `v1.0.4` changelog entry
- Backfilled `v1.0.2` changelog entry
- Added release notes source decision

Files changed:
- .github/workflows/android-build.yml
- app/build.gradle.kts
- CHANGELOG.md
- HISTORY.md
- .agent/progress.md
- .agent/decisions.md

Commands run:
- Updated existing `v1.0.2` GitHub Release notes from `CHANGELOG.md`
- Updated existing `v1.0.3` GitHub Release notes from `CHANGELOG.md`
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

Build/test result:
- Existing `v1.0.2` release body now contains the `CHANGELOG.md` v1.0.2 section
- Existing `v1.0.3` release body now contains the `CHANGELOG.md` v1.0.3 section
- `test` passed
- `assembleDebug` passed
- `assembleRelease` passed

---
## 2026-04-30 - Release Rule Violation Fix
Selected task:
- Fix release workflow so GitHub Releases contain only signed release APKs

What was found:
- `v1.0.2` release contained both `app-release.apk` and `app-debug.apk`
- `.github/workflows/release-apk.yml` uploaded the debug APK on tag pushes
- `gh release create` asset label did not rename the uploaded release APK file

What was implemented:
- Removed the duplicate debug APK release workflow
- Updated Android Build release job to copy the signed APK to `markleaf-${GITHUB_REF_NAME}.apk`
- Updated app version to `1.0.3` / `versionCode = 4`
- Updated changelog, history, and release asset decision

Files changed:
- .github/workflows/release-apk.yml
- .github/workflows/android-build.yml
- app/build.gradle.kts
- CHANGELOG.md
- HISTORY.md
- .agent/progress.md
- .agent/decisions.md

Commands run:
- `gh release delete-asset v1.0.2 app-debug.apk --repo jeiel85/markleaf-android --yes`
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

Build/test result:
- Removed incorrect `app-debug.apk` asset from `v1.0.2`
- `test` passed
- `assembleDebug` passed
- `assembleRelease` passed

---
## 2026-04-30 - Release Signing Automation
Selected task:
- Configure release keystore usage and GitHub Release automation

What was implemented:
- Added optional release signing configuration in Gradle using environment variables or local `release-signing.properties`
- Added GitHub Actions tag release workflow for `v*` tags
- Added release keystore secret restore step from `MARKLEAF_RELEASE_KEYSTORE_BASE64`
- Added release documentation
- Added signing secret files and keystore extensions to `.gitignore`

Files changed:
- app/build.gradle.kts
- .github/workflows/android-build.yml
- .gitignore
- docs/RELEASE.md
- .agent/progress.md
- .agent/decisions.md

Commands run:
- `keytool -genkeypair` for `.secrets/markleaf-release.p12`
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`
- `rg "android.permission.INTERNET" -n .`

Build/test result:
- `test` passed
- `assembleDebug` passed
- `assembleRelease` passed and produced `app/build/outputs/apk/release/app-release.apk`
- No `android.permission.INTERNET` declaration found in app source or manifest

---
## 2026-04-30 - Phase 3 Tags Tasks Complete
Selected task:
- Complete all Phase 3 tag-related tasks

What was implemented:
- Phase 3 tasks completed earlier: Tag domain model, TagEntity, NoteTagCrossRef, TagDao, tag parser, Korean tag tests, avoid heading/URL parsing, reindex tags
- Phase 4 all tasks completed earlier ✅
- Phase 5 most tasks completed: slug generation ✅, single note export ✅, settings screen ✅, app version ✅, verify no INTERNET ✅, review dependencies ✅, run test ✅, run assembleDebug ✅, polish typography ✅

Files changed:
- app/src/main/java/com/markleaf/notes/feature/tags/TagsScreen.kt (updated)
- .agent/tasks.md (Phase 3 tasks marked complete)
- .agent/progress.md (updated)

Commands run:
- ./gradlew test ✅
- ./gradlew assembleDebug ✅

Build/test result:
- BUILD SUCCESSFUL in 8s
- 52 actionable tasks: 15 executed, 37 up-to-date
- All tests passed ✅

INTERNET permission check result:
- No android.permission.INTERNET found ✅

---

## 2026-04-30 - Phase 5 Export and Share Tasks Complete
Selected task:
- Implement export all notes with Android Storage Access Framework
- Implement share note action

What was implemented:
- ExportAllNotes.kt: Implemented using DocumentFile API with Storage Access Framework
- ShareNoteUtil.kt: Implemented using FileProvider and Android share intent
- Added androidx.documentfile dependency to app/build.gradle.kts
- Fixed TagsScreen.kt missing Arrangement import
- All tests passed ✅

Files changed:
- app/src/main/java/com/markleaf/notes/util/ExportAllNotes.kt (fully implemented)
- app/src/main/java/com/markleaf/notes/util/ShareNoteUtil.kt (fully implemented)
- app/src/main/java/com/markleaf/notes/feature/tags/TagsScreen.kt (fixed import)
- app/build.gradle.kts (added documentfile dependency)
- .agent/tasks.md (Phase 1, 3, 5 tasks marked complete)
- .agent/progress.md (updated)

Commands run:
- ./gradlew test ✅
- ./gradlew assembleDebug ✅

Build/test result:
- BUILD SUCCESSFUL in 32s (assembleDebug)
- BUILD SUCCESSFUL in 14s (test)
- 52 actionable tasks: 15 executed, 37 up-to-date
- All tests passed ✅

INTERNET permission check result:
- No android.permission.INTERNET found ✅

Remaining next task:
- [x] Add GitHub Actions Android build workflow (Phase 1) ✅
- [x] Run initial Gradle build (Phase 1) ✅
- [x] Add export all notes with Android Storage Access Framework (Phase 5) ✅
- [x] Add share note action (Phase 5) ✅
- [ ] Later Versions tasks (Evaluate Markdown preview, SQLite FTS, image attachments, note links, tablet layout, backup strategy, network feature)

Risks or blockers:
- All Phase 1-5 MVP tasks completed successfully ✅
- Ready to move to Later Versions tasks or commit and push

---

## 2026-04-30 - Template Guidelines Integration
Selected task:
- Integrate reusable markdown guideline templates from `.templates` into this project documentation

What was implemented:
- Added root `CHANGELOG.md`
- Added root `HISTORY.md`
- Updated `README.md` document index to include new docs
- Updated `AGENTS.md` with Documentation/History 운영 섹션

Files changed:
- AGENTS.md
- README.md
- CHANGELOG.md
- HISTORY.md

Commands run:
- git pull --rebase --autostash
- git status

Build/test result:
- Not run (documentation-only changes)

---

## 2026-04-30 - CI APK Artifact Verification Added
Selected task:
- Add APK artifact verification and download-check guidance to build process

What was implemented:
- Updated GitHub Actions workflow to verify debug APK existence
- Added APK artifact upload step (`markleaf-debug-apk`)
- Updated `AGENTS.md` quality checks with APK verification requirements

Files changed:
- .github/workflows/android-build.yml
- AGENTS.md

Commands run:
- gh run watch (previous run)

Build/test result:
- Pending on next CI run after push

---

## 2026-04-30 - Node 20 Deprecation Warning Mitigation
Selected task:
- Resolve GitHub Actions Node 20 deprecation warnings

What was implemented:
- Updated workflow action majors:
  - actions/checkout: v4 -> v5
  - actions/setup-java: v4 -> v5
  - gradle/actions/setup-gradle: v3 -> v6
  - actions/upload-artifact: v4 -> v7
- Added `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true` at job level

Files changed:
- .github/workflows/android-build.yml

Build/test result:
- Pending CI verification after push

---

## 2026-04-30 - Issue #5 Tablet Two-Pane Evaluation Complete
Selected task:
- [#5] Evaluate tablet two-pane layout (P0)

What was implemented:
- Added evaluation document: `docs/TABLET_TWO_PANE_EVALUATION.md`
- Defined breakpoint policy (Compact vs Medium/Expanded)
- Proposed phased rollout plan and success criteria
- Marked issue #5 task as complete in `.agent/tasks.md`

Files changed:
- docs/TABLET_TWO_PANE_EVALUATION.md
- .agent/tasks.md

Build/test result:
- Not run (documentation/planning change)

---

## 2026-04-30 - Issue #1 Markdown Preview Implemented
Selected task:
- [#1] Evaluate Markdown preview renderer

What was implemented:
- Added basic local markdown preview parser (`SimpleMarkdownPreview`)
- Added editor preview mode toggle (Edit/Preview)
- Added styled rendering for headings, bullet lists, and checkboxes
- Added parser unit tests
- Marked `#1` task as complete in `.agent/tasks.md`

Files changed:
- app/src/main/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreview.kt
- app/src/main/java/com/markleaf/notes/feature/editor/EditorScreen.kt
- app/src/test/java/com/markleaf/notes/core/markdown/SimpleMarkdownPreviewTest.kt
- .agent/tasks.md

Commands run:
- ./gradlew test
- ./gradlew assembleDebug

Build/test result:
- Local failed due missing Android SDK path (`sdk.dir` / `ANDROID_HOME` not configured in this environment)
- CI verification required

---

## 2026-04-30 - Issue #2 SQLite FTS Evaluation Complete
Selected task:
- [#2] Evaluate SQLite FTS

What was implemented:
- Added `docs/SQLITE_FTS_EVALUATION.md`
- Defined phased adoption plan (additive migration + dual query path)
- Marked #2 complete in `.agent/tasks.md`

Build/test result:
- Not run (evaluation/documentation task)

---

## 2026-04-30 - Issue #3 Image Attachments Evaluation Complete
Selected task:
- [#3] Evaluate image attachments

What was implemented:
- Added `docs/IMAGE_ATTACHMENTS_EVALUATION.md`
- Defined local-first constrained attachment scope
- Marked #3 complete in `.agent/tasks.md`

Build/test result:
- Not run (evaluation/documentation task)

---

## 2026-04-30 - Issue #4 Note Links Evaluation Complete
Selected task:
- [#4] Evaluate `[[note links]]`

What was implemented:
- Added `docs/NOTE_LINKS_EVALUATION.md`
- Chose ID-backed link resolution with phased UX rollout
- Marked #4 complete in `.agent/tasks.md`

Build/test result:
- Not run (evaluation/documentation task)

---

## 2026-04-30 - Issue #6 Backup Strategy Evaluation Complete
Selected task:
- [#6] Evaluate optional backup strategy

What was implemented:
- Added `docs/BACKUP_STRATEGY_EVALUATION.md`
- Defined manual local backup + restore preview model
- Marked #6 complete in `.agent/tasks.md`

Build/test result:
- Not run (evaluation/documentation task)

---

## 2026-04-30 - Issue #7 Network Necessity Evaluation Complete
Selected task:
- [#7] Evaluate whether any network feature is necessary

What was implemented:
- Added `docs/NETWORK_FEATURE_NECESSITY_EVALUATION.md`
- Concluded network features are not necessary at current stage
- Added future adoption guardrails
- Marked #7 complete in `.agent/tasks.md`

Build/test result:
- Not run (evaluation/documentation task)

---

## 2026-04-30 - Release APK Asset Automation Added
Selected task:
- Ensure APK is attached to GitHub release

What was implemented:
- Uploaded APK asset to existing `v0.1.0` release
- Added `.github/workflows/release-apk.yml` for tag-based APK release upload automation

Files changed:
- .github/workflows/release-apk.yml

Build/test result:
- Not run locally (workflow/config change)
