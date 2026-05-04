package com.markleaf.notes.feature.notes

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import com.markleaf.notes.util.HapticFeedback
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNoteClick: (String?) -> Unit,
    onFabClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCollapseClick: (() -> Unit)? = null,
    selectedNoteId: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val context = LocalContext.current
    val notesState = remember { mutableStateOf<List<Note>>(emptyList()) }
    LaunchedEffect(Unit) {
        viewModel.notes.collect { noteList ->
            notesState.value = noteList
        }
    }
    val notes = notesState.value

    Scaffold(
        containerColor = containerColor,
        contentColor = contentColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notes_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    if (onCollapseClick != null) {
                        IconButton(onClick = onCollapseClick) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.collapse_note_list))
                        }
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = onTagsClick) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(R.string.tags))
                    }
                    IconButton(onClick = onTrashClick) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.trash))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = contentColor,
                    actionIconContentColor = contentColor,
                    navigationIconContentColor = contentColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    HapticFeedback.medium(context)
                    onFabClick()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_note)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "\uD83D\uDCDD",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_notes_yet),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.create_first_note_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onFabClick,
                        modifier = Modifier.padding(top = 20.dp)
                    ) {
                        Text(stringResource(R.string.create_note))
                    }
                }
            }
        } else {
            var dragList by remember { mutableStateOf(notes) }
            var draggedItemIndex by remember { mutableIntStateOf(-1) }
            var dragOffsetY by remember { mutableFloatStateOf(0f) }
            val itemHeight = 72f // Approximate item height in pixels
            
            LaunchedEffect(notes) {
                if (draggedItemIndex == -1) {
                    dragList = notes
                }
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    NoteCountDashboard(notes = dragList)
                }
                items(dragList.size, key = { dragList[it].id }) { index ->
                    val note = dragList[index]
                    val isDragging = index == draggedItemIndex
                    
                    NoteItem(
                        note = note,
                        selected = note.id == selectedNoteId,
                        isDragging = isDragging,
                        dragOffset = if (isDragging) dragOffsetY else 0f,
                        onClick = { onNoteClick(note.id) },
                        onMoveToTrash = { viewModel.moveToTrash(note.id) },
                        onDragStart = {
                            draggedItemIndex = index
                            dragOffsetY = 0f
                            HapticFeedback.medium(context)
                        },
                        onDrag = { deltaY ->
                            dragOffsetY += deltaY
                            
                            // Calculate new position based on drag offset
                            val itemOffset = (dragOffsetY / itemHeight).toInt()
                            val newIndex = (draggedItemIndex + itemOffset)
                                .coerceIn(0, dragList.size - 1)
                            
                            if (newIndex != draggedItemIndex) {
                                // Reorder the list
                                val mutableList = dragList.toMutableList()
                                val item = mutableList.removeAt(draggedItemIndex)
                                mutableList.add(newIndex, item)
                                dragList = mutableList
                                draggedItemIndex = newIndex
                                dragOffsetY = 0f // Reset offset after reordering
                                HapticFeedback.light(context)
                            }
                        },
                        onDragEnd = {
                            if (draggedItemIndex != -1) {
                                viewModel.reorderNotes(dragList)
                            }
                            draggedItemIndex = -1
                            dragOffsetY = 0f
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCountDashboard(
    notes: List<Note>,
    modifier: Modifier = Modifier
) {
    val totalNotes = notes.size
    val pinnedNotes = notes.count { it.pinned }
    val totalTags = notes.flatMap { it.tags }.map { it.name }.distinct().size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountItem(
                count = totalNotes,
                label = stringResource(R.string.dashboard_total_notes),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
            CountItem(
                count = pinnedNotes,
                label = stringResource(R.string.dashboard_pinned_notes),
                icon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
            CountItem(
                count = totalTags,
                label = stringResource(R.string.dashboard_total_tags),
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            )
        }
    }
}

@Composable
fun CountItem(
    count: Int,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    selected: Boolean = false,
    isDragging: Boolean = false,
    dragOffset: Float = 0f,
    onClick: (String) -> Unit,
    onMoveToTrash: (String) -> Unit,
    onDragStart: (() -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val itemBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
    } else if (isDragging) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }
    
    val dragModifier = if (onDragStart != null && onDrag != null && onDragEnd != null) {
        Modifier.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { onDragStart() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.y)
                },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragEnd() }
            )
        }
    } else {
        Modifier
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffset
            }
            .clip(MaterialTheme.shapes.medium)
            .background(itemBackground)
            .combinedClickable(
                onClick = { onClick(note.id) },
                onLongClick = {
                    if (onDragStart == null) {
                        HapticFeedback.error(context)
                        onMoveToTrash(note.id)
                    }
                }
            )
            .then(dragModifier)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDragStart != null) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = stringResource(R.string.drag_to_reorder),
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = note.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                    MaterialTheme.colorScheme.primary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        if (note.excerpt.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
