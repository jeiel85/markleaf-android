package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * Goldens for the tablet Settings surface: the list of sections and each page
 * opened from it, all in the 640dp centred column (#154).
 *
 * **One viewport is never the whole page.** A capture shows only what fits at
 * 800×600, so each page is scrolled to its *last* control first. Settings
 * added below an earlier anchor once went in visually ungated while the
 * golden kept passing (#255).
 *
 * The About page is deliberately left out: it renders `BuildConfig.VERSION_NAME`,
 * so an image of it would break on every release bump. Sync has goldens of its
 * own in [SyncSectionSnapshotTest], because its appearance depends on state
 * this screen cannot be put into from the outside.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w800dp-h600dp-mdpi")
class SettingsScreenSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sectionListTablet() = capture("settings_home_tablet")

    @Test
    fun appearancePage() = capture("settings_appearance_tablet", page = "Appearance", scrollTo = "Custom")

    @Test
    fun editorPage() = capture("settings_editor_tablet", page = "Editor", scrollTo = "Add snippet")

    /**
     * Anchored on the page's current last control. It was "First line", from
     * the title rule added in #280, until the new-note heading switch (#544)
     * landed below it; the groups (step 3) moved that switch to the end.
     */
    @Test
    fun notesPage() = capture(
        "settings_notes_tablet",
        page = "Notes",
        scrollTo = "Start new notes with a heading"
    )

    @Test
    fun privacyAndSecurityPage() =
        capture("settings_privacy_tablet", page = "Privacy & security", scrollTo = "View Privacy Dashboard")

    /** With no folder chosen; the sync block's other states are in [SyncSectionSnapshotTest]. */
    @Test
    fun syncAndExportPage() =
        capture("settings_sync_export_tablet", page = "Sync & export", scrollTo = "Export notes by tag…")

    /**
     * `performScrollTo` moves a node just far enough to be visible, so the
     * anchor must be the page's last control: anchoring higher leaves the rest
     * below the fold.
     */
    private fun capture(name: String, page: String? = null, scrollTo: String? = null) {
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
        page?.let {
            composeRule.onNode(hasText(it) and hasClickAction() and !isHeading()).performClick()
            composeRule.waitForIdle()
        }
        scrollTo?.let { composeRule.onNodeWithText(it).performScrollTo() }
        composeRule.onNodeWithTag("settingsScreenSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }
}
