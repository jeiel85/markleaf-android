package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.markleaf.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 0 ORDER BY pinned DESC, sortOrder ASC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countAllNotes(): Int

    @Query("SELECT * FROM notes WHERE title = :title AND trashed = 0 AND archived = 0 LIMIT 1")
    suspend fun getNoteByTitle(title: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET trashed = 1, deletedAt = :timestamp WHERE id = :noteId")
    suspend fun moveToTrash(noteId: String, timestamp: Long)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :noteId")
    suspend fun setPinned(noteId: String, pinned: Boolean)

    @Query("UPDATE notes SET archived = :archived WHERE id = :noteId")
    suspend fun setArchived(noteId: String, archived: Boolean)

    @Query("UPDATE notes SET trashed = 0, deletedAt = NULL WHERE id = :noteId")
    suspend fun restoreFromTrash(noteId: String)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteForever(noteId: String)

    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY deletedAt DESC")
    fun observeTrashedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        WHERE notes.trashed = 0 AND notes.archived = 0 AND notes_fts MATCH :query
        ORDER BY notes.pinned DESC, notes.sortOrder ASC, notes.updatedAt DESC
        LIMIT 200
    """)
    fun searchNotesFts(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 0 AND (title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' OR excerpt LIKE '%' || :query || '%') ORDER BY pinned DESC, sortOrder ASC, updatedAt DESC LIMIT 200")
    fun searchNotesLike(query: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET sortOrder = :sortOrder WHERE id = :noteId")
    suspend fun updateSortOrder(noteId: String, sortOrder: Int)

    @Query("SELECT MAX(sortOrder) FROM notes WHERE trashed = 0 AND archived = 0")
    suspend fun getMaxSortOrder(): Int?
}
