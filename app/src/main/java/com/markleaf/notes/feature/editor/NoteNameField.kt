package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.core.text.NoteNames

internal const val NOTE_NAME_FIELD_TAG = "note_name_field"

/**
 * The note's name above the body, under the File name title rule (D088) — the
 * name is not a line of the note, so this is the only place it is shown and
 * the one place it is changed (#611).
 *
 * Stateless: the editor owns the draft so a name still being typed when the
 * screen closes is committed too, not dropped with this field's state. [value]
 * is the draft while editing and the note's name otherwise. Characters a file
 * name cannot hold never reach the draft ([NoteNames.filterTyped]); whether the
 * rest is free is only known on commit, and [problem] says why it was not.
 */
@Composable
internal fun NoteNameField(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    problem: NoteNames.Problem?,
    modifier: Modifier = Modifier
) {
    val style = MaterialTheme.typography.headlineSmall.copy(
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold
    )
    val label = stringResource(R.string.note_name_hint)
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(NoteNames.filterTyped(it)) },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(NOTE_NAME_FIELD_TAG)
                .semantics { contentDescription = label }
                .onFocusChanged { if (!it.isFocused) onCommit() },
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(text = label, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                }
                inner()
            }
        )
        if (problem != null) {
            Text(
                text = when (problem) {
                    NoteNames.Problem.TAKEN -> stringResource(R.string.note_name_taken)
                    NoteNames.Problem.RESERVED -> stringResource(R.string.note_name_reserved)
                    NoteNames.Problem.FOLDER_KEPT_ANOTHER -> stringResource(R.string.note_name_folder_kept_another)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** The note's name at the top of Preview, where the body shows no title of its own (#611). */
@Composable
internal fun NoteNameHeader(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name.ifEmpty { stringResource(R.string.untitled) },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}
