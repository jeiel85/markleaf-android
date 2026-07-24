package com.markleaf.notes.data.sync

import com.markleaf.notes.data.sync.NoteFolderMirror.Reconcile
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The sidecar reconcile matrix (#216) — the decision made for one mirror file
 * when the metadata lives beside the notes rather than inside them.
 *
 * Deliberately the same shape as [NoteFolderMirrorConflictLogicTest], because
 * the two paths must agree on everything except how "has this file changed" is
 * answered: by content here, by timestamp there.
 */
class SidecarReconcileLogicTest {

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

    private fun action(existing: Note?, matches: Boolean) =
        NoteFolderMirror.sidecarReconcileAction(existing, matches)

    @Test
    fun `a file no entry claims becomes a new note`() {
        assertEquals(Reconcile.Create, action(existing = null, matches = false))
        // Even a hash that happens to match nothing in particular.
        assertEquals(Reconcile.Create, action(existing = null, matches = true))
    }

    /** #148: re-importing a trashed note resurrects a deletion the user made. */
    @Test
    fun `a trashed note is never re-imported`() {
        assertEquals(
            Reconcile.SkipTrashed,
            action(note(updatedAtMs = 1_000, trashed = true), matches = false)
        )
    }

    @Test
    fun `an archived note still takes edits`() {
        assertEquals(
            Reconcile.Overwrite,
            action(note(updatedAtMs = 1_000, lastImportMs = 1_000, archived = true), matches = false)
        )
    }

    /**
     * The whole point of hashing. A sync client re-downloading a file bumps its
     * mtime without changing a byte; the timestamp path has to reason around
     * that, and this one simply never asks.
     */
    @Test
    fun `an unchanged file is skipped however old the note looks`() {
        assertEquals(Reconcile.Skip, action(note(updatedAtMs = 0), matches = true))
        assertEquals(
            Reconcile.Skip,
            action(note(updatedAtMs = 9_999_999, lastImportMs = 0), matches = true)
        )
    }

    @Test
    fun `a changed file with no local edit overwrites`() {
        assertEquals(
            Reconcile.Overwrite,
            action(note(updatedAtMs = 5_000, lastImportMs = 5_000), matches = false)
        )
    }

    @Test
    fun `a changed file with a local edit conflicts`() {
        assertEquals(
            Reconcile.Conflict,
            action(note(updatedAtMs = 60_000, lastImportMs = 5_000), matches = false)
        )
    }

    /**
     * A note that has never been imported has a null stamp. Treating that as
     * "edited locally" would send the very first genuine remote edit down the
     * conflict path — the #217 defect, in the shape it would take here.
     */
    @Test
    fun `a never-imported note within the clock slack is not a conflict`() {
        assertEquals(
            Reconcile.Overwrite,
            action(note(updatedAtMs = 1_000, lastImportMs = null), matches = false)
        )
    }

    @Test
    fun `an edit inside the clock slack is not treated as a local edit`() {
        // 2s slack for filesystem clocks, same constant as the timestamp path.
        assertEquals(
            Reconcile.Overwrite,
            action(note(updatedAtMs = 6_000, lastImportMs = 5_000), matches = false)
        )
        assertEquals(
            Reconcile.Conflict,
            action(note(updatedAtMs = 8_000, lastImportMs = 5_000), matches = false)
        )
    }
}
