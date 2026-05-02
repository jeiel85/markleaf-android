package com.markleaf.notes.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class LocalNoteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LocalNoteRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = LocalNoteRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `updateNote reindexes wiki links for backlinks`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val target = Note(
            id = "target-note",
            title = "Target",
            contentMarkdown = "# Target",
            excerpt = "Target",
            createdAt = now,
            updatedAt = now
        )
        val source = Note(
            id = "source-note",
            title = "Source",
            contentMarkdown = "See [[Target]]",
            excerpt = "See Target",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(target)
        repository.createNote(source)
        repository.updateNote(source)

        val backlinks = repository.getBacklinks("target-note").first()
        assertEquals(listOf("source-note"), backlinks.map { it.id })
    }

    @Test
    fun `updateNote stores previous content as snapshot`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "note",
            title = "Original",
            contentMarkdown = "Original body",
            excerpt = "Original body",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)
        repository.updateNote(
            note.copy(
                title = "Updated",
                contentMarkdown = "Updated body",
                excerpt = "Updated body",
                updatedAt = Instant.ofEpochMilli(2L)
            )
        )

        val snapshots = repository.getSnapshots("note")
        assertEquals(1, snapshots.size)
        assertEquals("Original", snapshots.first().title)
        assertEquals("Original body", snapshots.first().contentMarkdown)
    }

    @Test
    fun `restoreSnapshot restores content and snapshots current version`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "note",
            title = "Original",
            contentMarkdown = "Original body",
            excerpt = "Original body",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)
        repository.updateNote(
            note.copy(
                title = "Updated",
                contentMarkdown = "Updated body",
                excerpt = "Updated body",
                updatedAt = Instant.ofEpochMilli(2L)
            )
        )

        val snapshot = repository.getSnapshots("note").first()
        val restored = repository.restoreSnapshot(snapshot.id)

        assertEquals("Original", restored?.title)
        assertEquals("Original body", repository.getNote("note")?.contentMarkdown)
        assertEquals(
            listOf("Updated body", "Original body"),
            repository.getSnapshots("note").map { it.contentMarkdown }
        )
    }

    @Test
    fun `searchNotes uses indexed fts path for large local datasets`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        repeat(10_000) { index ->
            repository.createNote(
                Note(
                    id = "note-$index",
                    title = "Note $index",
                    contentMarkdown = if (index == 9_876) {
                        "needle-token body"
                    } else {
                        "ordinary body $index"
                    },
                    excerpt = "ordinary body",
                    createdAt = now,
                    updatedAt = now.plusMillis(index.toLong())
                )
            )
        }

        val elapsedMillis = kotlin.system.measureTimeMillis {
            val results = repository.searchNotes("needle-token").first()
            assertEquals(listOf("note-9876"), results.map { it.id })
        }

        assertTrue("Search took ${elapsedMillis}ms", elapsedMillis < 2_000)
    }
}
