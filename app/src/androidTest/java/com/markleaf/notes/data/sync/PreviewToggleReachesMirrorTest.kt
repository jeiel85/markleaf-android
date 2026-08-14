package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.core.markdown.MarkdownEditActions
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import java.io.File
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Closes the #219 gap from #262: the preview checkbox toggle is covered up to
 * the callback (`MarkdownTaskToggleClickTest`) and the string edit
 * (`MarkdownEditActionsTest`), but that the flipped marker lands in the note
 * *and* in the synced folder file was only ever checked by hand on an
 * emulator. This drives the same steps the editor's autosave drives — toggle
 * the marker, write the note through the repository, mirror it — over the real
 * IO harness from [NoteFolderMirrorFolderTest], then reads the file back.
 *
 * Where that boundary is, precisely, because it is narrower than "the toggle
 * reaches the file": the steps are called directly rather than through
 * `EditorScreen`, so this stays green if the screen's callback stops updating
 * `editorState` or stops triggering the save. What it pins is that *those
 * steps, in that order,* produce a correctly rewritten file — the part that was
 * hand-verified.
 *
 * The other half is now covered too:
 * `EditorScreenTest.editorScreen_previewCheckboxTapReachesTheSavedNote` drives
 * the real screen from the tap to the saved note. It stops at the repository,
 * because the mirror write resolves its folder with `DocumentFile.fromTreeUri`
 * and a tree Uri needs a SAF grant a person has to tap — so the repository is
 * where the two tests meet, and the join is a boundary rather than a gap.
 */
@RunWith(AndroidJUnit4::class)
class PreviewToggleReachesMirrorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dir: File
    private lateinit var folder: DocumentFile
    private lateinit var db: AppDatabase
    private lateinit var repo: LocalNoteRepository

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "preview-toggle-mirror-test").apply {
            deleteRecursively()
            mkdirs()
        }
        folder = DocumentFile.fromFile(dir)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repo = LocalNoteRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    private fun note(id: String = "note-1", title: String = "Plan", body: String) = Note(
        id = id,
        title = title,
        contentMarkdown = body,
        excerpt = body.take(40),
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000)
    )

    @Test
    fun previewCheckboxToggle_reachesTheNoteAndTheMirrorFile() = runBlocking {
        val originalBody = "# Plan\n\n- [ ] first\n- [ ] second"
        val original = note(body = originalBody)
        repo.createNote(original)
        assertTrue(NoteFolderMirror.writeNoteInto(context, folder, original, SyncFileExtension.MD))

        // The preview tap reports the row's source line (#219); the editor turns
        // that into the string edit and triggers a save. Autosave then persists
        // the note and mirrors it.
        val toggledBody = MarkdownEditActions.toggleTaskAtLine(originalBody, 3)!!
        val toggled = original.copy(contentMarkdown = toggledBody)
        repo.updateNote(toggled)
        assertTrue(NoteFolderMirror.writeNoteInto(context, folder, toggled, SyncFileExtension.MD))

        // The flip landed in the note...
        assertEquals(toggledBody, repo.getNote("note-1")?.contentMarkdown)
        assertTrue("note body has the flipped marker", toggledBody.contains("- [x] second"))

        // ...and in the synced file, exactly one file — no fork, no stale copy.
        val mirrorFiles = dir.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".md") }
        assertEquals("toggle must rewrite the existing mirror, not fork a copy", 1, mirrorFiles.size)
        val fileText = mirrorFiles.single().readText()
        assertTrue("mirror file still carries the note's id: $fileText", fileText.contains("markleaf_id: note-1"))
        assertTrue("mirror file has the flipped marker: $fileText", fileText.contains("- [x] second"))
        assertFalse("mirror file no longer has the open marker: $fileText", fileText.contains("- [ ] second"))
    }

    @Test
    fun previewCheckboxToggle_roundTripsTheOtherMarkerThroughTheFile() = runBlocking {
        val originalBody = "- [x] done"
        val original = note(body = originalBody)
        repo.createNote(original)
        assertTrue(NoteFolderMirror.writeNoteInto(context, folder, original, SyncFileExtension.MD))

        val toggledBody = MarkdownEditActions.toggleTaskAtLine(originalBody, 0)!!
        val toggled = original.copy(contentMarkdown = toggledBody)
        repo.updateNote(toggled)
        assertTrue(NoteFolderMirror.writeNoteInto(context, folder, toggled, SyncFileExtension.MD))

        val mirrorFiles = dir.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(".md") }
        assertEquals(1, mirrorFiles.size)
        assertTrue(mirrorFiles.single().readText().contains("- [ ] done"))
    }
}
