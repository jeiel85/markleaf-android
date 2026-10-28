package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
 * A sidecar-mode file renamed outside Markleaf — `mv` in a shell, a desktop
 * editor's "rename", a file manager — while its note stays the same (#429).
 *
 * Sidecar files carry no header, so the only thing tying a file to its note is
 * the index entry's filename. A rename breaks that link, and before this fix
 * the import read the renamed file as a hand-dropped one: a second note. The
 * next save of the original then found nothing under the indexed name and
 * wrote a second file too. What is left to match on is the content: the entry
 * records the hash of what was last written, and a file that still holds
 * exactly that is the same note under a new name.
 *
 * Real [DocumentFile] trees over a temp directory, the technique
 * [MirrorTraversalTest] uses, so the matching, the index and the writes are the
 * production ones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SidecarExternalRenameTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = temporaryFolder.newFolder("sidecar-rename-test")
        folder = DocumentFile.fromFile(dir)
        SidecarStore.forget()
    }

    @After
    fun tearDown() {
        SidecarStore.forget()
    }

    private fun note(id: String = "note-1", body: String = "# Plan\n\nfirst") = Note(
        id = id,
        title = body.lineSequence().first().removePrefix("# "),
        contentMarkdown = body,
        excerpt = body.take(40),
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000)
    )

    private fun save(n: Note, device: String = DEVICE, keepFileName: Boolean = false) =
        NoteFolderMirror.writeNoteInto(
            context, folder, n, SyncFileExtension.MD, MirrorMetadata.Sidecar(device), keepFileName
        )

    private class Imported(val result: NoteFolderMirror.ImportResult, val created: List<Note>)

    private fun import(existing: List<Note>, device: String = DEVICE): Imported = runBlocking {
        val created = mutableListOf<Note>()
        val result = NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = existing,
            applyUpdate = { },
            applyCreate = { created.add(it) },
            metadata = MirrorMetadata.Sidecar(device)
        )
        Imported(result, created)
    }

    private fun markdownFiles() = dir.listFiles().orEmpty().filter { it.name.endsWith(".md") }

    private fun indexOf(device: String) = SidecarStore.readAll(context, folder)
        .firstOrNull { it.deviceId == device }
        ?.entries
        .orEmpty()

    private fun renameOnlyFile(to: String): File {
        val original = markdownFiles().single()
        val renamed = File(dir, to)
        assertTrue("the rename must succeed for this to test anything", original.renameTo(renamed))
        return renamed
    }

    @Test
    fun aRenamedFileStaysTheSameNoteThroughImportAndTheNextSave() {
        val plan = note()
        assertTrue(save(plan))
        renameOnlyFile("pl.md")

        val imported = import(listOf(plan))
        assertEquals("a rename is not a new note", 0, imported.result.created)
        assertTrue(imported.created.isEmpty())
        assertEquals(
            "the index must follow the file to its new name",
            "pl.md",
            indexOf(DEVICE).single { it.noteId == plan.id }.fileName
        )

        val edited = plan.copy(contentMarkdown = "# Plan\n\nsecond")
        assertTrue(save(edited))
        val files = markdownFiles()
        assertEquals("the save must write the renamed file, not fork a new one", 1, files.size)
        assertEquals("# Plan\n\nsecond", files.single().readText())
        assertEquals(files.single().name, indexOf(DEVICE).single().fileName)

        val again = import(listOf(edited))
        assertEquals(0, again.result.created)
    }

    @Test
    fun withFileNamesKeptTheRenamedFileKeepsItsNameThroughTheNextSave() {
        // The case #429 asked for: a short name given in a shell. The import
        // has to carry the new name into the index for "keep file names" to
        // have a file to keep — without it the save finds nothing under the
        // indexed name and writes the title-named file again.
        val plan = note()
        assertTrue(save(plan, keepFileName = true))
        renameOnlyFile("pl.md")

        assertEquals(0, import(listOf(plan)).result.created)
        assertTrue(save(plan.copy(contentMarkdown = "# Plan\n\nsecond"), keepFileName = true))

        val file = markdownFiles().single()
        assertEquals("pl.md", file.name)
        assertEquals("# Plan\n\nsecond", file.readText())
        assertEquals("pl.md", indexOf(DEVICE).single().fileName)
    }

    @Test
    fun aRenameIsFollowedAfterARestartToo() {
        // The cached own index is gone after a process restart, so the match has
        // to come from what is on disk.
        val plan = note()
        assertTrue(save(plan))
        renameOnlyFile("p.md")
        SidecarStore.forget()
        MirrorFileLookup.MirrorFileCache.forget(plan.id)

        assertEquals(0, import(listOf(plan)).result.created)
        assertEquals("p.md", indexOf(DEVICE).single().fileName)
    }

    @Test
    fun aRenameMatchedThroughAnotherDevicesEntryLeavesThatDevicesIndexAlone() {
        // The note was written by the other device; this one has only ever
        // read it, so the entry it matches on is not its own.
        val plan = note()
        assertTrue(save(plan, device = OTHER))
        val otherIndexBefore = File(dir, SidecarIndex.fileNameFor(OTHER)).readText()
        assertEquals(0, import(listOf(plan)).result.created)

        renameOnlyFile("pl.md")
        val imported = import(listOf(plan))

        assertEquals(0, imported.result.created)
        assertEquals(
            "another device's index is never written by this one",
            otherIndexBefore,
            File(dir, SidecarIndex.fileNameFor(OTHER)).readText()
        )
        assertEquals("pl.md", indexOf(DEVICE).single { it.noteId == plan.id }.fileName)
    }

    @Test
    fun aFileRenamedAndEditedIsStillANewNote() {
        // The documented limit: once the content moved as well, nothing ties
        // the file to the note any more.
        val plan = note()
        assertTrue(save(plan))
        renameOnlyFile("pl.md").writeText("# Plan\n\nedited in vim")

        val imported = import(listOf(plan))
        assertEquals(1, imported.result.created)
        assertTrue(imported.created.single().id != plan.id)
    }

    @Test
    fun aCopyBesideTheOriginalIsANewNote() {
        // The original is still under its indexed name, so its entry is not
        // missing and the copy has nothing to take over.
        val plan = note()
        assertTrue(save(plan))
        val original = markdownFiles().single()
        original.copyTo(File(dir, "plan copy.md"))

        val imported = import(listOf(plan))
        assertEquals(1, imported.result.created)
        assertEquals(original.name, indexOf(DEVICE).single { it.noteId == plan.id }.fileName)
    }

    @Test
    fun twoMissingNotesWithTheSameContentAreNotGuessedBetween() {
        // Identical content (two empty templates, say) gives no way to tell
        // which note a renamed file was. Guessing would swap them; taking
        // neither keeps both notes intact and imports the files as new.
        val a = note(id = "note-a", body = "# Same\n\ntext")
        val b = note(id = "note-b", body = "# Same\n\ntext")
        assertTrue(save(a))
        assertTrue(save(b))
        val files = markdownFiles().sortedBy { it.name }
        assertEquals(2, files.size)
        files.forEachIndexed { i, f -> assertTrue(f.renameTo(File(dir, "renamed-$i.md"))) }

        val imported = import(listOf(a, b))
        assertEquals(2, imported.result.created)
        assertTrue(imported.created.none { it.id == a.id || it.id == b.id })
    }

    @Test
    fun theSurveyCountsARenamedFileAsKnown() {
        val plan = note()
        assertTrue(save(plan))
        renameOnlyFile("pl.md")

        val survey = NoteFolderMirror.surveyFolderIn(
            context, folder, listOf(plan), MirrorMetadata.Sidecar(DEVICE)
        )
        assertEquals("the survey must promise what the import does", 0, survey.newFiles)
        assertEquals(1, survey.knownFiles)
    }

    private companion object {
        const val DEVICE = "device-1"
        const val OTHER = "device-2"
    }
}
