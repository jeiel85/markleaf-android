package com.markleaf.notes.util

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.TrashRetention
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The trash retention sweep (#566): which notes are due, and what deleting them removes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrashPurgeTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val now: Instant = Instant.parse("2026-11-02T12:00:00Z")

    @Test
    fun neverDeletesNothing() {
        val notes = listOf(trashed("old", now.minus(Duration.ofDays(400))))

        assertTrue(TrashPurge.expired(notes, TrashRetention.NEVER, now).isEmpty())
    }

    @Test
    fun aNoteIsDueOnceItHasBeenInTheTrashForTheWholePeriod() {
        val notes = listOf(
            trashed("exactly-a-week", now.minus(Duration.ofDays(7))),
            trashed("a-minute-short", now.minus(Duration.ofDays(7)).plusSeconds(60)),
            trashed("a-month", now.minus(Duration.ofDays(30)))
        )

        val due = TrashPurge.expired(notes, TrashRetention.ONE_WEEK, now).map { it.id }

        assertEquals(listOf("exactly-a-week", "a-month"), due)
    }

    @Test
    fun aNoteWithNoDeletionDateOrOutOfTheTrashIsNeverDue() {
        val notes = listOf(
            trashed("no-date", deletedAt = null),
            trashed("restored", now.minus(Duration.ofDays(90))).copy(trashed = false)
        )

        assertTrue(TrashPurge.expired(notes, TrashRetention.ONE_DAY, now).isEmpty())
    }

    @Test
    fun purgingDeletesDueNotesAndTheirAttachmentsOnly() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val due = trashed("due", now.minus(Duration.ofDays(2)))
        val recent = trashed("recent", now.minus(Duration.ofHours(3)))
        val dueAttachment = attachment(context, due.id)
        val recentAttachment = attachment(context, recent.id)
        val repository = RecordingRepository(listOf(due, recent))

        val purged = TrashPurge.purgeExpired(
            context, repository, AppSettings(trashRetention = TrashRetention.ONE_DAY), now
        )

        assertEquals(1, purged)
        assertEquals(listOf("due"), repository.deletedForever)
        assertFalse("the due note's attachments are removed", dueAttachment.exists())
        assertTrue("a note still within the period keeps its attachments", recentAttachment.exists())
    }

    @Test
    fun purgingWithRetentionOffTouchesNothing() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = RecordingRepository(listOf(trashed("old", now.minus(Duration.ofDays(400)))))

        val purged = TrashPurge.purgeExpired(context, repository, AppSettings(), now)

        assertEquals(0, purged)
        assertTrue(repository.deletedForever.isEmpty())
    }

    /**
     * Codex review of #569: with the folder out of reach, deleting the row and
     * leaving the file would let the next reconcile create the note again.
     * The sweep keeps the note in the trash and tries on a later launch.
     */
    @Test
    fun aLinkedFolderThatCannotBeReachedPostponesTheSweep() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = RecordingRepository(listOf(trashed("due", now.minus(Duration.ofDays(2)))))
        val settings = AppSettings(
            trashRetention = TrashRetention.ONE_DAY,
            syncFolderUri = "content://com.android.externalstorage.documents/tree/primary%3ANotes"
        )

        val purged = TrashPurge.purgeExpired(context, repository, settings, now, resolveFolder = { null })

        assertEquals(0, purged)
        assertTrue(repository.deletedForever.isEmpty())
    }

    @Test
    fun aDueNotesFileInTheSyncFolderGoesWithIt() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dir = temporaryFolder.newFolder("notes")
        val dueFile = File(dir, "Due.md").apply { writeText("---\nmarkleaf_id: due\n---\n# Due\n") }
        val keptFile = File(dir, "Recent.md").apply { writeText("---\nmarkleaf_id: recent\n---\n# Recent\n") }
        val repository = RecordingRepository(
            listOf(trashed("due", now.minus(Duration.ofDays(2))), trashed("recent", now.minus(Duration.ofHours(1))))
        )
        val settings = AppSettings(trashRetention = TrashRetention.ONE_DAY, syncFolderUri = "content://unused")

        val purged = TrashPurge.purgeExpired(
            context, repository, settings, now, resolveFolder = { DocumentFile.fromFile(dir) }
        )

        assertEquals(1, purged)
        assertEquals(listOf("due"), repository.deletedForever)
        assertFalse(dueFile.exists())
        assertTrue(keptFile.exists())
    }

    /**
     * Codex review of #569: a note restored after the sweep listed the trash
     * must keep its row and its attachments — the sweep reads it again before
     * touching anything.
     */
    @Test
    fun aNoteRestoredAfterTheTrashWasListedIsLeftAlone() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val listed = trashed("restored", now.minus(Duration.ofDays(3)))
        val keptAttachment = attachment(context, listed.id)
        val repository = RecordingRepository(
            trashedNotes = listOf(listed),
            live = mapOf(listed.id to listed.copy(trashed = false, deletedAt = null))
        )

        val purged = TrashPurge.purgeExpired(
            context, repository, AppSettings(trashRetention = TrashRetention.ONE_DAY), now
        )

        assertEquals(0, purged)
        assertTrue(repository.deletedForever.isEmpty())
        assertTrue(keptAttachment.exists())
    }

    private fun attachment(context: Context, noteId: String): File =
        File(File(File(context.filesDir, "attachments"), noteId), "image.png").apply {
            parentFile!!.mkdirs()
            writeText("png")
        }

    private fun trashed(id: String, deletedAt: Instant?) = Note(
        id = id,
        title = id,
        contentMarkdown = "# $id",
        excerpt = "",
        createdAt = now.minus(Duration.ofDays(500)),
        updatedAt = now.minus(Duration.ofDays(500)),
        trashed = true,
        deletedAt = deletedAt
    )

    /**
     * [trashedNotes] is what the trash listing returns; [live] is what reading
     * one note returns, which a test can set apart to stand in for a restore
     * that landed after the listing was read.
     */
    private class RecordingRepository(
        private val trashedNotes: List<Note>,
        private val live: Map<String, Note> = trashedNotes.associateBy { it.id }
    ) : NoteRepository {
        val deletedForever = mutableListOf<String>()

        override fun observeNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun getNote(noteId: String): Note? = live[noteId]
        override fun observeNote(noteId: String): Flow<Note?> = flowOf(null)
        override suspend fun getAllNotes(): List<Note> = trashedNotes
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun updateDerivedTitle(noteId: String, title: String, excerpt: String) = Unit
        override suspend fun moveToTrash(noteId: String) = Unit
        override suspend fun setPinned(noteId: String, pinned: Boolean) = Unit
        override suspend fun setArchived(noteId: String, archived: Boolean) = Unit
        override suspend fun restoreFromTrash(noteId: String) = Unit
        override suspend fun deleteForever(noteId: String) {
            deletedForever += noteId
        }
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(trashedNotes)
        override fun observeArchivedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeLockedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun setLocked(noteId: String, locked: Boolean) = Unit
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
