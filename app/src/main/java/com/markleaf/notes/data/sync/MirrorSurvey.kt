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
     *
     * [maxDepth] must be the same value the following import will be given.
     * This screen's whole job is to state a number the user then agrees to, so
     * surveying deeper than the import walks would promise arrivals that never
     * come, and surveying shallower would spring the rest on them after they
     * said yes. Both directions are resolved through
     * [MirrorTraversal.effectiveDepth], so the two passes cannot disagree even
     * in sidecar mode, where the requested depth is refused.
     */
    internal fun surveyFolder(
        context: Context,
        folder: DocumentFile,
        existing: List<Note>,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter,
        maxDepth: Int = 0
    ): NoteFolderMirror.FolderSurvey {
        if (!folder.canRead()) return NoteFolderMirror.FolderSurvey(0, 0, readable = false)

        val files = MirrorTraversal
            .walk(folder, MirrorTraversal.effectiveDepth(metadata, maxDepth))
            .files
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

        // The import follows a file renamed outside Markleaf back to its note
        // by content (#429), so the count has to as well, or a folder that was
        // only tidied up would be announced as that many new notes.
        val renamed = when (metadata) {
            is MirrorMetadata.Sidecar -> RenamedFileMatcher(
                SidecarStore.load(context, folder, metadata.deviceId).values,
                files.map { it.name }
            )
            else -> null
        }

        var newFiles = 0
        var knownFiles = 0
        for (ref in files) {
            val file = ref.file
            val idFromIndex = byFileName?.get(ref.name)?.noteId
                ?: renamed?.takeUnless { it.isEmpty }?.let { matcher ->
                    // Read in full only here — a file the index does not name
                    // while some entry's file is missing — which is rare.
                    val raw = runCatching {
                        context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                            ?.toString(Charsets.UTF_8)
                    }.getOrNull() ?: return@let null
                    val parsed = SyncFrontmatter.decode(raw)
                    matcher.claim(ref.name, SidecarIndex.hashOf(parsed.body), parsed.markleafId)?.noteId
                }
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
