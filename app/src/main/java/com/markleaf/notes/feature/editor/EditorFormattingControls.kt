package com.markleaf.notes.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

internal data class EditorFormattingUiState(
    val selectionActive: Boolean = false,
    val expanded: Boolean = false,
    val enabled: Boolean = true,
    val statsText: String? = null
)

private data class FormattingItem(
    val action: EditorFormattingAction,
    val label: String,
    val icon: ImageVector
)

private data class FormattingGroup(
    val label: String,
    val items: List<FormattingItem>
)

@Composable
internal fun EditorFormattingControls(
    state: EditorFormattingUiState,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (EditorFormattingAction) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val triggerFocusRequester = remember { FocusRequester() }
    val firstActionFocusRequester = remember { FocusRequester() }
    var openedFromKeyboard by remember { mutableStateOf(false) }

    LaunchedEffect(state.expanded) {
        if (state.expanded && openedFromKeyboard) {
            withFrameNanos { }
            firstActionFocusRequester.requestFocus()
        } else if (!state.expanded && openedFromKeyboard) {
            triggerFocusRequester.requestFocus()
            openedFromKeyboard = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        Column(modifier = Modifier.fillMaxWidth()) {
            if (state.expanded) {
                FormattingPanel(
                    groups = formattingGroups(),
                    onAction = { action ->
                        onExpandedChange(false)
                        onAction(action)
                    },
                    onDismiss = { onExpandedChange(false) },
                    firstActionFocusRequester = firstActionFocusRequester,
                    modifier = if (availableWidth >= 600.dp) {
                        Modifier.widthIn(max = 360.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
            }

            if (state.selectionActive) {
                SelectionFormattingActions(
                    showLink = availableWidth >= 192.dp,
                    expanded = state.expanded,
                    enabled = state.enabled,
                    triggerFocusRequester = triggerFocusRequester,
                    onKeyboardOpen = {
                        openedFromKeyboard = true
                        onExpandedChange(true)
                    },
                    onExpandedChange = onExpandedChange,
                    onAction = { action ->
                        if (state.expanded) onExpandedChange(false)
                        onAction(action)
                    }
                )
            } else {
                FormattingEntryRow(
                    state = state,
                    backgroundColor = backgroundColor,
                    triggerFocusRequester = triggerFocusRequester,
                    onKeyboardOpen = {
                        openedFromKeyboard = true
                        onExpandedChange(true)
                    },
                    onExpandedChange = onExpandedChange
                )
            }
        }
    }
}

@Composable
private fun FormattingEntryRow(
    state: EditorFormattingUiState,
    backgroundColor: Color,
    triggerFocusRequester: FocusRequester,
    onKeyboardOpen: () -> Unit,
    onExpandedChange: (Boolean) -> Unit
) {
    val label = stringResource(R.string.formatting)
    val stateLabel = stringResource(if (state.expanded) R.string.expanded else R.string.collapsed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FormattingTooltip(label = label) {
            Surface(
                color = if (state.expanded) MaterialTheme.colorScheme.secondaryContainer else backgroundColor,
                contentColor = if (state.expanded) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                shape = MaterialTheme.shapes.small
            ) {
                IconButton(
                    onClick = { onExpandedChange(!state.expanded) },
                    enabled = state.enabled,
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(triggerFocusRequester)
                        .openPanelFromKeyboard(state.enabled, onKeyboardOpen)
                        .semantics {
                            contentDescription = label
                            stateDescription = stateLabel
                        }
                        .focusable(state.enabled)
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        state.statsText?.let { stats ->
            Text(
                text = stats,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SelectionFormattingActions(
    showLink: Boolean,
    expanded: Boolean,
    enabled: Boolean,
    triggerFocusRequester: FocusRequester,
    onKeyboardOpen: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onAction: (EditorFormattingAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContextAction(EditorFormattingAction.BOLD, Icons.Default.FormatBold, R.string.bold, enabled, onAction)
        ContextAction(EditorFormattingAction.ITALIC, Icons.Default.FormatItalic, R.string.italic, enabled, onAction)
        if (showLink) {
            ContextAction(EditorFormattingAction.LINK, Icons.Default.Link, R.string.markdown_link, enabled, onAction)
        }
        val moreLabel = stringResource(R.string.more_options)
        val moreStateLabel = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
        FormattingTooltip(label = moreLabel) {
            IconButton(
                onClick = { onExpandedChange(!expanded) },
                enabled = enabled,
                modifier = Modifier
                    .size(48.dp)
                    .focusRequester(triggerFocusRequester)
                    .openPanelFromKeyboard(enabled, onKeyboardOpen)
                    .semantics {
                        contentDescription = moreLabel
                        stateDescription = moreStateLabel
                    }
                    .focusable(enabled)
            ) {
                Icon(Icons.Default.MoreHoriz, contentDescription = null)
            }
        }
    }
}

@Composable
private fun ContextAction(
    action: EditorFormattingAction,
    icon: ImageVector,
    labelRes: Int,
    enabled: Boolean,
    onAction: (EditorFormattingAction) -> Unit
) {
    val label = stringResource(labelRes)
    FormattingTooltip(label = label) {
        IconButton(
            onClick = { onAction(action) },
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = label }
        ) {
            Icon(icon, contentDescription = null)
        }
    }
}

@Composable
private fun FormattingPanel(
    groups: List<FormattingGroup>,
    onAction: (EditorFormattingAction) -> Unit,
    onDismiss: () -> Unit,
    firstActionFocusRequester: FocusRequester,
    modifier: Modifier
) {
    val actionCount = groups.sumOf { it.items.size }
    val actionFocusRequesters = remember(actionCount, firstActionFocusRequester) {
        List(actionCount) { index ->
            if (index == 0) firstActionFocusRequester else FocusRequester()
        }
    }
    var focusedIndex by remember { mutableStateOf(0) }

    Surface(
        modifier = modifier
            .padding(bottom = 4.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                if (event.key == Key.Escape) {
                    onDismiss()
                    return@onPreviewKeyEvent true
                }
                val focusDelta = when (event.key) {
                    Key.Tab -> if (event.isShiftPressed) -1 else 1
                    Key.DirectionDown, Key.DirectionRight -> 1
                    Key.DirectionUp, Key.DirectionLeft -> -1
                    else -> null
                }
                if (focusDelta != null) {
                    focusedIndex = (focusedIndex + focusDelta + actionCount) % actionCount
                    actionFocusRequesters[focusedIndex].requestFocus()
                    return@onPreviewKeyEvent true
                }
                val shortcut = if (event.isCtrlPressed || event.isMetaPressed) {
                    when (event.key) {
                        Key.B -> EditorFormattingAction.BOLD
                        Key.I -> EditorFormattingAction.ITALIC
                        Key.K -> EditorFormattingAction.LINK
                        Key.S -> if (event.isShiftPressed) EditorFormattingAction.STRIKETHROUGH else null
                        else -> null
                    }
                } else {
                    null
                }
                if (shortcut == null) false else {
                    onAction(shortcut)
                    true
                }
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 268.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            var actionIndex = 0
            groups.forEach { group ->
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .semantics { heading() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                group.items.forEach { item ->
                    val currentIndex = actionIndex
                    FormattingActionRow(
                        item = item,
                        onClick = { onAction(item.action) },
                        onFocused = { focusedIndex = currentIndex },
                        modifier = Modifier.focusRequester(actionFocusRequesters[currentIndex])
                    )
                    actionIndex++
                }
            }
        }
    }
}

@Composable
private fun FormattingActionRow(
    item: FormattingItem,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusedBackground = if (isFocused) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (isFocused) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(focusedBackground, MaterialTheme.shapes.extraSmall)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.Spacebar)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
        Spacer(Modifier.width(12.dp))
        Text(text = item.label, style = MaterialTheme.typography.bodyMedium, color = contentColor)
    }
}

@Composable
private fun formattingGroups(): List<FormattingGroup> = listOf(
    FormattingGroup(
        stringResource(R.string.formatting_inline),
        listOf(
            FormattingItem(EditorFormattingAction.BOLD, stringResource(R.string.bold), Icons.Default.FormatBold),
            FormattingItem(EditorFormattingAction.ITALIC, stringResource(R.string.italic), Icons.Default.FormatItalic),
            FormattingItem(
                EditorFormattingAction.STRIKETHROUGH,
                stringResource(R.string.strikethrough),
                Icons.Default.FormatStrikethrough
            ),
            FormattingItem(EditorFormattingAction.INLINE_CODE, stringResource(R.string.inline_code), Icons.Default.Code),
            FormattingItem(EditorFormattingAction.LINK, stringResource(R.string.markdown_link), Icons.Default.Link)
        )
    ),
    FormattingGroup(
        stringResource(R.string.formatting_structure),
        listOf(
            FormattingItem(EditorFormattingAction.HEADING, stringResource(R.string.heading), Icons.Default.Title),
            FormattingItem(
                EditorFormattingAction.BULLET_LIST,
                stringResource(R.string.bullet_list),
                Icons.AutoMirrored.Filled.FormatListBulleted
            ),
            FormattingItem(
                EditorFormattingAction.ORDERED_LIST,
                stringResource(R.string.ordered_list),
                Icons.Default.FormatListNumbered
            ),
            FormattingItem(EditorFormattingAction.CHECKLIST, stringResource(R.string.checkbox), Icons.Default.CheckBox),
            FormattingItem(EditorFormattingAction.QUOTE, stringResource(R.string.blockquote), Icons.Default.FormatQuote)
        )
    ),
    FormattingGroup(
        stringResource(R.string.formatting_block_media),
        listOf(
            FormattingItem(EditorFormattingAction.CODE_BLOCK, stringResource(R.string.code_block), Icons.Default.DataObject),
            FormattingItem(EditorFormattingAction.DIVIDER, stringResource(R.string.horizontal_rule), Icons.Default.HorizontalRule),
            FormattingItem(EditorFormattingAction.IMAGE, stringResource(R.string.insert_image), Icons.Default.Image)
        )
    )
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FormattingTooltip(label: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
        content = content
    )
}

private fun Modifier.openPanelFromKeyboard(enabled: Boolean, onOpen: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (
            enabled &&
            event.type == KeyEventType.KeyDown &&
            (event.key == Key.Enter || event.key == Key.Spacebar)
        ) {
            onOpen()
            true
        } else {
            false
        }
    }
