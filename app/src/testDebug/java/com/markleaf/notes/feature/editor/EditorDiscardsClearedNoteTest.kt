package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #405: a note that was typed into and then cleared back out is also gone
 * once the editor is left, not just a note that was never touched at all.
 *
 * See [EditorDiscardsBlankNoteTest] for why this is its own file rather than
 * a second method alongside it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorDiscardsClearedNoteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun noteTypedIntoAndThenClearedIsAlsoRemoved() {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            // Content already autosaved, then cleared back out -- matching
            // what the row looks like by the time a user deletes everything
            // they typed rather than never typing at all.
            repo.createNote(
                Note(
                    id = noteId,
                    title = "temporary",
                    contentMarkdown = "temporary",
                    excerpt = "temporary",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
            val note = repo.getNote(noteId)!!
            repo.updateNote(note.copy(contentMarkdown = "", title = "", excerpt = ""))
        }
        var showEditor by mutableStateOf(true)
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

        composeRule.setContent {
            val hostScope = rememberCoroutineScope()
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (showEditor) {
                    EditorScreen(
                        noteId = noteId,
                        onBack = {},
                        settingsRepository = settings,
                        hostScope = hostScope
                    )
                }
            }
        }
        composeRule.waitForIdle()

        showEditor = false
        composeRule.waitForIdle()

        assertNull(
            "A note cleared back to nothing should not survive leaving the editor either",
            runBlocking { repo.getNote(noteId) }
        )
    }
}
