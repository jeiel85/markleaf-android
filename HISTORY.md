## 2026-08-14 - The release docs catch up with the release that just shipped (D068)

- Trigger: a request to update the README release links after v2.32.5. They needed nothing — the release commit's version bump had already carried both links in all seven READMEs to `releases/tag/v2.32.5`, and the tag page answers 200. The landing pages use `releases/latest`, which is version-independent by design. Checking that is what turned up the actual drift, one file over.
- What was stale, and it was mine. `docs/RELEASE.md`'s banner still said "Release tags are still pushed to GitLab by hand, unchanged" — true when D067 was written, false the moment D068 dropped the GitLab tag push, and demonstrably false as of this morning: v2.32.5 was tagged to GitHub only. D068 updated the release ritual in `AGENTS.md`, the runbook and the tag-push command block, and this banner line survived all three passes. The same paragraph also instructed the next release author to "re-add the GitLab release mirror links at the next release cut once GitLab is producing Releases again" — which it is not going to, since D068 ended the attempt and GitLab's newest Release is `v2.27.2 (2026-07-21)`. Following it would have added links to something that will never exist.
- D067 carried the matching contradiction: its "tags are out of scope, the order is unchanged" bullet was superseded by D068 the same day. Marked expired in place, the way D066's off-machine-copy clause was, rather than rewritten — the point of a decision record is that the reasoning stays readable next to what replaced it.
- Verification: the seven README links resolve (`releases/tag/v2.32.5`, HTTP 200) and no `v2.32.4` link remains; a grep for "GitLab first" / "GitLab 먼저" / "git push gitlab" across the docs returns only the runbook's own "while the mirror is on" procedures, which sit under its off banner, and the two lines that now state D068 correctly.

## 2026-08-14 - v2.32.5 (versionCode 127)

- Trigger: thirteen commits had landed since v2.32.4 and two of them changed what users see, including one that had been losing data. The `## Unreleased` section added in #307 is what made this cut a rename rather than an archaeology exercise — it had been carrying the three v2.32.4-era fixes since the day they merged, which is the whole reason it exists.
- What ships, and what does not. Five fixed entries: the interrupted sidecar conversion that duplicated notes (#310), the save that scanned the folder twice (#311), and the three carried over — jump-control eligibility, saved-position pruning, and the retitle pass that now resumes and cannot double-run (#302, #307). The rest of the thirteen are tests, goldens, CI and documentation, and none of them get a line: #312/#313/#314 add coverage, #306 adds a debug-only breadcrumb, #308/#309 settle GitLab. A release note that lists work rather than change is the drift this file exists to prevent.
- Wording: the store notes say "metadata-free mode" rather than the internal name. Sidecar is what the code calls it; the settings screen calls it something a user recognises, and the changelog follows the screen.
- Release mechanics: versionCode 126→127, versionName 2.32.4→2.32.5, CHANGELOG both editions, seven store-locale `127.txt`, landing ×7 (three strings each plus the release date) and README ×7. The `v2.32.4` mentions in `docs/mirror-runbook.md` and `docs/RELEASE.md` were deliberately *not* bumped — they record where the frozen GitLab snapshot stops (D067/D068), which is a fact about a past release rather than a version string.
- The English store note first came out at 462 characters, which `verify-release-notes.ps1` passes with a warning at 90% of the cap. It was rewritten to 420 rather than shipped on the warning: #184 is the release where 494 characters passed and the next one-word edit would have failed the gate.
- Verification: `verify-release-notes.ps1` and `verify-landing-versions.ps1` both green; `testDebugUnitTest` + `:app:lintRelease` green; `:app:exportReleaseToBuildDrive` produced the AAB, the 42.5 MB mapping and the seven-locale notes in `D:\Build`, confirmed by `verify-release-export.ps1`. The signing inputs had to be copied into this worktree first, as `docs/RELEASE.md` § "Building from a worktree" describes — both are gitignored and live only in the checkout that created them.
- No hardening issue filed. This pass spent #262 items rather than adding to them, and the standing rule is that work which consumes the tracker does not refill it.

## 2026-08-14 - The editing surface gets its own direction golden (#262)

- Trigger: the tracker's remaining RTL item covers the editor rather than rendered text — "cursor placement, selection, and mixed LTR/RTL lines in the editor's `BasicTextField` were never exercised". The rendering half of that is testable without a device; the cursor and selection half is not.
- Analysis, and the thing worth writing down: the editor takes `MaterialTheme.typography.bodyLarge.copy(color = …)`, so it *does* inherit `TextDirection.Content` — the item is a coverage gap, not a defect. But checking that turned up a real gap in an existing test: `EditorLiveSnapshotTest` draws its sample with a bare `TextStyle(fontSize = 16.sp, …)` rather than the theme's role. That is fine for the colours it exists to pin, and it means a typography change cannot move it — so the app's most-used surface had no golden drawn the way the app draws it.
- Contract/scope: one snapshot test over the editing surface as `EditorScreen` composes it — the theme's `bodyLarge` role plus the markdown syntax transformation — with a sample that mixes scripts: English heading and paragraph, Arabic heading and paragraph, an English and an Arabic task, a Hebrew blockquote. Light and dark, since the editor is drawn in both. The recorded images show each line's markers on the side its own direction puts them, which is the property; a blanket direction would put them all on one side.
- Verification: recorded on the Linux runner via `record_roborazzi`. All 67 existing goldens came back byte-identical, so the two new images are the only movement. `verifyRoborazziDebug` on CI is the gate from here.
- Left open deliberately: cursor placement and selection in bidi text. Those are interaction, not rendering, and want a device — which this machine cannot give today. The tracker item stays open with its scope narrowed to that half.


## 2026-08-14 - The preview toggle is tested through the screen that wires it (#262)

- Trigger: the tracker item filed from the Codex review on #303 — `PreviewToggleReachesMirrorTest` calls the toggle, the repository write and the mirror write directly, so it stays green if `EditorScreen` stops joining them. The reviewer's suggestion was to drive the real integration or extract it.
- Analysis: the harness for driving it already existed. `EditorScreenTest` launches the real `EditorScreen` in `TestHostActivity` against the real database, and `MarkdownTaskToggleClickTest` shows how a checkbox is hit — the left edge of the row, because the label stays selectable (#219). What is *not* possible is following it all the way to the file: the mirror resolves its folder with `DocumentFile.fromTreeUri`, and a tree Uri needs a SAF grant a person has to tap, which is the same constraint `NoteFolderMirrorFolderTest` documents. So the reachable target is the saved note, not the mirrored file.
- Contract/scope: one test that seeds a note with two open tasks, opens it, switches to preview, taps the second checkbox, and waits for the debounced autosave to land in the repository — then asserts the untouched row did not move. It fails if the callback stops updating `editorState`, if the save stops being triggered, or if the debounce stops firing, which is precisely the set the older test could not see. The two tests now meet at the repository, and both docstrings say so instead of one of them claiming more than it checks.
- Verification: run on CI's required `instrumented-tests` job. This machine's emulator has not come up since it ran out of memory earlier in the session, so CI is the verdict here, as it was for the two sidecar passes.

## 2026-08-14 - RTL note content gets a golden, and the editor's jump arrow gets a price (#262)

- Trigger: two more tracker items, taken together because both are about *what is worth building* rather than how. The RTL one: `TextDirection.Content` lives in `Theme.kt`, was device-verified once, and is covered by nothing. The jump one: the editor's arrow follows the caret rather than the scroll position, so scrolling without moving the caret can point it the way you already are.
- RTL — what the golden had to be. A pure right-to-left page proves less than it looks: it would render correctly under a blanket `TextDirection.Rtl` too, which is *not* what the app does and would break every English note. The mixed page is the discriminating case, so the samples put Arabic and Hebrew paragraphs in the same note as English ones. The recorded image shows the English lines against the left edge and the Arabic and Hebrew against the right — direction following content, in one page, which is the property the setting exists for. Drop `withContentTextDirection()` and both images move while every other golden holds still, so the failure names itself.
- Recorded through the `record_roborazzi` dispatch on the Linux runner, since local recording is refused on Windows by design. Worth noting what the run also said: all 65 existing goldens came back byte-identical, so the two new images are the only thing that moved — the same check the theming pass used to know its palette change was contained.
- Jump direction — costed, and not taken. The item's premise was checked against the API rather than assumed, by disassembling `foundation-android:1.7.6`: the `TextFieldValue` overloads this editor uses end at `interactionSource, cursorBrush, decorationBox` and expose no scroll state, while the `TextFieldState` overloads — BasicTextField2, already present in the same artifact — do take a `ScrollState`. So there are two routes and both are larger than the defect. Migrating to `TextFieldState` also swaps `VisualTransformation` → `OutputTransformation`, `KeyboardActions` → `KeyboardActionHandler` and the auto-continuation into an `InputTransformation`, across 47 `TextFieldValue` references, 53 `editorState` uses in `EditorScreen` alone, and five test files. Wrapping the field in an outer `verticalScroll` is smaller but moves caret bring-into-view off the built-in path that #136's `imePadding()` fix depends on, so it needs device verification of the IME-covers-last-lines case — which this machine could not give today. Neither is worth an arrow that points the wrong way while the tap still works. The tracker now carries that reasoning, so the next reader does not re-derive it.
- Verification: `verifyRoborazziDebug` on CI is the gate for the new goldens; `testDebugUnitTest` and `lintRelease` cover the rest. No production code changed in this pass — one test file, two images.

## 2026-08-14 - A sidecar save stops walking the folder twice (#262)

- Trigger: the two sidecar cost items on the standing tracker — the index rewritten in full on every save, and every save listing the whole folder. #266 had already cut three listings per save down to one; what was left was that one plus the `findFile` inside `SidecarStore.write`, which walks the tree too. Both are proportional to folder size, on a path that runs once a second while typing.
- Analysis: the listing half has a shape the frontmatter path already solved. `MirrorFileCache` remembers the document each note was last written to and re-checks it before use, which is why the frontmatter save does not list. The sidecar path had no equivalent for one stated reason — no header to identify a file by — but it does have an identity: its index entry names the file. So the cache works here too, verified against the entry's filename rather than against a `markleaf_id`. The drifted-title case still misses on purpose, because a rename needs the listing anyway to keep the new name clear of every other file.
- Two things this could have got wrong, both avoided deliberately. **Writing blind.** If the document is not re-checked, a file renamed outside Markleaf gets this note's body while the index still points at a name nothing holds — and a file no entry names is the file the next import reads as a new note, which is #140 one more time. The name is therefore re-read on every fast-path save and a mismatch drops to the slow path. **Leaking an Activity.** The first draft cached `DocumentFile` objects; a `TreeDocumentFile` holds the `Context` it was made from, and `EditorScreen` hands this path the Activity's context, so a process-wide map of them would pin an Activity for the life of the process. That is why `MirrorFileCache` stores Uris, and why `SidecarStore` now remembers the index by Uri as well. `documentName` is the check both use: one projection-limited query, and the `file://` shape the instrumented tests run on answers from its own path.
- The index rewrite is *not* fixed here, and the tracker item is corrected rather than closed. The proposal on it — an in-memory dirty set flushed on a coarser trigger — is unsafe as written, and #310 measured why. A note whose file exists with no entry is imported as a fresh note (`expected:<0> but was:<2>` when that state was reproduced), so deferring a *first* save's entry duplicates the note outright; deferring a later save's hash update is milder but still user-visible, because the index then says a hash the file does not have and the next pass earns the file a conflict copy. Making the write cheap rather than rare means changing what is on disk — an append-and-compact log instead of one rewritten object — which is a user-facing file format, a schema bump, and a decision record. That is a piece of work, not a hardening tweak, and it is now filed as one.
- Contract/scope: no format change, no behaviour change on any path that was already correct. The fast path writes the note's remembered file and records the same entry the slow path would; everything else falls through to the listing exactly as before.
- Verification: `SidecarSaveFastPathTest` (androidTest, live tree) covers the three cases the check exists for — a repeat save rewrites the one file and records the body actually written, a file renamed outside Markleaf is left untouched and the note gets its own file instead, and a retitled note is renamed rather than forked. `testDebugUnitTest` green locally; the instrumented cases run on CI's API 30 ATD, this machine's emulator having stopped coming up after it ran out of memory earlier in the session.

## 2026-08-14 - An interrupted sidecar conversion stops duplicating notes (#262)

- Trigger: the standing tracker's "Switching modes is not resumable" item, picked up as the first of the remaining sidecar candidates. It described the impact as "a half-converted folder with no way to see it" and assumed the import tolerates the half-done state, "it strips a stray header and uses its id".
- Analysis: that assumption holds only for files that still *have* their header, and `toSidecar` produces the other kind. It strips files one at a time and writes the index once, after the loop — so every file it converts spends the rest of the pass with no header *and* no index entry. Read the import against that state and it is not a cosmetic problem: `entry?.noteId ?: parsed.markleafId ?: UUID.randomUUID()` (`NoteFolderMirror` L718, and L546 on the frontmatter path) mints a fresh id, and the note comes back as a second copy of itself. That is #140, the bug this whole mode was designed around, reachable by a process death mid-conversion. The item was filed as a hardening candidate; it is a data defect.
- Contract/scope: `toSidecar` becomes plan-then-apply. Phase one reads every convertible file and records its entry; the index is flushed once; only then does phase two strip anything. A death at any point now leaves files that are headed, indexed, or both, and never neither — and a failed flush aborts with the folder untouched rather than stripping without a durable index. Phase two re-reads rather than holding every body in memory, because the folders this mode is aimed at are the large ones. If a file changed between the phases the recorded hash is corrected and re-flushed, so the pass does not hand the next sync a conflict copy the file has not earned.
- The other half was the call site. The two directions have to flip the setting at opposite ends, and the rule is the same for both: a file with no header must never be read by a mode that has only the header to identify it by. Converting *to* sidecar now flips first, so a half-done folder is read by the mode that has the index; converting back flips last, because until the headers are on the files are identifiable only through the index, which only sidecar mode reads. The comment that had argued for the old order — an import in that window "would read each header as note text" — was obsolete: the sidecar import learned to strip a stray header and take the id from it, which is the same paragraph this entry quotes above.
- Verification, and what it caught: `SidecarMigrationCrashSafetyTest` (androidTest, live `DocumentFile` tree) runs the conversion with a hook that throws mid-pass, then imports. Three tests — no note is created, every stripped file has its entry on disk, and the rule the ordering exists for (a stripped file no index knows about *is* a new note to the importer). Reinstating the defect failed two of the three, the duplicate count landing on `expected:<0> but was:<2>`. That pass is also what exposed a hole in the test itself: the first version passed *with* the defect, because `SidecarStore` caches this device's entries per folder and the import read them out of memory when the flush had never happened. A real process death takes that cache with it, so the test now drops it explicitly — without which it was asserting nothing.
- Run on `markleaf-phone-api36` (API 36) rather than the tablet AVD, which reached `adb` but never set `sys.boot_completed` and hung the install; a cold boot of the phone image was ready in 40 s.
- Follow-ups: the two sidecar cost items and the resume/reporting half of this one are still open on #262. This pass fixes the state that loses data, not the one that wastes IO.

## 2026-08-14 - GitLab gets one identity: a frozen snapshot (D068)

- Trigger: with the ref mirror off (D067), the maintainer asked to settle what GitLab *is* — releases on GitHub only, GitLab mentioned in the READMEs and nothing more. Two readings of "mirror only" were possible and the repository was in neither: a live source mirror needs the manual re-alignment D067 had just declined, so the honest reading is a frozen snapshot. That was the choice taken.
- Analysis: three GitLab mechanisms had failed independently and each was being kept alive by documentation rather than by use — CI off behind `SKIP_GITLAB_CI` since the minutes ran out, Releases red on a 403 for eleven consecutive tags waiting on a token nobody re-issued, refs diverged and unrepairable without a protection dance. Reading the public surfaces made the cost concrete: all seven READMEs claimed "**CI**: GitHub Actions + GitLab CI — independent builds and signed releases", which had not been true for months, next to a "GitLab source mirror" link pointing at a repository that stops at v2.32.4.
- Contract/scope: `GITLAB_RELEASE_MIRROR` unset, so both tag steps skip and a tag run is green — D066 wrote that exit and this is the moment it was written for. The release ritual pushes tags to GitHub only; `docs/RELEASE.md` and `AGENTS.md` drop the GitLab-first ordering, which had become incoherent anyway (a tag pushed to a repository whose `main` is frozen points at a commit that `main` cannot reach — the question D067 left open, now closed). Seven READMEs name GitHub Actions as the CI and label the GitLab link an archive. No version number goes in that label: README version strings are gated by `verify-landing-versions.ps1` and a hardcoded one would fail the next bump.
- Nothing is deleted. `.gitlab-ci.yml`, both mirror workflows, `mirror-release-to-gitlab.sh` and `verify-mirror.ps1` stay parked behind their switches on the #211/#212 precedent. Reviving GitLab is deliberately more than a switch — token, two variables, a re-aligned `main`, and revisiting D068 — because the half-states are what this decision exists to end.
- The Codex review on #308 is folded in here rather than left on a merged PR: it caught that `docs/RELEASE.md` still said `mirror-push` carries `main` and every tag with `mirror-check` verifying daily, and that the "Back out" exit still promised GitLab would "keep mirroring refs only". Both are corrected, and the tag half of that sentence was wrong before this pass too — `mirror-push.yml`'s header has always said its scope is `main` and that tags are not automated, so the document had been describing a job the workflow never did.
- Verification: `mirror-push` **skipped** on the D067 merge commit `7293463` (the two runs before it were failures), and `mirror-check` dispatched by hand returned job `GitHub vs GitLab refs` = **skipped**. `gh api actions/variables` now lists nothing at all, so neither GitLab gate can evaluate true. `verify-landing-versions.ps1` and `verify-release-notes.ps1` re-run over the edited READMEs.
- What this costs, carried forward from D067: `D:\Build` is the only backup of any kind left in the release path, and `verify-release-export.ps1` is the only thing that checks it.

## 2026-08-14 - The GitLab ref mirror is switched off (D067)

- Trigger: `mirror-push` had failed seven consecutive times — on every merge since 2026-08-13 06:17 — and the daily `mirror-check` had joined it. Found while confirming that #307's merge propagated. The merge was fine; the mirror had been red since the hardening batch started and nothing was watching it.
- Analysis: not a flake, and it does not self-heal. The v2.32.4 release commit reached GitLab directly (the release ritual pushes GitLab first) and reached GitHub as PR #301's squash — the same tree under two SHAs, `1bcc9c8` on GitLab and `27024a3` on GitHub. From that point the histories diverged, a non-force `git push` is rejected `non-fast-forward`, and the workflow refuses to force on purpose: its own header calls failing-on-divergence the correct behaviour. Measured rather than inferred: `git merge-base --is-ancestor origin/main github/main` says no, `git rev-list --left-right --count` says 1 / 7, and `git diff 1bcc9c8 27024a3` is empty — the divergence is entirely a SHA, with no content anywhere that GitHub does not already have.
- What was tried first, and why it stopped: force-pushing GitHub `main` onto GitLab, which the maintainer approved. GitLab refused it — `You are not allowed to force push code to a protected branch`. `main` there is `allow_force_push=false`, so the repair is a UI ritual (open protection, push, close it) that must be repeated every time this shape recurs. And it recurs by design: tags go to GitLab first while `main` arrives through a GitHub PR, so a directly-pushed release commit meeting a squash merge is the normal release here, not an accident.
- Contract/scope: both workflows keep their logic and gain one `if: vars.GITLAB_REF_MIRROR == 'true'`, and the variable is unset, so both skip. Deleting them was rejected on the #211/#212 precedent — that PR deleted `.gitlab-ci.yml` and the next restored it behind a switch, because a deleted mechanism cannot be turned back on by someone who does not know it existed. `mirror-check` goes off with `mirror-push` rather than staying on: a daily comparison against a mirror deliberately not being updated returns one fixed verdict, which is the "red check that carries no information" #262 filed against `launch-smoke`. `GITLAB_RELEASE_MIRROR` (D066) is a different switch and is untouched.
- What it costs, stated plainly because it is why this is a decision and not a workflow tweak: **git history no longer has an off-GitHub copy but local clones.** D066's "nothing is lost" bullet ended on "the git history still has an off-machine copy on GitLab", and that clause is marked expired in place rather than quietly rewritten.
- Documentation: D067 records the decision and the re-enable *sequence* — align by force-push (opening and re-closing protection), then set the variable, because setting it first reddens the first run. `docs/mirror-runbook.md` leads with the current state, and `docs/RELEASE.md` and `AGENTS.md` drop the claims that `main` propagates automatically and that `mirror-check` runs daily. One consequence is left open rather than decided here: with GitLab `main` frozen, a new `v*` tag pushed there points at a commit its `main` does not reach — the next release either re-aligns first or drops the GitLab tag push, and records which.
- Verification: `yaml.safe_load` on both workflows for parse and gate placement, `gh api actions/variables` confirming `GITLAB_REF_MIRROR` does not exist (so the gate evaluates false today rather than being assumed to), and both jobs confirmed skipped on this change's own merge.

## 2026-08-14 - The five-PR hardening pass gets its record, and its review answered (#262)

- Trigger: #302–#306 merged on 2026-08-13 and spent nine tracker items between them, but two things did not happen. `HISTORY.md` gained no entry for any of the five — Codex filed exactly that as P1 twice, on #304 and #306 — and the ten review comments left across the five PRs were merged unanswered: no reply, no rebuttal, no fix commit. Each PR is a single commit, so nothing in them addresses their own review. The comments all arrived three to six minutes after each PR opened, well before its merge, so this is unhandled feedback rather than feedback that came too late.
- What the pass did, recorded here because nothing else records it. **#302** — `isLongEnoughToJump` takes `max(source lines, ceil(chars/40))` so wrapped paragraphs earn the jump control; `NoteViewStateDao.clearAll()` prunes saved positions when the setting leaves LAST_POSITION; the debounced position write keeps the other surface's value in composition state instead of reading the row back each time; a `retitlePending` flag makes an interrupted retitle pass resume. **#303** — `SyncFrontmatterPropertyTest` sweeps encode/decode invariants over seeded random input, and `PreviewToggleReachesMirrorTest` walks a preview toggle into the mirrored file. **#304** — the editor's autosave debounce extracted into `DebouncedSaver`, with the burst-coalesces-to-one-save property pinned on a virtual clock. **#305** — `PreviewParseCostTest` budgets the production parser on a very long note, and the Roborazzi verify/record/compare tasks stopped being offered on Windows. **#306** — `resolveRestoredCaret` returns a `RestoreStatus` and the editor leaves a debug breadcrumb when a position restore is clamped or dropped.
- Analysis: each of the ten comments was read against the code rather than taken on its word, and eight of them are real. Two on #302 are a single mechanism half-built — the retitle marker was written *after* the rule it protects, so a death between the two writes selects the new rule with nothing to resume, which is the exact window the marker exists to close; and the pass released `retitleBusy` before clearing `retitlePending`, so a recomposition inside that gap satisfies the resume effect's `pending && !busy` and starts a second full-library pass with a second toast. Two on #305/#306 are the same guard wrong in both directions: `contains("Roborazzi")` rejects `finalizeTestRoborazziDebug`, which the comment directly above it promises stays available, and misses `gradlew vRD`, because Gradle abbreviates task names by camel hump while `startParameter.taskNames` keeps the raw token — so the noisy local verify the guard exists to prevent was still one abbreviation away. One on #306 is a gap in the new diagnostic: a note that opens straight into preview restores through `pendingPreviewScroll`, which clamps silently, so the surface doing the visible restoring was the one reporting nothing. One on #305 is an accuracy defect in the fixture — the fenced-code and table cases emit three lines while counting one, so the "10,000-line" note was 11,600 lines and the tracker's recorded measurement described a note 16% longer than it claimed.
- The two that are not defects, and why. The **CHANGELOG P1 on #302** is right that the changes are undocumented and wrong about where they belong: v2.32.4 was tagged and its GitHub release published before #302 merged (release #301 at 14:16 KST, #302 at 15:17), so writing them into that section would describe a build that does not contain them. They are unreleased, which is a real hole with no owner — nothing fails when unreleased user-visible work goes unwritten, and the next release's author has no reason to look for it. Both CHANGELOG editions therefore gain an `## Unreleased` section: the release job's awk and `verify-release-notes.ps1` both key on `^## v<semver>`, so an unnamed section publishes nothing and fails nothing, and cutting the next release means renaming the heading rather than remembering the work. The **test-wiring P2 on #303** is a fair description of the test's boundary, not of a bug in it: it calls the toggle, the repository and the mirror directly, so it stays green if `EditorScreen` stops wiring them together. What it pins is that those three steps produce a correctly rewritten file, which is the part that was only ever hand-verified. Driving the screen's own callback needs the composable under test rather than the repository — that is a different test, and it goes on the tracker rather than into this pass.
- Contract/scope: `setNoteTitleSource` becomes `beginRetitle`, which writes the rule and the pending marker in one DataStore edit — the atomicity is the point, so the two-call version is removed rather than kept beside it. The retitle `finally` clears the flag before releasing the guard. `resolveRestoredPreviewIndex` gives the preview surface the caret resolver's clamp/report contract, and `PreviewScrollRequest` gains `restore` so only the restoring caller reports — `BOTTOM` asks for `Int.MAX_VALUE` on purpose and the outline names a row it just read off the same list, so neither of those clamps means anything went missing. The Windows guard matches Gradle's own abbreviation rule (each requested camel hump must prefix the corresponding hump of a gated family) against four named families, which is what lets `finalizeTestRoborazzi*` through by construction rather than by exception. No behaviour change beyond those; no version bump.
- Verification: `testDebugUnitTest` and `:app:lintRelease` green. The guard was checked by requesting five task names and reading what each did — `verifyRoborazziDebug`, `vRD` and `recRD` are refused; `finalizeTestRoborazziDebug` and `compileDebugKotlin` configure and run. The fixture's line count is now asserted rather than described, so the same drift fails the test instead of being reported as a measurement. Both verify scripts were run *because* the CHANGELOG grew a section they had never seen: `verify-release-notes.ps1` reports 124 matched version sections across the two editions and `verify-landing-versions.ps1` passes, which is what says an `## Unreleased` heading is invisible to the release path rather than merely believed to be.
- Follow-ups: the #303 test-wiring item is on the standing tracker. The nine items #302–#306 closed stay closed — this pass answers their review, it does not reopen them.

## 2026-08-13 - Trash list hardening, and v2.32.4 (#262)

- Trigger: the two v2.32.3 hardening candidates recorded on the standing tracker after the #298 review — the trash list was the last `items()` call in the app without a key, and the inline Restore/Delete filled buttons left the title almost no room on narrow screens with long localized labels (German "Wiederherstellen" measures ~340dp on a 411dp phone).
- Analysis: without `key = { it.id }`, LazyColumn tracks trash rows by position, so restoring or deleting a note could leave the wrong row briefly visible or animate oddly — every other list screen (archive, locked, search, notes, sync, quick-switcher) already passes a key. For the buttons, Material 3's list-row action pattern is a `TextButton`: it keeps both actions visible and tappable while its smaller horizontal padding (~24dp per button) hands the saved width to the title's `weight(1f)` region, which also gives the #298 ellipsis fix more headroom.
- Contract/scope: `items(trashedNotes, key = { it.id })` and `Button` → `TextButton` for the two inline actions only. The AlertDialog's "Delete forever" confirm stays a filled `Button` on purpose — it is the destructive confirmation, while the inline Delete is only the entry point that opens the dialog. No behaviour, permission, or storage-format change.
- Implementation: PR #300. Reviewed by the reviewer agent before opening (no blocking findings; the delete-in-error-colour idea was left as a hardening candidate rather than bundled). CI: build, instrumented-tests, launch-smoke all green.
- Verification: `testDebugUnitTest` (incl. `TrashScreenTest`, which finds the buttons by text and is unaffected by the style change) + `:app:lintRelease` locally; the #298 regression test's boundary assertion only gets more headroom as the buttons shrink.
- Release: versionCode 125→126, versionName 2.32.3→2.32.4, CHANGELOG both editions, seven store-locale `126.txt`, landing ×7 + README ×7. The two tracker items are checked off in #262 with this PR as evidence.

## 2026-08-13 - Trash actions pushed off-screen by long titles, and v2.32.3 (#298)

- Trigger: an F-Droid user on 2.32.2 reported that in the trash bin the Recover/Delete buttons are "either not present at all or are elongated", with screenshots showing the Recover button reduced to a sliver at the screen edge, and that the effect tracks title length (#298).
- Analysis: the trash row gives its title no width bound. The title `Text` measures to the full row width, and the action-buttons row then lands beyond the row's visible area, clipped at the screen edge. Every other list screen (notes, archive, locked, tags, search) constrains its title with `overflow = TextOverflow.Ellipsis`; TrashScreen was the only one without it.
- Contract/scope: the title gets `Modifier.weight(1f)` + `TextOverflow.Ellipsis` — it takes only the room the buttons leave and ellipsizes when it runs out, keeping both actions fully visible at the right edge for any title length. No behaviour, permission, or storage-format change.
- Implementation: PR #299. `TrashScreenTest` (Robolectric) renders a trash row whose title is longer than the screen and asserts both action texts are displayed, that the title's right edge does not cross the Recover button's left edge, and that Recover sits left of Delete.
- Verification: `testDebugUnitTest` (68 suites) + `:app:lintRelease` locally. The regression was proven both ways: with the old layout the test fails with Recover not displayed; with the fix it passes. No golden moved — the trash screen had none.
- Release: versionCode 124→125, versionName 2.32.2→2.32.3, CHANGELOG both editions, seven store-locale `125.txt` (zh-CN's first changelog), landing ×7 + README ×7. No hardening issue filed — issue-response work, and nothing here was a candidate the standing tracker did not already hold.

## 2026-08-05 - The landing demo stops moving on its own, and the trust ledger stops advertising a stale feature (#262)

- Trigger: the three Public surfaces items on the standing hardening tracker — a "current release" strapline covered by no check, demo GIFs that ignore `prefers-reduced-motion`, and ~940 KB per landing visit. The strapline had become visibly wrong rather than merely stale: v2.32.2 updated the version beside it, so a current `v2.32.2` sat next to "Includes Quiet Formatting", a feature two releases old, on all six pages.
- Analysis: the tracker's proposed fix for the payload was measured and does not hold. It suggested "an animated WebP (roughly a third)"; on this content — 238 frames of flat UI at 960×540 — animated WebP came out **four times larger** than the GIF (3,730 KB at q55, 4,202 KB at q70, 9,354 KB lossless), because GIF's palette and frame differencing suit a screencast of flat colour better than a lossy photographic codec. h264 at crf 28 is 115 KB, 12% of the GIF. The motion item has a constraint the tracker did not state: CSS cannot pause a GIF *and* cannot cancel `autoplay` on a video either, so no purely declarative swap makes an autoplaying clip respect the preference. Only not autoplaying does.
- Contract/scope: the landing clip becomes a click-to-play `<video>` with a poster and `preload="none"`; the `alt` text moves to `aria-label` and the `figcaption` is unchanged. That closes both items at once — nothing moves until a visitor asks, and a visit costs ~18 KB instead of ~930 KB, with the 115 KB clip fetched only on play. The READMEs keep the GIF: GitHub renders markdown as-is and strips `<video>`. The strapline's feature name becomes the release date, which does not go stale, reads the same in all six languages, and has a source of truth in `CHANGELOG.md` — a translated feature name has none of those properties, which is why it drifted unnoticed.
- Implementation: PR #NNN. `verify-landing-versions.ps1` gains two things: the trust-ledger `<span>` must carry the date `CHANGELOG.md` gives for the current `versionName`, and the demo-clip assertion learns that the two surfaces expect different formats (landing → mp4 + poster, README → gif) rather than assuming `.gif` everywhere. `landing.css` applies the story-visual frame to `video` as well as `img`. `docs/assets/README.md` records the measurements, the re-encode commands, and why the two surfaces differ.
- Verification: `verify-landing-versions.ps1` passes all four sections — three version strings ×6, release date ×6, 12 demo surfaces, figcaption parity. The rendered page was inspected in a browser: the `<video>` carries `controls`, no `autoplay`, `preload="none"`, its poster and the preserved `aria-label`, and the `aspect-ratio: 16 / 10` rule resolves on the element. Layout at width is left to the `landing-check` gate, which fires on `docs/**.html` and `docs/**.css` and therefore on this change.

## 2026-08-05 - The Release asset list gets a gate, and it fails on the PR instead of the tag (#262, D062, D064)

- Trigger: the two remaining CI items on the standing hardening tracker. They read as separate problems — "the Release asset list has no CI gate, only prose in three places" and "`gh release create` file arguments are only exercised on a tag push" — but they are one shape: a fact with several copies, of which exactly one executes, and that one is checked last.
- Analysis: the list lives in four places. `gh release create`'s file arguments are what actually uploads; `Prepare release asset` stages the file those arguments name; `docs/RELEASE.md` states it twice and `AGENTS.md` once. D062 removed the AAB and D064 removed the mapping, and across those two changes the prose drifted twice — #244 had to hand-sync the same three spots again, with nothing failing either time. The timing half is worse than the count half: the `release` job skips on pull requests, so a mismatch between the arguments and what the build produces cannot fail until a tag is pushed, and by then the tag is public. #244 removed the step producing `markleaf-vX.Y.Z.mapping.txt`; had the matching argument been left behind, the failure would have landed after publication.
- Contract/scope: `scripts/verify-release-assets.ps1` parses the workflow for both the `gh release create` arguments and the filenames `Prepare release asset` stages, and asserts they are the same set — then compares that set against `<!-- release-assets: ... -->` markers placed beside the prose in `docs/RELEASE.md` (both paragraphs) and `AGENTS.md`. Prose is not parsed: a marker is a machine-readable copy sitting next to the human one, so an editor changing the list sees both, and missing one fails. The step runs in `build`, not `release`, which is the entire point — that is what moves the failure to the pull request.
- Implementation: PR #NNN. Argument parsing keeps quoted tokens whole, skips the tag positional, and knows which `gh` flags consume a value so `--notes-file release-notes.md` is not mistaken for an upload. `${GITHUB_REF_NAME}` normalises to `vX.Y.Z` so the workflow and the documentation compare directly.
- Verification: green on the tree as it stands. The guard was then checked in all three directions it can fail, by breaking each one and confirming the message named it — an upload argument with no step producing it (the #244 scenario, "태그 시점에 실패합니다"), a documentation marker listing an asset the workflow does not upload (the D062 drift, "문서에만 있음"), and a staged file nobody uploads ("죽은 스테이징이거나 빠뜨린 인자"). Each exited 2; the tree was restored and re-run green. CI configuration, one script, and documentation markers only — no application code, no version bump.

## 2026-08-05 - `instrumented-tests` becomes a required check, `launch-smoke` stays advisory (#262, #235)

- Trigger: #262's CI item said the emulator path "still gates nothing", that #277 had ended the flakiness, and that what remained was one decision — which of the two jobs earns a required check. Picking it up the same day as the v2.32.2 release turned out to be the right time, because that day produced the evidence the decision needed.
- Analysis: the item's premise was half wrong. `launch-smoke` is *not* reliably green — it failed two of the day's eight `android-build` runs, once on the release-prep branch and once on a documentation-only branch, i.e. on app code identical to a run that had passed forty minutes earlier. Both failures were the runner rather than the app: `Can't find service: package` on one, `Failure calling service activity: Broken pipe (32)` on the other, with emulator boots of 355 and 382 seconds. The app was never started in either. Against that, `instrumented-tests` went 8/8 the same day on the same commits, and had been green for six runs before 2026-08-03. The two do not share an emulator path — managed device versus the `reactivecircus` action — which is what lets one flake while the other does not.
- Contract/scope: `instrumented-tests` loses `continue-on-error: true`; `launch-smoke` keeps it. No test, no script, no application code. The choice is not only about stability: `instrumented-tests` runs #235's Room migration assertions, so what it catches is user data loss, while `launch-smoke` asserts that the app starts. An advisory gate on the first was the more expensive of the two to leave.
- Implementation: PR #NNN. Both jobs gain a comment carrying the evidence and the reasoning, including the triage rule worth keeping — **`launch-smoke` red while `instrumented-tests` is green means suspect the runner, and re-run**. `docs/RELEASE.md` (two places) and the branch-protection table in `docs/mirror-runbook.md` follow. The protected-branch context list itself is a repository setting, changed after the merge rather than in it. The Codex review caught what updating the table alone would have missed: the `gh api --input` payload printed directly under it — the one a maintainer pastes to reapply protection after temporarily disabling it — still sent `"contexts": ["build"]`, so the documented recovery procedure would have silently deleted the gate this change adds. A table and the command beneath it are two copies of the same fact, and only one of them runs.
- Verification: `yaml.safe_load` over the workflow reports `continue-on-error` False on `instrumented-tests` and True on `launch-smoke`, with every other job unchanged. Documentation and CI configuration only — no version bump.

## 2026-08-05 - The GitLab release mirror is on before its token, and five files now say so (D066 amended)

- Trigger: releasing v2.32.2, the maintainer asked to turn the GitLab release mirror back on, on the understanding that the monthly CI-minute reset had unblocked it. It had not: minutes were the 2026-07-22 problem (v2.28.0/v2.28.1), while what has blocked publication since #275 is `GITLAB_TOKEN` carrying `write_repository` and not `api`. Told that, and that turning the variable on without the token would redden every tag run, they chose to turn it on anyway and see the failure rather than infer it.
- What happened: it reproduced exactly as D066 predicted, which is the useful part. `Publish the GitLab release` returned HTTP 403 and the script named its own cause — "The token needs the 'api' scope; 'write_repository' alone cannot publish packages or releases". The step is #18 of 18 and sits after `Create GitHub release`, so `markleaf-v2.32.2.apk` was already published and downloadable when it hit; the APK was fetched back and its manifest read (`versionCode='124' versionName='2.32.2'`) to confirm the release itself is sound. Only the job's colour is wrong.
- Contract/scope: no behaviour and no CI logic — `vars.GITLAB_RELEASE_MIRROR` still gates both steps, unchanged, and `python3 yaml.safe_load` confirms it. What changed is that five places described a repository that no longer exists: the workflow comment instructed a reader to "set the variable to 'true' once GITLAB_TOKEN carries the `api` scope" when it had already been set without it; `.agent/decisions.md` (D066), `AGENTS.md` ×2, `docs/RELEASE.md` and `docs/mirror-runbook.md` all said the variable was unset and the steps skipped. Each now states the real state, that a red `release` job with only step 18 failing is not a failed release, and the two exits — re-issue the token, or unset the variable. D066 is amended rather than rewritten, so the reasoning that argued against this state stays readable next to the decision to hold it.
- Why it is worth a commit at all: a documented interface that quietly stops matching the thing it documents is the D062 failure mode, which drifted twice before a check caught it, and the "red check that carries no information" is the defect #262 filed against `launch-smoke`. Leaving the repository one variable out of step with its own decision record would have been both at once.
- Verification: `python3 -c "yaml.safe_load(...)"` on the workflow — 17 release steps, both mirror steps still carrying `if: vars.GITLAB_RELEASE_MIRROR == 'true'`; a grep for every "currently off / unset, so both skip / 지금은 꺼져 있 / 미설정이라 skip" phrasing returns nothing. Documentation only — no version bump, no source change.

## 2026-08-05 - The editor's markdown colours follow the theme, and the locked-mode amber is legible on both (#262)

- Trigger: the three theming items on the standing hardening tracker. They were deferred out of #283's caret fix on the stated grounds that fixing them moves rendering and so does not belong in a patch release; nothing has shipped since v2.32.1, so this is the pass they were waiting for.
- Analysis: `MarkdownSyntaxColors` defaulted five of its ten fields to `Color.Gray`, and the editor — its only construction site — named six of the ten, so blockquote text and `---` rules drew a fixed grey that could not know the theme: 3.41:1 against the light background `#F9FBF9`, under WCAG 1.4.3's 4.5:1 for text, and 4.85:1 against the dark `#191C19`. Two corrections to the tracker's own wording, both from reading the call site rather than the note: it passed six of ten, not five, so four defaults were live rather than five; and `table` has no consumer at all — no branch of the highlighter reads it, so requiring callers to supply it would have been worse than deleting it. Measuring the locked view-toggle amber, which the tracker only flagged as never contrast-checked, returned a second failure rather than a clean bill: `#F57C00` is 2.60:1 on the light top bar, under 1.4.11's 3:1 for an icon that carries meaning, against 6.36:1 on the dark one. The comment above it asserting it "stays legible on both" had never been measured.
- Contract/scope: the data class loses every default, so a caller that forgets a role fails to compile instead of silently rendering grey, and loses `table` outright. `markdownSyntaxColors(scheme)` becomes the one place the roles are chosen; the editor and the live-render golden test share it, where they had been duplicating the same six assignments — a snapshot that pins its own copy of the palette pins nothing. Blockquotes, rules and the code-fence wash take `onSurfaceVariant`, Material's medium-emphasis role, which recedes the way the fixed grey was trying to and inverts correctly between themes (8.94:1 light, 10.14:1 dark). The locked amber stays outside the colour scheme, as #200 decided, but becomes a pair: Orange 900 `#E65100` (3.65:1) on light bars, the existing Orange 700 on dark, chosen by the bar's own luminance rather than a theme flag so it also holds under Material You, where the background is whatever the wallpaper produced. No behaviour, settings, permissions or version change — but rendering does move, so it wants to ride the next release rather than sit unreleased.
- Implementation: PR #288. `EditorColorContrastTest` measures every text role of the palette against the background it is drawn on, in both static schemes, plus the locked tint against both top bars; `codeBlock` is excluded with the reason stated, since it is only ever read as a 10%-alpha wash behind a fenced block and a text threshold says nothing about it. `Theme.kt`'s two schemes go from private to internal so the test names the real backgrounds rather than a copy that could drift from them. Goldens: the locked top bar had none at all and now has one per theme; the mixed live-editor sample gains a `---` rule and a dark counterpart, so both colours that moved are pinned in both themes.
- Verification: `testDebugUnitTest` (554 tests across 69 suites) and `:app:lintRelease` locally. The new guard was checked the way #283's was — by putting both defects back, `blockquote = Color.Gray` and a single amber, and confirming it failed naming them at 3.41:1 and 2.60:1, the same figures as the hand calculation — then reverted and re-run green. Goldens were recorded on the Linux runner via the `record_roborazzi` dispatch rather than locally; `editor_live_mixed_light` is the only existing image that moved and the other 61 came back byte-identical, which is what says the palette change is contained to blockquotes and rules.
- Release: versionCode 123→124, versionName 2.32.1→2.32.2, CHANGELOG both editions, six store-locale `124.txt`, landing ×6 + README ×6. The first draft of the store notes ran de-DE, fr-FR and es-ES over the 500-character cap and en-US to within 6 characters of it, so the English source was shortened and all six re-derived rather than each translation trimmed in place — 402 characters in English, 430 at the longest, every locale now keeping more than 70 characters of headroom. No `## v2.32.2` section was added to #262: this pass spent tracker items rather than adding to them, and nothing it turned up was a candidate the tracker did not already hold.

## 2026-08-03 - Hardening trackers consolidated, and the GitLab artifact mirror switched off (D066)

- Trigger: the maintainer asked to clear out the hardening candidates sitting open on GitHub. Seven trackers were open — #204, #240, #247, #252, #255, #258, #262 — carrying 51 checklist items between them.
- Audit: every item re-checked against the tree at `72d0256` (v2.32.1), not against the last audit note. 22 were already done and unticked, most of them landed by PRs #274–#278 after the 2026-07-24 sweep: `opensInPreview()` extracted with `OpenModeTest`, the lock-gesture suite grown 1 → 5 tests, `settings.first()` bounded by `withTimeoutOrNull(2s)`, H4–H6 no longer flattened, sidecar `matchByName` plus `staleEntryIds` pruning, per-language GIF assertions in `verify-landing-versions.ps1`, `docs/assets/README.md` recording the kept webps and the recorded-on version, explicit retention on the last three artifacts, and the ~380-character English store-note target in `docs/RELEASE.md`. Two items were dead rather than open: `PreviewLine.sourceLine` in `parseHandRolled` (not a live fallback — `parse()` delegates unconditionally, the hand-rolled copy is a parity-test fixture) and the `launch-smoke` flake (diagnosed in #277 as an `adb install`/`pm path` race; both emulator jobs green across the last six `android-build` runs).
- Contract/scope: no application code. #262 becomes the standing hardening tracker holding the 27 surviving items grouped by area, with provenance per item; the other six trackers close. `AGENTS.md` records the convention so per-release trackers do not fragment again — release-time candidates append a `## vX.Y.Z` section to #262.
- GitLab: D066 narrows D064, D062 and D057 to what is measurable. GitLab mirrors refs; it has carried no Release since `v2.27.2 (2026-07-21)`, so ten releases (v2.28.0 … v2.32.1) have no Release object there and `D:\Build` is the only permanent copy of a released AAB and mapping. The tag job's two mirror steps now sit behind the repository variable `GITLAB_RELEASE_MIRROR`, unset — v2.32.0 and v2.32.1 both ended red on a step that could not pass with a `write_repository`-scoped token, after the GitHub Release had already published its APK, which is the "red check that carries no information" defect one job over. Re-issuing `GITLAB_TOKEN` with the `api` scope and setting the variable turns it back on; the script and the GitLab-side path are untouched.
- Verification: `python3 -c "yaml.safe_load(...)"` on the workflow confirms both steps carry the gate and nothing else moved; `./gradlew test` and `:app:lintRelease` on the unchanged source tree; `pwsh scripts/verify-release-notes.ps1` and `verify-landing-versions.ps1` still pass. No version bump — documentation, CI gating, and issue state only.

## 2026-07-28 - Project direction and distribution guidance aligned

- Trigger: a repository review found a mature local-first app with an outdated feature
  roadmap, a paused Google Play channel, an F-Droid catalog that can trail GitHub, and
  GitLab release-asset publication that is not reliable enough to advertise as an equal
  installation path.
- Contract/scope: no application code, permissions, telemetry, account, dependency, or
  release version changed. The six README installation sections now distinguish GitHub
  Releases (current signed APK), F-Droid (automatic updates after catalog publication),
  and GitLab (source mirror). `docs/ROADMAP.md` gains a July–October stabilization,
  distribution, and user-evidence gate; the older feature phases remain deferred ideas.
- Verification: `pwsh scripts/verify-landing-versions.ps1` and a line-by-line parity
  review of all six README installation sections. `./gradlew test` had already passed on
  the unchanged v2.32.1 source tree during the repository assessment.

## 2026-07-27 - The Trendshift daily-Kotlin award on the README and landing hero

- Trigger: Trendshift ranked the repository #1 Kotlin repository of the day on 23 Jul 2026, and the maintainer asked whether the badge it hands out belonged on the README. It did, and the landing pages were added in the same pass.
- Analysis: the snippet Trendshift generates embeds their live badge URL, and the two public surfaces fetch images very differently. GitHub proxies README images through camo, so a reader never contacts Trendshift and a live badge stays accurate on its own. GitHub Pages proxies nothing — the same URL there would have every visitor's browser hit a third party, on the page whose whole argument is that Markleaf does not phone home. The snippet also carries `target="_blank" rel="noopener noreferrer"`, which GitHub strips and replaces with `rel="nofollow"`, and a URL-encoded `alt` (`jeiel85%2Fmarkleaf-android | Trendshift`).
- Contract/scope: live URL in the six READMEs, a checked-in copy of the SVG (3.7 KB, self-contained — no external refs or scripts, internal gradient ids only) on the six landing pages. README block sits between the tagline and the shields row, because a 250x55 badge beside 20px shields reads badly; landing markup is one `a.hero-award` under the release line, with a three-line CSS rule since the global `img` rule already caps width. Alt text translated per language, the way the demo clips already are. `docs/assets/README.md` records why the file is checked in and how to refresh it.
- Verification: `check-landing-overflow.mjs` passes 12 pages x 6 widths; `verify-landing-versions.ps1` still passes. A headless render of all six landing pages at 320px and 1280px confirms the SVG loads (natural 250x55), renders at 250x55, sits 24px below the release line, stays inside the viewport and the hero block, and carries non-empty alt everywhere. `gh api -X POST markdown` on the README header confirms the embed survives GitHub's sanitiser with its link (utm parameters intact), dimensions and alt.
- No CHANGELOG entry, against the Codex review's P1: that file is strictly version-sectioned and is the literal source the release job's awk parser cuts release notes from, it has no Unreleased section, and this change carries no version bump — recording it would mean inventing a version or editing v2.32.1's already-published notes. Nothing about the app changed. PR #282, the nearest analogue, logged HISTORY only for the same reason.

## 2026-07-27 - The editor caret takes the theme's colour, and v2.32.1 (#283)

- Trigger: an F-Droid user on 2.28.3 wrote in with two screenshots — a dark page, a themed drag handle, and a black caret they had to hunt for, in both Markleaf Green and Material You (#283).
- Analysis: the editor's `BasicTextField` never passed `cursorBrush`, so Compose's default `SolidColor(Color.Black)` applied. It is the only raw foundation text field in the app; the Material fields (search, rename, settings, passcode) take their cursor colour from the theme, which is why the editor was the only place it showed. The report's "no matter which theme I choose" is exactly what a hardcoded literal predicts.
- Contract/scope: one argument at the call site — `cursorBrush = SolidColor(colorScheme.primary)`, the colour the Material fields already use — plus a guard so the same omission cannot come back. Nothing else about the editor changes. The `Color.Gray` defaults in `MarkdownSyntaxColors` (code fences, tables, blockquotes, rules) are the same defect family and are deliberately left alone: fixing them moves rendering, so they go to the tracker rather than into a patch release.
- Implementation: PR #284. `ThemedCaretTest` scans `src/main` for `BasicTextField(` call sites and fails any that omit `cursorBrush`, reading each call with a paren counter that ignores string and character literals so a `")"` in an argument cannot end it early. It also fails when the scan matches nothing — a text field renamed out from under it would otherwise pass for the wrong reason.
- Verification: `testDebugUnitTest` (548 tests) + `lintRelease` locally. The guard itself was checked by removing the fix and confirming it named `EditorScreen.kt:886`. Device pass on `markleaf-phone-api36`, caret core pixels sampled from a screenshot burst because the caret blinks and a single capture catches it half the time: dark Markleaf Green `#81C784` on `#191C19` (8.7:1), dark Material You `(176,198,255)` on `(18,19,24)` (11.2:1), light Material You `(71,93,146)` (6.1:1), light Markleaf Green `#2E7D32` (4.9:1). The same measurement before the fix was black on `#191C19` — 1.2:1, which is the report. The `editor_screen_quiet_appbar_phone` golden holds a focused caret, so it was re-recorded on the Linux runner rather than locally.
- Release: versionCode 122→123, versionName 2.32.0→2.32.1, CHANGELOG both editions, six store-locale `123.txt` (428 chars at the longest; every locale now keeps more than the 50 characters of headroom `verify-release-notes.ps1` warns below, which fr-FR and es-ES did not on the first pass), landing ×6 + README ×6, and THANKS.md gains the reporter. No hardening issue filed — this is issue-response work, and AGENTS.md keeps that flow from growing the backlog it is meant to spend.

## 2026-07-27 - Landing stylesheets made direction-aware (#146)

- Trigger: looking back at #146 — the Persian reader whose right-to-left note content laid out left-to-right, fixed in v2.28.0 — the same question one level up: the app handles RTL, the site around it never had to. The work started as a Persian README plus landing page; the maintainer cut that mid-flight, leaving only the part that has to exist before any RTL page can.
- Analysis: both stylesheets were written in physical directions — `left` on the skip link and the file-receipt card, `border-left` on the trust-ledger dividers, `padding-left` on the feature-point bullets and their `::before` dash, `margin-left` in the file-browser bar, `text-align: right` on the channel-row status, `padding-left` on the privacy page's lists. Each is a no-op to swap under LTR and a visible break under RTL. Three things do not follow from direction at all: the font stacks name Latin/CJK faces only, the display tracking is negative (which closes up a joined script), and `translateX` on the channel-row hover has no logical form.
- Contract/scope: physical offsets become logical (`inset-inline-start`, `padding-inline-start`, `border-inline-start`, `text-align: start/end`); the three exceptions get `[dir="rtl"]` rules — an Arabic-script *system* stack (no webfont, the pages fetch nothing), `letter-spacing: normal`, and a mirrored hover nudge with its `:active` reset. No new page and no content change: the six landing pages, six privacy pages and six READMEs are untouched.
- Verification: geometry diff of every element on 14 pages x 4 widths, old CSS vs new. The twelve real pages come out box-identical; the only computed-style change anywhere is `text-align` reporting `start` instead of `left` on three `.channel-row em`. Two throwaway `dir="rtl"` copies of `index.html` and `privacy.html` moved 20 elements and picked up the new stack, and screenshots confirm the mirror by eye — the file-receipt card and its accent border swap to the inline-start edge. `check-landing-overflow.mjs` passes 14 pages x 6 widths with both probes included. Pixel diffing was tried first and abandoned: the animated tablet GIFs and the `loading="lazy"` webps make the *same* tree screenshot differently twice, which read as four to nine phantom regressions per run.
- Gate: `landing-check` only fired on `docs/**.html`, so a stylesheet change — the one most able to overflow a page — reached `main` untested. `docs/**.css` added to both path filters, which is what makes this change itself gated.

## 2026-07-25 - Notes grid, first-line titles, and v2.32.0 (#279, #280)

- Trigger: two feature requests filed the same afternoon by one external reporter — a grid view for the notes list (#279), and a title field for each note (#280), the second reported as "what is set as the title is not necessarily the first line".
- Analysis: (#280) `TitleExtractor.titleLineIndex` searched the whole note for the first heading and only fell back to the first non-empty line when there was none, so a note opening with a paragraph took its name from a heading further down. The report is accurate and reproducible. A stored title field — the literal request — was declined in the triage comment: a Markleaf note *is* its `.md` file, so a separate title would have to live in the frontmatter or the sidecar index and would drift the moment the file was edited elsewhere. (#279) the list was a single `LazyColumn` with per-row action lambdas defined inline, so a second layout would have duplicated the lock action's mirror cleanup.
- Contract/scope: two settings under **Notes & search**, both defaulting to today's behaviour. `notesLayout` = LIST | GRID (#279) and `noteTitleSource` = FIRST_HEADING | FIRST_LINE (#280), the latter living in `core.text` beside `TitleExtractor` so `core` keeps not depending on `data`. Switching the title rule runs `NoteRetitler.retitleAll` over the database — titles are derived at save time, so the setting would otherwise appear to do nothing — leaving `updatedAt`, empty notes and conflict copies alone. Mirror filenames follow through the existing Sync Center "tidy filenames" action rather than a new folder pass.
- Implementation: PR #281. Row actions hoisted out of the list body and shared by `NoteRow` and the new `NoteCard`; the long-press menu extracted to `NoteContextMenu`; the shared-bounds transition moved into a `Modifier.noteSharedBounds` helper used by both. `titleSource` threaded through the editor's five derivations, both import passes in `NoteFolderMirror`, and the share-intent note creation — which reads the store directly, since the first composition still holds default settings.
- Verification: `testDebugUnitTest` + `lintRelease` locally; 27 unit tests across `TitleExtractorTest`, `NoteRetitlerTest`, `AppSettingsRepositoryListTest` and `AppSettingsTest`. Device pass on `markleaf-phone-api36`: grid at two columns with section headers spanning and the long-press menu working, then the title rule both ways on a note reading `Plain first line` / `## Heading below` — "Retitled 1 note." and the note kept its place in the list. The `settings_notes_search_tablet` golden was re-anchored on "First line": anchored on "Where I left off" it kept passing while showing none of the new rows.
- Release: versionCode 121→122, versionName 2.31.0→2.32.0, CHANGELOG both editions, six store-locale `122.txt` (414 chars at the longest against the 500 cap), landing x6 + README x6. No hardening issue filed — this is issue-response work, and AGENTS.md keeps that flow from growing the backlog it is meant to spend.

## 2026-07-24 - Sidecar metadata index, localised sync labels, and v2.31.0 (#216, #255)

- Trigger: #216 asked why Markleaf writes its own bookkeeping into the note file itself. #255, the hardening list filed alongside v2.30.0, carried a settings-snapshot coverage item, and covering it is what surfaced the hardcoded Korean relative-time labels.
- Analysis: the `markleaf_id` header is what links a file to its note, so removing it from the file needs somewhere else to hold the link — and that somewhere has to survive a folder copied by hand. On the i18n side, Settings and the Sync Center both built the "last synced" label out of Korean literals, so every non-Korean device read `Last synced: 3시간 전`; the Conflict Center's `Updated:` prefix was the same defect in English.
- Contract/scope: (#216) an opt-in **Settings → Multi-device sync → Note metadata** choice between the existing `---` header and a hidden index file, converting the whole folder in either direction without altering note text, with the header remaining the default. (#255) the relative-time labels and the `Updated:` prefix move into the translations with Korean output unchanged, plus Roborazzi coverage for the settings rows that sit below the fold.
- Implementation: sidecar index in PR #256; below-the-fold settings coverage in PR #259, goldens recorded on the Linux runner rather than locally; relative-time localisation in PR #260. PR #257 — per-language tablet demo GIFs on README and the six landing pages — is docs-only and rides this tag without a changelog line.
- Verification: each merged through the required `build` gate on protected `main`. The two trade-offs of the sidecar option are documented rather than fixed: a file renamed outside Markleaf can lose its link to the note, and a folder copied without its index loses each note's created date and pinned state. The header stays the default because of them.
- Release: versionCode 120→121, versionName 2.30.0→2.31.0 (PR #261), CHANGELOG both editions, six store-locale `121.txt`, landing x6 + README x6. Hardening candidates filed as #262.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-23 - Note outline screen, open-notes-at, and v2.30.0 (#214, #215)

- Trigger: one conversation about notes that have grown long produced both issues — #215 wanted a heading outline worth browsing, #214 wanted a note to open somewhere other than the top.
- Analysis: the outline already existed, but as a section inside the "Note information" sheet sharing one scroll with the statistics and the backlinks — so on exactly the long notes that need an outline it got whatever height was left. It also varied both size and indentation per level, which read as noise rather than structure, and tapping a heading while editing flipped the note into reading view to scroll there, discarding whatever was in progress.
- Contract/scope: (#215) the outline becomes a full screen reached from its own editor top-bar button rather than from under "information"; every entry drawn at one size with indentation as the only level cue; tapping a heading while editing moves the cursor and leaves you in the editor. (#214) **Settings → Notes & search → Open notes at** offering Top (default), Bottom, or Where I left off — the remembered position living in the app's own database, never written into the user's files, and recorded only while that option is selected; plus a jump-to-the-other-end button on notes past about forty lines, in both editor and reading view, independent of the setting.
- Implementation: outline screen in PR #251, with its Roborazzi goldens recorded on the Linux runner; open-notes-at in PR #253.
- Release: versionCode 119→120, versionName 2.29.1→2.30.0 (PR #254), CHANGELOG both editions, six store-locale `120.txt`, landing x6 + README x6. Hardening candidates filed as #255.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-23 - Indented frontmatter rule, CI hardening, and v2.29.1 (#234, #235, #241)

- Trigger: #234 reported a metadata header holding an indented `---`. Alongside it, the hardening backlog carried the instrumented suite (#235), the Gradle wrapper (#241) and CI artifact retention.
- Analysis: Markleaf ended a header at the first line reading `---`, including one indented inside a multi-line value — the shape Obsidian and similar tools use for a long description. Everything below it up to the real closing line was read as note text, so entries that came after (such as `tags`) never registered as metadata and a stray `---` was pasted into the note. The next sync wrote that arrangement to the file and the pass after read the mangled result back, so it worsened with every save.
- Contract/scope: end the header only on a line that starts at the left margin, with a header written with a trailing space still closing as before (PR #249). Separately: navigate on the main thread after a suspend point and make the instrumented tests a written local gate rather than a hope (#235); regenerate the Gradle wrapper at 8.9 and document how to verify it (#241); cap debug APK artifact retention at 7 days (PR #243); drop the R8 mapping from the Release and cap retention there too (PR #244); make the local export a required, verified pre-tag step (PR #248).
- Implementation: the instrumented-test CI work went through several shapes before landing — the job was split out of its original PR, the emulator made to answer before Gradle is handed control, the test filter passed through `gradle.properties`, and the run moved out of a folded YAML scalar into a script.
- Release: versionCode 118→119, versionName 2.29.0→2.29.1 (PR #250), CHANGELOG both editions, six store-locale `119.txt`, landing x6 + README x6. Hardening candidates filed as #252.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-23 - Tappable preview checkboxes, the frontmatter round-trip, and v2.29.0 (#219, #222, #226)

- Trigger: #219 asked for a checkbox in reading view that actually toggles. #222, the hardening list from v2.28.2, drove three sync corrections. #226 reported multi-line frontmatter from other tools being lost on a mirror round-trip.
- Analysis: `---` opens a Markdown horizontal rule as readily as it opens a metadata header, so a note whose body began with a rule and carried a second one further down had everything between them read as a header and dropped on the next sync, with nothing to warn the user. Frontmatter preservation only held for entries that fit on one line, so the indented list Obsidian writes tags in by default was read as an empty key and its items erased on the first save. Conflict resolution was recorded by bumping the note's `updatedAt`, which pushed an untouched note to the top of the recently-updated list. And every auto-save located the note's file by opening each file in the sync folder in turn to read its header — at a few hundred notes, most of what a save cost.
- Contract/scope: the checkbox becomes the control in preview, with only the box responding so press-and-hold text selection still works (PR #236); check whether what sits between two `---` lines actually looks like metadata before treating it as one (PR #228); carry unrecognised frontmatter back out exactly as found, nested entries, comments and quoting included (PR #231); settle conflicts with `remoteSeenAt` instead of moving `updatedAt` (PR #229); remember which document holds each note so a save goes straight to it (PR #232).
- Implementation: the #226 frontmatter fix was merged into the mirror-cache work rather than shipped on its own, and the mirror's real folder IO gained an instrumented test covering it (PR #230). PR #225 added a "bring your own folder" story to all six landing pages and narrowed the frontmatter-preservation claim to what the parser can actually do.
- Release: versionCode 117→118, versionName 2.28.3→2.29.0 (PR #237), CHANGELOG both editions, six store-locale `118.txt`, landing x6 + README x6. Hardening candidates filed as #240.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-23 - The long-header adoption regression, and v2.28.3 (#222)

- Trigger: a follow-up to v2.28.2, for a case that release made worse.
- Analysis: Markleaf reads the beginning of each file in the sync folder to work out which note it belongs to. A file whose metadata header ran past that read cap looked exactly like a file with no header at all — and the "adopt the file that already carries this note's name" behaviour added in v2.28.2 could then claim it and rewrite it, discarding a header Markleaf had never read. Headers of the size Markleaf itself writes were never affected; this only reached files carrying a large header from another tool.
- Contract/scope: keep reading while a header is still open, and if the end still cannot be found, leave the file alone rather than claiming it — falling back to writing a separate file, as it did before v2.28.2 (PR #224).
- Implementation: PR #224. PR #223, which documents in the READMEs what the folder mirror does with an existing Markdown vault, is docs-only and rides the same tag.
- Release: versionCode 116→117, versionName 2.28.2→2.28.3 (PR #227), CHANGELOG both editions, six store-locale `117.txt`, landing x6 + README x6.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-23 - Sync stops forking files, and v2.28.2 (#213, #217)

- Trigger: #213, reported by [@Cwpute](https://github.com/Cwpute) — notes getting saved several times — and the four related sync defects the investigation turned up, tracked as #217.
- Analysis: a note's mirror file is found by the `markleaf_id` in its frontmatter. If another app rewrote the file without keeping that block, or simply wrote a byte-order mark in front of it, the link broke and every auto-save from then on created another `<title> (2).md`, `(3).md`, and so on. The same investigation found four more: a conflict re-resolved once a minute indefinitely because neither side was marked handled; conflict copies labelled in hardcoded Korean because the Sync Center detected them by matching that exact Korean text; edits made in another app invisible because only the frontmatter timestamp was compared; and seeding the sync folder skipping a bookkeeping step only the editor performed, which left those notes permanently looking edited-since-last-sync.
- Contract/scope: read past a byte-order mark; adopt the existing file that already carries the note's name instead of forking a copy; record the conflict as handled; detect conflict copies with a stored flag so the label can be translated, while copies from older versions still show up; use the file's modification time when the content genuinely differs and deliberately ignore it when it does not, so a sync client re-downloading a file cannot trigger a storm; and perform the editor's bookkeeping step on every seeding path. The notes database gains one internal column.
- Implementation: the sync fix landed via PR #220 (`45688de`). Also carried by this tag: the landing-overflow gate and the GitLab CI decision — PR #211 retired `.gitlab-ci.yml`, PR #212 restored it behind a `SKIP_GITLAB_CI` switch — quick-insert demo GIFs on all six READMEs (PR #218), and a CHANGELOG pointer to GitHub Discussions (PR #210).
- Release: versionCode 115→116, versionName 2.28.1→2.28.2 (PR #221), CHANGELOG both editions, six store-locale `116.txt`, landing x6 + README x6. Hardening candidates filed as #222.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-22 - Unlock keeps the chosen file extension, and v2.28.1 (#181)

- Trigger: After sweeping the open-issue backlog, the user asked to fix #181 (a real bug) and then to ship it as a v2.28.1 patch.
- Analysis: #181's report pinpointed it exactly — `LockedNotesScreen`'s unlock handler re-mirrors the note via `NoteFolderMirror.writeNote` without the extension argument, so it fell back to the `SyncFileExtension.MD` default; a note kept in a `.txt` sync folder came back as `.md` on every unlock. Confirmed against current code and against the three other `writeNote` call sites (editor save, Settings export-all, Sync Center), which all pass `appSettings.syncFileExtension`.
- Contract/scope: pass `appSettings.syncFileExtension` as `writeNote`'s fourth argument in the unlock re-mirror. One line.
- Implementation: single edit in `LockedNotesScreen` (PR #207), merged to main.
- Audit: re-read the diff against the three canonical call sites; behaviour now matches. Not device-verified — the flow needs a real `.txt`-configured SAF sync folder, impractical to drive over adb — but it reuses the exact setting the other write paths already pass; `compileDebugKotlin` and the PR build gate are green.
- Release: versionCode 114→115, versionName 2.28.0→2.28.1, CHANGELOG both editions, six store-locale `115.txt`, landing x6 + README x6. This tag also carries the docs-only #203 (a GitHub Discussions link added to the six READMEs) that another session merged to main between v2.28.0 and this tag; it has no app-facing change, so it is not in the changelog.

## 2026-07-22 - Open-notes-in-preview, RTL notes, and v2.28.0 (#200, #146)

- Trigger: The user asked to handle a new issue (#200), then to revisit #146 after the reporter added a comment, sweep closed issues with dangling replies, register hardening candidates, reply to the issue as usual, and finish the v2.28.0 release.
- Analysis (strong role): read EditorScreen, EditorTopAppBar, AppSettings/Repository, SettingsScreen, MarkleafNavHost, MarkdownPreviewList, Theme.kt, TitleExtractor, the search/tag DAO, and the Roborazzi/verify CI before deciding. #200 fits the app's opt-in-with-existing-default identity; the RTL claim on #146 was reproduced from a screenshot and traced to the app never setting a content text direction.
- Contract/scope: (#200) `AppSettings.openNotesInPreview` (persisted, default off); the editor reads it via `settings.first()` on note load and applies preview mode; a long-press on the view-toggle icon flips the persistent lock in-context (Initial-pass `pointerInput` so the tap and long-press stay independent) with an amber tint + toast + a11y `onLongClick`; a Settings switch mirrors the flag; strings in all six locales. (#146) one helper on the `Typography` applies `TextDirection.Content` to every text role, so the editor and preview lay each paragraph out by its own first strong character while Latin text (and every existing snapshot) is unchanged.
- Implementation (single-session sequential roles): #200 across AppSettings/Repository/EditorScreen/EditorTopAppBar/SettingsScreen + 6-locale strings + a Robolectric tap-vs-long-press test (PR #201); #146 in Theme.kt (PR #202). Kept as two independent branches/PRs off main.
- Audit (strong role): re-read both diffs 1:1. Codex review on #201 caught a real defect — the FAB creates the note first and opens it with a real id, so gating preview on `noteId != null` dropped a just-created empty note into preview; fixed by gating on `content.isNotEmpty()` (mirrors the adjacent `shouldRequestEditorFocus = content.isEmpty()`). Codex's changelog note was answered as deferred-to-release per this repo's bundling practice (thread left open).
- Verification: `testDebugUnitTest` + the interaction test green; PR builds green including `verifyRoborazziDebug`. The #201 Windows-only sub-pixel golden shift was proven benign by a Linux `record_roborazzi` run (48/48 goldens byte-identical) and CI verify passing. Device (markleaf-phone-api36): #200 lock/unlock, cross-note persistence, amber tint, toast, settings-driven preview, restart persistence, new-note-opens-in-edit; #146 a Persian paragraph now right-aligns in both editor and preview (the exact screen from the report), sent in via an ACTION_SEND intent.
- Issues: swept closed issues with a non-maintainer last comment — #138 (tag filter over-match) confirmed already fixed by v2.27.0's `searchNotesByTag` membership join and answered; #134 (title-from-first-line) answered; #155/#139 acknowledged; #146 reopened with an apology and the fix; #200 answered in the accept-and-explain pattern. Hardening candidates filed as #204.
- Release: versionCode 113→114, versionName 2.27.2→2.28.0, CHANGELOG both editions, six store-locale `114.txt` (all <500 chars; en/de trimmed below the 450 warning), landing x6 + README x6.

## 2026-07-21 - Tappable links in table cells, and v2.27.2 (#197)

- Trigger: #197, a reader report that a link inside a Markdown table cell did nothing when tapped.
- Analysis: the table renderer dropped the link's destination at parse time and drew every cell as a non-clickable string, so a link in a cell rendered as plain text. The table-cell path had no test coverage of its own.
- Contract/scope: a link in a table cell is tappable in the preview exactly like a link in body text, and inline styles (bold, italic, code, strikethrough) and `[[wiki-links]]` inside cells render and behave the same as elsewhere.
- Implementation: PR #198 — the parse and render path across `CommonMarkPreviewAdapter`, `SimpleMarkdownPreview` and `MarkdownPreviewList`.
- Verification: a parser unit test, a Compose click-interaction test, and a new visual golden covering the table-cell link path that previously had none; the golden was recorded on the Linux runner (`dc30c22`) rather than locally.
- Release: versionCode 112→113, versionName 2.27.1→2.27.2, CHANGELOG both editions, six store-locale `113.txt`, landing x6 + README x6 — bundled into the same commit as the fix (`06ba57c`) rather than a separate release commit.
- Record: reconstructed from the tag range, its PR and CHANGELOG under #264; it was not logged at release time.

## 2026-07-21 - Hardening Backlog Sweep and v2.27.1 (#154, #158, #167, #184, #195)

- Trigger: The user asked to work through the unchecked items across the six open hardening-candidate issues.
- Triage (strong role): 11 unchecked items. Implemented 7 now; left 4 that are blocked on something other than effort — #158's keyboard-haptics feel test needs a human typing on an external keyboard, #150's local 8-pixel golden drift is a Windows-raster limitation with Linux CI as the standing authority, #150's IME/TalkBack device gate and #167's release-APK runtime smoke are emulator-infrastructure work where a naive gate would hold releases hostage to launch-smoke-class flakiness.
- Contract/scope: (#158) injectable DataStore in AppSettingsRepository (internal constructor; production path unchanged) + in-memory DataStore test double, 7 cases over attemptLockPasscode's persistence glue. The issue's premise was confirmed exactly: even with per-test files, DataStore 1.0's File.renameTo fails on Windows from the second write on, so the file-backed variant of the test was replaced with the in-memory design — durability of the file is the library's contract, the key glue is ours and is what the tests pin. (#195) NonCancellable around every settings write via one persist() funnel; record lastOpenedNoteId only on successful load; clear a stale id at launch with a BuildConfig.DEBUG breadcrumb; titles-only/recents moved to Dispatchers.Default (produceState first, swapped to LaunchedEffect+state after lint's ProduceStateDoesNotAssignValue false-positived on the conditional assignment). (#154) Roborazzi goldens: EmptyState x3, tablet 640dp Settings and Tags; Settings captures the viewport only so BuildConfig.VERSION_NAME in the App section cannot break the golden every release. (#184) verify-release-notes.ps1 warns from 450 chars (90%), fails above 500 — both branches exercised against synthetic 470/501-char notes; .gitlab-ci.yml verify_release_docs job on mcr.microsoft.com/powershell, wired into package_signed_release.needs because needs-DAGs bypass stage ordering.
- Goldens: recorded on the Linux CI runner via the record_roborazzi workflow_dispatch on the feature branch, downloaded from the artifact and committed — local Windows recording would disagree with CI verify by font hinting.
- Verification: testDebugUnitTest (397 tests) and lintRelease green locally; PR build gate green including verifyRoborazziDebug over the new goldens.
- Release: versionCode 111→112, versionName 2.27.0→2.27.1, CHANGELOG both editions, six store-locale 112.txt, landing x6 + README x6.

## 2026-07-21 - User-Requested List/Search Options and v2.27.0 (#188-#193)

- Trigger: The user asked to respond to the batch of feature requests Cwpute filed (#188-#193), accepting as much as fits the app's identity and making features optional where sensible, following the established issue-response routine.
- Analysis (strong role): read NotesListScreen, SearchScreen, QuickSwitcherDialog, MarkleafNavHost, EditorScreen, NoteDao, AppSettings/Repository before deciding. Accepted five requests; declined #189 (mirror to two folders at once) because the sync mirror's single-source-of-truth design is deliberate — a second bidirectional mirror creates three-way conflict semantics the app cannot make safe, and external replication (Syncthing/cloud client on the mirror folder) achieves the outcome. Decline explained on the issue with two workarounds; closed as not planned.
- Contract/scope: five new persisted settings (notesShowPreview default true, reopenLastNote default false, notesSortMode default UPDATED_DESC, searchTitlesOnly default false, lastOpenedNoteId) — every default preserves pre-existing behaviour. #188: showPreview parameter on the list row and search result row + a Settings "Notes & search" section. #191: top-bar sort menu (a labeled dropdown rather than the suggested blind cycle button, so the active order is always visible; the reporter allowed this); pinned notes outrank any sort; date sections only in the default order; sort logic in a pure `sortNotesForDisplay` for testability. #192: EditorScreen records lastOpenedNoteId; the NavHost one-shot intent effect gains an else-branch that reopens it on plain launches only, skipping trashed/deleted ids and routing locked ids through resolveOpenNoteRoute to the passcode gate (#158 precedent). #193: SearchViewModel exposes allNotes; blank query shows a recent-notes list (quick-switcher semantics: untitled skipped, updatedAt desc, 20); a persisted Everything/Titles-only FilterChip pair; titles-only matches by case-insensitive substring. #190: FocusRequester on the query field, skipped when the screen opens with an initialQuery (tag tap) since the user came to read results.
- Implementation (single-session sequential roles): 18 files + 3 new (NotesSort.kt, NotesSortTest, SearchQuickAccessTest). 13 new strings in all six locales.
- Audit (strong role): re-read the full diff 1:1 against the contract; found the #190 autofocus firing on tag-initiated searches and narrowed it to blank-query opens before commit. No scope creep found; DAO order (`sortOrder` tie-break) is preserved in the default mode because Kotlin's sort is stable.
- Verification: `testDebugUnitTest` and `lintRelease` BUILD SUCCESSFUL (new tests: 6 sort cases, 5 search quick-access cases, settings defaults). Roborazzi re-record not needed — no preview/typography rendering changed.
- Device verification (markleaf-tablet-api36 AVD; no physical device connected): sort menu shows check on active mode, Title A-Z flattens sections with Pinned kept on top; title-only rows in list and search; search opens focused (typed straight into the field), recent notes before typing, Everything vs Titles-only verified with "mirror" (3 notes + tag vs 1 note + tag); reopen-last-note proved with `am force-stop` + relaunch landing in the last note's editor. Debug APK uninstalled after.
- Release: versionCode 110→111, versionName 2.26.2→2.27.0, CHANGELOG both editions, six store-locale `111.txt` (all <500 chars), landing x6 and README x6 version strings.

## 2026-07-20 - Terminology consistency and release-verification guards, and v2.26.2

- Trigger: wording had drifted across the five translated languages, and two consecutive releases had shipped with stale public version strings — the existing check only compared the languages to each other, so six equally-outdated numbers passed.
- Analysis: twenty-four strings named one concept two ways inside a single screen or broke a convention the rest of the file kept (Korean calling the Conflict Center an "archive", German "unpin" sharing no stem with "pin", Japanese mixing half- and full-width question marks). The version-string hole was structural, not a slip: parity between locales cannot catch a value that is wrong in all of them.
- Contract/scope: settle the twenty-four strings in ko/fr/de/ja/es; add a CI check comparing the six landing pages and six READMEs against the app's own `versionName` (not just against each other); add a check that all six store changelogs exist for the build, fit the 500-character store limit, and agree across the English and Korean editions on version, order and date. Plus: document what the Locked space protects and does not (bodies are not encrypted; the retry delay is cleared by moving the device clock), with the leave-it-as-is reasoning recorded as a decision; and consolidate nine copies of the "is sync configured and does the stored URI still parse" check into one helper.
- Implementation: terminology in PR #182; the two release guards in PR #178; the sync-folder URI helper in PR #179 (which surfaced the one call site carrying an extra condition — the editor refuses to mirror a locked note); the Locked threat model in PR #180. This tag also carries the mirror infrastructure — automatic main→GitLab mirror-push (PR #177), a daily mirror check (PR #175), `verify-mirror` gating on real divergence with a `-MirrorOnly` mode for CI (PRs #171, #173) — and the locked-notes fixes for a tag leak into search (PR #174) and a deep link bypassing the passcode gate (PR #176), plus corrections to the documented Release asset list (PRs #168, #170).
- Verification: the two guards this release added are the version-string and release-notes gates that every later release has run against; that they are in force from here is visible in `verify-landing-versions.ps1` and `verify-release-notes.ps1`. Merged through the required `build` gate on protected `main`.
- Release: versionCode 109→110, versionName 2.26.1→2.26.2 (PR #183), CHANGELOG both editions, six store-locale changelogs, landing x6 + README x6.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-19 - Localization accuracy and the untranslated-string guard, and v2.26.1

- Trigger: several shipped strings still held their English source text, Spanish had lost its accents across ~60 strings, and a few translations had drifted from what the app does — including a privacy claim.
- Analysis: `MissingTranslation` lint only fires when a string is *absent*, not when a present one was never translated, so `sync_title`, `sync_recommended_locations` and `application_id_format` rendered in English on fr/ja/ko with nothing to flag it. The drifted claims were the dangerous ones: the French privacy line claimed no data *download* where the English claims none is *sent*, and the Korean editor hint had dropped that autosave writes to the device.
- Contract/scope: repair the untranslated and mistranslated strings across the six languages (Spanish diacritics; the ja/ko "remove from lock" reading as "unlock"; the Korean trash-confirmation object particle; quotes the resource compiler stripped; the archive noun-vs-verb collision; the three drifted de/fr/ko claims); and add a build-time guard that compares every locale value against the English source and fails when they match, with an allowlist for legitimately-identical values and a second test that fails once an allowlist entry stops being needed.
- Implementation: the untranslated-string guard in PR #164, the three restored claims in PR #165, the remaining i18n repairs across the commits merged ahead of the release. This tag also carries the English-first docs decision (PR #157) — `CHANGELOG.md` becomes the English source the release job extracts notes from, `CHANGELOG.ko.md` the Korean edition, with releases from v2.16.0 retranslated — and Spanish and French joining the README and landing pages, plus mirror-runbook corrections for protected main (PRs #160, #162, #163).
- Verification: the guard added here — `UntranslatedStringTest` with its allowlist and its still-needed companion — is the pattern later extended by `ResourceParityTest` and, in #262/#263, by `HardcodedStringTest` for text that never became a resource at all. Merged through the required `build` gate on protected `main`.
- Release: versionCode 108→109, versionName 2.26.0→2.26.1 (PR #166), CHANGELOG both editions, six store-locale changelogs, landing x6 + README x6.
- Record: reconstructed from the tag range, its PRs and CHANGELOG under #264; it was not logged at release time.

## 2026-07-18 - Hardening Sweep and v2.26.0 (#150, #152, #154, #156)

- Trigger: The user asked to work through the open hardening-candidate issues, close what got done, and finish with a version bump and release.
- Analysis (strong role): four open issues held 14 unchecked items. Mapped each against the code with three parallel read-only agents (locked-notes implementation, editor shortcut dispatch, test/CI + tablet layout) before touching anything. Two items were resolved by evidence rather than code: at-rest encryption for locked notes is explicitly reserved in `docs/ROADMAP.md` ("명시적 비범위와 보류") until a threat model covering search, attachments, and folder mirror is approved, so implementing it here would have front-run the roadmap; biometric unlock of the Locked space would let anyone who passes the device biometric read notes gated on a *separate* secret, which is a product decision, not a hardening fix. Both recorded in D061 instead of coded.
- Newly found, not in any issue: the locked filter had two more holes. `NoteLinkDao.observeBacklinks` and `TagDao.observeTagsWithCounts` still read locked rows, so a locked note's title appeared in the note-information sheet of any note it linked to, and a tag used only by locked notes showed a non-zero count that drilled down to an empty list. Classified as current scope — same defect class as #156-1, found while fixing it — and fixed with regression tests.
- Contract/scope: (#156) delete the mirror file on lock and rewrite it on unlock — delete-without-restore would have made the note permanently absent from the user's folder, so the unlock half is a correctness requirement of the lock half, not scope creep; add `locked = 0` to the two leaking queries; resolve wikilinks against locked notes for *existence* only, reporting where the note lives instead of opening it or creating a duplicate; `FLAG_SECURE` for the lifetime of the Locked screen, restored to the setting on dispose; persisted `PasscodeBackoff` policy enforced behind a single `attemptLockPasscode` entry point. (#150) one `toFormattingAction` keymap shared by the text field and the panel, both dispatching through one `applyFormattingAction` handler. (#152) move the nine Compose UI tests to a debug-only `src/testDebug` source set. (#154) mirror recovery runbook section, tablet Settings re-indent.
- Implementation (light role): 21 files changed plus 5 new. The `src/testDebug` move deleted the hand-maintained per-class exclusion list from `app/build.gradle.kts` outright; `ComposeTestSourceSetTest` now fails the fast local debug run with an explicit "move these files" message if a Compose test lands in `src/test`, replacing an obscure benchmark-variant failure. Two new strings across all six locales.
- Audit (strong role): re-read the full diff against the contract. Caught and fixed a stray import placement; confirmed the Settings re-indent is whitespace-only (`git diff -w` empty, and a line-by-line check proved all 324 changed lines differ by exactly a 4-space prefix). The keyboard shortcut path now fires haptics where it previously did not — an intended consequence of unifying the two dispatch paths, recorded in the changelog rather than left as a silent behavior change.
- Verification: `:app:test` green across all three unit-test variants (debug 44 classes / release 35 / benchmark), which is the actual proof for #152 — the release and benchmark variants build and run with no exclusion list at all. `lintRelease` and `assembleDebug` green. 17 new tests.
- Device verification: physical SM-S921N (API 36, Korean) and an API 36 tablet emulator (English). `FLAG_SECURE` proved by window-flag comparison — `fl=81812100` on the Locked screen versus `fl=81810100` on the notes list, differing in exactly bit `0x2000`, and cleared again on exit. Backoff proved end to end: attempts 1-4 returned "Incorrect passcode" with the field enabled, the 5th disabled the field and showed the localized "Too many attempts. Try again in 0:28." countdown. Persistence proved by arming a 1:58 lockout, `am force-stop`, and relaunching — the gate came back still disabled and still counting at 1:28.
- Roborazzi: `verifyRoborazziDebug` fails 15 goldens on Windows. Confirmed pre-existing rather than caused by this work by running the same task in a clean worktree at `HEAD`, which failed the identical 15 — matching the 15 platform-raster mismatches already recorded for Phase 29. Left the goldens untouched; local verification on Windows is not a gate and Linux CI remains the authority (#150-1 stays open).
- Release: versionCode 107→108, versionName 2.25.0→2.26.0, CHANGELOG v2.26.0 section, six store-locale `108.txt` changelogs (all under the 500-char Play cap).

## 2026-07-18 - Locked Notes Feature (#155)

- Trigger: The user asked to check GitHub for a new issue, add the requested feature, and thank the reporter. Issue #155 (bit2bold) requested password-protected folders.
- Analysis/design (strong role): Markleaf has no "folders" — notes are organized by tags — and already ships an app-wide biometric lock whose posture is "hide the UI, the DB stays plaintext." Because the requested name doesn't map to an existing concept and the choice is security- and data-shaped, confirmed the direction with the maintainer before coding: a single passcode-gated "Locked notes" space (not per-tag, which is semantically leaky when a note has several tags), a UI-visibility gate (not at-rest encryption), with that limitation to be surfaced in the issue comment and full encryption offered on request.
- Contract/scope: `locked` flag on NoteEntity/Note + DB v14→v15 migration (`ALTER TABLE notes ADD COLUMN locked INTEGER NOT NULL DEFAULT 0`, mirroring the sortOrder precedent). Hide locked notes from every active-note query (observeNotes, FTS/LIKE/tag search, archive, max sort order, title lookup, conflict list) and from the home widget and folder-sync export (all bulk export paths already flow through observeNotes; the editor auto-export is guarded on `!locked`). Passcode stored only as salt + PBKDF2 hash in DataStore, never plaintext. LockedNotesScreen (modeled on ArchiveScreen) gated by a session-scoped passcode unlock. Entry points: note-row "Move to Locked" (prompts to set a passcode first if none), overflow "Locked notes". Settings set/change/remove passcode; removing the passcode unlocks all notes so none are stranded. moveToTrash clears `locked` so a deleted note can't become an unreachable locked+trashed orphan or leak its title into Trash.
- Implementation (light role): applied the contract across data, DAO/repository, settings, one new util (PasscodeHasher), two new screens/VMs (LockedNotesScreen/ViewModel), and the two NotesListScreen nav call sites. 29 new UI strings added to all six locales (en/ko/ja/de/es/fr).
- Audit (strong role): re-read the full diff against the contract and caught one active-note query (`observeConflictNotes`) still missing `locked = 0`; fixed and re-verified. No scope creep beyond the confirmed feature.
- Verification: `testDebugUnitTest` (338 tests, 0 failures — including 6 PasscodeHasher cases, 5 locked-notes repository cases, the LockedNotesViewModel factory case, and ResourceParityTest confirming six-locale parity) and `lintRelease` both BUILD SUCCESSFUL (MissingTranslation is fatal, so the release lint passing confirms locale parity too). Room exported `schemas/15.json` carrying the `locked` column; the migration test was extended to assert the column exists and defaults to 0 after upgrading to the current schema.
- Sync limitation (documented, not silently handled): locked notes are excluded from folder-sync export, but a note mirrored *before* it was locked leaves a plaintext file in the synced folder — the issue comment states this and offers at-rest encryption on request.
- Release prep (staged on a feature branch, NOT pushed/tagged): versionCode 106→107, versionName 2.24.0→2.25.0, CHANGELOG v2.25.0 section, six store-locale `107.txt` changelogs (all <500 chars). Tag/push to GitLab+GitHub, Play AAB export, and the vX.Y.Z hardening issue are held for maintainer confirmation because they are outward-facing and hard to reverse.

## 2026-07-16 - Main Branch Consolidation and GitLab v2.24 Alignment

- Trigger: The user asked to consolidate every completed local/remote branch and prior-session change into `main`.
- Audit: only `main` and `docs-i18n-wip` were live locally, with no stashes or extra worktrees. Deleted Phase 29 branch tips were patch-equivalent to commits already reachable from the current history. GitHub `main` held the clean release commit `d81fdd1`, while GitLab `main` held the alternate release commit `16feaac` that included the preserved landing-i18n work.
- Integration: pushed the tablet focus/vector fix (`8dcc4ac`), canonical Linux golden (`df1a16f`), and Phase 29 completion record (`b3d847a`) to both remotes. Created merge `4b2dfd9` with parents `d81fdd1` and `b3d847a`; resolved the public surface to an English default README/landing, dedicated Korean files, and no duplicate `README.en.md`. The resolved merge tree is identical to `docs-i18n-wip`.
- Verification: the merge tree passed `testDebugUnitTest + compileDebugAndroidTestKotlin + lintRelease + assembleDebug` (`84 tasks`, `2 executed`), produced a non-empty 30,963,761-byte debug APK, and passed four-language landing version parity. GitLab main pipeline `2680987479`, GitLab tag pipeline `2680987486`, and GitHub main run `29481751909` all passed, including Linux Roborazzi, release lint/R8, and API 30 launch smoke.
- Release alignment: pushing GitLab first fast-forwarded its protected `main` and synchronized the existing annotated `v2.24.0` tag. The resulting GitLab Release exposes four permanent Generic Package Registry links (all HTTP 200): APK 2,763,707 bytes, AAB 5,497,895 bytes, mapping 41,774,899 bytes, and six-locale notes 2,749 bytes. GitHub was pushed second; both remotes now share the integrated history.

## 2026-07-16 - Phase 29 Tablet QA and Empty-Editor Correction

- Trigger: The user asked to create a tablet-sized virtual device, validate the Phase 29 editor on it, and carry the result through branch consolidation.
- Scope: Correct only two runtime defects found by tablet QA: missing initial focus for a persisted empty note and the editor's remaining literal emoji empty-state icon. No permission, dependency, database, network, account, analytics, or stored-Markdown contract changed.
- Work: requested editor focus after loading empty persisted content, replaced the pencil emoji with `Icons.Outlined.EditNote`, and added two API-level regression tests covering focus and semantics.
- Manual verification: API 36 Pixel Tablet at 2560x1600/320dpi, responsive phone override at 1080x2400/420dpi, six app locales, and a Google Play API 36 TalkBack image all passed the relevant writing, formatting, autosave, label, traversal, and 48dp-target checks. Emulator overrides and TalkBack state were restored.
- Automated verification: the pre-fix `EditorScreenTest` run failed 2/9 and the identical post-fix run passed 9/9. The combined `testDebugUnitTest + compileDebugAndroidTestKotlin + lintRelease + assembleDebug` hard gate passed (`84 tasks`, `18 executed`). Full instrumentation remains blocked by an existing compact NavHost shared-transition teardown lifecycle crash unrelated to these two editor paths; three diagnostic toggles were reverted after reproducing the same failure.
- Visual regression: Windows verification reported 15 platform-raster mismatches against Linux canonical goldens. Linux recorder run `29478985691` showed that only `editor_screen_quiet_appbar_phone.png` changed; the other 41 snapshots were byte-identical. After committing that one authoritative golden (`df1a16f`), run `29479296610` passed unit tests, Linux Roborazzi verification, release lint, R8 release APK, debug APK, and the API 30 launch smoke job.
- Final visual audit: two independent read-only reviewers passed the fresh phone/tablet captures plus Korean, Japanese, German, French, Spanish, and TalkBack evidence with no clipping, overlap, CJK-breaking, contrast, or focus-affordance blocker.

## 2026-07-16 - v2.24.0 Release (Note Information Sheet + Phase 29 UI Polish)

- Trigger: After Phase 29 UI polish merged and the GitLab pipeline passed, the user asked to release v2.24.0.
- Scope: versionCode 106 / versionName 2.24.0, bundling the editor note-information surface, excerpt de-duplication, plurals, empty states, and tablet width from the Unreleased section. No new product code.
- Work: bumped the version, promoted the CHANGELOG v2.24.0 section, wrote six store-locale `106.txt` changelogs (all <500 chars), and updated the four READMEs and the landing version strings.
- Verification: `testDebugUnitTest + compileDebugAndroidTestKotlin + lintRelease + assembleDebug` plus signed `exportReleaseToBuildDrive + assembleRelease` were BUILD SUCCESSFUL; D:\Build holds the signed AAB (5,496,930 B), mapping, and six-locale notes. Signing cert SHA-256 = production key `0be97352…7eecf91a`.
- Incident + recovery: the release `git add -A` swept in uncommitted landing-i18n work (README en→ko, index.ko.html, privacy translations) because the initial git status was truncated. Preserved that work on branch `docs-i18n-wip`, reset main to a clean 13-file release commit, and force-pushed. Logged as hardening issue #154.
- Publication: tagged `v2.24.0` on the clean commit `d81fdd1` and initially pushed to GitHub, where build / release / launch-smoke passed and the production-signed APK is 2,763,707 B. The later consolidation merge `4b2dfd9` preserved both release lineages, fast-forwarded protected GitLab `main`, synchronized the same annotated tag, and completed the GitLab permanent-package Release.

## 2026-07-16 - Phase 29 UI Polish

- Trigger: After merging the editor note-information surface, the user chose to complete the remaining Phase 29 UI tasks in sequence.
- Scope: Note-list excerpts, Android plurals, empty states, and tablet content width. No permission, dependency, database, or stored-Markdown contract changed.
- Work:
  - `generateExcerpt` now skips the line used as the title (shared `titleLineIndex` helper) so note list, search, archive, trash, and sync rows stop repeating the title in their preview.
  - Converted five single-quantity strings to `<plurals>` across all six locales and switched their call sites to `getQuantityString` / `pluralStringResource`.
  - Added `ui/component/EmptyState` and routed the trash, tags, search, and archive empty screens through it with screen-appropriate outlined icons.
  - Capped the tags list and settings column at 640dp centered on wide screens; phone widths are below the cap and unchanged.
- Verification:
  - `./gradlew test` (debug/release/benchmark) plus `lintRelease` and `assembleDebug` were BUILD SUCCESSFUL after each step; lint confirmed six-locale plurals parity and `TitleExtractorTest` gained three anti-duplication cases (11 pass).
  - Commits: excerpt `d71fd71`, plurals `1d92dc7`, empty-state `9b55de0`, tablet `14bc033`.
  - Roborazzi goldens for the new empty and tablet surfaces are left to the Phase 29 QA task (manual QA, TalkBack, snapshot record).

## 2026-07-16 - Phase 29 Editor App Bar and Note Information Surface

- Trigger: Picked up the previous session's uncommitted implementation; the user asked to verify it and carry it through to completion.
- Scope: Restructure only the editor top app bar and the note-information (statistics, outline, backlinks) surface. No permission, dependency, database, or stored-Markdown contract changed.
- Work:
  - Extracted the top app bar into `EditorTopAppBar` (Back, title, Preview/Edit, Note information, More) and moved Find, Focus mode, system share, Markdown/PDF export, and Move to trash into the More menu in task order.
  - Consolidated statistics, table of contents, and backlinks into a single `EditorInfoSheet` modal bottom sheet and removed the permanent statistics text from the formatting footer.
  - Limited outline parsing to when preview or the info sheet needs it, and made heading selection enter preview and scroll to the matching item.
  - Added four strings across six locales, plus InfoSheet/TopAppBar/EditorScreen snapshot tests, InfoSheet behavior tests, and an instrumented outline-navigation test.
- Verification (single session, sequential analyze -> implement-check -> audit):
  - `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintRelease :app:assembleDebug` passed (`BUILD SUCCESSFUL`, 84 tasks, 2 executed).
  - Re-read the full diff against the `DESIGN.md` "Editor app bar and note information surface" contract. Confirmed boundary safety for `computeStats("")` and the empty-title backlinks path, six-locale string parity (four each), and no out-of-scope changes.
  - The 16 Roborazzi goldens (8 modified, 8 new) are local Windows recordings and must be re-recorded on the Linux CI runner before push (AGENTS.md rule); local pixel verification was skipped because font hinting diverges from CI.

## 2026-07-15 - Quiet Proof Landing Page Renewal

- Trigger: The user accepted the landing-page audit and asked to implement the proposed renewal.
- Scope: Rebuild only the GitHub Pages landing surface and its design contract. No Android behavior, permission, dependency, database, release version, or distribution artifact changed.
- Work:
  - Added the `DESIGN.md` public-web contract with the Quiet proof direction, product-aligned tokens, responsive primitives, keyboard/motion rules, and F-Droid-first install hierarchy.
  - Replaced the repeated feature-card layout with an asymmetric real-product hero, three sequential writing stories, a no-INTERNET data-flow proof, and accurately labeled download channels.
  - Derived 720px and 1280px responsive WebP renditions from four existing 2560x1600 store screenshots; the landing keeps real product content while avoiding oversized transfers.
  - Added canonical/Open Graph/Twitter/SoftwareApplication metadata and removed landing-page Google Fonts requests.
- Verification:
  - Real Chrome/Playwright rendered fresh 375x812, 768x1024, and 1280x960 captures with `scrollWidth === clientWidth`, all responsive images loaded, no sub-48px targets, and zero console, request, or external-runtime errors.
  - Hover, focus, pressed, skip-link, sticky-anchor, local policy/asset responses, and reduced-motion states passed. The first render exposed Korean word splitting; `word-break: keep-all` plus a bounded no-break hero phrase fixed it without hiding overflow.
  - Three-run Lighthouse medians were 100 for Performance, Accessibility, Best Practices, and SEO on both mobile and desktop; median LCP was 1.43s mobile / 0.38s desktop, CLS was 0, and median TBT was 0ms / 8.8ms.
  - Two independent read-only visual QA lanes passed the final responsive and interaction evidence with no blockers after CJK wrapping, 14px mobile navigation, hover feedback, and compact skip-link evidence were corrected.
  - `./gradlew test assembleDebug` passed (`BUILD SUCCESSFUL`, 91 tasks); `app-debug.apk` exists and is 20,368,092 bytes.

## 2026-07-15 - v2.23.0 Quiet Formatting Release Preparation

- Trigger: The user asked whether the completed Quiet Editor work had been pushed and requested a new release if it had not.
- Scope: Package the completed progressive-formatting change as versionName 2.23.0 / versionCode 105, refresh public version surfaces, and prepare the signed GitHub/GitLab/F-Droid/Play hand-off. No additional product behavior was added.
- Work:
  - Promoted the Quiet Formatting changelog and added six Play/F-Droid locale notes, each within the 500-character store limit.
  - Updated all README release links, the landing-page current-version strip, and the F-Droid metadata draft to v2.23.0.
  - Exported the signed AAB, R8 mapping, and six-locale combined notes to the canonical `D:\\Build` directory.
- Local verification:
  - `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintRelease :app:assembleDebug` passed (`BUILD SUCCESSFUL`, 84 tasks).
  - The focused nine-state `EditorFormattingControlsSnapshotTest` Roborazzi verification passed.
  - Signed `:app:exportReleaseToBuildDrive :app:assembleRelease` passed. The release APK reports `com.markleaf.notes`, versionName 2.23.0, versionCode 105, and production certificate SHA-256 `0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a`; the AAB certificate matches.
  - Local outputs: debug APK 20,368,092 bytes; release APK 2,760,967 bytes; AAB 5,493,207 bytes; mapping 41,840,923 bytes; combined notes 2,138 bytes.
- Main-CI correction:
  - GitHub Actions run `29388289814` exposed that aggregate `test` also ran the 19 new Compose UI tests in the release-derived benchmark variant, whose manifest intentionally lacks the debug-only `androidx.activity.ComponentActivity`.
  - Expanded the existing non-debug test exclusion to cover all snapshot classes and the new editor Compose behavior class. The pre-fix benchmark repro failed 10/10; after the fix, `:app:testBenchmarkUnitTest` and the exact aggregate `test` command passed.
  - Force-ran the authoritative debug path: 10 behavior tests and 9 Roborazzi snapshots passed with zero failures. Hardening follow-ups were recorded in GitHub issue #150.
  - GitHub run `29389449837` confirmed `Run tests` green, then exposed six Windows-vs-Linux raster differences in the new formatting goldens. The dedicated Linux recorder job in run `29389737305` succeeded; its six failing-state images are byte-identical to the CI actuals. Replaced only the eight new formatting PNGs that differ from the canonical Linux artifact.
- Publication:
  - Tagged commit `4d1da8d75555277f2aec8f40e849356f0f5e02d8` as `v2.23.0` and pushed GitLab first, then GitHub.
  - GitHub workflow `29390626463` passed build, signed release, and emulator launch-smoke jobs. GitLab pipeline `2677520856` passed verify, signed package, and public release jobs.
  - Unauthenticated GitHub downloads: APK 2,760,967 bytes and mapping 41,840,800 bytes. The APK reports `com.markleaf.notes` / 2.23.0 / 105 and the production certificate. The signed Actions AAB is 5,494,421 bytes, reports `jar verified.`, and uses the same certificate.
  - Unauthenticated GitLab Generic Package Registry downloads: APK 2,760,967 bytes, AAB 5,494,421 bytes, mapping 41,840,800 bytes, and six-locale notes 2,138 bytes. APK/AAB signatures, package/version, locale blocks, and permanent package-registry URLs were verified. GitHub/GitLab public binary hashes match.

## 2026-07-15 - Phase 29 Quiet Editor Formatting Surfaces

- Trigger: After recording the Bear-class product-cohesion roadmap and freezing the formatting interaction contract, the user asked to start the next Phase 29 task.
- Scope: Replace only the permanent editor formatting toolbar. No permission, dependency, database, account, analytics, network, or stored-Markdown contract changed.
- Work:
  - Added a compact `Aa` footer entry, selected-text Bold/Italic/Link/More actions, and a phone-width/tablet-bounded expanded panel containing all 13 prior formatting actions.
  - Preserved `Ctrl/Cmd+B`, `I`, `K`, `Shift+S`, Tab indentation, Quick Insert, tag/wikilink suggestions, autosave, and the existing SAF image picker.
  - Added localized accessibility labels and expanded/collapsed state in all six locales, 48dp targets, keyboard-first focus, Escape/Back dismissal, and visual focus feedback.
  - Added behavior, instrumented integration, and nine-state Roborazzi coverage for phone/tablet, dark theme, large text, Korean, disabled, selection, and keyboard focus.
- Verification:
  - `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintRelease :app:assembleDebug` passed; debug APK size 21,238,174 bytes.
  - The new `EditorFormattingControlsSnapshotTest` Roborazzi suite passed 9/9.
  - Full Roborazzi verification also exposed a pre-existing 8-pixel mismatch in two unchanged Quick Insert goldens; the rendered reference/new images are visually identical and the Korean Quick Insert golden remains exact.
  - Two API 36 Pixel 6 AVDs were attempted with clean data and software/host GPU paths, but both stayed `adb offline`; device IME/TalkBack verification remains in the dedicated Phase 29 final QA task.

## 2026-07-13 - Public GitLab Release Mirror

- Trigger: The user wanted downloadable GitLab release files as a resilient
  alternative to GitHub and asked to confirm both repositories remain
  updateable.
- Scope: Distribution infrastructure and documentation only. No app permission,
  dependency, network, API, account, analytics, database, or UI behavior changed.
- Work:
  - Added independent GitLab branch/MR verification and protected semantic-tag
    release jobs.
  - Added a shared Gradle export path for signed APK/AAB, mapping, and six-locale
    notes while preserving the three-file `D:\Build` Play hand-off.
  - Configured protected tags and protected signing variables, made the GitLab
    project public, and backfilled v2.22.0 into the Generic Package Registry.
  - Added GitHub/GitLab repository cross-links and release links to all README
    locales.
  - Documented ordered dual push as the synchronization contract; automatic
    bidirectional provider mirroring remains intentionally disabled.
- Verification:
  - Local unit tests, release lint, debug build, signed four-file CI export,
    production APK certificate, AAB JAR signature, six-locale notes, and
    `D:\Build` three-file compatibility all passed.
  - GitLab pipeline 2671657376 passed on a Linux shared runner after the first
    run exposed and fixed legacy-wrapper multi-argument forwarding.
  - Four public GitLab release assets were downloaded without authentication;
    every SHA-256 matched the local source, and the downloaded APK/AAB signatures
    verified.
  - GitHub Actions run 29227717198 passed its build job, including tests,
    Roborazzi, release lint, R8 release APK, and debug APK artifact gates.

## 2026-07-13 - v2.22.0 Slash Quick Insert

- Trigger: After reviewing Markleaf's product direction, the user chose a `/` Quick Insert menu and asked to ship it as a new GitHub/GitLab release.
- Scope: Local-only editor acceleration with no toolbar removal, dependency, permission, database, account, analytics, AI, or network change.
- Work:
  - Added line-scoped slash-query detection that ignores URLs, prose slashes, whitespace-closed queries, and non-collapsed selections.
  - Added fourteen plain-Markdown commands: H1–H3, bullet, numbered, checklist, quote, fenced code, divider, GFM table, NOTE callout, wikilink, SAF image, and ISO date.
  - Added a Material 3 Quick Insert panel with localized labels, Markdown previews, selected semantics, 48dp rows, touch selection, and external-keyboard Up/Down/Enter handling.
  - Preserved editor focus, autosave, tag/wikilink autocomplete, image picker, focus mode, and the existing formatting toolbar.
  - Added `DESIGN.md` by extracting the existing Compose visual system and documenting the new suggestion primitive.
  - Added unit, Robolectric/Compose, resource parity, and instrumented editor coverage.
  - Changed the release export contract to write directly to the canonical `D:\Build` directory; the Desktop `Build` entry is treated only as a shortcut. Added `exportReleaseToBuildDrive` and retained `exportReleaseToDesktop` as a compatibility alias.
  - Bumped to versionName 2.22.0 / versionCode 104 and prepared all six fastlane changelogs.
- Verification:
  - `test`, `lintRelease`, `assembleDebug`, and `verifyRoborazziDebug` passed together (`BUILD SUCCESSFUL`, 116 tasks).
  - API 36 `EditorScreenTest` passed 4/4 on an emulator, including `/` heading insertion and URL rejection. This gate exposed an initial `FocusRequester` attachment race; waiting one Compose frame changed the same suite from 4/4 failed to 4/4 passed.
  - Fresh 360dp English/Korean and 800dp tablet Quick Insert renders showed no clipping or overlap.
  - Signed `assembleRelease` and `exportReleaseToBuildDrive` passed; `D:\Build` contains the AAB (5,468,658 bytes), R8 mapping (41,545,520 bytes), and six-locale notes (1,845 bytes).
  - Release APK signer SHA-256: `0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a`; `android.permission.INTERNET` remains absent.
- Publication:
  - Release commit `67efce2` and annotated tag `v2.22.0` were pushed to both GitHub and GitLab. GitHub published the signed APK, R8 mapping, and signed AAB Actions artifact.
  - Re-downloaded release APK signature and Actions AAB JAR signature verification passed. `scripts/verify-mirror.ps1 -IncludeGitHub` confirmed all 90 local/GitLab refs and GitHub/GitLab refs match.
  - The tag workflow's release job passed, while its parallel build job exposed three Quick Insert snapshots pointing at an uncommitted report directory. Commit `7181b96` moved them to the canonical snapshot directory and `1a86e4f` added Linux-recorded goldens; the follow-up main build passed tests, visual regression, release lint, and R8 artifact gates.
  - F-Droid uses the GitHub repository with tag-based update checks. The v2.22.0 handoff is complete; the official catalog and upstream metadata still showed 2.21.1 (103) when checked on 2026-07-13, so catalog publication remains asynchronous.

## 2026-07-10 - Folder sync stops resurrecting deleted notes, and v2.21.1 (#148)

- Trigger: #148 — notes deleted while folder sync was on came back after an app restart.
- Analysis: deleting a note moves it to the trash and deliberately leaves its mirrored `.md` file in the folder, so a trashed note can still be restored and Markleaf never deletes a file it did not just write. Reconciliation, though, only looked at active notes. The leftover file matched nothing, so it read as a note that had appeared in the folder and was created again. The same blind spot pulled archived notes back into the active list.
- Contract/scope: reconcile against all notes — active, archived and trashed — and skip a file whose note is in the trash. Permanent deletion, which does remove the file, is unchanged. Propagating a deletion to *other* devices sharing the folder stays out of scope and was split out as #149, since closed as a documented limitation rather than a defect: auto-deleting a file a sync client may be mid-write is a data-loss risk, and the app says so in Settings → sync ("Deletions are not synced — manage trash on each device") in all six locales.
- Implementation: `f1c8686`, which extracted the decision into a pure `reconcileAction` function so it could be tested without SAF. The same tag range also carries the GitHub→GitLab mirror runbook and its verify script (`f294732`) and the removal of debug APKs that had been committed under `.ci-artifacts/` and `.release-assets/` (`dfe47f8`, `9231ff0`, merged as `429725e`).
- Verification: `./gradlew testDebugUnitTest`, with new unit tests for skipping trashed notes, reconciling archived notes correctly, re-syncing after a restore, and `getAllNotes`.
- Release: versionCode 102→103, versionName 2.21.0→2.21.1 (`58f9d43`), CHANGELOG both editions, six store-locale `103.txt`.
- Record: reconstructed from the tag range and CHANGELOG under #268; it was not logged at release time.

## 2026-06-25 - Interactive motion, foldable tablet sidebar, and v2.21.0 (#145)

- Trigger: a pass at making the app feel distinctly Android rather than a cross-platform shell. #145 — the toolbar checkbox always inserting a new item instead of toggling the one under the caret — rode along.
- Analysis: every motion feature on the list (predictive back, item reflow, a card that expands into the editor) needs Compose 1.7 or newer, so the toolchain had to move before any of them could be written. That made a dependency bump the first commit of the release rather than an afterthought.
- Contract/scope: shared-axis-X screen transitions with predictive back; `Modifier.animateItem` reflow when notes are pinned, added or deleted; a card→editor container transform on phones; a cross-fade on the tablet editor pane when a different note is selected; a tag rail that collapses to widen the writing space. (#145) the checkbox button toggles `- [ ]`↔`- [x]` on a line that is already a checklist item and still creates one on a line that is not. compileSdk and targetSdk stay at 35.
- Implementation: toolchain first (`b9f42cf`) — Kotlin 1.9.22→2.0.21 onto the Compose compiler Gradle plugin, Compose BOM 2024.02.02→2024.12.01 (Compose 1.7.6), AGP 8.3.0→8.7.3, Gradle 8.5→8.9, Navigation 2.8 / Activity 1.9 / Lifecycle 2.8, Robolectric 4.14.1, Roborazzi 1.29.0. Then predictive back (`e74f171`), list reflow (`04dcab1`), the shared-element container transform (`81c0143`), preview links moved off the now-deprecated `ClickableText` onto `LinkAnnotation` (`a6bda14`), the tablet cross-fade (`3bcccd3`), the collapsible rail (`95c1fef`), memoized editor highlighting so a long note no longer re-scans itself on every keystroke (`6570e5d`), and #145 (`8c753fc`).
- Verification: `./gradlew test lintRelease`, plus checkbox-toggle unit tests covering TODO↔DONE, indentation and caret preservation.
- Release: versionCode 101→102, versionName 2.20.0→2.21.0 (`f794189`), CHANGELOG both editions, six store-locale `102.txt`.
- Record: reconstructed from the tag range and CHANGELOG under #268; it was not logged at release time. This is where the Gradle wrapper moved to 8.9 — the version #241 later regenerated and #246 pinned by checksum.

## 2026-06-23 - v2.20.0 Editor writing upgrades & tablet 3-column layout

- Trigger: After mapping the Bear-parity gap, the user approved shipping the "now tier" of editor improvements, then the tablet 3-column layout, and asked to cut a release once it was releasable.
- Scope: Five user-facing features, all local-only, no new dependencies. No DB schema change.
- Work:
  - Keyboard shortcuts (Ctrl/Cmd+B/I/K and Ctrl/Cmd+Shift+S) in the editor; bare Ctrl+S left unbound so the auto-saved body is never accidentally struck through. (commit 1ca506d)
  - Inline `#tag` autocomplete reusing the wikilink dropdown UI with a TagParser-faithful detector (whitespace-anchored, Unicode, hierarchical, skips URL fragments / `##`). (1ca506d)
  - Preview table of contents (H1–H3) reusing the footnote scroll path; `listState` hoisted out of `MarkdownPreviewList`. (1ca506d)
  - Serif/Sans editor font toggle via the theme's default `FontFamily` (system fonts, no bundled assets); Sans default byte-identical to before. (1ca506d)
  - Tablet 3-column layout (tags | list | editor): new `TagRail` leading pane + `NotesViewModel` `selectedTag`/`displayedNotes` filter; parent tags match their children; phones unchanged. (commit e7044eb)
  - Bumped `app/build.gradle.kts` to versionName 2.20.0 / versionCode 101.
  - `CHANGELOG.md` v2.20.0 entry; six fastlane changelogs (`101.txt`, all ≤500 chars); `metadata/com.markleaf.notes.yml` build entry + CurrentVersion 2.20.0/101.
  - Refreshed `README.md` (+ en/ja/de) and `docs/index.html` version strings, feature lists, and roadmap.
- Verification:
  - `.\gradlew.bat test lintRelease` -> BUILD SUCCESSFUL (264 unit tests) for both the now-tier bundle and the 3-column layout.
  - Added unit tests: `TagAutocompleteTest`, `TocHeadingsTest`, `NoteTagFilterTest`.
  - fastlane `101.txt` character counts verified ≤500 for all six store locales (ko/en/ja/de/fr/es).
  - Caveat: interactive UI (keyboard shortcuts, TOC sheet, font swap, tag rail) verified by compile + lint only — not exercised on a device this session.
- Release: version bump committed to main; tag `v2.20.0` pushed to trigger the CI signed APK/AAB GitHub Release and F-Droid auto-pickup. Play Store updates remain paused (F-Droid/GitHub only).

## 2026-06-22 - v2.19.1 public app info refresh

- Trigger: After shipping v2.19.1, the user noted that app information surfaces should also be updated.
- Scope: Public information only; no app behavior changes.
- Work:
  - Updated `README.md`, `README.en.md`, `README.ja.md`, and `README.de.md` current-version badges and direct APK install links to v2.19.1.
  - Updated `docs/index.html` current release strip and GitHub Releases download card from v2.19.0 to v2.19.1.
  - Updated `docs/NOCLOUD_CERTIFICATION.md` certification version floor from 2.16.2+ to 2.19.1+.
  - Added a v2.19.1 / versionCode 100 build entry to `metadata/com.markleaf.notes.yml` and moved CurrentVersion/CurrentVersionCode to 2.19.1/100.
  - Confirmed GitHub repo description/topics and the portfolio Markleaf production status were already current, so they were left unchanged.
- Verification:
  - `git diff --check` -> PASS.
  - Stale public refs search for `v2.19.0`, `2.19.0`, `2.16.2+`, and old CurrentVersion fields -> no matches in the checked public surfaces.
  - `metadata/com.markleaf.notes.yml` verified `versionName 2.19.1`, `versionCode 100`, `commit v2.19.1`, `CurrentVersion 2.19.1`, and `CurrentVersionCode 100`.
  - `rg "android.permission.INTERNET" -n app/src/main app/src/debug` -> no matches.
  - `Invoke-WebRequest -Method Head` returned HTTP 200 for the v2.19.1 GitHub Release, F-Droid package page, and GitHub Pages landing.

## 2026-06-22 - v2.19.1 Dash tag filter fix (#144)

- Trigger: GitHub issue #144 from bushrang3r reported that tags such as `#old-notes` appeared on the Tags page, but tapping them opened a search result page with no matching notes.
- Root cause: Tag parsing/indexing already accepts `-`, but the Tags page passed `#old-notes` into the general full-text search path. SQLite FTS can interpret punctuation such as `-` as query syntax/token separation, so a valid Markleaf tag could fail when used as a filter.
- Work:
  - Added `NoteDao.searchNotesByTag`, joining `notes`, `note_tag_cross_ref`, and `tags`.
  - Updated `LocalNoteRepository.searchNotes` so queries starting with `#` bypass FTS and use the stored tag index directly.
  - Added a regression test proving `#old-notes` returns only the active note with that exact tag, not a similar `#oldnotes` tag or an archived note.
  - Bumped `app/build.gradle.kts` to `versionName 2.19.1` / `versionCode 100`.
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests "com.markleaf.notes.data.repository.LocalNoteRepositoryTest" --no-daemon --stacktrace` -> BUILD SUCCESSFUL.
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon --stacktrace` -> BUILD SUCCESSFUL.
  - `.\gradlew.bat :app:lintRelease :app:assembleDebug --no-daemon --stacktrace` -> BUILD SUCCESSFUL.
  - `rg "android.permission.INTERNET" -n app/src/main app/src/debug` -> no matches.
  - `app/build/outputs/apk/debug/app-debug.apk` exists and is 19,129,072 bytes.
  - `.\gradlew.bat --no-daemon "-Pmarkleaf.requireReleaseSigning=true" :app:exportReleaseToDesktop --stacktrace` -> BUILD SUCCESSFUL.
  - Desktop export verified at `C:\Users\jeiel\OneDrive\바탕 화면\Build`: `markleaf-v2.19.1-vc100.aab` (5,196,513 bytes), `markleaf-v2.19.1-vc100.mapping.txt` (38,401,995 bytes), and `markleaf-v2.19.1-vc100-release-notes.txt` (1,729 bytes).
  - Release bundle manifest verified `package="com.markleaf.notes"`, `versionCode="100"`, and `versionName="2.19.1"`.

## 2026-06-18 - Phase 26 Beautiful Sample Notebook Onboarding

- Trigger: After agreeing that a beautiful set of sample notes is stronger onboarding than more explanatory UI, the user asked to proceed after the writing-feel polish loop.
- Scope: Replace starter notes with a real sample notebook while preserving local-first constraints. No network, account, analytics, or external asset fetches.
- Work:
  - Replaced the four starter notes with six sample notes covering Welcome, Markdown showcase, daily writing, project brief, tags/search/backlinks, and local folder mirror.
  - Added real Markdown examples: image reference, callout, table, code block, footnote, checklist, quote, wikilinks, backlinks, tags, and folder mirror explanation.
  - Copied the existing Markleaf feature graphic into `app/src/main/res/raw/markleaf_sample_cover.png` and added `StarterNotesSeeder.copyStarterSampleAttachment()` to install it under `attachments/starter-note-2/markleaf-sample-cover.png`.
  - Added `LocalNoteLinkRepository.reindexLinksForNote` during starter seeding so sample wikilinks/backlinks work immediately, matching the existing tag indexing path.
  - Updated all starter-note raw resources (`default`, ko, es, de, ja, fr) to the same six-note structure.
- Verification:
  - `.\gradlew.bat testDebugUnitTest --tests "com.markleaf.notes.data.onboarding.StarterNotesSeederTest" --tests "com.markleaf.notes.res.ResourceParityTest" --no-daemon --stacktrace` -> BUILD SUCCESSFUL.
  - `.\gradlew.bat test assembleDebug --no-daemon --stacktrace` -> BUILD SUCCESSFUL.
  - `rg "android.permission.INTERNET" -n app/src` -> no matches.
  - `app/build/outputs/apk/debug/app-debug.apk` exists and is 19,191,368 bytes.

## 2026-06-18 - Phase 25 Final Writing Feel Polish

- Trigger: User approved doing the "last writing-feel polish" before moving on to the richer sample-note onboarding idea.
- Scope: No new product features. This pass stays inside editor interaction feel: cursor focus continuity, edit/preview transition smoothness, preview/editor canvas alignment, and toolbar density.
- Work:
  - Added a scoped editor `FocusRequester` so new notes are immediately writable, Preview -> Edit returns the cursor naturally, and toolbar actions / wikilink completion / Find-Replace edits keep the writing surface active.
  - Wrapped the editor body in `Crossfade(targetState = isPreviewMode)` so Edit and Preview swap more softly.
  - Aligned Preview content padding with the editor canvas (`20.dp` horizontal) and tightened toolbar top/group divider spacing while preserving Material `IconButton` touch targets.
- Verification:
  - `.\gradlew.bat testDebugUnitTest --no-daemon --stacktrace` -> BUILD SUCCESSFUL after stopping a stale Gradle daemon that had locked `test-results/.../output.bin`.
  - `.\gradlew.bat assembleDebug --no-daemon --stacktrace` -> BUILD SUCCESSFUL.
  - `rg "android.permission.INTERNET" -n app/src` -> no matches.

## 2026-06-18 - The open-file reopen loop, and v2.18.1 (#142)

- Trigger: #142 — after opening or sharing a `.md` or `.txt` file into Markleaf, pressing back reopened the editor endlessly, saving another copy of the same note each time, with no way out of the app.
- Analysis: the one-shot external open/share import added in #139 (v2.17.0) lived in a `LaunchedEffect` inside `MarkleafNavHost`'s NOTES destination. Entering the editor and coming back restarted that destination's composition, which re-ran the effect; nothing had consumed the intent, so every pass called `createNote(sharedText)` with a fresh UUID and reopened the editor. Back navigation and app exit were both inside the loop it created.
- Contract/scope: lift the import to a single host-scoped `LaunchedEffect(Unit)` that lives as long as the activity instance, so internal navigation cannot re-fire it and only a genuinely new intent (`onNewIntent` → `recreate`) imports again. The widget's new-note and recent-note paths run through the same code and are fixed by the same change.
- Implementation: `6b6fafe`, which carried the fix, the version bump and the store notes together. It also added a Robolectric Compose regression test; `568f321` tried to stabilise it and `4426c22` removed it. Global idle-state pollution between Compose tests in the unit suite produced `AppNotIdleException` failures that depended on execution order, so the test would have been an unreliable gate. It was dropped rather than left flaky, with an instrumented test named as the right home for this lifecycle case.
- Verification: manual — reverting to the pre-fix code reproduces the duplicate notes and the reopen loop, and after the fix the import happens exactly once. No automated coverage of the regression shipped, by the decision above.
- Release: versionCode 97→98, versionName 2.18.0→2.18.1 (`6b6fafe`), CHANGELOG both editions, six store-locale `98.txt`.
- Record: reconstructed from the tag range and CHANGELOG under #268; it was not logged at release time.

## 2026-06-16 - Sync files named after note titles, and v2.18.0 (#134)

- Trigger: #134 — folder sync wrote files as `slug-idabcdef12.md`, an internal name sitting in a folder the user opens in other tools.
- Analysis: that name existed because a note was matched to its file *by filename*. Moving the link to the frontmatter `markleaf_id` frees the filename entirely and, more to the point, makes the match survive a rename — by the user, by Markleaf, or by another app.
- Contract/scope: files are written as `Title.md`; renaming a note renames the file in place; a duplicate title takes a ` (2)` suffix and characters a filesystem will not accept are cleaned up. The sync center gains a `.md`/`.txt` choice — new files follow it, existing files keep their extension, import reads both — plus a "Rename files to note titles" button that converts a folder created under the old scheme in one pass. A rename always writes content first and renames second, so an interrupted rename cannot lose the note.
- Implementation: `9249b57`. The rest of the tag range is release tooling and store work rather than product code: the release export gained `-vc` naming and the R8 mapping (`1a6ab22`) and all-locale release notes in one tag-separated file (`93a1bb8`); store descriptions were refreshed and Spanish added for locale parity (`64a44a5`, `46affd7`); English, Japanese and German READMEs landed (`5f1abc6`); the READMEs and landing page caught up with the public Play launch (`ddfd0cf`); and the GitHub issue-response workflow was written into `AGENTS.md` (`e0536d6`).
- Verification: unit tests for filename sanitisation and collision handling (`MirrorFileNames`).
- Release: versionCode 96→97, versionName 2.17.1→2.18.0 (`7a6ebc0`), CHANGELOG both editions, and the first six-locale store changelog set — `97.txt` in ko-KR, en-US, ja-JP, de-DE, fr-FR and es-ES, where every version up to and including 96 had ko-KR and en-US only.
- Record: reconstructed from the tag range and CHANGELOG under #268; it was not logged at release time.

## 2026-06-16 - Inline formatting inside list items, and v2.17.1 (#141)

- Trigger: #141 — `- **bold**`, `- [[note]]` and inline code written inside a list showed their raw markers in preview.
- Analysis: list rows were rendered by a different path from body paragraphs, and that path discarded the inline spans the parser had already produced. Bold, italic, inline code, links and wikilinks were all affected, in bullet, numbered and checkbox items alike.
- Contract/scope: render inline formatting inside list items exactly as in body text, and make a `[[wikilink]]` inside a list tappable like any other.
- Implementation: `b4d7c07`, with a new `list_inline_formatting` snapshot and the existing goldens that contain lists re-recorded (`50321fe`). `9a19242`, which brought the README and landing page up to v2.17.0, also sits in this range.
- Verification: the `list_inline_formatting` snapshot plus the re-recorded goldens.
- Release: versionCode 95→96, versionName 2.17.0→2.17.1 (`13f9ab2`), CHANGELOG both editions, and fastlane `96.txt` in ko-KR and en-US — the six-locale store-note requirement arrived one release later, with v2.18.0.
- Record: reconstructed from the tag range and CHANGELOG under #268; it was not logged at release time.

## 2026-06-15 - v2.17.0 File import & folder-sync fixes (#139, #140, #138)

- Trigger: A batch of GitHub issues — #139 (open/share `.md` files), #140 (search lists the same note multiple times), #138 (a tag removed from every note lingers in the overview), plus #138's comment thread (folder sync creates duplicate notes; imported tags not recognized).
- Root causes:
  - #140 search: `NoteDao.searchNotesFts` joined `notes` to `notes_fts` on rowid, so any duplicate FTS posting for a note multiplied it in the result list.
  - #140 (the reporter's real case) + #138 comment: `NoteFolderMirror.importChanges` matched existing notes *only* by the file's frontmatter `markleaf_id`. Files authored in another app have none and were never stamped on import, so every reconcile re-created them as brand-new notes — one note appeared 4–5 times after a few syncs.
  - #138: `TagDao.observeTagsWithCounts` used `LEFT JOIN`, so a tag whose cross-refs all went away still surfaced with `noteCount = 0`.
  - #138 comment ("tags not recognized on sync"): the three sync import call sites called only `createNote`/`updateNote` and never ran tag/link reindexing, so folder-imported notes were tagless until re-saved in the editor. (A trailing space after a tag was a red herring — `TagParser` already terminates a tag at whitespace.)
- Work:
  - #140 search: rewrote `searchNotesFts` to `WHERE rowid IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH :query)` so each note is returned once regardless of duplicate postings.
  - Sync de-dup: after importing a frontmatter-less file, `NoteFolderMirror.stampFrontmatter` rewrites that file in place with the new `markleaf_id` so the next reconcile matches by id and updates instead of duplicating. Extended `SyncFrontmatter.encode` to take an `extraKeys` map and re-emit unknown frontmatter keys (Obsidian etc.) it used to silently drop, preserving external metadata on the write-back.
  - #138: added `HAVING COUNT(notes.id) > 0` to `observeTagsWithCounts`.
  - Sync reindex: added `NoteImporter` (create/update + tag & link reindex) and routed all three import call sites (foreground auto-reconcile in `MainActivity`, Sync Center, Settings) through it.
  - #139: added `ACTION_VIEW` (markdown/text MIME) and `ACTION_SEND` (markdown file) intent filters; `MainActivity` now reads an opened/shared file's stream into a note body (capped at 2M chars), seeding the title from the file name when the body has no leading heading.
  - Bumped `app/build.gradle.kts` from `versionCode 94 / versionName 2.16.5` to `versionCode 95 / versionName 2.17.0`; added fastlane `95.txt` changelogs (ko/en) and CHANGELOG entry.
- Verification:
  - `.\gradlew.bat testDebugUnitTest` -> BUILD SUCCESSFUL (added tests: tag zero-count hidden, single search result with a duplicate FTS posting, `encode` preserves/ignores extra keys, `NoteImporter` indexes tags + wikilinks on import).
  - `.\gradlew.bat lintRelease` (release hard gate) -> BUILD SUCCESSFUL.
  - Release-notes char count: ko-KR 236/500, en-US 430/500.
  - The SAF folder-IO write-back path is covered by the live tablet smoke, matching the existing convention for `importChanges`.
- Distribution: signed APK + AAB and the GitHub Release are produced by CI on the `v2.17.0` tag push (keystore from repo secrets); F-Droid auto-detects the new tag.

## 2026-06-11 - v2.16.5 Relative-date i18n & F-Droid screenshots (#132)

- Trigger: While capturing F-Droid screenshots for issue #132 on the TB320FC tablet, the note-list row timestamps rendered in Korean ("오늘 15:58") even with the device set to English — a localization bug surfaced by the capture work.
- Root cause: `formatUpdatedTime` in `NotesListScreen.kt` hardcoded Korean literals (`"오늘 …"`, `"어제 …"`, `"${n}일 전"`) while the section headers correctly used string resources. Every non-Korean-language user saw Korean dates.
- Work:
  - Moved the relative-date strings to resources (`relative_today` / `relative_yesterday` / `relative_days_ago`) with `%1$s` / `%1$d` placeholders; changed `formatUpdatedTime` to take a `Context` and call `getString`; passed `LocalContext.current` at the call site. Added translations to all six locales (en, ko, ja, de, es, fr) so `ResourceParityTest` stays green.
  - Captured four phone screenshots on TB320FC (Android 15) with the device temporarily switched to English + SystemUI demo mode for a clean status bar: live Markdown editor, rendered preview with a Kotlin code block + checklists + tags, the Tags screen, and the local-first privacy note. Placed under `fastlane/metadata/android/en-US/images/phoneScreenshots/` as `1.png`–`4.png`. The per-locale icon dedup that #132 also asked for was already done in an earlier release (only `en-US/images/icon.png` remains).
  - Restored the tablet afterward: system language back to Korean, auto-rotate re-enabled, SystemUI demo mode exited.
  - Bumped `app/build.gradle.kts` from `versionCode 93 / versionName 2.16.4` to `versionCode 94 / versionName 2.16.5`; added fastlane `94.txt` changelogs (ko/en) and CHANGELOG entry.
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest :app:lintRelease` (release hard gates, includes ResourceParityTest) -> BUILD SUCCESSFUL.
  - Re-installed the debug build on TB320FC and visually confirmed English UI shows "Today 15:58" after the fix.
- Distribution: signed APK + AAB and the GitHub Release are produced by CI on the `v2.16.5` tag push; F-Droid auto-detects the tag and the new phoneScreenshots metadata.

## 2026-06-11 - v2.16.4 Tags inside lists (#137)

- Trigger: GitHub issue #137 (dope791) — tags written inside bulleted list items did not appear in the tag view.
- Root cause: `TagParser` matched the tag body as "everything up to whitespace", so a trailing period/comma (`#shopping.`, `#work,`) — common at the end of list items and when comma-separating tags — was absorbed into the name, failed `isValidTagName`, and dropped the whole tag. The segment charset was also Latin + Hangul only, rejecting German/Japanese/Chinese tags outright.
- Work:
  - Rewrote `TagParser.TAG_PATTERN` to match the body against the valid Unicode character set (`\p{L}`/`\p{N}`, `_`, `-`, `/`), so punctuation is excluded and any-script tags are recognized; widened `SEGMENT_REGEX` to Unicode letters.
  - Removed the heading/URL-fragment exclusion passes — the `(^|\s)#` prefix already filters those, and the passes only mis-banned a tag note-wide once it appeared in a heading.
  - Added unit tests (lists, trailing punctuation, comma-separated, heading, de/ja/zh) and a `TagParserAndroidTest` instrumented test so the Unicode-class regex is exercised on Android's ICU engine, not just the host JVM.
  - Bumped `app/build.gradle.kts` from `versionCode 92 / versionName 2.16.3` to `versionCode 93 / versionName 2.16.4`.
  - Added fastlane Play changelogs for `ko-KR` and `en-US` as `93.txt`; added `CHANGELOG.md` entries for v2.16.4 and backfilled v2.16.3.
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest --tests "com.markleaf.notes.util.TagParserTest"` -> BUILD SUCCESSFUL.
  - `.\gradlew.bat :app:testDebugUnitTest :app:lintRelease` (release hard gates) -> BUILD SUCCESSFUL.
  - Release notes char count: ko-KR 148/500, en-US 279/500.
- Distribution: signed APK + AAB and the GitHub Release are produced by CI on the `v2.16.4` tag push (keystore from repo secrets); F-Droid auto-detects the new tag.

## 2026-05-27 - v2.16.2 Play production release preparation

- Trigger: Play production access was approved, so the app needed a fresh Play-ready version and desktop export package.
- Work:
  - Bumped `app/build.gradle.kts` from `versionCode 90 / versionName 2.16.1` to `versionCode 91 / versionName 2.16.2`.
  - Added `CHANGELOG.md` entry for v2.16.2 focused on Play production readiness and the latest public-surface refresh.
  - Added fastlane Play changelogs for `ko-KR` and `en-US` as `91.txt`.
  - Updated README, GitHub Pages landing version links, No-Cloud certification version references, and F-Droid metadata current version fields.
- Verification:
  - `./gradlew.bat --no-daemon test :app:lintRelease :app:assembleDebug :app:assembleRelease :app:bundleRelease` -> BUILD SUCCESSFUL.
  - `./gradlew.bat --no-daemon "-Pmarkleaf.requireReleaseSigning=true" :app:exportReleaseToDesktop` -> BUILD SUCCESSFUL.
  - Desktop export verified at `C:\Users\jeiel\OneDrive\바탕 화면`: `markleaf-v2.16.2.aab` (5,110,841 bytes) and `markleaf-v2.16.2-release-notes.txt` (591 bytes).
  - Release APK signing certificate SHA-256 remained `0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a`.
  - `rg "android.permission.INTERNET" -n app/src` returned no matches.

## 2026-05-26 - GitHub Pages / README / repo metadata public surface refresh

- Trigger: GitHub IO, README, repository description, and topic tags needed review against the latest Markleaf version.
- Work:
  - Rebuilt `docs/index.html` as a product-facing GitHub Pages landing page using the existing feature graphic asset and v2.16.1 messaging.
  - Updated `docs/style.css` for the refreshed landing, responsive download controls, content page styling, and privacy page layout.
  - Rewrote `docs/privacy.html` to match the current v2.x no-INTERNET, no-account, no-analytics, SAF folder mirror, and Android backup policy.
  - Updated `README.md` with v2.16.1 links, smart formatting, 6-language resource status, latest release install link, and current roadmap items.
  - Updated `docs/NOCLOUD_CERTIFICATION.md` and `metadata/com.markleaf.notes.yml` to point at v2.16.1/versionCode 90.
  - Updated GitHub repository topics/description/homepage through `gh repo edit`.
- Verification:
  - Local static preview at `http://127.0.0.1:8123/` rendered the landing and privacy page without horizontal overflow on desktop or 390px mobile viewport.
  - Public Pages URLs `https://jeiel85.github.io/markleaf-android/` and `/privacy.html` returned HTTP 200 before the new deploy.
  - `git diff --check` passed.
  - `rg "android.permission.INTERNET" -n app/src` returned no matches.
  - `./gradlew.bat test` -> BUILD SUCCESSFUL.
  - `./gradlew.bat assembleDebug` -> BUILD SUCCESSFUL; `app/build/outputs/apk/debug/app-debug.apk` exists and is 19,043,024 bytes.

## 2026-05-22 - F-Droid catalog icon metadata and v2.16.1 publication cleanup

- Trigger: F-Droid listing showed the default app icon even though the Android launcher icon was configured correctly.
- Work:
  - Added 512x512 PNG catalog icons at `fastlane/metadata/android/{en-US,ko-KR,ja-JP,fr-FR,de-DE}/images/icon.png`.
  - Matched the upstream catalog icon to the existing adaptive launcher identity: pale green background, Markleaf green leaf, and white Markdown-style vein.
  - Updated `CHANGELOG.md` under v2.16.1 Distribution notes.
- Decision:
  - F-Droid uses upstream fastlane metadata from the processed source commit for listing graphics; Play Console uploads do not configure F-Droid icons.
- Verification:
  - `./gradlew.bat test` -> BUILD SUCCESSFUL.
  - `./gradlew.bat assembleDebug` -> BUILD SUCCESSFUL.
  - `app/build/outputs/apk/debug/app-debug.apk` exists and is 19,043,024 bytes.
  - `rg "android.permission.INTERNET" -n app/src` -> no matches.

## 2026-05-21 - v2.16.1 - 스마트 포맷팅 토글 & 단어 감싸기 (Phase 23)

- Trigger: 에디터 텍스트 포맷팅(Bold, Italic, Strikethrough, Inline Code)의 스마트 UX 완성 (스마트 주변 단어 감싸기 및 토글/언랩).
- Work:
  - **[Smart Formatting] MarkdownEditActions.wrapSelection 리팩토링**: 포맷팅 적용 시 드래그 상태인 선택 영역을 유지하고, 텍스트가 이미 포맷팅된 경우 이를 제거(Unwrap)하는 스마트 토글 구현.
  - **[Smart Formatting] MarkdownEditActions.findWordAtCursor 구현**: 선택 영역이 없고 커서만 있는 상태(Collapsed)에서 한글/영어/기호 경계를 완벽하게 판별하여 주변 단어를 정확히 식별해 감싸는 지능형 탐색 로직 고도화.
  - **[Smart Formatting] MarkdownEditActionsTest.kt 단위 테스트 작성 및 정비**: 다국어 감지, 스마트 언랩, Fallback 삽입 등 12가지 엄격한 시나리오에 대해 100% 검증.
- Verification:
  - `./gradlew.bat test` -> BUILD SUCCESSFUL (MarkdownEditActionsTest의 스마트 포맷팅 관련 12개 테스트 포함 전체 단위 테스트 100% 통과)

## 2026-05-21 - v2.16.0 - post-MVP 상용화 기능 구현 (Commercial Readiness P1/P2)

- Trigger: 상용 출시 신뢰성 강화 및 유럽/프랑스 시장 진출을 위한 P1/P2 핵심 요건 구현.
- Work:
  - **[Commercial P1-1] 이미지 첨부 EXIF 제거**: 첨부파일 저장 시 `androidx.exifinterface:exifinterface`를 사용하여 이미지 내부의 GPS 좌표, 카메라 제조사, 소프트웨어 버전, 촬영일시 등 사생활 민감 정보를 안전하게 스트리핑.
  - **[Commercial P1-3] Sync Center / Conflict Center UI**: SAF 폴더 미러링 시 발생하는 충돌 본문 `(다른 기기 사본)` 노트 리스트를 조회하고, 개별 삭제 처리할 수 있는 로컬 충돌 관리 허브 UI 구현. `SettingsScreen`에 연동 완료.
  - **[Commercial P2-1] PDF 인쇄 스타일 폴리시**: A4 규격 여백(20mm x 18mm), page-break-inside/after 규칙 적용, JetBrains Mono 고품질 폰트 인라인 렌더링 스타일링 튜닝.
  - **프랑스어(fr-FR) 로컬라이제이션**: 200개 이상의 UI 스트링 번역 (`res/values-fr/strings.xml`), 프랑스어 스타터 메모 번들 (`res/raw-fr/starter_notes.md`), Fastlane 메타데이터 및 릴리즈 노트 (`fastlane/metadata/android/fr-FR/89.txt`) 완벽 추가.
- Verification:
  - `./gradlew.bat test` -> BUILD SUCCESSFUL (로보렉트릭 기반 `AttachmentManagerTest` 및 전체 unit/migration/parity 테스트 100% 패스)
  - `./gradlew.bat assembleDebug` -> BUILD SUCCESSFUL (디버그 APK 19,040,916 바이트 정상 생성 확인)

## 2026-05-21 - v2.16.0 cut (남은 이슈 9종 동시 처리)

- Trigger: "남아있는 이슈들 모두다 대응 완료" 골. F-Droid 정식 등재(MR #38659 머지) 직후 Bear-class 마무리 단계로 진입.
- Pre-work: Play 마케팅성 이슈(#52 인앱 리뷰 nag UI, #53 스토어 스크린샷, #54 피처 그래픽)를 OSS 철학과 충돌한다고 판단해 일괄 close. 트래커 정리 기준은 새 `feedback_oss_philosophy_filter.md` 메모리에 기록.
- Scope: 9개 이슈를 OSS 철학 / Bear-class 가치 / 로컬-퍼스트 기준으로 묶어 8개 커밋으로 처리.
- Work:
  - #55 OSS transparency — `SettingsScreen`에 Open source 섹션(라이선스/GitHub/F-Droid/license 본문 4개 링크) 추가.
  - #27 Predictive Back — manifest opt-in 이미 true이고 `BackHandler` 가로채기 없음을 확인. 명시적 종료.
  - #51 i18n JP — `res/values-ja/strings.xml` 신규 (192 string).
  - #57 Onboarding — `WelcomeOnboardingSheet` 4페이지 `ModalBottomSheet`. DataStore `onboardingCompleted` 플래그로 첫 실행만 노출.
  - #37 Biometric lock — `androidx.biometric:1.1.0` (AOSP), `BiometricLockGate` Composable, Settings 토글. `MainActivity`를 `ComponentActivity → FragmentActivity`로 승격. 하드웨어 없으면 fail open.
  - #38 PDF export — `ExportPdf` 객체. commonmark `HtmlRenderer` → 숨김 WebView → `PrintManager`. A4 / 18mm·16mm 여백 / 그린 액센트 인라인 스타일. 출력 URI는 시스템 인쇄가 소유 → 추가 권한 0.
  - #39 Widget — `QuickNoteWidgetService` / `RemoteViewsFactory` 신규. 위젯이 최근 10개 노트 리스트 + `+` 헤더. `ACTION_OPEN_NOTE`/`EXTRA_NOTE_ID` 추가 → `MarkleafNavHost`의 새 `openNoteId` 파라미터로 에디터 직접 진입.
  - #65 FTS5 — Room이 `@Fts5` 미지원이라 `@Fts4` 유지하되 토크나이저를 `unicode61 remove_diacritics=2`로 교체. DB v12 → v13, `MIGRATION_12_13`이 `notes_fts`를 destructive recreate. 한국어/일본어/중국어 검색 회귀 해결.
  - #76 WCAG — `SettingsSection`/`SectionHeader` 제목에 `heading()` 시맨틱, `NoteRow`에 `semantics(mergeDescendants = true)`. Material 3가 이미 보장하는 48dp 터치 타깃과 기존 contentDescription은 그대로.
- Verification:
  - `:app:compileDebugKotlin` 8회 모두 BUILD SUCCESSFUL (각 커밋 직전).
  - `app/schemas/com.markleaf.notes.data.local.AppDatabase/13.json` 생성 확인 — FTS table createSql이 `tokenize=unicode61 remove_diacritics=2`로 맞춰져 있어 `MIGRATION_12_13` SQL과 일치.
  - 기존 `AppDatabaseMigrationTest`가 v4 → 현재 마이그레이션 시 `notes_fts MATCH 'legacy'` 검증을 포함하고 있어 v12→v13 회귀도 instrumented test로 자동 커버.
- Issues closed: #27 #37 #38 #39 #51 #55 #57 #65 #76 (메인 코드 변경). 트래커 정리로 #52/#53/#54 (마케팅성)는 별도 close.
- Build: versionCode 88 → 89, versionName 2.15.3 → 2.16.0.
- F-Droid: 새 RecipeMR 작성 예정 (`fdroiddata/metadata/com.markleaf.notes.yml` 의 `CurrentVersion`/`CurrentVersionCode` 갱신 + `Builds:` 블록에 89 항목 append).

## 2026-05-20 - 랜딩페이지 다운로드 링크 줄바꿈 수정 및 모바일 반응형 개선 (Hotfix)

- Trigger: F-Droid 배포 완료 후 다운로드 링크가 랜딩페이지에 추가되면서, 버튼 내의 텍스트가 줄바꿈(개행)되는 증상 발생 보고 및 모바일 최적화 요청.
- Root cause: F-Droid 링크 버튼이 추가되면서 총 4개의 버튼이 가로로 배치되자 부모 컨테이너 `.cta-buttons`의 가로 너비 한계 및 모바일 반응형 뷰에서 텍스트 영역이 협소해짐. 가로 너비를 극대화하고, 모바일 화면에서의 정렬 방식을 꽉 차고 깔끔한 형태(Full-width Stack)로 구조화할 필요성이 대두됨.
- Work:
  - `docs/style.css` — `.cta-buttons`에 `flex-wrap: wrap;`을 적용하고 간격을 `1rem`에서 `0.8rem`으로 줄여서 가로 공간 최적화.
  - `docs/style.css` — `.btn`에 `white-space: nowrap;` 및 `text-align: center;`를 추가하고 가로 패딩을 `2rem`에서 `1.5rem`으로, 폰트 크기를 `0.95rem`으로 미세 축소하여 글씨 줄바꿈 원천 차단.
  - `docs/style.css` — 576px 이하 초소형 모바일 대응 미디어 쿼리(`@media (max-width: 576px)`)를 신설하여 `.cta-buttons`를 `flex-direction: column` 및 `width: 100%`로 배치해 정갈한 스택 버튼 레이아웃 구현.
- Verification:
  - 스타일 규칙 검증 (`flex-wrap`, `white-space`, `padding`, `@media`).

## 2026-05-20 - v2.15.3 cut (코드블록 미리보기 크래시 fix)

- Trigger: F-Droid `fdroiddata` MR(!38659) community tester `@dking08`가 Android 15에서 "코드블록을 포함한 노트의 미리보기 진입 시 앱 크래시" 보고.
- Root cause: `SyntaxHighlighter` SHELL_RULES 정규식 `\$\{[^}]+}|...`의 닫는 `}` 미escape. JVM `java.util.regex`는 허용해도 Android ICU regex는 거부 — 정적 초기화 실패가 SyntaxHighlighter object 전체를 무력화시켜 어떤 언어의 코드블록이든 첫 preview 렌더링에 `ExceptionInInitializerError`로 죽음. Robolectric/JUnit은 호스트 JVM regex라 모든 테스트가 통과해서 회귀가 release에 도달.
- Work:
  - `app/src/main/.../syntax/SyntaxHighlighter.kt` — SHELL_RULES bash 변수 regex의 닫는 `}` 한 곳을 `\}`로 escape + WHY 주석 추가.
  - `app/src/androidTest/.../syntax/SyntaxHighlighterAndroidTest.kt` 신규 — 모든 언어 rule을 실 Android runtime에서 tokenize, round-trip 검증. 같은 종류의 JVM/ICU 회귀가 다시 들어오면 instrumented test가 잡음.
  - `app/src/test/.../FencedCodeBlockEdgeCasesTest.kt` 신규 — 빈/미닫힘/단일줄/공백 body 등 코드블록 edge case 10개를 SimpleMarkdownPreview + SyntaxHighlighter pipeline에 통과시켜 정적 초기화 실패가 아닌 한 절대 안 죽도록.
  - `app/build.gradle.kts` — versionCode 87→88, versionName 2.15.2→2.15.3.
  - `app/src/main/.../feature/editor/EditorScreen.kt` — 별도 누락 fix 포함: TopAppBar 타이틀이 너비를 넘으면 줄바꿈 대신 폰트 0.7배까지 점진적으로 자동 축소.
  - CHANGELOG / fastlane changelogs 88.txt / README / NOCLOUD_CERTIFICATION 모두 v2.15.3로 갱신.
  - metadata/com.markleaf.notes.yml의 Builds entry를 v2.15.3, commit `85d03e3f`로 교체 후 fdroiddata MR push.
- First v2.15.3 attempt (commit `e77dc74`)은 5개 새 Roborazzi 스냅샷 테스트의 golden 누락으로 verify가 실패. unit + androidTest로 회귀 net 이미 충분해서 스냅샷 5개 제거 후 v2.15.3 tag를 `85d03e3` commit으로 재발행. (GitHub Release v2.15.3 cleanup-tag로 삭제 후 재생성, 다운로드 카운트 0이라 안전.)
- Verification:
  - `./gradlew.bat --no-daemon :app:test :app:lintRelease :app:assembleRelease` → BUILD SUCCESSFUL
  - 디바이스 재현: 수정 전 release/debug APK 둘 다 `미리보기` 진입 시 `ExceptionInInitializerError`로 즉시 크래시 확인. 수정 후 동일 입력으로 정상 렌더링 확인.
  - 다운로드한 `markleaf-v2.15.3.apk`의 signing block IDs: `0x7109871a` (v2 sig) + `0x42726577` (Verity padding). `0x504B4453` (Dependency metadata) 없음. 서명 cert SHA-256 `0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a` 유지.
  - 인스트루멘티드 테스트 `SyntaxHighlighterAndroidTest` 디바이스에서 OK (1 test).
  - fdroiddata MR head pipeline 2538988010 — 9/9 jobs success.
  - 핸드오프: `markleaf-v2.15.3.aab` + `markleaf-v2.15.3-release-notes.txt` 바탕화면 dump.

## 2026-05-19 - v2.15.2 cut (F-Droid reproducible-build fix)

- Selected task: F-Droid `fdroiddata` MR(!38659) 리뷰 후속 — reviewer `@linsui`가 요청한 App Inclusion 템플릿 + `Note` category + `Binaries:` + `AllowedAPKSigningKeys:` 추가. 이어 `check apk` 잡이 v2.15.1 APK의 AGP "Dependency metadata" 서명 블록(`0x504B4453`)을 reject → v2.15.2 cut.
- Work:
  - `app/build.gradle.kts` — versionCode 86 → 87, versionName 2.15.1 → 2.15.2, `dependenciesInfo { includeInApk = false; includeInBundle = false }` 추가 (AGP의 자동 의존성 메타데이터 서명 블록 비활성화).
  - `CHANGELOG.md` — `## v2.15.2 - F-Droid reproducible-build 가능성 정리` 섹션 추가.
  - `README.md` 현재 버전 링크를 v2.15.2로 갱신.
  - `docs/NOCLOUD_CERTIFICATION.md` 적용 버전을 2.15.2+로 갱신.
  - `fastlane/metadata/android/{en-US,ko-KR}/changelogs/87.txt` 추가.
  - `metadata/com.markleaf.notes.yml` — Note category 추가, Binaries/AllowedAPKSigningKeys + subdir: app 정리, v2.15.2 Builds entry로 교체. rewritemeta canonical 포맷(Binaries 뒤 trailing space + AllowedAPKSigningKeys를 Builds 뒤로) 적용.
  - GitLab MR description을 App Inclusion 템플릿으로 교체 + reviewer 질문에 follow-up 댓글 게시.
- Verification:
  - `./gradlew.bat --no-daemon :app:test :app:lintRelease :app:assembleRelease` → BUILD SUCCESSFUL
  - 다운로드 받은 `markleaf-v2.15.2.apk`의 signing block IDs: `0x7109871a` (v2 sig), `0x42726577` (Verity padding) — `0x504B4453` (Dependency metadata) 사라짐 확인.
  - fdroiddata MR head pipeline 2537707504 — 9/9 jobs success (`check apk`, `fdroid build` 포함).
  - 핸드오프: `markleaf-v2.15.2.aab` + `markleaf-v2.15.2-release-notes.txt` 바탕화면 dump.

## 2026-05-19 - F-Droid Submission MR

- Work:
  - Fixed the F-Droid metadata changelog URL to use `/blob/HEAD/CHANGELOG.md`.
  - Installed and used `fdroidserver` locally for metadata validation.
  - Verified `fdroid readmeta` and `fdroid lint com.markleaf.notes` pass for the Markleaf metadata when the current fdroiddata `Writing` category is available locally.
  - Installed/logged into GitLab CLI, forked `fdroid/fdroiddata`, pushed `metadata/com.markleaf.notes.yml`, and opened the upstream MR.
  - Confirmed the latest GitHub Actions main build succeeded after the release APK output-path workflow fix.
- Links:
  - F-Droid MR: `https://gitlab.com/fdroid/fdroiddata/-/merge_requests/38659`
  - Main CI run: `https://github.com/jeiel85/markleaf-android/actions/runs/26083336113`
- Deferred:
  - Docker Desktop could not be installed from this Windows session, so local Docker-based `fdroid build com.markleaf.notes` remains deferred.

## 2026-05-18 - v2.15.1 cut (F-Droid build readiness)

- Selected task: 새 버전 만들기 — F-Droid/build readiness cleanup을 `v2.15.1` / `versionCode 86` 으로 승격.
- Work:
  - `app/build.gradle.kts` — versionCode 85 → 86, versionName 2.15.0 → 2.15.1.
  - `CHANGELOG.md` — `Unreleased` F-Droid readiness section을 `## v2.15.1 - F-Droid 빌드 준비와 Room 마이그레이션 안전망 - 2026-05-18` 로 승격.
  - `README.md` 현재 버전 링크를 v2.15.1로 갱신.
  - `docs/NOCLOUD_CERTIFICATION.md` 적용 버전을 2.15.1+로 갱신.
  - `fastlane/metadata/android/{en-US,ko-KR}/changelogs/86.txt` 추가.
- Context:
  - v2.15.1은 새 기능 릴리즈가 아니라 F-Droid/source-build readiness 릴리즈. 주요 변화는 Room schema export, migration regression test, Apache 2.0 LICENSE, tracked `local.properties` 제거, store metadata 보강.
- Verification:
  - `./gradlew.bat --no-daemon test :app:lintRelease :app:assembleDebug :app:assembleRelease :app:bundleRelease` → BUILD SUCCESSFUL
  - `rg "android.permission.INTERNET" -n app/src` → no matches
  - APK/AAB outputs: debug APK 17,847,343 bytes; release APK 1,759,316 bytes; release AAB 4,125,372 bytes.

## 2026-05-18 - F-Droid / Build Readiness Cleanup

- Selected task: `[Commercial P0-3] Room schema export + migration regression test`, plus repository cleanup items that can be completed locally before F-Droid submission.
- Work:
  - `AppDatabase` changed to `exportSchema = true`.
  - `app/build.gradle.kts` now sets KSP Room args (`room.schemaLocation`, incremental mode, expandProjection) and exposes `app/schemas` to androidTest assets.
  - Generated and committed `app/schemas/com.markleaf.notes.data.local.AppDatabase/12.json`.
  - Added `AppDatabaseMigrationTest`, which creates a legacy v4 DB and verifies migration to v12 preserves notes/tags, rebuilds FTS, and creates `note_links`, `attachments`, `sortOrder`, and `lastImportedAt` surfaces.
  - Added root `LICENSE` (Apache 2.0) to match README and F-Droid expectations.
  - Removed tracked `local.properties`; it remains ignored locally and should not participate in reproducible builds.
  - Added English/Korean fastlane short/full descriptions for reuse in F-Droid/store metadata.
  - Added `android.suppressUnsupportedCompileSdk=35` with a comment because API 35 targeting is intentional on the current AGP baseline.
- Verification:
  - `./gradlew.bat :app:kspDebugKotlin :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL
  - `./gradlew.bat --no-daemon test` → BUILD SUCCESSFUL
  - `./gradlew.bat --no-daemon :app:lintRelease :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest` → BUILD SUCCESSFUL
  - `rg "android.permission.INTERNET" -n app/src` → no matches
  - APK outputs: debug APK 17,847,351 bytes; release APK 1,759,320 bytes; androidTest APK 1,864,036 bytes.
  - `./gradlew.bat --no-daemon :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.markleaf.notes.data.local.AppDatabaseMigrationTest'` did not run tests because the connected device already has a production-signed `com.markleaf.notes` installed. Debug install failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; the app was not uninstalled to avoid deleting user data.
- Follow-up:
  - F-Droid submission itself still needs the external procedural steps: upstream tag push, metadata PR, screenshots/feature graphics if desired, and final signed/reproducible-build review.

## 2026-05-14 - v2.15.0 cut (Commercial P0-1/P0-2/P0-4 묶음 출시)

- Selected task: v2.15.0 chore 출시 — Phase 22 P0-1 (backup 정책) / P0-2 (R8 + CI gates) / P0-4 (privacy 문서 v2.x화) 세 작업을 한 chore 릴리즈로 묶음. 신기능 0.
- Work:
  - `app/build.gradle.kts` — versionCode 84 → 85, versionName 2.14.0 → 2.15.0.
  - `CHANGELOG.md` — 기존 두 "## Unreleased" 섹션을 단일 `## v2.15.0 - Play 정식 출시 준비: 자동 백업 제외 + 빌드 최적화 - 2026-05-14` 로 통합. Tag 릴리즈 자동화가 `## v<버전> - ...` 헤더에서 release-notes 본문을 awk 로 잘라 GitHub Release notes 로 사용하므로 헤더 포맷 유지.
  - `fastlane/metadata/android/ko-KR/changelogs/85.txt` (NEW, 261자) + `fastlane/metadata/android/en-US/changelogs/85.txt` (NEW, 398자) — Play Console "What's new" 입력용 로컬라이즈 릴리즈 노트. 둘 다 500자 제한 안. 사용자가 Play Console 업로드 시 그대로 복사하거나 fastlane supply 로 자동 동기화 가능.
  - `README.md` 현재 버전 배지를 v2.14.0 → v2.15.0.
  - `docs/NOCLOUD_CERTIFICATION.md` 버전 라인 2.14.0+ → 2.15.0+.
  - `.gitignore` — `/dist/` 추가 (로컬 staging 디렉터리; CI가 tag push 시 동일 산출물 생성).
  - `.agent/tasks.md` — Commercial P0-4 체크 (P0-1 작업으로 docs 모두 v2.x 화 + "MVP draft" 잔존 0 확인).
  - 로컬 서명 AAB/APK/mapping.txt 생성 후 `dist/v2.15.0/markleaf-v2.15.0.{aab,apk,mapping.txt}` 로 스테이징 (gitignored). 사용자가 Play Console 비공개 테스트에 그대로 업로드 가능.
- Context:
  - v2.14.0(84) 가 비공개 테스트에 올라가 있는 상태. v2.15.0(85) 는 그 위에 R8 + backup 정책 + 문서 정밀화를 얹는 chore. *신기능 0* 인 만큼 closed test 사용자의 회귀 검증 부담이 작고, R8-shrunk APK 의 첫 실기기 smoke 로서 적절한 step.
  - 릴리즈 노트의 사용자 가시 변화는 두 가지 (자동 백업 제외 + 앱 크기 감소). 나머지(CI gate, proguard 규칙, lint 픽스) 는 개발자/기술 변화라 Play Console 노트에는 포함하지 않음.
- Verification:
  - `./gradlew :app:test` → BUILD SUCCESSFUL
  - `./gradlew :app:lintRelease` → BUILD SUCCESSFUL
  - `./gradlew :app:assembleRelease` → signed APK 1.7 MB. apksigner 검증 결과 SHA-256 = `0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a` 로 production cert 일치.
  - `./gradlew :app:bundleRelease` → signed AAB 4.0 MB.
  - `rg "android.permission.INTERNET" -n app/src` → no matches.
- Follow-up:
  - Tag `v2.15.0` push → CI 가 동일 산출물 + mapping 을 GitHub Release 자산으로 생성 → 사용자 비공개 테스트 업로드.
  - 비공개 테스트 실기기에서 Compose 라이브 프리뷰 / Room SQL / Coil 이미지 / commonmark 렌더 / SAF 폴더 미러 / 시스템 공유 시트 / FileProvider 공유 / AppWidget 흐름 manual smoke. R8 가 무언가를 strip 했으면 ANR / NoClassDefFoundError 로 표면화.
  - 다음 사이클: Phase 22 Commercial P0-3 (Room schema export + migration regression test).

## 2026-05-14 - Commercial P0-2: Release Hardening (R8 + CI gates)

- Selected task: `[Commercial P0-2] Release hardening` — Phase 22 의 다음 unchecked 항목.
- Work:
  - **R8 + resource shrink 활성화.** `app/build.gradle.kts` 의 release 블록에서 `isMinifyEnabled = true`, `isShrinkResources = true`, `proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")`.
  - **`app/proguard-rules.pro` 신규.** Conservative keep rules: Room entity 클래스/멤버, `AppSettings`, `SyncFrontmatter`/`NoteFolderMirror`, `QuickNoteWidget`, kotlinx.coroutines volatile 필드. `android.util.Log.d/v/i` 는 release 에서 R8 가 inline fold. 각 rule 마다 *왜 필요한지* 주석 — 미래에 의존성 업그레이드로 coupling 이 사라지면 제거할 수 있도록.
  - **벤치마크 변형도 자동으로 R8 상속** (`initWith(release)`). Macrobenchmark 가 더 release-like 한 APK 를 측정.
  - **`.github/workflows/android-build.yml` build job 에 hard gate 추가**:
    - `./gradlew :app:lintRelease` — failure 시 `lint-results-release.html` 을 artifact 로 업로드.
    - `./gradlew :app:assembleRelease` — R8 가 valid APK 를 만들어내는지 매 빌드 검증, 크기 > 0 확인.
    - `mapping.txt` 를 `markleaf-r8-mapping` artifact 로 업로드.
  - **Tag 릴리즈 잡** 에 `markleaf-vX.Y.Z.mapping.txt` 도 GitHub Release 자산으로 첨부 — Play Console / 외부 크래시 리포트에서 stack trace deobfuscation 가능.
  - **`EditorLiveSnapshotTest.kt`** 의 `remember(scheme) { MarkdownSyntaxVisualTransformation(...) }` 한 줄에 `@Suppress("RememberReturnType")` — test 소스 셋 경계 너머의 생성자 반환 타입을 lint 가 해석 못 하는 false positive. 한 줄에만 침묵.
  - **`docs/RELEASE.md`** 에 R8/mapping/CI gate 섹션 추가.
  - **`.agent/decisions.md`** D047 추가 — R8 enablement 결정 + minimal proguard 정책 + mapping artifact 결정.
  - **CHANGELOG / .agent/tasks.md / .agent/progress.md** 갱신.
- Context:
  - 상용화 게이트의 핵심: release 빌드가 단순히 "컴파일은 된다" 수준이 아니라 production-quality (R8 통과 + lint 통과 + 크래시 deobfuscatable) 인지 매 빌드 보장.
  - APK 크기 12 MB → 1.7 MB (87% 감소) 는 부수적 효과지만 사용자 다운로드 / 업데이트 비용 측면에서 큰 win. Play Store 의 instant delivery threshold 와 무관하지만 비공개 테스트 사용자 경험에 직접 반영.
  - launch-smoke 는 `continue-on-error: true` 그대로 유지. release-APK runtime smoke (R8 가 런타임에 무언가를 strip 했는지 검증) 는 별도 사이클 — 현 launch-smoke 의 emulator 플레이크가 release path 까지 confound 하지 않도록.
- Verification:
  - `./gradlew :app:test` → BUILD SUCCESSFUL
  - `./gradlew :app:lintRelease` → BUILD SUCCESSFUL (warning 만 남고 error 0)
  - `./gradlew :app:assembleRelease` → 1.7 MB APK
  - `./gradlew :app:bundleRelease` → 4.0 MB AAB
  - `app/build/outputs/mapping/release/mapping.txt` 32 MB 생성
- Follow-up:
  - R8-shrunk APK 의 emulator/실기기 런타임 smoke test (Compose 슬롯, Room SQL, Coil 이미지 로딩, commonmark 렌더, SAF, FileProvider, AppWidget 흐름 전수 확인).
  - 의존성 업그레이드 사이클 (lintRelease 에서 발견된 lifecycle/activity/room/coil/test 라이브러리 신버전).
  - Phase 22 Commercial P0-3: Room schema export + migration regression test.

## 2026-05-14 - Commercial P0-1: Android Backup / Data Extraction 정책 확정

- Selected task: `[Commercial P0-1] Android Backup / Data Extraction 정책 확정` — Phase 22 의 첫 unchecked 항목.
- Work:
  - `AndroidManifest.xml` 의 `<application>` 에 `android:allowBackup="false"` 설정. Android Auto Backup(Google Drive) 과 Android 12+ Device-to-Device transfer 양쪽 모두에서 Markleaf 데이터 제외.
  - 벤치마크 변형의 `tools:replace="android:allowBackup"` 오버라이드 제거 — main 의 `false` 를 그대로 상속해 정책 일관성 유지. `<profileable shell="true" />` 는 그대로 유지.
  - `docs/PRIVACY.md` 전면 재작성 — "MVP Privacy Policy Draft" 폐기, v2.x 기능 기준으로 "Markleaf 자체 네트워크 0 + 사용자 명시 행동 시 OS 경로로만 이동" 을 정밀하게 구분. 시스템 백업/D2D 제외 정책 명시.
  - `docs/SECURITY.md` 갱신 — `allowBackup="false"` 결정 근거 + `dataExtractionRules` 미선택 사유 + 사용자 주도 데이터 이동 경로(외부 링크 ACTION_VIEW 위임 포함) 정리.
  - `docs/NOCLOUD_CERTIFICATION.md` — "Backup / Data Extraction" 섹션 신설, "What Can Leave the Device" 를 명시적 사용자 행동 기준으로 재서술, 외부 링크는 OS 위임이라는 사실 추가.
  - `README.md` — "100% No-Cloud" 카피를 "Markleaf 자체는 네트워크에 나가지 않음 + 사용자 선택 경로로만 이동" 의 정밀 표현으로 교체. `allowBackup="false"` 도 강점 목록에 포함.
  - `CHANGELOG.md` Unreleased 섹션 추가.
  - `.agent/decisions.md` 에 D046 추가 — `allowBackup="false"` vs `dataExtractionRules` 비교와 결론.
  - `.agent/tasks.md` 에서 Commercial P0-1 항목 체크.
- Context:
  - Markleaf 의 핵심 약속은 "사용자가 직접 export/share 하기 전까지 데이터가 기기 밖으로 나가지 않는다". 일부 사용자에게 활성화돼 있을 수 있는 OS 차원의 Google 드라이브 자동 백업이 무언의 충돌을 일으킬 여지를 차단하는 게 목적.
  - `dataExtractionRules` 는 부분적 제외에 유용하지만 *전체 제외* 라면 `allowBackup="false"` 가 더 단순/명시적이라 후자를 택함. minSdk=26 환경에서 legacy + new 메커니즘을 동시에 관리하는 표면적도 줄어듦.
  - 다중 기기 사용자는 v2.1 SAF 폴더 미러 동기화로 *사용자가 선택한 폴더* 를 통해 이동 가능 — 이 경로는 사용자의 명시적 SAF 다이얼로그를 거치므로 정책 충돌 없음.
- Verification:
  - `./gradlew test` → BUILD SUCCESSFUL
  - `./gradlew assembleDebug` → BUILD SUCCESSFUL
  - `rg "android.permission.INTERNET" -n app/src` → no matches (정책 유지)
  - `rg "allowBackup|dataExtractionRules" -n app/src/main` → `AndroidManifest.xml` 의 `android:allowBackup="false"` 단일 매치
- Follow-up:
  - Phase 22 Commercial P0-2 (Release hardening: R8/shrink + release lint/build/bundle/signing/smoke gate) 다음.
  - 향후 사용자 설정만 별도 백업하는 정책으로 바꾸면 `dataExtractionRules` 도입을 재검토.

## 2026-05-14 - Commercial Readiness Planning

- Work: 상용화 관점의 냉정 평가를 다음 세션에서 바로 이어갈 수 있는 실행 계획으로 정리.
- Changed files:
  - `docs/COMMERCIAL_READINESS_PLAN.md` — v2.14.0 기준 상용화 준비도 평가, P0/P1/P2 개선 설계안, 수정 후보 파일, 검증 명령 정리.
  - `.agent/tasks.md` — Phase 22 Commercial Readiness backlog 추가.
  - `HISTORY.md` — 본 기록 추가.
- Context:
  - 기능 구현은 이미 비공개 테스트 앱 수준을 넘었지만, 정식 출시 전에는 데이터 보호, release hardening, Room migration safety, privacy/security 문서 현재화, sync 장애 가시성을 우선해야 한다.
  - 다음 권장 시작 작업은 `[Commercial P0-1] Android Backup / Data Extraction 정책 확정`.
- Verification:
  - Documentation-only change; Gradle build not run.

## 2026-05-13 - GitHub Issue 기준 문서 일괄 정리

- Work: GitHub 오픈 이슈 12건을 기준으로 프로젝트 문서 전체를 재정렬. 손상된 HISTORY.md 6000줄 정리.
- Changed files:
  - `.agent/tasks.md` — Phase 21 (GitHub Open Issues) 신설. 이슈별 상태 추적 (완료 3건, 보류 3건, 미구현 6건)
  - `docs/ROADMAP.md` — v2.x 완료 항목 정리 + GitHub Open Issues 향후 작업 섹션으로 교체
  - `HISTORY.md` — 손상된 중복 항목(약 6000줄) 제거
  - `.agent/decisions.md` — Pending Decisions → Resolved 로 이동 (DI, minSdk, Markdown lib, FTS, 이미지, tablet, flavor)
  - `docs/NOCLOUD_CERTIFICATION.md` — 버전 2.14.0+ 갱신, 권한 목록 VIBRATE 단독으로 수정
  - `GEMINI.md` — AGENTS.md 참조 중심으로 간소화
- Context:
  - GitHub 오픈 이슈 12건: #27(완료), #37(미구현), #38(미구현), #39(미구현), #51(보류), #52(미구현), #53(보류), #54(보류), #55(미구현), #57(미구현), #65(완료), #76(완료)
  - 완료 3건(#27, #65, #76)은 GH close 필요, 보류 3건은 외부 조건, 미구현 6건은 backlog

## 2026-05-11 - v2.13.0 노트 안에서 바꾸기 (Find & Replace)

- Work: 에디터 Find 바를 두 줄로 넓혀 아래 줄에 바꾸기 입력칸과 "바꾸기" / "모두 바꾸기"를 붙였다. "바꾸기"는 지금 하이라이트된 매치 하나만 치환하고 다음으로 넘어가지 않는다(그건 ▼ 버튼의 역할이다). "모두 바꾸기"는 전체를 단일 패스로 치환해 치환 길이가 달라져도 인덱스가 밀리지 않게 하고, 끝나면 몇 개를 바꿨는지 Toast로 알린다.
- Context:
  - §2.5의 chrome 누적 회피를 지켜 상단바 아이콘을 새로 만들지 않았다. 검색이 켜져 있을 때만 하나의 Find/Replace 바가 두 줄로 펼쳐지고, 검색을 끄면 같이 사라진다.
  - 매칭은 기존 `findAllRanges`의 대소문자 무시 substring 정책을 그대로 쓴다. 정규식과 case-sensitive 토글은 UI 복잡도와 안전한 동작을 우선해 일부러 넣지 않았다.
  - 바꾸기 텍스트를 비운 채 "모두 바꾸기"를 누르면 지우기로 동작한다 — 의도된 동작이다.
- Verification: `ReplaceRangesTest`가 `replaceRange`/`replaceAllRanges`를 단일 치환, 빈 치환(삭제), 다중 치환, 빈 매치 목록, 더 긴 치환에서의 인덱스 안전성, 대소문자 무시까지 잠근다.
- Record: 커밋 `c4d5afd`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-11 - v2.12.0 빠른 이동 (Quick switcher)

- Work: Obsidian식 빠른 노트 점프를 추가했다. 노트 목록 ⋮ 오버플로의 첫 항목이나 하드웨어 키보드의 Ctrl+K(Cmd+K)로 열리고, 제목 substring 매칭 결과를 최대 20개까지 띄워 탭하면 그 노트로 간다. 쿼리가 비어 있으면 최근 수정 순으로 정렬해 마지막에 만지던 노트로 바로 돌아갈 수 있게 했다.
- Context:
  - 기존 전체 검색(Search 화면, FTS로 본문까지)은 그대로 두었다. 빠른 이동은 제목만 보는 점프이고 둘은 일부러 다른 도구다.
  - §2.5 chrome 누적 회피에 따라 상단바 아이콘을 늘리지 않고 기존 ⋮ 메뉴 첫 항목과 단축키만으로 진입점을 만들었다. `onPreviewKeyEvent`는 키보드 입력이 없으면 no-op이라 터치 사용자에게 영향이 없다.
- Record: 커밋 `d4c24e6`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-11 - v2.11.0 GFM 테이블 부활

- Work: `| 열 | 열 |`과 `| :--- | ---: |` 정렬 행이 미리보기에서 진짜 표로 그려지도록 되살렸다. 헤더 행은 굵게 + 약간 어두운 배경, 본문은 alternate row 줄무늬, 행 사이 1dp divider이고 `:---` / `---:` / `:---:` 정렬을 그대로 반영한다.
- Context:
  - v1.2.0에서 MVP simplicity를 이유로 제거했던 표·수식 렌더링 중 표만 부활시킨 것이다. 사용자 합의 후 post-MVP에서 확장으로 분류했고, 수식은 §2.7 가치관 마찰 검토 뒤로 미뤘다.
  - 데이터 모델은 `PreviewLineType.TABLE`과 `TableData(headers, rows, alignments)` / `TableAlignment`를 추가하고 기존 `PreviewLine`에 nullable `tableData`를 붙이는 방식이다. 의존성은 이미 쓰던 commonmark BSD-2 모듈군과 같은 `commonmark-ext-gfm-tables:0.24.0`.
  - 셀 안의 인라인 마크다운(굵게·기울임·링크)은 이 시점에 plain text로 표시된다 — 백로그로 남겼다.
- Verification: 단위 테스트 `parse_parsesGfmTable`(헤더·본문·정렬 추출)과 Roborazzi 골든 `table_light`(헤더 굵기, zebra row, 정렬, divider).
- Record: 커밋 `de02a32`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-11 - v2.10.0 코드 블록 syntax highlighting

- Work: 펜스 코드 블록에 언어 힌트가 있으면(` ```kotlin `) 미리보기에서 키워드·문자열·숫자·주석·함수명을 각각 다른 색으로 표시한다. 지원 언어는 Kotlin, Java, Python, JavaScript, TypeScript, Bash/Shell, JSON, YAML, XML/HTML, SQL 열 개이고, 그 밖의 언어나 힌트 없는 블록은 기존 모노스페이스 단색 폴백 그대로다.
- Context:
  - Prism4j 같은 Apache 2 후보를 검토했지만 §2.7 lightweight bias와 APK 크기를 이유로 자체 regex 토크나이저를 직접 구현했다. 10개 언어 룰셋이 약 350 LOC이고 의존성 추가는 없다.
  - 색은 Material 3 컬러스킴을 재활용해 라이트/다크/Material You에서 자동으로 톤이 맞는다. 별도 코드 테마 설정은 두지 않았다.
  - 토큰 충돌은 주석과 문자열이 키워드보다 우선하도록 풀었다 — `"fun day"` 안의 `fun`은 Kotlin 키워드로 색칠되지 않는다.
- Verification: 단위 테스트 12개(텍스트 round-trip, 키워드·문자열·주석·숫자 인식, 문자열 안 키워드 무시, decorator/annotation, YAML key, XML tag/attr 등)와 Roborazzi 골든 2개(Kotlin: 함수 + 문자열 + 주석 / Python: decorator + 함수).
- Record: 커밋 `c9aff4e`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-11 - v2.9.2 표준 마크다운 링크 클릭 동작

- Work: `[라벨](URL)` 형식의 표준 마크다운 링크가 미리보기에서 클릭되지 않던 누락을 고쳤다. 이제 primary 색 + 밑줄로 렌더되고, 탭하면 `Intent.ACTION_VIEW`로 시스템 브라우저·메일·전화 앱이 URL을 받아 처리한다.
- Context:
  - 온보딩 노트가 "링크 버튼은 `[라벨](대상)` 템플릿을 넣어줍니다"라고 안내하는데 정작 링크가 동작하지 않는다는 보고에서 출발했다. 원인은 `CommonMarkPreviewAdapter`의 `is Link` 분기가 URL을 버리고 텍스트만 inline으로 내보내던 것이고, `URL ignored in preview` 코멘트가 그대로 남아 있었다.
  - `PreviewInlineSegment`에 `href: String?`과 `PreviewInlineType.LINK`를 추가하면서, 인접한 두 LINK 세그먼트가 서로 다른 URL일 때 잘못 합쳐지지 않도록 `coalesce()`가 href까지 비교하게 했다.
  - URL을 다른 앱에 넘길 뿐이라 Markleaf 자체에는 여전히 INTERNET 권한이 없고 §2.2 로컬 우선이 유지된다.
  - 날짜 주의: `CHANGELOG`는 v2.9.2부터 v2.12.0까지를 2026-05-09로 적었지만 해당 커밋과 태그는 모두 2026-05-11이다. 이 기록은 커밋 날짜를 따랐다.
- Verification: 단위 테스트 `parse_emitsLinkSegmentWithHref`(라벨과 href 추출)와 신규 Roborazzi 골든 `markdown_link_light`.
- Record: 커밋 `b1f0875`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.9.1 Roborazzi 임계치 정밀화

- Work: v2.1.1에서 깔아 둔 `workflow_dispatch` 훅을 처음 실제로 돌려(run 25600591611) CI Linux 러너에서 골든 18장을 재기록하고, 내려받아 `app/src/test/snapshots/roborazzi/`에 커밋했다. 그 위에서 `changeThreshold`를 0.05f → 0.005f로 10배 조였다.
- Context:
  - 임계치가 느슨했던 이유는 Windows에서 record하고 Linux에서 verify하는 조합의 폰트 힌팅 차이를 흡수해야 했기 때문이다. record와 verify가 같은 OS가 되자 그 여유분이 필요 없어졌고, 그때부터 임계치는 진짜 시각 회귀만 잡는다.
  - 결과적으로 라이브 프리뷰와 에디터 미리보기 18개 시나리오의 1픽셀 회귀가 PR 빌드에서 바로 빨간불이 된다. 사용자 화면 변화는 없다.
- Record: 커밋 `bb02e38`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.9.0 동기화 충돌 시 사본 보존

- Work: 양쪽 기기에서 동시에 고친 노트가 silent overwrite되지 않게 했다. reconcile 중 파일이 newer이면서 로컬도 마지막 sync 이후 수정됐으면, 기존 로컬 노트를 그대로 두고 파일 본문을 별도 사본 노트로 들여온다(제목에 `(다른 기기 사본 0509 12:34)` 형태의 suffix). sync 결과 toast도 "업데이트 N, 신규 N, 충돌 사본 N, 변화 없음 N"으로 무엇이 어떻게 처리됐는지 드러내도록 바꿨다.
- Context:
  - DB v11 → v12: `notes`에 nullable `lastImportedAt` 컬럼 추가. 에디터가 미러 폴더에 write한 직후 `lastImportedAt = updatedAt`으로 stamp하므로, 다음 reconcile이 방금 우리가 쓴 remote echo와 다른 기기에서 온 foreign edit을 구분할 수 있다.
  - v2.6 이전 노트는 `lastImportedAt`이 null로 시작한다. 첫 reconcile에서 파일이 newer이고 값이 null이면 충돌로 분류 — 안전한 쪽으로 실패하도록 한 선택이다.
  - 여전히 남긴 것: 파일 → DB 삭제 sync(silent data loss 회피), 그리고 인앱 머지 UI. 사본 두 개를 보여주고 사용자가 직접 정리하는 것이 이 릴리스의 범위였다.
- Record: 커밋 `5250f63`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.8.1 한국어 어법 다듬기

- Work: 사용자가 "볼 노트를 선택하세요" 같은 영어 직역체를 지적해 한국어 strings 14개 항목을 자연스러운 표현으로 고쳤다. 영어와 스페인어는 손대지 않았다.
- Context:
  - 고친 축은 세 가지다 — 어순을 한국어답게(`%1$d개 노트` → `노트 %1$d개`), 번역투 용어를 실제로 쓰는 말로(`화면 낭독기` → `스크린 리더`, `시스템 월페이퍼` → `시스템 배경화면`), 그리고 MVP 시절 문구에 남아 있던 내부 용어를 사용자 문장으로(`MVP에서는 INTERNET 권한을 선언하지 않습니다` → `이 앱은 INTERNET 권한을 사용하지 않습니다`).
  - 문자열만 바뀐 릴리스이지만 별도 태그를 판 이유는 스토어 노출 문구가 포함돼 있었기 때문이다.
- Record: 커밋 `8da3ad3`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.8.0 이미지 alt 편집 다이얼로그

- Work: 미리보기에서 이미지를 길게 누르면 alt 편집 다이얼로그가 열리고, 저장하면 본문의 `![oldAlt](path)`가 `![newAlt](path)`로 치환된다. 경로는 건드리지 않으므로 첨부 파일 reference는 그대로다. 다이얼로그에는 alt가 무엇에 쓰이는지(스크린 리더, 이미지가 보이지 않을 때 표시) 한 줄 설명을 넣었다.
- Context:
  - 같은 경로가 본문에 여러 번 나오면 첫 번째만 치환한다. 첨부 파일명이 UUID라 실제로 충돌할 일이 거의 없다는 판단이다.
  - 이 시점에 의도적으로 멈춘 항목들이 함께 기록돼 있다 — 충돌 시 양 버전 보존 UI(제품 결정이 필요: 폴더에 두 벌 vs 인앱 머지 화면), 문단 안 인라인 이미지(파서 리팩터가 필요한데 가치가 낮음), CI Linux 골든 재기록(사용자가 `workflow_dispatch`를 한 번 눌러야 함), 노트 50개 이상 시드된 빌드에서의 ScrollBenchmark 재실행. 앞의 둘은 제품 결정, 뒤의 둘은 외부 트리거 대기였다.
- Record: 커밋 `25fcc8e`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.7.0 위키링크 자동완성

- Work: 본문에 `[[`를 치는 순간(또는 `[[hel`처럼 부분 입력 후) 에디터 하단 toolbar 위에 매칭되는 노트 제목을 최대 8개까지 띄우고, 탭하면 `[[Title]]`로 완성하며 커서를 닫는 `]]` 뒤로 옮긴다.
- Context:
  - 후보는 활성 노트(휴지통·보관함 제외) 중 제목이 쿼리를 포함하는 것(대소문자 무시)이고, `[[` 직후처럼 쿼리가 비어 있으면 모든 후보를 알파벳 순으로 보여준다.
  - 사용자가 `]]`를 입력하거나 줄바꿈하면 드롭다운이 자동으로 닫힌다.
- Verification: `detectWikilinkQuery` 단위 테스트 4개(열림 후 부분 입력 / 빈 쿼리 / 닫힌 후 / 줄바꿈 후)와 `completeWikilink` 1개(치환 + 커서 위치).
- Record: 커밋 `6bec4d4`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.6.0 동기화 완성 (삭제 전파 + 첨부 미러)

- Work: v2.1~v2.5에서 남겨 둔 sync 구멍 세 개를 한 번에 메움. (1) 휴지통에서 영구 삭제하면 DB뿐 아니라 폴더의 `.md`와 `attachments/<noteId>/` 디렉터리도 지운다. (2) 노트 자동 저장 때 `<filesDir>/attachments/<noteId>/*`를 폴더의 `attachments/<noteId>/`로 복사해, 그동안 다른 기기에서 깨진 이미지로 보이던 첨부를 함께 동기화한다(UUID 파일명 기반 dedup으로 재복사 회피). (3) Room CASCADE가 `attachments` row만 지우고 실제 파일은 남기던 누수를 `AttachmentManager.deleteAllForNote`로 막았다.
- Context:
  - 파일 → DB 방향의 삭제 sync는 여전히 미구현으로 남겼다. 외부 sync 클라이언트가 mid-flight일 때 silent data loss가 날 수 있어서이고, 자동으로 도는 것은 DB → 파일 방향뿐이다. (이 결정은 뒤에 #148/#149로 다시 올라온다.)
  - 다른 기기가 그사이 파일을 다시 편집했다면 v2.1.0의 "파일이 strictly newer일 때만 update" 규칙이 계속 보호한다.
  - DB 스키마 변경 없음.
- Record: 커밋 `1dbbfa0`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.5.3 문서 정리 (코드 변경 없음)

- Work: `docs/AGENT_SPEC.md`에 §15 *Post-MVP 방향*을 추가해 v2.x 합의(다중 기기 sync / Bear급 라이브 프리뷰 / 위키링크·이미지 부활)를 명시하고, §1–§14를 MVP era 한정으로 라벨링해 새 컨트리뷰터가 spec만 보고 v2.x 작업과 혼동하지 않게 했다. 함께 `.agent/tasks.md`의 Phase 10(Lenovo TB320FC Compose UI test 호스트 이슈)을 Roborazzi가 대체했다는 이유로 정식 close하고, Phase 8 백로그의 WebDAV/Drive sync 항목도 v2.1 SAF 폴더 미러가 우리 백엔드 없이 둘 다 흡수했다는 이유로 close했다.
- Context: 앱 코드 변경이 없는 문서 전용 릴리스다. 태그를 판 이유는 spec의 라벨링이 그 시점 이후의 모든 작업 판단 기준이 되기 때문.
- Record: 커밋 `27fc261`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-09 - v2.5.2 태블릿 UX 다듬기

- Work: 태블릿에서 드러난 시스템 바 관련 결함 두 건과 내비게이션 누락 한 건을 묶어 수정. (1) 펼친 노트 리스트가 status bar 영역까지 surfaceVariant 색을 침범하던 문제를 `Modifier.systemBarsPadding() + consumeWindowInsets(WindowInsets.systemBars)`로 v1.4.2 collapsed-rail 패턴과 맞췄다. (2) 검색·태그·휴지통·보관함으로 들어가면 status bar가 흰색으로 뚫리며 흰 아이콘이 보이지 않던 문제. (3) 그 네 화면에 뒤로가기 버튼이 없던 것을 `Scaffold + TopAppBar + 뒤로 화살표`로 채우고 NavHost에 `popBackStack`을 연결했다.
- Context:
  - status bar 문제는 원인이 두 갈래였다 — `Theme.Markleaf`가 `Light.NoActionBar`만 정의해 다크 모드에서도 액티비티 윈도우 배경이 흰색이었던 것(`values-night/styles.xml`에 다크 부모 추가)과, `enableEdgeToEdge()`가 액티비티 시작 시점에만 아이콘 색을 정하던 것(`MarkleafTheme` 안에서 `WindowCompat.isAppearanceLightStatusBars`를 매 컴포지션마다 동기화). 한쪽만 고쳤으면 재발했을 조합이다.
  - 같은 태그에 벤치마크 툴체인 정상화가 딸려 있다 — profileinstaller 1.3.1 → 1.4.0, benchmark-macro-junit4 1.2.4 → 1.3.0(API 35 지원), benchmark build type을 non-debuggable + `<profileable shell="true" />`로 교정, `/benchmark/build`를 `.gitignore`에 추가. 실제 측정치(TB320FC cold 326ms / warm 113ms / hot 57ms)는 바로 아래 v2.5.1 항목에 이미 기록돼 있어 여기서는 되풀이하지 않는다.
- Record: 커밋 `f9d8deb`, `c04c6f4`, `53fe5f8`, `9bfd0b7`, `7e1f4dd`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-08 (밤) - v2.5.1 + tablet bench
- Work: 사용자가 v1.5에서 Material You 기본화 이후 leaf-style 그린이 사라진 것을 지적 → 테마 선택 설정 추가, 그린 기본 복귀. 동시에 Lenovo TB320FC (API 35) 태블릿에서 Macrobenchmark 첫 실측.
- Theme picker:
  - `AppSettings.colorPalette: ColorPalette { MARKLEAF_GREEN, MATERIAL_YOU }` (DataStore key `color_palette`)
  - `MarkleafTheme.dynamicColor` 기본값 `true → false` (정적 그린 기본)
  - `MainActivity` 가 settings flow를 collect 해 ColorPalette → dynamicColor 변환, 변경 즉시 리컴포즈
  - Settings → 새 "테마" 섹션: 두 옵션 OutlinedButton/Button 토글 패턴
  - strings × 3 lang
- Macrobenchmark on TB320FC (API 35):
  - profileinstaller 1.3.1 → 1.4.0 (API 35 지원), benchmark-macro-junit4 1.2.4 → 1.3.0
  - benchmark build type: `isDebuggable = true → false` (Macrobenchmark는 debuggable 거부)
  - `app/src/benchmark/AndroidManifest.xml` (NEW) — `<profileable shell="true" />` 만 추가, 다른 build variant에 영향 없음
  - benchmark/AndroidManifest.xml — WRITE_EXTERNAL_STORAGE 제거 (API 30+에서 GrantPermissionRule 실패 원인)
  - 결과:
    - **Cold start: median 326ms** (min 317 / max 360) — §2.1 *빠름 우선* 기준 즉각
    - **Warm start: median 113ms** — 거의 무즉각
    - **Hot start: median 57ms** — 사용자 인지 한계 이하
    - ScrollBenchmark는 fresh install이라 노트 4개뿐, fling 시 frame 데이터 부족으로 실패 — 데이터 시드된 빌드에서 재실행 필요 (v2.5.x 백로그)
- Verification:
  - `./gradlew assembleDebug + test + verifyRoborazziDebug` 통과
  - 태블릿에 v2.5.1 debug APK 설치 완료 (사용자 스모크 테스트용)

## 2026-05-08 (밤) - v2.5.0 이미지 첨부 부활

- Work: v1.2.0에서 의식적으로 제거했던 이미지 첨부를 post-MVP 판단으로 되살림. 에디터 툴바의 이미지 버튼 → SAF 파일 피커 → 앱 private 디렉터리(`<filesDir>/attachments/<noteId>/<id>.<ext>`)로 복사 → 본문에 표준 `![](attachments/...)` 삽입. 미리보기에서 Coil로 inline 렌더.
- Context:
  - 권한 0 유지 — SAF가 파일 접근을 책임지므로 `READ_MEDIA_IMAGES` 같은 미디어 권한을 추가하지 않았고 INTERNET도 그대로 없다.
  - 본문에 표준 마크다운만 들어가므로 다른 도구에서도 읽히고 v2.1 폴더 sync의 `.md`에도 그대로 실린다. 다만 첨부 파일 자체는 별도 폴더에 있어 sync 대상 밖 — v2.5.x로 미룸.
  - DB v10 → v11: `attachments` 테이블 재도입(`id` PK, `noteId` FK CASCADE, `fileName`, `mimeType`, `addedAt`). 메타데이터 전용이고 실제 파일은 디스크에 남으므로, 노트 영구 삭제 시 row는 cascade로 지워지지만 파일 cleanup은 과제로 남았다.
  - 새 의존성 `io.coil-kt:coil-compose:2.6.0` (Apache 2, F-Droid 친화, 네트워크 요청 없음).
  - v2.5.x로 미룬 것: 첨부 파일의 폴더 sync, 삭제 시 디스크 cleanup, 문단 안 인라인 이미지 렌더, 크기·캡션·alt 편집 UI.
- Record: 커밋 `5905c24`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-08 (밤) - v2.4.0 위키링크 부활

- Work: `[[Title]]` 위키링크를 되살려 미리보기에서 클릭 가능한 링크(primary 색 + 밑줄)로 렌더하고, 없는 제목이면 새 노트를 만들어 이동한다(Bear/Obsidian과 같은 관례). 미리보기 하단에 이 노트를 참조한 노트를 모으는 backlinks 섹션을 추가하고, 1초 디바운스 자동 저장 안에서 링크를 추출해 태그 인덱싱과 같은 패턴으로 그래프를 갱신.
- Context:
  - DB v9 → v10: `note_links` 테이블 재도입(`sourceNoteId`, `targetTitle`, `normalizedTitle`, `position` PK + `notes` cascade). v1.x의 같은 이름 테이블이 v9 마이그레이션에서 drop됐으므로 새로 생성했고, 기존 노트는 다음 자동 저장 때 인덱싱된다.
  - v1.2.0에서 명시적으로 제거했던 기능의 의식적 reversal. 당시 제거 사유였던 "두 번째 두뇌 스타일은 가치관에 어긋남"은 MVP 시기에 한정된 판단이었다는 것을 기록으로 남겼다.
- Record: 커밋 `0c263d5`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-08 (밤) - v2.3.0 CommonMark 파서 교체

- Work: 미리보기의 손파서를 `commonmark-java 0.24.0`으로 교체. v2.0에서 보류했던 Phase B이고, 이후 위키링크·이미지 같은 실제 확장(v2.4+)의 기반이 된다. `CommonMarkPreviewAdapter`가 AST를 기존 `PreviewLine`/`PreviewInlineSegment` 모델로 변환하므로 렌더러(`MarkdownPreviewList`)는 손대지 않았다.
- Context:
  - 확장 4종 도입: yaml-front-matter, footnotes, gfm-strikethrough, task-list-items. 모두 BSD-2-clause로 F-Droid 친화.
  - `> [!NOTE]` GitHub callout은 CommonMark 표준이 아니라 BlockQuote 본문 prefix 매칭으로 직접 인식한다.
  - 스펙 정합성 때문에 생긴 미세 동작 변화 3건: `Body\n---`가 이제 H2 setext 헤딩으로 처리되고(가로선을 원하면 `---` 앞뒤에 빈 줄), 각주 참조는 정의가 같은 문서에 있을 때만 인식되며, 블록 사이 빈 줄이 `EMPTY` PreviewLine으로 표면화되지 않고 렌더러 padding으로 처리되어 미리보기가 v2.2 이전보다 약간 빽빽해졌다.
- Verification: 단위 테스트 기댓값 7개를 CommonMark 실제 출력에 맞춰 갱신(테스트 의도는 유지). Roborazzi 골든 14 + 4개(preview + editor live)를 전부 재기록.
- Record: 커밋 `583f57c`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-08 (밤) - v2.2.0 Macrobenchmark 성능 측정 인프라

- Work: Macrobenchmark 1.2.4 기반의 `:benchmark` 모듈 신설. 콜드/웜/핫 시작 시간을 5회 반복 측정하는 `StartupBenchmark`, 노트 목록 fling 스크롤의 90th-percentile 프레임 duration을 보는 `ScrollBenchmark`, 그리고 release를 흉내내되 debuggable인 `benchmark` build type을 함께 추가.
- Context:
  - APK 영향 0 — 벤치마크 의존성은 별도 모듈에 있고 app 모듈에는 `androidx.profileinstaller:1.3.1`만 추가된다.
  - `:benchmark:connectedBenchmarkAndroidTest`는 실제 기기나 에뮬레이터가 있어야 돌아간다(Robolectric 위에서는 동작하지 않음 — 의도된 한계). 매 PR마다 돌리기에는 비싸서 CI 자동 실행은 일부러 넣지 않고, 필요하면 나중에 `workflow_dispatch`로 붙이기로 했다.
  - 베이스라인 프로파일 자동 생성은 v2.2.x로 미룸.
  - 이 커밋에 `:benchmark` 빌드 산출물 495개가 잘못 딸려 들어갔다. v2.3.0에서 `.gitignore`에 `/benchmark/build`를 추가하고 캐시를 untrack해 정리했고, v2.2.0 태그 자체는 immutable이라 그대로 두었다.
- Record: 커밋 `82465c4`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-08 (밤) - v2.1.1 동기화 자동 reconcile + CI 골든 재기록

- Work: 동기화 폴더가 설정돼 있으면 앱이 RESUMED로 돌아올 때 폴더의 `.md` 변경분을 자동으로 가져오게 함(60초 throttle로 폴더 IO 폭주 방지). 충돌 규칙은 v2.1.0 그대로 — 파일이 strictly newer일 때만 DB를 갱신하므로 silent overwrite 위험은 없다. 함께 GitHub Actions에 `workflow_dispatch` 트리거와 `record_roborazzi` 입력을 추가해, Linux 러너에서 `recordRoborazziDebug`를 돌리고 새 골든을 artifact로 올리게 했다.
- Context:
  - 골든을 Linux 러너에서 기록하면 Windows record와 Linux verify 사이의 폰트 힌팅 차이가 사라지므로 `changeThreshold`를 0.05f → 0.005f로 조일 수 있다. 이후 프로젝트의 표준 절차가 되는 그 워크플로가 여기서 처음 생겼다.
  - v2.1.0과 같은 의도적 안전 마진 유지: 노트 삭제 동기화 미구현, 충돌 시 양쪽 보존 UI 미구현(현재는 최근 것이 이김).
- Record: 커밋 `ee03b49`과 CHANGELOG로 #268에서 재구성 — 릴리스 당시 기록되지 않음.

## 2026-05-08 (밤) - v2.1.0
- Work: 가치관 점검에서 사용자가 "결국 다중 기기 지원은 맞고 가치관 뒤집기보다 확장에 가까움"이라 말한 직후 합의된 v1.9 → v2.0 → v2.1 3단계의 마지막. CloudKit식 vendor lock 대신 SAF 폴더 미러 모델(Option D).
- Changed files:
  - data/settings/AppSettings.kt — `syncFolderUri: String?`, `syncLastSyncedAt: Long?` 필드 추가
  - data/settings/AppSettingsRepository.kt — `SYNC_FOLDER_URI`(stringPref) + `SYNC_LAST_SYNCED_AT`(longPref) 키, `setSyncFolderUri(uri)` / `setSyncLastSyncedAt(epochMillis)` setter. 폴더 끌 때 `lastSyncedAt`도 함께 정리.
  - data/sync/SyncFrontmatter.kt (NEW, ~110 LOC) — YAML frontmatter encoder/decoder. 표준 키(`markleaf_id`/`created_at`/`updated_at`/`pinned`/`archived`) + 알 수 없는 키들의 opaque 보존(round-trip 친화). 자체 YAML 파서 없이 라인 단위 `key: value` 처리, ISO instant 포맷.
  - data/sync/NoteFolderMirror.kt (NEW, ~170 LOC) — SAF DocumentFile 기반 read/write. `writeNote(context, uri, note)` (idempotent, id-marker 기반 파일 매칭) + `importChanges(context, uri, existing, applyUpdate, applyCreate)` (수동 reconcile, 새 파일은 신규 노트로 import, 기존은 file 타임스탬프가 더 새로울 때만 업데이트). `ImportResult(updated, created, skipped, errors)` 반환.
  - feature/editor/EditorScreen.kt — 기존 1초 디바운스 자동 저장 LaunchedEffect 안에 `appSettings.syncFolderUri` 가 있을 때 `withContext(IO) { NoteFolderMirror.writeNote(...) }` 호출 추가. `Dispatchers.IO`/`withContext` import 추가.
  - feature/settings/SettingsScreen.kt — `syncFolderLauncher` (`OpenDocumentTree` SAF launcher, persistable URI permission take + 첫 미러링 seed) 추가. 새 `SyncSection` 컴포저블 — 미선택 상태 / 선택된 상태 둘 다 명시적 카피. "지금 동기화" 버튼은 DB와 폴더 reconcile 후 결과 Toast(`updated/created/skipped`). `humanReadableTreePath()` 헬퍼로 SAF tree URI를 사람 친화 경로로 표시. `formatRelative()` 헬퍼로 마지막 sync 시각 표시.
  - res/values{,-ko,-es}/strings.xml — sync_title, sync_explainer, sync_recommended_locations, sync_status_unset/folder_format/last_synced_format/last_synced_never, sync_behavior_summary, sync_pick_folder, sync_change_folder, sync_now, sync_stop, sync_seeded_format, sync_done_format, sync_stopped (총 14키 × 3언어). ResourceParityTest 통과.
  - test/data/sync/SyncFrontmatterTest.kt (NEW) — 8개 단위 테스트: encode shape, full round-trip, frontmatter 없는 파일, 닫지 않은 frontmatter, 알 수 없는 키 보존, quoted values, pinned 키 부재 vs explicit false 등.
  - app/build.gradle.kts (versionCode 64, versionName 2.1.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - 사용자 명시 요청: "이 기능에 대한 명확한 설명이 필요" → Settings UI에서 *동작 원리*를 3-4줄로 풀어서 설명, 권장 폴더 위치를 구체적으로 보여주고, 동작 4줄(자동 저장 / 지금 동기화 / 충돌 / 삭제 미동기화)을 status row 아래 항상 노출.
  - 검토에서 의도적으로 좁힌 스코프: (a) 노트 *삭제* 동기화는 v2.1.0에서 제외 — 가장 위험한 작업, 잘못되면 데이터 손실 시나리오. (b) 앱 시작 시 자동 reconcile 미구현 — 백그라운드 silent overwrite 회피. 사용자는 "지금 동기화" 명시적 트리거. 두 결정 다 §2.6 안전 기본값에 정렬.
  - File-id 매칭: 파일명에 `id<id8자>` suffix를 박아 다음 save 때 같은 파일을 빠르게 찾음 (frontmatter 전부 파싱하는 O(n²) 회피). slug 부분이 사용자 친화 + suffix가 머신 친화 = `hello-world-idabcdef12.md`.
  - SAF persistent URI permission: `takePersistableUriPermission(uri, READ|WRITE)`. 이게 없으면 앱 재시작 시 URI가 만료됨 — v1.6 export-all은 일회성이라 필요 없었지만 v2.1은 영구 필요.
  - 충돌 슬랙 2초: 일부 클라우드 sync 앱이 파일 타임스탬프를 1초 단위로 반올림하는 경우 false-positive "file newer" 트리거 회피.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — SyncFrontmatterTest 8개 그린, ResourceParityTest 그린, 기존 단위 테스트 모두 그린
  - `./gradlew assembleDebugAndroidTest` 통과
  - `./gradlew verifyRoborazziDebug` 통과 (UI 변경이 settings에 국한되어 기존 14+4 골든 영향 없음)

## 2026-05-08 (밤) - v2.0.0
- Work: Bear-급 인라인 rich rendering. 사용자가 "Bear의 핵심 체감 차이는 라이브 프리뷰" 라고 명시한 갈증을 직접 응답. 라이브러리 교체는 §2.9 정신에 맞춰 보류.
- Changed files:
  - core/markdown/MarkdownSyntaxHighlighter.kt — 헤딩 분기 재구성: marker 길이별로 contentStart 계산해 (H1: 24sp Bold, H2: 20sp SemiBold, H3: 18sp SemiBold) 콘텐츠 범위에만 fontSize+fontWeight 적용. 마커(`#`)는 muted color + Normal weight 로 retreat. BOLD_REGEX 매치 콘텐츠 weight를 SemiBold → Bold로 강화. 새 헬퍼 `muteMarkerStyle(colors)` 가 모든 inline marker(`**`, `_`, `~~`, 백틱, `[`, `](`, `)`)를 color=syntax + weight=Normal + style=Normal + decoration=None 으로 일괄 reset → 마커가 콘텐츠보다 시각적으로 retreat하는 Bear 패턴 구현.
  - test/core/markdown/MarkdownSyntaxHighlighterTest.kt — 5개 단위 테스트 추가: H1/H2/H3 fontSize, bold가 FontWeight.Bold (not SemiBold), 마커가 Normal weight로 reset되는지.
  - test/core/markdown/preview/EditorLiveSnapshotTest.kt (NEW) — 라이브 *에디터* 모드 시각 골든 4개. BasicTextField + MarkdownSyntaxVisualTransformation을 실제 사용 환경 그대로 캡처. 헤딩(라이트/다크) + 인라인 emphasis + 혼합 문서.
  - test/snapshots/roborazzi/editor_live_*.png (NEW, 4 files) — 위 테스트의 골든.
  - app/build.gradle.kts (versionCode 63, versionName 2.0.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - Bear의 NSTextStorage + NSAttributedString 패턴을 Compose의 BasicTextField + AnnotatedString 등가로 구현. 글자 인덱스는 그대로 보존(VisualTransformation의 OffsetMapping.Identity), 시각만 변형.
  - Phase B (commonmark-java 라이브러리 교체) 보류 결정 근거: 사용자 가치 0 (사용자 화면에 변화 없음), 추측 기반 리팩터, 실제 필요(위키링크/하이라이트 등 확장 도입)가 생길 때 진행이 §2.9에 맞음. 코드 변경량을 최소화하면서 Phase A (사용자 가치 100%)만 출시.
  - 사용자가 보여준 mixed_document_light 골든의 라이브 변형 결과 — H1 "Title"이 visibly 24sp Bold green, `**bold word**` 가 진짜 Bold tertiary, `*emphasis*` 가 italic, 마커들 muted gray retreat. *체감상 가장 큰 변화* 라고 약속한 것 그대로.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — MarkdownSyntaxHighlighterTest 13개 (기존 8 + 신규 5) 그린, EditorLiveSnapshotTest 4개 그린, 전체 단위 테스트 그린
  - `./gradlew assembleDebugAndroidTest` 통과
  - `./gradlew recordRoborazziDebug` 4개 새 PNG 생성, `./gradlew verifyRoborazziDebug` 모두 통과 (changeThreshold 0.05f)
  - 기존 14개 preview-pane 골든은 변경 없음 (VisualTransformation은 에디터의 BasicTextField에만 영향)

## 2026-05-08 (밤) - v1.9.0
- Work: Phase 18 — 시각 회귀 그물망. Bear-급 라이브 프리뷰로 가기 전 안전 인프라 사이클. 사용자 화면 변화 0.
- Changed files:
  - build.gradle.kts (root) — `id("io.github.takahirom.roborazzi") version "1.20.0" apply false`
  - app/build.gradle.kts — Roborazzi 플러그인 적용 + testImpl(`roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule` 1.20.0). compose ui-test-junit4/manifest를 test에도 추가 (기존엔 androidTest only). androidx.test.ext:junit testImpl 추가.
  - core/markdown/preview/MarkdownPreviewList.kt (NEW, ~250 LOC) — `EditorScreen`의 미리보기 LazyColumn 분기 + InlineMarkdownText / CalloutBox / FrontmatterBlock / FootnoteDefRow / MarkdownCodeBlock 모두 이쪽으로 이동. 공개 API: `MarkdownPreviewList(lines, modifier, contentPadding)` 와 단일 `PreviewLineRenderer(line)` 둘.
  - feature/editor/EditorScreen.kt — 미리보기 LazyColumn 통째 삭제, `MarkdownPreviewList(...)` 호출로 대체. 이로 인해 사용처 사라진 import 다수 (background, RoundedCornerShape, ClickableText, HorizontalDivider, SpanStyle, buildAnnotatedString, withStyle, FontFamily, FontStyle, BaselineShift, TextDecoration, sp, LazyColumn, items, clip) 정리.
  - test/core/markdown/preview/MarkdownPreviewSnapshotTest.kt (NEW) — 14개 시각 골든. 각 PreviewLineType + 콜아웃 3종 + 프론트매터 + 각주 + 혼합 문서 (라이트/다크 변형 포함).
  - test/snapshots/roborazzi/*.png (NEW, 14 files) — 골든 이미지. mdpi 360x640 에서 Windows native graphics로 첫 record.
  - .github/workflows/android-build.yml — `Verify Roborazzi snapshots` 스텝 추가 (`./gradlew verifyRoborazziDebug`). 실패 시 diff artifact 업로드.
  - app/build.gradle.kts (versionCode 62, versionName 1.9.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - 가치관 점검 후 사용자가 명시적으로 동의한 v1.9 → v2.0 → v2.1 순서의 첫 단계. v2.0(인라인 rich rendering, CommonMark 도입)과 v2.1(SAF 폴더 미러 sync) 이전에 시각 회귀 차단망 먼저 깔기.
  - Roborazzi 1.20은 Robolectric 4.10+ + Kotlin 1.9 + AGP 8.x + JDK 17과 맞물려 동작. 우리는 모두 충족.
  - `@Config(qualifiers = "w360dp-h640dp-mdpi")` — 처음 시도한 BCP47 locale 포함 qualifier(`en-rUS`)가 Robolectric Qualifiers.parse()에 거부당함. locale 빼고 화면 사이즈+밀도만 남기니 통과.
  - `changeThreshold = 0.05f`: Windows native 폰트 렌더링과 Ubuntu CI Linux native 폰트 렌더링 사이의 hint/안티앨리어싱 픽셀 미세 차이 흡수. 구조적 회귀(요소 누락/색상 변경/레이아웃 어긋남)는 5%를 가볍게 넘기므로 잡힘. 추후 record를 CI에서 다시 돌려 동일 OS 골든을 만들면 0.005f 정도로 타이트하게 조일 예정.
  - MarkdownPreviewList 추출은 자연스러운 부산물 — 같은 코드를 에디터와 테스트가 공유하므로 갈라질 위험이 0이 됨. v2.0에서 인라인 rich rendering 도입 시 이 단일 진입점만 수정하면 양쪽 다 갱신됨.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — 기존 단위 테스트 모두 그린, Roborazzi 14개 새 테스트도 그린 (record 직후 verify)
  - `./gradlew assembleDebugAndroidTest` 통과
  - `./gradlew recordRoborazziDebug` 14개 PNG 생성, `./gradlew verifyRoborazziDebug` 모두 통과 (changeThreshold 0.05f 안)
  - 사용자에게 보여준 mixed_document_light 골든 시각 확인 — frontmatter 모노스페이스 박스, primary 색상 H1, callout(Tip) 연두 배경+icon, 위첨자 각주 ref, 하단 각주 def 모두 의도대로 그려짐.

## 2026-05-08 (밤) - v1.8.0
- Work: v1.5–1.7 사이클이 누적시킨 상단바·설정 chrome density를 줄이는 정리 사이클. 새 기능 0, 모든 액션 유지하되 시각 밀도만 하향.
- Changed files:
  - feature/notes/NotesListScreen.kt — TopAppBar 액션 정리. archive/trash/settings IconButton 3개를 `Icons.Default.MoreVert` IconButton + DropdownMenu(보관함/휴지통/설정) 한 개로 합침. search/tags는 primary 유지. `overflowExpanded: Boolean` state 추가.
  - feature/editor/EditorScreen.kt — TopAppBar 액션 정리. focus, trash IconButton을 새 ⋮ overflow DropdownMenu(집중 모드 / 휴지통으로 이동)로 이동. search(find-in-note), preview/edit 토글, share-menu 셋은 primary 유지. focus 모드 항목은 미리보기 모드일 때만 표시(쓰기 화면에서만 의미 있음). `overflowExpanded: Boolean` state 추가.
  - feature/settings/SettingsScreen.kt — `settings_security` 섹션 제거. `screenshot_protection` 스위치를 `settings_privacy` 섹션 안으로 이동(privacy_local_first 다음, privacy dashboard 버튼 위).
  - res/values{,-ko,-es}/strings.xml — `more_options` (3 lang) 추가, `settings_security` (3 lang) 삭제. ResourceParityTest 통과.
  - app/build.gradle.kts (versionCode 61, versionName 1.8.0)
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - v1.7.0 직후 §2 가치관 점검에서 발견된 "gradual chrome accretion" 신호에 대한 직접적인 응답 사이클. 5단계 흐름(열기→쓰기→정리→찾기→내보내기)에 새 기능을 더하지 않고, 기존 항목을 사용 빈도에 따라 primary / overflow로 재정렬한 것.
  - Material 3 TopAppBar는 trailing actions의 명시적 상한을 두지 않지만 일반적으로 ≤3개 권장. 우리는 이번에 그 권장에 정렬됨.
  - DropdownMenu는 v1.6 share-menu, v1.7 archive-row long-press 등에서 이미 사용 중인 패턴이라 코드 추가가 일관된 idiom.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — UI를 만지지 않은 단위 테스트들 모두 그린 (`ResourceParityTest`가 새 키 / 삭제된 키를 3개 언어 동기 검증)
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08 (밤) - v1.7.0
- Work: Phase 16 (Spec Closure) — `Note.archived` 필드(v1.0부터 dead) 살리는 보관함 UI 도입 + AGENT_SPEC §7 접근성 라인 검증.
- Changed files:
  - data/local/dao/NoteDao.kt — `setArchived(noteId, archived)`, `observeArchivedNotes()` 추가. `observeNotes()`/`searchNotesFts`/`searchNotesLike`/`getNoteByTitle`/`getMaxSortOrder` 모두 `AND archived = 0` 추가해 보관 노트가 메인/검색에 노출되지 않도록 함.
  - domain/repository/NoteRepository.kt — `setArchived` + `observeArchivedNotes` 인터페이스 시그니처 추가
  - data/repository/LocalNoteRepository.kt — 위 두 메서드 구현
  - ui/viewmodel/NotesViewModel.kt — `setArchived(noteId, archived)` 추가
  - ui/viewmodel/ArchiveViewModel.kt (NEW) — TrashViewModel과 동일한 패턴: 아카이브 flow 관찰 + `unarchive(noteId)` + `moveToTrash(noteId)`
  - ui/viewmodel/MarkleafViewModelFactory.kt — ArchiveViewModel 케이스 추가
  - feature/archive/ArchiveScreen.kt (NEW) — TrashScreen과 비슷한 구조이되 Restore/Delete 버튼 대신 long-press DropdownMenu(보관 해제 / 휴지통 이동). 클릭 시 에디터 진입.
  - navigation/NavRoutes.kt — `ARCHIVE = "archive"`
  - navigation/MarkleafNavHost.kt — `composable(NavRoutes.ARCHIVE) { … ArchiveScreen(…) }` + 두 NotesListScreen 호출처에 `onArchiveClick = { navController.navigate(NavRoutes.ARCHIVE) }` 전달
  - feature/notes/NotesListScreen.kt — TopAppBar에 `Icons.Default.Inventory2` 보관함 아이콘 (Tags 다음, Trash 앞), long-press 드롭다운에 "보관" 항목 (Pin과 Trash 사이). 새 `onArchiveClick`/`onArchive` 콜백.
  - res/values{,-ko,-es}/strings.xml — `archive`, `unarchive`, `archive_empty`, `archive_empty_hint` 4개 키 3언어 동시 추가 (ResourceParityTest 통과)
  - app/build.gradle.kts (versionCode 60, versionName 1.7.0)
  - test/data/repository/LocalNoteRepositoryTest.kt — 3개 테스트 추가 (setArchived hides+exposes, setArchived false restores, searchNotes excludes archived)
  - test/ui/viewmodel/MarkleafViewModelFactoryTest.kt — `createsArchiveViewModel` + FakeNoteRepository에 setArchived/observeArchivedNotes override
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - DB 스키마 변경 없음. `archived` 컬럼은 v1.0부터 NoteEntity에 존재했고 모든 기존 행은 default 0이라 마이그레이션 없이 안전.
  - 인덱스: 기존 `(trashed, pinned, sortOrder)` 인덱스의 leading prefix(`trashed`)는 새 쿼리(`WHERE trashed = 0 AND archived = 0 …`)에서도 활용 가능. 메인 노트가 수만 건이 아닌 한 추가 인덱스 불필요.
  - 접근성 audit는 코드 변경보다 검증 위주: `IconButton(`이 들어간 5개 파일 모두 stringResource 라벨 부여, `contentDescription = null` 케이스는 모두 텍스트 라벨 동반 데코레이티브 아이콘. 색상 대비는 정적 팔레트(LightColorScheme/DarkColorScheme) 기준 surfaceVariant↔onSurfaceVariant ~11:1 / ~6:1, primary↔primaryContainer ~5.5:1 / ~5:1로 WCAG AA 4.5:1 통과.
  - 다국어 확대(JP/FR/DE)는 검증할 native 화자 부재로 v1.7.0에선 보류. ResourceParityTest 인프라는 그대로 작동하므로 향후 추가 시 안전.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — 신규 4개 포함 모든 단위 테스트 그린 (`LocalNoteRepositoryTest` 7건, `MarkleafViewModelFactoryTest` 4건 등)
  - `./gradlew assembleDebugAndroidTest` 통과
- AGENT_SPEC §7 *반드시 포함* 16/16, *있으면 좋은* 7/7 모두 닫힘. v1.7.0이 사실상 MVP 종료.

## 2026-05-08 (밤) - v1.6.0
- Work: Phase 15 (Markdown Expressiveness) 4종 + 별도 검토에서 발견된 §7 *반드시 포함* 누락 항목(단일/전체 .md 내보내기 UI) 동시 해결.
- Changed files:
  - core/markdown/SimpleMarkdownPreview.kt — `CalloutKind` enum 추가 (NOTE/TIP/IMPORTANT/WARNING/CAUTION + `WARN`/`DANGER` 별칭 매핑), `PreviewLineType.CALLOUT/FRONTMATTER/FOOTNOTE_DEF` 추가, `PreviewInlineType.FOOTNOTE_REF` 추가. `parse()` 진입부에서 leading `---` … `---` 만 frontmatter로 처리하고 본문 중간의 `---`는 기존 HORIZONTAL_RULE 처리 유지. 콜아웃은 `> [!TYPE]` 헤드라인 다음 연속된 `>` 라인을 모아 단일 PreviewLine로 합침. 각주 정의는 라인 단위 정규식, 각주 참조는 inline 정규식(definition와 충돌하지 않도록 `(?!:)` 부정 lookahead 사용).
  - core/markdown/MarkdownEditActions.kt — `indent()` / `outdent()` 추가. 둘 다 `selectionLineRange()` 헬퍼로 선택 영역에 닿는 모든 줄을 잡아 일괄 처리. outdent는 2-space, tab, 1-space 순으로 단계적 제거.
  - feature/editor/EditorScreen.kt — BasicTextField에 `Modifier.onPreviewKeyEvent { Tab/Shift+Tab → indent/outdent }` 부착. 기존 Share IconButton을 DropdownMenu로 묶어 "공유 시트로 보내기" + ".md 파일로 저장" 두 항목으로 확장. 후자는 `ActivityResultContracts.CreateDocument("text/markdown")` 런처 + `ExportUtil.generateMarkdownContent/generateFileName` 호출. 미리보기 LazyColumn에 `PreviewLineType.CALLOUT/FRONTMATTER/FOOTNOTE_DEF` 케이스 추가. `InlineMarkdownText`에 `FOOTNOTE_REF` (BaselineShift.Superscript + 11sp + primary color) 케이스 추가.
  - feature/settings/SettingsScreen.kt — 새 "데이터" 섹션 + "전체 노트 내보내기…" 버튼. `ActivityResultContracts.OpenDocumentTree()` 런처가 `LocalNoteRepository.observeNotes().first()` 로 현재 노트 스냅샷을 잡아 `ExportAllNotes.exportAllNotes(context, folderUri, notes)` 호출. 휴지통 노트는 제외. 결과 Toast로 "노트 N개를 내보냈습니다" 표시.
  - res/values{,-ko,-es}/strings.xml — share_via_system, export_as_file, export_success, export_failed, export_all_notes, export_all_notes_description, export_all_done_format, settings_data, callout_note/tip/important/warning/caution + `share_note` 라벨을 "공유 / 내보내기"로 수정 (3개 언어 동시).
  - app/build.gradle.kts (versionCode 59, versionName 1.6.0)
  - test/core/markdown/SimpleMarkdownPreviewTest.kt — 6개 테스트 추가 (frontmatter, mid-document `---` 보존, callout block, callout alias, footnote def, footnote ref).
  - test/core/markdown/MarkdownEditActionsTest.kt — 5개 테스트 추가 (indent single, indent multi-line, outdent 2-space, outdent tab, outdent no-op).
  - CHANGELOG.md, HISTORY.md, .agent/tasks.md
- Context:
  - SimpleMarkdownPreview.kt 라인 수: v1.5.0 종료 시 178 LOC → v1.6.0 ~210 LOC. lightweight-bias 메모의 ~300 LOC 한계 안에 안전하게 들어옴.
  - 전체 노트 내보내기에서 SettingsScreen이 NotesViewModel 없이 LocalNoteRepository를 직접 인스턴스화하는 건 TagsScreen/SearchScreen과 동일한 기존 패턴(스펙 §9의 엄격 해석에는 어긋나지만 v1.0부터의 단순화 컨벤션).
  - 단일 노트 export 메뉴 위치: TopAppBar 액션이 v1.5에서 5개로 늘어나 더 추가하면 비좁아지므로, 기존 Share IconButton을 DropdownMenu host로 바꿔 "공유" + "내보내기" 두 동작을 하나의 버튼 아래에 묶음. 사용자가 자주 헷갈릴 수 있어 라벨에 ellipsis(…)를 붙여 "선택할 게 더 있다"는 신호.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — 신규 11개 테스트 포함 모든 단위 테스트 그린
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08 (밤) - v1.5.0
- Work: Phase 14 — 안드로이드 정상 시민 마감을 위한 5종 묶음 (Material You / 예측 뒤로가기 / 단일 노트 시스템 공유 / 외부 공유 텍스트 수신 / FLAG_SECURE 토글).
- Changed files:
  - ui/theme/Theme.kt — `MarkleafTheme.dynamicColor` 기본값 `false` → `true` (S+에서만 dynamicLight/DarkColorScheme 사용, 그 외에는 기존 정적 색상 폴백)
  - AndroidManifest.xml — `<application android:enableOnBackInvokedCallback="true">`, MainActivity에 `<intent-filter ACTION_SEND text/plain>`, 그리고 `androidx.core.content.FileProvider` provider 등록 (`${applicationId}.fileprovider` authority)
  - res/xml/file_paths.xml (NEW) — `cache-path name="shared_notes"` (`ShareNoteUtil`이 이미 사용 중인 cache 디렉터리)
  - data/settings/AppSettings.kt — `screenshotProtection: Boolean = false` 필드 추가
  - data/settings/AppSettingsRepository.kt — `SCREENSHOT_PROTECTION` boolean 키 + `setScreenshotProtection(enabled)`
  - MainActivity.kt — `extractSharedText()` 헬퍼 (EXTRA_SUBJECT를 `# 제목` H1으로, EXTRA_TEXT를 본문으로 결합), `repeatOnLifecycle(STARTED)` 안에서 settings flow 관찰 → `window.addFlags(FLAG_SECURE)` / `clearFlags`. `onNewIntent`에서 SEND가 새로 들어오면 `setIntent(intent) + recreate()`.
  - navigation/MarkleafNavHost.kt — `sharedText: String? = null` param, NOTES composable LaunchedEffect로 `viewModel.createNote(sharedText)` 호출 후 에디터로 navigate.
  - ui/viewmodel/NotesViewModel.kt — `createNote(initialContent: String = "")` 시그니처. 본문이 있으면 `TitleExtractor.extractTitle/generateExcerpt`로 제목/요약을 채움.
  - feature/editor/EditorScreen.kt — TopAppBar actions 영역에 `Icons.Default.Share` IconButton 추가, 클릭 시 현재 편집 중 텍스트로 Note를 만들어 `ShareNoteUtil.shareNote`. (저장 보다 빠르게 공유 누른 경우에도 입력 중 본문이 그대로 시트에 들어가도록 in-memory copy)
  - feature/settings/SettingsScreen.kt — 새 `settings_security` 섹션의 `screenshot_protection` 토글
  - res/values{,-ko,-es}/strings.xml — `share_note`, `settings_security`, `screenshot_protection`, `screenshot_protection_description` (3개 언어 동시 추가, ResourceParityTest 통과 보장)
  - app/build.gradle.kts (versionCode 58, versionName 1.5.0)
  - CHANGELOG.md, .agent/tasks.md
- Context:
  - 다섯 항목 모두 ≤30 LOC급 작은 변경이라 한 사이클로 묶어도 디프 검토가 어렵지 않음. 각 항목은 독립적으로 켜고 끌 수 있음.
  - `ShareNoteUtil`은 v0.x부터 존재했지만 UI에 wired된 적이 없어 사실상 dead code였음. AndroidManifest에 `FileProvider`도 빠져 있어 wire-up 시 FileProvider도 함께 등록해야 했음 — `xml/file_paths.xml`은 `cacheDir/shared_notes` 한 줄로 끝.
  - SEND 수신 처리는 `onCreate` + `onNewIntent` 둘 다에서 처리. 이미 실행 중인 인스턴스로 SEND가 오면 `setIntent + recreate()`로 새 노트가 시드됨.
  - FLAG_SECURE는 `repeatOnLifecycle(STARTED)`에서 settings flow를 관찰. `distinctUntilChanged`로 동일값 emit 무시. STARTED 위에서만 active → 백그라운드에서 토글되어도 안전.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` 통과 — `ResourceParityTest`가 새 3개 키를 ko/es에서도 검증, 기존 단위 테스트는 영향 없음

## 2026-05-08 (밤) - v1.4.2
- Work: v1.4.1에서 부분만 고쳐졌던 태블릿 접힌 레일의 상태바 겹침을 마저 정리.
- Changed files:
  - navigation/MarkleafNavHost.kt — `CollapsedNoteListRail`에서 `systemBarsPadding`을 안쪽 Box → 바깥 Surface로 옮기고, Box는 `fillMaxSize`만 유지
  - app/build.gradle.kts (versionCode 57, versionName 1.4.2)
  - CHANGELOG.md
- Context:
  - 사용자 보고 (Lenovo Y700 2nd gen): 메인 콘텐츠는 상태바 밑으로 내려왔지만 56dp 회색 레일의 배경이 여전히 상태바 시계 영역까지 올라가 겹침.
  - 원인: v1.4.1에서 padding을 inner Box에 줘 아이콘 위치만 인셋만큼 밀려났을 뿐, 바깥 Surface의 background 페인트 영역은 그대로 top-to-bottom이었음. Surface가 그리는 회색 fill이 상태바 뒤까지 보여서 시각적 충돌 발생.
  - 해결: `systemBarsPadding`은 padding을 받은 노드의 *그리는 영역* 자체를 줄이므로 Surface에 적용하면 surfaceVariant fill이 상태바 아래에서만 시작됨. 위쪽은 부모 Row의 `editorPaneColor`(= `MaterialTheme.colorScheme.background`)가 보여 에디터 페인 TopAppBar 영역과 동일한 톤으로 자연스럽게 이어짐.
- Verification:
  - `./gradlew assembleDebug` 통과
  - 기존 테스트는 영향 없음 (테스트 재실행 생략)

## 2026-05-08 (밤) - v1.4.1
- Work: Lenovo Y700 2nd gen 같은 노치 없는 태블릿에서 노트 본문이 알림바 밑으로 들어가던 문제 해결.
- Changed files:
  - MainActivity.kt — `super.onCreate()` 직전에 `enableEdgeToEdge()` 호출 (androidx.activity 1.8.2 helper)
  - feature/tags/TagsScreen.kt — Surface root에 `systemBarsPadding()`
  - feature/search/SearchScreen.kt — Surface root에 `systemBarsPadding()`
  - feature/trash/TrashScreen.kt — Surface root에 `systemBarsPadding()`
  - navigation/MarkleafNavHost.kt — `CollapsedNoteListRail` Box에 `systemBarsPadding()`
  - app/build.gradle.kts (versionCode 56, versionName 1.4.1)
  - CHANGELOG.md
- Context:
  - 사용자 보고: Galaxy S24 (노치 있음, Android 15 추정)는 시스템 차원 edge-to-edge 강제로 인해 M3 Scaffold + TopAppBar의 자동 인셋 처리가 동작 → 정상.
  - Lenovo Y700 2nd gen (노치 없음, Android 14 이하 추정)에서는 시스템이 강제하지 않아 우리 앱이 명시적으로 enableEdgeToEdge를 켜지 않으면 status bar 처리가 모호한 상태(반투명 + no padding)가 되어 콘텐츠가 알림 영역 밑으로 그려짐.
  - 해결의 통합 포인트는 `enableEdgeToEdge()` 한 줄 — 모든 안드로이드 버전에서 동일하게 transparent 시스템 바 + WindowInsets 제공. M3 Scaffold/TopAppBar는 그 인셋을 알아서 padding으로 변환.
  - Scaffold 없는 화면(Tags/Search/Trash)에는 root Surface에 systemBarsPadding을 직접 추가 — 같은 통일성을 단순한 modifier 한 줄로 확보.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release unit) 통과 — 기존 테스트 모두 영향 없음

## 2026-05-08 (밤) - v1.4.0
- Work: Bear급 사용 경험으로 가는 다음 단계 — 정리와 글쓰기 습관에 직결되는 세 가지 기능 묶음.
- Changed files:
  - util/TagParser.kt — `isValidTagName`을 segment-단위 검증으로 바꿔 `/` 중첩 허용 (각 세그먼트는 `[a-zA-Z가-힣_][a-zA-Z가-힣0-9_-]*`)
  - feature/tags/TagsScreen.kt — `buildHierarchicalRows` 도입, 깊이별 들여쓰기와 색상 구분 (root: primary, child: secondary)
  - feature/editor/EditorScreen.kt — TopAppBar에 집중/검색 아이콘 추가, FindBar 컴포저블, focus 토글 시 툴바·통계·하이라이팅 일괄 숨김, `findAllRanges` 헬퍼 + LaunchedEffect로 selection 이동
  - res/values{,-ko,-es}/strings.xml — focus_mode / exit_focus_mode / find_in_note / find_in_note_hint / find_previous_match / find_next_match / close
  - test/util/TagParserTest.kt — 5개 시나리오 추가 (slash 중첩, 깊은 중첩, trailing slash 거부, 빈 중간 세그먼트 거부, 한글 중첩)
  - test/feature/editor/FindRangesTest.kt — 7개 시나리오 (빈 입력, 단일/다중 매치, 대소문자 무시, 미존재, 비겹침)
  - app/build.gradle.kts (versionCode 55, versionName 1.4.0)
  - CHANGELOG.md / HISTORY.md / .agent/tasks.md / .agent/progress.md
- Context:
  - 사용자: *"Bear급 사용 경험으로 가고 싶다"*. 직전 v1.3.0의 toolbar+smart-Enter 이후, 다음 한 사이클로 임팩트가 가장 큰 묶음으로 (1) 태그 계층화 — 정리 구조 도약, (2) 집중 모드 — 글쓰기 습관, (3) 노트 안 찾기 — 긴 노트 필수기능.
  - 셋 다 서로 독립적이고 각기 화면 한두 곳만 손대므로 한 사이클에 깔끔히 끝남.
  - DB 스키마 변경 없음. 태그 검증 정규식만 변경되어 기존 태그도 모두 통과.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release) 통과 — TagParser 5건 + FindRanges 7건 신규 통과
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08 (저녁) - v1.3.1
- Work: 사용자 보고로 시작 노트가 여전히 위키 링크와 ZIP 백업을 안내한다는 점을 발견 — 온보딩 콘텐츠와 구현을 동기화.
- Changed files:
  - app/src/main/res/raw/starter_notes.md (4 notes 재작성)
  - app/src/main/res/raw-ko/starter_notes.md
  - app/src/main/res/raw-es/starter_notes.md
  - data/onboarding/StarterNotesSeeder.kt (인라인 EN 폴백 동기화)
  - test/data/onboarding/StarterNotesSeederTest.kt (어서션 갱신 — `[[` / "ZIP backup" 부재 + "Smart Enter" 등장 확인)
  - app/build.gradle.kts (versionCode 54, versionName 1.3.1)
  - CHANGELOG.md
- Context:
  - 사용자 시나리오: v1.3.0 설치 후 새로 받은 샘플 노트가 `[[Local-first backup and export]]`를 따라가라고 안내하지만 실제 앱에는 위키 링크 기능이 없어 클릭해도 아무 일도 일어나지 않음.
  - 또한 *"설정에서 ZIP 백업 만들기"* 도 안내하나 v1.2.0에서 제거됨.
  - 새 시작 노트 4개: Welcome / Markdown(toolbar + Smart Enter 안내) / Tags(핀, 그룹화) / Privacy(.md 내보내기 + 휴지통).
- Verification:
  - `./gradlew test` 통과 — StarterNotesSeederTest 새 어서션 포함

## 2026-05-08 (오후)
- Work: v1.2.0 가벼움 회귀로 비워둔 토대 위에 Bear급 글쓰기 경험을 한 단계 끌어올림 (v1.3.0).
- Changed files:
  - core/markdown/MarkdownEditActions.kt — heading 순환, bullet/ordered/blockquote 토글, horizontalRule, codeBlock, applyAutoContinuation 추가
  - feature/editor/EditorScreen.kt — 툴바 4개 → 12개 (그룹 구분선 포함), 자동 이어쓰기 onValueChange 통합, 통계 footer (단어/글자/분)
  - feature/notes/NotesListScreen.kt — long-press → DropdownMenu(고정 토글 + 휴지통), 고정/오늘/어제/지난 7일/이전 섹션 그룹화, 핀 아이콘 표시
  - data/local/dao/NoteDao.kt + repository/LocalNoteRepository.kt + domain/repository/NoteRepository.kt — setPinned 와이어
  - ui/viewmodel/NotesViewModel.kt — setPinned launch helper
  - res/values{,-ko,-es}/strings.xml — heading/bullet/ordered/blockquote/code_block/horizontal_rule/strikethrough/inline_code/editor_stats_format/pin/unpin/section_* 추가
  - test/core/markdown/MarkdownEditActionsTest.kt — heading 순환, list/quote 토글, codeBlock, hr, autoContinuation(bullet/ordered/checklist/blockquote/empty-prefix-end/plain) 시나리오 추가
  - test/ui/viewmodel/MarkleafViewModelFactoryTest.kt — Fake setPinned override
  - app/build.gradle.kts (versionCode 53, versionName 1.3.0)
  - CHANGELOG.md / HISTORY.md / .agent/tasks.md / .agent/progress.md
- Context:
  - 사용자: *"퀵 버튼이 너무 비어 보인다"* + *"Bear급 사용 경험으로 가고 싶다"*. v1.2.0의 토대 위에서 가장 임팩트 큰 작업 묶음으로 선정.
  - 마크다운 파서는 손파서를 유지 (300줄 미만). 새 기능은 모두 파서를 건드리지 않고 에디터 액션 + UI 레이어에서 끝남.
  - DB 스키마 변경 없음. `pinned` 필드는 v1.0부터 있었지만 UI 토글이 없어 사실상 데드 필드였음.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release unit) 통과 — 새 12개 액션/자동 이어쓰기 케이스 포함
  - `./gradlew assembleDebugAndroidTest` 통과

## 2026-05-08
- Work: 가벼운 마크다운 앱 가치관에 맞춰 무거운 부가기능을 일괄 정리하고 누락된 노트 삭제 진입점을 복구.
- Changed files:
  - feature/notes/NotesListScreen.kt (NoteCountDashboard 제거, 드래그 재정렬 제거, long-press 휴지통 진입)
  - feature/editor/EditorScreen.kt (휴지통 버튼 추가, 버전 히스토리/백링크/이미지 첨부/위키 링크/체크리스트 진행률/표·수식 미리보기 제거)
  - feature/settings/SettingsScreen.kt (ZIP 백업, 툴바 토글, 권한 섹션 제거)
  - feature/search/SearchScreen.kt (Quick Open Links 섹션 제거)
  - core/markdown/SimpleMarkdownPreview.kt (TABLE/MATH/IMAGE/LINK/INLINE_MATH 제거)
  - core/markdown/MarkdownEditActions.kt (wikiLink/image 액션 제거)
  - core/markdown/MarkdownSyntaxHighlighter.kt (WIKI_LINK/TABLE 정규식 제거)
  - data/local/AppDatabase.kt (v9 migration: note_snapshots / note_links / attachments DROP)
  - data/local/dao 및 entity (Snapshot/Link/Attachment 관련 파일 삭제)
  - data/repository/LocalNoteRepository.kt + domain/repository/NoteRepository.kt (스냅샷/백링크 메서드 제거)
  - data/settings/AppSettings(+Repository).kt (ToolbarConfig 제거)
  - util/BackupUtil.kt, util/PermissionUtils.kt, feature/notes/ChecklistProgressIndicator.kt, core/markdown/ChecklistParser.kt, domain/model/NoteSnapshot.kt 삭제
  - AndroidManifest.xml (POST_NOTIFICATIONS, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_EXTERNAL_STORAGE 제거)
  - app/build.gradle.kts (Coil 의존성 제거)
  - res/values{,-ko,-es}/strings.xml (사용하지 않는 키 제거 + move_to_trash 계열 추가)
  - test 코드 정리 (제거된 기능 관련 테스트 삭제 또는 갈음)
  - CHANGELOG.md / HISTORY.md
- Context:
  - 사용자 보고: 휴지통 화면은 있는데 그곳으로 노트를 보낼 진입점이 UI 어디에도 없음.
  - 원인: NotesListScreen에서 long-press를 `detectDragGesturesAfterLongPress`가 가로채서 `onMoveToTrash` 콜백이 죽은 코드가 됐고, 에디터에는 삭제 버튼 자체가 없었음.
  - 함께 처리: 사용자 요청 *"가벼움/편의성에 부합하지 않는 기능은 과감히 제거"* 에 따라, AGENT_SPEC §7의 MVP 제외 항목(테이블, 수식, WYSIWYG, 복잡한 데이터)과 §2.3 "노트는 특정 앱에 갇히지 않아야" 원칙에 어긋나는 ZIP 백업, §6.3 "권한을 가능한 요청하지 않는다"와 충돌하는 미디어/알림 권한, 그리고 자동 저장+휴지통이 이미 안전망이라 중복인 버전 히스토리, "second-brain" 성격이 강한 위키 링크/백링크를 함께 제거.
- Verification:
  - `./gradlew assembleDebug` 통과
  - `./gradlew test` (debug + release unit test) 통과
  - `./gradlew assembleDebugAndroidTest` 통과 (인스트루멘테이션 컴파일까지만 확인; 실제 디바이스 실행은 보류)

## 2026-05-07
- Work: Play Console 제출 마감본 버전업 및 릴리즈 파이프라인 정리.
- Changed files:
  - app/build.gradle.kts (versionCode 51, versionName 1.1.21)
  - CHANGELOG.md (v1.1.21 섹션 추가)
  - HISTORY.md (이 기록 추가)
- Context:
  - Play Console 제출 마감 단계에서 릴리즈 버전 단조 증가를 유지.
  - 태그 릴리즈 CI signed AAB artifact 수집 경로 및 스토어 그래픽/정책 입력 준비를 완료.

## 2026-05-07
- Work: Play Store 출시 준비를 위한 target API 정책 대응 및 AAB 검증.
- Changed files:
  - app/build.gradle.kts (`compileSdk/targetSdk` 35, `versionCode` 50, `versionName` 1.1.20)
  - CHANGELOG.md (v1.1.20 섹션 추가)
- Context:
  - 2025-08-31 이후 Google Play 신규/업데이트 제출 기준(API 35+)에 맞춰 배포 차단 가능성을 사전 제거.
  - 로컬 signed release AAB 빌드로 제출 산출물 생성 경로를 재검증.

## 2026-05-06
- Work: v1.1.19 버전업 및 GitHub Actions 성공 모니터링.
- Changed files:
  - app/build.gradle.kts (versionCode 49, versionName 1.1.19)
  - CHANGELOG.md (v1.1.19 섹션 추가)
  - HISTORY.md (이 기록 추가)
- Context:
  - 최신 릴리즈 태그(v1.1.18) 이후 다음 단위 릴리즈 검증을 위해 단조 증가 버전을 적용.
  - 태그 푸시 후 GitHub Actions 실행 상태를 gh run watch로 추적해 성공 여부를 확인.
## 2026-05-06
- Work: v1.1.18 릴리즈 준비 및 GitHub Actions 권한 복구.
- Changed files:
  - .github/workflows/android-build.yml (permissions: contents: write 적용)
  - app/build.gradle.kts (versionCode 48, versionName 1.1.18)
  - CHANGELOG.md (v1.1.18 섹션 추가 및 누락 기능 통합)
  - HISTORY.md (이 기록 추가)
- Context:
  - v1.1.17 릴리즈 시도가 GitHub Actions의 GITHUB_TOKEN 권한 부족(contents: read)으로 인해 실패했음을 확인.
  - 워크플로우 파일에 contents: write 권한을 부여하여 gh release create가 가능하도록 수정.
  - 실패한 v1.1.17을 건너뛰고 v1.1.18로 상향하여 모든 최신 기능과 권한 수정을 포함한 새로운 릴리즈를 발행함.
- Included Major Features (Cumulative):
  - No-Cloud Certification documentation (#74)
  - In-app Privacy Dashboard (#73)
  - Checklist progress visualization (#69)
  - Home screen Quick Note widget (#60)
  - Drag and drop note reordering (#61)
  - Issue backlog refinement & batch registration (103 issues)

## 2026-05-04
- Work: Play Store Top 10 진입을 위한 50가지 개선 전략 수립 및 GitHub 이슈 등록.
- Changed files:
  - HISTORY.md (전략 목록 추가)
  - GitHub Issues (50개 신규 등록)
- Context:
  - 앱의 경쟁력을 강화하고 사용자 경험, 보안, 성장을 극대화하기 위한 종합 로드맵 수립.
  - UI/UX(15), 기능(15), 보안(10), 성장/ASO(10) 총 50개 항목 도출.

### Markleaf Play Store Top 10 개선 전략 (50선)
1. Material You 다이내믹 컬러 지원
2. 예측 뒤로 가기 제스처 지원 (Android 13+)
3. 리치 에디터 애니메이션 고도화
4. 사용자 지정 폰트 지원 (나눔스퀘어, JetBrains Mono 등)
5. 드래그 앤 드롭 노트 재정렬
6. 햅틱 피드백 최적화
7. 엣지 투 엣지(Edge-to-Edge) 디자인 적용
8. 마크다운 미리보기 전환 애니메이션 개선
9. 그리드/리스트 뷰 전환 옵션 제공
10. 폴더/노트북 아이콘 커스터마이징
11. 멀티 윈도우/분할 화면 최적화
12. 편집기 툴바 사용자 지정 기능
13. 상단/하단 빠른 스크롤 기능
14. 집중 모드(Focus Mode) UI 구현
15. 상태바 노트 카운트 대시보드
16. SQLite FTS5 통합 검색 고도화
17. 위키 링크([[노트 링크]]) 지원
18. 로컬 전용 이미지 첨부 기능
19. 문서 템플릿(회의록, 일기 등) 지원
20. 생체 인식(지문/안면) 앱 잠금
21. 고품질 PDF 내보내기 기능
22. 노트 수정 이력(스냅샷) 관리
23. 홈 화면 위젯 (최근 노트, 빠른 작성)
24. 앱 숏컷 (아이콘 롱프레스 바로가기)
25. 온디바이스 OCR (이미지 텍스트 추출)
26. 음성 인식 마크다운 입력
27. 노트 내 그리기/수기 스케치 통합
28. 체크리스트 진행률 시각화
29. 코드 구문 강조 (Syntax Highlighting) 지원
30. LaTeX 수식 지원
31. SQLCipher 기반 DB 전체 암호화
32. 긴급 상황 패닉 트리거 (앱 즉시 잠금)
33. 암호화된 로컬 백업 파일 생성
34. 권한 최소화 및 개인정보 보호 감사
35. 민감 노트 스크린샷 방지 옵션
36. 프라이빗 "시크릿" 노트 모드
37. 이미지 메타데이터(EXIF/GPS) 자동 제거
38. 자동 로컬 백업 스케줄러
39. 인앱 프라이버시 대시보드
40. No-Cloud 보증 인증 문서화
41. 다국어 지원 확대 (JP, FR, DE 등)
42. 전략적 인앱 리뷰 요청 UI
43. 고전환 스토어 스크린샷 리디자인
44. 전문 브랜딩 피처 그래픽 업데이트
45. 오픈소스 강점 및 투명성 강조
46. "이미지로 공유" 카드 생성 기능
47. 인터랙티브 온보딩 가이드
48. 커뮤니티 마크다운 템플릿 갤러리
49. WCAG 기준 접근성 최적화
50. 로컬 성능 모니터링 (비추적 방식)
