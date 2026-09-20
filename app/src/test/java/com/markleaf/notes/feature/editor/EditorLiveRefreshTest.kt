package com.markleaf.notes.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The decision behind the open editor's live refresh (#428).
 *
 * The bug: `EditorScreen` reads its note once and then stops listening, so a
 * version the folder reconcile imports while the note is open only appears
 * after leaving the screen and coming back. The screen now follows the row —
 * and the only hard part is knowing when *not* to follow it, which is what
 * these cases pin. Everything else in that path is Room and composition; this
 * is the part that can be got wrong silently.
 */
class EditorLiveRefreshTest {

    @Test fun anImportedVersionIsAdoptedWhenNothingIsUnsaved() {
        assertEquals(
            "from the folder",
            editorLiveRefreshText(onScreen = "as loaded", agreed = "as loaded", incoming = "from the folder")
        )
    }

    @Test fun keystrokesWaitingForTheDebounceAreNeverOverwritten() {
        // The user has typed; the save hasn't fired yet. Adopting here would
        // delete text they can still see on screen.
        assertNull(
            editorLiveRefreshText(onScreen = "typing…", agreed = "as loaded", incoming = "from the folder")
        )
    }

    @Test fun ourOwnSaveComingBackIsNotNews() {
        assertNull(
            editorLiveRefreshText(onScreen = "just saved", agreed = "as loaded", incoming = "just saved")
        )
    }

    @Test fun aRowChangeThatLeavesTheTextAloneIsNotNews() {
        // A `lastImportedAt` stamp, a retitle, a pin: the row version changes,
        // the text does not.
        assertNull(
            editorLiveRefreshText(onScreen = "unchanged", agreed = "unchanged", incoming = "unchanged")
        )
    }

    @Test fun aNoteDeletedElsewhereDoesNotBlankTheScreen() {
        assertNull(
            editorLiveRefreshText(onScreen = "still here", agreed = "still here", incoming = null)
        )
    }

    @Test fun nothingIsAdoptedBeforeTheNoteHasFinishedLoading() {
        // No agreement point yet: there is nothing to tell an import apart from
        // the empty text the field holds while the row is being read.
        assertNull(
            editorLiveRefreshText(onScreen = "", agreed = null, incoming = "from the folder")
        )
    }

    @Test fun anImportThatEmptiesTheNoteIsNotAdopted() {
        // Not squeamishness about the empty string: #405 permanently deletes a
        // note left blank when the editor closes, along with its attachments
        // and its file in the sync folder. Adopting an empty version would feed
        // that rule a note the user never cleared.
        assertNull(
            editorLiveRefreshText(onScreen = "as loaded", agreed = "as loaded", incoming = "")
        )
    }

    @Test fun aNoteThatWasAlreadyEmptyIsUntouched() {
        assertNull(editorLiveRefreshText(onScreen = "", agreed = "", incoming = ""))
    }

    @Test fun adoptingTwiceInARowFollowsEachVersion() {
        val first = editorLiveRefreshText(onScreen = "v1", agreed = "v1", incoming = "v2")
        assertEquals("v2", first)
        // After adopting, the screen and the agreement point are both v2, so a
        // third version still lands.
        assertEquals(
            "v3",
            editorLiveRefreshText(onScreen = first!!, agreed = first, incoming = "v3")
        )
    }
}
