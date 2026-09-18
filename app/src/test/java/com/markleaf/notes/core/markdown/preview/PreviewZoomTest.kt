package com.markleaf.notes.core.markdown.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewZoomTest {

    private val eps = 0.001f

    @Test
    fun clampScale_staysWithinOneAndFive() {
        assertEquals(1f, PreviewZoom.clampScale(0.4f), eps)
        assertEquals(1f, PreviewZoom.clampScale(1f), eps)
        assertEquals(3f, PreviewZoom.clampScale(3f), eps)
        assertEquals(5f, PreviewZoom.clampScale(12f), eps)
    }

    // --- pivotTranslationX ---------------------------------------------

    @Test
    fun pivotTranslationX_atRestNoTranslation_zoomingInFromTheLeftEdge() {
        // Pinching at the very left edge (focal x = 0) with no existing pan:
        // the left edge is already the pivot, so it stays put.
        assertEquals(0f, PreviewZoom.pivotTranslationX(focalX = 0f, oldTranslationX = 0f, oldScale = 1f, newScale = 2f), eps)
    }

    @Test
    fun pivotTranslationX_zoomingInAtTheCenterOfA400dpViewport() {
        // 400px wide viewport, pinch centered at x=200, scale 1 -> 2, no prior pan.
        // The content point under x=200 is x=200 (scale 1). At scale 2 that
        // point draws at 200*2=400 unless translation shifts it back to 200,
        // so translationX must become -200.
        val result = PreviewZoom.pivotTranslationX(focalX = 200f, oldTranslationX = 0f, oldScale = 1f, newScale = 2f)
        assertEquals(-200f, result, eps)
    }

    @Test
    fun pivotTranslationX_zoomingOutReturnsTowardZero() {
        // The inverse of the case above: scale 2 -> 1 with translationX = -200
        // at focal x = 200 must land back at translationX = 0.
        val result = PreviewZoom.pivotTranslationX(focalX = 200f, oldTranslationX = -200f, oldScale = 2f, newScale = 1f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun pivotTranslationX_holdsTheFocalScreenPositionFixed_acrossAnAlreadyPannedState() {
        // General property check rather than a hand-computed example: whatever
        // translation results, the content point that was under the focal
        // point before the scale change must still be under it after.
        val focalX = 137f
        val oldTranslationX = -63f
        val oldScale = 1.8f
        val newScale = 3.4f
        val contentXUnderFocal = (focalX - oldTranslationX) / oldScale

        val newTranslationX = PreviewZoom.pivotTranslationX(focalX, oldTranslationX, oldScale, newScale)
        val screenXAfter = newTranslationX + contentXUnderFocal * newScale

        assertEquals(focalX, screenXAfter, 0.01f)
    }

    // --- pivotScrollDelta -------------------------------------------------

    @Test
    fun pivotScrollDelta_atTheViewportTop_isZero() {
        // focalY = 0 means the pinch is centered on the very top of the
        // viewport, which is also the scroll anchor -- nothing to correct.
        assertEquals(0f, PreviewZoom.pivotScrollDelta(focalY = 0f, oldScale = 1f, newScale = 3f), eps)
    }

    @Test
    fun pivotScrollDelta_zoomingIn_scrollsForwardToKeepTheLowerFocalPointInPlace() {
        // 300px below the viewport top, scale 1 -> 3: the content that used to
        // draw 300px down now draws 900px down unless the list scrolls forward
        // by the equivalent content-space amount. 300 * (1/1 - 1/3) = 200.
        val delta = PreviewZoom.pivotScrollDelta(focalY = 300f, oldScale = 1f, newScale = 3f)
        assertEquals(200f, delta, 0.01f)
    }

    @Test
    fun pivotScrollDelta_zoomingOut_isTheExactInverseOfZoomingIn() {
        val zoomIn = PreviewZoom.pivotScrollDelta(focalY = 300f, oldScale = 1f, newScale = 3f)
        val zoomOut = PreviewZoom.pivotScrollDelta(focalY = 300f, oldScale = 3f, newScale = 1f)
        assertEquals(0f, zoomIn + zoomOut, 0.01f)
    }

    // --- clampTranslationX -------------------------------------------------

    @Test
    fun clampTranslationX_atScaleOne_onlyZeroIsAllowed() {
        // No overflow at scale 1 -- there's nothing to pan to. (Float zero has
        // a signed -0.0 representation that trips exact equality, hence eps.)
        assertEquals(0f, PreviewZoom.clampTranslationX(translationX = -50f, scale = 1f, viewportWidth = 400f), eps)
        assertEquals(0f, PreviewZoom.clampTranslationX(translationX = 50f, scale = 1f, viewportWidth = 400f), eps)
    }

    @Test
    fun clampTranslationX_atDoubleScale_allowsPanningAcrossTheFullOverflow() {
        // 400px viewport at scale 2 has 400px of overflow, so the valid
        // translationX range is [-400, 0].
        assertEquals(-400f, PreviewZoom.clampTranslationX(translationX = -999f, scale = 2f, viewportWidth = 400f), eps)
        assertEquals(0f, PreviewZoom.clampTranslationX(translationX = 999f, scale = 2f, viewportWidth = 400f), eps)
        assertEquals(-150f, PreviewZoom.clampTranslationX(translationX = -150f, scale = 2f, viewportWidth = 400f), eps)
    }
}
