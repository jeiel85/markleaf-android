package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import kotlin.math.max

/**
 * The number of lines a note has to reach before the jump control appears
 * (#214). Below this a note is at most a screen or two, and scrolling to either
 * end is a flick — a button would be clutter on every short note in the app.
 */
internal const val JUMP_CONTROL_MIN_LINES = 40

/**
 * Rough characters per rendered line of the editor on a phone. Used to estimate
 * how many screen lines a note actually needs: source-line counts alone miss
 * wrapping, so five long paragraphs can fill sixty screen lines while counting
 * as five (#262). 40 is what a comfortable-width `BasicTextField` shows on a
 * typical phone — an estimate on purpose, since the real number depends on the
 * font, the line width, and the screen.
 */
internal const val JUMP_CONTROL_CHARS_PER_LINE = 40

/**
 * Whether [markdown] is long enough to be worth offering a jump.
 *
 * Counted on the source text so both surfaces agree: it is the note that is
 * long, not one rendering of it, and a control that appeared in preview but not
 * in the editor for the same note would read as a bug. "Long enough" is the
 * larger of the two counts — the source lines and the rendered-line estimate —
 * so a note of forty short lines and a note whose text wraps past forty screen
 * lines both earn the control, and a short note earns it under neither.
 */
internal fun isLongEnoughToJump(markdown: String): Boolean {
    val sourceLines = markdown.count { it == '\n' } + 1
    val renderedEstimate =
        (markdown.length + JUMP_CONTROL_CHARS_PER_LINE - 1) / JUMP_CONTROL_CHARS_PER_LINE
    return max(sourceLines, renderedEstimate) >= JUMP_CONTROL_MIN_LINES
}

/**
 * A small button that jumps to whichever end of the note you are further from
 * (#214).
 *
 * Deliberately not tied to the "open notes at" setting: wanting to get to the
 * other end of a long note is not the same preference as wanting to start
 * there, and hiding it behind a toggle would mean the people who need it most
 * have to know it exists to turn it on.
 */
@Composable
internal fun JumpToEndButton(
    /** True when the far end is the bottom — i.e. you are nearer the top. */
    jumpsToBottom: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Icon(
            imageVector = if (jumpsToBottom) {
                Icons.Default.KeyboardArrowDown
            } else {
                Icons.Default.KeyboardArrowUp
            },
            contentDescription = stringResource(
                if (jumpsToBottom) R.string.jump_to_end else R.string.jump_to_start
            ),
            modifier = Modifier.size(24.dp)
        )
    }
}
