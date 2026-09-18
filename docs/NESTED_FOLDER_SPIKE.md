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
  hidden dot-directories at any depth, and the **root** `attachments/`
  directory Markleaf owns. The depth condition on that last one is
  load-bearing: `MirrorWrite.mirrorAttachments` resolves the directory on the
  linked folder itself, so a `projects/attachments/` deeper in the tree is the
  user's own, and matching it by name alone would drop the notes in it. The
  match is case-sensitive for the same reason: `MirrorWrite` looks the
  directory up by the lowercase name, so on a case-sensitive provider a user's
  `Attachments/` is a directory Markleaf does not own. Folding case would lose
  its notes; not folding costs, at worst, some wasted listings against a
  directory holding images.
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
| `MirrorTraversalTest` (new) | 38 tests, 0 failures |
| Full unit suite (`:app:testDebugUnitTest`) | 837 tests, 0 failures |
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

The walk now settles the file case first and asks `isDirectory` only when
`canDescendFrom` says the answer could change something, so depth 0 issues
exactly the queries the flat listing issued.

A second instance of the same mistake turned up on re-reading the diff, in the
fix itself: `MirrorFileLookup.isMirrorEntry` reads `name` internally, so
calling it and then building a relative path fetched the name **twice** per
file — and `directoryVerdict` plus the child path did the same per directory.
The walk now spells out `isFile` + `isMirrorFile(name)` with the name read into
a local and reused.

Four tests pin all of this, using Mockito to count the calls directly since
`RawDocumentFile` cannot show them. Each was confirmed to fail against the
version it guards against before the fix was kept.

The lesson generalises past these two: on `DocumentFile`, *every property read
is IO*. Reviewing this code means counting property accesses, not reading it
for style.

Two consequences to keep in mind. `directoriesSkipped` counts only rule-based
skips, because identifying a directory costs the query the cap exists to avoid.
And **at depth 0 both counters are always 0**, which is not the same as "the
folder was fine" — nothing past the file test runs, so a file whose own
`isFile` query failed is passed over as quietly as anything else. They are
instruments for the recursion experiment, not a health check on today's flat
pass.

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

### 5. One note id can appear in two directories (needs confirming on a folder)

`importChangesFrom` resolves a file to a note with
`parsed.markleafId?.let(byId::get)`, and nothing downstream expects two files
to resolve to the *same* note in one pass. At depth 0 that is nearly
impossible: filenames are unique within a directory, so it takes a sync client
duplicating a file to `Note (2).md` with the id intact.

Recursion makes it ordinary. Copy a note into a subfolder — something people do
with folders — and both files carry the same `markleaf_id`. The pass then
reconciles the note twice, and the second visit can see a version the first one
just wrote. Conflict copies on every sync are the plausible failure.

Stated as a hazard rather than a measurement: it follows from reading the
import, and was not reproduced on a real folder. Whatever gives `Note` a path
(finding 1) is also what would let the import tell the two files apart, so this
is an argument for doing that first, not a separate task.

### 6. An unreadable directory looks exactly like an empty one (blocking for recursion)

Both `DocumentFile` implementations swallow a listing failure —
`TreeDocumentFile.listFiles` catches `Exception` around its provider query and
returns what it has, `RawDocumentFile.listFiles` returns empty when
`File.listFiles()` gives null (verified in the `documentfile-1.0.1` bytecode).
Neither throws.

So a subdirectory the provider refuses arrives at the traversal as an empty
directory. The walk reports no files from it, counts nothing, and there is no
signal anywhere that something was missed.

That is the same silent drop this whole spike is about, one level down: today a
subfolder's notes are invisible because nothing looks in it; with recursion
switched on, an *unreadable* subfolder's notes would be invisible because
looking in it returns nothing. Anything that ships recursion needs a way to
tell "empty" from "refused" — which means going to the children URI through
`ContentResolver` rather than `DocumentFile`, since the abstraction has already
thrown the distinction away by the time the walk sees it.

The same swallowing happens one level down, on individual entries:

- `isDirectory` is `"vnd.android.document/directory".equals(getRawType(uri))`,
  so a failed MIME query answers **"not a directory"** and the entry, plus any
  subtree behind it, disappears without a signal.
- `getName` is `queryForString(_display_name, null)`, so a failed display-name
  query answers **"no name"**.

Neither is distinguishable from a legitimate answer. The walk now counts both
as `entriesUnclassified`, kept apart from `directoriesSkipped` so that
"Markleaf refused this" and "the provider would not say" are never added
together. That is the closest thing to an error signal the abstraction permits,
and it is still one-directional: non-zero means something could not be read,
zero does not mean everything could — a directory whose *listing* failed never
reaches the count at all.

Caught by Codex on #425 in two passes: first against a `runCatching` that
claimed to detect listing failures, with a test that only passed because a mock
threw; then against the version that fixed it, where a nameless directory was
still being added to the rule-based skip figure. Both counters now say only
what they can support.

### 7. SAF cost grows per directory (needs a device)

`listFiles()` is one ContentProvider query per directory. A flat folder costs
exactly 1; a tree costs one per directory entered, and that count scales with
the user's tree rather than their note count. `MirrorFileLookup` already
records (#222) that folder scans dominated the cost of saving a note at a few
hundred files, so this is the number to measure before raising any default.

## Verdict

The traversal is cheap. The spike's value is that it isolates the cost to a
place it can be seen: **the blocker is not walking the tree, it is that a note
does not remember where it came from.** Findings 1 and 2 are the real work, and
2 is riskier than 1 because it is a cross-device on-disk format. Finding 5
folds into 1 — a path is what would let the import tell two copies of a note
apart — which is another reason to do that one first.

Nothing here changes behaviour for any existing user, and nothing here commits
the project to folders as a product. If this is not pursued, the branch can be
dropped without consequence; if it is, the next step is `Note.relativePath` and
the write direction, not more traversal.
