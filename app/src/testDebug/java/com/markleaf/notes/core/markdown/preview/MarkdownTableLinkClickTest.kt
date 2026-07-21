package com.markleaf.notes.core.markdown.preview

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression net for #197: links inside GFM table cells must be tappable in
 * Preview mode, exactly like links in body text. Before the fix, table cells
 * were rendered as plain strings — the href was already gone at parse time —
 * so taps did nothing.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class MarkdownTableLinkClickTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingLinkInTableCell_dispatchesActionView() {
        val markdown = """
            | name | link |
            | --- | --- |
            | Restaurant 1 | [address](https://www.restaurant.com) |
        """.trimIndent()
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkdownPreviewList(lines = SimpleMarkdownPreview.parse(markdown))
            }
        }

        // Tap just inside the text's left edge: the cell Text is column-wide,
        // so its center can sit past the end of a short link label.
        composeRule.onNodeWithText("address").performTouchInput {
            click(centerLeft.copy(x = 20f))
        }

        val started = Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<Application>()
        ).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started?.action)
        assertEquals("https://www.restaurant.com", started?.data?.toString())
    }

    @Test
    fun tappingWikilinkInTableCell_invokesCallback() {
        val markdown = """
            | note |
            | --- |
            | [[Another Note]] |
        """.trimIndent()
        var clicked: String? = null
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkdownPreviewList(
                    lines = SimpleMarkdownPreview.parse(markdown),
                    onWikilinkClick = { clicked = it }
                )
            }
        }

        composeRule.onNodeWithText("Another Note").performTouchInput {
            click(centerLeft.copy(x = 20f))
        }

        assertEquals("Another Note", clicked)
    }
}
