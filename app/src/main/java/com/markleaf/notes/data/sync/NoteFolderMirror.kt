package com.markleaf.notes.data.sync

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.markleaf.notes.R
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.time.Instant
import java.util.UUID

/**
 * Read/write notes to a user-chosen SAF folder as `.md` / `.txt` files with our
 * [SyncFrontmatter] header. This is the heart of the v2.1 multi-device flow:
 * Markleaf itself never goes online — but if the user points us at a folder
 * that some other app (Dropbox / Drive / Syncthing / NAS WebDAV mount) syncs,
 * the notes follow.
 *
 * Filenames track the note **title** (#134): the canonical link is the
 * frontmatter `markleaf_id`, so a note's file is normally located by parsing
 * that id rather than by anything in the filename. When a title changes the file
 * is renamed in place (never deleted + recreated), so a mid-flight sync client
 * never sees a note vanish.
 *
 * The filename is the *fallback* link, not a second source of truth: when no
 * file carries the id, a same-named file that no other note claims is adopted
 * rather than left alone. Without that, another app rewriting a file without
 * preserving our block left the note permanently unlinked, and every auto-save
 * forked another ` (2)` copy (#213).
 *
 * Safety posture:
 * - Auto-export on save *only writes* (we are the source of truth for our own
 *   edits). It overwrites a file we already wrote, but never silently discards
 *   an unread newer file (`importChanges` is what reads).
 * - `importChanges` only updates a DB note when the file is *newer* than the DB
 *   record. Never deletes notes. (Auto-delete sync is deliberately deferred.)
 * - Content is written before any rename, so a failed rename leaves the bytes
 *   safely persisted under the old name.
 * - Filename collisions (two notes with the same title) get a " (2)" suffix,
 *   never overwriting an unrelated file.
 */
object NoteFolderMirror {

    /** Result of an import pass for the Settings UI to show. */
    data class ImportResult(
        val updated: Int,
        val created: Int,
        val skipped: Int,
        val errors: Int,
        /**
         * Number of notes where both the local DB and the remote file moved
         * since the last sync. The remote side was kept as a separate note
         * flagged [Note.isConflictCopy] — the Sync Center lists those — instead
         * of overwriting the local edits.
         */
        val conflicts: Int = 0
    )

    /**
     * Write a single note to the mirror folder. The file is named after the
     * note's title: we locate its existing file by frontmatter `markleaf_id`,
     * and if the title-derived name has drifted we rename the file in place. A
     * brand-new note's file is created with [extension]; an existing file keeps
     * whatever extension it already has (flipping the setting never rewrites old
     * files' suffixes).
     */
    fun writeNote(
        context: Context,
        folderUri: Uri,
        note: Note,
        extension: SyncFileExtension = SyncFileExtension.MD
    ): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false

        val mirrorFiles = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }
        val match = findMirrorFile(context, mirrorFiles, note)
        val existing = match?.file
        val ext = existing?.mirrorExtension() ?: extension.value
        val desiredName = resolveName(note, ext, mirrorFiles, ownFile = existing)

        val target = existing
            ?: folder.createFile(mimeTypeFor(ext), desiredName)
            ?: return false

        // Write content first, then rename. If the rename fails the bytes are
        // already safely persisted under the old name — no data loss.
        val wrote = runCatching {
            context.contentResolver.openOutputStream(target.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write(SyncFrontmatter.encode(note, match?.extraKeys.orEmpty()))
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
        if (!wrote) return false

        if (existing != null && existing.name != desiredName) {
            existing.renameTo(desiredName) // best-effort; old name is fine if it fails
        }
        return true
    }

    /**
     * [writeNote] plus the `lastImportedAt` stamp that records "the folder now
     * holds exactly this version of the note".
     *
     * Every mirror write needs that stamp. Only the editor's auto-save did it,
     * so a note seeded to the folder from Settings, the Sync Center or an
     * unlock kept `lastImportedAt == null` — which the reconcile reads as
     * "edited locally since the last import" for ever, sending every genuinely
     * newer file down the conflict path instead of overwriting cleanly (#217).
     *
     * [onStamped] receives the stamped note; callers pass their repository's
     * update so this stays free of a data-layer dependency.
     */
    suspend fun writeNoteAndStamp(
        context: Context,
        folderUri: Uri,
        note: Note,
        extension: SyncFileExtension,
        onStamped: suspend (Note) -> Unit
    ): Boolean {
        val wrote = writeNote(context, folderUri, note, extension)
        if (wrote) onStamped(note.copy(lastImportedAt = note.updatedAt))
        return wrote
    }

    /**
     * Rename a note's mirror file to match its current title *without* rewriting
     * the body — used by the Sync Center "tidy filenames" action so existing
     * mirrors created under the old `slug-id…` scheme migrate to clean titles in
     * one pass. Returns true only if a rename actually happened.
     */
    fun renameToTitle(context: Context, folderUri: Uri, note: Note): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false
        val mirrorFiles = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }
        val file = findFileForNote(context, mirrorFiles, note.id) ?: return false
        val desired = resolveName(note, file.mirrorExtension(), mirrorFiles, ownFile = file)
        if (file.name == desired) return false
        return runCatching { file.renameTo(desired) }.getOrDefault(false)
    }

    /**
     * Rewrite [file] in place with [note]'s frontmatter (incl. `markleaf_id`)
     * prepended, preserving any [extraKeys] the file already carried. Used when
     * importing a file that had no `markleaf_id` so the next reconcile can match
     * it by id rather than re-creating a duplicate note (#140). Best-effort —
     * returns false on any IO failure without throwing, so a write-back hiccup
     * never aborts an import.
     */
    private fun stampFrontmatter(
        context: Context,
        file: DocumentFile,
        note: Note,
        extraKeys: Map<String, String>
    ): Boolean {
        if (!file.canWrite()) return false
        return runCatching {
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    writer.write(SyncFrontmatter.encode(note, extraKeys))
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    /**
     * Delete the mirrored note file and any per-note attachment subfolder for
     * [noteId]. Called from the permanent-delete flow so the mirror folder
     * doesn't accumulate orphan files. Idempotent — missing files are not
     * an error. Returns true if anything was actually removed.
     *
     * Direction is *DB → file only*; the inverse (file deleted externally → drop
     * the DB note) is intentionally not implemented to avoid silent data loss
     * when a sync client is mid-flight.
     */
    fun deleteNote(context: Context, folderUri: Uri, noteId: String): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return false
        if (!folder.canWrite()) return false

        var changed = false
        val mirrorFiles = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }
        findFileForNote(context, mirrorFiles, noteId)?.let { file ->
            if (file.delete()) changed = true
        }
        // Also clear the per-note attachments subfolder.
        folder.findFile(ATTACHMENTS_DIR)?.findFile(noteId)?.let { dir ->
            if (deleteRecursively(dir)) changed = true
        }
        return changed
    }

    /**
     * Mirror the supplied attachment files into
     * `<folder>/attachments/<noteId>/`. Files already present with the same
     * name are *not* re-copied — attachment IDs are UUIDs so collisions imply
     * the same content. Used by the editor's auto-save hook so the note and
     * its referenced images travel together when the chosen folder is synced.
     */
    fun mirrorAttachments(
        context: Context,
        folderUri: Uri,
        noteId: String,
        sources: List<File>
    ): Int {
        if (sources.isEmpty()) return 0
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return 0
        if (!folder.canWrite()) return 0

        val attachmentsRoot = folder.findFile(ATTACHMENTS_DIR)
            ?: folder.createDirectory(ATTACHMENTS_DIR)
            ?: return 0
        val noteDir = attachmentsRoot.findFile(noteId)
            ?: attachmentsRoot.createDirectory(noteId)
            ?: return 0

        var copied = 0
        for (source in sources) {
            if (!source.exists()) continue
            val name = source.name
            // Skip if already mirrored — UUID filenames mean same name == same bytes.
            if (noteDir.findFile(name) != null) continue
            val target = noteDir.createFile(guessMimeType(name), name) ?: continue
            val ok = runCatching {
                context.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
                    source.inputStream().use { input -> input.copyTo(out) }
                } != null
            }.getOrDefault(false)
            if (ok) copied++ else target.delete()
        }
        return copied
    }

    private fun deleteRecursively(node: DocumentFile): Boolean {
        if (node.isDirectory) {
            node.listFiles().forEach { deleteRecursively(it) }
        }
        return node.delete()
    }

    private fun guessMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    /**
     * Walk the folder, parse each mirror file, and reconcile with the supplied
     * existing notes. Returns aggregated counts.
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
    suspend fun importChanges(
        context: Context,
        folderUri: Uri,
        existing: List<Note>,
        applyUpdate: suspend (Note) -> Unit,
        applyCreate: suspend (Note) -> Unit
    ): ImportResult {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: return ImportResult(0, 0, 0, 1)
        if (!folder.canRead()) return ImportResult(0, 0, 0, 1)

        var updated = 0
        var created = 0
        var skipped = 0
        var errors = 0
        var conflicts = 0

        val byId = existing.associateBy { it.id }
        val files = folder.listFiles().filter { it.isFile && isMirrorFile(it.name) }

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
            val fileTs = effectiveFileTimestamp(
                frontmatterUpdatedAt = parsed.updatedAt,
                fileModifiedAt = Instant.ofEpochMilli(file.lastModified()),
                bodyChanged = existingNote != null && parsed.body != existingNote.contentMarkdown
            )

            try {
                when (reconcileAction(existingNote, fileTs)) {
                    // A note in Trash keeps its mirror file on disk (deletion is
                    // reversible; only permanent delete removes it). Re-importing
                    // it would resurrect the deleted note (#148), so skip it.
                    Reconcile.SkipTrashed -> skipped++
                    // File isn't strictly newer than the DB note — nothing to do.
                    Reconcile.Skip -> skipped++
                    Reconcile.Create -> {
                        val now = Instant.now()
                        val newNote = Note(
                            id = parsed.markleafId ?: UUID.randomUUID().toString(),
                            title = TitleExtractor.extractTitle(parsed.body),
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body),
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
                            stampFrontmatter(context, file, newNote, parsed.unknownKeys)
                        }
                    }
                    Reconcile.Conflict -> {
                        // Both sides moved since the last sync. Keep the local
                        // note's content untouched and bring the remote in as a
                        // separate note so the user can compare and merge by hand.
                        val baseTitle = TitleExtractor.extractTitle(parsed.body)
                        val suffix = conflictSuffix(context, Instant.now())
                        val duplicate = Note(
                            id = UUID.randomUUID().toString(),
                            title = "$baseTitle $suffix",
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body),
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
                    Reconcile.Overwrite -> {
                        val note = existingNote!!
                        val merged = note.copy(
                            title = TitleExtractor.extractTitle(parsed.body),
                            contentMarkdown = parsed.body,
                            excerpt = TitleExtractor.generateExcerpt(parsed.body),
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

        return ImportResult(
            updated = updated,
            created = created,
            skipped = skipped,
            errors = errors,
            conflicts = conflicts
        )
    }

    /** The action the reconcile takes for one mirror file. */
    internal enum class Reconcile { Create, SkipTrashed, Skip, Overwrite, Conflict }

    /**
     * Decide what [importChanges] should do with a single mirror file, purely
     * from its matching DB note (if any) and the file's effective timestamp.
     * Extracted so the decision — the #148 Trash skip and the newer-file /
     * local-edit conflict matrix — is unit-testable without the SAF IO path
     * (which stays covered by the live tablet smoke).
     *
     * @param existingNote the DB note whose id matches the file, or null when
     *   the file is unknown (a genuinely new / hand-dropped note).
     * @param fileTs the file's effective modified time (frontmatter `updatedAt`,
     *   falling back to the filesystem mtime).
     */
    internal fun reconcileAction(existingNote: Note?, fileTs: Instant): Reconcile {
        if (existingNote == null) return Reconcile.Create
        // A note in Trash must never be re-imported — that resurrects a note the
        // user deleted (#148). Archived notes are *not* skipped: they stay hidden
        // but still take edits from the folder.
        if (existingNote.trashed) return Reconcile.SkipTrashed
        // A remote version we have already resolved is not news, whatever it
        // looks like next to `updatedAt`. This is what lets a conflict settle:
        // the copy is taken once, and this pair goes quiet until one side
        // actually moves again. Before it, the only lever was pushing
        // `updatedAt` past the file — reordering a note nobody had edited (#222).
        val remoteSeen = existingNote.remoteSeenAt?.toEpochMilli()
        if (remoteSeen != null && fileTs.toEpochMilli() <= remoteSeen) return Reconcile.Skip
        val fileNewer =
            fileTs.toEpochMilli() > existingNote.updatedAt.toEpochMilli() + SLACK_MILLIS
        if (!fileNewer) return Reconcile.Skip
        val lastImport = existingNote.lastImportedAt?.toEpochMilli() ?: 0L
        val localEditedSinceImport =
            existingNote.updatedAt.toEpochMilli() > lastImport + SLACK_MILLIS
        return if (localEditedSinceImport) Reconcile.Conflict else Reconcile.Overwrite
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
     * The timestamp to compare a mirror file against its DB note.
     *
     * The frontmatter `updated_at` is authoritative when present, with one
     * exception: an app that edits the body but leaves our block alone keeps the
     * old value, so the edit never looks newer and is never imported. There —
     * and only there — the filesystem mtime is the better signal.
     *
     * The mtime is deliberately *not* trusted when the body is unchanged. A sync
     * client that re-downloads a file bumps its mtime without touching a byte of
     * content; trusting it there would make every file look newer than its note
     * on every pass, which is a conflict storm rather than a sync.
     */
    internal fun effectiveFileTimestamp(
        frontmatterUpdatedAt: Instant?,
        fileModifiedAt: Instant,
        bodyChanged: Boolean
    ): Instant {
        if (frontmatterUpdatedAt == null) return fileModifiedAt
        if (bodyChanged && fileModifiedAt.isAfter(frontmatterUpdatedAt)) return fileModifiedAt
        return frontmatterUpdatedAt
    }

    /**
     * Locate the mirror file that belongs to [noteId] by parsing each file's
     * frontmatter `markleaf_id`. We only read the leading bytes — the
     * frontmatter always sits at the very top — so this stays cheap even though
     * it touches every file. Extension-agnostic: a note's file may be `.md` or
     * `.txt`.
     */
    private fun findFileForNote(
        context: Context,
        mirrorFiles: List<DocumentFile>,
        noteId: String
    ): DocumentFile? =
        mirrorFiles.firstOrNull { peekFrontmatter(context, it)?.markleafId == noteId }

    /** A note's mirror file together with the frontmatter keys it already carries. */
    private class MirrorMatch(val file: DocumentFile, val extraKeys: Map<String, String>)

    /**
     * The file [note] should be written to. The canonical link is the
     * frontmatter `markleaf_id`; when no file carries it we fall back to
     * *adopting* a file that already has this note's exact name and belongs to
     * nobody — one with no `markleaf_id` of its own.
     *
     * Without that fallback a single lost id forks a new file on every
     * auto-save (#213): the lookup fails, [writeNote] creates a file instead,
     * and the collision guard names it `<title> (2).md`, then `(3)`, and so on
     * for as long as the user keeps typing. Ids go missing whenever another app
     * rewrites the file without preserving our block. Adoption is safe because
     * a file carrying *someone else's* id is never taken — only unclaimed ones,
     * and only under the undisambiguated name (`Notes.md`, never `Notes (2).md`).
     * "Unclaimed" means [peekFrontmatter] read the file's whole block and found
     * no id; a file it could not read to the end of is skipped rather than
     * adopted, so a header longer than the read cap is never overwritten
     * unseen (#222).
     *
     * The file's own unknown frontmatter keys come back alongside it so the
     * write can put them back rather than dropping tags another tool wrote.
     */
    private fun findMirrorFile(
        context: Context,
        mirrorFiles: List<DocumentFile>,
        note: Note
    ): MirrorMatch? {
        val base = MirrorFileNames.sanitizeBase(note.title)
        var unclaimed: MirrorMatch? = null
        for (file in mirrorFiles) {
            val head = peekFrontmatter(context, file) ?: continue
            if (head.markleafId == note.id) return MirrorMatch(file, head.extraKeys)
            if (head.markleafId == null &&
                unclaimed == null &&
                MirrorFileNames.isPlainNameFor(file.name.orEmpty(), base)
            ) {
                unclaimed = MirrorMatch(file, head.extraKeys)
            }
        }
        return unclaimed
    }

    /** What a file's head says about which note owns it. */
    private class HeadInfo(val markleafId: String?, val extraKeys: Map<String, String>)

    /** Whether a head read has seen enough to answer that question. */
    internal enum class HeadScan {
        /** The block was read in full, or the file provably has none. */
        Done,

        /** A block is open and its end lies past what has been read. */
        NeedMore,

        /** The block never closed within the read cap — we cannot say. */
        Undetermined
    }

    /**
     * Whether a head read of [limit] bytes settled the question.
     *
     * Pulled out of the IO loop because the dangerous case is the quiet one:
     * a truncated read of a file whose frontmatter runs long looks exactly like
     * a file with no frontmatter at all. Treating the two alike let
     * [findMirrorFile] class such a file as *unclaimed*, adopt it, and rewrite
     * it — dropping metadata the file really had (#222).
     */
    internal fun headScanVerdict(
        hasFrontmatter: Boolean,
        opensFrontmatter: Boolean,
        blockClosed: Boolean,
        atEof: Boolean,
        limit: Int
    ): HeadScan = when {
        // Metadata parsed — everything we need is in hand.
        hasFrontmatter -> HeadScan.Done
        // No opening delimiter: this file has no block, and never will.
        !opensFrontmatter -> HeadScan.Done
        // The block opened and closed but held body text, not metadata — a pair
        // of horizontal rules. Reading further cannot change that verdict.
        blockClosed -> HeadScan.Done
        // Whole file read and the block never closed, so the opening `---` was
        // a rule too. decode() already treats it as body.
        atEof -> HeadScan.Done
        limit >= FRONTMATTER_MAX_BYTES -> HeadScan.Undetermined
        else -> HeadScan.NeedMore
    }

    /**
     * Read [file]'s head far enough to learn which note owns it.
     *
     * Starts at [FRONTMATTER_PEEK_BYTES] — enough for any block we write, and
     * cheap enough to run over every file in the folder on each save — and only
     * reads further when the file opens a block that hasn't closed yet, up to
     * [FRONTMATTER_MAX_BYTES].
     *
     * Returns null when the file can't be read, and when a block is still open
     * at the cap. Both mean "this file tells us nothing", which the callers
     * treat as *skip*: better to leave an unreadable file alone — and write a
     * duplicate — than to adopt it and overwrite metadata we never saw.
     *
     * Deliberately does not return the parsed body: at these read sizes the
     * body is usually truncated, and the type shouldn't offer it.
     */
    private fun peekFrontmatter(context: Context, file: DocumentFile): HeadInfo? =
        runCatching {
            context.contentResolver.openInputStream(file.uri)?.use { stream ->
                var limit = FRONTMATTER_PEEK_BYTES
                var buf = ByteArray(limit)
                var read = 0
                while (true) {
                    var eof = false
                    while (read < limit) {
                        val n = stream.read(buf, read, limit - read)
                        if (n < 0) { eof = true; break }
                        read += n
                    }
                    if (read <= 0) return@use null
                    val text = String(buf, 0, read, Charsets.UTF_8)
                    val parsed = SyncFrontmatter.decode(text)
                    val verdict = headScanVerdict(
                        hasFrontmatter = parsed.hasFrontmatter,
                        opensFrontmatter = SyncFrontmatter.opensFrontmatter(text),
                        blockClosed = parsed.blockClosed,
                        atEof = eof,
                        limit = limit
                    )
                    when (verdict) {
                        HeadScan.Done -> return@use HeadInfo(parsed.markleafId, parsed.unknownKeys)
                        HeadScan.Undetermined -> return@use null
                        HeadScan.NeedMore -> {
                            limit = (limit * 4).coerceAtMost(FRONTMATTER_MAX_BYTES)
                            buf = buf.copyOf(limit)
                        }
                    }
                }
                // Unreachable: every branch above returns.
                @Suppress("UNREACHABLE_CODE") null
            }
        }.getOrNull()

    /**
     * The title-derived filename to use for [note], disambiguated against the
     * other files in the folder. The note's own [ownFile] is not a collision.
     */
    private fun resolveName(
        note: Note,
        ext: String,
        mirrorFiles: List<DocumentFile>,
        ownFile: DocumentFile?
    ): String {
        val base = MirrorFileNames.sanitizeBase(note.title)
        val taken = mirrorFiles
            .filter { ownFile == null || it.uri != ownFile.uri }
            .mapNotNull { it.name?.lowercase() }
            .toHashSet()
        return MirrorFileNames.uniqueName(base, ext) { it.lowercase() in taken }
    }

    private fun isMirrorFile(name: String?): Boolean {
        val n = name?.lowercase() ?: return false
        return n.endsWith(".md") || n.endsWith(".txt")
    }

    private fun DocumentFile.mirrorExtension(): String =
        if (name?.lowercase()?.endsWith(".txt") == true) "txt" else "md"

    private fun mimeTypeFor(ext: String): String =
        if (ext == "txt") "text/plain" else "text/markdown"

    private const val SLACK_MILLIS = 2_000L
    private const val ATTACHMENTS_DIR = "attachments"
    private const val FRONTMATTER_PEEK_BYTES = 4096

    /**
     * Ceiling on how far [peekFrontmatter] will chase an unclosed frontmatter
     * block. A block we write is a few hundred bytes; one that has not ended
     * after 256 KB is not a header we should be reasoning about, and reading
     * further would put an unbounded cost on a path that runs over every file
     * in the folder each time a note is saved.
     */
    internal const val FRONTMATTER_MAX_BYTES = 256 * 1024
}

/**
 * The sync folder as a [Uri], or null when folder sync is off or the persisted
 * value no longer parses.
 *
 * Both guards are needed wherever the folder is touched: sync may be
 * unconfigured, and the stored string is opaque text that a revoked or
 * rewritten SAF grant can leave unparseable. Nine call sites spelled them out
 * separately, in three different shapes, which made the one call site carrying
 * an *extra* condition (the editor's `!note.locked`) hard to pick out (#158).
 */
fun AppSettings.syncFolderUriOrNull(): Uri? =
    syncFolderUri?.let { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
