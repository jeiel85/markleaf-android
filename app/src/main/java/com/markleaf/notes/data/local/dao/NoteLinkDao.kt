package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import kotlinx.coroutines.flow.Flow

data class NoteLinkSearchResult(
    val rawLabel: String,
    val targetNoteId: String?,
    val sourceCount: Int
)

@Dao
interface NoteLinkDao {
    @Query("SELECT * FROM note_links WHERE sourceNoteId = :sourceNoteId")
    fun getLinksFromNote(sourceNoteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE targetNoteId = :targetNoteId")
    fun getBacklinksToNote(targetNoteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE targetNoteId = :targetNoteId")
    suspend fun getBacklinksToNoteList(targetNoteId: String): List<NoteLinkEntity>

    @Query("""
        SELECT rawLabel, targetNoteId, COUNT(*) AS sourceCount
        FROM note_links
        GROUP BY rawLabel, targetNoteId
        ORDER BY rawLabel ASC
    """)
    fun observeQuickOpenLinks(): Flow<List<NoteLinkSearchResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLinkEntity)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteLinksFromNote(sourceNoteId: String)
}
