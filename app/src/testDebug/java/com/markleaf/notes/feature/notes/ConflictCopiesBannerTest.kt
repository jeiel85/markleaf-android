package com.markleaf.notes.feature.notes

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The note list's banner for waiting conflict copies (#434).
 *
 * The first version put the banner in the list as an item. On the emulator it
 * never showed: the count arrives after the list has laid out, and a row
 * inserted above the first visible one is scrolled out of view, because the
 * list keeps that row where it was. So the copies here arrive *after* the list
 * is on screen — the order a real launch has, and the one that hid it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class ConflictCopiesBannerTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val notes = (1..20).map { note("n$it", "Note $it", conflict = false) }
    private val copies = listOf(
        note("c1", "Note 1 (copy from another device 1007 10:01)", conflict = true),
        note("c2", "Note 2 (copy from another device 1007 10:02)", conflict = true)
    )

    @Test
    fun `copies that arrive after the list is drawn show a banner that opens the Conflict Center`() {
        val conflicts = MutableStateFlow<List<Note>>(emptyList())
        val viewModel = NotesViewModel(FakeNoteRepository(notes + copies, conflicts))
        var opened = 0
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = {},
                    onFabClick = {},
                    onConflictCopiesClick = { opened++ }
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Note 3").fetchSemanticsNodes().isNotEmpty()
        }

        conflicts.value = copies

        val text = composeRule.activity.resources
            .getQuantityString(R.plurals.conflict_copies_banner, 2, 2)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed().performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `no copies, no banner`() {
        val viewModel = NotesViewModel(FakeNoteRepository(notes, MutableStateFlow(emptyList())))
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                NotesListScreen(viewModel = viewModel, onNoteClick = {}, onFabClick = {})
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Note 3").fetchSemanticsNodes().isNotEmpty()
        }
        // Both English forms say "conflict cop…"; no note title here does.
        assertEquals(
            0,
            composeRule.onAllNodesWithText("conflict cop", substring = true)
                .fetchSemanticsNodes().size
        )
    }

    private fun note(id: String, title: String, conflict: Boolean) = Note(
        id = id,
        title = title,
        contentMarkdown = "# $title",
        excerpt = "",
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-10-01T00:00:00Z"),
        isConflictCopy = conflict
    )

    private class FakeNoteRepository(
        private val all: List<Note>,
        private val conflicts: Flow<List<Note>>
    ) : NoteRepository {
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
        override fun observeConflictNotes(): Flow<List<Note>> = conflicts
    }
}
