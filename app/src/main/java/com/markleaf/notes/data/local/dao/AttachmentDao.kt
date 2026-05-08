package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markleaf.notes.data.local.entity.AttachmentEntity

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY addedAt ASC")
    suspend fun forNote(noteId: String): List<AttachmentEntity>

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    suspend fun deleteById(attachmentId: String)

    @Query("DELETE FROM attachments WHERE noteId = :noteId")
    suspend fun clearForNote(noteId: String)
}
