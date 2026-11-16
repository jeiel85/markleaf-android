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

    // Created order must ignore later edits (#537): "old" was created first but edited last.
    private fun createdAndEdited(id: String, created: Long, edited: Long) = note(id, id, edited)
        .copy(createdAt = Instant.ofEpochMilli(created))

    @Test
    fun createdOrdersIgnoreLaterEdits() {
        val old = createdAndEdited("old", created = 1_000, edited = 9_000)
        val mid = createdAndEdited("mid", created = 2_000, edited = 3_000)
        val new = createdAndEdited("new", created = 3_000, edited = 4_000)
        assertEquals(
            listOf("new", "mid", "old"),
            sortNotesForDisplay(listOf(old, mid, new), NotesSortMode.CREATED_DESC).map { it.id }
        )
        assertEquals(
            listOf("old", "mid", "new"),
            sortNotesForDisplay(listOf(new, old, mid), NotesSortMode.CREATED_ASC).map { it.id }
        )
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

    // The archive sorts by the same modes but shows no pin, so a pin is no head start (#592).
    @Test
    fun archiveIgnoresPinsInEveryMode() {
        val pinnedZebra = note("zebra", "Zebra", updatedAt = 500, pinned = true)
        val notes = listOf(apple, banana, pinnedZebra, cherry)
        for (mode in NotesSortMode.entries) {
            assertEquals(
                "mode $mode",
                notes.sortedWith(mode.comparator()).map { it.id },
                sortArchivedNotes(notes, mode).map { it.id }
            )
        }
        assertEquals(
            listOf("apple", "banana", "cherry", "zebra"),
            sortArchivedNotes(notes, NotesSortMode.TITLE_ASC).map { it.id }
        )
    }

    // Created order ignores later edits in the archive too (#537, #592).
    @Test
    fun archiveCreatedOrderIgnoresLaterEdits() {
        val old = createdAndEdited("old", created = 1_000, edited = 9_000)
        val new = createdAndEdited("new", created = 3_000, edited = 4_000)
        assertEquals(
            listOf("old", "new"),
            sortArchivedNotes(listOf(new, old), NotesSortMode.CREATED_ASC).map { it.id }
        )
    }
}
