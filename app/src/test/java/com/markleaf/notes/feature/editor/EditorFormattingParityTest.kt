package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The formatting panel and the `/` menu are two doors onto one set of
 * constructs, and the app has now twice shipped a construct with only the
 * slash door open: tables and callouts until #390, wikilinks and today's date
 * until #424. Both were found by a user hunting the panel, failing, and
 * reporting a feature the app already had.
 *
 * `panelEquivalent` makes the omission a compile error for a *new* command;
 * this pins the other half — that the row it names is really in the panel, and
 * that both doors insert the same thing once it is.
 */
class EditorFormattingParityTest {

    private val today = LocalDate.of(2026, 9, 20)

    /**
     * Input: every `/` command. Output: the panel row each one names is really
     * in the panel.
     *
     * This is the half `panelEquivalent` cannot enforce — the compiler makes
     * someone name a row, not put one there.
     */
    @Test
    fun everyQuickInsertCommandIsReachableFromTheFormattingPanel() {
        val reachable = formattingPanelActions
        val missing = QuickInsertCommand.entries
            .filterNot { it.panelEquivalent() in reachable }
            .map { "${it.name} -> ${it.panelEquivalent().name}" }

        assertTrue(
            "these slash commands name a formatting panel row that is not in the panel: $missing",
            missing.isEmpty()
        )
    }

    /**
     * The #424 regression, pinned by name rather than only by the loop above:
     * deleting either row should fail a test that says what was lost.
     */
    @Test
    fun theWikilinkAndDateRowsAreInThePanel() {
        assertTrue(
            "the panel lost its \"link to note\" row — that is what #424 reported",
            EditorFormattingAction.WIKILINK in formattingPanelActions
        )
        assertTrue(
            "the panel lost its date row — that is what #424 reported",
            EditorFormattingAction.DATE in formattingPanelActions
        )
    }

    @Test
    fun noActionIsOfferedTwiceInThePanel() {
        val duplicates = formattingPanelActions
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        assertTrue("these actions appear in more than one panel row: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun bothDoorsInsertTheSameWikilink() {
        assertEquals(slashResultOf("/wiki", QuickInsertCommand.WIKILINK), panelResultOf(EditorFormattingAction.WIKILINK))
    }

    @Test
    fun bothDoorsInsertTheSameDate() {
        assertEquals(slashResultOf("/date", QuickInsertCommand.DATE), panelResultOf(EditorFormattingAction.DATE))
    }

    /**
     * What the slash command leaves behind when it is the only thing on the
     * line — text plus caret, so a template that agrees but lands the caret
     * somewhere else still fails.
     */
    private fun slashResultOf(typed: String, command: QuickInsertCommand): Pair<String, Int> {
        val value = TextFieldValue(typed, TextRange(typed.length))
        val query = checkNotNull(detectQuickInsertQuery(value)) { "$typed is not a quick-insert query" }
        val applied = applyQuickInsertCommand(value, query, command, today)
        return applied.text to applied.selection.start
    }

    /** The same, for the panel row, starting from the empty note the slash query reduces to. */
    private fun panelResultOf(action: EditorFormattingAction): Pair<String, Int> {
        val result = action.applyTo(TextFieldValue(""), today)
        val edited = result as? EditorFormattingResult.Edited
            ?: error("$action does not edit the text")
        return edited.value.text to edited.value.selection.start
    }
}
