package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditActionsTest {
    @Test
    fun indent_addsTwoSpacesAtLineStart() {
        val result = MarkdownEditActions.indent(
            TextFieldValue("hello", selection = TextRange(2))
        )
        assertEquals("  hello", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test
    fun indent_indentsEveryLineInMultiLineSelection() {
        val result = MarkdownEditActions.indent(
            TextFieldValue("a\nb\nc", selection = TextRange(0, 5))
        )
        assertEquals("  a\n  b\n  c", result.text)
    }

    @Test
    fun outdent_removesTwoLeadingSpaces() {
        val result = MarkdownEditActions.outdent(
            TextFieldValue("  hello", selection = TextRange(4))
        )
        assertEquals("hello", result.text)
        assertEquals(TextRange(2), result.selection)
    }

    @Test
    fun outdent_handlesTabIndent() {
        val result = MarkdownEditActions.outdent(
            TextFieldValue("\thello", selection = TextRange(3))
        )
        assertEquals("hello", result.text)
    }

    @Test
    fun outdent_isNoOpWhenLineHasNoLeadingWhitespace() {
        val result = MarkdownEditActions.outdent(
            TextFieldValue("hello", selection = TextRange(2))
        )
        assertEquals("hello", result.text)
    }

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
    fun strikethrough_wrapsSelectedText() {
        val result = MarkdownEditActions.strikethrough(
            TextFieldValue("hello world", selection = TextRange(6, 11))
        )

        assertEquals("hello ~~world~~", result.text)
        assertEquals(TextRange(15), result.selection)
    }

    @Test
    fun strikethrough_insertsPlaceholderWhenSelectionIsEmpty() {
        val result = MarkdownEditActions.strikethrough(
            TextFieldValue("hello ", selection = TextRange(6))
        )

        assertEquals("hello ~~text~~", result.text)
        assertEquals(TextRange(8), result.selection)
    }

    @Test
    fun inlineCode_wrapsSelectedText() {
        val result = MarkdownEditActions.inlineCode(
            TextFieldValue("use variable", selection = TextRange(4, 12))
        )

        assertEquals("use `variable`", result.text)
    }

    @Test
    fun inlineCode_insertsPlaceholderWhenSelectionIsEmpty() {
        val result = MarkdownEditActions.inlineCode(
            TextFieldValue("hello ", selection = TextRange(6))
        )

        assertEquals("hello `code`", result.text)
        assertEquals(TextRange(7), result.selection)
    }

    @Test
    fun heading_cyclesThroughLevels() {
        val plain = TextFieldValue("Title", selection = TextRange(5))
        val h1 = MarkdownEditActions.heading(plain)
        assertEquals("# Title", h1.text)

        val h2 = MarkdownEditActions.heading(h1)
        assertEquals("## Title", h2.text)

        val h3 = MarkdownEditActions.heading(h2)
        assertEquals("### Title", h3.text)

        val cleared = MarkdownEditActions.heading(h3)
        assertEquals("Title", cleared.text)
    }

    @Test
    fun bulletList_togglesOnAndOff() {
        val on = MarkdownEditActions.bulletList(
            TextFieldValue("hello", selection = TextRange(5))
        )
        assertEquals("- hello", on.text)

        val off = MarkdownEditActions.bulletList(on)
        assertEquals("hello", off.text)
    }

    @Test
    fun orderedList_togglesOnAndOff() {
        val on = MarkdownEditActions.orderedList(
            TextFieldValue("first", selection = TextRange(5))
        )
        assertEquals("1. first", on.text)

        val off = MarkdownEditActions.orderedList(on)
        assertEquals("first", off.text)
    }

    @Test
    fun blockquote_togglesOnAndOff() {
        val on = MarkdownEditActions.blockquote(
            TextFieldValue("quote", selection = TextRange(5))
        )
        assertEquals("> quote", on.text)

        val off = MarkdownEditActions.blockquote(on)
        assertEquals("quote", off.text)
    }

    @Test
    fun horizontalRule_insertsDividerOnFreshLine() {
        val result = MarkdownEditActions.horizontalRule(
            TextFieldValue("hello", selection = TextRange(5))
        )

        assertEquals("hello\n---\n", result.text)
    }

    @Test
    fun codeBlock_insertsEmptyFencesAroundCursor() {
        val result = MarkdownEditActions.codeBlock(
            TextFieldValue("", selection = TextRange(0))
        )

        assertEquals("```\n\n```\n", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test
    fun autoContinuation_continuesBulletList() {
        val before = TextFieldValue("- one", selection = TextRange(5))
        val typed = TextFieldValue("- one\n", selection = TextRange(6))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed)

        assertEquals("- one\n- ", result.text)
        assertEquals(TextRange(8), result.selection)
    }

    @Test
    fun autoContinuation_endsListWhenPrefixOnEmptyLine() {
        val before = TextFieldValue("- one\n- ", selection = TextRange(8))
        val typed = TextFieldValue("- one\n- \n", selection = TextRange(9))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed)

        assertEquals("- one\n\n", result.text)
    }

    @Test
    fun autoContinuation_incrementsOrderedList() {
        val before = TextFieldValue("1. first", selection = TextRange(8))
        val typed = TextFieldValue("1. first\n", selection = TextRange(9))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed)

        assertEquals("1. first\n2. ", result.text)
    }

    @Test
    fun autoContinuation_continuesChecklist() {
        val before = TextFieldValue("- [ ] task", selection = TextRange(10))
        val typed = TextFieldValue("- [ ] task\n", selection = TextRange(11))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed)

        assertEquals("- [ ] task\n- [ ] ", result.text)
    }

    @Test
    fun autoContinuation_continuesBlockquote() {
        val before = TextFieldValue("> quote", selection = TextRange(7))
        val typed = TextFieldValue("> quote\n", selection = TextRange(8))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed)

        assertEquals("> quote\n> ", result.text)
    }

    @Test
    fun autoContinuation_doesNothingForPlainText() {
        val before = TextFieldValue("hello", selection = TextRange(5))
        val typed = TextFieldValue("hello\n", selection = TextRange(6))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed)

        assertEquals("hello\n", result.text)
    }
}
