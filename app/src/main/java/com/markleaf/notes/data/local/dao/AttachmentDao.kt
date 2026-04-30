package com.markleaf.notes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.markleaf.notes.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun observeAttachmentsByNote(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE noteId = :noteId")
    suspend fun getAttachmentsForNote(noteId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments")
    suspend fun getAllAttachments(): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: String)
}
