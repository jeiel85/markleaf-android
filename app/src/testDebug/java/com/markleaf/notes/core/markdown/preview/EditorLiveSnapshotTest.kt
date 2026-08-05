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
import com.markleaf.notes.core.markdown.MarkdownSyntaxVisualTransformation
import com.markleaf.notes.core.markdown.markdownSyntaxColors
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
        LiveEditorRender(MIXED_SAMPLE)
    }

    /**
     * The dark counterpart of the mixed sample. Added with the palette fix that
     * removed the [com.markleaf.notes.core.markdown.MarkdownSyntaxColors]
     * `Color.Gray` defaults: blockquotes and rules follow the theme now, and
     * "follows the theme" is only pinned if both themes are captured.
     */
    @Test
    fun editor_live_mixed_dark() = snapshot("editor_live_mixed_dark", darkTheme = true) {
        LiveEditorRender(MIXED_SAMPLE)
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
        // lint's RememberReturnType check (lintRelease) cannot resolve the
        // constructor return type across the test source set boundary and
        // falsely flags this remember as Unit-returning. The constructor
        // clearly yields a MarkdownSyntaxVisualTransformation instance.
        @Suppress("RememberReturnType")
        val transformation = remember(scheme) {
            MarkdownSyntaxVisualTransformation(markdownSyntaxColors(scheme))
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

    private companion object {
        /** Carries one of every role whose colour is theme-derived — heading,
         *  emphasis, checkbox, blockquote and a `---` rule — so a palette change
         *  cannot move any of them unnoticed. */
        val MIXED_SAMPLE = """
            # Title

            A paragraph with **bold word** and an *emphasis*.

            > a blockquote line

            ---

            - [ ] todo
            - [x] done
        """.trimIndent()
    }
}
