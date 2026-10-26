package com.markleaf.notes.feature.notes

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.data.settings.NotesLayout
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A list filtered to a tag on a phone (#545) shows that tag and a way back to
 * all notes — phones have no tag rail to show either.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class SelectedTagChipTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val notes = listOf(
        note("j", "Morning pages", "#journal"),
        note("s", "Exam plan", "#study")
    )

    @Test
    fun `a filtered phone list names its tag and clears it`() {
        val viewModel = NotesViewModel(FakeNoteRepository(notes))
        viewModel.selectTag("journal")
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                val selectedTag by viewModel.selectedTag.collectAsState()
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = {},
                    onFabClick = {},
                    selectedTag = selectedTag,
                    onClearTag = { viewModel.selectTag(null) }
                )
            }
        }
        waitForText("Morning pages")
        composeRule.onNodeWithText("#journal").assertExists()
        composeRule.onNodeWithText("Exam plan").assertDoesNotExist()

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.show_all_notes)
        ).performClick()

        waitForText("Exam plan")
        composeRule.onNodeWithText("#journal").assertDoesNotExist()
    }

    @Test
    fun `clearing the filter starts the list at the top`() {
        // Seen on the emulator: the list kept "Older" at the top after the
        // filter was cleared, so the pinned notes above it stayed off screen.
        clearingTheFilterShowsThePinnedNote(NotesLayout.LIST)
    }

    @Test
    fun `clearing the filter starts the grid at the top`() {
        // #555 review: resetting the list first waited on a list that the grid
        // layout never lays out, so the grid was never reset.
        clearingTheFilterShowsThePinnedNote(NotesLayout.GRID)
    }

    private fun clearingTheFilterShowsThePinnedNote(layout: NotesLayout) {
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking { settings.setNotesLayout(layout) }
        val many = listOf(note("p", "Pinned guide", "#guide").copy(pinned = true)) +
            (1..12).map { note("n$it", "Filler $it", "#misc") } +
            note("j", "Morning pages", "#journal")
        val viewModel = NotesViewModel(FakeNoteRepository(many))
        viewModel.selectTag("journal")
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                val selectedTag by viewModel.selectedTag.collectAsState()
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = {},
                    onFabClick = {},
                    selectedTag = selectedTag,
                    onClearTag = { viewModel.selectTag(null) },
                    settingsRepository = settings
                )
            }
        }
        waitForText("Morning pages")

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.show_all_notes)
        ).performClick()

        waitForText("Filler 1")
        composeRule.onNodeWithText("Pinned guide").assertIsDisplayed()
    }

    @Test
    fun `no filter, no chip`() {
        val viewModel = NotesViewModel(FakeNoteRepository(notes))
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = {},
                    onFabClick = {},
                    onClearTag = { viewModel.selectTag(null) }
                )
            }
        }
        waitForText("Exam plan")
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.show_all_notes)
        ).assertDoesNotExist()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun note(id: String, title: String, tag: String) = Note(
        id = id,
        title = title,
        contentMarkdown = "# $title\n\n$tag",
        excerpt = "",
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-10-01T00:00:00Z")
    )

    private class FakeNoteRepository(private val all: List<Note>) : NoteRepository {
        override fun observeNotes(): Flow<List<Note>> = flowOf(all)
        override suspend fun getNote(noteId: String): Note? = all.firstOrNull { it.id == noteId }
        override fun observeNote(noteId: String): Flow<Note?> = flowOf(all.firstOrNull { it.id == noteId })
        override suspend fun getAllNotes(): List<Note> = all
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun updateDerivedTitle(noteId: String, title: String, excerpt: String) = Unit
        override suspend fun moveToTrash(noteId: String) = Unit
        override suspend fun setPinned(noteId: String, pinned: Boolean) = Unit
        override suspend fun setArchived(noteId: String, archived: Boolean) = Unit
        override suspend fun restoreFromTrash(noteId: String) = Unit
        override suspend fun deleteForever(noteId: String) = Unit
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeArchivedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeLockedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun setLocked(noteId: String, locked: Boolean) = Unit
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
