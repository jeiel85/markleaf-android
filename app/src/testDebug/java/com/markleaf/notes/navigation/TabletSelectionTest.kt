package com.markleaf.notes.navigation

import android.content.Context
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The note open in the tablet's editor pane stays open (#262, v2.63.2).
 *
 * The pane's selection was a plain `remember` inside the two-pane branch of the
 * NOTES destination, so three ordinary things closed the note and left "Select a
 * note to view": a rotation (the activity is rebuilt), a trip to Settings (the
 * destination leaves composition), and a tablet turned upright — about 800 dp
 * wide, narrower than the two-pane layout's 840, so the branch itself switched.
 * The last is the common one on a real tablet, and saving the state alone does
 * not fix it: the narrow layout has no pane to show the note in, so the note has
 * to move to the full-screen editor.
 *
 * Drives the real [MarkleafNavHost]. The window size class is a state the test
 * changes, the way a rotation hands the host a different one.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w1280dp-h800dp-mdpi")
class TabletSelectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val restoration = StateRestorationTester(composeRule)
    private lateinit var database: AppDatabase
    private lateinit var navController: NavHostController
    private var windowSize by mutableStateOf(LANDSCAPE)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val now = Instant.parse("2026-10-03T00:00:00Z")
        runBlocking {
            LocalNoteRepository(database).createNote(
                Note(
                    id = NOTE_ID,
                    title = NOTE_TITLE,
                    contentMarkdown = "# $NOTE_TITLE\nbody",
                    excerpt = "body",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun showHost() {
        val factory = MarkleafViewModelFactory(LocalNoteRepository(database))
        restoration.setContent {
            navController = rememberNavController()
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkleafNavHost(
                    navController = navController,
                    windowSizeClass = WindowSizeClass.calculateFromSize(windowSize),
                    viewModelFactory = factory,
                    shouldCreateNote = false,
                    openSearch = false,
                    // No launch request to act on, so the reopen-last-note
                    // fallback cannot open anything behind the test's back.
                    dispatchLaunchRequest = false
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(NOTE_TITLE).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun selectTheNote() {
        composeRule.onAllNodesWithText(NOTE_TITLE)[0].performClick()
        composeRule.waitForIdle()
        assertPaneShowsANote()
    }

    private fun assertPaneShowsANote() {
        composeRule.waitForIdle()
        assertEquals(
            "the editor pane fell back to its empty state",
            0,
            composeRule.onAllNodesWithText(EMPTY_PANE).fetchSemanticsNodes().size
        )
    }

    private fun currentRoute(): String? = composeRule.runOnIdle {
        navController.currentBackStackEntry?.destination?.route
    }

    @Test
    fun theEmptyPaneIsWhatItLooksLikeBeforeASelection() {
        // Guards the assertions below: they read "no empty-pane text" as "a note
        // is open", which only means something if that text shows with none.
        showHost()
        composeRule.onNodeWithText(EMPTY_PANE).fetchSemanticsNode()
    }

    @Test
    fun aRotationKeepsTheSelectedNoteOpen() {
        showHost()
        selectTheNote()

        restoration.emulateSavedInstanceStateRestore()

        assertPaneShowsANote()
    }

    @Test
    fun aTripToSettingsAndBackKeepsTheSelectedNoteOpen() {
        showHost()
        selectTheNote()

        composeRule.runOnUiThread { navController.navigate(NavRoutes.SETTINGS) }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { navController.popBackStack() }

        assertPaneShowsANote()
    }

    @Test
    fun aTabletTurnedUprightOpensTheSelectedNoteInTheFullScreenEditor() {
        showHost()
        selectTheNote()

        windowSize = PORTRAIT
        composeRule.waitUntil(timeoutMillis = 10_000) { currentRoute() == NavRoutes.EDITOR }

        val noteId = composeRule.runOnIdle {
            navController.currentBackStackEntry?.arguments?.getString("noteId")
        }
        assertEquals(NOTE_ID, noteId)
    }

    @Test
    fun backFromThatEditorLandsOnTheListAndStaysThere() {
        showHost()
        selectTheNote()
        windowSize = PORTRAIT
        composeRule.waitUntil(timeoutMillis = 10_000) { currentRoute() == NavRoutes.EDITOR }

        composeRule.runOnUiThread { navController.popBackStack() }
        composeRule.waitForIdle()
        // Give a wrongly kept selection the chance to reopen the editor.
        Thread.sleep(500)
        composeRule.waitForIdle()

        assertEquals(NavRoutes.NOTES, currentRoute())
    }

    @Test
    fun withNothingSelectedTurningUprightStaysOnTheList() {
        showHost()

        windowSize = PORTRAIT
        composeRule.waitForIdle()
        Thread.sleep(500)
        composeRule.waitForIdle()

        assertEquals(NavRoutes.NOTES, currentRoute())
    }

    private companion object {
        const val NOTE_ID = "tablet-note"
        const val NOTE_TITLE = "Pane note"
        const val EMPTY_PANE = "Select a note to view"
        val LANDSCAPE = DpSize(1280.dp, 800.dp)
        val PORTRAIT = DpSize(800.dp, 1280.dp)
    }
}
