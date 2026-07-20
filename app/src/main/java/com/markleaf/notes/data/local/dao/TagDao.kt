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

    /**
     * Tags that at least one visible note still carries. Locked and trashed notes are
     * excluded, so a tag used only by locked notes never reaches the search chips or
     * the editor's `#` autocomplete (#169) — the same rule [observeTagsWithCounts]
     * already applies to the tag overview (#156).
     *
     * Orphan rows are excluded by the same predicate: a tag whose notes have all been
     * retagged away keeps its row in `tags`, and matching the overview's
     * `HAVING COUNT(notes.id) > 0` means autocomplete stops suggesting names that
     * lead nowhere.
     */
    @Query("""
        SELECT * FROM tags
        WHERE EXISTS (
            SELECT 1 FROM note_tag_cross_ref
            INNER JOIN notes ON notes.id = note_tag_cross_ref.noteId
            WHERE note_tag_cross_ref.tagId = tags.id
              AND notes.trashed = 0
              AND notes.locked = 0
        )
        ORDER BY name ASC
    """)
    fun observeVisibleTags(): Flow<List<TagEntity>>

    /**
     * Locked notes are excluded from the count so a tag used only by locked notes
     * disappears from the tag list entirely. Without the filter such a tag showed a
     * non-zero count that drilled down to an empty list, since `searchNotesByTag`
     * already filters locked notes (#156).
     */
    @Query("""
        SELECT tags.id, tags.name, tags.createdAt, COUNT(notes.id) AS noteCount
        FROM tags
        LEFT JOIN note_tag_cross_ref ON tags.id = note_tag_cross_ref.tagId
        LEFT JOIN notes ON notes.id = note_tag_cross_ref.noteId AND notes.trashed = 0 AND notes.locked = 0
        GROUP BY tags.id, tags.name, tags.createdAt
        HAVING COUNT(notes.id) > 0
        ORDER BY tags.name ASC
    """)
    fun observeTagsWithCounts(): Flow<List<TagWithCount>>

    /**
     * Unfiltered — includes tags carried only by locked or trashed notes. Used to
     * assert seeded fixtures in tests; do not surface this to the UI without adding
     * the [observeVisibleTags] predicate.
     */
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
