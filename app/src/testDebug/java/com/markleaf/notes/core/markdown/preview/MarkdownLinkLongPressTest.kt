package com.markleaf.notes.core.markdown.preview

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.annotation.GraphicsMode

/**
 * Regression net for #386: preview mode has to give up a link's address without
 * a trip back to the editor, and the long press that does it has to stay out of
 * the way of everything else the preview already answers to — the tap that
 * opens a link, and the long press that now starts a text selection.
 *
 * sdk 27 rather than the 33 the other preview tests use: from API 28 Compose's
 * selection draws a platform Magnifier, which needs a real window and throws
 * inside Robolectric the moment a long press starts a selection. The gesture
 * under test does not branch on API level, so testing it a rung below the
 * magnifier costs nothing — except that the toast branch (< API 33) is the one
 * running here, which [longPressingLink_copiesItsAddress] asserts on.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [27], qualifiers = "w360dp-h640dp-mdpi")
class MarkdownLinkLongPressTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun longPressingLink_copiesItsAddress() {
        renderPreview("[the docs](https://markleaf.app/docs)")

        composeRule.onNodeWithText("the docs").performTouchInput {
            longClick(centerLeft.copy(x = 12f))
        }

        assertEquals("https://markleaf.app/docs", clipboardText())
        // Copying has to replace opening, not accompany it: on a device the
        // press otherwise landed the address on the clipboard *and* launched
        // the browser over the note.
        assertNull(startedActivity())
        // Below API 33 nothing else tells the user the copy happened.
        assertEquals(
            application().getString(R.string.preview_link_address_copied),
            ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun longPressingLinkInTableCell_copiesItsAddress() {
        renderPreview(
            """
            | name | link |
            | --- | --- |
            | Docs | [address](https://markleaf.app/docs) |
            """.trimIndent()
        )

        composeRule.onNodeWithText("address").performTouchInput {
            longClick(centerLeft.copy(x = 12f))
        }

        assertEquals("https://markleaf.app/docs", clipboardText())
        assertNull(startedActivity())
    }

    /**
     * The gesture must not consume a press that lands on ordinary prose —
     * that press belongs to the selection the preview now offers, and a
     * clipboard write here would mean the selection never started.
     */
    @Test
    fun longPressingPlainText_copiesNothing() {
        renderPreview("just some prose")

        composeRule.onNodeWithText("just some prose").performTouchInput {
            longClick(centerLeft.copy(x = 12f))
        }

        assertNull(clipboardText())
    }

    /**
     * Same row as a link, but past its end: the hit test has to work by text
     * offset rather than by "this Text contains a link somewhere".
     */
    @Test
    fun longPressingTextAfterLinkInSameRow_copiesNothing() {
        renderPreview("[docs](https://markleaf.app/docs) trailing words")

        composeRule.onNodeWithText("docs trailing words").performTouchInput {
            longClick(centerRight.copy(x = width - 12f))
        }

        assertNull(clipboardText())
    }

    /**
     * The press has to be claimed from the selection gesture, not merely raced
     * with it: copying an address while selection handles rise over the same
     * link would be two answers to one press.
     *
     * Robolectric makes that visible for free from API 28, where selection
     * draws a platform Magnifier that throws without a real window — so a
     * selection starting here fails this test rather than passing quietly. The
     * moves matter too: a resting finger still reports, and [longClick] sends
     * nothing between down and up, leaving the gesture no event to consume.
     */
    @Test
    @Config(sdk = [33])
    fun longPressingLink_doesNotStartASelection() {
        renderPreview("[the docs](https://markleaf.app/docs)")

        composeRule.onNodeWithText("the docs").performTouchInput {
            val target = centerLeft.copy(x = 12f)
            down(target)
            repeat(6) { step ->
                // Under the touch slop, so this stays a press and never becomes
                // a drag that would cancel it.
                moveTo(target + Offset(if (step % 2 == 0) 1f else 0f, 0f), delayMillis = 100)
            }
            up()
        }

        assertEquals("https://markleaf.app/docs", clipboardText())
        assertNull(startedActivity())
        // API 33 shows the platform's own clipboard confirmation, so the app
        // stays quiet.
        assertNull(ShadowToast.getTextOfLatestToast())
    }

    /**
     * Sliding off a link is not a long press on it. Nothing else would say so
     * either: a sideways drag is not a scroll the vertical list would claim, so
     * without a distance check of its own the address was still copied once the
     * timeout came round, wherever the finger had ended up.
     */
    @Test
    fun pressThatSlidesOffTheLink_copiesNothing() {
        renderPreview("[the docs](https://markleaf.app/docs) and words after it")

        composeRule.onNodeWithText("the docs and words after it").performTouchInput {
            val start = centerLeft.copy(x = 12f)
            down(start)
            moveTo(start + Offset(viewConfiguration.touchSlop * 4, 0f), delayMillis = 50)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis)
            up()
        }

        assertNull(clipboardText())
    }

    /**
     * A press released before the platform's own long-press threshold is a tap,
     * however slow. Deciding on a shortened threshold made the 400-499ms band
     * copy the address out from under a tap that Android still called a tap.
     */
    @Test
    fun slowTapUnderTheThreshold_opensTheLinkInstead() {
        renderPreview("[the docs](https://markleaf.app/docs)")

        composeRule.onNodeWithText("the docs").performTouchInput {
            val target = centerLeft.copy(x = 12f)
            down(target)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis - 50)
            up()
        }

        assertNull(clipboardText())
        assertEquals(Intent.ACTION_VIEW, startedActivity()?.action)
    }

    /** A tap still opens the link — the long-press handler must not eat it. */
    @Test
    fun tappingLink_stillOpensIt() {
        renderPreview("[the docs](https://markleaf.app/docs)")

        composeRule.onNodeWithText("the docs").performTouchInput {
            click(centerLeft.copy(x = 12f))
        }

        val started = startedActivity()
        assertEquals(Intent.ACTION_VIEW, started?.action)
        assertEquals("https://markleaf.app/docs", started?.data?.toString())
        assertNull(clipboardText())
    }

    private fun renderPreview(markdown: String) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                MarkdownPreviewList(lines = SimpleMarkdownPreview.parse(markdown))
            }
        }
    }

    private fun application(): Application = ApplicationProvider.getApplicationContext()

    private fun startedActivity(): Intent? =
        Shadows.shadowOf(application()).nextStartedActivity

    private fun clipboardText(): String? {
        val manager = application()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()
    }
}
