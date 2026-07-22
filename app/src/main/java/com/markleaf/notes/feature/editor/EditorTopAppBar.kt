package com.markleaf.notes.feature.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.markleaf.notes.R

/** Tint applied to the view-toggle icon while "open notes in preview" is locked
 *  on, signalling the sticky mode. A fixed amber that stays legible on both the
 *  light and dark top-bar backgrounds — it's a status colour, not a theme
 *  role, so it deliberately sits outside the colour scheme (#200). */
private val ViewModeLockedTint = Color(0xFFF57C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorTopAppBar(
    title: String,
    isPreviewMode: Boolean,
    isFocusMode: Boolean,
    showMore: Boolean,
    moreExpanded: Boolean,
    onBack: () -> Unit,
    onTogglePreview: () -> Unit,
    onExitFocusMode: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenMore: () -> Unit,
    onDismissMore: () -> Unit,
    moreMenuContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    isViewModeLocked: Boolean = false,
    onToggleLock: () -> Unit = {}
) {
    TopAppBar(
        title = {
            androidx.compose.material3.Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
            }
        },
        actions = {
            if (isFocusMode) {
                IconButton(onClick = onExitFocusMode) {
                    Icon(
                        Icons.Default.CenterFocusWeak,
                        contentDescription = stringResource(R.string.exit_focus_mode)
                    )
                }
            } else {
                // IconButton exposes no long-press hook, so the persistent lock
                // rides on a pointerInput watching the Initial pass: it fires the
                // toggle once the press crosses the long-press threshold, then
                // consumes the release so the IconButton's own Main-pass tap
                // (onTogglePreview) never also fires. The matching a11y action is
                // published via semantics.onLongClick. Locking recolours the icon
                // amber to signal the sticky mode (#200).
                val lockLabel = stringResource(R.string.lock_view_mode)
                val currentOnToggleLock by rememberUpdatedState(onToggleLock)
                IconButton(
                    onClick = onTogglePreview,
                    modifier = Modifier
                        .semantics {
                            onLongClick(label = lockLabel) {
                                currentOnToggleLock()
                                true
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial
                                )
                                try {
                                    withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                        waitForUpOrCancellation(PointerEventPass.Initial)
                                    }
                                } catch (_: PointerEventTimeoutCancellationException) {
                                    currentOnToggleLock()
                                    waitForUpOrCancellation(PointerEventPass.Initial)?.consume()
                                }
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (isPreviewMode) R.string.edit else R.string.preview
                        ),
                        tint = if (isViewModeLocked) {
                            ViewModeLockedTint
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                IconButton(onClick = onOpenInfo) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.note_information)
                    )
                }
                if (showMore) {
                    Box {
                        IconButton(onClick = onOpenMore) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = moreExpanded,
                            onDismissRequest = onDismissMore,
                            content = moreMenuContent
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
    )
}
