package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden for the tablet Settings surface — specifically the 640dp column
 * centering that shipped without a visual gate in v2.24.0 (#154) and whose
 * re-indent was verified only by `git diff -w` afterwards.
 *
 * Only the 800×600 viewport is captured: the App section at the bottom shows
 * `BuildConfig.VERSION_NAME`, and letting that into the golden would break the
 * image on every release bump.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w800dp-h600dp-mdpi")
class SettingsScreenSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun centeredColumnTablet() {
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
        composeRule.onNodeWithTag("settingsScreenSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/settings_centered_tablet.png"
        )
    }
}
