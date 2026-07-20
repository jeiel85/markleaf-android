package com.markleaf.notes.navigation

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.repository.LocalNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenNoteRouteTest {
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

    private suspend fun insertNote(id: String, locked: Boolean) {
        db.noteDao().insertNote(
            NoteEntity(
                id = id,
                title = "Note $id",
                contentMarkdown = "Body",
                excerpt = "Body",
                createdAt = 1L,
                updatedAt = 1L,
                locked = locked
            )
        )
    }

    @Test
    fun `a locked note id lands on the passcode gate, not the editor`() = runTest {
        insertNote("locked-1", locked = true)

        // The row is readable — getNote intentionally does not filter locked notes,
        // which is exactly why the editor route would have rendered it.
        assertNotNull(repository.getNote("locked-1"))

        assertEquals(NavRoutes.LOCKED, resolveOpenNoteRoute("locked-1", repository))
    }

    @Test
    fun `an unlocked note id still opens the editor`() = runTest {
        insertNote("open-1", locked = false)

        assertEquals(
            NavRoutes.editorRoute("open-1"),
            resolveOpenNoteRoute("open-1", repository)
        )
    }

    @Test
    fun `a note that becomes locked stops resolving to the editor`() = runTest {
        insertNote("note-1", locked = false)
        assertEquals(
            NavRoutes.editorRoute("note-1"),
            resolveOpenNoteRoute("note-1", repository)
        )

        // A widget's RemoteViews can still carry this id after the user locks the
        // note; the gate has to react to current state, not to what was true when
        // the intent was built.
        repository.setLocked("note-1", true)

        assertEquals(NavRoutes.LOCKED, resolveOpenNoteRoute("note-1", repository))
    }

    @Test
    fun `an unknown note id keeps the previous editor behaviour`() = runTest {
        assertEquals(
            NavRoutes.editorRoute("does-not-exist"),
            resolveOpenNoteRoute("does-not-exist", repository)
        )
    }
}
