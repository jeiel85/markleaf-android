package com.markleaf.notes.data.sync

import android.content.Context
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where a filename-based local Markdown link (#414) leads. */
sealed interface LocalNoteLinkResult {
    data class Open(val noteId: String) : LocalNoteLinkResult
    data object Locked : LocalNoteLinkResult
    data object NotFound : LocalNoteLinkResult
}

/**
 * Resolves a mirrored file's name to the note it represents, through the same
 * [NoteFolderMirror.noteIdForFileName] lookup the editor's own local-link
 * handling uses — frontmatter id or sidecar entry, whichever [MirrorMetadata]
 * folder sync is in. Shared so the read-only file viewer (#326) can open a
 * local link the same way the editor preview does, without a second copy of
 * the locked/trashed/archived handling to drift out of sync (#414).
 */
suspend fun resolveLocalNoteLink(
    context: Context,
    db: AppDatabase,
    settings: AppSettings,
    fileName: String
): LocalNoteLinkResult {
    val folder = settings.syncFolderUriOrNull() ?: return LocalNoteLinkResult.NotFound
    val linkedId = withContext(Dispatchers.IO) {
        NoteFolderMirror.noteIdForFileName(context, folder, fileName, settings.mirrorMetadata())
    } ?: return LocalNoteLinkResult.NotFound
    return localNoteLinkVerdict(db.noteDao().getNoteById(linkedId))
}

/**
 * The locked/trashed/archived-aware verdict once a filename has already
 * resolved to a note id (or hasn't). Split out from [resolveLocalNoteLink] so
 * this branching — the actual behavioral contract a link tap has to honour —
 * is unit-testable without a Room database or a SAF folder.
 */
internal fun localNoteLinkVerdict(target: NoteEntity?): LocalNoteLinkResult = when {
    target == null || target.trashed || target.archived -> LocalNoteLinkResult.NotFound
    target.locked -> LocalNoteLinkResult.Locked
    else -> LocalNoteLinkResult.Open(target.id)
}
