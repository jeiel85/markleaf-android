package com.markleaf.notes.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteLinkRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteImporterTest {
    private lateinit var db: AppDatabase
    private lateinit var importer: NoteImporter

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        importer = NoteImporter(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun note(id: String, body: String, ts: Long = 1L) = Note(
        id = id,
        title = "T",
        contentMarkdown = body,
        excerpt = "T",
        createdAt = Instant.ofEpochMilli(ts),
        updatedAt = Instant.ofEpochMilli(ts)
    )

    @Test
    fun `create indexes tags from the imported body`() = runTest {
        // Regression for the sync gap: notes pulled from a folder used to be
        // inserted without their tags ever being parsed, so they showed up
        // tagless until the user re-saved each one.
        importer.create(note("n1", "From another app #work #idea"))

        val tags = LocalTagRepository(db).observeTagsForNote("n1").first().map { it.name }
        assertEquals(setOf("work", "idea"), tags.toSet())
    }

    @Test
    fun `create indexes wikilinks from the imported body`() = runTest {
        importer.create(note("source", "See [[Target Note]] for details"))

        // The imported note must register as a backlink of its link target.
        val backlinks = LocalNoteLinkRepository(db)
            .observeBacklinks("Target Note", excludeNoteId = "other")
            .first()
            .map { it.id }
        assertEquals(listOf("source"), backlinks)
    }

    @Test
    fun `update re-indexes tags when the imported body changes`() = runTest {
        importer.create(note("n1", "Body #old"))
        importer.update(note("n1", "Body #new", ts = 2L))

        val tags = LocalTagRepository(db).observeTagsForNote("n1").first().map { it.name }
        assertEquals(listOf("new"), tags)
    }
}
