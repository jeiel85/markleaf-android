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

Keep the mapping that corresponds to each released APK. GitHub Actions and
GitLab CI attach a versioned mapping file to every tag release so historical
versions can still be deobfuscated.

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
release APK and AAB, and creates a GitHub Release with **two** assets attached:

- `markleaf-vX.Y.Z.apk` — signed, R8-shrunk APK for sideload installs and the Releases mirror
- `markleaf-vX.Y.Z.mapping.txt` — R8 mapping for deobfuscating crash stack traces

The signed AAB is built and verified in the same job but is **not** attached to
the GitHub Release. It is uploaded as the `markleaf-release-aab` workflow
artifact, and it reaches Play Console through the local
`exportReleaseToBuildDrive` hand-off (see [Release Artifact Export](../AGENTS.md#release-artifact-export))
rather than from GitHub. GitLab publishes the AAB to its package registry as
well, so a permanent download link for it exists there — see
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
The GitLab APK does not change that local directory contract.

Before tagging, run the one gate CI does not cover — see
[Instrumented Tests](#instrumented-tests--local-gate-not-ci):

```powershell
pwsh scripts/run-instrumented-tests.ps1
```

Push the release tag to GitLab first, then GitHub (unless `SKIP_GITLAB_CI` is
on, the GitLab tag push runs the GitLab pipeline and the GitHub tag push runs
the GitHub release job):

```bash
git tag v0.1.0
git push gitlab v0.1.0
git push github v0.1.0
```
