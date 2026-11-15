package com.markleaf.notes.feature.editor

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The selection row keeps "More options" on screen by dropping bold, italic
 * and link (all also in the panel) when the row runs out of 48dp slots
 * (Codex on #590).
 */
class SelectionShortcutCountTest {
    @Test
    fun withoutTheMoveButtonsTheRowIsAsBefore() {
        // Link joined bold and italic from 288dp, as it always has.
        assertEquals(3, selectionShortcutCount(288.dp, showUndo = true, showMoveLine = false))
        assertEquals(2, selectionShortcutCount(287.dp, showUndo = true, showMoveLine = false))
        assertEquals(3, selectionShortcutCount(320.dp, showUndo = false, showMoveLine = false))
    }

    @Test
    fun withTheMoveButtonsShortcutsGiveWayBeforeMoreOptions() {
        // A 360dp phone's row is about 320dp: six slots for undo, redo, up,
        // down, More, and one shortcut.
        assertEquals(1, selectionShortcutCount(320.dp, showUndo = true, showMoveLine = true))
        // A 320dp phone's row is about 280dp: five slots, no shortcut.
        assertEquals(0, selectionShortcutCount(280.dp, showUndo = true, showMoveLine = true))
        // Wide enough for all of them.
        assertEquals(3, selectionShortcutCount(384.dp, showUndo = true, showMoveLine = true))
        // Undo in the top bar frees two slots.
        assertEquals(3, selectionShortcutCount(320.dp, showUndo = false, showMoveLine = true))
    }

    @Test
    fun neverNegative() {
        assertEquals(0, selectionShortcutCount(100.dp, showUndo = true, showMoveLine = true))
    }
}
