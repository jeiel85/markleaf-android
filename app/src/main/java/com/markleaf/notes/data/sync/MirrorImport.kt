package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.R
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.domain.model.Note
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.Instant
import java.util.UUID

/**
 * The folder → DB direction of the mirror: walking the folder, parsing each
 * mirror file, and reconciling what it finds against the database. Only ever
 * *reads* the folder into notes — it never deletes one, and it updates a note
 * only when the file is provably newer ([MirrorReconcile] holds the rules).
 */
internal object MirrorImport {

    /**
     * [MirrorImport.importChanges] once the folder has been resolved — see
     * [MirrorWrite.writeNoteInto] for why the resolution is a separate step.
     *
     * [existing] must be the *complete* note set — active, archived and
     * trashed. A file matching a hidden note is otherwise mistaken for a new
     * note and re-imported, which un-archives it or resurrects it from Trash
     * (#148). Files matching a trashed note are skipped outright.
     *
     * Conflict rule: *file wins iff its timestamp is strictly newer than the DB
     * record* (with a 2-second slack for filesystem clocks). Otherwise no change
     * is applied. Files without a `markleaf_id` become new notes (typical when
     * the user dropped a note into the folder by hand).
     *
     * `applyUpdate` is invoked synchronously — caller is responsible for
     * shipping the resulting writes onto IO dispatcher and into Room.
     */
    internal suspend fun importChangesFrom(
        context: Context,
        folder: DocumentFile,
        existing: List<Note>,
        applyUpdate: suspend (Note) -> Unit,
        applyCreate: suspend (Note) -> Unit,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter,
        titleSource: NoteTitleSource = NoteTitleSource.FIRST_HEADING
    ): NoteFolderMirror.ImportResult {
        if (!folder.canRead()) return NoteFolderMirror.ImportResult(0, 0, 0, 1)
        if (metadata is MirrorMetadata.Sidecar) {
            return importChangesSidecar(
                context, folder, existing, applyUpdate, applyCreate, metadata.deviceId, titleSource
            )
        }

        var updated = 0
        var created = 0
        var skipped = 0
        var errors = 0
        var conflicts = 0

        val byId = existing.associateBy { it.id }
        val files = folder.listFiles().filter { MirrorFileLookup.isMirrorEntry(it) }

        for (file in files) {
            val raw = runCatching {
                context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    ?.toString(Charsets.UTF_8)
            }.getOrNull()
            if (raw == null) {
                errors++
                continue
            }

            val parsed = SyncFrontmatter.decode(raw)
            val existingNote = parsed.markleafId?.let(byId::get)
            val fileTs = MirrorReconcile.effectiveFileTimestamp(
                frontmatterUpdatedAt = parsed.updatedAt,
                fileModifiedAt = Instant.ofEpochMilli(file.lastModified()),
                bodyChanged = existingNote != null && parsed.body != existingNote.contentMarkdown
            )

            try {
                when (MirrorReconcile.reconcileAction(existingNote, fileTs)) {
                    // A note in Trash keeps its mirror file on disk (deletion is
                    // reversible; only permanent delete removes it). Re-importing
                    // it would resurrect the deleted note (#148), so skip it.
                    NoteFolderMirror.Reconcile.SkipTrashed -> skipped++
                    // File isn't strictly newer than the DB note — nothing to do.
                    NoteFolderMirror.Reconcile.Skip -> skipped++
                    NoteFolderMirror.Reconcile.Create -> {
                        val now = Instant.now()
                        val newNote = Note(
                            id = parsed.markleafId ?: UUID.randomUUID().toString(),
                            title = TitleExtractor.extractTitle(parsed.body, titleSource),
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body, titleSource),
                            createdAt = parsed.createdAt ?: now,
                            updatedAt = parsed.updatedAt ?: now,
                            pinned = parsed.pinned ?: false,
                            archived = parsed.archived ?: false,
                            lastImportedAt = parsed.updatedAt ?: now,
                            remoteSeenAt = parsed.updatedAt ?: now
                        )
                        applyCreate(newNote)
                        created++
                        // A file with no `markleaf_id` (created in another app, or
                        // hand-dropped) has nothing for the next reconcile to match
                        // on, so every subsequent import would re-create it as a
                        // brand-new note — the #140 "same note appears 4-5 times"
                        // duplication. Stamp our id back into the file *in place* so
                        // the next pass matches by id and updates instead. Existing
                        // frontmatter keys are preserved. Best-effort: a failed
                        // write just defers de-duplication to a later sync.
                        if (parsed.markleafId == null) {
                            stampFrontmatter(context, file, newNote, parsed.unknownEntries)
                        }
                    }
                    NoteFolderMirror.Reconcile.Conflict -> {
                        // Both sides moved since the last sync. Keep the local
                        // note's content untouched and bring the remote in as a
                        // separate note so the user can compare and merge by hand.
                        val baseTitle = TitleExtractor.extractTitle(parsed.body, titleSource)
                        val suffix = conflictSuffix(context, Instant.now())
                        val duplicate = Note(
                            id = UUID.randomUUID().toString(),
                            title = "$baseTitle $suffix",
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body, titleSource),
                            createdAt = parsed.createdAt ?: fileTs,
                            updatedAt = fileTs,
                            pinned = false,
                            archived = false,
                            lastImportedAt = fileTs,
                            remoteSeenAt = fileTs,
                            isConflictCopy = true
                        )
                        applyCreate(duplicate)
                        // Record that this remote version has been dealt with,
                        // so the next pass resolves to Skip instead of taking
                        // the copy again — once a minute, without end (#217).
                        // Nothing the *user* owns is touched: `updatedAt` stays
                        // put, so a note they never edited keeps its place in
                        // the list (#222). Only `remoteSeenAt` moves.
                        applyUpdate(existingNote!!.copy(remoteSeenAt = fileTs))
                        conflicts++
                    }
                    NoteFolderMirror.Reconcile.Overwrite -> {
                        val note = existingNote!!
                        val merged = note.copy(
                            title = TitleExtractor.extractTitle(parsed.body, titleSource),
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body, titleSource),
                            updatedAt = fileTs,
                            pinned = parsed.pinned ?: note.pinned,
                            archived = parsed.archived ?: note.archived,
                            lastImportedAt = fileTs,
                            remoteSeenAt = fileTs
                        )
                        applyUpdate(merged)
                        updated++
                    }
                }
            } catch (e: Exception) {
                errors++
            }
        }

        return NoteFolderMirror.ImportResult(
            updated = updated,
            created = created,
            skipped = skipped,
            errors = errors,
            conflicts = conflicts
        )
    }

    /**
     * [importChangesFrom] in sidecar mode (#216).
     *
     * Structurally the same pass as the frontmatter one — same Trash rule, same
     * conflict-copy behaviour — but every decision comes from the index rather
     * than the file's head, and "has this changed" is a hash comparison rather
     * than a timestamp one. See [MirrorReconcile.sidecarReconcileAction].
     *
     * The index is rewritten once at the end rather than per file: an import
     * that touches fifty notes should cost the folder one index write, not
     * fifty, and a sync client watching the folder should see one change.
     */
    private suspend fun importChangesSidecar(
        context: Context,
        folder: DocumentFile,
        existing: List<Note>,
        applyUpdate: suspend (Note) -> Unit,
        applyCreate: suspend (Note) -> Unit,
        deviceId: String,
        titleSource: NoteTitleSource
    ): NoteFolderMirror.ImportResult {
        var updated = 0
        var created = 0
        var skipped = 0
        var errors = 0
        var conflicts = 0

        val byId = existing.associateBy { it.id }
        val merged = SidecarStore.load(context, folder, deviceId)
        val byFileName = SidecarIndex.byFileName(merged)
        val ownEntries = SidecarStore.ownEntries(context, folder, deviceId)
        val files = folder.listFiles().filter { MirrorFileLookup.isMirrorEntry(it) }

        // Rows describing neither a note nor a file. A note deleted on another
        // device takes its file with it and cannot touch our index, so without
        // this our copy carries that row for the folder's lifetime — and the
        // same in reverse, leaving both devices holding the other's dead rows
        // (#262).
        for (id in staleEntryIds(ownEntries.values, byId.keys, files.map { it.name.orEmpty() })) {
            ownEntries.remove(id)
        }

        for (file in files) {
            val body = runCatching {
                context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    ?.toString(Charsets.UTF_8)
            }.getOrNull()
            if (body == null) {
                errors++
                continue
            }

            val fileName = file.name.orEmpty()
            // A file may still carry a header — written before the mode was
            // switched, or arriving from a device still in frontmatter mode. Its
            // block is metadata, not text, and reading it as text would paste it
            // into the note. Strip it, and use its id when the index has nothing
            // for this file: an id is a better link than a filename.
            val parsed = SyncFrontmatter.decode(body)
            val text = parsed.body
            val entry = byFileName[fileName]
            val existingNote = (entry?.noteId ?: parsed.markleafId)?.let(byId::get)
            val hash = SidecarIndex.hashOf(text)
            val matchesLastWrite = entry != null && entry.contentHash == hash

            try {
                when (MirrorReconcile.sidecarReconcileAction(existingNote, matchesLastWrite)) {
                    NoteFolderMirror.Reconcile.SkipTrashed -> skipped++
                    NoteFolderMirror.Reconcile.Skip -> skipped++
                    NoteFolderMirror.Reconcile.Create -> {
                        val now = Instant.now()
                        // An entry with no note behind it means the note was
                        // deleted elsewhere while the file stayed; reusing its
                        // id keeps the file attached to one note rather than
                        // spawning a fresh one on every pass.
                        val createdAt = entry?.createdAtMillis
                            ?.takeIf { it > 0L }
                            ?.let(Instant::ofEpochMilli)
                            ?: parsed.createdAt
                            ?: now
                        val newNote = Note(
                            id = entry?.noteId
                                // A leftover header still identifies its note —
                                // a file written before the switch, or by a
                                // device still in frontmatter mode. Minting a
                                // fresh id here would give that note two
                                // identities across devices.
                                ?: parsed.markleafId
                                ?: UUID.randomUUID().toString(),
                            title = TitleExtractor.extractTitle(text, titleSource),
                            contentMarkdown = text,
                            excerpt = TitleExtractor.generateExcerpt(text, titleSource),
                            createdAt = createdAt,
                            updatedAt = now,
                            pinned = entry?.pinned ?: parsed.pinned ?: false,
                            archived = entry?.archived ?: parsed.archived ?: false,
                            lastImportedAt = now,
                            remoteSeenAt = now
                        )
                        applyCreate(newNote)
                        created++
                        // The write-back that the frontmatter path performs by
                        // stamping an id into the file. Without it the next pass
                        // sees an unclaimed file and imports it again — #140.
                        ownEntries[newNote.id] = SidecarEntry(
                            noteId = newNote.id,
                            fileName = fileName,
                            contentHash = hash,
                            createdAtMillis = createdAt.toEpochMilli(),
                            pinned = newNote.pinned,
                            archived = newNote.archived
                        )
                    }
                    NoteFolderMirror.Reconcile.Conflict -> {
                        val note = existingNote!!
                        val baseTitle = TitleExtractor.extractTitle(text, titleSource)
                        val duplicate = Note(
                            id = UUID.randomUUID().toString(),
                            title = "$baseTitle ${conflictSuffix(context, Instant.now())}",
                            contentMarkdown = text,
                            excerpt = TitleExtractor.generateExcerpt(text, titleSource),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                            lastImportedAt = Instant.now(),
                            remoteSeenAt = Instant.now(),
                            isConflictCopy = true
                        )
                        applyCreate(duplicate)
                        // Record the version we just took a copy of. This is
                        // what `remoteSeenAt` does on the frontmatter path: it
                        // stops the next pass copying the same remote version
                        // again, once a minute, for ever (#217). Nothing the
                        // user owns moves — the local note is untouched.
                        ownEntries[note.id] = SidecarEntry(
                            noteId = note.id,
                            fileName = fileName,
                            contentHash = hash,
                            createdAtMillis = note.createdAt.toEpochMilli(),
                            pinned = note.pinned,
                            archived = note.archived
                        )
                        conflicts++
                    }
                    NoteFolderMirror.Reconcile.Overwrite -> {
                        val note = existingNote!!
                        val now = Instant.now()
                        applyUpdate(
                            note.copy(
                                title = TitleExtractor.extractTitle(text, titleSource),
                                contentMarkdown = text,
                                excerpt = TitleExtractor.generateExcerpt(text, titleSource),
                                updatedAt = now,
                                lastImportedAt = now,
                                remoteSeenAt = now
                            )
                        )
                        updated++
                        ownEntries[note.id] = SidecarEntry(
                            noteId = note.id,
                            fileName = fileName,
                            contentHash = hash,
                            createdAtMillis = note.createdAt.toEpochMilli(),
                            pinned = note.pinned,
                            archived = note.archived
                        )
                    }
                }
            } catch (e: Exception) {
                errors++
            }
        }

        SidecarStore.write(context, folder, deviceId, ownEntries.values)

        return NoteFolderMirror.ImportResult(
            updated = updated,
            created = created,
            skipped = skipped,
            errors = errors,
            conflicts = conflicts
        )
    }

    /**
     * The ids in [entries] that describe neither a live note nor a file that is
     * still in the folder — the rows an index can drop.
     *
     * **Both conditions are required, and the second one is the careful half.**
     * A missing file on its own means nothing: a sync client that has not
     * delivered it yet, or a listing that came back short, would have us drop a
     * mapping the next pass needs — and an entry dropped while its file is
     * still there is precisely how that file imports as a second copy of a note
     * (#140). Once the note is gone from the database as well, there is nothing
     * left for the entry to point at in either direction.
     *
     * [liveNoteIds] must therefore come from the complete note set — active,
     * archived *and* trashed, the same set [importChangesFrom] requires. A
     * trashed note still owns its entry; it is a note the user can restore.
     * Filenames are compared case-insensitively on purpose, the opposite choice
     * to [MirrorFileLookup.matchByName]: here a fold that matches too much only
     * keeps an entry alive, which is the harmless direction.
     */
    internal fun staleEntryIds(
        entries: Collection<SidecarEntry>,
        liveNoteIds: Set<String>,
        fileNames: Collection<String>
    ): Set<String> {
        val present = fileNames.mapTo(HashSet(fileNames.size)) { it.lowercase() }
        return entries
            .filterNot { it.noteId in liveNoteIds || it.fileName.lowercase() in present }
            .mapTo(HashSet()) { it.noteId }
    }

    /**
     * The title suffix marking a conflict copy — "(copy from another device 0509
     * 12:34)" in the user's language. The stamp inside it stays numeric and
     * locale-independent so it is scannable at a glance and sorts sensibly.
     *
     * This used to be a hardcoded Korean literal, which every language saw and
     * which the Sync Center's `title LIKE` query depended on. Detection now
     * rides on [Note.isConflictCopy], which is what freed the label to be
     * translated (#217).
     */
    private fun conflictSuffix(context: Context, now: Instant): String {
        val ldt = java.time.LocalDateTime.ofInstant(now, java.time.ZoneId.systemDefault())
        val stamp = "%02d%02d %02d:%02d".format(
            ldt.monthValue, ldt.dayOfMonth, ldt.hour, ldt.minute
        )
        return context.getString(R.string.sync_conflict_copy_suffix, stamp)
    }

    /**
     * Rewrite [file] in place with [note]'s frontmatter (incl. `markleaf_id`)
     * prepended, preserving any [extraEntries] the file already carried. Used when
     * importing a file that had no `markleaf_id` so the next reconcile can match
     * it by id rather than re-creating a duplicate note (#140). Best-effort —
     * returns false on any IO failure without throwing, so a write-back hiccup
     * never aborts an import.
     */
    private fun stampFrontmatter(
        context: Context,
        file: DocumentFile,
        note: Note,
        extraEntries: List<String>
    ): Boolean {
        if (!file.canWrite()) return false
        return runCatching {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write(SyncFrontmatter.encode(note, extraEntries))
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }
}
