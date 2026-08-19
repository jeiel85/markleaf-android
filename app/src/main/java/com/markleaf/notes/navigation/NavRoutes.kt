package com.markleaf.notes.navigation

object NavRoutes {
    const val NOTES = "notes"
    const val EDITOR = "editor/{noteId}"
    const val EDITOR_NEW = "editor"
    const val TAGS = "tags"
    const val SEARCH = "search"
    const val TRASH = "trash"
    const val ARCHIVE = "archive"
    const val LOCKED = "locked"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val SYNC_CENTER = "sync_center"
    const val VIEWER = "viewer/{uri}"

    /**
     * Read-only view of a file outside the app (#326). The whole content URI is
     * one path segment, so it is percent-encoded here — `/` included — and
     * Navigation decodes it once when it parses the argument back out.
     */
    fun viewerRoute(uri: String): String = "viewer/${android.net.Uri.encode(uri)}"

    fun editorRoute(noteId: String? = null): String {
        return if (noteId != null) {
            "editor/$noteId"
        } else {
            EDITOR_NEW
        }
    }
}
