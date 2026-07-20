# CHANGELOG

All notable changes to Markleaf are documented in this file. This English edition is the source for GitHub release notes; the Korean edition is kept at [CHANGELOG.ko.md](CHANGELOG.ko.md).

## v2.26.2 - Terminology consistency - 2026-07-20

A patch release that settles wording drift across the five translated languages, and adds release checks that run before a version can ship. No feature, permission, or storage changes.

### Changed
- **Consistent terminology in Korean, French, German, Japanese, and Spanish.** Twenty-four strings named one concept two different ways inside a single screen, or broke a convention the rest of the file kept. Korean called the Conflict Center an "archive" — the word already used for the Archive screen — and headed the Appearance section with the same word as the Theme row inside it. German used a word for "unpin" that shares no stem with "pin" and reads as "solve". Japanese mixed half- and full-width question marks and named analytics two ways. French left a determiner disagreeing with the nouns after it. Spanish called headings "titles", which is what it calls note titles elsewhere.

### Added
- **Release checks that run before the tag, not after.** Two scripts now run in CI on every push and pull request. The first compares the version shown on the six landing pages and six READMEs against the app's own `versionName`; the previous check only compared the languages to each other, so a release where all six were equally out of date passed — which is how two consecutive releases shipped with stale version strings. The second asserts that all six store changelogs exist for the current build and fit the 500-character store limit, and that the English and Korean changelogs carry the same versions in the same order with the same dates.

### Documentation
- **What the Locked space protects, and what it does not.** The security notes now separate the two locks the app has, and state plainly that locked note bodies are not encrypted and that the passcode retry delay is cleared by moving the device clock forward. Both were already true; neither was written down. The reasoning for leaving the delay as it is — every alternative trades one bypass for another, and the delay was never the boundary — is recorded as a decision.

### Internal
- **One helper for reading the sync folder.** Nine call sites each re-implemented the same "is sync configured, and does the stored value still parse" pair of checks, in three different shapes. Consolidating them made visible the one call site that carries an extra condition: the editor also refuses to mirror a locked note.

## v2.26.1 - Localization accuracy - 2026-07-19

A patch release that repairs localization defects across the six shipped languages. Some strings still held their English source text, Spanish had lost its accents, and a few translations had drifted from what the app actually does — including one privacy claim. No feature, permission, or storage changes.

### Fixed
- **Untranslated strings on shipped locales.** `sync_title`, `sync_recommended_locations`, and `application_id_format` still rendered in English on French, Japanese, and Korean devices. Android lint cannot catch this — `MissingTranslation` only fires when a string is *absent*, not when a present one was never translated.
- **Spanish accents.** Around sixty Spanish strings had lost their diacritics (`Mas`, `aqui`, `descripcion`, `Aun`), which read as typos to native speakers.
- **"Remove from lock" mistranslation (ja, ko).** The action read as "unlock", suggesting it cleared the passcode, when it actually moves a note back out of the locked space.
- **Korean object particle.** The trash confirmation built its prompt directly from the note title, producing the wrong particle whenever the title ended in a consonant. It now names the object explicitly so the particle is always correct.
- **Quotes stripped at build time.** Unescaped quotes in the sync explainer were removed by the resource compiler, so "Sync now" lost its quotation marks on every locale.
- **Archive: destination vs action.** One string served both the Archive screen title and the per-note archive action. Most locales need a distinct noun and verb, and the archive empty-state hint names the menu entry to look for — so the hint could describe an entry that did not exist under that name.
- **Three claims restored (de, fr, ko).** German `expanded`/`collapsed` used words meaning "extended"/"reduced"; the French privacy line claimed no data *download* where the English claims none is *sent*; the Korean editor hint had dropped the fact that autosave writes to the device.

### Added
- **Build-time guard for untranslated strings.** A test compares every locale value with the English source and fails the build when they match, with an allowlist for values that are legitimately identical (brand names, URLs, format-only strings). A second test fails once an allowlist entry stops being needed, so the list cannot decay into a blanket exemption.

### Documentation
- **English-first public docs.** `CHANGELOG.md` is now written in English and is the file the release job extracts GitHub release notes from; the Korean edition lives in `CHANGELOG.ko.md`. Releases from v2.16.0 onward were retranslated.
- **Six-language README and landing pages.** Spanish and French joined the existing four, matching the locales the app ships, with a unified language switcher and hreflang metadata.

## v2.26.0 - Locked notes hardening - 2026-07-18

A hardening release that closes the remaining gaps in the locked notes space added in v2.25.0. The lock is still a UI gate that hides notes from view and is not encryption at rest. The storage format and the local-first, no-INTERNET principle are unchanged.

### Fixed
- **Sync folder copies removed on lock (#156).** A note already exported to a folder before being locked stayed there as a plain text file. Moving a note into the lock now deletes the matching file in the sync folder, and unlocking exports it again.
- **Locked notes hidden from backlinks (#156).** When a locked note pointed at an ordinary note with a `[[link]]`, the locked note's title was still visible in that ordinary note's note info sheet.
- **Locked notes excluded from tag counts (#156).** A tag used only by locked notes stayed in the tag list with a count but showed an empty list when tapped. Such tags no longer appear.
- **Wikilinks pointing at locked notes (#156).** When `[[Title]]` pointed at a locked note, a new empty note with the same title was created instead. It now reports that the note is in Locked notes, without opening it without the passcode and without creating a duplicate.

### Security
- **Backoff after repeated passcode failures (#156).** Repeatedly entering the wrong passcode now introduces a wait. The first four attempts retry immediately; after that the wait starts at 30 seconds and doubles with each failure, up to five minutes. The backoff state is stored on the device, so force quitting the app does not reset it.
- **Screen capture blocked on locked notes (#156).** The locked notes screens always block screenshots and recent-apps previews, regardless of the global "block screen capture" setting. Leaving those screens restores the configured value.

### Changed
- **Unified editor shortcut handling (#150).** External keyboard shortcuts (`Ctrl/Cmd+B`, `I`, `K`, and `Shift+S`) are resolved from one shared table by both the editing surface and the style panel. Shortcuts pressed in the editing surface now give the same haptics and take the same save path as the panel.

### Internal
- Compose UI and Roborazzi tests moved to the debug-only `src/testDebug` source set so they drop out of the release and benchmark variants automatically. The per-class exclusion list in `app/build.gradle.kts` is gone, and a dedicated guard test fails immediately if a new Compose test is placed in `src/test` (#152).
- The GitHub↔GitLab mirror recovery procedure for diverged history on protected branches is documented in `docs/mirror-runbook.md` (#154).
- Cleaned up indentation that drifted while centering the tablet settings screen (#154).

## v2.25.0 - Locked notes - 2026-07-18

A feature release that adds a "Locked notes" space for keeping selected notes behind an app passcode. The lock is a UI gate that hides notes from view; note bodies stay in the same on-device local database as before (this is not encryption at rest). The local-first, no-INTERNET principle and the storage format are unchanged.

### Added
- **Locked notes + app passcode (#155).** "Move to lock" in the note menu hides a note in a separate locked space, reachable only through "Locked notes" in the overflow menu with the app passcode. Locked notes are excluded from the note list, search, tags, archive, the home screen widget, and folder sync export. The passcode is set in Settings and stored on the device only as a salt + PBKDF2 hash — never in plain text. Removing the passcode returns locked notes to the normal note list. This lock hides notes from view and is not encryption at rest — available in six languages.

### Fixed
- **Type immediately in an empty note.** Opening a newly saved empty note now focuses the editor right away, so you can start typing at once on both tablets and phones.

### Changed
- **Editor empty state icon.** Replaced the emoji, whose shape varied by device and font, with a Material vector icon consistent with the app's other empty states.

### Accessibility
- **Note info sheet action hints (#152).** Outline entries and backlink rows are announced as buttons with "jump to section" and "open note" action labels, making tap behavior clear in TalkBack.

## v2.24.0 - Note info sheet and polish - 2026-07-16

### Changed
- **Cleaner note list excerpts.** The first line, already used as the title, is no longer repeated in the preview, making the list, search, archive, and trash tidier.
- **Grammatically correct counts (plurals).** Per-tag note counts and the result text for find, export, and sync now use correct singular and plural forms (six languages).
- **Consistent empty states.** Empty screens for tags, search, archive, and trash now share one component, each with its own icon.
- **Tablet margins.** On wide screens, tag and settings content is centered at 640dp instead of stretching across the full width.
- **Quiet top app bar and note info sheet.** The editor's top bar is now Back · title · preview/edit · note info · overflow, with find, focus mode, share, export, and trash collected into the overflow menu. Character statistics, the outline, and backlinks — previously scattered — are unified into a single note info sheet, and the always-on statistics text was removed from the formatting area to give the body more room. Tapping an outline entry switches to preview and scrolls to that position. While editing, outline computation is deferred to when the preview or info sheet needs it, so Markdown is no longer parsed on every keystroke.
- **GitHub Pages landing redesign.** The public page was rebuilt around actual Markleaf editor, preview, and tag screens, showing the write → organize → own your files flow and the no-INTERNET privacy model at a glance. F-Droid is now clearly the recommended install path, with the latest GitHub APK, the paused Google Play update status, and the source code each stated accurately.
- **Public page accessibility and privacy.** Added 375/768/1280px responsive layouts, a skip to content link, strong keyboard focus, 48px actions, reduced motion support, and social/search metadata. Google Fonts requests were removed from the landing page, so every font and visual asset renders from local or same-origin sources.

## v2.23.0 - Quiet formatting - 2026-07-15

A feature release that restructures the formatting UI so it covers less of the writing surface while keeping every existing Markdown tool. Selection context, touch, external keyboards, and accessibility focus were refined together; the storage format and local-first principle are unchanged.

### Changed
- **Quiet `Aa` entry point.** The always-visible horizontal scrolling format toolbar was replaced with a small `Aa` button and compact character and word statistics, giving more space back to the body.
- **Selection context actions.** Selecting text brings bold, italic, link, and more actions close at hand, while Android's cut, copy, and paste menu is preserved.
- **Full style panel.** The expanded panel offers all 13 existing Markdown actions, using the editor width on phones and a calm surface of up to 360dp on tablets.
- **Public GitLab release mirror.** Provides independently built, production-signed APK and AAB files with permanent package-based download links, separate from GitHub.

### Accessibility
- Added 48dp touch targets, localized labels and expansion states, keyboard first focus and cyclic navigation, visible focus indicators, and Back/Escape/canvas-tap dismissal.

### Compatibility
- Preserved all 13 formatting commands, `Ctrl/Cmd+B`, `I`, `K`, `Shift+S`, Tab indentation, `/` quick insert, tag and wikilink autocomplete, autosave, and the SAF image picker flow.
- No new permissions, network access, accounts, analytics, ads, database changes, or external dependencies.

### Tests
- Tested format actions, disabled states, focus cycling, Escape, shortcuts, context action dismissal, and real editor integration.
- Added nine Roborazzi goldens covering phone and tablet, light and dark, large text, Korean, and selected, disabled, and keyboard focus states.

## v2.22.0 - Quick insert commands - 2026-07-13

A feature release that inserts common Markdown structures instantly by typing `/` at the start of a line in the editor. Every command produces standard Markdown, and the local-first, no-cloud principle and the existing toolbar are unchanged.

### Added
- **Quick insert commands.** Typing a command name after `/` lets you search for and insert headings (H1–H3), bullet, numbered, and checklists, quotes, code blocks, dividers, tables, callouts, wikilinks, images, and today's date.
- **Touch and keyboard selection.** Commands can be chosen by tap or with the up and down arrow keys and Enter on an external keyboard, and the cursor returns naturally to the writing surface after insertion.
- **Command names in six languages.** The quick insert menu is localized in Korean, English, Spanish, Japanese, French, and German.

### Privacy
- Command handling and search happen entirely on the device. No new permissions, network access, accounts, analytics, ads, database changes, or external dependencies.

### Tests
- Unit tested `/` trigger boundaries, exclusion of URLs and mid-line slashes, command filter ordering, and the Markdown output and cursor position of all 14 commands.
- Added Compose tests for the quick insert panel's selection and touch behavior and for the real editor insertion path.

## v2.21.1 - Folder sync: deleted-note resurrection fix - 2026-07-10

A patch release fixing an issue (#148) where deleted notes came back after an app restart when folder sync was in use. Local-only; notes and highlights are unchanged.

### Fixed
- **Deleted-note resurrection (#148).** With folder sync, deleting a note (moving it to trash) leaves the mirrored `.md` file in place. On restart, folder reconciliation considered only active notes, so it mistook that file for a new note and restored it — the same defect also returned archived notes to active. Reconciliation now considers all notes (active, archived, and trashed) and skips files belonging to deleted notes. Permanent deletion (emptying the trash) is unaffected.

### Tests
- Extracted the folder reconciliation decision into a pure `reconcileAction` function and added unit tests for trash skipping, correct archive reconciliation, re-sync after restore, and `getAllNotes`. `./gradlew testDebugUnitTest` passes.

## v2.21.0 - Interactive motion & foldable sidebar - 2026-06-25

A feature release that raises the app's distinctly Android interactive feel. It adds predictive back, screen and list transition animations, and a card to editor expansion, plus note switch fading and a collapsible tag sidebar on tablets. As groundwork, the toolchain was brought up to Kotlin 2.0 and Compose 1.7. Everything is local-only; notes and highlights are unchanged.

### Added
- **Predictive back.** Screen transitions gained shared-axis-X motion, so the edge back gesture previews the previous screen (`enableOnBackInvokedCallback` + Navigation 2.8 seekable pop).
- **Note list animations.** The list reflows smoothly with `Modifier.animateItem` when notes are pinned, added, or deleted.
- **Card to editor shared element transition.** On phones, tapping a note expands the card into the editor (`SharedTransitionLayout` container transform).
- **Tablet note switch fade.** In the 3-column layout, the editor panel cross-fades smoothly when a different note is selected.
- **Collapsible tablet tag sidebar.** The tag rail can be hidden entirely to widen the writing space (the "<" button) and reopened with the ">" button in the note list header — the same idea as Bear's sidebar collapse.

### Fixed
- **Checkbox toggle (#145).** The toolbar checkbox button now toggles `- [ ]`↔`- [x]` on lines that are already checklist items (previously it always inserted a new item). On regular lines it still creates a new `- [ ]` item.

### Changed
- **Toolchain update.** Kotlin 1.9.22 → 2.0.21 (switching to the Compose compiler Gradle plugin), Compose BOM 2024.02.02 → 2024.12.01 (Compose 1.7.6), AGP 8.3.0 → 8.7.3, Gradle 8.5 → 8.9, Navigation 2.8 / Activity 1.9 / Lifecycle 2.8, Robolectric 4.14.1, Roborazzi 1.29.0. The interaction features above require Compose 1.7+. compileSdk and targetSdk remain 35.
- **Editor input performance.** Live Markdown highlighting is memoized and the VisualTransformation stabilized, so large notes no longer re-scan the whole document with regular expressions on every keystroke.
- **Markdown preview links.** Replaced `ClickableText`, deprecated in Compose 1.7, with `LinkAnnotation` (better accessibility, identical rendering).

### Tests
- Added checkbox toggle unit tests (TODO↔DONE, indentation, caret preservation). `./gradlew test lintRelease` passes.

## v2.20.0 - Editor writing upgrades & tablet 3-column - 2026-06-23

A feature release that moves another step closer to Bear on iPad. It refines the writing experience with keyboard shortcuts, tag autocomplete, an outline, and serif typography, and introduces a "tags · note list · editor" 3-column layout on tablets. All local-only, with no new dependencies.

### Added
- **Keyboard shortcuts.** On a hardware keyboard, `Ctrl/Cmd+B` (bold), `I` (italic), `K` (link), and `Shift+S` (strikethrough) apply formatting. `Ctrl+S` alone is deliberately left unbound so that autosaved writing is not accidentally struck through.
- **Inline `#tag` autocomplete.** Typing `#` in the body autocompletes existing tags, just like `[[wikilinks]]`. URL fragments (`…com#frag`) and `##` do not trigger it, and hierarchical tags (`#parent/child`) are supported.
- **Outline (table of contents).** In preview mode, open an outline of headings (H1–H3) to jump straight to that position in a long note.
- **Serif and sans-serif typeface choice.** Settings > Markdown can switch the editor and preview to a serif face for a book-like feel. It uses system fonts, so no assets are bundled, and code blocks always stay monospaced.
- **Tablet 3-column layout.** Wide screens expand into "tag sidebar · note list · editor". Tapping a tag in the sidebar filters the adjacent note list in place (a parent tag includes notes from its child tags), and "All notes" or tapping the same tag again clears the filter. The phone layout is unchanged.

### Changed
- Preview note list scroll state is now host controlled, and that control is reused for outline navigation.

### Tests
- Added unit tests for `#tag` autocomplete detection and completion (`TagAutocompleteTest`), outline extraction (`TocHeadingsTest`), and tag filter hierarchy, normalization, and Unicode matching (`NoteTagFilterTest`). `./gradlew test lintRelease` passes.

## v2.19.1 - Dash tag filter fix - 2026-06-22

A patch release fixing an issue (#144) where tapping a tag containing a dash in the tag list returned no notes.

### Fixed
- **Dash tag filter (#144).** Markleaf indexed dashed tags such as `#old-notes` correctly, but tapping one in the tag list and moving to the search screen routed through the full-text search (FTS) query path, where `-` could be interpreted as search syntax. `#tag` filters now bypass FTS and find active notes directly from the stored tag index (`tags` + `note_tag_cross_ref`), so tags containing dashes or slashes open reliably.

### Tests
- Added an `#old-notes` tag filter regression test to `LocalNoteRepositoryTest`, verifying that only the correct active notes are returned even when a similar `#oldnotes` tag and archived notes are mixed in.

## v2.19.0 - Sample notebook & PDF title fix - 2026-06-18

A release that broadens the first-install experience into a real six-note sample notebook and fixes an issue (#143) where the title appeared twice when exporting a note to PDF or Markdown.

### Added
- **Beautiful sample notebook onboarding.** First-install starter notes grew from four instructional notes into a real six-note sample notebook — a Markdown showcase, a journal-style note, a project brief, tags, search, and backlinks, and a local folder mirror guide, all as ordinary notes.
- **Sample note with a local image.** Bundled Markleaf graphics are copied into the app's internal attachments so real images render in the sample note preview.

### Changed
- **Editor tactile polish.** New notes take focus so you can type immediately, and the cursor returns naturally to the editing surface after leaving preview or using the toolbar, wikilinks, or find and replace.
- **Preview transitions and canvas alignment.** Edit and preview switching is smoother, and preview side margins now match the editing canvas, reinforcing the sense of one shared note surface.
- **Toolbar density.** Slightly reduced the toolbar's top padding and group divider spacing while keeping button touch targets, so the writing surface feels lighter.
- **Better starter note indexing.** Sample note creation now indexes wikilinks as well as tags, so the backlink and local link examples work from the start.

### Fixed
- **Duplicated title in PDF and Markdown export (#143).** Markleaf uses a note's first line as its title (there is no separate title field), but PDF export inserted that first line again as a title heading above the body, showing the title twice — including forcing it in as a title even when the first line was not a heading. Export no longer injects a synthetic title and instead renders the note exactly as the in-app preview does, so the first line (the title) appears exactly once. Single `.md` file export (`ExportUtil`), which duplicated for the same reason, was fixed alongside it, so every export path (PDF, `.md`, share, and full export) now outputs note content as-is.

### Tests
- Extracted `ExportPdf.renderDocument` into a pure function and added regression unit tests (`ExportPdfTest`) verifying that the title is not duplicated in the body for heading first lines, plain first lines, and empty notes. Removed `ExportUtil.generateMarkdownContent`, which appended the title, and cleaned up its tests.

## v2.18.1 - Open-file reopen loop - 2026-06-18

A release fixing an issue (#142) where, after opening or sharing in a `.md` or `.txt` file, pressing back reopened the editor endlessly with no way out, saving a duplicate note with identical content each time.

### Fixed
- **File open and share import reopen loop plus duplicate saves (#142).** The cause was handling the one-shot external file open and share import (#139) inside a `LaunchedEffect` within `MarkleafNavHost`'s NOTES destination. Entering the editor and returning to the NOTES destination via back restarted that destination's composition, re-running the `LaunchedEffect` — so with the intent never consumed, each pass called `createNote(sharedText)` to create a duplicate note with a new UUID and immediately reopened the editor, trapping back navigation and app exit in a loop. The one-shot import was lifted to a single host-scoped `LaunchedEffect(Unit)` that lives as long as the activity instance, so it no longer re-fires on internal navigation re-entry and imports only on a new intent (`onNewIntent` → `recreate`). The widget new note and widget recent note paths are resolved by the same change.

### Tests
- Verified the regression manually — reverting to the pre-fix code reproduces the duplicate notes and editor reopen loop when pressing back after a file import, and after the fix the import happens exactly once and the loop is gone. An automated Robolectric Compose test for this behavior was attempted but excluded: global idle state pollution between Compose tests in the unit test suite caused intermittent, execution-order-dependent `AppNotIdleException` failures in CI, making the gate unreliable. This lifecycle regression is better covered by an instrumented test in future.

## v2.18.0 - Title-named sync files - 2026-06-16

With folder sync, each file is now saved under its **note title**, and renaming a note renames the file in the folder automatically (#134). You can also choose between `.md` and `.txt` extensions.

### Added
- **Filename equals note title (#134).** Folder sync saves notes as `Title.md` instead of the internal `slug-id…` name. Renaming a note renames the file in the folder in place — no need to go into the folder and rename it yourself. Duplicate titles get a ` (2)` suffix, and characters not allowed in filenames are cleaned up automatically.
- **`.md` and `.txt` choice.** Pick the sync file format in the sync center. New files use the chosen format, existing files keep theirs, and import recognizes both.
- **"Rename files to note titles" button.** Converts existing files created under the old naming scheme (`slug-id…`) to title names in one pass.

### Changed
- Notes and folder files are now matched by the frontmatter `markleaf_id` rather than the filename, so matching survives any rename. Renames always write content first and rename second, so data is never lost on failure.

### Tests
- Added unit tests for filename sanitization and collision handling (`MirrorFileNames`).

## v2.17.1 - List inline formatting - 2026-06-16

A release fixing an issue (#141) where formatting such as bold, italic, wikilinks, and inline code was not applied inside bullet, numbered, and checkbox list items, showing the raw markers instead.

### Fixed
- **Inline formatting not applied inside list items (#141).** Bold, italic, `[[wikilinks]]`, inline code, and links written inside bullet, numbered, or checkbox items — such as `- **bold**` or `- [[note]]` — appeared unformatted in preview with raw markers (`**`, `[[ ]]`). List rows took a different render path than body paragraphs and discarded the parsed inline spans. Formatting is now applied identically to body text, and `[[wikilinks]]` inside lists can be tapped to navigate.

### Tests
- Added a `list_inline_formatting` snapshot test covering bold, italic, wikilink, and inline code rendering inside list items, and re-recorded existing goldens that contain lists.

## v2.17.0 - File import & sync - 2026-06-15

A release that adds opening `.md` and text files from a file manager or importing them as notes via share (#139), along with fixes for duplicate notes and unrecognized tags in folder sync and for search and tag list bugs (#140, #138).

### Added
- **Open and share import external Markdown and text files (#139).** Tapping a `.md` or `.txt` file in a file manager (`ACTION_VIEW`) or sharing a file from another app (`ACTION_SEND` file stream) imports it into Markleaf as a new note. If the body has no title line, the filename becomes the note title. Previously only plain text sharing was supported.

### Fixed
- **Duplicate search results (#140).** The `JOIN` in full-text search (`searchNotesFts`) listed a note once per posting when the FTS index held several postings for it; changing to a `rowid IN (...)` form returns each note exactly once.
- **Leftover zero-count tags (#138).** Tags removed from every note remained in the tag list with a count of 0; adding `HAVING COUNT(notes.id) > 0` to `observeTagsWithCounts` shows only tags with at least one active note.
- **Duplicate notes in folder sync (#140 root cause).** `.md` files created by other apps, without a frontmatter `markleaf_id`, were recreated as new notes on every sync; the id is now written to the file on first import, preserving existing frontmatter keys, so later syncs match and update instead. Duplicates already created are not merged automatically, so a single cleanup is enough to stop them recurring.
- **Unrecognized tags and links in sync-imported notes (#138 related).** `#tags` and `[[wikilinks]]` in notes imported from a folder were not indexed until the note was saved again in the editor; the three sync paths were unified into `NoteImporter` so they are indexed at import time.

### Tests
- Added unit tests for hiding zero-count tags, returning a single search result for duplicate FTS postings, frontmatter encoding that preserves unknown keys, and tag and wikilink indexing on import.

## v2.16.5 - i18n & screenshots - 2026-06-11

A release that localizes relative date labels in the note list and adds F-Droid screenshots as requested in GitHub issue #132.

### Fixed
- **Removed hardcoded Korean from note list dates.** Unlike the section headers, `formatUpdatedTime` hardcoded row timestamps as Korean literals for "today", "yesterday", and "n days ago". Users whose device language was not Korean (English, Japanese, German, and so on) saw Korean dates inside an otherwise English UI. These moved to string resources (`relative_today`, `relative_yesterday`, `relative_days_ago`) with translations for all six locales (en, ko, ja, de, es, fr).

### Distribution
- **Added F-Droid phone screenshots (#132).** Four real-device captures (TB320FC, Android 15) were added to `fastlane/metadata/android/en-US/images/phoneScreenshots/` — the live Markdown editor, the code-highlighted preview, the tag screen, and the local-first guide. Per-locale duplicate icon cleanup was completed in an earlier release, so only `en-US/images/icon.png` remains.

## v2.16.4 - Tags in lists - 2026-06-11

A patch release resolving GitHub issue #137, where tags inside bullet list items did not appear in the tag view.

### Fixed
- **Tags in lists (#137).** `TagParser` matched a tag body as "everything up to the next whitespace", so a period or comma at the end of a list item (`#shopping.`, `#work,`) was absorbed into the tag name, failed validation, and dropped the tag entirely. Tag bodies are now matched directly against the set of valid characters (`\p{L}` and `\p{N}`), which naturally excludes trailing punctuation.
- **Multilingual tag support.** The segment character set was limited to Latin and Hangul, rejecting German umlauts and Japanese and Chinese tags; it was widened to Unicode character categories.
- **Removed unnecessary heading and URL exclusion logic.** The `(^|\s)#` prefix condition already filters out URL fragments and leading heading markers, so the previous exclusion pass had no effect other than the side effect of blocking a tag across the whole note if it appeared once in a heading.

### Tests
- Added unit tests for list items, trailing punctuation, comma separation, headings, and German, Japanese, and Chinese tags.
- Added an instrumented test verifying that the Unicode class regular expression works on the Android ICU engine as well as the host JVM.

## v2.16.3 - Stability - 2026-06-10

A stability patch resolving a launch crash on some devices and the keyboard covering text while editing.

### Fixed
- **Launch crash on some devices.** The app failed to start on devices without the `unicode61` FTS tokenizer; it now falls back to the default tokenizer, with a migration added. Existing notes are unaffected.
- **Keyboard covering long notes.** The keyboard covered the current line when typing in long notes; `imePadding` now keeps the cursor visible above the keyboard.

### Added
- **Monochrome (themed) launcher icon.** On Android 13 and later, the icon adapts its color to the home screen theme.

## v2.16.2 - Play production readiness - 2026-05-27

A distribution release preparing the first production submission after Play production access was granted.

### Distribution
- **Version bump for Play Console submission.** `versionCode 90 → 91` and `versionName 2.16.1 → 2.16.2`, so a new AAB can be submitted to the production track without upload conflicts.
- **Public surfaces brought up to date.** GitHub Pages, the README, the privacy page, F-Droid metadata, and repository metadata were aligned with current v2.16.x features and the no-cloud policy, and that state is included in this patch release.
- **Play release notes packaging.** `ko-KR` and `en-US` fastlane changelogs were written against versionCode 91 so the desktop export TXT is generated in a form that can be pasted directly into Play Console.

## v2.16.1 - Smart formatting toggle & word wrapping - 2026-05-21

A release bundling Bear-class smart text formatting improvements for bold, italic, strikethrough, and inline code.

### Added
- **Smart word wrapping.** When there is no drag selection and only a cursor (collapsed), applying a formatting shortcut or toolbar action analyzes Korean, English, and symbol boundaries to identify and wrap the surrounding word precisely.
- **Smart toggle and unwrap.** Applying a formatting button again from inside or just outside text already wrapped in formatting markers (`**`, `*`, `~~`, `` ` ``) intelligently removes the markers.
- **Selection preserved through formatting.** Toggling formatting on dragged text keeps the entire formatted range block-selected instead of clearing the selection, for a smooth continuous editing flow.

### Distribution
- **F-Droid catalog icon metadata.** Added 512x512 PNG icons at `fastlane/metadata/android/*/images/icon.png` so the F-Droid web and client show the Markleaf launcher icon instead of a placeholder.
- **Public surfaces aligned to v2.16.1.** The GitHub Pages landing, privacy page, README, F-Droid metadata, and GitHub repository description and topics were aligned with current features and the no-cloud policy.

## v2.16.0 - Nine Bear-class finishing touches - 2026-05-21

A feature bundle release that handled nine remaining GitHub issues at once against the project's OSS philosophy of local-first, privacy, and open source transparency. Play marketing issues (#52, #53, #54) were triaged separately and closed; only issues aligned with the core values were turned into code.

### Added
- **French (fr-FR) UI translation and onboarding assets.** Added `res/values-fr/strings.xml` and French starter note assets (`starter_notes.md`), substantially widening resource coverage and reaching parity for the French-speaking European market.
- **Automatic EXIF metadata stripping for images.** Private information inside image headers — GPS coordinates, device make and model, time offsets — is removed completely when attachments are inserted or mirrored, protecting local privacy.
- **Multi-device sync center and conflict center UI.** Added a local conflict management hub that collects the "copy from another device" conflict notes produced during SAF folder mirroring into one intuitive view, where they can be deleted individually and merged or cleaned up by hand. Wired into the Settings menu.
- **PDF output layout tuning.** Markdown HTML rendering gained A4 margins (20mm x 18mm), a page-break-inside avoid policy, and the JetBrains Mono font for print-quality output.
- **Four-step first-run guide sheet (#57).** `WelcomeOnboardingSheet` appears exactly once, in `Welcome → Markdown → #tags → local-first` order, without hovering. The DataStore `onboardingCompleted` flag hides it from the second launch onward.
- **Biometric app lock (#37).** `BiometricLockGate` requires fingerprint or face authentication via `androidx.biometric` (AOSP). Toggled in settings, off by default. Authentication completes entirely on-device and notes stay in the same Room DB — the lock is a UI gate and does not affect data. `MainActivity` was promoted to `FragmentActivity` to host `BiometricPrompt`.
- **Note to PDF export (#38).** Added `Export as PDF…` to the editor share menu. The pipeline is `commonmark HtmlRenderer → hidden WebView → PrintManager`. The system print dialog owns the resulting file, so Markleaf never owns the output URI and adds no INTERNET or storage permissions. A4, 18mm and 16mm margins, Markleaf green accent inline styles.
- **Home screen widget as a recent note list (#39).** The previous single-button widget was rewritten as a `ListView`-based collection widget: a `+` button at the top, retaining legacy quick compose, plus the 10 most recent non-trashed, non-archived notes. Tapping a row goes straight to the editor through `ACTION_OPEN_NOTE` → the new `openNoteId` route in `MarkleafNavHost`. `MainActivity.onPause()` refreshes widget data so returning to the launcher shows current state.
- **Japanese (ja) UI translation (#51).** New `res/values-ja/strings.xml` covering every key across notes, editor, settings, privacy dashboard, sync, open source, onboarding, biometrics, and PDF. Japanese was prioritized as the largest Bear user pool identified in the roadmap.
- **Open source section in settings (#55).** Four links — the license (Apache 2.0), the GitHub repo, the full license text, and the F-Droid package page — are collected under `Settings → Open source`, showing the OSS difference from Bear in-product.

### Changed
- **Search uses the unicode61 tokenizer (#65).** `notes_fts` was rebuilt from the `simple` tokenizer to `unicode61 remove_diacritics=2`, ending the regression where Korean, Japanese, and Chinese notes were only searchable through the LIKE fallback. DB v12 → v13; `MIGRATION_12_13` destructively recreates `notes_fts`, a derived index, so no user data is lost. The reason Room does not support `@Fts5` and the trade-off of staying on FTS4 are documented in the `NoteFtsEntity.kt` KDoc.
- **Predictive back gesture (#27).** `android:enableOnBackInvokedCallback="true"` was already active in `AndroidManifest`, and no `BackHandler` intercepts anywhere in the code, so the Android 13+ predictive back animation already worked. Explicitly closed out in this release.
- **TalkBack heading semantics (#76, partial).** Added `heading()` semantics to `SettingsSection` titles and the section headers in `NotesListScreen` (`Pinned / Today / Yesterday / Past 7 days / Older`), so TalkBack swipe up and down skips between sections. `NoteRow` is grouped with `semantics(mergeDescendants = true)` so each note reads as a single focus stop.

### Build
- versionCode 88 → 89, versionName 2.15.3 → 2.16.0.
- Added `androidx.biometric:biometric:1.1.0` (Apache 2.0, F-Droid friendly).
- Added `androidx.exifinterface:exifinterface:1.3.7` (Apache 2.0, F-Droid friendly).
- `MainActivity`: `ComponentActivity` → `FragmentActivity`.
- DB schema 12 → 13, including a new `13.json` schema export.
- Added the `QuickNoteWidgetService` service and widget intents to `app/src/main/AndroidManifest.xml`.

### Issues closed
- #27 #37 #38 #39 #51 #55 #57 #65 #76 [Commercial P1-1] [Commercial P1-3] [Commercial P2-1]

## Earlier releases (Korean)

Entries below predate the move to English-first release notes and are preserved in Korean. The complete Korean changelog, including every entry above, is kept at [CHANGELOG.ko.md](CHANGELOG.ko.md).

## 2026-05-20 - 랜딩페이지 다운로드 링크 줄바꿈 수정 및 모바일 반응형 개선 (Hotfix)

### Fixed
- **깃허브 IO 랜딩페이지(GitHub Pages)의 다운로드 링크 버튼 레이아웃 고도화.** 
  - F-Droid 링크 버튼이 추가되며 버튼 4개가 가로 배치될 때 텍스트가 개행되는 증상을 원천 방지하기 위해 `.btn` 가로 패딩 축소(`2rem` → `1.5rem`), 버튼 간의 간격 축소(`1rem` → `0.8rem`), 그리고 폰트 크기 미세 축소(`0.95rem`)를 통해 텍스트 줄바꿈을 완벽히 방지하고 가로 공간을 극대화했습니다.
  - 가로 너비가 극단적으로 좁아지는 소형 모바일 기기(576px 이하) 환경에서는 버튼들이 자연스러운 터치 영역을 형성하도록 가로 100% 비율의 세로 스택(`flex-direction: column`) 배치 규칙을 적용하여 프리미엄 반응형 레이아웃으로 다듬었습니다.

## v2.15.3 - 코드블록 미리보기 크래시 수정 - 2026-05-20

### Fixed
- **코드블록을 포함한 노트의 미리보기 진입 시 앱이 종료되던 문제 수정.** `SyntaxHighlighter`의 SHELL_RULES 정규식에서 닫는 `}`가 escape되지 않은 한 곳이 있었는데, JVM `java.util.regex`는 이걸 허용해도 Android ICU regex는 거부합니다. 정적 초기화가 실패하면서 어떤 언어의 코드블록이든 미리보기 렌더링 시점에 `ExceptionInInitializerError`로 죽었습니다. JVM 단위 테스트와 Robolectric 스냅샷 테스트는 호스트 JVM regex를 쓰기 때문에 전부 통과했고, 실기기/에뮬레이터에서만 재현되는 회귀였습니다. ([fdroiddata !38659](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/38659)에서 community tester @dking08가 발견)

### Improved
- **에디터 상단 타이틀이 너비를 넘어가면 자동으로 글자 크기를 줄입니다.** "노트 편집" 등 헤드라인이 줄바꿈되지 않고 한 줄로 들어가도록 0.7배까지 점진적으로 축소.

### Tests
- `SyntaxHighlighterAndroidTest` (instrumented): 모든 언어 rule을 실제 Android runtime에서 한 번씩 tokenize해서 ICU regex 호환성을 검증합니다. 같은 종류의 회귀가 다시 들어오면 instrumented test가 잡습니다.

### Build
- versionCode 87 → 88, versionName 2.15.2 → 2.15.3.

## v2.15.2 - F-Droid reproducible-build 가능성 정리 - 2026-05-19

F-Droid `fdroiddata` MR(!38659) 리뷰 후속. APK 자체 동작 변화는 없습니다.

### Build / F-Droid readiness
- **AGP의 "Dependency metadata" APK 서명 블록 비활성화.** `dependenciesInfo { includeInApk = false; includeInBundle = false }` 적용. AGP 8.x는 기본적으로 release APK/AAB에 의존성 메타데이터 서명 블록을 삽입하는데, F-Droid `scanner`가 이를 "extra signing block"으로 잡아 `check apk` 잡이 실패합니다. 우리는 어차피 이 메타데이터를 사용하지 않으므로 출력에서 제거 → F-Droid가 upstream 서명된 APK를 그대로 받아들일 수 있게 합니다 (Binaries + AllowedAPKSigningKeys 경로).
- `versionCode 86 → 87`, `versionName 2.15.1 → 2.15.2`.

## v2.15.1 - F-Droid 빌드 준비와 Room 마이그레이션 안전망 - 2026-05-18

### Build / F-Droid readiness
- **Room schema export 활성화.** `AppDatabase`가 `exportSchema = true`를 사용하고, KSP가 schema JSON을 `app/schemas`에 생성합니다.
- **마이그레이션 회귀 테스트 추가.** v4 레거시 DB를 현재 v12 schema까지 올리며 노트/태그 보존, FTS rebuild, 재도입된 `note_links` / `attachments` 테이블을 확인합니다.
- **재현 가능한 소스 빌드 정리.** Apache 2.0 `LICENSE` 파일을 추가하고, 빌드 서버가 로컬 Windows SDK 경로에 묶이지 않도록 추적 중이던 `local.properties`를 제거했습니다.
- **Fastlane 메타데이터 보강.** F-Droid/store metadata에 재사용할 수 있는 영어/한국어 short/full description을 추가했습니다.
- **compileSdk 35 경고 정리.** 현재 AGP baseline에서 API 35를 의도적으로 타깃한다는 점을 Gradle 설정에 명시했습니다.

## v2.15.0 - Play 정식 출시 준비: 자동 백업 제외 + 빌드 최적화 - 2026-05-14

상용 출시 게이트(Phase 22 Commercial P0-1 / P0-2 / P0-4)를 한 번에 닫는 chore 릴리즈. 사용자 입장에서 보이는 변화는 두 가지:

1. **Markleaf 데이터가 Android 자동 백업 / 기기 간 전송에서 제외됩니다.** 새 기기로 옮기려면 직접 `.md` 내보내기 / 공유 시트 / SAF 폴더 미러 중 하나를 사용해야 합니다.
2. **앱 크기가 ~12 MB → ~1.7 MB 로 줄었습니다** (R8 + 리소스 슈링킹).

신기능 추가 없음. v2.14.x 사용자가 *손에 잡힐 만큼* 체감할 변화는 다운로드 크기와 시스템 백업 동작뿐입니다.

### 정책 변경 — Android Auto Backup 제외 (Commercial P0-1, D046)
- **`AndroidManifest.xml` 의 `<application>` 요소에 `android:allowBackup="false"`.** Android 6+ 의 Google 드라이브 Auto Backup 과 Android 12+ 의 Device-to-Device transfer 모두에서 Markleaf 노트/태그/첨부/설정이 제외됩니다.
- **사용자 영향:** Markleaf 데이터를 새 기기로 옮기려면 명시적인 경로(Markdown export, 시스템 공유 시트, SAF 폴더 미러) 를 직접 사용. OS 차원의 보이지 않는 클라우드 백업이 "사용자가 직접 export/share 하기 전까지 데이터가 기기 밖으로 나가지 않는다" 약속과 충돌하지 않도록 보수적 기본값.
- `dataExtractionRules` 미도입 — 전체 제외 케이스에는 `allowBackup="false"` 가 더 단순/명시적. minSdk=26 환경에서 legacy + new 메커니즘을 동시에 관리하는 표면적 회피.
- 벤치마크 변형의 `tools:replace="android:allowBackup"` 오버라이드 제거 — main manifest 의 `false` 값을 상속.

### 빌드 최적화 — R8 + Resource Shrink (Commercial P0-2, D047)
- **R8 활성화 + 리소스 슈링킹.** `release` 빌드 타입에 `isMinifyEnabled = true` + `isShrinkResources = true` 적용. AGP의 `proguard-android-optimize.txt` 와 함께 minimal `app/proguard-rules.pro` 사용.
  - 결과: 서명된 release APK ~1.7 MB (이전 ~12 MB 대비 87% 감소), AAB ~4.0 MB. 단일 dex.
- **`app/proguard-rules.pro` 신규.** Room entity 클래스/멤버, `AppSettings`, `SyncFrontmatter`/`NoteFolderMirror`, `QuickNoteWidget`, kotlinx.coroutines volatile 필드 keep. `android.util.Log.{d,v,i}` 는 release 에서 R8 가 fold. 각 keep rule 에 *왜* 주석 — 미래에 의존성 업그레이드로 coupling 이 사라지면 제거 가능.
- **벤치마크 변형도 R8 상속.** `benchmark { initWith(release) }` 가 minify/shrink 도 상속 → Macrobenchmark 가 더 release-like 한 APK 측정.

### CI 게이트 강화 (Commercial P0-2)
- 모든 push/PR 빌드 잡에 새 hard-fail 게이트:
  - `./gradlew :app:lintRelease` — release variant 에서 Error 등급 lint 이슈가 하나라도 있으면 빌드 실패. 실패 시 `lint-results-release.html` artifact 업로드.
  - `./gradlew :app:assembleRelease` — R8 가 valid APK 를 만들어내는지 매 빌드 검증. APK 존재 + 크기 > 0 확인.
  - `mapping.txt` 를 `markleaf-r8-mapping` artifact 로 업로드.
- Tag 릴리즈 잡에 `markleaf-vX.Y.Z.mapping.txt` 추가 — 운영 환경 크래시 deobfuscation 가능.
- `launch-smoke` 는 기존대로 debug APK + `continue-on-error: true` 유지.

### 문서 정밀화 (Commercial P0-4)
- **`docs/PRIVACY.md`** — MVP draft 폐기. v2.x 기능(이미지 첨부, SAF 폴더 미러, 외부 링크 열기, 공유) 기준으로 *Markleaf 자체에는 INTERNET 권한이 없다* 와 *사용자가 명시적으로 선택한 OS 경로로 데이터가 이동할 수 있다* 를 구분.
- **`docs/SECURITY.md`** — v2.x 기준 보호 범위 + `allowBackup="false"` 결정 근거 + 외부 링크 `ACTION_VIEW` 위임 포함 사용자 주도 이동 경로.
- **`docs/NOCLOUD_CERTIFICATION.md`** — 시스템 백업 제외 섹션 신설. *What Can Leave the Device* 를 명시적 사용자 행동 기준으로 재서술.
- **`README.md`** — "100% No-Cloud" 카피를 "Markleaf 자체는 네트워크에 나가지 않음 + 사용자 선택 경로로만 이동" 정밀 표현으로 교체.
- **`docs/RELEASE.md`** — R8/mapping/CI gate 섹션 추가. R8 strip 대응 원칙(`-keep` 추가, R8 비활성화 금지) 명시.

### 테스트 픽스
- `EditorLiveSnapshotTest.kt` — `remember(scheme) { MarkdownSyntaxVisualTransformation(...) }` 호출의 lint `RememberReturnType` false positive 를 `@Suppress` 로 한 줄에만 침묵 (lint 가 test 소스 셋 경계 너머 생성자 반환 타입을 해석 못 하는 알려진 케이스).

### 검증
- `./gradlew :app:test` → BUILD SUCCESSFUL (debug + release unit test 모두)
- `./gradlew :app:lintRelease` → BUILD SUCCESSFUL (warning만, error 0)
- `./gradlew :app:assembleRelease` → 1.7 MB APK + 32 MB mapping.txt
- `./gradlew :app:bundleRelease` → 4.0 MB AAB
- `rg "android.permission.INTERNET" -n app/src` → no matches (정책 유지)

### 알려진 후속 작업
- v2.15.0 가 closed test 에 올라가는 것이 R8-shrunk APK 의 첫 실기기 smoke. Compose, Room, Coil, commonmark, SAF, FileProvider, AppWidget, ActivityResult 흐름의 *manual* 실기기 smoke 는 정식 공개 출시 전 수동 수행.
- Commercial P0-3 (Room schema export + migration regression test) 는 별도 cycle.

## v2.14.0 - 각주 점프 (Footnote ref ↔ def click jump) - 2026-05-11

### 새로운 기능
- **미리보기에서 `[^N]` 위첨자를 탭하면 같은 노트의 `[^N]: …` 정의 행으로 자동 스크롤.** 각주가 많은 노트에서 본문↔정의를 손가락 한 번에 왕복할 수 있습니다. 일치하는 정의가 없으면 silent no-op (오류 다이얼로그 X).
- 작동 방식: `MarkdownPreviewList` 가 자기 `LazyListState` 를 들고 있다가, 각주 ref 클릭 콜백을 받으면 `findFootnoteDefIndex` 로 매칭 정의의 라인 인덱스를 찾아 `animateScrollToItem` 으로 부드럽게 점프.
- 콜아웃, 인용문 등 nested 컨텍스트의 각주 ref도 동일하게 작동.

### 디자인 결정
- 정의 → ref 역방향 점프는 v2.14.0 범위에서 *의도적으로 미포함* — 각주 def가 보통 노트 맨 아래에 모이기 때문에 보통은 정의→ref가 아니라 ref→정의 흐름이 필요. 백링크 패널처럼 더 큰 디자인이 필요하다고 판단되면 별도 cycle에서.
- 위첨자 자체에는 underline을 *추가하지 않음* — superscript baseline shift + primary color 만으로 이미 클릭 가능 어포던스가 충분.

### 검증
- `FootnoteJumpTest` — `findFootnoteDefIndex` 의 (1) 매칭 없음 (2) 매칭 있음 (3) 중복 정의의 첫 번째 picking 동작을 단위 테스트로 잠금.

## v2.13.0 - 노트 안에서 바꾸기 (Find & Replace) - 2026-05-11

### 새로운 기능
- **에디터 Find 바에 "바꾸기" 행 추가.** 상단 검색 아이콘으로 열리는 Find 바가 이제 두 줄. 위에는 기존 *찾기 + 결과 카운터 + 이전/다음/닫기*, 아래에는 *바꾸기 텍스트 입력칸 + "바꾸기" / "모두 바꾸기"* 버튼.
- **"바꾸기"**: 현재 하이라이트된 매치 하나만 치환. 다음 매치로는 안 넘김 (그건 ▼ 버튼이 명확히 함).
- **"모두 바꾸기"**: 모든 매치를 단일 패스로 치환 — 길이가 달라져도 인덱스 시프트 없이 안전. 완료 후 Toast로 *N개를 바꿨습니다* 알림.

### 디자인 결정
- §2.5 chrome 누적 회피: 새 상단바 아이콘 *추가 없음*. 검색이 켜져 있을 때만 하나의 Find/Replace 바가 두 줄로 펼쳐짐. 검색이 꺼지면 같이 사라짐.
- 매칭 자체는 기존 `findAllRanges` 의 *대소문자 무시 substring* 정책 그대로 유지 — 정규식이나 case-sensitive 토글은 의도적으로 *지금은 미도입* (UI 복잡도 / 안전 동작 우선).
- 빈 바꾸기 텍스트로 "모두 바꾸기" 하면 *지우기* 효과 (의도된 동작).

### 검증
- `ReplaceRangesTest` — `replaceRange`/`replaceAllRanges` 동작을 단위 테스트로 잠금: 단일 치환, 빈 치환(삭제), 다중 치환, 빈 매치 목록, 더 긴 치환에서도 인덱스 시프트 안전, 대소문자 무시.

## v2.12.0 - 빠른 이동 (Quick switcher / Cmd+K) - 2026-05-09

### 새로운 기능
- **Obsidian 스타일 빠른 노트 점프.** 노트 목록 ⋮ 오버플로의 첫 항목 "빠른 이동", 또는 *하드웨어 키보드의 Ctrl+K* (macOS/iPad-스타일 키보드는 Cmd+K) 로 호출. 화면 위에 떠 있는 다이얼로그에 노트 제목으로 substring 매칭하면서 최대 20개 결과 표시. 탭하면 해당 노트로 이동.
- **빈 쿼리** 일 때는 *최근 수정 순* 으로 정렬된 후보 (마지막에 만지던 노트로 빠르게 돌아가는 흐름).
- *기존 전체 검색* (Search 화면, FTS로 본문까지 매칭) 은 그대로 유지 — 빠른 이동은 *제목만 보는 빠른 점프* 용도. 둘은 의도적으로 다른 도구.

### 디자인 결정
- §2.5 chrome 누적 회피: 새 상단바 아이콘 *추가 없음*. 기존 ⋮ overflow 메뉴 첫 항목 + 하드웨어 키 단축키만으로 surface 확보.
- 키보드 단축키는 *터치 사용자에게 영향 없음* — `onPreviewKeyEvent` 가 키보드 입력이 없으면 no-op.

## v2.11.0 - GFM 테이블 부활 (Tables) - 2026-05-09

### 새로운 기능
- **GFM 표 문법 미리보기 렌더링 부활.** `| 열 | 열 |` + `| :--- | ---: |` 정렬 행이 미리보기에서 진짜 표로 그려집니다. 헤더 행은 굵게 + 약간 어두운 배경, 본문은 alternate row 줄무늬, 행 사이 1dp divider.
- **열 정렬 (`:---` / `---:` / `:---:`)** 그대로 반영 — 좌/우/중앙 정렬.
- **셀 내용에 인라인 마크다운**: 현재는 plain text로 표시 (v2.x.x 백로그). 굵게/기울임/링크는 추후 cycle.

### v1.2 결정 의식적 reversal
- v1.2.0에서 *MVP simplicity* 명목으로 제거했던 표/수식 렌더링 중 표만 부활. 사용자 합의 후 post-MVP에서 *확장으로 분류*. 수식은 §2.7 가치관 마찰 검토 후 결정.

### 데이터 모델
- `PreviewLineType.TABLE` + `TableData(headers, rows, alignments)` + `TableAlignment { LEFT, CENTER, RIGHT }`. 기존 PreviewLine에 nullable `tableData` 필드 추가.
- `commonmark-ext-gfm-tables:0.24.0` 의존성 (이미 등록된 commonmark BSD-2 라이센스 모듈군과 동일).

### 검증
- 단위 테스트 `parse_parsesGfmTable` — 헤더/본문/정렬 모두 정확히 추출.
- Roborazzi 골든 `table_light` — 헤더 굵게, zebra row, 정렬, divider 모두 시각 회귀 락.

## v2.10.0 - 코드 블록 syntax highlighting - 2026-05-09

### 새로운 기능
- **펜스 코드 블록의 언어별 syntax highlighting.** ` ```kotlin ` 처럼 언어 힌트를 적으면 미리보기에서 키워드 / 문자열 / 숫자 / 주석 / 함수명이 각자 다른 색으로 표시됩니다.
- **지원 언어 10개**: Kotlin, Java, Python, JavaScript, TypeScript, Bash/Shell, JSON, YAML, XML/HTML, SQL. 그 외 언어 또는 언어 힌트 없는 블록은 기존대로 모노스페이스 단색 폴백 (변경 없음).

### 디자인 결정
- **자체 regex 기반 토크나이저**, 외부 라이브러리 0. Apache 2 라이선스의 Prism4j 등 후보를 검토했지만 *§2.7 lightweight bias + APK 크기* 정신에서 직접 구현. 10개 언어 룰셋 = 약 350 LOC, 의존성 추가 0.
- **색상은 Material 3 컬러스킴 재활용**. 라이트/다크/Material You 모두 자동으로 일관된 톤. 별도 코드 테마 설정 없음.
- **충돌 해결**: 코멘트/문자열이 키워드보다 우선. `"fun day"` 안의 `fun` 은 Kotlin 키워드로 잘못 색칠되지 않음.

### 검증
- 단위 테스트 12개 — round-trip (text preserved), 키워드/문자열/주석/숫자 인식, 스트링 안의 키워드 무시, decorator/annotation, YAML key, XML tag/attr 등.
- Roborazzi 골든 신규 2개: Kotlin (함수 + 문자열 + 주석) / Python (decorator + 함수).

## v2.9.2 - 표준 마크다운 링크 클릭 동작 (Markdown link click) - 2026-05-09

### 수정
- **`[라벨](URL)` 형식의 표준 마크다운 링크가 미리보기에서 클릭되지 않던 누락 수정.** 사용자가 온보딩 노트 "Markdown 으로 예쁘게 쓰기"에 있는 "링크 버튼은 `[라벨](대상)` 템플릿을 넣어줍니다" 설명을 보고 *링크가 작동하지 않는다* 고 보고. 원인: `CommonMarkPreviewAdapter` 가 `is Link` 분기에서 URL을 그냥 버리고 텍스트만 inline으로 emit하던 것 (`URL ignored in preview` 코멘트가 증거).
- 이제 표준 마크다운 링크가 primary 색 + 밑줄로 렌더되고, 탭하면 `Intent.ACTION_VIEW` 로 시스템 브라우저 / 메일 / 전화 앱이 URL을 받아 처리합니다.
- *Markleaf 자체는 여전히 INTERNET 권한 없음.* URL을 *다른 앱에게 넘기는 것* 일 뿐이라 §2.2 로컬 우선 유지.

### 데이터 모델
- `PreviewInlineSegment` 에 `href: String? = null` 추가, 새 `PreviewInlineType.LINK`.
- 두 인접 LINK 세그먼트가 *서로 다른 URL* 일 때 잘못 병합되지 않도록 `coalesce()` 가 href까지 비교.

### 검증
- 단위 테스트 `parse_emitsLinkSegmentWithHref` — 라벨 + href 정확히 추출.
- Roborazzi `markdown_link_light` 골든 신규 (primary 그린 + 밑줄 시각 회귀 차단).

## v2.9.1 - 시각 회귀 그물망 정밀화 (Roborazzi tight threshold) - 2026-05-09

### 인프라
- **CI Linux 런너에서 Roborazzi 골든 18장 재기록.** v2.1.1에서 깐 `workflow_dispatch` 훅을 처음 실행 (run 25600591611). Ubuntu에서 record한 골든을 다운로드해 로컬 `app/src/test/snapshots/roborazzi/` 에 commit.
- **`changeThreshold` 0.05f → 0.005f 타이트닝** (10배 정밀). 이전엔 Windows record/Linux verify 사이의 폰트 hint 차이를 흡수하느라 느슨한 임계치였는데, 동일 OS record/verify가 되니 *진짜 시각 회귀* 만 잡도록 조였음.
- 결과: 라이브 프리뷰 + 에디터 미리보기 18개 시나리오의 1픽셀 단위 회귀가 PR 빌드에서 즉시 빨간불.

### 사용자 화면 변화 0.

## v2.9.0 - 동기화 충돌 시 사본 보존 (Sync conflict → keep both) - 2026-05-09

### 새로운 기능
- **양 기기에서 동시 수정한 노트는 더 이상 silent overwrite 되지 않습니다.** "지금 동기화" 가 reconcile 도중 *file이 newer* 이면서 *동시에 local 도 마지막 sync 이후 수정* 됐을 때, 기존 local 노트는 그대로 두고 file 본문을 *별도 사본 노트* 로 가져옵니다. 사본 제목에 `(다른 기기 사본 0509 12:34)` suffix가 붙어 사용자가 직접 비교/병합 가능.
- **충돌 카운트가 sync 결과 toast에 표시.** "동기화 완료 — 업데이트 N, 신규 N, **충돌 사본 N**, 변화 없음 N" — 이전 silent 모드와 달리 무엇이 어떻게 처리됐는지 투명.

### 데이터
- **DB v11 → v12 migration:** `notes` 테이블에 `lastImportedAt INTEGER` 컬럼 추가 (nullable). v2.6 이전 노트는 null로 시작 → 첫 reconcile 때 *file newer + lastImportedAt null* 면 충돌로 분류 (안전쪽 fail).
- **자동 저장 시 lastImportedAt 업데이트.** EditorScreen이 mirror 폴더에 write 성공한 직후 `lastImportedAt = updatedAt` 으로 stamp. 다음 reconcile 이 *remote echo* (방금 우리가 쓴 것) 와 *foreign edit* (다른 기기에서 온 것) 를 구분 가능.

### 의도된 안전 마진 (계속)
- 파일→DB 삭제 sync는 v2.1 이후로 여전히 미구현 (silent data loss 회피).
- 충돌 시 인앱 머지 UI는 v2.9.0 범위 외 — 사본을 두 개 보여주고 사용자가 직접 정리. 머지 도구는 v2.x.x 백로그.

## v2.8.1 - 한국어 어법 다듬기 (Korean copy polish) - 2026-05-09

사용자가 "볼 노트를 선택하세요" 같은 영어 직역체를 지적해서, 한국어 strings 14개 항목을 자연스럽게 다듬음. 영어/스페인어는 그대로.

| 키 | Before | After |
|---|---|---|
| `select_note_to_view` | 볼 노트를 선택하세요 | 노트를 선택하세요 |
| `editor_empty_title` | 조용한 빈 페이지가 준비되었습니다 | 새 노트가 준비됐어요 |
| `editor_empty_hint` | …사용할 수 있습니다. 작성하는 동안 로컬에 저장됩니다. | …쓸 수 있고, 작성하는 동안 자동으로 저장됩니다. |
| `tag_note_count_format` | %1$d개 노트 | 노트 %1$d개 |
| `move_to_trash_message` 외 | …휴지통에서 다시 복원할 수 있습니다. | …나중에 다시 꺼낼 수 있습니다. |
| `show_markdown_syntax_description` | …하이라이팅합니다. | …색으로 강조합니다. |
| `theme_description` | 시스템 월페이퍼 | 시스템 배경화면 |
| `privacy_no_internet` | MVP에서는 INTERNET 권한을 선언하지 않습니다. | 이 앱은 INTERNET 권한을 사용하지 않습니다. |
| `sync_status_unset` | 폴더가 아직 선택되지 않았습니다. | 아직 동기화 폴더를 정하지 않았습니다. |
| `sync_behavior_summary` | 변경분 / 충돌 시 / 따로 관리 | 변경 내용 / 양쪽에서 바뀌면 / 따로 정리 |
| `quick_note_widget_description` | 탭하여 새 노트를 빠르게 생성합니다 | 탭해서 새 노트 바로 만들기 |
| `image_alt_dialog_description` | 화면 낭독기 / 로드되지 않을 때 | 스크린 리더 / 보이지 않을 때 |
| `archive_empty_hint` | 메인 목록에서 | 노트 목록에서 |
| `privacy_no_tracking_desc` 외 | 그것이 우리가 원하는 방식입니다 / 엄격한 …표준 | 그게 좋다고 생각합니다 / 원칙을 엄격히 지킵니다 |

## v2.8.0 - 이미지 alt 편집 다이얼로그 (Image alt edit) - 2026-05-09

### 새로운 기능
- **미리보기 모드에서 이미지 길게 누르기 → alt 편집 다이얼로그.** TextField로 새 alt를 입력하고 "저장" 누르면 본문의 `![oldAlt](path)` 가 `![newAlt](path)` 로 자동 치환. 경로는 그대로 유지되어 첨부 파일 reference 불변.
- 다이얼로그 본문에 alt 의 용도(*screen reader 도움 + 이미지 로드 실패 시 표시*) 한 줄 카피.
- 같은 path가 본문에 여러 번 나타나면 *첫 번째* 만 치환 (UUID 파일명이라 거의 충돌 없음).

### 보류 — 사용자 결정 시 진행
다음 항목들은 *제품 결정* 또는 *외부 트리거* 가 필요해 자동 진행 안 함:
- 충돌 시 양 버전 보존 UI (제품 결정: "two-version" 폴더 vs 인앱 머지 화면)
- 인라인 이미지 (paragraph 안 텍스트 섞인 케이스 — 파서 리팩터, 저가치)
- CI Linux 골든 재기록 (사용자가 GitHub Actions에서 1회 `workflow_dispatch` 트리거)
- ScrollBenchmark 재실행 (노트 50+ 시드된 빌드에서)

## v2.7.0 - 위키링크 자동완성 (Wikilink autocomplete) - 2026-05-09

### 새로운 기능
- **`[[` 입력 시 자동완성 드롭다운.** 본문에 `[[` 를 치는 순간 (또는 `[[hel` 처럼 부분 입력 후) 에디터 하단 — toolbar 위 — 에 매칭되는 노트 제목 최대 8개가 표시됩니다. 탭하면 `[[Title]]` 로 자동 완성되고 커서가 닫는 `]]` 뒤로 이동.
- **검색 알고리즘**: 활성 노트(휴지통/보관함 제외) 중 제목이 쿼리를 *포함* 하는 것 (대소문자 무시). 쿼리가 비어있으면(`[[` 직후) 모든 후보 노트의 알파벳 순.
- **닫는 조건**: 사용자가 `]]` 를 입력하거나 줄바꿈하면 드롭다운이 자동 사라짐.

### 단위 테스트
- `detectWikilinkQuery` 4개 (열림 후 부분 입력 / 빈 쿼리 / 닫힌 후 / 줄바꿈 후)
- `completeWikilink` 1개 (replace + cursor 위치)

## v2.6.0 - 동기화 완성 (Sync completion) - 2026-05-09

### 새로운 기능
- **노트 영구 삭제 시 폴더 mirror에서도 제거.** 휴지통에서 "영구 삭제" 누르면 DB 삭제 + (sync 폴더가 설정돼 있으면) 폴더의 `.md` 파일과 `attachments/<noteId>/` 디렉터리도 함께 삭제. 다른 기기에서 다음 reconcile 때까지 자동 회수됨 (단, 다른 기기가 그동안 재편집하면 v2.1.0의 *file이 strictly newer일 때만 update* 규칙이 보호).
- **첨부 파일이 폴더 mirror에 함께 동기화.** 기존엔 `.md` 만 폴더에 갔고 이미지는 app-private 저장소에만 있어서 다른 기기에서 *broken image* 였음. 이제 노트 자동 저장 시 `<filesDir>/attachments/<noteId>/*` 파일들이 폴더의 `attachments/<noteId>/` 로 자동 복사. UUID 파일명 기반 dedup으로 동일 파일 재복사 회피.
- **노트 영구 삭제 시 disk attachment 파일 cleanup.** Room CASCADE가 `attachments` 테이블 row만 지우고 실제 파일은 남기던 누수 수정. `AttachmentManager.deleteAllForNote(context, noteId)` 가 `<filesDir>/attachments/<noteId>/` 디렉터리 통째로 삭제.

### 의도된 안전 마진 (계속)
- **파일→DB 삭제 sync는 여전히 *미구현*.** 외부 sync 클라이언트가 mid-flight일 때 silent data loss 우려가 있어 보류. *DB→파일 삭제만 자동.*

### 데이터
- 데이터베이스 스키마 변경 없음.

## v2.5.3 - 문서 정리 (Spec + backlog cleanup) - 2026-05-09

### 문서
- **AGENT_SPEC.md §15 *Post-MVP 방향* 추가** — v2.x 합의(다중 기기 sync / Bear-class 라이브 프리뷰 / 위키링크·이미지 부활)를 명시. §1–§14는 *MVP era 한정* 으로 라벨링되어, 새 컨트리뷰터가 spec 보고 v2.x 작업과 혼란 없도록.
- **`.agent/tasks.md` Phase 10 정식 close** — Roborazzi가 대체했으므로 Lenovo TB320FC Compose UI test 호스트 이슈는 *추적 중지*.
- **Phase 8 backlog WebDAV/Drive sync 항목 close** — v2.1 SAF 폴더 mirror가 *우리 백엔드 없이* 두 기능 모두 subsume.

### 코드 변경 없음

## v2.5.2 - 태블릿 UX 다듬기 (Tablet polish bundle) - 2026-05-09

### 수정
- **펼친 노트 리스트가 status bar 영역까지 surfaceVariant 색을 침범하던 문제** — `Modifier.systemBarsPadding() + consumeWindowInsets(WindowInsets.systemBars)` 로 v1.4.2 collapsed-rail 패턴과 일치시킴.
- **검색/태그/휴지통/보관함 진입 시 status bar가 흰색으로 뚫리며 흰 아이콘이 안 보이던 문제** — 두 갈래 원인 모두 수정:
  1. `Theme.Markleaf` 가 `Light.NoActionBar` 만 정의돼 있어 다크 모드에서도 액티비티 윈도우 배경이 흰색이었음 → `values-night/styles.xml` 에 다크 부모 추가
  2. `enableEdgeToEdge()` 가 액티비티 시작 시점에만 status bar 아이콘 색을 결정 → `MarkleafTheme` 안에서 `WindowCompat.isAppearanceLightStatusBars` 를 매 컴포지션마다 동기화
- **태그/검색/휴지통/보관함 화면에 뒤로가기 버튼 없던 UX** — 4개 화면 모두 `Scaffold + TopAppBar + 뒤로 화살표` 추가, NavHost가 `popBackStack` 와이어링.

### 인프라
- **Macrobenchmark 첫 실 디바이스 실측 (Lenovo TB320FC, Android 15)**: cold median 326ms, warm 113ms, hot 57ms — §2.1 *빠름 우선* 을 *증거 기반* 으로 확정.
- profileinstaller 1.3.1 → 1.4.0, benchmark-macro-junit4 1.2.4 → 1.3.0 (API 35 지원). benchmark build type을 non-debuggable + `<profileable shell="true" />` 로 정상화.
- `/benchmark/build` `.gitignore` 추가.

## v2.5.1 - 테마 선택 (Theme picker, green default restored) - 2026-05-08

### 변경
- **기본 테마를 Markleaf 그린으로 되돌림.** v1.5에서 Material You가 기본값이 되며 Android 12+ 기기에서는 사용자 월페이퍼 색을 따라가 그린이 사라졌었음. 사용자 명시 의견("leaf와 비슷한 그린이 좋다")에 따라 *원래 정체성으로 복귀*.
- **설정에서 테마 선택 가능:** 설정 → "테마" 섹션에 두 옵션:
  - **Markleaf 그린** (기본) — 앱 이름의 leaf와 어울리는 원래 정적 팔레트
  - **Material You** — Android 12+ 시스템 월페이퍼 기반 동적 색상 (이전 기본값)
- 변경 즉시 반영 — `MainActivity` 가 settings flow를 collect해 ColorPalette 변경마다 `MarkleafTheme` 가 새 색상으로 리컴포즈.

## v2.5.0 - 이미지 첨부 부활 (Image Attachments Restored) - 2026-05-08

### 새로운 기능 (v1.2.0 결정 의식적 reversal)
- **이미지 첨부 부활:** 에디터 툴바에 이미지 아이콘이 추가되어 SAF 파일 피커로 이미지를 고르면 앱 private 디렉터리(`<filesDir>/attachments/<noteId>/<id>.<ext>`)로 자동 복사되고, 본문에 표준 마크다운 `![](attachments/...)` 가 삽입됩니다. 미리보기 모드에서 Coil 로 inline 렌더.
- **권한 0:** SAF 가 파일 접근을 책임지므로 `READ_MEDIA_IMAGES` 같은 미디어 권한 *추가하지 않음*. INTERNET 권한도 그대로 0.
- **마크다운 round-trip:** 본문에 표준 `![alt](path)` 만 들어가므로 다른 마크다운 도구에서도 그대로 읽힘. v2.1 폴더 sync 와도 호환 — `.md` 파일에 같이 동기화됨 (단, 첨부 파일 자체는 별도 폴더에 있어 sync 대상 외 — v2.5.x로 보류).

### 데이터
- **DB 스키마 v10 → v11:** `attachments` 테이블 재도입. `(id PK, noteId FK CASCADE, fileName, mimeType, addedAt)`. 메타데이터 전용 — 실제 파일은 디스크. 노트 영구 삭제 시 cascade로 row 삭제 (파일 삭제 cleanup 은 v2.5.x).

### 새 의존성
- `io.coil-kt:coil-compose:2.6.0` (Apache 2, F-Droid 친화). 이미지 로딩만 담당, 네트워크 요청 0.

### 보류 (v2.5.x)
- 첨부 파일 자체의 v2.1 폴더 sync 동기화
- 노트 삭제 시 disk 파일 cleanup
- 인라인 이미지 (paragraph 안에 텍스트와 섞인 경우) 렌더링
- 이미지 크기 조절 / 캡션 / alt 편집 UI

## v2.4.0 - 위키링크 부활 (Wikilinks Restored) - 2026-05-08

### 새로운 기능 (v1.2.0 결정 의식적 reversal)
- **`[[Title]]` 위키링크 부활:** 본문에 `[[다른 노트 제목]]` 을 입력하면 미리보기에서 클릭 가능한 링크로 렌더됩니다 (primary 색 + 밑줄). 클릭하면 해당 제목의 노트로 이동, 없으면 새 노트가 자동 생성됩니다 (Bear/Obsidian 동일 컨벤션).
- **Backlinks panel:** 미리보기 모드 하단에 *이 노트를 참조한 다른 노트* 섹션이 자동 표시됩니다. 다른 노트가 `[[현재 노트 제목]]` 으로 링크하면 그 노트가 backlinks 목록에 나타나고, 클릭해서 이동 가능.
- **저장 시 자동 인덱싱:** 노트 1초 디바운스 자동 저장 안에 위키링크 추출이 추가되어, 새 링크/제거된 링크가 즉시 backlinks 그래프에 반영됨. 태그 인덱싱과 동일 패턴.

### 데이터
- **DB 스키마 v9 → v10:** `note_links` 테이블 재도입. `(sourceNoteId, targetTitle, normalizedTitle, position)` PK + `notes` cascade. v1.x의 동일 이름 테이블이 v9 마이그레이션에서 dropped됐었기 때문에 fresh 생성. 기존 노트는 다음 자동 저장 시 인덱싱됨.

### 이력 정합성
v1.2.0에서 *명시적으로 제거*된 기능을 사용자의 의식적 결정 (post-MVP, "확장에 가깝다") 으로 부활. 기존 제거 사유였던 "두 번째 두뇌 스타일은 가치관 어긋남" 논거는 MVP-era에 한정됐음을 메모리(`feedback_lightweight_bias`)에 기록.

## v2.3.0 - 공개 마크다운 파서 (CommonMark library swap) - 2026-05-08

### 내부 변경 (사용자 화면 영향 미세)
- **`SimpleMarkdownPreview` 손파서 → `commonmark-java 0.24.0` 로 교체.** v2.0에서 보류했던 Phase B. 이제 위키링크/이미지/하이라이트 등 *실제 확장 도입* (v2.4+) 의 기반.
- **확장 패키지:** `commonmark-ext-yaml-front-matter` (frontmatter), `commonmark-ext-footnotes` (각주), `commonmark-ext-gfm-strikethrough` (취소선), `commonmark-ext-task-list-items` (체크박스). 모두 BSD-2-clause, F-Droid 친화.
- **`CommonMarkPreviewAdapter`** — AST → 기존 `PreviewLine`/`PreviewInlineSegment` 모델 변환. 렌더러(`MarkdownPreviewList`)는 변경 없음.
- **GitHub callout 처리** — `> [!NOTE]` 패턴은 CommonMark 표준이 아니라 우리가 BlockQuote 본문 prefix 매칭으로 인식.

### Spec 정합성으로 인한 미세 동작 변화
- *Setext 헤딩* 인식: `Body\n---` 은 이제 *H2 헤딩* 으로 처리 (기존 손파서는 BODY+HR로 잘못 처리). 가로선을 원하면 `---` 앞뒤에 빈 줄.
- *각주 참조*는 *해당 정의가 같은 문서에 있을 때만* 인식 (CommonMark spec). 참조만 있고 정의가 없으면 일반 텍스트.
- *블록 사이 빈 줄*은 더 이상 `EMPTY` PreviewLine 으로 표면화되지 않음 — 렌더러가 padding으로 시각 간격 처리. 미리보기가 v2.2 이전 대비 약간 더 빽빽.

### 검증
- 단위 테스트 expectation 7개 업데이트 (CommonMark 스펙 동작 반영). 테스트 의도 동일하게 유지하되 실제 파서 출력에 맞게 조정.
- Roborazzi 골든 14개 + 4개 (preview + editor live) 모두 재기록.

### 보류 (빌드 정리)
- `:benchmark` 모듈 빌드 산출물 495개가 v2.2.0 commit에 잘못 포함. v2.3에서 `.gitignore`에 `/benchmark/build` 추가하고 캐시 untrack. v2.2.0 태그 자체는 immutable이라 그대로 둠.

## v2.2.0 - 성능 측정 인프라 (Macrobenchmark) - 2026-05-08

### 새로운 인프라
- **`:benchmark` 모듈 신설** — Macrobenchmark 1.2.4 기반의 성능 측정 전용 Gradle 모듈. APK에는 영향 0 (벤치마크 의존성은 별도 모듈, app은 `androidx.profileinstaller:1.3.1` 만 추가).
- **StartupBenchmark** — 콜드/웜/핫 시작 시간 측정 (5회 반복). §2.1 *빠름 우선* 을 *증거 기반*으로 검증.
- **ScrollBenchmark** — 노트 목록 fling 스크롤 jank 측정 (`FrameTimingMetric`). 90th-percentile 프레임 duration이 회귀 시 빨간불.
- **`benchmark` build type** — 앱 빌드의 release-mirror + debuggable. `:benchmark:connectedBenchmarkAndroidTest` 가 이 빌드 위에서 측정 (실제 디바이스 또는 에뮬레이터 필요. Robolectric 위에선 작동 안함 — 의도된 한계).

### 의도적 비포함
- CI 자동 실행 — Macrobenchmark는 실 디바이스/에뮬레이터 필요라 매 PR마다 돌리는 건 비싸다. 필요 시 `workflow_dispatch` 로 추후 추가 가능.
- 베이스라인 프로파일 자동 생성 — v2.2.x로.

## v2.1.1 - 동기화 마감 (Auto-reconcile + CI golden re-record) - 2026-05-08

### 새로운 기능
- **앱 포그라운드 복귀 시 자동 reconcile:** 동기화 폴더가 설정돼 있으면 앱이 RESUMED 상태로 돌아올 때 폴더의 `.md` 변경분을 자동으로 가져옵니다. 60초 throttle로 폴더 IO 폭주 방지. 충돌 규칙은 v2.1.0 그대로 — *file이 strictly newer일 때만* DB 업데이트, silent overwrite 위험 0.
- **CI에서 골든 재기록 워크플로:** GitHub Actions에 `workflow_dispatch` 트리거 + `record_roborazzi: true` 입력 추가. Linux 런너에서 `recordRoborazziDebug` 돌려 새 골든을 artifact로 업로드. 사용자가 다운받아 commit하면 Windows record와 Linux verify 사이의 폰트 hint 차이가 사라져 `changeThreshold` 를 0.05f → 0.005f로 타이트하게 조일 수 있음.

### 의도적 안전 마진 (v2.1.0과 동일)
- 노트 *삭제* 동기화 여전히 미구현
- 충돌 시 양 버전 보존 UI 미구현 (현재: 최근 win)

## v2.1.0 - 다중 기기 동기화 (Folder Mirror Sync) - 2026-05-08

### 새로운 기능
- **다중 기기 동기화 — 우리 서버 0, INTERNET 권한 0:** 사용자가 SAF로 폴더 한 곳을 지정하면, 노트가 저장될 때마다 약 1초 후 그 폴더에 `.md` 파일로 자동 미러링됩니다. 그 폴더가 Dropbox / Drive / Syncthing / OneDrive / NAS 마운트 등으로 동기화되고 있다면, *Markleaf 자체는 네트워크 호출 없이* 다중 기기 작동. CloudKit 같은 잠금형 백엔드 대신 사용자가 자기 데이터의 위치를 정함.
- **노트 ↔ 파일 라운드트립 가능한 frontmatter:** 각 `.md`에 `markleaf_id`, `created_at`, `updated_at`, `pinned`, `archived` 가 표준 YAML frontmatter로 저장됩니다. Obsidian / VSCode / GitHub 등에서 그대로 열림. 외부 도구가 추가한 임의 키(`obsidian_tag` 등)는 round-trip 시 그대로 보존.
- **수동 "지금 동기화" 버튼:** 다른 기기에서 수정된 `.md` 변경분을 가져와 DB에 반영합니다. 충돌 시 *최근 수정본 우선* (file `lastModified` vs `updatedAt`, 2초 슬랙).

### 의도된 안전 마진
- **삭제는 동기화하지 않습니다.** v2.1.0은 *생성·수정만* 양방향 sync. DB→파일 자동 삭제, 파일→DB 자동 삭제는 가장 위험한 작업이라 보류 (가장 나쁜 시나리오 = "다른 기기에 안 지운 사본이 남음" — 회복 가능). 휴지통은 각 기기에서 따로 관리.
- **앱 시작 시 자동 reconcile 없음.** 다른 기기 변경분을 가져오는 건 사용자가 명시적으로 "지금 동기화" 버튼을 눌러야 작동. 백그라운드에서 silent overwrite 위험 0.
- **충돌은 최근 우선.** 두 기기 동시 수정 시 가장 최근 `updated_at`이 win. 양 버전 보존 UI는 v2.1.x로 보류.

### 설정 화면
- 새 "다중 기기 동기화 (폴더 미러)" 섹션 — *동작 원리* 가 명시적으로 설명됨 (3-4줄 카피 + 권장 폴더 위치 예시 + 행동 요약 4줄). 사용자가 세팅 후 어떻게 작동하는지 명확하게 알 수 있도록 의도적으로 carrying.
- 폴더 선택 / 변경 / 끄기 / 지금 동기화 4개 액션. persistent URI permission 자동 처리.

### §2 가치관 정렬
- §2.2 *로컬 우선* — INTERNET 권한 추가 0, 우리 서버 0. *spirit 유지*: 사용자가 데이터 위치를 직접 정하고, sync 인프라는 사용자가 신뢰하는 외부 도구가 담당.
- §2.3 *Plain Text/Markdown* — 노트가 표준 `.md` 파일로 사용자 파일시스템에 그대로. lock-in 0.
- §2.6 *안전 기본값* — 자동 삭제 sync 의도적 회피, last-write-wins 충돌 규칙 명시.

## v2.0.0 - 라이브 프리뷰 Bear-급 (Inline Rich Rendering) - 2026-05-08

### 새로운 기능
- **헤딩이 입력하는 즉시 *진짜로* 큽니다.** `# 제목` 을 치는 순간 그 줄이 H1 사이즈(24sp Bold), `## 부제` 는 H2 (20sp), `### 섹션` 은 H3 (18sp) 로 인라인 렌더링됩니다. 별도 미리보기 모드로 가지 않아도 됨.
- **굵게가 *진짜로* 굵습니다.** `**bold**` 가 SemiBold → Bold (FontWeight.Bold) 로 강화. 이탤릭·취소선·인라인 코드는 기존 의도된 스타일 유지.
- **마크다운 마커가 시각적으로 retreat 합니다.** `#`, `**`, `_`, `~~`, 백틱, `[`, `](`, `)` 등 마커 문자들이 muted color + Normal weight + Normal style로 재설정되어, 옆의 rich-styled 콘텐츠와 시각적 위계가 분명해집니다. Bear의 핵심 체감 차이.

### 동기
- §2.5 *단순하지만 허전하지 않게* + §2.9 *기능 수보다 속도와 디자인* 정신에서, 새 기능을 더하지 않고 기존 *글쓰기 흐름의 깊이* 를 강화. v1.9에서 깐 Roborazzi 시각 회귀망 위에 자신감 있게 진행됨.

### 검증
- `MarkdownSyntaxHighlighterTest` 에 fontSize/fontWeight 기대값 검증 5개 추가.
- 라이브 에디터 모드 시각 골든 4개 신규 (헤딩 라이트/다크, 인라인 emphasis, 혼합 문서) — 향후 회귀를 PR diff에서 즉시 잡음.

### 보류
- 손파서 → CommonMark 라이브러리 교체는 *실제로 위키링크/하이라이트 같은 마크다운 확장을 도입할 때* 까지 보류. 추측 기반 인프라 리팩터를 §2.9에 맞춰 회피.

## v1.9.0 - 시각 회귀 그물망 (Roborazzi Snapshot Tests) - 2026-05-08

### 개발 인프라 (사용자 화면 변화 없음)
- **Roborazzi 시각 스냅샷 테스트 도입:** 라이브 프리뷰 렌더링이 의도하지 않게 바뀌면 CI에서 빨간불이 들어옵니다. 14개 골든 이미지로 시작 — 헤딩, 리스트, 체크박스, 인라인 스타일, blockquote, code block, 콜아웃 5종 (NOTE/TIP/IMPORTANT/WARNING/CAUTION 중 대표 셋), frontmatter, 각주, 통합 문서 — 라이트/다크 양쪽 변형 포함. JVM에서 Robolectric으로 돌아 에뮬레이터 불필요.
- **GitHub Actions 통합:** `./gradlew verifyRoborazziDebug` 가 main push / PR 빌드에 추가. 회귀 발생 시 diff 이미지가 artifact로 업로드되어 PR에서 다운받아 확인 가능.
- **MarkdownPreviewList 컴포저블 추출:** `EditorScreen.kt`에 인라인으로 있던 미리보기 렌더링 코드를 새 `core/markdown/preview/MarkdownPreviewList.kt` 로 분리. 같은 렌더러를 에디터와 테스트가 공유 → 갈라지지 않음.

### 동기
- v1.7 직후 §2 가치관 점검에서 "라이브 프리뷰는 Bear의 강점인데 우리는 화면을 사람 눈으로만 검증하고 있다" 가 드러남. 다음 사이클 (v2.0의 인라인 rich rendering, v2.1의 sync) 에서 *진짜* 큰 변경이 들어오기 전에 그물망부터 두는 것.

### 의존성
- `io.github.takahirom.roborazzi:1.20.0` (Apache 2.0) — testImplementation 전용. APK에 포함 0.
- `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` — 동일하게 testImplementation.

## v1.8.0 - 표면 정리 (Chrome Consolidation) - 2026-05-08

### 정리 (기능 추가 0, 시각 밀도 ↓)
- **노트 목록 상단바 5 → 3 (+overflow):** 검색·태그·보관함·휴지통·설정 5개가 나란히 있던 것을 검색·태그 두 개만 primary로 두고, 보관함·휴지통·설정을 새 ⋮ 오버플로 메뉴로 묶었습니다.
- **에디터 상단바 5 → 3 (+overflow):** 검색·집중·미리보기·공유·휴지통 5개를 검색·미리보기·공유 셋 primary + ⋮ 오버플로(집중 모드 / 휴지통으로 이동)로 재구성했습니다. 자주 쓰는 액션(미리보기, 공유)은 한 탭으로 유지.
- **설정 섹션 5 → 4:** v1.5에서 추가됐던 단일 스위치 "화면 보안" 섹션을 *개인정보* 섹션으로 흡수했습니다. 스위치 자체는 그대로 동작.

### 동기
- v1.5–1.7 세 사이클이 각자 한두 개씩 상단바·설정에 항목을 추가하면서 *AGENT_SPEC §2.5 "단순하지만 허전하지 않게"* 의 임계점에 다가가고 있었습니다. 새 기능 0의 정리 사이클로 다시 호흡 확보.
- 모든 항목은 그대로 접근 가능합니다 — 위치만 이동.

### 데이터
- 데이터베이스 스키마 변경 없음.

## v1.7.0 - 보관함 + 접근성 마감 (Archive UI + Spec Closure) - 2026-05-08

### 새로운 기능
- **보관함 (Archive UI):** 노트 목록에서 길게 누르면 표시되는 메뉴에 "보관" 항목이 추가되었습니다. 보관된 노트는 메인 목록·검색 결과에서 제외되며, 노트 목록 상단바의 새 보관함 아이콘(혹은 태블릿 동일 위치)으로 별도 화면을 열어 조회할 수 있습니다. 보관함 화면에서 길게 눌러 "보관 해제" 또는 "휴지통으로 이동"을 선택할 수 있습니다. *`Note.archived` 필드는 v1.0부터 존재했지만 진입점이 없어 사실상 dead field였던 상태를 정식으로 닫습니다 (AGENT_SPEC §7 *있으면 좋은*의 마지막 미완 항목).*

### 접근성 검증 (#76)
- **TalkBack/contentDescription:** 모든 IconButton과 클릭 가능한 의미 단위 노드가 명시적 라벨을 가짐을 확인. 데코레이티브 아이콘(DropdownMenu leadingIcon, 대시보드 카드 그래픽)은 `contentDescription = null`로 두어 텍스트와 중복 발화되지 않도록 의도적으로 유지.
- **터치 타겟:** Material 3 `IconButton` 기본 48dp 영역, `DropdownMenuItem` 기본 48dp 높이, 노트 행/보관 행 전체 클릭 영역(>=56dp)이 WCAG 2.5.5 AA를 충족.
- **색상 대비:** 라이트/다크 모두 surfaceVariant↔onSurfaceVariant 약 11:1 / 6:1, primary↔primaryContainer 약 5.5:1 / 5:1 — 모두 WCAG AA 4.5:1 이상.

### 데이터
- 데이터베이스 스키마 변경 없음. `archived` 컬럼은 v1.0부터 존재했고 메인 쿼리/검색 쿼리에 `AND archived = 0` 만 추가되었습니다.

### 마감
- AGENT_SPEC §7 *반드시 포함* 16/16, *있으면 좋은* 7/7 모두 충족 — MVP 완료 기준이 정식으로 닫혔습니다.

## v1.6.0 - 마크다운 표현력 + MVP 내보내기 복구 (Markdown Expressiveness + Export Restored) - 2026-05-08

### 새로운 기능 (마크다운)
- **GitHub식 콜아웃 `> [!NOTE]`:** 인용 블록 첫 줄을 `[!NOTE]`, `[!TIP]`, `[!IMPORTANT]`, `[!WARNING]`, `[!CAUTION]` 중 하나로 선언하면 미리보기에서 해당 종류에 맞는 색상 박스로 렌더링됩니다 (`WARN`/`DANGER` 별칭도 인식).
- **YAML 프론트매터 인식:** 문서 상단의 `---` ... `---` 블록을 메타데이터 영역으로 분리해 미리보기에서 작은 모노스페이스 박스로 표시합니다. 본문 중간의 `---`는 기존대로 가로선으로 처리됩니다.
- **각주 `[^1]` / `[^1]: ...`:** 본문의 각주 참조는 위첨자로, 정의 라인은 미리보기 하단에 별도 행으로 표시됩니다. 각주 사이의 클릭 점프는 v1.7+로 미룹니다.
- **Tab/Shift+Tab 들여쓰기:** 에디터에서 Tab 키를 누르면 현재 줄 시작에 2칸 공백이 추가되고, Shift+Tab은 제거합니다. 여러 줄을 선택한 상태에서는 모든 선택 줄에 일괄 적용됩니다 (블루투스 키보드·태블릿 사용자 대상).

### 새로운 기능 (MVP 내보내기 복구)
- **단일 노트 .md 파일 저장:** 에디터 상단바 공유 버튼이 "공유 시트로 보내기" / "**`.md` 파일로 저장**" 두 항목 메뉴로 확장되었습니다. SAF `ACTION_CREATE_DOCUMENT` 로 사용자가 직접 위치/이름을 정해 저장합니다.
- **전체 노트 일괄 내보내기:** 설정 화면에 "데이터" 섹션이 추가되었습니다. SAF `ACTION_OPEN_DOCUMENT_TREE` 로 폴더를 선택하면 휴지통을 제외한 모든 노트가 각각의 `.md` 파일로 저장됩니다. *AGENT_SPEC §7 반드시 포함의 마지막 두 항목으로, ExportUtil/ExportAllNotes 코드는 v0.x부터 존재했지만 UI에 wired되지 않아 사실상 기능이 없던 상태였습니다 — v1.6에서 정식으로 복구.*

### 데이터
- 데이터베이스 스키마 변경 없음. 새 마크다운 문법은 모두 본문 텍스트 안에서 동작합니다.

## v1.5.0 - 플랫폼 마감 (Platform Polish) - 2026-05-08

### 새로운 기능
- **Material You 다이내믹 컬러 (#59):** Android 12 이상 기기에서 시스템 월페이퍼 색을 따라 앱 테마가 자동으로 맞춰집니다. Android 11 이하는 기존 그린 색상을 폴백으로 유지합니다.
- **예측 뒤로가기 제스처 (#27):** `enableOnBackInvokedCallback`을 켜 Android 13+의 새 백 제스처 미리보기 애니메이션이 동작합니다. NavHost가 자동으로 새 dispatcher와 연동되어 추가 코드 없이 뒤로 가기 동작 자체는 그대로 유지됩니다.
- **단일 노트 공유:** 에디터 상단바에 공유 아이콘이 추가되어 시스템 공유 시트로 노트를 .md 파일로 보낼 수 있습니다 (Markor, Telegram, Drive 등). 본문이 EXTRA_TEXT에도 동봉되어 텍스트만 받는 앱에도 폴백.
- **공유 시트로 받은 텍스트 → 새 노트:** 다른 앱에서 "Markleaf로 공유"를 선택하면 본문(필요 시 제목 포함)이 채워진 새 노트가 즉시 열립니다. `text/plain` MIME 만 받습니다.
- **스크린샷·최근 앱 차단 (#47):** 설정 화면에 "화면 보안" 섹션을 추가했습니다. 켜두면 `FLAG_SECURE`가 적용되어 노트 본문이 시스템 스크린샷과 최근 앱 미리보기에 표시되지 않습니다 (기본값: 꺼짐).

### 데이터
- 데이터베이스 스키마 변경 없음. 새 설정값 `screenshot_protection`은 DataStore에 저장됩니다.

## v1.4.2 - 접힌 노트 리스트 레일 인셋 보강 (Collapsed Rail Inset Fix) - 2026-05-08

### 수정
- **태블릿 접힌 노트 리스트 레일이 상태바와 겹치던 문제 해결:** v1.4.1에서 `systemBarsPadding`을 안쪽 Box에 적용해 아이콘은 상태바 밑으로 내려왔지만, 바깥 `Surface`의 회색 배경(`surfaceVariant`)은 여전히 상태바 영역까지 올라가 시계와 겹치는 잔여 문제가 있었습니다. 이번엔 `Surface` 자체에 `systemBarsPadding`을 적용해 회색 배경이 상태바 아래에서만 그려지도록 했습니다. 그 위 영역에는 부모 Row의 배경색(에디터 페인과 동일)이 보여 시각적으로 깔끔히 통일됩니다.

## v1.4.1 - 시스템 바 인셋 통일 (Unified System-Bar Insets) - 2026-05-08

### 수정
- **노치/펀치홀 없는 기기에서 상단 알림바 겹침 문제 해결:** Galaxy S24 같은 노치 있는 기기에서는 정상이지만 Lenovo Y700 2세대 같은 노치 없는 태블릿에서 노트 본문이 알림 영역 밑으로 들어가던 문제를 해결했습니다. `MainActivity.onCreate()` 첫 줄에서 `enableEdgeToEdge()`를 호출해 안드로이드 버전과 기기에 상관없이 동일한 edge-to-edge 동작을 보장하고, Scaffold가 없는 태그·검색·휴지통 화면과 태블릿 접힌 노트 리스트 레일에는 `Modifier.systemBarsPadding()`을 추가했습니다. Material 3 Scaffold + TopAppBar를 쓰는 화면(노트 목록, 에디터, 설정, 개인정보 대시보드)은 추가 작업 없이 자동으로 정확한 인셋이 들어갑니다.

## v1.4.0 - 정리와 글쓰기 습관 (Organization And Writing Habits) - 2026-05-08

### 새로운 기능
- **태그 계층화 (`#parent/child`):** 본문에 `#project/site`, `#meeting/team-a`처럼 슬래시로 태그를 중첩할 수 있습니다. 태그 화면은 자동으로 부모-자식 트리로 표시되며, 자식 태그는 들여쓰기와 함께 부드러운 보조 색상으로 그려집니다. 한글 태그도 똑같이 동작합니다 (`#프로젝트/현장`).
- **집중 모드 (Focus Mode):** 에디터 상단바의 집중 아이콘을 누르면 툴바, 통계 footer, Markdown 라이브 하이라이팅, 미리보기/휴지통 버튼이 모두 숨겨집니다. 본문만 남는 조용한 글쓰기 환경. 다시 누르면 되돌아옵니다.
- **노트 안에서 찾기 (Find in Note):** 에디터 상단바의 돋보기 아이콘으로 검색 바를 열어 현재 노트 안에서 텍스트를 빠르게 탐색할 수 있습니다. 대소문자 무시 매칭, 이전/다음 결과 이동, `현재/전체` 카운트 표시.

### 데이터
- 데이터베이스 스키마 변경 없음. 태그 정규식만 수정되어 기존 태그는 그대로, 새로 입력한 슬래시 태그도 인식됩니다.

## v1.3.1 - 온보딩 노트와 구현 동기화 (Starter Notes Aligned With Implementation) - 2026-05-08

### 수정
- **샘플 노트 재작성:** v1.2.0에서 제거한 위키 링크(`[[Title]]`)와 ZIP 백업을 여전히 안내하던 기본 4개의 시작 노트를, 현재 앱과 일치하도록 영어/한국어/스페인어 모두 다시 썼습니다. 새 시작 노트는 v1.3.0의 12개 툴바 버튼, 스마트 Enter 자동 이어쓰기, 길게 누르기 핀/휴지통 메뉴, 날짜별 그룹화, 그리고 `.md` 단일/전체 내보내기와 휴지통 안전망을 안내합니다.

## v1.3.0 - 글쓰기 도구로의 진화 (Writing Tool Evolution) - 2026-05-08

### 새로운 기능
- **에디터 툴바 대폭 확장:** 제목 순환(`#` → `##` → `###` → 평문), 글머리 기호, 번호 매기기, 인용, 코드 블록, 구분선, 취소선, 인라인 코드 버튼을 추가했습니다. 기존 4개에서 12개로 늘려 Bear급 빠른 마크다운 작성 경험을 제공합니다.
- **스마트 자동 이어쓰기:** `- ` `1. ` `> ` `- [ ] ` 등의 prefix가 있는 줄에서 Enter를 누르면 다음 줄에 같은 prefix가 자동 삽입됩니다. 번호 매기기는 자동으로 증가하며, 빈 prefix 줄에서 Enter를 누르면 prefix가 자동으로 제거되어 리스트를 종료할 수 있습니다.
- **노트 핀 토글 UI:** 노트 목록에서 길게 누르면 드롭다운 메뉴가 열리며, "고정"/"고정 해제"와 "휴지통으로 이동"을 선택할 수 있습니다. 고정된 노트는 핀 아이콘과 함께 목록 최상단의 "고정됨" 섹션에 표시됩니다.
- **노트 목록 날짜 그룹화:** 노트가 "고정됨 / 오늘 / 어제 / 지난 7일 / 이전" 섹션으로 자동 그룹화되어 시간 흐름을 파악하기 쉬워졌습니다 (Bear의 핵심 무드).
- **에디터 통계 표시:** 본문 하단에 단어 수 / 글자 수 / 예상 읽기 시간을 실시간으로 표시합니다 (스펙 §7 *있으면 좋은 기능* 반영).

### 데이터
- 기존 데이터베이스 스키마 변경 없음 (v9 유지). `pinned` 컬럼은 v1.0 시절부터 존재했으며 이제 UI에서 토글 가능합니다.

## v1.2.0 - 가벼움으로 회귀 (Lightweight Realignment) - 2026-05-08

### 새로운 기능
- **노트 삭제 진입점 복구:** 에디터 상단바에 휴지통 버튼을 추가하고, 노트 목록에서 길게 누르면 확인 다이얼로그를 거쳐 휴지통으로 보낼 수 있도록 했습니다. 그동안 휴지통 화면은 있지만 거기로 가는 길이 없던 치명적인 사용성 결함을 수정했습니다.

### 정리 (가벼운 마크다운 앱 가치관 복원)
- **노트 목록 상단 대시보드 제거:** 총 노트 / 고정 / 태그 카운트 카드를 없애 목록 본연의 화면을 차지하지 않도록 했습니다.
- **위키 링크 / 백링크 제거:** 두 번째 두뇌(second-brain) 스타일의 `[[Title]]` 링크 인덱스, 백링크 패널, Quick Open Links 검색 섹션을 모두 걷어냈습니다. 일반 Markdown 링크는 그대로 사용 가능합니다.
- **이미지 첨부 제거:** 첨부 테이블, 이미지 피커, Coil 의존성, 그리고 미디어 저장소 권한(`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE`)을 모두 삭제했습니다.
- **버전 히스토리(스냅샷) 제거:** 자동 스냅샷 테이블/DAO와 복원 다이얼로그를 제거했습니다. 자동 저장과 휴지통이 이미 되돌리기 안전망 역할을 합니다.
- **테이블 / 수식 미리보기 제거:** 스펙 §7에서 MVP 제외로 명시한 표·수식 블록 렌더링을 제거했습니다(평문 입력 자체는 자유).
- **ZIP 백업/복원 제거:** 무거운 사이드카(첨부·링크·스냅샷) 동봉 형식이라 *데이터가 특정 앱에 갇히지 않아야* 한다는 원칙과 어긋나 폐기했습니다. 단일/전체 `.md` 내보내기는 그대로 유지됩니다.
- **툴바 토글 설정 제거:** 굵게/기울임/체크박스/링크 4개 버튼이 항상 보이도록 단순화하고, 7개의 ON/OFF 스위치 메타 설정을 걷어냈습니다.
- **알림 권한 제거:** 실제로 알림을 보내지 않으면서 선언만 되어 있던 `POST_NOTIFICATIONS`와 권한 요청 화면을 삭제했습니다.
- **체크리스트 진행률 인디케이터 제거:** 에디터 하단 진행률 바와 헬퍼 컴포저블을 정리했습니다.
- **드래그 앤 드롭 재정렬 제거:** 길게 누르기를 휴지통 동작에 양보했습니다(기존 구현은 long-press와 충돌해 삭제 동작 자체가 동작하지 않던 버그의 원인이었습니다).

### 데이터
- **DB 스키마 v9 마이그레이션:** `note_snapshots`, `note_links`, `attachments` 세 테이블을 드롭합니다. 노트/태그/검색 인덱스는 그대로 유지됩니다.

### 의존성
- `io.coil-kt:coil-compose:2.5.0` 의존성을 제거했습니다.

## v1.1.21 - 플레이 콘솔 제출 마감 정리 (Play Console Submission Finalization) - 2026-05-07

### 개선
- **릴리즈 버전 상향:** 최종 제출 마감본으로 `versionCode`를 `51`, `versionName`을 `1.1.21`로 상향했습니다.
- **릴리즈 파이프라인 보강:** 태그 릴리즈 CI에서 signed AAB artifact(`markleaf-release-aab`)를 수집할 수 있도록 워크플로우를 정리했습니다.
- **스토어 등록정보 준비:** Play Console 제출용 아이콘/피처 그래픽 리소스를 추가했습니다.
- **이슈 가독성 정리:** 깨진 인코딩으로 읽기 어려웠던 GitHub 이슈 제목/본문을 정리했습니다.

## v1.1.20 - 플레이 스토어 API 레벨 대응 (Play Target API Compliance) - 2026-05-07

### 수정
- **Play 제출 기준 대응:** `compileSdk`와 `targetSdk`를 `35`로 상향해 2025-08-31 이후 Google Play target API 정책 기준을 충족하도록 맞췄습니다.
- **릴리즈 버전 상향:** Play 제출용 빌드 계보를 유지하기 위해 `versionCode`를 `50`, `versionName`을 `1.1.20`으로 상향했습니다.

## v1.1.19 - 릴리즈 모니터링 버전업 (Release Monitoring Version Bump) - 2026-05-06

### 개선
- **릴리즈 버전 상향:** GitHub 태그 릴리즈 재실행 및 이력 분리를 위해 `versionCode`를 `49`, `versionName`을 `1.1.19`로 상향했습니다.
- **릴리즈 모니터링 루프 실행:** 태그 푸시 후 GitHub Actions 빌드/테스트/릴리즈 워크플로우 완료 상태를 추적하는 운영 루프를 적용했습니다.

## v1.1.18 - 릴리즈 권한 복구 및 대규모 기능 업데이트 (Release Permissions Recovery & Major Feature Update) - 2026-05-06

### 새로운 기능
- **No-Cloud 인증 보증:** 앱이 네트워크 권한 없이 동작하며 데이터를 외부로 전송하지 않음을 증명하는 No-Cloud 인증 문서(`docs/NOCLOUD_CERTIFICATION.md`)를 추가했습니다.
- **인앱 개인정보 대시보드:** 설정에서 앱의 개인정보 보호 원칙과 로컬 데이터 저장 현황을 한눈에 확인할 수 있는 대시보드를 추가했습니다.
- **체크리스트 진행률 시각화:** 노트 목록과 에디터에서 체크리스트의 완료 상태를 진행 바와 퍼센트로 표시합니다.
- **홈 화면 위젯 (빠른 작성):** 홈 화면에서 한 번의 탭으로 즉시 새 노트를 작성할 수 있는 위젯을 추가했습니다.
- **드래그 앤 드롭 노트 재정렬:** 노트 목록에서 롱 프레스 후 드래그하여 수동으로 노트 순서를 변경할 수 있습니다.

### 개선
- **이슈 백로그 고도화:** 기존 53건의 이슈를 기술적으로 구체화하고 50건의 신규 제안을 추가하여 총 103건의 상세 기술 명세(`docs/ISSUE_BACKLOG_DETAIL.md`)를 작성했습니다.
- **GitHub Issues 일괄 등록:** 작성된 103건의 명세를 GitHub Issues에 자동 등록하여 프로젝트 로드맵 가시성을 확보했습니다.
- **릴리즈 워크플로우 권한 수정:** GitHub Actions의 `GITHUB_TOKEN` 권한을 `contents: write`로 수정하여 자동 릴리즈 생성이 실패하던 문제를 해결했습니다.

### 수정
- **마크다운 미리보기 파서 안정화:** 테이블, 수식, 인라인 요소 파싱 중 발생하던 비정상 종료 문제를 수정했습니다.

## v1.1.17 - 이슈 백로그 상세화 및 일괄 등록 (Issue Backlog Refinement & Batch Registration) - 2026-05-06 (Release Failed)
*(이 버전은 GITHUB_TOKEN 권한 부족으로 인해 v1.1.18로 대체되었습니다.)*

## v1.1.16 - CI 컴파일 복구 완료 (CI Compile Recovery Complete) - 2026-05-05

### 수정
- **Notes list 컴파일 복구:** `NotesListScreen.kt`에 섞여 있던 미완성 참조와 잘못된 의존을 정리해 GitHub Actions `assembleDebug` 및 `test`가 다시 통과하도록 복구했습니다.
- **새 복구 버전 발행:** 실패한 빌드 이후 복구 상태를 명확히 하기 위해 `versionCode`를 `46`, `versionName`을 `1.1.16`으로 올렸습니다.

## v1.1.15 - 편집기 링크 툴바 명확화 (Editor Link Toolbar Clarification) - 2026-05-02

### 개선
- **편집기 링크 툴바 명확화:** Markdown 링크와 위키 링크 버튼이 같은 체인 아이콘으로 연속 표시되지 않도록 위키 링크 버튼을 `[[ ]]` 문법 표시로 바꾸고, 편집 툴바 아이콘에 길게 누르기/hover 설명을 추가했습니다.

## v1.1.14 - 릴리즈 인증서 파싱 복구 완료 (Release Certificate Parsing Recovery Complete) - 2026-05-02

### 수정
- **인증서 digest 파싱 수정:** GitHub Actions release job이 `apksigner verify --print-certs` 출력에서 실제 SHA-256 digest 값(`$3`)을 읽도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.13` 태그를 재사용하지 않고 `versionCode`를 `44`, `versionName`을 `1.1.14`로 올려 새 자동 릴리즈 태그를 발행합니다.
- **자동 release 마무리 정합성 보강:** release APK 생성, 선택, 서명 검증, asset 업로드가 모두 동일한 production certificate 기준으로 이어지도록 정리했습니다.

## v1.1.13 - 릴리즈 인증서 진단 출력 추가 (Release Certificate Diagnostics) - 2026-05-02

### 수정
- **서명 진단 로그 추가:** GitHub Actions release job이 `signing-report.txt`, actual digest, expected digest를 로그에 직접 출력하도록 수정해 인증서 불일치 여부를 즉시 확인할 수 있게 했습니다.
- **새 진단 버전 발행:** 실패한 `v1.1.12` 태그를 재사용하지 않고 `versionCode`를 `43`, `versionName`을 `1.1.13`으로 올려 새 자동 진단 태그를 발행합니다.
- **release 검증 가시성 강화:** release signing step이 실패해도 원인을 로그만으로 판별할 수 있도록 했습니다.

## v1.1.12 - 릴리즈 APK 고정 경로 우선 복구 완료 (Release APK Fixed Path Priority Recovery Complete) - 2026-05-02

### 수정
- **고정 release 경로 우선 사용:** GitHub Actions release job이 먼저 `app/build/outputs/apk/release/app-release.apk`를 사용하고, 없을 때만 fallback 탐색을 하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.11` 태그를 재사용하지 않고 `versionCode`를 `42`, `versionName`을 `1.1.12`로 올려 새 자동 릴리즈 태그를 발행합니다.
- **release 검증 안정성 보강:** 일반적인 AGP release APK 경로를 우선 사용해 debug/release 선택 혼선을 줄였습니다.

## v1.1.11 - 릴리즈 APK 선택 복구 완료 (Release APK Selection Recovery Complete) - 2026-05-02

### 수정
- **release APK 선택 보정:** GitHub Actions release job이 `app/build/**/*.apk` 중 debug APK가 아니라 `release` 경로 또는 이름을 가진 APK만 선택하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.10` 태그를 재사용하지 않고 `versionCode`를 `41`, `versionName`을 `1.1.11`로 올려 새 자동 릴리즈 태그를 발행합니다.
- **서명 검증 정확성 강화:** release asset 준비와 서명 확인이 동일한 release APK 선택 규칙을 사용하도록 맞췄습니다.

## v1.1.10 - 릴리즈 Gradle 환경변수 복구 완료 (Release Gradle Environment Recovery Complete) - 2026-05-02

### 수정
- **Gradle property 전달 방식 교체:** GitHub Actions release job이 bash 인자 해석에 의존하지 않도록 `ORG_GRADLE_PROJECT_markleaf.requireReleaseSigning=true` 환경변수로 release signing 필수 플래그를 전달하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.9` 태그를 재사용하지 않고 `versionCode`를 `40`, `versionName`을 `1.1.10`으로 올려 새 자동 릴리즈 태그를 발행합니다.
- **자동 release 안정성 강화:** release task 실행, APK 탐색, 서명 검증, GitHub Release asset 업로드가 모두 shell 인자 파싱 영향 없이 이어지도록 정리했습니다.

## v1.1.9 - 릴리즈 Gradle 실행 복구 완료 (Release Gradle Execution Recovery Complete) - 2026-05-02

### 수정
- **release Gradle 실행 복구:** GitHub Actions release job이 `-Pmarkleaf.requireReleaseSigning=true`를 올바른 Gradle project property로 해석하도록 실행 구문을 `./gradlew -Pmarkleaf.requireReleaseSigning=true :app:assembleRelease`로 바로잡았습니다.
- **새 복구 버전 발행:** 기존 `v1.1.8` 수동 릴리즈는 유지하고, 자동 릴리즈 녹색 복구용으로 `versionCode`를 `39`, `versionName`을 `1.1.9`로 올렸습니다.
- **자동 릴리즈 경로 정합성 보강:** 이제 tag release job은 실제 release task를 수행한 뒤 APK 탐색/서명 확인/asset 업로드 단계로 이어지도록 정합성을 맞췄습니다.

## v1.1.8 - 릴리즈 APK 전체 경로 탐색 복구 완료 (Release APK Full Build Tree Discovery Complete) - 2026-05-02

### 수정
- **빌드 트리 전체 APK 탐색:** GitHub Actions release job이 `app/build/**/*.apk` 전체를 재귀 탐색해 CI 환경별로 달라질 수 있는 실제 release APK 위치를 찾도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.7` 태그를 재사용하지 않고 `versionCode`를 `38`, `versionName`을 `1.1.8`로 올려 새 태그 릴리즈를 발행합니다.
- **release 자동화 탄력성 강화:** 서명 검증과 release asset 준비가 동일한 전체 빌드 트리 APK 탐색 로직을 공유하도록 맞췄습니다.

## v1.1.7 - 릴리즈 APK 재귀 탐색 복구 완료 (Release APK Recursive Discovery Recovery Complete) - 2026-05-02

### 수정
- **재귀 APK 탐색:** GitHub Actions release job이 `app/build/outputs/apk/release/**/*.apk`를 재귀 탐색해 하위 디렉터리에 생성되는 release APK까지 찾도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.6` 태그를 재사용하지 않고 `versionCode`를 `37`, `versionName`을 `1.1.7`로 올려 새 태그 릴리즈를 발행합니다.
- **release 검증 경로 보강:** 서명 검증과 release asset 준비가 동일한 재귀 탐색 결과를 사용하도록 맞췄습니다.

## v1.1.6 - 릴리즈 APK 탐색 복구 완료 (Release APK Discovery Recovery Complete) - 2026-05-02

### 수정
- **APK 탐색 방식 보강:** GitHub Actions release job이 metadata 파일 존재를 가정하지 않고, `app/build/outputs/apk/release/*.apk`에서 실제 생성된 release APK를 직접 찾아 사용하도록 수정했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.5` 태그를 재사용하지 않고 `versionCode`를 `36`, `versionName`을 `1.1.6`으로 올려 새 태그 릴리즈를 발행합니다.
- **릴리즈 경로 단순화:** 서명 검증과 release asset 준비가 동일한 실제 APK 파일 탐색 로직을 공유하도록 맞췄습니다.

## v1.1.5 - 릴리즈 산출물 경로 복구 완료 (Release Artifact Path Recovery Complete) - 2026-05-02

### 수정
- **산출물 경로 동적 해석:** GitHub Actions release job이 `output-metadata.json`에서 실제 release APK 파일명을 읽도록 바꿔, 고정된 `app-release.apk` 경로 가정 때문에 실패하던 서명 확인 단계를 복구했습니다.
- **새 복구 버전 발행:** 실패한 `v1.1.4` 태그를 재사용하지 않고 `versionCode`를 `35`, `versionName`을 `1.1.5`로 올려 새 태그 릴리즈를 발행합니다.
- **릴리즈 파이프라인 정합성 강화:** 서명 검증과 GitHub Release asset 준비가 동일한 metadata 기반 APK 경로를 공유하도록 맞췄습니다.

## v1.1.4 - 릴리즈 태그 복구 완료 (Release Tag Recovery Complete) - 2026-05-02

### 수정
- **bash 인자 순서 보정:** GitHub Actions의 Ubuntu runner에서 Gradle project property가 task 이름에 흡수되지 않도록 `-Pmarkleaf.requireReleaseSigning=true`를 release task 앞에 배치했습니다.
- **재복구 버전 발행:** 실패한 `v1.1.3` 태그를 재사용하지 않고 `versionCode`를 `34`, `versionName`을 `1.1.4`로 올려 새 복구 릴리즈를 발행합니다.
- **복구 기록 정리:** `v1.1.3` 실패 시도와 `v1.1.4` 성공 경로를 구분할 수 있도록 작업 이력과 릴리즈 문서를 갱신했습니다.

## v1.1.3 - 릴리즈 워크플로우 복구 완료 (Release Workflow Recovery Complete) - 2026-05-02

### 수정
- **태그 릴리즈 복구:** GitHub Actions release job에서 `-Pmarkleaf.requireReleaseSigning=true` 인자를 안전하게 전달하도록 수정해 `v1.1.0`~`v1.1.2` 태그 릴리즈 실패 원인을 제거했습니다.
- **새 복구 버전 발행:** Android 업데이트 계보를 안전하게 이어가기 위해 `versionCode`를 `33`으로 올리고 `versionName`을 `1.1.3`으로 상향했습니다.
- **릴리즈 이력 정리:** 실패한 기존 태그를 재사용하지 않고 새 태그 릴리즈로 복구하도록 문서와 작업 기록을 정리했습니다.

## v1.1.2 - 버전 불일치 및 워크플로우 복구 (Version Sync and Workflow Recovery) - 2026-05-02

### 수정
- **릴리즈 워크플로우 복구:** `v1.1.1`에서 퇴행된 상세 릴리즈 제목 추출 로직과 잘못된 빌드 태스크 경로(`:app:assembleRelease`)를 복원했습니다.
- **버전 코드 보정:** 이전 버전과의 충돌 방지를 위해 `versionCode`를 `32`로 상향 조정했습니다.
- **문서 동기화:** 누락된 `v1.1.0` 및 `v1.1.1` 작업 내역을 `progress.md`에 반영하여 문서 정합성을 맞췄습니다.

## v1.1.1 - CI 릴리즈 안정화 (CI Release Stability) - 2026-05-02

### 개선
- **CI 워크플로우 수정:** GitHub Actions에서의 서명 빌드 및 릴리즈 프로세스 오류를 수정했습니다.
- **테스트 안정화:** CI 환경에서 간헐적으로 실패하던 성능 테스트 항목을 제거하여 빌드 안정성을 높였습니다.

## v1.1.0 - 릴리즈 및 대규모 기능 개선 (Comprehensive Release) - 2026-05-02

### 새로운 기능
- **백링크 컨텍스트 스니펫:** 노트 미리보기 및 편집 하단에 해당 노트를 참조하는 백링크들의 문맥 스니펫을 표시합니다.
- **태그별 노트 개수 표시:** 태그 목록 화면에서 각 태그가 몇 개의 노트에서 사용되었는지 숫자로 표시합니다.
- **포괄적 테스트 스위트:** 앱의 모든 기능을 검증하는 50개 항목의 자동화 통합 테스트를 구축했습니다.

### 개선
- **다국어 테스트 지원:** 자동화 테스트 코드가 한국어 등 다국어 환경의 기기에서도 정상 동작하도록 리소스 참조 방식으로 개선했습니다.
- **백업 상태 메시지 상세화:** ZIP 백업 및 복원 시 처리된 데이터 개수를 구체적으로 안내합니다.
- **테마 대비 개선:** 가독성을 높이기 위해 테마의 색상 대비를 조정했습니다.

### 검증
- 기기(SM-S921N, TB320FC) 상에서 50개 시나리오의 통합 테스트 수행 완료
- `./gradlew.bat test` (Unit Tests)
- `./gradlew.bat lintDebug`
- `apksigner verify`

## v1.0.27 - 백업 상태 메시지 개선 (Backup Status Messages) - 2026-05-02

### 개선
- Settings의 ZIP 백업/복원 결과 메시지에 처리된 노트, 첨부, 링크 개수를 표시합니다.
- 백업/복원 실패 시 사용자가 다음에 확인할 수 있는 구체적인 안내 문구를 표시합니다.
- 실패 메시지는 error 색상으로 표시해 성공 상태와 구분합니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat compileDebugKotlin`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.27` release APK 설치 및 실행 확인

## v1.0.26 - 백링크 문맥 표시 (Backlink Context Snippets) - 2026-05-02

### 개선
- 에디터 백링크 목록에서 링크가 등장한 주변 문맥을 함께 표시합니다.
- Preview/Edit 화면의 백링크 항목을 제목 + snippet 구조로 정리했습니다.
- 기존 백링크 제목 탭 이동은 유지했습니다.

### 테스트
- wiki link 주변 문맥이 backlink snippet에 포함되는지 검증하는 repository 테스트를 추가했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.26` release APK 설치 및 실행 확인

## v1.0.25 - 태그 화면 개선 (Tag Counts and Navigation) - 2026-05-02

### 개선
- Tags 화면에서 각 태그가 연결된 활성 노트 수를 함께 표시합니다.
- 태그 행을 누르면 기존처럼 `#태그` 검색으로 이동해 해당 태그의 노트를 바로 탐색할 수 있습니다.
- 휴지통으로 이동한 노트는 태그 카운트에서 제외합니다.

### 테스트
- 태그별 활성 노트 수 집계를 검증하는 repository 테스트를 추가했습니다.
- 기본/한국어/Spanish string resource parity를 유지했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalTagRepositoryTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.25` release APK 설치 및 실행 확인

## v1.0.24 - 테마 대비 점검 (Theme Contrast Audit) - 2026-05-02

### 수정
- 노트 목록 제목 색상을 앱 테마의 primary/onPrimaryContainer 색상으로 명시해 목록에서 더 잘 보이도록 개선했습니다.
- 태블릿 2-pane에서 선택된 노트가 없을 때 표시되는 안내 문구가 어두운 배경에서도 보이도록 색상을 명시했습니다.
- 태블릿 2-pane 목록 영역이 `surfaceVariant`와 `onSurfaceVariant`를 일관되게 사용하도록 조정했습니다.
- 앱 고유 색상 체계가 기기 동적 색상에 덮이지 않도록 기본 테마 설정을 고정했습니다.
- Typography letter spacing을 0으로 정리해 화면 전반의 텍스트 렌더링을 일관화했습니다.

### 테스트
- 10k 노트 검색 성능 테스트의 시간 임계값을 로컬/CI 부하에 덜 흔들리는 회귀 방지 기준으로 조정했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.24` release APK 설치 및 실행 확인

## v1.0.23 - 빠른 열기 검색 (Quick Open Search) - 2026-05-02

### 추가
- Search 화면에서 노트, 태그, 위키 링크 라벨을 함께 표시하는 quick-open 결과를 추가했습니다.
- 태그 결과를 누르면 `#태그` 검색으로 바로 전환됩니다.
- 해결된 위키 링크는 대상 노트로 바로 열고, 미해결 링크는 해당 라벨 검색으로 전환합니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.23` release APK 설치 및 실행 확인

## v1.0.22 - 빈 상태 개선 (Empty State Polish) - 2026-05-02

### 개선
- 노트 목록 빈 상태에 명시적인 노트 만들기 버튼을 추가했습니다.
- 에디터 빈 상태에 Markdown, 태그, 링크, 체크박스, 이미지 사용 힌트를 추가했습니다.
- 새 빈 상태 문구를 영어, 한국어, Spanish 리소스에 반영했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.22` release APK 설치 및 실행 확인

## v1.0.21 - 다국어 지원 확장 (Expanded i18n) - 2026-05-02

### 추가
- Spanish UI string resources를 추가했습니다.
- Spanish starter notes raw resource를 추가했습니다.
- 기본/한국어/Spanish 문자열 리소스 key parity를 검증하는 테스트를 추가했습니다.

### 개선
- Markdown preview 지원 설명에 표와 수식 표기 지원을 반영했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.res.ResourceParityTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.21` release APK 설치 및 실행 확인

## v1.0.20 - 10k 노트 성능 최적화 (10k Notes Performance) - 2026-05-02

### 개선
- 노트 목록, 휴지통, 제목 조회에 사용하는 SQLite index를 추가했습니다.
- 검색이 기존 LIKE 전체 스캔 대신 local FTS rowid join 경로를 사용하도록 정리했습니다.
- 검색 결과를 최대 200개로 제한해 큰 데이터셋에서도 화면 렌더링 부담을 줄였습니다.

### 테스트
- 10,000개 노트 데이터셋에서 FTS 검색 결과를 검증하는 repository 테스트를 추가했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.20` release APK 설치 및 실행 확인

## v1.0.19 - 노트 버전 기록 (Note Version History) - 2026-05-02

### 추가
- 노트 수정 전 이전 본문을 로컬 snapshot으로 저장합니다.
- 에디터 상단의 version history 버튼에서 최근 snapshot 목록을 볼 수 있습니다.
- 선택한 snapshot을 현재 노트로 복원할 수 있습니다.

### 개선
- 자동 저장이 너무 많은 버전을 만들지 않도록 snapshot 생성을 5분 단위로 제한하고 노트당 최근 50개만 유지합니다.
- snapshot 복원 전에 현재 버전을 다시 snapshot으로 보존합니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.data.repository.LocalNoteRepositoryTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.19` release APK 설치 및 실행 확인

## v1.0.18 - 고급 Markdown 미리보기 (Advanced Markdown Preview) - 2026-05-02

### 추가
- Preview 모드에서 Markdown table을 header와 row로 렌더링합니다.
- Inline 수식 표기 `$...$`를 본문 안에서 구분해 표시합니다.
- Display 수식 표기 `$$...$$`를 별도 block으로 표시합니다.

### 결정
- 네트워크, WebView 기반 원격 리소스, 폐쇄 SDK 없이 동작하도록 전체 KaTeX 엔진 대신 로컬 수식 표기 preview로 구현했습니다.

### 검증
- `./gradlew.bat testDebugUnitTest --tests com.markleaf.notes.core.markdown.SimpleMarkdownPreviewTest`
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `rg "android.permission.INTERNET" -n app\src`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`

## v1.0.17 - 릴리즈 서명 인증서 고정 (Fixed Release Signing Certificate) - 2026-05-02

### 수정
- 태그 릴리즈에서 release signing 값이 누락되면 빌드가 실패하도록 강제했습니다.
- GitHub Release 생성 전에 APK 서명 인증서 SHA-256이 고정 production 인증서와 일치하는지 검증합니다.
- 잘못된 키스토어로 빌드된 APK가 배포되어 기존 설치 앱과 업데이트 충돌을 일으키는 위험을 줄였습니다.

### 문서
- production release 인증서 SHA-256과 키스토어 교체 금지 기준을 `docs/RELEASE.md`에 기록했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'`
- `apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk`

## v1.0.16 - 노트 목록/편집기 빈 상태 개선 (Improve Empty States) - 2026-05-02

### 추가
- 노트 목록 빈 상태에 📝 아이콘과 안내 문구 개선
- 편집기 빈 상태에 ✏️ 아이콘과 안내 문구 추가
- 빈 상태 레이아웃에 중앙 정렬 및 여백 개선

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug assembleRelease`
- `rg "android.permission.INTERNET" -n app\src`

## v1.0.15 - 태블릿 2패널 시각 구분 개선 (Tablet Two-Pane Visual Polish) - 2026-05-01

### 개선
- 태블릿 expanded 화면에서 노트 목록 pane과 에디터 pane의 배경 톤을 분리했습니다.
- 두 pane 사이에 얇은 divider를 추가해 편집 상태에서도 영역 경계가 더 명확하게 보이도록 했습니다.
- 선택된 노트 row에 subtle highlight를 추가했습니다.
- 접힌 목록 rail에도 별도 표면 톤을 적용했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `rg "android.permission.INTERNET" -n app\src`

## v1.0.14 - 한국어 다국어 지원 (Korean Localization) - 2026-05-01

### 추가
- Android string resource 기반 다국어 구조를 추가했습니다.
- 기본 언어는 영어로 유지하고, 한국어 기기에서는 한국어 UI 문구가 표시되도록 `values-ko` 리소스를 추가했습니다.
- 첫 실행 샘플 노트도 영어 기본/한국어 로케일별 본문으로 분리했습니다.

### 개선
- 주요 화면의 제목, 버튼, 빈 상태, 설정 설명, 접근성 문구를 리소스 기반으로 전환했습니다.
- 설정의 line width 선택지도 한국어 환경에서 한국어로 표시되도록 했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `rg "android.permission.INTERNET" -n app\src`

## v1.0.13 - 라이브 Markdown 하이라이팅 (Live Markdown Highlighting) - 2026-05-01

### 추가
- Edit 화면에서 Markdown 원문을 유지한 채 heading, bold, italic, link, checkbox 문법을 실시간으로 하이라이팅합니다.
- Preview 토글은 그대로 유지하고, 편집 중인 원문 저장 구조는 변경하지 않았습니다.

### 개선
- 설정의 Markdown syntax 표시 옵션이 Edit 화면 하이라이팅에 반영되도록 연결했습니다.
- 하이라이팅은 문자 변환 없이 동일 offset을 유지해 커서와 선택 흐름이 깨지지 않도록 했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.13` release APK 설치 및 실행 확인

## v1.0.12 - 설정 옵션 기반 추가 (Settings Foundation) - 2026-05-01

### 추가
- DataStore Preferences 기반 앱 설정 저장 구조를 추가했습니다.
- 설정 화면에 Markdown syntax 표시/숨김 옵션을 추가했습니다.
- 설정 화면에 Line width 옵션을 추가했습니다: Narrow, Comfortable, Wide.

### 개선
- 태블릿 에디터 최대 폭이 Line width 설정값을 따르도록 연결했습니다.
- 기본 line width는 Comfortable 800dp로 유지합니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.12` release APK 설치 및 실행 확인

## v1.0.11 - 태블릿 노트 목록 접기 (Collapsible Tablet Note List) - 2026-05-01

### 추가
- 태블릿 two-pane 화면에서 왼쪽 노트 목록을 접고 펼칠 수 있는 버튼을 추가했습니다.
- 목록을 접은 상태에서도 다시 펼칠 수 있는 좁은 rail 버튼을 제공합니다.

### 개선
- 목록 접힘 상태에서도 선택된 노트를 유지합니다.
- 넓은 화면에서 에디터가 과도하게 넓어지지 않도록 본문 영역을 최대 800dp 폭으로 중앙 정렬했습니다.
- 폰 화면의 단일 pane 흐름은 변경하지 않았습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.11` release APK 설치 및 실행 확인

## v1.0.10 - Markdown 편집 툴바 추가 (Markdown Editing Toolbar) - 2026-05-01

### 추가
- Edit 화면 하단에 Markdown 편집 툴바를 추가했습니다.
- Bold, Italic, Checkbox, Markdown Link, Wiki Link, Image 삽입 액션을 제공합니다.
- 선택 영역이 있으면 해당 텍스트를 Markdown 문법으로 감싸고, 선택 영역이 없으면 기본 placeholder를 삽입합니다.

### 개선
- 에디터 입력 상태를 선택 영역까지 추적하도록 `TextFieldValue` 기반으로 전환했습니다.
- 이미지 삽입 액션을 편집 툴바로 통합했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.10` release APK 설치 및 실행 확인

## v1.0.9 - Markdown 링크 및 설정 화면 개선 (Markdown Links and Settings Polish) - 2026-05-01

### 수정
- Preview 모드에서 문장 중간의 `[[노트 링크]]`를 링크처럼 표시하도록 개선했습니다.
- 일반 Markdown 링크 `[label](target)`도 Preview 모드에서 링크처럼 표시하도록 개선했습니다.
- 로컬 노트 링크 대상은 기존 검색 흐름으로 연결하고, 외부 URL은 MVP 개인정보/네트워크 원칙에 따라 자동으로 열지 않도록 했습니다.

### 개선
- 설정 화면에 상단 뒤로가기 버튼을 추가했습니다.
- 설정 화면을 데이터 관리, Markdown 안내, 개인정보, 앱 정보 섹션으로 재구성했습니다.
- 백업/복원 실행 후 간단한 결과 메시지를 표시합니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.9` release APK 설치 및 실행 확인

## v1.0.8 - 첫 실행 샘플 노트 온보딩 (Starter Notes Onboarding) - 2026-05-01

### 추가
- 새 설치에서 Markleaf의 핵심 기능을 바로 이해할 수 있도록 샘플 노트 4개를 자동 생성합니다.
- 샘플 노트는 Markdown 작성, 태그, 위키 링크, 백업/내보내기, 로컬 우선 개인정보 원칙을 안내합니다.
- 사용자가 샘플 노트를 삭제한 뒤 앱을 다시 실행해도 자동으로 다시 생성되지 않도록 재시드 방지 플래그를 추가했습니다.

### 문서
- Bear급 Android Markdown 경험을 목표로 하는 갭 리뷰와 Phase 9 제품 개선 계획을 추가했습니다.
- 기능적 유사성은 허용하되, 이름/아이콘/색상/화면 구성/문구/브랜딩은 복제하지 않는 기준을 명확히 했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.8` release APK 설치 및 실행 확인

## v1.0.7 - 안정성 및 MVP 스펙 보강 (Stability and MVP Spec Hardening) - 2026-05-01

### 수정
- 노트 저장 시 제목, 요약, 태그 인덱스가 함께 갱신되도록 수정했습니다.
- 태그와 노트의 관계 테이블이 실제 문자열 노트 ID를 사용하도록 정리했습니다.
- 검색, 태그, 휴지통, 설정 화면으로 이동할 수 있는 상단 액션을 추가했습니다.
- 휴대폰 레이아웃에서 에디터 route가 잘못 생성되던 문제를 수정했습니다.
- DB 스키마 변경 시 사용자 데이터를 삭제할 수 있는 destructive migration을 제거하고 v4에서 v5로 명시 migration을 추가했습니다.
- 위키 링크 저장 시 backlink 인덱스가 갱신되도록 보강했습니다.
- 태블릿 에디터 상단의 Preview/Edit 액션이 잘리는 문제를 줄였습니다.
- 설정 화면 버전 표시가 실제 앱 버전을 따르도록 수정했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat lintDebug`
- `./gradlew.bat assembleDebug assembleRelease`
- `./gradlew.bat connectedDebugAndroidTest`
- Lenovo TB320FC Android 15 실기기에서 `v1.0.7` release APK 실행 확인

## v1.0.6 - 앱 시작 크래시 수정 (Startup Crash Fix) - 2026-05-01

### 수정
- 첫 화면에서 repository 인자가 필요한 ViewModel을 기본 factory로 생성하던 문제를 수정했습니다.
- 앱 시작 시 `NotesViewModel` 생성 실패로 즉시 종료될 수 있던 경로를 명시적인 `MarkleafViewModelFactory`로 교체했습니다.
- Notes, Search, Trash 화면이 동일한 repository 주입 경로를 사용하도록 정리했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.5 - 릴리즈 제목 규칙 보정 (Release Title Rule Fix) - 2026-04-30

### 수정
- GitHub Release 제목이 `v1.0.0 - 정식 출시 (First Major Release)`와 같은 changelog heading 형식을 따르도록 수정했습니다.
- 릴리즈 workflow가 changelog heading에서 끝의 날짜만 제거해 GitHub Release 제목으로 사용하도록 변경했습니다.
- `v1.0.2` 이후 기존 릴리즈 제목을 같은 형식으로 보정했습니다.
- 릴리즈 노트 본문은 한글로 작성하는 규칙을 명문화했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.4 - 릴리즈 노트 규칙 보정 (Release Notes Rule Fix) - 2026-04-30

### 수정
- GitHub 자동 생성 노트 대신 `CHANGELOG.md`의 해당 버전 섹션을 GitHub Release 본문으로 사용하도록 수정했습니다.
- 기존 `v1.0.2`, `v1.0.3` 릴리즈 노트를 문서화된 릴리즈 이력에 맞게 보정했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.3 - 릴리즈 자산 규칙 보정 (Release Asset Rule Fix) - 2026-04-30

### 수정
- GitHub Release에는 signed release APK만 첨부되도록 수정했습니다.
- 릴리즈 APK 파일명을 `markleaf-vX.Y.Z.apk` 형식으로 정규화했습니다.
- 태그 릴리즈에서 debug APK를 중복 업로드하던 별도 workflow를 제거했습니다.

### 검증
- `./gradlew.bat test`
- `./gradlew.bat assembleDebug`
- `./gradlew.bat assembleRelease`

## v1.0.2 - 릴리즈 서명 자동화 (Release Signing Automation) - 2026-04-30

### 변경
- GitHub 태그 릴리즈에서 release APK를 자동 서명하도록 구성했습니다.
- 커밋되지 않는 signing properties를 통해 로컬 release 서명을 선택적으로 사용할 수 있게 했습니다.
- 릴리즈 서명 및 GitHub Secrets 설정 문서를 추가했습니다.

## v1.0.0 - 정식 출시 (First Major Release) - 2026-04-30

### Added
- **정식 출시 (First Major Release)**
- **Tablet Two-Pane Layout**: 큰 화면에서 목록과 에디터를 동시에 볼 수 있는 최적화된 레이아웃 지원.
- **Backlinks**: 현재 노트를 참조하는 다른 노트들의 목록을 에디터에서 바로 확인 및 이동 가능.
- **Backup & Restore**: 모든 노트와 이미지 에셋을 포함한 ZIP 파일 백업 및 전체 복구 기능.
- **Material You**: Android 12 이상 기기에서 시스템 배경화면에 맞춘 다이내믹 컬러 테마 지원.
- **Search & Performance**: SQLite FTS4 기반의 초고속 전문 검색 엔진 완성.
- **Media Support**: 이미지 첨부 및 에디터 내 실시간 미리보기 기능 안정화.

### Changed
- 전반적인 UI/UX 폴리싱 및 간격 조정.
- 데이터베이스 스키마 최종 안정화 (v4).


### Changed
- Integrated reusable agent operation templates into this repository's documentation structure.

### Verification
- `./gradlew test`
- `./gradlew assembleDebug`
