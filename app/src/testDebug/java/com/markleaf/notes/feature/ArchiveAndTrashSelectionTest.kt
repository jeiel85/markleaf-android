package com.markleaf.notes.feature

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.feature.archive.ArchiveScreen
import com.markleaf.notes.feature.trash.TrashScreen
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.ArchiveViewModel
import com.markleaf.notes.ui.viewmodel.TrashViewModel
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The archive's and the trash's selection (#596). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class ArchiveAndTrashSelectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(id: Int) = composeRule.activity.getString(id)
    private fun plural(id: Int, n: Int) = composeRule.activity.resources.getQuantityString(id, n, n)

    @Test
    fun `the archive unarchives picked notes together`() {
        val repository = FakeNoteRepository(
            listOf(archived("a", "Alpha"), archived("b", "Bravo"), archived("c", "Charlie"))
        )
        val opened = mutableListOf<String>()
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                ArchiveScreen(viewModel = ArchiveViewModel(repository), onNoteClick = { opened += it })
            }
        }
        waitForText("Alpha")

        composeRule.onNodeWithText("Alpha").performTouchInput { longClick() }
        composeRule.onNodeWithText(str(R.string.select)).performClick()
        composeRule.onNodeWithText("Charlie").performClick()
        composeRule.onNodeWithText(plural(R.plurals.notes_selected, 2)).assertExists()
        assertTrue(opened.isEmpty())

        composeRule.onNodeWithContentDescription(str(R.string.unarchive)).performClick()
        composeRule.waitUntil(5_000) { repository.ids { it.archived } == setOf("b") }
        composeRule.onNodeWithText(str(R.string.archive)).assertExists()
    }

    @Test
    fun `the archive moves picked notes to the trash and Undo restores them`() {
        val repository = FakeNoteRepository(listOf(archived("a", "Alpha"), archived("b", "Bravo")))
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                ArchiveScreen(viewModel = ArchiveViewModel(repository))
            }
        }
        waitForText("Alpha")

        composeRule.onNodeWithText("Alpha").performTouchInput { longClick() }
        composeRule.onNodeWithText(str(R.string.select)).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.select_all)).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.move_to_trash)).performClick()
        waitForText(plural(R.plurals.notes_moved_to_trash, 2))
        assertEquals(setOf("a", "b"), repository.ids { it.trashed })

        composeRule.onNodeWithText(str(R.string.undo)).performClick()
        composeRule.waitUntil(5_000) { repository.ids { it.trashed }.isEmpty() }
    }

    @Test
    fun `a long press in the trash starts a selection that restores together`() {
        val repository = FakeNoteRepository(listOf(trashed("a", "Alpha"), trashed("b", "Bravo"), trashed("c", "Charlie")))
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                TrashScreen(viewModel = TrashViewModel(repository))
            }
        }
        waitForText("Alpha")
        // Outside a selection a tap does nothing, as before.
        composeRule.onNodeWithText("Alpha").performClick()
        composeRule.onNodeWithText(str(R.string.trash)).assertExists()

        composeRule.onNodeWithText("Alpha").performTouchInput { longClick() }
        composeRule.onNodeWithText(plural(R.plurals.notes_selected, 1)).assertExists()
        // The rows' own buttons give way to the bar's.
        composeRule.onNodeWithText(str(R.string.restore)).assertDoesNotExist()
        composeRule.onNodeWithText("Bravo").performClick()

        composeRule.onNodeWithContentDescription(str(R.string.restore)).performClick()
        composeRule.waitUntil(5_000) { repository.ids { it.trashed } == setOf("c") }
    }

    @Test
    fun `deleting picked notes forever asks first, counting them`() {
        val repository = FakeNoteRepository(listOf(trashed("a", "Alpha"), trashed("b", "Bravo"), trashed("c", "Charlie")))
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                TrashScreen(viewModel = TrashViewModel(repository))
            }
        }
        waitForText("Alpha")

        composeRule.onNodeWithText("Alpha").performTouchInput { longClick() }
        composeRule.onNodeWithText("Bravo").performClick()
        composeRule.onNodeWithContentDescription(str(R.string.delete_forever)).performClick()
        composeRule.onNodeWithText(plural(R.plurals.delete_notes_forever_message, 2)).assertExists()
        assertTrue(repository.deletedForever.isEmpty())

        composeRule.onNodeWithText(str(R.string.delete_forever)).performClick()
        composeRule.waitUntil(5_000) { repository.deletedForever.toSet() == setOf("a", "b") }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun note(id: String, title: String) = Note(
        id = id,
        title = title,
        contentMarkdown = "# $title",
        excerpt = "",
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-10-01T00:00:00Z")
    )

    private fun archived(id: String, title: String) = note(id, title).copy(archived = true)
    private fun trashed(id: String, title: String) =
        note(id, title).copy(trashed = true, deletedAt = Instant.parse("2026-10-02T00:00:00Z"))

    /** Keeps the notes and their flags, so each screen redraws as with Room. */
    private class FakeNoteRepository(notes: List<Note>) : NoteRepository {
        private val all = MutableStateFlow(notes)
        val deletedForever = mutableListOf<String>()

        fun ids(where: (Note) -> Boolean) = all.value.filter(where).map { it.id }.toSet()

        private fun edit(noteId: String, change: (Note) -> Note) {
            all.value = all.value.map { if (it.id == noteId) change(it) else it }
        }

        override fun observeNotes(): Flow<List<Note>> =
            all.map { l -> l.filter { !it.trashed && !it.archived && !it.locked } }
        override suspend fun getNote(noteId: String): Note? = all.value.firstOrNull { it.id == noteId }
        override fun observeNote(noteId: String): Flow<Note?> = all.map { l -> l.firstOrNull { it.id == noteId } }
        override suspend fun getAllNotes(): List<Note> = all.value
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun updateDerivedTitle(noteId: String, title: String, excerpt: String) = Unit
        override suspend fun moveToTrash(noteId: String) = edit(noteId) { it.copy(trashed = true) }
        override suspend fun setPinned(noteId: String, pinned: Boolean) = edit(noteId) { it.copy(pinned = pinned) }
        override suspend fun setArchived(noteId: String, archived: Boolean) = edit(noteId) { it.copy(archived = archived) }
        override suspend fun restoreFromTrash(noteId: String) = edit(noteId) { it.copy(trashed = false) }
        override suspend fun deleteForever(noteId: String) {
            deletedForever += noteId
            all.value = all.value.filter { it.id != noteId }
        }
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        // As in Room: a trashed note leaves the archive even if still flagged archived.
        override fun observeTrashedNotes(): Flow<List<Note>> = all.map { l -> l.filter { it.trashed } }
        override fun observeArchivedNotes(): Flow<List<Note>> =
            all.map { l -> l.filter { it.archived && !it.trashed } }
        override fun observeLockedNotes(): Flow<List<Note>> = all.map { l -> l.filter { it.locked } }
        override suspend fun setLocked(noteId: String, locked: Boolean) = edit(noteId) { it.copy(locked = locked) }
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
