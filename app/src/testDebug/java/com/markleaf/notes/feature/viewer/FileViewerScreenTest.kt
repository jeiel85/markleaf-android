package com.markleaf.notes.feature.viewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The read-only file viewer (#326). What matters here is what it does *not* do:
 * a file being looked at writes nothing, so the only route into the note
 * database is the explicit save action — and the bar has to say the surface is
 * a file, or that action reads as "save my edits".
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class FileViewerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun loaded(
        displayName: String? = "meeting-notes.md",
        text: String = "# Meeting notes\n\nDecided to ship it."
    ) = FileViewerState.Loaded(
        displayName = displayName,
        lines = SimpleMarkdownPreview.parse(text),
        noteBody = text
    )

    private fun render(
        state: FileViewerState,
        onBack: () -> Unit = {},
        onSaveAsNote: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                FileViewerContent(state = state, onBack = onBack, onSaveAsNote = onSaveAsNote)
            }
        }
    }

    @Test
    fun showsTheFileNameAndItsRenderedText() {
        render(loaded())

        composeRule.onNodeWithText("meeting-notes.md").assertIsDisplayed()
        composeRule.onNodeWithText("Meeting notes").assertIsDisplayed()
        composeRule.onNodeWithText("Decided to ship it.").assertIsDisplayed()
    }

    @Test
    fun saysTheSurfaceIsAFileAndReadOnly() {
        render(loaded())

        composeRule.onNodeWithText("Read-only file").assertIsDisplayed()
    }

    @Test
    fun aFileWithNoNameStillGetsATitle() {
        render(loaded(displayName = null))

        composeRule.onNodeWithText("File").assertIsDisplayed()
    }

    @Test
    fun savingHandsBackTheNoteBody() {
        var saved: String? = null
        render(loaded(), onSaveAsNote = { saved = it })

        composeRule.onNodeWithContentDescription("Save as note").performClick()

        assertEquals("# Meeting notes\n\nDecided to ship it.", saved)
    }

    @Test
    fun nothingCanBeSavedBeforeTheFileIsRead() {
        // The action appearing while the read is still running would let a tap
        // create an empty note out of a file that may yet turn out unreadable.
        render(FileViewerState.Loading)

        composeRule.onNodeWithContentDescription("Save as note").assertDoesNotExist()
    }

    @Test
    fun anUnreadableFileSaysSoAndOffersNoSave() {
        var saved: String? = null
        render(FileViewerState.Unreadable, onSaveAsNote = { saved = it })

        composeRule.onNodeWithText(
            "This file couldn't be opened. It may have been moved or deleted, or it isn't a text file."
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Save as note").assertDoesNotExist()
        assertNull(saved)
    }

    @Test
    fun theBarOffersAWayBack() {
        var backed = false
        render(loaded(), onBack = { backed = true })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(true, backed)
    }
}
