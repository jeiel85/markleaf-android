package com.markleaf.notes.feature.notes

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.NotesViewModel
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

/** Selecting several notes in the notes list and acting on them at once (#596). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class NotesListSelectionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun str(id: Int) = composeRule.activity.getString(id)
    private fun plural(id: Int, n: Int) = composeRule.activity.resources.getQuantityString(id, n, n)

    @Test
    fun `picked notes go to the trash together and Undo brings them back`() {
        val repository = FakeNoteRepository(listOf(note("a", "Alpha"), note("b", "Bravo"), note("c", "Charlie")))
        val opened = mutableListOf<String?>()
        show(repository, onNoteClick = { opened += it })

        startSelectionOn("Alpha")
        composeRule.onNodeWithText(plural(R.plurals.notes_selected, 1)).assertExists()
        // While selecting, a tap picks the note instead of opening it.
        composeRule.onNodeWithText("Bravo").performClick()
        composeRule.onNodeWithText(plural(R.plurals.notes_selected, 2)).assertExists()
        assertTrue(opened.isEmpty())

        composeRule.onNodeWithContentDescription(str(R.string.move_to_trash)).performClick()
        waitForText(plural(R.plurals.notes_moved_to_trash, 2))
        assertEquals(setOf("a", "b"), repository.trashedIds())
        composeRule.onNodeWithText("Charlie").assertExists()
        // The bar is gone once the action has used the selection.
        composeRule.onNodeWithText(str(R.string.notes_title)).assertExists()

        composeRule.onNodeWithText(str(R.string.undo)).performClick()
        waitForText("Alpha")
        composeRule.waitForIdle()
        assertEquals(emptySet<String>(), repository.trashedIds())
        composeRule.onNodeWithText("Bravo").assertExists()
    }

    @Test
    fun `a mixed selection is pinned, an all-pinned one is unpinned`() {
        val repository = FakeNoteRepository(listOf(note("a", "Alpha").copy(pinned = true), note("b", "Bravo")))
        show(repository)

        startSelectionOn("Alpha")
        composeRule.onNodeWithText("Bravo").performClick()
        composeRule.onNodeWithContentDescription(str(R.string.pin)).performClick()
        composeRule.waitUntil(5_000) { repository.pinnedIds() == setOf("a", "b") }

        startSelectionOn("Alpha")
        composeRule.onNodeWithText("Bravo").performClick()
        composeRule.onNodeWithContentDescription(str(R.string.unpin)).performClick()
        composeRule.waitUntil(5_000) { repository.pinnedIds().isEmpty() }
    }

    @Test
    fun `the overflow menu starts an empty selection and Select all picks every note`() {
        val repository = FakeNoteRepository(listOf(note("a", "Alpha"), note("b", "Bravo")))
        show(repository)

        composeRule.onNodeWithContentDescription(str(R.string.more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.select_notes)).performClick()
        // Nothing picked yet: the bar says so and its actions wait.
        composeRule.onNodeWithText(str(R.string.select_notes)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.archive_action)).assertIsNotEnabled()

        composeRule.onNodeWithContentDescription(str(R.string.more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.select_all)).performClick()
        composeRule.onNodeWithText(plural(R.plurals.notes_selected, 2)).assertExists()

        composeRule.onNodeWithContentDescription(str(R.string.archive_action)).performClick()
        composeRule.waitUntil(5_000) { repository.archivedIds() == setOf("a", "b") }
    }

    @Test
    fun `Back and Close end the selection without acting`() {
        val repository = FakeNoteRepository(listOf(note("a", "Alpha"), note("b", "Bravo")))
        show(repository)

        startSelectionOn("Alpha")
        // The back callback is enabled by the recomposition the tap schedules.
        composeRule.waitForIdle()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText(str(R.string.notes_title)).assertExists()

        startSelectionOn("Alpha")
        composeRule.onNodeWithContentDescription(str(R.string.clear_selection)).performClick()
        composeRule.onNodeWithText(str(R.string.notes_title)).assertExists()
        assertTrue(repository.trashedIds().isEmpty())
    }

    @Test
    fun `a picked note reads as selected`() {
        show(FakeNoteRepository(listOf(note("a", "Alpha"), note("b", "Bravo"))))

        startSelectionOn("Alpha")
        composeRule.onNodeWithText("Alpha").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule.onNodeWithText("Bravo").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun `locking without a passcode asks for one and keeps the selection`() {
        val repository = FakeNoteRepository(listOf(note("a", "Alpha")))
        show(repository, lockPasscodeSet = false)

        startSelectionOn("Alpha")
        composeRule.onNodeWithContentDescription(str(R.string.more_options)).performClick()
        composeRule.onNodeWithText(str(R.string.move_to_locked)).performClick()
        composeRule.onNodeWithText(str(R.string.locked_need_passcode_title)).assertExists()
        composeRule.onNodeWithText(str(R.string.cancel)).performClick()
        composeRule.onNodeWithText(plural(R.plurals.notes_selected, 1)).assertExists()
        assertTrue(repository.lockedIds().isEmpty())
    }

    @Test
    fun `one note moved to the trash from its menu also offers Undo`() {
        val repository = FakeNoteRepository(listOf(note("a", "Alpha"), note("b", "Bravo")))
        show(repository)

        composeRule.onNodeWithText("Alpha").performTouchInput { longClick() }
        composeRule.onNodeWithText(str(R.string.move_to_trash)).performClick()
        waitForText(plural(R.plurals.notes_moved_to_trash, 1))
        assertEquals(setOf("a"), repository.trashedIds())
    }

    private fun show(
        repository: FakeNoteRepository,
        onNoteClick: (String?) -> Unit = {},
        lockPasscodeSet: Boolean = true
    ) {
        val viewModel = NotesViewModel(repository)
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = onNoteClick,
                    onFabClick = {},
                    lockPasscodeSet = lockPasscodeSet
                )
            }
        }
        waitForText(repository.titles().first())
    }

    private fun startSelectionOn(title: String) {
        composeRule.onNodeWithText(title).performTouchInput { longClick() }
        composeRule.onNodeWithText(str(R.string.select)).performClick()
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

    /** Keeps the notes and their flags, so the list redraws as a real one would. */
    private class FakeNoteRepository(notes: List<Note>) : NoteRepository {
        private val all = MutableStateFlow(notes)

        fun titles() = all.value.map { it.title }
        fun trashedIds() = all.value.filter { it.trashed }.map { it.id }.toSet()
        fun pinnedIds() = all.value.filter { it.pinned }.map { it.id }.toSet()
        fun archivedIds() = all.value.filter { it.archived }.map { it.id }.toSet()
        fun lockedIds() = all.value.filter { it.locked }.map { it.id }.toSet()

        private fun edit(noteId: String, change: (Note) -> Note) {
            all.value = all.value.map { if (it.id == noteId) change(it) else it }
        }

        override fun observeNotes(): Flow<List<Note>> =
            all.map { list -> list.filter { !it.trashed && !it.archived && !it.locked } }
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
        override suspend fun deleteForever(noteId: String) = Unit
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = all.map { l -> l.filter { it.trashed } }
        override fun observeArchivedNotes(): Flow<List<Note>> = all.map { l -> l.filter { it.archived } }
        override fun observeLockedNotes(): Flow<List<Note>> = all.map { l -> l.filter { it.locked } }
        override suspend fun setLocked(noteId: String, locked: Boolean) = edit(noteId) { it.copy(locked = locked) }
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
