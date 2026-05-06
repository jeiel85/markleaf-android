package com.markleaf.notes.core.markdown

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
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
        table = Color.DarkGray,
        blockquote = Color.LightGray,
        horizontalRule = Color.Black
    )

    @Test
    fun highlight_keepsOriginalMarkdownText() {
        val markdown = "# Title\nWrite **bold**, *italic*, [[Note]], and [Link](Target)."

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
    }

    @Test
    fun highlight_addsStylesForCoreMarkdownPatterns() {
        val markdown = "# Title\n- [ ] Task\n**bold** *italic* [[Note]] [Link](Target)"

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
    fun highlight_addsStylesForTable() {
        val markdown = "| Header |\n| --- |\n| Cell |"

        val result = MarkdownSyntaxHighlighter.highlight(markdown, colors)

        assertEquals(markdown, result.text)
        assertTrue(result.spanStyles.any { it.item.color == Color.DarkGray })
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
}
