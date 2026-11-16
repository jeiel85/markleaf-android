package com.markleaf.notes.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.NotesSortMode

/**
 * The top-bar sort button and its menu, shared by the notes list (#191) and the
 * archive (#592). Each screen keeps its own setting; this only draws the choice.
 */
@Composable
internal fun NotesSortMenuButton(
    sortMode: NotesSortMode,
    onSortModeChange: (NotesSortMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.sort_notes)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NotesSortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    leadingIcon = {
                        if (mode == sortMode) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    text = { Text(stringResource(mode.labelResId())) },
                    onClick = {
                        expanded = false
                        onSortModeChange(mode)
                    }
                )
            }
        }
    }
}

private fun NotesSortMode.labelResId(): Int = when (this) {
    NotesSortMode.UPDATED_DESC -> R.string.sort_updated_newest
    NotesSortMode.UPDATED_ASC -> R.string.sort_updated_oldest
    NotesSortMode.TITLE_ASC -> R.string.sort_title_az
    NotesSortMode.TITLE_DESC -> R.string.sort_title_za
    NotesSortMode.CREATED_DESC -> R.string.sort_created_newest
    NotesSortMode.CREATED_ASC -> R.string.sort_created_oldest
}
