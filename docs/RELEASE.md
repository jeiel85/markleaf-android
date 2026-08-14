# Release Signing and Mirrored Releases

> **GitHub Actions is the primary CI and release path and never depends on
> GitLab.** GitLab CI is kept but gated by a manual switch, because GitLab
> shared-runner minutes are a monthly quota: set the project CI/CD variable
> `SKIP_GITLAB_CI=true` when the minutes run out and the whole GitLab pipeline
> skips cleanly (no blocked/stuck jobs, no minutes spent); unset it once the
> minutes reset and the GitLab pipeline + GitLab Release resume automatically.
> The GitLab-CI / GitLab-Release sections below apply whenever the pipeline is
> not skipped.
>
> **The git mirror is off as of 2026-08-14 (D067).** `mirror-push` and
> `mirror-check` are both gated behind the repository variable
> `GITLAB_REF_MIRROR`, which is unset, so GitLab `main` stops at the v2.32.4
> commit and no longer follows GitHub. The histories diverged when that release
> commit reached GitLab directly and GitHub by squash merge, and re-aligning
> needs a force push that GitLab's branch protection refuses. No release step
> reads GitLab, so nothing in this document's release path depends on it; the
> cost is that git history has no off-GitHub copy but local clones. Turning it
> back on is a sequence, not a variable — see `docs/mirror-runbook.md` "현재 상태".
> Release tags are still pushed to GitLab by hand, unchanged.
>
> While `SKIP_GITLAB_CI` was on (v2.28.0 / v2.28.1), no GitLab Release was
> created, so the READMEs currently link only to the GitHub Release. Re-add the
> "GitLab release mirror" links at the next release cut once GitLab is producing
> Releases again.

Markleaf release builds are signed only when release signing values are supplied.
The release keystore is a secret and must not be committed.

The production release certificate is fixed. Both GitHub and GitLab tag releases verify the signed APK against this SHA-256 certificate digest before creating a release:

```text
0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a
```

Do not replace the production keystore for normal releases. Android treats APKs signed with a different certificate as a different update lineage, so existing users cannot update over the previously installed app.

## Local Signed Release Build

Create a local `release-signing.properties` file:

```properties
MARKLEAF_RELEASE_STORE_FILE=.secrets/markleaf-release.p12
MARKLEAF_RELEASE_STORE_PASSWORD=your-keystore-password
MARKLEAF_RELEASE_KEY_ALIAS=markleaf-release
MARKLEAF_RELEASE_KEY_PASSWORD=your-keystore-password
```

For PKCS12 keystores, keep `MARKLEAF_RELEASE_KEY_PASSWORD` the same as `MARKLEAF_RELEASE_STORE_PASSWORD`.

Then build:

```bash
./gradlew assembleRelease
```

For release-candidate verification, require signing explicitly:

```bash
./gradlew assembleRelease -Pmarkleaf.requireReleaseSigning=true
```

On PowerShell, quote the Gradle property:

```powershell
./gradlew.bat assembleRelease '-Pmarkleaf.requireReleaseSigning=true'
```

The signed APK is written to:

```text
app/build/outputs/apk/release/app-release.apk
```

### Building from a worktree

Both signing inputs are gitignored at the repository root — `/.secrets/` and
`/release-signing.properties` in `.gitignore` — so they exist only in the
checkout where they were created. A `git worktree` is a separate checkout, and
concurrent sessions now make that the normal place to work, so a release built
there fails at signing until both are copied across:

```powershell
Copy-Item <main-checkout>\release-signing.properties .
New-Item -ItemType Directory -Force .secrets | Out-Null
Copy-Item <main-checkout>\.secrets\markleaf-release.p12 .secrets\
```

The failure without them is not obviously about a missing file — the build
reports unsigned output, or `-Pmarkleaf.requireReleaseSigning=true` fails
without naming the worktree as the reason. Copy, never move: the main checkout
has to keep its own copy. Nothing prunes these from a worktree, so delete the
worktree rather than leaving a keystore behind in it (#252).

## The Gradle Wrapper

`gradle/wrapper/gradle-wrapper.jar` is a binary in the build path, and F-Droid
builds with it. It should always be the jar Gradle publishes for the version in
`gradle-wrapper.properties`, and that is checkable:

```powershell
(Get-FileHash gradle/wrapper/gradle-wrapper.jar -Algorithm SHA256).Hash.ToLower()
```

against `https://downloads.gradle.org/distributions/gradle-<version>-wrapper.jar.sha256`.
For 8.9 that is
`498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17`.

It was not always so. Until #241 the committed jar was 48462 bytes against
Gradle's 43504, and `gradlew` was a malformed pre-8.x script — the `save ()`
helper that makes that script shape correct was missing entirely, so
`eval set -- "… GradleWrapperMain \"$@\""` collapsed every command-line argument
into one. `./gradlew :app:foo -Pbar=baz` reached Gradle as the single task name
`:app:foo -Pbar=baz`. Every CI call happens to pass exactly one argument, so
nothing tripped over it for the project's whole life.

**Never hand-edit these four files.** Regenerate them together so the scripts,
the jar and the properties stay a matched set from one distribution — and pass
the distribution checksum in the same command:

```powershell
.\gradlew.bat wrapper --gradle-version <version> --gradle-distribution-sha256-sum <sum>
```

**`distributionUrl` and `distributionSha256Sum` move together or not at all.**
Bumping the version without the matching sum leaves the old checksum in place
and every wrapper-driven build fails — CI, local, release. `<sum>` is Gradle's
published checksum for the distribution, from
`https://downloads.gradle.org/distributions/gradle-<version>-bin.zip.sha256`.

Use `gradlew.bat`, not the POSIX script, and expect `cmd` to complain at the end
of the run — it re-reads the batch file it is executing while the task rewrites
it. The task itself completes; check `git status` rather than the exit code.

Afterwards, verify:

- the jar's SHA-256 matches Gradle's published checksum,
- `gradlew` is committed with LF and `gradlew.bat` with CRLF,
- a genuinely multi-argument call works from a POSIX shell —
  `./gradlew :app:tasks -PsomeProperty=value`,
- the distribution checksum is enforced, on a **clean** `GRADLE_USER_HOME`. A
  machine that already has that distribution unpacked never re-verifies it, so
  a wrong sum looks fine locally and breaks everywhere else:

```powershell
$env:GRADLE_USER_HOME = "$env:TEMP\gradle-home-check"; .\gradlew.bat --version
```

For 8.9 the distribution sum is
`d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab` (#246).

F-Droid does not use any of this. `fdroidserver` deletes `gradlew`,
`gradlew.bat` and `gradle-wrapper.jar` from the checkout and builds with
`gradlew-fdroid`, which reads `gradle-wrapper.properties` only to pull the
version out of `distributionUrl` and then verifies its own download against the
F-Droid gradle-transparency-log. Pinning covers every other build path.

## R8 (Minification + Resource Shrinking)

Release builds run R8 with resource shrinking enabled. ProGuard rules live in
`app/proguard-rules.pro` and should stay minimal — every keep rule there
documents an undocumented coupling, so adding a rule should always come with a
comment explaining *why*.

R8 produces a mapping file used to deobfuscate stack traces from production
crash reports:

```text
app/build/outputs/mapping/release/mapping.txt
```

Keep the mapping that corresponds to each released APK. The permanent copies
live in `D:\Build` through the local `exportReleaseToBuildDrive` hand-off and
in the GitLab package registry. GitHub keeps it only as the 30-day
`markleaf-release-mapping` workflow artifact — it is no longer attached to the
GitHub Release, so pull it from one of the permanent copies when an older
version needs deobfuscating.

If R8 strips something at runtime (NoClassDefFoundError, missing reflection
target, lost Compose Composer slot), the fix is to add a precise `-keep`
rule in `app/proguard-rules.pro` — not to disable R8.

## CI Gates

Every push to `main` and every pull request runs:

- `pwsh -File scripts/verify-landing-versions.ps1` — public version strings match `versionName`
- `pwsh -File scripts/verify-release-notes.ps1` — fastlane changelogs exist and fit, CHANGELOG editions agree
- `./gradlew assembleDebug`
- `./gradlew test`
- `./gradlew verifyRoborazziDebug`
- `./gradlew :app:lintRelease` — fails on any Error-severity lint issue in the release variant
- `./gradlew :app:assembleRelease` — proves R8 still produces a valid APK

The two script checks run ahead of Gradle. They finish in seconds and cover what
a release-preparation commit most often forgets, so failing on them first costs
nothing. See [Release Version and Notes Checks](#release-version-and-notes-checks).

Two emulator jobs run alongside the `build` job, and since 2026-08-05 they are
treated differently:

- `instrumented-tests` — the `app/src/androidTest/` suite on an AGP managed
  device. **A required check.** It runs #235's Room migration assertions, so a
  broken migration is user data loss, and leaving it advisory meant nothing
  stopped one merging. Promoted after six consecutive green runs before
  2026-08-03 plus 8/8 on 2026-08-05. See
  [Instrumented Tests](#instrumented-tests).
- `launch-smoke` — installs the debug APK and starts the app. **Still
  `continue-on-error: true`, deliberately.** On 2026-08-05 it failed two of
  eight runs on code that was identical or documentation-only, each time with
  the runner's system_server dropping mid-call (`Can't find service: package`,
  `Failure calling service activity: Broken pipe (32)`, emulator boots of 355
  and 382 seconds). That is the runner failing, not the app, and a required
  check on it would let runner weather block merges. A release-APK runtime
  smoke (R8-shrunk, debug-signed) is a planned follow-up.

The two use different emulator paths — managed device versus the
`reactivecircus` action — which is why one can be flaky while the other is not.
**If `launch-smoke` is red and `instrumented-tests` is green, suspect the
runner first;** one re-run usually settles it.

GitLab CI independently runs `testDebugUnitTest`, `lintRelease`, and
`assembleDebug` for branches and merge requests. A semantic version tag
matching `vX.Y.Z` additionally builds the signed APK/AAB, exports the R8
mapping and six-locale notes, and verifies the APK certificate before
publishing. GitHub remains the canonical Roborazzi and emulator-smoke runner;
GitLab is an independent build and binary-distribution path.

## Instrumented Tests

**CI runs them on an AGP managed device, and they gate the merge.** The
`instrumented-tests` job calls `:app:ciAtdApi30DebugAndroidTest`, which starts,
targets and tears down its own API 30 `aosp-atd` emulator. Since 2026-08-05 it
is a required check — the "green for a few runs, not before" bar #235 set was
met (six consecutive before 2026-08-03, then 8/8 on 2026-08-05).

If it goes red on what looks like runner trouble rather than a real failure,
read the log and re-run it. Restoring `continue-on-error` would remove the gate
protecting the migration assertions, so that is a last resort, not a shortcut.

To run the same suite against a device or AVD you already have:

```powershell
pwsh scripts/run-instrumented-tests.ps1
```

The script picks the first connected device, checks it reports an API level,
and runs the whole suite — nothing is excluded any more. Add `-Serial` when
more than one device is attached, `-ExcludePackage` to skip one deliberately.

### What it covers

The places where a mistake costs a user their notes, and which no unit test can
reach:

- **Room migrations** — `AppDatabaseMigrationTest` walks v4 → current on a real
  Android runtime. A JVM test cannot; SQLite behaviour and Room's schema
  validation are the point.
- **The folder mirror's SAF IO** — `NoteFolderMirrorFolderTest`, twelve cases
  over a live `DocumentFile` tree: rewrite-in-place by id, rename on title
  change, adoption of an unclaimed file, never touching a file another note
  claims, preserving another tool's frontmatter, and the read-cap guard.
- **Navigation after a suspend point** — `NavigateAfterSuspendTest`, the #235
  regression.

- **The editor surface** — `EditorScreenTest`, ten cases over the real editor:
  the preview toggle, quick insert and its URL guard, the formatting panel, the
  information sheet, the outline screen leaving you in the editor when you jump
  to a heading (#215), and initial focus on a persisted empty note.
- **One end-to-end note flow** — `AppIntegrationTest`, create → type →
  auto-save → back → reopen, plus search navigation. It runs against the app's
  real database rather than an in-memory one, because `EditorScreen` builds its
  repository from `AppDatabase.getInstance` and an injected in-memory database
  would leave the editor and the list looking at different data. It deletes the
  notes it created afterwards and leaves any others alone.

`com.markleaf.notes.ui` used to be excluded: `ComprehensiveFeatureTest` asserted
against a UI that had moved on while a crash kept the suite from running, and 24
cases failed (#239). That class is gone. What it claimed to cover is covered
more cheaply elsewhere — preview and settings rendering by the Roborazzi
goldens, editor interaction by the Robolectric Compose tests in `src/testDebug`,
and note/tag/trash logic by the unit tests — and it was looking for controls
such as a "Preview" **text** button that had not existed for several versions.
Nothing is excluded now.

### Why `connectedDebugAndroidTest` is not what CI runs

It was tried, four times, in #238. The configuration works — the job builds,
filters, and reaches `connectedDebugAndroidTest`. The GitHub-hosted emulator
does not:

```text
[EmulatorConsole]: Failed to start Emulator console for 5554
[PropertyFetcher]: ShellCommandUnresponsiveException
Skipping device 'emulator-5554': Unknown API Level
Found 1 connected device(s), 0 of which were compatible.
```

Raw `adb` answers on that same device — a wait loop added before Gradle
reported `device settled at API 30` and ddmlib still timed out seconds later.
That is why `launch-smoke` passes while this fails: `launch-smoke` only uses
raw adb. It also means no adb-based waiting can fix it; the timeout is inside
ddmlib.

A managed device is the way around it, and is what the `instrumented-tests` job
uses. AGP starts and targets the emulator itself instead of discovering it
through ddmlib, so the property fetch that times out never happens. The device
is declared in `app/build.gradle.kts` under `testOptions.managedDevices`; the
CI job's only extra requirement is opening `/dev/kvm` permissions on the runner,
which the job does before calling Gradle.

Do not "fix" this by going back to `connectedDebugAndroidTest` plus a longer
adb wait. That was tried and the wait is not where the timeout lives.

### If a test fails, re-run on a fresh emulator before believing it

The UI-driving tests are sensitive to emulator health. On an emulator that has
been up for hours through repeated install cycles, they intermittently fail
with `No compose hierarchies found in the app` or a lone assertion failure that
does not reproduce. Cold-boot and re-run before investigating:

```powershell
emulator -avd markleaf-tablet-api36 -no-boot-anim -no-snapshot-load
```

A failure that survives a cold boot is real. One that does not is the emulator.

## Release Export — Local Gate, Not CI

**CI cannot check this.** `D:\Build` is a local path on the maintainer's
machine, so no pipeline can confirm the export ran. Run it, then verify it,
before pushing a tag:

```powershell
.\gradlew.bat :app:exportReleaseToBuildDrive
pwsh scripts/verify-release-export.ps1
```

The check asserts that three files exist and are non-empty for the current
`versionName`/`versionCode`, under the `markleaf-v<semver>-vc<code>` stem —
`.aab`, `.mapping.txt`, and `-release-notes.txt` — and that the notes file
carries a block for all six store locales. `D:\Build` is shared with other
projects, so only files matching that stem are considered.

This gate exists because the export is a manual step that silently went missing
for six consecutive releases. v2.27.2, v2.28.0, v2.28.1, v2.28.2, v2.28.3, and
v2.29.0 have no AAB and no mapping in `D:\Build`; the gap only surfaced while
removing the mapping from the GitHub Release assets (#244), and the mappings had
to be recovered from those assets before they were deleted.

It also checks the mapping specifically, which the export itself does not
guarantee: `writeReleaseArtifacts` throws when the AAB is missing but only logs
a warning and continues when the mapping is absent. A successful export is not
evidence that a mapping was written.

Since D064 the mapping is no longer a GitHub Release asset — it survives on
GitHub only as a 30-day workflow artifact. A release that skips this export and
is noticed more than 30 days later cannot be deobfuscated at all.

## Release Version and Notes Checks

Two PowerShell checks guard the parts of a release that are maintained by hand.
Both run in CI on every push and pull request, and both take their expected
version from `app/build.gradle.kts`. `versionName` and `versionCode` are the
source of truth rather than the latest git tag, because these checks run *before*
the tag is pushed — during release preparation the newest tag still points at the
previous version.

### Public version strings

```powershell
pwsh scripts/verify-landing-versions.ps1
```

The landing page ships in six languages: `docs/index.html` (English, the
canonical / x-default), `docs/index.ko.html`, `docs/index.ja.html`,
`docs/index.de.html`, `docs/index.es.html`, and `docs/index.fr.html`. The
"current release" version appears in three places per file — the JSON-LD
`softwareVersion`, the hero `release-line`, and the trust-ledger `<strong>`.

The six READMEs are checked as well. There the target is any line carrying a
release link (`releases/tag/vX.Y.Z` or `-/releases/vX.Y.Z`), and every version
string on such a line must match — so a stale link label is caught even when the
URL beside it was updated. Roadmap entries such as `- [x] v1.0.0 stable release`
carry no release link and are out of scope.

The hero `figcaption` screenshot version is tracked separately. It lags until
screenshots are re-taken, so it only has to stay consistent across the six
languages, not to equal the release version.

Missing files and missing version strings now fail. The earlier version of this
script compared the languages only against each other and merely warned when a
string was absent, so six equally-stale files passed: version-bump omissions went
unnoticed through v2.25.0 and v2.26.0 (#167).

When the check fails, update the lagging file — not the check.

The privacy pages (`docs/privacy*.html`) ship in the same six languages and
carry no version string, so they are outside this check.

### Release notes

```powershell
pwsh scripts/verify-release-notes.ps1
```

Three assertions:

- All six store locales have
  `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt`.
- Each stays within Play Console's 500-character-per-locale limit.
  `:app:exportReleaseToBuildDrive` enforces the same limit, but it runs at
  release time, after the release commit already exists — de-DE and fr-FR were
  once written at 526 and 525 characters and were caught only by counting them
  by hand (#167).

#### Aim the English draft at ~380 characters

Both checks above run *after* the six files are written, so an overrun means
rewriting all of them. That has happened at four cuts now (#167, then de/es/fr
at v2.30.0, then en-US at v2.31.0), and always the same way: the English note
fit, so it looked done.

It is not evidence the set fits. German compounds and the Romance languages run
consistently longer than the English source — v2.29.1's first draft came in at
545 for fr-FR and 517 for es-ES against an English note that was comfortably
inside the limit. Roughly 380 characters of English is what v2.29.1 settled at
and what leaves the translations room; at 450 the English passes and at least
one translation will not.

Count before translating, not after:

```powershell
(Get-Content -Raw fastlane/metadata/android/en-US/changelogs/<versionCode>.txt).Trim().Length
```

`verify-release-notes.ps1` warns from 450 (90% of the limit) so a note that
squeaks in still says so — but the warning has no teeth, and four of six
locales sat in that band at v2.31.0 (#252, #255, #262).
- `CHANGELOG.md` and `CHANGELOG.ko.md` carry the same version sections in the
  same order with the same dates, and both have a section for the current
  `versionName`. Titles are translations, so they are not compared.

## Release Notes Source and Language

GitHub release notes are not written separately — the tag job extracts them from
`CHANGELOG.md`. The `Prepare release notes` step in
`.github/workflows/android-build.yml` uses `awk` to take the section whose
heading starts with `## v<version>`, strips the trailing ` - YYYY-MM-DD` to form
the release title, and passes the remaining lines to `gh release create
--notes-file`.

Two consequences follow:

- `CHANGELOG.md` is written in **English**, so GitHub releases read in English
  by default. The Korean edition is kept in `CHANGELOG.ko.md`.
- The heading must stay in `## vX.Y.Z - Title - YYYY-MM-DD` form. A heading the
  parser cannot match yields an empty title file, and the job fails at
  `test -s release-title.txt` before anything is published.

Entries older than v2.16.0 remain in Korean below the "Earlier releases
(Korean)" heading and are not retranslated. That heading also stops the parser
from pulling Korean text into the v2.16.0 release body.

GitLab releases take a different path: they use the six-locale fastlane notes
bundled by `:app:exportReleaseArtifacts`, so they are already multilingual and
are unaffected by the language of `CHANGELOG.md`.

## CI Signing Secrets

Add the following names as GitHub Actions secrets and as GitLab CI/CD
variables:

```text
MARKLEAF_RELEASE_KEYSTORE_BASE64
MARKLEAF_RELEASE_STORE_PASSWORD
MARKLEAF_RELEASE_KEY_ALIAS
MARKLEAF_RELEASE_KEY_PASSWORD
```

`MARKLEAF_RELEASE_KEYSTORE_BASE64` is the Base64-encoded PKCS12 keystore file.

On GitLab, all four variables must be masked, hidden, and protected. Protect
the `v*` tag pattern so only Maintainers can create release tags and protected
tag pipelines can read the signing variables. Never expose these variables to
branch or merge-request jobs.

On tag pushes matching `v*`, GitHub Actions runs tests, builds the signed
release APK and AAB, and creates a GitHub Release with **one** asset attached:

<!-- release-assets: markleaf-vX.Y.Z.apk -->

- `markleaf-vX.Y.Z.apk` — signed, R8-shrunk APK for sideload installs and the Releases mirror

The comment above is not decoration. `scripts/verify-release-assets.ps1` reads
it and fails the `build` job if it disagrees with the arguments the workflow
actually passes to `gh release create` — this list drifted twice while D062 and
D064 shrank it, and nothing failed either time. Change the list and you change
all three copies together, or CI says so.

<!-- release-assets: markleaf-vX.Y.Z.apk -->

The signed AAB and the R8 mapping are built and verified in the same job but are
**not** attached to the GitHub Release. Both are uploaded as workflow artifacts
instead — `markleaf-release-aab` (14-day retention) and
`markleaf-release-mapping` (30-day retention) — and both reach their permanent
home through the local `exportReleaseToBuildDrive` hand-off
(see [Release Artifact Export](../AGENTS.md#release-artifact-export)) rather
than from GitHub. That hand-off is the **only** permanent copy: GitLab's
package registry stopped receiving them at v2.27.2 and the mirror step, though
it now runs, fails on every tag — see
[GitLab Release Assets](#gitlab-release-assets) and D066.

The release job fails before publishing if the keystore secret is missing, if
the APK certificate SHA-256 digest differs from the fixed production
certificate, or if the APK, AAB, or mapping file is missing from the build
outputs.

## GitLab Release Assets

**Attempted on every tag, and failing. GitLab mirrors refs, not release
artifacts (D066).** The two tag-job steps below run only when the repository
variable `GITLAB_RELEASE_MIRROR` is `true`, and since 2026-08-05 it is —
switched on while `GITLAB_TOKEN` still carries only `write_repository`. So
`Publish the GitLab release` returns HTTP 403 on every tag and the `release`
job ends red.

**A red `release` job does not mean the release failed.** The step runs after
`Create GitHub release`, so the APK is already published and downloadable when
it hits. v2.32.2 is the worked example: release published at 05:31Z with
`markleaf-v2.32.2.apk`, job red, nothing else wrong. Check the step list before
treating a red tag run as a broken release — if step 18 is the only failure,
this is what you are looking at.

`mirror-push` no longer carries `main`, and `mirror-check` no longer runs
(D067) — both skip on an unset `GITLAB_REF_MIRROR`. It never carried tags in
the first place, whatever this paragraph used to say: `mirror-push.yml`'s own
header states that its scope is `main` only and that tags are deliberately not
automated. The permanent copy of a released AAB and mapping is `D:\Build` — see
[Release Export](#release-export--local-gate-not-ci), which is a required
pre-tag step for exactly this reason, and now the only backup step that exists.

GitLab's newest Release is `v2.27.2 (2026-07-21)`. Eleven releases — v2.28.0
through v2.32.2 — have none.

### Settled: backed out (D068)

This section used to offer two ways out and note that the repository was in
neither. The second was taken on 2026-08-14:

1. ~~**Finish it.** Issue a GitLab Project Access Token with the `api` scope and
   replace the `GITLAB_TOKEN` secret.~~ Not taken. The token was never
   re-issued across three releases, and with D067 turning the ref mirror off
   there is no longer a GitLab role for it to complete.
2. **Back out — done.** The repository variable `GITLAB_RELEASE_MIRROR` is
   unset, so both steps skip and the tag job goes green. GitLab publishes
   nothing and mirrors nothing; it is a snapshot frozen at the v2.32.4 commit.

The machinery described below is kept, unchanged and unreferenced, on the
#211/#212 precedent: a deleted mechanism cannot be turned back on by someone
who does not know it existed. Reviving it means re-issuing the token **and**
setting the variable **and** deciding what GitLab is for again (D068).

### What the machinery does when it is on

The GitHub tag job calls `:app:exportReleaseArtifacts` with an explicit
`markleaf.releaseExportDir`. The task exports four flat, versioned files:

- `markleaf-vX.Y.Z-vcN.apk`
- `markleaf-vX.Y.Z-vcN.aab`
- `markleaf-vX.Y.Z-vcN.mapping.txt`
- `markleaf-vX.Y.Z-vcN-release-notes.txt`

`.github/scripts/mirror-release-to-gitlab.sh` uploads them to the GitLab
Generic Package Registry and creates the Release with links to them. Release
links therefore point to persistent package files rather than expiring CI job
artifacts.

### Why the GitHub runner publishes it, and why it still fails

This used to be `publish_gitlab_release` in `.gitlab-ci.yml`, and it stopped
working. GitLab's free shared-runner minutes are a monthly quota; the quota ran
out and every pipeline since has failed with `ci_quota_exceeded` before
reaching the release stage. Eight releases shipped that way — v2.28.0 through
v2.31.0 — while GitLab's newest Release stayed at v2.27.2 and D064 went on
describing GitLab as the off-machine permanent copy of the AAB and mapping
(#252).

The Packages and Releases **APIs do not consume CI minutes**; only the pipeline
did. Publishing from the GitHub runner, which is free for public repositories,
would restore the mirror without touching the quota (#275). The package
coordinates are the ones `glab` used, so links on v2.27.2 and links on later
releases address the same registry path.

Two things it depends on:

- **`GITLAB_TOKEN` needs the `api` scope.** The mirror-push token only needs
  `write_repository`, which cannot write packages or releases. The script
  probes the Packages API before uploading anything and fails with that
  message rather than part-publishing.
- **The tag must already be on GitLab.** This was true while release order was
  GitLab first, GitHub second. As of D068 tags are pushed to GitHub only, so
  this precondition can no longer be met — one more reason the machinery here
  stays parked rather than half-live. The script refuses rather than creating
  the tag itself.

The token has still not been re-issued, so v2.32.0, v2.32.1 and v2.32.2 all
ended their tag run red on a step that could not pass — after the GitHub
Release had already published its APK. D066 gated the steps behind
`GITLAB_RELEASE_MIRROR` for exactly that reason, but the variable was set on
2026-08-05 without the token, so they run and fail again rather than skip. The
token is the only thing left to fix; see
[Making it green again](#making-it-green-again).

The GitLab-side path is a separate switch and is genuinely off:
`package_signed_release` and
`publish_gitlab_release` require the project variable
`GITLAB_RELEASE_FROM_CI=true`. That is for the case it was built for — GitHub
unavailable and GitLab releasing on its own. With the variable unset the two
paths cannot both publish one tag.

The local Play Console hand-off remains separate:

```powershell
.\gradlew.bat :app:exportReleaseToBuildDrive
```

It writes only the signed AAB, mapping, and six-locale notes to `D:\Build`.
The GitLab APK does not change that local directory contract. This step is
mandatory before a tag, and verified — see
[Release Export](#release-export--local-gate-not-ci).

Before tagging, run the two gates CI does not cover — see
[Instrumented Tests](#instrumented-tests--local-gate-not-ci) and
[Release Export](#release-export--local-gate-not-ci):

```powershell
pwsh scripts/run-instrumented-tests.ps1
.\gradlew.bat :app:exportReleaseToBuildDrive
pwsh scripts/verify-release-export.ps1
```

Push the release tag to GitHub. That one push runs the GitHub release job,
which is the whole release: the signed APK, the GitHub Release, and F-Droid's
automatic pickup. The GitLab tag push was dropped in D068 — GitLab is a frozen
snapshot, its `main` stops at v2.32.4, and a tag pushed there would point at a
commit its own `main` cannot reach.

```bash
git tag v0.1.0
git push github v0.1.0
```
