package com.markleaf.notes.core.markdown.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * [Modifier.previewZoomGesture] driven by a real (simulated) two-pointer
 * touch sequence through Compose's own input pipeline — [PreviewZoomTest]
 * covers the arithmetic these assertions rely on, in isolation. What that
 * test can't show is whether an actual pinch reaches the modifier and is
 * correctly told apart from an ordinary single-finger touch; this does.
 *
 * A minimal host rather than the full [MarkdownPreviewList]: the gesture
 * modifier is the unit under test, and a small scrollable list gives
 * [androidx.compose.foundation.lazy.LazyListState] real content to scroll,
 * which the vertical half of a pinch drives through.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w400dp-h800dp-mdpi")
class PreviewZoomGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var reportedScale = 1f
    private var reportedTranslationX = 0f
    private var clicks = 0
    private lateinit var hostListState: LazyListState

    private fun renderHost(itemCount: Int = 50) {
        composeRule.setContent {
            var scale by remember { mutableFloatStateOf(1f) }
            var translationX by remember { mutableFloatStateOf(0f) }
            val listState = rememberLazyListState()
            hostListState = listState
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .previewZoomGesture(
                        scale = scale,
                        translationX = translationX,
                        viewportWidth = { 400f },
                        listState = listState,
                        onScaleChange = { scale = it; reportedScale = it },
                        onTranslationXChange = { translationX = it; reportedTranslationX = it }
                    )
            ) {
                // The same shrink-then-magnify layout MarkdownPreviewList uses
                // on its SelectionContainer (#423 review, scroll-extent
                // finding): measuring at full height would leave LazyColumn
                // computing a scroll range for the unscaled content, so at 2x
                // only the top half of even a short list would ever be
                // reachable. Reproduced here so this test host can verify the
                // mechanism itself, independent of the full preview.
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
                                    translationX = translationX
                                    transformOrigin = TransformOrigin(0f, 0f)
                                }
                            }
                        }
                ) {
                    items(itemCount) { index ->
                        Text(
                            "Line $index",
                            modifier = Modifier.fillMaxWidth().height(48.dp).clickable { clicks++ }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun aTwoFingerPinchApart_increasesTheReportedScale() {
        renderHost()

        composeRule.onRoot().performTouchInput {
            // Two fingers starting close together at the centroid, moving
            // apart symmetrically -- a textbook pinch-out (zoom in).
            down(0, Offset(180f, 300f))
            down(1, Offset(220f, 300f))
            moveTo(0, Offset(100f, 300f))
            moveTo(1, Offset(300f, 300f))
            up(0)
            up(1)
        }

        assertTrue("expected scale to grow past 1, was $reportedScale", reportedScale > 1.05f)
    }

    @Test
    fun aSingleFingerDragAtRest_neverChangesScaleOrTranslation() {
        // At scale 1 a single pointer must be left alone -- it's the list's
        // own scroll, not something this gesture claims. Regression guard for
        // the Initial-pass consumption rule the modifier depends on.
        renderHost()

        composeRule.onRoot().performTouchInput {
            down(0, Offset(200f, 500f))
            moveTo(0, Offset(200f, 200f))
            up(0)
        }

        assertEquals(1f, reportedScale, 0.001f)
        assertEquals(0f, reportedTranslationX, 0.001f)
    }

    @Test
    fun pinchingBackTogether_returnsScaleTowardOne() {
        renderHost()

        composeRule.onRoot().performTouchInput {
            down(0, Offset(100f, 300f))
            down(1, Offset(300f, 300f))
            moveTo(0, Offset(180f, 300f))
            moveTo(1, Offset(220f, 300f))
            up(0)
            up(1)
        }

        assertTrue("expected scale to shrink back toward 1, was $reportedScale", reportedScale < 1.3f)
    }

    @Test
    fun aSecondSeparatePinch_buildsOnTheScaleTheFirstOneLeft() {
        // Regression guard: previewZoomGesture reads scale/translationX through
        // rememberUpdatedState specifically so that a *second* pinch -- fingers
        // fully lifted, then a new gesture -- starts from where the first one
        // left off, not from the value scale had when the gesture's coroutine
        // was first launched. Two separate gestures, not one continuous drag.
        //
        // A gentle pinch (separation 100 -> 150, a 1.5x ratio) on purpose: two
        // of the sharp pinches the other tests use would clamp at MAX_SCALE
        // after the first gesture, and a scale already pinned at the ceiling
        // can't demonstrate a second gesture building on it.
        renderHost()

        composeRule.onRoot().performTouchInput {
            down(0, Offset(150f, 300f))
            down(1, Offset(250f, 300f))
            moveTo(0, Offset(125f, 300f))
            moveTo(1, Offset(275f, 300f))
            up(0)
            up(1)
        }
        val scaleAfterFirstPinch = reportedScale
        assertTrue("expected the first pinch to have zoomed in, was $scaleAfterFirstPinch", scaleAfterFirstPinch > 1.2f)
        assertTrue("expected room before the ceiling for a second pinch to show growth", scaleAfterFirstPinch < 3f)

        composeRule.onRoot().performTouchInput {
            down(0, Offset(150f, 300f))
            down(1, Offset(250f, 300f))
            moveTo(0, Offset(125f, 300f))
            moveTo(1, Offset(275f, 300f))
            up(0)
            up(1)
        }

        assertTrue(
            "expected the second pinch to zoom in further from $scaleAfterFirstPinch, landed at $reportedScale",
            reportedScale > scaleAfterFirstPinch * 1.1f
        )
    }

    @Test
    fun zoomingIn_shrinksTheListsMeasuredViewport() {
        // Regression guard for the #423 review's scroll-extent finding: measuring
        // LazyColumn at full height regardless of scale left it computing a
        // scroll range for the unscaled content, so scrollBy had nothing further
        // to give once the unscaled list thought it was already at the bottom --
        // at 2x, only the top half of even a short note was ever reachable. The
        // fix measures the list at height / scale so its own scroll range
        // accounts for how much magnified content doesn't fit on screen at once.
        // Checked here at the seam that actually matters: what LazyColumn itself
        // reports its viewport as, not the arithmetic already covered elsewhere.
        renderHost()
        composeRule.waitForIdle()
        val heightAtRest = hostListState.layoutInfo.viewportSize.height
        assertTrue("expected a real measured viewport before zooming", heightAtRest > 0)

        composeRule.onRoot().performTouchInput {
            down(0, Offset(150f, 300f))
            down(1, Offset(250f, 300f))
            moveTo(0, Offset(100f, 300f))
            moveTo(1, Offset(300f, 300f))
            up(0)
            up(1)
        }
        composeRule.waitForIdle()

        val expectedShrunkHeight = (heightAtRest / reportedScale).roundToInt()
        assertTrue(
            "expected viewport height to shrink from $heightAtRest toward $expectedShrunkHeight at scale $reportedScale, was ${hostListState.layoutInfo.viewportSize.height}",
            kotlin.math.abs(hostListState.layoutInfo.viewportSize.height - expectedShrunkHeight) <= 2
        )
    }

    @Test
    fun aLongPressThenDragWhileZoomed_doesNotPan_leavingItForSelection() {
        // Regression guard for the #423 review's selection finding: once
        // zoomed in, every single-finger move used to be claimed as pan the
        // instant it started, which meant a long press to begin a text
        // selection -- SelectionContainer's own gesture, starting exactly the
        // same way -- could never reach it.
        //
        // A stationary long press alone can't tell fixed from broken: with no
        // movement, the old code never panned either, since pan is driven by
        // positionChange(). The discriminating sequence is long press *then*
        // drag -- the shape of dragging a selection handle, or extending a
        // selection, after it starts. The old code didn't care what preceded
        // a move; it would have panned on that drag same as any other. The
        // fix's race times out into "hand off to selection" before the drag
        // ever happens, so scale/translationX must still be untouched after.
        renderHost()

        // Zoom in first (a real pinch), then release both fingers fully.
        composeRule.onRoot().performTouchInput {
            down(0, Offset(150f, 300f))
            down(1, Offset(250f, 300f))
            moveTo(0, Offset(100f, 300f))
            moveTo(1, Offset(300f, 300f))
            up(0)
            up(1)
        }
        val scaleAfterZoom = reportedScale
        val translationAfterZoom = reportedTranslationX
        assertTrue("expected to be zoomed in before testing the long-press race", scaleAfterZoom > 1.2f)

        // Long press (past the timeout, no movement yet), then drag -- as if
        // extending a selection after it started.
        composeRule.onRoot().performTouchInput {
            down(0, Offset(200f, 400f))
            advanceEventTime(600)
            moveTo(0, Offset(120f, 400f))
            up(0)
        }

        assertEquals(scaleAfterZoom, reportedScale, 0.001f)
        assertEquals(translationAfterZoom, reportedTranslationX, 0.001f)
    }

    // ---- momentum on a zoomed pan (#500) ----

    /** Rows are 48dp tall at the mdpi density this test runs at, so dp == px. */
    private fun scrolledPx(): Int =
        hostListState.firstVisibleItemIndex * 48 + hostListState.firstVisibleItemScrollOffset

    private fun zoomInTwofold() {
        composeRule.onRoot().performTouchInput {
            down(0, Offset(150f, 300f))
            down(1, Offset(250f, 300f))
            moveTo(0, Offset(100f, 300f))
            moveTo(1, Offset(300f, 300f))
            up(0)
            up(1)
        }
        assertTrue("expected to be zoomed in, was $reportedScale", reportedScale > 1.8f)
    }

    /** Six quick upward moves of 64px, 16ms apart: ~4000px/s, a flick rather than a drag. */
    private fun androidx.compose.ui.test.TouchInjectionScope.flickUp(restBeforeLifting: Boolean = false) {
        down(0, Offset(200f, 700f))
        repeat(6) { moveBy(0, Offset(0f, -64f)) }
        if (restBeforeLifting) advanceEventTime(300)
        up(0)
    }

    /** What the six moves alone scroll the list by, with no glide after them. */
    private fun dragOnlyScrollPx(): Float = 6 * 64f / reportedScale

    @Test
    fun aFastPanWhileZoomed_keepsGlidingAfterTheFingerLifts() {
        // #500: the pan used to move the content by exactly the distance the
        // finger travelled and stop the instant it lifted. A flick now carries
        // on, so it has to end far past where a finger that stopped would.
        renderHost()
        zoomInTwofold()
        val before = scrolledPx()

        composeRule.onRoot().performTouchInput { flickUp() }
        composeRule.waitForIdle()

        val travelled = scrolledPx() - before
        assertTrue(
            "expected the flick to glide well past the ${dragOnlyScrollPx()}px the finger covered, travelled $travelled",
            travelled > dragOnlyScrollPx() * 2
        )
    }

    /**
     * The flick a real touchscreen reports: the release arrives a few
     * milliseconds after the last move, at the position that move left. The
     * synthetic [flickUp] lifts at the very instant of its last move, which
     * hid the bug below — an extra stationary sample with no time between it
     * and the move before it barely moves a fitted velocity.
     */
    private fun androidx.compose.ui.test.TouchInjectionScope.deviceLikeFlickUp() {
        down(0, Offset(200f, 700f))
        repeat(6) { moveBy(0, Offset(0f, -64f)) }
        advanceEventTime(8)
        up(0)
    }

    /** How far, in screen pixels, [deviceLikeFlickUp] carries the list from the top: drag plus glide. */
    private fun screenTravelOfAFlickFromTheTop(): Float {
        composeRule.runOnIdle { hostListState.requestScrollToItem(0) }
        composeRule.waitForIdle()
        val scale = reportedScale
        composeRule.onRoot().performTouchInput { deviceLikeFlickUp() }
        composeRule.waitForIdle()
        return scrolledPx() * scale
    }

    @Test
    fun aFlickWhileZoomed_carriesAsFarOnScreenAsTheSameFlickAtRest() {
        // #500, second report: "it still feels like there's no momentum." The
        // glide measured the throw differently from the list it stands in for.
        // It added the release itself as a velocity sample — a stationary point
        // a few milliseconds after a fast move, which a least-squares fit reads
        // as the finger braking — where Compose's own tracker (and the
        // platform's) leaves the release out. The same flick then glided far
        // less zoomed than unzoomed. A zoomed glide converts its distance back
        // through the scale, so on screen the two must travel alike.
        renderHost(itemCount = 400)
        val atRest = screenTravelOfAFlickFromTheTop()
        zoomInTwofold()
        val zoomed = screenTravelOfAFlickFromTheTop()
        val ratio = zoomed / atRest
        assertTrue(
            "expected a zoomed flick to travel about as far on screen as at rest ($atRest px), travelled $zoomed px (ratio $ratio)",
            ratio in 0.8f..1.25f
        )
    }

    @Test
    fun aSidewaysFlickWhileZoomed_keepsGlidingAfterTheFingerLifts() {
        // #500, second report: sideways strokes still felt rigid. The glide is
        // two-dimensional, so a sideways throw carries on like a vertical one
        // until it reaches the side of the magnified note. At 2x on a 400px
        // window that side is 400px away; the finger itself covers 90px.
        //
        // A guard, not the regression test: this passed before the fix too,
        // because even the under-measured throw reached that side. Both axes
        // come from one tracker, so the measurement itself is pinned by
        // aFlickWhileZoomed_carriesAsFarOnScreenAsTheSameFlickAtRest.
        renderHost()
        zoomInTwofold()
        val before = reportedTranslationX

        composeRule.onRoot().performTouchInput {
            down(0, Offset(300f, 400f))
            repeat(3) { moveBy(0, Offset(-30f, 0f)) }
            advanceEventTime(8)
            up(0)
        }
        composeRule.waitForIdle()

        val travelled = before - reportedTranslationX
        assertTrue("expected the sideways flick to glide well past the 90px the finger covered, travelled $travelled", travelled > 180f)
    }

    @Test
    fun aPanThatRestsBeforeTheFingerLifts_doesNotGlide() {
        // A finger that stopped before lifting reads as slow: a release that
        // comes long after the last move resets the tracker, as Compose's own
        // scrolling does.
        renderHost()
        zoomInTwofold()
        val before = scrolledPx()

        composeRule.onRoot().performTouchInput { flickUp(restBeforeLifting = true) }
        composeRule.waitForIdle()

        val travelled = scrolledPx() - before
        assertTrue(
            "expected only the ${dragOnlyScrollPx()}px the finger covered, travelled $travelled",
            travelled <= dragOnlyScrollPx() + 1
        )
    }

    @Test
    fun aGlideStopsAtTheEndOfTheNote() {
        // 20 rows of 48px against a 400px window at 2x leaves 560px to scroll;
        // a flick carries well past that, so the glide has to end on the last
        // row rather than ask the list for more than it has.
        renderHost(itemCount = 20)
        zoomInTwofold()

        composeRule.onRoot().performTouchInput { flickUp() }
        composeRule.waitForIdle()

        assertTrue("expected to have reached the end of the list", !hostListState.canScrollForward)
    }

    @Test
    fun aTouchThatStopsAGlide_stopsIt_andIsNotATapOnTheRowUnderIt() {
        // A touch meant as "stop" must not also tick the checkbox or open the
        // link under the finger. Clock stepped by hand so the glide is still
        // running when the finger comes down.
        //
        // Not something previewZoomGesture does itself: with the glide inside the
        // list's own scroll, the lazy list consumes the touch before a row sees
        // it (checked by taking that handler's own consume away — nothing
        // changed). So this is the pin on that Compose behaviour, which the
        // feature quietly depends on.
        renderHost()
        zoomInTwofold()
        composeRule.mainClock.autoAdvance = false

        composeRule.onRoot().performTouchInput { flickUp() }
        composeRule.mainClock.advanceTimeBy(100)
        val earlyInGlide = scrolledPx()
        composeRule.mainClock.advanceTimeBy(100)
        assertTrue("expected the glide to be still moving", scrolledPx() > earlyInGlide)

        composeRule.onRoot().performTouchInput { down(0, Offset(200f, 400f)); up(0) }
        val atStop = scrolledPx()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals("the touch that stopped the glide must not click the row", 0, clicks)
        assertEquals("expected the glide to have stopped where it was touched", atStop, scrolledPx())

        // With nothing gliding, the same tap reaches the row: the consumption
        // above is for a stopping touch only.
        composeRule.onRoot().performTouchInput { down(0, Offset(200f, 400f)); up(0) }
        assertEquals(1, clicks)
    }

    @Test
    fun twoTapsInARowWhileZoomed_bothReachTheRow() {
        // Regression guard for a #423 defect found while adding the glide: the
        // race that tells a long press from a pan read the release of a tap and
        // left the gesture waiting, so the *next* touch's first event was taken
        // as a continuation and consumed as a pan — every second tap on a link
        // or checkbox was swallowed while zoomed.
        renderHost()
        zoomInTwofold()

        composeRule.onRoot().performTouchInput { down(0, Offset(200f, 400f)); up(0) }
        assertEquals(1, clicks)
        composeRule.onRoot().performTouchInput { down(0, Offset(200f, 400f)); up(0) }
        assertEquals(2, clicks)
    }
}
