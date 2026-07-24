package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.domain.model.Note
import java.io.BufferedWriter
import java.io.OutputStreamWriter

/**
 * Converts a mirror folder between the two metadata modes (#216).
 *
 * Switching the setting alone would leave every existing file carrying a header
 * that is now meaningless, and the user was promised the folder would actually
 * be cleaned rather than waiting for four hundred notes to each be edited. Both
 * directions run over the folder once and rewrite each file in place.
 *
 * **Note bodies are never altered.** Converting *to* sidecar removes the header
 * Markleaf itself wrote and keeps everything below it; converting *back* puts a
 * header on top of the text that is already there. A file that never had a
 * header is left exactly as it is.
 */
object SidecarMigration {

    /** What a conversion did, for the UI to report. */
    data class Result(val converted: Int, val skipped: Int, val errors: Int)

    /**
     * Strip the frontmatter from every mirror file and record what it said in
     * [deviceId]'s index.
     *
     * The header is the only place a pre-switch folder holds the note id and the
     * created/pinned/archived flags, so it is read before it is removed —
     * dropping it first would orphan every file in the folder.
     */
    fun toSidecar(context: Context, folderUri: Uri, deviceId: String): Result {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return Result(0, 0, 1)
        if (!folder.canWrite()) return Result(0, 0, 1)

        var converted = 0
        var skipped = 0
        var errors = 0
        val entries = SidecarStore.ownEntries(context, folder, deviceId)

        for (file in folder.listFiles()) {
            if (!file.isFile || !isMirrorFileName(file.name)) continue
            val raw = read(context, file)
            if (raw == null) {
                errors++
                continue
            }
            val parsed = SyncFrontmatter.decode(raw)
            val name = file.name.orEmpty()
            if (!parsed.hasFrontmatter) {
                // Nothing to strip. Still worth an entry when we can name the
                // note behind it, so the first import does not treat a file we
                // already own as a stranger.
                skipped++
                continue
            }
            val noteId = parsed.markleafId
            if (noteId == null) {
                // A header with no id of ours in it belongs to another tool.
                // Removing it would destroy metadata we did not write.
                skipped++
                continue
            }
            if (!write(context, file.uri, parsed.body)) {
                errors++
                continue
            }
            entries[noteId] = SidecarEntry(
                noteId = noteId,
                fileName = name,
                contentHash = SidecarIndex.hashOf(parsed.body),
                createdAtMillis = parsed.createdAt?.toEpochMilli() ?: 0L,
                pinned = parsed.pinned ?: false,
                archived = parsed.archived ?: false
            )
            converted++
        }

        SidecarStore.write(context, folder, deviceId, entries.values)
        return Result(converted, skipped, errors)
    }

    /**
     * Put the frontmatter back on every file the index knows about, then drop
     * this device's index.
     *
     * [notes] supplies the values the header carries; a file whose note is gone
     * is left alone rather than guessed at. Only *our* index is removed — the
     * others belong to other devices, and deleting a file this device did not
     * write is the rule that keeps a mid-flight sync client from losing data.
     */
    fun toFrontmatter(
        context: Context,
        folderUri: Uri,
        deviceId: String,
        notes: List<Note>
    ): Result {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return Result(0, 0, 1)
        if (!folder.canWrite()) return Result(0, 0, 1)

        var converted = 0
        var skipped = 0
        var errors = 0
        val byId = notes.associateBy { it.id }
        val merged = SidecarStore.load(context, folder, deviceId)
        val byName = SidecarIndex.byFileName(merged)

        for (file in folder.listFiles()) {
            if (!file.isFile || !isMirrorFileName(file.name)) continue
            val name = file.name.orEmpty()
            val note = byName[name]?.noteId?.let(byId::get)
            if (note == null) {
                skipped++
                continue
            }
            val raw = read(context, file)
            if (raw == null) {
                errors++
                continue
            }
            // Already headed — a file another device wrote before the switch.
            if (SyncFrontmatter.decode(raw).hasFrontmatter) {
                skipped++
                continue
            }
            if (write(context, file.uri, SyncFrontmatter.encode(note.copy(contentMarkdown = raw)))) {
                converted++
            } else {
                errors++
            }
        }

        folder.findFile(SidecarIndex.fileNameFor(deviceId))?.let { own ->
            // Ours to remove: we created it, it means nothing once the mode is
            // off, and a stale copy would be misread if the mode came back on.
            runCatching { own.delete() }
        }
        SidecarStore.forget()
        return Result(converted, skipped, errors)
    }

    private fun isMirrorFileName(name: String?): Boolean {
        val n = name?.lowercase() ?: return false
        return n.endsWith(".md") || n.endsWith(".txt")
    }

    private fun read(context: Context, file: DocumentFile): String? = runCatching {
        context.contentResolver.openInputStream(file.uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    private fun write(context: Context, target: Uri, content: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(target, "wt")?.use { stream ->
            BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { it.write(content) }
        } ?: return@runCatching false
        true
    }.getOrDefault(false)
}
