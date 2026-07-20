package com.markleaf.notes.navigation

import com.markleaf.notes.domain.repository.NoteRepository

/**
 * Resolves where an `ACTION_OPEN_NOTE` deep link should land.
 *
 * `MainActivity` is `exported="true"` because it carries the LAUNCHER filter, and
 * the widget's open-note action is a bare note id in an extra. There is no
 * intent-filter for that action, so it can only arrive as an explicit intent —
 * but any installed app can send one with an arbitrary id.
 *
 * That matters because [NoteRepository.getNote] deliberately returns locked notes:
 * the Locked space is a visibility gate over shared rows, not row-level access
 * control, and the editor needs the row to render a note the user just unlocked.
 * Navigating straight to the editor therefore rendered a locked note without ever
 * showing the passcode prompt, and without the `FLAG_SECURE` the Locked screen
 * raises (#158). Every other caller of the editor route sources its id from a
 * list that already filters `locked`, or from the Locked screen itself after the
 * passcode is accepted — this entry point was the only external one.
 *
 * A locked id is sent to [NavRoutes.LOCKED], which gates itself, rather than being
 * dropped: a widget whose `RemoteViews` were built before the note was locked can
 * legitimately still carry that id, and silently ignoring the tap would read as a
 * dead button. The Locked space asks for the passcode and the note is right there
 * once it is accepted.
 *
 * A missing note keeps the previous behaviour and resolves to the editor route, so
 * ids that simply no longer exist are unaffected by this gate.
 */
suspend fun resolveOpenNoteRoute(noteId: String, noteRepository: NoteRepository): String {
    val note = noteRepository.getNote(noteId)
    return if (note?.locked == true) NavRoutes.LOCKED else NavRoutes.editorRoute(noteId)
}
