package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.navigation.NavRoutes
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A note opened from a search result opens with Find already holding the
 * search term (#553), in whichever mode "Open notes in preview" opens it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorOpensFindFromSearchTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun editingOpensWithFindOnTheFirstMatch() {
        val noteId = createNote("# Pears\n\npear tart\n\nanother pear")
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

        setEditor(noteId, settings, initialFindQuery = "pear")

        composeRule.onNodeWithText("1/3").assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.find_next_match)).performClick()
        composeRule.onNodeWithText("2/3").assertExists()
    }

    @Test
    fun previewOpensWithFindOverTheRenderedText() {
        // "**pear**" renders as "pear": a preview find is what counts it.
        val noteId = createNote("# Plums\n\n**pear** tart")
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking { settings.setOpenNotesInPreview(true) }

        setEditor(noteId, settings, initialFindQuery = "pear")

        // The mode switch the load makes must not close the Find it opened.
        composeRule.onNodeWithText("1/1").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.replace_all_matches)).assertDoesNotExist()
    }

    @Test
    fun aQueryTheNoteDoesNotContainOpensNoFind() {
        val noteId = createNote("# Plums\n\nonly plums here")
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

        setEditor(noteId, settings, initialFindQuery = "pear")

        composeRule.onNodeWithContentDescription(context.getString(R.string.find_next_match)).assertDoesNotExist()
    }

    @Test
    fun theFindArgumentSurvivesTheRoute() {
        // Spaces, `&` and `#` all have to come back out of the route as typed.
        var received: String? = null
        composeRule.setContent {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "start") {
                composable("start") {
                    Text("start")
                    LaunchedEffect(Unit) {
                        nav.navigate(NavRoutes.editorRoute("a", find = "#pear & plum tart"))
                    }
                }
                composable(
                    route = NavRoutes.EDITOR,
                    arguments = listOf(navArgument("find") {
                        type = NavType.StringType
                        defaultValue = ""
                    })
                ) { entry ->
                    received = entry.arguments?.getString("find")
                    Text("editor")
                }
            }
        }
        composeRule.waitForIdle()

        assertEquals("#pear & plum tart", received)
    }

    private fun setEditor(noteId: String, settings: AppSettingsRepository, initialFindQuery: String) {
        composeRule.setContent {
            Screen {
                EditorScreen(
                    noteId = noteId,
                    onBack = {},
                    settingsRepository = settings,
                    initialFindQuery = initialFindQuery
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun Screen(content: @Composable () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }

    private fun createNote(content: String): String {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = content.lineSequence().first().removePrefix("# "),
                    contentMarkdown = content,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        return noteId
    }
}
