package com.markleaf.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins that note text lays out by its own direction (#262, from #204/#146).
 *
 * `MarkleafTheme` gives every typography role `TextDirection.Content`, which is
 * what makes an Arabic or Hebrew paragraph lay out right-to-left inside an
 * otherwise left-to-right app. Nothing covered it: it was verified on a device
 * once, so a typography refactor could drop it and every RTL note would quietly
 * start rendering left-aligned with its punctuation in the wrong place.
 *
 * That is exactly what a golden catches. Drop `withContentTextDirection()` and
 * these images change — the paragraphs swap edges — while every LTR golden in
 * the suite stays byte-identical, so the failure names itself.
 *
 * The samples deliberately mix scripts. A pure-RTL page would also look right
 * under a blanket `TextDirection.Rtl`, which is *not* what the app does and
 * would break every English note; the mixed case is the one only "direction
 * follows content" gets right.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class ContentDirectionSnapshotTest {

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

    /** Arabic and Hebrew paragraphs, which must sit against the right edge. */
    @Test
    fun content_direction_rtl_light() = snapshot("content_direction_rtl_light") {
        NoteBody(
            listOf(
                ARABIC_HEADING to MaterialTheme.typography.headlineSmall,
                ARABIC_BODY to MaterialTheme.typography.bodyLarge,
                HEBREW_BODY to MaterialTheme.typography.bodyLarge
            )
        )
    }

    /**
     * The mixed page: LTR paragraphs keep the left edge in the same note where
     * RTL ones take the right. A blanket direction cannot produce this.
     */
    @Test
    fun content_direction_mixed_light() = snapshot("content_direction_mixed_light") {
        NoteBody(
            listOf(
                "Meeting notes" to MaterialTheme.typography.headlineSmall,
                "A plain English paragraph that stays on the left." to
                    MaterialTheme.typography.bodyLarge,
                ARABIC_BODY to MaterialTheme.typography.bodyLarge,
                "Another English line after it." to MaterialTheme.typography.bodyLarge,
                HEBREW_BODY to MaterialTheme.typography.bodyLarge
            )
        )
    }

    private fun snapshot(name: String, content: @Composable () -> Unit) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "src/test/snapshots/roborazzi/$name.png")
    }

    @Composable
    private fun NoteBody(lines: List<Pair<String, androidx.compose.ui.text.TextStyle>>) {
        lines.forEach { (text, style) ->
            Text(
                text = text,
                style = style,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    private companion object {
        const val ARABIC_HEADING = "ملاحظات"
        const val ARABIC_BODY = "هذه فقرة عربية داخل ملاحظة."
        const val HEBREW_BODY = "זו פסקה בעברית בתוך פתק."
    }
}
