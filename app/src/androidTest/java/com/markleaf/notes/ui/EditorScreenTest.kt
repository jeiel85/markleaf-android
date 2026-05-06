package com.markleaf.notes.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
import com.markleaf.notes.TestHostActivity
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<TestHostActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        TestHostActivity.content = null
    }

    private fun launchEditor() {
        TestHostActivity.content = {
            MarkleafTheme {
                EditorScreen(onBack = {})
            }
        }
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
        composeTestRule.waitForIdle()
    }

    @Test
    fun editorScreen_showsTitle() {
        launchEditor()

        val newNoteLabel = context.getString(R.string.new_note)
        composeTestRule.onNodeWithText(newNoteLabel).assertIsDisplayed()
    }

    @Test
    fun editorScreen_togglePreview() {
        launchEditor()

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
