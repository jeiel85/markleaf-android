package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {
    @Query("SELECT * FROM note_links WHERE sourceNoteId = :sourceNoteId")
    fun getLinksFromNote(sourceNoteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE targetNoteId = :targetNoteId")
    fun getBacklinksToNote(targetNoteId: String): Flow<List<NoteLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLinkEntity)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteLinksFromNote(sourceNoteId: String)
}
