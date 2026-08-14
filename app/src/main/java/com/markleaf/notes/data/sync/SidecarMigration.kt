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
     *
     * ## Why this runs in two phases
     *
     * A stripped file with no index entry is the one state this folder must
     * never be in. It carries no id in either place, so the next import mints a
     * fresh one — `NoteFolderMirror` line-for-line: `entry?.noteId ?:
     * parsed.markleafId ?: UUID.randomUUID()` — and the note comes back as a
     * second copy of itself (#140). A *headed* file with no entry is harmless by
     * comparison: the import strips the stray header and uses the id inside it.
     *
     * Writing the index once at the end put every converted file through that
     * state for the length of the pass. Instead the whole plan is recorded and
     * flushed first, and only then is anything stripped — so a process death
     * leaves files that are headed, indexed, or both, and never neither. The
     * flush failing aborts the pass rather than stripping without it.
     *
     * [onFileStripped] is a test seam for exactly that: it runs after each file
     * is rewritten, and a test throws from it to stand in for the process dying
     * mid-pass.
     */
    fun toSidecar(
        context: Context,
        folderUri: Uri,
        deviceId: String,
        onFileStripped: () -> Unit = {}
    ): Result {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return Result(0, 0, 1)
        return toSidecarIn(context, folder, deviceId, onFileStripped)
    }

    /**
     * [toSidecar] once the folder has been resolved — the same split
     * [NoteFolderMirror.importChangesFrom] uses, and for the same reason: a test
     * can hand this a `DocumentFile.fromFile` tree, where `fromTreeUri` needs a
     * grant a person has to tap.
     */
    internal fun toSidecarIn(
        context: Context,
        folder: DocumentFile,
        deviceId: String,
        onFileStripped: () -> Unit = {}
    ): Result {
        if (!folder.canWrite()) return Result(0, 0, 1)

        var skipped = 0
        var errors = 0
        val entries = SidecarStore.ownEntries(context, folder, deviceId)

        // Phase 1 — plan. Read every convertible file and record what its header
        // said. Nothing is written to a note file here.
        val planned = mutableListOf<PlannedStrip>()
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
            entries[noteId] = SidecarEntry(
                noteId = noteId,
                fileName = name,
                contentHash = SidecarIndex.hashOf(parsed.body),
                createdAtMillis = parsed.createdAt?.toEpochMilli() ?: 0L,
                pinned = parsed.pinned ?: false,
                archived = parsed.archived ?: false
            )
            planned += PlannedStrip(file, noteId)
        }

        if (planned.isEmpty()) return Result(0, skipped, errors)

        // The barrier. Stripping without a durable index is the duplicate bug,
        // so a failed flush ends the pass with the folder untouched.
        if (!SidecarStore.write(context, folder, deviceId, entries.values)) {
            return Result(0, skipped, errors + 1)
        }

        // Phase 2 — apply. Every file below already has its entry on disk.
        var converted = 0
        var hashDrifted = false
        for (target in planned) {
            val raw = read(context, target.file)
            if (raw == null) {
                errors++
                continue
            }
            val parsed = SyncFrontmatter.decode(raw)
            if (!parsed.hasFrontmatter) {
                // Already stripped — an earlier pass that died after this file.
                // Its entry is on disk, so there is nothing left to do for it.
                converted++
                continue
            }
            if (!write(context, target.file.uri, parsed.body)) {
                errors++
                continue
            }
            // The file changed between the phases (another app, a sync client).
            // Left alone, the recorded hash would say "not what we wrote" on the
            // next pass and earn the file a conflict copy it has not earned.
            val hash = SidecarIndex.hashOf(parsed.body)
            if (entries[target.noteId]?.contentHash != hash) {
                entries[target.noteId]?.let { entries[target.noteId] = it.copy(contentHash = hash) }
                hashDrifted = true
            }
            converted++
            onFileStripped()
        }

        if (hashDrifted) SidecarStore.write(context, folder, deviceId, entries.values)
        return Result(converted, skipped, errors)
    }

    /** A file whose entry is recorded and whose header is still to be removed. */
    private class PlannedStrip(val file: DocumentFile, val noteId: String)

    /**
     * Put the frontmatter back on every file the index knows about, then drop
     * this device's index.
     *
     * [notes] supplies the values the header carries; a file whose note is gone
     * is left alone rather than guessed at. Only *our* index is removed — the
     * others belong to other devices, and deleting a file this device did not
     * write is the rule that keeps a mid-flight sync client from losing data.
     *
     * This direction needs no two-phase dance, and the setting is flipped after
     * it rather than before (see the call site). Its half-done state is files
     * that still have no header — identifiable only through the sidecar index,
     * which only the sidecar mode reads. So the mode has to stay on sidecar
     * until every header is back, which is the mirror image of [toSidecar].
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
