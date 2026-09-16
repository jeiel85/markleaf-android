package com.markleaf.notes.feature.editor

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorSaveTimeTest {
    private val openedAt = Instant.parse("2026-01-01T00:00:00Z")
    private val now = Instant.parse("2026-01-02T00:00:00Z")

    @Test fun openingTheKeyboardOrMovingTheCaretDoesNotChangeTheTimestamp() {
        assertNull(editorSaveTime("original", "original", "original", openedAt, now))
    }

    @Test fun typingChangesTheTimestamp() {
        assertEquals(now, editorSaveTime("original", "edited", "original", openedAt, now))
    }

    @Test fun undoingAnAlreadySavedEditRestoresTheOpeningTimestamp() {
        assertEquals(openedAt, editorSaveTime("edited", "original", "original", openedAt, now))
    }

    @Test fun undoingBeforeAutosaveLeavesTheTimestampUntouched() {
        assertNull(editorSaveTime("original", "original", "original", openedAt, now))
    }
}
