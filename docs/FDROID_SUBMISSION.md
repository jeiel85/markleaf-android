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

Then run the normal F-Droid metadata checks from the `fdroiddata` checkout:

```bash
fdroid readmeta
fdroid rewritemeta com.markleaf.notes
fdroid lint com.markleaf.notes
fdroid build com.markleaf.notes
```

If those pass, commit the metadata file in the fdroiddata fork and open a
GitLab merge request against `fdroid/fdroiddata`.

## Notes for reviewers

Markleaf is intentionally local-first. The app has no INTERNET permission and
does not contact a Markleaf server. User data can leave the device only through
explicit Android OS-mediated actions such as Markdown export, Android share
sheet, external link opening, or a user-selected Storage Access Framework
folder.
