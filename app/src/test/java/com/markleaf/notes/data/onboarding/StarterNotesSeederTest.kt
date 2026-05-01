package com.markleaf.notes.data.onboarding

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
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
class StarterNotesSeederTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `starterNotes creates useful local onboarding notes`() {
        val notes = StarterNotesSeeder.starterNotes(Instant.ofEpochMilli(1L))

        assertEquals(4, notes.size)
        assertTrue(notes.first().pinned)
        assertTrue(notes.any { it.contentMarkdown.contains("#markdown") })
        assertTrue(notes.any { it.contentMarkdown.contains("[[Local-first backup and export]]") })
    }

    @Test
    fun `seedIfNeeded inserts starter notes once and indexes tags`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("markleaf_onboarding", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        StarterNotesSeeder.seedIfNeeded(context, db)
        StarterNotesSeeder.seedIfNeeded(context, db)

        assertEquals(4, db.noteDao().countAllNotes())

        val tagNames = db.tagDao().getAllTagsList().map { it.name }
        assertTrue(tagNames.isNotEmpty())
        assertTrue(tagNames.contains("guide") || tagNames.contains("가이드"))
    }
}
