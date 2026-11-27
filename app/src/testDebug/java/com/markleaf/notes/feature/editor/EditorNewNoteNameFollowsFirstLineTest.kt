package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Under the File name title rule (D088) a new note's name follows its first
 * line until it is named — across every autosave of the visit, not only the
 * first. The debounced saver keeps the save callback of the composition that
 * made it, from before the load knew the note was new; a value captured there
 * froze the name at the first pause, at "Gro" (Codex on #613).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorNewNoteNameFollowsFirstLineTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    private fun field() = composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))

    @Test
    fun aPauseMidwayDoesNotFreezeTheNewNotesName() {
        val noteId = UUID.randomUUID().toString()
        val now = Instant.now()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "",
                    contentMarkdown = "",
                    excerpt = "",
                    // One clock read: a just-created note has createdAt == updatedAt.
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking {
            settings.beginRetitle(NoteTitleSource.FILE_NAME)
            settings.setRetitlePending(false)
        }
        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitForIdle()

        field().performTextInput("Gro")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId) }?.contentMarkdown == "Gro"
        }
        field().performTextInput("ceries")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId) }?.contentMarkdown == "Groceries"
        }

        assertEquals("Groceries", runBlocking { repo.getNote(noteId) }?.title)
    }
}
