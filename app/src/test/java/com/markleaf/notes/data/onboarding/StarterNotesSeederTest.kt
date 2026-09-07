package com.markleaf.notes.data.onboarding

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteLinkRepository
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

        assertEquals(6, notes.size)
        assertTrue(notes.first().pinned)
        assertTrue(notes.any { it.contentMarkdown.contains("#markdown") })
        assertTrue(notes.any { it.contentMarkdown.contains("![Markleaf sample canvas]") })
        assertTrue(notes.any { it.contentMarkdown.contains("[[Project Brief]]") })
        assertTrue(notes.any { it.contentMarkdown.contains("> [!NOTE]") })
        assertTrue(notes.any { it.contentMarkdown.contains("| Element | Use it for |") })
        assertTrue(notes.any { it.contentMarkdown.contains("[^1]:") })
        assertTrue(notes.none { it.contentMarkdown.contains("ZIP backup") })
    }

    @Test
    fun `seedIfNeeded inserts starter notes once even when startup paths race`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("markleaf_onboarding", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        coroutineScope {
            repeat(2) {
                launch {
                    StarterNotesSeeder.seedIfNeeded(context, db)
                }
            }
        }
        StarterNotesSeeder.seedIfNeeded(context, db)

        assertEquals(6, db.noteDao().countAllNotes())

        val tagNames = db.tagDao().getAllTagsList().map { it.name }
        assertTrue(tagNames.isNotEmpty())
        assertTrue(tagNames.contains("guide") || tagNames.contains("가이드"))

        val backlinks = LocalNoteLinkRepository(db)
            .observeBacklinks("Project Brief", "starter-note-4")
            .first()
        assertTrue(backlinks.any { it.id == "starter-note-2" })
        assertTrue(backlinks.any { it.id == "starter-note-3" })

        val sampleImage = File(context.filesDir, StarterNotesSeeder.STARTER_SAMPLE_IMAGE_PATH)
        assertTrue(sampleImage.exists())
        assertTrue(sampleImage.length() > 0L)
    }
}
