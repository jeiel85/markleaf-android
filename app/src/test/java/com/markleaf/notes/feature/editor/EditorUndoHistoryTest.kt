package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The behaviour [EditorUndoHistory] promises the editor, driven the way the
 * screen drives it: every value the text field takes is recorded, and the class
 * decides what counts as a step.
 */
class EditorUndoHistoryTest {

    private var clock = 1_000L
    private val history = EditorUndoHistory(now = { clock })

    @Test
    fun aFreshlyLoadedNoteHasNothingToUndo() {
        history.reset(value("Kept"))

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertNull(history.undo())
        assertNull(history.redo())
    }

    /** The report (#360): select all, type one character, everything is gone. */
    @Test
    fun typingOverTheWholeNoteIsOneUndoableStep() {
        history.reset(value("A long note the writer would hate to lose."))

        history.record(value("x"))

        assertTrue(history.canUndo)
        assertEquals("A long note the writer would hate to lose.", history.undo()?.text)
        assertFalse(history.canUndo)
    }

    @Test
    fun deletingEverythingIsOneUndoableStep() {
        history.reset(value("Everything."))

        history.record(value(""))

        assertEquals("Everything.", history.undo()?.text)
    }

    @Test
    fun aRunOfTypingCollapsesIntoOneStep() {
        history.reset(value(""))

        type("a", "ab", "abc", "abcd")

        assertEquals("", history.undo()?.text)
        assertFalse(history.canUndo)
    }

    @Test
    fun aPauseEndsTheTypingRun() {
        history.reset(value(""))
        type("a", "ab")

        clock += 5_000
        history.record(value("abc"))

        assertEquals("ab", history.undo()?.text)
        assertEquals("", history.undo()?.text)
    }

    @Test
    fun aNewlineEndsTheTypingRun() {
        history.reset(value(""))
        type("one", "one\n", "one\nt", "one\ntwo")

        // The newline is its own step, so undo walks back a line at a time
        // rather than erasing the paragraph in one jump.
        assertEquals("one\n", history.undo()?.text)
        assertEquals("one", history.undo()?.text)
        assertEquals("", history.undo()?.text)
    }

    @Test
    fun aLongUnbrokenRunStillBreaksIntoSteps() {
        history.reset(value(""))

        val typed = StringBuilder()
        repeat(200) {
            typed.append('x')
            history.record(value(typed.toString()))
        }

        // Not one step for two hundred characters, and not two hundred either.
        val undone = generateSequence { history.undo()?.text }.toList()
        assertTrue("steps=${undone.size}", undone.size in 2..20)
        assertEquals("", undone.last())
    }

    /**
     * Raised in review on #361: a small replacement used to look like typing,
     * so selecting what you had just typed and overtyping it merged into the
     * same run and undo skipped past the replaced text.
     */
    @Test
    fun typingOverASelectionEndsTheRunEvenWhenItIsTiny() {
        history.reset(value(""))
        type("a", "ab", "abc")

        // Select "abc", then type over it.
        history.record(value("abc", selection = TextRange(0, 3)))
        clock += 50
        history.record(value("x"))

        assertEquals("abc", history.undo()?.text)
        assertEquals("", history.undo()?.text)
    }

    @Test
    fun typingSomewhereElseEndsTheRun() {
        history.reset(value("hello world"))
        clock += 5_000
        history.record(value("hello world!"))

        // Caret jumps to the front, then types there.
        clock += 50
        history.record(value("hello world!", caret = 0))
        clock += 50
        history.record(value("Xhello world!", caret = 1))

        assertEquals("hello world!", history.undo()?.text)
        assertEquals("hello world", history.undo()?.text)
    }

    @Test
    fun backspacingAfterTypingEndsTheRun() {
        history.reset(value(""))
        type("ab", "abc")

        clock += 50
        history.record(value("ab"))

        assertEquals("abc", history.undo()?.text)
        assertEquals("", history.undo()?.text)
    }

    @Test
    fun aRunOfBackspacesCollapsesIntoOneStep() {
        history.reset(value("abcdef"))
        clock += 5_000
        type("abcde", "abcd", "abc")

        assertEquals("abcdef", history.undo()?.text)
        assertFalse(history.canUndo)
    }

    @Test
    fun aPasteIsItsOwnStepAndDoesNotSwallowTheTypingAfterIt() {
        history.reset(value(""))

        history.record(value("a pasted paragraph of text"))
        history.record(value("a pasted paragraph of text!"))

        assertEquals("a pasted paragraph of text", history.undo()?.text)
        assertEquals("", history.undo()?.text)
    }

    @Test
    fun movingTheCaretIsNotAStep() {
        history.reset(value("hello"))
        history.record(value("hello!"))

        history.record(value("hello!", caret = 0))
        history.record(value("hello!", caret = 3))

        assertEquals("hello", history.undo()?.text)
        assertFalse(history.canUndo)
    }

    @Test
    fun undoRestoresTheCaretThatWentWithTheText() {
        history.reset(value("hello", caret = 5))
        clock += 5_000
        history.record(value("hello there", caret = 11))

        val restored = history.undo()

        assertEquals("hello", restored?.text)
        assertEquals(TextRange(5), restored?.selection)
    }

    @Test
    fun replayingWhatUndoReturnedDoesNotCreateAStep() {
        // The screen feeds the restored value straight back in through the same
        // recorder, which must not turn the restore into a step of its own.
        history.reset(value("before"))
        clock += 5_000
        history.record(value("after"))

        val restored = history.undo()!!
        history.record(restored)

        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
        assertEquals("after", history.redo()?.text)
    }

    @Test
    fun redoIsAvailableAfterUndoAndIsDroppedByANewEdit() {
        history.reset(value("one"))
        clock += 5_000
        history.record(value("two"))

        assertEquals("one", history.undo()?.text)
        assertTrue(history.canRedo)

        clock += 5_000
        history.record(value("three"))

        assertFalse(history.canRedo)
        assertNull(history.redo())
        assertEquals("one", history.undo()?.text)
    }

    @Test
    fun theOldestStepsAreDroppedOnceTheStackIsFull() {
        val bounded = EditorUndoHistory(maxEntries = 3, now = { clock })
        bounded.reset(value("v0"))
        for (version in 1..5) {
            clock += 5_000
            bounded.record(value("v$version"))
        }

        val undone = generateSequence { bounded.undo()?.text }.toList()

        assertEquals(listOf("v4", "v3"), undone)
        assertFalse(bounded.canUndo)
    }

    @Test
    fun aCharacterBudgetAlsoBoundsTheStack() {
        val bounded = EditorUndoHistory(maxRetainedChars = 40, now = { clock })
        bounded.reset(value("-".repeat(20)))
        for (version in 1..5) {
            clock += 5_000
            bounded.record(value("$version".repeat(20)))
        }

        // Trimming never eats the last step: there is always a way back.
        assertTrue(bounded.canUndo)
        assertEquals("4".repeat(20), bounded.undo()?.text)
        assertFalse(bounded.canUndo)
    }

    @Test
    fun recordingBeforeAnyResetSeedsTheBaseline() {
        history.record(value("seeded"))

        assertFalse(history.canUndo)
        clock += 5_000
        history.record(value("edited"))
        assertEquals("seeded", history.undo()?.text)
    }

    @Test
    fun resetDropsEverythingBefore() {
        history.reset(value("first note"))
        clock += 5_000
        history.record(value("first note, edited"))

        history.reset(value("second note"))

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }

    private fun type(vararg versions: String) {
        versions.forEach { text ->
            clock += 50
            history.record(value(text))
        }
    }

    private fun value(
        text: String,
        caret: Int = text.length,
        selection: TextRange = TextRange(caret)
    ) = TextFieldValue(text, selection)
}
