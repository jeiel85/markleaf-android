package com.markleaf.notes.feature.editor

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The formatting keymap is the single source of truth shared by the editor text
 * field and the expanded formatting panel, so it is worth pinning directly.
 */
class EditorFormattingShortcutTest {
    @Test
    fun boldItalicAndLinkMapToTheirActions() {
        assertEquals(EditorFormattingAction.BOLD, formattingShortcutFor(Key.B, shiftPressed = false))
        assertEquals(EditorFormattingAction.ITALIC, formattingShortcutFor(Key.I, shiftPressed = false))
        assertEquals(EditorFormattingAction.LINK, formattingShortcutFor(Key.K, shiftPressed = false))
    }

    @Test
    fun modifierlessShortcutsStayBoundWhenShiftIsHeld() {
        assertEquals(EditorFormattingAction.BOLD, formattingShortcutFor(Key.B, shiftPressed = true))
        assertEquals(EditorFormattingAction.ITALIC, formattingShortcutFor(Key.I, shiftPressed = true))
        assertEquals(EditorFormattingAction.LINK, formattingShortcutFor(Key.K, shiftPressed = true))
    }

    @Test
    fun strikethroughRequiresShift() {
        assertEquals(
            EditorFormattingAction.STRIKETHROUGH,
            formattingShortcutFor(Key.S, shiftPressed = true)
        )
    }

    @Test
    fun bareSIsUnboundSoReflexSaveDoesNotMangleText() {
        assertNull(formattingShortcutFor(Key.S, shiftPressed = false))
    }

    @Test
    fun unmappedKeysResolveToNull() {
        listOf(Key.A, Key.Z, Key.Tab, Key.Enter, Key.Escape, Key.DirectionDown).forEach { key ->
            assertNull("expected $key to be unbound", formattingShortcutFor(key, shiftPressed = false))
            assertNull("expected shift+$key to be unbound", formattingShortcutFor(key, shiftPressed = true))
        }
    }
}
