package com.markleaf.notes.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.BuildConfig
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.TrashRetention
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.widget.WidgetRefresh
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Deletes notes that have sat in the trash longer than [AppSettings.trashRetention] (#566).
 *
 * Runs when the app comes to the foreground (MainActivity). Each note loses
 * what the Trash screen's "Delete forever" removes — its file in the sync
 * folder, its attachment bytes and its Room row — in that order; see
 * [purgeExpired] for why the row goes last.
 */
internal object TrashPurge {

    /**
     * The trashed notes that [retention] says are due at [now].
     *
     * A note with no `deletedAt` (trashed before the column was filled, or by
     * an import that left it empty) is never due: without a date, its age is
     * unknown, and guessing could delete a note the user trashed a minute ago.
     */
    fun expired(notes: List<Note>, retention: TrashRetention, now: Instant): List<Note> {
        val days = retention.days ?: return emptyList()
        val cutoff = now.minus(Duration.ofDays(days.toLong()))
        return notes.filter { note ->
            val deletedAt = note.deletedAt
            note.trashed && deletedAt != null && !deletedAt.isAfter(cutoff)
        }
    }

    /**
     * Deletes what [expired] returns for the current trash; returns how many notes went.
     *
     * With a sync folder linked, each note's file goes before its row. A row
     * deleted first, with its file left behind, would be read back by the next
     * reconcile as a note it has never seen and created again — active, not in
     * the trash (Codex review of #569). So the sweep waits, keeping every note
     * in the trash, while the folder can't be reached or written (a revoked
     * grant, an unmounted card), and a pass cut short after a file is gone
     * leaves the row trashed for the next pass to finish.
     *
     * [resolveFolder] is the tree lookup, replaceable so a test can hand in a
     * plain directory.
     */
    suspend fun purgeExpired(
        context: Context,
        repository: NoteRepository,
        settings: AppSettings,
        now: Instant = Instant.now(),
        resolveFolder: (Uri) -> DocumentFile? = { DocumentFile.fromTreeUri(context, it) }
    ): Int {
        if (settings.trashRetention.days == null) return 0
        val due = expired(repository.observeTrashedNotes().first(), settings.trashRetention, now)
        if (due.isEmpty()) return 0
        val folder = settings.syncFolderUriOrNull()?.let { uri ->
            withContext(Dispatchers.IO) { resolveFolder(uri)?.takeIf { it.canWrite() } }
                ?: return 0
        }
        var purged = 0
        due.forEach { snapshot ->
            // [due] was read before the folder lookup and the notes ahead of
            // this one, and the Trash screen stays usable meanwhile. Holding
            // [lock] and reading the row again means a note restored since —
            // or being restored now — keeps its file, attachments and row
            // (Codex review of #569).
            val deleted = lock.withLock {
                val note = repository.getNote(snapshot.id)
                    ?.takeIf { expired(listOf(it), settings.trashRetention, now).isNotEmpty() }
                    ?: return@withLock false
                try {
                    withContext(Dispatchers.IO) {
                        if (folder != null) {
                            NoteFolderMirror.deleteNoteIn(context, folder, note.id, settings.mirrorMetadata())
                        }
                        AttachmentManager.deleteAllForNote(context, note.id)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    // The row stays, still in the trash, and the next pass tries again.
                    if (BuildConfig.DEBUG) Log.d("TrashPurge", "cleanup failed for ${note.id}", error)
                    return@withLock false
                }
                repository.deleteForever(note.id)
                true
            }
            if (deleted) purged++
        }
        if (purged > 0) WidgetRefresh.notesChanged(context)
        return purged
    }

    /**
     * Held by the sweep for each note it deletes and by
     * [com.markleaf.notes.ui.viewmodel.TrashViewModel.restoreFromTrash], so a
     * restore lands either before the sweep re-reads the note (which then
     * leaves it alone) or after the note is gone — never between the check and
     * the deletion of its files.
     */
    val lock = Mutex()
}
