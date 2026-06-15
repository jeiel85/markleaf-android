package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.util.SlugGenerator
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.time.Instant
import java.util.UUID

/**
 * Read/write notes to a user-chosen SAF folder as `.md` files with our
 * [SyncFrontmatter] header. This is the heart of the v2.1 multi-device flow:
 * Markleaf itself never goes online — but if the user points us at a folder
 * that some other app (Dropbox / Drive / Syncthing / NAS WebDAV mount) syncs,
 * the notes follow.
 *
 * Safety posture for v2.1.0:
 * - Auto-export on save *only writes* (we are the source of truth for our
 *   own edits). It will overwrite a file we already wrote, but never silently
 *   discards an unread newer file (`importChanges` is what reads).
 * - `importChanges` only updates a DB note when the file is *newer* than the
 *   DB record. Never deletes notes. Never deletes files. (Auto-delete sync is
 *   deliberately deferred to a later cycle.)
 * - Filename collisions are resolved by appending a short suffix derived from
 *   the note id, never overwriting an unrelated file.
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
     * Write a single note to the mirror folder. Idempotent — if the file
     * already exists for this note id, it's overwritten. Uses [Note.id] as
     * the canonical link via frontmatter `markleaf_id`, and [SlugGenerator]
     * for the human-readable filename.
     */
    fun writeNote(context: Context, folderUri: Uri, note: Note): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false

        val target = findFileForNote(folder, note.id)
            ?: folder.createFile("text/markdown", uniqueFileName(folder, note))
            ?: return false

        return runCatching {
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write(SyncFrontmatter.encode(note))
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
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
     * Delete the mirrored `.md` and any per-note attachment subfolder for
     * [noteId]. Called from the permanent-delete flow so the mirror folder
     * doesn't accumulate orphan files. Idempotent — missing files are not
     * an error. Returns true if anything was actually removed.
     *
     * Direction is *DB → file only* in v2.6; the inverse (file deleted
     * externally → drop the DB note) is intentionally not implemented to
     * avoid silent data loss when a sync client is mid-flight.
     */
    fun deleteNote(context: Context, folderUri: Uri, noteId: String): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false

        var changed = false
        findFileForNote(folder, noteId)?.let { file ->
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
     * the same content. Used by the editor's auto-save hook so the .md and
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
     * Walk the folder, parse each `.md` file, and reconcile with the supplied
     * existing notes. Returns aggregated counts.
     *
     * Conflict rule for v2.1.0: *file wins iff its timestamp is strictly
     * newer than the DB record* (with a 2-second slack for filesystem clocks).
     * Otherwise no change is applied. Files without a `markleaf_id` become new
     * notes (typical when the user dropped a note into the folder by hand).
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
        val files = folder.listFiles().filter { it.isFile && it.name?.endsWith(".md") == true }

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

            try {
                if (existingNote == null) {
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
                } else {
                    val fileTs = parsed.updatedAt
                        ?: Instant.ofEpochMilli(file.lastModified())
                    val dbTs = existingNote.updatedAt
                    val fileNewer = fileTs.toEpochMilli() > dbTs.toEpochMilli() + SLACK_MILLIS
                    if (fileNewer) {
                        val lastImport = existingNote.lastImportedAt?.toEpochMilli() ?: 0L
                        val localEditedSinceImport = dbTs.toEpochMilli() > lastImport + SLACK_MILLIS
                        if (localEditedSinceImport) {
                            // Both sides moved since the last sync. Keep the
                            // local note untouched and bring the remote in as
                            // a separate "(다른 기기 사본 …)" note so the user
                            // can compare and merge by hand.
                            val now = Instant.now()
                            val baseTitle = TitleExtractor.extractTitle(parsed.body)
                            val suffix = conflictSuffix(now)
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
                        } else {
                            val merged = existingNote.copy(
                                title = TitleExtractor.extractTitle(parsed.body),
                                contentMarkdown = parsed.body,
                                excerpt = TitleExtractor.generateExcerpt(parsed.body),
                                updatedAt = fileTs,
                                pinned = parsed.pinned ?: existingNote.pinned,
                                archived = parsed.archived ?: existingNote.archived,
                                lastImportedAt = fileTs
                            )
                            applyUpdate(merged)
                            updated++
                        }
                    } else {
                        skipped++
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

    private fun conflictSuffix(now: Instant): String {
        // Stable, locale-independent suffix the user can scan at a glance:
        // "(다른 기기 사본 0509 12:34)"
        val ldt = java.time.LocalDateTime.ofInstant(now, java.time.ZoneId.systemDefault())
        val date = "%02d%02d".format(ldt.monthValue, ldt.dayOfMonth)
        val time = "%02d:%02d".format(ldt.hour, ldt.minute)
        return "(다른 기기 사본 $date $time)"
    }

    private fun findFileForNote(folder: DocumentFile, noteId: String): DocumentFile? {
        for (file in folder.listFiles()) {
            if (!file.isFile) continue
            val name = file.name ?: continue
            if (!name.endsWith(".md")) continue
            val raw = runCatching {
                file.uri // existence check
                file
            }.getOrNull() ?: continue
            // We could parse every file to find the matching id, but that's O(n²) on
            // every save. Instead, store the id in the filename suffix as a hash — see
            // [uniqueFileName]. If a file with our slug already exists, we trust it;
            // otherwise we create a new one. The reconcile pass handles the rare drift.
            // For now, match by id-suffix in the filename.
            if (name.contains(noteIdMarker(noteId))) return raw
        }
        return null
    }

    private fun uniqueFileName(folder: DocumentFile, note: Note): String {
        val slug = SlugGenerator.generateSlug(note.title).ifEmpty { "untitled" }
        val marker = noteIdMarker(note.id)
        val candidate = "$slug-$marker.md"
        // Collision is essentially impossible with the marker, but guard anyway.
        if (folder.findFile(candidate) == null) return candidate
        return "$slug-$marker-${System.currentTimeMillis()}.md"
    }

    /** Stable short suffix derived from the note id so we can find the file later. */
    private fun noteIdMarker(noteId: String): String =
        "id" + noteId.replace("-", "").take(8)

    private const val SLACK_MILLIS = 2_000L
    private const val ATTACHMENTS_DIR = "attachments"
}
