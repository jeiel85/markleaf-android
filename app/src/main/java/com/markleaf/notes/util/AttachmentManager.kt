package com.markleaf.notes.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.AttachmentEntity
import java.io.File
import java.util.UUID

/**
 * Copies a SAF-picked file into Markleaf's private files dir under a stable
 * `attachments/<noteId>/<id>.<ext>` layout. Returns the relative path the
 * markdown body should reference, e.g.
 *   `attachments/abc-123/xyz.png`
 *
 * No INTERNET permission, no media permission — SAF authorizes the read,
 * we copy bytes immediately and the source URI is never persisted.
 */
object AttachmentManager {

    private const val BASE_DIR = "attachments"

    data class Result(val relativePath: String, val attachmentId: String)

    suspend fun copyIntoStorage(
        context: Context,
        noteId: String,
        sourceUri: Uri
    ): Result? {
        val resolver = context.contentResolver
        val mime = resolver.getType(sourceUri) ?: "application/octet-stream"
        val ext = mime.substringAfter('/').takeIf { it.isNotEmpty() && it.length <= 6 }
            ?: "bin"

        val attachmentsRoot = File(context.filesDir, BASE_DIR).apply { mkdirs() }
        val noteDir = File(attachmentsRoot, noteId).apply { mkdirs() }

        val attachmentId = UUID.randomUUID().toString()
        val target = File(noteDir, "$attachmentId.$ext")

        val ok = runCatching {
            resolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            } != null
        }.getOrDefault(false)
        if (!ok) return null

        // Best-effort metadata insert. If DB write fails (rare), the file is
        // still on disk and renderable — we accept the orphan over a missing
        // image.
        runCatching {
            AppDatabase.getInstance(context).attachmentDao().insert(
                AttachmentEntity(
                    id = attachmentId,
                    noteId = noteId,
                    fileName = target.name,
                    mimeType = mime,
                    addedAt = System.currentTimeMillis()
                )
            )
        }

        val relative = "$BASE_DIR/$noteId/${target.name}"
        return Result(relativePath = relative, attachmentId = attachmentId)
    }

    /** Resolve a relative attachment path against [Context.getFilesDir]. */
    fun resolveFile(context: Context, relativePath: String): File? {
        if (relativePath.isBlank()) return null
        val sanitized = relativePath.removePrefix("./")
        val file = File(context.filesDir, sanitized)
        return file.takeIf { it.exists() && it.isFile }
    }

    /**
     * Convert a relative path into a content:// Uri the FileProvider can serve.
     * (Useful for sharing or for renderers that prefer Uri input.) Falls back
     * to file:// if the FileProvider authority is misconfigured.
     */
    fun resolveUri(context: Context, relativePath: String): Uri? {
        val file = resolveFile(context, relativePath) ?: return null
        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrElse { Uri.fromFile(file) }
    }

    /**
     * Every attachment file currently on disk for [noteId]. Used by the
     * folder-mirror flow to copy them alongside the note's `.md`.
     */
    fun filesForNote(context: Context, noteId: String): List<File> {
        val dir = File(File(context.filesDir, BASE_DIR), noteId)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()?.filter { it.isFile }.orEmpty()
    }

    /**
     * Remove the on-disk attachment directory for [noteId]. Called from the
     * permanent-delete flow so removing a note frees its image bytes — the
     * Room CASCADE drops the metadata row but the underlying files would
     * otherwise linger forever in app-private storage.
     */
    fun deleteAllForNote(context: Context, noteId: String): Boolean {
        val dir = File(File(context.filesDir, BASE_DIR), noteId)
        if (!dir.exists()) return false
        return dir.deleteRecursively()
    }
}
