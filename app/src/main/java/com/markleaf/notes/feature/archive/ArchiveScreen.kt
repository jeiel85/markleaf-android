package com.markleaf.notes.feature.archive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.feature.notes.sortArchivedNotes
import com.markleaf.notes.ui.component.NotesSortMenuButton
import com.markleaf.notes.ui.component.formatUpdatedTime
import com.markleaf.notes.ui.component.EmptyState
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.viewmodel.ArchiveViewModel
import com.markleaf.notes.navigation.NoteSource
import com.markleaf.notes.navigation.noteSharedBounds
import com.markleaf.notes.ui.component.pressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onBack: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    // The archive's own sort, kept in settings apart from the notes list's
    // (#592). Passed in rather than read here so the host owns the setting.
    sortMode: NotesSortMode = NotesSortMode.UPDATED_DESC,
    onSortModeChange: (NotesSortMode) -> Unit = {}
) {
    val notesState = remember { mutableStateOf<List<Note>>(emptyList()) }
    LaunchedEffect(Unit) {
        viewModel.archivedNotes.collect { notesState.value = it }
    }
    val notes = remember(notesState.value, sortMode) {
        sortArchivedNotes(notesState.value, sortMode)
    }
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    // A new order starts at the top. Rows are keyed by note id, so left alone
    // the list would keep whichever note was at the top and follow it to its
    // new place — sorting A to Z from the top would land at the bottom. Saved,
    // so coming back from a note is not a change and keeps the scroll.
    var scrolledForSort by rememberSaveable { mutableStateOf(sortMode) }
    LaunchedEffect(sortMode) {
        if (sortMode == scrolledForSort) return@LaunchedEffect
        scrolledForSort = sortMode
        listState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archive)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    NotesSortMenuButton(
                        sortMode = sortMode,
                        onSortModeChange = onSortModeChange
                    )
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
            if (notes.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Archive,
                    title = stringResource(R.string.archive_empty),
                    hint = stringResource(R.string.archive_empty_hint)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                    items(notes, key = { it.id }) { note ->
                        ArchiveRow(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onUnarchive = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.unarchive(note.id)
                            },
                            onMoveToTrash = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.moveToTrash(note.id)
                            },
                            onLongPress = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArchiveRow(
    note: Note,
    onClick: () -> Unit,
    onUnarchive: () -> Unit,
    onMoveToTrash: () -> Unit,
    onLongPress: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(Modifier.noteSharedBounds(note.id, NoteSource.ARCHIVE)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interactionSource)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Transparent)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = {
                        onLongPress()
                        menuExpanded = true
                    }
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (note.excerpt.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // The same date the notes list shows under each row (#592).
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatUpdatedTime(LocalContext.current, note.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Unarchive, contentDescription = null) },
                text = { Text(stringResource(R.string.unarchive)) },
                onClick = {
                    menuExpanded = false
                    onUnarchive()
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                text = { Text(stringResource(R.string.move_to_trash)) },
                onClick = {
                    menuExpanded = false
                    onMoveToTrash()
                }
            )
        }
    }
}
