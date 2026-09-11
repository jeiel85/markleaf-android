package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.core.markdown.MarkdownEditActions
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickInsertTest {
    @Test
    fun detectQuery_returnsEmptyQueryAfterSlashAtLineStart() {
        assertEquals(
            QuickInsertQuery(start = 0, end = 1, text = ""),
            detectQuickInsertQuery(TextFieldValue("/", selection = TextRange(1)))
        )
    }

    @Test
    fun detectQuery_preservesIndentationAndUsesCurrentLine() {
        assertEquals(
            QuickInsertQuery(start = 8, end = 11, text = "he"),
            detectQuickInsertQuery(TextFieldValue("first\n  /he", selection = TextRange(11)))
        )
    }

    @Test
    fun detectQuery_rejectsMidSentenceAndUrlSlashes() {
        assertNull(detectQuickInsertQuery(TextFieldValue("write /he", selection = TextRange(9))))
        assertNull(detectQuickInsertQuery(TextFieldValue("https://", selection = TextRange(8))))
        assertNull(detectQuickInsertQuery(TextFieldValue("a/b", selection = TextRange(3))))
    }

    @Test
    fun detectQuery_rejectsWhitespaceAndNonCollapsedSelection() {
        assertNull(detectQuickInsertQuery(TextFieldValue("/code block", selection = TextRange(11))))
        assertNull(detectQuickInsertQuery(TextFieldValue("/he", selection = TextRange(0, 3))))
    }

    @Test
    fun filterCommands_prioritizesLabelAndAliasPrefixes() {
        val items = listOf(
            QuickInsertSearchItem(QuickInsertCommand.CODE_BLOCK, "Code block", listOf("code", "fence")),
            QuickInsertSearchItem(QuickInsertCommand.QUOTE, "Block quote", listOf("quote")),
            QuickInsertSearchItem(QuickInsertCommand.HEADING_1, "Heading 1", listOf("h1"))
        )

        assertEquals(
            listOf(QuickInsertCommand.QUOTE, QuickInsertCommand.CODE_BLOCK),
            filterQuickInsertCommands(items, "block").map { it.command }
        )
        assertEquals(
            listOf(QuickInsertCommand.HEADING_1),
            filterQuickInsertCommands(items, "H1").map { it.command }
        )
    }

    @Test
    fun selectionIndex_isClampedWhenFilteringShrinksResults() {
        assertEquals(0, safeQuickInsertIndex(selectedIndex = 8, itemCount = 1))
        assertEquals(2, safeQuickInsertIndex(selectedIndex = 2, itemCount = 4))
    }

    @Test
    fun applyCommand_replacesOnlyTheOpenQueryAndPreservesIndentationAndSuffix() {
        val value = TextFieldValue("before\n  /h2 after", selection = TextRange(12))
        val query = checkNotNull(detectQuickInsertQuery(value))

        val result = applyQuickInsertCommand(value, query, QuickInsertCommand.HEADING_2)

        assertEquals("before\n  ##  after", result.text)
        assertEquals(TextRange(12), result.selection)
    }

    @Test
    fun applyCommand_insertsEveryMarkdownShapeWithExpectedCaret() {
        val today = LocalDate.of(2026, 7, 13)
        val cases = listOf(
            Case(QuickInsertCommand.HEADING_1, "# ", 2),
            Case(QuickInsertCommand.HEADING_2, "## ", 3),
            Case(QuickInsertCommand.HEADING_3, "### ", 4),
            Case(QuickInsertCommand.BULLET_LIST, "- ", 2),
            Case(QuickInsertCommand.NUMBERED_LIST, "1. ", 3),
            Case(QuickInsertCommand.CHECKLIST, "- [ ] ", 6),
            Case(QuickInsertCommand.QUOTE, "> ", 2),
            Case(QuickInsertCommand.CODE_BLOCK, "```\n\n```\n", 4),
            Case(QuickInsertCommand.DIVIDER, "---\n", 4),
            Case(
                QuickInsertCommand.TABLE,
                "| Column 1 | Column 2 |\n| --- | --- |\n|  |  |",
                2
            ),
            Case(QuickInsertCommand.CALLOUT, "> [!NOTE]\n> ", 12),
            Case(QuickInsertCommand.WIKILINK, "[[]]", 2),
            Case(QuickInsertCommand.IMAGE, "", 0),
            Case(QuickInsertCommand.DATE, "2026-07-13", 10)
        )

        cases.forEach { case ->
            val value = TextFieldValue("/", selection = TextRange(1))
            val query = checkNotNull(detectQuickInsertQuery(value))
            val result = applyQuickInsertCommand(value, query, case.command, today)
            assertEquals(case.command.name, case.text, result.text)
            assertEquals(case.command.name, TextRange(case.caret), result.selection)
        }
    }

    /**
     * Input: an empty note, and a `/` query. Output: both doors onto the table
     * and the callout produce byte-identical text and caret.
     *
     * The `/` menu and the formatting panel (#390) insert the same two
     * constructs. Someone who learns the shape through one door and reaches for
     * the other must not get a different table, so the agreement is pinned here
     * rather than left to the shared constants staying shared.
     */
    @Test
    fun tableAndCalloutAgreeWithTheFormattingPanel() {
        val empty = TextFieldValue("", selection = TextRange(0))
        val slash = TextFieldValue("/", selection = TextRange(1))
        val query = checkNotNull(detectQuickInsertQuery(slash))

        val slashTable = applyQuickInsertCommand(slash, query, QuickInsertCommand.TABLE)
        val panelTable = MarkdownEditActions.table(empty)
        assertEquals(slashTable.text, panelTable.text)
        assertEquals(slashTable.selection, panelTable.selection)

        val slashCallout = applyQuickInsertCommand(slash, query, QuickInsertCommand.CALLOUT)
        val panelCallout = MarkdownEditActions.callout(empty)
        assertEquals(slashCallout.text, panelCallout.text)
        assertEquals(slashCallout.selection, panelCallout.selection)
    }

    private data class Case(
        val command: QuickInsertCommand,
        val text: String,
        val caret: Int
    )
}
