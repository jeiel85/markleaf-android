package com.markleaf.notes.navigation

object NavRoutes {
    const val NOTES = "notes"
    const val EDITOR = "editor/{noteId}"
    const val EDITOR_NEW = "editor"

    /**
     * The editor opened for adding to the end of a note: edit mode, caret at
     * the end, keyboard up, whatever "Open notes at" and "Open notes in
     * preview" say (#481). A route of its own rather than an argument on
     * [EDITOR], whose route string the card transition compares.
     */
    const val EDITOR_APPEND = "editor/{noteId}/append"
    const val TAGS = "tags"
    const val SEARCH = "search"
    const val TRASH = "trash"
    const val ARCHIVE = "archive"
    const val LOCKED = "locked"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val SYNC_CENTER = "sync_center"

    /**
     * The Sync Center scrolled to its Conflict Center list — where the note
     * list's conflict-copy banner leads (#434). A route of its own for the same
     * reason as [EDITOR_APPEND]: one destination, entered at a different place.
     */
    const val SYNC_CENTER_CONFLICTS = "sync_center/conflicts"
    const val VIEWER = "viewer/{uri}"

    /**
     * Read-only view of a file outside the app (#326). The whole content URI is
     * one path segment, so it is percent-encoded here — `/` included — and
     * Navigation decodes it once when it parses the argument back out.
     */
    fun viewerRoute(uri: String): String = "viewer/${android.net.Uri.encode(uri)}"

    fun editorAppendRoute(noteId: String): String = "editor/$noteId/append"

    fun editorRoute(noteId: String? = null): String {
        return if (noteId != null) {
            "editor/$noteId"
        } else {
            EDITOR_NEW
        }
    }
}
