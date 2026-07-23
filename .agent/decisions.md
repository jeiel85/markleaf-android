# Markleaf Decisions

이 파일은 프로젝트의 중요한 결정 사항을 기록합니다.  
에이전트는 구현 중 새로운 구조적 결정이 생기면 이 파일에 추가합니다.

---

## Confirmed Decisions

### D064 - The R8 Mapping Is Not A GitHub Release Asset Either

The GitHub Release carries exactly one asset, the signed APK. The R8 mapping is
still built, verified, and kept — as a 30-day workflow artifact — but it is no
longer attached to the Release. This narrows the asset list D062 defined.

Why:
- Nobody was taking it. The mapping is ~41 MB on every tag and the download
  count is 0 — checked on both v2.29.0 and v2.28.1, whose APKs pulled 6 and 43
  downloads over the same window. It is the AAB problem from D062 in a second
  form: a large file nobody installs, sitting next to a 2.7 MB APK that is the
  only thing a Releases visitor actually wants.
- Permanent copies already exist, which is what made the AAB omission safe and
  makes this one safe too. `:app:exportReleaseToBuildDrive` writes the mapping
  to `D:\Build` next to the AAB, and GitLab publishes it to the Generic Package
  Registry (D057).
- Play Console holds its own deobfuscation copy for anything installed from
  Play, so the GitHub asset only ever mattered for sideloaded crash reports —
  and those are handled from `D:\Build` by the same maintainer who has the file.

Decision:
- `gh release create` attaches `markleaf-vX.Y.Z.apk` only.
- Retention is set per artifact and reflects how long each one can still be
  useful: `markleaf-r8-mapping` 7 days (regenerated on every PR and push, no
  post-hoc value), `markleaf-release-aab` 14 days (dead once uploaded to Play),
  `markleaf-release-mapping` 30 days (the only GitHub-side copy of a released
  version's mapping).
- CI verification is unchanged. The release job still fails if the APK, AAB, or
  mapping is missing from the build outputs — `Verify R8 mapping exists` kept
  the check when the release-asset copy step was removed.
- `D:\Build` is now the primary permanent copy, not a redundant one. GitLab is
  a real mirror but not a guaranteed one: its CI minutes have run out mid-month
  before (v2.28.0/v2.28.1 shipped without GitLab Releases) and `SKIP_GITLAB_CI`
  can disable it outright. If the local export is ever dropped, this entry has
  to be revisited before it is.

### D063 - Passcode Backoff Uses Wall-Clock And Does Not Resist Clock Tampering

`PasscodeBackoff`'s retry deadline is an absolute `System.currentTimeMillis`
timestamp persisted in DataStore, so moving the device clock forward clears an
in-progress wait. That is accepted, and the limit is now written down instead of
being left implied by the changelog's "force quitting the app does not reset it"
(#158).

Why:
- Every alternative trades one bypass for another. `SystemClock.elapsedRealtime`
  is monotonic and survives a clock change, but resets on reboot — and rebooting
  is cheaper than opening Settings to change the date, so that swap makes the
  gate weaker, not stronger. Persisting both and taking the longer remaining wait
  closes each one individually but still falls to reboot *plus* clock change, and
  doubles the state that has to stay consistent.
- Closing it properly needs a time source the device owner cannot move: a server,
  or a TEE-backed monotonic counter. Markleaf has no INTERNET permission
  (`AGENTS.md` non-negotiables), so a server is out, and a TEE counter is real
  work for a gate that is not the security boundary.
- The backoff exists to slow someone guessing at the keypad — a shoulder-surfer,
  or a borrowed unlocked phone. Against an attacker who can open Settings and
  change the system date, the Locked space was never the obstacle: note bodies
  are unencrypted in the app database (D061, `docs/ROADMAP.md`), so the same
  access already reads them without a passcode at all.

Decision:
- Keep the wall-clock deadline. Do not add `elapsedRealtime` tracking.
- `docs/SECURITY.md` states what the Locked space does and does not protect
  against, naming the clock-tampering limit. A rate limit whose strength is
  overstated is worse than one whose ceiling is written down.
- Revisit only if the Locked space stops being a visibility gate — that is, if
  per-note encryption lands and makes the passcode the actual boundary.

### D062 - The AAB Is Not A GitHub Release Asset

> Narrowed by D064: the Release now carries one asset, not two — the mapping was
> dropped from the list as well. Everything below about the AAB still holds.

The GitHub Release carries exactly two assets — the signed APK and the R8
mapping. The signed AAB is built and verified in the same tag job, but it is
published everywhere except the GitHub Release.

Why:
- The AAB exists for one consumer, Play Console, and that hand-off is local:
  `:app:exportReleaseToBuildDrive` writes it to `D:\Build` and the maintainer
  uploads it by hand. A GitHub copy would be a third artifact nobody installs —
  an `.aab` cannot be sideloaded, so offering it to the people who download from
  GitHub Releases invites the wrong file.
- A permanent download link already exists for anyone who needs one: GitLab
  publishes the AAB to its Generic Package Registry and links it from the GitLab
  Release (D057). The mirror is what makes the omission safe.
- CI still verifies the AAB — it is uploaded as the `markleaf-release-aab`
  workflow artifact, and the release job fails if the APK, AAB, or mapping is
  missing from the build outputs. Not attaching it is a distribution choice, not
  a gap in verification.

Decision:
- `gh release create` attaches `markleaf-vX.Y.Z.apk` and
  `markleaf-vX.Y.Z.mapping.txt` only. Do not add the AAB.
- Docs describing the release state the two-asset reality and say where the AAB
  actually goes. This drifted twice — `docs/RELEASE.md` claimed "all three
  assets" for two releases (#158, fixed in #168) and `AGENTS.md` claimed both
  providers publish "APK/AAB 릴리스" until this entry — so treat the asset list
  as a documented interface, not incidental prose.

### D061 - Locked Notes Stay Hidden From Every Derived Read Path

The Locked space is a UI-visibility gate, so every surface that derives text
from note rows — not just the note list — must apply the `locked = 0` filter.

Why:
- v2.25.0 filtered the primary list, search, tags drill-down, widget, and sync
  export, but backlinks and tag counts still read locked rows. A locked note's
  title appeared in the note-information sheet of any note it linked to, and a
  tag used only by locked notes showed a non-zero count that drilled down to an
  empty list.
- A note mirrored to the sync folder *before* being locked kept its readable
  plaintext file, because the export guard only stops future saves.
- `[[Title]]` pointing at a locked note fell through to the "create a new note"
  branch, silently producing a second note under the same title.

Decision:
- `NoteLinkDao.observeBacklinks` and `TagDao.observeTagsWithCounts` filter
  `locked = 0`, matching the queries that already did.
- Locking a note deletes its mirror file when a sync folder is configured, and
  unlocking rewrites it. Delete-on-lock without restore-on-unlock would make the
  note permanently absent from the user's folder.
- Wikilink resolution asks `countLockedNotesWithTitle` after a miss and reports
  "That note is in Locked notes." It does not open the note (that would bypass
  the passcode) and does not create a duplicate. Naming the whereabouts leaks
  nothing new, because the link text is already visible in the current body.
- The Locked screen raises `FLAG_SECURE` for as long as it is composed,
  independent of the global screenshot-protection setting, and restores the
  setting's value on exit.
- Wrong passcodes are rate-limited by `PasscodeBackoff`: four free attempts,
  then a 30s wait doubling per failure to a 5-minute cap. The counter and
  deadline live in DataStore so force-stopping the app cannot clear an
  in-progress backoff. `attemptLockPasscode` is the only unlock entry point so
  the policy cannot be bypassed by a new caller.

Not decided here:
- Encryption at rest for locked notes stays out of scope per `docs/ROADMAP.md`
  ("명시적 비범위와 보류"), which reserves per-note encryption until a separate
  threat model and migration design covering search, attachments, and folder
  mirror is approved.
- Biometric unlock of the Locked space is deferred: it would let anyone who can
  pass the device biometric read notes gated on a *separate* secret, which is a
  product decision about what the passcode promises rather than a hardening fix.

### D060 - Public Landing Uses Quiet Proof And Real Product Evidence

The GitHub Pages landing page prioritizes verifiable product behavior and data
ownership over a decorative feature graphic or repeated marketing cards.

Why:
- The previous hero repeated the Markleaf wordmark inside both the background
  graphic and heading, while no real app screen appeared on the page.
- Equal feature and download cards flattened the product story and made a
  paused Google Play channel look as current as F-Droid.
- A privacy-first app should not require a third-party font request merely to
  explain that promise.

Decision:
- Use the Quiet proof direction: product-aligned green-neutral tokens, large
  plain-spoken Korean copy, real store screenshots, and one DOM-rendered `.md`
  receipt as the signature visual.
- Structure the page as hero -> trust ledger -> write/organize/own stories ->
  privacy flow -> install hand-off.
- Recommend F-Droid, link GitHub to `releases/latest`, label Google Play as
  update-paused, and keep source/license visible.
- Ship static semantic HTML and token-driven CSS with no analytics, runtime API,
  font CDN, or decorative JavaScript. Motion is limited to interactive feedback
  and respects reduced-motion preferences.
- Derive only responsive WebP renditions from the existing fastlane screenshots
  so the landing shows real UI rather than an invented interface while avoiding
  oversized image transfers.

### D059 - Quiet Editor Uses Three Progressive Formatting Surfaces

The Phase 29 editor replaces permanent formatting chrome with progressive
disclosure while preserving the full Markdown action inventory.

Why:
- A permanently visible horizontal toolbar competes with the document and uses
  scarce phone viewport height even when formatting is not needed.
- Selection has a smaller, more predictable action set than general block and
  structure formatting.
- Android's platform selection menu, IME, accessibility focus, and Storage
  Access Framework picker must keep their established ownership boundaries.

Decision:
- Use a compact, footer-anchored `Aa` entry for normal editing.
- Replace that entry with Bold, Italic, Link, and More actions while source text
  is selected; keep Android Cut/Copy/Paste platform-owned.
- Put the complete existing action inventory in a non-modal expanded style
  panel anchored to the editor column. Use a content-width surface on phones
  and a bounded popover on expanded layouts without restoring a permanent
  tablet toolbar.
- Preserve the editor focus/selection and IME for touch entry. Keyboard or
  assistive-technology entry moves navigation focus into the panel and returns
  it on Back/Escape dismissal.
- Quick Insert, autocomplete, find/replace, preview, focus mode, and external
  pickers preempt formatting surfaces. Existing shortcuts and formatting
  transformations remain authoritative.
- Require a component-state showcase or Roborazzi harness before integrating
  the new primitives into `EditorScreen.kt`.

Implementation:
- Keep the 13 formatting transformations behind one `EditorFormattingAction`
  inventory, while retaining the editor's existing hardware shortcut handling
  and Quick Insert transformation path.
- Hide formatting whenever Quick Insert, autocomplete, find/replace, preview,
  focus mode, top-bar menus, dialogs, or the SAF picker owns the interaction.
- Use 48dp actions in a vertically scrolling panel. On phones it follows the
  editor content width; at expanded width it is capped at 360dp.
- Touch actions restore the editor caret/selection, keyboard opening focuses
  Bold first, and Back/Escape dismiss before leaving the editor.

### D058 - Bear-Class Roadmap Prioritizes Product Cohesion Over More Feature Breadth

After v2.22.0, Markleaf already covers the main local Markdown feature set. The
next roadmap prioritizes how existing capabilities appear, connect, and recede
over copying additional Bear checklist items.

Why:
- The remaining daily friction is concentrated in editor chrome, raw syntax
  presentation, fragmented navigation, and capture entry points.
- Bear is useful as an interaction-quality benchmark, but Markleaf must keep its
  own visual identity, Android conventions, and local-first product contract.
- OCR, sketching, broad document export, and dozens of themes have lower value
  than improving the open -> write -> organize -> find -> export loop.

Decision:
- Execute the sequence Quiet Editor -> Living Markdown -> Smart Library ->
  Capture Everywhere -> Personal Writing.
- Keep Markdown text as the database, export, and folder-mirror source of truth.
  Active-context marker hiding is a reversible presentation layer, not a block
  document model or full WYSIWYG editor.
- Preserve no-INTERNET, no-account, no-analytics, F-Droid-friendly constraints
  in every phase.
- Do not schedule per-note encryption/E2EE until a separate threat model covers
  search, attachments, folder mirror, migration, and recovery behavior.
- Treat `docs/ROADMAP.md` v2.23+ as the current execution contract and
  `docs/BEAR_BENCHMARK_GAP.md` as a historical MVP-era assessment.

### D057 - GitLab Is A Public, Independently Built Release Mirror

GitLab is a public source and binary mirror with its own signed tag pipeline,
not merely a private Git ref backup or a link back to GitHub artifacts.

Why:
- Users need a second place to download a release when GitHub is unavailable.
- Expiring CI artifacts are unsuitable as public release files.
- Rebuilding on GitLab verifies that the mirrored source can produce the same
  update-compatible binaries without depending on GitHub Actions.

Implementation:
- Branches and merge requests run non-secret test, lint, and debug APK gates.
- Protected `vX.Y.Z` tags can access hidden/protected signing variables, build
  signed APK/AAB files, and verify the fixed production certificate digest.
- The APK, AAB, R8 mapping, and six-locale notes are uploaded to the GitLab
  Generic Package Registry and linked from a GitLab Release.
- The Play Console export remains `D:\Build` with AAB, mapping, and notes only;
  GitLab-specific APK export uses an explicit CI output directory.
- GitHub tags remain the F-Droid update source, so GitLab redundancy does not
  alter the F-Droid pickup path.
- Repository updates use ordered dual push from one local history (GitLab,
  then GitHub). Automatic bidirectional provider mirroring is intentionally
  disabled to avoid conflicts, propagation loops, and duplicate tag releases.

### D056 - Release Artifacts Export To The Canonical Build Drive

Play submission artifacts are written directly to `D:\Build`.

Why:
- The Desktop `Build` item is a shortcut, while OneDrive can also expose a different directory with the same visible name.
- Resolving the Desktop first can silently place signed artifacts in the wrong directory even though the expected shortcut exists.
- A canonical destination makes release verification deterministic and keeps large AAB/mapping files out of OneDrive.

Implementation:
- `:app:exportReleaseToBuildDrive` creates or reuses `D:\Build` and writes the flat, versioned artifact set there.
- `:app:exportReleaseToDesktop` remains only as a compatibility alias and delegates to the canonical task.
- Release verification checks the files under `D:\Build`, not the Desktop path.

### D055 - Quick Insert Emits Plain Markdown And Keeps Existing Editor Chrome

Quick Insert is a local editor accelerator, not a new block model or stored command language.

Why:
- Markleaf's portability promise requires inserted content to remain ordinary Markdown in Room, exports, shares, and mirrored files.
- A line-scoped `/query` avoids false positives in URLs and prose while making common structures reachable without adding more toolbar buttons.
- Keeping the existing toolbar in the first release makes Quick Insert additive and reversible while its real-world usefulness is established.

Implementation:
- The current line must contain only optional indentation followed by `/query` at a collapsed caret.
- Fourteen commands insert headings, lists, quote/code/divider/table/callout structures, wikilink/image affordances, or an ISO local date.
- Filtering, selection, and Markdown transformation run entirely on-device with no permission, dependency, schema, or network change.
- The panel reuses the editor's Material suggestion-surface language and supports touch plus external-keyboard Up/Down/Enter selection.

### D054 - Tag Filters Use The Stored Tag Index, Not FTS Query Parsing

Tag-filter searches that start with `#` should resolve through the `tags` and `note_tag_cross_ref` tables instead of the general full-text search path.

Why:
- Markleaf's tag grammar allows characters such as `-`, `_`, and `/`.
- SQLite FTS query parsing can treat punctuation such as `-` as syntax or token boundaries, so a valid tag like `#old-notes` can be indexed correctly but fail when routed through `MATCH`.
- A tag tap is an exact local filter, not fuzzy body search, so the cross-ref table is the more accurate source of truth.

Implementation:
- `LocalNoteRepository.searchNotes` detects non-empty `#tag` queries, normalizes the tag name with `TagParser.normalizeTagName`, and calls a dedicated DAO query.
- `NoteDao.searchNotesByTag` joins active notes through `note_tag_cross_ref` and `tags`, preserving the existing pinned/sort/updated ordering.

### D053 - Starter Notes Should Teach By Being A Beautiful Sample Notebook

Markleaf's first-run notes should be a useful sample notebook rather than a thin feature checklist.

Why:
- The best onboarding for a writing app is a small set of notes that users can read, preview, edit, search, link, export, and delete like their own notes.
- A sample notebook can show the real product surface: Markdown styling, images, callouts, tables, code, footnotes, checklists, tags, wikilinks, backlinks, and folder mirror behavior.
- This matches the product direction of clearer proof and first-3-minute flow validation without adding network, account, analytics, or API behavior.

Implementation:
- First-run starter notes now seed six regular notes: Welcome, Markdown showcase, daily journal, project brief, tags/search/backlinks, and local folder mirror.
- The showcase note references a bundled Markleaf feature graphic copied into app-private `attachments/starter-note-2/`, so Preview renders a real local image.
- Starter seeding indexes both tags and wikilinks so the sample notebook's organization and backlink behavior are live immediately.
- All supported starter-note raw resources keep the same six-note separator structure.

### D052 - Final Writing Feel Polish Keeps Focus, Preview, and Toolbar Changes Small

Markleaf treats the final editor polish pass as a tactile interaction pass rather than a feature pass.

Why:
- The editor already has the larger Bear-class canvas work from D051; the next value is removing small breaks in flow rather than adding more chrome.
- Returning from Preview, tapping formatting controls, accepting wikilink suggestions, or closing Find/Replace should not leave the user hunting for the cursor again.
- Preview should feel like the same writing canvas rendered differently, so its left/right rhythm should align with Edit mode.
- Toolbar density can be made calmer without reducing the Material touch target size.

Implementation:
- Added an editor `FocusRequester` and a scoped focus flag so new notes, Preview -> Edit, toolbar actions, wikilink completion, and Find/Replace edits return focus to the text field.
- Wrapped Edit/Preview body swapping in `Crossfade` for a softer transition without changing navigation or persistence behavior.
- Matched preview content padding to the editor canvas (`20.dp` horizontal) and trimmed toolbar group spacing while keeping `IconButton` touch targets intact.

### D051 - Bear-class Editor Canvas, Typography, Empty States, and Notes List Spacing/Metadata Polish (Phase 24)

Markleaf refines the core visual identity and typography canvas to deliver a highly polished, serene, and premium writing and browsing experience, aligning with the Bear-class Android Markdown application guidelines.

Why:
- Premium note-taking apps depend heavily on spacing, grids, and typographical rhythm (the final 10% details) to alleviate eye strain during long writing sessions.
- Default system text settings and tight borders make standard text fields look cluttered and basic. Adding breathing room through a 1.6x line-height ratio (`26.sp` for `16.sp` font) transforms readability.
- BasicTextField's decorationBox empty-state placeholders must not block click events or hide the underlying editor field, which causes focus/cursor bugs. Rendering the inner text field unconditionally beneath the beautiful brand-emerald tinted placeholder ensures UX safety.
- The notes list visual density is enhanced by shifting minor layout controls (dates/times) to natural, clean row metadata and grouping lists cleanly via customized, toned-down section header gaps.

Implementation:
- **Typography and Canvas Spacing**: Updated `Theme.kt` bodyLarge typography lineHeight to `26.sp`. Adjusted `EditorScreen.kt` main Column pad to `horizontal = 20.dp, vertical = 12.dp` and mapped the bodyLarge typography to the `BasicTextField` explicitly.
- **Robust Empty States**: Modified `EditorScreen.kt`'s empty state to always render `innerTextField()` inside a parent Box, avoiding focus blocks, while painting the stylized brand-green emoji placeholder. Refined `NotesListScreen.kt`'s empty state with a bold header, M3-large rounded button incorporating an icon, and balanced paddings.
- **Notes List Metadata and Rhythm**: Added last modified relative dates to note items in `NotesListScreen.kt` using a custom `formatUpdatedTime` helper. Tuned `NoteRow` margins to `horizontal = 16.dp` and row interior pad to `16.dp/12.dp`. Redesigned `SectionHeader` with `labelMedium` SemiBold styling and comfortable top-weighted paddings.

### D050 - Bear-class Smart Text Formatting Toggle, Selection Retention & Word Wrapping (Phase 23)

Markleaf implements highly advanced, premium Markdown formatting actions (Bold, Italic, Strikethrough, Inline Code) inspired by the Bear editor, resolving smart toggling, unwrapping, and multi-lingual word wrapping directly on-device.

Why:
- Standard markdown editors often force users to manually select exact word boundaries or suffer empty placeholders.
- A premium, state-of-the-art writing experience requires wrapping the surrounding word seamlessly at a collapsed cursor, distinguishing Korean, English, and markdown boundaries.
- Re-triggering a formatting command within active markers must intelligently unwrap (toggle off) rather than nesting or breaking the document.
- Preserving the drag selection range even after applying markdown markers enables fluid, non-disruptive continuous editing.

Implementation:
- Modified `MarkdownEditActions.findWordAtCursor` to strictly respect whitespaces and markdown characters (`*`, `~`, `` ` ``, `_`) as boundaries, returning collapsed range when cursor is placed on a space unless at the end of the text.
- Overhauled `MarkdownEditActions.wrapSelection` to retain drag selection boundaries, identify already-wrapped texts (Case 1-A/1-B) and collapsed active formatted areas (Case 2-A) to perform seamless "Unwrap" toggles.
- Wired 12 unit tests in `MarkdownEditActionsTest.kt` verifying word-wrapping bounds, selection ranges, multi-marker boundaries, Korean/English inputs, and fallback structures.

### D049 - Offline-First Sync/Conflict Center, Attachment Privacy, and French Localization supporting Commercial Readiness

Markleaf integrates multi-device offline-first sync conflict resolution, stripes sensitive metadata from image attachments, adopts high-quality PDF print configurations, and expands European locale readiness via fr-FR translations.

Why:
- Play Store commercial launch requires robust local privacy (no accidental coordinate or metadata leaks inside attachment files).
- Multi-device sync using the folder-mirror flow generates local conflicts (other device copies) which must be manageable directly in-app.
- Providing high-fidelity PDF layouts ensures standard-compliant note exporting without network delegation.
- France and other French-speaking European locales are prime audiences for lightweight offline Markdown note applications.

Implementation:
- **Attachment EXIF Stripping**: `AttachmentManager` copies SAF-picked files to app-private storage, then immediately wipes coordinate (`TAG_GPS_LATITUDE`), device maker (`TAG_MAKE`), software, and timestamp markers from image headers.
- **Sync/Conflict Center**: `SyncCenterScreen` displays persistent sync folder configuration details, last synced timestamps, and registers the `observeConflictNotes` room flow query which detects notes matching the mirror conflict schema `%(다른 기기 사본%`. Users can inspect and delete duplicate copies permanently.
- **fr-FR Translations**: `res/values-fr/strings.xml` maps all 200+ localized string resources, matching English and Korean resource parity, coupled with raw-fr starter notes.
- **PDF Styling**: `ExportPdf` renders high-quality A4 document flows with 20mm vertical / 18mm horizontal margins, page-break safeguards on headers/lists/tables, and custom JetBrains Mono code rendering.

### D048 - Room Schema Export And Migration Regression Are Release Gates

Markleaf commits Room schema JSON from database version 12 onward and keeps migration regression coverage for the oldest supported public migration path.

Why:

- A local-first notes app cannot treat database migration as an implementation detail. User notes, tags, links, attachments, and sync metadata must survive app updates.
- F-Droid/source builds benefit from committed schema metadata because generated database structure is explicit and reviewable.

Implementation:

- `AppDatabase` uses `exportSchema = true`.
- KSP writes Room schemas to `app/schemas`.
- Android tests include `AppDatabaseMigrationTest`, which creates a representative v4 SQLite database and opens it with the production migration chain through v12.
- `AppDatabase.ALL_MIGRATIONS` is internal so tests and production share the exact migration list.

Legacy schema note:

- Historical v4-v11 Room JSON files were not committed before this decision, so they cannot be reconstructed with perfect Room identity hashes. Instead, the regression test builds the legacy v4 tables directly and validates the real migration behavior: notes, tags, cross refs, FTS rebuild, removed/reintroduced tables, and the v12 `lastImportedAt` column.

Implications:

- Future schema changes must update `app/schemas` and add or extend migration tests in the same change.
- `fallbackToDestructiveMigration()` remains forbidden for release paths.

### D047 - Release Builds Use R8 + Resource Shrinking With A Minimal proguard-rules.pro

Markleaf release builds run R8 with `isMinifyEnabled = true` and `isShrinkResources = true`. Custom keep rules live in `app/proguard-rules.pro` alongside the AGP-supplied `proguard-android-optimize.txt`. The R8 mapping file is attached to every CI build as an artifact and to every tag release as a downloadable asset (`markleaf-vX.Y.Z.mapping.txt`).

Why now:

- Phase 22 commercial readiness requires that the release build pass through a production gate rather than ship as a debug-quality APK. R8 catches a class of bugs (unused-code paths that survive minification, reflection couplings that need an explicit keep rule, lint Errors that only matter in release) that the debug-only CI never exercised.
- The size win is significant — APK 12 MB → 1.7 MB, AAB 4.0 MB — but the more important benefit is that we now have a real production gate.

Why a minimal proguard-rules.pro:

- AGP's default `proguard-android-optimize.txt` plus the consumer ProGuard rules shipped with Compose, Room, DataStore, Coil, kotlinx.coroutines, and commonmark-java already cover almost all of Markleaf's surface. Adding broad `-keep class **` style rules would defeat the purpose of R8.
- The only rules we add are ones that document an undocumented coupling: Room entity field names (used by generated SQL + the SAF folder mirror frontmatter codec), `AppSettings` (legible in user-supplied stack traces), `SyncFrontmatter` / `NoteFolderMirror` (debugging round-trip of user-supplied `.md` files), `QuickNoteWidget` (manifest entry redundancy), kotlinx.coroutines volatile fields, and `assumenosideeffects` for `android.util.Log` calls.
- Every rule has a comment explaining *why* it exists, so a future maintainer can remove rules whose underlying coupling has gone away.

Mapping artifact:

- The R8 mapping file is uploaded as `markleaf-r8-mapping` on every CI build for short-term debugging, and as `markleaf-vX.Y.Z.mapping.txt` attached to the GitHub Release for long-term archival.
- This lets Play Console crash reports and any external bug report referencing a stack trace be deobfuscated against the exact mapping that shipped.

CI gates added:

- `./gradlew :app:lintRelease` runs on every push/PR; failure uploads `lint-results-release.html` for triage.
- `./gradlew :app:assembleRelease` runs on every push/PR and verifies the APK exists and is non-empty. This is a hard gate so R8 regressions cannot land on `main` undetected.
- The `launch-smoke` job stays `continue-on-error: true` against the debug APK — its emulator-flakiness predates this change and is out of scope here. A release-APK runtime smoke (against the R8-shrunk + debug-signed `benchmark` variant) is a planned follow-up.

Implications when R8 strips something at runtime:

- The fix is always to add a precise `-keep` rule in `app/proguard-rules.pro` with a comment explaining the coupling. Disabling R8 entirely or adding overly broad keep rules is not acceptable.

### D046 - Disable Android Auto Backup Entirely Via allowBackup="false"

Markleaf excludes its app data from Android Auto Backup and Device-to-Device transfer by setting `android:allowBackup="false"` on the `<application>` element of the main `AndroidManifest.xml`. No `android:dataExtractionRules` resource is introduced.

Why:

- Markleaf's user-facing promise is that data does not leave the device until the user explicitly exports, shares, opens an external link, or points the SAF folder mirror at a folder. An OS-level Auto Backup to the user's Google Drive happening silently in the background would conflict with that promise, even though it is technically a Google Account-bound copy.
- The conservative default for a local-first privacy app is to opt out of system backup entirely, then re-introduce a partial backup only if a clear product reason emerges.

Why allowBackup="false" instead of dataExtractionRules:

- `android:dataExtractionRules` (Android 12+) shines when you need to back up *some* paths and exclude others. Markleaf wants to exclude *everything*. In the all-or-nothing case, `allowBackup="false"` is shorter, applies uniformly across API 26..latest, and removes the legacy `fullBackupContent` vs new-rules duality.
- Managing both `dataExtractionRules` and `allowBackup` on minSdk=26 doubles the policy surface area (legacy device behaviour vs Android 12+ behaviour) for no extra precision in this case.
- If a future change demands fine-grained policy (e.g. back up user settings but never the note DB), `dataExtractionRules` can be reintroduced at that time.

Implications:

- Android 6 through 11 Google Drive Auto Backup never receives Markleaf data.
- Android 12+ Device-to-Device transfer also excludes Markleaf data (since `allowBackup="false"` opts out of both backup paths in those versions).
- Multi-device users move data via Markdown export, share sheet, or the v2.1 SAF folder mirror — all explicit OS dialogs.
- The `benchmark` build variant no longer overrides `allowBackup`; it inherits the main manifest's `false`. The `<profileable>` element stays since it is what Macrobenchmark actually needs.
- Privacy / Security / No-Cloud documents and the README all reflect this policy and the precise distinction between "Markleaf itself has no INTERNET permission" and "the user can move data via OS-mediated paths."

### D045 - Play Distribution Baseline Targets API 35

Markleaf Play Store distributions should target Android 15 (API level 35) or higher.

Implications:

- `compileSdk` and `targetSdk` are set to 35 for release submissions.
- Version progression continues monotonically for Play upload compatibility.
- Release validation includes signed AAB generation in addition to APK checks.

### D044 - Compose UI Tests Use An Isolated Debug Test Host

Device Compose UI tests should not depend on production `MainActivity` state or the persistent Room database.

Implications:

- UI test scenarios can mount the real `MarkleafNavHost` in a debug-only `TestHostActivity`.
- Activity-based UI tests use an in-memory Room database per test so notes, tags, and trash state do not leak between cases.
- AndroidX test runtime dependencies stay aligned with the tracing runtime used by the test runner.
- Production release behavior remains unchanged because the test host exists only in the debug source set.

### D043 - Editor Toolbar Icons Must Explain Ambiguous Actions

Ambiguous editor toolbar actions should combine distinct visual affordances with Material tooltips.

Implications:

- Markdown link keeps the common chain icon because it represents inline link insertion.
- Wiki link uses the local `[[ ]]` syntax as its visible affordance instead of reusing the same chain icon.
- Icon-only editor toolbar actions expose plain tooltips for long press and hover without adding network, account, or external service behavior.

### D042 - Release Automation Prefers Canonical APK Output Path

GitHub release automation should prefer the canonical AGP release APK path before falling back to broader discovery heuristics.

Implications:

- The common `app/build/outputs/apk/release/app-release.apk` path is treated as the first-class source for signing verification and asset upload.
- Fallback discovery remains available only for unexpected AGP layout differences.
- Release automation minimizes ambiguity between debug, release, and nested APK outputs.

### D041 - Release Verification Must Exclude Debug APKs

GitHub release automation should never let debug APKs participate in release signing verification or release asset upload.

Implications:

- APK selection in CI filters for release-specific filenames or directory segments.
- Release signing checks validate the production-signed artifact only.
- GitHub Release assets cannot accidentally ship a debug-signed build.

### D040 - Tag Release Gradle Flags Use Environment Properties

GitHub Actions tag release automation should pass required Gradle project properties through `ORG_GRADLE_PROJECT_*` environment variables when shell argument parsing proves unreliable.

Implications:

- Required release flags no longer depend on bash quoting or task/option ordering.
- The release job executes the intended task graph consistently across CI runners.
- Signing verification and asset upload can rely on the release task having actually run.

### D039 - Tag Release Gradle Invocation Must Use Native CLI Ordering

GitHub Actions tag release steps must pass Gradle project properties using the normal CLI ordering instead of quoted pseudo-task forms.

Implications:

- `-P...` flags appear before the target task path in release automation.
- The release job must execute the real `:app:assembleRelease` task rather than silently falling back to `:help` or malformed task parsing.
- Subsequent signing verification and asset upload only make sense after the real release task has run.

### D038 - Release Automation Searches The Full App Build Tree

GitHub release automation should search the full app build tree for APK outputs when narrower output directory assumptions fail in CI.

Implications:

- Release verification survives AGP output layout differences across runners and plugin behaviors.
- Release asset upload uses the same full-build-tree APK discovery as signing verification.
- The release workflow prefers a real produced APK over any directory-layout assumption.

### D037 - Release Automation Recursively Discovers APK Outputs

GitHub release automation should search the full release output tree for APK files instead of assuming the final APK sits directly in the top-level release directory.

Implications:

- Release verification tolerates AGP layouts that place the final APK in nested subdirectories.
- Asset upload uses the same recursively discovered APK path as certificate verification.
- Release automation remains resilient to output layout changes across CI environments.

### D036 - Release Automation Discovers The Built APK Directly

GitHub release automation should be able to find the produced release APK even when AGP does not emit auxiliary metadata files in CI.

Implications:

- Release signing verification and asset upload discover the first real `*.apk` inside the release output directory.
- Tag release automation no longer depends on `output-metadata.json` being present in the CI workspace.
- Release publication remains tied to the actual built APK file, not to a guessed filename or optional metadata sidecar.

### D035 - Release APK Path Comes From Output Metadata

GitHub release automation should resolve the produced APK path from Gradle output metadata instead of assuming a fixed filename.

Implications:

- Certificate verification and release asset upload both use the same metadata-derived APK path.
- AGP output filename changes do not silently break tag release publication.
- Release workflow validation remains tied to the actual built artifact, not a guessed path.

### D034 - Failed Public Releases Recover With A New Monotonic Version

When a public tag release fails before publishing correctly, Markleaf should recover with a new app version and tag instead of rewriting or reusing the failed tag.

Implications:

- Android `versionCode` remains strictly increasing even if a failed build artifact was shared or cached externally.
- Git tags and GitHub Releases preserve an auditable history of failed versus successful release attempts.
- Release workflow fixes should be validated on the next fresh tag release, not bypassed by a manual-only publication path.

### D033 - Backup Status Should Report Local Operation Counts

Backup and restore feedback should describe what happened locally, not only success or failure.

Implications:

- Settings shows note, attachment, and link counts after ZIP backup/restore.
- Failed backup/restore attempts use error styling and actionable copy.
- Status feedback remains local and does not add telemetry or network reporting.

### D032 - Backlinks Show Local Context

Backlinks should show the source note title and a short local snippet around the link occurrence.

Implications:

- The editor can explain why a note links here without opening the source note first.
- Snippets are generated from local Markdown content and do not require search indexing or network services.
- Existing backlink navigation stays unchanged: tapping a backlink opens the source note.

### D031 - Tag Counts Include Active Notes Only

Tag list counts should describe currently visible, active notes.

Implications:

- Trashed notes do not contribute to tag counts.
- Tag rows navigate to the existing local `#tag` search instead of introducing a separate tag detail route.
- The tag screen remains a lightweight index over local Room data.

### D030 - Markleaf Uses A Stable App Color Scheme By Default

Markleaf should use its own theme colors by default instead of letting Android dynamic color replace the core palette.

Implications:

- Note list titles and prominent navigation text can use predictable theme contrast across devices.
- Dynamic color can still be reintroduced later as an explicit setting if it is tested against all key surfaces.
- Pane backgrounds should pair a `colorScheme` surface role with the matching content role instead of alpha-blending unrelated surfaces.

### D029 - Search Screen As Quick Open

The existing Search screen is the quick-open surface for local navigation.

Implications:

- A single query can surface notes, tags, and wiki-link labels.
- Tags refine the current query to `#tag`.
- Resolved wiki links open the target note directly; unresolved labels refine the query.
- Quick-open remains fully local and uses existing Room/Flow sources.

### D028 - Empty States Should Offer Direct Next Actions

Empty states should guide the next useful local action instead of only describing absence.

Implications:

- The empty note list includes a direct create-note action in addition to the floating action button.
- The empty editor explains the local Markdown writing affordances without sending users to external help.
- Empty-state copy is covered by locale resource parity checks.

### D027 - Locale Resource Parity

Every supported UI locale must keep the same string resource keys as the default locale.

Implications:

- Default English, Korean, and Spanish resources are maintained through Android resource qualifiers.
- Starter notes are localized through raw resource qualifiers where a locale-specific file exists.
- A unit test verifies localized string key parity and starter note file availability.

### D026 - Indexed Search Path For Large Local Datasets

Markleaf should use indexed local database paths for large note collections.

Implications:

- Notes list/trash/title lookup paths have explicit SQLite indexes.
- Search uses the local FTS table with rowid-based joins instead of title-based joins or full LIKE scans.
- Search results are capped to 200 rows to keep UI rendering predictable on large local datasets.
- Large dataset regressions are covered by a 10,000 note repository search test.

### D025 - Bounded Local Note Snapshots

Note version history is stored locally in Room as bounded note snapshots.

Implications:

- Markleaf snapshots the previous note body before meaningful title/content/excerpt updates.
- Autosave does not create unlimited versions; snapshots are rate-limited to about one per five minutes and pruned to the latest 50 per note.
- Restoring a snapshot stores the current version first, then restores the selected local snapshot.
- Snapshots are deleted with their parent note through Room cascade behavior.

### D024 - Local Advanced Markdown Preview Scope

Advanced Markdown preview should stay local and dependency-light.

Implications:

- Tables are parsed and rendered directly in Compose.
- Inline `$...$` and display `$$...$$` math notation are shown as readable local math blocks/segments.
- A full KaTeX-compatible rendering engine is deferred until it can be added without network access, proprietary SDKs, or F-Droid compatibility risk.

### D023 - Fixed Production Signing Certificate

Release APKs must continue to use the same production signing certificate.

Certificate SHA-256 digest:

```text
0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a
```

Implications:

- Tag release builds require release signing values through `-Pmarkleaf.requireReleaseSigning=true`.
- GitHub Actions verifies the signed APK certificate digest before creating a GitHub Release.
- Replacing the production keystore is not allowed for normal releases because Android users would hit update conflicts.

### D022 - External Markdown Links Are Display-Only In MVP

Preview can recognize external Markdown links, but it must not automatically open web URLs in the MVP.

Implications:

- `[[Note Title]]` and `[label](Note Title)` are treated as local note navigation through the existing search flow.
- `http://` and `https://` targets are visually marked as links but do not launch a browser.
- Adding browser launching or remote content behavior requires a separate network/privacy review.

### D020 - Bear-Class Experience Without Brand Copying

Markleaf can intentionally pursue feature-level parity with high-quality Markdown note apps when the feature is a common product expectation.

Implications:

- Markdown editing, tags, wiki links, backlinks, search, export, backup, and polished phone/tablet flows are valid product goals.
- Bear's name, icon, brand identity, color system, exact layout, marketing copy, and distinctive visual expression must not be copied.
- The product bar is "Bear-class Android local Markdown experience," while the implementation and visual identity remain independent.
- MVP privacy constraints remain active: no INTERNET permission, no API, no login, no analytics, no ads, no proprietary crash reporting.

### D021 - First-Run Starter Notes

New empty installs should start with local starter notes instead of a blank note list.

Implications:

- Starter notes demonstrate Markdown, tags, wiki links, backup/export, and local-first privacy.
- Starter notes are seeded only when the local database is empty and the starter-notes flag has not been set.
- If a user deletes starter notes, app restart must not recreate them.
- Starter notes are regular local notes and can be edited, exported, backed up, or deleted by the user.

### D001 - Repository

Final repository:

```text
https://github.com/jeiel85/markleaf-android.git
```

### D002 - Application ID

Final Android application ID:

```text
com.markleaf.notes
```

Reason:

- `io.github.*`는 샘플 프로젝트처럼 보일 수 있다.
- `dev.*`는 개발용 빌드처럼 보일 수 있다.
- `com.markleaf.notes`는 운영 배포 앱에 적합하고 앱의 성격을 명확히 보여준다.

### D003 - Distribution Direction

Markleaf는 향후 F-Droid 배포가 가능할 정도의 오픈소스 친화성을 유지한다.

Implications:

- 공개 소스만으로 빌드 가능해야 한다.
- closed SDK를 피한다.
- private token이나 secret file 없이 빌드 가능해야 한다.
- 의존성 라이선스를 명확히 관리한다.

### D004 - MVP No-API Policy

MVP에서는 API 연동을 하지 않는다.

금지:

- 로그인
- 계정 시스템
- 클라우드 동기화
- 분석
- 광고
- 추적
- Firebase
- remote config
- proprietary crash reporting SDK
- backend API client
- `android.permission.INTERNET`

### D005 - Product Focus

Markleaf는 기능 많은 생산성 도구가 아니라 빠르고 예쁜 Markdown 메모 앱이다.

Priority:

1. 속도
2. 안정성
3. 디자인
4. 로컬 저장
5. 데이터 소유권
6. 태그 기반 정리

### D006 - Release Signing Secrets

릴리즈 키스토어와 서명 비밀번호는 저장소에 커밋하지 않는다.

Implications:

- 로컬 서명은 `release-signing.properties` 또는 환경변수로만 활성화한다.
- GitHub Actions는 `MARKLEAF_RELEASE_KEYSTORE_BASE64` 시크릿을 복원해 태그 릴리즈에서만 signed APK를 생성한다.
- 일반 PR/main 빌드는 debug 빌드와 테스트만 수행해 공개 소스 빌드 가능성을 유지한다.

---

## Resolved (Pending → Confirmed)

- **Hilt vs Koin vs manual DI**: 수동 의존성 주입 사용. 코드베이스 단순성 유지.
- **Minimum SDK**: API 26 (Android 8.0).
- **Markdown preview library**: commonmark-java 0.24.0 도입 완료 (v2.3.0). CommonMark 0.30 + GFM 확장.
- **SQLite FTS 도입 시점**: FTS4/FTS5 도입 완료 (v0.3.0, 검증은 Phase 8).
- **이미지 첨부 지원 여부**: Coil 기반 이미지 첨부 부활 (v2.5.0). SAF로 권한 없이 동작.
- **tablet two-pane layout 도입 시점**: 2-Pane 레이아웃 도입 완료 (v1.0.x).
- **Play Store / F-Droid flavor 분리**: 단일 flavor로 양쪽 대응. F-Droid 호환 의존성 정책 유지.

### D006 - Documentation Baseline Integration

템플릿 기반 운영 문서를 프로젝트 루트에 통합한다.

Implications:

- `CHANGELOG.md`를 사용자 영향 변경의 공식 요약 문서로 사용
- `HISTORY.md`를 작업/검증 이력 문서로 사용
- 기존 Markleaf 전용 정책(`AGENTS.md`, `docs/AGENT_SPEC.md`)을 우선 적용

### D007 - CI APK Verification Policy

CI 성공 판정에 APK 산출물 존재 및 다운로드 가능 여부를 포함한다.

Implications:

- `assembleDebug` 이후 `app-debug.apk` 파일 존재를 워크플로우에서 강제 검증
- Actions artifact 업로드 실패 시 CI 실패 처리
- 릴리즈 전 artifact 다운로드 가능 여부를 확인 가능하게 유지

### D008 - Actions Runtime Baseline

GitHub Actions 워크플로우는 Node 24 지원 버전을 기본으로 유지한다.

Implications:

- 공식 액션은 Node 24 지원 메이저를 우선 사용
- 전환기 동안 `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`를 설정해 런타임 경고를 선제 대응

### D009 - Tablet UX Priority and Adaptive Two-Pane Direction

태블릿 경험을 Later Versions의 최우선(P0)으로 지정한다.

Implications:

- `Medium/Expanded` 폭에서 리스트+에디터 2-pane 레이아웃을 우선 도입
- `Compact`는 기존 phone 네비게이션 UX를 유지
- 초기 도입은 최소 침습(기존 route 구조 보존) 방식으로 진행

### D010 - Basic Local Markdown Preview Adoption

Editor에 외부 네트워크/SDK 없이 동작하는 기본 Markdown Preview 모드를 도입한다.

Implications:

- 초기 범위는 heading, bullet, checkbox 중심의 lightweight preview
- 복잡한 markdown spec 전체 지원은 후속 이슈로 확장
- 로컬 우선/F-Droid 친화 원칙 유지

### D011 - SQLite FTS Phased Adoption

검색 품질/성능 향상을 위해 FTS를 단계적으로 도입한다.

Implications:

- 기존 LIKE 경로를 즉시 제거하지 않고 fallback 유지
- 스키마 변경은 additive migration으로 진행
- 실제 데이터셋 성능 측정 후 기본 경로 전환

### D012 - Local-First Image Attachment Scope

이미지 첨부는 로컬 저장 기반의 최소 범위로 도입한다.

Implications:

- 원격 업로드/동기화 없이 메타데이터+로컬 URI 참조 모델 사용
- 내보내기 시 markdown과 자산 파일 구조를 함께 정의
- 고급 미디어 편집은 후속 단계로 연기

### D013 - Wiki Links with ID-backed Resolution

`[[note links]]`는 표시 텍스트와 내부 식별자를 분리한 ID 기반으로 도입한다.

Implications:

- 제목 변경 시 링크 무결성 유지 가능
- 저장 시 링크 인덱스 재생성 필요
- 미해결/중복 제목 케이스에 대한 UX 제공 필요

### D014 - Manual Local Backup Strategy

백업은 사용자 주도형 로컬 백업/복원으로 시작한다.

Implications:

- 자동 원격 전송 없이 zip 패키지 기반 백업 제공
- 복원 전 dry-run/conflict preview 필요
- 포맷 버전 관리로 향후 호환성 유지

### D015 - Network Features Deferred

현재 제품 단계에서는 네트워크 기능 도입이 필요하지 않다.

Implications:

- `android.permission.INTERNET` 미사용 정책 유지
- 계정/원격 동기화/API 연동은 후속 재평가 전까지 보류
- 도입 재검토 시 개인정보/운영/배포 가드레일 선충족 필요

### D016 - Release Asset Policy

태그 릴리즈(`v*`) 시 signed release APK만 GitHub Release 자산에 자동 첨부한다.

Implications:

- Debug APK는 CI artifact로만 보관하고 GitHub Release 자산에는 올리지 않는다.
- Release page에서는 `markleaf-vX.Y.Z.apk` 파일명으로 signed APK를 직접 다운로드 가능하게 유지한다.

### D017 - Release Notes Source

GitHub Release 본문은 해당 버전의 `CHANGELOG.md` 섹션을 기준으로 작성한다.

Implications:

- 태그 릴리즈 workflow는 `--generate-notes`를 사용하지 않는다.
- `vX.Y.Z` 태그를 만들기 전에 `CHANGELOG.md`에 `## vX.Y.Z` 섹션이 있어야 한다.
- 릴리즈 본문이 비어 있거나 changelog 섹션을 찾지 못하면 workflow가 실패해야 한다.
- GitHub Release 본문에 들어가는 changelog 섹션은 한글로 작성한다.

### D018 - Release Title Source

GitHub Release 제목은 `CHANGELOG.md`의 해당 버전 heading에서 가져온다.

Format:

```text
## vX.Y.Z - 한국어 제목 (English Title) - YYYY-MM-DD
```

Implications:

- GitHub Release 제목은 trailing date를 제거한 `vX.Y.Z - 한국어 제목 (English Title)` 형식을 사용한다.
- Workflow에서 `Markleaf vX.Y.Z` 같은 고정 title을 사용하지 않는다.
- `v1.0.0 - 정식 출시 (First Major Release)` 형식을 이후 릴리즈에도 유지한다.

### D019 - No Destructive Migration After Release

릴리즈 이후 앱 DB는 `fallbackToDestructiveMigration()`을 사용하지 않는다.

Implications:

- 스키마 변경은 명시적 Room migration으로 처리한다.
- 메모 앱의 핵심 데이터인 노트, 태그, 링크는 앱 업데이트 때문에 삭제되면 안 된다.
- 오래된 개발 빌드와 호환이 불가능한 경우에도 정식 릴리즈 경로는 데이터 보존을 우선한다.
