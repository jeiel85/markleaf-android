package com.markleaf.notes.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
}
