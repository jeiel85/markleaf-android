package tmpprobe

import android.content.Context
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.navigation.MarkleafNavHost
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
import java.time.Instant
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** TEMPORARY CI probe for the TabletSelectionTest flake — not to be merged. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w1280dp-h800dp-mdpi")
class PaneWaitProbe(private val iteration: Int) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data(): List<Array<Any>> = (1..60).map { arrayOf<Any>(it) }
        const val NOTE_ID = "tablet-note"
        const val NOTE_TITLE = "Pane note"
        const val EMPTY_PANE = "Select a note to view"
    }

    @get:Rule
    val composeRule = createComposeRule()
    private lateinit var database: AppDatabase
    private val watcher = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val now = Instant.parse("2026-10-03T00:00:00Z")
        runBlocking {
            LocalNoteRepository(database).createNote(
                Note(
                    id = NOTE_ID,
                    title = NOTE_TITLE,
                    contentMarkdown = "# Pane note" + System.lineSeparator() + "body",
                    excerpt = "body",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    @After
    fun tearDown() {
        watcher.cancel()
        database.close()
    }

    @Test
    fun lockRightAfterSelecting() {
        val factory = MarkleafViewModelFactory(LocalNoteRepository(database))
        composeRule.setContent {
            val navController = rememberNavController()
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkleafNavHost(
                    navController = navController,
                    windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1280.dp, 800.dp)),
                    viewModelFactory = factory,
                    shouldCreateNote = false,
                    openSearch = false,
                    dispatchLaunchRequest = false
                )
            }
        }
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText(NOTE_TITLE).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText(NOTE_TITLE)[0].performClick()
        composeRule.waitForIdle()

        val events = Collections.synchronizedList(mutableListOf<String>())
        val t0 = System.nanoTime()
        fun ms() = (System.nanoTime() - t0) / 1_000_000
        watcher.launch {
            LocalNoteRepository(database).observeNote(NOTE_ID).collect { events += "watch@${ms()} locked=${it?.locked}" }
        }
        runBlocking {
            val repo = LocalNoteRepository(database)
            repo.updateNote(requireNotNull(repo.getNote(NOTE_ID)).copy(locked = true))
        }
        events += "written@${ms()}"
        var ok = true
        try {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText(EMPTY_PANE).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            ok = false
        }
        val paneTitles = composeRule.onAllNodesWithText(NOTE_TITLE).fetchSemanticsNodes().size
        val line = "PROBE iter=$iteration ok=$ok ms=${ms()} titleNodes=$paneTitles events=$events"
        println(line)
        org.junit.Assert.assertTrue(line, ok)
    }
}
