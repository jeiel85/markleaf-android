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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Under the File name title rule (D088, #611) the name field at the top of the
 * editor renames the note, and a note given a name survives leaving the editor
 * even with nothing written in it — unlike the blank note of #405, which is
 * discarded ([EditorDiscardsBlankNoteTest]).
 *
 * One scenario per file, for the same `AppDatabase` singleton reason given on
 * [EditorDiscardsBlankNoteTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorNameFieldRenamesNoteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun aNameTypedInTheFieldRenamesTheNoteAndKeepsItWhenLeft() {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "",
                    contentMarkdown = "",
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking {
            settings.beginRetitle(NoteTitleSource.FILE_NAME)
            settings.setRetitlePending(false)
        }
        var showEditor by mutableStateOf(true)

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

        // `/` cannot be in a file name, so it never reaches the name. A name no
        // other test uses: the database is shared across test classes, and a
        // duplicate (ignoring case) is refused, so "groceries" collided with
        // the "Groceries" notes other editor tests leave behind.
        composeRule.onNodeWithTag(NOTE_NAME_FIELD_TAG).performTextInput("name field/test")
        composeRule.onNodeWithTag(NOTE_NAME_FIELD_TAG).performImeAction()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId) }?.title == "name fieldtest"
        }

        showEditor = false
        composeRule.waitForIdle()
        // The empty-note discard reads the row through Room's executor, so give
        // it the same chance to run that the discard tests wait for.
        Thread.sleep(500)
        composeRule.waitForIdle()

        assertEquals(
            "a named note is a note, even with nothing in it",
            "name fieldtest",
            runBlocking { repo.getNote(noteId) }?.title
        )
    }
}
