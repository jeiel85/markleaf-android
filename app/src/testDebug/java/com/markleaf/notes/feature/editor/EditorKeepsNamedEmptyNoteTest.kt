package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
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
 * Under the File name title rule (D088), an empty note named on an earlier
 * visit is still a note when it is opened again and left. Only the visit that
 * named it knew it had been named, so a later visit used to send it down the
 * blank-note discard of #405 (Codex on #613).
 *
 * One scenario per file, for the same `AppDatabase` singleton reason given on
 * [EditorDiscardsBlankNoteTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorKeepsNamedEmptyNoteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun anEmptyNoteNamedOnAnEarlierVisitSurvivesTheNextOne() {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            // What the visit that named it left behind: the name, no text, and
            // the rename's edit time after the creation time. Not "groceries":
            // the database is shared across test classes, and a note left
            // under that name makes EditorNameFieldRenamesNoteTest's rename a
            // duplicate.
            repo.createNote(
                Note(
                    id = noteId,
                    title = "packing list",
                    contentMarkdown = "",
                    excerpt = "",
                    createdAt = Instant.ofEpochMilli(1_000),
                    updatedAt = Instant.ofEpochMilli(2_000)
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking {
            settings.beginRetitle(NoteTitleSource.FILE_NAME)
            settings.setRetitlePending(false)
        }
        var showEditor by mutableStateOf(true)

        composeRule.setContent {
            val hostScope = rememberCoroutineScope()
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (showEditor) {
                    EditorScreen(
                        noteId = noteId,
                        onBack = {},
                        settingsRepository = settings,
                        hostScope = hostScope
                    )
                }
            }
        }
        composeRule.waitForIdle()

        showEditor = false
        composeRule.waitForIdle()
        // The discard reads the row through Room's executor; give it the same
        // chance to run that the discard tests wait for.
        Thread.sleep(500)
        composeRule.waitForIdle()

        assertEquals(
            "a note named earlier is a note, even with nothing in it",
            "packing list",
            runBlocking { repo.getNote(noteId) }?.title
        )
    }
}
