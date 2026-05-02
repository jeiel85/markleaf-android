package com.markleaf.notes.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun editorScreen_showsTitle() {
        composeTestRule.setContent {
            MarkleafTheme {
                EditorScreen(onBack = {})
            }
        }

        val newNoteLabel = context.getString(R.string.new_note)
        composeTestRule.onNodeWithText(newNoteLabel).assertIsDisplayed()
    }

    @Test
    fun editorScreen_togglePreview() {
        composeTestRule.setContent {
            MarkleafTheme {
                EditorScreen(onBack = {})
            }
        }

        val previewLabel = context.getString(R.string.preview)
        val editLabel = context.getString(R.string.edit)

        // Click Preview button
        composeTestRule.onNodeWithText(previewLabel).performClick()
        
        // Check if "Preview" text (from TopAppBar title) is displayed
        composeTestRule.onNodeWithText(previewLabel).assertIsDisplayed()
        
        // Button text should change to "Edit"
        composeTestRule.onNodeWithText(editLabel).assertIsDisplayed()
    }
}
