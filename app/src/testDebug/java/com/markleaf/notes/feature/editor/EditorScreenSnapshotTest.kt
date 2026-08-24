package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.ui.theme.MarkleafTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorScreenSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quietAppBarPhone() = snapshot("editor_screen_quiet_appbar_phone")

    /**
     * "Show formatting button" off (#331) removes the standing `Aa` row, and the
     * defect it exists to remove is a 48dp strip of empty screen above the
     * keyboard — so what the setting has to produce is an *absence with nothing
     * left behind*. Neither of the checks it shipped with can see that: the
     * instrumented assertion only says the node does not exist, and
     * [EditorFormattingControlsSnapshotTest] photographs the row itself, which
     * is not mounted at all in this state. It has to be the screen, and the
     * screen's bottom edge is the part that matters — compare it against
     * `editor_screen_quiet_appbar_phone`, where the same edge carries the row.
     */
    @Test
    fun formattingButtonOffPhone() =
        snapshot("editor_screen_formatting_off_phone", showFormattingButton = false)

    private fun snapshot(name: String, showFormattingButton: Boolean = true) {
        // Only passed when it is not the default, so the golden above keeps
        // rendering the screen exactly as the app builds it.
        val settings = if (showFormattingButton) {
            null
        } else {
            AppSettingsRepository(InMemoryPreferencesDataStore()).also { repository ->
                runBlocking { repository.setShowFormattingButton(false) }
            }
        }
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("editorScreenSurface"),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (settings == null) {
                        EditorScreen(onBack = {})
                    } else {
                        EditorScreen(onBack = {}, settingsRepository = settings)
                    }
                }
            }
        }
        // The image cannot say why it looks the way it does, and this one is
        // defined by something being absent: a golden that captured the screen
        // before the setting reached it would look plausible and pin the wrong
        // state. Assert the premise, then photograph it.
        val formatting = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.formatting)
        with(composeRule.onNodeWithContentDescription(formatting)) {
            if (showFormattingButton) assertExists() else assertDoesNotExist()
        }
        composeRule.onNodeWithTag("editorScreenSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }
}
