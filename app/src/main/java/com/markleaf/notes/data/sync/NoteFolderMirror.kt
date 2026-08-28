package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note

/**
 * Read/write notes to a user-chosen SAF folder as `.md` / `.txt` files with our
 * [SyncFrontmatter] header. This is the heart of the v2.1 multi-device flow:
 * Markleaf itself never goes online — but if the user points us at a folder
 * that some other app (Dropbox / Drive / Syncthing / NAS WebDAV mount) syncs,
 * the notes follow.
 *
 * Filenames track the note **title** (#134): the canonical link is the
 * frontmatter `markleaf_id`, so a note's file is normally located by parsing
 * that id rather than by anything in the filename. When a title changes the file
 * is renamed in place (never deleted + recreated), so a mid-flight sync client
 * never sees a note vanish.
 *
 * The filename is the *fallback* link, not a second source of truth: when no
 * file carries the id, a same-named file that no other note claims is adopted
 * rather than left alone. Without that, another app rewriting a file without
 * preserving our block left the note permanently unlinked, and every auto-save
 * forked another ` (2)` copy (#213).
 *
 * Safety posture:
 * - Auto-export on save *only writes* (we are the source of truth for our own
 *   edits). It overwrites a file we already wrote, but never silently discards
 *   an unread newer file (`importChanges` is what reads).
 * - `importChanges` only updates a DB note when the file is *newer* than the DB
 *   record. Never deletes notes. (Auto-delete sync is deliberately deferred.)
 * - Content is written before any rename, so a failed rename leaves the bytes
 *   safely persisted under the old name.
 * - Filename collisions (two notes with the same title) get a " (2)" suffix,
 *   never overwriting an unrelated file.
 *
 * An opt-in alternative keeps that bookkeeping in a hidden index beside the
 * notes instead of inside them — see [MirrorMetadata] and [SidecarIndex] (#216).
 *
 * This object is the public face of the mirror; the implementation lives across
 * [MirrorWrite] (DB → folder), [MirrorImport] (folder → DB),
 * [MirrorFileLookup] (where a note's file lives) and [MirrorReconcile] (whether
 * a file wins), and is reached through the members here so call sites and
 * tests keep one name to reference.
 */
object NoteFolderMirror {

    /** Result of an import pass for the Settings UI to show. */
    data class ImportResult(
        val updated: Int,
        val created: Int,
        val skipped: Int,
        val errors: Int,
        /**
         * Number of notes where both the local DB and the remote file moved
         * since the last sync. The remote side was kept as a separate note
         * flagged [Note.isConflictCopy] — the Sync Center lists those — instead
         * of overwriting the local edits.
         */
        val conflicts: Int = 0
    )

    /** The action the reconcile takes for one mirror file. */
    internal enum class Reconcile { Create, SkipTrashed, Skip, Overwrite, Conflict }

    /** Whether a head read has seen enough to answer that question. */
    internal enum class HeadScan {
        /** The block was read in full, or the file provably has none. */
        Done,

        /** A block is open and its end lies past what has been read. */
        NeedMore,

        /** The block never closed within the read cap — we cannot say. */
        Undetermined
    }

    internal val FRONTMATTER_MAX_BYTES: Int get() = MirrorFileLookup.FRONTMATTER_MAX_BYTES

    /**
     * Write a single note to the mirror folder. The file is named after the
     * note's title: we locate its existing file by frontmatter `markleaf_id`,
     * and if the title-derived name has drifted we rename the file in place. A
     * brand-new note's file is created with [extension]; an existing file keeps
     * whatever extension it already has (flipping the setting never rewrites old
     * files' suffixes).
     */
    fun writeNote(
        context: Context,
        folderUri: Uri,
        note: Note,
        extension: SyncFileExtension = SyncFileExtension.MD,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter
    ): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        return MirrorWrite.writeNoteInto(context, folder, note, extension, metadata)
    }

    /** [writeNote] once the folder has been resolved — see [MirrorWrite]. */
    internal fun writeNoteInto(
        context: Context,
        folder: DocumentFile,
        note: Note,
        extension: SyncFileExtension,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter
    ): Boolean = MirrorWrite.writeNoteInto(context, folder, note, extension, metadata)

    /**
     * [writeNote] plus the `lastImportedAt` stamp that records "the folder now
     * holds exactly this version of the note".
     *
     * Every mirror write needs that stamp. Only the editor's auto-save did it,
     * so a note seeded to the folder from Settings, the Sync Center or an
     * unlock kept `lastImportedAt == null` — which the reconcile reads as
     * "edited locally since the last import" for ever, sending every genuinely
     * newer file down the conflict path instead of overwriting cleanly (#217).
     *
     * [onStamped] receives the stamped note; callers pass their repository's
     * update so this stays free of a data-layer dependency.
     */
    suspend fun writeNoteAndStamp(
        context: Context,
        folderUri: Uri,
        note: Note,
        extension: SyncFileExtension,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter,
        onStamped: suspend (Note) -> Unit
    ): Boolean {
        val wrote = writeNote(context, folderUri, note, extension, metadata)
        if (wrote) onStamped(note.copy(lastImportedAt = note.updatedAt))
        return wrote
    }

    /** See [MirrorWrite.renameToTitle]. */
    fun renameToTitle(context: Context, folderUri: Uri, note: Note): Boolean =
        MirrorWrite.renameToTitle(context, folderUri, note)

    /** See [MirrorWrite.deleteNote]. */
    fun deleteNote(
        context: Context,
        folderUri: Uri,
        noteId: String,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter
    ): Boolean = MirrorWrite.deleteNote(context, folderUri, noteId, metadata)

    /** See [MirrorWrite.mirrorAttachments]. */
    fun mirrorAttachments(
        context: Context,
        folderUri: Uri,
        noteId: String,
        sources: List<java.io.File>
    ): Int = MirrorWrite.mirrorAttachments(context, folderUri, noteId, sources)

    /**
     * Walk the folder, parse each mirror file, and reconcile with the supplied
     * existing notes. Returns aggregated counts — see [MirrorImport].
     */
    suspend fun importChanges(
        context: Context,
        folderUri: Uri,
        existing: List<Note>,
        applyUpdate: suspend (Note) -> Unit,
        applyCreate: suspend (Note) -> Unit,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter,
        titleSource: NoteTitleSource = NoteTitleSource.FIRST_HEADING
    ): ImportResult {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return ImportResult(0, 0, 0, 1)
        return MirrorImport.importChangesFrom(
            context, folder, existing, applyUpdate, applyCreate, metadata, titleSource
        )
    }

    /** [importChanges] once the folder has been resolved — see [MirrorImport]. */
    internal suspend fun importChangesFrom(
        context: Context,
        folder: DocumentFile,
        existing: List<Note>,
        applyUpdate: suspend (Note) -> Unit,
        applyCreate: suspend (Note) -> Unit,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter,
        titleSource: NoteTitleSource = NoteTitleSource.FIRST_HEADING
    ): ImportResult = MirrorImport.importChangesFrom(
        context, folder, existing, applyUpdate, applyCreate, metadata, titleSource
    )

    /** See [MirrorFileLookup.matchByName]. */
    internal fun <T> matchByName(items: List<T>, name: String, nameOf: (T) -> String?): T? =
        MirrorFileLookup.matchByName(items, name, nameOf)

    /** See [MirrorReconcile.reconcileAction]. */
    internal fun reconcileAction(existingNote: Note?, fileTs: java.time.Instant): Reconcile =
        MirrorReconcile.reconcileAction(existingNote, fileTs)

    /** See [MirrorReconcile.sidecarReconcileAction]. */
    internal fun sidecarReconcileAction(
        existingNote: Note?,
        fileMatchesLastWrite: Boolean
    ): Reconcile = MirrorReconcile.sidecarReconcileAction(existingNote, fileMatchesLastWrite)

    /** See [MirrorReconcile.effectiveFileTimestamp]. */
    internal fun effectiveFileTimestamp(
        frontmatterUpdatedAt: java.time.Instant?,
        fileModifiedAt: java.time.Instant,
        bodyChanged: Boolean
    ): java.time.Instant = MirrorReconcile.effectiveFileTimestamp(
        frontmatterUpdatedAt, fileModifiedAt, bodyChanged
    )

    /** See [MirrorImport.staleEntryIds]. */
    internal fun staleEntryIds(
        entries: Collection<SidecarEntry>,
        liveNoteIds: Set<String>,
        fileNames: Collection<String>
    ): Set<String> = MirrorImport.staleEntryIds(entries, liveNoteIds, fileNames)

    /** See [MirrorFileLookup.headScanVerdict]. */
    internal fun headScanVerdict(
        hasFrontmatter: Boolean,
        opensFrontmatter: Boolean,
        blockClosed: Boolean,
        atEof: Boolean,
        limit: Int
    ): HeadScan = MirrorFileLookup.headScanVerdict(
        hasFrontmatter, opensFrontmatter, blockClosed, atEof, limit
    )
}
