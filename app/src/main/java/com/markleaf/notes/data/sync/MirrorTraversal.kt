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
     * [name] is the display name the walk already fetched to decide this entry
     * was a mirror file. It is carried rather than left to the caller because
     * `DocumentFile.name` is a provider query every time it is read: the walk
     * inlines `isMirrorEntry` precisely to spend that query once, and a caller
     * reaching for `file.name` afterwards spends it again — which both sidecar
     * call sites were doing, at roughly two extra round trips per file per pass
     * on the scan-dominated path of #222.
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
        val name: String,
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
     * What each figure can see at `maxDepth = 0`, since that is every
     * production call and the answer is not "everything":
     *
     * - [directoriesSkipped] is always 0. The loop returns before the directory
     *   branch, so no rule is ever applied.
     * - [entriesUnclassified] sees a **file the provider would not name** — the
     *   name is fetched anyway, so that costs nothing — and nothing else. An
     *   entry whose `isFile` query failed reads back as "not a file", falls
     *   through the depth gate, and is gone without being counted, because
     *   asking `isDirectory` to find out is the query the flat pass must not
     *   spend.
     *
     * So a zero here is weak evidence and a non-zero is strong evidence.
     *
     * **And nothing in production reads any of these.** All three call sites
     * take `.files` and drop the rest, so a provider failure the walk *did*
     * notice — a `.md` file whose display-name query failed, say — still leaves
     * `ImportResult.errors` at 0 and the user sees a clean sync. That follows
     * from what these are: instruments for the recursion experiment, read by
     * tests and by whoever measures on a device, not a health signal wired to
     * anything a user sees. Carrying them into `ImportResult` belongs to
     * whatever ships recursion, not here.
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
     * Whether [walk] should go into a directory named [name], when the walk is
     * allowed to reach [maxDepth].
     *
     * **[depth] is the depth of the directory this entry was *found in*, not of
     * the entry itself** — [walk] passes the containing directory's depth, so a
     * directory sitting in the linked root is judged at `depth = 0`. Reading it
     * the other way round inverts the `attachments/` rule: root-level
     * `attachments/` would be asked about at 1, get past the `depth == 0`
     * condition, and the walk would descend into Markleaf's own attachment
     * store. The tests encode this convention; the prose used to contradict it.
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
        // Only at the root, and only this exact name.
        //
        // [MirrorWrite.mirrorAttachments] resolves the attachments directory on
        // the linked folder itself, so Markleaf's storage is
        // `<root>/attachments/<noteId>/` and nothing else. A
        // `projects/attachments/` deeper in the tree is the user's own
        // directory with the user's own notes in it.
        //
        // The comparison is case-sensitive on purpose, and the two ways of
        // being wrong here are not worth the same. `MirrorWrite` looks the
        // directory up by the lowercase name, so on a case-sensitive provider a
        // user's `Attachments/` is a different directory that Markleaf does not
        // own — folding case would refuse it and take its notes with it. The
        // opposite error, on a provider that hands back another case for
        // Markleaf's own directory, costs a listing per note that has ever had
        // an attachment and imports nothing wrong, because what is in there is
        // images rather than `.md`. Wasted queries against lost notes is not a
        // close call.
        if (depth == 0 && n == MirrorWrite.ATTACHMENTS_DIR) {
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
     * nothing in it.
     *
     * What the guard costs depends on *which* directory throws, and only the
     * subdirectory case is cheap. A throwing subdirectory loses its own subtree
     * and the walk carries on. **A throwing root loses the whole walk**: the
     * queue is empty after it, so the result is a `MirrorWalk` with no files,
     * which the survey reports as an empty-but-readable folder and the import
     * as a pass that found nothing — a failure dressed as a clean sync. That is
     * unreachable through the two `documentfile` implementations, and the
     * production callers check `canRead()` before arriving here, but the
     * asymmetry is real and the earlier wording of this paragraph denied it.
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
                    if (name == null) {
                        // Called a file and then not named. `getName` defaults
                        // to null when its display-name query fails, so this is
                        // the same provider failure the directory branch counts
                        // — and `isMirrorFile(null)` is false, so without this
                        // the entry would be dropped as quietly as a `.png`
                        // while the counter reported nothing. The name is
                        // already fetched, so noticing costs no extra query.
                        entriesUnclassified++
                    } else if (MirrorFileLookup.isMirrorFile(name)) {
                        files.add(
                            MirrorFileRef(
                                file = entry,
                                name = name,
                                relativePath = childPath(current.parentPath, name)
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
