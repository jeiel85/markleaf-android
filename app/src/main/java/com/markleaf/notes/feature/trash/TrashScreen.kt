package com.markleaf.notes.feature.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.viewmodel.TrashViewModel

@Composable
fun TrashScreen(
    viewModel: TrashViewModel
) {
    val trashedNotesState = remember { mutableStateOf<List<Note>>(emptyList()) }
    LaunchedEffect(Unit) {
        viewModel.trashedNotes.collect { notes ->
            trashedNotesState.value = notes
        }
    }
    val trashedNotes = trashedNotesState.value

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    val showDeleteConfirm = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (trashedNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.trash_empty),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.trash_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(trashedNotes) { note ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (note.title.isBlank()) stringResource(R.string.untitled_parenthesized) else note.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Row {
                                Button(onClick = { viewModel.restoreFromTrash(note.id) }) {
                                    Text(stringResource(R.string.restore))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    noteToDelete = note
                                    showDeleteConfirm.value = true
                                }) {
                                    Text(stringResource(R.string.delete))
                                }
                            }
                        }
                        if (note.excerpt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.excerpt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = { Text(stringResource(R.string.delete_forever_title)) },
            text = { Text(stringResource(R.string.delete_forever_message)) },
            confirmButton = {
                Button(onClick = {
                    noteToDelete?.let { viewModel.deleteForever(it.id) }
                    showDeleteConfirm.value = false
                }) {
                    Text(stringResource(R.string.delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
