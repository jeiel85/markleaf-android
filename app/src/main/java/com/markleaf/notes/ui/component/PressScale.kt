package com.markleaf.notes.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.withTransform

/** How far a pressed card gives way; small enough to read as touch, not as a state change. */
internal const val PRESSED_SCALE = 0.97f

/**
 * A pressed card sinks slightly under the finger and springs back on release.
 *
 * Input: the [InteractionSource] handed to the card's `combinedClickable`.
 * Output: the modifier, to be placed first on the card so the whole card —
 * background, ripple and text — moves as one.
 *
 * Why the scale is only drawn, never laid out or applied as a graphics layer:
 * a layer scale also shrinks the pointer hit region, so the outer sliver of a
 * card stops responding for as long as it is held down (`PressScaleTest` shows
 * the edge tap being dropped). A draw transform leaves hit testing and layout
 * exactly as they were, and it is read in the draw phase, so the animation
 * costs no recomposition per frame. It also draws nothing extra at rest (scale
 * 1 skips the transform), so a settled list renders as it did before.
 *
 * The spring is created by Compose's animation APIs, which follow the system
 * animation scale like every other animation in the app.
 */
@Composable
internal fun Modifier.pressScale(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press scale"
    )
    return drawWithContent {
        val current = scale
        if (current == 1f) {
            drawContent()
        } else {
            withTransform({ scale(current, current, center) }) {
                this@drawWithContent.drawContent()
            }
        }
    }
}
