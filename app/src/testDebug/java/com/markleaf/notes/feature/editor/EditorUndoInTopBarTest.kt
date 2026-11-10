package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.data.settings.SnippetButton
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #542: on a small phone the row above the keyboard costs two lines of the
 * note, and with `Aa` off and no snippets it stood there for undo and redo
 * alone. In that configuration they move to the top bar, in the outline and
 * information slots, and those two move to the overflow menu. Every other
 * configuration keeps the row as it was.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorUndoInTopBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }
    private val content = "Practice notes\n\nbody"

    private fun show(showFormattingButton: Boolean, snippets: List<SnippetButton> = emptyList()) {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "Practice notes",
                    contentMarkdown = content,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore()).also {
            runBlocking {
                it.setShowFormattingButton(showFormattingButton)
                it.setSnippetButtons(snippets)
            }
        }
        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Practice notes", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun field() = composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))
    private fun text() = field().fetchSemanticsNode().config[SemanticsProperties.EditableText].text
    private fun undo() = composeRule.onAllNodesWithContentDescription(context.getString(R.string.undo))
    private fun outlineButton() =
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.table_of_contents))
    private fun infoButton() =
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.note_information))
    private fun formatting() =
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.formatting))

    /** Type one character so there is a step to undo, and wait until undo offers it. */
    private fun makeAnEdit() {
        field().performTextInputSelection(TextRange(content.length))
        field().performTextInput("!")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            undo().fetchSemanticsNodes().any { node ->
                isEnabled().matches(node)
            }
        }
    }

    @Test
    fun withNothingElseForTheRowUndoAndRedoSitInTheTopBar() {
        show(showFormattingButton = false)
        makeAnEdit()

        // One undo, and it is the top bar's: the row is not mounted at all, so
        // the outline and information icons it replaced are gone from the bar.
        undo().assertCountEquals(1)
        formatting().assertCountEquals(0)
        outlineButton().assertCountEquals(0)
        infoButton().assertCountEquals(0)

        composeRule.onNodeWithContentDescription(context.getString(R.string.undo)).assertIsEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(content, text())
    }

    @Test
    fun theOutlineAndInformationMoveToTheOverflowMenu() {
        show(showFormattingButton = false)

        composeRule.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(context.getString(R.string.table_of_contents)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.note_information)).performClick()
        composeRule.waitForIdle()

        // The menu closed and the information sheet opened, as it did from the
        // icon: its title is the only "Note information" left on screen.
        assertTrue(
            "the information sheet did not open from the menu",
            composeRule.onAllNodesWithText(context.getString(R.string.note_information))
                .fetchSemanticsNodes().isNotEmpty()
        )
        composeRule.onNodeWithText(context.getString(R.string.table_of_contents)).assertDoesNotExist()
    }

    @Test
    fun theDefaultRowKeepsUndoAndTheTopBarKeepsItsIcons() {
        show(showFormattingButton = true)
        makeAnEdit()

        undo().assertCountEquals(1)
        formatting().assertCountEquals(1)
        outlineButton().assertCountEquals(1)
        infoButton().assertCountEquals(1)
    }

    @Test
    fun snippetsKeepTheRowAndUndoStaysOnIt() {
        // `Aa` off, but the row still has the user's snippets to carry.
        show(showFormattingButton = false, snippets = listOf(SnippetButton(label = "", text = "[[{|}]]")))
        makeAnEdit()

        undo().assertCountEquals(1)
        outlineButton().assertCountEquals(1)
        infoButton().assertCountEquals(1)
    }
}
