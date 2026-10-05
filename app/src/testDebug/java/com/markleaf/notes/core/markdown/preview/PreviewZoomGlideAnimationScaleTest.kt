package com.markleaf.notes.core.markdown.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * A zoomed glide on a device whose animations are switched off (#500, fourth
 * report: "very quick movements that come to an instant halt").
 *
 * Android's animator duration scale — Developer options, "Remove animations"
 * in Accessibility, and some battery savers — reaches every Compose animation
 * through the coroutine's [MotionDurationScale]. The list's own fling opts out:
 * it runs under a scale fixed at 1, so a flick reads the same with animations
 * off. The zoomed glide ran in the composition's scope and inherited the
 * device's scale: at 0.5 it played twice as fast, at 0 it jumped to its end in
 * one frame. This composition runs with the scale at 0 to stand for such a
 * device; [PreviewZoomGestureTest] covers the gesture itself at the default.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-mdpi")
class PreviewZoomGlideAnimationScaleTest {

    private object AnimationsOff : MotionDurationScale {
        override val scaleFactor: Float = 0f
    }

    @get:Rule
    val composeRule = createComposeRule(effectContext = AnimationsOff)

    private var reportedScale = 1f
    private lateinit var hostListState: LazyListState

    /** The same shrink-then-magnify host [PreviewZoomGestureTest] uses, cut to what a glide needs. */
    private fun renderHost() {
        composeRule.setContent {
            var scale by remember { mutableFloatStateOf(1f) }
            var translationX by remember { mutableFloatStateOf(0f) }
            val listState = rememberLazyListState()
            hostListState = listState
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .previewZoomGesture(
                        scale = scale,
                        translationX = translationX,
                        viewportWidth = { 400f },
                        listState = listState,
                        onScaleChange = { scale = it; reportedScale = it },
                        onTranslationXChange = { translationX = it }
                    )
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .layout { measurable, constraints ->
                            val shrunkHeight = (constraints.maxHeight / scale)
                                .roundToInt()
                                .coerceIn(1, constraints.maxHeight)
                            val placeable = measurable.measure(
                                constraints.copy(minHeight = 0, maxHeight = shrunkHeight)
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeWithLayer(0, 0) {
                                    scaleX = scale
                                    scaleY = scale
                                    this.translationX = translationX
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        }
                ) {
                    items(400) { index ->
                        Text("Line $index", modifier = Modifier.fillMaxWidth().height(48.dp))
                    }
                }
            }
        }
    }

    /** Rows are 48dp tall at mdpi, so dp == px. */
    private fun scrolledPx(): Int =
        hostListState.firstVisibleItemIndex * 48 + hostListState.firstVisibleItemScrollOffset

    @Test
    fun withAnimationsOff_aZoomedFlickStillGlidesOverTime() {
        renderHost()
        composeRule.onRoot().performTouchInput {
            down(0, Offset(150f, 300f))
            down(1, Offset(250f, 300f))
            moveTo(0, Offset(100f, 300f))
            moveTo(1, Offset(300f, 300f))
            up(0)
            up(1)
        }
        assertTrue("expected to be zoomed in, was $reportedScale", reportedScale > 1.8f)
        // Let the zoom reach the gesture handler before the clock stops, or the
        // flick below would still see scale 1 and go to the list's own fling.
        composeRule.waitForIdle()

        composeRule.mainClock.autoAdvance = false
        composeRule.onRoot().performTouchInput {
            down(0, Offset(200f, 700f))
            repeat(6) { moveBy(0, Offset(0f, -64f)) }
            advanceEventTime(8)
            up(0)
        }
        composeRule.mainClock.advanceTimeBy(100)
        val earlyInGlide = scrolledPx()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        val settled = scrolledPx()

        assertTrue(
            "expected the glide to be still under way 100ms after release (at $earlyInGlide px), " +
                "but it had already settled where it ends ($settled px)",
            earlyInGlide < settled - 48
        )
    }
}
