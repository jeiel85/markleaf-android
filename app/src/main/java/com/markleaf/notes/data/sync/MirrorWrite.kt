package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter

/**
 * The DB → folder direction of the mirror: writing a note's file, renaming it
 * after a title change, mirroring attachments, and the deletes that permanent
 * removal implies. Auto-export on save *only writes* — we are the source of
 * truth for our own edits, and never silently discard an unread newer file
 * ([MirrorImport.importChanges] is what reads).
 *
 * Content is written before any rename, so a failed rename leaves the bytes
 * safely persisted under the old name.
 */
internal object MirrorWrite {

    private const val ATTACHMENTS_DIR = "attachments"

    /**
     * [MirrorWrite.writeNote] once the folder has been resolved.
     *
     * Split out because resolving a user-granted tree Uri and operating on a
     * folder are separate concerns — and because the second one is where all the
     * behaviour lives. A test can hand this a [DocumentFile] over a temp
     * directory and exercise the real matching, adoption and rename paths
     * instead of leaving them to manual device smoke (#222).
     */
    internal fun writeNoteInto(
        context: Context,
        folder: DocumentFile,
        note: Note,
        extension: SyncFileExtension,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter
    ): Boolean {
        if (!folder.canWrite()) return false
        if (metadata is MirrorMetadata.Sidecar) {
            return writeNoteSidecar(context, folder, note, extension, metadata.deviceId)
        }

        // Fast path: we remember which document holds this note, its name
        // already matches the title, and it still carries the note's id. One
        // read instead of listing the folder and reading the head of every file
        // in it (#222).
        MirrorFileLookup.MirrorFileCache.hit(context, folder.uri, note)?.let { cached ->
            return writeContents(context, cached.uri, note, cached.extraEntries)
        }

        val mirrorFiles = folder.listFiles().filter { MirrorFileLookup.isMirrorFile(it.name) }
        val match = MirrorFileLookup.findMirrorFile(context, mirrorFiles, note)
        val existing = match?.file
        val ext = existing?.mirrorExtension() ?: extension.value
        val desiredName = MirrorFileLookup.resolveName(note, ext, mirrorFiles, ownFile = existing)

        val target = existing
            ?: folder.createFile(mimeTypeFor(ext), desiredName)
            ?: return false

        // Write content first, then rename. If the rename fails the bytes are
        // already safely persisted under the old name — no data loss.
        if (!writeContents(context, target.uri, note, match?.extraEntries.orEmpty())) return false

        if (existing != null && existing.name != desiredName) {
            existing.renameTo(desiredName) // best-effort; old name is fine if it fails
        }
        // Remember it under whatever name it ended up with — renameTo updates
        // the DocumentFile's uri in place, so this is the post-rename identity.
        target.name?.let {
            MirrorFileLookup.MirrorFileCache.remember(folder.uri, note.id, target.uri, it)
        }
        return true
    }

    /**
     * [writeNoteInto] in sidecar mode: the file gets the note's text and nothing
     * else, and the bookkeeping goes into this device's index (#216).
     *
     * The file is located by the filename the index records, falling back to
     * adopting an unclaimed file that already carries the note's name — the
     * same fallback the frontmatter path grew after a lost id forked a copy on
     * every save (#213). Recording the entry after the write is not optional:
     * it is what stops the next import seeing an unknown file and importing it
     * as a second copy of the same note (#140).
     */
    private fun writeNoteSidecar(
        context: Context,
        folder: DocumentFile,
        note: Note,
        extension: SyncFileExtension,
        deviceId: String
    ): Boolean {
        val ownEntries = SidecarStore.ownEntries(context, folder, deviceId)

        // The note's own file, remembered from its last save. This is the
        // common case — the same note saved again a second later — and taking
        // it skips the folder listing below, which is proportional to folder
        // size and ran on every save (#262). The frontmatter path grew
        // `MirrorFileCache` for exactly this; the sidecar path had no
        // equivalent because it has no header to identify a file by. The index
        // entry is that identity instead, so the cache is verified against it.
        MirrorFileLookup.MirrorFileCache.hitSidecar(
            context, folder.uri, note, ownEntries[note.id]?.fileName
        )?.let { cached ->
            val body = note.contentMarkdown
            if (!writeRawContents(context, cached.uri, body)) return false
            ownEntries[note.id] = SidecarEntry(
                noteId = note.id,
                fileName = cached.name,
                contentHash = SidecarIndex.hashOf(body),
                createdAtMillis = note.createdAt.toEpochMilli(),
                pinned = note.pinned,
                archived = note.archived
            )
            SidecarStore.write(context, folder, deviceId, ownEntries.values)
            return true
        }

        val mirrorFiles = folder.listFiles().filter { MirrorFileLookup.isMirrorFile(it.name) }

        // Our own index answers this for every note we have already written, so
        // the common save never reads another device's file. Falling back to the
        // merged view costs a folder listing plus a parse of every index, and it
        // is only needed for a note this device has not seen before (#262).
        val existing = ownEntries[note.id]?.fileName
            ?.let { name -> MirrorFileLookup.matchByName(mirrorFiles, name) { it.name } }
            ?: run {
                val merged = SidecarStore.load(context, folder, deviceId)
                merged[note.id]?.fileName
                    ?.let { name -> MirrorFileLookup.matchByName(mirrorFiles, name) { it.name } }
                    ?: adoptUnclaimedFile(mirrorFiles, note, merged)
            }

        val ext = existing?.mirrorExtension() ?: extension.value
        val desiredName = MirrorFileLookup.resolveName(note, ext, mirrorFiles, ownFile = existing)
        val target = existing
            ?: folder.createFile(mimeTypeFor(ext), desiredName)
            ?: return false

        // Body first, rename second — a failed rename leaves the bytes safe
        // under the old name, exactly as in the frontmatter path.
        val body = note.contentMarkdown
        if (!writeRawContents(context, target.uri, body)) return false
        if (existing != null && existing.name != desiredName) {
            existing.renameTo(desiredName)
        }

        val finalName = target.name ?: desiredName
        ownEntries[note.id] = SidecarEntry(
            noteId = note.id,
            fileName = finalName,
            contentHash = SidecarIndex.hashOf(body),
            createdAtMillis = note.createdAt.toEpochMilli(),
            pinned = note.pinned,
            archived = note.archived
        )
        // Remembered so the next save of this note takes the fast path above.
        MirrorFileLookup.MirrorFileCache.remember(folder.uri, note.id, target.uri, finalName)
        SidecarStore.write(context, folder, deviceId, ownEntries.values)
        return true
    }

    /**
     * A file carrying [note]'s title that no index entry claims — the sidecar
     * equivalent of the frontmatter path's adoption rule.
     *
     * "Unclaimed" is checked against every device's entries, not just ours: a
     * file another device owns has a note behind it, and taking it would give
     * two notes one file.
     */
    private fun adoptUnclaimedFile(
        mirrorFiles: List<DocumentFile>,
        note: Note,
        merged: Map<String, SidecarEntry>
    ): DocumentFile? {
        val base = MirrorFileNames.sanitizeBase(note.title)
        val claimed = merged.values.mapTo(HashSet()) { it.fileName.lowercase() }
        return mirrorFiles.firstOrNull { file ->
            val name = file.name.orEmpty()
            name.lowercase() !in claimed && MirrorFileNames.isPlainNameFor(name, base)
        }
    }

    /** Write [content] verbatim — no header, no trailing additions. */
    private fun writeRawContents(context: Context, target: Uri, content: String): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(target, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write(content)
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)

    /** Write [note] (plus any [extraEntries] the file carried) over [target]. */
    private fun writeContents(
        context: Context,
        target: Uri,
        note: Note,
        extraEntries: List<String>
    ): Boolean = runCatching {
        context.contentResolver.openOutputStream(target, "wt")?.use { stream ->
            BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                writer.write(SyncFrontmatter.encode(note, extraEntries))
            }
        } ?: return@runCatching false
        true
    }.getOrDefault(false)

    /**
     * Rename a note's mirror file to match its current title *without* rewriting
     * the body — used by the Sync Center "tidy filenames" action so existing
     * mirrors created under the old `slug-id…` scheme migrate to clean titles in
     * one pass. Returns true only if a rename actually happened.
     */
    internal fun renameToTitle(context: Context, folderUri: Uri, note: Note): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false
        val mirrorFiles = folder.listFiles().filter { MirrorFileLookup.isMirrorFile(it.name) }
        val file = MirrorFileLookup.findFileForNote(context, mirrorFiles, note.id) ?: return false
        val desired = MirrorFileLookup.resolveName(
            note, file.mirrorExtension(), mirrorFiles, ownFile = file
        )
        if (file.name == desired) return false
        return runCatching { file.renameTo(desired) }.getOrDefault(false)
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
    internal fun deleteNote(
        context: Context,
        folderUri: Uri,
        noteId: String,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter
    ): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false

        // Drop the remembered document up front: whether or not the delete
        // succeeds, the entry has stopped being trustworthy.
        MirrorFileLookup.MirrorFileCache.forget(noteId)
        var changed = false
        val mirrorFiles = folder.listFiles().filter { MirrorFileLookup.isMirrorFile(it.name) }
        // In sidecar mode the file carries no id to find it by, so the index is
        // the only link — and the entry has to go with the file, or the next
        // pass sees an entry for a note that no longer exists.
        val target = when (metadata) {
            is MirrorMetadata.Sidecar -> {
                val entries = SidecarStore.ownEntries(context, folder, metadata.deviceId)
                // Our own entry names the file for anything this device wrote.
                // Only a note we have never written needs the merged view, and
                // paying for it on every delete is what #262 was about.
                val name = entries[noteId]?.fileName
                    ?: SidecarStore.load(context, folder, metadata.deviceId)[noteId]?.fileName
                if (entries.remove(noteId) != null) {
                    SidecarStore.write(context, folder, metadata.deviceId, entries.values)
                }
                name?.let { n -> MirrorFileLookup.matchByName(mirrorFiles, n) { it.name } }
            }
            MirrorMetadata.Frontmatter ->
                MirrorFileLookup.findFileForNote(context, mirrorFiles, noteId)
        }
        target?.let { file ->
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
    internal fun mirrorAttachments(
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

    private fun mimeTypeFor(ext: String): String =
        if (ext == "txt") "text/plain" else "text/markdown"
}
