package com.markleaf.notes.feature.editor

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun failedMirrorWriteIsRetriedWithoutAContentChange() {
        assertTrue(editorNeedsMirrorRetry(true, false, openedAt, now))
        assertFalse(editorNeedsMirrorRetry(true, false, now, now))
    }

    @Test fun failedMirrorWriteAfterUndoIsRetriedEvenWhenTheTimeMovesBackwards() {
        assertTrue(editorNeedsMirrorRetry(true, false, now, openedAt))
    }

    @Test fun unchangedLockedOrUnsyncedNotesDoNotWriteToTheMirror() {
        assertFalse(editorNeedsMirrorRetry(false, false, null, now))
        assertFalse(editorNeedsMirrorRetry(true, true, null, now))
    }
}
