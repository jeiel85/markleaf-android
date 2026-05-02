package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.markleaf.notes.data.local.entity.TagEntity
import com.markleaf.notes.data.local.entity.NoteTagCrossRef
import kotlinx.coroutines.flow.Flow

data class TagWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val noteCount: Int
)

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("""
        SELECT tags.id, tags.name, tags.createdAt, COUNT(notes.id) AS noteCount
        FROM tags
        LEFT JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId
        LEFT JOIN notes ON notes.id = note_tag_cross_ref.noteId AND notes.trashed = 0
        GROUP BY tags.id, tags.name, tags.createdAt
        ORDER BY tags.name ASC
    """)
    fun observeTagsWithCounts(): Flow<List<TagWithCount>>

    @Query("SELECT * FROM tags")
    suspend fun getAllTagsList(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT COUNT(*) FROM note_tag_cross_ref WHERE tagId = :tagId")
    suspend fun getTagUseCount(tagId: Long): Int

    @Transaction
    @Query("""
        SELECT tags.* FROM tags 
        INNER JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId 
        WHERE note_tag_cross_ref.noteId = :noteId
    """)
    fun getTagsForNote(noteId: String): Flow<List<TagEntity>>

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: String)
}
