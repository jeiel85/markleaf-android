# F-Droid Submission Notes

This file tracks the hand-off state for submitting Markleaf to the main
F-Droid repository.

## Current release

- App id: `com.markleaf.notes`
- Version name: `2.15.1`
- Version code: `86`
- Upstream tag: `v2.15.1`
- Tag commit: `6875a293e80bcec98118aa561b340d3bfa55f8a1`

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
pipeline is green: all nine CI jobs (including `fdroid build`) pass. The MR
is now awaiting F-Droid reviewer merge — no further action is required from
us until reviewers leave feedback.

## Notes for reviewers

Markleaf is intentionally local-first. The app has no INTERNET permission and
does not contact a Markleaf server. User data can leave the device only through
explicit Android OS-mediated actions such as Markdown export, Android share
sheet, external link opening, or a user-selected Storage Access Framework
folder.
