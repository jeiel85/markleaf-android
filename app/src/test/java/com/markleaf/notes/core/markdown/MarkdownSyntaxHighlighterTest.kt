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
        checkbox = Color.Magenta
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
}
