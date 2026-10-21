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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
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
 * #544 with #405: with "Start new notes with a heading" on, a new note left
 * without typing holds only `# ` on screen, and it must still be discarded on
 * leaving rather than stay in the list as a blank row. One scenario per file
 * for the same reason as [EditorDiscardsBlankNoteTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorDiscardsUntouchedHeadingNoteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun anUntouchedHeadingNoteIsRemovedWhenTheEditorIsLeft() {
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
        var showEditor by mutableStateOf(true)
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore()).also {
            runBlocking { it.setNewNotesStartWithHeading(true) }
        }

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
        // The seed has to be on screen first, or this would only re-test #405.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))
                .fetchSemanticsNode().config[SemanticsProperties.EditableText].text == "# "
        }

        showEditor = false
        // Polls for the outcome, see EditorDiscardsBlankNoteTest.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId) } == null
        }

        assertNull(
            "A new note holding only the heading marker should not survive leaving the editor",
            runBlocking { repo.getNote(noteId) }
        )
    }
}
