package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #588: move-up and move-down buttons on the row above the keyboard, off
 * unless the user turns them on. Each tap swaps the caret's line, or the
 * selected lines, with the neighbouring line.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorMoveLineButtonsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }
    private val content = "Groceries\n- [ ] milk\n- [ ] eggs\n- [ ] bread"

    private fun show(moveButtons: Boolean, showFormattingButton: Boolean = true) {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "Groceries",
                    contentMarkdown = content,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore()).also {
            runBlocking {
                it.setShowMoveLineButtons(moveButtons)
                it.setShowFormattingButton(showFormattingButton)
            }
        }
        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Groceries", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun field() = composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))
    private fun text() = field().fetchSemanticsNode().config[SemanticsProperties.EditableText].text
    private fun selection() = field().fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
    private fun up() = composeRule.onNodeWithContentDescription(context.getString(R.string.move_line_up))
    private fun down() = composeRule.onNodeWithContentDescription(context.getString(R.string.move_line_down))

    private fun select(range: TextRange) {
        field().performTextInputSelection(range)
        composeRule.waitForIdle()
    }

    @Test
    fun offByDefaultTheRowHasNoMoveButtons() {
        show(moveButtons = false)
        select(TextRange(content.indexOf("milk")))

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.move_line_up)).assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.move_line_down)).assertCountEquals(0)
    }

    @Test
    fun eachTapMovesTheCaretLineOneLineAndUndoTakesItBack() {
        show(moveButtons = true)
        select(TextRange(content.indexOf("milk")))

        down().assertIsEnabled().performClick()
        composeRule.waitForIdle()
        down().performClick()
        composeRule.waitForIdle()

        assertEquals("Groceries\n- [ ] eggs\n- [ ] bread\n- [ ] milk", text())
        // The caret came along, still in front of "milk".
        assertEquals(TextRange(text().indexOf("milk")), selection())
        // It is on the last line now: nothing below to swap with.
        down().assertIsNotEnabled()
        up().assertIsEnabled()

        composeRule.onNodeWithContentDescription(context.getString(R.string.undo)).performClick()
        composeRule.waitForIdle()
        assertEquals("Groceries\n- [ ] eggs\n- [ ] milk\n- [ ] bread", text())
    }

    @Test
    fun selectedLinesMoveAsABlock() {
        show(moveButtons = true)
        // From inside "milk" to inside "eggs".
        select(TextRange(content.indexOf("ilk"), content.indexOf("ggs")))

        up().performClick()
        composeRule.waitForIdle()

        assertEquals("- [ ] milk\n- [ ] eggs\nGroceries\n- [ ] bread", text())
        // The same characters are still selected, so another tap moves the same block.
        val moved = text()
        assertEquals("ilk\n- [ ] e", moved.substring(selection().min, selection().max))
        up().assertIsNotEnabled()
    }

    @Test
    fun onA360dpPhoneTheSelectionRowStillShowsMoreOptions() {
        // Undo, redo, up, down, bold, italic and More are seven 48dp buttons
        // for a row of about 320dp, and More went off the edge (Codex on
        // #590). The shortcuts give way instead; More holds them all.
        show(moveButtons = true)
        select(TextRange(content.indexOf("milk"), content.indexOf("milk") + 4))

        val more = composeRule.onAllNodesWithContentDescription(context.getString(R.string.more_options))
        // The top bar's ⋮ and the row's More.
        more.assertCountEquals(2)
        // Squeezed, it was laid out narrower than its 48dp, or not at all.
        val minWidthPx = with(composeRule.density) { 47.dp.toPx() }
        // The row's is the lower one; the top bar's ⋮ is a narrower icon by design.
        val rowMore = more.fetchSemanticsNodes().maxBy { it.boundsInRoot.top }
        assertTrue("More options is squeezed out: ${rowMore.boundsInRoot}", rowMore.boundsInRoot.width >= minWidthPx)
        up().assertIsDisplayed()
        down().assertIsDisplayed()
    }

    @Test
    fun withTheButtonsOnUndoStaysOnTheRowEvenWithAaOff() {
        // `Aa` off and no snippets would move undo to the top bar (#542), but
        // the row now has the move buttons to carry, so undo stays beside them.
        show(moveButtons = true, showFormattingButton = false)
        select(TextRange(content.indexOf("milk")))

        down().performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.undo)).assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.table_of_contents))
            .assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.formatting)).assertCountEquals(0)
    }
}
