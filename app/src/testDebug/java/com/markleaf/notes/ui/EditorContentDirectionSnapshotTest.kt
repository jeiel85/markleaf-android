package com.markleaf.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.core.markdown.MarkdownSyntaxVisualTransformation
import com.markleaf.notes.core.markdown.markdownSyntaxColors
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The editing surface's half of "note text lays out by its own direction"
 * (#262, from #204/#146).
 *
 * `ContentDirectionSnapshotTest` covers rendered note text; this covers the
 * `BasicTextField` the note is typed into, which is a separate risk because it
 * is a separate style path. The editor takes
 * `MaterialTheme.typography.bodyLarge.copy(color = …)`, so it inherits
 * `TextDirection.Content` from the theme — and this pins that it keeps doing so
 * while an RTL line is being edited.
 *
 * **The style here is the production one on purpose.** The existing
 * `EditorLiveSnapshotTest` builds a bare `TextStyle(fontSize = 16.sp, …)`
 * instead of the theme's role, which is fine for the colours it exists to pin
 * but means a typography change cannot move it. A golden meant to guard a
 * typography property has to be drawn with the typography.
 *
 * The sample mixes scripts for the same reason the rendered-text goldens do: a
 * page that is entirely right-to-left would look correct under a blanket
 * direction too, which is not what the app does.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorContentDirectionSnapshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeRule,
        captureRoot = composeRule.onRoot(),
        options = RoborazziRule.Options(
            outputDirectoryPath = "src/test/snapshots/roborazzi",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f)
            )
        )
    )

    @Test
    fun editor_content_direction_mixed_light() =
        snapshot("editor_content_direction_mixed_light", darkTheme = false)

    /** The dark counterpart, since the editor is drawn in both. */
    @Test
    fun editor_content_direction_mixed_dark() =
        snapshot("editor_content_direction_mixed_dark", darkTheme = true)

    private fun snapshot(name: String, darkTheme: Boolean) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp)
                ) {
                    EditorSurface(MIXED_DIRECTION_SAMPLE)
                }
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "src/test/snapshots/roborazzi/$name.png")
    }

    /**
     * The editor as `EditorScreen` composes it: the theme's `bodyLarge` role and
     * the markdown syntax transformation, so this golden moves when either does.
     */
    @Composable
    private fun EditorSurface(initial: String) {
        var value by remember { mutableStateOf(TextFieldValue(initial)) }
        val scheme = MaterialTheme.colorScheme
        @Suppress("RememberReturnType")
        val transformation = remember(scheme) {
            MarkdownSyntaxVisualTransformation(markdownSyntaxColors(scheme))
        }
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            visualTransformation = transformation
        )
    }

    private companion object {
        /**
         * Headings, emphasis and a task marker on both sides of the direction
         * split, so a change that only affects marked-up lines still shows.
         */
        val MIXED_DIRECTION_SAMPLE = """
            # Meeting notes

            A plain English paragraph with **bold** in it.

            ## ملاحظات الاجتماع

            هذه فقرة عربية مع **نص عريض** بداخلها.

            - [ ] an English task
            - [ ] مهمة عربية

            > זו פסקה בעברית
        """.trimIndent()
    }
}
