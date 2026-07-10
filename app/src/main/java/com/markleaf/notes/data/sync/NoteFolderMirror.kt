package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.time.Instant
import java.util.UUID

/**
 * Read/write notes to a user-chosen SAF folder as `.md` / `.txt` files with our
 * [SyncFrontmatter] header. This is the heart of the v2.1 multi-device flow:
 * Markleaf itself never goes online — but if the user points us at a folder
 * that some other app (Dropbox / Drive / Syncthing / NAS WebDAV mount) syncs,
 * the notes follow.
 *
 * Filenames track the note **title** (#134): the canonical link is the
 * frontmatter `markleaf_id`, so a note's file is located by parsing that id, not
 * by anything in the filename. When a title changes the file is renamed in place
 * (never deleted + recreated), so a mid-flight sync client never sees a note
 * vanish.
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
         * since the last sync. The remote file was kept as a duplicate note
         * (titled `<original> (다른 기기 사본 …)`) instead of overwriting
         * the local edits.
         */
        val conflicts: Int = 0
    )

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
        extension: SyncFileExtension = SyncFileExtension.MD
    ): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false

        val mirrorFiles = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }
        val existing = findFileForNote(context, mirrorFiles, note.id)
        val ext = existing?.mirrorExtension() ?: extension.value
        val desiredName = resolveName(note, ext, mirrorFiles, ownFile = existing)

        val target = existing
            ?: folder.createFile(mimeTypeFor(ext), desiredName)
            ?: return false

        // Write content first, then rename. If the rename fails the bytes are
        // already safely persisted under the old name — no data loss.
        val wrote = runCatching {
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write(SyncFrontmatter.encode(note))
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
        if (!wrote) return false

        if (existing != null && existing.name != desiredName) {
            existing.renameTo(desiredName) // best-effort; old name is fine if it fails
        }
        return true
    }

    /**
     * Rename a note's mirror file to match its current title *without* rewriting
     * the body — used by the Sync Center "tidy filenames" action so existing
     * mirrors created under the old `slug-id…` scheme migrate to clean titles in
     * one pass. Returns true only if a rename actually happened.
     */
    fun renameToTitle(context: Context, folderUri: Uri, note: Note): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false
        val mirrorFiles = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }
        val file = findFileForNote(context, mirrorFiles, note.id) ?: return false
        val desired = resolveName(note, file.mirrorExtension(), mirrorFiles, ownFile = file)
        if (file.name == desired) return false
        return runCatching { file.renameTo(desired) }.getOrDefault(false)
    }

    /**
     * Rewrite [file] in place with [note]'s frontmatter (incl. `markleaf_id`)
     * prepended, preserving any [extraKeys] the file already carried. Used when
     * importing a file that had no `markleaf_id` so the next reconcile can match
     * it by id rather than re-creating a duplicate note (#140). Best-effort —
     * returns false on any IO failure without throwing, so a write-back hiccup
     * never aborts an import.
     */
    private fun stampFrontmatter(
        context: Context,
        file: DocumentFile,
        note: Note,
        extraKeys: Map<String, String>
    ): Boolean {
        if (!file.canWrite()) return false
        return runCatching {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write(SyncFrontmatter.encode(note, extraKeys))
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    /**
     * Delete the mirrored note file and any per-note attachment subfolder for
     * [noteId]. Called from the permanent-delete flow so the mirror folder
     * doesn't accumulate orphan files. Idempotent — missing files are not
     * an error. Returns true if anything was actually removed.
     *
     * Direction is *DB → file only*; the inverse (file deleted externally → drop
     * the DB note) is intentionally not implemented to avoid silent data loss
     * when a sync client is mid-flight.
     */
    fun deleteNote(context: Context, folderUri: Uri, noteId: String): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false

        var changed = false
        val mirrorFiles = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }
        findFileForNote(context, mirrorFiles, noteId)?.let { file ->
            if (file.delete()) changed = true
        }
        // Also clear the per-note attachments subfolder.
        folder.findFile(ATTACHMENTS_DIR)?.findFile(noteId)?.let { dir ->
            if (deleteRecursively(dir)) changed = true
        }
        return changed
    }

    /**
     * Mirror the supplied attachment files into
     * `<folder>/attachments/<noteId>/`. Files already present with the same
     * name are *not* re-copied — attachment IDs are UUIDs so collisions imply
     * the same content. Used by the editor's auto-save hook so the note and
     * its referenced images travel together when the chosen folder is synced.
     */
    fun mirrorAttachments(
        context: Context,
        folderUri: Uri,
        noteId: String,
        sources: List<File>
    ): Int {
        if (sources.isEmpty()) return 0
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return 0
        if (!folder.canWrite()) return 0

        val attachmentsRoot = folder.findFile(ATTACHMENTS_DIR)
            ?: folder.createDirectory(ATTACHMENTS_DIR)
            ?: return 0
        val noteDir = attachmentsRoot.findFile(noteId)
            ?: attachmentsRoot.createDirectory(noteId)
            ?: return 0

        var copied = 0
        for (source in sources) {
            if (!source.exists()) continue
            val name = source.name
            // Skip if already mirrored — UUID filenames mean same name == same bytes.
            if (noteDir.findFile(name) != null) continue
            val target = noteDir.createFile(guessMimeType(name), name) ?: continue
            val ok = runCatching {
                context.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                    source.inputStream().use { input -> input.copyTo(out) }
                } != null
            }.getOrDefault(false)
            if (ok) copied++ else target.delete()
        }
        return copied
    }

    private fun deleteRecursively(node: DocumentFile): Boolean {
        if (node.isDirectory) {
            node.listFiles().forEach { deleteRecursively(it) }
        }
        return node.delete()
    }

    private fun guessMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    /**
     * Walk the folder, parse each mirror file, and reconcile with the supplied
     * existing notes. Returns aggregated counts.
     *
     * [existing] must be the *complete* note set — active, archived and
     * trashed. A file matching a hidden note is otherwise mistaken for a new
     * note and re-imported, which un-archives it or resurrects it from Trash
     * (#148). Files matching a trashed note are skipped outright.
     *
     * Conflict rule: *file wins iff its timestamp is strictly newer than the DB
     * record* (with a 2-second slack for filesystem clocks). Otherwise no change
     * is applied. Files without a `markleaf_id` become new notes (typical when
     * the user dropped a note into the folder by hand).
     *
     * `applyUpdate` is invoked synchronously — caller is responsible for
     * shipping the resulting writes onto IO dispatcher and into Room.
     */
    suspend fun importChanges(
        context: Context,
        folderUri: Uri,
        existing: List<Note>,
        applyUpdate: suspend (Note) -> Unit,
        applyCreate: suspend (Note) -> Unit
    ): ImportResult {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return ImportResult(0, 0, 0, 1)
        if (!folder.canRead()) return ImportResult(0, 0, 0, 1)

        var updated = 0
        var created = 0
        var skipped = 0
        var errors = 0
        var conflicts = 0

        val byId = existing.associateBy { it.id }
        val files = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }

        for (file in files) {
            val raw = runCatching {
                context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    ?.toString(Charsets.UTF_8)
            }.getOrNull()
            if (raw == null) {
                errors++
                continue
            }

            val parsed = SyncFrontmatter.decode(raw)
            val existingNote = parsed.markleafId?.let(byId::get)
            val fileTs = parsed.updatedAt ?: Instant.ofEpochMilli(file.lastModified())

            try {
                when (reconcileAction(existingNote, fileTs)) {
                    // A note in Trash keeps its mirror file on disk (deletion is
                    // reversible; only permanent delete removes it). Re-importing
                    // it would resurrect the deleted note (#148), so skip it.
                    Reconcile.SkipTrashed -> skipped++
                    // File isn't strictly newer than the DB note — nothing to do.
                    Reconcile.Skip -> skipped++
                    Reconcile.Create -> {
                        val now = Instant.now()
                        val newNote = Note(
                            id = parsed.markleafId ?: UUID.randomUUID().toString(),
                            title = TitleExtractor.extractTitle(parsed.body),
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body),
                            createdAt = parsed.createdAt ?: now,
                            updatedAt = parsed.updatedAt ?: now,
                            pinned = parsed.pinned ?: false,
                            archived = parsed.archived ?: false,
                            lastImportedAt = parsed.updatedAt ?: now
                        )
                        applyCreate(newNote)
                        created++
                        // A file with no `markleaf_id` (created in another app, or
                        // hand-dropped) has nothing for the next reconcile to match
                        // on, so every subsequent import would re-create it as a
                        // brand-new note — the #140 "same note appears 4-5 times"
                        // duplication. Stamp our id back into the file *in place* so
                        // the next pass matches by id and updates instead. Existing
                        // frontmatter keys are preserved. Best-effort: a failed
                        // write just defers de-duplication to a later sync.
                        if (parsed.markleafId == null) {
                            stampFrontmatter(context, file, newNote, parsed.unknownKeys)
                        }
                    }
                    Reconcile.Conflict -> {
                        // Both sides moved since the last sync. Keep the local
                        // note untouched and bring the remote in as a separate
                        // "(다른 기기 사본 …)" note so the user can compare and
                        // merge by hand.
                        val baseTitle = TitleExtractor.extractTitle(parsed.body)
                        val suffix = conflictSuffix(Instant.now())
                        val duplicate = Note(
                            id = UUID.randomUUID().toString(),
                            title = "$baseTitle $suffix",
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body),
                            createdAt = parsed.createdAt ?: fileTs,
                            updatedAt = fileTs,
                            pinned = false,
                            archived = false,
                            lastImportedAt = fileTs
                        )
                        applyCreate(duplicate)
                        conflicts++
                    }
                    Reconcile.Overwrite -> {
                        val note = existingNote!!
                        val merged = note.copy(
                            title = TitleExtractor.extractTitle(parsed.body),
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body),
                            updatedAt = fileTs,
                            pinned = parsed.pinned ?: note.pinned,
                            archived = parsed.archived ?: note.archived,
                            lastImportedAt = fileTs
                        )
                        applyUpdate(merged)
                        updated++
                    }
                }
            } catch (e: Exception) {
                errors++
            }
        }

        return ImportResult(
            updated = updated,
            created = created,
            skipped = skipped,
            errors = errors,
            conflicts = conflicts
        )
    }

    /** The action the reconcile takes for one mirror file. */
    internal enum class Reconcile { Create, SkipTrashed, Skip, Overwrite, Conflict }

    /**
     * Decide what [importChanges] should do with a single mirror file, purely
     * from its matching DB note (if any) and the file's effective timestamp.
     * Extracted so the decision — the #148 Trash skip and the newer-file /
     * local-edit conflict matrix — is unit-testable without the SAF IO path
     * (which stays covered by the live tablet smoke).
     *
     * @param existingNote the DB note whose id matches the file, or null when
     *   the file is unknown (a genuinely new / hand-dropped note).
     * @param fileTs the file's effective modified time (frontmatter `updatedAt`,
     *   falling back to the filesystem mtime).
     */
    internal fun reconcileAction(existingNote: Note?, fileTs: Instant): Reconcile {
        if (existingNote == null) return Reconcile.Create
        // A note in Trash must never be re-imported — that resurrects a note the
        // user deleted (#148). Archived notes are *not* skipped: they stay hidden
        // but still take edits from the folder.
        if (existingNote.trashed) return Reconcile.SkipTrashed
        val fileNewer =
            fileTs.toEpochMilli() > existingNote.updatedAt.toEpochMilli() + SLACK_MILLIS
        if (!fileNewer) return Reconcile.Skip
        val lastImport = existingNote.lastImportedAt?.toEpochMilli() ?: 0L
        val localEditedSinceImport =
            existingNote.updatedAt.toEpochMilli() > lastImport + SLACK_MILLIS
        return if (localEditedSinceImport) Reconcile.Conflict else Reconcile.Overwrite
    }

    private fun conflictSuffix(now: Instant): String {
        // Stable, locale-independent suffix the user can scan at a glance:
        // "(다른 기기 사본 0509 12:34)"
        val ldt = java.time.LocalDateTime.ofInstant(now, java.time.ZoneId.systemDefault())
        val date = "%02d%02d".format(ldt.monthValue, ldt.dayOfMonth)
        val time = "%02d:%02d".format(ldt.hour, ldt.minute)
        return "(다른 기기 사본 $date $time)"
    }

    /**
     * Locate the mirror file that belongs to [noteId] by parsing each file's
     * frontmatter `markleaf_id`. We only read the leading bytes — the
     * frontmatter always sits at the very top — so this stays cheap even though
     * it touches every file. Extension-agnostic: a note's file may be `.md` or
     * `.txt`.
     */
    private fun findFileForNote(
        context: Context,
        mirrorFiles: List<DocumentFile>,
        noteId: String
    ): DocumentFile? = mirrorFiles.firstOrNull { frontmatterId(context, it) == noteId }

    private fun frontmatterId(context: Context, file: DocumentFile): String? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.use { stream ->
            val buf = ByteArray(FRONTMATTER_PEEK_BYTES)
            var read = 0
            while (read < buf.size) {
                val n = stream.read(buf, read, buf.size - read)
                if (n < 0) break
                read += n
            }
            if (read <= 0) null
            else SyncFrontmatter.decode(String(buf, 0, read, Charsets.UTF_8)).markleafId
        }
    }.getOrNull()

    /**
     * The title-derived filename to use for [note], disambiguated against the
     * other files in the folder. The note's own [ownFile] is not a collision.
     */
    private fun resolveName(
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

    private fun isMirrorFile(name: String?): Boolean {
        val n = name?.lowercase() ?: return false
        return n.endsWith(".md") || n.endsWith(".txt")
    }

    private fun DocumentFile.mirrorExtension(): String =
        if (name?.lowercase()?.endsWith(".txt") == true) "txt" else "md"

    private fun mimeTypeFor(ext: String): String =
        if (ext == "txt") "text/plain" else "text/markdown"

    private const val SLACK_MILLIS = 2_000L
    private const val ATTACHMENTS_DIR = "attachments"
    private const val FRONTMATTER_PEEK_BYTES = 4096
}
