package com.markleaf.notes.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.domain.model.Note
import java.io.BufferedWriter
import java.io.OutputStreamWriter

object ExportAllNotes {
    fun exportAllNotes(
        context: Context,
        folderUri: Uri,
        notes: List<Note>
    ): Int {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return 0

        var exportedCount = 0

        for (note in notes) {
            try {
                val slug = SlugGenerator.generateSlug(note.title)
                val fileName = if (slug.isNotEmpty()) "$slug.md" else "untitled-${note.id}.md"

                val file = folder.createFile("text/markdown", fileName)
                    ?: continue

                context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(note.contentMarkdown)
                    }
                }
                exportedCount++
            } catch (e: Exception) {
                // Skip this note and continue with others
                continue
            }
        }

        return exportedCount
    }
}
