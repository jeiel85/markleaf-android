package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditActionsTest {
    @Test
    fun bold_wrapsSelectedText() {
        val result = MarkdownEditActions.bold(
            TextFieldValue("hello world", selection = TextRange(6, 11))
        )

        assertEquals("hello **world**", result.text)
        assertEquals(TextRange(15), result.selection)
    }

    @Test
    fun italic_insertsPlaceholderWhenSelectionIsEmpty() {
        val result = MarkdownEditActions.italic(
            TextFieldValue("hello ", selection = TextRange(6))
        )

        assertEquals("hello *italic*", result.text)
        assertEquals(TextRange(7), result.selection)
    }

    @Test
    fun checkbox_insertsAtCurrentLineStart() {
        val result = MarkdownEditActions.checkbox(
            TextFieldValue("one\ntwo", selection = TextRange(5))
        )

        assertEquals("one\n- [ ] two", result.text)
        assertEquals(TextRange(11), result.selection)
    }

    @Test
    fun markdownLink_usesSelectedTextAsLabelAndTarget() {
        val result = MarkdownEditActions.markdownLink(
            TextFieldValue("Open Target", selection = TextRange(5, 11))
        )

        assertEquals("Open [Target](Target)", result.text)
    }

    @Test
    fun wikiLink_wrapsSelectedText() {
        val result = MarkdownEditActions.wikiLink(
            TextFieldValue("See Note", selection = TextRange(4, 8))
        )

        assertEquals("See [[Note]]", result.text)
    }
}
