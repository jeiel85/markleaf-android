package com.markleaf.notes.util

import android.content.Context
import android.net.Uri
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.AttachmentEntity
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import com.markleaf.notes.data.local.entity.NoteTagCrossRef
import com.markleaf.notes.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtil {
    suspend fun createBackup(context: Context, zipUri: Uri): Boolean {
        val db = AppDatabase.getInstance(context)
        val notes = db.noteDao().observeNotes().first()
        val trashedNotes = db.noteDao().observeTrashedNotes().first()
        val allNotes = notes + trashedNotes
        
        val tags = db.tagDao().observeAllTags().first()
        
        val attachments = mutableListOf<AttachmentEntity>()
        val links = mutableListOf<NoteLinkEntity>()
        
        for (note in allNotes) {
            attachments.addAll(db.attachmentDao().getAttachmentsForNote(note.id))
            links.addAll(db.noteLinkDao().getLinksFromNote(note.id).first())
        }
        
        return try {
            context.contentResolver.openOutputStream(zipUri)?.use { os ->
                ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                    val dataJson = JSONObject().apply {
                        put("notes", JSONArray().apply {
                            allNotes.forEach { note ->
                                put(JSONObject().apply {
                                    put("id", note.id)
                                    put("title", note.title)
                                    put("contentMarkdown", note.contentMarkdown)
                                    put("excerpt", note.excerpt)
                                    put("pinned", note.pinned)
                                    put("trashed", note.trashed)
                                    put("createdAt", note.createdAt)
                                    put("updatedAt", note.updatedAt)
                                    put("deletedAt", note.deletedAt ?: 0L)
                                })
                            }
                        })
                        put("tags", JSONArray().apply {
                            tags.forEach { tag ->
                                put(JSONObject().apply {
                                    put("id", tag.id)
                                    put("name", tag.name)
                                })
                            }
                        })
                        put("attachments", JSONArray().apply {
                            attachments.forEach { att ->
                                put(JSONObject().apply {
                                    put("id", att.id)
                                    put("noteId", att.noteId)
                                    put("uri", att.uri)
                                    put("fileName", att.fileName)
                                    put("mimeType", att.mimeType)
                                    put("createdAt", att.createdAt)
                                })
                            }
                        })
                        put("links", JSONArray().apply {
                            links.forEach { link ->
                                put(JSONObject().apply {
                                    put("id", link.id)
                                    put("sourceNoteId", link.sourceNoteId)
                                    put("targetNoteId", link.targetNoteId ?: "")
                                    put("rawLabel", link.rawLabel)
                                    put("createdAt", link.createdAt)
                                })
                            }
                        })
                    }
                    
                    zos.putNextEntry(ZipEntry("data.json"))
                    zos.write(dataJson.toString().toByteArray())
                    zos.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(context: Context, zipUri: Uri): Boolean {
        val db = AppDatabase.getInstance(context)
        return try {
            context.contentResolver.openInputStream(zipUri)?.use { isStream ->
                ZipInputStream(isStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "data.json") {
                            val reader = BufferedReader(InputStreamReader(zis))
                            val content = reader.readText()
                            val dataJson = JSONObject(content)
                            
                            val notesArray = dataJson.optJSONArray("notes")
                            if (notesArray != null) {
                                for (i in 0 until notesArray.length()) {
                                    val obj = notesArray.getJSONObject(i)
                                    val note = NoteEntity(
                                        id = obj.getString("id"),
                                        title = obj.getString("title"),
                                        contentMarkdown = obj.getString("contentMarkdown"),
                                        excerpt = obj.getString("excerpt"),
                                        pinned = obj.getBoolean("pinned"),
                                        trashed = obj.getBoolean("trashed"),
                                        createdAt = obj.getLong("createdAt"),
                                        updatedAt = obj.getLong("updatedAt"),
                                        deletedAt = if (obj.optLong("deletedAt") == 0L) null else obj.getLong("deletedAt")
                                    )
                                    db.noteDao().insertNote(note)
                                }
                            }
                            
                            val tagsArray = dataJson.optJSONArray("tags")
                            if (tagsArray != null) {
                                for (i in 0 until tagsArray.length()) {
                                    val obj = tagsArray.getJSONObject(i)
                                    val tag = TagEntity(
                                        id = obj.getString("id"),
                                        name = obj.getString("name")
                                    )
                                    db.tagDao().insertTag(tag)
                                }
                            }
                            
                            val attArray = dataJson.optJSONArray("attachments")
                            if (attArray != null) {
                                for (i in 0 until attArray.length()) {
                                    val obj = attArray.getJSONObject(i)
                                    val att = AttachmentEntity(
                                        id = obj.getString("id"),
                                        noteId = obj.getString("noteId"),
                                        uri = obj.getString("uri"),
                                        fileName = obj.getString("fileName"),
                                        mimeType = obj.getString("mimeType"),
                                        createdAt = obj.getLong("createdAt")
                                    )
                                    db.attachmentDao().insertAttachment(att)
                                }
                            }
                            
                            val linksArray = dataJson.optJSONArray("links")
                            if (linksArray != null) {
                                for (i in 0 until linksArray.length()) {
                                    val obj = linksArray.getJSONObject(i)
                                    val link = NoteLinkEntity(
                                        id = obj.optLong("id"),
                                        sourceNoteId = obj.getString("sourceNoteId"),
                                        targetNoteId = obj.optString("targetNoteId").takeIf { it.isNotEmpty() },
                                        rawLabel = obj.getString("rawLabel"),
                                        createdAt = obj.getLong("createdAt")
                                    )
                                    db.noteLinkDao().insertLink(link)
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
