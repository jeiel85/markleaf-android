package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.domain.model.Note

/**
 * What a folder holds *before* it is linked, counted without changing anything.
 *
 * Linking a folder used to be an export-only step: notes were written out and
 * the files already sitting there were left for a later sync pass to discover
 * (#372). Reading them at link time is the fix, but "read them" is also the
 * moment a folder of 400 files becomes 400 notes — and, in the default
 * metadata mode, 400 files with a `---` header written into them. That is too
 * large a change to make on the user's behalf without saying so first, and the
 * metadata mode is chosen on the same screen, so link time is the last moment
 * it can still be changed without rewriting every file twice.
 *
 * This is the count that lets the screen ask. It only ever reads: no note is
 * created, no file is written, and nothing here decides what the import does —
 * [MirrorImport] holds those rules, and this mirrors its matching so the number
 * shown is the number that will actually arrive.
 */
internal object MirrorSurvey {

    /**
     * Mirror files in [folder], split by whether an import would take them in
     * as new notes.
     *
     * [existing] must be the *complete* note set, for the same reason
     * [MirrorImport.importChangesFrom] requires it: a file matching an archived
     * or trashed note is not a new note, and counting it as one would promise
     * the user arrivals that never come.
     */
    internal fun surveyFolder(
        context: Context,
        folder: DocumentFile,
        existing: List<Note>,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter
    ): NoteFolderMirror.FolderSurvey {
        if (!folder.canRead()) return NoteFolderMirror.FolderSurvey(0, 0, readable = false)

        val files = folder.listFiles().filter { MirrorFileLookup.isMirrorEntry(it) }
        if (files.isEmpty()) return NoteFolderMirror.FolderSurvey(0, 0)

        val knownIds = existing.mapTo(HashSet(existing.size)) { it.id }
        // Sidecar mode keeps the id → file mapping in a hidden index instead of
        // the file's head, so the same question ("does a note already own this
        // file?") is asked of the index. A file the index has never heard of is
        // new whatever its head says.
        val byFileName = when (metadata) {
            is MirrorMetadata.Sidecar ->
                SidecarIndex.byFileName(SidecarStore.load(context, folder, metadata.deviceId))
            else -> null
        }

        var newFiles = 0
        var knownFiles = 0
        for (file in files) {
            val idFromIndex = byFileName?.get(file.name.orEmpty())?.noteId
            // Even in sidecar mode a file may carry a header — written before
            // the mode was switched, or arriving from a device still writing
            // one — and the import prefers that id when the index has nothing.
            // Peeking second keeps the cheap lookup first.
            val id = idFromIndex ?: MirrorFileLookup.peekMarkleafId(context, file.uri)
            if (id != null && id in knownIds) knownFiles++ else newFiles++
        }
        return NoteFolderMirror.FolderSurvey(newFiles = newFiles, knownFiles = knownFiles)
    }
}
