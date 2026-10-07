package com.markleaf.notes.feature.sync

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.SyncCenterViewModel
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The note list's conflict-copy banner leads to the Sync Center scrolled to its
 * Conflict Center (#434). The folder card above it fills most of a phone
 * screen, so arriving at the top would show the reason you came only after a
 * scroll you weren't told to make.
 *
 * Enough copies are listed for the content to be taller than the screen;
 * otherwise there is nowhere to scroll and the landing can't be told apart from
 * an ordinary open. They arrive only after the screen has drawn, as they do
 * from Room on a device: the first version scrolled on entry, was clamped to
 * the short list it found, and stayed at the top — and a test that handed the
 * copies over before the first frame passed regardless.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class SyncCenterConflictLandingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun open(startAtConflicts: Boolean) {
        val conflicts = MutableStateFlow<List<Note>>(emptyList())
        val viewModel = SyncCenterViewModel(FakeNoteRepository(conflicts))
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                SyncCenterScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onNoteClick = {},
                    startAtConflicts = startAtConflicts
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.sync_title))
                .fetchSemanticsNodes().isNotEmpty()
        }
        conflicts.value = copies(12)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Copy 1", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `entering from the banner lands on the Conflict Center`() {
        open(startAtConflicts = true)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.conflict_center_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sync_title))
            .assertIsNotDisplayed()
    }

    @Test
    fun `entering from Settings still starts at the folder card`() {
        open(startAtConflicts = false)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.sync_title))
            .assertIsDisplayed()
    }

    private fun copies(n: Int): List<Note> = (1..n).map { i ->
        Note(
            id = "c$i",
            title = "Copy $i (copy from another device 1007 10:0$i)",
            contentMarkdown = "body $i",
            excerpt = "body $i",
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            isConflictCopy = true
        )
    }

    private class FakeNoteRepository(private val conflicts: Flow<List<Note>>) : NoteRepository {
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
        override fun observeArchivedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeLockedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun setLocked(noteId: String, locked: Boolean) = Unit
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = conflicts
    }
}
