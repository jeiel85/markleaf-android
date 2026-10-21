package com.markleaf.notes.core.markdown.preview

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [27], qualifiers = "w360dp-h640dp-mdpi")
class MarkdownCodeBlockCopyTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun fencedCodeBlockWithLanguage_showsLabelAndCopiesExactCode() {
        renderPreview(
            """
            ```kotlin
            val a = 1
            val b = 2
            ```
            """.trimIndent()
        )

        composeRule.onNodeWithText("kotlin").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(copyButtonLabel()).performClick()

        assertEquals("val a = 1\nval b = 2", clipboardText())
        assertEquals(
            application().getString(R.string.preview_code_copied),
            ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun fencedCodeBlockWithoutLanguage_copiesExactCodeWithNoLabel() {
        renderPreview(
            """
            ```
            plain snippet
            ```
            """.trimIndent()
        )

        composeRule.onNodeWithContentDescription(copyButtonLabel()).performClick()

        assertEquals("plain snippet", clipboardText())
    }

    @Test
    fun indentedCodeBlock_copiesExactCode() {
        renderPreview("    val indented = 42\n    println(indented)")

        composeRule.onNodeWithContentDescription(copyButtonLabel()).performClick()

        assertEquals("val indented = 42\nprintln(indented)", clipboardText())
    }

    @Test
    @Config(sdk = [33])
    fun copyingCodeBlockOnApi33_copiesWithoutToast() {
        renderPreview(
            """
            ```python
            def greet():
                return "hi"
            ```
            """.trimIndent()
        )

        composeRule.onNodeWithContentDescription(copyButtonLabel()).performClick()

        assertEquals("def greet():\n    return \"hi\"", clipboardText())
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun emptyCodeBlock_hidesCopyButton() {
        renderPreview("```kotlin\n```")

        composeRule.onNodeWithText("kotlin").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(copyButtonLabel()).assertDoesNotExist()
    }

    @Test
    fun codeBlockWithTrailingBlankLinesAndSpaces_copiesTrimmedLineText() {
        renderPreview("```kotlin\nval x = 1   \n\n\n```")

        composeRule.onNodeWithContentDescription(copyButtonLabel()).performClick()

        // CommonMarkPreviewAdapter applies trimEnd() to node.literal when
        // building PreviewLine.text, so trailing spaces and trailing blank
        // lines are already stripped before reaching MarkdownCodeBlock.
        assertEquals("val x = 1", clipboardText())
    }

    private fun renderPreview(markdown: String) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkdownPreviewList(lines = SimpleMarkdownPreview.parse(markdown))
            }
        }
    }

    private fun application(): Application = ApplicationProvider.getApplicationContext()

    private fun copyButtonLabel(): String =
        application().getString(R.string.copy_code_block)

    private fun clipboardText(): String? {
        val manager = application()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }
}
