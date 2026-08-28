package com.markleaf.notes.data.sync

import com.markleaf.notes.domain.model.Note
import java.time.Instant

/**
 * The reconcile decisions for the mirror import pass, kept apart from the SAF
 * IO that feeds them: every function here is pure, `internal`, and covered by
 * the conflict-logic unit tests without a device or a folder.
 */
internal object MirrorReconcile {

    private const val SLACK_MILLIS = 2_000L

    /**
     * Decide what [importChanges][NoteFolderMirror.importChanges] should do
     * with a single mirror file, purely from its matching DB note (if any) and
     * the file's effective timestamp. Extracted so the decision — the #148
     * Trash skip and the newer-file / local-edit conflict matrix — is
     * unit-testable without the SAF IO path (which stays covered by the live
     * tablet smoke).
     *
     * @param existingNote the DB note whose id matches the file, or null when
     *   the file is unknown (a genuinely new / hand-dropped note).
     * @param fileTs the file's effective modified time (frontmatter `updatedAt`,
     *   falling back to the filesystem mtime).
     */
    internal fun reconcileAction(existingNote: Note?, fileTs: Instant): NoteFolderMirror.Reconcile {
        if (existingNote == null) return NoteFolderMirror.Reconcile.Create
        // A note in Trash must never be re-imported — that resurrects a note the
        // user deleted (#148). Archived notes are *not* skipped: they stay hidden
        // but still take edits from the folder.
        if (existingNote.trashed) return NoteFolderMirror.Reconcile.SkipTrashed
        // A remote version we have already resolved is not news, whatever it
        // looks like next to `updatedAt`. This is what lets a conflict settle:
        // the copy is taken once, and this pair goes quiet until one side
        // actually moves again. Before it, the only lever was pushing
        // `updatedAt` past the file — reordering a note nobody had edited (#222).
        val remoteSeen = existingNote.remoteSeenAt?.toEpochMilli()
        if (remoteSeen != null && fileTs.toEpochMilli() <= remoteSeen) {
            return NoteFolderMirror.Reconcile.Skip
        }
        val fileNewer =
            fileTs.toEpochMilli() > existingNote.updatedAt.toEpochMilli() + SLACK_MILLIS
        if (!fileNewer) return NoteFolderMirror.Reconcile.Skip
        val lastImport = existingNote.lastImportedAt?.toEpochMilli() ?: 0L
        val localEditedSinceImport =
            existingNote.updatedAt.toEpochMilli() > lastImport + SLACK_MILLIS
        return if (localEditedSinceImport) {
            NoteFolderMirror.Reconcile.Conflict
        } else {
            NoteFolderMirror.Reconcile.Overwrite
        }
    }

    /**
     * [reconcileAction]'s counterpart for sidecar mode (#216), deciding from
     * *content* rather than time.
     *
     * With no frontmatter there is no `updated_at`, and the filesystem mtime is
     * not a usable substitute — a sync client that re-downloads a file bumps it
     * without changing a byte, so every file would look newer than its note on
     * every pass. [effectiveFileTimestamp] documents that trap for the case
     * where it is unavoidable; here it is avoidable, so this asks a question
     * that needs no clock: does the file still hold what Markleaf last wrote or
     * accepted there?
     *
     * @param existingNote the DB note the index maps this file to, or null when
     *   no index entry claims it (a hand-dropped file, or one whose index is
     *   missing).
     * @param fileMatchesLastWrite whether the file's hash equals the one
     *   recorded for it. False means somebody else has been in the file.
     */
    internal fun sidecarReconcileAction(
        existingNote: Note?,
        fileMatchesLastWrite: Boolean
    ): NoteFolderMirror.Reconcile {
        if (existingNote == null) return NoteFolderMirror.Reconcile.Create
        // Same rule as the frontmatter path: a note in Trash is never
        // re-imported, or a deletion the user performed comes back (#148).
        if (existingNote.trashed) return NoteFolderMirror.Reconcile.SkipTrashed
        if (fileMatchesLastWrite) return NoteFolderMirror.Reconcile.Skip
        val lastImport = existingNote.lastImportedAt?.toEpochMilli() ?: 0L
        val localEditedSinceImport =
            existingNote.updatedAt.toEpochMilli() > lastImport + SLACK_MILLIS
        return if (localEditedSinceImport) {
            NoteFolderMirror.Reconcile.Conflict
        } else {
            NoteFolderMirror.Reconcile.Overwrite
        }
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
}
