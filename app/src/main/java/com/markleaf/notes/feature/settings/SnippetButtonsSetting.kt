package com.markleaf.notes.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.SnippetButton
import com.markleaf.notes.data.settings.SnippetProblem

/**
 * Settings → Markdown → Snippet buttons (#436).
 *
 * Input: the current buttons, in row order, and whether a stored list exists
 * that couldn't be read. Output: [onChange] with the whole new list after any
 * add, edit, move or delete.
 *
 * An unreadable list shows as no buttons, and the first change replaces it, so
 * the screen says that before the user makes one rather than letting their
 * snippets disappear without a word (#262).
 *
 * Editing lives here rather than on the row itself: a `+` on the row would add
 * chrome for everyone who never uses snippets, and long-press there already
 * shows tooltips. Settings search finds this by name, so it isn't hidden.
 */
@Composable
internal fun SnippetButtonsSetting(
    buttons: List<SnippetButton>,
    onChange: (List<SnippetButton>) -> Unit,
    titleModifier: Modifier = Modifier,
    storedListUnreadable: Boolean = false
) {
    // Index being edited; [NEW_SNIPPET] for one being added, null when closed.
    var editing by remember { mutableStateOf<Int?>(null) }

    Text(
        text = stringResource(R.string.snippet_buttons_label),
        modifier = titleModifier,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.snippet_buttons_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (storedListUnreadable) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.snippet_buttons_unreadable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    Spacer(Modifier.height(4.dp))
    buttons.forEachIndexed { index, button ->
        SnippetListRow(
            button = button,
            canMoveUp = index > 0,
            canMoveDown = index < buttons.lastIndex,
            onEdit = { editing = index },
            onMoveUp = { onChange(buttons.swapped(index, index - 1)) },
            onMoveDown = { onChange(buttons.swapped(index, index + 1)) },
            onDelete = { onChange(buttons.filterIndexed { i, _ -> i != index }) }
        )
    }
    Spacer(Modifier.height(4.dp))
    OutlinedButton(
        onClick = { editing = NEW_SNIPPET },
        enabled = buttons.size < SnippetButton.MAX_COUNT
    ) {
        Text(stringResource(R.string.snippet_buttons_add))
    }

    editing?.let { index ->
        SnippetEditDialog(
            initial = buttons.getOrNull(index),
            onDismiss = { editing = null },
            onSave = { saved ->
                editing = null
                onChange(
                    if (index == NEW_SNIPPET) buttons + saved
                    else buttons.mapIndexed { i, old -> if (i == index) saved else old }
                )
            }
        )
    }
}

@Composable
private fun SnippetListRow(
    button: SnippetButton,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.snippet_buttons_edit), onClick = onEdit)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = button.displayLabel,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = button.preview,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.snippet_buttons_move_up))
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.snippet_buttons_move_down))
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.snippet_buttons_delete, button.displayLabel)
            )
        }
    }
}

/**
 * Add or edit one snippet. Save stays disabled until the snippet can be saved;
 * the reason, when there is one worth naming, replaces the field's hint.
 */
@Composable
private fun SnippetEditDialog(
    initial: SnippetButton?,
    onDismiss: () -> Unit,
    onSave: (SnippetButton) -> Unit
) {
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    val draft = SnippetButton(label = label.trim(), text = text)
    val problem = draft.problem

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.snippet_buttons_dialog_new else R.string.snippet_buttons_dialog_edit
                )
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(SnippetButton.MAX_TEXT_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.snippet_buttons_text)) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    minLines = 2,
                    maxLines = 6,
                    isError = problem == SnippetProblem.SEVERAL_MARKERS,
                    supportingText = {
                        Text(
                            stringResource(
                                if (problem == SnippetProblem.SEVERAL_MARKERS) R.string.snippet_buttons_error_markers
                                else R.string.snippet_buttons_text_help
                            )
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = SnippetButton.limitLabel(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.snippet_buttons_label_field)) },
                    singleLine = true,
                    isError = problem == SnippetProblem.LABEL_NEEDED,
                    supportingText = {
                        Text(
                            if (problem == SnippetProblem.LABEL_NEEDED) {
                                stringResource(R.string.snippet_buttons_error_label)
                            } else {
                                stringResource(R.string.snippet_buttons_label_help, SnippetButton.MAX_LABEL_LENGTH)
                            }
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }, enabled = problem == null) {
                Text(stringResource(R.string.snippet_buttons_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private const val NEW_SNIPPET = -1

private fun <T> List<T>.swapped(a: Int, b: Int): List<T> =
    toMutableList().also { list ->
        val held = list[a]
        list[a] = list[b]
        list[b] = held
    }
