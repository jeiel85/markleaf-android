package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Find in note from preview (#417): the overflow menu offers it there, it
 * counts matches in the rendered text, offers no replace row, and stepping to a
 * match inside a collapsed `<details>` section opens that section.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorPreviewFindTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun findInPreviewCountsRenderedMatchesAndOpensCollapsedSections() {
        val content = "# Pears\n\n**pear** tart\n\n<details>\n<summary>More</summary>\n\nhidden pear\n</details>"
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "Pears",
                    contentMarkdown = content,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).performClick()
        composeRule.onNodeWithText("hidden pear").assertDoesNotExist()

        composeRule.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.find_in_note)).performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("pear")

        // "Pears" heading, the bold "pear", and the one in the collapsed section.
        // "**pear**" in the source would not match "pear tart" as rendered text.
        composeRule.onNodeWithText("1/3").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.replace_all_matches)).assertDoesNotExist()

        val next = context.getString(R.string.find_next_match)
        composeRule.onNodeWithContentDescription(next).performClick()
        composeRule.onNodeWithContentDescription(next).performClick()
        composeRule.onNodeWithText("3/3").assertExists()
        composeRule.onNodeWithText("hidden pear").assertExists()
    }
}
