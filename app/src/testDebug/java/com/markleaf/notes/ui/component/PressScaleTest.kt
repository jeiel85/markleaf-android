package com.markleaf.notes.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [pressScale] must only *draw* smaller. If a press shrank the touch target, the
 * outer sliver of a card would stop responding for as long as the card is held
 * down — a defect that never shows in a screenshot and only happens to some
 * taps.
 *
 * The taps are injected in root coordinates on purpose: coordinates given
 * relative to a node are mapped through that node's own layer, which quietly
 * follows the shrunken card and would hide exactly the difference under test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class PressScaleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var clicks = 0
    private lateinit var source: MutableInteractionSource

    private fun setUpCard() {
        composeRule.setContent {
            source = remember { MutableInteractionSource() }
            // The same order as the note rows: the scale outermost, the click
            // target inside it (16dp of margin either side).
            Text(
                text = "Card",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .pressScale(source)
                    .padding(horizontal = 16.dp)
                    .background(Color.LightGray)
                    .clickable(interactionSource = source, indication = null) { clicks++ }
            )
        }
    }

    private fun tapAtRoot(x: Float, y: Float) {
        composeRule.onRoot().performTouchInput { down(Offset(x, y)) }
        composeRule.onRoot().performTouchInput { up() }
        composeRule.waitForIdle()
    }

    @Test
    fun theTouchTargetDoesNotShrinkWhileTheCardIsHeldDown() {
        setUpCard()
        // Another finger, or the same press held by a long-press menu: something
        // keeps the card pressed, so it sits at its pressed scale.
        composeRule.runOnUiThread { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        composeRule.waitForIdle()

        // 1px inside the clickable's left edge (16dp margin, mdpi).
        tapAtRoot(17f, 36f)

        assertEquals("the edge of a pressed card stopped responding", 1, clicks)
    }

    @Test
    fun aPlainTapStillClicks() {
        setUpCard()

        tapAtRoot(180f, 36f)

        assertEquals(1, clicks)
    }
}
