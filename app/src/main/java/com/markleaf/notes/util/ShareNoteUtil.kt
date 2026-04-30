package com.markleaf.notes.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.markleaf.notes.domain.model.Note
import java.io.File

object ShareNoteUtil {
    fun shareNote(context: Context, note: Note): Boolean {
        return try {
            val slug = SlugGenerator.generateSlug(note.title)
            val fileName = if (slug.isNotEmpty()) "$slug.md" else "note-${note.id}.md"

            val cacheDir = File(context.cacheDir, "shared_notes")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val file = File(cacheDir, fileName)
            file.writeText(note.contentMarkdown)

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_SUBJECT, note.title)
                putExtra(Intent.EXTRA_TEXT, note.contentMarkdown)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, null)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }
}
