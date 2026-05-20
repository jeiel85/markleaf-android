# F-Droid Submission Notes

This file tracks the hand-off state for submitting Markleaf to the main
F-Droid repository.

## Current release

- App id: `com.markleaf.notes`
- Version name: `2.15.3`
- Version code: `88`
- Upstream tag: `v2.15.3`
- Tag commit: `85d03e3f79433c919414b843b2497769647bc226`

## Upstream readiness

- Apache 2.0 `LICENSE` is present.
- Runtime dependencies are FLOSS-oriented AndroidX / Kotlin / Room / Compose /
  Coil / commonmark-java dependencies.
- No Firebase, Google Play Services, analytics, ads, tracking SDK, account
  system, or proprietary crash reporting SDK.
- `android.permission.INTERNET` is not declared.
- Fastlane metadata exists under `fastlane/metadata/android`.
- Room schema JSON is committed under `app/schemas`.
- The tracked Windows-only `local.properties` file was removed.

## F-Droid metadata draft

The draft metadata file lives at:

```text
metadata/com.markleaf.notes.yml
```

For submission, copy that file into a fork of the F-Droid `fdroiddata`
repository at:

```text
fdroiddata/metadata/com.markleaf.notes.yml
```

Submitted merge request:

```text
https://gitlab.com/fdroid/fdroiddata/-/merge_requests/38659
```

Normal F-Droid metadata checks from a `fdroiddata` checkout:

```bash
fdroid readmeta
fdroid rewritemeta com.markleaf.notes
fdroid lint com.markleaf.notes
fdroid build com.markleaf.notes
```

Local submission-prep checks completed on 2026-05-19:

- `fdroid readmeta`
- `fdroid lint com.markleaf.notes` with the current fdroiddata `Writing`
  category available locally
- GitLab MR opened with `glab`

Docker Desktop was not available on the Windows workstation, so the local
Docker-based `fdroid build com.markleaf.notes` check was deferred to F-Droid
CI. The first CI run on 2026-05-19 surfaced three issues that the GitLab MR
pipeline catches before review:

- `AutoUpdateMode: Version v%v` failed the metadata JSON schema. New schema
  only accepts `None` or `Version` (with optional `+suffix`), so the field is
  now `AutoUpdateMode: Version`.
- `checkupdates` wanted `AutoName: Markleaf` added (auto-derived from the
  `<application android:label>` resource).
- `fdroid build` reported `Failed to find any output apks` because
  `fdroidserver` searches `<root_dir>/build/outputs/apk/release/` and our APK
  lands in `app/build/outputs/apk/release/`. Adding `subdir: app` to the
  Builds entry matches the standard Android Studio template layout convention
  also used by Markor and other similar apps.

After fixing the metadata on the MR branch (commit `5d56c5e69` on the
`add-markleaf-notes` branch of `jeiel85/fdroiddata`), the upstream MR head
pipeline went green: all nine CI jobs (including `fdroid build`) pass.

Reviewer `@linsui` then asked for the App Inclusion template, the `Note`
category, and `Binaries:` / `AllowedAPKSigningKeys:` for reproducible
builds. Adding the binary URL pattern surfaced a second blocker:

- `check apk` failed because the upstream `markleaf-v2.15.1.apk` contained
  AGP's auto-injected "Dependency metadata" signing block (`0x504B4453`),
  which `fdroid scanner` flags as an extra block.
- Fixed in v2.15.2 by setting `dependenciesInfo { includeInApk = false;
  includeInBundle = false }` in `app/build.gradle.kts` (versionCode 86 →
  87). Verified locally on `markleaf-v2.15.2.apk` that the block is gone
  while v2 signature and Verity padding remain.
- `fdroid rewritemeta` formatting (Binaries on its own indented line with
  the trailing space, `AllowedAPKSigningKeys` after `Builds:`) applied at
  the same time.

After pushing v2.15.2 to MR commit `f4c818953`, the upstream MR head
pipeline went green — all nine CI jobs passed, including `check apk` and
`fdroid build`.

Community tester `@dking08` then reported that opening preview on any
note containing a fenced code block crashed the app on Android 15. Root
cause: the `SyntaxHighlighter` shell-rule regex closed `\${...}` with a
bare `}`. JVM `java.util.regex` accepts that — every host-JVM unit test
and Roborazzi snapshot passed — but Android's ICU engine rejects it,
throwing during the object's static initializer and poisoning every
fenced-code-block render forever after. Fixed in v2.15.3 by escaping the
`}` and adding `SyntaxHighlighterAndroidTest`, an instrumented test that
exercises every language rule on a real Android runtime so the same
JVM/ICU regex mismatch cannot slip past again.

After pushing v2.15.3 to MR commit `3a18bbe50`, the upstream MR head
pipeline is again green — all nine CI jobs pass. The MR is now awaiting
F-Droid reviewer merge.

## Notes for reviewers

Markleaf is intentionally local-first. The app has no INTERNET permission and
does not contact a Markleaf server. User data can leave the device only through
explicit Android OS-mediated actions such as Markdown export, Android share
sheet, external link opening, or a user-selected Storage Access Framework
folder.
