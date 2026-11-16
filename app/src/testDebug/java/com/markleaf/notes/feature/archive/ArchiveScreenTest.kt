package com.markleaf.notes.feature.archive

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.ArchiveViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The archive's sort menu and row dates (#592). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class ArchiveScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rowsShowTheSameDateAsTheNotesList() {
        val repository = FakeNoteRepository()
        setArchiveContent(repository)
        // Older than a week, so the row spells the date out.
        repository.archived.value = listOf(
            archivedNote("a", "Trip plan", LocalDate.of(2024, 3, 9))
        )

        waitForRow("Trip plan")
        composeRule.onNodeWithText("2024.03.09").assertIsDisplayed()
    }

    @Test
    fun sortMenuOffersEveryModeAndReportsTheChoice() {
        val chosen = mutableListOf<NotesSortMode>()
        val repository = FakeNoteRepository()
        setArchiveContent(repository, onSortModeChange = { chosen += it })
        repository.archived.value = listOf(archivedNote("a", "Alpha", LocalDate.of(2024, 1, 1)))
        waitForRow("Alpha")

        openSortMenu()
        val titleAz = composeRule.activity.getString(R.string.sort_title_az)
        composeRule.onNodeWithText(titleAz).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sort_created_oldest))
            .assertIsDisplayed()
        composeRule.onNodeWithText(titleAz).performClick()

        assertEquals(listOf(NotesSortMode.TITLE_ASC), chosen)
    }

    @Test
    fun theChosenModeOrdersTheRows() {
        val repository = FakeNoteRepository()
        var mode by mutableStateOf(NotesSortMode.UPDATED_DESC)
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                ArchiveScreen(
                    viewModel = ArchiveViewModel(repository),
                    sortMode = mode,
                    onSortModeChange = { mode = it }
                )
            }
        }
        repository.archived.value = listOf(
            archivedNote("c", "Cherry", LocalDate.of(2024, 1, 3)),
            archivedNote("a", "apple", LocalDate.of(2024, 1, 1)),
            archivedNote("b", "Banana", LocalDate.of(2024, 1, 2))
        )
        waitForRow("Cherry")
        assertEquals(listOf("Cherry", "Banana", "apple"), rowTitlesTopToBottom())

        openSortMenu()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sort_title_az))
            .performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("apple", "Banana", "Cherry"), rowTitlesTopToBottom())
    }

    /**
     * Rows are keyed by note id, so without the reset the list would follow
     * whichever note was at the top to its new place. Reversing a long archive
     * from the top would then open at its bottom.
     */
    @Test
    fun changingTheSortStartsAtTheTop() {
        val repository = FakeNoteRepository()
        var mode by mutableStateOf(NotesSortMode.UPDATED_DESC)
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                ArchiveScreen(
                    viewModel = ArchiveViewModel(repository),
                    sortMode = mode,
                    onSortModeChange = { mode = it }
                )
            }
        }
        val start = LocalDate.of(2024, 1, 1)
        repository.archived.value = (1..40).map { day ->
            archivedNote("n$day", "Note %02d".format(day), start.plusDays(day.toLong()))
        }
        waitForRow("Note 40")

        mode = NotesSortMode.UPDATED_ASC
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Note 01").assertIsDisplayed()
        composeRule.onNodeWithText("Note 40").assertDoesNotExist()
    }

    /** The archive shows no pin, so a pinned note gets no head start in it. */
    @Test
    fun aPinnedNoteIsSortedLikeAnyOther() {
        val repository = FakeNoteRepository()
        setArchiveContent(repository)
        repository.archived.value = listOf(
            archivedNote("old", "Old pinned", LocalDate.of(2024, 1, 1), pinned = true),
            archivedNote("new", "New", LocalDate.of(2024, 2, 1))
        )
        waitForRow("New")

        assertEquals(listOf("New", "Old pinned"), rowTitlesTopToBottom())
    }

    private fun setArchiveContent(
        repository: FakeNoteRepository,
        onSortModeChange: (NotesSortMode) -> Unit = {}
    ) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                ArchiveScreen(
                    viewModel = ArchiveViewModel(repository),
                    onSortModeChange = onSortModeChange
                )
            }
        }
    }

    private fun waitForRow(title: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSortMenu() {
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.sort_notes))
            .performClick()
    }

    private fun rowTitlesTopToBottom(): List<String> {
        val titles = listOf("Cherry", "Banana", "apple", "Alpha", "New", "Old pinned")
        return titles
            .mapNotNull { title ->
                composeRule.onAllNodesWithText(title).fetchSemanticsNodes().firstOrNull()
                    ?.let { title to it.boundsInRoot.top }
            }
            .sortedBy { it.second }
            .map { it.first }
            .also { assertTrue("No rows found", it.isNotEmpty()) }
    }

    private fun archivedNote(
        id: String,
        title: String,
        editedOn: LocalDate,
        pinned: Boolean = false
    ): Note {
        val at = editedOn.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
        return Note(
            id = id,
            title = title,
            contentMarkdown = "# $title",
            excerpt = "",
            createdAt = at,
            updatedAt = at,
            pinned = pinned,
            archived = true
        )
    }

    private class FakeNoteRepository : NoteRepository {
        // Starts empty and gets its rows after the first frame, the way Room
        // delivers them; a list that is there before the first frame hides
        // ordering and scroll bugs.
        val archived = MutableStateFlow<List<Note>>(emptyList())

        override fun observeNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun getNote(noteId: String): Note? = null
        override fun observeNote(noteId: String): Flow<Note?> = flowOf(null)
        override suspend fun getAllNotes(): List<Note> = emptyList()
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun updateDerivedTitle(
            noteId: String,
            title: String,
            excerpt: String
        ) = Unit
        override suspend fun moveToTrash(noteId: String) = Unit
        override suspend fun setPinned(noteId: String, pinned: Boolean) = Unit
        override suspend fun setArchived(noteId: String, archived: Boolean) = Unit
        override suspend fun restoreFromTrash(noteId: String) = Unit
        override suspend fun deleteForever(noteId: String) = Unit
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeArchivedNotes(): Flow<List<Note>> = archived
        override fun observeLockedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun setLocked(noteId: String, locked: Boolean) = Unit
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
