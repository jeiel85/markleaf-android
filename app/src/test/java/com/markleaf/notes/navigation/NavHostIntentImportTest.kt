package com.markleaf.notes.navigation

import android.content.Context
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Regression for #142: opening/sharing a file into Markleaf must import the
 * content exactly once. The import action used to live in a LaunchedEffect
 * inside the NOTES navigation destination, so pressing back from the editor
 * re-entered that destination, re-ran the effect, and saved a fresh duplicate
 * note every time — while immediately reopening the editor, which trapped the
 * user in a reopen loop.
 *
 * The fix hoists the one-shot import to MarkleafNavHost's host scope, which
 * stays composed for the activity instance and therefore does not re-fire when
 * an inner destination re-enters composition. This test drives the real
 * MarkleafNavHost through a cold start + a back navigation and asserts exactly
 * one create happened. It fails against the pre-fix code (the second create
 * fires on back), which is the point.
 *
 * Harness notes (each learned the hard way):
 *  - An in-memory [NoteRepository] backs the ViewModels so create + navigate
 *    stay on the test's main thread; Room's suspend writes resume on their own
 *    executor under the Compose test harness, a test-only threading artifact.
 *  - The editor (reached after import) reads the process-wide
 *    [AppDatabase.getInstance] singleton directly. Other tests in the suite
 *    leave that singleton populated, so its notes Flow keeps re-emitting and
 *    Compose never idles (AppNotIdleException — an "infinite composition loop"
 *    that only shows up when the whole suite runs, not in isolation). Swapping
 *    in a fresh in-memory database isolates this test from that pollution.
 *  - The clock is paused and advanced explicitly so the assertions depend only
 *    on the deterministic in-memory create count, not on background settling.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NavHostIntentImportTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun isolateDatabaseSingleton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clean = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        setDatabaseSingleton(clean)
    }

    @After
    fun resetDatabaseSingleton() {
        setDatabaseSingleton(null)
    }

    @Test
    fun sharedContent_importsExactlyOnce_andDoesNotReimportOnBack() {
        val repository = RecordingNoteRepository()
        val factory = MarkleafViewModelFactory(repository)

        composeRule.mainClock.autoAdvance = false

        lateinit var navController: NavHostController
        composeRule.setContent {
            navController = rememberNavController()
            MarkleafTheme {
                MarkleafNavHost(
                    navController = navController,
                    // Compact width → single-pane phone layout, the path the
                    // reporter exercised.
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 640.dp)),
                    viewModelFactory = factory,
                    sharedText = "# Imported note\n\nbody text"
                )
            }
        }

        // Run the host one-shot effect + initial navigation to the editor.
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals("cold start should import exactly once", 1, repository.created)

        // Back to the notes list. Pre-fix, re-entering the notes destination
        // re-ran the import → a second duplicate note.
        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals("returning from the editor must not re-import", 1, repository.created)
    }

    private fun setDatabaseSingleton(db: AppDatabase?) {
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        (field.get(null) as? AppDatabase)?.close()
        field.set(null, db)
    }

    /** In-memory repository that records how many notes were created. */
    private class RecordingNoteRepository : NoteRepository {
        var created = 0
            private set
        private val notes = MutableStateFlow<List<Note>>(emptyList())

        override fun observeNotes(): Flow<List<Note>> = notes
        override suspend fun getNote(noteId: String): Note? =
            notes.value.firstOrNull { it.id == noteId }

        override suspend fun createNote(note: Note) {
            created++
            notes.value = notes.value + note
        }

        override suspend fun updateNote(note: Note) = Unit
        override suspend fun moveToTrash(noteId: String) = Unit
        override suspend fun setPinned(noteId: String, pinned: Boolean) = Unit
        override suspend fun setArchived(noteId: String, archived: Boolean) = Unit
        override suspend fun restoreFromTrash(noteId: String) = Unit
        override suspend fun deleteForever(noteId: String) = Unit
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeArchivedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
