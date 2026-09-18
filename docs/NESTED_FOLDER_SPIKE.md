# Nested Folder Support — Import-Direction Spike

## Date
2026-09-18

## Issue
#424 (notebooks/subfolders + toolbar), and the flat-folder product contract in
`docs/ROADMAP.md`. Folders as a *feature* were declined in #325 and #189; this
spike is not a reversal of that. It asks a narrower question.

## Question

The linked sync folder is flat by design. A user who points Markleaf at a vault
that already has subdirectories gets no error and no warning — the `.md` files
in those subdirectories simply do not exist as far as the app is concerned.

**How much does it cost to make the import direction see them?** Not to build a
folder UI, not to let the user create folders — only to stop silently dropping
files that are already there.

## What was built

One new object, `data/sync/MirrorTraversal.kt`, holding the folder-enumeration
decision that was previously copy-pasted:

- `walk(root, maxDepth)` — breadth-first, returns each mirror file with its
  root-relative path, plus `listCalls` and `directoriesSkipped` counters.
- `directoryVerdict(name, depth, maxDepth)` — pure skip rules: the depth cap,
  hidden dot-directories, and the `attachments/` directory Markleaf owns.
- `childPath(parent, name)` — pure path assembly.
- `effectiveDepth(metadata, requested)` — the depth a pass may actually use.

Wiring, all of it defaulted to today's behaviour:

- `MirrorSurvey.surveyFolder` and `MirrorImport.importChangesFrom` gained a
  `maxDepth: Int = 0` parameter, threaded through `NoteFolderMirror`.
- `maxDepth = 0` reproduces the old flat listing exactly — same files, one
  `listFiles()` call. **No caller passes anything else**, so no user's folder is
  read differently than it was before this branch.

### Scope actually covered

The exact line `folder.listFiles().filter { isMirrorEntry(it) }` appeared 8
times in `data/sync/` (4 of them in `MirrorWrite`). Three now route through
`MirrorTraversal` — the survey and both import paths, i.e. every path that reads
the folder *into* notes. Five remain flat on purpose: the four write paths and
`NoteFolderMirror.noteIdForFileName`.

## Verification

Run locally against a real `DocumentFile` tree (`DocumentFile.fromFile` over a
temp directory, the same technique `NoteFolderMirrorFolderTest` uses):

| Check | Result |
|---|---|
| `MirrorTraversalTest` (new) | 26 tests, 0 failures |
| Full unit suite (`:app:testDebugUnitTest`) | 825 tests, 0 failures |
| `:app:assembleDebug` | pass |
| `:app:verifyRoborazziDebug` | pass |
| `:app:lintRelease` | pass |

What the tests **cannot** show: SAF's real cost. `DocumentFile.fromFile` is a
filesystem call; the user's folder is a ContentProvider query. `listCalls` was
added so that cost can be counted on a device — it is not measured here.

### The per-entry query trap

`TreeDocumentFile` caches nothing. `isFile`, `isDirectory` and `name` each run
`getRawType` → `queryForString` → `ContentResolver.query` — verified by reading
the bytecode of `documentfile-1.0.1`, not assumed. So *which property the walk
asks for first* is a cost decision, not a style one.

The first version of `walk` tested `entry.isDirectory` before the file check,
which added one provider round trip per entry at `maxDepth = 0` — roughly 400
extra round trips on a 400-note folder, on every foreground and manual sync,
for a recursion that is switched off. Caught in review by Codex on #425.

The walk now tests `isMirrorEntry` first and asks `isDirectory` only when
`canDescendFrom` says the answer could change something, so depth 0 issues
exactly the queries the flat listing issued. Two tests pin it — one using
Mockito to count the calls directly, since `RawDocumentFile` cannot show them —
and both were confirmed to fail against the original ordering before the fix
was kept.

The consequence to keep in mind: `directoriesSkipped` counts only rule-based
skips. At depth 0 it is always 0, because identifying a directory would cost
the query the cap exists to avoid.

## Findings — what blocks shipping this

The traversal itself was small. Everything expensive is downstream of it.

### 1. The write direction would relocate the user's files (blocking)

`Note` has no path field, so a note imported from `projects/alpha.md` is
indistinguishable from a root-level note. The next save writes it to
`alpha.md` at the root while the original stays put — one note, two files.

This is why nothing is wired to a user-facing switch. Import-only recursion is
**not shippable on its own**; it needs `Note.relativePath`, a Room migration
(18 → 19), and `MirrorWrite` honouring the path.

### 2. Sidecar mode cannot express a tree at all (blocking for that mode)

`SidecarEntry` maps a note to a **bare filename**, and both the import and the
survey look notes up through `SidecarIndex.byFileName`. Two files named
`note.md` in different directories collapse onto one key —
`MirrorImport.staleEntryIds` compares by filename too and would agree with the
wrong answer.

Fixing it means adding a path to `SidecarEntry` and bumping
`SidecarIndex.SCHEMA_VERSION`. That index is a format **other devices read**, so
a version old devices mishandle is exactly how #140 (one note appearing four or
five times) returns.

`effectiveDepth` therefore forces sidecar mode flat, and the survey resolves
depth through the same function so the two passes cannot disagree about which
files exist (#372's lesson).

### 3. Local Markdown links reject paths (small, real)

`LocalMarkdownLink.fileName` returns null for any href containing `/`, so
`[spec](projects/alpha.md)` does not resolve today. `noteIdForFileName` also
still lists flat, so it would only ever find root-level files.

### 4. Wikilink titles become ambiguous (design, not code)

`[[Meeting]]` resolves by title. Two `Meeting.md` files in different
directories is normal in a vault with folders, and nothing decides which one
wins.

### 5. SAF cost grows per directory (needs a device)

`listFiles()` is one ContentProvider query per directory. A flat folder costs
exactly 1; a tree costs one per directory entered, and that count scales with
the user's tree rather than their note count. `MirrorFileLookup` already
records (#222) that folder scans dominated the cost of saving a note at a few
hundred files, so this is the number to measure before raising any default.

## Verdict

The traversal is cheap. The spike's value is that it isolates the cost to a
place it can be seen: **the blocker is not walking the tree, it is that a note
does not remember where it came from.** Findings 1 and 2 are the real work, and
2 is riskier than 1 because it is a cross-device on-disk format.

Nothing here changes behaviour for any existing user, and nothing here commits
the project to folders as a product. If this is not pursued, the branch can be
dropped without consequence; if it is, the next step is `Note.relativePath` and
the write direction, not more traversal.
