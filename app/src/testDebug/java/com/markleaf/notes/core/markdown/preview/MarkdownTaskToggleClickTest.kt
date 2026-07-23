package com.markleaf.notes.core.markdown.preview

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
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
 * Regression net for #219: the checkbox drawn in Preview must be tappable, and
 * the tap has to name the right source line. Before the fix the glyph was a
 * plain text prefix with nothing behind it, so a tap did nothing at all.
 *
 * Only the marker is clickable, not the whole row — long-pressing the item text
 * to select it has to keep working.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class MarkdownTaskToggleClickTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun preview(markdown: String, onToggle: (Int) -> Unit) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkdownPreviewList(
                    lines = SimpleMarkdownPreview.parse(markdown),
                    onToggleTask = onToggle
                )
            }
        }
    }

    @Test
    fun tappingTheCheckbox_reportsThatRowsSourceLine() {
        var toggled: Int? = null
        preview("# Plan\n\n- [ ] first\n- [ ] second") { toggled = it }

        composeRule.onNodeWithText("☐ second").performTouchInput {
            click(centerLeft.copy(x = 4f))
        }

        assertEquals(3, toggled)
    }

    @Test
    fun tappingACompletedCheckbox_reportsItsSourceLineToo() {
        var toggled: Int? = null
        preview("- [x] done") { toggled = it }

        composeRule.onNodeWithText("☑ done").performTouchInput {
            click(centerLeft.copy(x = 4f))
        }

        assertEquals(0, toggled)
    }

    @Test
    fun tappingTheItemText_doesNotToggle() {
        // The marker is the control; the text stays selectable.
        var toggled: Int? = null
        preview("- [ ] a much longer task label") { toggled = it }

        composeRule.onNodeWithText("☐ a much longer task label").performTouchInput {
            click(centerRight.copy(x = width - 4f))
        }

        assertNull(toggled)
    }

    @Test
    fun withoutAHandler_theCheckboxIsInert() {
        // Snapshot tests and any other caller that passes no handler must not
        // gain a clickable region — that is what keeps the goldens honest.
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkdownPreviewList(lines = SimpleMarkdownPreview.parse("- [ ] task"))
            }
        }

        composeRule.onNodeWithText("☐ task").performTouchInput {
            click(centerLeft.copy(x = 4f))
        }
        // Nothing to assert beyond "this did not crash and nothing was called";
        // the handler is absent, so a click must simply be ignored.
        composeRule.onNodeWithText("☐ task").assertExists()
    }
}
