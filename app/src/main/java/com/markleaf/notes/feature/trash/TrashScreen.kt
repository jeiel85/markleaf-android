package com.markleaf.notes.feature.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.ui.component.EmptyState
import com.markleaf.notes.ui.component.SelectionMark
import com.markleaf.notes.ui.component.SelectionTopAppBar
import com.markleaf.notes.ui.component.rememberNoteSelection
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.viewmodel.TrashViewModel
import com.markleaf.notes.util.AttachmentManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())

    val trashedNotesState = remember { mutableStateOf<List<Note>>(emptyList()) }
    LaunchedEffect(Unit) {
        viewModel.trashedNotes.collect { notes ->
            trashedNotesState.value = notes
        }
    }
    val trashedNotes = trashedNotesState.value

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    val showDeleteConfirm = remember { mutableStateOf(false) }
    // The notes the user saw when they asked to empty the trash. Confirming
    // deletes exactly these, so a note trashed while the dialog is open is
    // not deleted without having been on the screen (#566).
    var notesToEmpty by remember { mutableStateOf<List<Note>?>(null) }

    // Several trashed notes at once (#596). A row has no menu here, so a long
    // press starts the selection directly.
    val selection = rememberNoteSelection()
    LaunchedEffect(trashedNotes) {
        if (trashedNotes.isNotEmpty()) selection.retain(trashedNotes.mapTo(HashSet()) { it.id })
    }
    val picked = remember(trashedNotes, selection.ids) { selection.of(trashedNotes) }
    BackHandler(enabled = selection.active) { selection.clear() }
    // The picked notes, as they were when Delete forever was tapped: the
    // dialog deletes exactly what it counted, as Empty trash does (#566).
    var pickedToDelete by remember { mutableStateOf<List<Note>?>(null) }
    val selectLabel = stringResource(R.string.select)

    // Deleting forever drops the Room row; the attachment bytes and the
    // sync-folder file are cleaned up here, where the context and settings are.
    fun deleteForever(notes: List<Note>) {
        notes.forEach { viewModel.deleteForever(it.id) }
        // Disk cleanup of attachment files (Room CASCADE drops
        // the attachments table rows but the bytes would linger).
        scope.launch(Dispatchers.IO) {
            notes.forEach { AttachmentManager.deleteAllForNote(context, it.id) }
        }
        // Mirror DB-side delete to the sync folder if configured.
        appSettings.syncFolderUriOrNull()?.let { uri ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    notes.forEach {
                        NoteFolderMirror.deleteNote(
                            context, uri, it.id, appSettings.mirrorMetadata()
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (selection.active) {
                SelectionTopAppBar(
                    count = picked.size,
                    onClose = { selection.clear() },
                    actions = {
                        IconButton(
                            enabled = picked.size < trashedNotes.size,
                            onClick = { selection.selectAll(trashedNotes.map { it.id }) }
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                        IconButton(
                            enabled = picked.isNotEmpty(),
                            onClick = {
                                viewModel.restoreFromTrash(picked.map { it.id })
                                selection.clear()
                            }
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = stringResource(R.string.restore))
                        }
                        IconButton(
                            enabled = picked.isNotEmpty(),
                            onClick = { pickedToDelete = picked }
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.delete_forever))
                        }
                    }
                )
            } else TopAppBar(
                title = { Text(stringResource(R.string.trash)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (trashedNotes.isNotEmpty()) {
                        TextButton(onClick = { notesToEmpty = trashedNotes }) {
                            Text(stringResource(R.string.empty_trash))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            if (trashedNotes.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.DeleteOutline,
                    title = stringResource(R.string.trash_empty),
                    hint = stringResource(R.string.trash_empty_hint)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // With a retention set, say so where the notes wait (#566):
                    // nothing else on this screen hints they will go by themselves.
                    appSettings.trashRetention.days?.let { days ->
                        item(key = "retention-notice") {
                            Text(
                                text = pluralStringResource(R.plurals.trash_retention_notice, days, days),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(trashedNotes, key = { it.id }) { note ->
                        val checked = if (selection.active) note.id in selection.ids else null
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (checked == true) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.background
                                    }
                                )
                                .then(
                                    if (checked != null) {
                                        // While selecting the whole row is one
                                        // target, read with its selected state.
                                        Modifier.combinedClickable(
                                            onClick = { selection.toggle(note.id) },
                                            onLongClick = { selection.toggle(note.id) }
                                        ).semantics(mergeDescendants = true) { selected = checked }
                                    } else {
                                        // Otherwise a tap still does nothing and the
                                        // row's own buttons stay separate targets, so
                                        // only a long press is added, with a label for
                                        // TalkBack's "double-tap and hold".
                                        Modifier
                                            .pointerInput(note.id) {
                                                detectTapGestures(onLongPress = { selection.start(note.id) })
                                            }
                                            .semantics {
                                                onLongClick(label = selectLabel) {
                                                    selection.start(note.id)
                                                    true
                                                }
                                            }
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (checked != null) SelectionMark(checked)
                                Text(
                                    text = if (note.title.isBlank()) stringResource(R.string.untitled_parenthesized) else note.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                // Hidden while selecting: the bar acts on the picked
                                // notes, and a row's own Delete beside it would act
                                // on that row alone.
                                if (checked == null) Row {
                                    TextButton(onClick = { viewModel.restoreFromTrash(note.id) }) {
                                        Text(stringResource(R.string.restore))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = {
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
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = { Text(stringResource(R.string.delete_forever_title)) },
            text = { Text(stringResource(R.string.delete_forever_message)) },
            confirmButton = {
                Button(onClick = {
                    val note = noteToDelete
                    showDeleteConfirm.value = false
                    if (note != null) {
                        deleteForever(listOf(note))
                    }
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

    pickedToDelete?.let { notes ->
        AlertDialog(
            onDismissRequest = { pickedToDelete = null },
            title = { Text(stringResource(R.string.delete_forever_title)) },
            text = {
                Text(pluralStringResource(R.plurals.delete_notes_forever_message, notes.size, notes.size))
            },
            confirmButton = {
                Button(onClick = {
                    pickedToDelete = null
                    selection.clear()
                    deleteForever(notes)
                }) {
                    Text(stringResource(R.string.delete_forever))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickedToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    notesToEmpty?.let { notes ->
        AlertDialog(
            onDismissRequest = { notesToEmpty = null },
            title = { Text(stringResource(R.string.empty_trash_title)) },
            text = {
                Text(pluralStringResource(R.plurals.empty_trash_message, notes.size, notes.size))
            },
            confirmButton = {
                Button(onClick = {
                    notesToEmpty = null
                    deleteForever(notes)
                }) {
                    Text(stringResource(R.string.empty_trash))
                }
            },
            dismissButton = {
                TextButton(onClick = { notesToEmpty = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
