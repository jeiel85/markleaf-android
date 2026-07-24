package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads and writes the sidecar index files in a mirror folder (#216).
 *
 * [SidecarIndex] is the pure format; this is the part that touches storage, and
 * storage here is SAF — where listing a folder is the expensive call and every
 * note save goes through this class.
 *
 * ## What is cached, and what deliberately is not
 *
 * **This device's own index is held in memory** and treated as the authority
 * once loaded. Only this device writes that file, so re-reading it before each
 * save told us nothing we did not already know — and it cost a folder listing
 * plus a full parse every time. Holding it also removes a lost-update window:
 * two writes that each read the same on-disk copy, added an entry and wrote
 * back would drop one of the two entries.
 *
 * **Other devices' indexes are never cached.** They change outside this
 * process, and a stale copy would attach a note to the wrong file. Every read
 * of the merged view goes to disk (#262).
 */
internal object SidecarStore {

    /** This device's entries plus the bytes last written, per folder+device. */
    private class OwnIndex(
        val entries: MutableMap<String, SidecarEntry>,
        var lastWritten: String?
    )

    private val own = ConcurrentHashMap<String, OwnIndex>()

    private fun keyOf(folder: DocumentFile, deviceId: String) = "${folder.uri}|$deviceId"

    /** Every index in [folder], ours and other devices'. Unreadable ones are skipped. */
    fun readAll(context: Context, folder: DocumentFile): List<ParsedIndex> =
        folder.listFiles()
            .filter { it.isFile && SidecarIndex.isIndexFile(it.name) }
            .mapNotNull { file ->
                runCatching {
                    context.contentResolver.openInputStream(file.uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                }.getOrNull()?.let(SidecarIndex::decode)
            }

    /**
     * The merged view of every index, with [deviceId]'s entries winning.
     *
     * Always hits the disk, because the point of it is the *other* devices'
     * entries. Callers that only need our own should use [ownEntries], which is
     * cached — see [NoteFolderMirror] for the save path that was reading both.
     */
    fun load(context: Context, folder: DocumentFile, deviceId: String): Map<String, SidecarEntry> {
        val indexes = readAll(context, folder)
        // The disk pass just told us what our own file holds; adopt it if we
        // have not loaded it yet, so a later ownEntries call costs nothing.
        val ours = own.computeIfAbsent(keyOf(folder, deviceId)) {
            val onDisk = indexes.firstOrNull { it.deviceId == deviceId }
            OwnIndex(
                onDisk?.entries?.associateByTo(mutableMapOf()) { it.noteId } ?: mutableMapOf(),
                null
            )
        }.entries
        // Others from disk, our own from memory. The cached copy is the
        // authority — it may already hold entries this pass added that have not
        // reached the file yet, and merging the file's version over them would
        // hand the caller a view that disagrees with what the next write emits.
        return SidecarIndex.merge(deviceId, indexes.filterNot { it.deviceId == deviceId }) + ours
    }

    /**
     * This device's own entries — the ones it may rewrite.
     *
     * The returned map is the live cached copy: callers mutate it and then call
     * [write], which is how a save records its entry.
     */
    fun ownEntries(
        context: Context,
        folder: DocumentFile,
        deviceId: String
    ): MutableMap<String, SidecarEntry> =
        own.computeIfAbsent(keyOf(folder, deviceId)) {
            val ours = readAll(context, folder).firstOrNull { it.deviceId == deviceId }
            OwnIndex(ours?.entries?.associateByTo(mutableMapOf()) { it.noteId } ?: mutableMapOf(), null)
        }.entries

    /**
     * Write [entries] as [deviceId]'s index. Best-effort: a failure here leaves
     * the notes themselves intact and costs only the mapping, which the next
     * pass rebuilds by filename.
     *
     * Skips the write when the bytes are unchanged. [SidecarIndex.encode] sorts,
     * so a pass that changed nothing produces an identical file and a folder
     * that syncs does not see it touched.
     */
    fun write(
        context: Context,
        folder: DocumentFile,
        deviceId: String,
        entries: Collection<SidecarEntry>
    ): Boolean {
        val encoded = SidecarIndex.encode(deviceId, entries)
        val cached = own[keyOf(folder, deviceId)]
        if (cached?.lastWritten == encoded) return true

        val name = SidecarIndex.fileNameFor(deviceId)
        return runCatching {
            val target = folder.findFile(name)
                ?: folder.createFile("application/json", name)
                ?: return@runCatching false
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { it.write(encoded) }
            } ?: return@runCatching false
            cached?.lastWritten = encoded
            true
        }.getOrDefault(false)
    }

    /**
     * Drop everything remembered about every folder.
     *
     * Called when the folder stops being a sidecar folder — switching the mode
     * off deletes our index, and a cached copy would otherwise be written back
     * on the next save as if nothing had happened.
     */
    fun forget() = own.clear()
}
