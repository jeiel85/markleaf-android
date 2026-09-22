package com.markleaf.notes.core.markdown

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.time.LocalDate
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
    fun checkbox_convertsEveryLineOfAMultiLineSelection() {
        val text = "one\ntwo\nthree"
        val result = MarkdownEditActions.checkbox(
            TextFieldValue(text, selection = TextRange(0, text.length))
        )

        assertEquals("- [ ] one\n- [ ] two\n- [ ] three", result.text)
        assertEquals(TextRange(0, result.text.length), result.selection)
    }

    @Test
    fun checkbox_leavesBlankLinesAloneInAMultiLineSelection() {
        val text = "one\n\nthree"
        val result = MarkdownEditActions.checkbox(
            TextFieldValue(text, selection = TextRange(0, text.length))
        )

        assertEquals("- [ ] one\n\n- [ ] three", result.text)
    }

    @Test
    fun checkbox_togglesExistingItemsWithinAMultiLineSelection() {
        val text = "- [ ] one\ntwo\n- [x] three"
        val result = MarkdownEditActions.checkbox(
            TextFieldValue(text, selection = TextRange(0, text.length))
        )

        assertEquals("- [x] one\n- [ ] two\n- [ ] three", result.text)
    }

    @Test
    fun checkbox_singleLineSelectionStillUsesTheSingleLineRule() {
        // A selection confined to one line (no newline in the touched block)
        // must not take the multi-line path -- it still toggles/prefixes only
        // the line the selection is on, same as a plain caret.
        val result = MarkdownEditActions.checkbox(
            TextFieldValue("one two", selection = TextRange(0, 3))
        )

        assertEquals("- [ ] one two", result.text)
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
    fun table_insertsSkeletonAsItsOwnBlockAndCaretsTheFirstHeaderCell() {
        val result = MarkdownEditActions.table(
            TextFieldValue("hello", selection = TextRange(5))
        )

        assertEquals(
            "hello\n\n| Column 1 | Column 2 |\n| --- | --- |\n|  |  |",
            result.text
        )
        // A GFM table cannot interrupt a paragraph, so the blank line is what
        // makes this render as a table at all rather than as literal pipes.
        assertEquals(TextRange(9), result.selection)
    }

    @Test
    fun table_insertsAfterTheLineRatherThanSplittingIt() {
        val result = MarkdownEditActions.table(
            TextFieldValue("hello world", selection = TextRange(5))
        )

        assertTrue(result.text.startsWith("hello world\n\n|"))
    }

    @Test
    fun table_keepsFollowingTextOutOfTheTable() {
        val result = MarkdownEditActions.table(
            TextFieldValue("intro\nafter", selection = TextRange(2))
        )

        // Without the blank line after it, `after` is read as another row.
        assertTrue(result.text.endsWith("|  |  |\n\nafter"))
    }

    @Test
    fun table_addsNoBlankLinesInAnEmptyNote() {
        val result = MarkdownEditActions.table(
            TextFieldValue("", selection = TextRange(0))
        )

        assertEquals(MarkdownEditActions.TABLE_TEMPLATE, result.text)
        assertEquals(TextRange(MarkdownEditActions.TABLE_CARET_OFFSET), result.selection)
    }

    @Test
    fun callout_insertsHeadAndAnEmptyBodyLine() {
        val result = MarkdownEditActions.callout(
            TextFieldValue("", selection = TextRange(0))
        )

        assertEquals("> [!NOTE]\n> ", result.text)
        assertEquals(TextRange(12), result.selection)
    }

    @Test
    fun callout_separatesItselfFromTheParagraphAbove() {
        val result = MarkdownEditActions.callout(
            TextFieldValue("hello", selection = TextRange(5))
        )

        assertEquals("hello\n\n> [!NOTE]\n> ", result.text)
        assertEquals(TextRange(19), result.selection)
    }

    @Test
    fun callout_wrapsTheSelectedLines() {
        val result = MarkdownEditActions.callout(
            TextFieldValue("one\ntwo", selection = TextRange(0, 7))
        )

        assertEquals("> [!NOTE]\n> one\n> two", result.text)
        assertEquals(TextRange(0, 21), result.selection)
    }

    @Test
    fun callout_wrapExpandsAPartialSelectionToWholeLines() {
        val result = MarkdownEditActions.callout(
            TextFieldValue("one\ntwo", selection = TextRange(1, 5))
        )

        // A selection that starts mid-word must not leave `o` outside the quote.
        assertEquals("> [!NOTE]\n> one\n> two", result.text)
    }

    @Test
    fun callout_leavesAnExistingCalloutAloneRatherThanNestingIt() {
        val value = TextFieldValue("> [!NOTE]\n> body", selection = TextRange(0, 16))

        assertEquals(value, MarkdownEditActions.callout(value))
    }

    @Test
    fun callout_insertsEmptyBlockWhenTheSelectionIsBlank() {
        val result = MarkdownEditActions.callout(
            TextFieldValue("   ", selection = TextRange(0, 3))
        )

        assertTrue(result.text.startsWith("> [!NOTE]\n> "))
    }

    @Test
    fun callout_leavesNoIndentationInFrontOfTheHead() {
        // Four spaces in front of it would make the head an indented code
        // block rather than a callout — from the Codex review on #390.
        val result = MarkdownEditActions.callout(
            TextFieldValue("    ", selection = TextRange(4))
        )

        assertTrue(result.text.startsWith("> [!NOTE]\n> "))
    }

    @Test
    fun table_leavesNoIndentationInFrontOfTheHeaderRow() {
        val result = MarkdownEditActions.table(
            TextFieldValue("a\n\n    ", selection = TextRange(7))
        )

        assertTrue(result.text.startsWith("a\n\n| Column 1 |"))
        assertEquals(TextRange(5), result.selection)
    }

    @Test
    fun table_leavesNoTabInFrontOfTheHeaderRow() {
        val result = MarkdownEditActions.table(
            TextFieldValue("\t", selection = TextRange(1))
        )

        assertTrue(result.text.startsWith("| Column 1 |"))
    }

    @Test
    fun table_handlesACaretOnAnEmptyFirstLine() {
        // A note that opens with a newline: the insertion point is 0, which is
        // where the line-start arithmetic used to run off the front.
        val result = MarkdownEditActions.table(
            TextFieldValue("\nafter", selection = TextRange(0))
        )

        assertTrue(result.text.startsWith("| Column 1 |"))
        assertTrue(result.text.endsWith("\n\nafter"))
    }

    @Test
    fun callout_stopsAtAnExclusiveEndpointOnTheNextLine() {
        // `one\n` selected: `two` was never in the selection and must not be
        // quoted — from the Codex review on #390.
        val result = MarkdownEditActions.callout(
            TextFieldValue("one\ntwo", selection = TextRange(0, 4))
        )

        assertEquals("> [!NOTE]\n> one\n\ntwo", result.text)
    }

    @Test
    fun indent_stopsAtAnExclusiveEndpointOnTheNextLine() {
        val result = MarkdownEditActions.indent(
            TextFieldValue("a\nb", selection = TextRange(0, 2))
        )

        assertEquals("  a\nb", result.text)
    }

    @Test
    fun autoContinuation_continuesBulletList() {
        val before = TextFieldValue("- one", selection = TextRange(5))
        val typed = TextFieldValue("- one\n", selection = TextRange(6))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("- one\n- ", result.value.text)
        assertEquals(TextRange(8), result.value.selection)
        // The continuation differs from what the keyboard sent, so a guard against
        // that exact pre-continuation text (#447) must come out the other side.
        assertEquals(result.value, result.pendingEcho?.expectedCurrent)
        assertEquals(typed, result.pendingEcho?.staleEcho)
    }

    @Test
    fun autoContinuation_endsListWhenPrefixOnEmptyLine() {
        val before = TextFieldValue("- one\n- ", selection = TextRange(8))
        val typed = TextFieldValue("- one\n- \n", selection = TextRange(9))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("- one\n\n", result.value.text)
    }

    @Test
    fun autoContinuation_incrementsOrderedList() {
        val before = TextFieldValue("1. first", selection = TextRange(8))
        val typed = TextFieldValue("1. first\n", selection = TextRange(9))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("1. first\n2. ", result.value.text)
    }

    @Test
    fun autoContinuation_continuesChecklist() {
        val before = TextFieldValue("- [ ] task", selection = TextRange(10))
        val typed = TextFieldValue("- [ ] task\n", selection = TextRange(11))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("- [ ] task\n- [ ] ", result.value.text)
    }

    @Test
    fun autoContinuation_continuesBlockquote() {
        val before = TextFieldValue("> quote", selection = TextRange(7))
        val typed = TextFieldValue("> quote\n", selection = TextRange(8))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("> quote\n> ", result.value.text)
    }

    @Test
    fun autoContinuation_doesNothingForPlainText() {
        val before = TextFieldValue("hello", selection = TextRange(5))
        val typed = TextFieldValue("hello\n", selection = TextRange(6))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("hello\n", result.value.text)
        // Nothing was added beyond what the keyboard sent, so there is nothing to
        // guard against an echo of.
        assertNull(result.pendingEcho)
    }

    // --- #447: a keyboard (SwiftKey, confirmed on a real device) resending its
    // own pre-continuation copy of the text one callback behind Markleaf's edit ---

    @Test
    fun autoContinuation_swiftKeyEchoSequence_capturedFromADevice() {
        // The exact onValueChange(old, new) pairs logged from a real Galaxy S24
        // running SwiftKey 9.13.16.5, typing "- logtest" and tapping its on-screen
        // Enter key (#447). Replayed here as a fixed regression fixture because the
        // race that produces it can't be reproduced deterministically in a test.
        var guard: MarkdownEditActions.PendingEcho? = null

        // SwiftKey finalizing the composed word: no-op.
        val step1 = MarkdownEditActions.applyAutoContinuation(
            old = TextFieldValue("- logtest", selection = TextRange(9)),
            new = TextFieldValue("- logtest", selection = TextRange(9)),
            pendingGuard = guard
        )
        assertEquals("- logtest", step1.value.text)
        guard = step1.pendingEcho

        // The real Enter keystroke: a clean +1 char, continuation fires.
        val step2 = MarkdownEditActions.applyAutoContinuation(
            old = TextFieldValue("- logtest", selection = TextRange(9)),
            new = TextFieldValue("- logtest\n", selection = TextRange(10)),
            pendingGuard = guard
        )
        assertEquals("- logtest\n- ", step2.value.text)
        guard = step2.pendingEcho

        // SwiftKey's stale echo: its own pre-continuation copy, one callback late.
        // Without the guard this overwrites step2's result right back to "- logtest\n".
        val step3 = MarkdownEditActions.applyAutoContinuation(
            old = TextFieldValue("- logtest\n- ", selection = TextRange(12)),
            new = TextFieldValue("- logtest\n", selection = TextRange(10)),
            pendingGuard = guard
        )
        assertEquals("- logtest\n- ", step3.value.text)
        guard = step3.pendingEcho

        // A settling no-op call some keyboards send after: still guarded, still holds.
        val step4 = MarkdownEditActions.applyAutoContinuation(
            old = TextFieldValue("- logtest\n- ", selection = TextRange(12)),
            new = TextFieldValue("- logtest\n", selection = TextRange(10)),
            pendingGuard = guard
        )
        assertEquals("- logtest\n- ", step4.value.text)
    }

    @Test
    fun autoContinuation_doesNotSuppressARealEditThatMatchesTheStaleEchoText() {
        // If the field has moved on from what the continuation produced by any path
        // other than that one callback (undo, a formatting shortcut, switching
        // notes), `old` on the next call no longer matches the guard's
        // expectedCurrent — so an edit that happens to reproduce the echo's text is
        // applied normally rather than swallowed.
        val synthesized = MarkdownEditActions.applyAutoContinuation(
            old = TextFieldValue("- one", selection = TextRange(5)),
            new = TextFieldValue("- one\n", selection = TextRange(6)),
            pendingGuard = null
        )
        assertEquals("- one\n- ", synthesized.value.text)
        val guard = synthesized.pendingEcho

        // The field was reset by something else (e.g. undo) to exactly the text the
        // guard is watching for — this must NOT be treated as the keyboard's echo,
        // because `old` here is not the value the guard expects.
        val afterUndo = TextFieldValue("- one\n", selection = TextRange(6))
        val result = MarkdownEditActions.applyAutoContinuation(
            old = afterUndo,
            new = afterUndo,
            pendingGuard = guard
        )
        assertEquals("- one\n", result.value.text)
    }

    @Test
    fun autoContinuation_aRealBackspaceThatUndoesTheContinuationIsNotSuppressed() {
        // Deleting the auto-inserted "- " one character at a time — the ordinary way
        // backspace works — must reach the same result a keyboard's stale echo of
        // the pre-continuation text would produce, but as two real edits rather than
        // an echo: the first backspace already breaks the guard's exact-match check.
        val synthesized = MarkdownEditActions.applyAutoContinuation(
            old = TextFieldValue("- one", selection = TextRange(5)),
            new = TextFieldValue("- one\n", selection = TextRange(6)),
            pendingGuard = null
        )
        assertEquals("- one\n- ", synthesized.value.text)

        val backspace1 = MarkdownEditActions.applyAutoContinuation(
            old = synthesized.value,
            new = TextFieldValue("- one\n-", selection = TextRange(7)),
            pendingGuard = synthesized.pendingEcho
        )
        assertEquals("- one\n-", backspace1.value.text)
        assertNull(backspace1.pendingEcho)

        val backspace2 = MarkdownEditActions.applyAutoContinuation(
            old = backspace1.value,
            new = TextFieldValue("- one\n", selection = TextRange(6)),
            pendingGuard = backspace1.pendingEcho
        )
        assertEquals("- one\n", backspace2.value.text)
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

    // --- #215: turning a line number into a caret, for the outline jump -----

    @Test
    fun offsetOfLine_findsTheStartOfEachLine() {
        val markdown = "# Title\n\nbody\n## Section"

        assertEquals(0, MarkdownEditActions.offsetOfLine(markdown, 0))
        assertEquals(8, MarkdownEditActions.offsetOfLine(markdown, 1))
        assertEquals(9, MarkdownEditActions.offsetOfLine(markdown, 2))
        assertEquals(14, MarkdownEditActions.offsetOfLine(markdown, 3))
    }

    @Test
    fun offsetOfLine_countsCarriageReturnsAsPartOfTheLine() {
        // The caret belongs at the start of the *next* line, not between \r\n.
        assertEquals(9, MarkdownEditActions.offsetOfLine("# Title\r\n## Next", 1))
    }

    @Test
    fun offsetOfLine_returnsNullWhenTheLineIsOutOfRange() {
        // An outline built from slightly older text can name a line the note no
        // longer has; the jump is dropped rather than aimed at the end.
        assertNull(MarkdownEditActions.offsetOfLine("one\ntwo", 2))
        assertNull(MarkdownEditActions.offsetOfLine("one\ntwo", -1))
    }

    @Test
    fun offsetOfLine_acceptsTheEmptyLineAfterATrailingNewline() {
        assertEquals(4, MarkdownEditActions.offsetOfLine("one\n", 1))
    }

    @Test
    fun wikilink_insertsEmptyBracketsWithTheCaretBetweenThem() {
        val value = TextFieldValue("see ", selection = TextRange(4))
        val result = MarkdownEditActions.wikilink(value)
        assertEquals("see [[]]", result.text)
        assertEquals(6, result.selection.start)
    }

    @Test
    fun wikilink_wrapsTheSelectionInsteadOfOverwritingIt() {
        // With "Show formatting button" off (#331) a selection is the only way
        // the panel opens, so this is the common path, not the edge one.
        val value = TextFieldValue("see Project Alpha", selection = TextRange(4, 17))
        val result = MarkdownEditActions.wikilink(value)
        assertEquals("see [[Project Alpha]]", result.text)
    }

    @Test
    fun date_insertsTheIsoDateAtTheCaret() {
        val value = TextFieldValue("due ", selection = TextRange(4))
        val result = MarkdownEditActions.date(value, LocalDate.of(2026, 9, 20))
        assertEquals("due 2026-09-20", result.text)
        assertEquals(14, result.selection.start)
    }
}
