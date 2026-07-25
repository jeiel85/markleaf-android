package com.markleaf.notes.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.NotesLayout
import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.navigation.LocalNavAnimatedVisibilityScope
import com.markleaf.notes.navigation.LocalSharedTransitionScope
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNoteClick: (String?) -> Unit,
    onFabClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onLockedClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    lockPasscodeSet: Boolean = false,
    onRequestSetPasscode: () -> Unit = {},
    onCollapseClick: (() -> Unit)? = null,
    onShowTagRail: (() -> Unit)? = null,
    selectedNoteId: String? = null,
    selectedTag: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val notesState = remember { mutableStateOf<List<Note>>(emptyList()) }
    val displayedState = remember { mutableStateOf<List<Note>>(emptyList()) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showQuickSwitcher by remember { mutableStateOf(false) }
    // Shown when the user taps "Move to Locked" but hasn't set a passcode yet —
    // locking a note is only meaningful once there's a passcode to gate it (#155).
    var showSetPasscodePrompt by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.notes.collect { noteList ->
            notesState.value = noteList
        }
    }
    LaunchedEffect(Unit) {
        viewModel.displayedNotes.collect { noteList ->
            displayedState.value = noteList
        }
    }
    // The quick switcher and the "create your first note" state consider every
    // note; the list itself shows the tag-filtered subset (which equals the full
    // list on phones, where there is no tag rail to set a filter).
    val notes = notesState.value
    val displayed = displayedState.value
    val sortMode = appSettings.notesSortMode
    val sections = remember(displayed, sortMode) { groupNotesForDisplay(displayed, sortMode) }

    if (showQuickSwitcher) {
        QuickSwitcherDialog(
            notes = notes,
            onSelect = { id ->
                showQuickSwitcher = false
                onNoteClick(id)
            },
            onDismiss = { showQuickSwitcher = false }
        )
    }

    if (showSetPasscodePrompt) {
        AlertDialog(
            onDismissRequest = { showSetPasscodePrompt = false },
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            title = { Text(stringResource(R.string.locked_need_passcode_title)) },
            text = { Text(stringResource(R.string.locked_need_passcode_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showSetPasscodePrompt = false
                    onRequestSetPasscode()
                }) {
                    Text(stringResource(R.string.locked_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasscodePrompt = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            // Ctrl+K (or Cmd+K on physical Mac/iPad keyboards) opens the
            // quick switcher. No-op when no hardware keyboard is attached —
            // touch users reach the same dialog via the ⋮ overflow menu.
            if (event.type == KeyEventType.KeyDown &&
                event.key == Key.K &&
                (event.isCtrlPressed || event.isMetaPressed)
            ) {
                showQuickSwitcher = true
                true
            } else {
                false
            }
        },
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
                navigationIcon = {
                    // Re-show the tag rail after it was hidden (tablet only — the
                    // host passes this just when the rail is collapsed). Paired with
                    // the rail's own "<" hide button.
                    if (onShowTagRail != null) {
                        IconButton(onClick = onShowTagRail) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.show_tags)
                            )
                        }
                    }
                },
                actions = {
                    if (onCollapseClick != null) {
                        IconButton(onClick = onCollapseClick) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.collapse_note_list))
                        }
                    }
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.sort_notes)
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
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
                                        sortMenuExpanded = false
                                        scope.launch {
                                            settingsRepository.setNotesSortMode(mode)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = onTagsClick) {
                        Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(R.string.tags))
                    }
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.KeyboardCommandKey, contentDescription = null) },
                                text = { Text(stringResource(R.string.quick_switcher_title)) },
                                onClick = {
                                    overflowExpanded = false
                                    showQuickSwitcher = true
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                text = { Text(stringResource(R.string.archive)) },
                                onClick = {
                                    overflowExpanded = false
                                    onArchiveClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                text = { Text(stringResource(R.string.locked_notes_title)) },
                                onClick = {
                                    overflowExpanded = false
                                    onLockedClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                text = { Text(stringResource(R.string.trash)) },
                                onClick = {
                                    overflowExpanded = false
                                    onTrashClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                text = { Text(stringResource(R.string.settings)) },
                                onClick = {
                                    overflowExpanded = false
                                    onSettingsClick()
                                }
                            )
                        }
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
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        text = "📝",
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_notes_yet),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.create_first_note_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 6.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onFabClick,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.create_note),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        } else if (displayed.isEmpty() && selectedTag != null) {
            // A tag filter is active but matches nothing — don't show the
            // "create your first note" onboarding, which would be misleading.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_notes_for_tag),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            // Both layouts drive the same actions (#279). Defined once here so
            // the list and the grid cannot drift apart — in particular the lock
            // action, which has to clean up the mirror file as well.
            val togglePin: (Note) -> Unit = { note ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setPinned(note.id, !note.pinned)
            }
            val archive: (Note) -> Unit = { note ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setArchived(note.id, true)
            }
            val lock: (Note) -> Unit = { note ->
                if (lockPasscodeSet) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setLocked(note.id, true)
                    // The export guard only stops *future* saves, so a note
                    // mirrored before it was locked would leave a readable
                    // plaintext copy in the sync folder. Remove it now, or
                    // the Locked space's privacy promise is only skin deep.
                    // LockedNotesScreen re-mirrors on unlock (#156).
                    appSettings.syncFolderUriOrNull()?.let { uri ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                NoteFolderMirror.deleteNote(
                                    context,
                                    uri,
                                    note.id,
                                    appSettings.mirrorMetadata()
                                )
                            }
                        }
                    }
                } else {
                    showSetPasscodePrompt = true
                }
            }
            val moveToTrash: (Note) -> Unit = { note ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.moveToTrash(note.id)
            }
            val longPress: () -> Unit = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            when (appSettings.notesLayout) {
                NotesLayout.LIST -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    sections.forEach { section ->
                        if (section.titleResId != null) {
                            item(key = "header-${section.titleResId}") {
                                SectionHeader(
                                    stringResource(section.titleResId),
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        items(section.notes, key = { it.id }) { note ->
                            NoteRow(
                                note = note,
                                selected = note.id == selectedNoteId,
                                showPreview = appSettings.notesShowPreview,
                                modifier = Modifier.animateItem(),
                                onClick = { onNoteClick(note.id) },
                                onTogglePin = { togglePin(note) },
                                onArchive = { archive(note) },
                                onLock = { lock(note) },
                                onMoveToTrash = { moveToTrash(note) },
                                onLongPress = longPress
                            )
                        }
                    }
                }

                NotesLayout.GRID -> LazyVerticalGrid(
                    // Adaptive, not a fixed column count: this screen is also the
                    // tablet's narrow list pane beside the editor, where a fixed
                    // two columns would leave nothing readable in either.
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sections.forEach { section ->
                        if (section.titleResId != null) {
                            item(
                                key = "header-${section.titleResId}",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                SectionHeader(
                                    stringResource(section.titleResId),
                                    modifier = Modifier.animateItem(),
                                    // The grid already pads its content by 12dp;
                                    // the list's 24 would double up here.
                                    horizontalPadding = 12.dp
                                )
                            }
                        }
                        items(section.notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                selected = note.id == selectedNoteId,
                                showPreview = appSettings.notesShowPreview,
                                modifier = Modifier.animateItem(),
                                onClick = { onNoteClick(note.id) },
                                onTogglePin = { togglePin(note) },
                                onArchive = { archive(note) },
                                onLock = { lock(note) },
                                onMoveToTrash = { moveToTrash(note) },
                                onLongPress = longPress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 24.dp
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
            .padding(start = horizontalPadding, top = 20.dp, end = horizontalPadding, bottom = 6.dp)
            .semantics { heading() }
    )
}

/**
 * Source half of the card→editor container transform. Only active on the phone
 * nav path, where both scopes are published; the tablet in-pane editor provides
 * neither, so the item just renders normally.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.noteSharedBounds(noteId: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val avScope = LocalNavAnimatedVisibilityScope.current
    return if (sharedScope != null && avScope != null) {
        with(sharedScope) {
            this@noteSharedBounds.sharedBounds(
                rememberSharedContentState(key = "note-$noteId"),
                animatedVisibilityScope = avScope
            )
        }
    } else {
        this
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun NoteRow(
    note: Note,
    selected: Boolean,
    showPreview: Boolean,
    onClick: (String) -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onLock: () -> Unit,
    onMoveToTrash: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val itemBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
    } else {
        Color.Transparent
    }

    Box(modifier.noteSharedBounds(note.id)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(itemBackground)
                .combinedClickable(
                    onClick = { onClick(note.id) },
                    onLongClick = {
                        onLongPress()
                        menuExpanded = true
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.pinned),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .height(14.dp)
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
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showPreview) {
                if (note.excerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatUpdatedTime(LocalContext.current, note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        NoteContextMenu(
            note = note,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onTogglePin = onTogglePin,
            onArchive = onArchive,
            onLock = onLock,
            onMoveToTrash = onMoveToTrash
        )
    }
}

/**
 * A note as a grid tile (#279) — the same content and the same long-press menu
 * as [NoteRow], laid out for a column narrower than the screen: the title gets
 * two lines instead of one and the excerpt four instead of two, because a tile
 * has height to spare and width it does not.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun NoteCard(
    note: Note,
    selected: Boolean,
    showPreview: Boolean,
    onClick: (String) -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onLock: () -> Unit,
    onMoveToTrash: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val itemBackground = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Box(modifier.noteSharedBounds(note.id)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // A title-only tile is otherwise a thin sliver next to a tile
                // with an excerpt; the minimum keeps a row of them even.
                .heightIn(min = if (showPreview) 120.dp else 64.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(itemBackground)
                .combinedClickable(
                    onClick = { onClick(note.id) },
                    onLongClick = {
                        onLongPress()
                        menuExpanded = true
                    }
                )
                .padding(12.dp)
                .semantics(mergeDescendants = true) {}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.pinned),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 6.dp, top = 2.dp)
                            .height(14.dp)
                    )
                }
                Text(
                    text = note.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showPreview) {
                if (note.excerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = note.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatUpdatedTime(LocalContext.current, note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        NoteContextMenu(
            note = note,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onTogglePin = onTogglePin,
            onArchive = onArchive,
            onLock = onLock,
            onMoveToTrash = onMoveToTrash
        )
    }
}

/** The long-press menu shared by [NoteRow] and [NoteCard]. */
@Composable
private fun NoteContextMenu(
    note: Note,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onLock: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = if (note.pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                    contentDescription = null
                )
            },
            text = {
                Text(
                    if (note.pinned) stringResource(R.string.unpin)
                    else stringResource(R.string.pin)
                )
            },
            onClick = {
                onDismiss()
                onTogglePin()
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
            text = { Text(stringResource(R.string.archive_action)) },
            onClick = {
                onDismiss()
                onArchive()
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            text = { Text(stringResource(R.string.move_to_locked)) },
            onClick = {
                onDismiss()
                onLock()
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            text = { Text(stringResource(R.string.move_to_trash)) },
            onClick = {
                onDismiss()
                onMoveToTrash()
            }
        )
    }
}

private data class NoteSection(val titleResId: Int?, val notes: List<Note>)

private fun NotesSortMode.labelResId(): Int = when (this) {
    NotesSortMode.UPDATED_DESC -> R.string.sort_updated_newest
    NotesSortMode.UPDATED_ASC -> R.string.sort_updated_oldest
    NotesSortMode.TITLE_ASC -> R.string.sort_title_az
    NotesSortMode.TITLE_DESC -> R.string.sort_title_za
}

/**
 * Sections for the list under the active sort mode (#191). The default
 * newest-first mode keeps the Today/Yesterday date buckets; every other mode
 * renders a Pinned section (pinning outranks any sort) followed by one flat,
 * header-less section in the chosen order — date buckets would be misleading
 * when the list isn't date-ordered.
 */
private fun groupNotesForDisplay(notes: List<Note>, mode: NotesSortMode): List<NoteSection> {
    if (mode == NotesSortMode.UPDATED_DESC) return groupNotes(notes)
    if (notes.isEmpty()) return emptyList()

    val sorted = sortNotesForDisplay(notes, mode)
    val pinned = sorted.filter { it.pinned }
    val rest = sorted.filter { !it.pinned }
    val sections = mutableListOf<NoteSection>()
    if (pinned.isNotEmpty()) sections += NoteSection(R.string.section_pinned, pinned)
    if (rest.isNotEmpty()) sections += NoteSection(null, rest)
    return sections
}

private fun groupNotes(notes: List<Note>): List<NoteSection> {
    if (notes.isEmpty()) return emptyList()

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    val pinned = notes.filter { it.pinned }
    val rest = notes.filter { !it.pinned }

    val byBucket = linkedMapOf<Int, MutableList<Note>>()
    for (note in rest) {
        val date = note.updatedAt.atZone(zone).toLocalDate()
        val daysFromToday = ChronoUnit.DAYS.between(date, today)
        val bucket = when {
            daysFromToday <= 0L -> R.string.section_today
            daysFromToday == 1L -> R.string.section_yesterday
            daysFromToday in 2..7 -> R.string.section_past_week
            else -> R.string.section_older
        }
        byBucket.getOrPut(bucket) { mutableListOf() }.add(note)
    }

    val sections = mutableListOf<NoteSection>()
    if (pinned.isNotEmpty()) sections += NoteSection(R.string.section_pinned, pinned)
    val ordered = listOf(
        R.string.section_today,
        R.string.section_yesterday,
        R.string.section_past_week,
        R.string.section_older
    )
    for (bucket in ordered) {
        byBucket[bucket]?.takeIf { it.isNotEmpty() }?.let {
            sections += NoteSection(bucket, it)
        }
    }
    return sections
}

private fun formatUpdatedTime(context: Context, instant: Instant): String {
    val zone = ZoneId.systemDefault()
    val now = LocalDate.now(zone)
    val date = instant.atZone(zone).toLocalDate()
    val time = instant.atZone(zone).toLocalTime()

    val daysFromToday = ChronoUnit.DAYS.between(date, now)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    return when {
        daysFromToday <= 0L ->
            context.getString(R.string.relative_today, time.format(timeFormatter))
        daysFromToday == 1L ->
            context.getString(R.string.relative_yesterday, time.format(timeFormatter))
        daysFromToday in 2..7 ->
            context.getString(R.string.relative_days_ago, daysFromToday.toInt())
        else -> {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            date.format(dateFormatter)
        }
    }
}

