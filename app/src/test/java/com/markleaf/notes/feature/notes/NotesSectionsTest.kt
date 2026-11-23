package com.markleaf.notes.feature.notes

import com.markleaf.notes.R
import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The headings the notes list splits into, and the continuous list without them (#600). */
class NotesSectionsTest {

    private fun note(id: String, title: String, updatedAt: Long, pinned: Boolean = false) = Note(
        id = id,
        title = title,
        contentMarkdown = title,
        excerpt = "",
        createdAt = Instant.ofEpochMilli(updatedAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        pinned = pinned
    )

    // Every date is decades old, so all unpinned notes fall under Older.
    private val older = note("older", "Banana", updatedAt = 1_000)
    private val newer = note("newer", "apple", updatedAt = 2_000)
    private val pinned = note("pinned", "Zebra", updatedAt = 500, pinned = true)

    @Test
    fun headingsSplitPinnedAndDateSectionsByDefault() {
        val sections = groupNotesForDisplay(listOf(newer, older, pinned), NotesSortMode.UPDATED_DESC)

        assertEquals(listOf(R.string.section_pinned, R.string.section_older), sections.map { it.titleResId })
        assertEquals(listOf("pinned"), sections[0].notes.map { it.id })
        assertEquals(listOf("newer", "older"), sections[1].notes.map { it.id })
    }

    @Test
    fun withoutHeadingsNewestEditsIsOneListWithPinnedFirst() {
        val sections = groupNotesForDisplay(
            listOf(older, pinned, newer),
            NotesSortMode.UPDATED_DESC,
            headings = false
        )

        assertEquals(1, sections.size)
        assertEquals(null, sections[0].titleResId)
        assertEquals(listOf("pinned", "newer", "older"), sections[0].notes.map { it.id })
    }

    @Test
    fun withoutHeadingsOtherSortsDropThePinnedHeadingToo() {
        NotesSortMode.entries.forEach { mode ->
            val sections = groupNotesForDisplay(listOf(older, newer, pinned), mode, headings = false)

            assertEquals("$mode", listOf(null), sections.map { it.titleResId })
            assertEquals("$mode", "pinned", sections[0].notes.first().id)
            assertEquals("$mode", 3, sections[0].notes.size)
        }
    }

    @Test
    fun noNotesMeansNoSectionsEitherWay() {
        assertTrue(groupNotesForDisplay(emptyList(), NotesSortMode.UPDATED_DESC).isEmpty())
        assertTrue(groupNotesForDisplay(emptyList(), NotesSortMode.TITLE_ASC, headings = false).isEmpty())
    }
}
