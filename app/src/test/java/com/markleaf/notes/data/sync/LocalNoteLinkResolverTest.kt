package com.markleaf.notes.data.sync

import com.markleaf.notes.data.local.entity.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [localNoteLinkVerdict] — what a local Markdown link (#414) does once its
 * filename has already resolved to a note id, or hasn't. The SAF/Room lookup
 * that produces that id is covered separately, in
 * [NoteFolderMirrorLocalLinkTest]; this is the behavioral contract a tap in
 * the editor preview or the read-only file viewer has to honour either way.
 */
class LocalNoteLinkResolverTest {

    private fun note(
        locked: Boolean = false,
        trashed: Boolean = false,
        archived: Boolean = false
    ) = NoteEntity(
        id = "note-1",
        title = "Target",
        contentMarkdown = "T",
        excerpt = "T",
        createdAt = 0,
        updatedAt = 0,
        locked = locked,
        trashed = trashed,
        archived = archived
    )

    @Test
    fun noNote_isNotFound() {
        assertEquals(LocalNoteLinkResult.NotFound, localNoteLinkVerdict(null))
    }

    @Test
    fun anActiveNote_opens() {
        assertEquals(LocalNoteLinkResult.Open("note-1"), localNoteLinkVerdict(note()))
    }

    @Test
    fun aLockedNote_reportsLocked_beforeTrashedOrArchivedWouldMatter() {
        assertEquals(LocalNoteLinkResult.Locked, localNoteLinkVerdict(note(locked = true)))
    }

    @Test
    fun aTrashedNote_isNotFound_notOpened() {
        // Matches the wikilink handler's own rule (EditorScreen): a link should
        // not resurrect a note the user deleted.
        assertEquals(LocalNoteLinkResult.NotFound, localNoteLinkVerdict(note(trashed = true)))
    }

    @Test
    fun anArchivedNote_isNotFound_notOpened() {
        assertEquals(LocalNoteLinkResult.NotFound, localNoteLinkVerdict(note(archived = true)))
    }

    @Test
    fun aLockedAndTrashedNote_isNotFound_trashedTakesPrecedence() {
        // Preserves the editor's pre-#414 ordering for this path: trashed/archived
        // is checked first, so a locked note that is also trashed reads as "not
        // found" rather than revealing it is locked. (The title-based wikilink
        // handler checks the opposite order — see the #262 note left for it.)
        assertEquals(LocalNoteLinkResult.NotFound, localNoteLinkVerdict(note(locked = true, trashed = true)))
    }
}
