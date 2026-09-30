package com.markleaf.notes.feature.editor

import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.core.markdown.PreviewLineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test

/**
 * Where the caret goes when the reader leaves Preview for the editor (#464).
 */
class PreviewReadingPositionTest {

    // Line 0 is "# Title", 2 is "para one", 4 is "para two", 6 is "para three".
    private val text = "# Title\n\npara one\n\npara two\n\npara three"
    private val rows = listOf(
        row("Title", startLine = 0),
        row("para one", startLine = 2),
        row("para two", startLine = 4),
        row("para three", startLine = 6)
    )

    private fun row(text: String, startLine: Int?) =
        PreviewLine(text = text, type = PreviewLineType.BODY, startLine = startLine)

    private fun offsetOfLine(line: Int) = text.split("\n").take(line).sumOf { it.length + 1 }

    @Test
    fun `scrolling down puts the caret at the start of the top block`() {
        val offset = caretOffsetForPreviewPosition(
            text, rows, now = PreviewScroll(2, 40), lastLeftAt = PreviewScroll.Top
        )

        assertEquals(offsetOfLine(4), offset)
    }

    @Test
    fun `a preview never scrolled leaves the caret alone`() {
        assertNull(
            caretOffsetForPreviewPosition(
                text, rows, now = PreviewScroll.Top, lastLeftAt = PreviewScroll.Top
            )
        )
    }

    @Test
    fun `scrolling only a little inside the first row still counts as scrolling`() {
        val offset = caretOffsetForPreviewPosition(
            text, rows, now = PreviewScroll(0, 12), lastLeftAt = PreviewScroll.Top
        )

        assertEquals(0, offset)
    }

    @Test
    fun `a preview left where it was last left leaves the caret alone`() {
        // Scrolled, edited at that spot, looked back at Preview without scrolling:
        // the caret belongs to what was just typed, not to the top of that block.
        val there = PreviewScroll(2, 40)

        assertNull(caretOffsetForPreviewPosition(text, rows, now = there, lastLeftAt = there))
    }

    @Test
    fun `scrolling again after a visit moves the caret again`() {
        val offset = caretOffsetForPreviewPosition(
            text, rows, now = PreviewScroll(3, 0), lastLeftAt = PreviewScroll(2, 40)
        )

        assertEquals(offsetOfLine(6), offset)
    }

    @Test
    fun `a row with no line falls back to the nearest one above it`() {
        val withGap = listOf(
            row("Title", startLine = 0),
            row("para one", startLine = 2),
            row("synthesized", startLine = null),
            row("para three", startLine = 6)
        )

        val offset = caretOffsetForPreviewPosition(
            text, withGap, now = PreviewScroll(2, 0), lastLeftAt = PreviewScroll.Top
        )

        assertEquals(offsetOfLine(2), offset)
    }

    @Test
    fun `no row at or above the top one knows its line leaves the caret alone`() {
        val unknown = listOf(row("a", null), row("b", null), row("c", null))

        assertNull(
            caretOffsetForPreviewPosition(
                text, unknown, now = PreviewScroll(2, 0), lastLeftAt = PreviewScroll.Top
            )
        )
    }

    @Test
    fun `an index past the end of the rows uses the last row`() {
        val offset = caretOffsetForPreviewPosition(
            text, rows, now = PreviewScroll(99, 0), lastLeftAt = PreviewScroll.Top
        )

        assertEquals(offsetOfLine(6), offset)
    }

    @Test
    fun `no rows leaves the caret alone`() {
        assertNull(
            caretOffsetForPreviewPosition(
                text, emptyList(), now = PreviewScroll(3, 0), lastLeftAt = PreviewScroll.Top
            )
        )
    }

    @Test
    fun `a line the text no longer has leaves the caret alone`() {
        // A preview built from older text can name a line that was since deleted;
        // the caret is not clamped to the end, it stays put.
        val stale = listOf(row("gone", startLine = 40))

        assertNull(
            caretOffsetForPreviewPosition(
                text, stale, now = PreviewScroll(1, 0), lastLeftAt = PreviewScroll.Top
            )
        )
    }
}

/**
 * The one-character step the editor takes away from its caret and back to
 * re-reveal it once the keyboard has settled (#464).
 */
class NudgeOffsetTest {

    @Test
    fun `steps right in the middle of the text`() {
        assertEquals(6, nudgeOffset(offset = 5, length = 40))
    }

    @Test
    fun `steps right from the very start`() {
        assertEquals(1, nudgeOffset(offset = 0, length = 40))
    }

    @Test
    fun `steps left at the end because there is nothing to the right`() {
        assertEquals(39, nudgeOffset(offset = 40, length = 40))
    }

    @Test
    fun `an empty note has nowhere to step`() {
        assertNull(nudgeOffset(offset = 0, length = 0))
    }

    @Test
    fun `a one character note steps to the only other place`() {
        assertEquals(1, nudgeOffset(offset = 0, length = 1))
        assertEquals(0, nudgeOffset(offset = 1, length = 1))
    }

    @Test
    fun `the step always lands inside the text`() {
        for (length in 1..6) {
            for (offset in 0..length) {
                val step = nudgeOffset(offset, length)!!
                assertEquals("offset $offset of $length", true, step in 0..length && step != offset)
            }
        }
    }
}

/**
 * [awaitKeyboardSettled] under virtual time, so "600 ms" is a number the test
 * reads off the scheduler rather than a wait it sits through.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AwaitKeyboardSettledTest {

    private fun readings(vararg heights: Int): () -> Int {
        val queue = ArrayDeque(heights.toList())
        // The last reading repeats once the list runs out, like a keyboard that has stopped.
        return { if (queue.size > 1) queue.removeFirst() else queue.first() }
    }

    @Test
    fun `returns once the keyboard has stopped growing`() = runTest {
        // The keyboard slides in over a few polls, then holds at 900px.
        awaitKeyboardSettled(readings(0, 0, 180, 520, 860, 900, 900, 900), pollMs = 60)

        // Readings at 0,60,...: 0,0,180,520,860,900 keep changing or stay hidden;
        // the seventh, at 360 ms, is the first repeat of a nonzero height.
        assertEquals(360L, testScheduler.currentTime)
    }

    @Test
    fun `a keyboard already up and still is settled on the second reading`() = runTest {
        awaitKeyboardSettled(readings(900), pollMs = 60)

        assertEquals(60L, testScheduler.currentTime)
    }

    @Test
    fun `two hidden readings in a row are not settled`() = runTest {
        // Zero twice is "no keyboard yet", not "keyboard finished".
        awaitKeyboardSettled(readings(0, 0, 0, 700, 700), pollMs = 60)

        assertEquals(240L, testScheduler.currentTime)
    }

    @Test
    fun `a keyboard that never comes waits for the caller's timeout and no less`() = runTest {
        val result = withTimeoutOrNull(FOLLOW_KEYBOARD_MS) {
            awaitKeyboardSettled(readings(0), pollMs = 60)
        }

        assertNull(result)
        assertEquals(FOLLOW_KEYBOARD_MS, testScheduler.currentTime)
    }
}
