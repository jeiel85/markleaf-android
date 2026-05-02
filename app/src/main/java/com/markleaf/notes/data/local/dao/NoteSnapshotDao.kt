package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markleaf.notes.data.local.entity.NoteSnapshotEntity

@Dao
interface NoteSnapshotDao {
    @Query("SELECT * FROM note_snapshots WHERE noteId = :noteId ORDER BY createdAt DESC")
    suspend fun getSnapshotsForNote(noteId: String): List<NoteSnapshotEntity>

    @Query("SELECT * FROM note_snapshots WHERE id = :snapshotId")
    suspend fun getSnapshotById(snapshotId: String): NoteSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: NoteSnapshotEntity)

    @Query("DELETE FROM note_snapshots WHERE noteId = :noteId AND id NOT IN (SELECT id FROM note_snapshots WHERE noteId = :noteId ORDER BY createdAt DESC LIMIT :keepCount)")
    suspend fun pruneSnapshotsForNote(noteId: String, keepCount: Int)
}
