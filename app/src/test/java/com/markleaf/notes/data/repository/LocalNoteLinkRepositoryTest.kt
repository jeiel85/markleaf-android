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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalNoteLinkRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LocalNoteLinkRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = LocalNoteLinkRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insert(
        id: String,
        title: String,
        content: String,
        locked: Boolean = false,
        archived: Boolean = false
    ) {
        db.noteDao().insertNote(
            NoteEntity(
                id = id,
                title = title,
                contentMarkdown = content,
                excerpt = content,
                createdAt = 1L,
                updatedAt = 1L,
                locked = locked,
                archived = archived
            )
        )
        repository.reindexLinksForNote(id, content)
    }

    @Test
    fun `observeBacklinks lists notes that link to the title`() = runTest {
        insert("target", "Target", "Body")
        insert("source", "Source", "See [[Target]]")

        val backlinks = repository.observeBacklinks("Target", excludeNoteId = "target").first()

        assertEquals(listOf("source"), backlinks.map { it.id })
    }

    @Test
    fun `observeBacklinks excludes locked notes so their titles stay hidden`() = runTest {
        insert("target", "Target", "Body")
        insert("visible", "Visible source", "See [[Target]]")
        insert("secret", "Secret source", "See [[Target]]", locked = true)

        val backlinks = repository.observeBacklinks("Target", excludeNoteId = "target").first()

        // A backlink row shows the linking note's title, so a locked note must not
        // appear in a visible note's note-information sheet (#156).
        assertEquals(listOf("visible"), backlinks.map { it.id })
    }

    @Test
    fun `observeBacklinks still excludes archived notes`() = runTest {
        insert("target", "Target", "Body")
        insert("archived", "Archived source", "See [[Target]]", archived = true)

        val backlinks = repository.observeBacklinks("Target", excludeNoteId = "target").first()

        assertEquals(emptyList<String>(), backlinks.map { it.id })
    }
}
