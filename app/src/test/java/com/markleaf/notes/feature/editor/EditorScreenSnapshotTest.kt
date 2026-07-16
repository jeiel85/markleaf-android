package com.markleaf.notes.feature.editor

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

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorScreenSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun quietAppBarPhone() = snapshot("editor_screen_quiet_appbar_phone")

    private fun snapshot(name: String) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("editorScreenSurface"),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorScreen(onBack = {})
                }
            }
        }
        composeRule.onNodeWithTag("editorScreenSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }
}
