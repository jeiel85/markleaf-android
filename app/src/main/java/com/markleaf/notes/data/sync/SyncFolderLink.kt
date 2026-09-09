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
     * Point folder sync at [folderUri]: record it, read the folder in, then
     * write every live note out.
     *
     * **Import first, and the order is the whole safety of this.** Seeding
     * writes a note into the file that carries its id — and, when no file does,
     * into an unclaimed file that merely bears its title ([MirrorFileLookup]
     * adopts one, which is what keeps a lost id from forking a new file on
     * every save). Both of those files may be the user's, holding text we have
     * never read: a note edited on another device since this one last saw it,
     * or a hand-dropped file that happens to be called `Groceries.md`. Seeding
     * first overwrites them and the import that followed would read back only
     * what we just wrote, so the edit is gone with nothing to show it ever
     * existed. Reading first cannot lose anything: a newer file updates its
     * note, an unowned one becomes a note of its own, and the seed that follows
     * writes notes whose content is by then the file's.
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

        // Read after the import, so a note the folder just updated is written
        // back as the merged version rather than the one we started with.
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

        settingsRepository.setSyncLastSyncedAt(System.currentTimeMillis())
        return LinkResult(seeded = seeded, imported = imported)
    }
}
