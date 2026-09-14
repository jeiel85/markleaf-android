package com.markleaf.notes.feature.editor

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the editor body's bottom inset arithmetic (#398).
 *
 * Input: the editor screen composed as the app builds it, with navigation-bar
 * and IME insets dispatched into the view hierarchy by hand.
 * Output: the distance the body's bottom-most row travels when the keyboard
 * appears.
 *
 * Core logic — and why the test is shaped as a *movement* rather than a
 * position. Window insets are distances from the window edge, so two bottom
 * insets overlap instead of stacking; the framework's own `safeDrawing` unions
 * `systemBars` with `ime` and takes the larger of the two. The editor has to
 * reach that same `max(navBar, ime)`, but it gets there in two steps that a
 * single measurement cannot tell apart from the wrong answer: Scaffold hands
 * the navigation-bar inset down as `PaddingValues`, and `imePadding()` adds the
 * keyboard on top of it. Before #398 nothing marked the first one consumed, so
 * the two were summed and one navigation bar of dead space sat above the
 * keyboard.
 *
 * Asserting an absolute position would mean hard-coding every padding inside
 * the editor body and the formatting row, and would break on any unrelated
 * spacing change. The *travel* of the bottom row is free of all of them:
 *
 *   correct — bottom padding goes navBar → max(navBar, ime) = ime,  travel = ime - navBar
 *   summed  — bottom padding goes navBar → navBar + ime,            travel = ime
 *   insets never arrived (the test's own premise failing)           travel = 0
 *
 * The three are far apart with the values used here, so the assertion also
 * reports which of them happened rather than only that the number was wrong.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorImeInsetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun keyboardMovesTheBodyByTheKeyboardMinusTheNavigationBar() {
        composeRule.setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                EditorScreen(onBack = {})
            }
        }

        // The bottom-most thing in the editor body, so its bottom edge is the
        // body's bottom edge plus a fixed offset that cancels in the subtraction.
        val formatting = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.formatting)
        val bottomRow = composeRule.onNodeWithContentDescription(formatting)
        bottomRow.assertExists(
            "The formatting row is the measured edge; without it there is nothing to measure."
        )

        dispatchInsets(navigationBar = NAV_BAR, ime = 0.dp)
        val keyboardDown = bottomRow.getUnclippedBoundsInRoot().bottom

        dispatchInsets(navigationBar = NAV_BAR, ime = IME)
        val keyboardUp = bottomRow.getUnclippedBoundsInRoot().bottom

        val travel = keyboardDown - keyboardUp
        val expected = IME - NAV_BAR

        assertTrue(
            buildString {
                appendLine("The editor body moved ${travel.value}dp when the keyboard appeared.")
                appendLine("Expected ${expected.value}dp — max($NAV_BAR, $IME) minus $NAV_BAR.")
                when {
                    abs(travel.value) < 1f ->
                        appendLine(
                            "It did not move at all, so the dispatched insets never reached " +
                                "composition and this test proved nothing. Fix the test, not " +
                                "the editor."
                        )
                    abs((travel - IME).value) < TOLERANCE.value ->
                        appendLine(
                            "It moved the whole keyboard height, which means the " +
                                "navigation-bar inset is being added on top of the IME inset " +
                                "instead of being consumed by it: the #398 gap is back. Look " +
                                "for a lost consumeWindowInsets(paddingValues) above " +
                                "imePadding() in EditorScreen."
                        )
                    else ->
                        appendLine("Neither the fixed nor the summed value — check the layout.")
                }
            },
            abs((travel - expected).value) < TOLERANCE.value
        )

        // Stated separately so a future reader sees the invariant the numbers
        // above are standing in for, not just the arithmetic.
        assertEquals(
            "With the keyboard up the body's bottom inset must be the larger of the two, " +
                "never their sum.",
            maxOf(NAV_BAR.value, IME.value),
            (NAV_BAR + travel).value,
            TOLERANCE.value
        )
    }

    /**
     * Feeds insets to the composition the way the platform would.
     *
     * Compose reads window insets from a listener it installs on its own host
     * view, so the values have to be dispatched into the view tree rather than
     * injected through a CompositionLocal — there is no public API for the
     * latter. Dispatching on the content root lets the framework's own
     * propagation carry them down to that host view.
     */
    private fun dispatchInsets(navigationBar: Dp, ime: Dp) {
        val density = composeRule.density
        val insets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(0, 0, 0, with(density) { navigationBar.roundToPx() })
            )
            .setInsets(
                WindowInsetsCompat.Type.ime(),
                Insets.of(0, 0, 0, with(density) { ime.roundToPx() })
            )
            .build()

        composeRule.runOnUiThread {
            val root = contentRoot()
            ViewCompat.dispatchApplyWindowInsets(root, insets)
        }
        composeRule.waitForIdle()
    }

    private fun contentRoot(): View {
        val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
        return content.getChildAt(0) ?: content
    }

    private companion object {
        /** A three-button navigation bar; the gap #398 reported was this tall. */
        val NAV_BAR = 48.dp

        /** Roughly a soft keyboard on the 640dp-tall device this test configures. */
        val IME = 280.dp

        /** Rounding between dp and px, not a licence for a real difference. */
        val TOLERANCE = 2.dp
    }
}
