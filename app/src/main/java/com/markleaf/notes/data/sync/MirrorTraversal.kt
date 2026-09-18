package com.markleaf.notes.data.sync

import androidx.documentfile.provider.DocumentFile

/**
 * How the mirror enumerates the linked folder.
 *
 * ## Why this exists
 *
 * Eight places in this package spelled out the same line —
 * `folder.listFiles().filter { MirrorFileLookup.isMirrorEntry(it) }` (four of
 * them in [MirrorWrite] alone). That line is a *flat* listing, and since
 * [MirrorFileLookup.isMirrorEntry] requires `isFile`, a directory in the linked
 * folder is skipped without a word. A user whose vault has `projects/alpha.md`
 * therefore sees that file silently missing from Markleaf: no error, no count,
 * nothing to notice.
 *
 * Three of those eight — the survey and both import paths — now come through
 * here, which is every place that reads the folder *into* notes. The write
 * direction and [NoteFolderMirror.noteIdForFileName] still list flat on
 * purpose: they decide where a file goes, and nothing yet records where it came
 * from. See "What this deliberately does not do".
 *
 * ## Input / Output
 *
 * [walk] takes the folder root and a [maxDepth]; it returns every mirror file
 * at or under it, each paired with its path relative to the root, plus the
 * counters [MirrorWalk] carries for measuring what the traversal cost.
 *
 * ## Depth is a parameter, and its default is today's behaviour
 *
 * `maxDepth = 0` means "list the root and nothing else", which reproduces the
 * old line exactly — same files, same order, one `listFiles()` call. Every
 * caller still passes 0, so this change moves no user's files; the parameter is
 * what lets a test, or a later settings-driven import, ask for more without a
 * second code path to keep in step with the first.
 *
 * ## What this deliberately does not do
 *
 * It does not tell anyone where a note came from. [MirrorFileRef.relativePath]
 * is produced and then dropped by both call sites, because `Note` has no field
 * to put it in. Until it does, a note imported from `projects/alpha.md` is a
 * note like any other, and the *write* direction will put it back at the root.
 * That is why nothing here is wired to a user-facing switch yet — see the
 * spike notes in `docs/NESTED_FOLDER_SPIKE.md`.
 */
internal object MirrorTraversal {

    /**
     * One mirror file found by [walk].
     *
     * [relativePath] is `/`-separated and root-relative — `"note.md"` for a file
     * at the top, `"projects/alpha/note.md"` below it. It is always the
     * traversal's own view of where the file sits, never something read out of
     * the file, so two files with the same name in different directories are
     * distinguishable here even though `DocumentFile.name` cannot tell them
     * apart. Nothing consumes it yet; see the object's note on why.
     */
    internal data class MirrorFileRef(
        val file: DocumentFile,
        val relativePath: String
    )

    /**
     * The result of a [walk], with the counters that make its cost visible.
     *
     * The counters are the point of the spike rather than decoration:
     * `listFiles()` is a ContentProvider query, and [MirrorFileLookup] already
     * records (#222) that folder scans were the dominant cost of saving a note
     * at a few hundred files. A flat folder costs exactly one such call; a tree
     * costs one per directory, and [listCalls] is what says how many that turned
     * out to be for a real folder instead of a guessed one.
     *
     * [directoriesSkipped] counts only the directories skipped by a *rule* —
     * hidden, or `attachments/`. One the depth cap alone rules out is not
     * counted, because recognising it as a directory would cost the very query
     * the cap exists to avoid ([canDescendFrom]). At `maxDepth = 0` it is
     * therefore always 0, however many directories the folder holds.
     */
    internal data class MirrorWalk(
        val files: List<MirrorFileRef>,
        val listCalls: Int,
        val directoriesSkipped: Int
    )

    /** What [walk] should do with a directory it has just come across. */
    internal enum class DirectoryVerdict { DESCEND, SKIP }

    /**
     * Whether a directory found at [depth] could be descended into at all,
     * given [maxDepth] — the depth rule on its own, with no query behind it.
     *
     * Separate from [directoryVerdict] because of what asking costs. On a
     * SAF-backed folder `DocumentFile.isDirectory` is not a field: it runs
     * `getRawType` → `ContentResolver.query`, one provider round trip, and
     * `isFile` and `name` are each another. So "is this entry a directory" is a
     * question [walk] must not ask unless the answer can change what it does —
     * at `maxDepth = 0`, which is every production caller, it never can, and
     * asking anyway would have added one query per entry to every import and
     * survey pass. On a 400-file folder that is 400 extra round trips for
     * nothing, on the path #222 already found to be scan-dominated.
     */
    internal fun canDescendFrom(depth: Int, maxDepth: Int): Boolean = depth < maxDepth

    /**
     * Whether [walk] should go into a directory named [name], found at [depth]
     * levels below the root, when the walk is allowed to reach [maxDepth].
     *
     * Pure so the rules can be unit-tested without Android — the same reason
     * [MirrorFileNames] keeps its string logic separate from the IO around it.
     *
     * Three rules, each with a reason to be here:
     *
     * - **Depth.** A linked folder is someone's real directory tree and may sit
     *   above a deep one. The cap bounds the cost rather than trusting the
     *   folder to be shallow, and bounds it in `listFiles()` calls, which is the
     *   expensive unit.
     * - **Hidden directories.** `.git`, `.obsidian`, `.stfolder`,
     *   `.trash` — every sync client and editor keeps state in a dot-directory,
     *   and `.md` files in there are that tool's business, not notes the user
     *   wrote. Importing a Syncthing versioning folder would resurrect every
     *   note the user ever deleted.
     * - **The attachments directory.** Markleaf owns it ([MirrorWrite] writes
     *   images into `attachments/<noteId>/`). It holds no notes by
     *   construction, and descending into it would cost one `listFiles()` per
     *   note that has ever had an attachment.
     */
    internal fun directoryVerdict(name: String?, depth: Int, maxDepth: Int): DirectoryVerdict {
        if (!canDescendFrom(depth, maxDepth)) return DirectoryVerdict.SKIP
        val n = name ?: return DirectoryVerdict.SKIP
        if (n.startsWith(".")) return DirectoryVerdict.SKIP
        if (n.equals(MirrorWrite.ATTACHMENTS_DIR, ignoreCase = true)) return DirectoryVerdict.SKIP
        return DirectoryVerdict.DESCEND
    }

    /**
     * [parentPath] and a child [name] joined into a root-relative path.
     *
     * Pure, and separate from [walk] so the root case is pinned by a test: at
     * the root the parent path is empty and the result must be the bare name,
     * with no leading `/`. A path that started with a slash would read as
     * absolute to anything that later tried to resolve it.
     */
    internal fun childPath(parentPath: String, name: String): String =
        if (parentPath.isEmpty()) name else "$parentPath/$name"

    /**
     * The depth an import may actually use, given the [requested] depth and the
     * [metadata] mode the folder is in.
     *
     * **Sidecar mode is forced flat, and this is a real limitation rather than
     * caution.** The sidecar index maps a note to a *bare filename*
     * ([SidecarEntry.fileName]), and both the import and the survey look notes
     * up through [SidecarIndex.byFileName]. Two files named `note.md` in
     * different directories collapse to one key in that map, so a recursive
     * sidecar import would attach the wrong note to the wrong file — and
     * [MirrorImport.staleEntryIds], which compares the index against the folder
     * by filename, would agree with it. Making sidecar mode recurse means giving
     * [SidecarEntry] a path and bumping [SidecarIndex.SCHEMA_VERSION], and that
     * index is a format *other devices read*, so a version of it that old
     * devices mishandle is how #140 (the same note appearing four or five times)
     * comes back. Out of scope here; forcing 0 keeps the two modes from
     * disagreeing about which files exist.
     */
    internal fun effectiveDepth(metadata: MirrorMetadata, requested: Int): Int =
        if (metadata is MirrorMetadata.Sidecar) 0 else requested.coerceAtLeast(0)

    /**
     * Every mirror file at or under [root], breadth-first, to [maxDepth].
     *
     * Iterative rather than recursive: the depth cap already bounds this, but a
     * stack overflow on someone's notes folder is not a failure mode worth
     * leaving available at all.
     *
     * A directory that cannot be listed is counted and stepped over rather than
     * failing the walk. An unreadable corner of a tree is a normal thing for a
     * half-synced folder to contain, and the existing import treats an
     * unreadable *file* the same way — it counts an error and carries on, on the
     * principle that one bad entry must not cost the user the other 399.
     */
    internal fun walk(root: DocumentFile, maxDepth: Int): MirrorWalk {
        val files = mutableListOf<MirrorFileRef>()
        var listCalls = 0
        var directoriesSkipped = 0

        val queue = ArrayDeque<Pending>()
        queue.add(Pending(root, parentPath = "", depth = 0))

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            listCalls++
            val entries = runCatching { current.dir.listFiles() }.getOrNull()
            if (entries == null) {
                directoriesSkipped++
                continue
            }

            for (entry in entries) {
                // The file test comes first, and the directory test is guarded
                // by the depth rule, so that this loop costs exactly what the
                // flat listing it replaced cost. See [canDescendFrom]: every
                // one of these properties is a provider query on a real folder.
                // A mirror file is settled by `isMirrorEntry` alone; a
                // directory fails its `isFile` check and, at `maxDepth = 0`, is
                // never asked anything further.
                if (MirrorFileLookup.isMirrorEntry(entry)) {
                    files.add(
                        MirrorFileRef(
                            file = entry,
                            relativePath = childPath(current.parentPath, entry.name.orEmpty())
                        )
                    )
                    continue
                }
                if (!canDescendFrom(current.depth, maxDepth)) continue
                if (!entry.isDirectory) continue

                when (directoryVerdict(entry.name, current.depth, maxDepth)) {
                    DirectoryVerdict.SKIP -> directoriesSkipped++
                    DirectoryVerdict.DESCEND -> queue.add(
                        Pending(
                            dir = entry,
                            parentPath = childPath(current.parentPath, entry.name.orEmpty()),
                            depth = current.depth + 1
                        )
                    )
                }
            }
        }

        return MirrorWalk(
            files = files,
            listCalls = listCalls,
            directoriesSkipped = directoriesSkipped
        )
    }

    /** A directory the walk has queued but not yet listed. */
    private data class Pending(
        val dir: DocumentFile,
        val parentPath: String,
        val depth: Int
    )
}
