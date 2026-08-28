package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.domain.model.Note
import java.util.concurrent.ConcurrentHashMap

/**
 * Everything about *finding* a note's mirror file: the frontmatter head peek,
 * the remembered-document cache, and the name rules that keep one note on one
 * file. The write and import paths in [MirrorWrite] and [MirrorImport] call
 * into this; nothing here decides *what* a sync pass does, only *where* a
 * note's bytes live.
 */
internal object MirrorFileLookup {

    private const val FRONTMATTER_PEEK_BYTES = 4096

    /**
     * Ceiling on how far [peekFrontmatter] will chase an unclosed frontmatter
     * block. A block we write is a few hundred bytes; one that has not ended
     * after 256 KB is not a header we should be reasoning about, and reading
     * further would put an unbounded cost on a path that runs over every file
     * in the folder each time a note is saved.
     */
    internal const val FRONTMATTER_MAX_BYTES = 256 * 1024

    /**
     * The item in [items] whose name is [name] — matched exactly if one is, and
     * only then ignoring case.
     *
     * The case-insensitive fallback is what a folder on exFAT or a Windows
     * share needs: those cannot hold `Notes.md` and `notes.md` at once, so a
     * name whose case changed outside Markleaf is still the same file and
     * refusing it would orphan the note. ext4 can hold both, and there the same
     * fallback picks one of two files belonging to two notes — which, on the
     * write path, overwrites a note the user never touched. Trying the exact
     * name first costs one pass and removes that coin flip; the fallback is
     * then only reached when no file bears the recorded name at all (#262).
     */
    internal fun <T> matchByName(items: List<T>, name: String, nameOf: (T) -> String?): T? =
        items.firstOrNull { nameOf(it) == name }
            ?: items.firstOrNull { nameOf(it).equals(name, ignoreCase = true) }

    /** A note's mirror file together with the frontmatter keys it already carries. */
    internal class MirrorMatch(val file: DocumentFile, val extraEntries: List<String>)

    /** A cached document that has been re-verified as this note's. */
    internal class CachedTarget(val uri: Uri, val extraEntries: List<String>)

    /** A remembered sidecar file, with the name its index entry must keep. */
    internal class SidecarTarget(val uri: Uri, val name: String)

    /**
     * Remembers which document holds each note, so an ordinary save doesn't have
     * to list the folder and read the head of every file in it to find one.
     *
     * That scan is O(files) SAF opens per save, on the path that holds up the
     * mirror write; at a few hundred notes it is the dominant cost of saving one
     * (#222). A remembered entry turns it into a single read.
     *
     * It caches a Uri and a name, never a `DocumentFile` — those hold a Context,
     * and a static cache holding an Activity's Context is a leak. The name is
     * what we last wrote the file as, which is enough to decide whether a rename
     * is due without touching the folder.
     *
     * Every hit is re-verified by reading the file's `markleaf_id`, so nothing
     * here is trusted:
     * - file gone, or the Uri now resolves elsewhere → the read fails or returns
     *   another id → forget it and take the slow path
     * - file renamed behind our back but still ours → the id matches, so its
     *   contents are still correct to write; the name is fixed on the next pass
     *   that goes the slow way
     *
     * Switching sync folders drops everything, since a Uri from one folder means
     * nothing in another.
     */
    internal object MirrorFileCache {
        private class Entry(val uri: Uri, val name: String)

        private val entries = ConcurrentHashMap<String, Entry>()

        @Volatile
        private var folder: Uri? = null

        /**
         * The document for [note] if it is remembered, its name still matches
         * the note's title, and it still carries the note's id — otherwise null,
         * and the caller falls back to scanning the folder.
         */
        fun hit(context: Context, folderUri: Uri, note: Note): CachedTarget? {
            if (folder != folderUri) return null
            val entry = entries[note.id] ?: return null
            // A drifted title needs the folder listing anyway, to disambiguate
            // the new name against every other file. Let the slow path have it.
            if (!MirrorFileNames.isPlainNameFor(entry.name, MirrorFileNames.sanitizeBase(note.title))) {
                return null
            }
            val head = peekFrontmatter(context, entry.uri)
            if (head?.markleafId != note.id) {
                entries.remove(note.id)
                return null
            }
            return CachedTarget(entry.uri, head.extraEntries)
        }

        /**
         * The same idea for sidecar mode, where there is no header to check the
         * document against. [indexName] — the name this device's index records
         * for the note — is the identity instead, and it has to still be the
         * document's actual name: a file renamed outside Markleaf would
         * otherwise be written under an entry pointing at a name nothing holds,
         * and a file no entry names is a file the next import reads as a new
         * note (#140).
         *
         * A drifted title also misses on purpose. Renaming needs the folder
         * listing anyway, to keep the new name clear of every other file.
         */
        fun hitSidecar(
            context: Context,
            folderUri: Uri,
            note: Note,
            indexName: String?
        ): SidecarTarget? {
            if (folder != folderUri) return null
            val name = indexName ?: return null
            val entry = entries[note.id] ?: return null
            if (!MirrorFileNames.isPlainNameFor(name, MirrorFileNames.sanitizeBase(note.title))) {
                return null
            }
            if (SidecarStore.documentName(context, entry.uri) != name) {
                entries.remove(note.id)
                return null
            }
            return SidecarTarget(entry.uri, name)
        }

        fun remember(folderUri: Uri, noteId: String, document: Uri, name: String) {
            if (folder != folderUri) {
                entries.clear()
                folder = folderUri
            }
            entries[noteId] = Entry(document, name)
        }

        fun forget(noteId: String) {
            entries.remove(noteId)
        }
    }

    /**
     * The file [note] should be written to. The canonical link is the
     * frontmatter `markleaf_id`; when no file carries it we fall back to
     * *adopting* a file that already has this note's exact name and belongs to
     * nobody — one with no `markleaf_id` of its own.
     *
     * Without that fallback a single lost id forks a new file on every
     * auto-save (#213): the lookup fails, [MirrorWrite.writeNote] creates a file
     * instead, and the collision guard names it `<title> (2).md`, then `(3)`,
     * and so on for as long as the user keeps typing. Ids go missing whenever
     * another app rewrites the file without preserving our block. Adoption is
     * safe because a file carrying *someone else's* id is never taken — only
     * unclaimed ones, and only under the undisambiguated name (`Notes.md`,
     * never `Notes (2).md`). "Unclaimed" means [peekFrontmatter] read the file's
     * whole block and found no id; a file it could not read to the end of is
     * skipped rather than adopted, so a header longer than the read cap is never
     * overwritten unseen (#222).
     *
     * The file's own unknown frontmatter keys come back alongside it so the
     * write can put them back rather than dropping tags another tool wrote.
     */
    internal fun findMirrorFile(
        context: Context,
        mirrorFiles: List<DocumentFile>,
        note: Note
    ): MirrorMatch? {
        val base = MirrorFileNames.sanitizeBase(note.title)
        var unclaimed: MirrorMatch? = null
        for (file in mirrorFiles) {
            val head = peekFrontmatter(context, file.uri) ?: continue
            if (head.markleafId == note.id) return MirrorMatch(file, head.extraEntries)
            if (head.markleafId == null &&
                unclaimed == null &&
                MirrorFileNames.isPlainNameFor(file.name.orEmpty(), base)
            ) {
                unclaimed = MirrorMatch(file, head.extraEntries)
            }
        }
        return unclaimed
    }

    /** What a file's head says about which note owns it. */
    private class HeadInfo(val markleafId: String?, val extraEntries: List<String>)

    /**
     * Whether a head read of [limit] bytes settled the question.
     *
     * Pulled out of the IO loop because the dangerous case is the quiet one:
     * a truncated read of a file whose frontmatter runs long looks exactly like
     * a file with no frontmatter at all. Treating the two alike let
     * [findMirrorFile] class such a file as *unclaimed*, adopt it, and rewrite
     * it — dropping metadata the file really had (#222).
     */
    internal fun headScanVerdict(
        hasFrontmatter: Boolean,
        opensFrontmatter: Boolean,
        blockClosed: Boolean,
        atEof: Boolean,
        limit: Int
    ): NoteFolderMirror.HeadScan = when {
        // Metadata parsed — everything we need is in hand.
        hasFrontmatter -> NoteFolderMirror.HeadScan.Done
        // No opening delimiter: this file has no block, and never will.
        !opensFrontmatter -> NoteFolderMirror.HeadScan.Done
        // The block opened and closed but held body text, not metadata — a pair
        // of horizontal rules. Reading further cannot change that verdict.
        blockClosed -> NoteFolderMirror.HeadScan.Done
        // Whole file read and the block never closed, so the opening `---` was
        // a rule too. decode() already treats it as body.
        atEof -> NoteFolderMirror.HeadScan.Done
        limit >= FRONTMATTER_MAX_BYTES -> NoteFolderMirror.HeadScan.Undetermined
        else -> NoteFolderMirror.HeadScan.NeedMore
    }

    /**
     * Read the head of the document at [documentUri] far enough to learn which
     * note owns it.
     *
     * Starts at [FRONTMATTER_PEEK_BYTES] — enough for any block we write, and
     * cheap enough to run over every file in the folder on each save — and only
     * reads further when the file opens a block that hasn't closed yet, up to
     * [FRONTMATTER_MAX_BYTES].
     *
     * Returns null when the file can't be read, and when a block is still open
     * at the cap. Both mean "this file tells us nothing", which the callers
     * treat as *skip*: better to leave an unreadable file alone — and write a
     * duplicate — than to adopt it and overwrite metadata we never saw.
     *
     * Deliberately does not return the parsed body: at these read sizes the
     * body is usually truncated, and the type shouldn't offer it.
     */
    private fun peekFrontmatter(context: Context, documentUri: Uri): HeadInfo? =
        runCatching {
            context.contentResolver.openInputStream(documentUri)?.use { stream ->
                var limit = FRONTMATTER_PEEK_BYTES
                var buf = ByteArray(limit)
                var read = 0
                while (true) {
                    var eof = false
                    while (read < limit) {
                        val n = stream.read(buf, read, limit - read)
                        if (n < 0) { eof = true; break }
                        read += n
                    }
                    if (read <= 0) return@use null
                    val text = String(buf, 0, read, Charsets.UTF_8)
                    val parsed = SyncFrontmatter.decode(text)
                    val verdict = headScanVerdict(
                        hasFrontmatter = parsed.hasFrontmatter,
                        opensFrontmatter = SyncFrontmatter.opensFrontmatter(text),
                        blockClosed = parsed.blockClosed,
                        atEof = eof,
                        limit = limit
                    )
                    when (verdict) {
                        NoteFolderMirror.HeadScan.Done ->
                            return@use HeadInfo(parsed.markleafId, parsed.unknownEntries)
                        NoteFolderMirror.HeadScan.Undetermined -> return@use null
                        NoteFolderMirror.HeadScan.NeedMore -> {
                            limit = (limit * 4).coerceAtMost(FRONTMATTER_MAX_BYTES)
                            buf = buf.copyOf(limit)
                        }
                    }
                }
                // Unreachable: every branch above returns.
                @Suppress("UNREACHABLE_CODE") null
            }
        }.getOrNull()

    /**
     * Locate the mirror file that belongs to [noteId] by parsing each file's
     * frontmatter `markleaf_id`. We only read the leading bytes — the
     * frontmatter always sits at the very top — so this stays cheap even though
     * it touches every file. Extension-agnostic: a note's file may be `.md` or
     * `.txt`.
     */
    internal fun findFileForNote(
        context: Context,
        mirrorFiles: List<DocumentFile>,
        noteId: String
    ): DocumentFile? =
        mirrorFiles.firstOrNull { peekFrontmatter(context, it.uri)?.markleafId == noteId }

    /**
     * The title-derived filename to use for [note], disambiguated against the
     * other files in the folder. The note's own [ownFile] is not a collision.
     */
    internal fun resolveName(
        note: Note,
        ext: String,
        mirrorFiles: List<DocumentFile>,
        ownFile: DocumentFile?
    ): String {
        val base = MirrorFileNames.sanitizeBase(note.title)
        val taken = mirrorFiles
            .filter { ownFile == null || it.uri != ownFile.uri }
            .mapNotNull { it.name?.lowercase() }
            .toHashSet()
        return MirrorFileNames.uniqueName(base, ext) { it.lowercase() in taken }
    }

    internal fun isMirrorFile(name: String?): Boolean {
        val n = name?.lowercase() ?: return false
        return n.endsWith(".md") || n.endsWith(".txt")
    }
}

internal fun DocumentFile.mirrorExtension(): String =
    if (name?.lowercase()?.endsWith(".txt") == true) "txt" else "md"
