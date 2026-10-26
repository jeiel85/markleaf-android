package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.core.markdown.MarkdownEditActions.PARAGRAPH_INDENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** "Indent new paragraphs" (#543): Enter after plain text starts the next line indented. */
class ParagraphIndentTest {

    private val ind = PARAGRAPH_INDENT

    /** Types Enter with the caret at the end of [before], as one keyboard callback does. */
    private fun enter(before: String, indent: Boolean = true): TextFieldValue {
        val old = TextFieldValue(before, TextRange(before.length))
        val new = TextFieldValue(before + "\n", TextRange(before.length + 1))
        return MarkdownEditActions.applyAutoContinuation(old, new, null, indentParagraphs = indent).value
    }

    @Test
    fun `the indent is two ideographic spaces`() {
        assertEquals("　　", ind)
    }

    @Test
    fun `enter after text starts the next line indented`() {
        val result = enter("${ind}First paragraph.")
        assertEquals("${ind}First paragraph.\n$ind", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun `a line without an indent of its own still gets one after it`() {
        assertEquals("Opening line\n$ind", enter("Opening line").text)
    }

    @Test
    fun `off by default and when turned off`() {
        assertEquals("Opening line\n", enter("Opening line", indent = false).text)
        val old = TextFieldValue("text", TextRange(4))
        val new = TextFieldValue("text\n", TextRange(5))
        assertEquals("text\n", MarkdownEditActions.applyAutoContinuation(old, new, null).value.text)
    }

    @Test
    fun `enter on an indent-only line removes the indent`() {
        val result = enter("${ind}Done.\n$ind")
        assertEquals("${ind}Done.\n\n", result.text)
        assertEquals(TextRange(result.text.length), result.selection)
    }

    @Test
    fun `lists and quotes keep their own continuation`() {
        assertEquals("- item\n- ", enter("- item").text)
        assertEquals("1. one\n2. ", enter("1. one").text)
        assertEquals("> quoted\n> ", enter("> quoted").text)
        assertEquals("- [ ] task\n- [ ] ", enter("- [ ] task").text)
    }

    @Test
    fun `headings, tables, breaks and blank lines are not indented after`() {
        assertEquals("# Title\n", enter("# Title").text)
        assertEquals("### Sub\n", enter("### Sub").text)
        assertEquals("| a | b |\n", enter("| a | b |").text)
        assertEquals("---\n", enter("---").text)
        assertEquals("___\n", enter("___").text)
        assertEquals("text\n\n", enter("text\n").text)
    }

    @Test
    fun `a hashtag line is text, not a heading`() {
        assertEquals("#journal today\n$ind", enter("#journal today").text)
    }

    @Test
    fun `code is never indented`() {
        assertEquals("```kotlin\n", enter("```kotlin").text)
        assertEquals("```\nval x = 1\n", enter("```\nval x = 1").text)
        assertEquals("~~~\ncode\n~~~\n", enter("~~~\ncode\n~~~").text)
        assertEquals("    indented code\n", enter("    indented code").text)
    }

    @Test
    fun `text after a closed fence is indented again`() {
        assertEquals("```\ncode\n```\nprose\n$ind", enter("```\ncode\n```\nprose").text)
        // A longer run of the same character closes it too.
        assertEquals("```\ncode\n````\nprose\n$ind", enter("```\ncode\n````\nprose").text)
    }

    @Test
    fun `a fence-like line that cannot close the block is code`() {
        // #556 review: these were counted as closing fences.
        assertEquals("~~~\n```\ncode\n", enter("~~~\n```\ncode").text)
        assertEquals("````\n```\ncode\n", enter("````\n```\ncode").text)
        assertEquals("```\n``` not a close\ncode\n", enter("```\n``` not a close\ncode").text)
    }

    @Test
    fun `an indent-only line inside a fence is code and is kept`() {
        // #556 review: this took the removal path before the fence check.
        assertEquals("~~~\n$ind\n", enter("~~~\n$ind").text)
    }

    @Test
    fun `enter in the middle of a line indents the moved text`() {
        val old = TextFieldValue("${ind}ab", TextRange(3))
        val new = TextFieldValue("${ind}a\nb", TextRange(4))
        val result = MarkdownEditActions.applyAutoContinuation(old, new, null, indentParagraphs = true).value
        assertEquals("${ind}a\n${ind}b", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    @Test
    fun `a keyboard echo of the plain newline is swallowed like a list one`() {
        // #447: SwiftKey resends its pre-continuation text one callback late.
        val old = TextFieldValue("text", TextRange(4))
        val plain = TextFieldValue("text\n", TextRange(5))
        val first = MarkdownEditActions.applyAutoContinuation(old, plain, null, indentParagraphs = true)
        val echo = MarkdownEditActions.applyAutoContinuation(first.value, plain, first, indentParagraphs = true)
        assertEquals("text\n$ind", echo.value.text)
        assertNull(MarkdownEditActions.applyAutoContinuation(old, old, null, indentParagraphs = true).staleEcho)
    }
}
