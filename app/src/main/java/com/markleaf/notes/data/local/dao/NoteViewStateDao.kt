package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markleaf.notes.data.local.entity.NoteViewStateEntity

/**
 * Reads and writes the per-note scroll position behind "Open notes at: where I
 * left off" (#214). One row per note, replaced outright each time.
 */
@Dao
interface NoteViewStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: NoteViewStateEntity)

    @Query("SELECT * FROM note_view_state WHERE noteId = :noteId")
    suspend fun getForNote(noteId: String): NoteViewStateEntity?

    /** Drop every recorded position — called when the "where I left off"
     *  setting is switched off, so turning it back on later starts clean
     *  rather than restoring a stale position for every note (#262). */
    @Query("DELETE FROM note_view_state")
    suspend fun clearAll()
}
