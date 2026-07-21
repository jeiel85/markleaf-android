package com.markleaf.notes.feature.tags

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
 * Golden for the tablet Tags surface: the 640dp-centered column from the same
 * Phase 29 polish as Settings, shipped without a visual gate (#154). The
 * database starts empty under Robolectric, so this also pins the [EmptyState]
 * presentation inside a real screen at tablet width.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w800dp-h600dp-mdpi")
class TagsScreenSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyCenteredTablet() {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tagsScreenSurface"),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TagsScreen(onBack = {}, onTagClick = {})
                }
            }
        }
        composeRule.onNodeWithTag("tagsScreenSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/tags_empty_centered_tablet.png"
        )
    }
}
