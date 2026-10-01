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
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
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
 * The host's one-shot launch dispatch, and the switch that stops a *recreated*
 * activity from running it again (review of #483).
 *
 * A launch request is either an entry intent (a widget tap, a share, a file) or,
 * with none, the plain-launch fallback that reopens the last note. Android hands a
 * recreated activity the same intent again, so acting on it a second time made a
 * second note out of a share — and with no intent left to act on, the fallback ran
 * instead and pushed the last note on top of the restored back stack. A host told
 * `dispatchLaunchRequest = false` does neither, and does not report a dispatch:
 * `onEntryDispatched` is what MainActivity records as "acted on", so a host that
 * dispatched nothing must not say it did.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class LaunchDispatchTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AppDatabase
    private var dispatched = 0
    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun setHost(
        dispatchLaunchRequest: Boolean,
        shouldCreateNote: Boolean = false,
        openSearch: Boolean = false
    ) {
        val factory = MarkleafViewModelFactory(LocalNoteRepository(database))
        composeRule.setContent {
            navController = rememberNavController()
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkleafNavHost(
                    navController = navController,
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(360.dp, 800.dp)),
                    viewModelFactory = factory,
                    shouldCreateNote = shouldCreateNote,
                    openSearch = openSearch,
                    dispatchLaunchRequest = dispatchLaunchRequest,
                    onEntryDispatched = { dispatched++ }
                )
            }
        }
    }

    private fun blankNotes(): Int = runBlocking {
        database.noteDao().getAllNotes().count { it.contentMarkdown.isEmpty() }
    }

    /** Lets a dispatch that is going to happen happen, so "nothing happened" means something. */
    private fun settle() {
        composeRule.waitForIdle()
        Thread.sleep(1_000)
        composeRule.waitForIdle()
    }

    @Test
    fun aFreshLaunchWithNoRequestStillRunsTheFallbackAndReportsIt() {
        setHost(dispatchLaunchRequest = true)

        composeRule.waitUntil(timeoutMillis = 10_000) { dispatched == 1 }
        settle()

        assertEquals("reported exactly once", 1, dispatched)
    }

    @Test
    fun aFreshLaunchWithANewNoteRequestCreatesOneNoteAndReportsIt() {
        setHost(dispatchLaunchRequest = true, shouldCreateNote = true)

        composeRule.waitUntil(timeoutMillis = 10_000) { dispatched == 1 }
        settle()

        assertEquals(1, blankNotes())
        assertEquals(1, dispatched)
    }

    @Test
    fun aRecreationOfAnActedOnLaunchDispatchesNothingAndReportsNothing() {
        // The intent is still the new-note request; the flag says it was acted on.
        setHost(dispatchLaunchRequest = false, shouldCreateNote = true)

        settle()

        assertEquals("no second note", 0, blankNotes())
        assertEquals("nothing dispatched, so nothing reported", 0, dispatched)
    }

    @Test
    fun aRecreationOfAPlainLaunchDoesNotRunTheReopenFallbackEither() {
        // No request at all — the fallback's own branch — and still nothing runs.
        setHost(dispatchLaunchRequest = false)

        settle()

        assertEquals(0, dispatched)
    }

    @Test
    fun theLauncherSearchShortcutStartsOnTheSearchScreenAndReportsIt() {
        setHost(dispatchLaunchRequest = true, openSearch = true)

        composeRule.waitUntil(timeoutMillis = 10_000) { dispatched == 1 }
        settle()

        assertEquals(NavRoutes.SEARCH, navController.currentDestination?.route?.substringBefore('?'))
        assertEquals("no note made on the way", 0, blankNotes())
    }

    @Test
    fun aRecreationOfASearchLaunchDoesNotPushSearchAgain() {
        setHost(dispatchLaunchRequest = false, openSearch = true)

        settle()

        assertEquals(NavRoutes.NOTES, navController.currentDestination?.route)
        assertEquals(0, dispatched)
    }
}
