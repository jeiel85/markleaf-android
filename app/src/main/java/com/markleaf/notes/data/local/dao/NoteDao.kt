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
    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 0 AND locked = 0 ORDER BY pinned DESC, sortOrder ASC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    /**
     * Notes in the passcode-gated "Locked notes" space (#155). Shown only inside
     * the unlocked LockedNotesScreen; every other list/search/export path filters
     * `locked = 0` so these stay hidden. A trashed note is never locked
     * (moveToTrash clears the flag), so the `trashed = 0` guard is belt-and-braces.
     */
    @Query("SELECT * FROM notes WHERE trashed = 0 AND locked = 1 ORDER BY pinned DESC, sortOrder ASC, updatedAt DESC")
    fun observeLockedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countAllNotes(): Int

    @Query("SELECT * FROM notes WHERE title = :title AND trashed = 0 AND archived = 0 AND locked = 0 LIMIT 1")
    suspend fun getNoteByTitle(title: String): NoteEntity?

    /**
     * Every note regardless of state — active, archived, or trashed. The folder
     * reconcile needs the complete id set so a mirror file whose note is only
     * hidden (in Trash or Archive) is recognised as already-existing instead of
     * being re-imported as a brand-new active note (#148).
     */
    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    // Trashing also clears `locked`: a deleted note leaves the Locked space so it
    // shows normally in Trash (no leak of a still-locked title) and can never
    // become an unreachable locked+trashed orphan (#155).
    @Query("UPDATE notes SET trashed = 1, deletedAt = :timestamp, locked = 0 WHERE id = :noteId")
    suspend fun moveToTrash(noteId: String, timestamp: Long)

    @Query("UPDATE notes SET locked = :locked WHERE id = :noteId")
    suspend fun setLocked(noteId: String, locked: Boolean)

    /** Clear the lock on every locked note — used when the passcode is removed so
     *  the notes return to the normal list instead of being stranded (#155). */
    @Query("UPDATE notes SET locked = 0 WHERE locked = 1")
    suspend fun unlockAllLocked()

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

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 1 AND locked = 0 ORDER BY updatedAt DESC")
    fun observeArchivedNotes(): Flow<List<NoteEntity>>

    // A plain JOIN on notes_fts can list the same note once per FTS posting:
    // if the index ever holds duplicate postings for a note's rowid the result
    // multiplies (#140 "search shows the same note multiple times"). Matching
    // by `rowid IN (...)` collapses that to one row per note regardless of how
    // many postings exist.
    @Query("""
        SELECT * FROM notes
        WHERE trashed = 0 AND archived = 0 AND locked = 0
          AND rowid IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH :query)
        ORDER BY pinned DESC, sortOrder ASC, updatedAt DESC
        LIMIT 200
    """)
    fun searchNotesFts(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 0 AND locked = 0 AND (title LIKE '%' || :query || '%' OR contentMarkdown LIKE '%' || :query || '%' OR excerpt LIKE '%' || :query || '%') ORDER BY pinned DESC, sortOrder ASC, updatedAt DESC LIMIT 200")
    fun searchNotesLike(query: String): Flow<List<NoteEntity>>

    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN note_tag_cross_ref ON notes.id = note_tag_cross_ref.noteId
        INNER JOIN tags ON tags.id = note_tag_cross_ref.tagId
        WHERE notes.trashed = 0 AND notes.archived = 0 AND notes.locked = 0 AND tags.name = :tagName
        ORDER BY notes.pinned DESC, notes.sortOrder ASC, notes.updatedAt DESC
        LIMIT 200
    """)
    fun searchNotesByTag(tagName: String): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET sortOrder = :sortOrder WHERE id = :noteId")
    suspend fun updateSortOrder(noteId: String, sortOrder: Int)

    @Query("SELECT MAX(sortOrder) FROM notes WHERE trashed = 0 AND archived = 0 AND locked = 0")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT * FROM notes WHERE trashed = 0 AND locked = 0 AND title LIKE '%(다른 기기 사본%' ORDER BY updatedAt DESC")
    fun observeConflictNotes(): Flow<List<NoteEntity>>
}
