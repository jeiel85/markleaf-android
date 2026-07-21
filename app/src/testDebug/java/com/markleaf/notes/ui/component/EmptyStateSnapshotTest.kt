package com.markleaf.notes.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
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
 * Goldens for the shared [EmptyState] layout — it fronts the trash, tags,
 * search, and archive empty screens, so a regression here shows on four
 * surfaces at once. Shipped without goldens in v2.24.0 (#154).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EmptyStateSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun titleOnlyPhone() = snapshot("empty_state_title_only_phone", hint = null)

    @Test
    fun titleAndHintPhone() = snapshot("empty_state_title_hint_phone", hint = "Notes you delete appear here")

    @Test
    fun titleAndHintDarkPhone() = snapshot(
        "empty_state_title_hint_dark_phone",
        hint = "Notes you delete appear here",
        darkTheme = true
    )

    private fun snapshot(name: String, hint: String?, darkTheme: Boolean = false) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("emptyStateSurface"),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = "No results found",
                        hint = hint
                    )
                }
            }
        }
        composeRule.onNodeWithTag("emptyStateSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }
}
