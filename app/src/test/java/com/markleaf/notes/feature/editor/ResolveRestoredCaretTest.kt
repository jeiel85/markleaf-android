package com.markleaf.notes.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The position-restore clamp/drop contract (#262).
 *
 * `EditorScreen` restores the caret from a saved `NoteViewStateEntity`, but the
 * note can be shorter than when the position was recorded — edited in another
 * app, or a smaller version brought in by sync — so the saved offset is never
 * trusted, only clamped. A null saved offset is a dropped restore. The status
 * lets the screen leave a debug breadcrumb instead of failing silently.
 *
 * These are pure functions, so they are tested directly rather than through a
 * device-bound load effect.
 */
class ResolveRestoredCaretTest {

    @Test
    fun savedOffsetWithinBounds_isOk() {
        val (caret, status) = resolveRestoredCaret(saved = 5, contentLength = 10)
        assertEquals(5, caret)
        assertEquals(RestoreStatus.OK, status)
    }

    @Test
    fun savedOffsetPastEnd_isClamped() {
        val (caret, status) = resolveRestoredCaret(saved = 15, contentLength = 10)
        assertEquals(10, caret)
        assertEquals(RestoreStatus.CLAMPED, status)
    }

    @Test
    fun noSavedPosition_isDropped() {
        val (caret, status) = resolveRestoredCaret(saved = null, contentLength = 10)
        assertEquals(0, caret)
        assertEquals(RestoreStatus.DROPPED, status)
    }

    @Test
    fun emptyNote_clampsToZero() {
        val (caret, status) = resolveRestoredCaret(saved = 5, contentLength = 0)
        assertEquals(0, caret)
        assertEquals(RestoreStatus.CLAMPED, status)
    }

    @Test
    fun savedAtExactEnd_isOk() {
        val (caret, status) = resolveRestoredCaret(saved = 10, contentLength = 10)
        assertEquals(10, caret)
        assertEquals(RestoreStatus.OK, status)
    }
}