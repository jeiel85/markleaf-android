package com.markleaf.notes.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalTagRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LocalTagRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = LocalTagRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `reindexTagsForNote stores tags for string note id`() = runTest {
        db.noteDao().insertNote(
            NoteEntity(
                id = "note-1",
                title = "Note",
                contentMarkdown = "Body #work #한글",
                excerpt = "Body",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        repository.reindexTagsForNote("note-1", "Body #work #한글")

        val tags = repository.observeTagsForNote("note-1").first()
        assertEquals(listOf("work", "한글"), tags.map { it.name })
    }

    @Test
    fun `observeTagSummaries counts active notes for each tag`() = runTest {
        db.noteDao().insertNote(
            NoteEntity(
                id = "active-1",
                title = "Active 1",
                contentMarkdown = "Body #work #home",
                excerpt = "Body",
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        db.noteDao().insertNote(
            NoteEntity(
                id = "active-2",
                title = "Active 2",
                contentMarkdown = "Body #work",
                excerpt = "Body",
                createdAt = 2L,
                updatedAt = 2L
            )
        )
        db.noteDao().insertNote(
            NoteEntity(
                id = "trashed",
                title = "Trashed",
                contentMarkdown = "Body #work",
                excerpt = "Body",
                createdAt = 3L,
                updatedAt = 3L,
                trashed = true,
                deletedAt = 4L
            )
        )

        repository.reindexTagsForNote("active-1", "Body #work #home")
        repository.reindexTagsForNote("active-2", "Body #work")
        repository.reindexTagsForNote("trashed", "Body #work")

        val countsByTag = repository.observeTagSummaries()
            .first()
            .associate { it.tag.name to it.noteCount }

        assertEquals(mapOf("home" to 1, "work" to 2), countsByTag)
    }
}
