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
}
