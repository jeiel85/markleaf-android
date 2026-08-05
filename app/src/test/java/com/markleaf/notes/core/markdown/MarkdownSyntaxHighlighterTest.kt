package com.markleaf.notes.core.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownSyntaxHighlighterTest {
    private val colors = MarkdownSyntaxColors(
        heading = Color.Red,
        emphasis = Color.Blue,
        link = Color.Green,
        syntax = Color.Gray,
        checkbox = Color.Magenta,
        code = Color.Cyan,
        codeBlock = Color.Yellow,
        blockquote = Color.LightGray,
        horizontalRule = Color.Black
    )

    @Test
    fun highlight_keepsOriginalMarkdownText() {
        val markdown = "# Title\nWrite **bold**, *italic*, and [Link](Target)."

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
    }

    @Test
    fun highlight_addsStylesForCoreMarkdownPatterns() {
        val markdown = "# Title\n- [ ] Task\n**bold** *italic* [Link](Target)"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertTrue(result.spanStyles.any { it.item.color == Color.Red })
        assertTrue(result.spanStyles.any { it.item.color == Color.Blue })
        assertTrue(result.spanStyles.any { it.item.color == Color.Green })
        assertTrue(result.spanStyles.any { it.item.color == Color.Magenta })
        assertTrue(result.spanStyles.any { it.item.color == Color.Gray })
    }

    @Test
    fun highlight_addsStylesForUnderscoreItalic() {
        val markdown = "This is _italic_ text"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
        assertTrue(result.spanStyles.any { it.item.color == Color.Blue })
    }

    @Test
    fun highlight_addsStylesForStrikethrough() {
        val markdown = "This is ~~deleted~~ text"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
        assertTrue(result.spanStyles.any { it.item.color == Color.Blue })
    }

    @Test
    fun highlight_addsStylesForInlineCode() {
        val markdown = "Use `code` here"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
        assertTrue(result.spanStyles.any { it.item.color == Color.Cyan })
    }

    @Test
    fun highlight_addsStylesForCodeBlock() {
        val markdown = "```kotlin\nval x = 1\n```"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
        assertTrue(result.spanStyles.any { it.item.background == Color.Yellow.copy(alpha = 0.1f) })
    }

    @Test
    fun highlight_addsStylesForBlockquote() {
        val markdown = "> This is a quote"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
        assertTrue(result.spanStyles.any { it.item.color == Color.LightGray })
    }

    @Test
    fun highlight_addsStylesForHorizontalRule() {
        val markdown = "---"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
        assertTrue(result.spanStyles.any { it.item.color == Color.Black })
    }

    @Test
    fun highlight_h1_appliesLargeFontSizeAndBoldWeightToContent() {
        val markdown = "# Hello"
        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        // Heading 1 content range: after "# " starts at index 2 (inclusive), ends at last char.
        val contentSpan = result.spanStyles.firstOrNull {
            it.start == 2 && it.item.fontSize == 24.sp && it.item.fontWeight == FontWeight.Bold
        }
        assertNotNull("expected H1 content span at offset 2 with 24sp Bold", contentSpan)
    }

    @Test
    fun highlight_h2_appliesSmallerSizeThanH1() {
        val result = MarkdownSyntaxHighlighter.highlight("## H2 line", colors)
        val contentSpan = result.spanStyles.firstOrNull { it.item.fontSize == 20.sp }
        assertNotNull("expected H2 content span at 20sp", contentSpan)
    }

    @Test
    fun highlight_h3_appliesEvenSmallerSize() {
        val result = MarkdownSyntaxHighlighter.highlight("### H3 line", colors)
        val contentSpan = result.spanStyles.firstOrNull { it.item.fontSize == 18.sp }
        assertNotNull("expected H3 content span at 18sp", contentSpan)
    }

    @Test
    fun highlight_bold_usesFullBoldWeightNotSemiBold() {
        val result = MarkdownSyntaxHighlighter.highlight("a **bold** b", colors)
        // Bold span covers `**bold**` (offsets 2..9 inclusive).
        val boldSpan = result.spanStyles.firstOrNull {
            it.start == 2 && it.end == 10 && it.item.fontWeight == FontWeight.Bold
        }
        assertNotNull("expected Bold weight on **bold**", boldSpan)
    }

    @Test
    fun highlight_marker_resetsRichAttributesAroundContent() {
        val result = MarkdownSyntaxHighlighter.highlight("**bold**", colors)
        // Both `**` markers should carry FontWeight.Normal so they recede next to bold content.
        val markerSpans = result.spanStyles.filter {
            it.item.color == Color.Gray && it.item.fontWeight == FontWeight.Normal
        }
        assertTrue(
            "expected both **markers** to be muted to Normal weight",
            markerSpans.size >= 2
        )
    }
}
