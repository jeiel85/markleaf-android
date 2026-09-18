package com.markleaf.notes.core.markdown.preview

import androidx.compose.foundation.background
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
    private lateinit var hostListState: LazyListState

    private fun renderHost() {
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
                    items(50) { index ->
                        Text("Line $index", modifier = Modifier.fillMaxWidth().height(48.dp))
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
}
