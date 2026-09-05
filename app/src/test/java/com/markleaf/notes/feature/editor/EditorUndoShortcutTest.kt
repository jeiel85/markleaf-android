package com.markleaf.notes.feature.editor

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The hardware-keyboard undo keymap, pinned the way the formatting one is. */
class EditorUndoShortcutTest {
    @Test
    fun zUndoesAndShiftZRedoes() {
        assertEquals(UndoAction.UNDO, undoShortcutFor(Key.Z, shiftPressed = false))
        assertEquals(UndoAction.REDO, undoShortcutFor(Key.Z, shiftPressed = true))
    }

    /** What a keyboard carried over from Windows reaches for. */
    @Test
    fun yAlsoRedoes() {
        assertEquals(UndoAction.REDO, undoShortcutFor(Key.Y, shiftPressed = false))
    }

    @Test
    fun shiftYIsUnbound() {
        assertNull(undoShortcutFor(Key.Y, shiftPressed = true))
    }

    @Test
    fun theFormattingKeymapIsNotDisturbed() {
        listOf(Key.B, Key.I, Key.K, Key.S, Key.A, Key.Tab, Key.Enter).forEach { key ->
            assertNull("expected $key to be unbound", undoShortcutFor(key, shiftPressed = false))
            assertNull("expected shift+$key to be unbound", undoShortcutFor(key, shiftPressed = true))
        }
        assertNull(formattingShortcutFor(Key.Z, shiftPressed = false))
        assertNull(formattingShortcutFor(Key.Y, shiftPressed = false))
    }
}
