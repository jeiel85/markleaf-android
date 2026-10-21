package com.markleaf.notes.feature.editor

import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Start new notes with a heading" (#544): which notes get the `# ` seed, and
 * that a note holding only the seed still counts as empty for the discard on
 * leaving (#405).
 */
class NewNoteHeadingTest {

    @Test
    fun `an empty note opens on the heading when the setting is on`() {
        assertEquals("# ", newNoteSeed(content = "", startWithHeading = true))
    }

    @Test
    fun `setting off leaves an empty note empty`() {
        assertNull(newNoteSeed(content = "", startWithHeading = false))
    }

    /** "Existing notes are not changed" — anything with text opens as stored. */
    @Test
    fun `a note with text is never seeded`() {
        assertNull(newNoteSeed(content = "Groceries", startWithHeading = true))
        assertNull(newNoteSeed(content = " ", startWithHeading = true))
        assertNull(newNoteSeed(content = "\n", startWithHeading = true))
    }

    private val created = Instant.parse("2026-10-21T00:00:00Z")
    private fun note() = Note(
        id = "n", title = "", contentMarkdown = "", excerpt = "",
        createdAt = created, updatedAt = created
    )

    @Test
    fun `a note never saved since creation is just created`() {
        assertTrue(isJustCreated(note()))
    }

    /** #547 review: these are notes someone kept, so the seed must not touch them. */
    @Test
    fun `kept empty notes are not just created`() {
        assertFalse(isJustCreated(note().copy(lastImportedAt = created)))
        assertFalse(isJustCreated(note().copy(pinned = true)))
        assertFalse(isJustCreated(note().copy(archived = true)))
        assertFalse(isJustCreated(note().copy(locked = true)))
        assertFalse(isJustCreated(note().copy(updatedAt = created.plusSeconds(60))))
    }

    @Test
    fun `the seed alone is an empty note when this visit put it there`() {
        assertTrue(isEmptyNoteText("# ", seededThisVisit = true))
        assertTrue(isEmptyNoteText("#", seededThisVisit = true))
        assertTrue(isEmptyNoteText("#\n\n", seededThisVisit = true))
    }

    /**
     * #547 review: a `#` the user saved, or one the note held when opened, is
     * theirs — the discard deletes for good, so it must not count as empty.
     */
    @Test
    fun `a bare heading marker this visit did not seed is kept`() {
        assertFalse(isEmptyNoteText("# ", seededThisVisit = false))
        assertFalse(isEmptyNoteText("#", seededThisVisit = false))
    }

    /**
     * #547 review: an empty synced note opened on the seed must still adopt a
     * version the folder imports while it is open (#428), because the seed is
     * not an edit. Once the user types, the import waits as for any edit.
     */
    @Test
    fun `an untouched seed does not hold back an import`() {
        assertEquals(
            "from the folder",
            editorLiveRefreshText(
                onScreen = storedText("# ", seededThisVisit = true),
                agreed = "",
                incoming = "from the folder"
            )
        )
        assertNull(
            editorLiveRefreshText(
                onScreen = storedText("# Gro", seededThisVisit = true),
                agreed = "",
                incoming = "from the folder"
            )
        )
        // A `# ` the note already held is real text, compared as it is.
        assertEquals("# ", storedText("# ", seededThisVisit = false))
    }

    @Test
    fun `blank text is still an empty note`() {
        assertTrue(isEmptyNoteText("", seededThisVisit = false))
        assertTrue(isEmptyNoteText("  \n ", seededThisVisit = false))
        assertTrue(isEmptyNoteText("", seededThisVisit = true))
    }

    @Test
    fun `a heading with words is kept`() {
        assertFalse(isEmptyNoteText("# Groceries", seededThisVisit = true))
        assertFalse(isEmptyNoteText("# \nmilk", seededThisVisit = true))
        assertFalse(isEmptyNoteText("## ", seededThisVisit = true))
        assertFalse(isEmptyNoteText("#tag", seededThisVisit = true))
    }
}
