package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Goldens for the tablet Settings surface — originally the 640dp column
 * centering that shipped without a visual gate in v2.24.0 (#154) and whose
 * re-indent was verified only by `git diff -w` afterwards.
 *
 * **One viewport is never the whole screen.** The first golden here captured
 * only what fits at 800×600, which is the Appearance section and part of
 * Markdown. Everything added below that since — the notes-and-search rows, the
 * privacy toggles, the data actions — went in visually ungated, and nobody
 * noticed because the golden kept passing (#255). The scrolled captures below
 * close that.
 *
 * The App section is still deliberately excluded: it renders
 * `BuildConfig.VERSION_NAME`, so putting it in an image would break that image
 * on every release bump. The sync section has goldens of its own in
 * [SyncSectionSnapshotTest], because its appearance depends on state this
 * screen cannot be put into from the outside.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w800dp-h600dp-mdpi")
class SettingsScreenSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun centeredColumnTablet() = capture("settings_centered_tablet")

    @Test
    fun notesAndSearchSection() =
        capture("settings_notes_search_tablet", scrollTo = "Where I left off")

    @Test
    fun privacySection() = capture("settings_privacy_tablet", scrollTo = "View Privacy Dashboard")

    @Test
    fun dataSection() = capture("settings_data_tablet", scrollTo = "Export all notes…")

    /**
     * Each capture is anchored on its section's *last* control, not its
     * heading. `performScrollTo` moves a node just far enough to be visible,
     * so anchoring on a heading leaves the section body below the fold — which
     * is how the first attempt at this produced a golden byte-identical to the
     * unscrolled one.
     */
    private fun capture(name: String, scrollTo: String? = null) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("settingsScreenSurface"),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(onBack = {})
                }
            }
        }
        // The sections live in a plain verticalScroll Column, so every one of
        // them is composed and reachable — no lazy layout to coax into
        // building the row first.
        scrollTo?.let { composeRule.onNodeWithText(it).performScrollTo() }
        composeRule.onNodeWithTag("settingsScreenSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }
}
