package com.markleaf.notes.util

import android.content.Context
import android.net.Uri
import com.markleaf.notes.domain.model.Note

object ExportAllNotes {
    fun exportAllNotes(
        context: Context,
        folderUri: Uri,
        notes: List<Note>
    ): Int {
        // TODO: Implement with Storage Access Framework
        // For now, return 0 as placeholder
        return 0
    }
}
