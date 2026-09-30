package com.markleaf.notes.feature.editor

import com.markleaf.notes.core.markdown.MarkdownEditActions
import com.markleaf.notes.core.markdown.PreviewLine
import kotlinx.coroutines.delay

/** Where the preview list was scrolled to: its `firstVisibleItemIndex` and `firstVisibleItemScrollOffset`. */
internal data class PreviewScroll(val index: Int, val offset: Int) {
    companion object {
        /** The top of the list — where a preview that was never scrolled sits. */
        val Top = PreviewScroll(0, 0)
    }
}

/**
 * The caret offset to land on when the reader leaves Preview for the editor
 * (#464), or null to leave the caret exactly where it was.
 *
 * Someone who spots a sentence to change while reading a long note switches to
 * the editor to change it. Without this the editor opens at the caret they left
 * there before they scrolled, so they scroll again to find the same place. With
 * it the caret goes to the start of the block at the top of what they were
 * reading, and the editor brings it into view.
 *
 * [now] is the preview's scroll as the reader leaves, and [rows] is the list
 * that is actually laid out — the two have to be the same list for the index to
 * mean anything, which is why the caller passes the visible rows and not the
 * parsed ones (a collapsed `<details>` section drops rows from one and not the
 * other).
 *
 * Null in two cases, and both keep today's behaviour:
 *  - **The reader did not scroll since they last left Preview** ([lastLeftAt];
 *    [PreviewScroll.Top] before the first time). The list keeps its position
 *    between visits, so without this baseline someone who scrolled, edited at
 *    that spot and glanced back at Preview would have the caret thrown from
 *    what they had just typed to the top of the block they typed in. A note
 *    that fits on screen is the same case: it never moves off the top.
 *  - **No row at or above the top one knows its line** (or the line no longer
 *    exists in [text]). The caret is not moved to a guess.
 *
 * Looks upward from the top row because that is the direction a row's block
 * could have started in: a row a parser synthesized has no line, and the
 * nearest known one above it is the closest honest answer.
 */
internal fun caretOffsetForPreviewPosition(
    text: String,
    rows: List<PreviewLine>,
    now: PreviewScroll,
    lastLeftAt: PreviewScroll
): Int? {
    if (now == lastLeftAt) return null
    val line = (now.index.coerceAtMost(rows.lastIndex) downTo 0)
        .firstNotNullOfOrNull { rows[it].startLine }
        ?: return null
    return MarkdownEditActions.offsetOfLine(text, line)
}

/**
 * The most the editor waits, after leaving Preview, for a soft keyboard to come
 * up. It is a ceiling for the case where none does — a hardware keyboard, or an
 * IME the user has hidden — not an expected delay: on a phone whose keyboard is
 * slow to start it is the keyboard that decides, and a short ceiling would give
 * up on exactly the phones that need the help.
 */
internal const val FOLLOW_KEYBOARD_MS = 10_000L

/** How often [awaitKeyboardSettled] looks at the keyboard's height. */
internal const val KEYBOARD_SETTLE_POLL_MS = 60L

/**
 * Suspends until the soft keyboard is up and has stopped moving. Never returns
 * if it does not come, so callers bound it with a timeout.
 *
 * [keyboardHeight] is the keyboard's current height in pixels, 0 while it is
 * hidden. "Settled" is two readings in a row, [pollMs] apart, that are equal and
 * above zero: the height climbs over a few frames as the keyboard slides in, and
 * the first reading that stops changing is the last one. Two zeros are not
 * settled — that is "no keyboard yet".
 */
internal suspend fun awaitKeyboardSettled(
    keyboardHeight: () -> Int,
    pollMs: Long = KEYBOARD_SETTLE_POLL_MS
) {
    var previous = -1
    while (true) {
        val current = keyboardHeight()
        if (current > 0 && current == previous) return
        previous = current
        delay(pollMs)
    }
}

/**
 * An offset one character away from [offset], for moving the caret there and
 * straight back, or null when the note is empty and there is nowhere to go.
 *
 * Why the editor moves its caret at all after placing it (#464): a text field
 * scrolls to its caret when the caret *moves*, not when the room around it
 * shrinks. Place the caret while the view is full height and it is scrolled to
 * the bottom edge; the keyboard the same tap raises then covers that edge, and
 * the field does not scroll again until the next keystroke. Stepping the caret
 * away and back once the keyboard has settled is a move in the view that is
 * actually there. A neighbouring offset is on the same line, so the step never
 * scrolls anywhere the caret was not already going.
 *
 * At the end of the text the step goes left, because there is no character to
 * the right.
 */
internal fun nudgeOffset(offset: Int, length: Int): Int? = when {
    length <= 0 -> null
    offset < length -> offset + 1
    else -> offset - 1
}

/**
 * A caret move asked for on leaving Preview: the [offset] to go to, and the [text]
 * it was worked out against. If the note has changed before the editor acts on it,
 * the offset no longer means what it did and the move is dropped.
 */
internal data class PendingCaret(val offset: Int, val text: String)
