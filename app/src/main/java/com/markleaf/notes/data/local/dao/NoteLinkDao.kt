package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<NoteLinkEntity>)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :noteId")
    suspend fun clearForNote(noteId: String)

    /**
     * Notes whose body contains at least one `[[…]]` matching the supplied normalized title.
     *
     * Locked notes are excluded: a backlink row shows the linking note's title, so
     * without this filter a locked note's title would surface in the note-information
     * sheet of any note it links to (#156).
     */
    @Query(
        """
        SELECT DISTINCT notes.* FROM notes
        JOIN note_links ON note_links.sourceNoteId = notes.id
        WHERE note_links.normalizedTitle = :normalizedTitle
          AND notes.trashed = 0
          AND notes.archived = 0
          AND notes.locked = 0
          AND notes.id != :excludeNoteId
        ORDER BY notes.updatedAt DESC
        """
    )
    fun observeBacklinks(normalizedTitle: String, excludeNoteId: String): Flow<List<NoteEntity>>
}
