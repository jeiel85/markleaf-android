package com.markleaf.notes.core.markdown.preview

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.core.markdown.MarkdownSyntaxColors
import com.markleaf.notes.core.markdown.MarkdownSyntaxVisualTransformation
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Snapshot regression for the *live editor* — the inline rich rendering that
 * Bear made famous (headings actually larger, bold actually bolder, markers
 * receding to muted small chars). Any drift in that experience is a noticeable
 * UX change and should be a deliberate decision, not an accident.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorLiveSnapshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeRule,
        captureRoot = composeRule.onRoot(),
        options = RoborazziRule.Options(
            outputDirectoryPath = "src/test/snapshots/roborazzi",
            roborazziOptions = RoborazziOptions(
                // Tightened in v2.9.1 alongside Linux-recorded goldens.
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f)
            )
        )
    )

    @Test
    fun editor_live_headings_light() = snapshot("editor_live_headings_light", darkTheme = false) {
        LiveEditorRender(
            """
            # Heading 1
            ## Heading 2
            ### Heading 3
            body line
            """.trimIndent()
        )
    }

    @Test
    fun editor_live_headings_dark() = snapshot("editor_live_headings_dark", darkTheme = true) {
        LiveEditorRender(
            """
            # Heading 1
            ## Heading 2
            ### Heading 3
            body line
            """.trimIndent()
        )
    }

    @Test
    fun editor_live_inline_emphasis_light() =
        snapshot("editor_live_inline_emphasis_light", darkTheme = false) {
            LiveEditorRender("Plain with **bold**, *italic*, ~~strike~~, `code`.")
        }

    @Test
    fun editor_live_mixed_light() = snapshot("editor_live_mixed_light", darkTheme = false) {
        LiveEditorRender(
            """
            # Title

            A paragraph with **bold word** and an *emphasis*.

            > a blockquote line
            - [ ] todo
            - [x] done
            """.trimIndent()
        )
    }

    private fun snapshot(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp)
                ) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "src/test/snapshots/roborazzi/$name.png")
    }

    @Composable
    private fun LiveEditorRender(initial: String) {
        var value by remember { mutableStateOf(TextFieldValue(initial)) }
        val scheme = MaterialTheme.colorScheme
        val transformation = remember(scheme) {
            MarkdownSyntaxVisualTransformation(
                MarkdownSyntaxColors(
                    heading = scheme.primary,
                    emphasis = scheme.tertiary,
                    link = scheme.primary,
                    syntax = scheme.onSurfaceVariant,
                    checkbox = scheme.secondary,
                    code = scheme.tertiary
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = scheme.onBackground
            ),
            visualTransformation = transformation
        )
    }
}
