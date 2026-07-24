package com.markleaf.notes.data.sync

import com.markleaf.notes.data.sync.NoteFolderMirror.Reconcile
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // ---- index pruning (#262) ----

    private fun sidecarEntry(noteId: String, fileName: String) = SidecarEntry(
        noteId = noteId,
        fileName = fileName,
        contentHash = "h",
        createdAtMillis = 0L,
        pinned = false,
        archived = false
    )

    private fun stale(
        entries: List<SidecarEntry>,
        liveNoteIds: Set<String>,
        fileNames: List<String>
    ) = NoteFolderMirror.staleEntryIds(entries, liveNoteIds, fileNames)

    /** The shape the item describes: another device deleted note and file both. */
    @Test
    fun `an entry with neither note nor file is prunable`() {
        assertEquals(
            setOf("gone"),
            stale(
                entries = listOf(sidecarEntry("gone", "Gone.md"), sidecarEntry("kept", "Kept.md")),
                liveNoteIds = setOf("kept"),
                fileNames = listOf("Kept.md")
            )
        )
    }

    /**
     * The half that keeps #140 shut. A file we still hold has to keep its
     * mapping, or the next pass reads it as an unknown file and imports it as a
     * second copy of the note.
     */
    @Test
    fun `an entry whose file is still there is kept even with no note`() {
        assertEquals(
            emptySet<String>(),
            stale(
                entries = listOf(sidecarEntry("n", "Notes.md")),
                liveNoteIds = emptySet(),
                fileNames = listOf("Notes.md")
            )
        )
    }

    /** A folder that failed to list, or a sync client mid-download. */
    @Test
    fun `an entry whose note is still there is kept even with no file`() {
        assertEquals(
            emptySet<String>(),
            stale(
                entries = listOf(sidecarEntry("n", "Notes.md")),
                liveNoteIds = setOf("n"),
                fileNames = emptyList()
            )
        )
    }

    /**
     * Folding too much here only keeps an entry alive, so the prune folds where
     * [NoteFolderMirror.matchByName] deliberately does not.
     */
    @Test
    fun `a file differing only in case still protects its entry`() {
        assertEquals(
            emptySet<String>(),
            stale(
                entries = listOf(sidecarEntry("n", "Notes.md")),
                liveNoteIds = emptySet(),
                fileNames = listOf("notes.md")
            )
        )
    }

    // ---- filename matching (#262) ----

    private fun match(names: List<String>, wanted: String) =
        NoteFolderMirror.matchByName(names, wanted) { it }

    @Test
    fun `an exact name wins over one differing only in case`() {
        assertEquals("notes.md", match(listOf("Notes.md", "notes.md"), "notes.md"))
        assertEquals("Notes.md", match(listOf("Notes.md", "notes.md"), "Notes.md"))
    }

    @Test
    fun `a name nothing bears exactly falls back to ignoring case`() {
        assertEquals("Notes.md", match(listOf("Notes.md"), "notes.md"))
        assertNull(match(listOf("Notes.md"), "Other.md"))
    }
}
