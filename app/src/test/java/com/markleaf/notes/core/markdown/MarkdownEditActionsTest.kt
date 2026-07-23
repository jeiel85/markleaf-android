package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownEditActionsTest {
    @Test
    fun detectWikilinkQuery_returnsPartialAfterDoubleBracket() {
        val value = TextFieldValue("see [[hel", selection = TextRange(9))
        assertEquals(
            "hel",
            com.markleaf.notes.feature.editor.detectWikilinkQuery(value)
        )
    }

    @Test
    fun detectWikilinkQuery_emptyAfterOpening() {
        val value = TextFieldValue("see [[", selection = TextRange(6))
        assertEquals(
            "",
            com.markleaf.notes.feature.editor.detectWikilinkQuery(value)
        )
    }

    @Test
    fun detectWikilinkQuery_nullWhenClosed() {
        val value = TextFieldValue("[[done]] cursor", selection = TextRange(15))
        assertEquals(
            null,
            com.markleaf.notes.feature.editor.detectWikilinkQuery(value)
        )
    }

    @Test
    fun detectWikilinkQuery_nullAcrossNewline() {
        val value = TextFieldValue("[[start\n", selection = TextRange(8))
        assertEquals(
            null,
            com.markleaf.notes.feature.editor.detectWikilinkQuery(value)
        )
    }

    @Test
    fun completeWikilink_replacesPartialAndAddsClosing() {
        val value = TextFieldValue("see [[hel", selection = TextRange(9))
        val result = com.markleaf.notes.feature.editor.completeWikilink(value, "Hello World")
        assertEquals("see [[Hello World]]", result.text)
        assertEquals(TextRange(19), result.selection)
    }

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
        assertEquals(TextRange(6, 15), result.selection)
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
    fun checkbox_togglesTodoToDone() {
        val result = MarkdownEditActions.checkbox(
            TextFieldValue("- [ ] task", selection = TextRange(8))
        )

        assertEquals("- [x] task", result.text)
        // Toggling swaps one character, so the caret must not move (#145).
        assertEquals(TextRange(8), result.selection)
    }

    @Test
    fun checkbox_togglesDoneToTodo() {
        val result = MarkdownEditActions.checkbox(
            TextFieldValue("- [x] task", selection = TextRange(8))
        )

        assertEquals("- [ ] task", result.text)
        assertEquals(TextRange(8), result.selection)
    }

    @Test
    fun checkbox_togglesIndentedItem() {
        val result = MarkdownEditActions.checkbox(
            TextFieldValue("  - [ ] sub", selection = TextRange(10))
        )

        assertEquals("  - [x] sub", result.text)
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
        assertEquals(TextRange(6, 15), result.selection)
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

    @Test
    fun findWordAtCursor_englishWord() {
        val text = "hello world"
        // w
        val r1 = MarkdownEditActions.findWordAtCursor(text, 6)
        assertEquals(TextRange(6, 11), r1)
        
        // o
        val r2 = MarkdownEditActions.findWordAtCursor(text, 8)
        assertEquals(TextRange(6, 11), r2)

        // d
        val r3 = MarkdownEditActions.findWordAtCursor(text, 11)
        assertEquals(TextRange(6, 11), r3)
    }

    @Test
    fun findWordAtCursor_koreanWord() {
        val text = "안녕 세상아"
        // 세
        val r1 = MarkdownEditActions.findWordAtCursor(text, 3)
        assertEquals(TextRange(3, 6), r1)

        // 아
        val r2 = MarkdownEditActions.findWordAtCursor(text, 6)
        assertEquals(TextRange(3, 6), r2)
    }

    @Test
    fun findWordAtCursor_atWhitespace() {
        val text = "hello world"
        val r1 = MarkdownEditActions.findWordAtCursor(text, 5)
        assertEquals(TextRange(5), r1)
    }

    @Test
    fun findWordAtCursor_atMarkdownBoundary() {
        val text = "hello **world**"
        // inside w
        val r1 = MarkdownEditActions.findWordAtCursor(text, 10)
        assertEquals(TextRange(8, 13), r1) // index 8 is 'w', 13 is after 'd'
    }

    @Test
    fun wrapSelection_boldUnwrapSelf() {
        // Case 1-A: 선택 영역 자체가 마커로 감싸임
        val result = MarkdownEditActions.bold(
            TextFieldValue("**hello**", selection = TextRange(0, 9))
        )
        assertEquals("hello", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun wrapSelection_boldUnwrapOuter() {
        // Case 1-B: 선택 영역 바로 바깥에 마커 존재
        val result = MarkdownEditActions.bold(
            TextFieldValue("**hello**", selection = TextRange(2, 7))
        )
        assertEquals("hello", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun wrapSelection_boldWrapNormal() {
        // Case 1-C: Wrap normal
        val result = MarkdownEditActions.bold(
            TextFieldValue("hello", selection = TextRange(0, 5))
        )
        assertEquals("**hello**", result.text)
        assertEquals(TextRange(0, 9), result.selection)
    }

    @Test
    fun wrapSelection_collapsedUnwrapInside() {
        // Case 2-A: Collapsed, inside marker -> Unwrap
        val result = MarkdownEditActions.bold(
            TextFieldValue("**hello**", selection = TextRange(5)) // "hel|lo"
        )
        assertEquals("hello", result.text)
        assertEquals(TextRange(3), result.selection) // "hel|lo"
    }

    @Test
    fun wrapSelection_collapsedUnwrapMultipleMarkers() {
        // Case 2-A: Multiple markers on the same line, unwrap the active one
        val result = MarkdownEditActions.bold(
            TextFieldValue("**hello** and **world**", selection = TextRange(18)) // "wo|rld" (index 18)
        )
        assertEquals("**hello** and world", result.text)
        assertEquals(TextRange(16), result.selection) // "wo|rld"
    }

    @Test
    fun wrapSelection_collapsedWrapWord() {
        // Case 2-B: Collapsed, wrap surrounding word
        val result = MarkdownEditActions.bold(
            TextFieldValue("hello world", selection = TextRange(8)) // "wo|rld"
        )
        assertEquals("hello **world**", result.text)
        assertEquals(TextRange(10), result.selection) // "wo|rld"
    }

    @Test
    fun wrapSelection_collapsedWrapKoreanWord() {
        // Case 2-B: Collapsed, Korean word
        val result = MarkdownEditActions.bold(
            TextFieldValue("안녕 세상아", selection = TextRange(4)) // "세|상아"
        )
        assertEquals("안녕 **세상아**", result.text)
        assertEquals(TextRange(6), result.selection) // "세|상아"
    }

    @Test
    fun wrapSelection_collapsedFallback() {
        // Case 2-C: Collapsed, no word at cursor -> Fallback
        val result = MarkdownEditActions.bold(
            TextFieldValue("hello ", selection = TextRange(6)) // "hello |"
        )
        assertEquals("hello **bold**", result.text)
        assertEquals(TextRange(8), result.selection)
    }

    // --- #219: toggling a task from the preview, by line number ------------

    @Test
    fun toggleTaskAtLine_flipsTodoToDone() {
        val markdown = """
            # Plan

            - [ ] Tag the release
            - [ ] Write store note
        """.trimIndent()

        val result = MarkdownEditActions.toggleTaskAtLine(markdown, 2)

        assertEquals(
            """
                # Plan

                - [x] Tag the release
                - [ ] Write store note
            """.trimIndent(),
            result
        )
    }

    @Test
    fun toggleTaskAtLine_flipsDoneBackToTodo() {
        assertEquals("- [ ] done", MarkdownEditActions.toggleTaskAtLine("- [x] done", 0))
    }

    @Test
    fun toggleTaskAtLine_treatsUppercaseXAsDone() {
        assertEquals("- [ ] done", MarkdownEditActions.toggleTaskAtLine("- [X] done", 0))
    }

    @Test
    fun toggleTaskAtLine_handlesIndentedItems() {
        val markdown = """
            - outer
              - [ ] nested
        """.trimIndent()

        assertEquals(
            """
                - outer
                  - [x] nested
            """.trimIndent(),
            MarkdownEditActions.toggleTaskAtLine(markdown, 1)
        )
    }

    @Test
    fun toggleTaskAtLine_returnsNullWhenTheLineIsNotATask() {
        // A preview built from slightly older text can point at a line that has
        // since become prose; rewriting it anyway would corrupt the note.
        val markdown = """
            just text
            - [ ] task
        """.trimIndent()

        assertNull(MarkdownEditActions.toggleTaskAtLine(markdown, 0))
    }

    @Test
    fun toggleTaskAtLine_returnsNullWhenTheLineDoesNotExist() {
        assertNull(MarkdownEditActions.toggleTaskAtLine("- [ ] task", 5))
        assertNull(MarkdownEditActions.toggleTaskAtLine("- [ ] task", -1))
    }

    @Test
    fun toggleTaskAtLine_changesExactlyOneCharacter() {
        // Windows line endings and trailing spaces have to survive: the note is
        // mirrored to a file, and reflowing it would surface as a whole-file
        // diff in whatever syncs that folder.
        val markdown = "- [ ] a\r\n- [ ] b  \r\n"

        val result = MarkdownEditActions.toggleTaskAtLine(markdown, 1)!!

        assertEquals(markdown.length, result.length)
        assertEquals(1, markdown.indices.count { markdown[it] != result[it] })
        assertTrue(result.contains("- [x] b  \r\n"))
    }
}
