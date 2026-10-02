package com.markleaf.notes.navigation

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [NoteOriginSaver] is what lets the host keep the tapped row's origin across a
 * recreation (#499). Pure JVM: the composition-level behaviour is covered by
 * `NoteOriginRestorationTest`; this pins the format the bundle sees.
 */
class NoteOriginSaverTest {

    private fun save(origin: NoteOrigin?): ArrayList<String>? =
        with(NoteOriginSaver) { SaverScope { true }.save(origin) }

    private fun restore(saved: ArrayList<String>): NoteOrigin? = NoteOriginSaver.restore(saved)

    @Test
    fun everySourceRoundTrips() {
        for (source in NoteSource.entries) {
            val origin = NoteOrigin("note-1", source)
            assertEquals(origin, restore(save(origin)!!))
        }
    }

    @Test
    fun noOriginSavesNothing() {
        assertNull(save(null))
    }

    @Test
    fun theSavedFormIsTheNoteIdAndTheSourceName() {
        val saved = save(NoteOrigin("note-1", NoteSource.TILE))!!
        assertEquals(arrayListOf("note-1", "TILE"), saved)
    }

    @Test
    fun aSourceThatNoLongerExistsRestoresAsNoOrigin() {
        assertNull(restore(arrayListOf("note-1", "REMOVED_SOURCE")))
    }

    @Test
    fun aTruncatedSavedValueRestoresAsNoOrigin() {
        assertNull(restore(arrayListOf("note-1")))
        assertNull(restore(arrayListOf()))
    }
}
