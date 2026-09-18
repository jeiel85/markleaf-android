package com.markleaf.notes.core.markdown.preview

/**
 * The arithmetic behind pinch-to-magnify on the preview (#423) — kept free of
 * gesture APIs and `LazyListState` so it can be tested without Compose or
 * Robolectric. The gesture handler in [MarkdownPreviewList] is the only
 * caller; it owns the actual scale/offset state and drives
 * `LazyListState.scrollBy` with [pivotScrollDelta]'s result.
 *
 * Two axes, two different mechanisms, on purpose:
 * - **Vertical** stays real scroll (`LazyListState`), because that is the only
 *   way the list keeps composing rows as the visible position moves — a
 *   `graphicsLayer` translation alone would leave rows outside the original
 *   viewport uncomposed, i.e. blank, exactly the risk named when this was
 *   scoped out as a spike.
 * - **Horizontal** has no such virtualization to protect (a row's full width is
 *   already laid out), so it is a plain `graphicsLayer` `translationX` value
 *   clamped to the overflow zoom creates.
 *
 * Both directions need the same property under a pinch: the content point
 * under the gesture's focal point must not visibly move when the scale
 * changes, or the zoom reads as jumping around rather than magnifying in
 * place. [pivotTranslationX] and [pivotScrollDelta] are that property,
 * expressed in each axis's own unit — a translation offset for X, a scroll
 * delta for Y — derived from the same "screen position = (content position -
 * origin) * scale" model, solved for the origin that keeps the focal point
 * fixed before and after the scale change.
 */
object PreviewZoom {
    const val MIN_SCALE = 1f
    const val MAX_SCALE = 5f

    /** Keeps a pinch from shrinking the preview below its normal size or past a usable ceiling. */
    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * The `translationX` that keeps [focalX] over the same content after
     * [oldScale] becomes [newScale], given the translation that was in effect
     * at [oldScale].
     *
     * Derivation: a content point at local x-coordinate `cx` draws at screen
     * position `translationX + cx * scale`. Solving the *old* equation for
     * `cx` at the focal point and substituting into the *new* equation, with
     * both required to equal [focalX], gives
     * `newTranslationX = focalX - (focalX - oldTranslationX) * (newScale / oldScale)`.
     */
    fun pivotTranslationX(focalX: Float, oldTranslationX: Float, oldScale: Float, newScale: Float): Float {
        if (oldScale == 0f) return oldTranslationX
        return focalX - (focalX - oldTranslationX) * (newScale / oldScale)
    }

    /**
     * The additional scroll, in content pixels (the unit `LazyListState.scrollBy`
     * takes), that keeps [focalY] — measured from the current top of the
     * viewport — over the same content after [oldScale] becomes [newScale].
     *
     * Derivation: with no separate translation on this axis, a content point
     * `dy` below the current scroll-top draws at screen position `dy * scale`.
     * Holding the focal point's screen position fixed across the scale change
     * and solving for the scroll adjustment gives
     * `scrollDelta = focalY * (1 / oldScale - 1 / newScale)`. A positive result
     * is a downward scroll (content moves up), matching `scrollBy`'s own sign.
     */
    fun pivotScrollDelta(focalY: Float, oldScale: Float, newScale: Float): Float {
        if (oldScale == 0f || newScale == 0f) return 0f
        return focalY * (1f / oldScale - 1f / newScale)
    }

    /**
     * Clamps a horizontal translation so zoomed content never leaves a gap
     * beside the viewport. At [scale] the content is [viewportWidth] *
     * ([scale] - 1) wider than the viewport it started filling exactly, all of
     * it to the right of x = 0 before any translation — so translation ranges
     * from 0 (left edges aligned) to minus that overflow (right edges
     * aligned), and never further either way.
     */
    fun clampTranslationX(translationX: Float, scale: Float, viewportWidth: Float): Float {
        val overflow = (viewportWidth * (scale - 1f)).coerceAtLeast(0f)
        return translationX.coerceIn(-overflow, 0f)
    }
}
