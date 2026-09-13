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

Markleaf is intentionally local-first. **The artefact F-Droid distributes (the
`store` flavour) declares no INTERNET permission**, and no build of Markleaf
contacts a Markleaf server — there is none. User data can leave the device only through
explicit Android OS-mediated actions such as Markdown export, Android share
sheet, external link opening, or a user-selected Storage Access Framework
folder.

## Phase 34 — the `store` flavour recipe MR (draft, not submitted)

This section is the P0 prerequisite recorded in D073 and
`docs/UPDATE_STRATEGY_EVALUATION.md`: once the app gains productFlavors
`store` / `github`, the upstream recipe must name the flavour.

### Why it is required (primary source)

F-Droid's Build Metadata Reference says of the `gradle:` field:

> "Build with Gradle instead of Ant, specifying what flavours to use. Flavours
> are case sensitive since the path to the output APK is as well."

> "If only one flavour is given and it is 'yes', no flavour will be used. Note
> that for projects with flavours, you must specify at least one valid flavour
> since 'yes' will build all of them separately."

So leaving `gradle: - yes` after flavours land does **not** fail loudly — it asks
F-Droid to build *every* flavour, including `github` (the one that declares
`INTERNET`). The reference also states the task becomes `assemble<flavours>Release`,
i.e. `assembleStoreRelease` for us.

The consequence is bounded but real. With `Binaries:` + `AllowedAPKSigningKeys`,
F-Droid publishes our APK only when it matches the one it built; a mismatch means
**that version is not published at all**. So the failure mode is a silently stalled
F-Droid update, not F-Droid shipping the sideload build.

### The metadata change

In the upstream `fdroiddata` repository, file `metadata/com.markleaf.notes.yml`.
The `Binaries:` line does **not** change — `markleaf-v%v.apk` stays the name of the
`store` artefact, and the sideload artefact ships under a different name that
F-Droid never looks at.

```diff
   - versionName: X.Y.Z
     versionCode: NNN
     commit: vX.Y.Z
     subdir: app
     gradle:
-      - yes
+      - store
```

Only the entry for the first flavour-carrying release changes. **Earlier entries
stay `yes`** — those versions genuinely had no flavours, and rewriting their recipe
would break the record of how they were built.

The copy of this file at `metadata/com.markleaf.notes.yml` in *this* repository is
a stale reference snapshot (it stops at v2.23.0). Editing it changes nothing
upstream, so it is deliberately left alone.

### Ordering — the MR cannot precede the tag

`docs/UPDATE_STRATEGY_EVALUATION.md` and D073 first said "don't push a
flavour-carrying release tag until the MR is merged." **That is not achievable**,
and the correction matters more than the original instruction:

- A Builds entry needs `commit: vX.Y.Z`. Before the tag exists there is nothing to
  check out, so the MR's own `fdroid build` job cannot run.
- `check apk` downloads the `Binaries:` URL, which only exists once the GitHub
  Release is published.

An MR opened before the release is therefore unverifiable, and F-Droid reviewers
merge on green pipelines. The workable sequence is:

1. Cut the flavour-carrying release and publish the GitHub Release as usual.
2. Open the fdroiddata MR for that version's entry immediately, with
   `gradle: - store`.
3. Until it merges, F-Droid publishes nothing new. Users stay on the previous
   version — a delay, not a broken install.

The real rule is therefore: **the release and the MR are one operation, and the
delay window is accepted knowingly** — not "tag only after merge."

### MR description draft

> **Markleaf (com.markleaf.notes): build the `store` flavour**
>
> Markleaf vX.Y.Z introduces two product flavours:
>
> - `store` — what F-Droid and Google Play distribute. Identical permissions to
>   every previous release: the app declares no `INTERNET` permission.
> - `github` — sideload-only. It adds `INTERNET` for an opt-in update check
>   (default off) against a static JSON file, and is published under a separate
>   release asset name that this recipe never references.
>
> Because `gradle: - yes` builds every flavour separately, this entry names
> `store` explicitly. `Binaries:` is unchanged: `markleaf-v%v.apk` is still the
> `store` artefact, so reproducible-build verification continues to compare the
> same thing it always has.
>
> The app's local-first guarantees are unchanged for the artefact F-Droid ships:
> no `INTERNET` permission, no analytics, no crash reporting, no Markleaf server.
> The upstream spec records this split in `docs/AGENT_SPEC.md` §15.1 and §15.9,
> and the reasoning in `docs/UPDATE_STRATEGY_EVALUATION.md`.

### Checklist before submitting

- [ ] The release is tagged and the GitHub Release carries `markleaf-vX.Y.Z.apk`
      built from the **`store`** flavour.
- [ ] Locally: `./gradlew :app:assembleStoreRelease` produces the published APK,
      and the `github` flavour is not part of that artefact.
- [ ] `aapt dump permissions` (or equivalent) on the published APK shows no
      `android.permission.INTERNET`.
- [ ] MR pipeline green — in particular `fdroid build` (it must locate the APK
      under the flavour-specific output path, which is where `subdir: app` mattered
      before) and `check apk`.
- [ ] After the *next* release, confirm the auto-added Builds entry inherited
      `gradle: - store`. **Whether `AutoUpdateMode: Version` copies fields from the
      previous entry is not documented in the pages consulted here**, so this is
      verified by observation rather than assumed — if it does not inherit, every
      future release needs the same one-line MR and that cost belongs in the
      decision.
