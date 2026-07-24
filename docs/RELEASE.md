# Release Signing and Mirrored Releases

> **GitHub Actions is the primary CI and release path and never depends on
> GitLab.** GitLab CI is kept but gated by a manual switch, because GitLab
> shared-runner minutes are a monthly quota: set the project CI/CD variable
> `SKIP_GITLAB_CI=true` when the minutes run out and the whole GitLab pipeline
> skips cleanly (no blocked/stuck jobs, no minutes spent); unset it once the
> minutes reset and the GitLab pipeline + GitLab Release resume automatically.
> The GitLab-CI / GitLab-Release sections below apply whenever the pipeline is
> not skipped. GitLab also stays a git mirror — `main` via
> `.github/workflows/mirror-push.yml`, release tags pushed by hand, refs checked
> by `.github/workflows/mirror-check.yml`.
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

The `launch-smoke` job (emulator-based) currently runs on a debug APK and is
marked `continue-on-error: true` because of historical emulator flakiness on
GitHub-hosted runners. A release-APK runtime smoke (R8-shrunk, debug-signed)
is a planned follow-up.

GitLab CI independently runs `testDebugUnitTest`, `lintRelease`, and
`assembleDebug` for branches and merge requests. A semantic version tag
matching `vX.Y.Z` additionally builds the signed APK/AAB, exports the R8
mapping and six-locale notes, and verifies the APK certificate before
publishing. GitHub remains the canonical Roborazzi and emulator-smoke runner;
GitLab is an independent build and binary-distribution path.

## Instrumented Tests — Local Gate, Not CI

**CI does not run `connectedDebugAndroidTest`.** Nothing in
`app/src/androidTest/` is executed by any pipeline. Run it by hand before
cutting a release:

```powershell
pwsh scripts/run-instrumented-tests.ps1
```

The script picks the first connected device, checks it reports an API level,
and runs the suite with `com.markleaf.notes.ui` excluded. Add `-Serial` when
more than one device is attached.

### What it covers

The two places where a mistake costs a user their notes, and which no unit test
can reach:

- **Room migrations** — `AppDatabaseMigrationTest` walks v4 → current on a real
  Android runtime. A JVM test cannot; SQLite behaviour and Room's schema
  validation are the point.
- **The folder mirror's SAF IO** — `NoteFolderMirrorFolderTest`, twelve cases
  over a live `DocumentFile` tree: rewrite-in-place by id, rename on title
  change, adoption of an unclaimed file, never touching a file another note
  claims, preserving another tool's frontmatter, and the read-cap guard.
- **Navigation after a suspend point** — `NavigateAfterSuspendTest`, the #235
  regression.

`com.markleaf.notes.ui` is excluded because those classes drifted against the
UI they assert on while a crash made the suite unrunnable; 24 of them fail
(#239). The exclusion is by package, not an allow-list, so a test added
anywhere else runs by default rather than being silently skipped.

### Why it is not in CI

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
ddmlib. Tracked in #235, where Gradle Managed Devices is the next thing to try.

The honest trade: this is a **manual** gate, so it is only as reliable as the
person cutting the release. That is worse than automation and better than the
previous state, where these tests were run when someone happened to remember.

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

- `markleaf-vX.Y.Z.apk` — signed, R8-shrunk APK for sideload installs and the Releases mirror

The signed AAB and the R8 mapping are built and verified in the same job but are
**not** attached to the GitHub Release. Both are uploaded as workflow artifacts
instead — `markleaf-release-aab` (14-day retention) and
`markleaf-release-mapping` (30-day retention) — and both reach their permanent
home through the local `exportReleaseToBuildDrive` hand-off
(see [Release Artifact Export](../AGENTS.md#release-artifact-export)) rather
than from GitHub. GitLab publishes both to its package registry as well, so
permanent download links exist there — see
[GitLab Release Assets](#gitlab-release-assets).

The release job fails before publishing if the keystore secret is missing, if
the APK certificate SHA-256 digest differs from the fixed production
certificate, or if the APK, AAB, or mapping file is missing from the build
outputs.

## GitLab Release Assets

GitLab tag pipelines call `:app:exportReleaseArtifacts` with an explicit
`markleaf.releaseExportDir`. The task exports four flat, versioned files:

- `markleaf-vX.Y.Z-vcN.apk`
- `markleaf-vX.Y.Z-vcN.aab`
- `markleaf-vX.Y.Z-vcN.mapping.txt`
- `markleaf-vX.Y.Z-vcN-release-notes.txt`

The release job uploads them to the GitLab Generic Package Registry through
`glab release create --use-package-registry`. Release links therefore point to
persistent package files rather than expiring CI job artifacts. Job artifacts
are retained only long enough to transfer the files between CI stages.

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

Push the release tag to GitLab first, then GitHub (unless `SKIP_GITLAB_CI` is
on, the GitLab tag push runs the GitLab pipeline and the GitHub tag push runs
the GitHub release job):

```bash
git tag v0.1.0
git push gitlab v0.1.0
git push github v0.1.0
```
