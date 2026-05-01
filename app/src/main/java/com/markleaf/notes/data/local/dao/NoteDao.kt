package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.markleaf.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE trashed = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countAllNotes(): Int

    @Query("SELECT * FROM notes WHERE title = :title AND trashed = 0 LIMIT 1")
    suspend fun getNoteByTitle(title: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET trashed = 1, deletedAt = :timestamp WHERE id = :noteId")
    suspend fun moveToTrash(noteId: String, timestamp: Long)

    @Query("UPDATE notes SET trashed = 0, deletedAt = NULL WHERE id = :noteId")
    suspend fun restoreFromTrash(noteId: String)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteForever(noteId: String)

    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY deletedAt DESC")
    fun observeTrashedNotes(): Flow<List<NoteEntity>>

    @Query("""
        SELECT notes.* FROM notes 
        JOIN notes_fts ON notes.title = notes_fts.title 
        WHERE notes.trashed = 0 AND notes_fts MATCH :query 
        ORDER BY notes.pinned DESC, notes.updatedAt DESC
    """)
    fun searchNotesFts(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND (title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' OR excerpt LIKE '%' || :query || '%') ORDER BY pinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT notes.* FROM notes
        JOIN note_links ON notes.id = note_links.sourceNoteId
        WHERE note_links.targetNoteId = :targetNoteId AND notes.trashed = 0
    """)
    fun getBacklinkingNotes(targetNoteId: String): Flow<List<NoteEntity>>
}
