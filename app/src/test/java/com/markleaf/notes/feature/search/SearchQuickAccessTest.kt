package com.markleaf.notes.feature.search

import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQuickAccessTest {

    private fun note(id: String, title: String, updatedAt: Long) = Note(
        id = id,
        title = title,
        contentMarkdown = title,
        excerpt = "",
        createdAt = Instant.ofEpochMilli(updatedAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )

    @Test
    fun recentNotesAreNewestFirstAndSkipUntitled() {
        val notes = listOf(
            note("old", "Old note", updatedAt = 1_000),
            note("untitled", "", updatedAt = 9_000),
            note("new", "New note", updatedAt = 5_000)
        )

        assertEquals(listOf("new", "old"), recentNotesForSearch(notes).map { it.id })
    }

    @Test
    fun recentNotesAreCappedAtTwenty() {
        val notes = (1..30).map { note("n$it", "Note $it", updatedAt = it.toLong()) }
        assertEquals(20, recentNotesForSearch(notes).size)
    }

    @Test
    fun titleFilterMatchesSubstringsCaseInsensitively() {
        val notes = listOf(
            note("groceries", "Groceries list", updatedAt = 1_000),
            note("meeting", "Meeting notes", updatedAt = 2_000),
            note("body-only", "Journal", updatedAt = 3_000)
        )

        val matches = filterNotesByTitle(notes, "gro")
        assertEquals(listOf("groceries"), matches.map { it.id })
    }

    @Test
    fun titleFilterOrdersMatchesByMostRecentEdit() {
        val notes = listOf(
            note("a", "Plan A", updatedAt = 1_000),
            note("b", "Plan B", updatedAt = 3_000),
            note("c", "Plan C", updatedAt = 2_000)
        )

        assertEquals(listOf("b", "c", "a"), filterNotesByTitle(notes, "plan").map { it.id })
    }

    @Test
    fun titleFilterIgnoresSurroundingWhitespaceAndBlankQueries() {
        val notes = listOf(note("a", "Plan A", updatedAt = 1_000))

        assertEquals(listOf("a"), filterNotesByTitle(notes, "  plan  ").map { it.id })
        assertTrue(filterNotesByTitle(notes, "   ").isEmpty())
    }
}
