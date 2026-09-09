package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.SyncFileExtension
import kotlinx.coroutines.flow.first

/**
 * Linking a folder to folder sync, in both directions.
 *
 * Two screens offer the folder picker — Settings and the Sync Center — and both
 * used to spell the link out for themselves: take the grant, write every note
 * out, report how many were seeded. Neither read the folder, so a user pointing
 * sync at a folder that *already held* their notes saw a message about writing
 * notes out and a list that did not contain their files, and had no way to know
 * that **Sync now** was one tap away (#370, #372). The duplication is why: the
 * import was added to the three passes that run later and to neither copy of
 * the link.
 *
 * So the link lives here once, and does both halves.
 */
object SyncFolderLink {

    /** What linking did, in both directions, for the screen to report. */
    data class LinkResult(
        /** Notes written out into the folder. */
        val seeded: Int,
        /** What reading the folder brought back. */
        val imported: NoteFolderMirror.ImportResult
    )

    /**
     * Point folder sync at [folderUri]: record it, write every live note out,
     * then read the folder back in.
     *
     * Export first, import second, deliberately. The export stamps each note's
     * file with its id, so the import that follows recognises those files as
     * notes it already has and skips them; the other order would read the
     * folder before our own notes were in it, which changes nothing for a
     * folder of somebody else's files but costs a pass on every seeded one.
     */
    suspend fun link(
        context: Context,
        folderUri: Uri,
        settingsRepository: AppSettingsRepository,
        noteRepository: LocalNoteRepository,
        noteImporter: NoteImporter,
        extension: SyncFileExtension,
        metadata: MirrorMetadata,
        titleSource: NoteTitleSource
    ): LinkResult {
        settingsRepository.setSyncFolderUri(folderUri.toString())

        val live = noteRepository.observeNotes().first().filter { !it.trashed }
        var seeded = 0
        live.forEach { note ->
            // writeNoteAndStamp, not writeNote: a seeded note whose
            // lastImportedAt stays null reads as "edited locally since the last
            // import" for ever, so the next genuinely newer file becomes a
            // conflict copy instead of a clean overwrite (#217).
            val wrote = NoteFolderMirror.writeNoteAndStamp(
                context,
                folderUri,
                note,
                extension,
                metadata
            ) { stamped -> noteRepository.updateNote(stamped) }
            if (wrote) seeded++
        }

        // Full set (incl. trashed/archived) so a hidden note isn't re-imported
        // as new — see #148.
        val all = noteRepository.getAllNotes()
        val imported = NoteFolderMirror.importChanges(
            context = context,
            folderUri = folderUri,
            existing = all,
            applyUpdate = { updated -> noteImporter.update(updated) },
            applyCreate = { created -> noteImporter.create(created) },
            metadata = metadata,
            titleSource = titleSource
        )

        settingsRepository.setSyncLastSyncedAt(System.currentTimeMillis())
        return LinkResult(seeded = seeded, imported = imported)
    }
}
