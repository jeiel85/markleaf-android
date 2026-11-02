package com.markleaf.notes.util

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.withContext

/**
 * Deletes notes that have sat in the trash longer than [AppSettings.trashRetention] (#566).
 *
 * Runs when the app comes to the foreground (MainActivity). Each note goes the
 * way the Trash screen's "Delete forever" sends it: the Room row, the
 * attachment bytes, and the file in the sync folder, one after another.
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

    /** Deletes what [expired] returns for the current trash; returns how many notes went. */
    suspend fun purgeExpired(
        context: Context,
        repository: NoteRepository,
        settings: AppSettings,
        now: Instant = Instant.now()
    ): Int {
        if (settings.trashRetention.days == null) return 0
        val due = expired(repository.observeTrashedNotes().first(), settings.trashRetention, now)
        if (due.isEmpty()) return 0
        val folderUri = settings.syncFolderUriOrNull()
        due.forEach { note ->
            repository.deleteForever(note.id)
            // The row is gone either way; leftover bytes or a stale folder file
            // must not stop the next note, nor crash the app on its way to the
            // foreground. The folder file is retried by nothing, as with the
            // Trash screen's own delete.
            try {
                withContext(Dispatchers.IO) {
                    AttachmentManager.deleteAllForNote(context, note.id)
                    if (folderUri != null) {
                        NoteFolderMirror.deleteNote(context, folderUri, note.id, settings.mirrorMetadata())
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (BuildConfig.DEBUG) Log.d("TrashPurge", "cleanup failed for ${note.id}", error)
            }
        }
        WidgetRefresh.notesChanged(context)
        return due.size
    }
}
