package com.markleaf.notes.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The jump control is meant to appear only on notes long enough to need it
 * (#214) — a button floating over every two-line note would be clutter in an
 * app whose whole posture is staying out of the way.
 */
class JumpToEndButtonTest {

    @Test
    fun `a short note gets no jump control`() {
        assertFalse(isLongEnoughToJump(""))
        assertFalse(isLongEnoughToJump("just a line"))
        assertFalse(isLongEnoughToJump("a\nb\nc"))
    }

    @Test
    fun `the threshold counts lines, not newlines`() {
        // A note of exactly the threshold has one fewer newline than it has
        // lines. Off by one here would hide the control on the first note that
        // qualifies, which is impossible to notice from the outside.
        val atThreshold = (1..JUMP_CONTROL_MIN_LINES).joinToString("\n") { "line $it" }
        val justUnder = (1 until JUMP_CONTROL_MIN_LINES).joinToString("\n") { "line $it" }

        assertEquals(JUMP_CONTROL_MIN_LINES - 1, atThreshold.count { it == '\n' })
        assertTrue(isLongEnoughToJump(atThreshold))
        assertFalse(isLongEnoughToJump(justUnder))
    }

    @Test
    fun `a long note gets the control however short its lines are`() {
        assertTrue(isLongEnoughToJump("\n".repeat(200)))
    }
}
