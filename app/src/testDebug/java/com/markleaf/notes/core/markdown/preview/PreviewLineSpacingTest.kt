package com.markleaf.notes.core.markdown.preview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.data.settings.EditorLineSpacing
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The line-spacing setting (#561) in the preview: the wrapped lines of a
 * paragraph move closer together or further apart, and so do one-line rows
 * such as list items, by about the same amount.
 *
 * The second half is the part that needs code of its own. Compose trims a
 * text's line height above its first line and below its last, so a one-line row
 * is the same height at every setting; without the row padding that makes up
 * for it, a checklist would look the same in Compact as in Relaxed while the
 * prose beside it changed.
 *
 * mdpi, so a dp is a pixel and the measurements below read as dp.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class PreviewLineSpacingTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var lineSpacing by mutableFloatStateOf(1f)

    private data class Measured(val rowPitch: Float, val linePitch: Float)

    @Before
    fun render() {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkdownPreviewList(
                    lines = SimpleMarkdownPreview.parse(NOTE),
                    lineSpacing = lineSpacing
                )
            }
        }
    }

    /** Row pitch: top of one list item to the next. Line pitch: inside the paragraph. */
    private fun measure(spacing: EditorLineSpacing): Measured {
        lineSpacing = spacing.factor
        composeRule.waitForIdle()
        val first = composeRule.onNodeWithText("• first").getBoundsInRoot().top
        val second = composeRule.onNodeWithText("• second").getBoundsInRoot().top
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(PARAGRAPH).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        val layout = layouts.single()
        assertTrue("the paragraph has to wrap onto three lines", layout.lineCount >= 3)
        return Measured(
            rowPitch = (second - first).value,
            // From the second line to the third: the first line has the share
            // of its line height that sits above the text trimmed off, so it
            // is shorter than the rest.
            linePitch = layout.getLineTop(2) - layout.getLineTop(1)
        )
    }

    @Test
    fun compactAndRelaxedMoveTheLinesOfAParagraph() {
        val normal = measure(EditorLineSpacing.NORMAL).linePitch
        val compact = measure(EditorLineSpacing.COMPACT).linePitch
        val relaxed = measure(EditorLineSpacing.RELAXED).linePitch

        // The theme's body line height, untouched at Normal.
        assertEquals(26f, normal, 0.5f)
        assertTrue("compact $compact < normal $normal", compact < normal)
        assertTrue("relaxed $relaxed > normal $normal", relaxed > normal)
    }

    @Test
    fun oneLineRowsMoveByAboutAsMuchAsTheLines() {
        val normal = measure(EditorLineSpacing.NORMAL)
        for (spacing in listOf(EditorLineSpacing.COMPACT, EditorLineSpacing.RELAXED)) {
            val other = measure(spacing)
            val rowChange = other.rowPitch - normal.rowPitch
            val lineChange = other.linePitch - normal.linePitch
            // Within 2dp: the line height is rounded up to a whole pixel and
            // each side's padding to the nearest one, so the two land a pixel
            // or two apart rather than exactly together.
            assertEquals(
                "$spacing: rows moved $rowChange dp, lines $lineChange dp",
                lineChange,
                rowChange,
                2f
            )
        }
    }

    private companion object {
        const val PARAGRAPH =
            "A paragraph long enough to wrap onto several lines at this width, so the " +
                "space between its own lines can be measured against the space between rows."
        const val NOTE = "- first\n- second\n\n$PARAGRAPH"
    }
}
