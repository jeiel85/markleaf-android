package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.data.settings.OpenNotesAt
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
 * The editor as the "Today's note" shortcut opens it (#481): edit mode, caret
 * at the end, whatever "Open notes in preview" and "Open notes at" say — the
 * note is opened to be added to. Both settings are set against it here, and the
 * same note opened without [openForAppend] is checked to obey them, so a pass
 * means the flag overrode them rather than that they were never in effect.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorOpenForAppendTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }
    private val content = "# 2026-10-06\n\nmorning: call the dentist"

    private fun createNote(): String {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "2026-10-06",
                    contentMarkdown = content,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        return noteId
    }

    /** Preview and the top of the note: the two settings the shortcut has to override. */
    private fun readingSettings() = AppSettingsRepository(InMemoryPreferencesDataStore()).also {
        runBlocking {
            it.setOpenNotesInPreview(true)
            it.setOpenNotesAt(OpenNotesAt.TOP)
        }
    }

    private fun show(noteId: String, openForAppend: Boolean) {
        val settings = readingSettings()
        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings, openForAppend = openForAppend)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("call the dentist", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun openedForAppendingItIsInEditModeWithTheCaretAtTheEnd() {
        show(createNote(), openForAppend = true)

        // The toggle offers the mode you are not in.
        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).assertExists()
        val field = composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))
            .fetchSemanticsNode()
        assertEquals(TextRange(content.length), field.config[SemanticsProperties.TextSelectionRange])
    }

    /**
     * #516 review: a rotation reloads the note, and the append flag used to put
     * the caret back at the end (and the keyboard back up) over wherever the
     * reader had moved it. The clock keeps running here: a StateRestorationTester
     * on a paused clock never actually rebuilds anything.
     */
    @Test
    fun aRotationKeepsTheCaretWhereTheReaderMovedIt() {
        val noteId = createNote()
        val settings = readingSettings()
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings, openForAppend = true)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("call the dentist", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        val field = composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))
        field.performTextInputSelection(TextRange(3))
        composeRule.waitForIdle()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("call the dentist", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        assertEquals(
            TextRange(3),
            composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))
                .fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
        )
    }

    @Test
    fun openedNormallyTheSameNoteFollowsTheSettings() {
        show(createNote(), openForAppend = false)

        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertExists()
    }
}
