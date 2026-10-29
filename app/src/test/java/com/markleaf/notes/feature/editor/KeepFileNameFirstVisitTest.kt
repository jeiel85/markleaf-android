package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.data.sync.MirrorFileLookup
import com.markleaf.notes.data.sync.MirrorMetadata
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.domain.model.Note
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

/**
 * "Keep file names" names a note's file after its title once (#429), and the
 * editor's autosave is what creates that file, a second after the first
 * keystroke. Keeping the name from that first save on froze a partial title:
 * typing "Groceries" with a pause after "Gro" left the note in `Gro.md` for
 * good. These run the editor's rule ([isJustCreated] at open, then
 * [keepsFileName] per save) against the real mirror writes, over a temp
 * directory as [com.markleaf.notes.data.sync.SidecarExternalRenameTest] does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeepFileNameFirstVisitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = temporaryFolder.newFolder("keep-file-name-test")
        folder = DocumentFile.fromFile(dir)
    }

    @After
    fun tearDown() {
        MirrorFileLookup.MirrorFileCache.forget(NOTE_ID)
    }

    private val created = Instant.ofEpochMilli(1_000)

    /** What the FAB makes: an empty row, never saved, never written out. */
    private fun newNote() = Note(
        id = NOTE_ID, title = "", contentMarkdown = "", excerpt = "",
        createdAt = created, updatedAt = created
    )

    /** The note as an autosave stores it: the title is the first heading. */
    private fun Note.typed(title: String, rest: String = "", at: Long) = copy(
        title = title,
        contentMarkdown = "# $title\n\n$rest",
        updatedAt = Instant.ofEpochMilli(at)
    )

    private fun save(note: Note, keepFileName: Boolean) {
        assertTrue(
            NoteFolderMirror.writeNoteInto(
                context, folder, note, SyncFileExtension.MD, MirrorMetadata.Frontmatter, keepFileName
            )
        )
    }

    private fun files() = dir.listFiles()?.map { it.name }?.sorted().orEmpty()

    @Test
    fun `the visit that creates a note names its file after the finished title`() {
        val opened = newNote()
        val keep = keepsFileName(keepFileNames = true, createdThisVisit = isJustCreated(opened))

        save(opened.typed("Gro", at = 2_000), keep)
        assertEquals(listOf("Gro.md"), files())

        save(opened.typed("Groceries", "milk", at = 3_000), keep)
        assertEquals(listOf("Groceries.md"), files())
    }

    @Test
    fun `from the next visit on the name stays`() {
        val firstVisit = newNote()
        val keepWhileCreating = keepsFileName(true, isJustCreated(firstVisit))
        save(firstVisit.typed("Gro", at = 2_000), keepWhileCreating)
        val stored = firstVisit.typed("Groceries", "milk", at = 3_000)
        save(stored, keepWhileCreating)

        // Reopened: the row has been saved and its file written, so it is no
        // longer the note the new-note flow just made.
        val reopened = stored.copy(lastImportedAt = stored.updatedAt)
        val keep = keepsFileName(true, isJustCreated(reopened))
        assertTrue(keep)

        save(reopened.typed("Shopping", "milk, eggs", at = 4_000), keep)
        assertEquals(listOf("Groceries.md"), files())
        assertTrue(File(dir, "Groceries.md").readText().contains("# Shopping"))
    }

    /** The default: names follow the title on every visit, as before #429. */
    @Test
    fun `with the setting off a name always follows the title`() {
        assertFalse(keepsFileName(keepFileNames = false, createdThisVisit = true))
        assertFalse(keepsFileName(keepFileNames = false, createdThisVisit = false))
    }

    private companion object {
        const val NOTE_ID = "note-1"
    }
}
