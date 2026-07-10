package com.markleaf.notes.data.sync

import com.markleaf.notes.data.sync.NoteFolderMirror.Reconcile
import com.markleaf.notes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Pure-logic checks of [NoteFolderMirror.reconcileAction] — the decision the
 * folder reconcile makes for a single mirror file. These drive the real
 * production function (no reimplementation) but don't exercise the SAF IO,
 * which stays covered by the live tablet smoke.
 */
class NoteFolderMirrorConflictLogicTest {

    /** A note with the given sync timestamps; active unless flagged otherwise. */
    private fun note(
        updatedAtMs: Long,
        lastImportMs: Long? = null,
        trashed: Boolean = false,
        archived: Boolean = false
    ) = Note(
        id = "n1",
        title = "T",
        contentMarkdown = "T",
        excerpt = "T",
        createdAt = Instant.ofEpochMilli(0),
        updatedAt = Instant.ofEpochMilli(updatedAtMs),
        trashed = trashed,
        archived = archived,
        lastImportedAt = lastImportMs?.let { Instant.ofEpochMilli(it) }
    )

    private fun action(existing: Note?, fileTsMs: Long) =
        NoteFolderMirror.reconcileAction(existing, Instant.ofEpochMilli(fileTsMs))

    // --- unknown file → new note -------------------------------------------

    @Test
    fun noMatchingNote_isCreate() {
        assertEquals(Reconcile.Create, action(existing = null, fileTsMs = 100_000))
    }

    // --- #148: a note in Trash is never re-imported ------------------------

    @Test
    fun trashedNote_isSkipped_evenWhenFileIsNewer() {
        // File looks far newer, but the note is deleted — it must not resurrect.
        assertEquals(
            Reconcile.SkipTrashed,
            action(note(updatedAtMs = 0, trashed = true), fileTsMs = 100_000)
        )
    }

    @Test
    fun archivedNote_reconcilesNormally_notSkipped() {
        // Archived notes stay hidden but still take folder edits — only Trash is
        // skipped (#148).
        assertEquals(
            Reconcile.Overwrite,
            action(note(updatedAtMs = 10_000, lastImportMs = 10_000, archived = true), fileTsMs = 100_000)
        )
    }

    @Test
    fun restoredFromTrash_reconcilesNormally() {
        // After restoreFromTrash the note is active again (trashed = false), so a
        // later sync updates it as usual instead of skipping it.
        assertEquals(
            Reconcile.Overwrite,
            action(note(updatedAtMs = 10_000, lastImportMs = 10_000, trashed = false), fileTsMs = 100_000)
        )
    }

    // --- newer-file vs local-edit conflict matrix --------------------------

    @Test
    fun fileOlder_isSkipped() {
        // Local was edited; the remote file hasn't moved past it.
        assertEquals(Reconcile.Skip, action(note(updatedAtMs = 5_000, lastImportMs = 0), fileTsMs = 1_000))
    }

    @Test
    fun fileNewerAndLocalUntouched_isOverwrite() {
        // Last sync stamped lastImport at 10s; local hasn't moved past that.
        // Remote file is at 100s → safe to overwrite.
        assertEquals(
            Reconcile.Overwrite,
            action(note(updatedAtMs = 10_000, lastImportMs = 10_000), fileTsMs = 100_000)
        )
    }

    @Test
    fun fileNewerAndLocalEditedAfterLastImport_isConflict() {
        // Local moved to 20s after the 10s sync; remote also at 100s → conflict.
        assertEquals(
            Reconcile.Conflict,
            action(note(updatedAtMs = 20_000, lastImportMs = 10_000), fileTsMs = 100_000)
        )
    }

    @Test
    fun fileNewerAndNoPriorImport_isConflict() {
        // Never imported (lastImport null); any remote-newer case is a conflict
        // to avoid silently overwriting a locally-created note.
        assertEquals(
            Reconcile.Conflict,
            action(note(updatedAtMs = 50_000, lastImportMs = null), fileTsMs = 100_000)
        )
    }

    @Test
    fun slackWindowAroundEqualTimestamps_treatedAsSkip() {
        // 1s difference is inside the 2s slack — treat as in sync.
        assertEquals(
            Reconcile.Skip,
            action(note(updatedAtMs = 10_000, lastImportMs = 10_000), fileTsMs = 11_000)
        )
    }

    @Test
    fun base_noteCarriesLastImportedAtField() {
        // Smoke check that the field exists on the domain model and defaults to
        // null for fresh local notes.
        assertEquals(null, note(updatedAtMs = 0).lastImportedAt)
    }
}
