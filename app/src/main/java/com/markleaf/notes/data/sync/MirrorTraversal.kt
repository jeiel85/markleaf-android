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
     * costs one per directory.
     *
     * [listCalls] counts listings *attempted*, including one that then failed,
     * because the round trip is spent either way — it is a cost figure, not a
     * success figure.
     *
     * [directoriesSkipped] counts the directories a *rule* refused — hidden, or
     * `attachments/`. Nothing else is in it:
     *
     * - A directory the depth cap alone rules out is not counted, because
     *   recognising it as a directory would cost the very query the cap exists
     *   to avoid ([canDescendFrom]). At `maxDepth = 0` the figure is therefore
     *   0, however many directories the folder holds.
     * - **A directory whose listing failed is not counted either, and cannot
     *   be.** Both `DocumentFile` implementations swallow that failure:
     *   `TreeDocumentFile.listFiles` catches `Exception` around its provider
     *   query and returns what it has, and `RawDocumentFile.listFiles` returns
     *   empty when `File.listFiles()` gives null. An unreadable directory
     *   therefore arrives here as an *empty* one, indistinguishable from a
     *   directory with nothing in it, and the walk has nothing to count.
     * - **An entry whose metadata failed goes in [entriesUnclassified], not
     *   here.** `isDirectory` is `"…/directory".equals(getRawType(uri))` and
     *   `getName` defaults to null, so a failed MIME or display-name query
     *   reads back as "not a directory" or "no name" rather than as an error.
     *   Counting those as skips would say Markleaf chose to pass over them,
     *   which is the opposite of what happened.
     *
     * [entriesUnclassified] is that second bucket: entries the provider would
     * not describe. It is the closest thing to an error signal this abstraction
     * permits, and it is still not a reliable one — a genuinely odd entry lands
     * here too, and a directory whose *listing* failed never reaches it at all.
     * Non-zero means something in the folder could not be read; zero does not
     * mean everything could. See the spike notes.
     *
     * **At `maxDepth = 0` both counters are always 0**, and that is not the
     * same as "the folder was fine". Nothing past the file test runs at that
     * depth, so a file whose own `isFile` query failed is passed over as
     * quietly as everything else. Neither figure says anything about
     * readability until recursion is switched on; they are instruments for the
     * experiment, not a health check on today's flat pass.
     */
    internal data class MirrorWalk(
        val files: List<MirrorFileRef>,
        val listCalls: Int,
        val directoriesSkipped: Int,
        val entriesUnclassified: Int
    )

    /**
     * What [walk] should do with a directory it has just come across.
     *
     * [UNKNOWN] is separate from [SKIP] because the two are different facts
     * about the folder, and merging them is how a counter starts lying. A skip
     * is Markleaf's own decision; an unknown is the provider declining to say
     * what the entry is, which is a thing the *user* might want to hear about.
     */
    internal enum class DirectoryVerdict { DESCEND, SKIP, UNKNOWN }

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
     *   above a deep one, so the cap stops the walk trusting it to be shallow —
     *   and it is what makes a symlink cycle terminate. It bounds *depth* only:
     *   breadth is still whatever the folder holds, so a shallow directory with
     *   a thousand subdirectories costs a thousand `listFiles()` calls. If a
     *   default above 0 is ever shipped, that is the case to measure, not this
     *   one.
     * - **Hidden directories.** `.git`, `.obsidian`, `.stfolder`,
     *   `.trash` — every sync client and editor keeps state in a dot-directory,
     *   and `.md` files in there are that tool's business, not notes the user
     *   wrote. Importing a Syncthing versioning folder would resurrect every
     *   note the user ever deleted.
     * - **The root attachments directory, and only that one.** Markleaf owns
     *   `<root>/attachments/<noteId>/` — [MirrorWrite.mirrorAttachments]
     *   resolves it on the linked folder itself, never deeper. It holds no
     *   notes by construction, and descending into it would cost one
     *   `listFiles()` per note that has ever had an attachment. The [depth]
     *   condition is load-bearing: a `projects/attachments/` further down is
     *   the user's own directory, and matching it by name would drop the notes
     *   inside it.
     */
    internal fun directoryVerdict(name: String?, depth: Int, maxDepth: Int): DirectoryVerdict {
        if (!canDescendFrom(depth, maxDepth)) return DirectoryVerdict.SKIP
        // Not a rule: `getName` returns null when its display-name query fails,
        // so a nameless directory is one the provider would not describe rather
        // than one Markleaf chose to pass over. The rules below cannot be
        // applied to it either way, but which bucket it lands in is the
        // difference between a counter that reports and one that misreports.
        val n = name ?: return DirectoryVerdict.UNKNOWN
        if (n.startsWith(".")) return DirectoryVerdict.SKIP
        // Only at the root. [MirrorWrite.mirrorAttachments] resolves the
        // attachments directory on the linked folder itself, so Markleaf's
        // storage is `<root>/attachments/<noteId>/` and nothing else. A
        // `projects/attachments/` deeper in the tree is the user's own
        // directory with the user's own notes in it, and skipping that by name
        // would drop them exactly the way this spike exists to stop.
        if (depth == 0 && n.equals(MirrorWrite.ATTACHMENTS_DIR, ignoreCase = true)) {
            return DirectoryVerdict.SKIP
        }
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
     * The `runCatching` around the listing is a guard for a [DocumentFile]
     * implementation that throws, and **not** a way of detecting an unreadable
     * directory: neither implementation in `documentfile` throws, they both
     * hand back an empty listing instead (see [MirrorWalk]). So an unreadable
     * directory is stepped over here in the only way it can be — as one with
     * nothing in it — and the guard exists so that a subclass which does throw
     * costs the user one directory rather than the whole walk.
     */
    internal fun walk(root: DocumentFile, maxDepth: Int): MirrorWalk {
        val files = mutableListOf<MirrorFileRef>()
        var listCalls = 0
        var directoriesSkipped = 0
        var entriesUnclassified = 0

        val queue = ArrayDeque<Pending>()
        queue.add(Pending(root, parentPath = "", depth = 0))

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            listCalls++
            // Not counted as a skip: a directory that fails to list is
            // indistinguishable from an empty one on a real provider, so
            // counting the rare throwing case would make the figure mean two
            // things depending on which implementation was underneath it.
            val entries = runCatching { current.dir.listFiles() }.getOrNull() ?: continue

            for (entry in entries) {
                // Every property read here is a provider query on a real
                // folder, so the order and the count both matter — see
                // [canDescendFrom]. Two rules hold this loop to what the flat
                // listing it replaced cost:
                //
                //  - the file case is settled first, and `name` is read into a
                //    local because the path below needs the same string. This
                //    is `MirrorFileLookup.isMirrorEntry` spelled out rather
                //    than called: that helper reads `name` itself, so calling
                //    it and then building a path cost two `getName()` queries
                //    per file where the old line cost one.
                //  - "is this a directory" is asked only once the depth rule
                //    says the answer could change something, which at
                //    `maxDepth = 0` it never can. A directory is dismissed by
                //    its `isFile` alone, before its name is ever fetched.
                if (entry.isFile) {
                    val name = entry.name
                    if (MirrorFileLookup.isMirrorFile(name)) {
                        files.add(
                            MirrorFileRef(
                                file = entry,
                                relativePath = childPath(current.parentPath, name.orEmpty())
                            )
                        )
                    }
                    continue
                }
                if (!canDescendFrom(current.depth, maxDepth)) continue
                if (!entry.isDirectory) {
                    // Neither a file nor a directory. `isDirectory` is
                    // `"…/directory".equals(getRawType(uri))`, so a MIME query
                    // the provider failed reads back here as a plain "no" — and
                    // whatever was behind it, subtree included, is gone without
                    // a word. Counting it is the only trace this abstraction
                    // leaves, and it is not the same fact as a rule-based skip.
                    entriesUnclassified++
                    continue
                }

                val name = entry.name
                when (directoryVerdict(name, current.depth, maxDepth)) {
                    DirectoryVerdict.SKIP -> directoriesSkipped++
                    DirectoryVerdict.UNKNOWN -> entriesUnclassified++
                    DirectoryVerdict.DESCEND -> queue.add(
                        Pending(
                            dir = entry,
                            parentPath = childPath(current.parentPath, name.orEmpty()),
                            depth = current.depth + 1
                        )
                    )
                }
            }
        }

        return MirrorWalk(
            files = files,
            listCalls = listCalls,
            directoriesSkipped = directoriesSkipped,
            entriesUnclassified = entriesUnclassified
        )
    }

    /** A directory the walk has queued but not yet listed. */
    private data class Pending(
        val dir: DocumentFile,
        val parentPath: String,
        val depth: Int
    )
}
