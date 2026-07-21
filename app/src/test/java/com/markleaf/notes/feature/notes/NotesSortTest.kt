package com.markleaf.notes.feature.notes

import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesSortTest {

    private fun note(
        id: String,
        title: String,
        updatedAt: Long,
        pinned: Boolean = false
    ) = Note(
        id = id,
        title = title,
        contentMarkdown = title,
        excerpt = "",
        createdAt = Instant.ofEpochMilli(updatedAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        pinned = pinned
    )

    private val banana = note("banana", "Banana", updatedAt = 3_000)
    private val apple = note("apple", "apple", updatedAt = 1_000)
    private val cherry = note("cherry", "Cherry", updatedAt = 2_000)

    @Test
    fun updatedDescOrdersNewestFirst() {
        val sorted = sortNotesForDisplay(listOf(apple, banana, cherry), NotesSortMode.UPDATED_DESC)
        assertEquals(listOf("banana", "cherry", "apple"), sorted.map { it.id })
    }

    @Test
    fun updatedAscOrdersOldestFirst() {
        val sorted = sortNotesForDisplay(listOf(apple, banana, cherry), NotesSortMode.UPDATED_ASC)
        assertEquals(listOf("apple", "cherry", "banana"), sorted.map { it.id })
    }

    @Test
    fun titleAscIsCaseInsensitive() {
        val sorted = sortNotesForDisplay(listOf(cherry, banana, apple), NotesSortMode.TITLE_ASC)
        assertEquals(listOf("apple", "banana", "cherry"), sorted.map { it.id })
    }

    @Test
    fun titleDescReversesTheAlphabet() {
        val sorted = sortNotesForDisplay(listOf(apple, cherry, banana), NotesSortMode.TITLE_DESC)
        assertEquals(listOf("cherry", "banana", "apple"), sorted.map { it.id })
    }

    @Test
    fun pinnedNotesStayFirstInEveryMode() {
        val pinnedZebra = note("zebra", "Zebra", updatedAt = 500, pinned = true)
        for (mode in NotesSortMode.entries) {
            val sorted = sortNotesForDisplay(listOf(apple, banana, pinnedZebra, cherry), mode)
            assertEquals("mode $mode should keep the pinned note first", "zebra", sorted.first().id)
        }
    }

    @Test
    fun pinnedGroupIsSortedByTheSameMode() {
        val pinnedA = note("pa", "Alpha", updatedAt = 100, pinned = true)
        val pinnedB = note("pb", "Beta", updatedAt = 200, pinned = true)
        val sorted = sortNotesForDisplay(listOf(pinnedA, pinnedB, apple), NotesSortMode.UPDATED_DESC)
        assertEquals(listOf("pb", "pa", "apple"), sorted.map { it.id })
    }
}
