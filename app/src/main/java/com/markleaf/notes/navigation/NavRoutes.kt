package com.markleaf.notes.navigation

object NavRoutes {
    const val NOTES = "notes"
    const val EDITOR = "editor/{noteId}"
    const val EDITOR_NEW = "editor"
    const val TAGS = "tags"
    const val SEARCH = "search"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    
    fun editorRoute(noteId: String? = null): String {
        return if (noteId != null) {
            "editor/$noteId"
        } else {
            EDITOR_NEW
        }
    }
}
