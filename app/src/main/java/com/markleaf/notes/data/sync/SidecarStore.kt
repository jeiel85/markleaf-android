package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap

/**
 * Reads and writes the sidecar index files in a mirror folder (#216).
 *
 * [SidecarIndex] is the pure format; this is the part that touches storage. It
 * holds this device's index in memory between calls so a note save costs one
 * write rather than a read-modify-write, and skips the write entirely when the
 * encoded bytes have not changed — a folder that syncs should not see its index
 * touched by a save that changed nothing.
 */
internal object SidecarStore {

    /**
     * Last content written per folder, so an unchanged index is not rewritten.
     * Keyed by folder Uri + device, as [MirrorFileCache] is: one process can
     * mirror to only one folder at a time today, but nothing here depends on
     * that and a stale entry across a folder change would be a real bug.
     */
    private val lastWritten = ConcurrentHashMap<String, String>()

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
     * See [SidecarIndex.merge] for why another device's hash cannot stand in
     * for our own.
     */
    fun load(context: Context, folder: DocumentFile, deviceId: String): Map<String, SidecarEntry> =
        SidecarIndex.merge(deviceId, readAll(context, folder))

    /**
     * Write [entries] as [deviceId]'s index. Best-effort: a failure here leaves
     * the notes themselves intact and costs only the mapping, which the next
     * pass rebuilds by filename.
     */
    fun write(
        context: Context,
        folder: DocumentFile,
        deviceId: String,
        entries: Collection<SidecarEntry>
    ): Boolean {
        val encoded = SidecarIndex.encode(deviceId, entries)
        val key = "${folder.uri}|$deviceId"
        if (lastWritten[key] == encoded) return true

        val name = SidecarIndex.fileNameFor(deviceId)
        return runCatching {
            val target = folder.findFile(name)
                ?: folder.createFile("application/json", name)
                ?: return@runCatching false
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { it.write(encoded) }
            } ?: return@runCatching false
            lastWritten[key] = encoded
            true
        }.getOrDefault(false)
    }

    /** Only this device's own entries, which are the ones it may rewrite. */
    fun ownEntries(
        context: Context,
        folder: DocumentFile,
        deviceId: String
    ): MutableMap<String, SidecarEntry> {
        val own = readAll(context, folder).firstOrNull { it.deviceId == deviceId }
        return own?.entries?.associateBy { it.noteId }?.toMutableMap() ?: mutableMapOf()
    }

    /** Drop the cached copy — used by tests and when the mirror folder changes. */
    fun forget() = lastWritten.clear()
}
