package com.markleaf.notes.feature.trash

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
import com.markleaf.notes.ui.viewmodel.TrashViewModel
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression tests for #298: with a long note title the trash row used to give
 * the title the whole width, squeezing the Recover/Delete actions into the last
 * ~150dp of the row — overlapped by the title and easy to miss or tap wrong.
 * Also covers the "Empty trash" action (#566).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class TrashScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longTitleKeepsRecoverAndDeleteVisible() {
        val longTitle = "A very long note title that must never fit on one phone row ".repeat(4)
        val viewModel = TrashViewModel(FakeNoteRepository(longTitleNote(longTitle)))

        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                TrashScreen(viewModel = viewModel, onBack = {})
            }
        }

        // Wait for the ViewModel flow to be collected and the row composed.
        val deleteText = composeRule.activity.getString(R.string.delete)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(deleteText).fetchSemanticsNodes().isNotEmpty()
        }

        val restoreText = composeRule.activity.getString(R.string.restore)
        composeRule.onNodeWithText(restoreText).assertIsDisplayed()
        composeRule.onNodeWithText(deleteText).assertIsDisplayed()

        // The title must end where the actions begin, not underneath them.
        // Before the fix it measured to the full row width and the buttons were
        // pushed into the row's tail, overlapped by the title text.
        val titleBounds = composeRule
            .onNodeWithText(longTitle, substring = true)
            .fetchSemanticsNode()
            .boundsInRoot
        val restoreBounds = composeRule.onNodeWithText(restoreText)
            .fetchSemanticsNode()
            .boundsInRoot
        val deleteBounds = composeRule.onNodeWithText(deleteText)
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "Title must not overlap the Recover button (title.right=${titleBounds.right}, recover.left=${restoreBounds.left})",
            titleBounds.right <= restoreBounds.left + 0.1f
        )
        assertTrue(
            "Recover must sit left of Delete (recover.right=${restoreBounds.right}, delete.left=${deleteBounds.left})",
            restoreBounds.right <= deleteBounds.left + 0.1f
        )
    }

    // #566: emptying the trash asks first, says how many notes go, and then
    // deletes exactly the notes that were listed.
    @Test
    fun emptyTrashConfirmsThenDeletesEveryListedNote() {
        val repository = FakeNoteRepository(
            listOf(trashedNote("a", "First"), trashedNote("b", "Second"), trashedNote("c", "Third"))
        )
        setTrashContent(repository)

        val emptyTrash = composeRule.activity.getString(R.string.empty_trash)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyTrash).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(emptyTrash).performClick()

        val message = composeRule.activity.resources
            .getQuantityString(R.plurals.empty_trash_message, 3, 3)
        composeRule.onNodeWithText(message).assertIsDisplayed()
        assertTrue("Nothing is deleted before confirming", repository.deletedForever.isEmpty())

        // The confirm button carries the same label as the top-bar action.
        composeRule.onAllNodesWithText(emptyTrash)[1].performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("a", "b", "c"), repository.deletedForever.sorted())
    }

    @Test
    fun cancellingEmptyTrashDeletesNothing() {
        val repository = FakeNoteRepository(listOf(trashedNote("a", "First"), trashedNote("b", "Second")))
        setTrashContent(repository)

        val emptyTrash = composeRule.activity.getString(R.string.empty_trash)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyTrash).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(emptyTrash).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.cancel)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.empty_trash_title))
            .assertDoesNotExist()
        assertTrue(repository.deletedForever.isEmpty())
    }

    @Test
    fun emptyTrashIsHiddenWhenTrashIsEmpty() {
        setTrashContent(FakeNoteRepository(emptyList()))

        val emptyHint = composeRule.activity.getString(R.string.trash_empty)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyHint).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.empty_trash))
            .assertDoesNotExist()
    }

    private fun setTrashContent(repository: FakeNoteRepository) {
        val viewModel = TrashViewModel(repository)
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                TrashScreen(viewModel = viewModel, onBack = {})
            }
        }
    }

    private fun trashedNote(id: String, title: String) = Note(
        id = id,
        title = title,
        contentMarkdown = "body",
        excerpt = "",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        trashed = true,
        deletedAt = Instant.now()
    )

    private fun longTitleNote(title: String) = Note(
        id = "trashed-long-title",
        title = title,
        contentMarkdown = "body",
        excerpt = "An excerpt that is also fairly long",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        trashed = true,
        deletedAt = Instant.now()
    )

    private class FakeNoteRepository(private val notes: List<Note>) : NoteRepository {
        constructor(note: Note) : this(listOf(note))

        val deletedForever = mutableListOf<String>()

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
        override suspend fun deleteForever(noteId: String) {
            deletedForever += noteId
        }
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(notes)
        override fun observeArchivedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeLockedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun setLocked(noteId: String, locked: Boolean) = Unit
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
